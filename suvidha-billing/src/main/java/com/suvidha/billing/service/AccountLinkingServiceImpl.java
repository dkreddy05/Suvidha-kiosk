package com.suvidha.billing.service;

import com.suvidha.billing.config.OtpProperties;
import com.suvidha.billing.dto.request.*;
import com.suvidha.billing.dto.response.*;
import com.suvidha.billing.entity.*;
import com.suvidha.billing.enums.*;
import com.suvidha.billing.exception.*;
import com.suvidha.billing.repository.*;
import com.suvidha.billing.security.CitizenAuthDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
public class AccountLinkingServiceImpl implements AccountLinkingService {

    private static final Logger log = LoggerFactory.getLogger(AccountLinkingServiceImpl.class);

    private final ServiceAccountRepository serviceAccountRepository;
    private final AccountLinkRepository accountLinkRepository;
    private final AccountVerificationRequestRepository accountVerificationRequestRepository;
    private final OtpRedisService otpRedisService;
    private final OtpProperties otpProperties;
    private final SecureRandom secureRandom;
    private final RefNoGeneratorService refNoGeneratorService;

    public AccountLinkingServiceImpl(
            ServiceAccountRepository serviceAccountRepository,
            AccountLinkRepository accountLinkRepository,
            AccountVerificationRequestRepository accountVerificationRequestRepository,
            OtpRedisService otpRedisService,
            OtpProperties otpProperties,
            SecureRandom secureRandom,
            RefNoGeneratorService refNoGeneratorService) {
        this.serviceAccountRepository = serviceAccountRepository;
        this.accountLinkRepository = accountLinkRepository;
        this.accountVerificationRequestRepository = accountVerificationRequestRepository;
        this.otpRedisService = otpRedisService;
        this.otpProperties = otpProperties;
        this.secureRandom = secureRandom;
        this.refNoGeneratorService = refNoGeneratorService;
    }

    // ===================== CORE FLOW ===================== //

    @Override
    @Transactional
    public Object verifyOwnership(String citizenId, VerifyOwnershipRequest req) {

        String mobile = currentMobile();
        String sanitizedMobile = otpRedisService.sanitizeMobile(mobile);

        if (otpRedisService.isOtpLocked(sanitizedMobile)) {
            throw new OtpAttemptsExceededException("Too many failed attempts.");
        }

        Optional<ServiceAccount> accountOpt = serviceAccountRepository.findByAccountNoAndServiceType(
                req.getConsumerNo(), req.getServiceType());

        // ===== CASE 1: ACCOUNT EXISTS =====
        if (accountOpt.isPresent()) {

            ServiceAccount account = accountOpt.get();
            UtilityType utilityType = UtilityType.valueOf(req.getServiceType().name());

            // ✅ Direct linking (mobile match)
            if (sanitizedMobile.equals(account.getRegisteredMobile())) {

                account.setCitizenId(citizenId);
                account.setActive(true);
                serviceAccountRepository.save(account);

                saveLinkAudit(citizenId, req.getConsumerNo(), sanitizedMobile,
                        utilityType, LinkRequestStatus.COMPLETED);

                otpRedisService.deleteAllKeys(sanitizedMobile);

                return AccountLinkStatusResponse.of(
                        LinkRequestStatus.COMPLETED,
                        req.getConsumerNo(),
                        req.getServiceType());
            }

            // ===== OTP FLOW =====
            if (otpRedisService.isCooldownActive(sanitizedMobile)) {
                throw new OtpCooldownException("Wait before requesting another OTP");
            }

            String otp = generateOtp();
            otpRedisService.storeOtp(sanitizedMobile, otp, otpProperties.getTtl());
            otpRedisService.storeCooldown(sanitizedMobile, otpProperties.getCooldown());
            otpRedisService.storeContext(sanitizedMobile,
                    new OtpContext(req.getConsumerNo(), citizenId, req.getServiceType()),
                    otpProperties.getTtl());

            saveLinkAudit(citizenId, req.getConsumerNo(), sanitizedMobile,
                    utilityType, LinkRequestStatus.OTP_SENT);

            return AccountLinkStatusResponse.of(
                    LinkRequestStatus.OTP_SENT,
                    req.getConsumerNo(),
                    req.getServiceType());
        }

        // ===== CASE 2: ACCOUNT NOT FOUND =====
        Optional<AccountVerificationRequest> pending = accountVerificationRequestRepository
                .findByConsumerNoAndStatus(req.getConsumerNo(), VerificationStatus.PENDING);

        if (pending.isPresent()) {
            AccountVerificationRequest vr = pending.get();
            return VerificationStatusResponse.of(
                    vr.getRefNo(), vr.getStatus(),
                    vr.getConsumerNo(), vr.getServiceType());

        }

        return ProviderListResponse.of(req.getServiceType(), providersFor(req.getServiceType()));
    }

    @Override
    @Transactional
    public Object confirmLink(String citizenId, ConfirmLinkRequest req) {

        String mobile = currentMobile();
        String sanitizedMobile = otpRedisService.sanitizeMobile(mobile);

        OtpContext ctx = otpRedisService.getContext(sanitizedMobile)
                .orElseThrow(() -> new OtpExpiredException("OTP context expired"));

        if (!otpRedisService.verifyOtp(sanitizedMobile, req.getOtp())) {
            otpRedisService.incrementOtpAttempts(sanitizedMobile);
            throw new InvalidOtpException("Invalid OTP");
        }

        if (!citizenId.equals(ctx.getCitizenId())) {
            throw new UnauthorizedException("Citizen mismatch");
        }

        ServiceAccount account = serviceAccountRepository
                .findByAccountNoAndServiceTypeForUpdate(req.getConsumerNo(), req.getServiceType())
                .orElseGet(() -> ServiceAccount.builder()
                        .accountNo(req.getConsumerNo())
                        .serviceType(req.getServiceType())
                        .registeredMobile(sanitizedMobile)
                        .build());

        account.setCitizenId(citizenId);
        account.setActive(true);
        serviceAccountRepository.save(account);

        UtilityType utilityType = UtilityType.valueOf(req.getServiceType().name());

        saveLinkAudit(citizenId, req.getConsumerNo(), sanitizedMobile,
                utilityType, LinkRequestStatus.COMPLETED);

        otpRedisService.deleteAllKeys(sanitizedMobile);

        return AccountLinkStatusResponse.of(
                LinkRequestStatus.COMPLETED,
                req.getConsumerNo(),
                req.getServiceType());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountLinkStatusResponse trackStatus(String citizenId) {

        Optional<AccountLinkRequest> latest = accountLinkRepository.findFirstByCitizenIdOrderByCreatedAtDesc(citizenId);

        if (latest.isEmpty()) {
            return AccountLinkStatusResponse.of(null, null, null);
        }

        AccountLinkRequest req = latest.get();

        return AccountLinkStatusResponse.of(
                req.getStatus(),
                req.getAccountNo(),
                ServiceType.valueOf(req.getUtilityType().name()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountLinkHistoryResponse> getHistory(String citizenId) {

        return accountLinkRepository.findByCitizenId(citizenId)
                .stream()
                .map(req -> AccountLinkHistoryResponse.builder()
                        .id(req.getId().toString())
                        .citizenId(req.getCitizenId())
                        .accountNo(req.getAccountNo())
                        .mobile(req.getMobile())
                        .utilityType(req.getUtilityType())
                        .status(req.getStatus())
                        .createdAt(req.getCreatedAt())
                        .updatedAt(req.getUpdatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public CancelResponse cancelLink(String requestId) {

        UUID id = UUID.fromString(requestId);

        AccountLinkRequest req = accountLinkRepository.findById(id)
                .orElseThrow(() -> new AccountLinkNotFoundException("Not found"));

        if (req.getStatus() == LinkRequestStatus.COMPLETED) {
            throw new InvalidOtpException("Cannot cancel completed request");
        }

        String mobile = otpRedisService.sanitizeMobile(req.getMobile());
        otpRedisService.deleteAllKeys(mobile);

        req.setStatus(LinkRequestStatus.CANCELLED);
        accountLinkRepository.save(req);

        return new CancelResponse(
                requestId,
                LinkRequestStatus.CANCELLED.name(),
                "Cancelled successfully");
    }

    private void saveLinkAudit(String citizenId, String accountNo,
            String mobile, UtilityType utilityType,
            LinkRequestStatus status) {

        accountLinkRepository.save(AccountLinkRequest.builder()
                .citizenId(citizenId)
                .accountNo(accountNo)
                .mobile(mobile)
                .utilityType(utilityType)
                .status(status)
                .attemptCount(0)
                .build());
    }

    private String currentMobile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            throw new UnauthorizedException("Missing auth");

        Object details = auth.getDetails();
        if (details instanceof CitizenAuthDetails d)
            return d.getMobile();

        throw new UnauthorizedException("Missing mobile");
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private List<String> providersFor(ServiceType serviceType) {
        return switch (serviceType) {
            case ELECTRICITY -> List.of("Suvidha Electricity Board", "City Power Ltd");
            case WATER -> List.of("Suvidha Water Board", "City Water Supply");
            case GAS -> List.of("Suvidha Gas Board", "City Gas Networks");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Object getProviders(ServiceType serviceType) {
        return ProviderListResponse.of(serviceType, providersFor(serviceType));
    }

    @Override
    @Transactional
    public Object requestVerification(String citizenId, AccountVerificationRequestDto req) {
        String refNo = refNoGeneratorService.generateAvr();

        AccountVerificationRequest avr = AccountVerificationRequest.builder()
                .citizenId(citizenId)
                .consumerNo(req.getConsumerNo())
                .accountHolderName(req.getAccountHolderName())
                .registeredMobile(req.getRegisteredMobile())
                .address(req.getAddress())
                .providerName(req.getProviderName())
                .serviceType(req.getServiceType())
                .status(VerificationStatus.PENDING)
                .refNo(refNo)
                .build();

        accountVerificationRequestRepository.save(avr);

        return VerificationStatusResponse.of(
                refNo,
                VerificationStatus.PENDING,
                req.getConsumerNo(),
                req.getServiceType());
    }

    @Override
    @Transactional(readOnly = true)
    public Object getVerificationStatus(String refNo) {
        AccountVerificationRequest avr = accountVerificationRequestRepository.findByRefNo(refNo)
                .orElseThrow(() -> new AccountNotFoundException("Verification request not found for ref: " + refNo));
        return VerificationStatusResponse.of(
                avr.getRefNo(),
                avr.getStatus(),
                avr.getConsumerNo(),
                avr.getServiceType());
    }
}
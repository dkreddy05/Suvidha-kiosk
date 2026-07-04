package com.suvidha.billing.service;

import com.suvidha.billing.config.OtpProperties;
import com.suvidha.billing.dto.request.ConfirmLinkRequest;
import com.suvidha.billing.dto.request.VerifyOwnershipRequest;
import com.suvidha.billing.dto.response.AccountLinkStatusResponse;
import com.suvidha.billing.entity.ServiceAccount;
import com.suvidha.billing.enums.LinkRequestStatus;
import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.exception.InvalidOtpException;
import com.suvidha.billing.exception.OtpExpiredException;
import com.suvidha.billing.repository.AccountLinkRepository;
import com.suvidha.billing.repository.AccountVerificationRequestRepository;
import com.suvidha.billing.repository.ServiceAccountRepository;
import com.suvidha.billing.security.CitizenAuthDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountLinkingServiceImplTest {

    @Mock
    private ServiceAccountRepository serviceAccountRepository;

    @Mock
    private AccountLinkRepository accountLinkRepository;

    @Mock
    private AccountVerificationRequestRepository accountVerificationRequestRepository;

    @Mock
    private OtpRedisService otpRedisService;

    private AccountLinkingServiceImpl service;

    private final ByteArrayOutputStream outCapture = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        OtpProperties otpProperties = new OtpProperties();
        otpProperties.setTtl(Duration.ofMinutes(5));
        otpProperties.setCooldown(Duration.ofMinutes(2));

        service = new AccountLinkingServiceImpl(
                serviceAccountRepository,
                accountLinkRepository,
                accountVerificationRequestRepository,
                otpRedisService,
                otpProperties,
                new java.security.SecureRandom(),
                mock(RefNoGeneratorService.class));

        System.setOut(new PrintStream(outCapture));
        setSecurityContext("citizen-1", "9876543210");
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("verifyOwnership must not print OTP to stdout")
    void verifyOwnership_mustNotPrintOtpToStdout() {
        VerifyOwnershipRequest req = new VerifyOwnershipRequest();
        req.setConsumerNo("E123456789");
        req.setServiceType(ServiceType.ELECTRICITY);

        ServiceAccount account = ServiceAccount.builder()
                .accountNo("E123456789")
                .serviceType(ServiceType.ELECTRICITY)
                .registeredMobile("9999999999")
                .citizenId("other-citizen")
                .build();

        when(serviceAccountRepository.findByAccountNoAndServiceType("E123456789", ServiceType.ELECTRICITY))
                .thenReturn(Optional.of(account));
        when(otpRedisService.isCooldownActive("9876543210")).thenReturn(false);
        when(otpRedisService.sanitizeMobile("9876543210")).thenReturn("9876543210");
        when(otpRedisService.isOtpLocked("9876543210")).thenReturn(false);
        doNothing().when(otpRedisService).storeOtp(anyString(), anyString(), any(Duration.class));
        doNothing().when(otpRedisService).storeCooldown(anyString(), any(Duration.class));
        doNothing().when(otpRedisService).storeContext(anyString(), any(), any(Duration.class));

        service.verifyOwnership("citizen-1", req);

        String stdoutOutput = outCapture.toString();
        assertThat(stdoutOutput).doesNotContain("OTP");
        assertThat(stdoutOutput).doesNotMatch(".*\\d{6}.*");
    }

    @Test
    @DisplayName("verifyOwnership must not leak OTP value in any form to stdout")
    void verifyOwnership_mustNotLeakOtpValueInAnyForm() {
        VerifyOwnershipRequest req = new VerifyOwnershipRequest();
        req.setConsumerNo("E123456789");
        req.setServiceType(ServiceType.ELECTRICITY);

        ServiceAccount account = ServiceAccount.builder()
                .accountNo("E123456789")
                .serviceType(ServiceType.ELECTRICITY)
                .registeredMobile("9999999999")
                .citizenId("other-citizen")
                .build();

        when(serviceAccountRepository.findByAccountNoAndServiceType("E123456789", ServiceType.ELECTRICITY))
                .thenReturn(Optional.of(account));
        when(otpRedisService.isCooldownActive("9876543210")).thenReturn(false);
        when(otpRedisService.sanitizeMobile("9876543210")).thenReturn("9876543210");
        when(otpRedisService.isOtpLocked("9876543210")).thenReturn(false);
        doNothing().when(otpRedisService).storeOtp(anyString(), anyString(), any(Duration.class));
        doNothing().when(otpRedisService).storeCooldown(anyString(), any(Duration.class));
        doNothing().when(otpRedisService).storeContext(anyString(), any(), any(Duration.class));

        service.verifyOwnership("citizen-1", req);

        String stdoutOutput = outCapture.toString();
        assertThat(stdoutOutput).isEmpty();
    }

    @Test
    @DisplayName("verifyOwnership returns OTP_SENT when mobile does not match")
    void verifyOwnership_returnsOtpSentWhenMobileMismatch() {
        VerifyOwnershipRequest req = new VerifyOwnershipRequest();
        req.setConsumerNo("E123456789");
        req.setServiceType(ServiceType.ELECTRICITY);

        ServiceAccount account = ServiceAccount.builder()
                .accountNo("E123456789")
                .serviceType(ServiceType.ELECTRICITY)
                .registeredMobile("9999999999")
                .citizenId("other-citizen")
                .build();

        when(serviceAccountRepository.findByAccountNoAndServiceType("E123456789", ServiceType.ELECTRICITY))
                .thenReturn(Optional.of(account));
        when(otpRedisService.isCooldownActive("9876543210")).thenReturn(false);
        when(otpRedisService.sanitizeMobile("9876543210")).thenReturn("9876543210");
        when(otpRedisService.isOtpLocked("9876543210")).thenReturn(false);
        doNothing().when(otpRedisService).storeOtp(anyString(), anyString(), any(Duration.class));
        doNothing().when(otpRedisService).storeCooldown(anyString(), any(Duration.class));
        doNothing().when(otpRedisService).storeContext(anyString(), any(), any(Duration.class));

        Object result = service.verifyOwnership("citizen-1", req);

        assertThat(result).isInstanceOf(AccountLinkStatusResponse.class);
        AccountLinkStatusResponse response = (AccountLinkStatusResponse) result;
        assertThat(response.getStatus()).isEqualTo(LinkRequestStatus.OTP_SENT);
    }

    @Test
    @DisplayName("verifyOwnership completes link when mobile matches")
    void verifyOwnership_completesLinkWhenMobileMatches() {
        VerifyOwnershipRequest req = new VerifyOwnershipRequest();
        req.setConsumerNo("E123456789");
        req.setServiceType(ServiceType.ELECTRICITY);

        ServiceAccount account = ServiceAccount.builder()
                .accountNo("E123456789")
                .serviceType(ServiceType.ELECTRICITY)
                .registeredMobile("9876543210")
                .citizenId(null)
                .build();

        when(serviceAccountRepository.findByAccountNoAndServiceType("E123456789", ServiceType.ELECTRICITY))
                .thenReturn(Optional.of(account));
        when(serviceAccountRepository.save(any(ServiceAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(otpRedisService.sanitizeMobile("9876543210")).thenReturn("9876543210");
        when(otpRedisService.isOtpLocked("9876543210")).thenReturn(false);
        doNothing().when(otpRedisService).deleteAllKeys("9876543210");

        Object result = service.verifyOwnership("citizen-1", req);

        assertThat(result).isInstanceOf(AccountLinkStatusResponse.class);
        AccountLinkStatusResponse response = (AccountLinkStatusResponse) result;
        assertThat(response.getStatus()).isEqualTo(LinkRequestStatus.COMPLETED);
    }

    private void setSecurityContext(String citizenId, String mobile) {
        CitizenAuthDetails details = new CitizenAuthDetails(mobile);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(citizenId, null, java.util.List.of());
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("confirmLink uses verifyOtp (hashed) not getOtp (plaintext)")
    void confirmLink_usesVerifyOtpNotGetOtp() {
        ConfirmLinkRequest req = new ConfirmLinkRequest();
        req.setConsumerNo("E123456789");
        req.setOtp("123456");
        req.setServiceType(ServiceType.ELECTRICITY);

        OtpContext ctx = new OtpContext("E123456789", "citizen-1", ServiceType.ELECTRICITY);

        when(otpRedisService.sanitizeMobile("9876543210")).thenReturn("9876543210");
        when(otpRedisService.getContext("9876543210")).thenReturn(Optional.of(ctx));
        when(otpRedisService.verifyOtp("9876543210", "123456")).thenReturn(true);
        when(serviceAccountRepository.findByAccountNoAndServiceTypeForUpdate("E123456789", ServiceType.ELECTRICITY))
                .thenReturn(Optional.empty());
        when(serviceAccountRepository.save(any(ServiceAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(otpRedisService).deleteAllKeys("9876543210");

        Object result = service.confirmLink("citizen-1", req);

        assertThat(result).isInstanceOf(AccountLinkStatusResponse.class);
        verify(otpRedisService).verifyOtp("9876543210", "123456");
        verify(otpRedisService, never()).getOtp(anyString());
    }

    @Test
    @DisplayName("confirmLink rejects wrong OTP via verifyOtp")
    void confirmLink_rejectsWrongOtp() {
        ConfirmLinkRequest req = new ConfirmLinkRequest();
        req.setConsumerNo("E123456789");
        req.setOtp("999999");
        req.setServiceType(ServiceType.ELECTRICITY);

        OtpContext ctx = new OtpContext("E123456789", "citizen-1", ServiceType.ELECTRICITY);

        when(otpRedisService.sanitizeMobile("9876543210")).thenReturn("9876543210");
        when(otpRedisService.getContext("9876543210")).thenReturn(Optional.of(ctx));
        when(otpRedisService.verifyOtp("9876543210", "999999")).thenReturn(false);
        doNothing().when(otpRedisService).incrementOtpAttempts("9876543210");

        assertThatThrownBy(() -> service.confirmLink("citizen-1", req))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid OTP");

        verify(otpRedisService).incrementOtpAttempts("9876543210");
    }

    @Test
    @DisplayName("confirmLink throws when OTP expired (no hash in Redis)")
    void confirmLink_throwsWhenOtpExpired() {
        ConfirmLinkRequest req = new ConfirmLinkRequest();
        req.setConsumerNo("E123456789");
        req.setOtp("123456");
        req.setServiceType(ServiceType.ELECTRICITY);

        when(otpRedisService.sanitizeMobile("9876543210")).thenReturn("9876543210");
        when(otpRedisService.getContext("9876543210")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmLink("citizen-1", req))
                .isInstanceOf(OtpExpiredException.class)
                .hasMessageContaining("OTP context expired");
    }
}

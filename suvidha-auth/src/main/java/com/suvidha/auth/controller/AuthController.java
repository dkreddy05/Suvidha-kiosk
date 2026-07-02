package com.suvidha.auth.controller;

import com.suvidha.auth.Dto.CitizenDTO;
import com.suvidha.auth.Dto.OtpSendResponseDTO;
import com.suvidha.auth.Dto.OtpRequest;
import com.suvidha.auth.Dto.VerifyOtpRequest;
import com.suvidha.auth.Dto.RefreshTokenResponseDTO;
import com.suvidha.auth.Dto.RegisterRequest;
import com.suvidha.auth.Dto.UserAuthDto;
import com.suvidha.auth.Dto.VerifyOtpResponse;
import com.suvidha.auth.Dto.VerifyOtpResponseDTO;
import com.suvidha.auth.model.Citizen;
import com.suvidha.auth.model.RefreshToken;
import com.suvidha.auth.repo.CitizenRepo;
import com.suvidha.auth.service.AuthenticationService;
import com.suvidha.auth.service.AuditService;
import com.suvidha.auth.service.RefreshTokenService;
import com.suvidha.auth.service.UserService;
import com.suvidha.auth.service.RsaKeyService;
import com.suvidha.auth.exception.InvalidRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.suvidha.auth.token.JwtToken;

@RestController
@RequestMapping({ "/api/auth", "/api/v1/auth" })
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final CitizenRepo citizenRepo;
    private final JwtToken jwtToken;
    private final RsaKeyService rsaKeyService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    public AuthController(
            AuthenticationService authenticationService,
            UserService userService,
            CitizenRepo citizenRepo,
            JwtToken jwtToken,
            RsaKeyService rsaKeyService,
            RefreshTokenService refreshTokenService,
            AuditService auditService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.citizenRepo = citizenRepo;
        this.jwtToken = jwtToken;
        this.rsaKeyService = rsaKeyService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "suvidha-auth");
    }

    @GetMapping("/public-key")
    public ResponseEntity<java.util.List<Map<String, Object>>> getPublicKeys() {
        java.util.List<Map<String, Object>> keys = rsaKeyService.getAllKeys().stream()
            .map(k -> Map.<String, Object>of(
                "kid", k.getKid(),
                "public_key", k.getPublicKey(),
                "is_active", k.isActive(),
                "expires_at", k.getExpiresAt()
            )).toList();
        return ResponseEntity.ok(keys);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<OtpSendResponseDTO> sendOtp(@Valid @RequestBody OtpRequest otpRequest,
                                                       HttpServletRequest request) {
        StringBuilder devOtp = new StringBuilder();
        String sessionId = authenticationService.sendOtp(otpRequest.getSessionId(), otpRequest.getMobile(), devOtp);
        auditService.log("SEND_OTP", otpRequest.getMobile(), "OTP sent to mobile", request);
        return ResponseEntity.ok(new OtpSendResponseDTO(sessionId, "OTP sent.", devOtp.toString()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponseDTO> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest,
                                                           HttpServletRequest request) {
        VerifyOtpResponse result = authenticationService.verifyOtp(verifyOtpRequest.getSessionId(), verifyOtpRequest.getOtp());
        auditService.log("VERIFY_OTP", result.getMobile(), "OTP verified", request);

        boolean isNewUser = !result.isRegistered();
        CitizenDTO citizen = buildCitizen(result.getMobile(), result.isRegistered());
        
        String citizenId = isNewUser ? result.getMobile() : citizen.getId();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(citizenId);

        return ResponseEntity.ok(new VerifyOtpResponseDTO(result.getToken(), refreshToken.getToken(), citizen, isNewUser));
    }

    @GetMapping("/profile")
    public ResponseEntity<CitizenDTO> profile() {
        String principal = getAuthenticatedMobile();
        boolean exists = citizenRepo.findById(principal).isPresent() || citizenRepo.findByMobile(principal).isPresent();
        CitizenDTO citizen = buildCitizen(principal, exists);
        return ResponseEntity.ok(citizen);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@RequestBody Map<String, String> request) {
        String refreshTokenStr = request.get("refresh_token");
        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            refreshTokenStr = request.get("refreshToken");
        }
        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            throw new InvalidRequestException("Refresh token is required");
        }

        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshTokenStr);
        String citizenId = newRefreshToken.getCitizenId();
        
        Citizen citizen = citizenRepo.findById(citizenId).orElse(null);
        String mobile = citizen != null ? citizen.getMobile() : citizenId;
        String name = citizen != null ? citizen.getName() : "";
        
        String role = citizen != null && citizen.getRole() != null
            ? citizen.getRole().name()
            : "USER";
        String newAccessToken = jwtToken.generateToken(citizenId, mobile, name, role);
        
        return ResponseEntity.ok(new RefreshTokenResponseDTO(newAccessToken, newRefreshToken.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> request,
                                       HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7).trim();
            if (!accessToken.isBlank()) {
                try {
                    jwtToken.blacklistToken(accessToken);
                } catch (Exception e) {
                    log.warn("Logout: failed to blacklist access token: {}", e.getMessage());
                }
            }
        }

        String refreshTokenStr = request.get("refresh_token");
        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            refreshTokenStr = request.get("refreshToken");
        }
        String citizenId = null;
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            try {
                RefreshToken rt = refreshTokenService.verifyRefreshToken(refreshTokenStr);
                citizenId = rt.getCitizenId();
                refreshTokenService.revokeRefreshToken(refreshTokenStr);
            } catch (Exception e) {
                log.warn("Logout: failed to revoke refresh token: {}", e.getMessage());
            }
        }
        auditService.log("LOGOUT", citizenId != null ? citizenId : "unknown", "User logged out", httpRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<VerifyOtpResponseDTO> register(@Valid @RequestBody RegisterRequest request,
                                                          HttpServletRequest httpRequest) {
        UserAuthDto user = userService.registerUser(request);
        String token = jwtToken.generateToken(user.getId(), user.getMobile(), user.getName(), user.getRole());
        CitizenDTO citizen = buildCitizen(user.getMobile(), true);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        auditService.log("REGISTER", user.getId(), "User registered", httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new VerifyOtpResponseDTO(token, refreshToken.getToken(), citizen, false));
    }

    private String getAuthenticatedMobile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth != null ? auth.getPrincipal() : null;
        return principal != null ? String.valueOf(principal) : null;
    }

    private CitizenDTO buildCitizen(String identifier, boolean registered) {
        if (identifier == null || identifier.isBlank()) {
            return new CitizenDTO("", "", null, null, "en", Instant.now(), null);
        }

        if (registered) {
            Optional<Citizen> userOpt = citizenRepo.findById(identifier);
            if (userOpt.isEmpty()) {
                userOpt = citizenRepo.findByMobile(identifier);
            }
            if (userOpt.isPresent()) {
                Citizen user = userOpt.get();
                String aadhar = user.getAadhar();
                String aadhaarLast4 = aadhar != null && aadhar.length() >= 4 ? aadhar.substring(aadhar.length() - 4)
                        : null;
                String lang = user.getLanguagePreference();
                if (lang == null || lang.isBlank())
                    lang = "en";
                return new CitizenDTO(user.getId(), user.getMobile(), aadhaarLast4, user.getName(), lang,
                        user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now(), user.getConsumerId());
            }
        }

        return new CitizenDTO(identifier, identifier, null, null, "en", Instant.now(), null);
    }
}

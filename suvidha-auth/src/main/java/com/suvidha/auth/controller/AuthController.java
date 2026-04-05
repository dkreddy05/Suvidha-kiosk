package com.suvidha.auth.controller;

import com.suvidha.auth.Dto.CitizenDTO;
import com.suvidha.auth.Dto.OtpSendResponseDTO;
import com.suvidha.auth.Dto.OtpRequest;
import com.suvidha.auth.Dto.RefreshTokenResponseDTO;
import com.suvidha.auth.Dto.RegisterRequest;
import com.suvidha.auth.Dto.VerifyOtpResponse;
import com.suvidha.auth.Dto.VerifyOtpResponseDTO;
import com.suvidha.auth.repo.CitizenRepo;
import com.suvidha.auth.model.Citizen;
import com.suvidha.auth.service.AuthenticationService;
import com.suvidha.auth.service.UserService;
import java.util.Map;
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

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final CitizenRepo citizenRepo;
    private final JwtToken jwtToken;

    public AuthController(
            AuthenticationService authenticationService,
            UserService userService,
            CitizenRepo citizenRepo,
            JwtToken jwtToken) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.citizenRepo = citizenRepo;
        this.jwtToken = jwtToken;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "suvidha-auth");
    }

    @PostMapping("/send-otp")
    public ResponseEntity<OtpSendResponseDTO> sendOtp(@RequestBody OtpRequest otpRequest) {
        String sessionId = authenticationService.sendOtp(otpRequest.getSessionId(), otpRequest.getMobile());

        return ResponseEntity.ok(new OtpSendResponseDTO(sessionId, "OTP sent.", null));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponseDTO> verifyOtp(@RequestBody OtpRequest otpRequest) {
        VerifyOtpResponse result = authenticationService.verifyOtp(otpRequest.getSessionId(), otpRequest.getOtp());

        boolean isNewUser = !result.isRegistered();
        CitizenDTO citizen = buildCitizen(result.getMobile(), result.isRegistered());

        return ResponseEntity.ok(new VerifyOtpResponseDTO(result.getToken(), citizen, isNewUser));
    }

    @GetMapping("/profile")
    public ResponseEntity<CitizenDTO> profile() {
        String mobile = getAuthenticatedMobile();
        CitizenDTO citizen = buildCitizen(mobile, citizenRepo.findByMobile(mobile).isPresent());
        return ResponseEntity.ok(citizen);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken() {
        String mobile = getAuthenticatedMobile();
        String token = jwtToken.generateToken(mobile);
        return ResponseEntity.ok(new RefreshTokenResponseDTO(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(request));
    }

    private String getAuthenticatedMobile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth != null ? auth.getPrincipal() : null;
        return principal != null ? String.valueOf(principal) : null;
    }

    private CitizenDTO buildCitizen(String mobile, boolean registered) {
        if (mobile == null || mobile.isBlank()) {
            return new CitizenDTO("", "", null, null, "en", Instant.now());
        }

        if (registered) {
            Optional<Citizen> userOpt = citizenRepo.findByMobile(mobile);
            if (userOpt.isPresent()) {
                Citizen user = userOpt.get();
                String aadhar = user.getAadhar();
                String aadhaarLast4 = aadhar != null && aadhar.length() >= 4 ? aadhar.substring(aadhar.length() - 4)
                        : null;
                String lang = user.getLanguagePreference();
                if (lang == null || lang.isBlank())
                    lang = "en";
                return new CitizenDTO(user.getId(), user.getMobile(), aadhaarLast4, user.getName(), lang,
                        user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now());
            }
        }

        return new CitizenDTO(mobile, mobile, null, null, "en", Instant.now());
    }
}

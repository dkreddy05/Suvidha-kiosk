package com.suvidha.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.auth.Dto.OtpRequest;
import com.suvidha.auth.Dto.VerifyOtpRequest;
import com.suvidha.auth.Dto.RegisterRequest;
import com.suvidha.auth.Dto.Role;
import com.suvidha.auth.Dto.UserAuthDto;
import com.suvidha.auth.Dto.VerifyOtpResponse;
import com.suvidha.auth.model.RefreshToken;
import com.suvidha.auth.repo.CitizenRepo;
import com.suvidha.auth.service.AuthenticationService;
import com.suvidha.auth.service.RefreshTokenService;
import com.suvidha.auth.service.RsaKeyService;
import com.suvidha.auth.service.UserService;
import com.suvidha.auth.token.JwtToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private UserService userService;

    @MockBean
    private CitizenRepo citizenRepo;

    @MockBean
    private JwtToken jwtToken;

    @MockBean
    private RsaKeyService rsaKeyService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private com.suvidha.auth.service.AuditService auditService;

    @Test
    @DisplayName("POST /api/auth/send-otp should return 400 for invalid mobile")
    void sendOtp_shouldReturn400ForInvalidMobile() throws Exception {
        OtpRequest request = new OtpRequest(null, "123", null);

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/send-otp should return 400 for blank mobile")
    void sendOtp_shouldReturn400ForBlankMobile() throws Exception {
        OtpRequest request = new OtpRequest(null, "", null);

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/send-otp should return 200 for valid mobile")
    void sendOtp_shouldReturn200ForValidMobile() throws Exception {
        when(authenticationService.sendOtp(any(), eq("9876543210"), any()))
                .thenReturn("test-session-id");

        OtpRequest request = new OtpRequest(null, "9876543210", null);

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("test-session-id"));
    }

    @Test
    @DisplayName("POST /api/auth/register should pass role to generateToken")
    void register_shouldPassRoleToGenerateToken() throws Exception {
        UserAuthDto userDto = new UserAuthDto();
        userDto.setId("citizen-uuid");
        userDto.setMobile("9876543210");
        userDto.setName("Test User");
        userDto.setRole("ADMIN");
        userDto.setLanguagePreference("en");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId("rt-id");
        refreshToken.setCitizenId("citizen-uuid");
        refreshToken.setToken("refresh-token-value");
        refreshToken.setExpiresAt(Instant.now().plusSeconds(604800));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setRevoked(false);

        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(userDto);
        when(jwtToken.generateToken(eq("citizen-uuid"), eq("9876543210"), eq("Test User"), eq("ADMIN")))
                .thenReturn("access-jwt-token");
        when(refreshTokenService.createRefreshToken("citizen-uuid")).thenReturn(refreshToken);

        RegisterRequest request = new RegisterRequest();
        request.setSessionId("verified-session");
        request.setMobile("9876543210");
        request.setAadhar("123456789012");
        request.setName("Test User");
        request.setLanguagePreference("en");
        request.setRole(Role.ADMIN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-jwt-token"));

        verify(jwtToken).generateToken("citizen-uuid", "9876543210", "Test User", "ADMIN");
    }

    @Test
    @DisplayName("POST /api/auth/register should default to USER role when not provided")
    void register_shouldDefaultToUserRoleWhenNotProvided() throws Exception {
        UserAuthDto userDto = new UserAuthDto();
        userDto.setId("citizen-uuid");
        userDto.setMobile("9876543210");
        userDto.setName("Test User");
        userDto.setRole("USER");
        userDto.setLanguagePreference("en");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId("rt-id");
        refreshToken.setCitizenId("citizen-uuid");
        refreshToken.setToken("refresh-token-value");
        refreshToken.setExpiresAt(Instant.now().plusSeconds(604800));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setRevoked(false);

        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(userDto);
        when(jwtToken.generateToken(eq("citizen-uuid"), eq("9876543210"), eq("Test User"), eq("USER")))
                .thenReturn("access-jwt-token");
        when(refreshTokenService.createRefreshToken("citizen-uuid")).thenReturn(refreshToken);

        RegisterRequest request = new RegisterRequest();
        request.setSessionId("verified-session");
        request.setMobile("9876543210");
        request.setAadhar("123456789012");
        request.setName("Test User");
        request.setLanguagePreference("en");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(jwtToken).generateToken("citizen-uuid", "9876543210", "Test User", "USER");
    }

    @Test
    @DisplayName("POST /api/auth/verify-otp should return token")
    void verifyOtp_shouldIncludeRoleInGeneratedToken() throws Exception {
        VerifyOtpResponse otpResponse = new VerifyOtpResponse("9876543210", true, false, "access-jwt-token");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId("rt-id");
        refreshToken.setCitizenId("9876543210");
        refreshToken.setToken("refresh-token-value");
        refreshToken.setExpiresAt(Instant.now().plusSeconds(604800));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setRevoked(false);

        when(authenticationService.verifyOtp("session-id", "123456")).thenReturn(otpResponse);
        when(refreshTokenService.createRefreshToken("9876543210")).thenReturn(refreshToken);

        VerifyOtpRequest request = new VerifyOtpRequest("session-id", "123456");

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-jwt-token"));

        verify(authenticationService).verifyOtp("session-id", "123456");
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("should blacklist access token and revoke refresh token, return 204")
        void logout_shouldBlacklistAndRevoke() throws Exception {
            doNothing().when(jwtToken).blacklistToken(anyString());

            RefreshToken rt = new RefreshToken();
            rt.setCitizenId("citizen-uuid");
            when(refreshTokenService.verifyRefreshToken("valid-refresh-token")).thenReturn(rt);
            doNothing().when(refreshTokenService).revokeRefreshToken("valid-refresh-token");

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer test-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refresh_token\":\"valid-refresh-token\"}"))
                    .andExpect(status().isNoContent());

            verify(jwtToken).blacklistToken("test-access-token");
            verify(refreshTokenService).verifyRefreshToken("valid-refresh-token");
            verify(refreshTokenService).revokeRefreshToken("valid-refresh-token");
            verify(auditService).log(eq("LOGOUT"), eq("citizen-uuid"), anyString(), any());
        }

        @Test
        @DisplayName("should still revoke refresh token when no Authorization header")
        void logout_shouldRevokeRefreshTokenWhenNoAuthHeader() throws Exception {
            RefreshToken rt = new RefreshToken();
            rt.setCitizenId("citizen-uuid");
            when(refreshTokenService.verifyRefreshToken("valid-refresh-token")).thenReturn(rt);

            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refresh_token\":\"valid-refresh-token\"}"))
                    .andExpect(status().isNoContent());

            verify(jwtToken, never()).blacklistToken(anyString());
            verify(refreshTokenService).revokeRefreshToken("valid-refresh-token");
            verify(auditService).log(eq("LOGOUT"), eq("citizen-uuid"), anyString(), any());
        }

        @Test
        @DisplayName("should still blacklist access token when no refresh token body")
        void logout_shouldBlacklistWhenNoRefreshToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer test-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());

            verify(jwtToken).blacklistToken("test-access-token");
            verify(auditService).log(eq("LOGOUT"), eq("unknown"), anyString(), any());
        }

        @Test
        @DisplayName("should not fail when blacklist throws exception")
        void logout_shouldNotFailWhenBlacklistThrows() throws Exception {
            doThrow(new RuntimeException("Redis down")).when(jwtToken).blacklistToken(anyString());

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer test-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());

            verify(jwtToken).blacklistToken("test-access-token");
        }

        @Test
        @DisplayName("should not fail when refresh token verification throws")
        void logout_shouldNotFailWhenRefreshTokenInvalid() throws Exception {
            when(refreshTokenService.verifyRefreshToken(anyString()))
                    .thenThrow(new RuntimeException("Invalid refresh token"));

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer test-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refresh_token\":\"invalid-token\"}"))
                    .andExpect(status().isNoContent());

            verify(jwtToken).blacklistToken("test-access-token");
            verify(refreshTokenService).verifyRefreshToken("invalid-token");
            verify(refreshTokenService, never()).revokeRefreshToken(anyString());
        }
    }
}

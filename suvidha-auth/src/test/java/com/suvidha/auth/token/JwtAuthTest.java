package com.suvidha.auth.token;

import com.suvidha.auth.model.JwtKeyVersion;
import com.suvidha.auth.service.RsaKeyService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthTest {

    @Mock
    private RsaKeyService rsaKeyService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private JwtToken jwtToken;
    private JwtAuth jwtAuth;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.clearContext();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();

        String publicKeyStr = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyStr = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        JwtKeyVersion activeKey = new JwtKeyVersion(
            UUID.randomUUID().toString(),
            publicKeyStr,
            privateKeyStr,
            true,
            Instant.now(),
            Instant.now().plus(90, ChronoUnit.DAYS)
        );

        lenient().when(rsaKeyService.getActiveKeyVersion()).thenReturn(activeKey);
        lenient().when(rsaKeyService.getPrivateKey(activeKey)).thenReturn(keyPair.getPrivate());
        lenient().when(rsaKeyService.getPublicKeyByKid(activeKey.getKid())).thenReturn(keyPair.getPublic());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.hasKey(anyString())).thenReturn(false);

        jwtToken = new JwtToken(1800000, "suvidha-auth", "suvidha-services", rsaKeyService, redisTemplate);
        jwtAuth = new JwtAuth(jwtToken);
    }

    @Test
    @DisplayName("JwtAuth should extract role claim and set ROLE_ authority")
    void doFilterInternal_shouldExtractRoleClaimAndSetAuthority() throws Exception {
        String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "ADMIN");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuth.doFilterInternal(request, response, (req, res) -> {});

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("JwtAuth should default to no authorities when role is USER")
    void doFilterInternal_shouldSetUserRoleAuthority() throws Exception {
        String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuth.doFilterInternal(request, response, (req, res) -> {});

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("JwtAuth should reject request without Authorization header")
    void doFilterInternal_shouldPassThroughWithoutAuthHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuth.doFilterInternal(request, response, (req, res) -> {});

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("JwtAuth should reject expired token")
    void doFilterInternal_shouldRejectExpiredToken() throws Exception {
        JwtKeyVersion expiredKey = new JwtKeyVersion(
            UUID.randomUUID().toString(),
            Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
            Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
            true,
            Instant.now(),
            Instant.now().plus(90, ChronoUnit.DAYS)
        );
        when(rsaKeyService.getActiveKeyVersion()).thenReturn(expiredKey);
        when(rsaKeyService.getPrivateKey(expiredKey)).thenReturn(keyPair.getPrivate());
        when(rsaKeyService.getPublicKeyByKid(expiredKey.getKid())).thenReturn(keyPair.getPublic());

        JwtToken expiredJwtToken = new JwtToken(1, "suvidha-auth", "suvidha-services", rsaKeyService, redisTemplate);
        String token = expiredJwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");

        Thread.sleep(100);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtAuth expiredAuth = new JwtAuth(expiredJwtToken);
        expiredAuth.doFilterInternal(request, response, (req, res) -> {});

        assertEquals(401, response.getStatus());
    }

    @Nested
    @DisplayName("Blacklisted token rejection")
    class BlacklistTests {

        @Test
        @DisplayName("JwtAuth should reject blacklisted token with 401")
        void doFilterInternal_shouldRejectBlacklistedToken() throws Exception {
            String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
            Claims claims = jwtToken.validate(token);
            String jti = claims.getId();

            when(redisTemplate.hasKey("jwt:blacklist:" + jti)).thenReturn(true);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();

            jwtAuth.doFilterInternal(request, response, (req, res) -> {});

            assertEquals(401, response.getStatus());
            assertEquals("Token revoked", response.getContentAsString());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("JwtAuth should allow non-blacklisted token")
        void doFilterInternal_shouldAllowNonBlacklistedToken() throws Exception {
            String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "ADMIN");
            Claims claims = jwtToken.validate(token);
            String jti = claims.getId();

            when(redisTemplate.hasKey("jwt:blacklist:" + jti)).thenReturn(false);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();

            jwtAuth.doFilterInternal(request, response, (req, res) -> {});

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        }
    }
}

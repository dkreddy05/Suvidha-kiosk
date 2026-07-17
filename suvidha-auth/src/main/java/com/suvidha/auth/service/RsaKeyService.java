package com.suvidha.auth.service;

import com.suvidha.auth.model.JwtKeyVersion;
import com.suvidha.auth.repo.JwtKeyVersionRepo;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RsaKeyService {

    private final JwtKeyVersionRepo jwtKeyVersionRepo;

    public RsaKeyService(JwtKeyVersionRepo jwtKeyVersionRepo) {
        this.jwtKeyVersionRepo = jwtKeyVersionRepo;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "rsaKeyRotation", lockAtMostFor = "10m")
    public void rotateKeys() {
        log.info("Running scheduled RSA key rotation check");
        if (jwtKeyVersionRepo.count() == 0) {
            log.info("No RSA keys found. Generating initial key pair...");
            generateNewKey();
        } else {
            JwtKeyVersion activeKey = jwtKeyVersionRepo.findByIsActiveTrue().orElse(null);
            if (activeKey == null || activeKey.getExpiresAt().isBefore(Instant.now())) {
                log.info("Active RSA key is missing or expired. Rotating keys...");
                generateNewKey();
                log.info("Key rotation completed successfully");
            } else {
                log.debug("Active key {} is still valid until {}", activeKey.getKid(), activeKey.getExpiresAt());
            }
        }
    }

    public JwtKeyVersion getActiveKeyVersion() {
        return jwtKeyVersionRepo.findByIsActiveTrue().orElseGet(this::generateNewKey);
    }

    public List<JwtKeyVersion> getAllKeys() {
        return jwtKeyVersionRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public JwtKeyVersion generateNewKey() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();

            String publicKeyStr = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
            String privateKeyStr = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

            // Deactivate old active key
            jwtKeyVersionRepo.findByIsActiveTrue().ifPresent(oldKey -> {
                oldKey.setActive(false);
                jwtKeyVersionRepo.save(oldKey);
            });

            JwtKeyVersion newKey = new JwtKeyVersion(
                    UUID.randomUUID().toString(),
                    publicKeyStr,
                    privateKeyStr,
                    true,
                    Instant.now(),
                    Instant.now().plus(90, ChronoUnit.DAYS)
            );

            return jwtKeyVersionRepo.save(newKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    public PrivateKey getPrivateKey(JwtKeyVersion keyVersion) {
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(keyVersion.getPrivateKey());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse private key", e);
        }
    }

    public PublicKey getPublicKey(String publicKeyStr) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse public key", e);
        }
    }

    public PublicKey getPublicKeyByKid(String kid) {
        return jwtKeyVersionRepo.findByKid(kid)
                .map(k -> getPublicKey(k.getPublicKey()))
                .orElse(null);
    }
}

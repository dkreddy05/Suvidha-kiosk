package com.suvidha.auth.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class AadharEncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private volatile SecretKeySpec keySpec;

    private SecretKeySpec getKeySpec() {
        if (keySpec == null) {
            synchronized (this) {
                if (keySpec == null) {
                    String envKey = System.getenv("AADHAR_ENCRYPTION_KEY");
                    if (envKey == null || envKey.isBlank()) {
                        throw new IllegalStateException(
                                "AADHAR_ENCRYPTION_KEY environment variable is not set. " +
                                "It must be set to a 32-byte (or longer) AES-256 key.");
                    }
                    if (envKey.getBytes(StandardCharsets.UTF_8).length < 32) {
                        throw new IllegalStateException(
                                "AADHAR_ENCRYPTION_KEY must be at least 32 bytes. Got " +
                                envKey.getBytes(StandardCharsets.UTF_8).length + " bytes.");
                    }
                    keySpec = new SecretKeySpec(envKey.getBytes(StandardCharsets.UTF_8), "AES");
                }
            }
        }
        return keySpec;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(), gcmSpec);
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Aadhaar encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] decoded = Base64.getDecoder().decode(dbData);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getKeySpec(), gcmSpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Aadhaar decryption failed", e);
        }
    }

    public static String generateAadharHash(String plainTextAadhar) {
        String key = System.getProperty("AADHAR_BLIND_INDEX_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("AADHAR_BLIND_INDEX_KEY");
        }
        return generateAadharHash(plainTextAadhar, key);
    }

    public static String generateAadharHash(String plainTextAadhar, String pepper) {
        if (plainTextAadhar == null) return null;
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("AADHAR_BLIND_INDEX_KEY environment variable is not set.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] input = (plainTextAadhar + pepper).getBytes(StandardCharsets.UTF_8);
            byte[] hash = digest.digest(input);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Aadhaar hash generation failed", e);
        }
    }
}

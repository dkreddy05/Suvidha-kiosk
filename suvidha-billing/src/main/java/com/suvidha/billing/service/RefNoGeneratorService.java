package com.suvidha.billing.service;

import com.suvidha.billing.repository.AccountVerificationRequestRepository;
import com.suvidha.billing.repository.NewConnectionRequestRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class RefNoGeneratorService {

    private final SecureRandom secureRandom;
    private final AccountVerificationRequestRepository avrRepo;
    private final NewConnectionRequestRepository connRepo;

    public RefNoGeneratorService(SecureRandom secureRandom,
                                AccountVerificationRequestRepository avrRepo,
                                NewConnectionRequestRepository connRepo) {
        this.secureRandom = secureRandom;
        this.avrRepo = avrRepo;
        this.connRepo = connRepo;
    }

    public String generateAvr() {
        for (int i = 0; i < 100; i++) {
            String ref = "AVR-" + fourDigits();
            if (!avrRepo.existsByRefNo(ref)) {
                return ref;
            }
        }
        throw new IllegalStateException("Failed to generate unique AVR refNo");
    }

    public String generateConn() {
        for (int i = 0; i < 100; i++) {
            String ref = "CONN-" + fourDigits();
            if (connRepo.findByRefNo(ref).isEmpty()) {
                return ref;
            }
        }
        throw new IllegalStateException("Failed to generate unique CONN refNo");
    }

    private String fourDigits() {
        int n = secureRandom.nextInt(10_000);
        return String.format("%04d", n);
    }
}

package com.suvidha.auth.service;

import com.suvidha.auth.model.AadharEncryptionConverter;
import com.suvidha.auth.model.Citizen;
import com.suvidha.auth.repo.CitizenRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AadharDataMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(AadharDataMigrationRunner.class);

    private static final String MIGRATION_FLAG_KEY = "aadhar_migration_complete";

    @PersistenceContext
    private EntityManager em;

    private final CitizenRepo citizenRepo;

    public AadharDataMigrationRunner(CitizenRepo citizenRepo) {
        this.citizenRepo = citizenRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateExistingAadharData() {
        Query checkFlag = em.createNativeQuery(
            "SELECT COUNT(*) FROM auth.citizens_table WHERE aadhar_hash IS NULL AND aadhar IS NOT NULL");
        long pending = ((Number) checkFlag.getSingleResult()).longValue();
        if (pending == 0) {
            return;
        }
        log.info("Migrating {} existing aadhar records to new hash format", pending);

        Query query = em.createNativeQuery(
            "SELECT id FROM auth.citizens_table WHERE aadhar IS NOT NULL AND aadhar_hash IS NULL");
        @SuppressWarnings("unchecked")
        List<String> ids = query.getResultList();

        int count = 0;
        for (String id : ids) {
            Citizen citizen = citizenRepo.findById(id).orElse(null);
            if (citizen == null || citizen.getAadhar() == null) continue;
            String plaintextAadhar = citizen.getAadhar();
            String aadharHash = AadharEncryptionConverter.generateAadharHash(plaintextAadhar);
            em.createNativeQuery(
                "UPDATE auth.citizens_table SET aadhar_hash = :hash WHERE id = :id")
                .setParameter("hash", aadharHash)
                .setParameter("id", id)
                .executeUpdate();
            count++;
        }
        log.info("Successfully migrated {} aadhar records", count);
    }
}

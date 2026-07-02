package com.suvidha.grievance.repo;

import com.suvidha.grievance.model.Grievance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrievanceRepo extends JpaRepository<Grievance, Long> {
    Optional<Grievance> findByReferenceNumber(String referenceNumber);
    Page<Grievance> findByCitizenIdOrderBySubmittedAtDesc(String citizenId, Pageable pageable);
    Page<Grievance> findByCitizenIdAndStatusOrderBySubmittedAtDesc(String citizenId, String status, Pageable pageable);
}

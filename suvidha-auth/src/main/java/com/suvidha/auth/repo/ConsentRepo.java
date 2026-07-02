package com.suvidha.auth.repo;

import com.suvidha.auth.model.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsentRepo extends JpaRepository<ConsentRecord, String> {
    List<ConsentRecord> findByCitizenId(String citizenId);
}

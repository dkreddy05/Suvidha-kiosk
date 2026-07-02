package com.suvidha.auth.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import com.suvidha.auth.model.Citizen;

@Repository
public interface CitizenRepo extends JpaRepository<Citizen, String> {
    Optional<Citizen> findByMobile(String mobile);

    Optional<Citizen> findByAadhar(String aadhar);

    Optional<Citizen> findByAadharAndMobile(String aadhar, String mobile);

    Optional<Citizen> findByConsumerId(String consumerId);
}

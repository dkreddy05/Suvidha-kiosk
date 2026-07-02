package com.suvidha.auth.repo;

import com.suvidha.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByCitizenIdAndRevokedFalse(String citizenId);
    void deleteByCitizenId(String citizenId);
}
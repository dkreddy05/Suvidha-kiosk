package com.suvidha.auth.repo;

import com.suvidha.auth.model.JwtKeyVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JwtKeyVersionRepo extends JpaRepository<JwtKeyVersion, String> {
    Optional<JwtKeyVersion> findByIsActiveTrue();
    List<JwtKeyVersion> findAllByOrderByCreatedAtDesc();
    Optional<JwtKeyVersion> findByKid(String kid);
}

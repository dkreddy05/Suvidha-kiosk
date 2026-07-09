package com.suvidha.connections.repository;

import com.suvidha.connections.model.ConnectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, UUID> {

    List<ConnectionRequest> findByCitizenIdOrderBySubmittedAtDesc(String citizenId);

    Optional<ConnectionRequest> findByDisplayId(String displayId);

    @Query(value = "SELECT nextval('connection_display_seq')", nativeQuery = true)
    long nextDisplaySequence();
}

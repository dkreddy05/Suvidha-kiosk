package com.suvidha.connections.repository;

import com.suvidha.connections.model.ConnectionTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ConnectionTimelineRepository extends JpaRepository<ConnectionTimeline, UUID> {
}

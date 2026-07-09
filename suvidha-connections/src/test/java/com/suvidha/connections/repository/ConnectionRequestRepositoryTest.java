package com.suvidha.connections.repository;

import com.suvidha.connections.model.ConnectionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Sql(statements = "CREATE SEQUENCE IF NOT EXISTS connection_display_seq START WITH 10000")
class ConnectionRequestRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ConnectionRequestRepository repository;

    private ConnectionRequest createRequest(String citizenId, String displayId) {
        return ConnectionRequest.builder()
                .id(UUID.randomUUID())
                .displayId(displayId)
                .citizenId(citizenId)
                .serviceType("WATER")
                .address("123 Main St")
                .status("SUBMITTED")
                .submittedAt(Instant.now())
                .estimatedDays(7)
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("findByDisplayId returns request")
    void findByDisplayId() {
        ConnectionRequest req = createRequest("citizen-1", "CONN-10001");
        em.persistAndFlush(req);

        Optional<ConnectionRequest> found = repository.findByDisplayId("CONN-10001");
        assertTrue(found.isPresent());
        assertEquals("citizen-1", found.get().getCitizenId());
    }

    @Test
    @DisplayName("findByCitizenIdOrderBySubmittedAtDesc returns citizen's requests")
    void findByCitizenId() {
        em.persist(createRequest("citizen-A", "CONN-10001"));
        em.persist(createRequest("citizen-B", "CONN-10002"));
        em.persist(createRequest("citizen-A", "CONN-10003"));
        em.flush();

        List<ConnectionRequest> aReqs = repository.findByCitizenIdOrderBySubmittedAtDesc("citizen-A");
        assertEquals(2, aReqs.size());

        List<ConnectionRequest> bReqs = repository.findByCitizenIdOrderBySubmittedAtDesc("citizen-B");
        assertEquals(1, bReqs.size());
    }

    @Test
    @DisplayName("nextDisplaySequence returns incrementing values")
    void nextDisplaySequence() {
        long s1 = repository.nextDisplaySequence();
        long s2 = repository.nextDisplaySequence();
        long s3 = repository.nextDisplaySequence();

        assertTrue(s1 < s2);
        assertTrue(s2 < s3);
    }

    @Test
    @DisplayName("display_id is unique")
    void displayIdUnique() {
        em.persistAndFlush(createRequest("citizen-1", "CONN-10001"));

        ConnectionRequest dup = createRequest("citizen-2", "CONN-10001");
        assertThrows(Exception.class, () -> {
            em.persistAndFlush(dup);
        });
    }
}

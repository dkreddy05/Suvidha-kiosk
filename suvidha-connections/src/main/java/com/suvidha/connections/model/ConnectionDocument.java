package com.suvidha.connections.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "connection_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionDocument {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", nullable = false)
    private ConnectionRequest connection;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Column(name = "base64", nullable = false, columnDefinition = "TEXT")
    private String base64;
}

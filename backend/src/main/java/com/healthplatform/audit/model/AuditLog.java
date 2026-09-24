package com.healthplatform.audit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    private UUID actorId;
    private String actorEmail;
    private String actorRole;

    @Column(nullable = false)
    private String action;

    private String resourceType;
    private String resourceId;
    private String httpMethod;
    private String path;
    private Integer statusCode;

    @Column(nullable = false)
    private String outcome;

    private String ipAddress;
    private String requestId;
    private String detail;
}

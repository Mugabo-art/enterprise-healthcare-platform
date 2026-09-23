package com.healthplatform.lab.model;

import com.healthplatform.patient.model.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "requested_by_id")
    private UUID requestedById;

    @Column(name = "test_type", nullable = false)
    private String testType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LabRequestStatus status = LabRequestStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private Instant createdAt = Instant.now();
    private Instant updatedAt;
}

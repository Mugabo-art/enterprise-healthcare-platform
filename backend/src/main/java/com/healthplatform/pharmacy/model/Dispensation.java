package com.healthplatform.pharmacy.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispensations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispensation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "prescription_id", nullable = false, unique = true)
    private UUID prescriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "dispensed_by_id")
    private UUID dispensedById;

    @Builder.Default
    private Instant dispensedAt = Instant.now();
}

package com.healthplatform.medicalhistory.model;

import com.healthplatform.patient.model.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medical_history_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalHistoryEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HistoryCategory category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate recordedDate;

    @Column(name = "recorded_by_id")
    private UUID recordedById;

    @Builder.Default
    private Instant createdAt = Instant.now();
}

package com.healthplatform.attachment.model;

import com.healthplatform.patient.model.Patient;
import com.healthplatform.visit.model.Visit;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSizeBytes;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] fileData;

    @Column(name = "uploaded_by_id")
    private UUID uploadedById;

    @Builder.Default
    private Instant createdAt = Instant.now();
}

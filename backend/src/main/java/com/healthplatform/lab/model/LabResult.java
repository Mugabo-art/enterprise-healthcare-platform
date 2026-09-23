package com.healthplatform.lab.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "lab_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "lab_request_id", nullable = false, unique = true)
    private UUID labRequestId;

    @Column(name = "recorded_by_id")
    private UUID recordedById;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_data", nullable = false)
    private Map<String, Object> resultData;

    @Builder.Default
    @Column(name = "recorded_at")
    private Instant recordedAt = Instant.now();
}

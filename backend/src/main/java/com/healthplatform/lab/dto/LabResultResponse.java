package com.healthplatform.lab.dto;

import com.healthplatform.lab.model.LabResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LabResultResponse(
        UUID id, UUID labRequestId, UUID recordedById, Map<String, Object> resultData, Instant recordedAt
) {
    public static LabResultResponse from(LabResult r) {
        return new LabResultResponse(
                r.getId(), r.getLabRequestId(), r.getRecordedById(), r.getResultData(), r.getRecordedAt()
        );
    }
}

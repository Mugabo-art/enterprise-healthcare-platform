package com.healthplatform.audit.dto;

import com.healthplatform.audit.model.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        Instant occurredAt,
        UUID actorId,
        String actorEmail,
        String actorRole,
        String action,
        String resourceType,
        String resourceId,
        String httpMethod,
        String path,
        Integer statusCode,
        String outcome,
        String ipAddress,
        String requestId,
        String detail
) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getOccurredAt(), a.getActorId(), a.getActorEmail(), a.getActorRole(),
                a.getAction(), a.getResourceType(), a.getResourceId(), a.getHttpMethod(), a.getPath(),
                a.getStatusCode(), a.getOutcome(), a.getIpAddress(), a.getRequestId(), a.getDetail());
    }
}

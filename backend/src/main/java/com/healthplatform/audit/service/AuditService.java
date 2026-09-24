package com.healthplatform.audit.service;

import com.healthplatform.audit.dto.AuditLogResponse;
import com.healthplatform.audit.model.AuditLog;
import com.healthplatform.audit.repository.AuditLogRepository;
import com.healthplatform.auth.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Writes the append-only audit trail. Recording NEVER throws: an audit-store outage must not
 * take patient care down with it, so failures are logged loudly (ERROR) instead of propagated.
 */
@Service
public class AuditService {

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILURE = "FAILURE";
    public static final String OUTCOME_DENIED = "DENIED";

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** Security-relevant event (login, logout, registration...) with an explicit actor. */
    public void recordEvent(String action, User actor, String actorEmail, String outcome, String detail, String ipAddress) {
        save(AuditLog.builder()
                .action(action)
                .actorId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : actorEmail)
                .actorRole(actor != null && actor.getRole() != null ? actor.getRole().name() : null)
                .outcome(outcome)
                .detail(truncate(detail, 500))
                .ipAddress(ipAddress != null ? ipAddress : currentClientIp())
                .requestId(MDC.get("requestId"))
                .build());
    }

    private static String currentClientIp() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest().getRemoteAddr()
                : null;
    }

    /** One authenticated API call — the "who accessed which record" trail. */
    public void recordAccess(User actor, String httpMethod, String path, String resourceType, String resourceId,
                             int statusCode, String ipAddress) {
        save(AuditLog.builder()
                .action(actionFor(httpMethod))
                .actorId(actor.getId())
                .actorEmail(actor.getEmail())
                .actorRole(actor.getRole() != null ? actor.getRole().name() : null)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .httpMethod(httpMethod)
                .path(truncate(path, 255))
                .statusCode(statusCode)
                .outcome(outcomeFor(statusCode))
                .ipAddress(ipAddress)
                .requestId(MDC.get("requestId"))
                .build());
    }

    public Page<AuditLogResponse> search(UUID actorId, String resourceType, String resourceId, Pageable pageable) {
        Page<AuditLog> page;
        if (actorId != null) {
            page = repository.findByActorId(actorId, pageable);
        } else if (resourceType != null && resourceId != null) {
            page = repository.findByResourceTypeAndResourceId(resourceType, resourceId, pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return page.map(AuditLogResponse::from);
    }

    static String actionFor(String httpMethod) {
        return switch (httpMethod) {
            case "GET", "HEAD" -> "READ";
            case "POST" -> "CREATE";
            case "DELETE" -> "DELETE";
            default -> "UPDATE"; // PUT / PATCH
        };
    }

    static String outcomeFor(int status) {
        if (status == 401 || status == 403) return OUTCOME_DENIED;
        return status >= 400 ? OUTCOME_FAILURE : OUTCOME_SUCCESS;
    }

    private void save(AuditLog entry) {
        try {
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.error("AUDIT WRITE FAILED action={} actor={} resource={}/{}", entry.getAction(), entry.getActorId(),
                    entry.getResourceType(), entry.getResourceId(), ex);
        }
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}

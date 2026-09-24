package com.healthplatform.audit.service;

import com.healthplatform.auth.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Records every authenticated /api call (method, resource, status, actor, IP) after it completes.
 * Runs after the Spring Security chain, so the authenticated principal is available.
 * /api/auth/** is excluded here — auth events are recorded explicitly by AuthService.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class AuditLoggingFilter extends OncePerRequestFilter {

    // /api/<resource>[/<uuid>...]
    private static final Pattern RESOURCE = Pattern.compile(
            "^/api/([a-z][a-z0-9-]*)(?:/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}))?.*$");

    private final AuditService auditService;

    public AuditLoggingFilter(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/auth/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                String path = request.getRequestURI();
                Matcher m = RESOURCE.matcher(path);
                boolean matched = m.matches();
                auditService.recordAccess(user, request.getMethod(), path,
                        matched ? m.group(1) : null, matched ? m.group(2) : null,
                        response.getStatus(), request.getRemoteAddr());
            }
        }
    }
}

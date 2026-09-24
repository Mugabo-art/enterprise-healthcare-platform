package com.healthplatform.audit;

import com.healthplatform.audit.model.AuditLog;
import com.healthplatform.audit.repository.AuditLogRepository;
import com.healthplatform.audit.service.AuditLoggingFilter;
import com.healthplatform.audit.service.AuditService;
import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository repository;

    private final User doctor = User.builder().id(UUID.randomUUID()).email("d@x.test").role(Role.DOCTOR).build();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordAccess_mapsMethodToActionAndStatusToOutcome() {
        AuditService service = new AuditService(repository);

        service.recordAccess(doctor, "GET", "/api/patients/1", "patients", "1", 200, "10.0.0.1");
        service.recordAccess(doctor, "PUT", "/api/patients/1", "patients", "1", 403, "10.0.0.1");
        service.recordAccess(doctor, "POST", "/api/patients", "patients", null, 500, "10.0.0.1");

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository, times(3)).save(saved.capture());
        assertEquals("READ", saved.getAllValues().get(0).getAction());
        assertEquals("SUCCESS", saved.getAllValues().get(0).getOutcome());
        assertEquals("UPDATE", saved.getAllValues().get(1).getAction());
        assertEquals("DENIED", saved.getAllValues().get(1).getOutcome());
        assertEquals("CREATE", saved.getAllValues().get(2).getAction());
        assertEquals("FAILURE", saved.getAllValues().get(2).getOutcome());
        assertEquals(doctor.getId(), saved.getAllValues().get(0).getActorId());
    }

    @Test
    void recordEvent_neverPropagatesRepositoryFailures() {
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));
        AuditService service = new AuditService(repository);

        assertDoesNotThrow(() -> service.recordEvent("LOGIN", null, "x@x.test", AuditService.OUTCOME_FAILURE, "bad", null));
    }

    @Test
    void recordEvent_capturesUnknownActorEmailForFailedLogins() {
        AuditService service = new AuditService(repository);

        service.recordEvent("LOGIN", null, "ghost@x.test", AuditService.OUTCOME_FAILURE, "invalid credentials", "1.2.3.4");

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        assertNull(saved.getValue().getActorId());
        assertEquals("ghost@x.test", saved.getValue().getActorEmail());
        assertEquals("1.2.3.4", saved.getValue().getIpAddress());
    }

    @Test
    void filter_recordsAuthenticatedApiCallWithResourceAndId() throws Exception {
        AuditService service = mock(AuditService.class);
        AuditLoggingFilter filter = new AuditLoggingFilter(service);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(doctor, null, doctor.getAuthorities()));
        String id = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients/" + id + "/visits");
        request.setRemoteAddr("10.1.1.1");

        filter.doFilter(request, new MockHttpServletResponse(), new org.springframework.mock.web.MockFilterChain());

        verify(service).recordAccess(eq(doctor), eq("GET"), eq("/api/patients/" + id + "/visits"), eq("patients"), eq(id), anyInt(), eq("10.1.1.1"));
    }

    @Test
    void filter_skipsAuthEndpointsAndAnonymousCalls() throws Exception {
        AuditService service = mock(AuditService.class);
        AuditLoggingFilter filter = new AuditLoggingFilter(service);

        filter.doFilter(new MockHttpServletRequest("POST", "/api/auth/login"), new MockHttpServletResponse(), new org.springframework.mock.web.MockFilterChain());
        filter.doFilter(new MockHttpServletRequest("GET", "/api/patients"), new MockHttpServletResponse(), new org.springframework.mock.web.MockFilterChain());

        verifyNoInteractions(service);
    }
}

package com.healthplatform.patient;

import com.healthplatform.auth.service.AuthService;
import com.healthplatform.common.exception.ApiException;
import com.healthplatform.patient.dto.PatientCreateRequest;
import com.healthplatform.patient.dto.PatientResponse;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.model.Sex;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.patient.service.PatientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private AuthService authService;
    @InjectMocks private PatientService patientService;

    private static PatientCreateRequest request() {
        return new PatientCreateRequest("Ada", "Lovelace", LocalDate.of(1990, 12, 10), Sex.FEMALE,
                "+250700000000", "ada@x.test", "Kigali");
    }

    private static Patient patient(UUID id) {
        return Patient.builder().id(id).firstName("Ada").lastName("Lovelace")
                .dateOfBirth(LocalDate.of(1990, 12, 10)).sex(Sex.FEMALE).build();
    }

    @Test
    void create_persistsAllFields() {
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PatientResponse response = patientService.create(request());

        assertEquals("Ada", response.firstName());
        verify(patientRepository).save(argThat(p -> "Kigali".equals(p.getAddress()) && "ada@x.test".equals(p.getContactEmail())));
    }

    @Test
    void list_withoutSearchUsesUnfilteredQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        when(patientRepository.findByDeletedAtIsNull(pageable)).thenReturn(new PageImpl<>(List.of(patient(UUID.randomUUID()))));

        Page<PatientResponse> page = patientService.list(null, pageable);

        assertEquals(1, page.getTotalElements());
        verify(patientRepository, never()).findByDeletedAtIsNullAndLastNameContainingIgnoreCase(any(), any());
    }

    @Test
    void list_withSearchFiltersByLastName() {
        Pageable pageable = PageRequest.of(0, 10);
        when(patientRepository.findByDeletedAtIsNullAndLastNameContainingIgnoreCase("love", pageable))
                .thenReturn(new PageImpl<>(List.of(patient(UUID.randomUUID()))));

        assertEquals(1, patientService.list("love", pageable).getTotalElements());
    }

    @Test
    void getById_hidesSoftDeletedPatients() {
        UUID id = UUID.randomUUID();
        Patient deleted = patient(id);
        deleted.setDeletedAt(Instant.now());
        when(patientRepository.findById(id)).thenReturn(Optional.of(deleted));

        ApiException ex = assertThrows(ApiException.class, () -> patientService.getById(id));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void update_changesFieldsAndStampsUpdatedAt() {
        UUID id = UUID.randomUUID();
        Patient existing = patient(id);
        when(patientRepository.findById(id)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientResponse response = patientService.update(id,
                new PatientCreateRequest("Ada", "Byron", LocalDate.of(1990, 12, 10), Sex.FEMALE, null, null, null));

        assertEquals("Byron", response.lastName());
        assertNotNull(existing.getUpdatedAt());
    }

    @Test
    void update_unknownPatientIs404() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        assertEquals(404, assertThrows(ApiException.class, () -> patientService.update(id, request())).getStatus().value());
    }

    @Test
    void linkUser_requiresExistingPatient() {
        UUID patientId = UUID.randomUUID();
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> patientService.linkUser(patientId, UUID.randomUUID()));
        verifyNoInteractions(authService);
    }

    @Test
    void linkUser_delegatesToAuthServiceForLivePatient() {
        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient(patientId)));

        patientService.linkUser(patientId, userId);

        verify(authService).linkPatient(userId, patientId);
    }
}

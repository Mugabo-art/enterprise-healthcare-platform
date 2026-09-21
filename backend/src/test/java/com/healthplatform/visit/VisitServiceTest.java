package com.healthplatform.visit;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.model.Sex;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.visit.dto.VisitCreateRequest;
import com.healthplatform.visit.dto.VisitResponse;
import com.healthplatform.visit.dto.VisitUpdateRequest;
import com.healthplatform.visit.model.Visit;
import com.healthplatform.visit.model.VisitStatus;
import com.healthplatform.visit.model.VisitType;
import com.healthplatform.visit.repository.VisitRepository;
import com.healthplatform.visit.service.VisitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock private VisitRepository visitRepository;
    @Mock private PatientRepository patientRepository;

    @InjectMocks
    private VisitService visitService;

    private Patient activePatient(UUID id) {
        return Patient.builder()
                .id(id)
                .firstName("Grace")
                .lastName("Uwase")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .sex(Sex.FEMALE)
                .build();
    }

    @Test
    void create_savesVisitForExistingPatient() {
        UUID patientId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        VisitCreateRequest request = new VisitCreateRequest(Instant.now(), VisitType.OUTPATIENT, "Annual checkup", "Notes");

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(visitRepository.save(any(Visit.class))).thenAnswer(inv -> {
            Visit v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        VisitResponse response = visitService.create(patientId, request, staffId);

        assertEquals(patientId, response.patientId());
        assertEquals("Annual checkup", response.reason());
        assertEquals(staffId, response.attendingStaffId());
        verify(visitRepository).save(any(Visit.class));
    }

    @Test
    void create_rejectsUnknownPatient() {
        UUID patientId = UUID.randomUUID();
        VisitCreateRequest request = new VisitCreateRequest(Instant.now(), VisitType.OUTPATIENT, "Checkup", null);
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> visitService.create(patientId, request, UUID.randomUUID()));
        assertEquals(404, ex.getStatus().value());
        verifyNoInteractions(visitRepository);
    }

    @Test
    void create_rejectsSoftDeletedPatient() {
        UUID patientId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        patient.setDeletedAt(Instant.now());
        VisitCreateRequest request = new VisitCreateRequest(Instant.now(), VisitType.OUTPATIENT, "Checkup", null);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        assertThrows(ApiException.class, () -> visitService.create(patientId, request, UUID.randomUUID()));
    }

    @Test
    void list_returnsVisitsForPatient() {
        UUID patientId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        Visit visit = Visit.builder().id(UUID.randomUUID()).patient(patient).visitDate(Instant.now())
                .visitType(VisitType.EMERGENCY).reason("Fall").build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(visitRepository.findByPatientIdOrderByVisitDateDesc(patientId)).thenReturn(List.of(visit));

        List<VisitResponse> result = visitService.list(patientId);

        assertEquals(1, result.size());
        assertEquals("Fall", result.get(0).reason());
    }

    @Test
    void update_appliesDiagnosisCodeAndStatusToVisitOwnedByPatient() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        Visit visit = Visit.builder().id(visitId).patient(patient).visitDate(Instant.now())
                .visitType(VisitType.OUTPATIENT).reason("Checkup").build();
        VisitUpdateRequest request = new VisitUpdateRequest("Looks stable", "J06.9", VisitStatus.COMPLETED);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(visitRepository.save(any(Visit.class))).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse response = visitService.update(patientId, visitId, request);

        assertEquals("Looks stable", response.notes());
        assertEquals("J06.9", response.diagnosisCode());
        assertEquals(VisitStatus.COMPLETED, response.status());
    }

    @Test
    void update_rejectsVisitBelongingToAnotherPatient() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        Patient otherPatient = activePatient(UUID.randomUUID());
        Visit visit = Visit.builder().id(visitId).patient(otherPatient).visitDate(Instant.now())
                .visitType(VisitType.OUTPATIENT).reason("Checkup").build();
        VisitUpdateRequest request = new VisitUpdateRequest(null, "J06.9", null);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        assertThrows(ApiException.class, () -> visitService.update(patientId, visitId, request));
        verify(visitRepository, never()).save(any());
    }

    @Test
    void findByDoctor_returnsVisitsForAttendingStaff() {
        UUID doctorId = UUID.randomUUID();
        Patient patient = activePatient(UUID.randomUUID());
        Visit visit = Visit.builder().id(UUID.randomUUID()).patient(patient).visitDate(Instant.now())
                .visitType(VisitType.OUTPATIENT).reason("Follow-up").attendingStaffId(doctorId).build();

        when(visitRepository.findByAttendingStaffIdOrderByVisitDateDesc(doctorId)).thenReturn(List.of(visit));

        List<VisitResponse> result = visitService.findByDoctor(doctorId);

        assertEquals(1, result.size());
        assertEquals(doctorId, result.get(0).attendingStaffId());
    }
}

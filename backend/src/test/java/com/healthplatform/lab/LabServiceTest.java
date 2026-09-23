package com.healthplatform.lab;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.lab.dto.*;
import com.healthplatform.lab.model.LabRequest;
import com.healthplatform.lab.model.LabRequestStatus;
import com.healthplatform.lab.model.LabResult;
import com.healthplatform.lab.repository.LabRequestRepository;
import com.healthplatform.lab.repository.LabResultRepository;
import com.healthplatform.lab.service.LabService;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.model.Sex;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.visit.dto.VisitResponse;
import com.healthplatform.visit.model.VisitStatus;
import com.healthplatform.visit.model.VisitType;
import com.healthplatform.visit.service.VisitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabServiceTest {

    @Mock private LabRequestRepository labRequestRepository;
    @Mock private LabResultRepository labResultRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private VisitService visitService;

    @InjectMocks
    private LabService labService;

    private Patient activePatient(UUID id) {
        return Patient.builder()
                .id(id)
                .firstName("Grace")
                .lastName("Uwase")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .sex(Sex.FEMALE)
                .build();
    }

    private LabRequest labRequestFor(UUID patientId, UUID labRequestId, LabRequestStatus status) {
        return LabRequest.builder()
                .id(labRequestId)
                .patient(activePatient(patientId))
                .visitId(UUID.randomUUID())
                .testType("CBC")
                .status(status)
                .build();
    }

    @Test
    void createRequest_savesRequestForExistingPatientAndVisit() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        LabRequestCreateRequest request = new LabRequestCreateRequest("CBC", "Fasting sample");

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(visitService.getById(patientId, visitId)).thenReturn(
                new VisitResponse(visitId, patientId, Instant.now(), VisitType.OUTPATIENT, "Checkup", null, staffId, VisitStatus.SCHEDULED, null, Instant.now())
        );
        when(labRequestRepository.save(any(LabRequest.class))).thenAnswer(inv -> {
            LabRequest r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        LabRequestResponse response = labService.createRequest(patientId, visitId, request, staffId);

        assertEquals(patientId, response.patientId());
        assertEquals(visitId, response.visitId());
        assertEquals("CBC", response.testType());
        assertEquals(LabRequestStatus.REQUESTED, response.status());
        verify(labRequestRepository).save(any(LabRequest.class));
    }

    @Test
    void createRequest_rejectsUnknownPatient() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        LabRequestCreateRequest request = new LabRequestCreateRequest("CBC", null);
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> labService.createRequest(patientId, visitId, request, UUID.randomUUID()));
        assertEquals(404, ex.getStatus().value());
        verifyNoInteractions(labRequestRepository, visitService);
    }

    @Test
    void createRequest_rejectsVisitNotBelongingToPatient() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        LabRequestCreateRequest request = new LabRequestCreateRequest("CBC", null);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(visitService.getById(patientId, visitId)).thenThrow(new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Visit not found"));

        assertThrows(ApiException.class, () -> labService.createRequest(patientId, visitId, request, UUID.randomUUID()));
        verifyNoInteractions(labRequestRepository);
    }

    @Test
    void list_returnsRequestsForPatient() {
        UUID patientId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        LabRequest labRequest = labRequestFor(patientId, UUID.randomUUID(), LabRequestStatus.REQUESTED);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(labRequestRepository.findByPatientIdOrderByCreatedAtDesc(patientId)).thenReturn(List.of(labRequest));

        List<LabRequestResponse> result = labService.list(patientId);

        assertEquals(1, result.size());
        assertEquals("CBC", result.get(0).testType());
    }

    @Test
    void recordResult_savesResultAndCompletesRequest() {
        UUID patientId = UUID.randomUUID();
        UUID labRequestId = UUID.randomUUID();
        UUID labTechId = UUID.randomUUID();
        LabRequest labRequest = labRequestFor(patientId, labRequestId, LabRequestStatus.REQUESTED);
        LabResultCreateRequest request = new LabResultCreateRequest(Map.of("wbc", "6.2"));

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient(patientId)));
        when(labRequestRepository.findById(labRequestId)).thenReturn(Optional.of(labRequest));
        when(labResultRepository.findByLabRequestId(labRequestId)).thenReturn(Optional.empty());
        when(labResultRepository.save(any(LabResult.class))).thenAnswer(inv -> {
            LabResult r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(labRequestRepository.save(any(LabRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LabResultResponse response = labService.recordResult(patientId, labRequestId, request, labTechId);

        assertEquals(labRequestId, response.labRequestId());
        assertEquals(labTechId, response.recordedById());
        assertEquals(LabRequestStatus.COMPLETED, labRequest.getStatus());
        verify(labRequestRepository).save(labRequest);
    }

    @Test
    void recordResult_rejectsDuplicateResult() {
        UUID patientId = UUID.randomUUID();
        UUID labRequestId = UUID.randomUUID();
        LabRequest labRequest = labRequestFor(patientId, labRequestId, LabRequestStatus.REQUESTED);
        LabResultCreateRequest request = new LabResultCreateRequest(Map.of("wbc", "6.2"));

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient(patientId)));
        when(labRequestRepository.findById(labRequestId)).thenReturn(Optional.of(labRequest));
        when(labResultRepository.findByLabRequestId(labRequestId)).thenReturn(Optional.of(LabResult.builder().id(UUID.randomUUID()).build()));

        ApiException ex = assertThrows(ApiException.class,
                () -> labService.recordResult(patientId, labRequestId, request, UUID.randomUUID()));
        assertEquals(409, ex.getStatus().value());
        verify(labResultRepository, never()).save(any());
    }

    @Test
    void getResult_rejectsWhenNoResultYet() {
        UUID patientId = UUID.randomUUID();
        UUID labRequestId = UUID.randomUUID();
        LabRequest labRequest = labRequestFor(patientId, labRequestId, LabRequestStatus.REQUESTED);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient(patientId)));
        when(labRequestRepository.findById(labRequestId)).thenReturn(Optional.of(labRequest));
        when(labResultRepository.findByLabRequestId(labRequestId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> labService.getResult(patientId, labRequestId));
        assertEquals(404, ex.getStatus().value());
    }
}

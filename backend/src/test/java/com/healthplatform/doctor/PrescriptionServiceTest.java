package com.healthplatform.doctor;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.doctor.dto.PrescriptionCreateRequest;
import com.healthplatform.doctor.dto.PrescriptionResponse;
import com.healthplatform.doctor.model.Prescription;
import com.healthplatform.doctor.repository.PrescriptionRepository;
import com.healthplatform.doctor.service.PrescriptionService;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private VisitService visitService;

    @InjectMocks
    private PrescriptionService prescriptionService;

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
    void create_savesPrescriptionForExistingPatientAndVisit() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        PrescriptionCreateRequest request = new PrescriptionCreateRequest("Amoxicillin", "500mg", "3x daily", 7, "Take with food");

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(visitService.getById(patientId, visitId)).thenReturn(
                new VisitResponse(visitId, patientId, Instant.now(), VisitType.OUTPATIENT, "Checkup", null, doctorId, VisitStatus.SCHEDULED, null, Instant.now())
        );
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> {
            Prescription p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PrescriptionResponse response = prescriptionService.create(patientId, visitId, request, doctorId);

        assertEquals(patientId, response.patientId());
        assertEquals(visitId, response.visitId());
        assertEquals(doctorId, response.prescribedById());
        assertEquals("Amoxicillin", response.medicationName());
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    void create_rejectsUnknownPatient() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        PrescriptionCreateRequest request = new PrescriptionCreateRequest("Amoxicillin", "500mg", "3x daily", 7, null);
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> prescriptionService.create(patientId, visitId, request, UUID.randomUUID()));
        assertEquals(404, ex.getStatus().value());
        verifyNoInteractions(prescriptionRepository, visitService);
    }

    @Test
    void create_rejectsVisitNotBelongingToPatient() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        PrescriptionCreateRequest request = new PrescriptionCreateRequest("Amoxicillin", "500mg", "3x daily", 7, null);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(visitService.getById(patientId, visitId)).thenThrow(new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Visit not found"));

        assertThrows(ApiException.class, () -> prescriptionService.create(patientId, visitId, request, UUID.randomUUID()));
        verifyNoInteractions(prescriptionRepository);
    }

    @Test
    void list_returnsPrescriptionsForPatient() {
        UUID patientId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        Prescription prescription = Prescription.builder()
                .id(UUID.randomUUID()).patient(patient).visitId(UUID.randomUUID())
                .medicationName("Ibuprofen").dosage("200mg").frequency("2x daily").build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId)).thenReturn(List.of(prescription));

        List<PrescriptionResponse> result = prescriptionService.list(patientId);

        assertEquals(1, result.size());
        assertEquals("Ibuprofen", result.get(0).medicationName());
    }
}

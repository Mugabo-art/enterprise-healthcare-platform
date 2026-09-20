package com.healthplatform.medicalhistory;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.medicalhistory.dto.MedicalHistoryCreateRequest;
import com.healthplatform.medicalhistory.dto.MedicalHistoryResponse;
import com.healthplatform.medicalhistory.model.HistoryCategory;
import com.healthplatform.medicalhistory.model.MedicalHistoryEntry;
import com.healthplatform.medicalhistory.repository.MedicalHistoryRepository;
import com.healthplatform.medicalhistory.service.MedicalHistoryService;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.model.Sex;
import com.healthplatform.patient.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalHistoryServiceTest {

    @Mock private MedicalHistoryRepository medicalHistoryRepository;
    @Mock private PatientRepository patientRepository;

    @InjectMocks
    private MedicalHistoryService medicalHistoryService;

    private Patient activePatient(UUID id) {
        return Patient.builder()
                .id(id)
                .firstName("Jean")
                .lastName("Mugisha")
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .sex(Sex.MALE)
                .build();
    }

    @Test
    void create_savesEntryForExistingPatient() {
        UUID patientId = UUID.randomUUID();
        UUID recordedById = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        MedicalHistoryCreateRequest request = new MedicalHistoryCreateRequest(
                HistoryCategory.ALLERGY, "Penicillin", LocalDate.of(2020, 1, 1));

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(medicalHistoryRepository.save(any(MedicalHistoryEntry.class))).thenAnswer(inv -> {
            MedicalHistoryEntry e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        MedicalHistoryResponse response = medicalHistoryService.create(patientId, request, recordedById);

        assertEquals(patientId, response.patientId());
        assertEquals(HistoryCategory.ALLERGY, response.category());
        assertEquals("Penicillin", response.description());
        assertEquals(recordedById, response.recordedById());
    }

    @Test
    void create_rejectsUnknownPatient() {
        UUID patientId = UUID.randomUUID();
        MedicalHistoryCreateRequest request = new MedicalHistoryCreateRequest(
                HistoryCategory.CONDITION, "Asthma", LocalDate.now());
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> medicalHistoryService.create(patientId, request, UUID.randomUUID()));
        assertEquals(404, ex.getStatus().value());
        verifyNoInteractions(medicalHistoryRepository);
    }

    @Test
    void list_returnsEntriesForPatient() {
        UUID patientId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        MedicalHistoryEntry entry = MedicalHistoryEntry.builder()
                .id(UUID.randomUUID()).patient(patient).category(HistoryCategory.MEDICATION)
                .description("Metformin").recordedDate(LocalDate.now()).build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(medicalHistoryRepository.findByPatientIdOrderByRecordedDateDesc(patientId)).thenReturn(List.of(entry));

        List<MedicalHistoryResponse> result = medicalHistoryService.list(patientId);

        assertEquals(1, result.size());
        assertEquals("Metformin", result.get(0).description());
    }
}

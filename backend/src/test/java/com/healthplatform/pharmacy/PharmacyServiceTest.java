package com.healthplatform.pharmacy;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.doctor.model.Prescription;
import com.healthplatform.doctor.service.PrescriptionService;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.model.Sex;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.pharmacy.dto.*;
import com.healthplatform.pharmacy.model.Dispensation;
import com.healthplatform.pharmacy.model.Medication;
import com.healthplatform.pharmacy.repository.DispensationRepository;
import com.healthplatform.pharmacy.repository.MedicationRepository;
import com.healthplatform.pharmacy.service.PharmacyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

    @Mock private MedicationRepository medicationRepository;
    @Mock private DispensationRepository dispensationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private PrescriptionService prescriptionService;

    @InjectMocks
    private PharmacyService pharmacyService;

    private Medication medication(UUID id, int stock, int threshold, boolean active) {
        return Medication.builder().id(id).name("Amoxicillin").unit("capsules")
                .stockQuantity(stock).reorderThreshold(threshold).active(active).build();
    }

    private Prescription prescription(UUID id, UUID patientId) {
        Patient patient = Patient.builder().id(patientId).firstName("Grace").lastName("Uwase")
                .dateOfBirth(LocalDate.of(1990, 1, 1)).sex(Sex.FEMALE).build();
        return Prescription.builder().id(id).patient(patient).visitId(UUID.randomUUID())
                .medicationName("Amoxicillin").dosage("500mg").frequency("3x daily").build();
    }

    @Test
    void createMedicationRejectsDuplicateName() {
        when(medicationRepository.existsByNameIgnoreCase("Amoxicillin")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> pharmacyService.createMedication(new MedicationCreateRequest("Amoxicillin", "capsules", 10, 5)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(medicationRepository, never()).save(any());
    }

    @Test
    void listMedicationsLowStockOnlyExcludesHealthyAndInactive() {
        Medication low = medication(UUID.randomUUID(), 5, 10, true);
        Medication healthy = medication(UUID.randomUUID(), 50, 10, true);
        Medication inactiveLow = medication(UUID.randomUUID(), 1, 10, false);
        when(medicationRepository.findAllByOrderByNameAsc()).thenReturn(List.of(low, healthy, inactiveLow));

        List<MedicationResponse> result = pharmacyService.listMedications(true);

        assertEquals(1, result.size());
        assertEquals(low.getId(), result.get(0).id());
        assertTrue(result.get(0).lowStock());
    }

    @Test
    void restockAddsToExistingQuantity() {
        UUID id = UUID.randomUUID();
        when(medicationRepository.findById(id)).thenReturn(Optional.of(medication(id, 5, 10, true)));
        when(medicationRepository.save(any(Medication.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicationResponse response = pharmacyService.restock(id, new RestockRequest(20));

        assertEquals(25, response.stockQuantity());
        assertFalse(response.lowStock());
    }

    @Test
    void dispenseDecrementsStockRecordsDispensationAndCompletesPrescription() {
        UUID prescriptionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID medId = UUID.randomUUID();
        UUID pharmacistId = UUID.randomUUID();
        Prescription prescription = prescription(prescriptionId, patientId);
        Medication med = medication(medId, 30, 10, true);
        when(prescriptionService.getActiveForDispense(prescriptionId)).thenReturn(prescription);
        when(medicationRepository.findById(medId)).thenReturn(Optional.of(med));
        when(dispensationRepository.save(any(Dispensation.class))).thenAnswer(inv -> inv.getArgument(0));

        DispensationResponse response = pharmacyService.dispense(prescriptionId, new DispenseRequest(medId, 21), pharmacistId);

        assertEquals(9, med.getStockQuantity());
        assertEquals(patientId, response.patientId());
        assertEquals(21, response.quantity());
        assertEquals(pharmacistId, response.dispensedById());
        verify(medicationRepository).save(med);
        verify(prescriptionService).markCompleted(prescription);
    }

    @Test
    void dispenseRejectsInsufficientStockWithoutSideEffects() {
        UUID prescriptionId = UUID.randomUUID();
        UUID medId = UUID.randomUUID();
        Medication med = medication(medId, 5, 10, true);
        when(prescriptionService.getActiveForDispense(prescriptionId)).thenReturn(prescription(prescriptionId, UUID.randomUUID()));
        when(medicationRepository.findById(medId)).thenReturn(Optional.of(med));

        ApiException ex = assertThrows(ApiException.class,
                () -> pharmacyService.dispense(prescriptionId, new DispenseRequest(medId, 6), UUID.randomUUID()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(5, med.getStockQuantity());
        verify(dispensationRepository, never()).save(any());
        verify(prescriptionService, never()).markCompleted(any());
    }

    @Test
    void dispenseRejectsInactiveMedication() {
        UUID prescriptionId = UUID.randomUUID();
        UUID medId = UUID.randomUUID();
        when(prescriptionService.getActiveForDispense(prescriptionId)).thenReturn(prescription(prescriptionId, UUID.randomUUID()));
        when(medicationRepository.findById(medId)).thenReturn(Optional.of(medication(medId, 50, 10, false)));

        ApiException ex = assertThrows(ApiException.class,
                () -> pharmacyService.dispense(prescriptionId, new DispenseRequest(medId, 1), UUID.randomUUID()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(dispensationRepository, never()).save(any());
    }

    @Test
    void listDispensationsRejectsUnknownPatient() {
        UUID patientId = UUID.randomUUID();
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> pharmacyService.listDispensations(patientId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}

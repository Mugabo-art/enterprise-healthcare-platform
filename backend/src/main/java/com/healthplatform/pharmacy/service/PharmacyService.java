package com.healthplatform.pharmacy.service;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.doctor.dto.PrescriptionResponse;
import com.healthplatform.doctor.model.Prescription;
import com.healthplatform.doctor.service.PrescriptionService;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.pharmacy.dto.*;
import com.healthplatform.pharmacy.model.Dispensation;
import com.healthplatform.pharmacy.model.Medication;
import com.healthplatform.pharmacy.repository.DispensationRepository;
import com.healthplatform.pharmacy.repository.MedicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PharmacyService {

    private final MedicationRepository medicationRepository;
    private final DispensationRepository dispensationRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionService prescriptionService;

    public PharmacyService(MedicationRepository medicationRepository, DispensationRepository dispensationRepository,
                            PatientRepository patientRepository, PrescriptionService prescriptionService) {
        this.medicationRepository = medicationRepository;
        this.dispensationRepository = dispensationRepository;
        this.patientRepository = patientRepository;
        this.prescriptionService = prescriptionService;
    }

    public List<MedicationResponse> listMedications(boolean lowStockOnly) {
        return medicationRepository.findAllByOrderByNameAsc().stream()
                .filter(m -> !lowStockOnly || (m.isActive() && m.isLowStock()))
                .map(MedicationResponse::from)
                .toList();
    }

    @Transactional
    public MedicationResponse createMedication(MedicationCreateRequest request) {
        String name = request.name().trim();
        if (medicationRepository.existsByNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "A medication with this name already exists");
        }
        Medication medication = Medication.builder()
                .name(name)
                .unit(request.unit().trim())
                .stockQuantity(request.stockQuantity())
                .reorderThreshold(request.reorderThreshold())
                .build();
        return MedicationResponse.from(medicationRepository.save(medication));
    }

    @Transactional
    public MedicationResponse updateMedication(UUID id, MedicationUpdateRequest request) {
        Medication medication = findMedication(id);
        medication.setUnit(request.unit().trim());
        medication.setReorderThreshold(request.reorderThreshold());
        medication.setActive(request.active());
        medication.setUpdatedAt(Instant.now());
        return MedicationResponse.from(medicationRepository.save(medication));
    }

    @Transactional
    public MedicationResponse restock(UUID id, RestockRequest request) {
        Medication medication = findMedication(id);
        medication.setStockQuantity(medication.getStockQuantity() + request.quantity());
        medication.setUpdatedAt(Instant.now());
        return MedicationResponse.from(medicationRepository.save(medication));
    }

    public List<PrescriptionResponse> pendingPrescriptions() {
        return prescriptionService.listActive();
    }

    @Transactional
    public DispensationResponse dispense(UUID prescriptionId, DispenseRequest request, UUID dispensedById) {
        Prescription prescription = prescriptionService.getActiveForDispense(prescriptionId);
        Medication medication = findMedication(request.medicationId());
        if (!medication.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "Medication is not active");
        }
        if (medication.getStockQuantity() < request.quantity()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Insufficient stock: " + medication.getStockQuantity() + " " + medication.getUnit() + " available");
        }

        medication.setStockQuantity(medication.getStockQuantity() - request.quantity());
        medication.setUpdatedAt(Instant.now());
        medicationRepository.save(medication);

        Dispensation saved = dispensationRepository.save(Dispensation.builder()
                .patientId(prescription.getPatient().getId())
                .prescriptionId(prescriptionId)
                .medication(medication)
                .quantity(request.quantity())
                .dispensedById(dispensedById)
                .build());

        prescriptionService.markCompleted(prescription);
        return DispensationResponse.from(saved);
    }

    public List<DispensationResponse> listDispensations(UUID patientId) {
        patientRepository.findById(patientId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));
        return dispensationRepository.findByPatientIdOrderByDispensedAtDesc(patientId).stream()
                .map(DispensationResponse::from)
                .toList();
    }

    private Medication findMedication(UUID id) {
        return medicationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Medication not found"));
    }
}

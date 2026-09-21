package com.healthplatform.doctor.service;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.doctor.dto.PrescriptionCreateRequest;
import com.healthplatform.doctor.dto.PrescriptionResponse;
import com.healthplatform.doctor.model.Prescription;
import com.healthplatform.doctor.repository.PrescriptionRepository;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.visit.service.VisitService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final VisitService visitService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository, PatientRepository patientRepository, VisitService visitService) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.visitService = visitService;
    }

    @Transactional
    public PrescriptionResponse create(UUID patientId, UUID visitId, PrescriptionCreateRequest request, UUID prescribedById) {
        Patient patient = findActivePatient(patientId);
        visitService.getById(patientId, visitId);

        Prescription prescription = Prescription.builder()
                .patient(patient)
                .visitId(visitId)
                .prescribedById(prescribedById)
                .medicationName(request.medicationName())
                .dosage(request.dosage())
                .frequency(request.frequency())
                .durationDays(request.durationDays())
                .instructions(request.instructions())
                .build();

        return PrescriptionResponse.from(prescriptionRepository.save(prescription));
    }

    public List<PrescriptionResponse> list(UUID patientId) {
        findActivePatient(patientId);
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(PrescriptionResponse::from)
                .toList();
    }

    private Patient findActivePatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));
    }
}

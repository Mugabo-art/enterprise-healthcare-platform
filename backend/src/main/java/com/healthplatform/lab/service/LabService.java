package com.healthplatform.lab.service;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.lab.dto.*;
import com.healthplatform.lab.model.LabRequest;
import com.healthplatform.lab.model.LabRequestStatus;
import com.healthplatform.lab.model.LabResult;
import com.healthplatform.lab.repository.LabRequestRepository;
import com.healthplatform.lab.repository.LabResultRepository;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.visit.service.VisitService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LabService {

    private final LabRequestRepository labRequestRepository;
    private final LabResultRepository labResultRepository;
    private final PatientRepository patientRepository;
    private final VisitService visitService;

    public LabService(LabRequestRepository labRequestRepository, LabResultRepository labResultRepository,
                       PatientRepository patientRepository, VisitService visitService) {
        this.labRequestRepository = labRequestRepository;
        this.labResultRepository = labResultRepository;
        this.patientRepository = patientRepository;
        this.visitService = visitService;
    }

    @Transactional
    public LabRequestResponse createRequest(UUID patientId, UUID visitId, LabRequestCreateRequest request, UUID requestedById) {
        Patient patient = findActivePatient(patientId);
        visitService.getById(patientId, visitId);

        LabRequest labRequest = LabRequest.builder()
                .patient(patient)
                .visitId(visitId)
                .requestedById(requestedById)
                .testType(request.testType())
                .notes(request.notes())
                .build();

        return LabRequestResponse.from(labRequestRepository.save(labRequest));
    }

    public List<LabRequestResponse> list(UUID patientId) {
        findActivePatient(patientId);
        return labRequestRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(LabRequestResponse::from)
                .toList();
    }

    @Transactional
    public LabRequestResponse updateStatus(UUID patientId, UUID labRequestId, LabRequestStatusUpdateRequest request) {
        LabRequest labRequest = findLabRequest(patientId, labRequestId);
        if (labRequest.getStatus() == LabRequestStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot change status of a completed lab request");
        }

        labRequest.setStatus(request.status());
        labRequest.setUpdatedAt(Instant.now());

        return LabRequestResponse.from(labRequestRepository.save(labRequest));
    }

    @Transactional
    public LabResultResponse recordResult(UUID patientId, UUID labRequestId, LabResultCreateRequest request, UUID recordedById) {
        LabRequest labRequest = findLabRequest(patientId, labRequestId);
        if (labResultRepository.findByLabRequestId(labRequestId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A result has already been recorded for this lab request");
        }

        LabResult result = LabResult.builder()
                .labRequestId(labRequestId)
                .recordedById(recordedById)
                .resultData(request.resultData())
                .build();
        LabResult saved = labResultRepository.save(result);

        labRequest.setStatus(LabRequestStatus.COMPLETED);
        labRequest.setUpdatedAt(Instant.now());
        labRequestRepository.save(labRequest);

        return LabResultResponse.from(saved);
    }

    public LabResultResponse getResult(UUID patientId, UUID labRequestId) {
        findLabRequest(patientId, labRequestId);
        return labResultRepository.findByLabRequestId(labRequestId)
                .map(LabResultResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No result recorded for this lab request yet"));
    }

    private LabRequest findLabRequest(UUID patientId, UUID labRequestId) {
        findActivePatient(patientId);
        return labRequestRepository.findById(labRequestId)
                .filter(r -> r.getPatient().getId().equals(patientId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lab request not found"));
    }

    private Patient findActivePatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));
    }
}

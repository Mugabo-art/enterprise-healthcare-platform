package com.healthplatform.patient.service;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.patient.dto.PatientCreateRequest;
import com.healthplatform.patient.dto.PatientResponse;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    public PatientResponse create(PatientCreateRequest request) {
        Patient patient = Patient.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .dateOfBirth(request.dateOfBirth())
                .sex(request.sex())
                .contactPhone(request.contactPhone())
                .contactEmail(request.contactEmail())
                .address(request.address())
                .build();
        return PatientResponse.from(patientRepository.save(patient));
    }

    public Page<PatientResponse> list(String search, Pageable pageable) {
        Page<Patient> page = StringUtils.hasText(search)
                ? patientRepository.findByDeletedAtIsNullAndLastNameContainingIgnoreCase(search, pageable)
                : patientRepository.findByDeletedAtIsNull(pageable);
        return page.map(PatientResponse::from);
    }

    public PatientResponse getById(UUID id) {
        Patient patient = patientRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));
        return PatientResponse.from(patient);
    }

    @Transactional
    public PatientResponse update(UUID id, PatientCreateRequest request) {
        Patient patient = patientRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));

        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setSex(request.sex());
        patient.setContactPhone(request.contactPhone());
        patient.setContactEmail(request.contactEmail());
        patient.setAddress(request.address());
        patient.setUpdatedAt(Instant.now());

        return PatientResponse.from(patientRepository.save(patient));
    }
}

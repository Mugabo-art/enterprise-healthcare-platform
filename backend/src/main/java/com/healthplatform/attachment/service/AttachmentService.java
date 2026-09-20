package com.healthplatform.attachment.service;

import com.healthplatform.attachment.dto.AttachmentResponse;
import com.healthplatform.attachment.model.Attachment;
import com.healthplatform.attachment.repository.AttachmentRepository;
import com.healthplatform.common.exception.ApiException;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.visit.model.Visit;
import com.healthplatform.visit.repository.VisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;

    public AttachmentService(AttachmentRepository attachmentRepository, PatientRepository patientRepository, VisitRepository visitRepository) {
        this.attachmentRepository = attachmentRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
    }

    @Transactional
    public AttachmentResponse upload(UUID patientId, UUID visitId, MultipartFile file, UUID uploadedById) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must not be empty");
        }

        Patient patient = patientRepository.findById(patientId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));

        Visit visit = null;
        if (visitId != null) {
            visit = visitRepository.findById(visitId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Visit not found"));
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }

        Attachment attachment = Attachment.builder()
                .patient(patient)
                .visit(visit)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .fileSizeBytes(file.getSize())
                .fileData(data)
                .uploadedById(uploadedById)
                .build();

        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    public List<AttachmentResponse> list(UUID patientId) {
        findActivePatient(patientId);
        return attachmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    public Attachment getForDownload(UUID patientId, UUID attachmentId) {
        findActivePatient(patientId);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (!attachment.getPatient().getId().equals(patientId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Attachment not found");
        }
        return attachment;
    }

    private Patient findActivePatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));
    }
}

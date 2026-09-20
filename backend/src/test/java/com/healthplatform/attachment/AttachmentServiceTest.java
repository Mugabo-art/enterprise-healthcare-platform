package com.healthplatform.attachment;

import com.healthplatform.attachment.dto.AttachmentResponse;
import com.healthplatform.attachment.model.Attachment;
import com.healthplatform.attachment.repository.AttachmentRepository;
import com.healthplatform.attachment.service.AttachmentService;
import com.healthplatform.common.exception.ApiException;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.model.Sex;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.visit.repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock private AttachmentRepository attachmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private VisitRepository visitRepository;

    @InjectMocks
    private AttachmentService attachmentService;

    private Patient activePatient(UUID id) {
        return Patient.builder()
                .id(id)
                .firstName("Alice")
                .lastName("Keza")
                .dateOfBirth(LocalDate.of(1995, 3, 3))
                .sex(Sex.FEMALE)
                .build();
    }

    @Test
    void upload_savesAttachmentForExistingPatient() {
        UUID patientId = UUID.randomUUID();
        UUID uploaderId = UUID.randomUUID();
        Patient patient = activePatient(patientId);
        MockMultipartFile file = new MockMultipartFile("file", "lab-result.pdf", "application/pdf", "content".getBytes());

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AttachmentResponse response = attachmentService.upload(patientId, null, file, uploaderId);

        assertEquals(patientId, response.patientId());
        assertEquals("lab-result.pdf", response.fileName());
        assertEquals("application/pdf", response.contentType());
        assertEquals(uploaderId, response.uploadedById());
        verifyNoInteractions(visitRepository);
    }

    @Test
    void upload_rejectsEmptyFile() {
        UUID patientId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        ApiException ex = assertThrows(ApiException.class,
                () -> attachmentService.upload(patientId, null, emptyFile, UUID.randomUUID()));
        assertEquals(400, ex.getStatus().value());
        verifyNoInteractions(patientRepository, attachmentRepository);
    }

    @Test
    void upload_rejectsUnknownPatient() {
        UUID patientId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "scan.png", "image/png", "content".getBytes());
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> attachmentService.upload(patientId, null, file, UUID.randomUUID()));
        assertEquals(404, ex.getStatus().value());
        verifyNoInteractions(attachmentRepository);
    }
}

package com.healthplatform.attachment.repository;

import com.healthplatform.attachment.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}

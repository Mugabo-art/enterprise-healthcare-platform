package com.healthplatform.attachment.dto;

import com.healthplatform.attachment.model.Attachment;

import java.time.Instant;
import java.util.UUID;

/** Metadata only — file bytes are served separately via the download endpoint. */
public record AttachmentResponse(
        UUID id, UUID patientId, UUID visitId, String fileName,
        String contentType, long fileSizeBytes, UUID uploadedById, Instant createdAt
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(
                a.getId(), a.getPatient().getId(), a.getVisit() != null ? a.getVisit().getId() : null,
                a.getFileName(), a.getContentType(), a.getFileSizeBytes(), a.getUploadedById(), a.getCreatedAt()
        );
    }
}

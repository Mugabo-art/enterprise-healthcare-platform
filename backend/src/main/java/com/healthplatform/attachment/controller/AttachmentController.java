package com.healthplatform.attachment.controller;

import com.healthplatform.attachment.dto.AttachmentResponse;
import com.healthplatform.attachment.model.Attachment;
import com.healthplatform.attachment.service.AttachmentService;
import com.healthplatform.auth.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients/{patientId}/attachments")
@Tag(name = "Attachments", description = "Files attached to a patient record or a specific visit")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping
    @Operation(summary = "List a patient's attachments, newest first")
    public ResponseEntity<List<AttachmentResponse>> list(@PathVariable UUID patientId) {
        return ResponseEntity.ok(attachmentService.list(patientId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'DOCTOR')")
    @Operation(summary = "Upload a file attachment, optionally linked to a visit")
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable UUID patientId,
            @RequestParam(required = false) UUID visitId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(patientId, visitId, file, user.getId()));
    }

    @GetMapping("/{attachmentId}/download")
    @Operation(summary = "Download an attachment's raw file")
    public ResponseEntity<byte[]> download(@PathVariable UUID patientId, @PathVariable UUID attachmentId) {
        Attachment attachment = attachmentService.getForDownload(patientId, attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(attachment.getFileName()).build().toString())
                .body(attachment.getFileData());
    }
}

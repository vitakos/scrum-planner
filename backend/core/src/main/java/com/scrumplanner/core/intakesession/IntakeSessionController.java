package com.scrumplanner.core.intakesession;

import com.scrumplanner.core.intakesession.dto.IntakeMessageRequest;
import com.scrumplanner.core.intakesession.dto.IntakeSessionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

// IntakeSessionController (AISC-100): REST endpoints for intake sessions.
// Provides endpoints to retrieve, update, and manage intake chat sessions per project.
@RestController
@RequestMapping("/api/projects/{projectId}/intake-session")
public class IntakeSessionController {

    private final IntakeSessionService sessionService;
    private final IntakeAttachmentStorage attachmentStorage;

    public IntakeSessionController(IntakeSessionService sessionService, IntakeAttachmentStorage attachmentStorage) {
        this.sessionService = sessionService;
        this.attachmentStorage = attachmentStorage;
    }

    /**
     * GET /api/projects/{projectId}/intake-session
     * Retrieve the intake session for a project (or create if doesn't exist).
     */
    @GetMapping
    public ResponseEntity<IntakeSessionResponse> getSession(@PathVariable String projectId) {
        IntakeSessionDocument session = sessionService.getOrCreateSession(projectId);
        return ResponseEntity.ok(mapToResponse(session));
    }

    /**
     * POST /api/projects/{projectId}/intake-session/messages
     * Add a message to the intake session.
     */
    @PostMapping("/messages")
    public ResponseEntity<IntakeSessionResponse> addMessage(
        @PathVariable String projectId,
        @RequestBody IntakeMessageRequest request
    ) {
        IntakeSessionDocument session = sessionService.addMessage(
            projectId,
            request.sender(),
            request.text()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(session));
    }

    /**
     * POST /api/projects/{projectId}/intake-session/attachments
     * Upload an attachment for the latest message in the session.
     */
    @PostMapping("/attachments")
    public ResponseEntity<?> uploadAttachment(
        @PathVariable String projectId,
        @RequestParam("file") MultipartFile file
    ) {
        try {
            byte[] fileContent = file.getBytes();
            IntakeAttachmentStorage.StoredAttachment stored = attachmentStorage.storeFile(
                projectId,
                fileContent,
                file.getOriginalFilename()
            );

            // Add attachment to the latest message
            IntakeSessionDocument session = sessionService.addAttachmentToLatestMessage(
                projectId,
                stored.getAttachmentId(),
                file.getOriginalFilename(),
                (int) file.getSize(),
                stored.getStoragePath()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(session));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/projects/{projectId}/intake-session/attachments/{attachmentId}
     * Delete an attachment from storage.
     */
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
        @PathVariable String projectId,
        @PathVariable String attachmentId
    ) {
        // In a real implementation, we would look up the storage name from the session
        // For now, we'll accept the storage name via a query parameter or assume it matches the ID
        attachmentStorage.deleteFile(projectId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/projects/{projectId}/intake-session
     * Clear/delete the entire session for a project.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteSession(@PathVariable String projectId) {
        sessionService.deleteSession(projectId);
        attachmentStorage.deleteProjectAttachments(projectId);
        return ResponseEntity.noContent().build();
    }

    private IntakeSessionResponse mapToResponse(IntakeSessionDocument doc) {
        var messages = doc.getMessages().stream()
            .map(msg -> new IntakeSessionResponse.IntakeMessageResponse(
                msg.getId(),
                msg.getSender(),
                msg.getText(),
                msg.getTimestamp(),
                msg.getAttachments().stream()
                    .map(att -> new IntakeSessionResponse.IntakeAttachmentResponse(
                        att.getId(),
                        att.getName(),
                        att.getSize()
                    ))
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());

        return new IntakeSessionResponse(
            doc.getId(),
            doc.getProjectId(),
            messages,
            doc.getCreatedAt(),
            doc.getUpdatedAt()
        );
    }
}

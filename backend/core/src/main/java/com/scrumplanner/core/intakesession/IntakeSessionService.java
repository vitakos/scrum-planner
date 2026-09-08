package com.scrumplanner.core.intakesession;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// IntakeSessionService (AISC-98): Service layer for managing intake sessions.
// Handles creating sessions, adding messages, and managing attachments.
@Service
public class IntakeSessionService {

    private final IntakeSessionRepository repository;

    public IntakeSessionService(IntakeSessionRepository repository) {
        this.repository = repository;
    }

    /**
     * Get or create an intake session for a project.
     */
    public IntakeSessionDocument getOrCreateSession(String projectId) {
        return repository.findByProjectId(projectId)
            .orElseGet(() -> {
                IntakeSessionDocument session = new IntakeSessionDocument(projectId);
                return repository.save(session);
            });
    }

    /**
     * Get an existing intake session for a project.
     */
    public IntakeSessionDocument getSession(String projectId) {
        return repository.findByProjectId(projectId)
            .orElseThrow(() -> new IllegalArgumentException("No intake session found for project: " + projectId));
    }

    /**
     * Add a plain (user-authored) message to an intake session.
     */
    public IntakeSessionDocument addMessage(String projectId, String sender, String text) {
        return addMessage(projectId, sender, text, IntakeSessionDocument.MessageKind.USER);
    }

    /**
     * Add a message of a given kind to an intake session (AISC-162). Used directly by
     * assistant-generated output (gap analysis, follow-up replies) so it can be
     * distinguished from plain chat turns once persisted.
     */
    public IntakeSessionDocument addMessage(String projectId, String sender, String text, IntakeSessionDocument.MessageKind kind) {
        IntakeSessionDocument session = getOrCreateSession(projectId);

        IntakeSessionDocument.IntakeMessage message = new IntakeSessionDocument.IntakeMessage(
            UUID.randomUUID().toString(),
            sender,
            text,
            OffsetDateTime.now(),
            kind
        );

        List<IntakeSessionDocument.IntakeMessage> messages = new ArrayList<>(session.getMessages());
        messages.add(message);
        session.setMessages(messages);

        return repository.save(session);
    }

    /**
     * Add an attachment to the latest message in a session.
     */
    public IntakeSessionDocument addAttachmentToLatestMessage(String projectId, String attachmentId, String fileName, int fileSize, String filePath) {
        IntakeSessionDocument session = getSession(projectId);

        if (session.getMessages().isEmpty()) {
            throw new IllegalStateException("No messages in session to attach file to");
        }

        List<IntakeSessionDocument.IntakeMessage> messages = new ArrayList<>(session.getMessages());
        IntakeSessionDocument.IntakeMessage lastMessage = messages.get(messages.size() - 1);

        List<IntakeSessionDocument.Attachment> attachments = new ArrayList<>(lastMessage.getAttachments());
        attachments.add(new IntakeSessionDocument.Attachment(attachmentId, fileName, fileSize, filePath));
        lastMessage.setAttachments(attachments);

        session.setMessages(messages);
        return repository.save(session);
    }

    /**
     * Clear all messages from a session.
     */
    public void clearSession(String projectId) {
        repository.findByProjectId(projectId).ifPresent(session -> {
            session.setMessages(new ArrayList<>());
            repository.save(session);
        });
    }

    /**
     * Delete a session.
     */
    public void deleteSession(String projectId) {
        repository.findByProjectId(projectId).ifPresent(repository::delete);
    }
}

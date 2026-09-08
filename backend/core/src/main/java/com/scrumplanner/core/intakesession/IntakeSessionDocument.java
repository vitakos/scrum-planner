package com.scrumplanner.core.intakesession;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// IntakeSessionDocument (AISC-98): MongoDB document holding intake chat session for a project.
// Persists messages and attachments per project.
@Document(collection = "intake_session")
public class IntakeSessionDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String projectId;

    private List<IntakeMessage> messages;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected IntakeSessionDocument() {
        // MongoDB
    }

    public IntakeSessionDocument(String projectId) {
        this.projectId = projectId;
        this.messages = List.of();
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getProjectId() {
        return projectId;
    }

    public List<IntakeMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<IntakeMessage> messages) {
        this.messages = messages;
        this.updatedAt = OffsetDateTime.now();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Nested class for messages
    public static class IntakeMessage {
        private String id;
        private String sender;
        private String text;
        private OffsetDateTime timestamp;
        private List<Attachment> attachments;

        protected IntakeMessage() {
            // MongoDB
        }

        public IntakeMessage(String id, String sender, String text, OffsetDateTime timestamp) {
            this.id = id;
            this.sender = sender;
            this.text = text;
            this.timestamp = timestamp;
            this.attachments = List.of();
        }

        public String getId() {
            return id;
        }

        public String getSender() {
            return sender;
        }

        public String getText() {
            return text;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public List<Attachment> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<Attachment> attachments) {
            this.attachments = attachments;
        }
    }

    // Nested class for attachments
    public static class Attachment {
        private String id;
        private String name;
        private int size;
        private String path;

        protected Attachment() {
            // MongoDB
        }

        public Attachment(String id, String name, int size, String path) {
            this.id = id;
            this.name = name;
            this.size = size;
            this.path = path;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getSize() {
            return size;
        }

        public String getPath() {
            return path;
        }
    }
}

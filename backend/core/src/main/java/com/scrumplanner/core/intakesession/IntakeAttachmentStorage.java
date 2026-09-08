package com.scrumplanner.core.intakesession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// IntakeAttachmentStorage (AISC-99): Local-disk storage for intake message attachments.
// Stores files in a local directory and provides retrieval/deletion methods.
@Service
public class IntakeAttachmentStorage {

    @Value("${intake.attachments.dir:./intake_attachments}")
    private String attachmentDir;

    /**
     * Initialize the attachment storage directory if it doesn't exist.
     */
    public void initialize() {
        Path path = Paths.get(attachmentDir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create attachment directory: " + attachmentDir, e);
            }
        }
    }

    /**
     * Store an uploaded file and return its storage path.
     */
    public StoredAttachment storeFile(String projectId, byte[] fileContent, String originalFileName) {
        initialize();

        String attachmentId = UUID.randomUUID().toString();
        String fileExtension = getFileExtension(originalFileName);
        String storageName = attachmentId + fileExtension;

        // Create project-specific subdirectory
        Path projectDir = Paths.get(attachmentDir, projectId);
        try {
            Files.createDirectories(projectDir);
            Path filePath = projectDir.resolve(storageName);
            Files.write(filePath, fileContent);

            return new StoredAttachment(
                attachmentId,
                storageName,
                filePath.toString(),
                fileContent.length
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to store attachment: " + originalFileName, e);
        }
    }

    /**
     * Retrieve a file from storage.
     */
    public byte[] retrieveFile(String projectId, String storageName) {
        Path filePath = Paths.get(attachmentDir, projectId, storageName);

        try {
            if (!Files.exists(filePath)) {
                throw new IllegalArgumentException("File not found: " + filePath);
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve attachment", e);
        }
    }

    /**
     * Delete a file from storage.
     */
    public void deleteFile(String projectId, String storageName) {
        Path filePath = Paths.get(attachmentDir, projectId, storageName);

        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete attachment", e);
        }
    }

    /**
     * Delete all attachments for a project.
     */
    public void deleteProjectAttachments(String projectId) {
        Path projectDir = Paths.get(attachmentDir, projectId);

        try {
            if (Files.exists(projectDir)) {
                Files.walk(projectDir)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete directory contents", e);
                        }
                    });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete project attachments", e);
        }
    }

    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return "";
    }

    /**
     * Data class for stored attachment information.
     */
    public static class StoredAttachment {
        private final String attachmentId;
        private final String storageName;
        private final String storagePath;
        private final long fileSize;

        public StoredAttachment(String attachmentId, String storageName, String storagePath, long fileSize) {
            this.attachmentId = attachmentId;
            this.storageName = storageName;
            this.storagePath = storagePath;
            this.fileSize = fileSize;
        }

        public String getAttachmentId() {
            return attachmentId;
        }

        public String getStorageName() {
            return storageName;
        }

        public String getStoragePath() {
            return storagePath;
        }

        public long getFileSize() {
            return fileSize;
        }
    }
}

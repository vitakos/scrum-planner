package com.scrumplanner.core.content;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reads and writes the flexible content (Markdown description + custom
 * field values) for a work item, stored in the MongoDB
 * {@code work_item_content} collection and linked from
 * {@code work_item.content_ref} on the Postgres side (see
 * docs/backlog-data-model.md).
 */
@Service
public class WorkItemContentService {

    private final WorkItemContentRepository repository;

    public WorkItemContentService(WorkItemContentRepository repository) {
        this.repository = repository;
    }

    public Optional<WorkItemContent> find(String contentRef) {
        if (contentRef == null) {
            return Optional.empty();
        }
        return repository.findById(contentRef).map(WorkItemContentService::toContent);
    }

    /**
     * Batch lookup for list endpoints, keyed by content_ref, to avoid one
     * Mongo round trip per work item.
     */
    public Map<String, WorkItemContent> findByRefs(List<String> contentRefs) {
        if (contentRefs.isEmpty()) {
            return Map.of();
        }
        return repository.findAllById(contentRefs).stream()
                .collect(Collectors.toMap(WorkItemContentDocument::getId, WorkItemContentService::toContent));
    }

    /**
     * Creates the content document on first write, or updates it in place
     * when a content_ref already exists. {@code description} and
     * {@code customFields} left {@code null} are unchanged (pass an empty
     * string/map to clear them instead). Returns the content_ref to persist
     * back onto the work_item row (unchanged when one already existed).
     */
    public String upsert(String existingContentRef, UUID workItemId, String description, Map<String, Object> customFields) {
        WorkItemContentDocument document = existingContentRef != null
                ? repository.findById(existingContentRef)
                        .orElseGet(() -> new WorkItemContentDocument(workItemId.toString()))
                : new WorkItemContentDocument(workItemId.toString());

        if (description != null) {
            document.setDescription(description);
        }
        if (customFields != null) {
            document.setCustomFields(customFields);
        }
        return repository.save(document).getId();
    }

    public void delete(String contentRef) {
        if (contentRef != null) {
            repository.deleteById(contentRef);
        }
    }

    private static WorkItemContent toContent(WorkItemContentDocument document) {
        return new WorkItemContent(
                document.getDescription(),
                document.getCustomFields() != null ? document.getCustomFields() : new HashMap<>());
    }

    public record WorkItemContent(String description, Map<String, Object> customFields) {
        public static final WorkItemContent EMPTY = new WorkItemContent(null, Map.of());
    }
}

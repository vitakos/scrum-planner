package com.scrumplanner.core.content;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reads and writes the Markdown content body for a work item, stored in the
 * MongoDB {@code work_item_content} collection and linked from
 * {@code work_item.content_ref} on the Postgres side (see
 * docs/backlog-data-model.md). Kept generic — a plain text body keyed by
 * content_ref — since this is also the persistence the future Intake
 * Assistant epic will use for gap-analysis notes and drafted-item rationale.
 */
@Service
public class WorkItemContentService {

    private final WorkItemContentRepository repository;

    public WorkItemContentService(WorkItemContentRepository repository) {
        this.repository = repository;
    }

    public Optional<String> findContent(String contentRef) {
        if (contentRef == null) {
            return Optional.empty();
        }
        return repository.findById(contentRef).map(WorkItemContentDocument::getDescription);
    }

    /**
     * Batch lookup for list endpoints, keyed by content_ref, to avoid one
     * Mongo round trip per work item.
     */
    public Map<String, String> findContentByRefs(List<String> contentRefs) {
        if (contentRefs.isEmpty()) {
            return Map.of();
        }
        return repository.findAllById(contentRefs).stream()
                .collect(Collectors.toMap(
                        WorkItemContentDocument::getId,
                        doc -> doc.getDescription() != null ? doc.getDescription() : ""));
    }

    /**
     * Creates the content document on first write, or updates it in place
     * when a content_ref already exists. Returns the content_ref to persist
     * back onto the work_item row (unchanged when one already existed).
     */
    public String saveContent(String existingContentRef, UUID workItemId, String markdown) {
        WorkItemContentDocument document = existingContentRef != null
                ? repository.findById(existingContentRef)
                        .orElseGet(() -> new WorkItemContentDocument(workItemId.toString(), null))
                : new WorkItemContentDocument(workItemId.toString(), null);
        document.setDescription(markdown);
        return repository.save(document).getId();
    }

    public void deleteContent(String contentRef) {
        if (contentRef != null) {
            repository.deleteById(contentRef);
        }
    }
}

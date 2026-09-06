package com.scrumplanner.core.workitem;

import com.scrumplanner.core.workitem.dto.WorkItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Project-agnostic work item lookup by human-readable key (e.g. "SPAI-1").
 * Separate from WorkItemController, which is scoped under a known
 * project id — this exists specifically so the frontend can resolve a
 * work item (and, from its response, which project it belongs to) from a
 * bare key, such as a URL path segment or a "$SPAI-1" reference inserted
 * from the rich text editor. See WorkItemService#getWorkItemByKey.
 */
@RestController
@RequestMapping("/api/work-items")
public class WorkItemLookupController {

    private final WorkItemService workItemService;

    public WorkItemLookupController(WorkItemService workItemService) {
        this.workItemService = workItemService;
    }

    @GetMapping("/by-key/{key}")
    public WorkItemResponse getByKey(@PathVariable String key) {
        return workItemService.getWorkItemByKey(key);
    }
}

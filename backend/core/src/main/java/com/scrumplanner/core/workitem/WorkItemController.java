package com.scrumplanner.core.workitem;

import com.scrumplanner.core.workitem.dto.ApplyTransitionRequest;
import com.scrumplanner.core.workitem.dto.CreateWorkItemRequest;
import com.scrumplanner.core.workitem.dto.UpdateWorkItemRequest;
import com.scrumplanner.core.workitem.dto.WorkItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/work-items")
public class WorkItemController {

    private final WorkItemService workItemService;

    public WorkItemController(WorkItemService workItemService) {
        this.workItemService = workItemService;
    }

    @GetMapping
    public List<WorkItemResponse> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String type
    ) {
        return workItemService.listWorkItems(projectId, type);
    }

    @GetMapping("/{id}")
    public WorkItemResponse get(@PathVariable UUID projectId, @PathVariable UUID id) {
        return workItemService.getWorkItem(projectId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkItemResponse create(@PathVariable UUID projectId, @Valid @RequestBody CreateWorkItemRequest request) {
        return workItemService.createWorkItem(projectId, request);
    }

    @PutMapping("/{id}")
    public WorkItemResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkItemRequest request
    ) {
        return workItemService.updateWorkItem(projectId, id, request);
    }

    @PostMapping("/{id}/transitions")
    public WorkItemResponse applyTransition(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody ApplyTransitionRequest request
    ) {
        return workItemService.applyTransition(projectId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID projectId, @PathVariable UUID id) {
        workItemService.deleteWorkItem(projectId, id);
    }
}

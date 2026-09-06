package com.scrumplanner.core.worktype;

import com.scrumplanner.core.worktype.dto.WorkItemTypeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/work-item-types")
public class WorkItemTypeController {

    private final WorkItemTypeCatalogRepository repository;

    public WorkItemTypeController(WorkItemTypeCatalogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<WorkItemTypeResponse> list() {
        return repository.findAllByOrderBySortOrderAsc().stream()
                .map(WorkItemTypeResponse::from)
                .toList();
    }
}

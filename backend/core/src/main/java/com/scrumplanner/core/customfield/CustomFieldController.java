package com.scrumplanner.core.customfield;

import com.scrumplanner.core.customfield.dto.CreateCustomFieldRequest;
import com.scrumplanner.core.customfield.dto.CustomFieldDefinitionResponse;
import com.scrumplanner.core.customfield.dto.FieldCatalogResponse;
import com.scrumplanner.core.customfield.dto.UpdateCustomFieldRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/custom-fields")
public class CustomFieldController {

    private final CustomFieldService customFieldService;

    public CustomFieldController(CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @GetMapping
    public List<FieldCatalogResponse> list(@PathVariable UUID projectId) {
        return customFieldService.listFieldCatalogs(projectId);
    }

    @PostMapping("/{workItemType}")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomFieldDefinitionResponse addField(
            @PathVariable UUID projectId,
            @PathVariable String workItemType,
            @Valid @RequestBody CreateCustomFieldRequest request
    ) {
        return customFieldService.addField(projectId, workItemType, request);
    }

    @PutMapping("/{workItemType}/{fieldId}")
    public CustomFieldDefinitionResponse updateField(
            @PathVariable UUID projectId,
            @PathVariable String workItemType,
            @PathVariable UUID fieldId,
            @Valid @RequestBody UpdateCustomFieldRequest request
    ) {
        return customFieldService.updateField(projectId, workItemType, fieldId, request);
    }

    @DeleteMapping("/{workItemType}/{fieldId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteField(
            @PathVariable UUID projectId,
            @PathVariable String workItemType,
            @PathVariable UUID fieldId
    ) {
        customFieldService.deleteField(projectId, workItemType, fieldId);
    }
}

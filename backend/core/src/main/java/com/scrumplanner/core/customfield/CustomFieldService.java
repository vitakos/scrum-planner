package com.scrumplanner.core.customfield;

import com.scrumplanner.core.common.ConflictException;
import com.scrumplanner.core.common.NotFoundException;
import com.scrumplanner.core.customfield.dto.CreateCustomFieldRequest;
import com.scrumplanner.core.customfield.dto.CustomFieldDefinitionResponse;
import com.scrumplanner.core.customfield.dto.FieldCatalogResponse;
import com.scrumplanner.core.customfield.dto.PredefinedFieldResponse;
import com.scrumplanner.core.customfield.dto.UpdateCustomFieldRequest;
import com.scrumplanner.core.workflow.WorkflowDefinitionRepository;
import com.scrumplanner.core.worktype.WorkItemTypeCatalog;
import com.scrumplanner.core.worktype.WorkItemTypeCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomFieldService {

    // Kept in sync with chk_custom_field_definition_data_type in
    // database/sql/migrations/0008_custom_field_definition_data_type_check.sql.
    private static final Set<String> VALID_DATA_TYPES = Set.of(
            "TEXT", "NUMBER", "DATE", "BOOLEAN", "SINGLE_SELECT", "MULTI_SELECT"
    );
    private static final Set<String> SELECT_DATA_TYPES = Set.of("SINGLE_SELECT", "MULTI_SELECT");

    private final WorkItemTypeCatalogRepository typeCatalogRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;

    public CustomFieldService(
            WorkItemTypeCatalogRepository typeCatalogRepository,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            CustomFieldDefinitionRepository customFieldDefinitionRepository
    ) {
        this.typeCatalogRepository = typeCatalogRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.customFieldDefinitionRepository = customFieldDefinitionRepository;
    }

    @Transactional(readOnly = true)
    public List<FieldCatalogResponse> listFieldCatalogs(UUID projectId) {
        Map<String, String> typeNames = typeCatalogRepository.findAll().stream()
                .collect(Collectors.toMap(WorkItemTypeCatalog::getCode, WorkItemTypeCatalog::getName));

        return workflowDefinitionRepository.findAllByProjectId(projectId).stream()
                .map(workflow -> toCatalogResponse(
                        projectId, workflow.getWorkItemType(),
                        typeNames.getOrDefault(workflow.getWorkItemType(), workflow.getWorkItemType())
                ))
                .toList();
    }

    @Transactional
    public CustomFieldDefinitionResponse addField(UUID projectId, String workItemType, CreateCustomFieldRequest request) {
        requireWorkItemType(projectId, workItemType);
        String name = request.name().trim();
        String dataType = parseDataType(request.dataType());
        List<String> options = normalizeOptions(dataType, request.options());
        requireNameAvailable(projectId, workItemType, name, null);

        CustomFieldDefinition field = customFieldDefinitionRepository.save(
                new CustomFieldDefinition(projectId, workItemType, name, dataType, options)
        );
        return CustomFieldDefinitionResponse.from(field);
    }

    @Transactional
    public CustomFieldDefinitionResponse updateField(
            UUID projectId, String workItemType, UUID fieldId, UpdateCustomFieldRequest request
    ) {
        requireWorkItemType(projectId, workItemType);
        CustomFieldDefinition field = requireField(projectId, workItemType, fieldId);
        String name = request.name().trim();
        String dataType = parseDataType(request.dataType());
        List<String> options = normalizeOptions(dataType, request.options());
        requireNameAvailable(projectId, workItemType, name, fieldId);

        field.update(name, dataType, options);
        return CustomFieldDefinitionResponse.from(field);
    }

    @Transactional
    public void deleteField(UUID projectId, String workItemType, UUID fieldId) {
        requireWorkItemType(projectId, workItemType);
        CustomFieldDefinition field = requireField(projectId, workItemType, fieldId);
        customFieldDefinitionRepository.delete(field);
    }

    private FieldCatalogResponse toCatalogResponse(UUID projectId, String workItemType, String workItemTypeName) {
        List<PredefinedFieldResponse> predefinedFields = PredefinedFieldCatalog.list().stream()
                .map(PredefinedFieldResponse::from)
                .toList();
        List<CustomFieldDefinitionResponse> customFields = customFieldDefinitionRepository
                .findAllByProjectIdAndWorkItemTypeOrderByNameAsc(projectId, workItemType).stream()
                .map(CustomFieldDefinitionResponse::from)
                .toList();
        return new FieldCatalogResponse(workItemType, workItemTypeName, predefinedFields, customFields);
    }

    private void requireNameAvailable(UUID projectId, String workItemType, String name, UUID excludingFieldId) {
        boolean clashesWithPredefined = PredefinedFieldCatalog.list().stream()
                .anyMatch(f -> f.name().equalsIgnoreCase(name));
        if (clashesWithPredefined) {
            throw new ConflictException("'" + name + "' is a pre-defined field and cannot be used as a custom field name");
        }
        boolean clashesWithCustom = customFieldDefinitionRepository
                .findAllByProjectIdAndWorkItemTypeOrderByNameAsc(projectId, workItemType).stream()
                .anyMatch(f -> !f.getId().equals(excludingFieldId) && f.getName().equalsIgnoreCase(name));
        if (clashesWithCustom) {
            throw new ConflictException("A custom field named '" + name + "' already exists for this work item type");
        }
    }

    private String parseDataType(String dataType) {
        if (dataType == null || !VALID_DATA_TYPES.contains(dataType)) {
            throw new IllegalArgumentException("dataType must be one of " + VALID_DATA_TYPES);
        }
        return dataType;
    }

    private List<String> normalizeOptions(String dataType, List<String> options) {
        List<String> cleaned = options == null
                ? List.of()
                : options.stream()
                        .filter(o -> o != null && !o.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();

        if (SELECT_DATA_TYPES.contains(dataType)) {
            if (cleaned.isEmpty()) {
                throw new IllegalArgumentException("options must include at least one value for a " + dataType + " field");
            }
            return cleaned;
        }
        if (!cleaned.isEmpty()) {
            throw new IllegalArgumentException("options are only allowed for SINGLE_SELECT or MULTI_SELECT fields");
        }
        return null;
    }

    private void requireWorkItemType(UUID projectId, String workItemType) {
        workflowDefinitionRepository.findByProjectIdAndWorkItemType(projectId, workItemType)
                .orElseThrow(() -> new NotFoundException(
                        "No work item type '" + workItemType + "' configured for project " + projectId));
    }

    private CustomFieldDefinition requireField(UUID projectId, String workItemType, UUID fieldId) {
        return customFieldDefinitionRepository.findById(fieldId)
                .filter(f -> f.getProjectId().equals(projectId) && f.getWorkItemType().equals(workItemType))
                .orElseThrow(() -> new NotFoundException("Custom field not found in this work item type: " + fieldId));
    }
}

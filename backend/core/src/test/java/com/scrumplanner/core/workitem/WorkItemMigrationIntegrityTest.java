package com.scrumplanner.core.workitem;

import com.scrumplanner.core.workitem.dto.WorkItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers AISC-53's acceptance criteria: after migration 0011 renames the
 * "Bug" work item type to "Defect", a previously-Bug work item must report
 * type "defect" with every other field (title, content, custom fields,
 * state, parent, timestamps) unchanged, and no work item is findable by
 * type "bug" any more.
 *
 * {@code @WebMvcTest} slice (no real Postgres required), same approach as
 * {@link WorkItemTypeControllerTest} and {@link WorkItemControllerTest}:
 * the HTTP layer is exercised for real, while {@link WorkItemService} is
 * mocked to stand in for the (already-migrated) work_item table.
 */
@WebMvcTest(WorkItemController.class)
class WorkItemMigrationIntegrityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkItemService workItemService;

    @Test
    void migratedItemReportsDefectTypeWithOtherFieldsUnchanged() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID workItemId = UUID.randomUUID();
        UUID stateId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-05T10:15:30Z");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-02-10T08:00:00Z");

        WorkItemResponse migrated = new WorkItemResponse(
                workItemId, "SPAI-42", projectId, "defect", "Defect",
                "Login button unresponsive", "Steps to reproduce: ...",
                Map.of("Severity", "High"),
                stateId, "In Progress", "in_progress",
                parentId, "SPAI-10", "Auth epic",
                0, List.of(), createdAt, updatedAt);

        when(workItemService.getWorkItem(eq(projectId), eq(workItemId))).thenReturn(migrated);

        mockMvc.perform(get("/api/projects/{projectId}/work-items/{id}", projectId, workItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("defect"))
                .andExpect(jsonPath("$.typeName").value("Defect"))
                .andExpect(jsonPath("$.title").value("Login button unresponsive"))
                .andExpect(jsonPath("$.content").value("Steps to reproduce: ..."))
                .andExpect(jsonPath("$.customFields.Severity").value("High"))
                .andExpect(jsonPath("$.stateName").value("In Progress"))
                .andExpect(jsonPath("$.parentKey").value("SPAI-10"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void queryingWorkItemsByBugTypeReturnsNoResults() throws Exception {
        UUID projectId = UUID.randomUUID();

        when(workItemService.listWorkItems(eq(projectId), eq("bug"), eq(false))).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/{projectId}/work-items", projectId).param("type", "bug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}

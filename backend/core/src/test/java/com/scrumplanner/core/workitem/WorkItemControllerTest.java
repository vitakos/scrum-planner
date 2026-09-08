package com.scrumplanner.core.workitem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumplanner.core.workitem.dto.CreateWorkItemRequest;
import com.scrumplanner.core.workitem.dto.WorkItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the AISC-51 Scenario 2 acceptance criterion: after the "Bug" ->
 * "Defect" catalog rename, POSTing a work item with type "defect" must
 * succeed and come back as type "Defect".
 *
 * {@code @WebMvcTest} slice (no real Postgres required): the HTTP layer and
 * {@code @Valid} validation are exercised for real, while
 * {@link WorkItemService} is mocked to stand in for persistence and catalog
 * lookup.
 */
@WebMvcTest(WorkItemController.class)
class WorkItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkItemService workItemService;

    @Test
    void creatingWorkItemWithDefectTypeSucceeds() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID workItemId = UUID.randomUUID();

        WorkItemResponse response = new WorkItemResponse(
                workItemId, "SPAI-100", projectId, "defect", "Defect", "Login button unresponsive",
                null, null, UUID.randomUUID(), "To Do", "to_do", null, null, null, 0,
                List.of(), OffsetDateTime.now(), OffsetDateTime.now());

        when(workItemService.createWorkItem(eq(projectId), any(CreateWorkItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/projects/{projectId}/work-items", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateWorkItemRequest("defect", "Login button unresponsive", null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("defect"))
                .andExpect(jsonPath("$.typeName").value("Defect"));
    }
}

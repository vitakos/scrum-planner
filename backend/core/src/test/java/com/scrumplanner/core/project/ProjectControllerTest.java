package com.scrumplanner.core.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumplanner.core.project.dto.UpdateProjectRequest;
import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the AISC-12 acceptance scenarios for updating a project's
 * repository URL through the PUT /api/projects/{id} endpoint:
 *   1. A syntactically valid URL is saved and reflected on a later GET.
 *   2. An empty/absent value is accepted (the field is optional).
 *   3. A syntactically invalid value is rejected with 400.
 *
 * This is a @WebMvcTest slice (no real Postgres/Mongo required): the HTTP
 * layer, {@code @Valid} validation and {@link com.scrumplanner.core.common.ApiExceptionHandler}
 * are exercised for real, while {@link ProjectService} is mocked to stand
 * in for persistence.
 */
@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @Test
    void savingValidRepositoryUrlPersistsAndIsReflectedOnGet() throws Exception {
        UUID projectId = UUID.randomUUID();
        String repositoryUrl = "https://github.com/example/scrum-planner";

        Project project = new Project("SPAI", "Scrum Planner", null);
        project.updateRepositoryUrl(repositoryUrl);

        when(projectService.updateProject(eq(projectId), any(UpdateProjectRequest.class))).thenReturn(project);
        when(projectService.getProject(projectId)).thenReturn(project);

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProjectRequest(repositoryUrl))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryUrl").value(repositoryUrl));

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryUrl").value(repositoryUrl));
    }

    @Test
    void savingEmptyRepositoryUrlIsAccepted() throws Exception {
        UUID projectId = UUID.randomUUID();

        Project project = new Project("SPAI", "Scrum Planner", null);
        project.updateRepositoryUrl(null);

        when(projectService.updateProject(eq(projectId), any(UpdateProjectRequest.class))).thenReturn(project);

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProjectRequest(""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryUrl").value(Matchers.nullValue()));
    }

    @Test
    void savingInvalidRepositoryUrlIsRejected() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProjectRequest("not a url"))))
                .andExpect(status().isBadRequest());
    }

    /**
     * AISC-13: a configured repository URL must be readable back through the
     * plain GET /api/projects/{id} endpoint (not only right after a PUT), so
     * the frontend can display it. Exercises GET in isolation, independent of
     * the update flow already covered above.
     */
    @Test
    void gettingProjectWithConfiguredRepositoryUrlReturnsIt() throws Exception {
        UUID projectId = UUID.randomUUID();
        String repositoryUrl = "git@github.com:example/scrum-planner.git";

        Project project = new Project("SPAI", "Scrum Planner", null);
        project.updateRepositoryUrl(repositoryUrl);

        when(projectService.getProject(projectId)).thenReturn(project);

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryUrl").value(repositoryUrl));
    }
}

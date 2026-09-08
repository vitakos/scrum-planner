package com.scrumplanner.core.worktype;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the AISC-51 Scenario 1 acceptance criterion: after the "Bug" ->
 * "Defect" catalog rename, GET /api/work-item-types must return "Defect" as
 * a type name/label and no entry named "Bug" remains.
 *
 * {@code @WebMvcTest} slice (no real Postgres required): the HTTP layer is
 * exercised for real, while {@link WorkItemTypeCatalogRepository} is mocked
 * to stand in for the (already-migrated) catalog table.
 */
@WebMvcTest(WorkItemTypeController.class)
class WorkItemTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkItemTypeCatalogRepository repository;

    @Test
    void catalogListsDefectAndNoLongerListsBug() throws Exception {
        WorkItemTypeCatalog defect = newCatalogEntry("defect", "Defect", true, 50);

        when(repository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(defect));

        mockMvc.perform(get("/api/work-item-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Defect')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Bug')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.code == 'bug')]").doesNotExist());
    }

    /**
     * Covers AISC-64 Scenario 1: on a fresh environment (all migrations
     * through 0012 applied, no separate seed step — see database/README.md)
     * the catalog contains exactly the seven current system types, with no
     * leftover "Bug"/"Test Run" entries and nothing missing.
     */
    @Test
    void freshCatalogContainsExactlyTheSevenCurrentTypes() throws Exception {
        List<WorkItemTypeCatalog> freshCatalog = List.of(
                newCatalogEntry("epic", "Epic", true, 10),
                newCatalogEntry("feature", "Feature", true, 20),
                newCatalogEntry("user_story", "User Story", true, 30),
                newCatalogEntry("task", "Task", true, 40),
                newCatalogEntry("defect", "Defect", true, 50),
                newCatalogEntry("test_case", "Test Case", true, 60),
                newCatalogEntry("issue", "Issue", true, 70));

        when(repository.findAllByOrderBySortOrderAsc()).thenReturn(freshCatalog);

        mockMvc.perform(get("/api/work-item-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].code").value("epic"))
                .andExpect(jsonPath("$[1].code").value("feature"))
                .andExpect(jsonPath("$[2].code").value("user_story"))
                .andExpect(jsonPath("$[3].code").value("task"))
                .andExpect(jsonPath("$[4].code").value("defect"))
                .andExpect(jsonPath("$[5].code").value("test_case"))
                .andExpect(jsonPath("$[6].code").value("issue"))
                .andExpect(jsonPath("$[?(@.code == 'bug')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.code == 'test_run')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.name == 'Bug')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.name == 'Test Run')]").doesNotExist());
    }

    private static WorkItemTypeCatalog newCatalogEntry(String code, String name, boolean system, int sortOrder) {
        WorkItemTypeCatalog entry = BeanUtils.instantiateClass(WorkItemTypeCatalog.class);
        ReflectionTestUtils.setField(entry, "code", code);
        ReflectionTestUtils.setField(entry, "name", name);
        ReflectionTestUtils.setField(entry, "system", system);
        ReflectionTestUtils.setField(entry, "sortOrder", sortOrder);
        return entry;
    }
}

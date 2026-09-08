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

    private static WorkItemTypeCatalog newCatalogEntry(String code, String name, boolean system, int sortOrder) {
        WorkItemTypeCatalog entry = BeanUtils.instantiateClass(WorkItemTypeCatalog.class);
        ReflectionTestUtils.setField(entry, "code", code);
        ReflectionTestUtils.setField(entry, "name", name);
        ReflectionTestUtils.setField(entry, "system", system);
        ReflectionTestUtils.setField(entry, "sortOrder", sortOrder);
        return entry;
    }
}

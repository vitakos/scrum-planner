package com.scrumplanner.core.workitem;

import com.scrumplanner.core.common.ConflictException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for WorkItemRelationRules. Covers AISC-57: validating allowed
 * parent-child type relationships, including the test_case -> user_story
 * relation; and AISC-61/AISC-62: Issue as a root-level type with no
 * required parent, optionally linkable to epic/feature/user_story.
 */
class WorkItemRelationRulesTest {

    @Test
    void testCaseCanHaveUserStoryAsParent() {
        // AISC-57: Test Case should accept User Story as parent
        WorkItemRelationRules.validateParentType("test_case", "user_story");
        // No exception thrown = success
    }

    @Test
    void testCaseCannotHaveFeatureAsParent() {
        assertThatThrownBy(() -> WorkItemRelationRules.validateParentType("test_case", "feature"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("test_case")
                .hasMessageContaining("feature");
    }

    @Test
    void userStoryCanHaveFeatureAsParent() {
        WorkItemRelationRules.validateParentType("user_story", "feature");
        // No exception thrown = success
    }

    @Test
    void userStoryCannotHaveEpicAsParent() {
        assertThatThrownBy(() -> WorkItemRelationRules.validateParentType("user_story", "epic"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("user_story")
                .hasMessageContaining("epic");
    }

    @Test
    void featureCanHaveEpicAsParent() {
        WorkItemRelationRules.validateParentType("feature", "epic");
        // No exception thrown = success
    }

    @Test
    void taskCanHaveUserStoryAsParent() {
        WorkItemRelationRules.validateParentType("task", "user_story");
        // No exception thrown = success
    }

    @Test
    void defectCanHaveUserStoryAsParent() {
        WorkItemRelationRules.validateParentType("defect", "user_story");
        // No exception thrown = success
    }

    // AISC-61/AISC-62: Issue is a root-level type — it has no required parent, but optionally
    // accepts epic, feature, or user_story as parents to track issues against relevant work.

    @Test
    void issueCanHaveEpicAsParent() {
        WorkItemRelationRules.validateParentType("issue", "epic");
        // No exception thrown = success
    }

    @Test
    void issueCanHaveFeatureAsParent() {
        WorkItemRelationRules.validateParentType("issue", "feature");
        // No exception thrown = success
    }

    @Test
    void issueCanHaveUserStoryAsParent() {
        WorkItemRelationRules.validateParentType("issue", "user_story");
        // No exception thrown = success
    }

    @Test
    void issueCannotHaveTestCaseAsParent() {
        assertThatThrownBy(() -> WorkItemRelationRules.validateParentType("issue", "test_case"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("issue")
                .hasMessageContaining("test_case");
    }

    @Test
    void issueCannotHaveTaskOrDefectAsParent() {
        // Issue cannot link to Task or Defect (only to the three top-level types)
        for (String parentType : new String[] {"task", "defect"}) {
            assertThatThrownBy(() -> WorkItemRelationRules.validateParentType("issue", parentType))
                    .as("issue should not accept '%s' as a parent", parentType)
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Test
    void epicCannotHaveParent() {
        assertThatThrownBy(() -> WorkItemRelationRules.validateParentType("epic", "feature"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("epic");
    }

    @Test
    void isValidParentReturnsTrueForAllowedRelation() {
        assertThat(WorkItemRelationRules.isValidParent("test_case", "user_story")).isTrue();
        assertThat(WorkItemRelationRules.isValidParent("user_story", "feature")).isTrue();
        assertThat(WorkItemRelationRules.isValidParent("task", "user_story")).isTrue();
        assertThat(WorkItemRelationRules.isValidParent("issue", "epic")).isTrue();
        assertThat(WorkItemRelationRules.isValidParent("issue", "feature")).isTrue();
        assertThat(WorkItemRelationRules.isValidParent("issue", "user_story")).isTrue();
    }

    @Test
    void isValidParentReturnsFalseForDisallowedRelation() {
        assertThat(WorkItemRelationRules.isValidParent("test_case", "feature")).isFalse();
        assertThat(WorkItemRelationRules.isValidParent("user_story", "epic")).isFalse();
        assertThat(WorkItemRelationRules.isValidParent("epic", "feature")).isFalse();
        assertThat(WorkItemRelationRules.isValidParent("issue", "test_case")).isFalse();
    }
}

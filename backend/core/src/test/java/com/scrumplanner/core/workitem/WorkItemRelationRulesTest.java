package com.scrumplanner.core.workitem;

import com.scrumplanner.core.common.ConflictException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for WorkItemRelationRules. Covers AISC-57: validating allowed
 * parent-child type relationships, including the new test_case -> user_story
 * relation.
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

    @Test
    void issueCanHaveTestCaseAsParent() {
        WorkItemRelationRules.validateParentType("issue", "test_case");
        // No exception thrown = success
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
    }

    @Test
    void isValidParentReturnsFalseForDisallowedRelation() {
        assertThat(WorkItemRelationRules.isValidParent("test_case", "feature")).isFalse();
        assertThat(WorkItemRelationRules.isValidParent("user_story", "epic")).isFalse();
        assertThat(WorkItemRelationRules.isValidParent("epic", "feature")).isFalse();
    }
}

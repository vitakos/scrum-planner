package com.scrumplanner.core.workitem;

import com.scrumplanner.core.common.ConflictException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines which work item types can be parents for other work item types.
 * This enforces the valid hierarchy: Epic -> Feature -> User Story -> Task/Defect/Test Case -> Issue.
 *
 * Used by WorkItemService to validate parent-child relationships when creating or updating work items.
 */
public class WorkItemRelationRules {

    private static final Map<String, Set<String>> ALLOWED_PARENTS = new HashMap<>();

    static {
        // Initialize the allowed parent types for each work item type
        // Format: childType -> set of allowed parentTypes

        // feature can have epic as parent
        ALLOWED_PARENTS.put("feature", Set.of("epic"));

        // user_story can have feature as parent
        ALLOWED_PARENTS.put("user_story", Set.of("feature"));

        // task can have user_story as parent
        ALLOWED_PARENTS.put("task", Set.of("user_story"));

        // defect can have user_story as parent
        ALLOWED_PARENTS.put("defect", Set.of("user_story"));

        // test_case can only have user_story as parent (AISC-57 adds, AISC-59 removes task/defect)
        ALLOWED_PARENTS.put("test_case", Set.of("user_story"));

        // issue can have test_case as parent
        ALLOWED_PARENTS.put("issue", Set.of("test_case"));
    }

    /**
     * Validates that a child work item type can have a parent of the given type.
     *
     * @param childType the type of the work item being created/updated
     * @param parentType the type of the proposed parent work item
     * @throws ConflictException if the parent type is not allowed for this child type
     */
    public static void validateParentType(String childType, String parentType) {
        Set<String> allowedParents = ALLOWED_PARENTS.get(childType);

        if (allowedParents == null) {
            // Unknown type - no allowed parents defined (e.g., epic, or future custom types with no hierarchy)
            throw new ConflictException(
                    "Work item type '" + childType + "' cannot have a parent in this system");
        }

        if (!allowedParents.contains(parentType)) {
            throw new ConflictException(
                    "Work item type '" + childType + "' cannot have a parent of type '" + parentType + "'");
        }
    }

    /**
     * Returns true if the child type can have a parent of the given type, false otherwise.
     */
    public static boolean isValidParent(String childType, String parentType) {
        Set<String> allowedParents = ALLOWED_PARENTS.get(childType);
        return allowedParents != null && allowedParents.contains(parentType);
    }
}

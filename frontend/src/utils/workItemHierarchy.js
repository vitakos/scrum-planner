// The standard work item type hierarchy for this app:
// Epic -> Feature -> User Story -> Task/Defect -> Test Case -> Issue.
// Keyed by child type -> its natural parent type(s). Epic has no parent,
// Issue has no natural child. A type not listed here (e.g. a future
// custom type) simply gets no parent/child affordances.
export const NATURAL_PARENT_TYPES = {
  feature: ['epic'],
  user_story: ['feature'],
  task: ['user_story'],
  defect: ['user_story'],
  test_case: ['user_story'],
  issue: ['test_case']
};

const NATURAL_CHILD_TYPES = Object.entries(NATURAL_PARENT_TYPES).reduce((acc, [childType, parentTypes]) => {
  for (const parentType of parentTypes) {
    (acc[parentType] ?? (acc[parentType] = [])).push(childType);
  }
  return acc;
}, {});

export function parentTypesFor(type) {
  return NATURAL_PARENT_TYPES[type] ?? [];
}

export function childTypesFor(type) {
  return NATURAL_CHILD_TYPES[type] ?? [];
}

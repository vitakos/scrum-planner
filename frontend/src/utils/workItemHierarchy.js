// The standard work item type hierarchy for this app:
// Epic -> Feature -> User Story -> Task/Defect/Test Case.
// Keyed by child type -> its natural parent type(s). Epic has no parent.
// Issue sits outside this chain: it's a root-level type with no required
// parent (AISC-61), optionally linkable to epic/feature/user_story
// (AISC-62) to track customer-reported issues against planned work.
// A type not listed here (e.g. a future custom type) simply gets no
// parent/child affordances.
export const NATURAL_PARENT_TYPES = {
  feature: ['epic'],
  user_story: ['feature'],
  task: ['user_story'],
  defect: ['user_story'],
  test_case: ['user_story'],
  issue: ['epic', 'feature', 'user_story']
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

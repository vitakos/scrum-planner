import { describe, it, expect } from 'vitest';
import { parentTypesFor, childTypesFor } from './workItemHierarchy.js';

describe('workItemHierarchy', () => {
  describe('parentTypesFor', () => {
    it('returns the natural parent types for a defect', () => {
      expect(parentTypesFor('defect')).toEqual(['user_story']);
    });

    it('returns user_story as the natural parent for test_case (AISC-57)', () => {
      expect(parentTypesFor('test_case')).toEqual(['user_story']);
    });

    it('returns an empty array for a type with no configured parent', () => {
      expect(parentTypesFor('epic')).toEqual([]);
    });

    // AISC-61/AISC-62: Issue is a root-level type with no required parent, but optionally
    // accepts epic, feature, or user_story as optional parents (to track issues against
    // relevant work without requiring a parent).
    it('returns epic, feature, user_story as optional parents for issue (AISC-61/62)', () => {
      expect(parentTypesFor('issue')).toEqual(
        expect.arrayContaining(['epic', 'feature', 'user_story'])
      );
      // Verify there are exactly 3 parents (not including any others)
      expect(parentTypesFor('issue').length).toBe(3);
    });

    it('returns an empty array for an unknown type', () => {
      expect(parentTypesFor('not_a_real_type')).toEqual([]);
    });
  });

  describe('childTypesFor', () => {
    it('includes defect among the natural child types of a user story', () => {
      expect(childTypesFor('user_story')).toEqual(expect.arrayContaining(['defect']));
    });

    it('includes test_case among the natural child types of a user story (AISC-57)', () => {
      expect(childTypesFor('user_story')).toEqual(expect.arrayContaining(['test_case']));
    });

    // AISC-61/AISC-62: Issue is a root-level type with no parent and no children.
    // Even after AISC-62 adds epic/feature/user_story as optional parents for Issue,
    // none of those types will have Issue as a child (because Issue doesn't reverse-link).
    it('returns an empty array for a type with no configured children (issue, AISC-61)', () => {
      expect(childTypesFor('issue')).toEqual([]);
    });
  });
});

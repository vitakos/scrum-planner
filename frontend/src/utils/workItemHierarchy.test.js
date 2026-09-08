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

    it('returns an empty array for a type with no configured children', () => {
      expect(childTypesFor('issue')).toEqual([]);
    });
  });
});

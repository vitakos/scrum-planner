-- 0013_reparent_legacy_qa_children.sql
-- Fixes the parent-child relations for existing work items under the new hierarchy rules.
--
-- Old chain (linear): Epic → Feature → User Story → Task → Test Case → Test Run (Issue)
-- New rules:
--   Epic → Feature → User Story → {Task, Test Case, Defect}
--   Issue: optional parent (can be root-level, or linked to Epic/Feature/User Story)
--
-- This migration re-parents:
-- 1. Test Cases from Task → User Story (the Task's parent).
-- 2. Defects from Task → User Story (the Task's parent).
-- 3. Issues from Test Case → User Story (the Test Case's grandparent).
-- 4. Any row that can't be cleanly mapped is left as-is and logged.
--
-- After re-parenting, a diagnostic query lists any remaining rule violations.

-- 1. Re-parent Test Cases whose parent is a Task → the Task's parent (User Story)
UPDATE work_item tc
SET parent_id = (SELECT parent_id FROM work_item t WHERE t.id = tc.parent_id)
WHERE tc.type = 'test_case'
  AND tc.parent_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM work_item t WHERE t.id = tc.parent_id AND t.type = 'task')
  AND (SELECT parent_id FROM work_item t WHERE t.id = tc.parent_id) IS NOT NULL;

-- Log Test Cases that had a Task parent with no grandparent (orphaned Task)
DO $$
DECLARE
  orphaned_tc_count INT;
BEGIN
  SELECT COUNT(*) INTO orphaned_tc_count
  FROM work_item tc
  WHERE tc.type = 'test_case'
    AND tc.parent_id IS NOT NULL
    AND EXISTS (SELECT 1 FROM work_item t WHERE t.id = tc.parent_id AND t.type = 'task')
    AND (SELECT parent_id FROM work_item t WHERE t.id = tc.parent_id) IS NULL;

  IF orphaned_tc_count > 0 THEN
    RAISE NOTICE '[0013] Found % Test Case(s) with orphaned Task parent (no reparenting done)', orphaned_tc_count;
  END IF;
END
$$;

-- 2. Re-parent Defects whose parent is a Task → the Task's parent (User Story)
UPDATE work_item d
SET parent_id = (SELECT parent_id FROM work_item t WHERE t.id = d.parent_id)
WHERE d.type = 'defect'
  AND d.parent_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM work_item t WHERE t.id = d.parent_id AND t.type = 'task')
  AND (SELECT parent_id FROM work_item t WHERE t.id = d.parent_id) IS NOT NULL;

-- Log Defects that had a Task parent with no grandparent (orphaned Task)
DO $$
DECLARE
  orphaned_d_count INT;
BEGIN
  SELECT COUNT(*) INTO orphaned_d_count
  FROM work_item d
  WHERE d.type = 'defect'
    AND d.parent_id IS NOT NULL
    AND EXISTS (SELECT 1 FROM work_item t WHERE t.id = d.parent_id AND t.type = 'task')
    AND (SELECT parent_id FROM work_item t WHERE t.id = d.parent_id) IS NULL;

  IF orphaned_d_count > 0 THEN
    RAISE NOTICE '[0013] Found % Defect(s) with orphaned Task parent (no reparenting done)', orphaned_d_count;
  END IF;
END
$$;

-- 3. Re-parent Issues whose parent is a Test Case → the Test Case's parent (User Story)
UPDATE work_item i
SET parent_id = (SELECT parent_id FROM work_item tc WHERE tc.id = i.parent_id)
WHERE i.type = 'issue'
  AND i.parent_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM work_item tc WHERE tc.id = i.parent_id AND tc.type = 'test_case')
  AND (SELECT parent_id FROM work_item tc WHERE tc.id = i.parent_id) IS NOT NULL;

-- Log Issues that had a Test Case parent with no grandparent (orphaned Test Case)
DO $$
DECLARE
  orphaned_i_count INT;
BEGIN
  SELECT COUNT(*) INTO orphaned_i_count
  FROM work_item i
  WHERE i.type = 'issue'
    AND i.parent_id IS NOT NULL
    AND EXISTS (SELECT 1 FROM work_item tc WHERE tc.id = i.parent_id AND tc.type = 'test_case')
    AND (SELECT parent_id FROM work_item tc WHERE tc.id = i.parent_id) IS NULL;

  IF orphaned_i_count > 0 THEN
    RAISE NOTICE '[0013] Found % Issue(s) with orphaned Test Case parent (no reparenting done)', orphaned_i_count;
  END IF;
END
$$;

-- 4. Diagnostic: list any remaining violations against the new parent-child rules
-- New valid parent→children relations:
--   epic → feature
--   feature → user_story
--   user_story → {task, test_case, defect}
--   task → (no children allowed)
--   test_case → (no children allowed)
--   defect → (no children allowed)
--   issue → (no children allowed; can optionally have a parent: epic/feature/user_story)
--
-- This query identifies any row that violates these rules and is still in the DB.
DO $$
DECLARE
  violation_count INT;
BEGIN
  SELECT COUNT(*) INTO violation_count
  FROM work_item wi
  JOIN work_item p ON wi.parent_id = p.id
  WHERE NOT (
    (wi.type = 'feature' AND p.type = 'epic') OR
    (wi.type = 'user_story' AND p.type = 'feature') OR
    (wi.type = 'task' AND p.type = 'user_story') OR
    (wi.type = 'test_case' AND p.type = 'user_story') OR
    (wi.type = 'defect' AND p.type = 'user_story') OR
    (wi.type = 'issue' AND p.type IN ('epic', 'feature', 'user_story'))
  );

  IF violation_count > 0 THEN
    RAISE NOTICE '[0013] Found % remaining parent-child relation violation(s) — inspect manually', violation_count;
  ELSE
    RAISE NOTICE '[0013] Migration complete: no remaining parent-child relation violations detected';
  END IF;
END
$$;

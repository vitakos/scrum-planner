-- 0012_rename_test_run_type_to_issue.sql
-- Renames the "Test Run" work item type to "Issue" in the type catalog, so
-- the system's terminology matches the new model for customer-reported
-- production issues (AISC-47). Inserts the new 'issue' catalog row first
-- (FK targets require it to exist before any row can reference it),
-- repoints every workflow_definition, custom_field_definition and
-- work_item row from 'test_run' to 'issue', then removes the
-- now-unreferenced 'test_run' catalog row. All in one transaction via the
-- existing psql -1 migration runner (also covers AISC-54's data migration
-- in the same transaction).
--
-- Mirrors 0011_rename_bug_type_to_defect.sql.

INSERT INTO work_item_type_catalog (code, name, is_system, sort_order)
SELECT 'issue', 'Issue', is_system, sort_order
FROM work_item_type_catalog
WHERE code = 'test_run';

UPDATE workflow_definition
    SET work_item_type = 'issue'
    WHERE work_item_type = 'test_run';

UPDATE custom_field_definition
    SET work_item_type = 'issue'
    WHERE work_item_type = 'test_run';

UPDATE work_item
    SET type = 'issue'
    WHERE type = 'test_run';

DELETE FROM work_item_type_catalog WHERE code = 'test_run';

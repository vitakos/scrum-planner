-- 0011_rename_bug_type_to_defect.sql
-- Renames the "Bug" work item type to "Defect" in the type catalog, so the
-- system's terminology matches the new backlog model (AISC-51). Inserts the
-- new 'defect' catalog row first (FK targets require it to exist before any
-- row can reference it), repoints every workflow_definition,
-- custom_field_definition and work_item row from 'bug' to 'defect', then
-- removes the now-unreferenced 'bug' catalog row. All in one transaction via
-- the existing psql -1 migration runner (also covers AISC-53's data
-- migration in the same transaction).

INSERT INTO work_item_type_catalog (code, name, is_system, sort_order)
SELECT 'defect', 'Defect', is_system, sort_order
FROM work_item_type_catalog
WHERE code = 'bug';

UPDATE workflow_definition
    SET work_item_type = 'defect'
    WHERE work_item_type = 'bug';

UPDATE custom_field_definition
    SET work_item_type = 'defect'
    WHERE work_item_type = 'bug';

UPDATE work_item
    SET type = 'defect'
    WHERE type = 'bug';

DELETE FROM work_item_type_catalog WHERE code = 'bug';

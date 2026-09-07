-- 0010_project_repository_url.sql
-- Adds an optional repository URL to project, so a project's source
-- repository can be linked from its configuration (empty/absent is fine —
-- the field is nullable and validated only when a value is provided).

ALTER TABLE project
    ADD COLUMN repository_url VARCHAR(2048) NULL;

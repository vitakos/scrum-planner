-- 0002_flexible_work_item_types.sql
-- Replaces the fixed work_item_type Postgres enum with a work_item_type_catalog
-- table, so new artifact types can be added later by inserting a row instead of
-- an ALTER TYPE schema migration (this was the explicit design intent behind
-- the single polymorphic work_item table in docs/backlog-data-model.md).
-- Also relaxes workflow_state.category and work_item_link.link_type from
-- enums to checked TEXT columns for the same reason and to simplify the JPA
-- mapping in backend/core.

CREATE TABLE work_item_type_catalog (
    code        TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    is_system   BOOLEAN NOT NULL DEFAULT false,
    sort_order  INT NOT NULL DEFAULT 0
);

INSERT INTO work_item_type_catalog (code, name, is_system, sort_order) VALUES
    ('epic',       'Epic',       true, 10),
    ('feature',    'Feature',    true, 20),
    ('user_story', 'User Story', true, 30),
    ('task',       'Task',       true, 40),
    ('bug',        'Bug',        true, 50),
    ('test_case',  'Test Case',  true, 60),
    ('test_run',   'Test Run',   true, 70);

ALTER TABLE workflow_definition
    ALTER COLUMN work_item_type TYPE TEXT USING work_item_type::text;
ALTER TABLE workflow_definition
    ADD CONSTRAINT fk_workflow_definition_type
        FOREIGN KEY (work_item_type) REFERENCES work_item_type_catalog(code);

ALTER TABLE custom_field_definition
    ALTER COLUMN work_item_type TYPE TEXT USING work_item_type::text;
ALTER TABLE custom_field_definition
    ADD CONSTRAINT fk_custom_field_definition_type
        FOREIGN KEY (work_item_type) REFERENCES work_item_type_catalog(code);

ALTER TABLE work_item
    ALTER COLUMN type TYPE TEXT USING type::text;
ALTER TABLE work_item
    ADD CONSTRAINT fk_work_item_type
        FOREIGN KEY (type) REFERENCES work_item_type_catalog(code);

ALTER TABLE work_item_link
    ALTER COLUMN link_type TYPE TEXT USING link_type::text;
ALTER TABLE work_item_link
    ADD CONSTRAINT chk_work_item_link_type
        CHECK (link_type IN ('blocks', 'relates_to', 'duplicates', 'tests'));

DROP TYPE work_item_type;
DROP TYPE work_item_link_type;

ALTER TABLE workflow_state
    ALTER COLUMN category TYPE TEXT USING category::text;
ALTER TABLE workflow_state
    ADD CONSTRAINT chk_workflow_state_category
        CHECK (category IN ('to_do', 'in_progress', 'done'));

DROP TYPE workflow_state_category;

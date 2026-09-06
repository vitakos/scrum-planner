-- 0001_init_schema.sql
-- Initial relational schema for Scrum Planner.
-- Mirrors docs/backlog-data-model.md: work_item is a single polymorphic
-- table across all backlog artifact types, with workflow and custom-field
-- definitions kept relational and free-form content kept in MongoDB.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         TEXT NOT NULL UNIQUE,
    display_name  TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE project (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key           TEXT NOT NULL UNIQUE,
    name          TEXT NOT NULL,
    description   TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE project_member (
    project_id    UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role          TEXT NOT NULL,
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE sprint (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    name          TEXT NOT NULL,
    starts_on     DATE,
    ends_on       DATE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TYPE work_item_type AS ENUM (
    'epic', 'feature', 'user_story', 'task', 'bug', 'test_case', 'test_run'
);

CREATE TABLE workflow_definition (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    work_item_type  work_item_type NOT NULL,
    name            TEXT NOT NULL,
    UNIQUE (project_id, work_item_type)
);

CREATE TYPE workflow_state_category AS ENUM ('to_do', 'in_progress', 'done');

CREATE TABLE workflow_state (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id   UUID NOT NULL REFERENCES workflow_definition(id) ON DELETE CASCADE,
    name          TEXT NOT NULL,
    category      workflow_state_category NOT NULL,
    UNIQUE (workflow_id, name)
);

CREATE TABLE workflow_transition (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id     UUID NOT NULL REFERENCES workflow_definition(id) ON DELETE CASCADE,
    from_state_id   UUID NOT NULL REFERENCES workflow_state(id) ON DELETE CASCADE,
    to_state_id     UUID NOT NULL REFERENCES workflow_state(id) ON DELETE CASCADE,
    allowed_roles   TEXT[] NOT NULL DEFAULT '{}',
    UNIQUE (workflow_id, from_state_id, to_state_id)
);

CREATE TABLE work_item (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    type          work_item_type NOT NULL,
    title         TEXT NOT NULL,
    state_id      UUID NOT NULL REFERENCES workflow_state(id),
    parent_id     UUID REFERENCES work_item(id) ON DELETE SET NULL,
    assignee_id   UUID REFERENCES app_user(id),
    reporter_id   UUID REFERENCES app_user(id),
    sprint_id     UUID REFERENCES sprint(id),
    content_ref   TEXT, -- id of the matching document in MongoDB work_item_content
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_work_item_project ON work_item(project_id);
CREATE INDEX idx_work_item_parent  ON work_item(parent_id);
CREATE INDEX idx_work_item_sprint  ON work_item(sprint_id);

CREATE TYPE work_item_link_type AS ENUM ('blocks', 'relates_to', 'duplicates', 'tests');

CREATE TABLE work_item_link (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id     UUID NOT NULL REFERENCES work_item(id) ON DELETE CASCADE,
    target_id     UUID NOT NULL REFERENCES work_item(id) ON DELETE CASCADE,
    link_type     work_item_link_type NOT NULL,
    UNIQUE (source_id, target_id, link_type)
);

CREATE TABLE custom_field_definition (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    work_item_type  work_item_type NOT NULL,
    name            TEXT NOT NULL,
    data_type       TEXT NOT NULL,
    options         JSONB,
    UNIQUE (project_id, work_item_type, name)
);

CREATE TABLE work_item_state_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_item_id    UUID NOT NULL REFERENCES work_item(id) ON DELETE CASCADE,
    from_state_id   UUID REFERENCES workflow_state(id),
    to_state_id     UUID NOT NULL REFERENCES workflow_state(id),
    changed_by      UUID REFERENCES app_user(id),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_work_item_state_log_item ON work_item_state_log(work_item_id);

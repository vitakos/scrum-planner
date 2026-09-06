# Backlog Data Model

This document defines the initial data model for backlog artifacts (Epics, Features, User Stories, Tasks, Bugs, Test Cases, Test Runs) in Scrum Planner Assisted by AI.

## Storage split

Following the project's storage strategy (PostgreSQL for relational data, MongoDB for unstructured data):

- **PostgreSQL** holds the structural/relational core of every work item — enough to query, filter, join, and report on, regardless of type.
- **MongoDB** holds the flexible/free-form content — description body, custom field values, comments, attachments, and AI-assistant conversation/result data.

## Work item modeling: single polymorphic table

All backlog artifact types (Epic, Feature, User Story, Task, Bug, Test Case, Test Run) are modeled as a **single `work_item` table with a `type` discriminator**, rather than one table per type.

**Why:** it keeps cross-cutting features (workflows, custom fields, custom menu options, linking, sprints) uniform across all artifact types instead of duplicating them per table. Adding a new artifact type later (e.g. "Risk") means adding a new `type` value, not a new table + new workflow engine wiring + new custom-field wiring.

**Trade-off accepted:** type-specific validation (e.g. "a Test Run must reference a Test Case") lives in the application layer rather than in the database schema. This is standard for this pattern and is enforced via services/domain rules, not DB constraints.

### `work_item` (PostgreSQL)

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `project_id` | FK → project | |
| `type` | enum | Epic, Feature, UserStory, Task, Bug, TestCase, TestRun |
| `title` | text | |
| `state_id` | FK → workflow_state | Current state (see Workflow model) |
| `parent_id` | FK → work_item (nullable) | Hierarchy: Epic → Feature → User Story → Task/Bug; Test Case → Test Run |
| `assignee_id` | FK → user (nullable) | |
| `reporter_id` | FK → user (nullable) | |
| `sprint_id` | FK → sprint (nullable) | |
| `content_ref` | reference (Mongo doc id) | Points to the flexible-content document |
| `created_at` / `updated_at` | timestamp | |

### `work_item_link` (PostgreSQL)
Non-hierarchical relationships: `source_id`, `target_id`, `link_type` (blocks, relates_to, duplicates, tests).

## Custom fields: all in MongoDB

Custom field **values** are stored entirely in MongoDB, inside each work item's content document, as a free-form map (`{ field_name: value }`). This keeps the schema open — a new custom field on a project doesn't require a migration.

Custom field **definitions** (name, type, which work item type/project they apply to, validation rules) still live in PostgreSQL (`custom_field_definition`), since those need referential integrity (e.g. deleting a project should cascade its field definitions) and are queried relationally (e.g. "show me all custom fields configured for Bugs in Project X").

### `custom_field_definition` (PostgreSQL)
`id`, `project_id`, `work_item_type`, `name`, `data_type`, `options` (for select-type fields).

### Content document (MongoDB) — `work_item_content`
```json
{
  "_id": "<content_ref>",
  "work_item_id": "<uuid>",
  "description": "<rich text / markdown>",
  "custom_fields": {
    "risk_level": "High",
    "business_value": 8
  },
  "comments": [
    { "author_id": "...", "body": "...", "created_at": "..." }
  ],
  "attachments": [
    { "url": "...", "filename": "..." }
  ]
}
```

## Versioning: state history log (not full snapshots)

Full version snapshots were **not** chosen for the initial version — only the current state of each `work_item` plus a log of state transitions is kept. This is simpler to build and covers the primary need (status-transition metrics, audit of who moved what and when). Full restorable snapshots can be added later as a `work_item_history` table if the need arises (e.g. compliance requirements), without changing the core model.

### `work_item_state_log` (PostgreSQL)
`id`, `work_item_id`, `from_state_id`, `to_state_id`, `changed_by`, `changed_at`.

This table is also the source for the metrics already planned in the Features list: cycle time, lead time, cumulative flow, and the status-transition audit trail.

## Workflow model (PostgreSQL)

- `workflow_definition`: `id`, `project_id`, `work_item_type` — one workflow per artifact type per project.
- `workflow_state`: `id`, `workflow_id`, `name`, `category` (To Do / In Progress / Done).
- `workflow_transition`: `id`, `workflow_id`, `from_state_id`, `to_state_id`, `allowed_roles`.

`work_item.state_id` references `workflow_state`; a transition is only allowed if it exists in `workflow_transition` for that item's workflow.

## Open items for later
- Type-specific validation rules (e.g. required parent type per child type) — to be defined per artifact type as each is implemented.
- Whether `work_item_link` needs its own workflow/approval (e.g. approving a "blocks" relationship) — deferred.
- Full snapshot versioning — revisit if compliance/audit needs grow beyond the state-transition log.

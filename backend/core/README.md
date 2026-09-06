# backend/core

The core platform service: projects, work item types, and workflows for now
(see the root [README](../../README.md) for the overall architecture).

No authentication yet — this is a single-user local setup.

## Running

Requires the Postgres container to be up and its migrations applied first
(`database/migrate.js` from the repo root — see [`../../database/README.md`](../../database/README.md)),
since this app validates its JPA mappings against the existing schema on
startup rather than creating tables itself.

Via Docker (part of the full stack, see the root README):
```bash
cd ../../infra
docker compose up -d --build backend
```

For local dev without Docker (hot-reload friendly):
```bash
cd backend/core
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/scrum_planner
export SPRING_DATASOURCE_USERNAME=scrum_planner
export SPRING_DATASOURCE_PASSWORD=scrum_planner
mvn spring-boot:run
```
(adjust the datasource values to match `infra/.env` if you changed the defaults there)

## API

All endpoints are under `/api`. No pagination yet — fine while there's a
handful of projects.

| Method | Path | Description |
|---|---|---|
| GET | `/api/work-item-types` | Predefined + custom work item type catalog |
| GET | `/api/projects` | List projects |
| POST | `/api/projects` | Create a project — `{ key, name, description }`. Seeds a default (To Do → In Progress → Done) workflow for every system work item type. |
| GET | `/api/projects/{id}` | Get a project |
| GET | `/api/projects/{id}/workflows` | List each work item type's workflow (states + transitions) for a project |
| POST | `/api/projects/{id}/workflows/{workItemType}/states` | Add a state — `{ name, category }` (`category` is one of `to_do`, `in_progress`, `done`) |
| PUT | `/api/projects/{id}/workflows/{workItemType}/states/{stateId}` | Rename a state / change its category |
| DELETE | `/api/projects/{id}/workflows/{workItemType}/states/{stateId}` | Delete a state (rejected for the initial state, or one still used by a transition) |
| POST | `/api/projects/{id}/workflows/{workItemType}/transitions` | Add a transition — `{ name, fromStateId, toStateId }` (e.g. `{ "name": "Start", "fromStateId": ..., "toStateId": ... }`) |
| PUT | `/api/projects/{id}/workflows/{workItemType}/transitions/{transitionId}` | Rename a transition — `{ name }` |
| DELETE | `/api/projects/{id}/workflows/{workItemType}/transitions/{transitionId}` | Delete a transition |
| GET | `/api/projects/{id}/work-items?type=epic` | List work items (optionally filtered by type), ordered by creation |
| GET | `/api/projects/{id}/work-items/{workItemId}` | Get one work item |
| POST | `/api/projects/{id}/work-items` | Create a work item — `{ type, title, parentId?, content?, customFields? }`. Starts in its type's initial workflow state, gets a human-readable key (`{projectKey}-{n}`). `content` (Markdown) and `customFields` (`{ fieldName: value }`, must match this type's configured custom fields) are stored in MongoDB and linked via `content_ref` |
| PUT | `/api/projects/{id}/work-items/{workItemId}` | Update a work item — `{ title, parentId?, content?, customFields? }`. Omit `content`/`customFields` to leave them unchanged; pass a value (`""` / `{}`) to replace/clear them |
| POST | `/api/projects/{id}/work-items/{workItemId}/transitions` | Move a work item — `{ transitionId }` (must be a transition out of its current state; logged to the state-transition history) |
| DELETE | `/api/projects/{id}/work-items/{workItemId}` | Delete a work item |

A work item response includes `availableTransitions` (id/name/toStateId/toStateName) computed from its
current state, so the frontend doesn't need a separate lookup to know which moves are valid.

Errors are returned as JSON: `{ timestamp, status, error, message }`.

Not yet implemented (see the root README's roadmap): assignee/reporter, sprints, comments/attachments,
and custom fields. The `work_item` table already has the columns for assignee/reporter/sprint; `backend/core`'s
`WorkItem` entity just doesn't map them yet. Parent/child hierarchy and the MongoDB-backed content body
(`content` / `content_ref`) are now implemented — the latter via `WorkItemContentService`, storing a Markdown
string in the `work_item_content` collection (see `docs/backlog-data-model.md`).

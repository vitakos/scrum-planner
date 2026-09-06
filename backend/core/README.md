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
| POST | `/api/projects/{id}/workflows/{workItemType}/transitions` | Add a transition — `{ fromStateId, toStateId }` |
| DELETE | `/api/projects/{id}/workflows/{workItemType}/transitions/{transitionId}` | Delete a transition |

Errors are returned as JSON: `{ timestamp, status, error, message }`.

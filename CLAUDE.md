# Working with Claude on this project

Notes for AI assistants (and humans) working in this repo.

## UI / visual work

Claude has access to a browser tool ("Claude in Chrome" / the built-in browser) that can open the
running frontend, click around, and read the actual rendered page — not just the source code. When
working on frontend/UI tasks:

- Prefer starting the app locally (see "Running Locally" in the README) and having Claude open it
  in the browser to inspect layout, verify a change visually, or debug a UI bug against the real
  rendered output, instead of reasoning from JSX/CSS alone.
- Screenshots and page reads from the browser tool are a good way to sanity-check design/UX changes
  before opening a PR.
- The person can also watch/drive the same browser session live, so UI work can be done
  collaboratively in real time (see below).

## Pairing on design in real time

Yes — this works both ways:

- If Claude uses its **built-in browser** (a browser pane inside the Claude desktop app), the same
  pane is visible to the person in the app, and they can take over or point things out live while
  Claude is navigating the UI.
- If Claude uses **Claude in Chrome**, it drives the person's actual Chrome browser, so the person
  is looking at their own browser window the whole time and can literally watch each click/navigation
  happen, or take the mouse back at any point.

Either way, no separate screen-share is needed — pick whichever browser is already open and go.

## Development Environment

### Java

The project requires Java 22 for building and running the backend. It is installed at:

```
C:\InstalledSoftware\Java\jdk-22
```

Make sure this path is set in your `JAVA_HOME` environment variable when building with `./gradlew` or
running the backend via IDE/CLI.

## Accessing the backend REST API directly

The backend (`backend/core`, Spring Boot) is a REST API with **no authentication** for local dev. When
the stack is running (`docker compose up` in `infra/`, or `./gradlew bootRun` / your IDE), it's reachable at:

- `http://localhost:8080` when hit from the **host machine** (browser, `curl` from PowerShell/WSL, Claude's
  built-in browser via `Claude_Browser__navigate` / `Claude_Browser__javascript_tool` with `fetch(...)`).
  Port is configurable via `BACKEND_PORT` in `infra/.env` (defaults to 8080).
- `http://backend:8080` from **inside** another container on the `scrum-planner` compose network (e.g. the
  frontend container's nginx same-origin proxy — see `frontend/nginx.conf`).

### Using CURL to test the API

You can use **CURL** from PowerShell to interact with the REST API directly:

```powershell
# List all projects
curl http://localhost:8080/api/projects

# Fetch a work item by key
curl http://localhost:8080/api/work-items/by-key/AISC-13

# Create a work item
curl -X POST http://localhost:8080/api/projects/{projectId}/work-items `
  -Header "Content-Type: application/json" `
  -Body '{"type":"task","title":"...","parentId":"<uuid>"}'
```

This is useful for quick API testing and debugging without needing the browser UI.

Note for Claude specifically: a `device_bash` shell (the sandboxed Linux VM behind the remote-devices bridge)
does **not** share the host's network namespace, so `curl http://localhost:8080/...` from there will fail
with "Connection refused" even while the stack is up on the user's machine. Use the **built-in browser**
(`Claude_Browser__javascript_tool` with `fetch`) to reach the API instead — it runs on the actual host.

### Discovering what the API offers

There's no Swagger/OpenAPI exposed (no springdoc dependency). To see what's available, read the
`@RestController` classes directly — each maps 1:1 to a REST resource:

| Resource | Controller | Base path |
|---|---|---|
| Projects | `backend/core/.../project/ProjectController.java` | `/api/projects` |
| Work items (Epics, Features, User Stories, Tasks, Bugs, Test Cases, Test Runs) | `.../workitem/WorkItemController.java` | `/api/projects/{projectId}/work-items` |
| Work item lookup by human key (e.g. `AISC-13`) | `.../workitem/WorkItemLookupController.java` | `/api/work-items/by-key/{key}` |
| Work item type catalog | `.../worktype/WorkItemTypeController.java` | `/api/work-item-types` |
| Workflows (states/transitions per type) | `.../workflow/WorkflowController.java` | `/api/projects/{projectId}/workflows` |
| Custom field definitions | `.../customfield/CustomFieldController.java` | `/api/projects/{projectId}/custom-fields` |

All request/response shapes are plain DTOs (Java records) next to each controller, in a `dto/` subpackage —
read those instead of guessing a payload shape. Errors come back as JSON
(`{timestamp, status, error, message}`, see `common/ApiExceptionHandler.java`), with `message` holding the
first field-validation error when a request is rejected (HTTP 400).

Useful starting points:
- `GET /api/projects` — list projects (grab a project's `id`/`key`).
- `GET /api/work-items/by-key/AISC-13` — fetch a work item (Epic/Feature/User Story/Task/…) by its
  human-readable key without needing the project id first. The response includes `parentId`/`parentKey`
  and `childCount`, useful for walking the backlog hierarchy from a single story.
- `POST /api/projects/{projectId}/work-items` with `{"type":"task","title":"...","parentId":"<uuid>"}` —
  create a child task under any work item (e.g. to track an implementation plan's subtasks under a user
  story). `type` must be one of the codes from `GET /api/work-item-types` (`epic`, `feature`, `user_story`,
  `task`, `bug`, `test_case`, `test_run`).

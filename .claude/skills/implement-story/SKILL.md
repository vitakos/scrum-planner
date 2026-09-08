---
name: implement-story
model: claude-sonnet-5
description: Implement a Scrum Planner User Story that is already Planned — reads the Story's child Tasks and implements them one by one against the local working tree, driving the Story through Ready for Develop -> In Progress -> In Code Review. Never commits; hands the developer a one-line commit message for manual review/merge. Use when asked to implement, build, or work a User Story by its key (e.g. "implement AISC-51", "run implement-story on SPAI-42").
---

# implement-story

Implements a single User Story in the Scrum Planner app by working its child Tasks one at a time
against the repo's current branch, and drives the Story through its workflow states as work
progresses. This skill only talks to the Scrum Planner backend REST API for state/task management;
all code changes are made directly in the local working tree.

**This skill never runs `git commit`, `git push`, or any other commit/merge command.** All changes
stay uncommitted on the current branch. The developer reviews the diff and merges manually — the
skill's only job at the end is to hand back a one-line suggested commit message.

## Input

The skill is invoked with a work item **key** (e.g. `AISC-51`).

## 0. Reaching the API and the repo

Same as `story-planner`:

- Read `CLAUDE.md` at the repo root for the current base URL/port (default `http://localhost:8080`,
  no auth in local dev).
- **Important:** call the API from the **built-in browser** (`Claude_Browser__javascript_tool` with
  `fetch(...)`), not from a `device_bash` shell — `device_bash` doesn't share the host's network
  namespace, so `curl`/`fetch` to `localhost:8080` from there fails with connection refused even
  while the stack is running.
- If the stack isn't running, ask the user to start it before continuing.
- All code edits happen via `device_bash` in the connected repo folder, on whatever branch is
  currently checked out. Do not create, switch, or touch branches unless the user explicitly asks.

## 1. Resolve & validate the Story

1. `GET /api/work-items/by-key/{key}` — resolves the item and gives you `projectId`.
2. Validate `type == "user_story"`. If not, stop and tell the user the key isn't a User Story
   (report its actual `typeName`).
3. Validate `stateName == "Ready for Develop"`. If the Story is earlier in the flow (e.g. still
   `Planning`/`Planned`), tell the user to run `story-planner` on it first and stop. If it's already
   past this point (`In Progress`, `In Code Review`, ...), report the current state and ask the user
   how to proceed rather than assuming — don't silently restart or redo finished work.

## 2. Move to `In Progress`

Find the transition in `availableTransitions` with `toStateName == "In Progress"` (case-insensitive)
and `POST /api/projects/{projectId}/work-items/{storyId}/transitions` with its id. If no such
transition exists from the current state, stop and tell the user — do not invent workflow states or
transitions.

## 3. Load the child Tasks

`GET /api/projects/{projectId}/work-items?type=task`, filter client-side to items whose `parentId`
equals the Story's id. If there are no child Tasks, stop and tell the user — there's nothing to
implement (the Story likely needs re-planning).

Work the Tasks **one at a time**, in a sensible order (respect obvious sequencing — e.g. a schema
migration before the code that depends on it — rather than list order blindly). For each Task:

1. Read the Task's `title` and `content` for what it asks for.
2. Implement it in the local working tree via `device_bash`: edit/create the files it describes,
   following the repo's existing conventions (read neighboring code first — don't guess a pattern
   the codebase doesn't use). Prefer small, focused changes that do only what the Task describes;
   larger judgment calls the Task didn't anticipate are worth a one-line note to the user at the end
   rather than silently expanding scope.
3. If the Task is a test-writing task, run it locally (existing test command for that
   module — e.g. `./gradlew test` for backend, the frontend's test script for frontend) via
   `device_bash` and fix failures before moving on. If the project has no runnable test setup for
   that layer yet (see `story-planner`'s notes on this), say so rather than fabricating a run.
4. Once the Task's implementation is in place (and passing tests where applicable), transition it to
   Done: find the transition on that Task with `toStateName == "Done"` (case-insensitive) and
   `POST /api/projects/{projectId}/work-items/{taskId}/transitions` with its id.
5. If a Task turns out to be unimplementable as written (missing information, conflicts with an
   earlier Task's outcome, etc.), stop, explain the blocker to the user, and wait — don't guess past
   a genuine blocker or mark it Done anyway.

## 4. Move the Story to `In Code Review`

Once every child Task is Done, find the transition on the Story with `toStateName == "In Code
Review"` (case-insensitive) and apply it. If that transition isn't available from the Story's
current state, stop and tell the user rather than forcing a workaround.

## 5. Hand back to the user

Reply to the user summarizing what was implemented (per Task, one line each) and ask them to review
the changes on their current branch. Include a single suggested commit message (one line, imperative
mood, referencing the Story key, e.g. `AISC-51: rename Bug type to Defect in backend catalog`) —
nothing more elaborate, and don't write it to a file or run `git commit` with it. Make clear that
committing, approving, and merging is the developer's own manual step.

## Notes

- Always resolve transition ids dynamically from `availableTransitions` — target state names
  (`In Progress`, `Done`, `In Code Review`, ...) are project configuration, not constants.
- Never run `git commit`, `git push`, `git merge`, or any command that changes branch/history state.
  Read-only git commands (`git status`, `git diff`) are fine to sanity-check your own work.
- Never create, switch, rename, or delete branches.
- If a required target state or transition is missing from the project's workflow, stop and tell the
  user rather than working around it.
- Full endpoint/DTO reference: `backend/core/.../workitem/`, `.../workflow/` — read the controller +
  `dto/` records directly when a payload shape is unclear (no Swagger/OpenAPI is exposed).

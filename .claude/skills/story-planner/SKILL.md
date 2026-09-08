---
name: story-planner
model: claude-sonnet-5
description: Plan the implementation of a Scrum Planner User Story — reads the Story, its Feature and Epic, checks dependencies, produces a Gap Analysis and solution plan, and drives the Story through the Planning workflow (Ready for Plan & Estimate -> Planning -> Planned -> Ready for Develop / In Refinement). Use when asked to plan, estimate-prep, or architect a User Story by its key (e.g. "plan SPAI-42", "run story-planner on AISC-13").
---

# story-planner

Plans the implementation of a single User Story in the Scrum Planner app: reads the Story plus its
parent Feature and Epic, resolves and checks dependencies, produces a Gap Analysis and an
implementation plan (optionally including an architecture proposal), and drives the Story through
its workflow states as the plan is drafted, returned to the user, and approved or revised.

This skill only talks to the Scrum Planner backend REST API. It never edits the database directly
and never invents workflow state or transition ids — every state change is done by finding a real
transition on the work item and posting its id.

## Input

The skill is invoked with a work item **key** (e.g. `SPAI-42`). If the project or API base URL is
ambiguous, ask the user; otherwise resolve the project from the key lookup response (see below).

## 0. Reaching the API

Read `CLAUDE.md` at the repo root first — it documents the current base URL, port and the fact that
there is no auth in local dev. As of this writing:

- Base URL: `http://localhost:8080` (host machine) — configurable via `BACKEND_PORT`.
- **Important:** a `device_bash` shell does not share the host's network namespace, so `curl
  http://localhost:8080/...` from there fails with "connection refused" even while the stack is
  running. Call the API from the **built-in browser** instead, e.g.
  `Claude_Browser__javascript_tool` running `fetch('http://localhost:8080/api/...')`, or from
  Claude in Chrome if that's the active browser for the session.
- If the stack isn't running, ask the user to start it (`docker compose up` in `infra/`, or
  `./gradlew bootRun`) before continuing.

All endpoints below are relative to the base URL. No auth headers are needed in local dev.

## 1. Resolve & validate the User Story

1. `GET /api/work-items/by-key/{key}` — resolves the item regardless of project, and gives you
   `projectId` for every subsequent call.
2. Validate `type == "user_story"`. If not, stop and tell the user the key does not point to a
   User Story (report its actual `typeName`) — do not proceed.
3. Validate `stateName == "Ready for Plan & Estimate"`. If not, stop and report the current state —
   do not force a transition from an unexpected state.

If either check fails, this is the end of the run; report clearly and wait for the user.

## 2. Move to `Planning`

Look for a transition to `Planning` in the Story's `availableTransitions`
(`toStateName == "Planning"`, case-insensitive). If found:

`POST /api/projects/{projectId}/work-items/{storyId}/transitions` with `{"transitionId": "<id>"}`.

If no such transition exists from the current state, the project's workflow for User Story is not
configured with a `Planning` state reachable from `Ready for Plan & Estimate`. Stop and tell the
user — do not add states/transitions yourself unless they explicitly ask you to fix the workflow.

From here on the Story is "in planning"; all following analysis happens before you return the plan.

## 3. Gather context

1. **Feature**: use the Story's `parentId`/`parentKey` to fetch it (`GET
   /api/projects/{projectId}/work-items/{parentId}` or the by-key lookup). Confirm its type is
   `feature`.
2. **Epic**: repeat one level up from the Feature's `parentId`. Confirm its type is `epic`.
3. Read `content` and `customFields` on all three items — the Story's acceptance criteria and the
   Feature/Epic's goals and constraints are the primary planning input.
4. If the Story is missing a Feature parent, or the Feature is missing an Epic parent, note that as
   a gap (see §5) rather than failing outright — some backlogs are shallow on purpose.

## 4. Dependency check

Scrum Planner has no built-in "depends on" relation (only parent/child). Dependencies are tracked
via a custom field, by convention named `Dependencies` (case-insensitive), holding one or more work
item keys.

1. `GET /api/projects/{projectId}/custom-fields` (or the `user_story`/`feature`-scoped field
   catalog) to find a custom field named like `Dependencies` for each type. If none exists, note
   that dependency tracking isn't configured for this project and skip to §5 — do not guess.
2. Read that field's value from the Story's `customFields` (its own dependencies) and from the
   Feature's `customFields` (feature-level dependencies).
3. For each referenced key, `GET /api/work-items/by-key/{key}` to get its current `stateName` and
   `type`.
4. To decide whether a dependency is "at least `Deployed to UAT`", don't compare state names as
   strings — fetch that dependency's own workflow: `GET
   /api/projects/{projectId}/workflows`, find the entry for its `type`, locate the state named
   `Deployed to UAT` (case-insensitive) and compare `sortOrder`:
   - dependency's current state `sortOrder` >= `Deployed to UAT` state's `sortOrder` → satisfied.
   - otherwise → open/incomplete dependency.
   - if that type's workflow has no `Deployed to UAT` state at all, flag it as unverifiable rather
     than silently treating it as satisfied.
5. Open dependencies do **not** by themselves send the Story back to `In Refinement`. Instead:
   - List them explicitly as **blockers/risks** in the Gap Analysis and in the plan you return.
   - Use them to shape the plan (e.g. sequence tasks so blocked work isn't scheduled first, add an
     explicit "depends on X" note or a coordination task).
   - Only escalate to `In Refinement` if an open dependency makes the Story genuinely unplannable
     (see §5.3) — e.g. the Story can't be scoped without knowing an interface that the blocking item
     hasn't defined yet.

## 5. Gap Analysis & solution plan

Using the Story, Feature, Epic, and dependency findings, produce:

1. **Gap Analysis** — what already exists in the codebase vs. what the Story requires. Read the
   relevant backend/frontend modules (see `CLAUDE.md` and `docs/backlog-data-model.md` for the data
   model) to ground this in the real code, not assumptions.
2. **Solution plan** — a short architecture proposal when the Story touches new components,
   endpoints, schema, or cross-module contracts; otherwise a straightforward implementation
   approach. Call out affected repos/modules (`backend/core`, `frontend`, `database`, `infra`).
3. **Task breakdown** — a list of concrete, independently workable tasks that together implement the
   Story, each with a one-line description. This list becomes the child Tasks in §6 once approved.
   Tasks must be **implementation work only**: code/config/DB changes, new functionality, migrations,
   and unit tests where warranted. Never include tasks that are manual developer actions instead of
   implementation — no "Manual validation of...", "Manual review of...", "Manually test...", "QA the
   feature", etc. The workflow already has dedicated stages for testing and test automation later on;
   this breakdown is not where those happen.
4. **Open questions** — anything you could not resolve from the Story/Feature/Epic content or from
   the code. If the requirements conflict with each other, or with what the user is asking for in
   this conversation, or there are unresolved questions blocking a confident plan, **do not** return
   a plan: move the Story back to `In Refinement` (find that transition the same way as §2) and
   explain why to the user. This is the one path that skips §6 entirely.

## 6. Return the plan — move to `Planned`

If the plan is complete (no blocking open questions), transition the Story to `Planned` (again: find
the real transition by `toStateName`, don't hardcode an id) and present the Gap Analysis, solution
plan, task breakdown, and any dependency risks to the user for review.

## 7. User decision loop

- **Approved**: for each task in the breakdown, `POST /api/projects/{projectId}/work-items` with
  `{"type": "task", "title": "...", "parentId": "<storyId>", "content": "..."}`. Once all Task
  children are created, transition the Story to `Ready for Develop`.
- **Changes requested**: transition the Story back to `Planning`, revise the plan against the user's
  comments (repeat §3–§5 as needed against the new feedback), and return to §6. Repeat until
  approved.
- **Conflicting/unresolved requirements** surface at any point in this loop (not just first pass):
  transition to `In Refinement` and stop, explaining the conflict clearly.

Never create Task children before the user has explicitly approved the plan.

## Notes

- Task breakdown = implementation only. Never propose a task whose action is manual developer
  verification, review, or testing (e.g. "Manual validation of X", "Manually verify Y") — the
  workflow already has dedicated testing/test-automation stages downstream. Unit test
  implementation tasks are fine when warranted; manual QA tasks are not.
- Always resolve transition ids dynamically from `availableTransitions` / the workflow endpoint —
  target state names (`Planning`, `Planned`, `Ready for Develop`, `In Refinement`,
  `Ready for Plan & Estimate`, `Deployed to UAT`) are project configuration, not constants.
- If a required target state or transition is missing from the project's workflow, stop and tell the
  user rather than working around it.
- Full endpoint/DTO reference: `backend/core/.../workitem/`, `.../workflow/`, `.../customfield/` —
  read the controller + `dto/` records directly when a payload shape is unclear (no Swagger/OpenAPI
  is exposed).

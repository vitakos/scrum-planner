---
name: epic-feature-planner
model: claude-sonnet-5
description: Plan a Scrum Planner Epic or Feature that is sitting in "Intake" — assesses whether the Epic, its Features, and (for a Feature) its parent Epic carry enough clear information to plan from, runs a Gap Analysis at the Feature level (proposing new draft Features when coverage is incomplete), drafts the needed User Stories (title + "As ... I ..." only, no Acceptance Criteria yet), and drives the Epic/Features through the workflow (Intake -> Plan -> Stories Prep) once the user approves the plan. Use when asked to plan an Epic or Feature by its key (e.g. "plan EPIC-7", "run epic-feature-planner on FEAT-21", "prepare stories for this epic").
---

# epic-feature-planner

Plans a Scrum Planner **Epic** or **Feature** that is still in **Intake**: checks that the
information available is clear enough to plan from, works out via a Gap Analysis which Features are
needed to deliver the Epic, and drafts the User Stories each Feature will need — without writing
Acceptance Criteria yet, since Stories get refined in a later stage (see `story-planner`, which picks
up from `Ready for Plan & Estimate` onward). This skill's job ends once Features carry a solid,
detailed description and their draft-titled User Stories exist in `Stories Prep`.

This skill only talks to the Scrum Planner backend REST API. It never edits the database directly and
never invents workflow state or transition ids — every state change is done by finding a real
transition on the work item and posting its id.

## Input

The skill is invoked with a work item **key** (e.g. `EPIC-7`, `FEAT-21`). If the project or API base
URL is ambiguous, ask the user; otherwise resolve the project from the key lookup response.

## 0. Reaching the API

Read `CLAUDE.md` at the repo root first — it documents the current base URL, port, and the fact that
there is no auth in local dev. As of this writing:

- Base URL: `http://localhost:8080` (host machine) — configurable via `BACKEND_PORT`.
- **Important:** a `device_bash` shell does not share the host's network namespace, so `curl
  http://localhost:8080/...` from there fails with "connection refused" even while the stack is
  running. Call the API from the **built-in browser** instead, e.g.
  `Claude_Browser__javascript_tool` running `fetch('http://localhost:8080/api/...')`, or from Claude
  in Chrome if that's the active browser for the session.
- If the stack isn't running, ask the user to start it (`docker compose up` in `infra/`, or
  `./gradlew bootRun`) before continuing.

All endpoints below are relative to the base URL. No auth headers are needed in local dev.

## 1. Resolve & validate the item

1. `GET /api/work-items/by-key/{key}` — resolves the item regardless of project and gives you
   `projectId` for every subsequent call.
2. Validate `type` is `epic` or `feature`. If it's anything else, stop and tell the user the key does
   not point to an Epic or a Feature (report its actual `typeName`) — do not proceed.
3. Validate `stateName == "Intake"`. If not, stop and report the current state — do not force a
   transition from an unexpected state, and do not try to "catch it up" through intermediate states.
4. **If the item is a Feature**, also resolve its parent Epic via `parentId`/`parentKey`
   (`GET /api/projects/{projectId}/work-items/{parentId}`). Confirm its type is `epic`. A Feature
   with no Epic parent is itself a gap — note it in §3 rather than failing outright.

If any check in steps 2–3 fails, this is the end of the run; report clearly and wait for the user.

## 2. Determine scope

- **Feature key given**: scope is that single Feature (plus its Epic for context — read-only, the
  Epic itself is not re-validated or moved unless it also happens to need a state fix per §4).
- **Epic key given**: scope is the Epic plus **every child Feature that is not already in
  `Stories Prep`**.
  1. `GET /api/projects/{projectId}/work-items?type=feature` and keep the ones whose `parentId`
     equals the Epic's id (the list endpoint filters by type only, not by parent).
  2. Features already in `Stories Prep` are done — leave them alone, don't re-plan or re-open them.
  3. Every other child Feature (including ones already in `Plan`, or anywhere except `Stories Prep`)
     is in scope for this run, regardless of whether it started in `Intake` — the Epic-level run is
     what re-validates and moves the whole set together. Note each Feature's starting state so §4
     only transitions the ones that actually need it.
  4. If the Epic has no child Features yet, that's not a stopping condition — proceed to §3, and let
     the Gap Analysis in §5 be the source of the Feature set instead.

## 3. Information assessment

Before planning anything, read the Epic's and every in-scope Feature's `content` and `customFields`
(and, for a single-Feature run, the parent Epic's too) and judge whether they are clear and complete
enough to serve as real planning input — a goal that's actually a goal, constraints that are
concrete, scope that doesn't contradict a sibling Feature or the Epic, etc. This is a judgment call,
not a checklist: vague filler ("improve performance"), missing actors, or content that contradicts
the Epic's stated goal are the kind of thing that should stop the run for that item.

- If everything is clear: proceed to §4.
- If something is unclear or contradictory: for **each Feature whose content raised a doubt**,
  transition it back to `Intake` (find the transition the same way as always — by `toStateName`,
  case-insensitive — and skip Features already in `Intake`, there's nothing to transition), and
  write the concern into its `content` (via `PUT .../work-items/{id}`, preserving the rest of the
  content and appending a clearly marked "Planning feedback" note — never overwrite what's already
  there) explaining precisely what's missing or contradictory and what's needed to move forward.
  - If the doubt is about the **Epic itself** rather than a specific Feature, do not transition the
    Epic (it has no equivalent "send it back" target here — Epic-level ambiguity is reported
    directly to the user instead) — surface it clearly in your report.
  - Do not proceed to §4 for any Feature you just sent back to `Intake`; other Features in scope
    that were clear can still continue.
  - If *every* in-scope Feature gets sent back, stop here, report the reasons, and wait for the user
    — there's nothing left to plan this run.

## 4. Move to `Plan`

For the Epic and for each Feature that passed §3 (i.e. wasn't sent back to `Intake`) and isn't
already in `Plan` or beyond: find a transition to `Plan` in `availableTransitions`
(`toStateName == "Plan"`, case-insensitive) and `POST .../work-items/{id}/transitions` with
`{"transitionId": "<id>"}`. If the item is already in `Plan` (an Epic-scoped run picking up a Feature
that was left mid-flight), leave it — don't re-transition. If no such transition exists from the
current state, the project's workflow isn't configured with a `Plan` state reachable from `Intake`;
stop and tell the user rather than working around it.

## 5. Gap Analysis (Feature-level)

With the Epic's goal and the in-scope Features' content in hand:

1. List the functionalities the Epic needs to deliver, and map each one to an existing Feature (or
   note that none covers it).
2. For any needed functionality with no covering Feature, propose a **new draft Feature** as part of
   the plan you present in §7 — title, a detailed description (goal, scope, constraints: enough for
   someone to run this same skill against it or refine Stories from it later), and which Epic it
   belongs to. Do not create it in the system yet; it's part of the proposal.
3. Conversely, flag any in-scope Feature that doesn't map to anything in the Epic's stated goal — it
   may be out of scope, duplicated, or a sign the Epic's own description is incomplete. Surface this
   as a question rather than silently dropping or keeping the Feature.
4. This analysis, and the reasoning behind any proposed new Feature, becomes part of what you present
   for approval — don't create work items from it silently.

## 6. Draft the User Stories

For each in-scope Feature (existing ones that passed §3, plus any new ones proposed in §5 — treat a
proposed new Feature as if it will be approved and plan its Stories in the same pass so the whole set
comes back to the user together):

- Identify the User Stories needed to deliver that Feature. Each Story needs only:
  - A short title.
  - A one-line definition in the form **"As a `<user/system/role>`, I `<want/need to>` ...`"** —
    pick whichever subject is actually right for that Story, not always "user".
  - No Acceptance Criteria yet — that's `story-planner`'s job once the Story reaches
    `Ready for Plan & Estimate`. Don't over-invest in wording here; the goal is a complete,
    correctly-scoped *list*, not polished Stories.
- Alongside this, make sure the Feature's own `content` is or becomes detailed enough to guide that
  later refinement (goal, scope boundaries, key constraints, how it fits the Epic) — a thin Feature
  description with a long Story list under it is a gap in the Feature, not in the Stories. Draft the
  improved Feature content now; it gets written in §8, not before approval.

## 7. Present the plan for approval

Present, for the whole scope of this run:

- The information-assessment outcome (§3): what passed, what was sent back to `Intake` and why.
- The Gap Analysis (§5): functionality-to-Feature mapping, any proposed new draft Features with their
  full description, and any flagged out-of-scope Features.
- For every existing or proposed Feature that's part of the plan: its (possibly revised) detailed
  description, and its list of draft User Stories (title + "As ... I ..." line each).

Nothing beyond the §3/§4 state transitions already made is written to the system yet. Wait for the
user's decision.

## 8. User decision loop

- **Approved**: for each Feature in the approved plan —
  1. If its content changed during drafting, `PUT /api/projects/{projectId}/work-items/{id}` with the
     full updated `title`/`parentId`/`content`/`customFields` (this endpoint replaces the item, so
     always send back the fields you're keeping, not just the one you changed).
  2. If it's a newly proposed Feature, create it first: `POST /api/projects/{projectId}/work-items`
     with `{"type": "feature", "title": "...", "parentId": "<epicId>", "content": "..."}`.
  3. For each drafted User Story, `POST /api/projects/{projectId}/work-items` with
     `{"type": "user_story", "title": "...", "parentId": "<featureId>", "content": "As ... I ..."}`.
  4. Once all its Stories exist, transition that Feature to `Stories Prep` (find the transition by
     `toStateName`, don't hardcode an id).
  5. Leave the Epic in `Plan` — this skill doesn't move the Epic past `Plan`; a later stage (or the
     user) decides when the whole Epic is done.
- **Changes requested**: revise the plan against the user's comments (repeat §5–§6 as needed) and
  return to §7. Repeat until approved. Items already moved to `Plan` in §4 stay there while you
  iterate — don't bounce them back and forth.
- **New doubts surface during review**: handle exactly like §3 — send the affected Feature(s) back to
  `Intake` with the reason, drop them from the approved set, and continue with the rest.

Never create Feature or User Story children, and never transition a Feature to `Stories Prep`, before
the user has explicitly approved that part of the plan.

## Notes

- Always resolve transition ids dynamically from `availableTransitions` / the workflow endpoint
  (`GET /api/projects/{projectId}/workflows`) — target state names (`Intake`, `Plan`, `Stories Prep`)
  are project configuration, not constants. If a required target state or transition is missing from
  the project's workflow, stop and tell the user rather than working around it.
- Epic and Feature each have their own workflow (one workflow per work item type per project, see
  `docs/backlog-data-model.md`) — don't assume the Epic's states/transitions apply to Features or
  vice versa.
- User Stories drafted here intentionally have no Acceptance Criteria — don't add any, even if you
  can infer them; that's `story-planner`'s responsibility later.
- The list endpoint (`GET /api/projects/{projectId}/work-items?type=...`) filters by type only, not
  by parent — always filter by `parentId` client-side when you need "children of X".
- Full endpoint/DTO reference: `backend/core/.../workitem/`, `.../workflow/`, `.../customfield/` —
  read the controller + `dto/` records directly when a payload shape is unclear (no Swagger/OpenAPI
  is exposed).

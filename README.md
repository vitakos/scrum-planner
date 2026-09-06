# Scrum Planner Assisted by AI

This is an application to assist project planning and delivery using the Scrum agile methodology, combining classic backlog/metrics management with AI-assisted tools that help plan, design, and implement the work — from intake through delivery.

## Table of Contents
- [Features](#features)
- [AI-based Assistants](#ai-based-assistants)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

## Features

### Project & Team Management
- Create Projects, binding their source repositories and documentation (e.g. wiki integration).
- Manage Teams: users, roles, and permissions per project.
- Multi-project / multi-team support (portfolio-level visibility).

### Backlog & Artifact Management
- Create/Update Epics, Features, User Stories, Tasks, Bugs, Test Cases, and Test Run artifacts, handling their states and versioning.
- Define customized workflows (states and transitions) for each artifact type.
- Add custom fields to each artifact type.
- Add custom menu/action options to each artifact type.
- Backlog grooming and prioritization (ranking, story points/estimation, dependencies between artifacts).
- Traceability between artifacts (Epic → Feature → User Story → Task/Bug → Test Case → Test Run).

### Scrum Ceremonies & Metrics
- Store information about Scrum ceremonies (Sprint Planning, Daily Standup, Review, Retrospective).
- Sprint management (sprint creation, capacity planning, sprint backlog).
- Metrics and reporting: burndown/burnup charts, velocity, cycle time, lead time, cumulative flow, status-transition history/audit trail.

### Intake & Planning
- Handle Intake Change Requests through AI-based Assistants.
- Epic & Feature planning support, breaking down intake into actionable backlog items.
- Architecture review and analysis assistance for proposed features.
- Documentation generation and management (architecture decisions/ADRs, feature specs, design docs), with wiki integration.

### AI Pipeline Integration
- Integration to AI Pipelines to extend functionality via configurable, provider-agnostic AI Assistants.
- Each project — and even each individual AI pipeline — can be connected to its own LLM provider/model.
- Repository integration for AI-driven implementation (branch/PR creation, code review).
- CI/CD hook points for AI-generated code and automated test execution.

### Proposed Additional Features
- **Requirements/Definition of Ready & Definition of Done checks** — configurable checklists enforced per artifact type before transitions.
- **Risk & dependency management** — flag cross-team/cross-project dependencies and risks surfaced during planning.
- **Notifications & integrations** — Slack/Teams/email notifications; integrations with Git providers (GitHub/GitLab/Bitbucket), CI systems, and issue trackers for migration/sync.
- **Audit log & permissions model** — fine-grained RBAC and full audit trail of who changed what, when (including AI-assistant actions).
- **AI usage governance** — track cost/tokens per assistant/provider/project, guardrails and approval gates before AI-generated code/PRs merge.
- **Knowledge base / RAG over project docs** — let assistants ground answers in the project's own docs, ADRs, and codebase.
- **Release management** — release notes generation (AI-assisted from completed stories), versioning, changelogs.
- **Dashboards** — configurable dashboards per role (PM, Scrum Master, Developer, Stakeholder).
- **Import/Export & migration tools** — import backlogs from Jira/Azure DevOps/etc.

## AI-based Assistants
- **Epic & Features Planner** — breaks down Intake Change Requests into Epics and Features.
- **User Story Assistant** — helps write/update user stories (e.g. INVEST criteria, acceptance criteria).
- **Task Planner** — helps plan tasks needed to complete a user story.
- **Architecture Assistant** — helps propose/define architecture for features and review architectural impact.
- **Documentation Assistant** — helps draft/maintain architecture decisions, feature docs, and wiki content.
- **User Story Implementor** — AI-based developer pipeline that implements user stories/tasks, creating a PR.
- **Code Review Assistant** — receives a PR and reviews it against what was planned for the user story/task.
- **Test Case Generator** — helps write test cases based on a user story.
- **Test Case Automator** — helps automate E2E test cases.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21 (LTS), Spring Boot |
| Relational Database | PostgreSQL (relational data: projects, teams, workflows, users/roles) |
| Document Database | MongoDB (unstructured/semi-structured data: artifact content, AI conversation logs, generated docs) |
| Frontend | ReactJS |
| AI/LLM Layer | Provider-agnostic — configurable to use OpenAI, Anthropic (Claude), AWS Bedrock, Google Gemini, and others |

## Architecture Overview

**For the initial version, the whole system lives in a single repository (monorepo)** to move faster while there's not yet a running system to split apart. It's organized internally by clear module boundaries so it can be extracted into separate repositories/services later without a major rewrite:

- A **core platform module** owns projects, backlog, workflows, custom fields, ceremonies/metrics, and team/role management.
- An **AI Assistants module** contains all the built-in assistants (they ship with the application). **Custom/organization-specific assistants are added through a plugin/extension mechanism**, kept isolated from the core assistants module.
- An **AI Gateway module** hosts the provider-agnostic LLM adapters (OpenAI, Claude, Bedrock, Gemini, ...) and resolves *which* provider/model/credentials to use **per project and per pipeline** — so different projects, or even different pipelines within the same project, can be wired to different LLMs.
- Communication between the core platform, AI Gateway, and Assistants is **hybrid**: quick, synchronous assistants (e.g. User Story Assistant, Task Planner) are called via **REST** (in-process calls for now, since everything runs in one deployable), while pipeline-shaped assistants that run longer or are triggered by external events (e.g. User Story Implementor, Code Review Assistant, Test Case Automator) run **asynchronously** via internal events — modeled the same way they would be over a message broker, so moving to Kafka/RabbitMQ later is a swap of the event transport, not a redesign.

> As the system matures, the plan is to extract these modules (AI Gateway, AI Assistants, frontend) into their own repositories/services, following the module boundaries established here.

## Project Structure

```
scrum-planner/
├── backend/
│   ├── core/                 # Projects, teams, users, roles, backlog, workflows, custom fields, ceremonies/metrics
│   ├── ai-gateway/           # Provider-agnostic LLM adapters (OpenAI, Claude, Bedrock, Gemini) + per-project/per-pipeline routing
│   ├── ai-assistants/        # Built-in assistants (sync ones as REST endpoints, pipeline-shaped ones as async event handlers)
│   ├── plugin-sdk/           # Contract/SDK for custom assistants added as plugins/extensions
│   ├── shared-contracts/     # Shared DTOs/event schemas used across core, ai-gateway, ai-assistants, plugins
│   ├── integration/          # Git providers, wiki, CI/CD, notifications
│   └── config/
├── frontend/
│   └── src/ (components/, pages/, features/, services/)
├── infra/                    # Docker Compose for local dev (docker-compose.yml, .env.example), IaC, CI/CD templates
├── database/                  # DB scripts and migration tooling — see database/README.md
│   ├── migrate.js             # applies pending SQL/Mongo migrations, tracked in upgrade.json
│   ├── sql/migrations/        # PostgreSQL schema migrations (*.sql) + upgrade.json
│   └── mongo/migrations/      # MongoDB migrations (*.js) + upgrade.json
└── docs/                     # Architecture decisions, feature specs
```

> TODO: confirm final module/package naming.

## Getting Started

### Prerequisites
- Java 21 (LTS)
- Node.js (for the frontend, and for `database/migrate.js`)
- Docker & Docker Compose (Docker Desktop with the WSL2 backend, on Windows)
> TODO: confirm Node.js version, PostgreSQL/MongoDB versions, and whether an internal message broker is introduced now or deferred until the AI Assistants module is extracted into its own service.

### Installation
```bash
git clone <repo-url>
cd scrum-planner

# Backend
cd backend
./mvnw clean install

# Frontend
cd ../frontend
npm install
```

### Running Locally

**1. Data storage (PostgreSQL + MongoDB):**
```bash
cd infra
cp .env.example .env      # first time only; adjust credentials/DB_DATA_DIR if needed
docker compose up -d postgres mongo
node ../database/migrate.js   # applies pending schema/data migrations — see database/README.md
```
Data is persisted to `DB_DATA_DIR` on the host (defaults to `C:\temp\scrum-planner-data`) and a
named Docker volume for Postgres, so it survives container recreation. Re-run
`node database/migrate.js` any time new migration files are added — **do this before starting
`backend`**, since it validates its schema on startup rather than creating tables itself.

**2. Backend + frontend:**
```bash
docker compose up -d --build backend frontend
```
Or bring up everything at once (`docker compose up -d --build`) once the data containers already
have their migrations applied.

- Backend API: http://localhost:8080/api (see [`backend/core/README.md`](backend/core/README.md))
- Frontend: http://localhost:5173 — if there are no projects yet, it opens straight into the
  first-project setup wizard (predefined work item types + a default workflow per type, editable
  afterwards from the project configuration screen).

## Configuration
Local PostgreSQL/MongoDB connection settings and the data folder location are configured via
`infra/.env` (copy from `infra/.env.example`) — see [`database/README.md`](database/README.md).

> TODO: document remaining environment variables, including: per-project/per-pipeline AI provider &
> model selection in the AI Gateway module, and provider credentials (OpenAI, Claude, Bedrock, Gemini).

## Roadmap

> **Nota:** este proyecto se auto-planifica — se usará la propia aplicación para gestionar su backlog una vez exista un MVP mínimo utilizable. Esto prioriza tener Projects + Work Items + Workflow básico funcionando temprano, para poder cargar en la herramienta misma el resto de este roadmap.

- [ ] Define data model for Projects, Teams, Users/Roles
- [x] Define data model for artifacts (Epics, Features, User Stories, Tasks, Bugs, Test Cases, Test Runs) — see `docs/backlog-data-model.md`
- [x] Local database environment: Docker Compose (PostgreSQL + MongoDB, persisted data) and migration tooling — see `database/README.md`
- [x] First feature: project setup wizard — predefined work item types, default workflow per type, project configuration screen to edit states/transitions (`backend/core`, `frontend`)
- [ ] **MVP: minimal Project + Work Item CRUD + basic Workflow, usable to self-host this project's own backlog** (work item CRUD still to do)
- [ ] Migrate this roadmap into the application itself once the MVP is usable
- [ ] Implement customizable workflow engine per artifact type
- [ ] Implement custom fields and custom menu options
- [ ] Stand up AI Gateway module with provider adapters (OpenAI, Claude, Bedrock, Gemini) and per-project/per-pipeline routing
- [ ] Define shared contracts (API/event schemas) between core, ai-gateway, ai-assistants, and plugins
- [ ] Define the plugin/extension SDK for custom assistants
- [ ] Integrate first AI Assistant (Epic & Features Planner)
- [ ] Integrate remaining AI Assistants
- [ ] Scrum ceremonies tracking and metrics/reporting module
- [ ] Repository & wiki integrations
- [ ] Dashboards per role
- [ ] Evaluate extracting AI Gateway / AI Assistants / frontend into separate repositories/services once the system stabilizes

## Working with Claude
See [`CLAUDE.md`](CLAUDE.md) for notes on using Claude's browser tools to inspect/pair on the UI.

## Contributing
- **Branching strategy:** trunk-based development — short-lived feature branches, frequent PRs into `main`, feature flags for incomplete work.
> TODO: define PR review requirements and coding standards (Java/Spring conventions for backend, React/JS conventions for frontend).

## License
MIT License.

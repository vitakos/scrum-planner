# Database

Local data storage for Scrum Planner: **PostgreSQL** for the relational core
(projects, work items, workflows, custom-field definitions, sprints,
state-transition log) and **MongoDB** for flexible content (work item
descriptions, custom field values, comments, attachments, AI-assistant
conversation data). See [`../docs/backlog-data-model.md`](../docs/backlog-data-model.md)
for the full rationale behind the split.

The containers themselves (with data persisted to a host folder) are defined
in [`../infra/docker-compose.yml`](../infra/docker-compose.yml).

## Layout

```
database/
├── migrate.js              # run this after (re)creating the containers, or after adding a migration
├── sql/
│   ├── migrations/          # ordered *.sql files, applied against PostgreSQL
│   └── upgrade.json         # tracks which sql/migrations files have been applied, and when
└── mongo/
    ├── migrations/          # ordered *.js files, applied against MongoDB via mongosh
    └── upgrade.json         # tracks which mongo/migrations files have been applied, and when
```

Each `upgrade.json` looks like:

```json
{
  "applied": [
    { "file": "0001_init_schema.sql", "appliedAt": "2026-09-06T12:00:00.000Z" }
  ]
}
```

`database/migrate.js` reads this file, works out which migration files in the
matching `migrations/` folder haven't been applied yet, runs them in filename
order, and appends an entry after each one succeeds. It's safe to run
repeatedly — migrations already recorded in `upgrade.json` are skipped.

## Adding a migration

1. Add a new file to `sql/migrations/` or `mongo/migrations/`, numbered after
   the last one (`0002_...`, `0003_...`). Migrations are applied in filename
   sort order and never re-run automatically, so don't edit a file that has
   already shipped — add a new one instead.
   - SQL files are plain `.sql`, executed with `psql`.
   - Mongo files are plain `.js`, executed with `mongosh` — use the `db`
     global (the connection URI already selects the right database).
2. Run `node database/migrate.js` to apply it and update `upgrade.json`.
3. Commit the new migration file together with the updated `upgrade.json`.

## Usage

### 1. Start the containers

```bash
cd infra
cp .env.example .env   # first time only; adjust credentials/paths if needed
docker compose up -d
```

### 2. Apply migrations

```bash
node database/migrate.js
```

Run this every time the containers are (re)created (e.g. after
`docker compose down -v` or a fresh `docker compose up -d` on a clean data
folder), and again whenever new migration files land (e.g. after `git pull`).

Flags:

```bash
node database/migrate.js --sql-only
node database/migrate.js --mongo-only
```

### Resetting local data

The containers bind-mount their data directories to the host folder
`DB_DATA_DIR` (see `infra/.env`, defaults to `C:\temp\scrum-planner-data`).
To start over completely:

```bash
cd infra
docker compose down
# then delete the postgres/ and mongo/ subfolders under DB_DATA_DIR
docker compose up -d
node ../database/migrate.js
```

#!/usr/bin/env node
'use strict';

/**
 * database/migrate.js
 *
 * Applies pending SQL (PostgreSQL) and Mongo migrations against the local
 * docker compose containers defined in ../infra/docker-compose.yml, and
 * records what was applied in sql/upgrade.json and mongo/upgrade.json.
 *
 * Run this:
 *   - the first time you bring the containers up (`docker compose up -d`),
 *   - and again any time a new file is added under sql/migrations or
 *     mongo/migrations (e.g. after `git pull`, or while changing the schema
 *     yourself).
 *
 * Usage:
 *   node database/migrate.js               # apply both engines
 *   node database/migrate.js --sql-only
 *   node database/migrate.js --mongo-only
 *
 * Requires: Docker Desktop running, and the containers from
 * ../infra/docker-compose.yml started (this script does not start them).
 */

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const ROOT = __dirname;
const INFRA_DIR = path.join(ROOT, '..', 'infra');
const COMPOSE_FILE = path.join(INFRA_DIR, 'docker-compose.yml');
const ENV_FILE = fs.existsSync(path.join(INFRA_DIR, '.env'))
  ? path.join(INFRA_DIR, '.env')
  : path.join(INFRA_DIR, '.env.example');

const SQL_MIGRATIONS_DIR = path.join(ROOT, 'sql', 'migrations');
const SQL_UPGRADE_FILE = path.join(ROOT, 'sql', 'upgrade.json');
const MONGO_MIGRATIONS_DIR = path.join(ROOT, 'mongo', 'migrations');
const MONGO_UPGRADE_FILE = path.join(ROOT, 'mongo', 'upgrade.json');

function loadEnvFile(file) {
  const env = {};
  if (!fs.existsSync(file)) return env;
  for (const line of fs.readFileSync(file, 'utf8').split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const idx = trimmed.indexOf('=');
    if (idx === -1) continue;
    env[trimmed.slice(0, idx).trim()] = trimmed.slice(idx + 1).trim();
  }
  return env;
}

const fileEnv = loadEnvFile(ENV_FILE);
function cfg(name, fallback) {
  return process.env[name] || fileEnv[name] || fallback;
}

function compose(args, options = {}) {
  return execFileSync(
    'docker',
    ['compose', '-f', COMPOSE_FILE, '--env-file', ENV_FILE, ...args],
    { stdio: options.capture ? 'pipe' : 'inherit', encoding: 'utf8' }
  );
}

function sleep(ms) {
  execFileSync(process.execPath, ['-e', `setTimeout(()=>process.exit(0), ${ms})`]);
}

function withRetry(fn, { retries = 15, delayMs = 2000, label = '' } = {}) {
  let lastErr;
  for (let i = 0; i < retries; i++) {
    try {
      return fn();
    } catch (err) {
      lastErr = err;
      if (i < retries - 1) {
        console.log(`  ... ${label || 'operation'} not ready yet, retrying (${i + 1}/${retries})`);
        sleep(delayMs);
      }
    }
  }
  throw lastErr;
}

function readUpgradeLog(file) {
  if (!fs.existsSync(file)) return { applied: [] };
  const parsed = JSON.parse(fs.readFileSync(file, 'utf8'));
  if (!Array.isArray(parsed.applied)) parsed.applied = [];
  return parsed;
}

function writeUpgradeLog(file, log) {
  fs.writeFileSync(file, JSON.stringify(log, null, 2) + '\n');
}

function pendingMigrations(migrationsDir, log, ext) {
  const applied = new Set(log.applied.map((entry) => entry.file));
  if (!fs.existsSync(migrationsDir)) return [];
  return fs
    .readdirSync(migrationsDir)
    .filter((f) => f.endsWith(ext))
    .sort()
    .filter((f) => !applied.has(f));
}

function applySqlMigration(file) {
  const src = path.join(SQL_MIGRATIONS_DIR, file);
  const dest = `/tmp/${file}`;
  withRetry(() => compose(['cp', src, `postgres:${dest}`]), { label: 'postgres container' });
  compose([
    'exec', '-T', 'postgres',
    'psql',
    '-v', 'ON_ERROR_STOP=1',
    '-U', cfg('POSTGRES_USER', 'scrum_planner'),
    '-d', cfg('POSTGRES_DB', 'scrum_planner'),
    '-f', dest
  ]);
}

function applyMongoMigration(file) {
  const src = path.join(MONGO_MIGRATIONS_DIR, file);
  const dest = `/tmp/${file}`;
  withRetry(() => compose(['cp', src, `mongo:${dest}`]), { label: 'mongo container' });
  const uri =
    `mongodb://${cfg('MONGO_ROOT_USER', 'scrum_planner')}:${cfg('MONGO_ROOT_PASSWORD', 'scrum_planner')}` +
    `@localhost:27017/${cfg('MONGO_DB', 'scrum_planner')}?authSource=admin`;
  compose(['exec', '-T', 'mongo', 'mongosh', uri, '--quiet', '--file', dest]);
}

function run(engine) {
  const isSql = engine === 'sql';
  const migrationsDir = isSql ? SQL_MIGRATIONS_DIR : MONGO_MIGRATIONS_DIR;
  const upgradeFile = isSql ? SQL_UPGRADE_FILE : MONGO_UPGRADE_FILE;
  const ext = isSql ? '.sql' : '.js';

  const log = readUpgradeLog(upgradeFile);
  const pending = pendingMigrations(migrationsDir, log, ext);

  if (pending.length === 0) {
    console.log(`[${engine}] up to date, nothing to apply.`);
    return;
  }

  for (const file of pending) {
    console.log(`[${engine}] applying ${file} ...`);
    withRetry(() => (isSql ? applySqlMigration(file) : applyMongoMigration(file)), {
      label: `${engine} migration ${file}`
    });
    log.applied.push({ file, appliedAt: new Date().toISOString() });
    writeUpgradeLog(upgradeFile, log);
    console.log(`[${engine}] applied ${file}`);
  }
}

function main() {
  const args = process.argv.slice(2);
  const sqlOnly = args.includes('--sql-only');
  const mongoOnly = args.includes('--mongo-only');

  if (!fs.existsSync(path.join(INFRA_DIR, '.env'))) {
    console.log(`[warn] infra/.env not found, using defaults from ${path.basename(ENV_FILE)}.`);
    console.log('       Copy infra/.env.example to infra/.env to customize credentials/paths.');
  }

  if (!mongoOnly) run('sql');
  if (!sqlOnly) run('mongo');
}

main();

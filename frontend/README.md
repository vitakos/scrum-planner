# frontend

React (Vite) UI for Scrum Planner. No authentication yet — single local user.

Behavior: if the backend reports no projects, the app shows the first-project
setup wizard (predefined work item types, default workflow created on
submit). Otherwise it shows the project configuration screen, where you can
edit each work item type's workflow (add/rename/delete states, add/delete
transitions).

## Running

Via Docker (part of the full stack, see the root README):
```bash
cd ../infra
docker compose up -d --build frontend
```
Note the frontend bakes `VITE_API_BASE_URL` in at *build* time (see
`infra/.env.example`) — rebuild the image after changing it.

For local dev with hot-reload against a backend running elsewhere:
```bash
cd frontend
cp .env.example .env.local   # adjust VITE_API_BASE_URL if needed
npm install
npm run dev
```

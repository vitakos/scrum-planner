import { useEffect, useState } from 'react';
import { api } from './services/api.js';
import ProjectWizardPage from './pages/ProjectWizardPage.jsx';
import ConfigurePage from './pages/ConfigurePage.jsx';

export default function App() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [projects, setProjects] = useState([]);
  const [workItemTypes, setWorkItemTypes] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);

  useEffect(() => {
    loadInitialData();
  }, []);

  async function loadInitialData() {
    setLoading(true);
    setError(null);
    try {
      const [projectList, typeList] = await Promise.all([
        api.listProjects(),
        api.listWorkItemTypes()
      ]);
      setProjects(projectList);
      setWorkItemTypes(typeList);
      setSelectedProjectId((current) => current ?? projectList[0]?.id ?? null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleProjectCreated(project) {
    setProjects((prev) => [...prev, project]);
    setSelectedProjectId(project.id);
  }

  if (loading) {
    return <div className="page-center">Loading…</div>;
  }

  if (error) {
    return (
      <div className="page-center">
        <p className="error-banner">Couldn't reach the backend: {error}</p>
        <button onClick={loadInitialData}>Retry</button>
      </div>
    );
  }

  if (projects.length === 0) {
    return <ProjectWizardPage workItemTypes={workItemTypes} onCreated={handleProjectCreated} />;
  }

  const selectedProject = projects.find((p) => p.id === selectedProjectId) ?? projects[0];

  return (
    <div className="app-shell">
      <header className="app-header">
        <div>
          <span className="app-title">Scrum Planner</span>
        </div>
        {projects.length > 1 ? (
          <select
            value={selectedProject.id}
            onChange={(e) => setSelectedProjectId(e.target.value)}
          >
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.key} — {p.name}
              </option>
            ))}
          </select>
        ) : (
          <span className="project-badge">
            {selectedProject.key} — {selectedProject.name}
          </span>
        )}
      </header>
      <main>
        <ConfigurePage project={selectedProject} />
      </main>
    </div>
  );
}

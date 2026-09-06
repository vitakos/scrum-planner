import { useEffect, useState } from 'react';
import { api } from '../services/api.js';
import WorkflowEditor from '../components/WorkflowEditor.jsx';

export default function ConfigurePage({ project }) {
  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeType, setActiveType] = useState(null);

  useEffect(() => {
    loadWorkflows();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [project.id]);

  async function loadWorkflows() {
    setLoading(true);
    setError(null);
    try {
      const data = await api.listWorkflows(project.id);
      setWorkflows(data);
      setActiveType((current) => current ?? data[0]?.workItemType ?? null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function updateWorkflow(workItemType, updater) {
    setWorkflows((prev) =>
      prev.map((w) => (w.workItemType === workItemType ? updater(w) : w))
    );
  }

  if (loading) return <p className="page-center">Loading workflows…</p>;
  if (error) return <p className="error-banner">{error}</p>;

  const active = workflows.find((w) => w.workItemType === activeType);

  return (
    <div className="configure-page">
      {project.description && <p className="project-description">{project.description}</p>}

      <div className="configure-layout">
        <nav className="type-nav">
          {workflows.map((w) => (
            <button
              key={w.workItemType}
              className={w.workItemType === activeType ? 'type-nav-item active' : 'type-nav-item'}
              onClick={() => setActiveType(w.workItemType)}
            >
              {w.workItemTypeName}
              <span className="type-nav-count">{w.states.length}</span>
            </button>
          ))}
        </nav>

        <section className="workflow-panel">
          {active ? (
            <WorkflowEditor
              key={active.workItemType}
              projectId={project.id}
              workflow={active}
              onChange={(updater) => updateWorkflow(active.workItemType, updater)}
            />
          ) : (
            <p>No work item types configured for this project.</p>
          )}
        </section>
      </div>
    </div>
  );
}

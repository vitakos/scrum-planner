import { useEffect, useState } from 'react';
import { api } from '../services/api.js';
import WorkflowEditor from '../components/WorkflowEditor.jsx';
import CustomFieldsEditor from '../components/CustomFieldsEditor.jsx';

export default function ConfigurePage({ project, onProjectUpdated }) {
  const [workflows, setWorkflows] = useState([]);
  const [fieldCatalogs, setFieldCatalogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeType, setActiveType] = useState(null);
  const [section, setSection] = useState('workflow');

  useEffect(() => {
    loadConfiguration();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [project.id]);

  async function loadConfiguration() {
    setLoading(true);
    setError(null);
    try {
      const [workflowData, fieldData] = await Promise.all([
        api.listWorkflows(project.id),
        api.listCustomFields(project.id)
      ]);
      setWorkflows(workflowData);
      setFieldCatalogs(fieldData);
      setActiveType((current) => current ?? workflowData[0]?.workItemType ?? null);
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

  function updateFieldCatalog(workItemType, updater) {
    setFieldCatalogs((prev) =>
      prev.map((c) => (c.workItemType === workItemType ? updater(c) : c))
    );
  }

  if (loading) return <p className="page-center">Loading configuration…</p>;
  if (error) return <p className="error-banner">{error}</p>;

  const activeWorkflow = workflows.find((w) => w.workItemType === activeType);
  const activeFieldCatalog = fieldCatalogs.find((c) => c.workItemType === activeType);

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
          <nav className="tab-nav configure-section-nav">
            <button
              className={section === 'general' ? 'tab-nav-item active' : 'tab-nav-item'}
              onClick={() => setSection('general')}
            >
              General
            </button>
            {activeType && (
              <>
                <button
                  className={section === 'workflow' ? 'tab-nav-item active' : 'tab-nav-item'}
                  onClick={() => setSection('workflow')}
                >
                  Workflow
                </button>
                <button
                  className={section === 'fields' ? 'tab-nav-item active' : 'tab-nav-item'}
                  onClick={() => setSection('fields')}
                >
                  Custom Fields
                </button>
              </>
            )}
          </nav>

          {section === 'general' && (
            <GeneralSection project={project} onProjectUpdated={onProjectUpdated} />
          )}

          {section !== 'general' &&
            (activeType ? (
              <>
                {section === 'workflow' && activeWorkflow && (
                  <WorkflowEditor
                    key={activeWorkflow.workItemType}
                    projectId={project.id}
                    workflow={activeWorkflow}
                    onChange={(updater) => updateWorkflow(activeWorkflow.workItemType, updater)}
                  />
                )}

                {section === 'fields' && activeFieldCatalog && (
                  <CustomFieldsEditor
                    key={activeFieldCatalog.workItemType}
                    projectId={project.id}
                    catalog={activeFieldCatalog}
                    onChange={(updater) => updateFieldCatalog(activeFieldCatalog.workItemType, updater)}
                  />
                )}
              </>
            ) : (
              <p>No work item types configured for this project.</p>
            ))}
        </section>
      </div>
    </div>
  );
}

function GeneralSection({ project, onProjectUpdated }) {
  const [repositoryUrl, setRepositoryUrl] = useState(project.repositoryUrl ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setRepositoryUrl(project.repositoryUrl ?? '');
  }, [project.id, project.repositoryUrl]);

  async function handleSave(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const updated = await api.updateProject(project.id, { repositoryUrl });
      setRepositoryUrl(updated.repositoryUrl ?? '');
      onProjectUpdated?.(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      {project.repositoryUrl && (
        <p className="project-repository-url">
          Repository:{' '}
          <a href={project.repositoryUrl} target="_blank" rel="noreferrer">
            {project.repositoryUrl}
          </a>
        </p>
      )}
      <form onSubmit={handleSave} className="state-add-form">
        <label>
          Repository URL
          <input
            type="text"
            value={repositoryUrl}
            onChange={(e) => setRepositoryUrl(e.target.value)}
            placeholder="https://github.com/org/repo"
          />
        </label>
        {error && <span className="field-error">{error}</span>}
        <button type="submit" disabled={busy}>
          {busy ? 'Saving…' : 'Save'}
        </button>
      </form>
    </div>
  );
}

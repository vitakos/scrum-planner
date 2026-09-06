import { useEffect, useState } from 'react';
import { api } from '../services/api.js';
import WorkItemBoard from '../components/WorkItemBoard.jsx';

export default function BacklogPage({ project }) {
  const [workflows, setWorkflows] = useState([]);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeType, setActiveType] = useState(null);

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [project.id]);

  async function loadAll() {
    setLoading(true);
    setError(null);
    try {
      const [workflowList, itemList] = await Promise.all([
        api.listWorkflows(project.id),
        api.listWorkItems(project.id)
      ]);
      setWorkflows(workflowList);
      setItems(itemList);
      setActiveType((current) => current ?? workflowList[0]?.workItemType ?? null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function upsertItem(updated) {
    setItems((prev) => {
      const exists = prev.some((i) => i.id === updated.id);
      return exists ? prev.map((i) => (i.id === updated.id ? updated : i)) : [...prev, updated];
    });
  }

  function removeItem(id) {
    setItems((prev) => prev.filter((i) => i.id !== id));
  }

  if (loading) return <p className="page-center">Loading backlog…</p>;
  if (error) return <p className="error-banner">{error}</p>;

  const activeWorkflow = workflows.find((w) => w.workItemType === activeType);
  const itemsForType = items.filter((i) => i.type === activeType);

  return (
    <div className="configure-page">
      <div className="configure-layout">
        <nav className="type-nav">
          {workflows.map((w) => (
            <button
              key={w.workItemType}
              className={w.workItemType === activeType ? 'type-nav-item active' : 'type-nav-item'}
              onClick={() => setActiveType(w.workItemType)}
            >
              {w.workItemTypeName}
              <span className="type-nav-count">{items.filter((i) => i.type === w.workItemType).length}</span>
            </button>
          ))}
        </nav>

        <section className="workflow-panel">
          {activeWorkflow ? (
            <WorkItemBoard
              key={activeWorkflow.workItemType}
              projectId={project.id}
              workflow={activeWorkflow}
              items={itemsForType}
              onItemChanged={upsertItem}
              onItemDeleted={removeItem}
            />
          ) : (
            <p>No work item types configured for this project.</p>
          )}
        </section>
      </div>
    </div>
  );
}

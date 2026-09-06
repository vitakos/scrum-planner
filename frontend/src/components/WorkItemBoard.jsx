import { useState } from 'react';
import { api } from '../services/api.js';

export default function WorkItemBoard({ projectId, workflow, items, onItemChanged, onItemDeleted }) {
  const [newTitle, setNewTitle] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState(null);

  const states = [...workflow.states].sort((a, b) => a.sortOrder - b.sortOrder);
  const itemsByState = Object.fromEntries(states.map((s) => [s.id, []]));
  for (const item of items) {
    (itemsByState[item.stateId] ?? (itemsByState[item.stateId] = [])).push(item);
  }

  async function handleCreate(e) {
    e.preventDefault();
    if (!newTitle.trim()) return;
    setCreating(true);
    setError(null);
    try {
      const created = await api.createWorkItem(projectId, {
        type: workflow.workItemType,
        title: newTitle.trim()
      });
      onItemChanged(created);
      setNewTitle('');
    } catch (err) {
      setError(err.message);
    } finally {
      setCreating(false);
    }
  }

  async function handleMove(item, transitionId) {
    setBusyId(item.id);
    setError(null);
    try {
      const updated = await api.applyWorkItemTransition(projectId, item.id, { transitionId });
      onItemChanged(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(item) {
    setBusyId(item.id);
    setError(null);
    try {
      await api.deleteWorkItem(projectId, item.id);
      onItemDeleted(item.id);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      <h2>{workflow.workItemTypeName}</h2>

      {error && <p className="error-banner">{error}</p>}

      <form onSubmit={handleCreate} className="inline-form">
        <input
          type="text"
          placeholder={`New ${workflow.workItemTypeName.toLowerCase()} title`}
          value={newTitle}
          onChange={(e) => setNewTitle(e.target.value)}
          maxLength={500}
          className="new-item-input"
        />
        <button type="submit" disabled={creating}>
          Add {workflow.workItemTypeName.toLowerCase()}
        </button>
      </form>

      <div className="board">
        {states.map((state) => (
          <div key={state.id} className="board-column">
            <div className="board-column-header">
              <span className={`category-dot category-${state.category}`} />
              {state.name}
              <span className="type-nav-count">{itemsByState[state.id]?.length ?? 0}</span>
            </div>
            <div className="board-column-body">
              {(itemsByState[state.id] ?? []).map((item) => (
                <div key={item.id} className="board-card">
                  <div className="board-card-key">{item.key}</div>
                  <div className="board-card-title">{item.title}</div>
                  <div className="board-card-actions">
                    {item.availableTransitions.map((t) => (
                      <button
                        key={t.id}
                        disabled={busyId === item.id}
                        onClick={() => handleMove(item, t.id)}
                        title={`Move to ${t.toStateName}`}
                      >
                        {t.name}
                      </button>
                    ))}
                    <button
                      className="link-button"
                      disabled={busyId === item.id}
                      onClick={() => handleDelete(item)}
                    >
                      Delete
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

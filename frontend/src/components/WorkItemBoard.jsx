import { useState } from 'react';
import { api } from '../services/api.js';
import { resolveStateColor } from '../utils/stateColors.js';
import { parentTypesFor, childTypesFor } from '../utils/workItemHierarchy.js';

function parentCandidatesFor(type, allItems) {
  const parentTypes = parentTypesFor(type);
  if (parentTypes.length === 0) return [];
  return allItems.filter((i) => parentTypes.includes(i.type));
}

export default function WorkItemBoard({
  projectId,
  workflow,
  workflows,
  items,
  allItems,
  presetParentId,
  onItemChanged,
  onItemDeleted,
  onAddChild,
  onDrillDown
}) {
  const [newTitle, setNewTitle] = useState('');
  const [newParentId, setNewParentId] = useState(presetParentId ?? '');
  const [busyId, setBusyId] = useState(null);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState(null);

  const states = [...workflow.states].sort((a, b) => a.sortOrder - b.sortOrder);
  const itemsByState = Object.fromEntries(states.map((s) => [s.id, []]));
  for (const item of items) {
    (itemsByState[item.stateId] ?? (itemsByState[item.stateId] = [])).push(item);
  }

  const parentCandidates = parentCandidatesFor(workflow.workItemType, allItems ?? []);
  const typeNameByCode = Object.fromEntries((workflows ?? []).map((w) => [w.workItemType, w.workItemTypeName]));
  const configuredTypes = new Set((workflows ?? []).map((w) => w.workItemType));

  async function handleCreate(e) {
    e.preventDefault();
    if (!newTitle.trim()) return;
    setCreating(true);
    setError(null);
    try {
      const created = await api.createWorkItem(projectId, {
        type: workflow.workItemType,
        title: newTitle.trim(),
        parentId: newParentId || null
      });
      onItemChanged(created);
      setNewTitle('');
      setNewParentId('');
    } catch (err) {
      setError(err.message);
    } finally {
      setCreating(false);
    }
  }

  async function handleReparent(item, parentId) {
    setBusyId(item.id);
    setError(null);
    try {
      const updated = await api.updateWorkItem(projectId, item.id, {
        title: item.title,
        parentId: parentId || null
      });
      onItemChanged(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
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
        {parentCandidates.length > 0 && (
          <select
            value={newParentId}
            onChange={(e) => setNewParentId(e.target.value)}
            className="parent-select"
          >
            <option value="">No parent</option>
            {parentCandidates.map((p) => (
              <option key={p.id} value={p.id}>
                {p.key} — {p.title}
              </option>
            ))}
          </select>
        )}
        <button type="submit" disabled={creating}>
          Add {workflow.workItemTypeName.toLowerCase()}
        </button>
      </form>

      <div className="board">
        {states.map((state) => (
          <div key={state.id} className="board-column">
            <div className="board-column-header">
              <span className="category-dot" style={{ backgroundColor: resolveStateColor(state) }} />
              {state.name}
              <span className="type-nav-count">{itemsByState[state.id]?.length ?? 0}</span>
            </div>
            <div className="board-column-body">
              {(itemsByState[state.id] ?? []).map((item) => {
                const childTypes = childTypesFor(item.type).filter((ct) => configuredTypes.has(ct));
                return (
                  <div key={item.id} className="board-card">
                    <div className="board-card-key">{item.key}</div>
                    <div className="board-card-title">{item.title}</div>
                    {item.parentKey && (
                      <div className="board-card-parent">
                        Parent: {item.parentKey} — {item.parentTitle}
                      </div>
                    )}
                    {parentCandidates.length > 0 && (
                      <select
                        value={item.parentId ?? ''}
                        disabled={busyId === item.id}
                        onChange={(e) => handleReparent(item, e.target.value)}
                        className="parent-select parent-select-card"
                      >
                        <option value="">No parent</option>
                        {parentCandidates.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.key} — {p.title}
                          </option>
                        ))}
                      </select>
                    )}
                    {(childTypes.length > 0 || item.childCount > 0) && (
                      <div className="board-card-hierarchy">
                        {item.childCount > 0 && childTypes.length > 0 && (
                          <button
                            type="button"
                            className="link-button"
                            onClick={() => onDrillDown(item, childTypes[0])}
                          >
                            View {item.childCount} {item.childCount === 1 ? 'child' : 'children'}
                          </button>
                        )}
                        {item.childCount > 0 && childTypes.length === 0 && (
                          <span className="board-card-children">
                            {item.childCount} {item.childCount === 1 ? 'child item' : 'child items'}
                          </span>
                        )}
                        {childTypes.map((ct) => (
                          <button
                            key={ct}
                            type="button"
                            className="link-button-add"
                            onClick={() => onAddChild(item, ct)}
                          >
                            + Add {(typeNameByCode[ct] ?? ct).toLowerCase()}
                          </button>
                        ))}
                      </div>
                    )}
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
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

import { useState } from 'react';
import { api } from '../services/api.js';
import WorkflowDiagram from './WorkflowDiagram.jsx';
import { resolveStateColor } from '../utils/stateColors.js';

const CATEGORY_LABELS = {
  to_do: 'To Do',
  in_progress: 'In Progress',
  done: 'Done'
};

export default function WorkflowEditor({ projectId, workflow, onChange }) {
  const [tab, setTab] = useState('states');
  const [newStateName, setNewStateName] = useState('');
  const [newStateCategory, setNewStateCategory] = useState('to_do');
  const [newStateDescription, setNewStateDescription] = useState('');
  const [newTransitionName, setNewTransitionName] = useState('');
  const [newTransitionFrom, setNewTransitionFrom] = useState(workflow.states[0]?.id ?? '');
  const [newTransitionTo, setNewTransitionTo] = useState(workflow.states[1]?.id ?? workflow.states[0]?.id ?? '');
  const [editingTransitionId, setEditingTransitionId] = useState(null);
  const [editingTransitionName, setEditingTransitionName] = useState('');
  const [editingDescriptionId, setEditingDescriptionId] = useState(null);
  const [editingDescriptionText, setEditingDescriptionText] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const sortedStates = [...workflow.states].sort((a, b) => a.sortOrder - b.sortOrder);
  const stateNameById = Object.fromEntries(workflow.states.map((s) => [s.id, s.name]));

  async function run(action) {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function handleAddState(e) {
    e.preventDefault();
    if (!newStateName.trim()) return;
    run(async () => {
      const created = await api.addState(projectId, workflow.workItemType, {
        name: newStateName.trim(),
        category: newStateCategory,
        description: newStateDescription.trim() || null
      });
      onChange((w) => ({ ...w, states: [...w.states, created] }));
      setNewStateName('');
      setNewStateDescription('');
    });
  }

  function handleDeleteState(stateId) {
    run(async () => {
      await api.deleteState(projectId, workflow.workItemType, stateId);
      onChange((w) => ({
        ...w,
        states: w.states.filter((s) => s.id !== stateId),
        transitions: w.transitions.filter((t) => t.fromStateId !== stateId && t.toStateId !== stateId)
      }));
    });
  }

  function handleColorChange(state, color) {
    run(async () => {
      const updated = await api.updateState(projectId, workflow.workItemType, state.id, {
        name: state.name,
        category: state.category,
        color,
        description: state.description ?? null
      });
      onChange((w) => ({
        ...w,
        states: w.states.map((s) => (s.id === state.id ? updated : s))
      }));
    });
  }

  function startEditDescription(state) {
    setEditingDescriptionId(state.id);
    setEditingDescriptionText(state.description ?? '');
  }

  function cancelEditDescription() {
    setEditingDescriptionId(null);
    setEditingDescriptionText('');
  }

  function saveEditDescription(e, state) {
    e.preventDefault();
    run(async () => {
      const updated = await api.updateState(projectId, workflow.workItemType, state.id, {
        name: state.name,
        category: state.category,
        color: state.color ?? null,
        description: editingDescriptionText.trim() || null
      });
      onChange((w) => ({
        ...w,
        states: w.states.map((s) => (s.id === state.id ? updated : s))
      }));
      setEditingDescriptionId(null);
      setEditingDescriptionText('');
    });
  }

  function moveState(index, direction) {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= sortedStates.length) return;
    const reordered = [...sortedStates];
    const [moved] = reordered.splice(index, 1);
    reordered.splice(targetIndex, 0, moved);
    const stateIds = reordered.map((s) => s.id);
    run(async () => {
      const updated = await api.reorderStates(projectId, workflow.workItemType, stateIds);
      onChange((w) => ({ ...w, states: updated }));
    });
  }

  function handleAddTransition(e) {
    e.preventDefault();
    if (!newTransitionName.trim() || !newTransitionFrom || !newTransitionTo) return;
    run(async () => {
      const created = await api.addTransition(projectId, workflow.workItemType, {
        name: newTransitionName.trim(),
        fromStateId: newTransitionFrom,
        toStateId: newTransitionTo
      });
      onChange((w) => ({ ...w, transitions: [...w.transitions, created] }));
      setNewTransitionName('');
    });
  }

  function startRenameTransition(transition) {
    setEditingTransitionId(transition.id);
    setEditingTransitionName(transition.name);
  }

  function cancelRenameTransition() {
    setEditingTransitionId(null);
    setEditingTransitionName('');
  }

  function saveRenameTransition(e) {
    e.preventDefault();
    if (!editingTransitionName.trim()) return;
    const transitionId = editingTransitionId;
    run(async () => {
      const updated = await api.updateTransition(projectId, workflow.workItemType, transitionId, {
        name: editingTransitionName.trim()
      });
      onChange((w) => ({
        ...w,
        transitions: w.transitions.map((t) => (t.id === transitionId ? updated : t))
      }));
      setEditingTransitionId(null);
      setEditingTransitionName('');
    });
  }

  function handleDeleteTransition(transitionId) {
    run(async () => {
      await api.deleteTransition(projectId, workflow.workItemType, transitionId);
      onChange((w) => ({ ...w, transitions: w.transitions.filter((t) => t.id !== transitionId) }));
    });
  }

  return (
    <div>
      <h2>{workflow.workItemTypeName}</h2>
      <p className="field-hint">{workflow.name}</p>

      {error && <p className="error-banner">{error}</p>}

      <nav className="tab-nav workflow-tab-nav">
        <button
          className={tab === 'states' ? 'tab-nav-item active' : 'tab-nav-item'}
          onClick={() => setTab('states')}
        >
          States
        </button>
        <button
          className={tab === 'transitions' ? 'tab-nav-item active' : 'tab-nav-item'}
          onClick={() => setTab('transitions')}
        >
          Transitions
        </button>
      </nav>

      {tab === 'states' && (
        <div className="workflow-tab-panel">
          <ul className="state-list">
            {sortedStates.map((s, index) => (
              <li key={s.id} className="state-row-group">
                <div className="state-row">
                  <div className="state-reorder-controls">
                    <button
                      type="button"
                      className="reorder-button"
                      disabled={busy || index === 0}
                      onClick={() => moveState(index, -1)}
                      title="Move up"
                      aria-label={`Move ${s.name} up`}
                    >
                      ▲
                    </button>
                    <button
                      type="button"
                      className="reorder-button"
                      disabled={busy || index === sortedStates.length - 1}
                      onClick={() => moveState(index, 1)}
                      title="Move down"
                      aria-label={`Move ${s.name} down`}
                    >
                      ▼
                    </button>
                  </div>
                  <input
                    type="color"
                    className="state-color-input"
                    value={resolveStateColor(s)}
                    disabled={busy}
                    title="State color"
                    onChange={(e) => handleColorChange(s, e.target.value)}
                  />
                  <span className="state-name">{s.name}</span>
                  <span className="field-hint">{CATEGORY_LABELS[s.category] ?? s.category}</span>
                  {s.initial && <span className="initial-badge">initial</span>}
                  {s.color && (
                    <button className="link-button" disabled={busy} onClick={() => handleColorChange(s, null)}>
                      Reset color
                    </button>
                  )}
                  {editingDescriptionId !== s.id && (
                    <button className="link-button" disabled={busy} onClick={() => startEditDescription(s)}>
                      {s.description ? 'Edit description' : 'Add description'}
                    </button>
                  )}
                  <button
                    className="link-button"
                    disabled={busy || s.initial}
                    onClick={() => handleDeleteState(s.id)}
                    title={s.initial ? 'Cannot delete the initial state' : 'Delete state'}
                  >
                    Delete
                  </button>
                </div>

                {editingDescriptionId === s.id ? (
                  <form onSubmit={(e) => saveEditDescription(e, s)} className="state-description-form">
                    <textarea
                      autoFocus
                      rows={2}
                      maxLength={2000}
                      placeholder="Explain what this state means (entry/exit criteria, expectations…)"
                      value={editingDescriptionText}
                      onChange={(e) => setEditingDescriptionText(e.target.value)}
                    />
                    <div className="state-description-actions">
                      <button type="submit" disabled={busy}>
                        Save
                      </button>
                      <button type="button" disabled={busy} onClick={cancelEditDescription}>
                        Cancel
                      </button>
                    </div>
                  </form>
                ) : (
                  s.description && <p className="state-description">{s.description}</p>
                )}
              </li>
            ))}
          </ul>

          <form onSubmit={handleAddState} className="state-add-form">
            <div className="inline-form">
              <input
                type="text"
                placeholder="New state name"
                value={newStateName}
                onChange={(e) => setNewStateName(e.target.value)}
                maxLength={100}
              />
              <select value={newStateCategory} onChange={(e) => setNewStateCategory(e.target.value)}>
                {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
              <button type="submit" disabled={busy}>
                Add state
              </button>
            </div>
            <textarea
              rows={2}
              maxLength={2000}
              placeholder="Description (optional)"
              value={newStateDescription}
              onChange={(e) => setNewStateDescription(e.target.value)}
              className="state-add-description"
            />
          </form>
        </div>
      )}

      {tab === 'transitions' && (
        <div className="workflow-tab-panel">
          <h3>Diagram</h3>
          <WorkflowDiagram workflow={workflow} />

          <h3>Transitions</h3>
          <ul className="transition-list">
            {workflow.transitions.map((t) =>
              editingTransitionId === t.id ? (
                <li key={t.id} className="transition-row">
                  <form onSubmit={saveRenameTransition} className="inline-form inline-form-tight">
                    <input
                      type="text"
                      autoFocus
                      value={editingTransitionName}
                      onChange={(e) => setEditingTransitionName(e.target.value)}
                      maxLength={100}
                    />
                    <span className="field-hint">
                      {stateNameById[t.fromStateId] ?? '?'} → {stateNameById[t.toStateId] ?? '?'}
                    </span>
                    <button type="submit" disabled={busy}>
                      Save
                    </button>
                    <button type="button" disabled={busy} onClick={cancelRenameTransition}>
                      Cancel
                    </button>
                  </form>
                </li>
              ) : (
                <li key={t.id} className="transition-row">
                  <span className="transition-name">{t.name}</span>
                  <span className="field-hint">
                    {stateNameById[t.fromStateId] ?? '?'} → {stateNameById[t.toStateId] ?? '?'}
                  </span>
                  <button className="link-button" disabled={busy} onClick={() => startRenameTransition(t)}>
                    Rename
                  </button>
                  <button className="link-button" disabled={busy} onClick={() => handleDeleteTransition(t.id)}>
                    Delete
                  </button>
                </li>
              )
            )}
          </ul>

          {workflow.states.length >= 2 && (
            <form onSubmit={handleAddTransition} className="inline-form">
              <input
                type="text"
                placeholder="Transition name (e.g. Start)"
                value={newTransitionName}
                onChange={(e) => setNewTransitionName(e.target.value)}
                maxLength={100}
              />
              <select value={newTransitionFrom} onChange={(e) => setNewTransitionFrom(e.target.value)}>
                {workflow.states.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
              <span>→</span>
              <select value={newTransitionTo} onChange={(e) => setNewTransitionTo(e.target.value)}>
                {workflow.states.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
              <button type="submit" disabled={busy}>
                Add transition
              </button>
            </form>
          )}
        </div>
      )}
    </div>
  );
}

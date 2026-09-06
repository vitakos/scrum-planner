import { useState } from 'react';
import { api } from '../services/api.js';

const CATEGORY_LABELS = {
  to_do: 'To Do',
  in_progress: 'In Progress',
  done: 'Done'
};

export default function WorkflowEditor({ projectId, workflow, onChange }) {
  const [newStateName, setNewStateName] = useState('');
  const [newStateCategory, setNewStateCategory] = useState('to_do');
  const [newTransitionName, setNewTransitionName] = useState('');
  const [newTransitionFrom, setNewTransitionFrom] = useState(workflow.states[0]?.id ?? '');
  const [newTransitionTo, setNewTransitionTo] = useState(workflow.states[1]?.id ?? workflow.states[0]?.id ?? '');
  const [editingTransitionId, setEditingTransitionId] = useState(null);
  const [editingTransitionName, setEditingTransitionName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

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
        category: newStateCategory
      });
      onChange((w) => ({ ...w, states: [...w.states, created] }));
      setNewStateName('');
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

      <h3>States</h3>
      <ul className="state-list">
        {workflow.states.map((s) => (
          <li key={s.id} className="state-row">
            <span className={`category-dot category-${s.category}`} />
            <span className="state-name">{s.name}</span>
            <span className="field-hint">{CATEGORY_LABELS[s.category] ?? s.category}</span>
            {s.initial && <span className="initial-badge">initial</span>}
            <button
              className="link-button"
              disabled={busy || s.initial}
              onClick={() => handleDeleteState(s.id)}
              title={s.initial ? 'Cannot delete the initial state' : 'Delete state'}
            >
              Delete
            </button>
          </li>
        ))}
      </ul>

      <form onSubmit={handleAddState} className="inline-form">
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
      </form>

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
  );
}

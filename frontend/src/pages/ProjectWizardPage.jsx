import { useState } from 'react';
import { api } from '../services/api.js';

function suggestKey(name) {
  const letters = name.replace(/[^a-zA-Z]/g, '').toUpperCase();
  return letters.slice(0, 4) || '';
}

export default function ProjectWizardPage({ workItemTypes, onCreated, onCancel }) {
  const [name, setName] = useState('');
  const [key, setKey] = useState('');
  const [keyEdited, setKeyEdited] = useState(false);
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  function handleNameChange(value) {
    setName(value);
    if (!keyEdited) setKey(suggestKey(value));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const project = await api.createProject({ key: key.trim(), name: name.trim(), description });
      onCreated(project);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="wizard">
      <div className="wizard-card">
        <h1>Welcome to Scrum Planner</h1>
        <p className="wizard-intro">
          There's no project yet — let's set up your first one. It starts with a predefined set of
          work item types and a default workflow for each; you can rename, add, or remove states and
          transitions afterwards from the project configuration screen.
        </p>

        <form onSubmit={handleSubmit} className="wizard-form">
          <label>
            Project name
            <input
              type="text"
              value={name}
              onChange={(e) => handleNameChange(e.target.value)}
              placeholder="e.g. Scrum Planner Assisted by AI"
              required
              maxLength={200}
            />
          </label>

          <label>
            Project key
            <input
              type="text"
              value={key}
              onChange={(e) => {
                setKeyEdited(true);
                setKey(e.target.value.toUpperCase());
              }}
              placeholder="e.g. SPAI"
              required
              maxLength={10}
              pattern="[A-Z][A-Z0-9]*"
              title="2-10 uppercase letters/digits, starting with a letter"
            />
            <span className="field-hint">Used as a short prefix for work items later (e.g. SPAI-123).</span>
          </label>

          <label>
            Description <span className="field-hint">(optional)</span>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={2000}
              rows={3}
            />
          </label>

          {error && <p className="error-banner">{error}</p>}

          <div className="wizard-actions">
            <button type="submit" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create project'}
            </button>
            {onCancel && (
              <button type="button" className="secondary-button" onClick={onCancel} disabled={submitting}>
                Cancel
              </button>
            )}
          </div>
        </form>

        <div className="wizard-types">
          <h2>Predefined work item types</h2>
          <ul className="type-chip-list">
            {workItemTypes.map((t) => (
              <li key={t.code} className="type-chip">
                {t.name}
              </li>
            ))}
          </ul>
          <p className="field-hint">
            Each gets its own default workflow (To Do → In Progress → Done) that you can customize
            once the project is created.
          </p>
        </div>
      </div>
    </div>
  );
}

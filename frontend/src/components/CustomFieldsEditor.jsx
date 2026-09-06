import { useState } from 'react';
import { api } from '../services/api.js';

const DATA_TYPE_LABELS = {
  TEXT: 'Text',
  NUMBER: 'Number',
  DATE: 'Date',
  BOOLEAN: 'Boolean',
  SINGLE_SELECT: 'Single select',
  MULTI_SELECT: 'Multi select'
};

const PREDEFINED_TYPE_LABELS = {
  TEXT: 'Text',
  DATE: 'Date',
  USER: 'User',
  REFERENCE: 'Reference',
  RICH_TEXT: 'Rich text',
  SINGLE_SELECT: 'Single select'
};

const SELECT_TYPES = new Set(['SINGLE_SELECT', 'MULTI_SELECT']);

function parseOptions(text) {
  return text
    .split(',')
    .map((option) => option.trim())
    .filter(Boolean);
}

export default function CustomFieldsEditor({ projectId, catalog, onChange }) {
  const [newName, setNewName] = useState('');
  const [newDataType, setNewDataType] = useState('TEXT');
  const [newOptions, setNewOptions] = useState('');
  const [editingFieldId, setEditingFieldId] = useState(null);
  const [editingName, setEditingName] = useState('');
  const [editingDataType, setEditingDataType] = useState('TEXT');
  const [editingOptions, setEditingOptions] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

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

  function handleAddField(e) {
    e.preventDefault();
    if (!newName.trim()) return;
    if (SELECT_TYPES.has(newDataType) && parseOptions(newOptions).length === 0) {
      setError('Add at least one option for a select field');
      return;
    }
    run(async () => {
      const created = await api.addCustomField(projectId, catalog.workItemType, {
        name: newName.trim(),
        dataType: newDataType,
        options: SELECT_TYPES.has(newDataType) ? parseOptions(newOptions) : null
      });
      onChange((c) => ({ ...c, customFields: [...c.customFields, created] }));
      setNewName('');
      setNewDataType('TEXT');
      setNewOptions('');
    });
  }

  function startEdit(field) {
    setEditingFieldId(field.id);
    setEditingName(field.name);
    setEditingDataType(field.dataType);
    setEditingOptions((field.options ?? []).join(', '));
  }

  function cancelEdit() {
    setEditingFieldId(null);
    setEditingName('');
    setEditingOptions('');
  }

  function saveEdit(e) {
    e.preventDefault();
    if (!editingName.trim()) return;
    if (SELECT_TYPES.has(editingDataType) && parseOptions(editingOptions).length === 0) {
      setError('Add at least one option for a select field');
      return;
    }
    const fieldId = editingFieldId;
    run(async () => {
      const updated = await api.updateCustomField(projectId, catalog.workItemType, fieldId, {
        name: editingName.trim(),
        dataType: editingDataType,
        options: SELECT_TYPES.has(editingDataType) ? parseOptions(editingOptions) : null
      });
      onChange((c) => ({
        ...c,
        customFields: c.customFields.map((f) => (f.id === fieldId ? updated : f))
      }));
      cancelEdit();
    });
  }

  function handleDelete(fieldId) {
    run(async () => {
      await api.deleteCustomField(projectId, catalog.workItemType, fieldId);
      onChange((c) => ({ ...c, customFields: c.customFields.filter((f) => f.id !== fieldId) }));
    });
  }

  return (
    <div>
      <h2>{catalog.workItemTypeName}</h2>
      <p className="field-hint">Fields available on every {catalog.workItemTypeName.toLowerCase()}.</p>

      {error && <p className="error-banner">{error}</p>}

      <h3>Pre-defined fields</h3>
      <ul className="field-list">
        {catalog.predefinedFields.map((f) => (
          <li key={f.code} className="field-row">
            <span className="state-name">{f.name}</span>
            <span className="field-type-badge">{PREDEFINED_TYPE_LABELS[f.dataType] ?? f.dataType}</span>
            <span className="field-hint">{f.description}</span>
            <span className="initial-badge">system</span>
          </li>
        ))}
      </ul>

      <h3>Custom fields</h3>
      <ul className="field-list">
        {catalog.customFields.length === 0 && (
          <li className="field-hint field-list-empty">No custom fields yet — add one below.</li>
        )}
        {catalog.customFields.map((f) =>
          editingFieldId === f.id ? (
            <li key={f.id} className="field-row-group">
              <form onSubmit={saveEdit} className="inline-form">
                <input
                  type="text"
                  autoFocus
                  value={editingName}
                  onChange={(e) => setEditingName(e.target.value)}
                  maxLength={100}
                />
                <select value={editingDataType} onChange={(e) => setEditingDataType(e.target.value)}>
                  {Object.entries(DATA_TYPE_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
                <button type="submit" disabled={busy}>
                  Save
                </button>
                <button type="button" disabled={busy} onClick={cancelEdit}>
                  Cancel
                </button>
              </form>
              {SELECT_TYPES.has(editingDataType) && (
                <input
                  type="text"
                  className="field-options-input"
                  placeholder="Options, comma separated"
                  value={editingOptions}
                  onChange={(e) => setEditingOptions(e.target.value)}
                />
              )}
            </li>
          ) : (
            <li key={f.id} className="field-row">
              <span className="state-name">{f.name}</span>
              <span className="field-type-badge">{DATA_TYPE_LABELS[f.dataType] ?? f.dataType}</span>
              {f.options?.length > 0 && <span className="field-hint">{f.options.join(', ')}</span>}
              <button className="link-button" disabled={busy} onClick={() => startEdit(f)}>
                Edit
              </button>
              <button className="link-button" disabled={busy} onClick={() => handleDelete(f.id)}>
                Delete
              </button>
            </li>
          )
        )}
      </ul>

      <form onSubmit={handleAddField} className="state-add-form">
        <div className="inline-form">
          <input
            type="text"
            placeholder="New field name"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            maxLength={100}
          />
          <select value={newDataType} onChange={(e) => setNewDataType(e.target.value)}>
            {Object.entries(DATA_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
          <button type="submit" disabled={busy}>
            Add field
          </button>
        </div>
        {SELECT_TYPES.has(newDataType) && (
          <input
            type="text"
            className="field-options-input"
            placeholder="Options, comma separated (e.g. Low, Medium, High)"
            value={newOptions}
            onChange={(e) => setNewOptions(e.target.value)}
          />
        )}
      </form>
    </div>
  );
}

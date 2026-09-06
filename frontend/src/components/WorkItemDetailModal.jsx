import { useState } from 'react';
import { api } from '../services/api.js';
import MarkdownEditor from './MarkdownEditor.jsx';
import CustomFieldValueInput from './CustomFieldValueInput.jsx';

function formatDate(value) {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

export default function WorkItemDetailModal({ projectId, item, items, customFieldDefs, onClose, onSaved }) {
  const [content, setContent] = useState(item.content ?? '');
  const [customFieldValues, setCustomFieldValues] = useState(item.customFields ?? {});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const fieldDefs = customFieldDefs ?? [];

  function setFieldValue(name, value) {
    setCustomFieldValues((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      // Drop values for fields that no longer exist (e.g. deleted since this
      // item was last loaded) instead of re-sending stale keys the backend
      // would reject.
      const validNames = new Set(fieldDefs.map((f) => f.name));
      const payloadCustomFields = Object.fromEntries(
        Object.entries(customFieldValues).filter(([name]) => validNames.has(name))
      );

      const updated = await api.updateWorkItem(projectId, item.id, {
        title: item.title,
        parentId: item.parentId ?? null,
        content,
        customFields: payloadCustomFields
      });
      onSaved(updated);
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel work-item-detail-panel" onClick={(e) => e.stopPropagation()}>
        <div className="work-item-detail-header">
          <div>
            <div className="board-card-key">{item.key}</div>
            <h3>{item.title}</h3>
          </div>
          <button type="button" className="link-button" onClick={onClose} disabled={saving}>
            Close
          </button>
        </div>

        {error && <p className="error-banner">{error}</p>}

        <dl className="work-item-detail-facts">
          <div>
            <dt>Type</dt>
            <dd>{item.typeName}</dd>
          </div>
          <div>
            <dt>State</dt>
            <dd>{item.stateName}</dd>
          </div>
          <div>
            <dt>Parent</dt>
            <dd>{item.parentKey ? `${item.parentKey} — ${item.parentTitle}` : '—'}</dd>
          </div>
          <div>
            <dt>Child items</dt>
            <dd>{item.childCount}</dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd>{formatDate(item.createdAt)}</dd>
          </div>
          <div>
            <dt>Updated</dt>
            <dd>{formatDate(item.updatedAt)}</dd>
          </div>
        </dl>

        <label className="field-label" htmlFor="work-item-description">
          Description
        </label>
        <MarkdownEditor
          value={content}
          onChange={setContent}
          placeholder="Add a description… (Markdown supported)"
          disabled={saving}
          items={items}
        />

        {fieldDefs.length > 0 && (
          <div className="work-item-detail-custom-fields">
            <h4>Custom fields</h4>
            {fieldDefs.map((field) => (
              <div key={field.id} className="custom-field-row">
                {field.dataType !== 'BOOLEAN' && <label className="field-label">{field.name}</label>}
                <CustomFieldValueInput
                  field={field}
                  value={customFieldValues[field.name] ?? null}
                  onChange={(value) => setFieldValue(field.name, value)}
                  disabled={saving}
                  items={items}
                />
              </div>
            ))}
          </div>
        )}

        <div className="work-item-detail-actions">
          <button type="button" className="secondary-button" onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button type="button" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  );
}

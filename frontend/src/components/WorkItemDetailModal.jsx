import { useState } from 'react';
import { api } from '../services/api.js';
import MarkdownEditor from './MarkdownEditor.jsx';

export default function WorkItemDetailModal({ projectId, item, onClose, onSaved }) {
  const [content, setContent] = useState(item.content ?? '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      const updated = await api.updateWorkItem(projectId, item.id, {
        title: item.title,
        parentId: item.parentId ?? null,
        content
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

        <label className="field-label" htmlFor="work-item-description">
          Description
        </label>
        <MarkdownEditor
          value={content}
          onChange={setContent}
          placeholder="Add a description… (Markdown supported)"
          disabled={saving}
        />

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

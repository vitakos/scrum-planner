import { useRef } from 'react';

// ChatPromptInput (AISC-93, AISC-102): a textarea with submit-on-Enter,
// that no-ops when the input is empty or whitespace-only.
// AISC-102 adds file attachment control.
export default function ChatPromptInput({ value, onChange, onSubmit, attachments = [], onAttachmentsChange }) {
  const fileInputRef = useRef(null);

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  }

  function handleSubmit() {
    const trimmedValue = value.trim();
    if (trimmedValue || attachments.length > 0) {
      onSubmit(trimmedValue, attachments);
      // Clear attachments after submit (caller will reset the input)
      if (onAttachmentsChange) {
        onAttachmentsChange([]);
      }
    }
  }

  function handleFileSelect(e) {
    const files = Array.from(e.target.files || []);
    const newAttachments = files.map((file) => ({
      id: Date.now().toString() + Math.random(),
      name: file.name,
      size: file.size,
      file, // Keep the File object for upload
    }));
    if (onAttachmentsChange) {
      onAttachmentsChange([...attachments, ...newAttachments]);
    }
    // Reset the input so the user can select the same file again
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }

  function handleRemoveAttachment(attachmentId) {
    if (onAttachmentsChange) {
      onAttachmentsChange(attachments.filter((a) => a.id !== attachmentId));
    }
  }

  return (
    <div className="chat-prompt-input">
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Type a message... (Enter to send)"
        aria-label="Chat message input"
        rows="3"
      />
      {attachments.length > 0 && (
        <div className="chat-attachments">
          {attachments.map((attachment) => (
            <div key={attachment.id} className="chat-attachment-chip">
              <span className="chat-attachment-name">{attachment.name}</span>
              <button
                type="button"
                className="chat-attachment-remove"
                onClick={() => handleRemoveAttachment(attachment.id)}
                aria-label={`Remove ${attachment.name}`}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}
      <div className="chat-prompt-actions">
        <input
          ref={fileInputRef}
          type="file"
          multiple
          onChange={handleFileSelect}
          aria-label="Attach files"
          hidden
          id="chat-file-input"
        />
        <label htmlFor="chat-file-input" className="chat-file-button">
          📎 Attach Files
        </label>
        <button type="button" onClick={handleSubmit} className="chat-prompt-submit">
          Send
        </button>
      </div>
    </div>
  );
}

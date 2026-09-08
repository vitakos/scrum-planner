import { useEffect, useRef, useState } from 'react';
import ChatMessageList from './ChatMessageList.jsx';
import ChatPromptInput from './ChatPromptInput.jsx';
import { api } from '../services/api.js';

// The "Intake Request" entry point (AISC-14): a button next to Backlog/Configure
// that opens a chat-style popover scoped to the current project. The popover
// itself only has placeholder chrome for now — the actual chat-driven intake
// conversation is built in later stories under this Feature (AISC-5).
//
// The popover stays mounted at all times (toggled via the `hidden` attribute,
// not conditional rendering) so its internal state — the draft input and any
// messages started — survives a close/reopen cycle without a backend call
// (AISC-89). Rendering one instance per project (see App.jsx, which keys this
// component by the selected project's id) keeps that state scoped per-project.
export default function IntakeRequestPopover({ project }) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState('');
  const [messages, setMessages] = useState([]);
  const [attachments, setAttachments] = useState([]);
  const rootRef = useRef(null);

  // Hydrate session messages from backend on open (AISC-103)
  useEffect(() => {
    if (!open) return undefined;

    // Load messages from backend session if this is the first open
    async function loadSession() {
      if (messages.length === 0) {
        try {
          const session = await api.getIntakeSession(project.id);
          if (session && session.messages && session.messages.length > 0) {
            setMessages(session.messages);
          }
        } catch (error) {
          // Intake session endpoint may not exist yet in backend (AISC-97-100)
          // Silently ignore for now; messages will use local state
          console.debug('Intake session not yet available:', error.message);
        }
      }
    }

    loadSession();

    function handleClickOutside(e) {
      if (rootRef.current && !rootRef.current.contains(e.target)) {
        setOpen(false);
      }
    }
    function handleKeyDown(e) {
      if (e.key === 'Escape') setOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [open, messages.length, project.id]);

  return (
    <div className="intake-request" ref={rootRef}>
      <button
        type="button"
        className="intake-request-trigger"
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="true"
        aria-expanded={open}
      >
        Intake Request
      </button>

      <div className="intake-request-popover" hidden={!open} role="dialog" aria-label={`Intake Request — ${project.key}`}>
        <div className="intake-request-popover-header">
          <span>Intake Request — {project.key}</span>
          <button
            type="button"
            className="intake-request-popover-close"
            aria-label="Close"
            onClick={() => setOpen(false)}
          >
            ×
          </button>
        </div>
        <div className="intake-request-popover-body">
          {messages.length === 0 ? (
            <p className="intake-request-placeholder">
              Start a conversation about your change request.
            </p>
          ) : (
            <ChatMessageList messages={messages} />
          )}
        </div>
        <ChatPromptInput
          value={draft}
          onChange={setDraft}
          attachments={attachments}
          onAttachmentsChange={setAttachments}
          onSubmit={(text, submittedAttachments) => {
            const newMessage = {
              id: Date.now().toString(),
              sender: 'You',
              text,
              timestamp: new Date(),
              attachments: submittedAttachments.map((att) => ({
                id: att.id,
                name: att.name,
                size: att.size,
              })),
            };
            setMessages((prev) => [...prev, newMessage]);
            setDraft('');
            setAttachments([]);
          }}
        />
      </div>
    </div>
  );
}

import { useEffect, useRef, useState } from 'react';

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
  const rootRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
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
  }, [open]);

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
          <p className="intake-request-placeholder">
            Chat-driven intake for this project is coming soon.
          </p>
        </div>
        <div className="intake-request-popover-input">
          <input
            type="text"
            placeholder="Describe what you need…"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            aria-label="Intake Request message"
            disabled
          />
        </div>
      </div>
    </div>
  );
}

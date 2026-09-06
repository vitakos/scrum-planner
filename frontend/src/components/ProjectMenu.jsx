import { useEffect, useRef, useState } from 'react';

export default function ProjectMenu({ projects, selectedProject, onSelectProject, onAddProject }) {
  const [open, setOpen] = useState(false);
  const [switching, setSwitching] = useState(false);
  const rootRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    function handleClickOutside(e) {
      if (rootRef.current && !rootRef.current.contains(e.target)) {
        closeAll();
      }
    }
    function handleKeyDown(e) {
      if (e.key === 'Escape') closeAll();
    }
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function closeAll() {
    setOpen(false);
    setSwitching(false);
  }

  return (
    <div className="project-menu" ref={rootRef}>
      <button
        type="button"
        className="project-menu-trigger"
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="true"
        aria-expanded={open}
      >
        <span className="project-menu-icon" aria-hidden="true">
          ☰
        </span>
        <span className="project-badge">
          {selectedProject.key} — {selectedProject.name}
        </span>
      </button>

      {open && (
        <div className="project-menu-dropdown">
          {!switching ? (
            <>
              <button
                type="button"
                className="project-menu-item"
                onClick={() => {
                  closeAll();
                  onAddProject();
                }}
              >
                Add Project
              </button>
              <button
                type="button"
                className="project-menu-item"
                onClick={() => setSwitching(true)}
              >
                Switch Project
                <span className="project-menu-item-arrow" aria-hidden="true">
                  ›
                </span>
              </button>
            </>
          ) : (
            <>
              <button type="button" className="project-menu-item project-menu-back" onClick={() => setSwitching(false)}>
                <span aria-hidden="true">‹</span> Back
              </button>
              <ul className="project-menu-list">
                {projects.map((p) => (
                  <li key={p.id}>
                    <button
                      type="button"
                      className={p.id === selectedProject.id ? 'project-menu-item active' : 'project-menu-item'}
                      onClick={() => {
                        closeAll();
                        onSelectProject(p.id);
                      }}
                    >
                      {p.key} — {p.name}
                      {p.id === selectedProject.id && <span aria-hidden="true">✓</span>}
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </div>
  );
}

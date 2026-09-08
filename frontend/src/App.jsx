import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from './services/api.js';
import ProjectWizardPage from './pages/ProjectWizardPage.jsx';
import ConfigurePage from './pages/ConfigurePage.jsx';
import BacklogPage from './pages/BacklogPage.jsx';
import ProjectMenu from './components/ProjectMenu.jsx';
import IntakeRequestPopover from './components/IntakeRequestPopover.jsx';

// Matches a work item key such as "SPAI-1": project key + '-' + sequence
// (see docs/backlog-data-model.md). Used to recognize internal links —
// both the "/SPAI-1" route and links generated from "$SPAI-1" references
// typed in the rich text editor (see MarkdownEditor's ITEM_LINK_PATTERN) —
// so real navigation away from the app is left alone.
const ITEM_LINK_PATH = /^\/([A-Za-z][A-Za-z0-9]*-\d+)$/;

export default function App() {
  const { itemKey } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [projects, setProjects] = useState([]);
  const [workItemTypes, setWorkItemTypes] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [activeTab, setActiveTab] = useState('backlog');
  const [showAddProject, setShowAddProject] = useState(false);
  const [focusItemKey, setFocusItemKey] = useState(null);

  useEffect(() => {
    loadInitialData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Resolves the "/:itemKey" route (a shared link, a browser back/forward,
  // or a click on a "$SPAI-1"-style reference — see the click handler below)
  // to the project + item it points at, so the right project/tab is shown
  // and BacklogPage can open that item's detail view once its data loads.
  useEffect(() => {
    if (!itemKey) {
      setFocusItemKey(null);
      return;
    }
    let cancelled = false;
    api
      .getWorkItemByKey(itemKey)
      .then((item) => {
        if (cancelled) return;
        setSelectedProjectId(item.projectId);
        setActiveTab('backlog');
        setFocusItemKey(item.key);
      })
      .catch(() => {
        if (!cancelled) navigate('/', { replace: true });
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [itemKey]);

  // Rich text (description / RICH_TEXT custom fields) renders "$SPAI-1"
  // references as plain anchors pointing at "/SPAI-1" (see MarkdownEditor).
  // Intercept clicks on those so they navigate client-side instead of
  // reloading the page; anything else (a real external link) is untouched.
  useEffect(() => {
    function handleClick(e) {
      const anchor = e.target.closest('a[href]');
      if (!anchor) return;
      const href = anchor.getAttribute('href');
      if (href && ITEM_LINK_PATH.test(href)) {
        e.preventDefault();
        navigate(href);
      }
    }
    document.addEventListener('click', handleClick);
    return () => document.removeEventListener('click', handleClick);
  }, [navigate]);

  async function loadInitialData() {
    setLoading(true);
    setError(null);
    try {
      const [projectList, typeList] = await Promise.all([
        api.listProjects(),
        api.listWorkItemTypes()
      ]);
      setProjects(projectList);
      setWorkItemTypes(typeList);
      setSelectedProjectId((current) => current ?? projectList[0]?.id ?? null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function handleProjectCreated(project) {
    setProjects((prev) => [...prev, project]);
    setSelectedProjectId(project.id);
    setShowAddProject(false);
  }

  function handleProjectUpdated(project) {
    setProjects((prev) => prev.map((p) => (p.id === project.id ? project : p)));
  }

  function handleItemOpened(item) {
    navigate(`/${item.key}`);
  }

  function handleItemClosed() {
    setFocusItemKey(null);
    navigate('/');
  }

  if (loading) {
    return <div className="page-center">Loading…</div>;
  }

  if (error) {
    return (
      <div className="page-center">
        <p className="error-banner">Couldn't reach the backend: {error}</p>
        <button onClick={loadInitialData}>Retry</button>
      </div>
    );
  }

  if (projects.length === 0) {
    return <ProjectWizardPage workItemTypes={workItemTypes} onCreated={handleProjectCreated} />;
  }

  const selectedProject = projects.find((p) => p.id === selectedProjectId) ?? projects[0];

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header-left">
          <span className="app-title">Scrum Planner</span>
          <nav className="tab-nav">
            <button
              className={activeTab === 'backlog' ? 'tab-nav-item active' : 'tab-nav-item'}
              onClick={() => setActiveTab('backlog')}
            >
              Backlog
            </button>
            <button
              className={activeTab === 'configure' ? 'tab-nav-item active' : 'tab-nav-item'}
              onClick={() => setActiveTab('configure')}
            >
              Configure
            </button>
          </nav>
          <IntakeRequestPopover key={selectedProject.id} project={selectedProject} />
        </div>
        <ProjectMenu
          projects={projects}
          selectedProject={selectedProject}
          onSelectProject={setSelectedProjectId}
          onAddProject={() => setShowAddProject(true)}
        />
      </header>
      <main>
        {activeTab === 'backlog' ? (
          <BacklogPage
            key={selectedProject.id}
            project={selectedProject}
            focusItemKey={focusItemKey}
            onItemOpened={handleItemOpened}
            onItemClosed={handleItemClosed}
          />
        ) : (
          <ConfigurePage
            key={selectedProject.id}
            project={selectedProject}
            onProjectUpdated={handleProjectUpdated}
          />
        )}
      </main>

      {showAddProject && (
        <div className="modal-overlay" onClick={() => setShowAddProject(false)}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <ProjectWizardPage
              workItemTypes={workItemTypes}
              onCreated={handleProjectCreated}
              onCancel={() => setShowAddProject(false)}
            />
          </div>
        </div>
      )}
    </div>
  );
}

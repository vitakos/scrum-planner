import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../services/api.js';
import WorkItemBoard from '../components/WorkItemBoard.jsx';
import BacklogTreeView from '../components/BacklogTreeView.jsx';
import WorkItemDetailModal from '../components/WorkItemDetailModal.jsx';
import { parentTypesFor } from '../utils/workItemHierarchy.js';

export default function BacklogPage({ project, focusItemKey, onItemOpened, onItemClosed }) {
  const [workflows, setWorkflows] = useState([]);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [viewMode, setViewMode] = useState('backlog'); // 'backlog' (tree) | 'sprint' (kanban board)
  const [activeType, setActiveType] = useState(null);
  const [parentFilter, setParentFilter] = useState(null);
  const [presetParentId, setPresetParentId] = useState(null);
  const [customFieldCatalogs, setCustomFieldCatalogs] = useState([]);
  const [itemsLoading, setItemsLoading] = useState(false);

  // "Hide closed items" defaults to on, and is persisted in the URL's
  // ?hideClosed= query param so a page refresh or direct navigation to this
  // URL keeps the user's choice (AISC-XXX bug fix). Only the literal string
  // "false" turns it off; anything else (including the param being absent)
  // means on.
  const [searchParams, setSearchParams] = useSearchParams();
  const [hideClosed, setHideClosedState] = useState(() => searchParams.get('hideClosed') !== 'false');

  function setHideClosed(value) {
    setHideClosedState(value);
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.set('hideClosed', String(value));
        return next;
      },
      { replace: true }
    );
  }
  // Tree view's popover: editing an existing item, or creating a new one
  // (root-level or as a child) — see WorkItemDetailModal's mode prop.
  const [modal, setModal] = useState(null);

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [project.id]);

  // Re-fetch just the work item list (not workflows/custom fields) whenever
  // the "Hide closed items" toggle changes, so the tree/board rebuild from a
  // freshly filtered response rather than filtering client-side.
  useEffect(() => {
    if (loading) return;
    refetchItems();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hideClosed]);

  async function loadAll() {
    setLoading(true);
    setError(null);
    try {
      const [workflowList, itemList, fieldCatalogs] = await Promise.all([
        api.listWorkflows(project.id),
        api.listWorkItems(project.id, undefined, hideClosed),
        api.listCustomFields(project.id)
      ]);
      setWorkflows(workflowList);
      setItems(itemList);
      setCustomFieldCatalogs(fieldCatalogs);
      setActiveType((current) => current ?? workflowList[0]?.workItemType ?? null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function refetchItems() {
    setItemsLoading(true);
    setError(null);
    try {
      const itemList = await api.listWorkItems(project.id, undefined, hideClosed);
      setItems(itemList);
    } catch (err) {
      setError(err.message);
    } finally {
      setItemsLoading(false);
    }
  }

  function upsertItem(updated) {
    setItems((prev) => {
      const exists = prev.some((i) => i.id === updated.id);
      return exists ? prev.map((i) => (i.id === updated.id ? updated : i)) : [...prev, updated];
    });
    setModal((current) => (current?.mode === 'edit' && current.item.id === updated.id ? { ...current, item: updated } : current));
  }

  function removeItem(id) {
    setItems((prev) => prev.filter((i) => i.id !== id));
    setModal((current) => (current?.mode === 'edit' && current.item.id === id ? null : current));
  }

  function selectType(type) {
    setActiveType(type);
    setParentFilter(null);
    setPresetParentId(null);
  }

  function selectView(mode) {
    setViewMode(mode);
    setParentFilter(null);
    setPresetParentId(null);
  }

  function handleAddChild(item, childType) {
    setActiveType(childType);
    setPresetParentId(item.id);
    setParentFilter(null);
  }

  function handleDrillDown(item, childType) {
    setActiveType(childType);
    setParentFilter({ id: item.id, key: item.key, title: item.title });
    setPresetParentId(null);
  }

  function openTreeDetail(item) {
    setModal({ mode: 'edit', item });
    onItemOpened?.(item);
  }

  function openTreeAddChild(item, childType) {
    setModal({ mode: 'create', parent: item, type: childType });
  }

  function openTreeAddRoot(type) {
    setModal({ mode: 'create', parent: null, type });
  }

  function closeModal() {
    const wasEdit = modal?.mode === 'edit';
    setModal(null);
    if (wasEdit) onItemClosed?.();
  }

  // A focused item (from the "/:itemKey" route, see App) may belong to a
  // work item type other than the one currently active — switch to it so
  // WorkItemBoard actually has the item in its `items` list to open. The
  // tree view shows every type at once, so it only needs the item itself.
  useEffect(() => {
    if (!focusItemKey) return;
    const target = items.find((i) => i.key === focusItemKey);
    if (!target) return;
    if (viewMode === 'sprint' && target.type !== activeType) {
      setActiveType(target.type);
      setParentFilter(null);
    }
    if (viewMode === 'backlog') {
      setModal({ mode: 'edit', item: target });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focusItemKey, items, viewMode]);

  if (loading) return <p className="page-center">Loading backlog…</p>;
  if (error) return <p className="error-banner">{error}</p>;

  const activeWorkflow = workflows.find((w) => w.workItemType === activeType);
  const itemsForType = items.filter(
    (i) => i.type === activeType && (!parentFilter || i.parentId === parentFilter.id)
  );
  const typeNameByCode = Object.fromEntries(workflows.map((w) => [w.workItemType, w.workItemTypeName]));

  return (
    <div className="configure-page">
      <div className="view-toggle">
        <button
          type="button"
          className={viewMode === 'backlog' ? 'view-toggle-item active' : 'view-toggle-item'}
          onClick={() => selectView('backlog')}
        >
          Backlog
        </button>
        <button
          type="button"
          className={viewMode === 'sprint' ? 'view-toggle-item active' : 'view-toggle-item'}
          onClick={() => selectView('sprint')}
        >
          Sprint board
        </button>
      </div>

      {viewMode === 'backlog' ? (
        <>
          <BacklogTreeView
            projectId={project.id}
            workflows={workflows}
            items={items}
            onItemChanged={upsertItem}
            onItemDeleted={removeItem}
            openDetail={openTreeDetail}
            onAddChild={openTreeAddChild}
            onAddRoot={openTreeAddRoot}
            parentTypesFor={parentTypesFor}
            hideClosed={hideClosed}
            onToggleHideClosed={setHideClosed}
          />
          {itemsLoading && <p className="page-center">Refreshing…</p>}
          {modal?.mode === 'edit' && (
            <WorkItemDetailModal
              projectId={project.id}
              mode="edit"
              item={modal.item}
              items={items}
              customFieldDefs={
                customFieldCatalogs.find((c) => c.workItemType === modal.item.type)?.customFields ?? []
              }
              onClose={closeModal}
              onSaved={upsertItem}
            />
          )}
          {modal?.mode === 'create' && (
            <WorkItemDetailModal
              projectId={project.id}
              mode="create"
              createType={modal.type}
              createTypeName={typeNameByCode[modal.type] ?? modal.type}
              parent={modal.parent}
              items={items}
              customFieldDefs={
                customFieldCatalogs.find((c) => c.workItemType === modal.type)?.customFields ?? []
              }
              onClose={closeModal}
              onSaved={upsertItem}
            />
          )}
        </>
      ) : (
        <div className="configure-layout">
          <nav className="type-nav">
            {workflows.map((w) => (
              <button
                key={w.workItemType}
                className={w.workItemType === activeType ? 'type-nav-item active' : 'type-nav-item'}
                onClick={() => selectType(w.workItemType)}
              >
                {w.workItemTypeName}
                <span className="type-nav-count">{items.filter((i) => i.type === w.workItemType).length}</span>
              </button>
            ))}
          </nav>

          <section className="workflow-panel">
            {parentFilter && (
              <div className="filter-banner">
                Showing children of <strong>{parentFilter.key}</strong> — {parentFilter.title}
                <button type="button" className="link-button" onClick={() => setParentFilter(null)}>
                  Clear filter
                </button>
              </div>
            )}

            {activeWorkflow ? (
              <WorkItemBoard
                key={activeWorkflow.workItemType}
                projectId={project.id}
                workflow={activeWorkflow}
                workflows={workflows}
                items={itemsForType}
                allItems={items}
                customFieldCatalogs={customFieldCatalogs}
                presetParentId={presetParentId}
                focusItemKey={focusItemKey}
                onItemChanged={upsertItem}
                onItemDeleted={removeItem}
                onAddChild={handleAddChild}
                onDrillDown={handleDrillDown}
                onItemOpened={onItemOpened}
                onItemClosed={onItemClosed}
              />
            ) : (
              <p>No work item types configured for this project.</p>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

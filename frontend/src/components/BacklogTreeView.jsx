import { useMemo, useState } from 'react';
import { api } from '../services/api.js';
import { childTypesFor } from '../utils/workItemHierarchy.js';
import { resolveStateColor } from '../utils/stateColors.js';

export default function BacklogTreeView({
  projectId,
  workflows,
  items,
  onItemChanged,
  onItemDeleted,
  openDetail,
  onAddChild,
  onAddRoot,
  parentTypesFor,
  hideClosed,
  onToggleHideClosed
}) {
  const [titleFilter, setTitleFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [collapsed, setCollapsed] = useState(() => new Set());
  const [openMenuId, setOpenMenuId] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState(null);

  const [addingRoot, setAddingRoot] = useState(false);
  const [rootType, setRootType] = useState(workflows[0]?.workItemType ?? '');

  const typeNameByCode = useMemo(
    () => Object.fromEntries((workflows ?? []).map((w) => [w.workItemType, w.workItemTypeName])),
    [workflows]
  );
  const configuredTypes = useMemo(() => new Set((workflows ?? []).map((w) => w.workItemType)), [workflows]);

  // Depth of each configured type within the hierarchy (root = 0), so the
  // type filter can offer just the "top 3 levels" as the spec asks.
  const typeDepth = useMemo(() => {
    const depths = {};
    function depthOf(type) {
      if (type in depths) return depths[type];
      const parents = parentTypesFor(type).filter((p) => configuredTypes.has(p));
      const d = parents.length === 0 ? 0 : 1 + Math.min(...parents.map(depthOf));
      depths[type] = d;
      return d;
    }
    for (const w of workflows ?? []) depthOf(w.workItemType);
    return depths;
  }, [workflows, configuredTypes, parentTypesFor]);

  const topLevelTypes = useMemo(
    () => (workflows ?? []).filter((w) => (typeDepth[w.workItemType] ?? 99) < 3),
    [workflows, typeDepth]
  );

  // Newest-created-first ordering: every list derived below (top-level
  // roots and each parent's children list) is built from this copy so new
  // items appear at the top without needing a page reload.
  const sortedItems = useMemo(
    () => [...items].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)),
    [items]
  );

  const byId = useMemo(() => new Map(sortedItems.map((i) => [i.id, i])), [sortedItems]);
  const childrenOf = useMemo(() => {
    const map = new Map();
    for (const item of sortedItems) {
      const parentKey = item.parentId && byId.has(item.parentId) ? item.parentId : null;
      if (!map.has(parentKey)) map.set(parentKey, []);
      map.get(parentKey).push(item);
    }
    return map;
  }, [sortedItems, byId]);

  // Roots for the tree: normally items with no (known) parent. When a type
  // filter is active, items of that type become the roots instead, so the
  // tree shows "that type and the ones under it" as the spec asks for.
  const roots = useMemo(() => {
    if (typeFilter) return sortedItems.filter((i) => i.type === typeFilter);
    return childrenOf.get(null) ?? [];
  }, [sortedItems, childrenOf, typeFilter]);

  // When a title filter is active, keep matching items plus their ancestors
  // (within the current root set) so the tree stays connected and shows
  // where each match lives.
  const keepIds = useMemo(() => {
    if (!titleFilter.trim()) return null;
    const needle = titleFilter.trim().toLowerCase();
    const keep = new Set();
    const rootIds = new Set(roots.map((r) => r.id));
    for (const item of sortedItems) {
      if (!item.title.toLowerCase().includes(needle)) continue;
      let current = item;
      // Walk up to the nearest tree root we're rendering from, marking the
      // whole chain as visible.
      while (current) {
        keep.add(current.id);
        if (rootIds.has(current.id)) break;
        current = current.parentId ? byId.get(current.parentId) : null;
      }
      keep.add(item.id);
    }
    return keep;
  }, [titleFilter, sortedItems, byId, roots]);

  function isVisible(item) {
    return !keepIds || keepIds.has(item.id);
  }

  function toggleCollapsed(id) {
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function handleMove(item, transitionId) {
    setOpenMenuId(null);
    setBusyId(item.id);
    setError(null);
    try {
      const updated = await api.applyWorkItemTransition(projectId, item.id, { transitionId });
      onItemChanged(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(item) {
    setOpenMenuId(null);
    setBusyId(item.id);
    setError(null);
    try {
      await api.deleteWorkItem(projectId, item.id);
      onItemDeleted(item.id);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  function handleAddRootSubmit(e) {
    e.preventDefault();
    if (!rootType) return;
    onAddRoot(rootType);
    setAddingRoot(false);
  }

  function renderRow(item, depth) {
    if (!isVisible(item)) return null;
    const children = (childrenOf.get(item.id) ?? []).filter(isVisible);
    const hasChildren = children.length > 0;
    const isCollapsed = collapsed.has(item.id);
    const childTypes = childTypesFor(item.type).filter((ct) => configuredTypes.has(ct));
    const menuOpen = openMenuId === item.id;

    return (
      <div key={item.id}>
        <div className="tree-row" style={{ paddingLeft: depth * 22 }}>
          <span className="tree-row-toggle">
            {hasChildren ? (
              <button
                type="button"
                className="tree-toggle"
                onClick={() => toggleCollapsed(item.id)}
                aria-label={isCollapsed ? 'Expand' : 'Collapse'}
              >
                {isCollapsed ? '▸' : '▾'}
              </button>
            ) : (
              <span className="tree-toggle-spacer" />
            )}
          </span>
          <span className="tree-cell tree-cell-id">{item.key}</span>
          <span
            className="tree-cell tree-cell-title"
            role="button"
            tabIndex={0}
            onClick={() => openDetail(item)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') openDetail(item);
            }}
          >
            <span className="tree-type-badge">{typeNameByCode[item.type] ?? item.type}</span>
            {item.title}
            {item.childCount > 0 && (
              <span className="tree-child-count">
                {item.childCount} {item.childCount === 1 ? 'child' : 'children'}
              </span>
            )}
          </span>
          <span className="tree-cell tree-cell-status">
            <span className="category-dot" style={{ backgroundColor: resolveStateColor({ category: item.stateCategory }) }} />
            {item.stateName}
          </span>
          <span className="tree-cell tree-cell-menu">
            <button
              type="button"
              className="tree-menu-button"
              disabled={busyId === item.id}
              onClick={(e) => {
                e.stopPropagation();
                setOpenMenuId(menuOpen ? null : item.id);
              }}
              aria-label="More actions"
            >
              …
            </button>
            {menuOpen && (
              <div className="tree-menu-dropdown" onMouseLeave={() => setOpenMenuId(null)}>
                {item.availableTransitions.length > 0 && (
                  <>
                    {item.availableTransitions.map((t) => (
                      <button
                        key={t.id}
                        type="button"
                        className="tree-menu-item"
                        onClick={() => handleMove(item, t.id)}
                      >
                        {t.name}
                      </button>
                    ))}
                    <div className="tree-menu-divider" />
                  </>
                )}
                {childTypes.length > 0 ? (
                  childTypes.map((ct) => (
                    <button
                      key={ct}
                      type="button"
                      className="tree-menu-item"
                      onClick={() => {
                        setOpenMenuId(null);
                        onAddChild(item, ct);
                      }}
                    >
                      + Add {(typeNameByCode[ct] ?? ct).toLowerCase()}
                    </button>
                  ))
                ) : (
                  <span className="tree-menu-empty">No child types</span>
                )}
                <div className="tree-menu-divider" />
                <button
                  type="button"
                  className="tree-menu-item tree-menu-item-danger"
                  disabled={busyId === item.id}
                  onClick={() => handleDelete(item)}
                >
                  Delete
                </button>
              </div>
            )}
          </span>
        </div>

        {!isCollapsed && children.map((child) => renderRow(child, depth + 1))}
      </div>
    );
  }

  const visibleRoots = roots.filter(isVisible);

  return (
    <div className="tree-view" onClick={() => setOpenMenuId(null)}>
      <div className="tree-toolbar">
        <input
          type="text"
          placeholder="Filter by title..."
          value={titleFilter}
          onChange={(e) => setTitleFilter(e.target.value)}
          className="tree-filter-input"
        />
        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} className="tree-type-select">
          <option value="">All types</option>
          {topLevelTypes.map((w) => (
            <option key={w.workItemType} value={w.workItemType}>
              {w.workItemTypeName}
            </option>
          ))}
        </select>

        <label className={hideClosed ? 'tree-toggle-control active' : 'tree-toggle-control'}>
          <input
            type="checkbox"
            checked={hideClosed}
            onChange={(e) => onToggleHideClosed?.(e.target.checked)}
          />
          Hide closed items
        </label>

        <div className="tree-toolbar-spacer" />

        {addingRoot ? (
          <form className="inline-form inline-form-tight" onClick={(e) => e.stopPropagation()} onSubmit={handleAddRootSubmit}>
            <select value={rootType} onChange={(e) => setRootType(e.target.value)}>
              {(workflows ?? []).map((w) => (
                <option key={w.workItemType} value={w.workItemType}>
                  {w.workItemTypeName}
                </option>
              ))}
            </select>
            <button type="submit">Add</button>
            <button type="button" className="secondary-button" onClick={() => setAddingRoot(false)}>
              Cancel
            </button>
          </form>
        ) : (
          <button type="button" onClick={(e) => { e.stopPropagation(); setAddingRoot(true); }}>
            + Add item
          </button>
        )}
      </div>

      {error && <p className="error-banner">{error}</p>}

      <div className="tree-table">
        <div className="tree-row tree-row-header">
          <span className="tree-row-toggle" />
          <span className="tree-cell tree-cell-id">ID</span>
          <span className="tree-cell tree-cell-title">Title</span>
          <span className="tree-cell tree-cell-status">Status</span>
          <span className="tree-cell tree-cell-menu" />
        </div>
        {visibleRoots.length === 0 ? (
          <p className="tree-empty">No items match this filter.</p>
        ) : (
          visibleRoots.map((item) => renderRow(item, 0))
        )}
      </div>
    </div>
  );
}

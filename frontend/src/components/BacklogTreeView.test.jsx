import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import BacklogTreeView from './BacklogTreeView.jsx';
import { parentTypesFor } from '../utils/workItemHierarchy.js';

vi.mock('../services/api.js', () => ({
  api: {
    applyWorkItemTransition: vi.fn(),
    deleteWorkItem: vi.fn()
  }
}));

const WORKFLOWS = [
  { workItemType: 'epic', workItemTypeName: 'Epic' },
  { workItemType: 'user_story', workItemTypeName: 'User Story' },
  { workItemType: 'task', workItemTypeName: 'Task' }
];

function workItem(overrides) {
  return {
    id: 'id',
    key: 'AISC-1',
    projectId: 'project-1',
    type: 'task',
    typeName: 'Task',
    title: 'Item',
    content: null,
    customFields: {},
    stateId: 'state-1',
    stateName: 'To Do',
    stateCategory: 'to_do',
    parentId: null,
    parentKey: null,
    parentTitle: null,
    childCount: 0,
    availableTransitions: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides
  };
}

function renderTree(items) {
  return render(
    <BacklogTreeView
      projectId="project-1"
      workflows={WORKFLOWS}
      items={items}
      onItemChanged={vi.fn()}
      onItemDeleted={vi.fn()}
      openDetail={vi.fn()}
      onAddChild={vi.fn()}
      onAddRoot={vi.fn()}
      parentTypesFor={parentTypesFor}
      hideClosed={false}
      onToggleHideClosed={vi.fn()}
    />
  );
}

// Reads the rendered tree-row titles in DOM order, which corresponds to
// visual top-to-bottom order for this test's flat/nested fixtures. The title
// is rendered as a plain text node inside .tree-cell-title, alongside the
// type-badge and (optional) child-count elements, so pick that text node out
// directly rather than trying to strip surrounding text.
function renderedTitles() {
  return Array.from(document.querySelectorAll('.tree-row:not(.tree-row-header) .tree-cell-title')).map((el) => {
    const textNode = Array.from(el.childNodes).find((n) => n.nodeType === Node.TEXT_NODE && n.textContent.trim());
    return textNode ? textNode.textContent.trim() : '';
  });
}

describe('BacklogTreeView - newest-created-first ordering (AISC-77)', () => {
  it('renders the top-level (root) list with the most recently created item first', () => {
    const oldEpic = workItem({ id: 'epic-old', key: 'AISC-1', type: 'epic', typeName: 'Epic', title: 'Old Epic', createdAt: '2026-01-01T00:00:00Z' });
    const newEpic = workItem({ id: 'epic-new', key: 'AISC-2', type: 'epic', typeName: 'Epic', title: 'New Epic', createdAt: '2026-02-01T00:00:00Z' });
    const midEpic = workItem({ id: 'epic-mid', key: 'AISC-3', type: 'epic', typeName: 'Epic', title: 'Mid Epic', createdAt: '2026-01-15T00:00:00Z' });

    renderTree([oldEpic, newEpic, midEpic]);

    expect(renderedTitles()).toEqual(['New Epic', 'Mid Epic', 'Old Epic']);
  });

  it('renders a children list with the most recently created child first', () => {
    const story = workItem({
      id: 'story-1',
      key: 'AISC-10',
      type: 'user_story',
      typeName: 'User Story',
      title: 'Parent Story',
      childCount: 2,
      createdAt: '2026-01-01T00:00:00Z'
    });
    const oldTask = workItem({
      id: 'task-old',
      key: 'AISC-11',
      title: 'Old Task',
      parentId: story.id,
      parentKey: story.key,
      createdAt: '2026-01-05T00:00:00Z'
    });
    const newTask = workItem({
      id: 'task-new',
      key: 'AISC-12',
      title: 'New Task',
      parentId: story.id,
      parentKey: story.key,
      createdAt: '2026-01-20T00:00:00Z'
    });

    renderTree([story, oldTask, newTask]);

    expect(renderedTitles()).toEqual(['Parent Story', 'New Task', 'Old Task']);
  });

  it('shows a newly created item at the top of its list on re-render, without a reload', () => {
    const first = workItem({ id: 'epic-1', key: 'AISC-1', type: 'epic', typeName: 'Epic', title: 'First Epic', createdAt: '2026-01-01T00:00:00Z' });
    const { rerender } = renderTree([first]);
    expect(renderedTitles()).toEqual(['First Epic']);

    const second = workItem({ id: 'epic-2', key: 'AISC-2', type: 'epic', typeName: 'Epic', title: 'Second Epic', createdAt: '2026-01-02T00:00:00Z' });
    rerender(
      <BacklogTreeView
        projectId="project-1"
        workflows={WORKFLOWS}
        items={[first, second]}
        onItemChanged={vi.fn()}
        onItemDeleted={vi.fn()}
        openDetail={vi.fn()}
        onAddChild={vi.fn()}
        onAddRoot={vi.fn()}
        parentTypesFor={parentTypesFor}
        hideClosed={false}
        onToggleHideClosed={vi.fn()}
      />
    );

    expect(renderedTitles()).toEqual(['Second Epic', 'First Epic']);
  });
});

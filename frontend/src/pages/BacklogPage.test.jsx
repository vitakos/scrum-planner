import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BacklogPage from './BacklogPage.jsx';
import { api } from '../services/api.js';

vi.mock('../services/api.js', () => ({
  api: {
    listWorkflows: vi.fn(),
    listWorkItems: vi.fn(),
    listCustomFields: vi.fn()
  }
}));

const PROJECT = { id: 'project-1', key: 'AISC' };

const WORKFLOWS = [
  { workItemType: 'user_story', workItemTypeName: 'User Story' },
  { workItemType: 'task', workItemTypeName: 'Task' }
];

function workItem(overrides) {
  return {
    id: 'id',
    key: 'AISC-1',
    projectId: PROJECT.id,
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
    ...overrides
  };
}

// A parent Story with two children: one open, one Done/Closed — used to
// verify the "hide closed" filter respects nesting (AISC-76 Scenario 3).
const STORY = workItem({
  id: 'story-1',
  key: 'AISC-10',
  type: 'user_story',
  typeName: 'User Story',
  title: 'Story A',
  stateName: 'In Progress',
  stateCategory: 'in_progress',
  childCount: 2
});
const OPEN_TASK = workItem({
  id: 'task-open',
  key: 'AISC-11',
  title: 'Open Task',
  parentId: STORY.id,
  parentKey: STORY.key,
  stateName: 'To Do',
  stateCategory: 'to_do'
});
const DONE_TASK = workItem({
  id: 'task-done',
  key: 'AISC-12',
  title: 'Done Task',
  parentId: STORY.id,
  parentKey: STORY.key,
  stateName: 'Done',
  stateCategory: 'done'
});

const FULL_LIST = [STORY, OPEN_TASK, DONE_TASK];
const FILTERED_LIST = [STORY, OPEN_TASK];

beforeEach(() => {
  vi.clearAllMocks();
  api.listWorkflows.mockResolvedValue(WORKFLOWS);
  api.listCustomFields.mockResolvedValue([]);
});

async function renderBacklog() {
  render(<BacklogPage project={PROJECT} focusItemKey={null} onItemOpened={vi.fn()} onItemClosed={vi.fn()} />);
  await screen.findByText('Story A');
}

describe('BacklogPage - Hide closed items toggle (AISC-76)', () => {
  it('shows Done/Closed items by default, including as nested children', async () => {
    api.listWorkItems.mockResolvedValue(FULL_LIST);

    await renderBacklog();

    expect(screen.getByText('Open Task')).toBeInTheDocument();
    expect(screen.getByText('Done Task')).toBeInTheDocument();
    expect(api.listWorkItems).toHaveBeenCalledWith(PROJECT.id, undefined, false);
  });

  it('turning the toggle on re-fetches with excludeDoneCategory and hides Done/Closed items, including nested children', async () => {
    api.listWorkItems.mockResolvedValueOnce(FULL_LIST).mockResolvedValueOnce(FILTERED_LIST);

    await renderBacklog();

    const toggle = screen.getByRole('checkbox', { name: /hide closed items/i });
    await userEvent.click(toggle);

    await waitFor(() => expect(api.listWorkItems).toHaveBeenLastCalledWith(PROJECT.id, undefined, true));
    await waitFor(() => expect(screen.queryByText('Done Task')).not.toBeInTheDocument());
    expect(screen.getByText('Open Task')).toBeInTheDocument();
  });

  it('turning the toggle back off re-fetches without the filter and restores Done/Closed items', async () => {
    api.listWorkItems
      .mockResolvedValueOnce(FULL_LIST)
      .mockResolvedValueOnce(FILTERED_LIST)
      .mockResolvedValueOnce(FULL_LIST);

    await renderBacklog();

    const toggle = screen.getByRole('checkbox', { name: /hide closed items/i });
    await userEvent.click(toggle);
    await waitFor(() => expect(screen.queryByText('Done Task')).not.toBeInTheDocument());

    await userEvent.click(toggle);

    await waitFor(() => expect(api.listWorkItems).toHaveBeenLastCalledWith(PROJECT.id, undefined, false));
    await waitFor(() => expect(screen.getByText('Done Task')).toBeInTheDocument());
  });

  it('visibly indicates the toggle is active while it is on', async () => {
    api.listWorkItems.mockResolvedValueOnce(FULL_LIST).mockResolvedValueOnce(FILTERED_LIST);

    await renderBacklog();

    const toggle = screen.getByRole('checkbox', { name: /hide closed items/i });
    const label = toggle.closest('label');
    expect(label.className).not.toMatch(/active/);

    await userEvent.click(toggle);

    await waitFor(() => expect(label.className).toMatch(/active/));
    expect(toggle).toBeChecked();
  });
});

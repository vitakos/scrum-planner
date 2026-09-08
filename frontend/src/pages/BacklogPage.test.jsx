import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
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

async function renderBacklog(initialEntries = ['/']) {
  render(
    <MemoryRouter initialEntries={initialEntries}>
      <BacklogPage project={PROJECT} focusItemKey={null} onItemOpened={vi.fn()} onItemClosed={vi.fn()} />
    </MemoryRouter>
  );
  await screen.findByText('Story A');
}

describe('BacklogPage - Hide closed items toggle defaults to on (bug fix)', () => {
  it('hides Done/Closed items by default, with no query param present', async () => {
    api.listWorkItems.mockResolvedValue(FILTERED_LIST);

    await renderBacklog();

    expect(api.listWorkItems).toHaveBeenCalledWith(PROJECT.id, undefined, true);
    expect(screen.getByText('Open Task')).toBeInTheDocument();
    expect(screen.queryByText('Done Task')).not.toBeInTheDocument();

    const toggle = screen.getByRole('checkbox', { name: /hide closed items/i });
    expect(toggle).toBeChecked();
    expect(toggle.closest('label').className).toMatch(/active/);
  });

  it('turning the toggle off re-fetches without the filter and shows Done/Closed items, including nested children', async () => {
    api.listWorkItems.mockResolvedValueOnce(FILTERED_LIST).mockResolvedValueOnce(FULL_LIST);

    await renderBacklog();

    const toggle = screen.getByRole('checkbox', { name: /hide closed items/i });
    await userEvent.click(toggle);

    await waitFor(() => expect(api.listWorkItems).toHaveBeenLastCalledWith(PROJECT.id, undefined, false));
    await waitFor(() => expect(screen.getByText('Done Task')).toBeInTheDocument());
    expect(screen.getByText('Open Task')).toBeInTheDocument();
    expect(toggle).not.toBeChecked();
    expect(toggle.closest('label').className).not.toMatch(/active/);
  });

  it('turning the toggle back on re-fetches with the filter and hides Done/Closed items again', async () => {
    api.listWorkItems
      .mockResolvedValueOnce(FILTERED_LIST)
      .mockResolvedValueOnce(FULL_LIST)
      .mockResolvedValueOnce(FILTERED_LIST);

    await renderBacklog();

    const toggle = screen.getByRole('checkbox', { name: /hide closed items/i });
    await userEvent.click(toggle);
    await waitFor(() => expect(screen.getByText('Done Task')).toBeInTheDocument());

    await userEvent.click(toggle);

    await waitFor(() => expect(api.listWorkItems).toHaveBeenLastCalledWith(PROJECT.id, undefined, true));
    await waitFor(() => expect(screen.queryByText('Done Task')).not.toBeInTheDocument());
  });

  it('respects an explicit ?hideClosed=false in the URL, starting with the toggle off', async () => {
    api.listWorkItems.mockResolvedValue(FULL_LIST);

    await renderBacklog(['/?hideClosed=false']);

    expect(api.listWorkItems).toHaveBeenCalledWith(PROJECT.id, undefined, false);
    const toggle = screen.getByRole('checkbox', { name: /hide closed items/i });
    expect(toggle).not.toBeChecked();
    expect(screen.getByText('Done Task')).toBeInTheDocument();
  });
});

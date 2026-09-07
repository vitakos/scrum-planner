import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ConfigurePage from './ConfigurePage.jsx';
import { api } from '../services/api.js';

vi.mock('../services/api.js', () => ({
  api: {
    listWorkflows: vi.fn(),
    listCustomFields: vi.fn(),
    updateProject: vi.fn()
  }
}));

function baseProject(overrides = {}) {
  return {
    id: 'project-1',
    description: 'Test project',
    repositoryUrl: null,
    ...overrides
  };
}

beforeEach(() => {
  vi.clearAllMocks();
  api.listWorkflows.mockResolvedValue([]);
  api.listCustomFields.mockResolvedValue([]);
});

async function renderGeneralSection(project, onProjectUpdated = vi.fn()) {
  render(<ConfigurePage project={project} onProjectUpdated={onProjectUpdated} />);
  // Wait for the initial configuration load to settle, then switch to the General tab.
  const generalTab = await screen.findByRole('button', { name: 'General' });
  await userEvent.click(generalTab);
  return { onProjectUpdated };
}

describe('ConfigurePage - General section - repository URL (AISC-13)', () => {
  it('saves a valid repository URL, calls onProjectUpdated and updates the displayed value', async () => {
    const project = baseProject({ repositoryUrl: null });
    const updated = { ...project, repositoryUrl: 'https://github.com/org/repo' };
    api.updateProject.mockResolvedValue(updated);

    const { onProjectUpdated } = await renderGeneralSection(project);

    const input = screen.getByLabelText('Repository URL');
    await userEvent.clear(input);
    await userEvent.type(input, 'https://github.com/org/repo');
    await userEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => expect(onProjectUpdated).toHaveBeenCalledWith(updated));

    expect(api.updateProject).toHaveBeenCalledWith('project-1', {
      repositoryUrl: 'https://github.com/org/repo'
    });

    const link = await screen.findByRole('link', { name: 'https://github.com/org/repo' });
    expect(link).toHaveAttribute('href', 'https://github.com/org/repo');
    expect(screen.getByText(/Repository:/)).toBeInTheDocument();
  });

  it('shows an inline validation error on an invalid URL, keeps the typed value, and does not call onProjectUpdated', async () => {
    const project = baseProject({ repositoryUrl: 'https://github.com/org/already-saved' });
    api.updateProject.mockRejectedValue(new Error('repositoryUrl must be a valid http(s)/git/ssh URL'));

    const { onProjectUpdated } = await renderGeneralSection(project);

    const input = screen.getByLabelText('Repository URL');
    await userEvent.clear(input);
    await userEvent.type(input, 'not-a-valid-url');
    await userEvent.click(screen.getByRole('button', { name: /save/i }));

    expect(
      await screen.findByText('repositoryUrl must be a valid http(s)/git/ssh URL')
    ).toBeInTheDocument();

    expect(input).toHaveValue('not-a-valid-url');
    expect(onProjectUpdated).not.toHaveBeenCalled();

    const link = screen.getByRole('link', { name: 'https://github.com/org/already-saved' });
    expect(link).toHaveAttribute('href', 'https://github.com/org/already-saved');
  });

  it('shows the read-only Repository line on mount when repositoryUrl is already configured, and hides it when empty', async () => {
    const configured = baseProject({ repositoryUrl: 'https://github.com/org/repo' });
    await renderGeneralSection(configured);
    expect(screen.getByText(/Repository:/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'https://github.com/org/repo' })).toBeInTheDocument();

    vi.clearAllMocks();
    api.listWorkflows.mockResolvedValue([]);
    api.listCustomFields.mockResolvedValue([]);

    const empty = baseProject({ repositoryUrl: null });
    await renderGeneralSection(empty);
    expect(screen.queryByText(/Repository:/)).not.toBeInTheDocument();
  });
});

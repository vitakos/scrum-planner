import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import IntakeRequestPopover from './IntakeRequestPopover.jsx';
import { api } from '../services/api.js';

// AISC-19: covers the "Generate Gap Analysis" trigger in IntakeRequestPopover — the
// call it makes, and its loading/error states. Kept separate from
// IntakeRequestPopover.test.jsx (AISC-14/AISC-89) since those tests rely on the
// unmocked api module failing silently; here the api module is mocked outright so the
// trigger's behavior is deterministic.
vi.mock('../services/api.js', () => ({
  api: {
    getIntakeSession: vi.fn(),
    generateGapAnalysis: vi.fn(),
    answerGapAnalysisFollowUp: vi.fn(),
  },
}));

function baseProject(overrides = {}) {
  return {
    id: 'project-1',
    key: 'AISC',
    name: 'Test Project',
    ...overrides,
  };
}

async function openPopover() {
  render(<IntakeRequestPopover project={baseProject()} />);
  await userEvent.click(screen.getByRole('button', { name: 'Intake Request' }));
  await waitFor(() => expect(screen.getByRole('dialog')).toBeVisible());
}

describe('IntakeRequestPopover — Generate Gap Analysis (AISC-19)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.getIntakeSession.mockResolvedValue({ messages: [] });
  });

  it('calls the gap analysis endpoint for the current project and renders the returned messages', async () => {
    api.generateGapAnalysis.mockResolvedValue({
      messages: [
        {
          id: '1',
          sender: 'Assistant',
          text: 'Here is the gap analysis.',
          timestamp: new Date(),
          kind: 'GAP_ANALYSIS',
        },
      ],
    });

    await openPopover();
    await userEvent.click(screen.getByRole('button', { name: 'Generate Gap Analysis' }));

    expect(api.generateGapAnalysis).toHaveBeenCalledWith('project-1');
    await waitFor(() =>
      expect(screen.getByText('Here is the gap analysis.')).toBeInTheDocument()
    );
  });

  it('disables the trigger and shows a loading label while generating', async () => {
    let resolveGeneration;
    api.generateGapAnalysis.mockReturnValue(
      new Promise((resolve) => {
        resolveGeneration = resolve;
      })
    );

    await openPopover();
    await userEvent.click(screen.getByRole('button', { name: 'Generate Gap Analysis' }));

    const loadingButton = screen.getByRole('button', { name: 'Generating Gap Analysis…' });
    expect(loadingButton).toBeDisabled();

    resolveGeneration({ messages: [] });
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Generate Gap Analysis' })).not.toBeDisabled()
    );
  });

  it('shows an error message when generation fails', async () => {
    api.generateGapAnalysis.mockRejectedValue(new Error('No LLM API key configured'));

    await openPopover();
    await userEvent.click(screen.getByRole('button', { name: 'Generate Gap Analysis' }));

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('No LLM API key configured')
    );
  });
});

// AISC-20: once a gap analysis exists in the session, prompt submissions become
// follow-up questions routed through api.answerGapAnalysisFollowUp instead of the
// local-only chat state used before a gap analysis exists.
describe('IntakeRequestPopover — Follow-up questions (AISC-20)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  async function openPopoverWithGapAnalysis() {
    api.getIntakeSession.mockResolvedValue({
      messages: [
        { id: '1', sender: 'You', text: 'We need a login page', timestamp: new Date() },
        {
          id: '2',
          sender: 'Assistant',
          text: 'Here is the gap analysis.',
          timestamp: new Date(),
          kind: 'GAP_ANALYSIS',
        },
      ],
    });
    render(<IntakeRequestPopover project={baseProject()} />);
    await userEvent.click(screen.getByRole('button', { name: 'Intake Request' }));
    await waitFor(() =>
      expect(screen.getByText('Here is the gap analysis.')).toBeInTheDocument()
    );
  }

  it('routes a prompt submission to the follow-up endpoint once a gap analysis exists', async () => {
    api.answerGapAnalysisFollowUp.mockResolvedValue({
      messages: [
        { id: '1', sender: 'You', text: 'We need a login page', timestamp: new Date() },
        {
          id: '2',
          sender: 'Assistant',
          text: 'Here is the gap analysis.',
          timestamp: new Date(),
          kind: 'GAP_ANALYSIS',
        },
        { id: '3', sender: 'You', text: 'What about password reset?', timestamp: new Date() },
        {
          id: '4',
          sender: 'Assistant',
          text: 'Here is the follow-up answer.',
          timestamp: new Date(),
          kind: 'FOLLOW_UP',
        },
      ],
    });

    await openPopoverWithGapAnalysis();

    await userEvent.type(screen.getByLabelText('Chat message input'), 'What about password reset?');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(api.answerGapAnalysisFollowUp).toHaveBeenCalledWith('project-1', {
      sender: 'You',
      text: 'What about password reset?',
    });
    await waitFor(() =>
      expect(screen.getByText('Here is the follow-up answer.')).toBeInTheDocument()
    );
  });

  it('shows an error message when the follow-up call fails', async () => {
    api.answerGapAnalysisFollowUp.mockRejectedValue(new Error('No LLM API key configured'));

    await openPopoverWithGapAnalysis();

    await userEvent.type(screen.getByLabelText('Chat message input'), 'What about password reset?');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('No LLM API key configured')
    );
  });

  it('does not route to the follow-up endpoint before a gap analysis exists', async () => {
    api.getIntakeSession.mockResolvedValue({ messages: [] });
    render(<IntakeRequestPopover project={baseProject()} />);
    await userEvent.click(screen.getByRole('button', { name: 'Intake Request' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeVisible());

    await userEvent.type(screen.getByLabelText('Chat message input'), 'Just a plain message');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(api.answerGapAnalysisFollowUp).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.getByText('Just a plain message')).toBeInTheDocument());
  });
});

// AISC-21: a session fetched on reopen (not freshly generated in this render) must
// render a persisted gap analysis the same way a live one does — same component, same
// kind-based styling — and must not trigger a regeneration.
describe('IntakeRequestPopover — Restoring a persisted gap analysis (AISC-21)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders a previously persisted gap analysis message on reopen without regenerating it', async () => {
    api.getIntakeSession.mockResolvedValue({
      messages: [
        { id: '1', sender: 'You', text: 'We need a login page', timestamp: new Date(), kind: 'USER' },
        {
          id: '2',
          sender: 'Assistant',
          text: 'Here is the gap analysis.',
          timestamp: new Date(),
          kind: 'GAP_ANALYSIS',
        },
      ],
    });

    render(<IntakeRequestPopover project={baseProject()} />);
    await userEvent.click(screen.getByRole('button', { name: 'Intake Request' }));

    await waitFor(() =>
      expect(screen.getByText('Here is the gap analysis.')).toBeInTheDocument()
    );
    expect(screen.getByText('Gap Analysis')).toBeInTheDocument();
    expect(api.generateGapAnalysis).not.toHaveBeenCalled();
  });

  it('renders a session message with no kind (persisted before AISC-162) as plain chat, not an error', async () => {
    api.getIntakeSession.mockResolvedValue({
      messages: [
        { id: '1', sender: 'You', text: 'An old message from before kind existed', timestamp: new Date() },
      ],
    });

    render(<IntakeRequestPopover project={baseProject()} />);
    await userEvent.click(screen.getByRole('button', { name: 'Intake Request' }));

    await waitFor(() =>
      expect(screen.getByText('An old message from before kind existed')).toBeInTheDocument()
    );
    expect(screen.queryByText('Gap Analysis')).not.toBeInTheDocument();
    expect(screen.queryByText('Follow-up')).not.toBeInTheDocument();
  });
});

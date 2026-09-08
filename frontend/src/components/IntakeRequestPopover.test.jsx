import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import IntakeRequestPopover from './IntakeRequestPopover.jsx';

function baseProject(overrides = {}) {
  return {
    id: 'project-1',
    key: 'AISC',
    name: 'Test Project',
    ...overrides
  };
}

// The popover is a hidden (not unmounted) element while closed, so role
// queries need { hidden: true } to find it regardless of open/closed state.
// Its accessible name computes to empty while hidden (jsdom follows real
// accessibility-tree behavior here), so it's looked up by role alone —
// there's only one dialog in these tests — and its content is asserted
// separately once open.
function getPopover() {
  return screen.getByRole('dialog', { hidden: true });
}

describe('IntakeRequestPopover (AISC-14)', () => {
  it('is closed by default and opens a chat-style popover scoped to the project when clicked', async () => {
    render(<IntakeRequestPopover project={baseProject()} />);

    expect(getPopover()).not.toBeVisible();

    await userEvent.click(screen.getByRole('button', { name: 'Intake Request' }));

    await waitFor(() => expect(getPopover()).toBeVisible());
    expect(getPopover()).toHaveAttribute('aria-label', 'Intake Request — AISC');
    expect(screen.getByText('Intake Request — AISC')).toBeInTheDocument();
  });

  it('closes via the close control and keeps its state when reopened (AISC-89)', async () => {
    render(<IntakeRequestPopover project={baseProject()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Intake Request' }));
    await waitFor(() => expect(getPopover()).toBeVisible());

    const input = screen.getByLabelText('Intake Request message');
    expect(input).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Close' }));
    await waitFor(() => expect(getPopover()).not.toBeVisible());

    // Reopen: the same popover element stays mounted throughout (it's only
    // ever toggled via the `hidden` attribute, never conditionally
    // rendered), so its internal state survives the close/reopen cycle
    // without any backend call.
    await userEvent.click(screen.getByRole('button', { name: 'Intake Request' }));
    await waitFor(() => expect(getPopover()).toBeVisible());
    expect(screen.getByLabelText('Intake Request message')).toBe(input);
  });

  it('closes when clicking outside the popover', async () => {
    render(
      <div>
        <IntakeRequestPopover project={baseProject()} />
        <button type="button">Outside</button>
      </div>
    );

    await userEvent.click(screen.getByRole('button', { name: 'Intake Request' }));
    await waitFor(() => expect(getPopover()).toBeVisible());

    await userEvent.click(screen.getByRole('button', { name: 'Outside' }));
    await waitFor(() => expect(getPopover()).not.toBeVisible());
  });

  it('closes on Escape', async () => {
    render(<IntakeRequestPopover project={baseProject()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Intake Request' }));
    await waitFor(() => expect(getPopover()).toBeVisible());

    await userEvent.keyboard('{Escape}');
    await waitFor(() => expect(getPopover()).not.toBeVisible());
  });
});

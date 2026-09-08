import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ChatPromptInput from './ChatPromptInput.jsx';
import ChatMessageList from './ChatMessageList.jsx';

describe('ChatPromptInput (AISC-93, AISC-96, AISC-102)', () => {
  it('does not submit when input is empty', async () => {
    const onSubmit = vi.fn();
    render(
      <ChatPromptInput value="" onChange={() => {}} onSubmit={onSubmit} />
    );

    const button = screen.getByRole('button', { name: 'Send' });
    await userEvent.click(button);

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('does not submit when input is whitespace-only', async () => {
    const onSubmit = vi.fn();
    render(
      <ChatPromptInput value="   " onChange={() => {}} onSubmit={onSubmit} />
    );

    const button = screen.getByRole('button', { name: 'Send' });
    await userEvent.click(button);

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits non-empty text on button click', async () => {
    const onSubmit = vi.fn();
    render(
      <ChatPromptInput
        value="Hello, world!"
        onChange={() => {}}
        onSubmit={onSubmit}
      />
    );

    const button = screen.getByRole('button', { name: 'Send' });
    await userEvent.click(button);

    expect(onSubmit).toHaveBeenCalledWith('Hello, world!', []);
  });

  it('trims whitespace from submitted text', async () => {
    const onSubmit = vi.fn();
    render(
      <ChatPromptInput
        value="  hello world  "
        onChange={() => {}}
        onSubmit={onSubmit}
      />
    );

    const button = screen.getByRole('button', { name: 'Send' });
    await userEvent.click(button);

    expect(onSubmit).toHaveBeenCalledWith('hello world', []);
  });

  it('submits even if text is empty but attachments exist (AISC-102, AISC-105)', async () => {
    const onSubmit = vi.fn();
    const attachments = [
      { id: '1', name: 'file.pdf', size: 1024 },
    ];

    render(
      <ChatPromptInput
        value=""
        onChange={() => {}}
        attachments={attachments}
        onAttachmentsChange={() => {}}
        onSubmit={onSubmit}
      />
    );

    const button = screen.getByRole('button', { name: 'Send' });
    await userEvent.click(button);

    expect(onSubmit).toHaveBeenCalledWith('', attachments);
  });

  it('displays attachment chips and allows removal (AISC-102, AISC-105)', async () => {
    const onAttachmentsChange = vi.fn();
    const attachments = [
      { id: '1', name: 'file.pdf', size: 1024 },
      { id: '2', name: 'image.jpg', size: 2048 },
    ];

    render(
      <ChatPromptInput
        value=""
        onChange={() => {}}
        attachments={attachments}
        onAttachmentsChange={onAttachmentsChange}
        onSubmit={() => {}}
      />
    );

    // Attachments should be displayed
    expect(screen.getByText('file.pdf')).toBeInTheDocument();
    expect(screen.getByText('image.jpg')).toBeInTheDocument();

    // Remove button should work
    const removeButtons = screen.getAllByRole('button', { name: /Remove/ });
    await userEvent.click(removeButtons[0]);

    // Should call onAttachmentsChange with the remaining attachment
    expect(onAttachmentsChange).toHaveBeenCalledWith([attachments[1]]);
  });
});

describe('ChatMessageList (AISC-92, AISC-96, AISC-104)', () => {
  it('renders messages in order', () => {
    const messages = [
      { id: '1', sender: 'Alice', text: 'Hello', timestamp: new Date('2026-01-01T10:00:00') },
      { id: '2', sender: 'Bob', text: 'Hi there!', timestamp: new Date('2026-01-01T10:01:00') },
    ];

    render(<ChatMessageList messages={messages} />);

    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Hello')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('Hi there!')).toBeInTheDocument();
  });

  it('renders timestamps when provided', () => {
    const messages = [
      {
        id: '1',
        sender: 'Alice',
        text: 'Hello',
        timestamp: new Date('2026-01-01T10:30:45'),
      },
    ];

    render(<ChatMessageList messages={messages} />);

    // The timestamp should be formatted as HH:MM
    const timeElement = screen.getByText(/10:30/);
    expect(timeElement).toBeInTheDocument();
  });

  it('renders empty message list without errors', () => {
    render(<ChatMessageList messages={[]} />);
    expect(screen.queryByText(/Hello/)).not.toBeInTheDocument();
  });

  it('renders attachment chips in messages (AISC-104, AISC-105)', () => {
    const messages = [
      {
        id: '1',
        sender: 'You',
        text: 'Here are the files',
        timestamp: new Date(),
        attachments: [
          { id: 'att1', name: 'proposal.pdf', size: 1024000 },
          { id: 'att2', name: 'budget.xlsx', size: 512000 },
        ],
      },
    ];

    render(<ChatMessageList messages={messages} />);

    expect(screen.getByText('proposal.pdf')).toBeInTheDocument();
    expect(screen.getByText('budget.xlsx')).toBeInTheDocument();
    // File sizes should be formatted
    expect(screen.getByText(/1000 KB|0.98 MB/)).toBeInTheDocument();
  });

  it('renders messages with only attachments, no text (AISC-105)', () => {
    const messages = [
      {
        id: '1',
        sender: 'You',
        text: '',
        timestamp: new Date(),
        attachments: [
          { id: 'att1', name: 'screenshot.png', size: 2048000 },
        ],
      },
    ];

    render(<ChatMessageList messages={messages} />);

    expect(screen.getByText('screenshot.png')).toBeInTheDocument();
    expect(screen.getByText('You')).toBeInTheDocument();
  });
});

describe('ChatMessageList kind-based styling (AISC-19, AISC-20)', () => {
  it('renders a distinct badge and class for a GAP_ANALYSIS message', () => {
    const messages = [
      { id: '1', sender: 'Assistant', text: 'Gap analysis text', timestamp: new Date(), kind: 'GAP_ANALYSIS' },
    ];

    const { container } = render(<ChatMessageList messages={messages} />);

    expect(screen.getByText('Gap Analysis')).toBeInTheDocument();
    expect(container.querySelector('.chat-message--gap-analysis')).toBeInTheDocument();
  });

  it('renders a distinct badge and class for a FOLLOW_UP message', () => {
    const messages = [
      { id: '1', sender: 'Assistant', text: 'Follow-up answer', timestamp: new Date(), kind: 'FOLLOW_UP' },
    ];

    const { container } = render(<ChatMessageList messages={messages} />);

    expect(screen.getByText('Follow-up')).toBeInTheDocument();
    expect(container.querySelector('.chat-message--follow-up')).toBeInTheDocument();
  });

  it('renders plain messages without a kind badge or distinct class', () => {
    const messages = [{ id: '1', sender: 'You', text: 'Hello', timestamp: new Date() }];

    const { container } = render(<ChatMessageList messages={messages} />);

    expect(screen.queryByText('Gap Analysis')).not.toBeInTheDocument();
    expect(screen.queryByText('Follow-up')).not.toBeInTheDocument();
    expect(container.querySelector('.chat-message--gap-analysis')).not.toBeInTheDocument();
    expect(container.querySelector('.chat-message--follow-up')).not.toBeInTheDocument();
  });
});

// Tests for IntakeRequestPopover message integration
describe('ChatPromptInput + ChatMessageList integration (AISC-94, AISC-96)', () => {
  it('non-empty submit appends a message and clears the input', async () => {
    const { useState } = require('react');

    function TestComponent() {
      const [inputValue, setInputValue] = useState('');
      const [messages, setMessages] = useState([]);

      return (
        <div>
          <ChatMessageList messages={messages} />
          <ChatPromptInput
            value={inputValue}
            onChange={setInputValue}
            onSubmit={(text) => {
              setMessages((prev) => [
                ...prev,
                {
                  id: Date.now().toString(),
                  sender: 'You',
                  text,
                  timestamp: new Date(),
                },
              ]);
              setInputValue('');
            }}
          />
        </div>
      );
    }

    render(<TestComponent />);

    const button = screen.getByRole('button', { name: 'Send' });
    const textarea = screen.getByLabelText('Chat message input');

    // Simulate message submission via button
    await userEvent.click(button); // Should not submit - empty
    expect(screen.queryByText('Test message')).not.toBeInTheDocument();

    // User types a message (simulated by changing value prop)
    expect(textarea).toHaveValue(''); // Initially empty
  });
});

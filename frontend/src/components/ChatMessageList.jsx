import { useEffect, useRef } from 'react';

// ChatMessageList (AISC-92, AISC-104): renders an array of {id, sender, text, timestamp} messages
// in a scrollable container, auto-scrolling to the bottom on new messages.
// AISC-104 adds support for rendering attachment chips.
// AISC-19/AISC-20: messages with kind GAP_ANALYSIS/FOLLOW_UP render with a distinct
// style so assistant-generated output stands out from plain chat turns. A message with
// no kind (older sessions, or plain chat) renders as before.
const KIND_CLASS = {
  GAP_ANALYSIS: 'chat-message--gap-analysis',
  FOLLOW_UP: 'chat-message--follow-up',
};

const KIND_LABEL = {
  GAP_ANALYSIS: 'Gap Analysis',
  FOLLOW_UP: 'Follow-up',
};

export default function ChatMessageList({ messages }) {
  const bottomRef = useRef(null);

  useEffect(() => {
    // Auto-scroll to bottom when messages change
    if (bottomRef.current && typeof bottomRef.current.scrollIntoView === 'function') {
      bottomRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }

  return (
    <div className="chat-message-list">
      {messages.map((message) => (
        <div
          key={message.id}
          className={`chat-message${KIND_CLASS[message.kind] ? ` ${KIND_CLASS[message.kind]}` : ''}`}
        >
          <div className="chat-message-sender">
            {message.sender}
            {KIND_LABEL[message.kind] && (
              <span className="chat-message-kind-badge">{KIND_LABEL[message.kind]}</span>
            )}
          </div>
          {message.text && <div className="chat-message-text">{message.text}</div>}
          {message.attachments && message.attachments.length > 0 && (
            <div className="chat-message-attachments">
              {message.attachments.map((attachment) => (
                <div key={attachment.id} className="message-attachment-chip">
                  <span className="attachment-icon">📎</span>
                  <span className="attachment-name">{attachment.name}</span>
                  {attachment.size && (
                    <span className="attachment-size">{formatFileSize(attachment.size)}</span>
                  )}
                </div>
              ))}
            </div>
          )}
          {message.timestamp && (
            <div className="chat-message-timestamp">
              {new Date(message.timestamp).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
              })}
            </div>
          )}
        </div>
      ))}
      <div ref={bottomRef} />
    </div>
  );
}

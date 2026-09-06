import { useEffect, useRef, useState } from 'react';
import EasyMDE from 'easymde';
import 'easymde/dist/easymde.min.css';

// Triggers a work item link: "$" followed by a key such as "AISC-2" (project
// key + '-' + sequence, see docs/backlog-data-model.md). Chosen over Jira's
// "#" specifically to avoid clashing with Markdown headings ("# Heading").
const LINK_TRIGGER = /\$([A-Za-z0-9-]*)$/;
const LINK_PATTERN = /\$([A-Za-z][A-Za-z0-9]*-\d+)\b/g;
const MAX_SUGGESTIONS = 8;

// The preview toggle button reuses EasyMDE's default "fa fa-eye" icon (its
// action already toggles an "active" class on the button while previewing).
// We only override the glyph while active, swapping the eye for a pencil so
// the icon always signals what clicking it will do next: eye = "click to
// preview", pencil = "click to edit". A plain unicode glyph is used (rather
// than another fa- icon name) so it renders regardless of which Font Awesome
// version/icon set the host page has loaded.
let previewIconStylesInjected = false;
function ensurePreviewIconStyles() {
  if (previewIconStylesInjected || typeof document === 'undefined') return;
  const style = document.createElement('style');
  style.setAttribute('data-markdown-editor-preview-icon', 'true');
  style.textContent = `
    .markdown-editor-wrapper .editor-toolbar a.fa-eye.active::before {
      content: "\\270E";
      font-family: inherit;
    }
  `;
  document.head.appendChild(style);
  previewIconStylesInjected = true;
}

/**
 * A toolbar-driven rich editor over a plain Markdown textarea (EasyMDE).
 * The value passed in/out is always Markdown source text — never HTML —
 * matching how work item content is stored (see backend WorkItemContentService).
 *
 * Also supports linking to other work items inline: typing "$" opens an
 * autocomplete of `items` (matched by key or title); picking one inserts
 * "$KEY". The preview renderer turns any "$KEY" reference into a link to
 * "/KEY" — App.jsx intercepts clicks on those to navigate within the app
 * (see its ITEM_LINK_PATH click handler) instead of reloading the page.
 *
 * Opens in preview (read) mode when there's existing content, since most of
 * the time a field is opened to read it, not to change it; the toolbar's
 * eye/pencil button switches between preview and edit. A field with no
 * content yet opens in edit mode, since an empty preview gives the user
 * nothing to look at and no obvious way to start typing.
 */
export default function MarkdownEditor({ value, onChange, placeholder, disabled, items }) {
  const textareaRef = useRef(null);
  const editorRef = useRef(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  const itemsRef = useRef(items);
  itemsRef.current = items;
  const suggestionsRef = useRef(null);
  const [suggestions, setSuggestions] = useState(null);

  useEffect(() => {
    suggestionsRef.current = suggestions;
  }, [suggestions]);

  useEffect(() => {
    ensurePreviewIconStyles();

    function updateSuggestions() {
      const cm = editorRef.current?.codemirror;
      const candidates = itemsRef.current;
      if (!cm || !candidates || candidates.length === 0) {
        setSuggestions(null);
        return;
      }
      const cursor = cm.getCursor();
      const beforeCursor = cm.getLine(cursor.line).slice(0, cursor.ch);
      const match = beforeCursor.match(LINK_TRIGGER);
      if (!match) {
        setSuggestions(null);
        return;
      }
      const query = match[1].toLowerCase();
      const options = candidates
        .filter((i) => i.key.toLowerCase().includes(query) || i.title.toLowerCase().includes(query))
        .slice(0, MAX_SUGGESTIONS);
      if (options.length === 0) {
        setSuggestions(null);
        return;
      }
      setSuggestions({
        options,
        activeIndex: 0,
        from: { line: cursor.line, ch: cursor.ch - match[0].length },
        to: cursor,
        coords: cm.cursorCoords(cursor, 'page')
      });
    }

    function selectSuggestion(option) {
      const cm = editorRef.current?.codemirror;
      const current = suggestionsRef.current;
      if (!cm || !current) return;
      cm.replaceRange(`$${option.key} `, current.from, current.to);
      setSuggestions(null);
      cm.focus();
    }

    const editor = new EasyMDE({
      element: textareaRef.current,
      initialValue: value ?? '',
      spellChecker: false,
      status: false,
      placeholder,
      toolbar: [
        'bold', 'italic', 'heading', '|',
        'quote', 'unordered-list', 'ordered-list', '|',
        'link',
        {
          name: 'link-item',
          action(instance) {
            instance.codemirror.replaceSelection('$');
            instance.codemirror.focus();
            updateSuggestions();
          },
          className: 'fa fa-hashtag',
          title: 'Link a work item ($KEY, e.g. $AISC-2)'
        },
        'code', '|',
        'preview', 'guide'
      ],
      previewRender(plainText) {
        const withLinks = plainText.replace(LINK_PATTERN, (match, key) => `[${key}](/${key})`);
        return editorRef.current.markdown(withLinks);
      }
    });
    editorRef.current = editor;

    // Default to preview mode when there's already content to show; an
    // empty field opens in edit mode instead (see doc comment above).
    const previewButton = editor.toolbarElements?.preview;
    if ((value ?? '').trim().length > 0) {
      editor.togglePreview();
    }
    if (previewButton) {
      const syncPreviewTitle = () => {
        previewButton.title = editor.isPreviewActive() ? 'Edit' : 'Preview';
      };
      syncPreviewTitle();
      previewButton.addEventListener('click', syncPreviewTitle);
    }

    editor.codemirror.on('change', () => {
      onChangeRef.current?.(editor.value());
      updateSuggestions();
    });
    editor.codemirror.on('cursorActivity', updateSuggestions);
    editor.codemirror.on('blur', () => {
      // Defer so a click on a suggestion (which also blurs the editor) still
      // lands before the list disappears.
      setTimeout(() => setSuggestions(null), 150);
    });
    editor.codemirror.on('keydown', (cmInstance, event) => {
      const current = suggestionsRef.current;
      if (!current) return;
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        setSuggestions((s) => (s ? { ...s, activeIndex: (s.activeIndex + 1) % s.options.length } : s));
      } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        setSuggestions((s) => (s ? { ...s, activeIndex: (s.activeIndex - 1 + s.options.length) % s.options.length } : s));
      } else if (event.key === 'Enter' || event.key === 'Tab') {
        event.preventDefault();
        selectSuggestion(current.options[current.activeIndex]);
      } else if (event.key === 'Escape') {
        event.preventDefault();
        setSuggestions(null);
      }
    });

    editorRef.current._selectSuggestion = selectSuggestion;

    return () => {
      editor.toTextArea();
      editorRef.current = null;
    };
    // Mount once; `value` only seeds the initial content, further external
    // changes aren't pushed back in (this editor owns its own state while open).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    editorRef.current?.codemirror.setOption('readOnly', disabled ? 'nocursor' : false);
  }, [disabled]);

  return (
    <div className="markdown-editor-wrapper" style={{ position: 'relative' }}>
      <textarea ref={textareaRef} defaultValue={value} />
      {suggestions && (
        <ul
          className="markdown-link-suggestions"
          style={{
            position: 'fixed',
            top: suggestions.coords.bottom + 4,
            left: suggestions.coords.left,
            zIndex: 20,
            listStyle: 'none',
            margin: 0,
            padding: '4px 0',
            background: 'var(--panel-bg, #fff)',
            border: '1px solid var(--border-color, #ccc)',
            borderRadius: 4,
            boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
            minWidth: 220,
            maxHeight: 220,
            overflowY: 'auto'
          }}
        >
          {suggestions.options.map((option, index) => (
            <li
              key={option.id ?? option.key}
              onMouseDown={(e) => {
                // mousedown (not click) so this fires before the editor's blur handler
                e.preventDefault();
                editorRef.current?._selectSuggestion(option);
              }}
              style={{
                padding: '6px 10px',
                cursor: 'pointer',
                background: index === suggestions.activeIndex ? 'var(--hover-bg, #eef2ff)' : 'transparent'
              }}
            >
              <strong>{option.key}</strong> — {option.title}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

import { useEffect, useRef } from 'react';
import EasyMDE from 'easymde';
import 'easymde/dist/easymde.min.css';

/**
 * A toolbar-driven rich editor over a plain Markdown textarea (EasyMDE).
 * The value passed in/out is always Markdown source text — never HTML —
 * matching how work item content is stored (see backend WorkItemContentService).
 */
export default function MarkdownEditor({ value, onChange, placeholder, disabled }) {
  const textareaRef = useRef(null);
  const editorRef = useRef(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  useEffect(() => {
    const editor = new EasyMDE({
      element: textareaRef.current,
      initialValue: value ?? '',
      spellChecker: false,
      status: false,
      placeholder,
      toolbar: [
        'bold', 'italic', 'heading', '|',
        'quote', 'unordered-list', 'ordered-list', '|',
        'link', 'code', '|',
        'preview', 'guide'
      ]
    });
    editorRef.current = editor;
    editor.codemirror.on('change', () => {
      onChangeRef.current?.(editor.value());
    });

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

  return <textarea ref={textareaRef} defaultValue={value} />;
}

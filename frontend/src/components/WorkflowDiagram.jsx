import { useEffect, useRef, useState } from 'react';
import mermaid from 'mermaid';
import { resolveStateColor, pickReadableTextColor } from '../utils/stateColors.js';

mermaid.initialize({ startOnLoad: false, theme: 'neutral', securityLevel: 'strict' });

let diagramSeq = 0;

function sanitizeId(rawId) {
  return `s${String(rawId).replace(/[^a-zA-Z0-9]/g, '')}`;
}

function escapeLabel(label) {
  return String(label).replace(/"/g, "'");
}

export function buildWorkflowDiagramDefinition(workflow) {
  const states = workflow?.states ?? [];
  const transitions = workflow?.transitions ?? [];

  if (states.length === 0) {
    return 'stateDiagram-v2\n  [*] --> NoStates\n  NoStates: No states yet';
  }

  const lines = ['stateDiagram-v2'];
  const initial = states.find((s) => s.initial) ?? states[0];
  lines.push(`  [*] --> ${sanitizeId(initial.id)}`);

  states.forEach((s) => {
    lines.push(`  ${sanitizeId(s.id)}: ${escapeLabel(s.name)}`);
  });

  transitions.forEach((t) => {
    const label = t.name ? `: ${escapeLabel(t.name)}` : '';
    lines.push(`  ${sanitizeId(t.fromStateId)} --> ${sanitizeId(t.toStateId)}${label}`);
  });

  states.forEach((s) => {
    const id = sanitizeId(s.id);
    const fill = resolveStateColor(s);
    const text = pickReadableTextColor(fill);
    lines.push(`  classDef cls${id} fill:${fill},color:${text},stroke:${fill},stroke-width:1px`);
    lines.push(`  class ${id} cls${id}`);
  });

  return lines.join('\n');
}

export default function WorkflowDiagram({ workflow }) {
  const containerRef = useRef(null);
  const idRef = useRef(`workflow-diagram-${diagramSeq++}`);
  const [renderError, setRenderError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function render() {
      const definition = buildWorkflowDiagramDefinition(workflow);
      try {
        const { svg } = await mermaid.render(idRef.current, definition);
        if (!cancelled && containerRef.current) {
          containerRef.current.innerHTML = svg;
          setRenderError(null);
        }
      } catch (err) {
        if (!cancelled) {
          setRenderError(err.message ?? 'Failed to render diagram');
          if (containerRef.current) containerRef.current.innerHTML = '';
        }
      }
    }

    render();
    return () => {
      cancelled = true;
    };
  }, [workflow]);

  return (
    <div className="workflow-diagram-wrapper">
      {renderError && <p className="error-banner">Couldn't render the diagram: {renderError}</p>}
      <div className="workflow-diagram" ref={containerRef} />
    </div>
  );
}

/**
 * Renders the right input for one custom field's value, based on its
 * configured dataType (see backend CustomFieldService's VALID_DATA_TYPES).
 * `value`/`onChange` carry the raw JS value that gets sent as-is in the
 * work item's `customFields` map (number, boolean, string, or string[]
 * for MULTI_SELECT).
 */
export default function CustomFieldValueInput({ field, value, onChange, disabled }) {
  switch (field.dataType) {
    case 'NUMBER':
      return (
        <input
          type="number"
          value={value ?? ''}
          disabled={disabled}
          onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
        />
      );

    case 'DATE':
      return (
        <input
          type="date"
          value={value ?? ''}
          disabled={disabled}
          onChange={(e) => onChange(e.target.value || null)}
        />
      );

    case 'BOOLEAN':
      return (
        <label className="checkbox-option">
          <input
            type="checkbox"
            checked={Boolean(value)}
            disabled={disabled}
            onChange={(e) => onChange(e.target.checked)}
          />
          {field.name}
        </label>
      );

    case 'SINGLE_SELECT':
      return (
        <select value={value ?? ''} disabled={disabled} onChange={(e) => onChange(e.target.value || null)}>
          <option value="">—</option>
          {(field.options ?? []).map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      );

    case 'MULTI_SELECT': {
      const selected = Array.isArray(value) ? value : [];
      function toggle(option) {
        onChange(selected.includes(option) ? selected.filter((v) => v !== option) : [...selected, option]);
      }
      return (
        <div className="multi-select-options">
          {(field.options ?? []).map((option) => (
            <label key={option} className="checkbox-option">
              <input
                type="checkbox"
                checked={selected.includes(option)}
                disabled={disabled}
                onChange={() => toggle(option)}
              />
              {option}
            </label>
          ))}
        </div>
      );
    }

    case 'TEXT':
    default:
      return (
        <input
          type="text"
          value={value ?? ''}
          disabled={disabled}
          onChange={(e) => onChange(e.target.value || null)}
        />
      );
  }
}

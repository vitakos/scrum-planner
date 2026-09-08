// Empty by default: same-origin in dev, proxied by Vite (see vite.config.js) to the backend.
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });

  if (!response.ok) {
    let message = `Request failed: ${response.status}`;
    try {
      const body = await response.json();
      if (body?.message) message = body.message;
    } catch {
      // ignore non-JSON error bodies
    }
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.json();
}

export const api = {
  listProjects: () => request('/api/projects'),
  getProject: (id) => request(`/api/projects/${id}`),
  getWorkItemByKey: (key) => request(`/api/work-items/by-key/${encodeURIComponent(key)}`),
  createProject: (payload) =>
    request('/api/projects', { method: 'POST', body: JSON.stringify(payload) }),
  updateProject: (id, payload) =>
    request(`/api/projects/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),

  listWorkItemTypes: () => request('/api/work-item-types'),

  listWorkflows: (projectId) => request(`/api/projects/${projectId}/workflows`),

  addState: (projectId, workItemType, payload) =>
    request(`/api/projects/${projectId}/workflows/${workItemType}/states`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateState: (projectId, workItemType, stateId, payload) =>
    request(`/api/projects/${projectId}/workflows/${workItemType}/states/${stateId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteState: (projectId, workItemType, stateId) =>
    request(`/api/projects/${projectId}/workflows/${workItemType}/states/${stateId}`, {
      method: 'DELETE'
    }),
  reorderStates: (projectId, workItemType, stateIds) =>
    request(`/api/projects/${projectId}/workflows/${workItemType}/states/reorder`, {
      method: 'PUT',
      body: JSON.stringify({ stateIds })
    }),

  addTransition: (projectId, workItemType, payload) =>
    request(`/api/projects/${projectId}/workflows/${workItemType}/transitions`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateTransition: (projectId, workItemType, transitionId, payload) =>
    request(`/api/projects/${projectId}/workflows/${workItemType}/transitions/${transitionId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteTransition: (projectId, workItemType, transitionId) =>
    request(`/api/projects/${projectId}/workflows/${workItemType}/transitions/${transitionId}`, {
      method: 'DELETE'
    }),

  listCustomFields: (projectId) => request(`/api/projects/${projectId}/custom-fields`),
  addCustomField: (projectId, workItemType, payload) =>
    request(`/api/projects/${projectId}/custom-fields/${workItemType}`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateCustomField: (projectId, workItemType, fieldId, payload) =>
    request(`/api/projects/${projectId}/custom-fields/${workItemType}/${fieldId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteCustomField: (projectId, workItemType, fieldId) =>
    request(`/api/projects/${projectId}/custom-fields/${workItemType}/${fieldId}`, {
      method: 'DELETE'
    }),

  listWorkItems: (projectId, type, excludeDoneCategory) => {
    const params = new URLSearchParams();
    if (type) params.set('type', type);
    if (excludeDoneCategory) params.set('excludeDoneCategory', 'true');
    const query = params.toString();
    return request(`/api/projects/${projectId}/work-items${query ? `?${query}` : ''}`);
  },
  createWorkItem: (projectId, payload) =>
    request(`/api/projects/${projectId}/work-items`, { method: 'POST', body: JSON.stringify(payload) }),
  updateWorkItem: (projectId, workItemId, payload) =>
    request(`/api/projects/${projectId}/work-items/${workItemId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  applyWorkItemTransition: (projectId, workItemId, payload) =>
    request(`/api/projects/${projectId}/work-items/${workItemId}/transitions`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  deleteWorkItem: (projectId, workItemId) =>
    request(`/api/projects/${projectId}/work-items/${workItemId}`, { method: 'DELETE' }),

  // Intake session API (AISC-101)
  getIntakeSession: (projectId) =>
    request(`/api/projects/${projectId}/intake-session`),
  createIntakeSession: (projectId, payload) =>
    request(`/api/projects/${projectId}/intake-session`, { method: 'POST', body: JSON.stringify(payload) }),
  addIntakeMessage: (projectId, payload) =>
    request(`/api/projects/${projectId}/intake-session/messages`, { method: 'POST', body: JSON.stringify(payload) }),
  uploadIntakeAttachment: async (projectId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await fetch(`${BASE_URL}/api/projects/${projectId}/intake-session/attachments`, {
      method: 'POST',
      body: formData
    });
    if (!response.ok) throw new Error(`Upload failed: ${response.status}`);
    return response.json();
  },
  deleteIntakeAttachment: (projectId, attachmentId) =>
    request(`/api/projects/${projectId}/intake-session/attachments/${attachmentId}`, { method: 'DELETE' }),

  // Gap Analysis assistant (AISC-19, AISC-20)
  generateGapAnalysis: (projectId) =>
    request(`/api/projects/${projectId}/intake-session/gap-analysis`, { method: 'POST' }),
  answerGapAnalysisFollowUp: (projectId, payload) =>
    request(`/api/projects/${projectId}/intake-session/gap-analysis/follow-up`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
};

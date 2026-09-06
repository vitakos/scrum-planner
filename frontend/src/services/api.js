const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

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
  createProject: (payload) =>
    request('/api/projects', { method: 'POST', body: JSON.stringify(payload) }),

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

  listWorkItems: (projectId, type) =>
    request(`/api/projects/${projectId}/work-items${type ? `?type=${encodeURIComponent(type)}` : ''}`),
  createWorkItem: (projectId, payload) =>
    request(`/api/projects/${projectId}/work-items`, { method: 'POST', body: JSON.stringify(payload) }),
  updateWorkItemTitle: (projectId, workItemId, payload) =>
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
    request(`/api/projects/${projectId}/work-items/${workItemId}`, { method: 'DELETE' })
};

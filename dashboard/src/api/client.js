const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1';

export function getToken() {
  return sessionStorage.getItem('bugdna_token');
}

export function setToken(token) {
  if (token) {
    sessionStorage.setItem('bugdna_token', token);
  } else {
    sessionStorage.removeItem('bugdna_token');
  }
}

export function removeToken() {
  sessionStorage.removeItem('bugdna_token');
  sessionStorage.removeItem('bugdna_user');
}

export async function request(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
  const token = getToken();

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 401 && !endpoint.includes('/auth/login')) {
    removeToken();
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
    throw new Error('Unauthorized');
  }

  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const errorMsg = data?.detail || data?.message || `HTTP ${response.status}: Request failed`;
    const error = new Error(errorMsg);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

export const api = {
  auth: {
    login: (username, password) =>
      request('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      }),
    logout: () =>
      request('/auth/logout', { method: 'POST' }).finally(() => removeToken()),
    me: () => request('/auth/me'),
  },

  bugs: {
    list: (params = {}) => {
      const searchParams = new URLSearchParams();
      if (params.status) searchParams.set('status', params.status);
      if (params.priority) searchParams.set('priority', params.priority);
      if (params.errorType) searchParams.set('errorType', params.errorType);
      if (params.q) searchParams.set('q', params.q);
      if (params.sort) searchParams.set('sort', params.sort);
      const query = searchParams.toString();
      return request(`/bugs${query ? `?${query}` : ''}`);
    },
    get: (id) => request(`/bugs/${id}`),
    patch: (id, payload) =>
      request(`/bugs/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      }),
    delete: (id) => request(`/bugs/${id}`, { method: 'DELETE' }),
    occurrences: (id) => request(`/bugs/${id}/occurrences`),
    timeline: (id, occurrenceId) => {
      const q = occurrenceId ? `?occurrenceId=${occurrenceId}` : '';
      return request(`/bugs/${id}/timeline${q}`);
    },
  },

  comments: {
    list: (bugId) => request(`/bugs/${bugId}/comments`),
    create: (bugId, payload) =>
      request(`/bugs/${bugId}/comments`, {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    update: (id, payload) =>
      request(`/comments/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      }),
    delete: (id) => request(`/comments/${id}`, { method: 'DELETE' }),
  },

  activity: {
    get: (afterId = null, limit = 50) => {
      const params = new URLSearchParams();
      if (afterId) params.set('afterId', afterId);
      if (limit) params.set('limit', limit);
      return request(`/activity?${params.toString()}`);
    },
  },

  sync: {
    getState: (signal) => request('/sync/state', { signal }),
  },

  projects: {
    list: () => request('/projects'),
  },
};


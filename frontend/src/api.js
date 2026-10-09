import axios from 'axios';

const client = axios.create({
  baseURL: 'http://localhost:8080/api',
});

/**
 * Unwraps successful responses to their data (empty for 204 No Content) and turns failures
 * into an Error whose message comes from the backend's error body when available.
 */
client.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response) {
      const { data, status } = error.response;
      return Promise.reject(new Error(data?.message || `Request failed with status ${status}`));
    }
    return Promise.reject(new Error('Cannot reach the server. Is the backend running on port 8080?'));
  }
);

export const taskApi = {
  list: () => client.get('/tasks'),
  create: (task) => client.post('/tasks', task),
  update: (id, task) => client.put(`/tasks/${id}`, task),
  remove: (id) => client.delete(`/tasks/${id}`),
};

export const imageApi = {
  /** Uploads an image and resolves to { id, url }. */
  upload: (file) => {
    const form = new FormData();
    form.append('file', file);
    return client.post('/images', form);
  },
};

import { useCallback, useEffect, useState } from 'react';
import { taskApi } from './api.js';
import TaskForm from './TaskForm.jsx';
import ConfirmDialog from './ConfirmDialog.jsx';
import { statusLabel } from './status.js';
import { sanitizeHtml } from './richText.js';

export default function App() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hasLoaded, setHasLoaded] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [actionError, setActionError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [editingTask, setEditingTask] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [taskToDelete, setTaskToDelete] = useState(null);

  const loadTasks = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      setTasks(await taskApi.list());
      setHasLoaded(true);
    } catch (err) {
      setLoadError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  // Success toasts close themselves; error toasts stay until dismissed.
  useEffect(() => {
    if (!successMessage) return undefined;
    const timer = setTimeout(() => setSuccessMessage(''), 3000);
    return () => clearTimeout(timer);
  }, [successMessage]);

  // Runs a create/update/delete, then reloads the list so the UI reflects the change.
  async function runChange(change, message) {
    setSubmitting(true);
    setActionError('');
    setSuccessMessage('');
    try {
      await change();
      setSuccessMessage(message);
      await loadTasks();
      return true;
    } catch (err) {
      setActionError(err.message);
      return false;
    } finally {
      setSubmitting(false);
    }
  }

  // Returns true on success so the form knows it can clear itself.
  async function handleSave(payload) {
    const ok = editingTask
      ? await runChange(() => taskApi.update(editingTask.id, payload), 'Task updated.')
      : await runChange(() => taskApi.create(payload), 'Task added.');
    if (ok) setEditingTask(null);
    return ok;
  }

  // Deleting is two steps: the Delete button opens the confirmation dialog, which calls this.
  async function confirmDelete() {
    const task = taskToDelete;
    const ok = await runChange(() => taskApi.remove(task.id), 'Task deleted.');
    setTaskToDelete(null);
    if (ok && editingTask?.id === task.id) setEditingTask(null);
  }

  return (
    <>
      <header className="masthead">
        <div className="masthead-inner">
          <h1>Task Manager</h1>
          <p>Plan the work, track it, and get it done.</p>
        </div>
      </header>

      <main className="container">
        <div className="layout">
          <TaskForm
            key={editingTask?.id ?? 'new'}
            initialTask={editingTask}
            onSubmit={handleSave}
            onCancel={() => setEditingTask(null)}
            submitting={submitting}
          />

          <section className="task-list" aria-busy={loading}>
            <h2>Tasks</h2>

            {/* Only the first load shows a loading line; refreshes keep the current list on screen. */}
            {loading && !hasLoaded && (
              <p className="muted" role="status">
                Loading tasks…
              </p>
            )}
            {loadError && (
              <div className="banner error" role="alert">
                {loadError} <button onClick={loadTasks}>Retry</button>
              </div>
            )}
            {hasLoaded && !loadError && tasks.length === 0 && (
              <p className="muted">No tasks yet. Create one using the form.</p>
            )}

            <ul>
              {tasks.map((task) => (
                <li key={task.id} className={`task-item status-${task.status.toLowerCase()}`}>
                  <div className="task-main">
                    <strong>{task.title}</strong>
                    <span className="badge">{statusLabel(task.status)}</span>
                  </div>
                  {task.description && (
                    <div
                      className="task-desc rich-text"
                      dangerouslySetInnerHTML={{ __html: sanitizeHtml(task.description) }}
                    />
                  )}
                  <div className="task-actions">
                    <button className="secondary" onClick={() => setEditingTask(task)} disabled={submitting}>
                      Edit
                    </button>
                    <button className="danger" onClick={() => setTaskToDelete(task)} disabled={submitting}>
                      Delete
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </section>
        </div>
      </main>

      <footer className="site-footer">
        <div className="site-footer-inner">
          <span>Task Manager</span>
          <span>Built with React and Spring Boot</span>
        </div>
      </footer>

      {/* Floating feedback: it overlays the page instead of pushing the layout down. */}
      <div className="toast-region">
        {successMessage && (
          <div className="toast success" role="status">
            <span>{successMessage}</span>
            <button type="button" className="toast-close" onClick={() => setSuccessMessage('')} aria-label="Dismiss">
              ×
            </button>
          </div>
        )}
        {actionError && (
          <div className="toast error" role="alert">
            <span>{actionError}</span>
            <button type="button" className="toast-close" onClick={() => setActionError('')} aria-label="Dismiss">
              ×
            </button>
          </div>
        )}
      </div>

      <ConfirmDialog
        open={taskToDelete !== null}
        title="Delete this task?"
        message={taskToDelete ? `"${taskToDelete.title}" will be removed permanently.` : ''}
        confirmLabel="Delete task"
        busyLabel="Deleting…"
        busy={submitting}
        onConfirm={confirmDelete}
        onCancel={() => setTaskToDelete(null)}
      />
    </>
  );
}

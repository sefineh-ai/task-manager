import { useState } from 'react';
import { STATUSES, statusLabel } from './status.js';
import RichTextEditor from './RichTextEditor.jsx';
import { MAX_DESCRIPTION_LENGTH, plainTextLength } from './richText.js';

const EMPTY = { title: '', description: '', status: 'TODO' };

/** Client-side checks for quick feedback; the backend re-validates authoritatively. */
function validate({ title, description }) {
  const errors = {};
  const trimmedTitle = title.trim();
  if (!trimmedTitle) {
    errors.title = 'Title is required.';
  } else if (trimmedTitle.length < 3 || trimmedTitle.length > 100) {
    errors.title = 'Title must be between 3 and 100 characters.';
  }
  if (plainTextLength(description) > MAX_DESCRIPTION_LENGTH) {
    errors.description = `Description must be at most ${MAX_DESCRIPTION_LENGTH} characters.`;
  }
  return errors;
}

export default function TaskForm({ initialTask, onSubmit, onCancel, submitting }) {
  const [form, setForm] = useState(
    initialTask ? { ...initialTask, description: initialTask.description ?? '' } : EMPTY
  );
  const [errors, setErrors] = useState({});
  const isEdit = Boolean(initialTask);
  const descriptionLength = plainTextLength(form.description);

  function updateField(name, value) {
    setForm((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: undefined }));
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const validationErrors = validate(form);
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    const saved = await onSubmit({
      title: form.title.trim(),
      description: form.description || null,
      status: form.status,
    });
    if (saved && !isEdit) {
      setForm(EMPTY);
    }
  }

  return (
    <form className={`task-form${isEdit ? ' is-editing' : ''}`} onSubmit={handleSubmit} noValidate>
      <h2>{isEdit ? 'Edit task' : 'Create a task'}</h2>

      <label htmlFor="task-title">
        Title <span className="required" aria-hidden="true">*</span>
      </label>
      <input
        id="task-title"
        name="title"
        value={form.title}
        onChange={(e) => updateField('title', e.target.value)}
        placeholder="e.g. Prepare weekly report"
        required
        aria-invalid={Boolean(errors.title)}
        aria-describedby={errors.title ? 'task-title-error' : undefined}
      />
      {errors.title && (
        <p id="task-title-error" className="field-error" role="alert">
          {errors.title}
        </p>
      )}

      <label id="task-description-label">Description</label>
      <RichTextEditor
        value={form.description}
        onChange={(html) => updateField('description', html)}
        labelId="task-description-label"
        placeholder="Optional details, up to 500 characters"
        disabled={submitting}
        invalid={Boolean(errors.description)}
      />
      <div className="field-meta">
        <span className="field-meta-error" role={errors.description ? 'alert' : undefined}>
          {errors.description}
        </span>
        <span className={descriptionLength > MAX_DESCRIPTION_LENGTH ? 'is-over' : undefined}>
          {descriptionLength}/{MAX_DESCRIPTION_LENGTH}
        </span>
      </div>

      <label htmlFor="task-status">Status</label>
      <select
        id="task-status"
        name="status"
        value={form.status}
        onChange={(e) => updateField('status', e.target.value)}
      >
        {STATUSES.map((s) => (
          <option key={s} value={s}>
            {statusLabel(s)}
          </option>
        ))}
      </select>

      <div className="form-actions">
        <button type="submit" disabled={submitting}>
          {submitting ? 'Saving…' : isEdit ? 'Save changes' : 'Add task'}
        </button>
        {isEdit && (
          <button type="button" className="secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}

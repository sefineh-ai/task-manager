export const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'];

const LABELS = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  DONE: 'Done',
};

export function statusLabel(status) {
  return LABELS[status] ?? status;
}

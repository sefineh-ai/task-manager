import DOMPurify from 'dompurify';

export const MAX_DESCRIPTION_LENGTH = 500;

// Same tags the backend allows; the backend sanitizes on save, this guards rendering.
const PURIFY_CONFIG = {
  ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'u', 's', 'code', 'pre', 'ul', 'ol', 'li', 'span', 'img'],
  ALLOWED_ATTR: ['style', 'src', 'alt'],
};

export function sanitizeHtml(html) {
  return DOMPurify.sanitize(html ?? '', PURIFY_CONFIG);
}

/**
 * Counts visible characters the way the backend does: markup ignored, blocks separated by
 * one space, whitespace collapsed.
 */
export function plainTextLength(html) {
  if (!html) return 0;
  const doc = new DOMParser().parseFromString(html, 'text/html');
  doc.body.querySelectorAll('p, li, pre, br').forEach((el) => el.after(' '));
  return doc.body.textContent.replace(/\s+/g, ' ').trim().length;
}

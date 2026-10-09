import { useEffect, useRef, useState } from 'react';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import TextStyle from '@tiptap/extension-text-style';
import FontFamily from '@tiptap/extension-font-family';
import Placeholder from '@tiptap/extension-placeholder';
import Image from '@tiptap/extension-image';
import { Markdown } from 'tiptap-markdown';
import { FontSize } from './fontSize.js';
import { imageApi } from './api.js';

const BLOCK_STYLES = [
  { label: 'Text', value: 'paragraph' },
  { label: 'Heading 1', value: '1' },
  { label: 'Heading 2', value: '2' },
  { label: 'Heading 3', value: '3' },
];

const FONT_FAMILIES = [
  { label: 'Font', value: '' },
  { label: 'Serif', value: 'Georgia, serif' },
  { label: 'Monospace', value: 'ui-monospace, Menlo, Consolas, monospace' },
];

const FONT_SIZES = [
  { label: 'Size', value: '' },
  { label: 'Small', value: '13px' },
  { label: 'Large', value: '18px' },
  { label: 'Huge', value: '24px' },
];

const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];
const MAX_IMAGE_BYTES = 2 * 1024 * 1024;

// Toolbar toggles: [name used by isActive, label, accessible title, command]
const TOGGLES = [
  ['bold', 'B', 'Bold (Ctrl+B)', (e) => e.chain().focus().toggleBold().run()],
  ['italic', 'I', 'Italic (Ctrl+I)', (e) => e.chain().focus().toggleItalic().run()],
  ['underline', 'U', 'Underline (Ctrl+U)', (e) => e.chain().focus().toggleUnderline().run()],
  ['strike', 'S', 'Strikethrough', (e) => e.chain().focus().toggleStrike().run()],
  ['code', '</>', 'Inline code', (e) => e.chain().focus().toggleCode().run()],
  ['codeBlock', '{ }', 'Code block', (e) => e.chain().focus().toggleCodeBlock().run()],
  ['bulletList', '•', 'Bulleted list', (e) => e.chain().focus().toggleBulletList().run()],
  ['orderedList', '1.', 'Numbered list', (e) => e.chain().focus().toggleOrderedList().run()],
];

function editorHtml(editor) {
  return editor.isEmpty ? '' : editor.getHTML();
}

/**
 * Rich-text editor for the task description. Emits HTML through onChange ('' when empty).
 * Images are uploaded to the API and inserted by URL.
 */
export default function RichTextEditor({ value, onChange, labelId, placeholder, disabled, invalid }) {
  const fileInputRef = useRef(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');

  const editor = useEditor({
    extensions: [
      StarterKit.configure({ heading: { levels: [1, 2, 3] } }),
      Underline,
      TextStyle,
      FontFamily,
      FontSize,
      Image.configure({ inline: false, allowBase64: false }),
      Placeholder.configure({ placeholder }),
      // Pasted Markdown becomes formatting: # headings, **bold**, *italic*, ~~strike~~, `code`,
      // lists, > quotes, --- dividers and code blocks.
      Markdown.configure({ html: true, transformPastedText: true, transformCopiedText: false }),
    ],
    content: value,
    editorProps: {
      attributes: {
        class: 'rich-editor-content',
        role: 'textbox',
        'aria-multiline': 'true',
        'aria-labelledby': labelId,
      },
    },
    onUpdate: ({ editor: current }) => onChange(editorHtml(current)),
  });

  // The form clears its value after a successful create; mirror that in the editor.
  useEffect(() => {
    if (editor && value === '' && !editor.isEmpty) {
      editor.commands.clearContent();
    }
  }, [editor, value]);

  useEffect(() => {
    editor?.setEditable(!disabled);
  }, [editor, disabled]);

  async function handleImageSelected(e) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !editor) return;

    setUploadError('');
    if (!IMAGE_TYPES.includes(file.type)) {
      setUploadError('Choose a PNG, JPEG, GIF or WebP image.');
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setUploadError('Images must be 2 MB or smaller.');
      return;
    }

    setUploading(true);
    try {
      const { url } = await imageApi.upload(file);
      // Keep a line of text above and below the image: the cursor lands below it, ready to type,
      // and an image placed at the very top still leaves room to write above it.
      editor
        .chain()
        .focus()
        .insertContent([{ type: 'image', attrs: { src: url, alt: file.name } }, { type: 'paragraph' }])
        .command(({ tr }) => {
          if (tr.doc.firstChild?.type.name === 'image') {
            tr.insert(0, editor.schema.nodes.paragraph.create());
          }
          return true;
        })
        .run();
    } catch (err) {
      setUploadError(err.message);
    } finally {
      setUploading(false);
    }
  }

  if (!editor) return null;

  const activeHeading = [1, 2, 3].find((level) => editor.isActive('heading', { level }));
  const blockStyle = activeHeading ? String(activeHeading) : 'paragraph';
  const fontFamily = editor.getAttributes('textStyle').fontFamily ?? '';
  const fontSize = editor.getAttributes('textStyle').fontSize ?? '';
  const locked = disabled || uploading;

  return (
    <div className={`rich-editor${invalid ? ' is-invalid' : ''}`}>
      <div className="rich-toolbar" role="toolbar" aria-label="Text formatting">
        <select
          className="tb-select"
          aria-label="Text style"
          value={blockStyle}
          disabled={locked}
          onChange={(e) => {
            const chain = editor.chain().focus();
            (e.target.value === 'paragraph'
              ? chain.setParagraph()
              : chain.setHeading({ level: Number(e.target.value) })
            ).run();
          }}
        >
          {BLOCK_STYLES.map((b) => (
            <option key={b.value} value={b.value}>
              {b.label}
            </option>
          ))}
        </select>
        <select
          className="tb-select"
          aria-label="Font"
          value={fontFamily}
          disabled={locked}
          onChange={(e) => {
            const chain = editor.chain().focus();
            (e.target.value ? chain.setFontFamily(e.target.value) : chain.unsetFontFamily()).run();
          }}
        >
          {FONT_FAMILIES.map((f) => (
            <option key={f.label} value={f.value}>
              {f.label}
            </option>
          ))}
        </select>
        <select
          className="tb-select"
          aria-label="Text size"
          value={fontSize}
          disabled={locked}
          onChange={(e) => {
            const chain = editor.chain().focus();
            (e.target.value ? chain.setFontSize(e.target.value) : chain.unsetFontSize()).run();
          }}
        >
          {FONT_SIZES.map((s) => (
            <option key={s.label} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>

        <span className="tb-divider" aria-hidden="true" />

        {TOGGLES.map(([name, label, title, run]) => (
          <button
            key={name}
            type="button"
            className={`tb-btn tb-${name}${editor.isActive(name) ? ' is-active' : ''}`}
            title={title}
            aria-label={title}
            aria-pressed={editor.isActive(name)}
            disabled={locked}
            onClick={() => run(editor)}
          >
            {label}
          </button>
        ))}

        <span className="tb-divider" aria-hidden="true" />

        <button
          type="button"
          className="tb-btn tb-image"
          title="Insert image"
          aria-label="Insert image"
          disabled={locked}
          onClick={() => fileInputRef.current?.click()}
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2"
            strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <rect x="3" y="4" width="18" height="16" rx="2" />
            <circle cx="9" cy="10" r="2" />
            <path d="M21 16l-5-5-9 9" />
          </svg>
        </button>
        <button
          type="button"
          className="tb-btn"
          title="Clear formatting"
          aria-label="Clear formatting"
          disabled={locked}
          onClick={() => editor.chain().focus().unsetAllMarks().clearNodes().run()}
        >
          T<sub>x</sub>
        </button>

        <input
          ref={fileInputRef}
          type="file"
          accept={IMAGE_TYPES.join(',')}
          onChange={handleImageSelected}
          hidden
        />
      </div>

      <EditorContent editor={editor} />

      {(uploading || uploadError) && (
        <p className={`rich-editor-status${uploadError ? ' is-error' : ''}`} role={uploadError ? 'alert' : 'status'}>
          {uploading ? 'Uploading image…' : uploadError}
        </p>
      )}
    </div>
  );
}

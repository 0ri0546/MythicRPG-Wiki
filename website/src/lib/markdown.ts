function escapeHtml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

export function markdownToHtml(markdown: string): string {
  return markdown
    .split(/\n{2,}/)
    .map((block) => {
      const value = block.trim();
      if (!value) return '';
      if (value.startsWith('# ')) return `<h2>${escapeHtml(value.slice(2))}</h2>`;
      if (value.startsWith('## ')) return `<h3>${escapeHtml(value.slice(3))}</h3>`;
      return `<p>${escapeHtml(value).replaceAll('\n', '<br>')}</p>`;
    })
    .join('\n');
}

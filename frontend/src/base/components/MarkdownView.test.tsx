import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import MarkdownView from './MarkdownView';

describe('MarkdownView', () => {
  it('渲染标题/加粗/代码块/表格(GFM)', () => {
    const { container } = render(
      <MarkdownView
        content={'# 思路\n\n**哈希表**一次遍历\n\n```java\nint a = 1;\n```\n\n| a | b |\n|---|---|\n| 1 | 2 |'}
      />,
    );

    expect(screen.getByRole('heading', { level: 1, name: '思路' })).toBeInTheDocument();
    expect(container.querySelector('strong')?.textContent).toBe('哈希表');
    expect(container.querySelector('pre code')?.textContent).toContain('int a = 1;');
    expect(container.querySelector('table')).not.toBeNull(); // remark-gfm 生效
  });

  it('不渲染原始 HTML(默认防 XSS)', () => {
    const { container } = render(<MarkdownView content={'<script>alert(1)</script>'} />);
    expect(container.querySelector('script')).toBeNull();
  });
});

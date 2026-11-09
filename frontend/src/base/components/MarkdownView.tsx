import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

/**
 * Markdown 渲染: GFM(表格/任务列表/删除线) + 代码块
 * 安全: react-markdown 默认不渲染原始 HTML, 天然防 XSS
 */
export default function MarkdownView({ content }: { content: string }) {
  return (
    <div className="note-content">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
    </div>
  );
}

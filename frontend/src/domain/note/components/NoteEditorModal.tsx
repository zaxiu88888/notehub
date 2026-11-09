import { useEffect, useState } from 'react';
import { App, Button, Col, Input, Modal, Row, Segmented, Select, Space, Typography } from 'antd';
import { createNote, updateNote } from '@/domain/note/api';
import { getTags } from '@/domain/tag/api';
import { handleAuthError } from '@/request/http';
import MarkdownView from '@/base/components/MarkdownView';

interface Props {
  open: boolean;
  questionId: number;
  /** 有值=编辑, 空=新建 */
  noteId?: number | null;
  initialContent?: string;
  /** 编辑时为笔记标签; 新建时预填题目标签(后端不传则继承) */
  initialTags?: string[];
  onClose: () => void;
  onSaved: () => void;
}

const MAX_LEN = 50000;
/** 稳定引用: 默认参数若用 [] 会每次渲染新建数组, 触发 effect 死循环 */
const EMPTY_TAGS: string[] = [];

/** 写/改笔记: 左编辑右预览(或小屏切页签), Ctrl+Enter 快速保存 */
export default function NoteEditorModal({
  open,
  questionId,
  noteId,
  initialContent = '',
  initialTags = EMPTY_TAGS,
  onClose,
  onSaved,
}: Props) {
  const { message } = App.useApp();
  const [content, setContent] = useState(initialContent);
  const [tags, setTags] = useState<string[]>(initialTags);
  const [tagOptions, setTagOptions] = useState<{ value: string; label: string }[]>([]);
  const [saving, setSaving] = useState(false);
  const [view, setView] = useState<'split' | 'edit' | 'preview'>('split');

  useEffect(() => {
    if (open) {
      setContent(initialContent);
      setTags(initialTags);
    }
  }, [open, initialContent, initialTags]);

  useEffect(() => {
    if (!open) return;
    getTags(30)
      .then((list) => setTagOptions(list.map((t) => ({ value: t.name, label: t.name }))))
      .catch(() => undefined);
  }, [open]);

  const handleSave = async () => {
    if (!content.trim()) {
      message.warning('内容不能为空');
      return;
    }
    if (tags.length > 5) {
      message.warning('最多 5 个标签');
      return;
    }
    setSaving(true);
    try {
      if (noteId) {
        await updateNote(noteId, content, tags);
        message.success('笔记已更新');
      } else {
        await createNote({
          questionId,
          content,
          // 新建时不选标签 → 不传, 后端继承题目标签
          ...(tags.length > 0 ? { tags } : {}),
        });
        message.success('笔记已发布');
      }
      onSaved();
      onClose();
    } catch (e) {
      if (!handleAuthError(e)) {
        message.error(e instanceof Error ? e.message : '保存失败');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={noteId ? '编辑笔记' : '写笔记'}
      width={1000}
      destroyOnHidden
      footer={
        <Space>
          <Typography.Text type="secondary">{content.length}/{MAX_LEN}</Typography.Text>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" loading={saving} onClick={handleSave}>
            {noteId ? '保存修改' : '发布笔记'}
          </Button>
        </Space>
      }
    >
      <div style={{ marginBottom: 8 }}>
        <Segmented
          value={view}
          onChange={(v) => setView(v as typeof view)}
          options={[
            { label: '双栏', value: 'split' },
            { label: '编辑', value: 'edit' },
            { label: '预览', value: 'preview' },
          ]}
        />
        <Typography.Text type="secondary" style={{ marginLeft: 12 }}>
          支持 Markdown · Ctrl/⌘ + Enter 保存
        </Typography.Text>
      </div>

      <div style={{ marginBottom: 10 }}>
        <Select
          mode="tags"
          aria-label="笔记标签"
          placeholder="选择或输入标签, 最多 5 个 (不选则继承题目标签)"
          style={{ width: '100%' }}
          value={tags}
          onChange={setTags}
          options={tagOptions}
          maxCount={5}
          tokenSeparators={[',', '，']}
        />
      </div>

      <Row gutter={12}>
        {view !== 'preview' && (
          <Col span={view === 'split' ? 12 : 24}>
            <Input.TextArea
              aria-label="笔记内容"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              maxLength={MAX_LEN}
              autoSize={{ minRows: 18, maxRows: 28 }}
              placeholder={'# 思路\n\n用哈希表一次遍历...\n\n```java\n// 代码\n```'}
              onKeyDown={(e) => {
                if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                  e.preventDefault();
                  handleSave();
                }
              }}
            />
          </Col>
        )}
        {view !== 'edit' && (
          <Col span={view === 'split' ? 12 : 24}>
            <div
              style={{
                border: '1px solid #f0f0f0',
                borderRadius: 8,
                padding: 12,
                minHeight: 380,
                maxHeight: 560,
                overflow: 'auto',
                background: '#fafafa',
              }}
            >
              {content.trim() ? (
                <MarkdownView content={content} />
              ) : (
                <Typography.Text type="secondary">预览区: 左侧输入后这里实时渲染</Typography.Text>
              )}
            </div>
          </Col>
        )}
      </Row>
    </Modal>
  );
}

import { useEffect, useState } from 'react';
import { App, Button, Col, Input, Modal, Row, Segmented, Space, Typography } from 'antd';
import { createNote, updateNote } from '@/domain/note/api';
import { handleAuthError } from '@/request/http';
import MarkdownView from '@/base/components/MarkdownView';

interface Props {
  open: boolean;
  questionId: number;
  /** 有值=编辑, 空=新建 */
  noteId?: number | null;
  initialContent?: string;
  onClose: () => void;
  onSaved: () => void;
}

const MAX_LEN = 50000;

/** 写/改笔记: 左编辑右预览(或小屏切页签), Ctrl+Enter 快速保存 */
export default function NoteEditorModal({
  open,
  questionId,
  noteId,
  initialContent = '',
  onClose,
  onSaved,
}: Props) {
  const { message } = App.useApp();
  const [content, setContent] = useState(initialContent);
  const [saving, setSaving] = useState(false);
  const [view, setView] = useState<'split' | 'edit' | 'preview'>('split');

  useEffect(() => {
    if (open) setContent(initialContent);
  }, [open, initialContent]);

  const handleSave = async () => {
    if (!content.trim()) {
      message.warning('内容不能为空');
      return;
    }
    setSaving(true);
    try {
      if (noteId) {
        await updateNote(noteId, content);
        message.success('笔记已更新');
      } else {
        await createNote({ questionId, content });
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

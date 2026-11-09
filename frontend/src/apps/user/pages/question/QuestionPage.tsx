import { useCallback, useEffect, useState } from 'react';
import {
  App,
  Button,
  Card,
  Drawer,
  Empty,
  Result,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd';
import { EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons';
import { Link, useParams } from 'react-router-dom';
import { getQuestion } from '@/domain/question/api';
import type { QuestionVO } from '@/domain/question/types';
import { getNotes } from '@/domain/note/api';
import type { NoteVO } from '@/domain/note/types';
import { ApiError } from '@/request/http';
import { requireLogin } from '@/utils/auth';
import { useAppSelector } from '@/store/hooks';
import DifficultyTag from '@/base/components/DifficultyTag';
import MarkdownView from '@/base/components/MarkdownView';
import NoteCard from '@/domain/note/components/NoteCard';
import NoteEditorModal from '@/domain/note/components/NoteEditorModal';
import CommentsPanel from '@/domain/note/components/CommentsPanel';

/** 题目详情: 题干 + 该题全部笔记 + 写/改我的笔记 + 评论抽屉 */
export default function QuestionPage() {
  const { questionId: rawId } = useParams();
  const questionId = Number(rawId);
  const { message } = App.useApp();
  const { token } = useAppSelector((s) => s.user);

  const [question, setQuestion] = useState<QuestionVO | null>(null);
  const [notes, setNotes] = useState<NoteVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingNote, setEditingNote] = useState<NoteVO | null>(null);
  const [commentNoteId, setCommentNoteId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!Number.isFinite(questionId)) {
      setNotFound(true);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const [q, notePage] = await Promise.all([
        getQuestion(questionId),
        getNotes({ questionId, page: 1, pageSize: 20 }),
      ]);
      setQuestion(q);
      setNotes(notePage.list);
    } catch (e) {
      if (e instanceof ApiError && e.code === 40401) {
        setNotFound(true);
      } else {
        message.error(e instanceof Error ? e.message : '加载失败');
      }
    } finally {
      setLoading(false);
    }
  }, [questionId, message]);

  useEffect(() => {
    load();
  }, [load]);

  // 仅首次加载显示骨架; 已有数据时后台刷新不再卸载页面(否则抽屉/弹窗会被连带销毁)
  if (loading && !question) {
    return (
      <Card>
        <Skeleton active paragraph={{ rows: 8 }} />
      </Card>
    );
  }

  if (notFound || !question) {
    return (
      <Result
        status="404"
        title="题目不存在"
        extra={
          <Link to="/">
            <Button type="primary">回到题库</Button>
          </Link>
        }
      />
    );
  }

  const myNote = question.myNoteId != null ? notes.find((n) => n.noteId === question.myNoteId) : undefined;

  const openEditor = () => {
    if (!requireLogin(token)) return;
    setEditingNote(null);
    setEditorOpen(true);
  };

  return (
    <>
      <Card style={{ marginBottom: 16 }}>
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          <Space size={8} wrap>
            <DifficultyTag difficulty={question.difficulty} />
            <Tag>{question.categoryName ?? '未分类'}</Tag>
            {question.examPoint && (
              <Typography.Text type="secondary">考点: {question.examPoint}</Typography.Text>
            )}
            <Typography.Text type="secondary">
              <EyeOutlined /> {question.viewCount}
            </Typography.Text>
          </Space>

          <Typography.Title level={3} style={{ margin: 0 }}>
            {question.title}
          </Typography.Title>

          {question.description ? (
            <MarkdownView content={question.description} />
          ) : (
            <Typography.Text type="secondary">该题暂无题干描述</Typography.Text>
          )}

          <Space>
            {myNote ? (
              <Button
                icon={<EditOutlined />}
                onClick={() => {
                  setEditingNote(myNote);
                  setEditorOpen(true);
                }}
              >
                编辑我的笔记
              </Button>
            ) : (
              <Button type="primary" icon={<PlusOutlined />} onClick={openEditor}>
                写笔记
              </Button>
            )}
            <Typography.Text type="secondary">
              已有 {notes.length} 篇笔记
            </Typography.Text>
          </Space>
        </Space>
      </Card>

      <Typography.Title level={4} style={{ margin: '0 0 12px' }}>
        题解笔记
      </Typography.Title>

      {notes.length === 0 ? (
        <Card>
          <Empty description="还没有人写笔记, 来做第一个吧">
            <Button type="primary" onClick={openEditor}>
              写第一篇笔记
            </Button>
          </Empty>
        </Card>
      ) : (
        notes.map((note) => (
          <NoteCard
            key={note.noteId}
            note={note}
            onEdit={(n) => {
              setEditingNote(n);
              setEditorOpen(true);
            }}
            onOpenComments={setCommentNoteId}
            onChanged={load}
          />
        ))
      )}

      <NoteEditorModal
        open={editorOpen}
        questionId={questionId}
        noteId={editingNote?.noteId}
        initialContent={editingNote?.content ?? ''}
        onClose={() => setEditorOpen(false)}
        onSaved={load}
      />

      <Drawer
        open={commentNoteId != null}
        onClose={() => setCommentNoteId(null)}
        title="评论"
        width={560}
        destroyOnClose
      >
        {commentNoteId != null && (
          // 注意: 不要把 onCountChange 接回整页 reload(会卸载抽屉并形成重挂载循环)
          <CommentsPanel noteId={commentNoteId} />
        )}
      </Drawer>
    </>
  );
}

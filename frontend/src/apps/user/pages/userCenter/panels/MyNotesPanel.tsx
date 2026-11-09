import { useCallback, useEffect, useState } from 'react';
import { App, Card, Empty, Pagination, Skeleton } from 'antd';
import { Link } from 'react-router-dom';
import { getNotes } from '@/domain/note/api';
import type { NoteVO } from '@/domain/note/types';
import NoteCard from '@/domain/note/components/NoteCard';
import { useAppSelector } from '@/store/hooks';

const PAGE_SIZE = 5;

/** 我的笔记: 分页 + 编辑/删除(NoteCard 内置作者操作) */
export default function MyNotesPanel() {
  const { message } = App.useApp();
  const user = useAppSelector((s) => s.user.user);
  const [notes, setNotes] = useState<NoteVO[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await getNotes({ authorId: user.userId, page, pageSize: PAGE_SIZE });
      setNotes(data.list);
      setTotal(data.total);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [user, page, message]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading && notes.length === 0) return <Skeleton active paragraph={{ rows: 6 }} />;

  if (notes.length === 0) {
    return (
      <Card>
        <Empty description="还没有写过笔记">
          <Link to="/">去题库找一道题 →</Link>
        </Empty>
      </Card>
    );
  }

  return (
    <>
      {notes.map((note) => (
        <NoteCard key={note.noteId} note={note} collapsible onChanged={load} />
      ))}
      <div style={{ textAlign: 'right' }}>
        <Pagination
          current={page}
          pageSize={PAGE_SIZE}
          total={total}
          showSizeChanger={false}
          onChange={setPage}
          hideOnSinglePage
        />
      </div>
    </>
  );
}

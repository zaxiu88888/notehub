import { useCallback, useEffect, useState } from 'react';
import { App, Button, Card, Empty, Pagination, Skeleton, Typography } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { downloadNotes, getNotes } from '@/domain/note/api';
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
  const [exporting, setExporting] = useState(false);

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

  const handleExport = async () => {
    setExporting(true);
    try {
      await downloadNotes();
      message.success('已开始下载全部笔记');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '导出失败');
    } finally {
      setExporting(false);
    }
  };

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
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 12,
        }}
      >
        <Typography.Text type="secondary">共 {total} 篇笔记</Typography.Text>
        <Button icon={<DownloadOutlined />} loading={exporting} onClick={handleExport}>
          导出全部 (Markdown)
        </Button>
      </div>
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

import { useCallback, useEffect, useState } from 'react';
import {
  App,
  Avatar,
  Card,
  Empty,
  List,
  Pagination,
  Skeleton,
  Space,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { Link, useSearchParams } from 'react-router-dom';
import { searchNotes, searchUsers } from '@/domain/search/api';
import type { NoteVO } from '@/domain/note/types';
import type { UserVO } from '@/domain/user/types';
import NoteCard from '@/domain/note/components/NoteCard';
import NoteCommentsDrawer from '@/domain/note/components/NoteCommentsDrawer';
import { ApiError } from '@/request/http';

const PAGE_SIZE = 10;

/** 搜索结果页: 笔记(全文索引) / 用户(昵称账号) */
export default function SearchPage() {
  const [params, setParams] = useSearchParams();
  const keyword = (params.get('keyword') ?? '').trim();
  const tab = params.get('type') ?? 'notes';
  const page = Number(params.get('page') ?? '1') || 1;
  const { message } = App.useApp();

  const [notes, setNotes] = useState<NoteVO[]>([]);
  const [users, setUsers] = useState<UserVO[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [commentNoteId, setCommentNoteId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!keyword) {
      setNotes([]);
      setUsers([]);
      setTotal(0);
      return;
    }
    setLoading(true);
    try {
      if (tab === 'users') {
        const data = await searchUsers({ keyword, page, pageSize: PAGE_SIZE });
        setUsers(data.list);
        setTotal(data.total);
      } else {
        const data = await searchNotes({ keyword, page, pageSize: PAGE_SIZE });
        setNotes(data.list);
        setTotal(data.total);
      }
    } catch (e) {
      if (e instanceof ApiError) message.error(e.message);
    } finally {
      setLoading(false);
    }
  }, [keyword, tab, page, message]);

  useEffect(() => {
    load();
  }, [load]);

  const changePage = (next: number) => {
    setParams({ keyword, type: tab, page: String(next) });
  };

  const changeTab = (next: string) => {
    setParams({ keyword, type: next, page: '1' });
  };

  return (
    <>
      <Card title={keyword ? `搜索「${keyword}」` : '搜索'}>
      {!keyword ? (
        <Empty description="在顶部搜索框输入关键词，搜索笔记或用户" />
      ) : (
        <Tabs
          activeKey={tab}
          onChange={changeTab}
          items={[
            {
              key: 'notes',
              label: `笔记 ${tab === 'notes' ? `(${total})` : ''}`,
              children: loading ? (
                <Skeleton active paragraph={{ rows: 6 }} />
              ) : notes.length === 0 ? (
                <Empty description="没有匹配的笔记，换个关键词试试" />
              ) : (
                <>
                  {notes.map((note) => (
                    <NoteCard
                      key={note.noteId}
                      note={note}
                      collapsible
                      onOpenComments={setCommentNoteId}
                    />
                  ))}
                  <div style={{ textAlign: 'right' }}>
                    <Pagination
                      current={page}
                      pageSize={PAGE_SIZE}
                      total={total}
                      showSizeChanger={false}
                      onChange={changePage}
                      hideOnSinglePage
                    />
                  </div>
                </>
              ),
            },
            {
              key: 'users',
              label: `用户 ${tab === 'users' ? `(${total})` : ''}`,
              children: loading ? (
                <Skeleton active paragraph={{ rows: 4 }} />
              ) : users.length === 0 ? (
                <Empty description="没有匹配的用户" />
              ) : (
                <>
                  <List
                    dataSource={users}
                    renderItem={(u) => (
                      <List.Item>
                        <List.Item.Meta
                          avatar={
                            <Avatar src={u.avatarUrl ?? undefined} icon={<UserOutlined />} />
                          }
                          title={
                            <Space>
                              <Link to={`/users/${u.userId}`}>{u.username}</Link>
                              {u.isAdmin === 1 && <Tag color="gold">管理员</Tag>}
                            </Space>
                          }
                          description={
                            <Typography.Text type="secondary">
                              {u.signature ?? '这个人很懒, 什么都没写'}
                            </Typography.Text>
                          }
                        />
                      </List.Item>
                    )}
                  />
                  <div style={{ textAlign: 'right' }}>
                    <Pagination
                      current={page}
                      pageSize={PAGE_SIZE}
                      total={total}
                      showSizeChanger={false}
                      onChange={changePage}
                      hideOnSinglePage
                    />
                  </div>
                </>
              ),
            },
          ]}
        />
      )}
    </Card>
      <NoteCommentsDrawer
        noteId={commentNoteId}
        onClose={() => setCommentNoteId(null)}
        onCountChange={load}
      />
    </>
  );
}

import { useEffect, useState } from 'react';
import { App, Avatar, Card, Empty, Result, Skeleton, Space, Tag, Typography } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { Link, useParams } from 'react-router-dom';
import dayjs from 'dayjs';
import { getPublicProfile } from '@/domain/user/api';
import type { UserVO } from '@/domain/user/types';
import { getNotes } from '@/domain/note/api';
import type { NoteVO } from '@/domain/note/types';
import { ApiError } from '@/request/http';
import NoteCard from '@/domain/note/components/NoteCard';

/** 他人主页: 公开资料 + TA 的笔记 */
export default function UserHomePage() {
  const { userId: rawId } = useParams();
  const userId = Number(rawId);
  const { message } = App.useApp();
  const [profile, setProfile] = useState<UserVO | null>(null);
  const [notes, setNotes] = useState<NoteVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!Number.isFinite(userId)) {
      setNotFound(true);
      setLoading(false);
      return;
    }
    setLoading(true);
    Promise.all([
      getPublicProfile(userId),
      getNotes({ authorId: userId, page: 1, pageSize: 20 }),
    ])
      .then(([user, notePage]) => {
        setProfile(user);
        setNotes(notePage.list);
      })
      .catch((e) => {
        if (e instanceof ApiError && e.code === 40401) setNotFound(true);
        else message.error('加载失败');
      })
      .finally(() => setLoading(false));
  }, [userId, message]);

  if (loading) return <Card><Skeleton active avatar paragraph={{ rows: 6 }} /></Card>;
  if (notFound || !profile) {
    return (
      <Result
        status="404"
        title="用户不存在"
        extra={<Link to="/">回到首页</Link>}
      />
    );
  }

  return (
    <>
      <Card style={{ marginBottom: 16 }}>
        <Space size={20} align="start">
          <Avatar size={80} src={profile.avatarUrl ?? undefined} icon={<UserOutlined />} />
          <Space direction="vertical" size={4}>
            <Space size={8}>
              <Typography.Title level={4} style={{ margin: 0 }}>
                {profile.username}
              </Typography.Title>
              {profile.isAdmin === 1 && <Tag color="gold">管理员</Tag>}
            </Space>
            <Typography.Text type="secondary">
              {profile.signature ?? '这个人很懒, 什么都没写'}
            </Typography.Text>
            <Space size={12} wrap>
              {profile.school && <Tag>{profile.school}</Tag>}
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                加入于 {dayjs(profile.createdAt).format('YYYY-MM-DD')} · {notes.length} 篇笔记
              </Typography.Text>
            </Space>
          </Space>
        </Space>
      </Card>

      <Typography.Title level={4} style={{ margin: '0 0 12px' }}>
        TA 的笔记
      </Typography.Title>
      {notes.length === 0 ? (
        <Card>
          <Empty description="还没有写过笔记" />
        </Card>
      ) : (
        notes.map((note) => <NoteCard key={note.noteId} note={note} collapsible />)
      )}
    </>
  );
}

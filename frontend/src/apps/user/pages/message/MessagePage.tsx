import { useCallback, useEffect, useState } from 'react';
import {
  App,
  Avatar,
  Badge,
  Button,
  Card,
  Empty,
  List,
  Pagination,
  Popconfirm,
  Space,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import { DeleteOutlined, UserOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import {
  deleteMessage,
  getMessages,
  getNote,
  getUnreadCount,
  markAllMessagesRead,
  markMessageRead,
} from '@/domain/note/api';
import type { MessageVO, UnreadCount } from '@/domain/note/types';
import { handleAuthError } from '@/request/http';

const PAGE_SIZE = 10;

const TYPE_TABS = [
  { key: 'all', label: '全部' },
  { key: '1', label: '点赞' },
  { key: '2', label: '评论' },
  { key: '3', label: '系统' },
];

/** 消息中心: 分类型 / 未读角标 / 已读 / 删除 */
export default function MessagePage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [tab, setTab] = useState('all');
  const [page, setPage] = useState(1);
  const [items, setItems] = useState<MessageVO[]>([]);
  const [total, setTotal] = useState(0);
  const [unread, setUnread] = useState<UnreadCount | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const type = tab === 'all' ? undefined : Number(tab);
      const [data, count] = await Promise.all([
        getMessages({ type, page, pageSize: PAGE_SIZE }),
        getUnreadCount(),
      ]);
      setItems(data.list);
      setTotal(data.total);
      setUnread(count);
    } catch (e) {
      if (!handleAuthError(e)) message.error('消息加载失败');
    } finally {
      setLoading(false);
    }
  }, [tab, page, message]);

  useEffect(() => {
    load();
  }, [load]);

  const openMessage = async (item: MessageVO) => {
    // 导航前的已读处理不阻塞跳转
    if (item.isRead === 0) {
      markMessageRead(item.messageId).catch(() => undefined);
      setItems((prev) =>
        prev.map((m) => (m.messageId === item.messageId ? { ...m, isRead: 1 } : m)),
      );
    }
    // 点赞/评论消息: 定位到目标笔记所在的题目页
    if (item.targetType === 1) {
      try {
        const note = await getNote(item.targetId);
        navigate(`/questions/${note.questionId}#note-${item.targetId}`);
      } catch {
        // 笔记可能已删除, 停留消息页
      }
    }
  };

  const handleReadAll = async () => {
    try {
      await markAllMessagesRead();
      message.success('已全部标记为已读');
      await load();
    } catch (e) {
      if (!handleAuthError(e)) message.error('操作失败');
    }
  };

  const handleDelete = async (messageId: number) => {
    try {
      await deleteMessage(messageId);
      await load();
    } catch (e) {
      if (!handleAuthError(e)) message.error('删除失败');
    }
  };

  const unreadBadge: Record<string, number> = {
    all: unread?.total ?? 0,
    '1': unread?.like ?? 0,
    '2': unread?.comment ?? 0,
    '3': unread?.system ?? 0,
  };

  return (
    <Card
      title="消息中心"
      extra={
        <Button onClick={handleReadAll} disabled={(unread?.total ?? 0) === 0}>
          全部已读
        </Button>
      }
    >
      <Tabs
        activeKey={tab}
        onChange={(k) => {
          setTab(k);
          setPage(1);
        }}
        items={TYPE_TABS.map((t) => ({
          key: t.key,
          label: (
            <Space size={6}>
              {t.label}
              {unreadBadge[t.key] > 0 && <Badge count={unreadBadge[t.key]} size="small" />}
            </Space>
          ),
          children: (
            <List
              loading={loading}
              dataSource={items}
              locale={{ emptyText: <Empty description="暂无消息" /> }}
              renderItem={(item) => (
                <List.Item
                  onClick={() => openMessage(item)}
                  style={{
                    cursor: 'pointer',
                    opacity: item.isRead === 0 ? 1 : 0.6,
                    fontWeight: item.isRead === 0 ? 500 : 400,
                  }}
                  actions={[
                    <Popconfirm
                      key="del"
                      title="删除这条消息?"
                      onConfirm={(e) => {
                        e?.stopPropagation();
                        handleDelete(item.messageId);
                      }}
                      okText="删除"
                    >
                      <Button
                        type="text"
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={(e) => e.stopPropagation()}
                      />
                    </Popconfirm>,
                  ]}
                >
                  <List.Item.Meta
                    avatar={
                      <Badge dot={item.isRead === 0}>
                        <Avatar
                          src={item.sender?.avatarUrl ?? undefined}
                          icon={<UserOutlined />}
                        />
                      </Badge>
                    }
                    title={
                      <Space size={8} wrap>
                        {item.sender ? (
                          <Link to={`/users/${item.sender.userId}`}>{item.sender.username}</Link>
                        ) : (
                          <span>系统通知</span>
                        )}
                        <Tag color={item.type === 1 ? 'magenta' : item.type === 2 ? 'blue' : 'default'}>
                          {item.type === 1 ? '点赞' : item.type === 2 ? '评论' : '系统'}
                        </Tag>
                        <Typography.Text type="secondary" style={{ fontSize: 12, fontWeight: 'normal' }}>
                          {dayjs(item.createdAt).format('MM-DD HH:mm')}
                        </Typography.Text>
                      </Space>
                    }
                    description={item.content}
                  />
                </List.Item>
              )}
            />
          ),
        }))}
      />

      {total > PAGE_SIZE && (
        <div style={{ textAlign: 'right', marginTop: 12 }}>
          <Pagination
            current={page}
            pageSize={PAGE_SIZE}
            total={total}
            showSizeChanger={false}
            onChange={setPage}
          />
        </div>
      )}
    </Card>
  );
}

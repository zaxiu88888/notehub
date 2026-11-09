import { useState } from 'react';
import { App, Avatar, Button, Card, Popconfirm, Space, Tag, Tooltip, Typography } from 'antd';
import {
  CommentOutlined,
  DeleteOutlined,
  EditOutlined,
  LikeFilled,
  LikeOutlined,
  StarFilled,
  StarOutlined,
  UserOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import type { NoteVO } from '@/domain/note/types';
import { deleteNote, likeNote, unlikeNote } from '@/domain/note/api';
import { handleAuthError } from '@/request/http';
import { requireLogin } from '@/utils/auth';
import { useAppSelector } from '@/store/hooks';
import MarkdownView from '@/base/components/MarkdownView';
import CollectModal from './CollectModal';

interface Props {
  note: NoteVO;
  /** 内容折叠高度(首页用), 题目页可不传 */
  collapsible?: boolean;
  onEdit?: (note: NoteVO) => void;
  onOpenComments?: (noteId: number) => void;
  onChanged?: () => void;
}

/** 笔记卡片: Markdown 渲染 + 点赞/收藏/评论 + 作者操作 */
export default function NoteCard({
  note,
  collapsible = false,
  onEdit,
  onOpenComments,
  onChanged,
}: Props) {
  const { message } = App.useApp();
  const { token, user } = useAppSelector((s) => s.user);
  const [liked, setLiked] = useState(note.myActions.liked);
  const [likeCount, setLikeCount] = useState(note.likeCount);
  const [collected, setCollected] = useState(note.myActions.collected);
  const [expanded, setExpanded] = useState(!collapsible);
  const [collectOpen, setCollectOpen] = useState(false);

  const mine = user?.userId === note.author.userId;

  const toggleLike = async () => {
    if (!requireLogin(token)) return;
    const next = !liked;
    setLiked(next); // 乐观更新
    setLikeCount((c) => c + (next ? 1 : -1));
    try {
      if (next) {
        const result = await likeNote(note.noteId);
        setLikeCount(result.likeCount);
      } else {
        await unlikeNote(note.noteId);
      }
    } catch (e) {
      setLiked(!next); // 回滚
      setLikeCount((c) => c + (next ? -1 : 1));
      if (!handleAuthError(e)) message.error('操作失败');
    }
  };

  const handleDelete = async () => {
    try {
      await deleteNote(note.noteId);
      message.success('笔记已删除');
      onChanged?.();
    } catch (e) {
      if (!handleAuthError(e)) message.error(e instanceof Error ? e.message : '删除失败');
    }
  };

  return (
    <Card
      style={{ marginBottom: 16 }}
      styles={{ body: { paddingTop: 12 } }}
      title={
        <Space size={8} wrap>
          <Avatar src={note.author.avatarUrl ?? undefined} icon={<UserOutlined />} size="small" />
          <span>{note.author.username}</span>
          {mine && <Tag color="blue">我的笔记</Tag>}
          <Typography.Text type="secondary" style={{ fontWeight: 'normal', fontSize: 12 }}>
            {dayjs(note.createdAt).format('YYYY-MM-DD HH:mm')}
          </Typography.Text>
        </Space>
      }
      extra={
        mine ? (
          <Space>
            <Tooltip title="编辑">
              <Button
                type="text"
                size="small"
                icon={<EditOutlined />}
                onClick={() => onEdit?.(note)}
              />
            </Tooltip>
            <Popconfirm title="删除后不可恢复, 确定?" onConfirm={handleDelete} okText="删除">
              <Button type="text" size="small" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          </Space>
        ) : null
      }
    >
      <div
        style={
          expanded
            ? undefined
            : { maxHeight: 160, overflow: 'hidden', position: 'relative', maskImage: 'linear-gradient(#000 70%, transparent)' }
        }
      >
        <MarkdownView content={note.content} />
      </div>
      {!expanded && (
        <Button type="link" size="small" onClick={() => setExpanded(true)}>
          展开全文
        </Button>
      )}

      <Space size={20} style={{ marginTop: 8 }}>
        <Button
          type="text"
          size="small"
          icon={liked ? <LikeFilled style={{ color: '#eb2f96' }} /> : <LikeOutlined />}
          onClick={toggleLike}
        >
          {likeCount}
        </Button>
        <Button
          type="text"
          size="small"
          icon={<CommentOutlined />}
          onClick={() => onOpenComments?.(note.noteId)}
        >
          {note.commentCount}
        </Button>
        <Button
          type="text"
          size="small"
          icon={collected ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />}
          onClick={() => {
            if (!requireLogin(token)) return;
            setCollectOpen(true);
          }}
        >
          {note.collectCount}
        </Button>
      </Space>

      <CollectModal
        open={collectOpen}
        noteId={note.noteId}
        onClose={() => setCollectOpen(false)}
        onCollected={() => {
          setCollected(true);
          onChanged?.();
        }}
      />
    </Card>
  );
}

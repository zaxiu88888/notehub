import { useCallback, useEffect, useState } from 'react';
import {
  App,
  Avatar,
  Button,
  Divider,
  Empty,
  Input,
  List,
  Pagination,
  Popconfirm,
  Space,
  Tag,
  Typography,
} from 'antd';
import { DeleteOutlined, LikeFilled, LikeOutlined, UserOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { CommentVO } from '@/domain/note/types';
import {
  editComment,
  getComments,
  likeComment,
  postComment,
  removeComment,
  unlikeComment,
} from '@/domain/note/api';
import { handleAuthError } from '@/request/http';
import { requireLogin } from '@/utils/auth';
import { useAppSelector } from '@/store/hooks';

const PAGE_SIZE = 10;

interface Props {
  noteId: number;
  onCountChange?: (total: number) => void;
}

/** 评论区: 顶层分页 + 楼中楼回复 + 点赞 + 作者编辑/删除 */
export default function CommentsPanel({ noteId, onCountChange }: Props) {
  const { message } = App.useApp();
  const { token, user } = useAppSelector((s) => s.user);
  const [comments, setComments] = useState<CommentVO[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [content, setContent] = useState('');
  const [replyTo, setReplyTo] = useState<CommentVO | null>(null);
  const [posting, setPosting] = useState(false);
  const [editing, setEditing] = useState<CommentVO | null>(null);
  const [editContent, setEditContent] = useState('');

  const load = useCallback(
    async (nextPage = page) => {
      setLoading(true);
      try {
        const data = await getComments({ noteId, page: nextPage, pageSize: PAGE_SIZE });
        setComments(data.list);
        setTotal(data.total);
        onCountChange?.(data.total);
      } catch (e) {
        if (!handleAuthError(e)) message.error('评论加载失败');
      } finally {
        setLoading(false);
      }
    },
    [noteId, page, onCountChange, message],
  );

  useEffect(() => {
    load(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, noteId]);

  const submit = async () => {
    if (!requireLogin(token)) return;
    if (!content.trim()) {
      message.warning('请输入评论内容');
      return;
    }
    setPosting(true);
    try {
      await postComment({
        noteId,
        parentId: replyTo?.commentId,
        content: content.trim(),
      });
      setContent('');
      setReplyTo(null);
      await load(1);
      setPage(1);
      message.success('评论成功');
    } catch (e) {
      if (!handleAuthError(e)) message.error(e instanceof Error ? e.message : '评论失败');
    } finally {
      setPosting(false);
    }
  };

  const toggleLike = async (comment: CommentVO) => {
    if (!requireLogin(token)) return;
    try {
      if (comment.liked) {
        await unlikeComment(comment.commentId);
      } else {
        await likeComment(comment.commentId);
      }
      await load();
    } catch (e) {
      if (!handleAuthError(e)) message.error('操作失败');
    }
  };

  const handleDelete = async (comment: CommentVO) => {
    try {
      await removeComment(comment.commentId);
      message.success('已删除');
      await load();
    } catch (e) {
      if (!handleAuthError(e)) message.error(e instanceof Error ? e.message : '删除失败');
    }
  };

  const saveEdit = async () => {
    if (!editing) return;
    try {
      await editComment(editing.commentId, editContent.trim());
      setEditing(null);
      await load();
      message.success('已修改');
    } catch (e) {
      if (!handleAuthError(e)) message.error(e instanceof Error ? e.message : '修改失败');
    }
  };

  const renderComment = (c: CommentVO, isReply = false) => {
    const mine = user?.userId === c.author.userId;
    const deleted = c.isDeleted === 1;
    return (
      <div
        key={c.commentId}
        style={{ display: 'flex', gap: 8, marginLeft: isReply ? 40 : 0, marginBottom: 12 }}
      >
        <Avatar
          size={isReply ? 'small' : 'default'}
          src={c.author.avatarUrl ?? undefined}
          icon={<UserOutlined />}
        />
        <div style={{ flex: 1, minWidth: 0 }}>
          <Space size={6} wrap>
            <Typography.Text strong>{c.author.username}</Typography.Text>
            {c.replyToUserName && (
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                回复 @{c.replyToUserName}
              </Typography.Text>
            )}
            {mine && <Tag>我</Tag>}
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {dayjs(c.createdAt).format('MM-DD HH:mm')}
            </Typography.Text>
          </Space>

          {editing?.commentId === c.commentId ? (
            <Space.Compact style={{ width: '100%', marginTop: 6 }}>
              <Input
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                onPressEnter={saveEdit}
              />
              <Button type="primary" onClick={saveEdit}>
                保存
              </Button>
              <Button onClick={() => setEditing(null)}>取消</Button>
            </Space.Compact>
          ) : (
            <div style={{ margin: '4px 0', whiteSpace: 'pre-wrap', color: deleted ? '#bfbfbf' : undefined }}>
              {c.content}
            </div>
          )}

          {!deleted && (
            <Space size={12}>
              <Button
                type="text"
                size="small"
                icon={c.liked ? <LikeFilled style={{ color: '#eb2f96' }} /> : <LikeOutlined />}
                onClick={() => toggleLike(c)}
              >
                {c.likeCount}
              </Button>
              <Button
                type="link"
                size="small"
                style={{ padding: 0 }}
                onClick={() => {
                  if (!requireLogin(token)) return;
                  setReplyTo(c);
                }}
              >
                回复
              </Button>
              {mine && (
                <>
                  <Button
                    type="link"
                    size="small"
                    style={{ padding: 0 }}
                    onClick={() => {
                      setEditing(c);
                      setEditContent(c.content);
                    }}
                  >
                    编辑
                  </Button>
                  <Popconfirm title="删除这条评论?" onConfirm={() => handleDelete(c)} okText="删除">
                    <Button type="link" size="small" danger style={{ padding: 0 }} icon={<DeleteOutlined />} />
                  </Popconfirm>
                </>
              )}
            </Space>
          )}

          {c.replies.length > 0 && (
            <div style={{ marginTop: 8 }}>
              {c.replies.map((r) => renderComment(r, true))}
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div>
      <Space direction="vertical" style={{ width: '100%' }}>
        {replyTo && (
          <Typography.Text type="secondary">
            回复 @{replyTo.author.username}
            <Button type="link" size="small" onClick={() => setReplyTo(null)}>
              取消
            </Button>
          </Typography.Text>
        )}
        <Input.TextArea
          aria-label="评论内容"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder={replyTo ? `回复 @${replyTo.author.username}...` : '写下你的想法...'}
          autoSize={{ minRows: 2, maxRows: 5 }}
          maxLength={2000}
        />
        <div style={{ textAlign: 'right' }}>
          <Button type="primary" loading={posting} onClick={submit}>
            发表评论
          </Button>
        </div>
      </Space>

      <Divider style={{ margin: '12px 0' }} />

      <List
        loading={loading}
        dataSource={comments}
        locale={{ emptyText: <Empty description="还没有评论, 来说两句" /> }}
        renderItem={(c) => <List.Item style={{ padding: '8px 0', border: 'none' }}>{renderComment(c)}</List.Item>}
      />

      {total > PAGE_SIZE && (
        <div style={{ textAlign: 'right' }}>
          <Pagination
            current={page}
            pageSize={PAGE_SIZE}
            total={total}
            showSizeChanger={false}
            onChange={setPage}
          />
        </div>
      )}
    </div>
  );
}

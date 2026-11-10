import { useCallback, useEffect, useState } from 'react';
import {
  App,
  Button,
  Card,
  Empty,
  List,
  Popconfirm,
  Space,
  Tag,
  Typography,
} from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import {
  deleteCollection,
  getCollectionNotes,
  getCollections,
  removeFromCollection,
} from '@/domain/note/api';
import type { CollectionVO, NoteVO } from '@/domain/note/types';
import NoteCard from '@/domain/note/components/NoteCard';
import NoteCommentsDrawer from '@/domain/note/components/NoteCommentsDrawer';
import { handleAuthError } from '@/request/http';

/** 我的收藏: 左收藏夹列表, 右夹内笔记 */
export default function CollectionsPanel() {
  const { message } = App.useApp();
  const [collections, setCollections] = useState<CollectionVO[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [notes, setNotes] = useState<NoteVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [commentNoteId, setCommentNoteId] = useState<number | null>(null);

  const loadCollections = useCallback(async () => {
    try {
      const data = await getCollections();
      setCollections(data);
      setActiveId((prev) => prev ?? data[0]?.collectionId ?? null);
    } catch (e) {
      if (!handleAuthError(e)) message.error('收藏夹加载失败');
    } finally {
      setLoading(false);
    }
  }, [message]);

  const loadNotes = useCallback(async (collectionId: number) => {
    try {
      const data = await getCollectionNotes({ collectionId, page: 1, pageSize: 50 });
      setNotes(data.list);
    } catch (e) {
      if (!handleAuthError(e)) message.error('笔记加载失败');
    }
  }, [message]);

  useEffect(() => {
    loadCollections();
  }, [loadCollections]);

  useEffect(() => {
    if (activeId != null) loadNotes(activeId);
    else setNotes([]);
  }, [activeId, loadNotes]);

  const handleDeleteCollection = async (collectionId: number) => {
    try {
      await deleteCollection(collectionId);
      message.success('收藏夹已删除');
      setActiveId(null);
      await loadCollections();
    } catch (e) {
      if (!handleAuthError(e)) message.error(e instanceof Error ? e.message : '删除失败');
    }
  };

  const handleRemoveNote = async (noteId: number) => {
    if (activeId == null) return;
    try {
      await removeFromCollection(activeId, noteId);
      message.success('已移出收藏夹');
      await loadNotes(activeId);
      await loadCollections();
    } catch (e) {
      if (!handleAuthError(e)) message.error(e instanceof Error ? e.message : '移出失败');
    }
  };

  if (loading) return <Card loading />;

  if (collections.length === 0) {
    return (
      <Card>
        <Empty description="还没有收藏夹: 在笔记上点星标即可收藏" />
      </Card>
    );
  }

  const active = collections.find((c) => c.collectionId === activeId);

  return (
    <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
      <Card title="收藏夹" style={{ width: 260, flexShrink: 0 }} styles={{ body: { padding: 8 } }}>
        <List
          size="small"
          dataSource={collections}
          renderItem={(c) => (
            <List.Item
              style={{
                cursor: 'pointer',
                borderRadius: 6,
                paddingInline: 8,
                background: c.collectionId === activeId ? '#f0f5ff' : undefined,
              }}
              onClick={() => setActiveId(c.collectionId)}
              actions={[
                <Popconfirm
                  key="del"
                  title="删除收藏夹(不会删除笔记), 确定?"
                  onConfirm={(e) => {
                    e?.stopPropagation();
                    handleDeleteCollection(c.collectionId);
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
                title={c.name}
                description={
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {c.noteCount} 篇
                  </Typography.Text>
                }
              />
            </List.Item>
          )}
        />
      </Card>

      <div style={{ flex: 1, minWidth: 0 }}>
        <Space style={{ marginBottom: 12 }}>
          <Typography.Title level={5} style={{ margin: 0 }}>
            {active?.name ?? '收藏夹'}
          </Typography.Title>
          {active?.description && <Tag>{active.description}</Tag>}
        </Space>

        {notes.length === 0 ? (
          <Card>
            <Empty description="这个收藏夹还是空的" />
          </Card>
        ) : (
          notes.map((note) => (
            <div key={note.noteId} style={{ position: 'relative' }}>
              <NoteCard
                note={note}
                collapsible
                onChanged={() => activeId != null && loadNotes(activeId)}
                onOpenComments={setCommentNoteId}
              />
              <Button
                size="small"
                type="text"
                danger
                style={{ position: 'absolute', right: 12, top: 64, zIndex: 1 }}
                onClick={() => handleRemoveNote(note.noteId)}
              >
                移出收藏夹
              </Button>
            </div>
          ))
        )}
      </div>
      <NoteCommentsDrawer
        noteId={commentNoteId}
        onClose={() => setCommentNoteId(null)}
        onCountChange={() => activeId != null && loadNotes(activeId)}
      />
    </div>
  );
}

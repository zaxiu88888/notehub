import { useEffect, useState } from 'react';
import { App, Button, Divider, Empty, Input, List, Modal, Space, Typography } from 'antd';
import {
  batchCollect,
  createCollection,
  getCollections,
} from '@/domain/note/api';
import type { CollectionVO } from '@/domain/note/types';
import { handleAuthError } from '@/request/http';

interface Props {
  open: boolean;
  noteId: number;
  onClose: () => void;
  onCollected?: () => void;
}

/** 收藏到收藏夹: 选择已有夹或现场新建 */
export default function CollectModal({ open, noteId, onClose, onCollected }: Props) {
  const { message } = App.useApp();
  const [collections, setCollections] = useState<CollectionVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [newName, setNewName] = useState('');
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = () => {
    setLoading(true);
    getCollections()
      .then(setCollections)
      .catch((e) => {
        if (!handleAuthError(e)) message.error('收藏夹加载失败');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (open) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const collectTo = async (collectionId: number) => {
    setBusyId(collectionId);
    try {
      const result = await batchCollect({ collectionId, noteIds: [noteId] });
      message.success(result.added > 0 ? '已收藏' : '该笔记已在此收藏夹中');
      onCollected?.();
      onClose();
    } catch (e) {
      if (!handleAuthError(e)) message.error(e instanceof Error ? e.message : '收藏失败');
    } finally {
      setBusyId(null);
    }
  };

  const handleCreate = async () => {
    if (!newName.trim()) return;
    try {
      const created = await createCollection({ name: newName.trim() });
      setNewName('');
      await collectTo(created.collectionId);
    } catch (e) {
      if (!handleAuthError(e)) message.error(e instanceof Error ? e.message : '创建失败');
    }
  };

  return (
    <Modal open={open} onCancel={onClose} footer={null} title="收藏到收藏夹" width={460}>
      <List
        loading={loading}
        dataSource={collections}
        locale={{ emptyText: <Empty description="还没有收藏夹, 先建一个" /> }}
        renderItem={(c) => (
          <List.Item
            actions={[
              <Button
                key="add"
                type="link"
                loading={busyId === c.collectionId}
                onClick={() => collectTo(c.collectionId)}
              >
                收藏
              </Button>,
            ]}
          >
            <List.Item.Meta
              title={c.name}
              description={
                <Typography.Text type="secondary">
                  {c.description ?? '暂无描述'} · {c.noteCount} 篇
                </Typography.Text>
              }
            />
          </List.Item>
        )}
      />
      <Divider plain>新建收藏夹</Divider>
      <Space.Compact style={{ width: '100%' }}>
        <Input
          aria-label="新收藏夹名称"
          placeholder="例如: 必看题解"
          value={newName}
          maxLength={32}
          onChange={(e) => setNewName(e.target.value)}
          onPressEnter={handleCreate}
        />
        <Button type="primary" onClick={handleCreate} disabled={!newName.trim()}>
          新建并收藏
        </Button>
      </Space.Compact>
    </Modal>
  );
}

import { useState } from 'react';
import { App, Button, Empty, Modal, Skeleton, Tag, Tooltip, Typography } from 'antd';
import { RobotOutlined } from '@ant-design/icons';
import { summarizeNote } from '@/domain/note/api';
import type { AiSummaryVO } from '@/domain/note/types';
import { handleAuthError } from '@/request/http';
import { requireLogin } from '@/utils/auth';
import { useAppSelector } from '@/store/hooks';

interface Props {
  noteId: number;
}

/** 笔记 AI 总结: 点击生成/展示; 后端未配置模型时自动使用本地摘要 */
export default function AiSummaryButton({ noteId }: Props) {
  const { message } = App.useApp();
  const { token } = useAppSelector((s) => s.user);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AiSummaryVO | null>(null);
  const [failed, setFailed] = useState(false);

  const handleOpen = async () => {
    if (!requireLogin(token)) return;
    setOpen(true);
    if (data) return;
    setLoading(true);
    setFailed(false);
    try {
      setData(await summarizeNote(noteId));
    } catch (e) {
      setFailed(true);
      if (!handleAuthError(e)) {
        message.error(e instanceof Error ? e.message : '总结生成失败');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Tooltip title="AI 总结">
        <Button
          type="text"
          size="small"
          aria-label="AI 总结"
          icon={<RobotOutlined />}
          onClick={handleOpen}
        />
      </Tooltip>
      <Modal
        open={open}
        onCancel={() => setOpen(false)}
        footer={null}
        width={520}
        title={
          <span>
            <RobotOutlined style={{ color: '#722ed1', marginRight: 8 }} />
            AI 题解总结
          </span>
        }
      >
        {loading ? (
          <Skeleton active paragraph={{ rows: 4 }} />
        ) : data ? (
          <>
            <Tag color={data.source === 'ai' ? 'purple' : 'default'}>
              {data.source === 'ai' ? 'AI 生成' : '本地摘要'}
            </Tag>
            <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginTop: 12 }}>
              {data.summary}
            </Typography.Paragraph>
            {data.source === 'local' && (
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                未配置 AI 模型，当前为本地摘要；配置 ai.summary.api-key 后可启用模型总结。
              </Typography.Text>
            )}
          </>
        ) : (
          <Empty description={failed ? '生成失败，请稍后重试' : '暂无总结'} />
        )}
      </Modal>
    </>
  );
}

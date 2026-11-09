import { useEffect, useState } from 'react';
import { App, Card, Empty, List, Select, Skeleton, Space, Tag, Typography } from 'antd';
import { CommentOutlined, FireFilled, LikeOutlined, StarOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { getNoteRanklist } from '@/domain/note/api';
import type { RankNoteVO, RankPeriod } from '@/domain/note/types';

const PERIOD_OPTIONS: { value: RankPeriod; label: string }[] = [
  { value: 'week', label: '近 7 天' },
  { value: 'month', label: '近 30 天' },
  { value: 'all', label: '总榜' },
];

const RANK_COLORS = ['#f5222d', '#fa8c16', '#faad14'];

/** 笔记热度榜: 窗口内 赞*2 + 评*3 + 藏*2 排序 */
export default function RankListCard() {
  const { message } = App.useApp();
  const [period, setPeriod] = useState<RankPeriod>('week');
  const [list, setList] = useState<RankNoteVO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getNoteRanklist({ period, limit: 10 })
      .then((data) => {
        if (!cancelled) setList(data);
      })
      .catch((e) => {
        if (!cancelled) message.error(e instanceof Error ? e.message : '热度榜加载失败');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [period, message]);

  return (
    <Card
      title={
        <Space>
          <FireFilled style={{ color: '#fa541c' }} />
          笔记热度榜
        </Space>
      }
      extra={
        <Select
          aria-label="榜单周期"
          size="small"
          value={period}
          options={PERIOD_OPTIONS}
          onChange={setPeriod}
          style={{ width: 88 }}
        />
      }
      styles={{ body: { paddingTop: 4 } }}
    >
      {loading ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : list.length === 0 ? (
        <Empty description="该周期还没有互动数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <List
          size="small"
          dataSource={list}
          renderItem={(item) => (
            <List.Item style={{ paddingInline: 0 }}>
              <Space align="start" size={10} style={{ width: '100%' }}>
                <span
                  style={{
                    width: 18,
                    textAlign: 'center',
                    fontWeight: 700,
                    color: RANK_COLORS[item.rank - 1] ?? '#8c8c8c',
                  }}
                >
                  {item.rank}
                </span>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <Link to={`/questions/${item.questionId}`}>
                    <Typography.Text strong ellipsis style={{ maxWidth: 170 }}>
                      {item.questionTitle}
                    </Typography.Text>
                  </Link>
                  <Typography.Paragraph
                    type="secondary"
                    ellipsis={{ rows: 1 }}
                    style={{ marginBottom: 2, fontSize: 12 }}
                  >
                    {item.excerpt || '（无正文）'}
                  </Typography.Paragraph>
                  <Space size={10} style={{ fontSize: 12, color: '#8c8c8c' }}>
                    <span>{item.author.username}</span>
                    <span>
                      <LikeOutlined /> {item.likeCount}
                    </span>
                    <span>
                      <CommentOutlined /> {item.commentCount}
                    </span>
                    <span>
                      <StarOutlined /> {item.collectCount}
                    </span>
                  </Space>
                </div>
                <Tag color="volcano" style={{ marginInlineEnd: 0 }}>
                  {item.hotScore}
                </Tag>
              </Space>
            </List.Item>
          )}
        />
      )}
    </Card>
  );
}

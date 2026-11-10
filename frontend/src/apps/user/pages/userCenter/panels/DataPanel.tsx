import { useEffect, useState } from 'react';
import { App, Button, Card, Col, Empty, Row, Select, Skeleton, Space, Statistic, Typography } from 'antd';
import {
  CalendarOutlined,
  CommentOutlined,
  FileTextOutlined,
  FireOutlined,
  LikeOutlined,
  StarOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { getHeatmap, getNoteStats } from '@/domain/note/api';
import type { HeatmapVO, NoteStatsVO } from '@/domain/note/types';
import { checkin, getCheckinStatus } from '@/domain/checkin/api';
import type { CheckinStatusVO } from '@/domain/checkin/types';
import Heatmap from '@/domain/note/components/Heatmap';

const CURRENT_YEAR = dayjs().year();
const YEAR_OPTIONS = Array.from({ length: 5 }, (_, i) => CURRENT_YEAR - i).map((y) => ({
  value: y,
  label: `${y} 年`,
}));

/** 个人中心 · 数据统计: Top3 数据卡片 + 全年刷题热力图 */
export default function DataPanel() {
  const { message } = App.useApp();
  const [year, setYear] = useState(CURRENT_YEAR);
  const [stats, setStats] = useState<NoteStatsVO | null>(null);
  const [heatmap, setHeatmap] = useState<HeatmapVO | null>(null);
  const [checkinStatus, setCheckinStatus] = useState<CheckinStatusVO | null>(null);
  const [loadingStats, setLoadingStats] = useState(true);
  const [loadingHeatmap, setLoadingHeatmap] = useState(true);
  const [loadingCheckin, setLoadingCheckin] = useState(true);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    setLoadingCheckin(true);
    getCheckinStatus()
      .then(setCheckinStatus)
      .catch(() => undefined)
      .finally(() => setLoadingCheckin(false));
  }, []);

  const handleCheckin = async () => {
    setChecking(true);
    try {
      const status = await checkin();
      setCheckinStatus(status);
      message.success(`打卡成功，已连续打卡 ${status.currentStreak} 天`);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '打卡失败');
    } finally {
      setChecking(false);
    }
  };

  useEffect(() => {
    setLoadingStats(true);
    getNoteStats()
      .then(setStats)
      .catch((e) => message.error(e instanceof Error ? e.message : '数据加载失败'))
      .finally(() => setLoadingStats(false));
  }, [message]);

  useEffect(() => {
    setLoadingHeatmap(true);
    getHeatmap(year)
      .then(setHeatmap)
      .catch((e) => message.error(e instanceof Error ? e.message : '热力图加载失败'))
      .finally(() => setLoadingHeatmap(false));
  }, [year, message]);

  const cards = [
    { title: '笔记总数', value: stats?.noteCount ?? 0, icon: <FileTextOutlined />, color: '#2f54eb' },
    { title: '收获点赞', value: stats?.likeCount ?? 0, icon: <LikeOutlined />, color: '#eb2f96' },
    { title: '连续提交', value: stats?.currentStreak ?? 0, suffix: '天', icon: <FireOutlined />, color: '#fa541c' },
  ];

  return (
    <>
      <Card
        size="small"
        style={{ marginBottom: 16 }}
        title={
          <Space>
            <CalendarOutlined style={{ color: '#fa541c' }} />
            每日打卡
          </Space>
        }
      >
        <Row align="middle" justify="space-between" gutter={[12, 12]}>
          <Col>
            <Space size={32} wrap>
              <Statistic
                title="连续打卡"
                value={checkinStatus?.currentStreak ?? 0}
                suffix="天"
                prefix={<CalendarOutlined style={{ color: '#fa541c' }} />}
                loading={loadingCheckin}
              />
              <Statistic
                title="累计打卡"
                value={checkinStatus?.totalDays ?? 0}
                suffix="天"
                loading={loadingCheckin}
              />
              <Typography.Text type="secondary">
                最长连击 {checkinStatus?.longestStreak ?? 0} 天
              </Typography.Text>
            </Space>
          </Col>
          <Col>
            <Button
              type="primary"
              icon={<CalendarOutlined />}
              loading={checking}
              disabled={loadingCheckin || checkinStatus?.checkedInToday}
              onClick={handleCheckin}
            >
              {checkinStatus?.checkedInToday ? '今日已打卡' : '今日打卡'}
            </Button>
          </Col>
        </Row>
      </Card>

      <Row gutter={[16, 16]}>
        {cards.map((card) => (
          <Col xs={24} sm={8} key={card.title}>
            <Card size="small">
              <Statistic
                title={card.title}
                value={card.value}
                suffix={card.suffix}
                prefix={<span style={{ color: card.color }}>{card.icon}</span>}
                loading={loadingStats}
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Card size="small" style={{ marginTop: 16 }}>
        <Space size={24} wrap>
          <Typography.Text type="secondary">
            <StarOutlined /> 收获收藏 {stats?.collectCount ?? 0}
          </Typography.Text>
          <Typography.Text type="secondary">
            <CommentOutlined /> 收获评论 {stats?.commentCount ?? 0}
          </Typography.Text>
          <Typography.Text type="secondary">
            <ThunderboltOutlined /> 活跃天数 {stats?.activeDays ?? 0}
          </Typography.Text>
          <Typography.Text type="secondary">
            累计字数 {stats?.wordCount ?? 0}
          </Typography.Text>
          <Typography.Text type="secondary">
            本月新增 {stats?.monthNoteCount ?? 0} 篇
          </Typography.Text>
          <Typography.Text type="secondary">
            最长连击 {stats?.longestStreak ?? 0} 天
          </Typography.Text>
        </Space>
      </Card>

      <Card
        title="刷题热力图"
        style={{ marginTop: 16 }}
        extra={
          <Select
            aria-label="热力图年份"
            size="small"
            value={year}
            options={YEAR_OPTIONS}
            onChange={setYear}
            style={{ width: 100 }}
          />
        }
      >
        {loadingHeatmap ? (
          <Skeleton active paragraph={{ rows: 5 }} />
        ) : heatmap ? (
          <>
            <Heatmap year={heatmap.year} days={heatmap.days} />
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              全年 {heatmap.total} 篇 · 活跃 {heatmap.activeDays} 天 · 单日最多 {heatmap.max} 篇
            </Typography.Text>
          </>
        ) : (
          <Empty description="暂无热力图数据" />
        )}
      </Card>
    </>
  );
}

import { useEffect, useState } from 'react';
import { App, Card, Col, Row, Statistic, Table, Tag, Typography } from 'antd';
import {
  FileAddOutlined,
  FileTextOutlined,
  LoginOutlined,
  TeamOutlined,
  UserAddOutlined,
  UserOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { adminApi } from '@/domain/admin/api';
import type { StatisticVO } from '@/domain/statistic/types';

/** 数据看板: 今日 6 指标卡片 + 每日快照趋势表 */
export default function DashboardPage() {
  const { message } = App.useApp();
  const [data, setData] = useState<StatisticVO | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi
      .getStatistic(30)
      .then(setData)
      .catch((e) => message.error(e instanceof Error ? e.message : '看板加载失败'))
      .finally(() => setLoading(false));
  }, [message]);

  const today = data?.today;

  return (
    <div>
      <Typography.Title level={4}>数据看板</Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={12} md={8} lg={4}>
          <Card loading={loading}>
            <Statistic title="今日登录" value={today?.loginCount ?? 0} prefix={<LoginOutlined />} />
          </Card>
        </Col>
        <Col xs={12} md={8} lg={4}>
          <Card loading={loading}>
            <Statistic title="今日注册" value={today?.registerCount ?? 0} prefix={<UserAddOutlined />} />
          </Card>
        </Col>
        <Col xs={12} md={8} lg={4}>
          <Card loading={loading}>
            <Statistic title="累计注册" value={today?.totalRegisterCount ?? 0} prefix={<TeamOutlined />} />
          </Card>
        </Col>
        <Col xs={12} md={8} lg={4}>
          <Card loading={loading}>
            <Statistic title="今日笔记" value={today?.noteCount ?? 0} prefix={<FileAddOutlined />} />
          </Card>
        </Col>
        <Col xs={12} md={8} lg={4}>
          <Card loading={loading}>
            <Statistic title="今日提交人数" value={today?.submitNoteCount ?? 0} prefix={<UserOutlined />} />
          </Card>
        </Col>
        <Col xs={12} md={8} lg={4}>
          <Card loading={loading}>
            <Statistic title="累计笔记" value={today?.totalNoteCount ?? 0} prefix={<FileTextOutlined />} />
          </Card>
        </Col>
      </Row>

      <Card
        title="每日快照趋势"
        style={{ marginTop: 24 }}
        extra={
          <Typography.Text type="secondary">
            每日 23:59 定时落库 · 共 {data?.trend.length ?? 0} 天
          </Typography.Text>
        }
      >
        <Table
          rowKey="date"
          loading={loading}
          dataSource={[...(data?.trend ?? [])].reverse()}
          pagination={{ pageSize: 10, hideOnSinglePage: true }}
          locale={{ emptyText: '暂无快照(今日 23:59 后生成)' }}
          columns={[
            {
              title: '日期',
              dataIndex: 'date',
              render: (d: string) => (
                <Tag color={dayjs(d).isSame(dayjs(), 'day') ? 'blue' : undefined}>{d}</Tag>
              ),
            },
            { title: '登录', dataIndex: 'loginCount' },
            { title: '注册', dataIndex: 'registerCount' },
            { title: '新增笔记', dataIndex: 'noteCount' },
          ]}
        />
      </Card>
    </div>
  );
}

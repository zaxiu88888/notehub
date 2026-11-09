import { useCallback, useEffect, useState } from 'react';
import { App, Card, Input, Space, Switch, Table, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import { adminApi } from '@/domain/admin/api';
import type { UserVO } from '@/domain/user/types';

/** 用户管理: 账号搜索 / 分页 / 封禁开关 */
export default function UsersAdminPage() {
  const { message } = App.useApp();
  const [rows, setRows] = useState<UserVO[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [account, setAccount] = useState('');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminApi.listUsers({
        account: keyword || undefined,
        page,
        pageSize: 10,
      });
      setRows(data.list);
      setTotal(data.total);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '用户加载失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, page, message]);

  useEffect(() => {
    load();
  }, [load]);

  const toggleBan = async (record: UserVO, checked: boolean) => {
    try {
      await adminApi.banUser(record.userId, checked ? 1 : 0);
      message.success(checked ? `已封禁 ${record.username}` : `已解封 ${record.username}`);
      await load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '操作失败');
    }
  };

  return (
    <div>
      <Typography.Title level={4}>用户管理</Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Input.Search
            aria-label="搜索账号"
            placeholder="搜索账号"
            allowClear
            value={account}
            onChange={(e) => setAccount(e.target.value)}
            onSearch={(v) => {
              setKeyword(v.trim());
              setPage(1);
            }}
            style={{ width: 260 }}
          />
          <Typography.Text type="secondary">
            封禁后该用户无法登录, 已登录会话按匿名处理
          </Typography.Text>
        </Space>

        <Table
          rowKey="userId"
          loading={loading}
          dataSource={rows}
          pagination={{
            current: page,
            pageSize: 10,
            total,
            onChange: setPage,
            showSizeChanger: false,
          }}
          columns={[
            { title: 'ID', dataIndex: 'userId', width: 70 },
            { title: '账号', dataIndex: 'account', render: (a: string | null) => a ?? '-' },
            { title: '昵称', dataIndex: 'username' },
            {
              title: '角色',
              width: 100,
              render: (_: unknown, r: UserVO) =>
                r.isAdmin === 1 ? <Tag color="gold">管理员</Tag> : <Tag>用户</Tag>,
            },
            {
              title: '状态',
              width: 100,
              render: (_: unknown, r: UserVO) =>
                r.isAdmin === 1 ? (
                  <Tag color="gold">管理员</Tag>
                ) : r.isBanned === 1 ? (
                  <Tag color="red">已封禁</Tag>
                ) : (
                  <Tag color="green">正常</Tag>
                ),
            },
            {
              title: '注册时间',
              dataIndex: 'createdAt',
              width: 140,
              render: (d: string) => dayjs(d).format('YYYY-MM-DD'),
            },
            {
              title: '封禁',
              width: 100,
              render: (_: unknown, r: UserVO) =>
                r.isAdmin === 1 ? (
                  <Typography.Text type="secondary">不可操作</Typography.Text>
                ) : (
                  <Switch
                    aria-label={`封禁-${r.username}`}
                    checked={r.isBanned === 1}
                    checkedChildren="已封禁"
                    unCheckedChildren="正常"
                    onChange={(checked) => toggleBan(r, checked)}
                  />
                ),
            },
          ]}
        />
      </Card>
    </div>
  );
}

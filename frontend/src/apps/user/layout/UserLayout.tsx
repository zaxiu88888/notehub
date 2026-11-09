import { useEffect, useState } from 'react';
import { Avatar, Badge, Button, Dropdown, Layout, Menu, Space, Typography, App } from 'antd';
import { MessageOutlined, UserOutlined } from '@ant-design/icons';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { UNAUTHORIZED_EVENT } from '@/request/http';
import { getUnreadCount } from '@/domain/note/api';
import { fetchWhoamiThunk, logout } from '@/store/userSlice';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import LoginModal from '@/domain/user/components/LoginModal';

const { Header, Content, Footer } = Layout;

/** 用户端整体布局: 顶部导航(消息角标 30s 轮询) + 内容区 + 页脚 */
export default function UserLayout() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { message } = App.useApp();
  const { token, user } = useAppSelector((s) => s.user);
  const [loginOpen, setLoginOpen] = useState(false);
  const [unread, setUnread] = useState(0);

  // 有 token 时恢复登录态
  useEffect(() => {
    if (token && !user) {
      dispatch(fetchWhoamiThunk());
    }
  }, [token, user, dispatch]);

  // 未读消息轮询(30s): 登录后开启, 退出即停; I3 迭代会换成 WebSocket 推送
  useEffect(() => {
    if (!token) {
      setUnread(0);
      return;
    }
    let cancelled = false;
    const pull = () => {
      getUnreadCount()
        .then((c) => {
          if (!cancelled) setUnread(c.total);
        })
        .catch(() => undefined);
    };
    pull();
    const timer = setInterval(pull, 30_000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [token]);

  // 全局会话失效: 弹登录框
  useEffect(() => {
    const onUnauthorized = () => setLoginOpen(true);
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  const handleLogout = () => {
    dispatch(logout());
    message.success('已退出登录');
    navigate('/');
  };

  const navItems = [
    { key: '/', label: <Link to="/">首页</Link> },
    { key: '/question-lists', label: <Link to="/question-lists">题单</Link> },
    ...(token
      ? [
          {
            key: '/messages',
            label: (
              <Link to="/messages">
                <Badge count={unread} size="small" offset={[6, -2]}>
                  <MessageOutlined /> 消息
                </Badge>
              </Link>
            ),
          },
        ]
      : []),
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 10,
          display: 'flex',
          alignItems: 'center',
          background: '#fff',
          boxShadow: '0 1px 4px rgba(0,21,41,.08)',
          paddingInline: 24,
        }}
      >
        <Space align="center" size={32} style={{ width: '100%' }}>
          <Typography.Title level={4} style={{ margin: 0, whiteSpace: 'nowrap' }}>
            <Link to="/">
              note<span style={{ color: '#2f54eb' }}>hub</span>
            </Link>
          </Typography.Title>
          <Menu
            mode="horizontal"
            selectedKeys={[location.pathname]}
            items={navItems}
            style={{ flex: 1, minWidth: 200, borderBottom: 'none' }}
          />
          {token && user ? (
            <Dropdown
              menu={{
                items: [
                  { key: 'center', label: '个人中心', onClick: () => navigate('/user-center') },
                  {
                    key: 'home',
                    label: '我的主页',
                    onClick: () => navigate(`/users/${user.userId}`),
                  },
                  { type: 'divider' },
                  { key: 'logout', label: '退出登录', onClick: handleLogout },
                ],
              }}
            >
              <Space style={{ cursor: 'pointer' }}>
                <Avatar src={user.avatarUrl ?? undefined} icon={<UserOutlined />} />
                <span>{user.username}</span>
              </Space>
            </Dropdown>
          ) : (
            <Button type="primary" onClick={() => setLoginOpen(true)}>
              登录 / 注册
            </Button>
          )}
        </Space>
      </Header>

      <Content style={{ maxWidth: 1200, width: '100%', margin: '0 auto', padding: '24px 16px' }}>
        <Outlet />
      </Content>

      <Footer style={{ textAlign: 'center', color: '#8c8c8c' }}>
        notehub · 刷题笔记社区 · 在题目下写下你的思考
      </Footer>

      <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
    </Layout>
  );
}

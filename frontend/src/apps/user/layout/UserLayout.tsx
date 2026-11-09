import { useEffect, useState } from 'react';
import { Avatar, Badge, Button, Dropdown, Input, Layout, Menu, Space, Typography, App } from 'antd';
import { MessageOutlined, UserOutlined } from '@ant-design/icons';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { UNAUTHORIZED_EVENT } from '@/request/http';
import { getUnreadCount } from '@/domain/note/api';
import { connectUnreadSocket } from '@/domain/notification/socket';
import { fetchWhoamiThunk, logout } from '@/store/userSlice';
import { resetNotification, setConnected, setUnread } from '@/store/notificationSlice';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import LoginModal from '@/domain/user/components/LoginModal';

const { Header, Content, Footer } = Layout;

/** 用户端整体布局: 顶部导航(WebSocket 实时未读角标) + 内容区 + 页脚 */
export default function UserLayout() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { message } = App.useApp();
  const { token, user } = useAppSelector((s) => s.user);
  const unread = useAppSelector((s) => s.notification.unread.total);
  const [loginOpen, setLoginOpen] = useState(false);
  const [searchValue, setSearchValue] = useState("");

  // 有 token 时恢复登录态
  useEffect(() => {
    if (token && !user) {
      dispatch(fetchWhoamiThunk());
    }
  }, [token, user, dispatch]);

  // I3 实时未读: WebSocket 推送 + 连接/重连时 HTTP 补拉一次(断线期间消息)
  useEffect(() => {
    if (!token) {
      dispatch(resetNotification());
      return;
    }
    let cancelled = false;
    const refresh = () => {
      getUnreadCount()
        .then((counts) => {
          if (!cancelled) dispatch(setUnread(counts));
        })
        .catch(() => undefined);
    };
    refresh();
    const socket = connectUnreadSocket({
      token,
      onUnread: (counts) => dispatch(setUnread(counts)),
      onConnected: () => {
        if (cancelled) return;
        dispatch(setConnected(true));
        refresh();
      },
      onDisconnected: () => {
        if (!cancelled) dispatch(setConnected(false));
      },
    });
    return () => {
      cancelled = true;
      socket.disconnect();
      dispatch(setConnected(false));
    };
  }, [token, dispatch]);

  // 全局会话失效: 弹登录框
  useEffect(() => {
    const onUnauthorized = () => setLoginOpen(true);
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  /** 顶栏搜索: 关键词跳转搜索结果页 */
  const submitSearch = (value: string) => {
    const keyword = value.trim();
    if (keyword) {
      navigate(`/search?keyword=${encodeURIComponent(keyword)}`);
      setSearchValue("");
    }
  };

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
          <Space.Compact>
            <Input
              aria-label="全局搜索"
              placeholder="搜索笔记 / 用户"
              allowClear
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
              onPressEnter={() => submitSearch(searchValue)}
              style={{ width: 220 }}
            />
            <Button type="primary" aria-label="搜索" onClick={() => submitSearch(searchValue)}>
              搜索
            </Button>
          </Space.Compact>
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

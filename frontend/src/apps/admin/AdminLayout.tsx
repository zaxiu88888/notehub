import { useEffect } from 'react';
import { App, Layout, Menu, Result, Spin, Typography } from 'antd';
import {
  DashboardOutlined,
  FileTextOutlined,
  FolderOutlined,
  ProfileOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { Link, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchWhoamiThunk } from '@/store/userSlice';

const { Header, Sider, Content } = Layout;

const MENU = [
  { key: '/admin', icon: <DashboardOutlined />, label: <Link to="/admin">数据看板</Link> },
  { key: '/admin/questions', icon: <FileTextOutlined />, label: <Link to="/admin/questions">题库管理</Link> },
  { key: '/admin/question-lists', icon: <ProfileOutlined />, label: <Link to="/admin/question-lists">题单管理</Link> },
  { key: '/admin/categories', icon: <FolderOutlined />, label: <Link to="/admin/categories">分类管理</Link> },
  { key: '/admin/users', icon: <TeamOutlined />, label: <Link to="/admin/users">用户管理</Link> },
];

/** 管理端布局: 侧边栏 + 权限守卫(非管理员拒入) */
export default function AdminLayout() {
  const dispatch = useAppDispatch();
  const { token, user } = useAppSelector((s) => s.user);
  const location = useLocation();
  const navigate = useNavigate();
  const { message } = App.useApp();

  // 直接进入 /admin 时用户端布局未挂载: 这里自行恢复登录信息, 否则会一直卡在"校验权限中"
  useEffect(() => {
    if (token && !user) {
      dispatch(fetchWhoamiThunk());
    }
  }, [token, user, dispatch]);

  useEffect(() => {
    if (token && user && user.isAdmin !== 1) {
      message.warning('需要管理员权限');
      navigate('/');
    }
  }, [token, user, message, navigate]);

  if (!token) return <Navigate to="/" replace />;
  if (!user) {
    return (
      <div style={{ padding: 80, textAlign: 'center' }}>
        <Spin tip="校验权限中..." />
      </div>
    );
  }
  if (user.isAdmin !== 1) {
    return <Result status="403" title="403" subTitle="需要管理员权限" extra={<Link to="/">回到首页</Link>} />;
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="light" width={200} style={{ borderRight: '1px solid #f0f0f0' }}>
        <div style={{ padding: '16px 20px' }}>
          <Typography.Title level={5} style={{ margin: 0 }}>
            <Link to="/admin">
              note<span style={{ color: '#2f54eb' }}>hub</span> 管理后台
            </Link>
          </Typography.Title>
        </div>
        <Menu mode="inline" selectedKeys={[location.pathname]} items={MENU} style={{ borderRight: 'none' }} />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)',
            paddingInline: 24,
          }}
        >
          <Typography.Text type="secondary">内容与数据管理</Typography.Text>
          <Link to="/">← 返回用户端</Link>
        </Header>
        <Content style={{ padding: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

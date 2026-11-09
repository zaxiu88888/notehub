import { useEffect, useState } from 'react';
import { Alert, App, Button, Form, Input, Modal, Tabs } from 'antd';
import { LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';
import { sendVerifyCode } from '@/domain/user/api';
import { ApiError } from '@/request/http';
import { loginThunk, registerThunk } from '@/store/userSlice';
import { useAppDispatch, useAppSelector } from '@/store/hooks';

interface Props {
  open: boolean;
  onClose: () => void;
}

/** 登录 / 注册弹窗: 注册走邮箱验证码(dev 模式后端会直接回 devCode) */
export default function LoginModal({ open, onClose }: Props) {
  const dispatch = useAppDispatch();
  const loading = useAppSelector((s) => s.user.loading);
  const { message } = App.useApp();
  const [tab, setTab] = useState('login');
  const [sending, setSending] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [devCode, setDevCode] = useState<string | null>(null);

  const [loginForm] = Form.useForm();
  const [registerForm] = Form.useForm();

  // 60s 重发倒计时(与后端限流一致)
  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [countdown]);

  const handleSendCode = async () => {
    const email = registerForm.getFieldValue('email') as string | undefined;
    if (!email) {
      message.warning('请先填写邮箱');
      return;
    }
    setSending(true);
    try {
      const result = await sendVerifyCode(email);
      setCountdown(60);
      if (result.devCode) {
        setDevCode(result.devCode);
        registerForm.setFieldValue('code', result.devCode);
        message.success('开发模式: 验证码已自动填充');
      } else {
        message.success('验证码已发送, 请查收邮箱');
      }
    } catch (e) {
      message.error(e instanceof ApiError ? e.message : '发送失败');
    } finally {
      setSending(false);
    }
  };

  const handleLogin = async () => {
    const values = await loginForm.validateFields();
    try {
      await dispatch(loginThunk(values)).unwrap();
      message.success('登录成功');
      onClose();
    } catch (e) {
      message.error(e instanceof ApiError ? e.message : '登录失败');
    }
  };

  const handleRegister = async () => {
    const values = await registerForm.validateFields();
    try {
      await dispatch(registerThunk(values)).unwrap();
      message.success('注册成功, 已自动登录');
      onClose();
    } catch (e) {
      message.error(e instanceof ApiError ? e.message : '注册失败');
    }
  };

  return (
    <Modal open={open} onCancel={onClose} footer={null} width={420} destroyOnHidden>
      <Tabs
        activeKey={tab}
        onChange={setTab}
        items={[
          {
            key: 'login',
            label: '登录',
            children: (
              <Form form={loginForm} layout="vertical" onFinish={handleLogin}>
                <Form.Item
                  name="account"
                  label="账号"
                  rules={[{ required: true, message: '请输入账号' }]}
                >
                  <Input prefix={<UserOutlined />} placeholder="admin / demo" autoComplete="username" />
                </Form.Item>
                <Form.Item
                  name="password"
                  label="密码"
                  rules={[{ required: true, message: '请输入密码' }]}
                >
                  <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
                </Form.Item>
                <Button type="primary" htmlType="submit" block loading={loading}>
                  登录
                </Button>
              </Form>
            ),
          },
          {
            key: 'register',
            label: '注册',
            children: (
              <Form form={registerForm} layout="vertical" onFinish={handleRegister}>
                {devCode && (
                  <Alert
                    type="info"
                    showIcon
                    style={{ marginBottom: 12 }}
                    message={`开发模式验证码: ${devCode}`}
                  />
                )}
                <Form.Item
                  name="email"
                  label="邮箱"
                  rules={[
                    { required: true, message: '请输入邮箱' },
                    { type: 'email', message: '邮箱格式不正确' },
                  ]}
                >
                  <Input prefix={<MailOutlined />} placeholder="用于接收验证码" />
                </Form.Item>
                <Form.Item label="验证码" required>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Form.Item
                      name="code"
                      noStyle
                      rules={[{ required: true, message: '请输入验证码' }]}
                    >
                      <Input placeholder="6 位数字" maxLength={6} />
                    </Form.Item>
                    <Button onClick={handleSendCode} loading={sending} disabled={countdown > 0}>
                      {countdown > 0 ? `${countdown}s` : '获取验证码'}
                    </Button>
                  </div>
                </Form.Item>
                <Form.Item
                  name="account"
                  label="账号"
                  rules={[
                    { required: true, message: '请输入账号' },
                    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '4-20 位字母/数字/下划线' },
                  ]}
                >
                  <Input prefix={<UserOutlined />} autoComplete="username" />
                </Form.Item>
                <Form.Item
                  name="username"
                  label="昵称"
                  rules={[{ required: true, message: '请输入昵称' }]}
                >
                  <Input maxLength={30} />
                </Form.Item>
                <Form.Item
                  name="password"
                  label="密码"
                  rules={[
                    { required: true, message: '请输入密码' },
                    { min: 6, message: '至少 6 位' },
                  ]}
                >
                  <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
                </Form.Item>
                <Button type="primary" htmlType="submit" block loading={loading}>
                  注册并登录
                </Button>
              </Form>
            ),
          },
        ]}
      />
    </Modal>
  );
}

import { useEffect, useState } from 'react';
import { App, Avatar, Button, Col, DatePicker, Form, Input, Row, Select, Space, Upload } from 'antd';
import { UploadOutlined, UserOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { UploadProps } from 'antd';
import { updateMe, uploadAvatar } from '@/domain/user/api';
import { fetchWhoamiThunk } from '@/store/userSlice';
import { useAppDispatch, useAppSelector } from '@/store/hooks';

/** 个人资料: 头像上传 + 基本资料编辑 */
export default function ProfilePanel() {
  const { message } = App.useApp();
  const dispatch = useAppDispatch();
  const user = useAppSelector((s) => s.user.user);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    if (user) {
      form.setFieldsValue({
        username: user.username,
        gender: user.gender,
        birthday: user.birthday ? dayjs(user.birthday) : null,
        school: user.school,
        signature: user.signature,
      });
    }
  }, [user, form]);

  const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
    setUploading(true);
    try {
      await uploadAvatar(file);
      await dispatch(fetchWhoamiThunk()).unwrap();
      message.success('头像已更新');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '头像上传失败');
    } finally {
      setUploading(false);
    }
    return false; // 阻止 antd 自动上传
  };

  const handleSave = async (values: {
    username?: string;
    gender?: number;
    birthday?: dayjs.Dayjs | null;
    school?: string;
    signature?: string;
  }) => {
    setSaving(true);
    try {
      await updateMe({
        username: values.username,
        gender: values.gender,
        birthday: values.birthday ? values.birthday.format('YYYY-MM-DD') : undefined,
        school: values.school,
        signature: values.signature,
      });
      await dispatch(fetchWhoamiThunk()).unwrap();
      message.success('资料已保存');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Row gutter={32}>
      <Col xs={24} md={7} style={{ textAlign: 'center' }}>
        <Avatar size={96} src={user?.avatarUrl ?? undefined} icon={<UserOutlined />} />
        <div style={{ marginTop: 12 }}>
          <Upload showUploadList={false} beforeUpload={beforeUpload} accept="image/*">
            <Button icon={<UploadOutlined />} loading={uploading}>
              更换头像
            </Button>
          </Upload>
        </div>
        <div style={{ marginTop: 8, color: '#8c8c8c', fontSize: 12 }}>
          支持 jpg/png/gif/webp, 不超过 5MB
        </div>
      </Col>
      <Col xs={24} md={17}>
        <Form form={form} layout="vertical" onFinish={handleSave} style={{ maxWidth: 480 }}>
          <Form.Item label="账号">
            <Input value={user?.account ?? '-'} disabled />
          </Form.Item>
          <Form.Item
            name="username"
            label="昵称"
            rules={[{ required: true, message: '昵称不能为空' }, { max: 30 }]}
          >
            <Input />
          </Form.Item>
          <Space size={24} style={{ display: 'flex' }}>
            <Form.Item name="gender" label="性别" style={{ flex: 1 }}>
              <Select
                aria-label="性别"
                options={[
                  { value: 0, label: '保密' },
                  { value: 1, label: '男' },
                  { value: 2, label: '女' },
                ]}
              />
            </Form.Item>
            <Form.Item name="birthday" label="生日" style={{ flex: 1 }}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item name="school" label="学校">
            <Input maxLength={64} placeholder="选填" />
          </Form.Item>
          <Form.Item name="signature" label="个性签名">
            <Input.TextArea maxLength={128} autoSize={{ minRows: 2, maxRows: 4 }} placeholder="选填" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={saving}>
            保存资料
          </Button>
        </Form>
      </Col>
    </Row>
  );
}

import { useEffect, useState } from 'react';
import { Card, Tabs } from 'antd';
import { useNavigate } from 'react-router-dom';
import ProfilePanel from './panels/ProfilePanel';
import MyNotesPanel from './panels/MyNotesPanel';
import CollectionsPanel from './panels/CollectionsPanel';
import { useAppSelector } from '@/store/hooks';
import { UNAUTHORIZED_EVENT } from '@/request/http';

/** 个人中心: 资料 / 我的笔记 / 我的收藏 */
export default function UserCenterPage() {
  const navigate = useNavigate();
  const { token } = useAppSelector((s) => s.user);
  const [tab, setTab] = useState('profile');

  // 未登录: 回首页并主动弹登录框(而不是静默跳走)
  useEffect(() => {
    if (!token) {
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
      navigate('/');
    }
  }, [token, navigate]);

  return (
    <Card>
      <Tabs
        activeKey={tab}
        onChange={setTab}
        items={[
          { key: 'profile', label: '个人资料', children: <ProfilePanel /> },
          { key: 'notes', label: '我的笔记', children: <MyNotesPanel /> },
          { key: 'collections', label: '我的收藏', children: <CollectionsPanel /> },
        ]}
      />
    </Card>
  );
}

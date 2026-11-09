import { useEffect, useState } from 'react';
import { App, Card, Col, Empty, List, Row, Skeleton, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router-dom';
import { getQuestionListItems, getQuestionLists } from '@/domain/question/api';
import type { QuestionListItemVO, QuestionListVO } from '@/domain/question/types';
import DifficultyTag from '@/base/components/DifficultyTag';
import { ApiError } from '@/request/http';
import { useAppSelector } from '@/store/hooks';

/** 题单页: 左侧题单列表, 右侧选中题单的题目(含我的刷题进度) */
export default function QuestionListPage() {
  const { message } = App.useApp();
  const token = useAppSelector((s) => s.user.token);
  const [lists, setLists] = useState<QuestionListVO[]>([]);
  const [activeId, setActiveId] = useState<number | undefined>();
  const [items, setItems] = useState<QuestionListItemVO[]>([]);
  const [loadingLists, setLoadingLists] = useState(true);
  const [loadingItems, setLoadingItems] = useState(false);

  useEffect(() => {
    getQuestionLists()
      .then((data) => {
        setLists(data);
        if (data.length > 0) setActiveId(data[0].questionListId);
      })
      .catch((e) => message.error(e instanceof ApiError ? e.message : '题单加载失败'))
      .finally(() => setLoadingLists(false));
  }, [message]);

  useEffect(() => {
    if (activeId == null) return;
    setLoadingItems(true);
    // 未登录会返回 40101: 展示"登录后查看进度"的引导, 不弹错误
    getQuestionListItems(activeId)
      .then(setItems)
      .catch((e) => {
        if (e instanceof ApiError && e.code === 40101) {
          setItems([]);
        } else {
          message.error(e instanceof ApiError ? e.message : '题目加载失败');
        }
      })
      .finally(() => setLoadingItems(false));
  }, [activeId, message, token]); // 登录态变化后重新拉取(刷题进度依赖登录)

  const active = lists.find((l) => l.questionListId === activeId);
  const doneCount = items.filter((i) => i.hasMyNote).length;

  return (
    <Row gutter={[24, 24]}>
      <Col xs={24} md={9} lg={8}>
        <Card title="题单">
          {loadingLists ? (
            <Skeleton active />
          ) : (
            <List
              dataSource={lists}
              locale={{ emptyText: <Empty description="暂无题单" /> }}
              renderItem={(list) => (
                <List.Item
                  onClick={() => setActiveId(list.questionListId)}
                  style={{
                    cursor: 'pointer',
                    background:
                      list.questionListId === activeId ? '#f0f5ff' : undefined,
                    borderRadius: 6,
                    paddingInline: 12,
                  }}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        {list.name}
                        <Tag color={list.type === 1 ? 'gold' : 'blue'}>
                          {list.type === 1 ? '官方推荐' : '专题'}
                        </Tag>
                      </Space>
                    }
                    description={
                      <Typography.Text type="secondary">
                        {list.description ?? '暂无描述'} · {list.questionCount} 题
                      </Typography.Text>
                    }
                  />
                </List.Item>
              )}
            />
          )}
        </Card>
      </Col>

      <Col xs={24} md={15} lg={16}>
        <Card
          title={active ? active.name : '题单详情'}
          extra={
            items.length > 0 && (
              <Typography.Text type="secondary">
                进度 {doneCount}/{items.length}
              </Typography.Text>
            )
          }
        >
          {loadingItems ? (
            <Skeleton active paragraph={{ rows: 6 }} />
          ) : items.length === 0 ? (
            <Empty description="暂无题目或需要登录后查看" />
          ) : (
            <List
              dataSource={items}
              renderItem={(item, index) => (
                <List.Item
                  actions={[
                    item.hasMyNote ? (
                      <Tag color="blue" key="done">
                        已写笔记
                      </Tag>
                    ) : (
                      <Typography.Text type="secondary" key="todo">
                        待完成
                      </Typography.Text>
                    ),
                  ]}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <Typography.Text type="secondary">{index + 1}.</Typography.Text>
                        <DifficultyTag difficulty={item.difficulty} />
                        <Link to={`/questions/${item.questionId}`}>{item.title}</Link>
                      </Space>
                    }
                    description={
                      <Space size={8} wrap>
                        <Tag>{item.categoryName ?? '未分类'}</Tag>
                        {item.examPoint && (
                          <Typography.Text type="secondary">考点: {item.examPoint}</Typography.Text>
                        )}
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          )}
        </Card>
      </Col>
    </Row>
  );
}

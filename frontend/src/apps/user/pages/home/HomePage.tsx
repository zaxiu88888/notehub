import { useCallback, useEffect, useState } from 'react';
import {
  App,
  Button,
  Card,
  Col,
  Empty,
  List,
  Pagination,
  Row,
  Select,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd';
import { CommentOutlined, FireOutlined, LikeOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import DifficultyTag from '@/base/components/DifficultyTag';
import { getCategoryTree, getQuestions } from '@/domain/question/api';
import type { CategoryVO, QuestionVO } from '@/domain/question/types';
import { getNotes } from '@/domain/note/api';
import type { NoteVO } from '@/domain/note/types';
import { ApiError } from '@/request/http';
import dayjs from 'dayjs';

const PAGE_SIZE = 10;

interface FlatCategory {
  value: number;
  label: string;
}

function flattenCategories(tree: CategoryVO[], depth = 0): FlatCategory[] {
  return tree.flatMap((c) => [
    { value: c.categoryId, label: `${'　'.repeat(depth)}${c.name}` },
    ...flattenCategories(c.children ?? [], depth + 1),
  ]);
}

/** 首页: 左侧题库(可筛选), 右侧社区最新笔记 */
export default function HomePage() {
  const { message } = App.useApp();
  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [difficulty, setDifficulty] = useState<number | undefined>();
  const [page, setPage] = useState(1);
  const [questions, setQuestions] = useState<QuestionVO[]>([]);
  const [total, setTotal] = useState(0);
  const [notes, setNotes] = useState<NoteVO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getCategoryTree()
      .then((tree) => setCategories(flattenCategories(tree)))
      .catch(() => message.error('分类加载失败, 请确认后端已启动'));
  }, [message]);

  const loadQuestions = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getQuestions({ categoryId, difficulty, page, pageSize: PAGE_SIZE });
      setQuestions(data.list);
      setTotal(data.total);
    } catch (e) {
      message.error(e instanceof ApiError ? e.message : '题目加载失败');
    } finally {
      setLoading(false);
    }
  }, [categoryId, difficulty, page, message]);

  useEffect(() => {
    loadQuestions();
  }, [loadQuestions]);

  useEffect(() => {
    getNotes({ page: 1, pageSize: 8, sort: 'latest' })
      .then((data) => setNotes(data.list))
      .catch(() => {
        /* 未登录也可看笔记流, 失败静默(题目区已有错误提示) */
      });
  }, []);

  const handleFilterChange = (nextCategory?: number, nextDifficulty?: number) => {
    setCategoryId(nextCategory);
    setDifficulty(nextDifficulty);
    setPage(1);
  };

  return (
    <Row gutter={[24, 24]}>
      <Col xs={24} lg={15}>
        <Card
          title={
            <Space>
              <FireOutlined style={{ color: '#fa541c' }} />
              题库
            </Space>
          }
          extra={
            <Space wrap>
              <Select
                allowClear
                aria-label="分类筛选"
                placeholder="全部分类"
                style={{ width: 180 }}
                options={categories}
                value={categoryId}
                onChange={(v) => handleFilterChange(v, difficulty)}
              />
              <Select
                allowClear
                aria-label="难度筛选"
                placeholder="难度"
                style={{ width: 110 }}
                value={difficulty}
                onChange={(v) => handleFilterChange(categoryId, v)}
                options={[
                  { value: 1, label: '简单' },
                  { value: 2, label: '中等' },
                  { value: 3, label: '困难' },
                ]}
              />
            </Space>
          }
          styles={{ body: { paddingTop: 8 } }}
        >
          {loading ? (
            <Skeleton active paragraph={{ rows: 6 }} />
          ) : (
            <List
              dataSource={questions}
              locale={{ emptyText: <Empty description="该筛选条件下暂无题目" /> }}
              renderItem={(q) => (
                <List.Item
                  actions={[
                    <Typography.Text type="secondary" key="views">
                      {q.viewCount} 次浏览
                    </Typography.Text>,
                  ]}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <DifficultyTag difficulty={q.difficulty} />
                        <Link to={`/questions/${q.questionId}`}>{q.title}</Link>
                      </Space>
                    }
                    description={
                      <Space size={8} wrap>
                        <Tag>{q.categoryName ?? '未分类'}</Tag>
                        {q.examPoint && (
                          <Typography.Text type="secondary">
                            考点: {q.examPoint}
                          </Typography.Text>
                        )}
                        {q.myNoteId != null && <Tag color="blue">已写笔记</Tag>}
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          )}
          <div style={{ textAlign: 'right', marginTop: 16 }}>
            <Pagination
              current={page}
              pageSize={PAGE_SIZE}
              total={total}
              showSizeChanger={false}
              onChange={setPage}
              hideOnSinglePage
            />
          </div>
        </Card>
      </Col>

      <Col xs={24} lg={9}>
        <Card title="最新笔记" styles={{ body: { paddingTop: 8 } }}>
          <List
            dataSource={notes}
            locale={{ emptyText: <Empty description="还没有人写笔记, 来做第一个吧" /> }}
            renderItem={(note) => (
              <List.Item>
                <List.Item.Meta
                  title={
                    <Space size={6}>
                      <span>{note.author.username}</span>
                      <Typography.Text type="secondary" style={{ fontWeight: 'normal' }}>
                        写于《{note.question.title}》
                      </Typography.Text>
                    </Space>
                  }
                  description={
                    <Space direction="vertical" size={4} style={{ width: '100%' }}>
                      <Typography.Paragraph
                        ellipsis={{ rows: 2 }}
                        style={{ marginBottom: 0, color: 'rgba(0,0,0,.65)' }}
                      >
                        {note.content}
                      </Typography.Paragraph>
                      <Space size={12} style={{ color: '#8c8c8c', fontSize: 12 }}>
                        <span>
                          <LikeOutlined /> {note.likeCount}
                        </span>
                        <span>
                          <CommentOutlined /> {note.commentCount}
                        </span>
                        <span>{dayjs(note.createdAt).format('MM-DD HH:mm')}</span>
                      </Space>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
          {notes.length > 0 && (
            <Link to="/question-lists">
              <Button type="link" block>
                浏览题单 →
              </Button>
            </Link>
          )}
        </Card>
      </Col>
    </Row>
  );
}

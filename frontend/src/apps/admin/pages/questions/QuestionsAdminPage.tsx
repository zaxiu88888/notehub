import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  App,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Typography,
} from 'antd';
import { ImportOutlined, PlusOutlined } from '@ant-design/icons';
import { adminApi } from '@/domain/admin/api';
import { getCategoryTree } from '@/domain/question/api';
import type { CategoryVO, QuestionVO } from '@/domain/question/types';
import DifficultyTag from '@/base/components/DifficultyTag';

interface FlatCategory {
  value: number;
  label: string;
}

function flatten(tree: CategoryVO[], depth = 0): FlatCategory[] {
  return tree.flatMap((c) => [
    { value: c.categoryId, label: `${'　'.repeat(depth)}${c.name}` },
    ...flatten(c.children ?? [], depth + 1),
  ]);
}

interface QuestionFormValues {
  categoryId: number;
  title: string;
  difficulty: number;
  examPoint?: string;
  description?: string;
}

/** 题库管理: 列表筛选 + 新建/编辑 + 删除 + Markdown 批量导入 */
export default function QuestionsAdminPage() {
  const { message } = App.useApp();
  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [rows, setRows] = useState<QuestionVO[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [difficulty, setDifficulty] = useState<number | undefined>();
  const [loading, setLoading] = useState(true);

  const [editOpen, setEditOpen] = useState(false);
  const [editing, setEditing] = useState<QuestionVO | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<QuestionFormValues>();

  const [importOpen, setImportOpen] = useState(false);
  const [importCategory, setImportCategory] = useState<number | undefined>();
  const [importMarkdown, setImportMarkdown] = useState('');
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<{
    total: number;
    success: number;
    failures: { title: string; reason: string }[];
  } | null>(null);

  useEffect(() => {
    getCategoryTree()
      .then((tree) => setCategories(flatten(tree)))
      .catch(() => message.error('分类加载失败'));
  }, [message]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminApi.listQuestions({ categoryId, difficulty, page, pageSize: 10 });
      setRows(data.list);
      setTotal(data.total);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [categoryId, difficulty, page, message]);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ difficulty: 1, categoryId: categories[0]?.value });
    setEditOpen(true);
  };

  const openEdit = (record: QuestionVO) => {
    setEditing(record);
    form.setFieldsValue({
      categoryId: record.categoryId,
      title: record.title,
      difficulty: record.difficulty,
      examPoint: record.examPoint ?? undefined,
      description: record.description ?? undefined,
    });
    setEditOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editing) {
        await adminApi.updateQuestion(editing.questionId, values);
        message.success('题目已更新');
        await load();
      } else {
        await adminApi.createQuestion(values);
        message.success('题目已创建');
        // 新建题 ID 最大, 主动跳末页, 管理员立刻看到结果
        const lastPage = Math.max(1, Math.ceil((total + 1) / 10));
        if (lastPage === page) await load();
        else setPage(lastPage);
      }
      setEditOpen(false);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (record: QuestionVO) => {
    try {
      await adminApi.deleteQuestion(record.questionId);
      message.success('题目已删除');
      await load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败');
    }
  };

  const handleImport = async () => {
    if (importCategory == null || !importMarkdown.trim()) {
      message.warning('请选择分类并填写 Markdown');
      return;
    }
    setImporting(true);
    try {
      const result = await adminApi.batchImportQuestions({
        categoryId: importCategory,
        markdown: importMarkdown,
      });
      setImportResult(result);
      message.success(`导入完成: 成功 ${result.success} 题`);
      await load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '导入失败');
    } finally {
      setImporting(false);
    }
  };

  return (
    <div>
      <Typography.Title level={4}>题库管理</Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }} wrap>
          <Select
            allowClear
            aria-label="分类筛选"
            placeholder="全部分类"
            style={{ width: 200 }}
            options={categories}
            value={categoryId}
            onChange={(v) => {
              setCategoryId(v);
              setPage(1);
            }}
          />
          <Select
            allowClear
            aria-label="难度筛选"
            placeholder="难度"
            style={{ width: 120 }}
            value={difficulty}
            onChange={(v) => {
              setDifficulty(v);
              setPage(1);
            }}
            options={[
              { value: 1, label: '简单' },
              { value: 2, label: '中等' },
              { value: 3, label: '困难' },
            ]}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建题目
          </Button>
          <Button icon={<ImportOutlined />} onClick={() => setImportOpen(true)}>
            Markdown 批量导入
          </Button>
        </Space>

        <Table
          rowKey="questionId"
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
            { title: 'ID', dataIndex: 'questionId', width: 70 },
            { title: '标题', dataIndex: 'title' },
            {
              title: '难度',
              dataIndex: 'difficulty',
              width: 90,
              render: (d: number) => <DifficultyTag difficulty={d} />,
            },
            { title: '分类', dataIndex: 'categoryName', width: 140 },
            {
              title: '考点',
              dataIndex: 'examPoint',
              render: (p: string | null) => p ?? '-',
            },
            { title: '浏览', dataIndex: 'viewCount', width: 80 },
            {
              title: '操作',
              width: 160,
              render: (_: unknown, record: QuestionVO) => (
                <Space>
                  <Button type="link" size="small" onClick={() => openEdit(record)}>
                    编辑
                  </Button>
                  <Popconfirm
                    title="删除后不可恢复(有笔记会被拒绝), 确定?"
                    onConfirm={() => handleDelete(record)}
                    okText="删除"
                  >
                    <Button type="link" size="small" danger>
                      删除
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        open={editOpen}
        title={editing ? '编辑题目' : '新建题目'}
        onCancel={() => setEditOpen(false)}
        onOk={handleSave}
        confirmLoading={saving}
        destroyOnHidden
        width={640}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="categoryId" label="分类" rules={[{ required: true, message: '请选择分类' }]}>
            <Select options={categories} />
          </Form.Item>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input maxLength={128} />
          </Form.Item>
          <Form.Item name="difficulty" label="难度" rules={[{ required: true, message: '请选择难度' }]}>
            <Select
              options={[
                { value: 1, label: '简单' },
                { value: 2, label: '中等' },
                { value: 3, label: '困难' },
              ]}
            />
          </Form.Item>
          <Form.Item name="examPoint" label="考点">
            <Input maxLength={128} placeholder="分号分隔, 如: 数组;哈希表" />
          </Form.Item>
          <Form.Item name="description" label="题干(Markdown)">
            <Input.TextArea autoSize={{ minRows: 5, maxRows: 10 }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={importOpen}
        title="Markdown 批量导入题目"
        onCancel={() => {
          setImportOpen(false);
          setImportResult(null);
        }}
        onOk={handleImport}
        okText="开始导入"
        confirmLoading={importing}
        destroyOnHidden
        width={720}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <Select
            aria-label="导入到分类"
            placeholder="选择目标分类"
            style={{ width: 240 }}
            options={categories}
            value={importCategory}
            onChange={setImportCategory}
          />
          <Input.TextArea
            aria-label="导入 Markdown"
            value={importMarkdown}
            onChange={(e) => setImportMarkdown(e.target.value)}
            autoSize={{ minRows: 10, maxRows: 16 }}
            placeholder={'# 两数之和\n难度：简单\n考点：数组；哈希表\n题干正文...'}
          />
          {importResult && (
            <Alert
              type={importResult.failures.length > 0 ? 'warning' : 'success'}
              message={`共 ${importResult.total} 题, 成功 ${importResult.success} 题`}
              description={
                importResult.failures.length > 0 && (
                  <ul style={{ margin: 0, paddingLeft: 18 }}>
                    {importResult.failures.map((f, idx) => (
                      <li key={idx}>
                        {f.title || '(无标题)'}: {f.reason}
                      </li>
                    ))}
                  </ul>
                )
              }
            />
          )}
        </Space>
      </Modal>
    </div>
  );
}

import { useCallback, useEffect, useState } from 'react';
import {
  App,
  Button,
  Card,
  Drawer,
  Form,
  Input,
  List,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import { ArrowDownOutlined, ArrowUpOutlined, PlusOutlined } from '@ant-design/icons';
import { adminApi } from '@/domain/admin/api';
import type { QuestionListItemVO, QuestionListVO, QuestionVO } from '@/domain/question/types';
import DifficultyTag from '@/base/components/DifficultyTag';

interface ListFormValues {
  name: string;
  type: number;
  description?: string;
}

/** 题单管理: 题单 CRUD + 条目增删与排序 */
export default function QuestionListsAdminPage() {
  const { message } = App.useApp();
  const [lists, setLists] = useState<QuestionListVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<QuestionListVO | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<ListFormValues>();

  const [manageList, setManageList] = useState<QuestionListVO | null>(null);
  const [items, setItems] = useState<QuestionListItemVO[]>([]);
  const [itemsLoading, setItemsLoading] = useState(false);
  const [questions, setQuestions] = useState<QuestionVO[]>([]);
  const [pickedQuestionId, setPickedQuestionId] = useState<number | undefined>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setLists(await adminApi.listQuestionLists());
    } catch (e) {
      message.error(e instanceof Error ? e.message : '题单加载失败');
    } finally {
      setLoading(false);
    }
  }, [message]);

  const loadItems = useCallback(
    async (listId: number) => {
      setItemsLoading(true);
      try {
        setItems(await adminApi.listQuestionListItems(listId));
      } catch (e) {
        message.error(e instanceof Error ? e.message : '条目加载失败');
      } finally {
        setItemsLoading(false);
      }
    },
    [message],
  );

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (manageList) {
      loadItems(manageList.questionListId);
      adminApi
        .listQuestions({ page: 1, pageSize: 50 })
        .then((d) => setQuestions(d.list))
        .catch(() => undefined);
    }
  }, [manageList, loadItems]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ type: 1 });
    setOpen(true);
  };

  const openEdit = (record: QuestionListVO) => {
    setEditing(record);
    form.setFieldsValue({
      name: record.name,
      type: record.type,
      description: record.description ?? undefined,
    });
    setOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editing) {
        await adminApi.updateQuestionList(editing.questionListId, values);
        message.success('题单已更新');
      } else {
        await adminApi.createQuestionList(values);
        message.success('题单已创建');
      }
      setOpen(false);
      await load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (record: QuestionListVO) => {
    try {
      await adminApi.deleteQuestionList(record.questionListId);
      message.success('题单已删除');
      await load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败');
    }
  };

  const handleAddItem = async () => {
    if (!manageList || pickedQuestionId == null) return;
    try {
      await adminApi.addQuestionListItem({
        questionListId: manageList.questionListId,
        questionId: pickedQuestionId,
      });
      message.success('已添加到题单');
      setPickedQuestionId(undefined);
      await loadItems(manageList.questionListId);
      await load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '添加失败');
    }
  };

  const handleRemoveItem = async (questionId: number) => {
    if (!manageList) return;
    try {
      await adminApi.removeQuestionListItem(manageList.questionListId, questionId);
      await loadItems(manageList.questionListId);
      await load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '移除失败');
    }
  };

  const move = async (index: number, delta: number) => {
    if (!manageList) return;
    const next = [...items];
    const target = index + delta;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    try {
      await adminApi.sortQuestionListItems(
        manageList.questionListId,
        next.map((i) => i.questionId),
      );
      setItems(next);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '排序失败');
    }
  };

  return (
    <div>
      <Typography.Title level={4}>题单管理</Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建题单
          </Button>
        </Space>

        <Table
          rowKey="questionListId"
          loading={loading}
          dataSource={lists}
          pagination={false}
          columns={[
            { title: 'ID', dataIndex: 'questionListId', width: 70 },
            { title: '名称', dataIndex: 'name' },
            {
              title: '类型',
              dataIndex: 'type',
              width: 110,
              render: (t: number) => (
                <Tag color={t === 1 ? 'gold' : 'blue'}>{t === 1 ? '官方推荐' : '专题'}</Tag>
              ),
            },
            { title: '题目数', dataIndex: 'questionCount', width: 90 },
            { title: '描述', dataIndex: 'description', render: (d: string | null) => d ?? '-' },
            {
              title: '操作',
              width: 240,
              render: (_: unknown, record: QuestionListVO) => (
                <Space>
                  <Button type="link" size="small" onClick={() => setManageList(record)}>
                    管理题目
                  </Button>
                  <Button type="link" size="small" onClick={() => openEdit(record)}>
                    编辑
                  </Button>
                  <Popconfirm
                    title="删除题单会连带清空条目, 确定?"
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
        open={open}
        title={editing ? '编辑题单' : '新建题单'}
        onCancel={() => setOpen(false)}
        onOk={handleSave}
        confirmLoading={saving}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择类型' }]}>
            <Select
              options={[
                { value: 1, label: '官方推荐' },
                { value: 2, label: '专题' },
              ]}
            />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea maxLength={255} autoSize={{ minRows: 2, maxRows: 4 }} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        open={manageList != null}
        onClose={() => setManageList(null)}
        title={`管理题目 · ${manageList?.name ?? ''}`}
        width={640}
        destroyOnHidden
      >
        <Space.Compact style={{ width: '100%', marginBottom: 16 }}>
          <Select
            showSearch
            aria-label="选择题号"
            placeholder="选择要添加的题目"
            style={{ flex: 1 }}
            value={pickedQuestionId}
            onChange={setPickedQuestionId}
            optionFilterProp="label"
            options={questions.map((q) => ({
              value: q.questionId,
              label: `#${q.questionId} ${q.title}`,
            }))}
          />
          <Button type="primary" onClick={handleAddItem} disabled={pickedQuestionId == null}>
            添加
          </Button>
        </Space.Compact>

        <List
          loading={itemsLoading}
          dataSource={items}
          locale={{ emptyText: '题单还没有题目' }}
          renderItem={(item, index) => (
            <List.Item
              actions={[
                <Button
                  key="up"
                  type="text"
                  size="small"
                  icon={<ArrowUpOutlined />}
                  disabled={index === 0}
                  onClick={() => move(index, -1)}
                />,
                <Button
                  key="down"
                  type="text"
                  size="small"
                  icon={<ArrowDownOutlined />}
                  disabled={index === items.length - 1}
                  onClick={() => move(index, 1)}
                />,
                <Popconfirm
                  key="rm"
                  title="从题单移除?"
                  onConfirm={() => handleRemoveItem(item.questionId)}
                  okText="移除"
                >
                  <Button type="link" size="small" danger>
                    移除
                  </Button>
                </Popconfirm>,
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <Typography.Text type="secondary">{index + 1}.</Typography.Text>
                    <DifficultyTag difficulty={item.difficulty} />
                    {item.title}
                  </Space>
                }
                description={<Tag>{item.categoryName ?? '未分类'}</Tag>}
              />
            </List.Item>
          )}
        />
      </Drawer>
    </div>
  );
}

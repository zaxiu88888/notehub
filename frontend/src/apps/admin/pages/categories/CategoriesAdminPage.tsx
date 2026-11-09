import { useCallback, useEffect, useState } from 'react';
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { adminApi } from '@/domain/admin/api';
import { getCategoryTree } from '@/domain/question/api';
import type { CategoryVO } from '@/domain/question/types';

interface FormValues {
  name: string;
  parentCategoryId?: number;
}

/** 分类管理: 两级树表 + 新建/编辑/删除 */
export default function CategoriesAdminPage() {
  const { message } = App.useApp();
  const [tree, setTree] = useState<CategoryVO[]>([]);
  const [expandedKeys, setExpandedKeys] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<CategoryVO | null>(null);
  const [presetParent, setPresetParent] = useState<number | undefined>();
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getCategoryTree();
      setTree(data);
      setExpandedKeys(data.map((c) => c.categoryId)); // 默认展开全部顶级
    } catch (e) {
      message.error(e instanceof Error ? e.message : '分类加载失败');
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = (parentCategoryId?: number) => {
    setEditing(null);
    setPresetParent(parentCategoryId);
    form.resetFields();
    form.setFieldsValue({ parentCategoryId });
    setOpen(true);
  };

  const openEdit = (record: CategoryVO) => {
    setEditing(record);
    setPresetParent(undefined);
    form.setFieldsValue({ name: record.name, parentCategoryId: record.parentCategoryId ?? undefined });
    setOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editing) {
        await adminApi.updateCategory(editing.categoryId, values);
        message.success('分类已更新');
      } else {
        await adminApi.createCategory(values);
        message.success('分类已创建');
      }
      setOpen(false);
      await load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (record: CategoryVO) => {
    try {
      await adminApi.deleteCategory(record.categoryId);
      message.success('分类已删除');
      await load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败');
    }
  };

  const topOptions = tree.map((c) => ({ value: c.categoryId, label: c.name }));

  return (
    <div>
      <Typography.Title level={4}>分类管理</Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate()}>
            新建顶级分类
          </Button>
          <Typography.Text type="secondary">
            仅支持两级; 有子分类或有题目的分类不允许删除
          </Typography.Text>
        </Space>

        <Table
          rowKey="categoryId"
          loading={loading}
          dataSource={tree}
          pagination={false}
          expandable={{
            expandedRowKeys: expandedKeys,
            onExpandedRowsChange: (keys) => setExpandedKeys(keys as number[]),
          }}
          columns={[
            { title: 'ID', dataIndex: 'categoryId', width: 80 },
            { title: '名称', dataIndex: 'name' },
            {
              title: '类型',
              width: 120,
              render: (_: unknown, r: CategoryVO) =>
                r.parentCategoryId == null ? '顶级分类' : '子分类',
            },
            {
              title: '操作',
              width: 260,
              render: (_: unknown, record: CategoryVO) =>
                record.parentCategoryId == null ? (
                  <Space>
                    <Button type="link" size="small" onClick={() => openCreate(record.categoryId)}>
                      加子分类
                    </Button>
                    <Button type="link" size="small" onClick={() => openEdit(record)}>
                      编辑
                    </Button>
                    <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record)} okText="删除">
                      <Button type="link" size="small" danger>
                        删除
                      </Button>
                    </Popconfirm>
                  </Space>
                ) : (
                  <Space>
                    <Button type="link" size="small" onClick={() => openEdit(record)}>
                      编辑
                    </Button>
                    <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record)} okText="删除">
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
        title={editing ? '编辑分类' : '新建分类'}
        onCancel={() => setOpen(false)}
        onOk={handleSave}
        confirmLoading={saving}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="分类名" rules={[{ required: true, message: '请输入分类名' }]}>
            <Input maxLength={32} />
          </Form.Item>
          <Form.Item name="parentCategoryId" label="父分类(留空=顶级)">
            <Select
              allowClear
              aria-label="父分类"
              placeholder="顶级分类"
              options={topOptions.filter((o) => o.value !== editing?.categoryId)}
              disabled={presetParent !== undefined && !editing}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

import { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  message,
  Popconfirm,
  Card,
  Tooltip,
  Row,
  Col,
  Statistic,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  TeamOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { agentRelationApi } from '@/api';
import type { AgentRelation, AgentRelationSaveRequest } from '@/types/agent';
import { useUserStore } from '@/store';
import { formatDateTime } from '@/utils';

const SubordinateMerchantPage = () => {
  const { user } = useUserStore();
  const isAdmin = user?.role === 'admin' || user?.role === 'operator';

  const [form] = Form.useForm();
  const [queryForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AgentRelation[]>([]);
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });

  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<AgentRelation | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const currentMerchantNo = isAdmin ? '' : user?.username || '';

  const fetchData = async (page = pagination.current, size = pagination.size) => {
    try {
      setLoading(true);
      const values = queryForm.getFieldsValue();
      const params: any = {
        pageNum: page,
        pageSize: size,
        ...values,
      };
      if (!isAdmin) {
        params.parentMerchantNo = currentMerchantNo;
      }
      const result = await agentRelationApi.list(params);
      setData(result.records);
      setPagination({ current: result.current, size: result.size, total: result.total });
    } catch (e: any) {
      message.error(e?.message || '加载下级商户列表失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleAdd = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      status: 1,
      commissionRate: 10,
      agentLevel: 2,
      parentMerchantNo: currentMerchantNo,
    });
    setModalVisible(true);
  };

  const handleEdit = (record: AgentRelation) => {
    setEditing(record);
    form.setFieldsValue({
      ...record,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await agentRelationApi.delete(id);
      message.success('删除成功');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '删除失败');
    }
  };

  const handleToggleStatus = async (record: AgentRelation) => {
    try {
      const newStatus = record.status === 1 ? 0 : 1;
      await agentRelationApi.updateStatus(record.id, newStatus);
      message.success('状态更新成功');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '状态更新失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload: AgentRelationSaveRequest = {
        ...values,
      };
      await agentRelationApi.save(payload);
      message.success(editing ? '更新成功' : '添加成功');
      setModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error?.message || (editing ? '更新失败' : '添加失败'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleSearch = () => {
    fetchData(1, pagination.size);
  };

  const handleReset = () => {
    queryForm.resetFields();
    fetchData(1, pagination.size);
  };

  const columns: ColumnsType<AgentRelation> = [
    {
      title: '商户号',
      dataIndex: 'merchantNo',
      key: 'merchantNo',
      width: 140,
    },
    {
      title: '商户名称',
      dataIndex: 'merchantName',
      key: 'merchantName',
      width: 160,
    },
    {
      title: '上级商户',
      key: 'parent',
      width: 160,
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          <span>{r.parentMerchantName || '-'}</span>
          <span style={{ color: '#999', fontSize: 12 }}>{r.parentMerchantNo || ''}</span>
        </Space>
      ),
    },
    {
      title: '代理层级',
      dataIndex: 'agentLevel',
      key: 'agentLevel',
      width: 100,
      render: (level: number) => <Tag color="blue">L{level}</Tag>,
    },
    {
      title: '分润比例',
      dataIndex: 'commissionRate',
      key: 'commissionRate',
      width: 100,
      render: (rate: number) => <Tag color="gold">{rate || 0}%</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number, record) => (
        <Tag color={status === 1 ? 'green' : 'default'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (val: string) => formatDateTime(val),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            >
              编辑
            </Button>
          </Tooltip>
          <Tooltip title={record.status === 1 ? '禁用' : '启用'}>
            <Button
              type="link"
              size="small"
              icon={record.status === 1 ? <CloseCircleOutlined /> : <CheckCircleOutlined />}
              onClick={() => handleToggleStatus(record)}
            >
              {record.status === 1 ? '禁用' : '启用'}
            </Button>
          </Tooltip>
          <Popconfirm
            title="确定删除该代理关系？"
            description="删除后不可恢复"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const activeCount = data.filter((d) => d.status === 1).length;
  const disabledCount = data.filter((d) => d.status === 0).length;

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card>
            <Statistic
              title="下级商户总数"
              value={pagination.total}
              prefix={<TeamOutlined style={{ color: '#1677ff' }} />}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="已启用"
              value={activeCount}
              prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="已禁用"
              value={disabledCount}
              prefix={<CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
      </Row>

      <Card
        title="下级商户管理"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              添加下级
            </Button>
          </Space>
        }
      >
        <Form
          form={queryForm}
          layout="inline"
          style={{ marginBottom: 16 }}
          onFinish={handleSearch}
        >
          <Form.Item name="merchantNo" label="商户号">
            <Input placeholder="请输入商户号" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="merchantName" label="商户名称">
            <Input placeholder="请输入商户名称" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="agentLevel" label="代理层级">
            <Select
              placeholder="全部层级"
              allowClear
              style={{ width: 120 }}
              options={[
                { label: '一级', value: 1 },
                { label: '二级', value: 2 },
                { label: '三级', value: 3 },
                { label: '四级', value: 4 },
                { label: '五级', value: 5 },
              ]}
            />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              placeholder="全部状态"
              allowClear
              style={{ width: 120 }}
              options={[
                { label: '启用', value: 1 },
                { label: '禁用', value: 0 },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} htmlType="submit">
                查询
              </Button>
              <Button onClick={handleReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>

        <Table<AgentRelation>
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.size,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchData(page, pageSize),
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      <Modal
        title={editing ? '编辑下级商户' : '添加下级商户'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical" name="subordinate_form">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="merchantNo"
                label="商户号"
                rules={[{ required: true, message: '请输入商户号' }]}
              >
                <Input placeholder="请输入商户号" disabled={!!editing} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="merchantName"
                label="商户名称"
                rules={[{ required: true, message: '请输入商户名称' }]}
              >
                <Input placeholder="请输入商户名称" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="parentMerchantNo" label="上级商户号">
                <Input disabled={!isAdmin} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="agentLevel" label="代理层级">
                <InputNumber min={1} max={10} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="commissionRate"
                label="分润比例(%)"
                rules={[{ required: true, message: '请输入分润比例' }]}
              >
                <InputNumber min={0} max={100} step={0.1} precision={2} style={{ width: '100%' }} placeholder="如：10" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态" initialValue={1}>
                <Select
                  options={[
                    { label: '启用', value: 1 },
                    { label: '禁用', value: 0 },
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SubordinateMerchantPage;

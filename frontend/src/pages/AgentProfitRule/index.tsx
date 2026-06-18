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
  Switch,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  FundOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { agentProfitRuleApi } from '@/api';
import type {
  AgentProfitRule,
  AgentProfitRuleSaveRequest,
  AgentSettleType,
} from '@/types/agent';
import { useUserStore } from '@/store';
import { formatDateTime, formatPercent, formatAmount } from '@/utils';

const AgentProfitRulePage = () => {
  const { user } = useUserStore();
  const isAdmin = user?.role === 'admin' || user?.role === 'operator';

  const [form] = Form.useForm();
  const [queryForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AgentProfitRule[]>([]);
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });

  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<AgentProfitRule | null>(null);
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
        params.merchantNo = currentMerchantNo;
      }
      const result = await agentProfitRuleApi.list(params);
      setData(result.records);
      setPagination({ current: result.current, size: result.size, total: result.total });
    } catch (e: any) {
      message.error(e?.message || '加载分润规则列表失败');
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
      agentLevel: 1,
      commissionRate: 10,
      minCommission: 0,
      settleType: 0 as AgentSettleType,
      merchantNo: currentMerchantNo,
    });
    setModalVisible(true);
  };

  const handleEdit = (record: AgentProfitRule) => {
    setEditing(record);
    form.setFieldsValue({
      ...record,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await agentProfitRuleApi.delete(id);
      message.success('删除成功');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '删除失败');
    }
  };

  const handleToggleStatus = async (record: AgentProfitRule) => {
    try {
      await agentProfitRuleApi.toggle(record.id);
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
      const payload: AgentProfitRuleSaveRequest = {
        ...values,
      };
      await agentProfitRuleApi.save(payload);
      message.success(editing ? '更新成功' : '创建成功');
      setModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error?.message || (editing ? '更新失败' : '创建失败'));
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

  const columns: ColumnsType<AgentProfitRule> = [
    {
      title: '规则编号',
      dataIndex: 'ruleNo',
      key: 'ruleNo',
      width: 160,
    },
    {
      title: '规则名称',
      dataIndex: 'ruleName',
      key: 'ruleName',
      width: 160,
    },
    {
      title: '代理商',
      key: 'merchant',
      width: 180,
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          <span>{r.merchantName || '-'}</span>
          <span style={{ color: '#999', fontSize: 12 }}>{r.merchantNo || ''}</span>
        </Space>
      ),
    },
    {
      title: '适用层级',
      dataIndex: 'agentLevel',
      key: 'agentLevel',
      width: 100,
      render: (level: number) => <Tag color="blue">L{level}</Tag>,
    },
    {
      title: '分润比例',
      dataIndex: 'commissionRate',
      key: 'commissionRate',
      width: 120,
      render: (rate: number) => (
        <Tag color="gold" style={{ fontSize: 14, padding: '2px 10px' }}>
          {formatPercent(rate / 100)}
        </Tag>
      ),
    },
    {
      title: '结算方式',
      dataIndex: 'settleTypeDesc',
      key: 'settleTypeDesc',
      width: 130,
      render: (desc: string, r) => {
        const color = r.settleType === 0 ? 'cyan' : 'purple';
        return <Tag color={color}>{desc}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number, record) => (
        <Switch
          checked={status === 1}
          checkedChildren="启用"
          unCheckedChildren="禁用"
          onClick={() => handleToggleStatus(record)}
        />
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
      width: 180,
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
            title="确定删除该规则？"
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
              title="规则总数"
              value={pagination.total}
              prefix={<FundOutlined style={{ color: '#1677ff' }} />}
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
        title="分润规则管理"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增规则
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
          <Form.Item name="ruleNo" label="规则编号">
            <Input placeholder="请输入规则编号" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="ruleName" label="规则名称">
            <Input placeholder="请输入规则名称" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="merchantNo" label="代理商">
            <Input placeholder="请输入代理商号" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="agentLevel" label="适用层级">
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

        <Table<AgentProfitRule>
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
          scroll={{ x: 1300 }}
        />
      </Card>

      <Modal
        title={editing ? '编辑分润规则' : '新增分润规则'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical" name="agent_profit_rule_form">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="ruleName"
                label="规则名称"
                rules={[{ required: true, message: '请输入规则名称' }]}
              >
                <Input placeholder="如：一级代理商分润规则" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="merchantNo"
                label="代理商号"
                rules={[{ required: true, message: '请输入代理商号' }]}
              >
                <Input placeholder="请输入代理商号" disabled={!!editing || !isAdmin} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="merchantName"
                label="代理商名称"
              >
                <Input placeholder="请输入代理商名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="agentLevel"
                label="适用层级"
                rules={[{ required: true, message: '请选择适用层级' }]}
              >
                <Select
                  placeholder="请选择层级"
                  options={[
                    { label: '一级', value: 1 },
                    { label: '二级', value: 2 },
                    { label: '三级', value: 3 },
                    { label: '四级', value: 4 },
                    { label: '五级', value: 5 },
                  ]}
                />
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
                <InputNumber min={0} max={100} step={0.01} precision={4} style={{ width: '100%' }} placeholder="如：10" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="minCommission"
                label="最低分润金额（元）"
              >
                <InputNumber min={0} step={0.01} precision={2} style={{ width: '100%' }} placeholder="默认0" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="settleType"
                label="结算方式"
                initialValue={0 as AgentSettleType}
              >
                <Select
                  options={[
                    { label: '单独结算', value: 0 as AgentSettleType },
                    { label: '叠加分账', value: 1 as AgentSettleType },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="status"
                label="状态"
                initialValue={1}
              >
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

export default AgentProfitRulePage;

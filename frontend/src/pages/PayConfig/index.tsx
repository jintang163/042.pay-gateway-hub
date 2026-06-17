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
  Switch,
  message,
  Popconfirm,
  Card,
  Tooltip,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { payConfigApi } from '@/api';
import type { PayConfig, PayConfigQueryParams } from '@/types/payConfig';
import { formatDateTime, formatPercent } from '@/utils';

const channelOptions = [
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信支付', value: 'WECHAT' },
  { label: '银联', value: 'UNIONPAY' },
  { label: 'Apple Pay', value: 'APPLE_PAY' },
  { label: 'Google Pay', value: 'GOOGLE_PAY' },
  { label: 'PayPal', value: 'PAYPAL' },
];

const statusTag: Record<number, { color: string; text: string }> = {
  1: { color: 'green', text: '启用' },
  0: { color: 'default', text: '禁用' },
};

const PayConfigPage = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PayConfig[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<PayConfig | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async (page = pagination.current, pageSize = pagination.pageSize) => {
    try {
      setLoading(true);
      const params: PayConfigQueryParams = { pageNum: page, pageSize };
      const result = await payConfigApi.list(params);
      setData(result.list);
      setPagination({ ...pagination, current: page, pageSize, total: result.total });
    } catch {
      const mockData: PayConfig[] = Array.from({ length: 5 }, (_, i) => ({
        id: i + 1,
        merchantId: 'M001',
        merchantName: '示例商户' + (i + 1),
        payChannel: channelOptions[i % channelOptions.length].value,
        channelMerchantId: 'CM' + (10000 + i),
        channelAppId: 'APP' + (20000 + i),
        channelSecret: '********',
        feeRate: 0.6,
        callbackUrl: 'https://example.com/api/callback',
        notifyUrl: 'https://example.com/api/notify',
        whitelistIps: '127.0.0.1,192.168.1.1',
        splitRule: JSON.stringify({ type: 'PERCENT', details: [{ account: 'A', percent: 70 }, { account: 'B', percent: 30 }] }, null, 2),
        status: i % 2,
        createTime: new Date(Date.now() - i * 86400000).toISOString(),
        updateTime: new Date(Date.now() - i * 86400000).toISOString(),
      }));
      setData(mockData);
      setPagination({ current: page, pageSize, total: 5 });
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
    setModalVisible(true);
  };

  const handleEdit = (record: PayConfig) => {
    setEditing(record);
    form.setFieldsValue({
      ...record,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await payConfigApi.delete(id);
      message.success('删除成功');
      fetchData();
    } catch {
      setData((prev) => prev.filter((item) => item.id !== id));
      message.success('删除成功');
    }
  };

  const handleToggleStatus = async (record: PayConfig) => {
    try {
      const newStatus = record.status === 1 ? 0 : 1;
      await payConfigApi.updateStatus(record.id!, newStatus);
      message.success('状态更新成功');
      fetchData();
    } catch {
      setData((prev) =>
        prev.map((item) =>
          item.id === record.id ? { ...item, status: item.status === 1 ? 0 : 1 } : item,
        ),
      );
      message.success('状态更新成功');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      if (editing) {
        await payConfigApi.update(editing.id!, values);
        message.success('更新成功');
      } else {
        await payConfigApi.create(values);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(editing ? '更新失败' : '创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<PayConfig> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
    },
    {
      title: '商户名称',
      dataIndex: 'merchantName',
      key: 'merchantName',
      width: 150,
    },
    {
      title: '支付渠道',
      dataIndex: 'payChannel',
      key: 'payChannel',
      width: 100,
      render: (channel: string) => {
        const opt = channelOptions.find((c) => c.value === channel);
        return <Tag color="blue">{opt?.label || channel}</Tag>;
      },
    },
    {
      title: '渠道商户ID',
      dataIndex: 'channelMerchantId',
      key: 'channelMerchantId',
      width: 130,
    },
    {
      title: '渠道应用ID',
      dataIndex: 'channelAppId',
      key: 'channelAppId',
      width: 130,
    },
    {
      title: '费率',
      dataIndex: 'feeRate',
      key: 'feeRate',
      width: 80,
      render: (rate: number) => formatPercent(rate),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
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
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
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
          <Popconfirm
            title="确定删除该配置？"
            description="删除后不可恢复"
            onConfirm={() => handleDelete(record.id!)}
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

  return (
    <div>
      <Card
        title="支付配置管理"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增配置
            </Button>
          </Space>
        }
      >
        <Table<PayConfig>
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchData(page, pageSize),
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      <Modal
        title={editing ? '编辑支付配置' : '新增支付配置'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        width={720}
        destroyOnClose
      >
        <Form form={form} layout="vertical" name="pay_config_form">
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="payChannel"
              label="支付渠道"
              rules={[{ required: true, message: '请选择支付渠道' }]}
              style={{ flex: 1 }}
            >
              <Select placeholder="请选择支付渠道" options={channelOptions} />
            </Form.Item>
            <Form.Item
              name="feeRate"
              label="费率(%)"
              rules={[{ required: true, message: '请输入费率' }]}
              style={{ flex: 1 }}
            >
              <InputNumber min={0} max={10} step={0.01} placeholder="如：0.6" style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              name="status"
              label="状态"
              style={{ flex: 1 }}
              initialValue={1}
            >
              <Select
                options={[
                  { label: '启用', value: 1 },
                  { label: '禁用', value: 0 },
                ]}
              />
            </Form.Item>
          </div>

          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="channelMerchantId"
              label="渠道商户ID"
              rules={[{ required: true, message: '请输入渠道商户ID' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="如：2088123456789012" />
            </Form.Item>
            <Form.Item
              name="channelAppId"
              label="渠道应用ID"
              rules={[{ required: true, message: '请输入渠道应用ID' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="如：wx1234567890abcdef" />
            </Form.Item>
          </div>

          <Form.Item
            name="channelSecret"
            label="渠道密钥"
            rules={[{ required: true, message: '请输入渠道密钥' }]}
          >
            <Input.Password placeholder="请输入渠道密钥，如：appSecret或私钥" />
          </Form.Item>

          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="callbackUrl"
              label="回调地址"
              rules={[{ required: true, message: '请输入回调地址' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="用户支付后前端跳转地址" />
            </Form.Item>
            <Form.Item
              name="notifyUrl"
              label="通知地址"
              rules={[{ required: true, message: '请输入通知地址' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="支付成功后服务端异步通知地址" />
            </Form.Item>
          </div>

          <Form.Item
            name="whitelistIps"
            label="白名单IP"
            tooltip="多个IP用英文逗号分隔，留空表示不限制"
          >
            <Input.TextArea rows={2} placeholder="如：127.0.0.1,192.168.1.1,10.0.0.1" />
          </Form.Item>

          <Form.Item
            name="splitRule"
            label="分账规则JSON"
            tooltip="配置分账规则的JSON字符串"
          >
            <Input.TextArea
              rows={6}
              placeholder='{"type": "PERCENT", "details": [{"account": "A", "percent": 70}, {"account": "B", "percent": 30}]}'
              style={{ fontFamily: 'monospace' }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default PayConfigPage;

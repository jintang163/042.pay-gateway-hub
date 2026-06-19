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
  Row,
  Col,
  DatePicker,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  LinkOutlined,
  CopyOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { payLinkApi } from '@/api/marketing';
import type { PayLink, PayLinkSaveRequest } from '@/types/marketing';
import { useUserStore } from '@/store';
import { formatDateTime, formatAmount } from '@/utils';

const { RangePicker } = DatePicker;

const channelOptions = [
  { label: '全部渠道', value: '' },
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信支付', value: 'WECHAT' },
  { label: '银联', value: 'UNIONPAY' },
];

const statusColorMap: Record<number, string> = {
  1: 'green',
  2: 'default',
  3: 'red',
  4: 'orange',
};

const PayLinkManagePage = () => {
  const { user } = useUserStore();
  const [form] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PayLink[]>([]);
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });

  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<PayLink | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [queryStatus, setQueryStatus] = useState<number | undefined>();
  const [queryTitle, setQueryTitle] = useState<string>('');

  const fetchData = async (page = pagination.current, size = pagination.size) => {
    try {
      setLoading(true);
      const result = await payLinkApi.list({
        current: page,
        size,
        status: queryStatus,
        title: queryTitle || undefined,
      });
      setData(result.records);
      setPagination({ current: result.current, size: result.size, total: result.total });
    } catch (e: any) {
      message.error(e?.message || '加载支付链接列表失败');
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
      amountEditable: false,
      singleUse: false,
      merchantNo: user?.username || '',
    });
    setModalVisible(true);
  };

  const handleEdit = (record: PayLink) => {
    setEditing(record);
    form.setFieldsValue({
      ...record,
      expireTime: record.expireTime || undefined,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await payLinkApi.remove(id);
      message.success('删除成功');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '删除失败');
    }
  };

  const handleToggleStatus = async (record: PayLink) => {
    try {
      await payLinkApi.toggle(record.id);
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
      const payload: PayLinkSaveRequest = {
        ...values,
        id: editing?.id,
        fixedAmount: values.fixedAmount || undefined,
        minAmount: values.minAmount || undefined,
        maxAmount: values.maxAmount || undefined,
        maxUseCount: values.singleUse ? 1 : (values.maxUseCount || undefined),
      };
      await payLinkApi.save(payload);
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

  const handleCopyLink = (linkCode: string) => {
    const url = `${window.location.origin}/h5/pay-link/${linkCode}`;
    navigator.clipboard.writeText(url).then(() => {
      message.success('链接已复制到剪贴板');
    }).catch(() => {
      message.info(`链接编码: ${linkCode}`);
    });
  };

  const columns: ColumnsType<PayLink> = [
    {
      title: '链接编码',
      dataIndex: 'linkCode',
      key: 'linkCode',
      width: 200,
      render: (code: string) => (
        <Space>
          <LinkOutlined style={{ color: '#1677ff' }} />
          <span style={{ fontFamily: 'monospace' }}>{code}</span>
        </Space>
      ),
    },
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      width: 160,
      ellipsis: true,
    },
    {
      title: '金额(元)',
      key: 'amount',
      width: 180,
      render: (_, r) => {
        if (r.fixedAmount) return <Tag color="blue">{formatAmount(r.fixedAmount)}</Tag>;
        if (r.amountEditable) {
          const min = r.minAmount ? formatAmount(r.minAmount) : '0';
          const max = r.maxAmount ? formatAmount(r.maxAmount) : '∞';
          return <span>{min} ~ {max}</span>;
        }
        return <Tag>自定义</Tag>;
      },
    },
    {
      title: '使用限制',
      key: 'usage',
      width: 120,
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          <span>{r.singleUse ? '单次使用' : `最多${r.maxUseCount || '∞'}次`}</span>
          <span style={{ color: '#999', fontSize: 12 }}>已用 {r.usedCount} 次</span>
        </Space>
      ),
    },
    {
      title: '有效期',
      dataIndex: 'expireTime',
      key: 'expireTime',
      width: 170,
      render: (val: string) => val ? formatDateTime(val) : <Tag>永久</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => (
        <Tag color={statusColorMap[status] || 'default'}>
          {status === 1 ? '生效中' : status === 2 ? '已过期' : status === 3 ? '已禁用' : status === 4 ? '已用尽' : '未知'}
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
      width: 220,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Tooltip title="复制链接">
            <Button
              type="link"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => handleCopyLink(record.linkCode)}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            />
          </Tooltip>
          <Tooltip title={record.status === 3 ? '启用' : '禁用'}>
            <Switch
              size="small"
              checked={record.status === 1}
              onChange={() => handleToggleStatus(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确定删除该支付链接？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card
        title="支付链接管理"
        extra={
          <Space>
            <Input
              placeholder="搜索标题"
              value={queryTitle}
              onChange={(e) => setQueryTitle(e.target.value)}
              onPressEnter={() => fetchData(1)}
              style={{ width: 180 }}
              allowClear
            />
            <Select
              placeholder="状态筛选"
              value={queryStatus}
              onChange={(v) => { setQueryStatus(v); fetchData(1); }}
              style={{ width: 120 }}
              allowClear
              options={[
                { label: '生效中', value: 1 },
                { label: '已过期', value: 2 },
                { label: '已禁用', value: 3 },
                { label: '已用尽', value: 4 },
              ]}
            />
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>刷新</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>创建链接</Button>
          </Space>
        }
      >
        <Table<PayLink>
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
        title={editing ? '编辑支付链接' : '创建支付链接'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        width={720}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="title" label="链接标题" rules={[{ required: true, message: '请输入链接标题' }]}>
                <Input placeholder="如：会员年费支付" maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="payChannel" label="支付渠道">
                <Select placeholder="留空=全部渠道" allowClear options={channelOptions} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="fixedAmount" label="固定金额(元)">
                <InputNumber min={0.01} step={1} precision={2} style={{ width: '100%' }} placeholder="留空=自定义" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="amountEditable" label="允许自定义金额" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={4}>
              <Form.Item name="minAmount" label="最低金额">
                <InputNumber min={0} step={0.01} precision={2} style={{ width: '100%' }} placeholder="0" />
              </Form.Item>
            </Col>
            <Col span={4}>
              <Form.Item name="maxAmount" label="最高金额">
                <InputNumber min={0} step={0.01} precision={2} style={{ width: '100%' }} placeholder="∞" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="singleUse" label="单次使用" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="maxUseCount" label="最大使用次数">
                <InputNumber min={1} style={{ width: '100%' }} placeholder="不限" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="productSubject" label="商品描述">
                <Input placeholder="可选" maxLength={200} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="productDetail" label="商品详情">
                <Input placeholder="可选" maxLength={500} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="notifyUrl" label="异步通知地址">
                <Input placeholder="支付成功后回调URL" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="redirectUrl" label="同步跳转地址">
                <Input placeholder="支付完成后跳转URL" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="expireTime" label="过期时间">
                <Input placeholder="如：2026-12-31 23:59:59" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="remark" label="备注">
                <Input placeholder="可选" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
};

export default PayLinkManagePage;

import { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Card,
  Form,
  Input,
  Select,
  DatePicker,
  Drawer,
  Descriptions,
  message,
  Popconfirm,
  Modal,
  InputNumber,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  RedoOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { Dayjs } from 'dayjs';
import { orderApi } from '@/api';
import type { Order, OrderQueryParams, RefundApplyRequest } from '@/types/order';
import { formatAmount, formatDateTime } from '@/utils';

const { RangePicker } = DatePicker;

const statusTag: Record<string, { color: string; text: string }> = {
  success: { color: 'green', text: '成功' },
  pending: { color: 'orange', text: '处理中' },
  failed: { color: 'red', text: '失败' },
  closed: { color: 'default', text: '已关闭' },
  refunding: { color: 'purple', text: '退款中' },
  refunded: { color: 'geekblue', text: '已退款' },
};

const payChannelOptions = [
  { label: '全部', value: '' },
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信支付', value: 'WECHAT' },
  { label: '银联', value: 'UNIONPAY' },
  { label: 'Apple Pay', value: 'APPLE_PAY' },
];

const statusOptions = [
  { label: '全部', value: '' },
  { label: '成功', value: 'success' },
  { label: '处理中', value: 'pending' },
  { label: '失败', value: 'failed' },
  { label: '已关闭', value: 'closed' },
  { label: '退款中', value: 'refunding' },
  { label: '已退款', value: 'refunded' },
];

const OrderList = () => {
  const [queryForm] = Form.useForm();
  const [refundForm] = Form.useForm<RefundApplyRequest>();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Order[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentOrder, setCurrentOrder] = useState<Order | null>(null);
  const [refundModalVisible, setRefundModalVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const generateMockData = (count: number): Order[] =>
    Array.from({ length: count }, (_, i) => {
      const statuses = Object.keys(statusTag);
      const channels = ['ALIPAY', 'WECHAT', 'UNIONPAY', 'APPLE_PAY'];
      return {
        id: i + 1,
        orderNo: 'PG' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        merchantOrderNo: 'MO' + Date.now() + i,
        merchantId: 'M' + String(1000 + (i % 10)).padStart(4, '0'),
        merchantName: '示例商户' + ((i % 10) + 1),
        amount: Math.random() * 5000 + 10,
        payChannel: channels[i % channels.length],
        payMethod: ['支付宝', '微信支付', '银联', 'Apple Pay'][i % 4],
        status: statuses[i % statuses.length],
        clientIp: '127.0.0.1',
        subject: '商品订单-' + (i + 1),
        body: '订单描述信息',
        callbackUrl: 'https://example.com/callback',
        notifyUrl: 'https://example.com/notify',
        extraParams: '{}',
        createTime: new Date(Date.now() - i * 60000 * 30).toISOString(),
        updateTime: new Date(Date.now() - i * 60000 * 30).toISOString(),
      };
    });

  const fetchData = async (
    page = pagination.current,
    pageSize = pagination.pageSize,
    params?: Partial<OrderQueryParams>,
  ) => {
    try {
      setLoading(true);
      const queryParams: OrderQueryParams = {
        pageNum: page,
        pageSize,
        ...params,
      };
      const result = await orderApi.list(queryParams);
      setData(result.list);
      setPagination({ current: page, pageSize, total: result.total });
    } catch {
      const mockData = generateMockData(23);
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      setData(mockData.slice(start, end));
      setPagination({ current: page, pageSize, total: mockData.length });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleQuery = () => {
    const values = queryForm.getFieldsValue();
    const params: Partial<OrderQueryParams> = {
      ...values,
    };
    if (values.dateRange) {
      params.startTime = (values.dateRange as [Dayjs, Dayjs])[0].format('YYYY-MM-DD HH:mm:ss');
      params.endTime = (values.dateRange as [Dayjs, Dayjs])[1].format('YYYY-MM-DD HH:mm:ss');
    }
    fetchData(1, pagination.pageSize, params);
  };

  const handleReset = () => {
    queryForm.resetFields();
    fetchData(1, pagination.pageSize);
  };

  const handleViewDetail = (record: Order) => {
    setCurrentOrder(record);
    setDetailVisible(true);
  };

  const handleOpenRefund = (record: Order) => {
    if (record.status !== 'success') {
      message.warning('只有成功的订单才能申请退款');
      return;
    }
    setCurrentOrder(record);
    refundForm.setFieldsValue({
      orderNo: record.orderNo,
      refundAmount: record.amount,
      reason: '',
    });
    setRefundModalVisible(true);
  };

  const handleApplyRefund = async () => {
    try {
      const values = await refundForm.validateFields();
      setSubmitting(true);
      await orderApi.refund(values);
      message.success('退款申请已提交');
      setRefundModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('退款申请失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<Order> = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 200,
      ellipsis: true,
    },
    {
      title: '商户订单号',
      dataIndex: 'merchantOrderNo',
      key: 'merchantOrderNo',
      width: 200,
      ellipsis: true,
    },
    {
      title: '商户名称',
      dataIndex: 'merchantName',
      key: 'merchantName',
      width: 140,
    },
    {
      title: '金额',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      render: (val: number) => formatAmount(val),
      sorter: (a, b) => a.amount - b.amount,
    },
    {
      title: '支付渠道',
      dataIndex: 'payChannel',
      key: 'payChannel',
      width: 100,
      render: (val: string) => {
        const opt = payChannelOptions.find((c) => c.value === val);
        return <Tag color="blue">{opt?.label || val}</Tag>;
      },
    },
    {
      title: '支付状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const tag = statusTag[status] || { color: 'default', text: status };
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (val: string) => formatDateTime(val),
      sorter: (a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime(),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          {record.status === 'success' && (
            <Popconfirm
              title="确认申请退款？"
              onConfirm={() => handleOpenRefund(record)}
              okText="确认"
              cancelText="取消"
            >
              <Button type="link" size="small" danger icon={<RedoOutlined />}>
                退款
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card title="订单查询" style={{ marginBottom: 16 }}>
        <Form form={queryForm} layout="inline" onFinish={handleQuery}>
          <Form.Item name="orderNo" label="订单号">
            <Input placeholder="请输入订单号" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="merchantOrderNo" label="商户订单号">
            <Input placeholder="请输入商户订单号" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="payChannel" label="支付渠道" initialValue="">
            <Select options={payChannelOptions} style={{ width: 130 }} />
          </Form.Item>
          <Form.Item name="status" label="订单状态" initialValue="">
            <Select options={statusOptions} style={{ width: 130 }} />
          </Form.Item>
          <Form.Item name="dateRange" label="创建时间">
            <RangePicker showTime style={{ width: 340 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button onClick={handleReset} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card title="订单列表">
        <Table<Order>
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1400 }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchData(page, pageSize),
          }}
        />
      </Card>

      <Drawer
        title="订单详情"
        width={640}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
        extra={
          <Space>
            <Button onClick={() => setDetailVisible(false)}>关闭</Button>
          </Space>
        }
      >
        {currentOrder && (
          <div>
            <Descriptions title="订单信息" bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="订单号">{currentOrder.orderNo}</Descriptions.Item>
              <Descriptions.Item label="商户订单号">{currentOrder.merchantOrderNo}</Descriptions.Item>
              <Descriptions.Item label="商户ID">{currentOrder.merchantId}</Descriptions.Item>
              <Descriptions.Item label="商户名称">{currentOrder.merchantName}</Descriptions.Item>
              <Descriptions.Item label="订单金额">{formatAmount(currentOrder.amount)}</Descriptions.Item>
              <Descriptions.Item label="支付渠道">{currentOrder.payMethod}</Descriptions.Item>
              <Descriptions.Item label="订单状态">
                {(() => {
                  const tag = statusTag[currentOrder.status] || { color: 'default', text: currentOrder.status };
                  return <Tag color={tag.color}>{tag.text}</Tag>;
                })()}
              </Descriptions.Item>
              <Descriptions.Item label="商品主题">{currentOrder.subject}</Descriptions.Item>
              <Descriptions.Item label="订单描述">{currentOrder.body || '-'}</Descriptions.Item>
              <Descriptions.Item label="客户端IP">{currentOrder.clientIp || '-'}</Descriptions.Item>
              <Descriptions.Item label="回调地址">{currentOrder.callbackUrl || '-'}</Descriptions.Item>
              <Descriptions.Item label="通知地址">{currentOrder.notifyUrl || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{formatDateTime(currentOrder.createTime)}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{formatDateTime(currentOrder.updateTime)}</Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Drawer>

      <Modal
        title="申请退款"
        open={refundModalVisible}
        onCancel={() => setRefundModalVisible(false)}
        onOk={handleApplyRefund}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form form={refundForm} layout="vertical">
          <Form.Item
            name="orderNo"
            label="订单号"
            rules={[{ required: true }]}
          >
            <Input disabled />
          </Form.Item>
          <Form.Item
            name="refundAmount"
            label="退款金额"
            rules={[{ required: true, message: '请输入退款金额' }]}
          >
            <InputNumber min={0.01} step={0.01} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="reason"
            label="退款原因"
            rules={[{ required: true, message: '请输入退款原因' }]}
          >
            <Input.TextArea rows={4} placeholder="请输入退款原因" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default OrderList;

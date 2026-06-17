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
  message,
  Modal,
  InputNumber,
  Drawer,
  Descriptions,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { Dayjs } from 'dayjs';
import { refundApi } from '@/api';
import type { Refund, RefundQueryParams, RefundApplyRequest } from '@/types/refund';
import { formatAmount, formatDateTime } from '@/utils';

const { RangePicker } = DatePicker;

const statusTag: Record<string, { color: string; text: string }> = {
  pending: { color: 'orange', text: '处理中' },
  success: { color: 'green', text: '成功' },
  failed: { color: 'red', text: '失败' },
  closed: { color: 'default', text: '已关闭' },
};

const statusOptions = [
  { label: '全部', value: '' },
  { label: '处理中', value: 'pending' },
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' },
  { label: '已关闭', value: 'closed' },
];

const RefundList = () => {
  const [queryForm] = Form.useForm();
  const [applyForm] = Form.useForm<RefundApplyRequest>();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Refund[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [applyModalVisible, setApplyModalVisible] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentRefund, setCurrentRefund] = useState<Refund | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const generateMockData = (count: number): Refund[] =>
    Array.from({ length: count }, (_, i) => {
      const statuses = Object.keys(statusTag);
      return {
        id: i + 1,
        refundNo: 'RF' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        orderNo: 'PG' + dayjs().format('YYYYMMDD') + String(2000 + i).padStart(6, '0'),
        merchantId: 'M' + String(1000 + (i % 10)).padStart(4, '0'),
        merchantName: '示例商户' + ((i % 10) + 1),
        refundAmount: Math.random() * 500 + 10,
        orderAmount: Math.random() * 1000 + 500,
        reason: '用户主动申请退款',
        status: statuses[i % statuses.length],
        channelRefundNo: 'CHRF' + (10000 + i),
        createTime: new Date(Date.now() - i * 60000 * 30).toISOString(),
        successTime: i % 3 === 0 ? new Date(Date.now() - i * 60000 * 30 + 120000).toISOString() : undefined,
        createUser: 'admin',
      };
    });

  const fetchData = async (
    page = pagination.current,
    pageSize = pagination.pageSize,
    params?: Partial<RefundQueryParams>,
  ) => {
    try {
      setLoading(true);
      const queryParams: RefundQueryParams = {
        pageNum: page,
        pageSize,
        ...params,
      };
      const result = await refundApi.list(queryParams);
      setData(result.list);
      setPagination({ current: page, pageSize, total: result.total });
    } catch {
      const mockData = generateMockData(15);
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
    const params: Partial<RefundQueryParams> = { ...values };
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

  const handleViewDetail = (record: Refund) => {
    setCurrentRefund(record);
    setDetailVisible(true);
  };

  const handleApply = () => {
    applyForm.resetFields();
    setApplyModalVisible(true);
  };

  const handleApplySubmit = async () => {
    try {
      const values = await applyForm.validateFields();
      setSubmitting(true);
      await refundApi.apply(values);
      message.success('退款申请已提交');
      setApplyModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('退款申请失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<Refund> = [
    { title: '退款单号', dataIndex: 'refundNo', key: 'refundNo', width: 200 },
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 200 },
    { title: '商户名称', dataIndex: 'merchantName', key: 'merchantName', width: 140 },
    {
      title: '退款金额',
      dataIndex: 'refundAmount',
      key: 'refundAmount',
      width: 120,
      render: (val: number) => formatAmount(val),
    },
    {
      title: '订单金额',
      dataIndex: 'orderAmount',
      key: 'orderAmount',
      width: 120,
      render: (val: number) => formatAmount(val),
    },
    {
      title: '退款状态',
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
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card title="退款查询" style={{ marginBottom: 16 }}>
        <Form form={queryForm} layout="inline" onFinish={handleQuery}>
          <Form.Item name="refundNo" label="退款单号">
            <Input placeholder="退款单号" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="orderNo" label="订单号">
            <Input placeholder="订单号" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="status" label="退款状态" initialValue="">
            <Select options={statusOptions} style={{ width: 130 }} />
          </Form.Item>
          <Form.Item name="dateRange" label="创建时间">
            <RangePicker showTime style={{ width: 340 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
              <Button onClick={handleReset} icon={<ReloadOutlined />}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        title="退款列表"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleApply}>
            申请退款
          </Button>
        }
      >
        <Table<Refund>
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1300 }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchData(page, pageSize),
          }}
        />
      </Card>

      <Modal
        title="申请退款"
        open={applyModalVisible}
        onCancel={() => setApplyModalVisible(false)}
        onOk={handleApplySubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={480}
      >
        <Form form={applyForm} layout="vertical">
          <Form.Item name="orderNo" label="订单号" rules={[{ required: true, message: '请输入订单号' }]}>
            <Input placeholder="请输入要退款的订单号" />
          </Form.Item>
          <Form.Item
            name="refundAmount"
            label="退款金额"
            rules={[{ required: true, message: '请输入退款金额' }]}
          >
            <InputNumber min={0.01} step={0.01} style={{ width: '100%' }} placeholder="请输入退款金额" />
          </Form.Item>
          <Form.Item name="reason" label="退款原因" rules={[{ required: true, message: '请输入退款原因' }]}>
            <Input.TextArea rows={4} placeholder="请输入退款原因" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="退款详情"
        width={640}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
      >
        {currentRefund && (
          <Descriptions title="退款信息" bordered column={1} size="small">
            <Descriptions.Item label="退款单号">{currentRefund.refundNo}</Descriptions.Item>
            <Descriptions.Item label="订单号">{currentRefund.orderNo}</Descriptions.Item>
            <Descriptions.Item label="商户名称">{currentRefund.merchantName}</Descriptions.Item>
            <Descriptions.Item label="退款金额">{formatAmount(currentRefund.refundAmount)}</Descriptions.Item>
            <Descriptions.Item label="订单金额">{formatAmount(currentRefund.orderAmount)}</Descriptions.Item>
            <Descriptions.Item label="退款原因">{currentRefund.reason}</Descriptions.Item>
            <Descriptions.Item label="渠道退款号">{currentRefund.channelRefundNo || '-'}</Descriptions.Item>
            <Descriptions.Item label="退款状态">
              {(() => {
                const tag = statusTag[currentRefund.status] || { color: 'default', text: currentRefund.status };
                return <Tag color={tag.color}>{tag.text}</Tag>;
              })()}
            </Descriptions.Item>
            <Descriptions.Item label="创建人">{currentRefund.createUser}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatDateTime(currentRefund.createTime)}</Descriptions.Item>
            <Descriptions.Item label="成功时间">
              {currentRefund.successTime ? formatDateTime(currentRefund.successTime) : '-'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </div>
  );
};

export default RefundList;

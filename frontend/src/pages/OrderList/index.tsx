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
  Divider,
  Alert,
  Tooltip,
  Collapse,
  Typography,
  Badge,
  Row,
  Col,
  Empty,
  Spin,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  RedoOutlined,
  FileTextOutlined,
  ExclamationCircleOutlined,
  BulbOutlined,
  RobotOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  ClockCircleOutlined,
  StopOutlined,
  SafetyOutlined,
  FundProjectionScreenOutlined,
  FileInvoiceOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { Dayjs } from 'dayjs';
import { orderApi } from '@/api';
import type { Order, OrderQueryParams, RefundApplyRequest, OrderAttributionVO } from '@/types/order';
import { formatAmount, formatDateTime } from '@/utils';
import { InvoiceApplyModal } from '@/components';

const { RangePicker } = DatePicker;
const { Panel } = Collapse;
const { Paragraph, Text } = Typography;

const statusTag: Record<number, { color: string; text: string; status: 'success' | 'processing' | 'error' | 'default' | 'warning' }> = {
  1: { color: 'green', text: '支付成功', status: 'success' },
  0: { color: 'orange', text: '待支付', status: 'processing' },
  2: { color: 'red', text: '支付失败', status: 'error' },
  3: { color: 'default', text: '已关闭', status: 'default' },
  4: { color: 'purple', text: '退款中', status: 'warning' },
  5: { color: 'geekblue', text: '已退款', status: 'success' },
};

const payStatusText = (val?: number) => {
  if (val == null) return { color: 'default', text: '-', status: 'default' as const };
  return statusTag[val] || { color: 'default', text: String(val), status: 'default' as const };
};

const payChannelOptions = [
  { label: '全部', value: '' },
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信支付', value: 'WECHAT_PAY' },
  { label: '银联', value: 'UNION_PAY' },
];

const statusOptions = [
  { label: '全部', value: '' },
  { label: '支付成功', value: '1' },
  { label: '待支付', value: '0' },
  { label: '支付失败', value: '2' },
  { label: '已关闭', value: '3' },
  { label: '退款中', value: '4' },
  { label: '已退款', value: '5' },
];

const failCategoryMeta: Record<string, { color: string; label: string; icon: React.ReactNode }> = {
  BALANCE: { color: '#d46b08', label: '余额问题', icon: <ExclamationCircleOutlined /> },
  RISK: { color: '#a8071a', label: '风控拦截', icon: <SafetyOutlined /> },
  CHANNEL: { color: '#1d39c4', label: '通道异常', icon: <WarningOutlined /> },
  SIGN: { color: '#873800', label: '签名错误', icon: <StopOutlined /> },
  PARAM: { color: '#389e0d', label: '参数问题', icon: <CloseCircleOutlined /> },
  MERCHANT: { color: '#8c8c8c', label: '商户限制', icon: <StopOutlined /> },
  USER: { color: '#597ef7', label: '用户操作', icon: <CheckCircleOutlined /> },
  SYSTEM: { color: '#2f54eb', label: '系统原因', icon: <ClockCircleOutlined /> },
  OTHER: { color: '#8c8c8c', label: '未知', icon: <ExclamationCircleOutlined /> },
};

const channelText = (v?: string) => {
  const map: Record<string, string> = {
    ALIPAY: '支付宝',
    WECHAT_PAY: '微信支付',
    UNION_PAY: '银联',
    APPLE_PAY: 'Apple Pay',
  };
  if (!v) return '-';
  return map[v] || v;
};

const OrderList = () => {
  const [queryForm] = Form.useForm();
  const [refundForm] = Form.useForm<RefundApplyRequest>();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<any[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentOrder, setCurrentOrder] = useState<any>(null);
  const [refundModalVisible, setRefundModalVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [attribution, setAttribution] = useState<OrderAttributionVO | null>(null);
  const [attributionLoading, setAttributionLoading] = useState(false);
  const [invoiceApplyVisible, setInvoiceApplyVisible] = useState(false);
  const [selectedOrderForInvoice, setSelectedOrderForInvoice] = useState<any>(null);

  const fetchData = async (
    page = pagination.current,
    pageSize = pagination.pageSize,
    params?: any,
  ) => {
    try {
      setLoading(true);
      const queryParams: any = {
        current: page,
        size: pageSize,
        ...params,
      };
      const result: any = await orderApi.list(queryParams as any);
      const list: any[] = result?.records || result?.list || [];
      const total = result?.total || list.length;
      setData(list);
      setPagination({ current: page, pageSize, total });
    } catch (e) {
      message.error('订单列表加载失败，使用示例数据');
      const statuses = [1, 2, 0, 3, 1, 2, 1, 2, 1, 2];
      const channels = ['ALIPAY', 'WECHAT_PAY', 'UNION_PAY'];
      const mockList = Array.from({ length: 12 }, (_, i) => ({
        id: i + 1,
        orderNo: 'PG' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        merchantOrderNo: 'MO' + Date.now() + i,
        merchantNo: 'M0000' + ((i % 2) + 1),
        payAmount: Number((Math.random() * 5000 + 10).toFixed(2)),
        payChannel: channels[i % 3],
        payType: ['NATIVE', 'H5', 'JSAPI'][i % 3],
        payStatus: statuses[i % statuses.length],
        productSubject: '沙箱模拟商品订单-' + (i + 1),
        clientIp: '127.0.0.1',
        notifyUrl: 'https://example.com/notify',
        createdAt: dayjs().subtract(i * 30, 'minute').format('YYYY-MM-DD HH:mm:ss'),
        updatedAt: dayjs().subtract(i * 30, 'minute').format('YYYY-MM-DD HH:mm:ss'),
      }));
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      setData(mockList.slice(start, end));
      setPagination({ current: page, pageSize, total: mockList.length });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleQuery = () => {
    const values = queryForm.getFieldsValue();
    const params: any = {};
    if (values.orderNo) params.orderNo = values.orderNo;
    if (values.merchantOrderNo) params.merchantOrderNo = values.merchantOrderNo;
    if (values.payChannel) params.payChannel = values.payChannel;
    if (values.payStatus) params.payStatus = values.payStatus;
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

  const handleViewDetail = async (record: any) => {
    setCurrentOrder(record);
    setDetailVisible(true);
    setAttribution(null);
    if (record.payStatus === 2 || record.payStatus === 3) {
      try {
        setAttributionLoading(true);
        const result: any = await orderApi.attribution(record.orderNo);
        setAttribution(result || null);
      } catch (e) {
        setAttribution({
          orderNo: record.orderNo,
          failCode: 'FAIL_UNKNOWN',
          failMessage: '归因服务暂不可用',
          failCategory: 'OTHER',
          suggestion: '稍后刷新页面重试，或联系技术支持',
          ruleDescription: '未能成功从后端加载归因结果',
          evidence: ['归因接口调用异常'],
        });
      } finally {
        setAttributionLoading(false);
      }
    }
  };

  const handleOpenRefund = (record: any) => {
    if (record.payStatus !== 1) {
      message.warning('只有支付成功的订单才能申请退款');
      return;
    }
    setCurrentOrder(record);
    refundForm.setFieldsValue({
      orderNo: record.orderNo,
      refundAmount: record.payAmount || record.amount,
      reason: '',
    });
    setRefundModalVisible(true);
  };

  const handleApplyRefund = async () => {
    try {
      const values = await refundForm.validateFields();
      setSubmitting(true);
      await orderApi.refund(currentOrder?.orderNo || currentOrder?.id || '', values as any);
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

  const handleOpenInvoiceApply = (record: any) => {
    if (record.payStatus !== 1) {
      message.warning('只有支付成功的订单才能申请发票');
      return;
    }
    setSelectedOrderForInvoice({
      orderNo: record.orderNo,
      payAmount: record.payAmount ?? record.amount,
      productSubject: record.productSubject ?? record.subject,
    });
    setInvoiceApplyVisible(true);
  };

  const renderFailBadge = (record: any) => {
    if (record.payStatus !== 2 && record.payStatus !== 3) return null;
    return (
      <Tooltip title="点击详情查看智能归因分析">
        <Tag color="orange" style={{ marginRight: 0, marginTop: 4, cursor: 'pointer' }}>
          <EyeOutlined /> 查看归因
        </Tag>
      </Tooltip>
    );
  };

  const columns: ColumnsType<any> = [
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
      render: (v) => v || '-',
    },
    {
      title: '商户号',
      dataIndex: 'merchantNo',
      key: 'merchantNo',
      width: 110,
      render: (v) => v || '-',
    },
    {
      title: '金额',
      dataIndex: 'payAmount',
      key: 'payAmount',
      width: 120,
      render: (val: number, record) => formatAmount(val ?? record.amount),
      sorter: (a, b) => (a.payAmount ?? a.amount) - (b.payAmount ?? b.amount),
    },
    {
      title: '支付渠道',
      dataIndex: 'payChannel',
      key: 'payChannel',
      width: 100,
      render: (val) => <Tag color="blue">{channelText(val)}</Tag>,
    },
    {
      title: '支付方式',
      dataIndex: 'payType',
      key: 'payType',
      width: 90,
      render: (v) => v || '-',
    },
    {
      title: '支付状态',
      dataIndex: 'payStatus',
      key: 'payStatus',
      width: 100,
      render: (status) => {
        const tag = payStatusText(status);
        return (
          <Badge status={tag.status} text={<span style={{ color: tag.color }}>{tag.text}</span>} />
        );
      },
    },
    {
      title: '失败原因',
      key: 'failReason',
      width: 160,
      render: (_, record) => renderFailBadge(record),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (val, record) => formatDateTime(val ?? record.createTime),
      sorter: (a, b) =>
        new Date(a.createdAt ?? a.createTime).getTime() - new Date(b.createdAt ?? b.createTime).getTime(),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          {record.payStatus === 1 && (
            <>
              <Button type="link" size="small" icon={<FileInvoiceOutlined />} onClick={() => handleOpenInvoiceApply(record)}>
                开发票
              </Button>
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
            </>
          )}
        </Space>
      ),
    },
  ];

  const renderAttributionCard = () => {
    if (attributionLoading) {
      return (
        <Card size="small" style={{ marginTop: 16, borderRadius: 8 }}>
          <div style={{ textAlign: 'center', padding: 24 }}>
            <Spin tip="智能归因分析中..." />
          </div>
        </Card>
      );
    }
    if (!attribution || !attribution.failCode) {
      return null;
    }

    const meta = failCategoryMeta[attribution.failCategory || 'OTHER'] || failCategoryMeta.OTHER;
    const bgColor = attribution.failCategory === 'RISK'
      ? '#fff1f0'
      : attribution.failCategory === 'CHANNEL'
      ? '#e6f4ff'
      : attribution.failCategory === 'BALANCE'
      ? '#fff7e6'
      : '#fffbe6';

    return (
      <Card
        size="small"
        style={{
          marginTop: 16,
          borderRadius: 10,
          border: `1px solid ${meta.color}33`,
          background: bgColor,
        }}
        title={
          <Space>
            <RobotOutlined style={{ color: meta.color }} />
            <span style={{ color: meta.color }}>智能归因分析</span>
            <Tag color={meta.color} icon={meta.icon} style={{ marginLeft: 8 }}>
              {meta.label}
            </Tag>
          </Space>
        }
        extra={<span style={{ fontSize: 12, color: '#8c8c8c' }}>规则 #{attribution.priority ?? 99}</span>}
      >
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Alert
              type="error"
              showIcon
              icon={<ExclamationCircleOutlined style={{ color: meta.color }} />}
              message={
                <Space>
                  <Text strong style={{ color: meta.color }}>
                    失败代码
                  </Text>
                  <Text code>{attribution.failCode}</Text>
                </Space>
              }
              description={<Text strong>{attribution.failMessage}</Text>}
              style={{ background: '#fff', borderRadius: 6 }}
            />
          </Col>
          <Col xs={24} md={12}>
            <Alert
              type="info"
              showIcon
              icon={<BulbOutlined style={{ color: '#1677ff' }} />}
              message={
                <Text strong style={{ color: '#1677ff' }}>
                  解决建议
                </Text>
              }
              description={attribution.suggestion || '-'}
              style={{ background: '#fff', borderRadius: 6 }}
            />
          </Col>
        </Row>

        <Divider style={{ margin: '16px 0 12px' }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            <FundProjectionScreenOutlined /> 归因分析说明
          </Text>
        </Divider>

        <Paragraph style={{ marginBottom: 8 }} type="secondary" ellipsis={{ rows: 2, expandable: true, symbol: '展开' }}>
          <Text strong>匹配规则：</Text>
          {attribution.ruleDescription || '-'}
        </Paragraph>

        {attribution.evidence && attribution.evidence.length > 0 && (
          <Collapse size="small" ghost defaultActiveKey={['1']}>
            <Panel
              header={
                <Space>
                  <FileTextOutlined />
                  证据链 ({attribution.evidence.length})
                </Space>
              }
              key="1"
            >
              <ul style={{ paddingLeft: 18, margin: 0 }}>
                {attribution.evidence.map((ev, i) => (
                  <li key={i} style={{ lineHeight: 1.9, color: '#595959', fontSize: 13 }}>
                    <span style={{ color: meta.color, fontWeight: 600, marginRight: 6 }}>•</span>
                    {ev}
                  </li>
                ))}
              </ul>
            </Panel>
          </Collapse>
        )}

        <Row gutter={16} style={{ marginTop: 12 }}>
          {attribution.latestChannelLog && (
            <Col xs={24} md={12}>
              <Card size="small" title={<span style={{ fontSize: 13 }}><WarningOutlined /> 最近通道日志</span>} bordered>
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="通道">
                    {attribution.latestChannelLog.channelCode || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="请求类型">
                    {attribution.latestChannelLog.requestType || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="耗时">
                    <Text type={attribution.latestChannelLog.costTime && attribution.latestChannelLog.costTime > 10000 ? 'danger' : undefined}>
                      {attribution.latestChannelLog.costTime ?? 0} ms
                    </Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="错误信息">
                    <Tooltip title={attribution.latestChannelLog.errorMsg}>
                      <Text type="danger" ellipsis style={{ maxWidth: '100%', display: 'inline-block' }}>
                        {attribution.latestChannelLog.errorMsg || '-'}
                      </Text>
                    </Tooltip>
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
          )}
          {attribution.latestRiskLog && (
            <Col xs={24} md={12}>
              <Card size="small" title={<span style={{ fontSize: 13 }}><SafetyOutlined /> 最近风控日志</span>} bordered>
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="风险等级">
                    {(() => {
                      const lvl = attribution.latestRiskLog?.riskLevel;
                      if (lvl === 3) return <Tag color="red">高风险 (3)</Tag>;
                      if (lvl === 2) return <Tag color="orange">中风险 (2)</Tag>;
                      if (lvl === 1) return <Tag color="green">低风险 (1)</Tag>;
                      return <Tag>未知</Tag>;
                    })()}
                  </Descriptions.Item>
                  <Descriptions.Item label="命中规则">
                    {attribution.latestRiskLog.riskRule || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="处理结果">
                    {(() => {
                      const result = attribution.latestRiskLog?.handleResult;
                      const desc = attribution.latestRiskLog?.handleDesc;
                      if (result === 0) return <Tag color="red">拒绝交易</Tag>;
                      if (result === 1) return <Tag color="green">通过</Tag>;
                      return desc || '-';
                    })()}
                  </Descriptions.Item>
                  <Descriptions.Item label="详情">
                    <Tooltip title={attribution.latestRiskLog.riskDesc}>
                      <Text ellipsis style={{ maxWidth: '100%', display: 'inline-block' }}>
                        {attribution.latestRiskLog.riskDesc || '-'}
                      </Text>
                    </Tooltip>
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
          )}
        </Row>
      </Card>
    );
  };

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
          <Form.Item name="payStatus" label="订单状态" initialValue="">
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
        <Table<any>
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
        width={720}
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
            <Descriptions title="订单基本信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="订单号" span={2}>
                {currentOrder.orderNo}
              </Descriptions.Item>
              <Descriptions.Item label="商户订单号">
                {currentOrder.merchantOrderNo || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="商户号">
                {currentOrder.merchantNo || currentOrder.merchantId || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="订单金额">
                <Text strong style={{ color: '#cf1322' }}>
                  {formatAmount(currentOrder.payAmount ?? currentOrder.amount)}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="实际金额">
                {formatAmount(currentOrder.actualAmount ?? 0)}
              </Descriptions.Item>
              <Descriptions.Item label="支付渠道">
                {channelText(currentOrder.payChannel)}
              </Descriptions.Item>
              <Descriptions.Item label="支付方式">
                {currentOrder.payType || currentOrder.payMethod || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="订单状态" span={2}>
                {(() => {
                  const tag = payStatusText(currentOrder.payStatus);
                  return <Badge status={tag.status} text={<span style={{ color: tag.color, fontWeight: 600 }}>{tag.text}</span>} />;
                })()}
              </Descriptions.Item>
              <Descriptions.Item label="商品主题" span={2}>
                {currentOrder.productSubject || currentOrder.subject || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="商品描述" span={2}>
                {currentOrder.productDetail || currentOrder.body || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="客户端IP">
                {currentOrder.clientIp || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="通知地址">
                {currentOrder.notifyUrl || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {formatDateTime(currentOrder.createdAt ?? currentOrder.createTime)}
              </Descriptions.Item>
              <Descriptions.Item label="更新时间">
                {formatDateTime(currentOrder.updatedAt ?? currentOrder.updateTime)}
              </Descriptions.Item>
            </Descriptions>

            {renderAttributionCard()}

            {!attributionLoading && !attribution && (currentOrder.payStatus === 1 || currentOrder.payStatus === 0) && (
              <Alert
                style={{ marginTop: 16 }}
                type="success"
                showIcon
                icon={<CheckCircleOutlined />}
                message="订单状态正常"
                description="该订单未失败，无需归因分析。支付成功订单可在列表页点击「退款」按钮发起退款。"
              />
            )}

            {!attributionLoading && !attribution && currentOrder.payStatus !== 1 && currentOrder.payStatus !== 0 &&
              currentOrder.payStatus !== 2 && currentOrder.payStatus !== 3 && (
                <Empty style={{ marginTop: 24 }} description="暂无归因信息" />
              )}
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
          <Form.Item name="orderNo" label="订单号" rules={[{ required: true }]}>
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

      <InvoiceApplyModal
        visible={invoiceApplyVisible}
        onClose={() => setInvoiceApplyVisible(false)}
        onSuccess={() => {
          message.success('发票申请已提交');
        }}
        orderInfo={selectedOrderForInvoice}
      />
    </div>
  );
};

export default OrderList;

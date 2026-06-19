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
  Modal,
  Badge,
  Typography,
  Row,
  Col,
  Tooltip,
  Alert,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  DownloadOutlined,
  FileInvoiceOutlined,
  UndoOutlined,
  SyncOutlined,
  PrinterOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { Dayjs } from 'dayjs';
import { invoiceApi } from '@/api';
import { InvoiceApplyModal } from '@/components';
import BatchInvoiceApplyModal from './BatchInvoiceApplyModal';
import type { Invoice, InvoiceRedFlushRequest, InvoiceQueryParams } from '@/types/invoice';
import {
  invoiceStatusMap,
  invoiceTypeMap,
  titleTypeMap,
  channelMap,
} from '@/types/invoice';
import { formatAmount, formatDateTime } from '@/utils';

const { RangePicker } = DatePicker;
const { Text } = Typography;

const InvoiceList = () => {
  const [queryForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Invoice[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentInvoice, setCurrentInvoice] = useState<Invoice | null>(null);
  const [applyModalVisible, setApplyModalVisible] = useState(false);
  const [selectedOrderInfo, setSelectedOrderInfo] = useState<any>(null);
  const [redFlushModalVisible, setRedFlushModalVisible] = useState(false);
  const [redFlushForm] = Form.useForm<InvoiceRedFlushRequest>();
  const [submitting, setSubmitting] = useState(false);
  const [refreshingStatus, setRefreshingStatus] = useState<string | null>(null);
  const [batchApplyModalVisible, setBatchApplyModalVisible] = useState(false);
  const [batchInvoiceData, setBatchInvoiceData] = useState<any[]>([]);

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
      const result: any = await invoiceApi.list(queryParams as any);
      const list: any[] = result?.records || result?.list || [];
      const total = result?.total || list.length;
      setData(list);
      setPagination({ current: page, pageSize, total });
    } catch (e) {
      message.error('发票列表加载失败，使用示例数据');
      const mockList = Array.from({ length: 8 }, (_, i) => ({
        id: i + 1,
        invoiceNo: 'INV' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        orderNo: 'PG' + dayjs().format('YYYYMMDD') + String(2000 + i).padStart(6, '0'),
        merchantNo: 'M00001',
        channelCode: i % 2 === 0 ? 'NUONUO' : 'BAIWANG',
        invoiceType: i < 6 ? 1 : 2,
        invoiceStatus: [0, 1, 2, 2, 3, 2, 12, 13][i],
        invoiceStatusDesc: '',
        titleType: i % 2 === 0 ? 1 : 2,
        titleTypeDesc: '',
        buyerTitle: i % 2 === 0 ? '张三' : '某某科技有限公司',
        buyerTaxNo: i % 2 === 0 ? '' : '91110108MA01ABCDEF',
        buyerEmail: 'test@example.com',
        invoiceContent: '商品明细',
        invoiceAmount: Number((Math.random() * 1000 + 100).toFixed(2)),
        taxAmount: Number((Math.random() * 60 + 6).toFixed(2)),
        totalAmount: 0,
        taxRate: '6%',
        pdfUrl: i === 2 || i === 5 || i === 6 ? `https://example.com/invoice/${i}.pdf` : '',
        createdAt: dayjs().subtract(i * 60, 'minute').format('YYYY-MM-DD HH:mm:ss'),
        issueTime: dayjs().subtract(i * 55, 'minute').format('YYYY-MM-DD HH:mm:ss'),
      }));
      mockList.forEach((item: any) => {
        item.invoiceStatusDesc = invoiceStatusMap[item.invoiceStatus]?.text || '';
        item.titleTypeDesc = titleTypeMap[item.titleType] || '';
        item.totalAmount = item.invoiceAmount;
      });
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
    if (values.invoiceNo) params.invoiceNo = values.invoiceNo;
    if (values.orderNo) params.orderNo = values.orderNo;
    if (values.channelCode) params.channelCode = values.channelCode;
    if (values.invoiceStatus !== undefined && values.invoiceStatus !== '') params.invoiceStatus = values.invoiceStatus;
    if (values.invoiceType !== undefined && values.invoiceType !== '') params.invoiceType = values.invoiceType;
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

  const handleViewDetail = async (record: Invoice) => {
    try {
      const detail = await invoiceApi.detail(record.invoiceNo);
      setCurrentInvoice(detail || record);
    } catch (e) {
      setCurrentInvoice(record);
    }
    setDetailVisible(true);
  };

  const handleDownload = async (record: Invoice) => {
    const statusOk = record.invoiceStatus === 2 || record.invoiceStatus === 12;
    if (!statusOk) {
      message.warning('只有开票成功或红冲成功的发票才能下载');
      return;
    }
    try {
      if (record.pdfUrl) {
        window.open(record.pdfUrl, '_blank');
        message.success('已打开发票PDF');
      } else {
        await invoiceApi.downloadPdf(record.invoiceNo);
      }
    } catch (e) {
      message.error('下载失败');
    }
  };

  const handleRefreshStatus = async (record: Invoice) => {
    try {
      setRefreshingStatus(record.invoiceNo);
      const result = await invoiceApi.queryStatus(record.invoiceNo);
      if (result) {
        setData((prev) => prev.map((item) => (item.invoiceNo === record.invoiceNo ? { ...item, ...result } : item)));
        message.success('状态已更新');
      }
    } catch (e) {
      message.error('刷新状态失败');
    } finally {
      setRefreshingStatus(null);
    }
  };

  const handleOpenApply = () => {
    setSelectedOrderInfo(null);
    setApplyModalVisible(true);
  };

  const handleOpenRedFlush = (record: Invoice) => {
    if (record.invoiceStatus !== 2) {
      message.warning('只有开票成功的蓝票才能红冲');
      return;
    }
    if (record.invoiceType !== 1) {
      message.warning('红票不能再次红冲');
      return;
    }
    redFlushForm.setFieldsValue({
      originalInvoiceNo: record.invoiceNo,
      redReason: '',
    });
    setRedFlushModalVisible(true);
  };

  const handleRedFlush = async () => {
    try {
      const values = await redFlushForm.validateFields();
      setSubmitting(true);
      await invoiceApi.redFlush(values as any);
      message.success('红冲申请已提交');
      setRedFlushModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('红冲申请失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleBatchApply = async (invoices: any[]) => {
    try {
      setSubmitting(true);
      const res = await invoiceApi.batchApply(invoices as any);
      message.success(`批量开票已提交，共 ${invoices.length} 条记录`);
      setBatchApplyModalVisible(false);
      fetchData();
    } catch (error: any) {
      message.error('批量开票申请失败');
    } finally {
      setSubmitting(false);
    }
  };

  const renderStatusBadge = (status: number) => {
    const tag = invoiceStatusMap[status];
    if (!tag) return <Tag>-</Tag>;
    return (
      <Badge status={tag.status} text={<span style={{ color: tag.color }}>{tag.text}</span>} />
    );
  };

  const columns: ColumnsType<Invoice> = [
    {
      title: '发票号',
      dataIndex: 'invoiceNo',
      key: 'invoiceNo',
      width: 200,
      ellipsis: true,
    },
    {
      title: '关联订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 180,
      ellipsis: true,
    },
    {
      title: '发票类型',
      dataIndex: 'invoiceType',
      key: 'invoiceType',
      width: 80,
      render: (val) => <Tag color={val === 1 ? 'blue' : 'volcano'}>{invoiceTypeMap[val]}</Tag>,
    },
    {
      title: '开票渠道',
      dataIndex: 'channelCode',
      key: 'channelCode',
      width: 100,
      render: (val) => channelMap[val] || val,
    },
    {
      title: '抬头类型',
      dataIndex: 'titleType',
      key: 'titleType',
      width: 80,
      render: (val) => titleTypeMap[val] || '-',
    },
    {
      title: '发票抬头',
      dataIndex: 'buyerTitle',
      key: 'buyerTitle',
      width: 180,
      ellipsis: true,
    },
    {
      title: '开票金额',
      dataIndex: 'invoiceAmount',
      key: 'invoiceAmount',
      width: 120,
      render: (val) => <Text strong type="danger">{formatAmount(val)}</Text>,
      sorter: (a, b) => (a.invoiceAmount || 0) - (b.invoiceAmount || 0),
    },
    {
      title: '开票状态',
      dataIndex: 'invoiceStatus',
      key: 'invoiceStatus',
      width: 100,
      render: (val) => renderStatusBadge(val),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (val) => formatDateTime(val),
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          <Tooltip title="刷新开票状态">
            <Button
              type="link"
              size="small"
              icon={<SyncOutlined spin={refreshingStatus === record.invoiceNo} />}
              onClick={() => handleRefreshStatus(record)}
              loading={refreshingStatus === record.invoiceNo}
            >
              刷新
            </Button>
          </Tooltip>
          {(record.invoiceStatus === 2 || record.invoiceStatus === 12) && (
            <Button
              type="link"
              size="small"
              icon={<DownloadOutlined />}
              onClick={() => handleDownload(record)}
            >
              下载
            </Button>
          )}
          {record.invoiceStatus === 2 && record.invoiceType === 1 && (
            <Button
              type="link"
              size="small"
              danger
              icon={<UndoOutlined />}
              onClick={() => handleOpenRedFlush(record)}
            >
              红冲
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card title="发票查询" style={{ marginBottom: 16 }} extra={
        <Space>
          <Button icon={<FileInvoiceOutlined />} onClick={handleOpenApply}>
            申请开票
          </Button>
          <Button type="primary" icon={<FileInvoiceOutlined />} onClick={() => setBatchApplyModalVisible(true)}>
            批量开票
          </Button>
        </Space>
      }>
        <Form form={queryForm} layout="inline" onFinish={handleQuery}>
          <Form.Item name="invoiceNo" label="发票号">
            <Input placeholder="请输入发票号" allowClear style={{ width: 180 }} />
          </Form.Item>
          <Form.Item name="orderNo" label="订单号">
            <Input placeholder="请输入订单号" allowClear style={{ width: 180 }} />
          </Form.Item>
          <Form.Item name="channelCode" label="开票渠道" initialValue="">
            <Select
              options={[
                { label: '全部', value: '' },
                { label: '诺诺发票', value: 'NUONUO' },
                { label: '百望发票', value: 'BAIWANG' },
              ]}
              style={{ width: 130 }}
            />
          </Form.Item>
          <Form.Item name="invoiceType" label="发票类型" initialValue="">
            <Select
              options={[
                { label: '全部', value: '' },
                { label: '蓝票', value: 1 },
                { label: '红票', value: 2 },
              ]}
              style={{ width: 110 }}
            />
          </Form.Item>
          <Form.Item name="invoiceStatus" label="开票状态" initialValue="">
            <Select
              options={[
                { label: '全部', value: '' },
                { label: '待开票', value: 0 },
                { label: '开票中', value: 1 },
                { label: '开票成功', value: 2 },
                { label: '开票失败', value: 3 },
                { label: '红冲成功', value: 12 },
                { label: '红冲失败', value: 13 },
              ]}
              style={{ width: 110 }}
            />
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

      <Card title="发票列表">
        <Table<Invoice>
          columns={columns}
          dataSource={data}
          rowKey="invoiceNo"
          loading={loading}
          scroll={{ x: 1500 }}
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
        title="发票详情"
        width={720}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
        extra={
          <Space>
            {currentInvoice && (currentInvoice.invoiceStatus === 2 || currentInvoice.invoiceStatus === 12) && (
              <Button type="primary" icon={<PrinterOutlined />} onClick={() => handleDownload(currentInvoice)}>
                下载PDF
              </Button>
            )}
            <Button onClick={() => setDetailVisible(false)}>关闭</Button>
          </Space>
        }
      >
        {currentInvoice && (
          <div>
            <Descriptions title="发票基本信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="发票号" span={2}>
                <Text copyable>{currentInvoice.invoiceNo}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="关联订单号">
                {currentInvoice.orderNo}
              </Descriptions.Item>
              <Descriptions.Item label="发票类型">
                <Tag color={currentInvoice.invoiceType === 1 ? 'blue' : 'volcano'}>
                  {invoiceTypeMap[currentInvoice.invoiceType]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="开票渠道">
                {channelMap[currentInvoice.channelCode] || currentInvoice.channelCode}
              </Descriptions.Item>
              <Descriptions.Item label="开票状态">
                {renderStatusBadge(currentInvoice.invoiceStatus)}
              </Descriptions.Item>
              <Descriptions.Item label="抬头类型">
                {titleTypeMap[currentInvoice.titleType]}
              </Descriptions.Item>
              <Descriptions.Item label="发票抬头" span={2}>
                {currentInvoice.buyerTitle}
              </Descriptions.Item>
              {currentInvoice.buyerTaxNo && (
                <Descriptions.Item label="企业税号" span={2}>
                  {currentInvoice.buyerTaxNo}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="开票金额">
                <Text strong type="danger">{formatAmount(currentInvoice.invoiceAmount)}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="税额">
                {formatAmount(currentInvoice.taxAmount || 0)}
              </Descriptions.Item>
              <Descriptions.Item label="价税合计">
                <Text strong>{formatAmount(currentInvoice.totalAmount || currentInvoice.invoiceAmount)}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="税率">
                {currentInvoice.taxRate || '6%'}
              </Descriptions.Item>
              {currentInvoice.buyerEmail && (
                <Descriptions.Item label="接收邮箱">
                  {currentInvoice.buyerEmail}
                </Descriptions.Item>
              )}
              {currentInvoice.buyerPhone && (
                <Descriptions.Item label="接收手机号">
                  {currentInvoice.buyerPhone}
                </Descriptions.Item>
              )}
              {currentInvoice.buyerAddress && (
                <Descriptions.Item label="企业地址" span={2}>
                  {currentInvoice.buyerAddress}
                </Descriptions.Item>
              )}
              {currentInvoice.buyerBankName && (
                <Descriptions.Item label="开户银行">
                  {currentInvoice.buyerBankName}
                </Descriptions.Item>
              )}
              {currentInvoice.buyerBankAccount && (
                <Descriptions.Item label="银行账号">
                  {currentInvoice.buyerBankAccount}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="发票内容">
                {currentInvoice.invoiceContent}
              </Descriptions.Item>
              <Descriptions.Item label="渠道发票号">
                {currentInvoice.channelInvoiceNo || '-'}
              </Descriptions.Item>
              {currentInvoice.originalInvoiceNo && (
                <Descriptions.Item label="原发票号" span={2}>
                  {currentInvoice.originalInvoiceNo}
                </Descriptions.Item>
              )}
              {currentInvoice.redReason && (
                <Descriptions.Item label="红冲原因" span={2}>
                  {currentInvoice.redReason}
                </Descriptions.Item>
              )}
              {currentInvoice.failReason && (
                <Descriptions.Item label="失败原因" span={2}>
                  <Text type="danger">{currentInvoice.failReason}</Text>
                </Descriptions.Item>
              )}
              {currentInvoice.remark && (
                <Descriptions.Item label="备注" span={2}>
                  {currentInvoice.remark}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="创建时间">
                {formatDateTime(currentInvoice.createdAt)}
              </Descriptions.Item>
              <Descriptions.Item label="开票时间">
                {formatDateTime(currentInvoice.issueTime)}
              </Descriptions.Item>
            </Descriptions>

            {currentInvoice.pdfUrl && (
              <Alert
                type="success"
                showIcon
                message={
                  <Space>
                    <FileInvoiceOutlined />
                    <Text>发票PDF已生成</Text>
                    <a onClick={() => window.open(currentInvoice.pdfUrl, '_blank')}>点击查看</a>
                  </Space>
                }
                style={{ marginTop: 16 }}
              />
            )}
          </div>
        )}
      </Drawer>

      <InvoiceApplyModal
        visible={applyModalVisible}
        onClose={() => setApplyModalVisible(false)}
        onSuccess={() => {
          message.success('发票申请已提交');
          fetchData();
        }}
        orderInfo={selectedOrderInfo}
      />

      <Modal
        title="申请发票红冲"
        open={redFlushModalVisible}
        onCancel={() => setRedFlushModalVisible(false)}
        onOk={handleRedFlush}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form form={redFlushForm} layout="vertical">
          <Form.Item name="originalInvoiceNo" label="原发票号" rules={[{ required: true }]}>
            <Input disabled />
          </Form.Item>
          <Form.Item
            name="redReason"
            label="红冲原因"
            rules={[{ required: true, message: '请输入红冲原因' }]}
          >
            <Input.TextArea rows={4} placeholder="请输入红冲原因" />
          </Form.Item>
          <Alert
            type="warning"
            showIcon
            message="红冲说明"
            description="发票红冲后将生成一张红字发票，原发票状态将变更为已红冲。此操作不可撤销。"
          />
        </Form>
      </Modal>

      <BatchInvoiceApplyModal
        visible={batchApplyModalVisible}
        onClose={() => setBatchApplyModalVisible(false)}
        onSubmit={handleBatchApply}
        submitting={submitting}
      />
    </div>
  );
};

export default InvoiceList;

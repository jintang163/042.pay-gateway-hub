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
  Tabs,
  Descriptions,
  Drawer,
  Row,
  Col,
  Statistic,
  Divider,
  InputNumber,
  Tooltip,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlayCircleOutlined,
  DownloadOutlined,
  EyeOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  DollarOutlined,
  MinusCircleOutlined,
  PlusCircleOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  StopOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { reconcileApi, reconcileDetailApi, errorOrderApi } from '@/api';
import type {
  Reconcile,
  ReconcileQueryParams,
  ReconcileDetail,
  ReconcileDetailQueryParams,
  ReconcileSummary,
  ErrorOrder,
  ErrorOrderQueryParams,
  ErrorOrderApplyRequest,
  ErrorOrderAuditRequest,
  DiffType,
  HandleStatus,
  ErrorStatus,
  ErrorHandleType,
  ErrorAuditStatus,
} from '@/types/settlement';
import { formatDateTime } from '@/utils';

const { RangePicker } = DatePicker;

const formatAmount = (fen: number | undefined | null): string => {
  if (fen === undefined || fen === null) return '-';
  return `¥${(fen / 100).toFixed(2)}`;
};

const formatDiffAmount = (fen: number | undefined | null): string => {
  if (fen === undefined || fen === null) return '-';
  const sign = fen > 0 ? '+' : '';
  return `${sign}¥${(fen / 100).toFixed(2)}`;
};

const reconcileStatusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'processing', text: '处理中' },
  1: { color: 'success', text: '完成' },
  2: { color: 'error', text: '异常' },
};

const diffTypeMap: Record<number, { color: string; text: string; icon: React.ReactNode }> = {
  1: { color: 'green', text: '长款', icon: <PlusCircleOutlined /> },
  2: { color: 'red', text: '短款', icon: <MinusCircleOutlined /> },
  3: { color: 'orange', text: '金额不一致', icon: <DollarOutlined /> },
  4: { color: 'purple', text: '状态不一致', icon: <WarningOutlined /> },
};

const handleStatusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'warning', text: '待处理' },
  1: { color: 'processing', text: '处理中' },
  2: { color: 'success', text: '已处理' },
  3: { color: 'default', text: '已忽略' },
};

const errorStatusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'warning', text: '待处理' },
  1: { color: 'processing', text: '处理中' },
  2: { color: 'success', text: '处理成功' },
  3: { color: 'error', text: '处理失败' },
  4: { color: 'default', text: '已关闭' },
};

const errorHandleTypeMap: Record<number, { color: string; text: string }> = {
  1: { color: 'blue', text: '补单' },
  2: { color: 'orange', text: '退款' },
  3: { color: 'purple', text: '调账' },
  4: { color: 'default', text: '忽略' },
};

const auditStatusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'warning', text: '待审核' },
  1: { color: 'success', text: '审核通过' },
  2: { color: 'error', text: '审核拒绝' },
};

const payChannelOptions = [
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信支付', value: 'WECHAT_PAY' },
  { label: '银联', value: 'UNION_PAY' },
];

const ReconcilePage = () => {
  const [activeTab, setActiveTab] = useState('reconcile');

  const [reconcileQueryForm] = Form.useForm();
  const [detailQueryForm] = Form.useForm();
  const [errorQueryForm] = Form.useForm();
  const [executeForm] = Form.useForm();
  const [applyForm] = Form.useForm();
  const [auditForm] = Form.useForm();

  const [reconcileLoading, setReconcileLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [errorLoading, setErrorLoading] = useState(false);
  const [summaryLoading, setSummaryLoading] = useState(false);

  const [reconcileData, setReconcileData] = useState<Reconcile[]>([]);
  const [detailData, setDetailData] = useState<ReconcileDetail[]>([]);
  const [errorData, setErrorData] = useState<ErrorOrder[]>([]);
  const [summary, setSummary] = useState<ReconcileSummary | null>(null);

  const [reconcilePagination, setReconcilePagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [detailPagination, setDetailPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [errorPagination, setErrorPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const [executeModalVisible, setExecuteModalVisible] = useState(false);
  const [detailDrawerVisible, setDetailDrawerVisible] = useState(false);
  const [currentDetail, setCurrentDetail] = useState<ReconcileDetail | null>(null);
  const [applyModalVisible, setApplyModalVisible] = useState(false);
  const [auditModalVisible, setAuditModalVisible] = useState(false);
  const [errorDrawerVisible, setErrorDrawerVisible] = useState(false);
  const [currentError, setCurrentError] = useState<ErrorOrder | null>(null);
  const [errorOrderList, setErrorOrderList] = useState<ErrorOrder[]>([]);

  const [executing, setExecuting] = useState(false);
  const [applySubmitting, setApplySubmitting] = useState(false);
  const [auditSubmitting, setAuditSubmitting] = useState(false);

  const fetchReconcileData = async (
    page = reconcilePagination.current,
    pageSize = reconcilePagination.pageSize,
    params?: Partial<ReconcileQueryParams>,
  ) => {
    try {
      setReconcileLoading(true);
      const queryParams: ReconcileQueryParams = { pageNum: page, pageSize, ...params };
      const result = await reconcileApi.list(queryParams);
      setReconcileData(result.list);
      setReconcilePagination({ current: page, pageSize, total: result.total });
    } catch (e) {
      message.error('加载对账记录失败');
    } finally {
      setReconcileLoading(false);
    }
  };

  const fetchDetailData = async (
    page = detailPagination.current,
    pageSize = detailPagination.pageSize,
    params?: Partial<ReconcileDetailQueryParams>,
  ) => {
    try {
      setDetailLoading(true);
      const queryParams: ReconcileDetailQueryParams = { pageNum: page, pageSize, ...params };
      const result = await reconcileDetailApi.list(queryParams);
      setDetailData(result.list);
      setDetailPagination({ current: page, pageSize, total: result.total });
    } catch (e) {
      message.error('加载差异明细失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const fetchErrorData = async (
    page = errorPagination.current,
    pageSize = errorPagination.pageSize,
    params?: Partial<ErrorOrderQueryParams>,
  ) => {
    try {
      setErrorLoading(true);
      const queryParams: ErrorOrderQueryParams = { pageNum: page, pageSize, ...params };
      const result = await errorOrderApi.list(queryParams);
      setErrorData(result.list);
      setErrorPagination({ current: page, pageSize, total: result.total });
    } catch (e) {
      message.error('加载差错单失败');
    } finally {
      setErrorLoading(false);
    }
  };

  const fetchSummary = async (params: { reconcileNo?: string; reconcileDate?: string; payChannel?: string }) => {
    try {
      setSummaryLoading(true);
      const result = await reconcileApi.summary(params);
      setSummary(result);
    } catch (e) {
      console.warn('加载汇总信息失败', e);
    } finally {
      setSummaryLoading(false);
    }
  };

  useEffect(() => {
    fetchReconcileData();
    fetchDetailData();
    fetchErrorData();
  }, []);

  useEffect(() => {
    if (reconcileData.length > 0) {
      fetchSummary({ reconcileNo: reconcileData[0].reconcileNo });
    }
  }, [reconcileData]);

  const handleReconcileQuery = () => {
    const values = reconcileQueryForm.getFieldsValue();
    const params: Partial<ReconcileQueryParams> = { ...values };
    if (values.reconcileDate) {
      params.reconcileDate = (values.reconcileDate as Dayjs).format('YYYY-MM-DD');
    }
    fetchReconcileData(1, reconcilePagination.pageSize, params);
  };

  const handleReconcileReset = () => {
    reconcileQueryForm.resetFields();
    fetchReconcileData(1, reconcilePagination.pageSize);
  };

  const handleDetailQuery = () => {
    const values = detailQueryForm.getFieldsValue();
    const params: Partial<ReconcileDetailQueryParams> = { ...values };
    if (values.reconcileDate) {
      params.reconcileDate = (values.reconcileDate as Dayjs).format('YYYY-MM-DD');
    }
    fetchDetailData(1, detailPagination.pageSize, params);
  };

  const handleDetailReset = () => {
    detailQueryForm.resetFields();
    fetchDetailData(1, detailPagination.pageSize);
  };

  const handleErrorQuery = () => {
    const values = errorQueryForm.getFieldsValue();
    fetchErrorData(1, errorPagination.pageSize, values);
  };

  const handleErrorReset = () => {
    errorQueryForm.resetFields();
    fetchErrorData(1, errorPagination.pageSize);
  };

  const handleExecuteReconcile = async () => {
    try {
      const values = await executeForm.validateFields();
      setExecuting(true);
      const reconcileDate = (values.reconcileDate as Dayjs).format('YYYY-MM-DD');
      await reconcileApi.execute({ payChannel: values.payChannel, reconcileDate });
      message.success('对账任务已提交执行');
      setExecuteModalVisible(false);
      fetchReconcileData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('执行对账失败');
    } finally {
      setExecuting(false);
    }
  };

  const handleViewReconcileDetails = (record: Reconcile) => {
    fetchSummary({ reconcileNo: record.reconcileNo });
    detailQueryForm.setFieldsValue({ reconcileNo: record.reconcileNo });
    fetchDetailData(1, detailPagination.pageSize, { reconcileNo: record.reconcileNo });
    setActiveTab('detail');
  };

  const handleViewDetail = async (record: ReconcileDetail) => {
    setCurrentDetail(record);
    setDetailDrawerVisible(true);
    try {
      const list = await errorOrderApi.listByDetail(record.id);
      setErrorOrderList(list);
    } catch (e) {
      setErrorOrderList([]);
    }
  };

  const handleApplyErrorOrder = (record: ReconcileDetail) => {
    setCurrentDetail(record);
    applyForm.resetFields();
    applyForm.setFieldsValue({
      reconcileDetailId: record.id,
      handleType: record.diffType === 1 ? 1 : record.diffType === 2 ? 2 : 3,
    });
    setApplyModalVisible(true);
  };

  const handleSubmitApply = async () => {
    try {
      const values = await applyForm.validateFields();
      setApplySubmitting(true);
      await errorOrderApi.apply(values as ErrorOrderApplyRequest);
      message.success('差错单申请成功');
      setApplyModalVisible(false);
      fetchDetailData();
      fetchErrorData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('申请失败');
    } finally {
      setApplySubmitting(false);
    }
  };

  const handleAudit = (record: ErrorOrder) => {
    setCurrentError(record);
    auditForm.resetFields();
    auditForm.setFieldsValue({ errorOrderId: record.id });
    setAuditModalVisible(true);
  };

  const handleSubmitAudit = async () => {
    try {
      const values = await auditForm.validateFields();
      setAuditSubmitting(true);
      await errorOrderApi.audit(values as ErrorOrderAuditRequest);
      message.success('审核完成');
      setAuditModalVisible(false);
      fetchErrorData();
      fetchDetailData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('审核失败');
    } finally {
      setAuditSubmitting(false);
    }
  };

  const handleProcessError = async (record: ErrorOrder, action: 'supplement' | 'refund' | 'adjust' | 'ignore') => {
    try {
      Modal.confirm({
        title: '确认处理',
        content: `确定要执行"${action === 'supplement' ? '补单' : action === 'refund' ? '退款' : action === 'adjust' ? '调账' : '忽略'}"操作吗？`,
        onOk: async () => {
          let result;
          switch (action) {
            case 'supplement':
              result = await errorOrderApi.processSupplement(record.id);
              break;
            case 'refund':
              result = await errorOrderApi.processRefund(record.id);
              break;
            case 'adjust':
              result = await errorOrderApi.processAdjust(record.id);
              break;
            case 'ignore':
              result = await errorOrderApi.processIgnore(record.id);
              break;
          }
          message.success('处理成功');
          fetchErrorData();
          fetchDetailData();
        },
      });
    } catch (e) {
      message.error('处理失败');
    }
  };

  const handleExport = async () => {
    const values = detailQueryForm.getFieldsValue();
    const params: Record<string, unknown> = { ...values };
    if (values.reconcileDate) {
      params.reconcileDate = (values.reconcileDate as Dayjs).format('YYYY-MM-DD');
    }
    try {
      await reconcileApi.export(params);
    } catch (e) {
      message.error('导出失败');
    }
  };

  const handleIgnoreDetail = async (record: ReconcileDetail) => {
    Modal.confirm({
      title: '确认忽略',
      content: '确定要忽略该差异明细吗？',
      onOk: async () => {
        try {
          await reconcileDetailApi.ignore(record.id);
          message.success('已忽略');
          fetchDetailData();
        } catch (e) {
          message.error('操作失败');
        }
      },
    });
  };

  const handleViewErrorDetail = (record: ErrorOrder) => {
    setCurrentError(record);
    setErrorDrawerVisible(true);
  };

  const reconcileColumns: ColumnsType<Reconcile> = [
    { title: '对账单号', dataIndex: 'reconcileNo', key: 'reconcileNo', width: 200 },
    { title: '对账日期', dataIndex: 'reconcileDate', key: 'reconcileDate', width: 120 },
    {
      title: '支付渠道',
      dataIndex: 'payChannel',
      key: 'payChannel',
      width: 120,
      render: (val) => <Tag color="blue">{val}</Tag>,
    },
    { title: '总笔数', dataIndex: 'totalCount', key: 'totalCount', width: 90 },
    {
      title: '匹配笔数',
      dataIndex: 'matchCount',
      key: 'matchCount',
      width: 100,
      render: (val) => <span style={{ color: '#52c41a' }}>{val}</span>,
    },
    {
      title: '差异笔数',
      dataIndex: 'mismatchCount',
      key: 'mismatchCount',
      width: 100,
      render: (val) => (
        <span style={{ color: val > 0 ? '#ff4d4f' : undefined, fontWeight: val > 0 ? 600 : undefined }}>
          {val}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'reconcileStatus',
      key: 'reconcileStatus',
      width: 100,
      render: (status: number) => {
        const tag = reconcileStatusMap[status] || { color: 'default', text: String(status) };
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (val) => formatDateTime(val),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewReconcileDetails(record)}>
            查看差异
          </Button>
        </Space>
      ),
    },
  ];

  const detailColumns: ColumnsType<ReconcileDetail> = [
    { title: '差异单号', dataIndex: 'detailNo', key: 'detailNo', width: 200 },
    { title: '对账单号', dataIndex: 'reconcileNo', key: 'reconcileNo', width: 200 },
    { title: '对账日期', dataIndex: 'reconcileDate', key: 'reconcileDate', width: 120 },
    {
      title: '支付渠道',
      dataIndex: 'payChannelDesc',
      key: 'payChannelDesc',
      width: 100,
      render: (val) => <Tag color="blue">{val}</Tag>,
    },
    {
      title: '差异类型',
      dataIndex: 'diffType',
      key: 'diffType',
      width: 130,
      render: (type: number, record) => {
        const info = diffTypeMap[type] || { color: 'default', text: String(type), icon: null };
        return (
          <Tag color={info.color} icon={info.icon}>
            {info.text}
          </Tag>
        );
      },
    },
    { title: '平台订单号', dataIndex: 'orderNo', key: 'orderNo', width: 200, ellipsis: true },
    { title: '商户号', dataIndex: 'merchantNo', key: 'merchantNo', width: 140 },
    { title: '渠道交易号', dataIndex: 'channelTradeNo', key: 'channelTradeNo', width: 180, ellipsis: true },
    {
      title: '本地金额',
      dataIndex: 'localAmount',
      key: 'localAmount',
      width: 120,
      render: (val) => formatAmount(val),
    },
    {
      title: '渠道金额',
      dataIndex: 'channelAmount',
      key: 'channelAmount',
      width: 120,
      render: (val) => formatAmount(val),
    },
    {
      title: '差异金额',
      dataIndex: 'diffAmount',
      key: 'diffAmount',
      width: 130,
      render: (val) => {
        if (val === undefined || val === null) return '-';
        const color = val > 0 ? '#ff4d4f' : val < 0 ? '#52c41a' : undefined;
        return <span style={{ color, fontWeight: 600 }}>{formatDiffAmount(val)}</span>;
      },
    },
    {
      title: '处理状态',
      dataIndex: 'handleStatus',
      key: 'handleStatus',
      width: 100,
      render: (status: number) => {
        const tag = handleStatusMap[status] || { color: 'default', text: String(status) };
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '差错单号',
      dataIndex: 'errorOrderNo',
      key: 'errorOrderNo',
      width: 180,
      render: (val) => val || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (val) => formatDateTime(val),
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          {record.handleStatus === 0 && (
            <>
              <Button
                type="link"
                size="small"
                icon={<FileTextOutlined />}
                onClick={() => handleApplyErrorOrder(record)}
              >
                申请差错
              </Button>
              <Button type="link" size="small" danger icon={<StopOutlined />} onClick={() => handleIgnoreDetail(record)}>
                忽略
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  const errorColumns: ColumnsType<ErrorOrder> = [
    { title: '差错单号', dataIndex: 'errorNo', key: 'errorNo', width: 200 },
    { title: '对账单号', dataIndex: 'reconcileNo', key: 'reconcileNo', width: 200 },
    {
      title: '支付渠道',
      dataIndex: 'payChannelDesc',
      key: 'payChannelDesc',
      width: 100,
      render: (val) => <Tag color="blue">{val}</Tag>,
    },
    {
      title: '差错类型',
      dataIndex: 'errorType',
      key: 'errorType',
      width: 120,
      render: (type: number, record) => {
        const info = diffTypeMap[type] || { color: 'default', text: String(type), icon: null };
        return (
          <Tag color={info.color} icon={info.icon}>
            {info.text}
          </Tag>
        );
      },
    },
    {
      title: '处理方式',
      dataIndex: 'handleType',
      key: 'handleType',
      width: 100,
      render: (type: number) => {
        if (type === undefined || type === null) return '-';
        const info = errorHandleTypeMap[type] || { color: 'default', text: String(type) };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    { title: '平台订单号', dataIndex: 'orderNo', key: 'orderNo', width: 200, ellipsis: true },
    {
      title: '差异金额',
      dataIndex: 'diffAmount',
      key: 'diffAmount',
      width: 130,
      render: (val) => {
        if (val === undefined || val === null) return '-';
        const color = val > 0 ? '#ff4d4f' : val < 0 ? '#52c41a' : undefined;
        return <span style={{ color, fontWeight: 600 }}>{formatDiffAmount(val)}</span>;
      },
    },
    {
      title: '差错状态',
      dataIndex: 'errorStatus',
      key: 'errorStatus',
      width: 100,
      render: (status: number) => {
        const tag = errorStatusMap[status] || { color: 'default', text: String(status) };
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '审核状态',
      dataIndex: 'auditStatus',
      key: 'auditStatus',
      width: 100,
      render: (status: number) => {
        if (status === undefined || status === null) return '-';
        const tag = auditStatusMap[status] || { color: 'default', text: String(status) };
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    { title: '申请人', dataIndex: 'applyUserName', key: 'applyUserName', width: 100 },
    {
      title: '申请时间',
      dataIndex: 'applyTime',
      key: 'applyTime',
      width: 180,
      render: (val) => (val ? formatDateTime(val) : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewErrorDetail(record)}>
            详情
          </Button>
          {record.auditStatus === 0 && (
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleAudit(record)}
            >
              审核
            </Button>
          )}
          {record.auditStatus === 1 && record.errorStatus === 1 && (
            <>
              {record.handleType === 1 && (
                <Button
                  type="link"
                  size="small"
                  icon={<PlusCircleOutlined />}
                  onClick={() => handleProcessError(record, 'supplement')}
                >
                  执行补单
                </Button>
              )}
              {record.handleType === 2 && (
                <Button
                  type="link"
                  size="small"
                  icon={<MinusCircleOutlined />}
                  onClick={() => handleProcessError(record, 'refund')}
                >
                  执行退款
                </Button>
              )}
              {record.handleType === 3 && (
                <Button
                  type="link"
                  size="small"
                  icon={<DollarOutlined />}
                  onClick={() => handleProcessError(record, 'adjust')}
                >
                  执行调账
                </Button>
              )}
              {record.handleType === 4 && (
                <Button
                  type="link"
                  size="small"
                  icon={<StopOutlined />}
                  onClick={() => handleProcessError(record, 'ignore')}
                >
                  确认忽略
                </Button>
              )}
            </>
          )}
        </Space>
      ),
    },
  ];

  const renderSummary = () => {
    if (!summary) return null;
    return (
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={4}>
          <Card loading={summaryLoading}>
            <Statistic
              title="交易总笔数"
              value={summary.totalCount}
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card loading={summaryLoading}>
            <Statistic
              title="匹配笔数"
              value={summary.matchCount}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card loading={summaryLoading}>
            <Statistic
              title="差异笔数"
              value={summary.mismatchCount}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card loading={summaryLoading}>
            <Statistic
              title="长款笔数"
              value={summary.longFund.count}
              prefix={<PlusCircleOutlined />}
              suffix={`笔 / ${formatAmount(summary.longFund.totalAmount)}`}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card loading={summaryLoading}>
            <Statistic
              title="短款笔数"
              value={summary.shortFund.count}
              prefix={<MinusCircleOutlined />}
              suffix={`笔 / ${formatAmount(summary.shortFund.totalAmount)}`}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card loading={summaryLoading}>
            <Statistic
              title="金额不一致"
              value={summary.amountMismatch.count}
              prefix={<DollarOutlined />}
              suffix={`笔 / 合计 ${formatAmount(summary.amountMismatch.totalDiffAmount)}`}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>
    );
  };

  const renderReconcileTab = () => (
    <div>
      {renderSummary()}
      <Card style={{ marginBottom: 16 }}>
        <Form form={reconcileQueryForm} layout="inline" onFinish={handleReconcileQuery}>
          <Form.Item name="payChannel" label="支付渠道">
            <Select placeholder="全部渠道" style={{ width: 150 }} allowClear options={payChannelOptions} />
          </Form.Item>
          <Form.Item name="reconcileDate" label="对账日期">
            <DatePicker style={{ width: 180 }} />
          </Form.Item>
          <Form.Item name="reconcileStatus" label="状态" initialValue={undefined}>
            <Select
              placeholder="全部状态"
              style={{ width: 130 }}
              allowClear
              options={[
                { label: '处理中', value: 0 },
                { label: '完成', value: 1 },
                { label: '异常', value: 2 },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button onClick={handleReconcileReset} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        extra={
          <Space>
            <Button icon={<DownloadOutlined />} onClick={handleExport}>
              导出差异
            </Button>
            <Button type="primary" icon={<PlayCircleOutlined />} onClick={() => setExecuteModalVisible(true)}>
              执行对账
            </Button>
          </Space>
        }
      >
        <Table<Reconcile>
          columns={reconcileColumns}
          dataSource={reconcileData}
          rowKey="id"
          loading={reconcileLoading}
          scroll={{ x: 1400 }}
          pagination={{
            ...reconcilePagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchReconcileData(page, pageSize),
          }}
        />
      </Card>
    </div>
  );

  const renderDetailTab = () => (
    <div>
      {renderSummary()}
      <Card style={{ marginBottom: 16 }}>
        <Form form={detailQueryForm} layout="inline" onFinish={handleDetailQuery}>
          <Form.Item name="reconcileNo" label="对账单号">
            <Input placeholder="请输入对账单号" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="payChannel" label="支付渠道">
            <Select placeholder="全部渠道" style={{ width: 130 }} allowClear options={payChannelOptions} />
          </Form.Item>
          <Form.Item name="diffType" label="差异类型" initialValue={undefined}>
            <Select
              placeholder="全部类型"
              style={{ width: 150 }}
              allowClear
              options={[
                { label: '长款', value: 1 },
                { label: '短款', value: 2 },
                { label: '金额不一致', value: 3 },
                { label: '状态不一致', value: 4 },
              ]}
            />
          </Form.Item>
          <Form.Item name="handleStatus" label="处理状态" initialValue={undefined}>
            <Select
              placeholder="全部状态"
              style={{ width: 130 }}
              allowClear
              options={[
                { label: '待处理', value: 0 },
                { label: '处理中', value: 1 },
                { label: '已处理', value: 2 },
                { label: '已忽略', value: 3 },
              ]}
            />
          </Form.Item>
          <Form.Item name="orderNo" label="订单号">
            <Input placeholder="请输入订单号" allowClear style={{ width: 180 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button onClick={handleDetailReset} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        extra={
          <Button icon={<DownloadOutlined />} onClick={handleExport}>
            导出差异
          </Button>
        }
      >
        <Table<ReconcileDetail>
          columns={detailColumns}
          dataSource={detailData}
          rowKey="id"
          loading={detailLoading}
          scroll={{ x: 2200 }}
          pagination={{
            ...detailPagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchDetailData(page, pageSize),
          }}
        />
      </Card>
    </div>
  );

  const renderErrorTab = () => (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Form form={errorQueryForm} layout="inline" onFinish={handleErrorQuery}>
          <Form.Item name="errorNo" label="差错单号">
            <Input placeholder="请输入差错单号" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="payChannel" label="支付渠道">
            <Select placeholder="全部渠道" style={{ width: 130 }} allowClear options={payChannelOptions} />
          </Form.Item>
          <Form.Item name="errorType" label="差错类型" initialValue={undefined}>
            <Select
              placeholder="全部类型"
              style={{ width: 130 }}
              allowClear
              options={[
                { label: '长款', value: 1 },
                { label: '短款', value: 2 },
                { label: '金额不一致', value: 3 },
                { label: '状态不一致', value: 4 },
              ]}
            />
          </Form.Item>
          <Form.Item name="errorStatus" label="差错状态" initialValue={undefined}>
            <Select
              placeholder="全部状态"
              style={{ width: 120 }}
              allowClear
              options={[
                { label: '待处理', value: 0 },
                { label: '处理中', value: 1 },
                { label: '处理成功', value: 2 },
                { label: '处理失败', value: 3 },
                { label: '已关闭', value: 4 },
              ]}
            />
          </Form.Item>
          <Form.Item name="auditStatus" label="审核状态" initialValue={undefined}>
            <Select
              placeholder="全部状态"
              style={{ width: 120 }}
              allowClear
              options={[
                { label: '待审核', value: 0 },
                { label: '审核通过', value: 1 },
                { label: '审核拒绝', value: 2 },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button onClick={handleErrorReset} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card>
        <Table<ErrorOrder>
          columns={errorColumns}
          dataSource={errorData}
          rowKey="id"
          loading={errorLoading}
          scroll={{ x: 2200 }}
          pagination={{
            ...errorPagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchErrorData(page, pageSize),
          }}
        />
      </Card>
    </div>
  );

  return (
    <div>
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <Tabs.TabPane tab="对账记录" key="reconcile">
          {renderReconcileTab()}
        </Tabs.TabPane>
        <Tabs.TabPane tab="差异明细" key="detail">
          {renderDetailTab()}
        </Tabs.TabPane>
        <Tabs.TabPane tab="差错处理" key="error">
          {renderErrorTab()}
        </Tabs.TabPane>
      </Tabs>

      <Modal
        title="执行对账"
        open={executeModalVisible}
        onCancel={() => setExecuteModalVisible(false)}
        onOk={handleExecuteReconcile}
        confirmLoading={executing}
        destroyOnClose
      >
        <Form form={executeForm} layout="vertical">
          <Form.Item
            name="payChannel"
            label="支付渠道"
            rules={[{ required: true, message: '请选择支付渠道' }]}
          >
            <Select placeholder="请选择支付渠道" options={payChannelOptions} />
          </Form.Item>
          <Form.Item
            name="reconcileDate"
            label="对账日期"
            rules={[{ required: true, message: '请选择对账日期' }]}
          >
            <DatePicker style={{ width: '100%' }} disabledDate={(d) => d && d.isAfter(dayjs().subtract(1, 'day'))} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="差异明细详情"
        width={720}
        open={detailDrawerVisible}
        onClose={() => setDetailDrawerVisible(false)}
      >
        {currentDetail && (
          <div>
            <Descriptions title="基本信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="差异单号">{currentDetail.detailNo}</Descriptions.Item>
              <Descriptions.Item label="对账单号">{currentDetail.reconcileNo}</Descriptions.Item>
              <Descriptions.Item label="对账日期">{currentDetail.reconcileDate}</Descriptions.Item>
              <Descriptions.Item label="支付渠道">{currentDetail.payChannelDesc}</Descriptions.Item>
              <Descriptions.Item label="差异类型" span={2}>
                <Tag color={diffTypeMap[currentDetail.diffType]?.color} icon={diffTypeMap[currentDetail.diffType]?.icon}>
                  {currentDetail.diffTypeDesc}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="处理状态" span={2}>
                <Tag color={handleStatusMap[currentDetail.handleStatus]?.color}>
                  {currentDetail.handleStatusDesc}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="差错单号" span={2}>
                {currentDetail.errorOrderNo || '-'}
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="订单信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="平台订单号" span={2}>
                {currentDetail.orderNo || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="商户号">{currentDetail.merchantNo || '-'}</Descriptions.Item>
              <Descriptions.Item label="渠道交易号" span={2}>
                {currentDetail.channelTradeNo || '-'}
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="金额比对" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="本地金额">{formatAmount(currentDetail.localAmount)}</Descriptions.Item>
              <Descriptions.Item label="渠道金额">{formatAmount(currentDetail.channelAmount)}</Descriptions.Item>
              <Descriptions.Item label="差异金额" span={2}>
                <span
                  style={{
                    color:
                      (currentDetail.diffAmount ?? 0) > 0
                        ? '#ff4d4f'
                        : (currentDetail.diffAmount ?? 0) < 0
                          ? '#52c41a'
                          : undefined,
                    fontWeight: 600,
                    fontSize: 16,
                  }}
                >
                  {formatDiffAmount(currentDetail.diffAmount)}
                </span>
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="状态比对" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="本地状态">{currentDetail.localStatusDesc || '-'}</Descriptions.Item>
              <Descriptions.Item label="渠道状态">{currentDetail.channelStatus || '-'}</Descriptions.Item>
              <Descriptions.Item label="本地支付时间">
                {currentDetail.localPayTime ? formatDateTime(currentDetail.localPayTime) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="渠道支付时间">
                {currentDetail.channelPayTime ? formatDateTime(currentDetail.channelPayTime) : '-'}
              </Descriptions.Item>
            </Descriptions>

            {currentDetail.handleStatus !== 0 && (
              <Descriptions title="处理信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
                <Descriptions.Item label="处理人">{currentDetail.handleUserName || '-'}</Descriptions.Item>
                <Descriptions.Item label="处理时间">
                  {currentDetail.handleTime ? formatDateTime(currentDetail.handleTime) : '-'}
                </Descriptions.Item>
                <Descriptions.Item label="处理备注" span={2}>
                  {currentDetail.handleRemark || '-'}
                </Descriptions.Item>
              </Descriptions>
            )}

            <Divider orientation="left">关联差错单</Divider>
            {errorOrderList.length > 0 ? (
              <Table
                dataSource={errorOrderList}
                rowKey="id"
                size="small"
                pagination={false}
                columns={[
                  { title: '差错单号', dataIndex: 'errorNo', key: 'errorNo' },
                  {
                    title: '处理方式',
                    dataIndex: 'handleType',
                    key: 'handleType',
                    render: (type) => errorHandleTypeMap[type]?.text || '-',
                  },
                  {
                    title: '差错状态',
                    dataIndex: 'errorStatus',
                    key: 'errorStatus',
                    render: (status) => {
                      const tag = errorStatusMap[status];
                      return tag ? <Tag color={tag.color}>{tag.text}</Tag> : status;
                    },
                  },
                  {
                    title: '审核状态',
                    dataIndex: 'auditStatus',
                    key: 'auditStatus',
                    render: (status) => {
                      if (status === undefined || status === null) return '-';
                      const tag = auditStatusMap[status];
                      return tag ? <Tag color={tag.color}>{tag.text}</Tag> : status;
                    },
                  },
                  { title: '申请人', dataIndex: 'applyUserName', key: 'applyUserName' },
                  {
                    title: '申请时间',
                    dataIndex: 'applyTime',
                    key: 'applyTime',
                    render: (val) => (val ? formatDateTime(val) : '-'),
                  },
                ]}
              />
            ) : (
              <div style={{ textAlign: 'center', padding: '24px 0', color: '#999' }}>暂无差错单记录</div>
            )}
          </div>
        )}
      </Drawer>

      <Modal
        title="申请差错单"
        open={applyModalVisible}
        onCancel={() => setApplyModalVisible(false)}
        onOk={handleSubmitApply}
        confirmLoading={applySubmitting}
        width={560}
        destroyOnClose
      >
        {currentDetail && (
          <div style={{ marginBottom: 16 }}>
            <Descriptions column={2} size="small" bordered>
              <Descriptions.Item label="差异单号">{currentDetail.detailNo}</Descriptions.Item>
              <Descriptions.Item label="差异类型">
                <Tag color={diffTypeMap[currentDetail.diffType]?.color}>{currentDetail.diffTypeDesc}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="差异金额" span={2}>
                <span style={{ fontWeight: 600 }}>{formatDiffAmount(currentDetail.diffAmount)}</span>
              </Descriptions.Item>
            </Descriptions>
          </div>
        )}
        <Form form={applyForm} layout="vertical">
          <Form.Item name="reconcileDetailId" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            name="handleType"
            label="处理方式"
            rules={[{ required: true, message: '请选择处理方式' }]}
          >
            <Select
              placeholder="请选择处理方式"
              options={[
                { label: '补单', value: 1 },
                { label: '退款', value: 2 },
                { label: '调账', value: 3 },
                { label: '忽略', value: 4 },
              ]}
            />
          </Form.Item>
          <Form.Item
            name="applyRemark"
            label="申请备注"
          >
            <Input.TextArea rows={3} placeholder="请输入申请备注" maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="差错单审核"
        open={auditModalVisible}
        onCancel={() => setAuditModalVisible(false)}
        onOk={handleSubmitAudit}
        confirmLoading={auditSubmitting}
        width={560}
        destroyOnClose
      >
        {currentError && (
          <div style={{ marginBottom: 16 }}>
            <Descriptions column={2} size="small" bordered>
              <Descriptions.Item label="差错单号">{currentError.errorNo}</Descriptions.Item>
              <Descriptions.Item label="差错类型">
                <Tag color={diffTypeMap[currentError.errorType]?.color}>{currentError.errorTypeDesc}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="处理方式">
                <Tag color={errorHandleTypeMap[currentError.handleType!]?.color}>
                  {currentError.handleTypeDesc}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="差异金额">
                <span style={{ fontWeight: 600 }}>{formatDiffAmount(currentError.diffAmount)}</span>
              </Descriptions.Item>
              <Descriptions.Item label="申请人">{currentError.applyUserName}</Descriptions.Item>
              <Descriptions.Item label="申请时间">
                {currentError.applyTime ? formatDateTime(currentError.applyTime) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="申请备注" span={2}>
                {currentError.applyRemark || '-'}
              </Descriptions.Item>
            </Descriptions>
          </div>
        )}
        <Form form={auditForm} layout="vertical">
          <Form.Item name="errorOrderId" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            name="auditStatus"
            label="审核结果"
            rules={[{ required: true, message: '请选择审核结果' }]}
          >
            <Select
              placeholder="请选择审核结果"
              options={[
                { label: '审核通过', value: 1 },
                { label: '审核拒绝', value: 2 },
              ]}
            />
          </Form.Item>
          <Form.Item
            name="auditRemark"
            label="审核备注"
          >
            <Input.TextArea rows={3} placeholder="请输入审核备注" maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="差错单详情"
        width={720}
        open={errorDrawerVisible}
        onClose={() => setErrorDrawerVisible(false)}
      >
        {currentError && (
          <div>
            <Descriptions title="基本信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="差错单号">{currentError.errorNo}</Descriptions.Item>
              <Descriptions.Item label="对账单号">{currentError.reconcileNo || '-'}</Descriptions.Item>
              <Descriptions.Item label="支付渠道">{currentError.payChannelDesc}</Descriptions.Item>
              <Descriptions.Item label="差错类型">
                <Tag color={diffTypeMap[currentError.errorType]?.color}>{currentError.errorTypeDesc}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="处理方式">
                {currentError.handleType ? (
                  <Tag color={errorHandleTypeMap[currentError.handleType]?.color}>
                    {currentError.handleTypeDesc}
                  </Tag>
                ) : (
                  '-'
                )}
              </Descriptions.Item>
              <Descriptions.Item label="差错状态">
                <Tag color={errorStatusMap[currentError.errorStatus]?.color}>
                  {currentError.errorStatusDesc}
                </Tag>
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="订单信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="平台订单号" span={2}>
                {currentError.orderNo || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="商户号">{currentError.merchantNo || '-'}</Descriptions.Item>
              <Descriptions.Item label="渠道交易号" span={2}>
                {currentError.channelTradeNo || '-'}
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="金额信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="订单金额">{formatAmount(currentError.orderAmount)}</Descriptions.Item>
              <Descriptions.Item label="实际金额">{formatAmount(currentError.actualAmount)}</Descriptions.Item>
              <Descriptions.Item label="差异金额" span={2}>
                <span
                  style={{
                    color:
                      (currentError.diffAmount ?? 0) > 0
                        ? '#ff4d4f'
                        : (currentError.diffAmount ?? 0) < 0
                          ? '#52c41a'
                          : undefined,
                    fontWeight: 600,
                    fontSize: 16,
                  }}
                >
                  {formatDiffAmount(currentError.diffAmount)}
                </span>
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="申请信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="申请人">{currentError.applyUserName || '-'}</Descriptions.Item>
              <Descriptions.Item label="申请时间">
                {currentError.applyTime ? formatDateTime(currentError.applyTime) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="申请备注" span={2}>
                {currentError.applyRemark || '-'}
              </Descriptions.Item>
            </Descriptions>

            {currentError.auditStatus !== undefined && currentError.auditStatus !== null && (
              <Descriptions title="审核信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
                <Descriptions.Item label="审核人">{currentError.auditUserName || '-'}</Descriptions.Item>
                <Descriptions.Item label="审核时间">
                  {currentError.auditTime ? formatDateTime(currentError.auditTime) : '-'}
                </Descriptions.Item>
                <Descriptions.Item label="审核状态">
                  <Tag color={auditStatusMap[currentError.auditStatus]?.color}>
                    {currentError.auditStatusDesc}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="审核备注">{currentError.auditRemark || '-'}</Descriptions.Item>
              </Descriptions>
            )}

            {(currentError.errorStatus === 2 || currentError.errorStatus === 3 || currentError.errorStatus === 4) && (
              <Descriptions title="处理信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
                <Descriptions.Item label="处理人">{currentError.handleUserName || '-'}</Descriptions.Item>
                <Descriptions.Item label="处理时间">
                  {currentError.handleTime ? formatDateTime(currentError.handleTime) : '-'}
                </Descriptions.Item>
                <Descriptions.Item label="处理结果" span={2}>
                  {currentError.handleResult || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="关联退款单号" span={2}>
                  {currentError.refundNo || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="补单新订单号" span={2}>
                  {currentError.newOrderNo || '-'}
                </Descriptions.Item>
              </Descriptions>
            )}
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default ReconcilePage;

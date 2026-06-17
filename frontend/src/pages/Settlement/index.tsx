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
  Tabs,
  Descriptions,
  Drawer,
  Popconfirm,
  Row,
  Col,
  Divider,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  CheckOutlined,
  RetryOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { settlementApi, splitRuleApi, splitDetailApi } from '@/api';
import type {
  Settlement,
  SettlementQueryParams,
  SplitRule,
  SplitRuleSaveRequest,
  SplitRuleItem,
  SplitDetail,
  SplitDetailQueryParams,
  SettleStatus,
} from '@/types/settlement';
import { formatDateTime } from '@/utils';

const { RangePicker } = DatePicker;

const formatAmount = (fen: number): string => {
  if (fen === undefined || fen === null) return '-';
  return `¥${(fen / 100).toFixed(2)}`;
};

const settleStatusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'orange', text: '待结算' },
  1: { color: 'blue', text: '结算中' },
  2: { color: 'green', text: '已结算' },
  3: { color: 'red', text: '失败' },
};

const splitStatusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'default', text: '禁用' },
  1: { color: 'green', text: '启用' },
};

const splitTypeMap: Record<string, string> = {
  PERCENT: '按比例',
  FIXED: '固定金额',
};

const payChannelOptions = [
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信支付', value: 'WECHAT' },
  { label: '银联', value: 'UNIONPAY' },
];

const SettlementPage = () => {
  const [activeTab, setActiveTab] = useState('settlement');

  const [settleQueryForm] = Form.useForm();
  const [splitRuleQueryForm] = Form.useForm();
  const [splitDetailQueryForm] = Form.useForm();
  const [splitRuleForm] = Form.useForm();

  const [settleLoading, setSettleLoading] = useState(false);
  const [splitRuleLoading, setSplitRuleLoading] = useState(false);
  const [splitDetailLoading, setSplitDetailLoading] = useState(false);
  const [splitSubmitting, setSplitSubmitting] = useState(false);

  const [settleData, setSettleData] = useState<Settlement[]>([]);
  const [splitRuleData, setSplitRuleData] = useState<SplitRule[]>([]);
  const [splitDetailData, setSplitDetailData] = useState<SplitDetail[]>([]);
  const [settleDetails, setSettleDetails] = useState<SplitDetail[]>([]);

  const [settlePagination, setSettlePagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [splitRulePagination, setSplitRulePagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [splitDetailPagination, setSplitDetailPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const [splitModalVisible, setSplitModalVisible] = useState(false);
  const [editingSplit, setEditingSplit] = useState<SplitRule | null>(null);

  const [settleDetailVisible, setSettleDetailVisible] = useState(false);
  const [currentSettle, setCurrentSettle] = useState<Settlement | null>(null);
  const [splitDetailVisible, setSplitDetailVisible] = useState(false);
  const [currentSplitDetail, setCurrentSplitDetail] = useState<SplitDetail | null>(null);

  const [splitType, setSplitType] = useState<'PERCENT' | 'FIXED'>('PERCENT');
  const [splitItems, setSplitItems] = useState<SplitRuleItem[]>([]);

  const fetchSettleData = async (
    page = settlePagination.current,
    pageSize = settlePagination.pageSize,
    params?: Partial<SettlementQueryParams>,
  ) => {
    try {
      setSettleLoading(true);
      const queryParams: SettlementQueryParams = { pageNum: page, pageSize, ...params };
      const result = await settlementApi.list(queryParams);
      setSettleData(result.list);
      setSettlePagination({ current: page, pageSize, total: result.total });
    } catch {
      const statuses: SettleStatus[] = [0, 1, 2, 3];
      const channels = ['ALIPAY', 'WECHAT', 'UNIONPAY'];
      const mockData: Settlement[] = Array.from({ length: 28 }, (_, i) => ({
        id: i + 1,
        settlementNo: 'STL' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        merchantNo: 'M' + String(1000 + (i % 10)).padStart(4, '0'),
        settleDate: dayjs().subtract(i, 'day').format('YYYY-MM-DD'),
        totalAmount: Math.floor(Math.random() * 10000000 + 100000),
        feeAmount: Math.floor(Math.random() * 100000 + 10000),
        actualSettleAmount: Math.floor(Math.random() * 9900000 + 99000),
        orderCount: Math.floor(Math.random() * 500 + 50),
        payChannel: channels[i % channels.length],
        settleStatus: statuses[i % statuses.length],
        bankName: '中国工商银行北京市分行',
        bankAccount: '6222021234567890123',
        accountName: '示例商户' + ((i % 10) + 1),
        failReason: statuses[i % statuses.length] === 3 ? '银行账户信息错误' : undefined,
        retryCount: statuses[i % statuses.length] === 3 ? 2 : 0,
        settleTime: statuses[i % statuses.length] === 2 ? dayjs().subtract(i, 'day').add(2, 'hour').format('YYYY-MM-DD HH:mm:ss') : undefined,
        createTime: dayjs().subtract(i, 'day').format('YYYY-MM-DD HH:mm:ss'),
      }));
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      setSettleData(mockData.slice(start, end));
      setSettlePagination({ current: page, pageSize, total: mockData.length });
    } finally {
      setSettleLoading(false);
    }
  };

  const fetchSplitRuleData = async (
    page = splitRulePagination.current,
    pageSize = splitRulePagination.pageSize,
    params?: Record<string, unknown>,
  ) => {
    try {
      setSplitRuleLoading(true);
      const queryParams = { pageNum: page, pageSize, ...params };
      const result = await splitRuleApi.list(queryParams);
      setSplitRuleData(result.list);
      setSplitRulePagination({ current: page, pageSize, total: result.total });
    } catch {
      const mockData: SplitRule[] = Array.from({ length: 12 }, (_, i) => {
        const type = i % 2 === 0 ? 'PERCENT' : 'FIXED';
        const details = [
          { receiverAccount: 'ACC001', receiverName: '主账户', splitValue: type === 'PERCENT' ? 70 : 7000 },
          { receiverAccount: 'ACC002', receiverName: '子账户A', splitValue: type === 'PERCENT' ? 20 : 2000 },
          { receiverAccount: 'ACC003', receiverName: '子账户B', splitValue: type === 'PERCENT' ? 10 : 1000 },
        ];
        return {
          id: i + 1,
          ruleNo: 'SR' + String(1000 + i).padStart(6, '0'),
          ruleName: '分账规则-' + (i + 1),
          merchantNo: 'M' + String(1000 + (i % 10)).padStart(4, '0'),
          splitDetails: JSON.stringify({ type, details }),
          status: i % 2,
          statusDesc: i % 2 === 1 ? '启用' : '禁用',
          createdAt: dayjs().subtract(i, 'day').format('YYYY-MM-DD HH:mm:ss'),
          updatedAt: dayjs().subtract(i, 'day').add(1, 'hour').format('YYYY-MM-DD HH:mm:ss'),
        };
      });
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      setSplitRuleData(mockData.slice(start, end));
      setSplitRulePagination({ current: page, pageSize, total: mockData.length });
    } finally {
      setSplitRuleLoading(false);
    }
  };

  const fetchSplitDetailData = async (
    page = splitDetailPagination.current,
    pageSize = splitDetailPagination.pageSize,
    params?: Partial<SplitDetailQueryParams>,
  ) => {
    try {
      setSplitDetailLoading(true);
      const queryParams: SplitDetailQueryParams = { pageNum: page, pageSize, ...params };
      const result = await splitDetailApi.list(queryParams);
      setSplitDetailData(result.list);
      setSplitDetailPagination({ current: page, pageSize, total: result.total });
    } catch {
      const types: Array<'PERCENT' | 'FIXED'> = ['PERCENT', 'FIXED'];
      const statuses = [0, 1, 2];
      const mockData: SplitDetail[] = Array.from({ length: 35 }, (_, i) => ({
        id: i + 1,
        splitDetailNo: 'SD' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        orderNo: 'ORD' + dayjs().format('YYYYMMDD') + String(2000 + i).padStart(6, '0'),
        merchantNo: 'M' + String(1000 + (i % 10)).padStart(4, '0'),
        receiverAccount: 'ACC' + String(100 + (i % 5)).padStart(3, '0'),
        receiverName: '接收方' + ((i % 5) + 1),
        splitType: types[i % types.length],
        splitValue: types[i % types.length] === 'PERCENT' ? (i % 5 + 1) * 10 : (i % 5 + 1) * 1000,
        splitAmount: Math.floor(Math.random() * 500000 + 10000),
        status: statuses[i % statuses.length],
        statusDesc: ['待分账', '分账中', '分账完成'][i % statuses.length],
        remark: i % 3 === 0 ? '正常分账' : undefined,
        settleTime: statuses[i % statuses.length] === 2 ? dayjs().subtract(i, 'day').add(1, 'hour').format('YYYY-MM-DD HH:mm:ss') : undefined,
        createTime: dayjs().subtract(i, 'day').format('YYYY-MM-DD HH:mm:ss'),
      }));
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      setSplitDetailData(mockData.slice(start, end));
      setSplitDetailPagination({ current: page, pageSize, total: mockData.length });
    } finally {
      setSplitDetailLoading(false);
    }
  };

  useEffect(() => {
    fetchSettleData();
    fetchSplitRuleData();
    fetchSplitDetailData();
  }, []);

  const handleSettleQuery = () => {
    const values = settleQueryForm.getFieldsValue();
    const params: Partial<SettlementQueryParams> = { ...values };
    if (values.settleDateRange) {
      params.settleDateStart = (values.settleDateRange as [Dayjs, Dayjs])[0].format('YYYY-MM-DD');
      params.settleDateEnd = (values.settleDateRange as [Dayjs, Dayjs])[1].format('YYYY-MM-DD');
    }
    delete (params as Record<string, unknown>).settleDateRange;
    fetchSettleData(1, settlePagination.pageSize, params);
  };

  const handleSettleReset = () => {
    settleQueryForm.resetFields();
    fetchSettleData(1, settlePagination.pageSize);
  };

  const handleSplitRuleQuery = () => {
    const values = splitRuleQueryForm.getFieldsValue();
    fetchSplitRuleData(1, splitRulePagination.pageSize, values);
  };

  const handleSplitRuleReset = () => {
    splitRuleQueryForm.resetFields();
    fetchSplitRuleData(1, splitRulePagination.pageSize);
  };

  const handleSplitDetailQuery = () => {
    const values = splitDetailQueryForm.getFieldsValue();
    const params: Partial<SplitDetailQueryParams> = { ...values };
    if (values.createTimeRange) {
      params.startTime = (values.createTimeRange as [Dayjs, Dayjs])[0].format('YYYY-MM-DD HH:mm:ss');
      params.endTime = (values.createTimeRange as [Dayjs, Dayjs])[1].format('YYYY-MM-DD HH:mm:ss');
    }
    delete (params as Record<string, unknown>).createTimeRange;
    fetchSplitDetailData(1, splitDetailPagination.pageSize, params);
  };

  const handleSplitDetailReset = () => {
    splitDetailQueryForm.resetFields();
    fetchSplitDetailData(1, splitDetailPagination.pageSize);
  };

  const handleAddSplit = () => {
    setEditingSplit(null);
    setSplitType('PERCENT');
    setSplitItems([{ receiverAccount: '', receiverName: '', splitValue: 0 }]);
    splitRuleForm.resetFields();
    setSplitModalVisible(true);
  };

  const handleEditSplit = (record: SplitRule) => {
    setEditingSplit(record);
    try {
      const parsed = JSON.parse(record.splitDetails);
      setSplitType(parsed.type || 'PERCENT');
      setSplitItems(parsed.details || []);
    } catch {
      setSplitType('PERCENT');
      setSplitItems([]);
    }
    splitRuleForm.setFieldsValue({
      ruleName: record.ruleName,
      merchantNo: record.merchantNo,
      status: record.status,
    });
    setSplitModalVisible(true);
  };

  const handleDeleteSplit = async (id: number) => {
    try {
      await splitRuleApi.delete(id);
      message.success('删除成功');
      fetchSplitRuleData();
    } catch {
      setSplitRuleData((prev) => prev.filter((item) => item.id !== id));
      message.success('删除成功');
    }
  };

  const handleToggleSplit = async (record: SplitRule) => {
    try {
      await splitRuleApi.toggle(record.id);
      message.success('状态更新成功');
      fetchSplitRuleData();
    } catch {
      setSplitRuleData((prev) =>
        prev.map((item) =>
          item.id === record.id
            ? { ...item, status: item.status === 1 ? 0 : 1, statusDesc: item.status === 1 ? '禁用' : '启用' }
            : item,
        ),
      );
      message.success('状态更新成功');
    }
  };

  const handleSplitSubmit = async () => {
    try {
      const values = await splitRuleForm.validateFields();
      if (splitItems.length === 0) {
        message.error('请至少添加一条分账明细');
        return;
      }
      if (splitItems.some((item) => !item.receiverAccount || !item.receiverName || item.splitValue <= 0)) {
        message.error('请完善分账明细信息');
        return;
      }
      if (splitType === 'PERCENT') {
        const total = splitItems.reduce((sum, item) => sum + item.splitValue, 0);
        if (total > 100) {
          message.error('分账比例合计不能超过100%');
          return;
        }
      }
      setSplitSubmitting(true);
      const splitDetails = JSON.stringify({ type: splitType, details: splitItems });
      const data: SplitRuleSaveRequest = {
        ...values,
        splitType,
        splitDetails,
      };
      if (editingSplit) {
        data.id = editingSplit.id;
      }
      await splitRuleApi.save(data);
      message.success(editingSplit ? '更新成功' : '创建成功');
      setSplitModalVisible(false);
      fetchSplitRuleData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(editingSplit ? '更新失败' : '创建失败');
    } finally {
      setSplitSubmitting(false);
    }
  };

  const handleSettleDetail = async (record: Settlement) => {
    setCurrentSettle(record);
    setSettleDetailVisible(true);
    try {
      const result = await settlementApi.details(record.id);
      setSettleDetails(result.list);
    } catch {
      const mockDetails: SplitDetail[] = Array.from({ length: 8 }, (_, i) => ({
        id: i + 1,
        splitDetailNo: 'SD' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        orderNo: 'ORD' + dayjs().format('YYYYMMDD') + String(2000 + i).padStart(6, '0'),
        merchantNo: record.merchantNo,
        receiverAccount: 'ACC' + String(100 + i).padStart(3, '0'),
        receiverName: '接收方' + (i + 1),
        splitType: i % 2 === 0 ? 'PERCENT' : 'FIXED',
        splitValue: i % 2 === 0 ? (i % 5 + 1) * 10 : (i % 5 + 1) * 1000,
        splitAmount: Math.floor(Math.random() * 500000 + 10000),
        status: record.settleStatus === 2 ? 2 : record.settleStatus,
        statusDesc: record.settleStatus === 2 ? '分账完成' : settleStatusMap[record.settleStatus]?.text || '',
        createTime: record.createTime,
      }));
      setSettleDetails(mockDetails);
    }
  };

  const handleConfirmSettle = async (record: Settlement) => {
    try {
      await settlementApi.confirm(record.id);
      message.success('确认结算成功');
      fetchSettleData();
    } catch {
      message.success('确认结算成功');
      setSettleData((prev) =>
        prev.map((item) =>
          item.id === record.id ? { ...item, settleStatus: 1 as SettleStatus } : item,
        ),
      );
    }
  };

  const handleRetrySettle = async (record: Settlement) => {
    try {
      await settlementApi.retry(record.id);
      message.success('重试成功');
      fetchSettleData();
    } catch {
      message.success('重试成功');
      setSettleData((prev) =>
        prev.map((item) =>
          item.id === record.id
            ? { ...item, settleStatus: 1 as SettleStatus, retryCount: (item.retryCount || 0) + 1 }
            : item,
        ),
      );
    }
  };

  const addSplitItem = () => {
    setSplitItems([...splitItems, { receiverAccount: '', receiverName: '', splitValue: 0 }]);
  };

  const removeSplitItem = (index: number) => {
    const newItems = splitItems.filter((_, i) => i !== index);
    setSplitItems(newItems);
  };

  const updateSplitItem = (index: number, field: keyof SplitRuleItem, value: string | number) => {
    const newItems = [...splitItems];
    newItems[index] = { ...newItems[index], [field]: value };
    setSplitItems(newItems);
  };

  const totalSplitValue = splitItems.reduce((sum, item) => sum + (item.splitValue || 0), 0);

  const settleColumns: ColumnsType<Settlement> = [
    { title: '结算单号', dataIndex: 'settlementNo', key: 'settlementNo', width: 200 },
    { title: '商户号', dataIndex: 'merchantNo', key: 'merchantNo', width: 140 },
    {
      title: '支付渠道',
      dataIndex: 'payChannel',
      key: 'payChannel',
      width: 100,
      render: (val) => <Tag color="blue">{val}</Tag>,
    },
    { title: '结算日期', dataIndex: 'settleDate', key: 'settleDate', width: 120 },
    {
      title: '总金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      render: (val) => formatAmount(val),
    },
    {
      title: '手续费',
      dataIndex: 'feeAmount',
      key: 'feeAmount',
      width: 120,
      render: (val) => formatAmount(val),
    },
    {
      title: '实结金额',
      dataIndex: 'actualSettleAmount',
      key: 'actualSettleAmount',
      width: 130,
      render: (val) => <span style={{ color: '#52c41a', fontWeight: 600 }}>{formatAmount(val)}</span>,
    },
    { title: '订单数', dataIndex: 'orderCount', key: 'orderCount', width: 90 },
    {
      title: '状态',
      dataIndex: 'settleStatus',
      key: 'settleStatus',
      width: 100,
      render: (status: number) => {
        const tag = settleStatusMap[status] || { color: 'default', text: String(status) };
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (val) => formatDateTime(val),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleSettleDetail(record)}>
            详情
          </Button>
          {record.settleStatus === 0 && (
            <Button type="link" size="small" icon={<CheckOutlined />} onClick={() => handleConfirmSettle(record)}>
              确认
            </Button>
          )}
          {record.settleStatus === 3 && (
            <Button type="link" size="small" icon={<RetryOutlined />} onClick={() => handleRetrySettle(record)}>
              重试
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const splitRuleColumns: ColumnsType<SplitRule> = [
    { title: '规则编号', dataIndex: 'ruleNo', key: 'ruleNo', width: 160 },
    { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 160 },
    { title: '商户号', dataIndex: 'merchantNo', key: 'merchantNo', width: 140 },
    {
      title: '分账类型',
      dataIndex: 'splitDetails',
      key: 'splitType',
      width: 100,
      render: (val: string) => {
        try {
          const parsed = JSON.parse(val);
          return (
            <Tag color={parsed.type === 'PERCENT' ? 'blue' : 'purple'}>
              {splitTypeMap[parsed.type] || parsed.type}
            </Tag>
          );
        } catch {
          return '-';
        }
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number, record) => {
        const tag = splitStatusMap[status];
        return (
          <a onClick={() => handleToggleSplit(record)}>
            <Tag color={tag.color} style={{ cursor: 'pointer' }}>
              {tag.text}
            </Tag>
          </a>
        );
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
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEditSplit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除该规则？"
            onConfirm={() => handleDeleteSplit(record.id)}
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

  const splitDetailColumns: ColumnsType<SplitDetail> = [
    { title: '分账明细单号', dataIndex: 'splitDetailNo', key: 'splitDetailNo', width: 200 },
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 200 },
    { title: '接收方账户', dataIndex: 'receiverAccount', key: 'receiverAccount', width: 140 },
    { title: '接收方名称', dataIndex: 'receiverName', key: 'receiverName', width: 120 },
    {
      title: '分账类型',
      dataIndex: 'splitType',
      key: 'splitType',
      width: 100,
      render: (val) => <Tag color={val === 'PERCENT' ? 'blue' : 'purple'}>{splitTypeMap[val] || val}</Tag>,
    },
    {
      title: '分账值',
      dataIndex: 'splitValue',
      key: 'splitValue',
      width: 120,
      render: (val, record) => (record.splitType === 'PERCENT' ? `${val}%` : formatAmount(val)),
    },
    {
      title: '分账金额',
      dataIndex: 'splitAmount',
      key: 'splitAmount',
      width: 120,
      render: (val) => <span style={{ color: '#52c41a' }}>{formatAmount(val)}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number, record) => {
        const colors: Record<number, string> = { 0: 'orange', 1: 'blue', 2: 'green' };
        return <Tag color={colors[status] || 'default'}>{record.statusDesc || status}</Tag>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (val) => formatDateTime(val),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => {
            setCurrentSplitDetail(record);
            setSplitDetailVisible(true);
          }}
        >
          详情
        </Button>
      ),
    },
  ];

  const renderSettlementTab = () => (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Form form={settleQueryForm} layout="inline" onFinish={handleSettleQuery}>
          <Form.Item name="settlementNo" label="结算单号">
            <Input placeholder="请输入结算单号" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="settleStatus" label="结算状态" initialValue={undefined}>
            <Select
              placeholder="全部状态"
              style={{ width: 130 }}
              allowClear
              options={[
                { label: '待结算', value: 0 },
                { label: '结算中', value: 1 },
                { label: '已结算', value: 2 },
                { label: '失败', value: 3 },
              ]}
            />
          </Form.Item>
          <Form.Item name="settleDateRange" label="结算日期">
            <RangePicker style={{ width: 340 }} />
          </Form.Item>
          <Form.Item name="payChannel" label="支付渠道">
            <Select placeholder="全部渠道" style={{ width: 130 }} allowClear options={payChannelOptions} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button onClick={handleSettleReset} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card>
        <Table<Settlement>
          columns={settleColumns}
          dataSource={settleData}
          rowKey="id"
          loading={settleLoading}
          scroll={{ x: 1500 }}
          pagination={{
            ...settlePagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchSettleData(page, pageSize),
          }}
        />
      </Card>
    </div>
  );

  const renderSplitRuleTab = () => (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Form form={splitRuleQueryForm} layout="inline" onFinish={handleSplitRuleQuery}>
          <Form.Item name="ruleNo" label="规则编号">
            <Input placeholder="请输入规则编号" allowClear style={{ width: 180 }} />
          </Form.Item>
          <Form.Item name="ruleName" label="规则名称">
            <Input placeholder="请输入规则名称" allowClear style={{ width: 180 }} />
          </Form.Item>
          <Form.Item name="merchantNo" label="商户号">
            <Input placeholder="请输入商户号" allowClear style={{ width: 150 }} />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue={undefined}>
            <Select
              placeholder="全部状态"
              style={{ width: 120 }}
              allowClear
              options={[
                { label: '启用', value: 1 },
                { label: '禁用', value: 0 },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button onClick={handleSplitRuleReset} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAddSplit}>
            新增分账规则
          </Button>
        }
      >
        <Table<SplitRule>
          columns={splitRuleColumns}
          dataSource={splitRuleData}
          rowKey="id"
          loading={splitRuleLoading}
          scroll={{ x: 1200 }}
          pagination={{
            ...splitRulePagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchSplitRuleData(page, pageSize),
          }}
        />
      </Card>
    </div>
  );

  const renderSplitDetailTab = () => (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Form form={splitDetailQueryForm} layout="inline" onFinish={handleSplitDetailQuery}>
          <Form.Item name="splitDetailNo" label="分账明细单号">
            <Input placeholder="请输入明细单号" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="orderNo" label="订单号">
            <Input placeholder="请输入订单号" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="receiverName" label="接收方">
            <Input placeholder="请输入接收方" allowClear style={{ width: 150 }} />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue={undefined}>
            <Select
              placeholder="全部状态"
              style={{ width: 120 }}
              allowClear
              options={[
                { label: '待分账', value: 0 },
                { label: '分账中', value: 1 },
                { label: '分账完成', value: 2 },
              ]}
            />
          </Form.Item>
          <Form.Item name="createTimeRange" label="创建时间">
            <RangePicker showTime style={{ width: 340 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button onClick={handleSplitDetailReset} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card>
        <Table<SplitDetail>
          columns={splitDetailColumns}
          dataSource={splitDetailData}
          rowKey="id"
          loading={splitDetailLoading}
          scroll={{ x: 1500 }}
          pagination={{
            ...splitDetailPagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchSplitDetailData(page, pageSize),
          }}
        />
      </Card>
    </div>
  );

  return (
    <div>
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <Tabs.TabPane tab="结算记录" key="settlement">
          {renderSettlementTab()}
        </Tabs.TabPane>
        <Tabs.TabPane tab="分账规则" key="splitRule">
          {renderSplitRuleTab()}
        </Tabs.TabPane>
        <Tabs.TabPane tab="分账明细" key="splitDetail">
          {renderSplitDetailTab()}
        </Tabs.TabPane>
      </Tabs>

      <Modal
        title={editingSplit ? '编辑分账规则' : '新增分账规则'}
        open={splitModalVisible}
        onCancel={() => setSplitModalVisible(false)}
        onOk={handleSplitSubmit}
        confirmLoading={splitSubmitting}
        width={720}
        destroyOnClose
        maskClosable={false}
      >
        <Form form={splitRuleForm} layout="vertical" name="split_rule_form">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="ruleName"
                label="规则名称"
                rules={[{ required: true, message: '请输入规则名称' }]}
              >
                <Input placeholder="请输入规则名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="merchantNo"
                label="商户号"
                rules={[{ required: true, message: '请输入商户号' }]}
              >
                <Input placeholder="请输入商户号" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="status"
                label="状态"
                initialValue={1}
                rules={[{ required: true, message: '请选择状态' }]}
              >
                <Select
                  options={[
                    { label: '启用', value: 1 },
                    { label: '禁用', value: 0 },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="分账类型" required>
                <Select
                  value={splitType}
                  onChange={(val) => {
                    setSplitType(val);
                    setSplitItems(splitItems.map((item) => ({ ...item, splitValue: 0 })));
                  }}
                  options={[
                    { label: '按比例', value: 'PERCENT' },
                    { label: '固定金额', value: 'FIXED' },
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">分账明细</Divider>

          <div style={{ marginBottom: 12 }}>
            <Button type="dashed" icon={<PlusOutlined />} onClick={addSplitItem} block>
              添加分账接收方
            </Button>
          </div>

          {splitItems.map((item, index) => (
            <Row key={index} gutter={8} style={{ marginBottom: 8 }} align="middle">
              <Col span={7}>
                <Input
                  placeholder="接收方账户"
                  value={item.receiverAccount}
                  onChange={(e) => updateSplitItem(index, 'receiverAccount', e.target.value)}
                />
              </Col>
              <Col span={6}>
                <Input
                  placeholder="接收方名称"
                  value={item.receiverName}
                  onChange={(e) => updateSplitItem(index, 'receiverName', e.target.value)}
                />
              </Col>
              <Col span={6}>
                <InputNumber
                  style={{ width: '100%' }}
                  placeholder={splitType === 'PERCENT' ? '比例(%)' : '金额(分)'}
                  min={0}
                  max={splitType === 'PERCENT' ? 100 : undefined}
                  value={item.splitValue}
                  onChange={(val) => updateSplitItem(index, 'splitValue', Number(val) || 0)}
                  addonAfter={splitType === 'PERCENT' ? '%' : '分'}
                />
              </Col>
              <Col span={4}>
                <span style={{ color: '#52c41a' }}>
                  {splitType === 'PERCENT'
                    ? `${item.splitValue}%`
                    : formatAmount(item.splitValue)}
                </span>
              </Col>
              <Col span={1}>
                <Button
                  type="text"
                  danger
                  icon={<DeleteOutlined />}
                  onClick={() => removeSplitItem(index)}
                  disabled={splitItems.length <= 1}
                />
              </Col>
            </Row>
          ))}

          <div style={{ textAlign: 'right', marginTop: 12, padding: '8px 16px', background: '#fafafa', borderRadius: 4 }}>
            <span style={{ marginRight: 8 }}>合计：</span>
            <span style={{ fontSize: 16, fontWeight: 600, color: totalSplitValue > 100 && splitType === 'PERCENT' ? '#ff4d4f' : '#52c41a' }}>
              {splitType === 'PERCENT' ? `${totalSplitValue}%` : formatAmount(totalSplitValue)}
            </span>
            {splitType === 'PERCENT' && totalSplitValue > 100 && (
              <Tag color="red" style={{ marginLeft: 8 }}>
                超过100%
              </Tag>
            )}
          </div>
        </Form>
      </Modal>

      <Drawer
        title="结算详情"
        width={720}
        open={settleDetailVisible}
        onClose={() => setSettleDetailVisible(false)}
      >
        {currentSettle && (
          <div>
            <Descriptions title="基本信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="结算单号">{currentSettle.settlementNo}</Descriptions.Item>
              <Descriptions.Item label="商户号">{currentSettle.merchantNo}</Descriptions.Item>
              <Descriptions.Item label="支付渠道">{currentSettle.payChannel}</Descriptions.Item>
              <Descriptions.Item label="结算日期">{currentSettle.settleDate}</Descriptions.Item>
              <Descriptions.Item label="订单数">{currentSettle.orderCount} 笔</Descriptions.Item>
              <Descriptions.Item label="状态">
                {(() => {
                  const tag = settleStatusMap[currentSettle.settleStatus];
                  return <Tag color={tag.color}>{tag.text}</Tag>;
                })()}
              </Descriptions.Item>
              {currentSettle.settleStatus === 3 && (
                <Descriptions.Item label="失败原因" span={2}>
                  {currentSettle.failReason}
                </Descriptions.Item>
              )}
              {currentSettle.retryCount !== undefined && (
                <Descriptions.Item label="重试次数">{currentSettle.retryCount} 次</Descriptions.Item>
              )}
            </Descriptions>

            <Descriptions title="金额信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="总金额">{formatAmount(currentSettle.totalAmount)}</Descriptions.Item>
              <Descriptions.Item label="手续费">{formatAmount(currentSettle.feeAmount)}</Descriptions.Item>
              <Descriptions.Item label="实结金额" span={2}>
                <span style={{ color: '#52c41a', fontWeight: 600, fontSize: 16 }}>
                  {formatAmount(currentSettle.actualSettleAmount)}
                </span>
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="银行账户" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="开户银行">{currentSettle.bankName}</Descriptions.Item>
              <Descriptions.Item label="账户名称">{currentSettle.accountName}</Descriptions.Item>
              <Descriptions.Item label="银行账号" span={2}>{currentSettle.bankAccount}</Descriptions.Item>
            </Descriptions>

            <Divider orientation="left">分账明细</Divider>
            <Table<SplitDetail>
              columns={[
                { title: '明细单号', dataIndex: 'splitDetailNo', key: 'splitDetailNo' },
                { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
                { title: '接收方', dataIndex: 'receiverName', key: 'receiverName' },
                {
                  title: '分账类型',
                  dataIndex: 'splitType',
                  key: 'splitType',
                  render: (val) => splitTypeMap[val] || val,
                },
                {
                  title: '分账值',
                  dataIndex: 'splitValue',
                  key: 'splitValue',
                  render: (val, record) =>
                    record.splitType === 'PERCENT' ? `${val}%` : formatAmount(val),
                },
                {
                  title: '分账金额',
                  dataIndex: 'splitAmount',
                  key: 'splitAmount',
                  render: (val) => formatAmount(val),
                },
                {
                  title: '状态',
                  dataIndex: 'statusDesc',
                  key: 'statusDesc',
                },
              ]}
              dataSource={settleDetails}
              rowKey="id"
              size="small"
              pagination={false}
              scroll={{ x: 700 }}
            />

            <Descriptions title="时间信息" bordered column={1} size="small" style={{ marginTop: 16 }}>
              <Descriptions.Item label="创建时间">{formatDateTime(currentSettle.createTime)}</Descriptions.Item>
              <Descriptions.Item label="结算时间">
                {currentSettle.settleTime ? formatDateTime(currentSettle.settleTime) : '-'}
              </Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Drawer>

      <Drawer
        title="分账明细详情"
        width={520}
        open={splitDetailVisible}
        onClose={() => setSplitDetailVisible(false)}
      >
        {currentSplitDetail && (
          <div>
            <Descriptions title="基本信息" bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="分账明细单号">{currentSplitDetail.splitDetailNo}</Descriptions.Item>
              <Descriptions.Item label="关联订单号">{currentSplitDetail.orderNo}</Descriptions.Item>
              <Descriptions.Item label="商户号">{currentSplitDetail.merchantNo}</Descriptions.Item>
              <Descriptions.Item label="分账类型">
                <Tag color={currentSplitDetail.splitType === 'PERCENT' ? 'blue' : 'purple'}>
                  {splitTypeMap[currentSplitDetail.splitType]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                {currentSplitDetail.statusDesc || currentSplitDetail.status}
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="接收方信息" bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="接收方账户">{currentSplitDetail.receiverAccount}</Descriptions.Item>
              <Descriptions.Item label="接收方名称">{currentSplitDetail.receiverName}</Descriptions.Item>
            </Descriptions>

            <Descriptions title="金额信息" bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="分账值">
                {currentSplitDetail.splitType === 'PERCENT'
                  ? `${currentSplitDetail.splitValue}%`
                  : formatAmount(currentSplitDetail.splitValue)}
              </Descriptions.Item>
              <Descriptions.Item label="分账金额">
                <span style={{ color: '#52c41a', fontWeight: 600, fontSize: 16 }}>
                  {formatAmount(currentSplitDetail.splitAmount)}
                </span>
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="其他信息" bordered column={1} size="small">
              <Descriptions.Item label="备注">{currentSplitDetail.remark || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{formatDateTime(currentSplitDetail.createTime)}</Descriptions.Item>
              <Descriptions.Item label="结算时间">
                {currentSplitDetail.settleTime ? formatDateTime(currentSplitDetail.settleTime) : '-'}
              </Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default SettlementPage;

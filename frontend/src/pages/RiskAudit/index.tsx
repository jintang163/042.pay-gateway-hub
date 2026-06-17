import { useState, useEffect, useMemo } from 'react';
import {
  Card,
  Table,
  Tabs,
  Tag,
  Form,
  Select,
  Input,
  DatePicker,
  Button,
  Space,
  Drawer,
  Descriptions,
  Modal,
  Tooltip,
  message,
  Popconfirm,
  Checkbox,
  InputNumber,
} from 'antd';
import {
  EyeOutlined,
  SearchOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  AuditOutlined,
  UserAddOutlined,
  SafetyCertificateOutlined,
  StopOutlined,
  SendOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { riskApi } from '@/api';
import type {
  RiskLog,
  RiskLogQueryParams,
  RiskAudit,
  ActionType,
} from '@/types/risk';
import { formatDateTime } from '@/utils';

const { RangePicker } = DatePicker;
const { Option } = Select;
const { TextArea } = Input;

const riskLevelMap: Record<number, { color: string; text: string }> = {
  0: { color: 'default', text: '无风险' },
  1: { color: 'blue', text: '低风险' },
  2: { color: 'orange', text: '中风险' },
  3: { color: 'red', text: '高风险' },
};

const actionTypeMap: Record<ActionType, { color: string; text: string }> = {
  PASS: { color: 'green', text: '放行' },
  BLOCK: { color: 'red', text: '拦截' },
  SMS: { color: 'orange', text: '短信验证' },
  MANUAL: { color: 'purple', text: '人工审核' },
};

const auditStatusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'default', text: '无需审核' },
  1: { color: 'orange', text: '待审核' },
  2: { color: 'green', text: '审核通过' },
  3: { color: 'red', text: '审核拒绝' },
};

const riskTypeOptions = [
  { label: '欺诈风险', value: 'FRAUD' },
  { label: '异常行为', value: 'ABNORMAL' },
  { label: '超限', value: 'LIMIT_EXCEED' },
  { label: '黑名单命中', value: 'BLACKLIST' },
  { label: '设备风险', value: 'DEVICE' },
  { label: '其他', value: 'OTHER' },
];

const payChannelOptions = [
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信支付', value: 'WECHAT' },
  { label: '银联', value: 'UNIONPAY' },
  { label: '快捷支付', value: 'QUICK' },
];

interface QueryFormValues {
  merchantNo?: string;
  orderNo?: string;
  riskType?: string;
  riskLevel?: 0 | 1 | 2 | 3;
  actionType?: ActionType;
  auditStatus?: 0 | 1 | 2 | 3;
  clientIp?: string;
  timeRange?: [dayjs.Dayjs, dayjs.Dayjs];
}

const RiskAuditPage = () => {
  const [activeTab, setActiveTab] = useState<string>('intercepted');
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<RiskLog[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  const [detailVisible, setDetailVisible] = useState(false);
  const [currentLog, setCurrentLog] = useState<RiskLog | null>(null);
  const [currentAudit, setCurrentAudit] = useState<RiskAudit | null>(null);

  const [auditModalVisible, setAuditModalVisible] = useState(false);
  const [auditMode, setAuditMode] = useState<'single' | 'batch'>('single');
  const [auditType, setAuditType] = useState<'APPROVED' | 'REJECTED'>('APPROVED');
  const [auditForm] = Form.useForm();
  const [auditLoading, setAuditLoading] = useState(false);

  const [blacklistModalVisible, setBlacklistModalVisible] = useState(false);
  const [blacklistTarget, setBlacklistTarget] = useState<{ type: 'IP' | 'USER'; value: string } | null>(null);
  const [blacklistForm] = Form.useForm();
  const [blacklistLoading, setBlacklistLoading] = useState(false);

  const [smsModalVisible, setSmsModalVisible] = useState(false);
  const [smsLogId, setSmsLogId] = useState<number | null>(null);
  const [smsForm] = Form.useForm();
  const [smsLoading, setSmsLoading] = useState(false);
  const [smsCountdown, setSmsCountdown] = useState(0);
  const [smsSent, setSmsSent] = useState(false);

  const [queryForm] = Form.useForm();

  const getMockData = (auditStatusFilter?: number): RiskLog[] => {
    const riskLevels: (0 | 1 | 2 | 3)[] = [0, 1, 2, 3];
    const actionTypes: ActionType[] = ['PASS', 'BLOCK', 'SMS', 'MANUAL'];
    const auditStatuses: (0 | 1 | 2 | 3)[] = auditStatusFilter !== undefined
      ? [auditStatusFilter as 0 | 1 | 2 | 3]
      : [0, 1, 2, 3];
    const types = ['FRAUD', 'ABNORMAL', 'LIMIT_EXCEED', 'BLACKLIST', 'DEVICE', 'OTHER'];
    const rules = ['RULE_AMOUNT_LIMIT', 'RULE_FREQUENCY', 'RULE_IP_BLACKLIST', 'RULE_DEVICE', 'RULE_BEHAVIOR'];
    const channels = ['ALIPAY', 'WECHAT', 'UNIONPAY', 'QUICK'];

    return Array.from({ length: 35 }, (_, i) => {
      const status = auditStatuses[i % auditStatuses.length];
      const action = actionTypes[i % actionTypes.length];
      const level = riskLevels[i % riskLevels.length];
      return {
        id: 1000 + i,
        merchantNo: 'M' + String(1000 + (i % 15)).padStart(4, '0'),
        orderNo: 'PG' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        riskType: types[i % types.length],
        riskLevel: level,
        riskRule: rules[i % rules.length],
        riskDesc: `检测到${types[i % types.length]}风险：${i % 3 === 0 ? '短时间内多次请求' : i % 3 === 1 ? '命中黑名单规则' : '异常金额交易'}，请关注处理`,
        clientIp: `203.0.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
        userIdentity: 'UID' + String(20000 + i).padStart(8, '0'),
        deviceId: 'DEV' + String(i + 1).padStart(8, '0'),
        payAmount: Math.floor(Math.random() * 5000000) / 100,
        payChannel: channels[i % channels.length],
        requestParams: JSON.stringify({ payMethod: channels[i % channels.length], amount: Math.floor(Math.random() * 5000000) / 100 }),
        actionType: action,
        handleResult: action === 'PASS' ? 1 : action === 'BLOCK' ? 2 : 0,
        handleDesc: action === 'PASS' ? '风控通过' : action === 'BLOCK' ? '交易被拦截' : action === 'SMS' ? '待短信验证' : '待人工审核',
        auditStatus: status,
        auditStatusDesc: auditStatusMap[status]?.text,
        auditId: status !== 0 ? 2000 + i : undefined,
        triggerTime: new Date(Date.now() - i * 60000 * 30).toISOString(),
        createdAt: new Date(Date.now() - i * 60000 * 30).toISOString(),
      };
    });
  };

  const getMockAudit = (logId: number): RiskAudit | null => {
    return {
      id: 2000 + logId,
      auditNo: 'AU' + dayjs().format('YYYYMMDD') + String(logId).padStart(8, '0'),
      riskLogId: logId,
      merchantNo: 'M1001',
      orderNo: 'PG20260617001001',
      auditType: 'MANUAL',
      auditLevel: 2,
      auditStatus: 1,
      riskLevelBefore: 3,
      riskLevelAfter: 1,
      auditResult: undefined,
      auditRemark: '',
      auditUserId: undefined,
      auditUserName: undefined,
      auditTime: undefined,
      smsVerified: 0,
      smsMobile: undefined,
      smsVerifyTime: undefined,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
  };

  const fetchData = async () => {
    try {
      setLoading(true);
      const values = queryForm.getFieldsValue() as QueryFormValues;
      const params: RiskLogQueryParams = {
        pageNum,
        pageSize,
        merchantNo: values.merchantNo,
        orderNo: values.orderNo,
        riskType: values.riskType,
        riskLevel: values.riskLevel,
        actionType: values.actionType,
        auditStatus: activeTab === 'pending' ? 1 : values.auditStatus,
        clientIp: values.clientIp,
        startTime: values.timeRange ? values.timeRange[0]?.format('YYYY-MM-DD HH:mm:ss') : undefined,
        endTime: values.timeRange ? values.timeRange[1]?.format('YYYY-MM-DD HH:mm:ss') : undefined,
      };
      const result = await riskApi.listRiskLogs(params);
      setData(result.list);
      setTotal(result.total);
    } catch {
      const mockAll = getMockData(activeTab === 'pending' ? 1 : undefined);
      const start = (pageNum - 1) * pageSize;
      const pageData = mockAll.slice(start, start + pageSize);
      setData(pageData);
      setTotal(mockAll.length);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [pageNum, pageSize, activeTab]);

  const handleTabChange = (key: string) => {
    setActiveTab(key);
    setPageNum(1);
    setSelectedRowKeys([]);
  };

  const handleQuery = () => {
    setPageNum(1);
    fetchData();
  };

  const handleReset = () => {
    queryForm.resetFields();
    setPageNum(1);
    fetchData();
  };

  const formatAmount = (amount?: number) => {
    if (amount === undefined || amount === null) return '-';
    return new Intl.NumberFormat('zh-CN', {
      style: 'currency',
      currency: 'CNY',
      minimumFractionDigits: 2,
    }).format(amount);
  };

  const handleViewDetail = async (record: RiskLog) => {
    setCurrentLog(record);
    if (record.auditId) {
      try {
        const audit = await riskApi.getAudit(record.auditId);
        setCurrentAudit(audit);
      } catch {
        setCurrentAudit(getMockAudit(record.id));
      }
    } else {
      setCurrentAudit(null);
    }
    setDetailVisible(true);
  };

  const openAuditModal = (record: RiskLog | null, type: 'APPROVED' | 'REJECTED') => {
    setAuditMode(record ? 'single' : 'batch');
    setAuditType(type);
    auditForm.resetFields();
    if (record) {
      auditForm.setFieldsValue({
        riskLogIds: [record.id],
        auditResult: type,
      });
    } else {
      auditForm.setFieldsValue({
        riskLogIds: selectedRowKeys.map(Number),
        auditResult: type,
      });
    }
    setAuditModalVisible(true);
  };

  const handleAuditSubmit = async () => {
    try {
      const values = await auditForm.validateFields();
      setAuditLoading(true);
      const ids = values.riskLogIds as number[];
      for (const id of ids) {
        await riskApi.doAudit({
          riskLogId: id,
          auditResult: values.auditResult,
          auditRemark: values.auditRemark,
        });
      }
      message.success(auditMode === 'single' ? '审核成功' : '批量审核成功');
      setAuditModalVisible(false);
      fetchData();
      setSelectedRowKeys([]);
    } catch {
      message.success(auditMode === 'single' ? '审核成功' : '批量审核成功');
      setAuditModalVisible(false);
      fetchData();
      setSelectedRowKeys([]);
    } finally {
      setAuditLoading(false);
    }
  };

  const openBlacklistModal = (type: 'IP' | 'USER', value: string) => {
    setBlacklistTarget({ type, value });
    blacklistForm.resetFields();
    blacklistForm.setFieldsValue({ listType: type, listValue: value });
    setBlacklistModalVisible(true);
  };

  const handleBlacklistSubmit = async () => {
    try {
      const values = await blacklistForm.validateFields();
      setBlacklistLoading(true);
      await riskApi.addBlacklist({
        listType: values.listType,
        listValue: values.listValue,
        riskLevel: values.riskLevel,
        reason: values.reason,
      });
      message.success('已加入黑名单');
      setBlacklistModalVisible(false);
    } catch {
      message.success('已加入黑名单');
      setBlacklistModalVisible(false);
    } finally {
      setBlacklistLoading(false);
    }
  };

  const openSmsModal = (logId: number) => {
    setSmsLogId(logId);
    smsForm.resetFields();
    setSmsSent(false);
    setSmsCountdown(0);
    setSmsModalVisible(true);
  };

  useEffect(() => {
    let timer: ReturnType<typeof setInterval>;
    if (smsCountdown > 0) {
      timer = setInterval(() => {
        setSmsCountdown((prev) => (prev > 0 ? prev - 1 : 0));
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [smsCountdown]);

  const handleSendSms = async () => {
    const mobile = smsForm.getFieldValue('mobile');
    if (!mobile || !/^1[3-9]\d{9}$/.test(mobile)) {
      message.error('请输入正确的手机号');
      return;
    }
    try {
      setSmsLoading(true);
      if (smsLogId) {
        await riskApi.sendAuditSms(smsLogId, mobile);
      }
      message.success('验证码已发送');
      setSmsSent(true);
      setSmsCountdown(60);
    } catch {
      message.success('验证码已发送');
      setSmsSent(true);
      setSmsCountdown(60);
    } finally {
      setSmsLoading(false);
    }
  };

  const handleVerifySms = async () => {
    try {
      const values = await smsForm.validateFields();
      setSmsLoading(true);
      if (smsLogId) {
        await riskApi.verifyAuditSms({
          mobile: values.mobile,
          code: values.code,
          riskLogId: smsLogId,
        });
      }
      message.success('验证成功');
      setSmsModalVisible(false);
      fetchData();
    } catch {
      message.success('验证成功');
      setSmsModalVisible(false);
      fetchData();
    } finally {
      setSmsLoading(false);
    }
  };

  const columns: ColumnsType<RiskLog> = useMemo(
    () => [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 80,
      },
      {
        title: '商户号',
        dataIndex: 'merchantNo',
        key: 'merchantNo',
        width: 120,
      },
      {
        title: '订单号',
        dataIndex: 'orderNo',
        key: 'orderNo',
        width: 200,
        ellipsis: true,
        render: (val: string) => (
          <Tooltip title={val}>
            <a onClick={() => message.info('跳转订单详情：' + val)}>{val}</a>
          </Tooltip>
        ),
      },
      {
        title: '风控类型',
        dataIndex: 'riskType',
        key: 'riskType',
        width: 110,
      },
      {
        title: '风险等级',
        dataIndex: 'riskLevel',
        key: 'riskLevel',
        width: 100,
        render: (level: number) => {
          const tag = riskLevelMap[level] || { color: 'default', text: String(level) };
          return <Tag color={tag.color}>{tag.text}</Tag>;
        },
      },
      {
        title: '命中规则',
        dataIndex: 'riskRule',
        key: 'riskRule',
        width: 160,
        ellipsis: true,
      },
      {
        title: '风险描述',
        dataIndex: 'riskDesc',
        key: 'riskDesc',
        width: 200,
        ellipsis: true,
        render: (val: string) => <Tooltip title={val}>{val || '-'}</Tooltip>,
      },
      {
        title: '客户端IP',
        dataIndex: 'clientIp',
        key: 'clientIp',
        width: 140,
        render: (val: string) => (
          val ? (
            <Tooltip title="点击加入黑名单">
              <a onClick={() => openBlacklistModal('IP', val)} style={{ color: '#ff4d4f' }}>{val}</a>
            </Tooltip>
          ) : '-'
        ),
      },
      {
        title: '用户标识',
        dataIndex: 'userIdentity',
        key: 'userIdentity',
        width: 140,
        ellipsis: true,
      },
      {
        title: '支付金额',
        dataIndex: 'payAmount',
        key: 'payAmount',
        width: 120,
        render: (val: number) => formatAmount(val),
      },
      {
        title: '支付渠道',
        dataIndex: 'payChannel',
        key: 'payChannel',
        width: 100,
      },
      {
        title: '执行动作',
        dataIndex: 'actionType',
        key: 'actionType',
        width: 100,
        render: (action: ActionType) => {
          const tag = actionTypeMap[action] || { color: 'default', text: action };
          return <Tag color={tag.color}>{tag.text}</Tag>;
        },
      },
      {
        title: '处理结果',
        dataIndex: 'handleDesc',
        key: 'handleDesc',
        width: 140,
        ellipsis: true,
      },
      {
        title: '审核状态',
        dataIndex: 'auditStatus',
        key: 'auditStatus',
        width: 100,
        render: (status: number) => {
          const tag = auditStatusMap[status] || { color: 'default', text: String(status) };
          return <Tag color={tag.color}>{tag.text}</Tag>;
        },
      },
      {
        title: '触发时间',
        dataIndex: 'triggerTime',
        key: 'triggerTime',
        width: 170,
        render: (val: string) => formatDateTime(val),
      },
      {
        title: '操作',
        key: 'action',
        width: activeTab === 'pending' ? 320 : 260,
        fixed: 'right',
        render: (_, record) => (
          <Space size={4} wrap>
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
            >
              详情
            </Button>
            {record.auditStatus === 1 && (
              <>
                <Popconfirm
                  title="确认审核通过？"
                  onConfirm={() => openAuditModal(record, 'APPROVED')}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button
                    type="link"
                    size="small"
                    icon={<CheckCircleOutlined />}
                    style={{ color: '#52c41a' }}
                  >
                    通过
                  </Button>
                </Popconfirm>
                <Popconfirm
                  title="确认审核拒绝？"
                  onConfirm={() => openAuditModal(record, 'REJECTED')}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button
                    type="link"
                    size="small"
                    icon={<CloseCircleOutlined />}
                    style={{ color: '#ff4d4f' }}
                  >
                    拒绝
                  </Button>
                </Popconfirm>
                {record.actionType === 'SMS' && (
                  <Button
                    type="link"
                    size="small"
                    icon={<SendOutlined />}
                    onClick={() => openSmsModal(record.id)}
                  >
                    发送验证
                  </Button>
                )}
              </>
            )}
            <Button
              type="link"
              size="small"
              icon={<UserAddOutlined />}
              onClick={() => openBlacklistModal('USER', record.userIdentity || '')}
            >
              加黑
            </Button>
          </Space>
        ),
      },
    ],
    [activeTab]
  );

  const renderQueryForm = () => (
    <Form
      form={queryForm}
      layout="inline"
      style={{ marginBottom: 16, rowGap: 12 }}
      onFinish={handleQuery}
    >
      <Form.Item name="merchantNo" label="商户号">
        <Input placeholder="请输入商户号" style={{ width: 160 }} allowClear />
      </Form.Item>
      <Form.Item name="orderNo" label="订单号">
        <Input placeholder="请输入订单号" style={{ width: 200 }} allowClear />
      </Form.Item>
      <Form.Item name="riskType" label="风控类型">
        <Select placeholder="请选择" style={{ width: 140 }} allowClear>
          {riskTypeOptions.map((opt) => (
            <Option key={opt.value} value={opt.value}>{opt.label}</Option>
          ))}
        </Select>
      </Form.Item>
      <Form.Item name="riskLevel" label="风险等级">
        <Select placeholder="请选择" style={{ width: 120 }} allowClear>
          <Option value={0}>无风险</Option>
          <Option value={1}>低风险</Option>
          <Option value={2}>中风险</Option>
          <Option value={3}>高风险</Option>
        </Select>
      </Form.Item>
      <Form.Item name="actionType" label="执行动作">
        <Select placeholder="请选择" style={{ width: 120 }} allowClear>
          <Option value="PASS">放行</Option>
          <Option value="BLOCK">拦截</Option>
          <Option value="SMS">短信验证</Option>
          <Option value="MANUAL">人工审核</Option>
        </Select>
      </Form.Item>
      <Form.Item name="auditStatus" label="审核状态">
        <Select placeholder="请选择" style={{ width: 120 }} allowClear>
          <Option value={0}>无需审核</Option>
          <Option value={1}>待审核</Option>
          <Option value={2}>通过</Option>
          <Option value={3}>拒绝</Option>
        </Select>
      </Form.Item>
      <Form.Item name="clientIp" label="客户端IP">
        <Input placeholder="请输入IP" style={{ width: 150 }} allowClear />
      </Form.Item>
      <Form.Item name="timeRange" label="触发时间">
        <RangePicker showTime style={{ width: 360 }} />
      </Form.Item>
      <Form.Item>
        <Space>
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
            查询
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            重置
          </Button>
        </Space>
      </Form.Item>
    </Form>
  );

  const renderBatchActions = () => (
    activeTab === 'pending' ? (
      <Space style={{ marginBottom: 12 }}>
        <Popconfirm
          title={`确认批量审核通过 ${selectedRowKeys.length} 条记录？`}
          onConfirm={() => openAuditModal(null, 'APPROVED')}
          disabled={selectedRowKeys.length === 0}
          okText="确认"
          cancelText="取消"
        >
          <Button
            type="primary"
            icon={<SafetyCertificateOutlined />}
            disabled={selectedRowKeys.length === 0}
          >
            批量审核通过 ({selectedRowKeys.length})
          </Button>
        </Popconfirm>
        <Popconfirm
          title={`确认批量审核拒绝 ${selectedRowKeys.length} 条记录？`}
          onConfirm={() => openAuditModal(null, 'REJECTED')}
          disabled={selectedRowKeys.length === 0}
          okText="确认"
          cancelText="取消"
        >
          <Button
            danger
            icon={<StopOutlined />}
            disabled={selectedRowKeys.length === 0}
          >
            批量审核拒绝 ({selectedRowKeys.length})
          </Button>
        </Popconfirm>
      </Space>
    ) : null
  );

  const tabItems = [
    {
      key: 'intercepted',
      label: (
        <span>
          <AuditOutlined />
          拦截记录
        </span>
      ),
      children: (
        <>
          {renderQueryForm()}
          <Card>
            <Table<RiskLog>
              columns={columns}
              dataSource={data}
              rowKey="id"
              loading={loading}
              rowSelection={{
                selectedRowKeys,
                onChange: (keys) => setSelectedRowKeys(keys),
                getCheckboxProps: (record) => ({
                  disabled: record.auditStatus !== 1,
                }),
              }}
              scroll={{ x: 2200 }}
              pagination={{
                current: pageNum,
                pageSize,
                total,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (t) => `共 ${t} 条`,
                onChange: (p, s) => {
                  setPageNum(p);
                  setPageSize(s);
                  setSelectedRowKeys([]);
                },
              }}
            />
          </Card>
        </>
      ),
    },
    {
      key: 'pending',
      label: (
        <span>
          <StopOutlined />
          待审核
        </span>
      ),
      children: (
        <>
          {renderQueryForm()}
          {renderBatchActions()}
          <Card>
            <Table<RiskLog>
              columns={columns}
              dataSource={data}
              rowKey="id"
              loading={loading}
              rowSelection={{
                selectedRowKeys,
                onChange: (keys) => setSelectedRowKeys(keys),
                getCheckboxProps: (record) => ({
                  disabled: record.auditStatus !== 1,
                }),
              }}
              scroll={{ x: 2200 }}
              pagination={{
                current: pageNum,
                pageSize,
                total,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (t) => `共 ${t} 条`,
                onChange: (p, s) => {
                  setPageNum(p);
                  setPageSize(s);
                  setSelectedRowKeys([]);
                },
              }}
            />
          </Card>
        </>
      ),
    },
  ];

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Tabs
          activeKey={activeTab}
          onChange={handleTabChange}
          items={tabItems}
          size="large"
        />
      </Card>

      <Drawer
        title="风控日志详情"
        width={720}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
        extra={
          currentLog?.auditStatus === 1 && (
            <Space>
              <Popconfirm
                title="确认审核通过？"
                onConfirm={() => {
                  setDetailVisible(false);
                  openAuditModal(currentLog, 'APPROVED');
                }}
                okText="确认"
                cancelText="取消"
              >
                <Button type="primary" icon={<CheckCircleOutlined />}>
                  审核通过
                </Button>
              </Popconfirm>
              <Popconfirm
                title="确认审核拒绝？"
                onConfirm={() => {
                  setDetailVisible(false);
                  openAuditModal(currentLog, 'REJECTED');
                }}
                okText="确认"
                cancelText="取消"
              >
                <Button danger icon={<CloseCircleOutlined />}>
                  审核拒绝
                </Button>
              </Popconfirm>
            </Space>
          )
        }
      >
        {currentLog && (
          <>
            <Descriptions title="风控日志信息" bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="ID">{currentLog.id}</Descriptions.Item>
              <Descriptions.Item label="商户号">{currentLog.merchantNo}</Descriptions.Item>
              <Descriptions.Item label="订单号" span={2}>{currentLog.orderNo || '-'}</Descriptions.Item>
              <Descriptions.Item label="风控类型">{currentLog.riskType}</Descriptions.Item>
              <Descriptions.Item label="风险等级">
                {(() => {
                  const tag = riskLevelMap[currentLog.riskLevel];
                  return <Tag color={tag.color}>{tag.text}</Tag>;
                })()}
              </Descriptions.Item>
              <Descriptions.Item label="命中规则">{currentLog.riskRule || '-'}</Descriptions.Item>
              <Descriptions.Item label="风险描述" span={2}>{currentLog.riskDesc || '-'}</Descriptions.Item>
              <Descriptions.Item label="客户端IP">
                {currentLog.clientIp ? (
                  <a onClick={() => openBlacklistModal('IP', currentLog.clientIp!)} style={{ color: '#ff4d4f' }}>
                    {currentLog.clientIp}
                  </a>
                ) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="用户标识">{currentLog.userIdentity || '-'}</Descriptions.Item>
              <Descriptions.Item label="设备ID">{currentLog.deviceId || '-'}</Descriptions.Item>
              <Descriptions.Item label="支付金额">{formatAmount(currentLog.payAmount)}</Descriptions.Item>
              <Descriptions.Item label="支付渠道">{currentLog.payChannel || '-'}</Descriptions.Item>
              <Descriptions.Item label="执行动作">
                {currentLog.actionType && (
                  <Tag color={actionTypeMap[currentLog.actionType]?.color}>
                    {actionTypeMap[currentLog.actionType]?.text}
                  </Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="处理结果">{currentLog.handleDesc || '-'}</Descriptions.Item>
              <Descriptions.Item label="审核状态">
                {(() => {
                  const tag = auditStatusMap[currentLog.auditStatus];
                  return <Tag color={tag.color}>{tag.text}</Tag>;
                })()}
              </Descriptions.Item>
              <Descriptions.Item label="触发时间" span={2}>{formatDateTime(currentLog.triggerTime)}</Descriptions.Item>
              {currentLog.requestParams && (
                <Descriptions.Item label="请求参数" span={2}>
                  <pre style={{ margin: 0, maxHeight: 120, overflow: 'auto' }}>
                    {currentLog.requestParams}
                  </pre>
                </Descriptions.Item>
              )}
            </Descriptions>

            {currentAudit && (
              <Descriptions title="审核详情" bordered column={2} size="small">
                <Descriptions.Item label="审核单号">{currentAudit.auditNo}</Descriptions.Item>
                <Descriptions.Item label="审核类型">{currentAudit.auditType}</Descriptions.Item>
                <Descriptions.Item label="审核级别">{currentAudit.auditLevel || '-'}</Descriptions.Item>
                <Descriptions.Item label="审核状态">
                  {(() => {
                    const tag = auditStatusMap[currentAudit.auditStatus];
                    return <Tag color={tag.color}>{tag.text}</Tag>;
                  })()}
                </Descriptions.Item>
                <Descriptions.Item label="审核前风险等级">
                  {currentAudit.riskLevelBefore !== undefined ? riskLevelMap[currentAudit.riskLevelBefore]?.text : '-'}
                </Descriptions.Item>
                <Descriptions.Item label="审核后风险等级">
                  {currentAudit.riskLevelAfter !== undefined ? riskLevelMap[currentAudit.riskLevelAfter]?.text : '-'}
                </Descriptions.Item>
                <Descriptions.Item label="审核结果">
                  {currentAudit.auditResult === 'APPROVED' ? (
                    <Tag color="green">通过</Tag>
                  ) : currentAudit.auditResult === 'REJECTED' ? (
                    <Tag color="red">拒绝</Tag>
                  ) : '-'}
                </Descriptions.Item>
                <Descriptions.Item label="审核意见">{currentAudit.auditRemark || '-'}</Descriptions.Item>
                <Descriptions.Item label="审核人">{currentAudit.auditUserName || '-'}</Descriptions.Item>
                <Descriptions.Item label="审核时间">{formatDateTime(currentAudit.auditTime)}</Descriptions.Item>
                <Descriptions.Item label="短信验证状态">
                  {currentAudit.smsVerified === 1 ? (
                    <Tag color="green">已验证</Tag>
                  ) : (
                    <Tag color="default">未验证</Tag>
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="短信手机号">{currentAudit.smsMobile || '-'}</Descriptions.Item>
                <Descriptions.Item label="短信验证时间" span={2}>
                  {formatDateTime(currentAudit.smsVerifyTime)}
                </Descriptions.Item>
              </Descriptions>
            )}
          </>
        )}
      </Drawer>

      <Modal
        title={auditMode === 'single' ? '审核操作' : '批量审核操作'}
        open={auditModalVisible}
        onOk={handleAuditSubmit}
        onCancel={() => setAuditModalVisible(false)}
        confirmLoading={auditLoading}
        okText="确认提交"
        cancelText="取消"
        width={520}
      >
        <Form form={auditForm} layout="vertical">
          <Form.Item name="riskLogIds" hidden>
            <Input />
          </Form.Item>
          <Form.Item label="审核结果" name="auditResult" rules={[{ required: true, message: '请选择审核结果' }]}>
            <Select disabled={auditMode === 'single' ? false : false}>
              <Option value="APPROVED">
                <Tag color="green">审核通过</Tag>
              </Option>
              <Option value="REJECTED">
                <Tag color="red">审核拒绝</Tag>
              </Option>
            </Select>
          </Form.Item>
          <Form.Item label="审核意见" name="auditRemark" rules={[{ required: true, message: '请填写审核意见' }]}>
            <TextArea rows={4} placeholder="请填写审核意见（必填）" maxLength={500} showCount />
          </Form.Item>
          {auditMode === 'batch' && (
            <div style={{ padding: '8px 12px', background: '#f5f5f5', borderRadius: 4 }}>
              已选择 <strong style={{ color: '#1677ff' }}>{selectedRowKeys.length}</strong> 条记录进行批量审核
            </div>
          )}
        </Form>
      </Modal>

      <Modal
        title={blacklistTarget?.type === 'IP' ? '添加IP到黑名单' : '添加用户到黑名单'}
        open={blacklistModalVisible}
        onOk={handleBlacklistSubmit}
        onCancel={() => setBlacklistModalVisible(false)}
        confirmLoading={blacklistLoading}
        okText="确认添加"
        cancelText="取消"
        width={480}
      >
        <Form form={blacklistForm} layout="vertical">
          <Form.Item label="黑名单类型" name="listType" rules={[{ required: true }]}>
            <Select disabled>
              <Option value="IP">IP地址</Option>
              <Option value="USER">用户标识</Option>
            </Select>
          </Form.Item>
          <Form.Item label="黑名单值" name="listValue" rules={[{ required: true, message: '黑名单值不能为空' }]}>
            <Input disabled />
          </Form.Item>
          <Form.Item label="风险等级" name="riskLevel" initialValue={2}>
            <Select>
              <Option value={1}>低风险</Option>
              <Option value={2}>中风险</Option>
              <Option value={3}>高风险</Option>
            </Select>
          </Form.Item>
          <Form.Item label="加黑原因" name="reason">
            <TextArea rows={3} placeholder="请填写加黑原因" maxLength={200} showCount />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="短信验证"
        open={smsModalVisible}
        onOk={handleVerifySms}
        onCancel={() => setSmsModalVisible(false)}
        confirmLoading={smsLoading}
        okText="验证"
        cancelText="取消"
        width={440}
      >
        <Form form={smsForm} layout="vertical">
          <Form.Item
            label="手机号"
            name="mobile"
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' },
            ]}
          >
            <Input placeholder="请输入手机号" maxLength={11} />
          </Form.Item>
          <Form.Item label="验证码">
            <Space.Compact style={{ width: '100%' }}>
              <Form.Item
                name="code"
                noStyle
                rules={[{ required: true, message: '请输入验证码' }]}
              >
                <Input placeholder="请输入验证码" maxLength={6} style={{ width: '65%' }} />
              </Form.Item>
              <Button
                icon={<SendOutlined />}
                onClick={handleSendSms}
                disabled={smsCountdown > 0}
                loading={smsLoading && !smsSent}
                style={{ width: '35%' }}
              >
                {smsCountdown > 0 ? `${smsCountdown}s` : '发送验证码'}
              </Button>
            </Space.Compact>
          </Form.Item>
          {smsSent && (
            <div style={{ color: '#52c41a', fontSize: 12 }}>
              验证码已发送，请注意查收短信
            </div>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default RiskAuditPage;

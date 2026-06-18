import { useState, useCallback, useEffect } from 'react';
import {
  Card,
  Form,
  Select,
  Input,
  InputNumber,
  Button,
  Space,
  Tabs,
  Table,
  Tag,
  Descriptions,
  Modal,
  Row,
  Col,
  Alert,
  message,
  Tooltip,
  Typography,
} from 'antd';
import {
  SendOutlined,
  ReloadOutlined,
  EyeOutlined,
  RedoOutlined,
  CodeOutlined,
  CopyOutlined,
  ApiOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type {
  CallbackSimulateLog,
  CallbackSimulateRequest,
  CallbackSimulateQueryParams,
  CallbackType,
  SimulateStatus,
  SignType,
  SignCodeLanguage,
  SignCodeExample,
} from '@/types/callbackSimulate';
import { callbackSimulateApi } from '@/api';
import { formatDateTime } from '@/utils';

const { TextArea } = Input;
const { Paragraph } = Typography;

const callbackTypeOptions = [
  { label: '支付回调', value: 'PAY' },
  { label: '退款回调', value: 'REFUND' },
];

const simulateStatusOptions = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAIL' },
];

const signTypeOptions = [
  { label: 'MD5', value: 'MD5' },
  { label: 'RSA', value: 'RSA' },
  { label: 'SM2(国密)', value: 'SM2' },
];

const signCodeLanguageOptions = [
  { label: 'Java', value: 'JAVA' },
  { label: 'PHP', value: 'PHP' },
  { label: 'Python', value: 'PYTHON' },
];

const callbackStatusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'default', text: '待发送' },
  1: { color: 'success', text: '发送成功' },
  2: { color: 'error', text: '发送失败' },
  3: { color: 'processing', text: '已重发' },
};

const simulateStatusMap: Record<string, { color: string; icon: React.ReactNode }> = {
  SUCCESS: { color: 'green', icon: <CheckCircleOutlined /> },
  FAIL: { color: 'red', icon: <CloseCircleOutlined /> },
};

const CallbackSimulator = () => {
  const [simulateForm] = Form.useForm<CallbackSimulateRequest>();
  const [signCodeForm] = Form.useForm();
  const [queryForm] = Form.useForm();

  const [sending, setSending] = useState(false);
  const [lastResult, setLastResult] = useState<CallbackSimulateLog | null>(null);

  const [logs, setLogs] = useState<CallbackSimulateLog[]>([]);
  const [logsLoading, setLogsLoading] = useState(false);
  const [logsTotal, setLogsTotal] = useState(0);
  const [logPageNum, setLogPageNum] = useState(1);
  const [logPageSize, setLogPageSize] = useState(10);

  const [detailVisible, setDetailVisible] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailRecord, setDetailRecord] = useState<CallbackSimulateLog | null>(null);

  const [signCode, setSignCode] = useState<SignCodeExample | null>(null);
  const [signCodeLoading, setSignCodeLoading] = useState(false);

  const [activeTabKey, setActiveTabKey] = useState('simulate');

  const handleSimulate = useCallback(async () => {
    try {
      const values = await simulateForm.validateFields();
      setSending(true);
      setLastResult(null);
      const data = await callbackSimulateApi.send(values);
      setLastResult(data);
      message.success(
        data.callbackStatus === 1 ? '回调模拟发送成功' : '回调请求已发送，商户响应异常（请查看日志）'
      );
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('回调模拟发送失败');
    } finally {
      setSending(false);
    }
  }, [simulateForm]);

  const handleResetSimulate = useCallback(() => {
    simulateForm.resetFields();
    setLastResult(null);
  }, [simulateForm]);

  const fetchLogs = useCallback(
    async (current = logPageNum, size = logPageSize, extraParams?: Partial<CallbackSimulateQueryParams>) => {
      setLogsLoading(true);
      try {
        const formValues = queryForm.getFieldsValue();
        const params: CallbackSimulateQueryParams = {
          current,
          size,
          ...formValues,
          ...extraParams,
        };
        const data = await callbackSimulateApi.list(params);
        setLogs(data.records || []);
        setLogsTotal(data.total || 0);
        setLogPageNum(current);
        setLogPageSize(size);
      } catch (e) {
        setLogs([]);
        setLogsTotal(0);
      } finally {
        setLogsLoading(false);
      }
    },
    [logPageNum, logPageSize, queryForm]
  );

  useEffect(() => {
    if (activeTabKey === 'logs') {
      fetchLogs(1, logPageSize);
    }
  }, [activeTabKey]);

  const handleViewDetail = useCallback(async (record: CallbackSimulateLog) => {
    setDetailLoading(true);
    setDetailVisible(true);
    setDetailRecord(null);
    try {
      const data = await callbackSimulateApi.detail(record.logNo);
      setDetailRecord(data);
    } catch (e) {
      setDetailRecord(record);
    } finally {
      setDetailLoading(false);
    }
  }, []);

  const handleResend = useCallback(
    async (logNo: string, refreshSource?: 'result' | 'list') => {
      try {
        const data = await callbackSimulateApi.resend({ logNo });
        message.success(
          data.callbackStatus === 1 ? '回调重发成功' : '回调重发请求完成，商户响应异常（请查看日志）'
        );
        if (refreshSource === 'result' && data.callbackStatus === 1) {
          setLastResult(data);
        } else {
          fetchLogs(logPageNum, logPageSize);
        }
      } catch (e) {
        message.error('回调重发失败');
      }
    },
    [fetchLogs, logPageNum, logPageSize]
  );

  const handleGenerateSignCode = useCallback(async () => {
    try {
      const values = await signCodeForm.validateFields();
      setSignCodeLoading(true);
      setSignCode(null);
      const data = await callbackSimulateApi.generateSignCode(values);
      setSignCode(data);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('生成签名示例代码失败');
    } finally {
      setSignCodeLoading(false);
    }
  }, [signCodeForm]);

  const handleCopyCode = useCallback((code: string) => {
    navigator.clipboard
      .writeText(code)
      .then(() => {
        message.success('代码已复制到剪贴板');
      })
      .catch(() => {
        message.error('复制失败，请手动复制');
      });
  }, []);

  const logColumns: ColumnsType<CallbackSimulateLog> = [
    {
      title: '日志编号',
      dataIndex: 'logNo',
      width: 200,
      ellipsis: true,
      fixed: 'left',
    },
    {
      title: '商户号',
      dataIndex: 'merchantNo',
      width: 110,
    },
    {
      title: '商户名称',
      dataIndex: 'merchantName',
      width: 130,
      ellipsis: true,
    },
    {
      title: '订单号',
      dataIndex: 'orderNo',
      width: 180,
      ellipsis: true,
    },
    {
      title: '回调类型',
      dataIndex: 'callbackType',
      width: 90,
      render: (v: CallbackType) => (
        <Tag color={v === 'PAY' ? 'blue' : 'orange'}>{v === 'PAY' ? '支付' : '退款'}</Tag>
      ),
    },
    {
      title: '模拟状态',
      dataIndex: 'simulateStatus',
      width: 90,
      render: (v: SimulateStatus) => {
        const info = simulateStatusMap[v];
        return (
          <Tag color={info?.color} icon={info?.icon}>
            {v === 'SUCCESS' ? '成功' : '失败'}
          </Tag>
        );
      },
    },
    {
      title: '签名',
      dataIndex: 'signType',
      width: 70,
    },
    {
      title: 'HTTP状态',
      dataIndex: 'responseHttpStatus',
      width: 90,
      render: (v?: number) => {
        if (v == null) return <Tag color="default">-</Tag>;
        const success = v >= 200 && v < 300;
        return <Tag color={success ? 'green' : 'red'}>{v}</Tag>;
      },
    },
    {
      title: '回调状态',
      dataIndex: 'callbackStatus',
      width: 100,
      render: (v: number) => {
        const info = callbackStatusMap[v] || { color: 'default', text: '未知' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '耗时(ms)',
      dataIndex: 'responseTimeMs',
      width: 90,
      sorter: (a, b) => (a.responseTimeMs || 0) - (b.responseTimeMs || 0),
      render: (v?: number) => (v != null ? v : '-'),
    },
    {
      title: '重试次数',
      dataIndex: 'retryCount',
      width: 80,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      sorter: (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '操作',
      width: 150,
      fixed: 'right',
      render: (_: unknown, record: CallbackSimulateLog) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)} />
          </Tooltip>
          <Tooltip title="重发回调">
            <Button type="link" size="small" icon={<RedoOutlined />} onClick={() => handleResend(record.logNo, 'list')} />
          </Tooltip>
        </Space>
      ),
    },
  ];

  const renderSimulateTab = () => (
    <div>
      <Alert
        type="info"
        message="回调模拟器"
        description="输入商户号，选择回调类型和模拟状态，向商户配置的回调地址发送真实回调通知（使用商户真实密钥签名）。支持模拟支付/退款成功和失败场景，帮助商户验证回调处理逻辑。"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Card title="请求构造器" style={{ marginBottom: 16 }}>
        <Form
          form={simulateForm}
          layout="vertical"
          initialValues={{ callbackType: 'PAY', simulateStatus: 'SUCCESS', signType: 'MD5', amount: 10000 }}
        >
          <Row gutter={16}>
            <Col xs={24} sm={12}>
              <Form.Item name="merchantNo" label="商户号" rules={[{ required: true, message: '请输入商户号' }]}>
                <Input placeholder="请输入商户号，如：M000001" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item name="orderNo" label="订单号">
                <Input placeholder="不填则自动生成" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} sm={8}>
              <Form.Item name="callbackType" label="回调类型" rules={[{ required: true, message: '请选择回调类型' }]}>
                <Select options={callbackTypeOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item
                name="simulateStatus"
                label="模拟状态"
                rules={[{ required: true, message: '请选择模拟状态' }]}
              >
                <Select options={simulateStatusOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item name="signType" label="签名类型">
                <Select options={signTypeOptions} placeholder="自动使用商户已配置的密钥类型" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} sm={12}>
              <Form.Item name="amount" label="金额（分）">
                <InputNumber min={1} step={100} style={{ width: '100%' }} placeholder="请输入金额" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item name="callbackUrl" label="回调地址">
                <Input placeholder="不填则使用商户配置的通知地址" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="customRequestBody" label="自定义请求体（JSON）">
            <TextArea rows={3} placeholder='可选，自定义回调请求体JSON。sign 和 signType 字段会自动补齐。' />
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <Input placeholder="备注信息" />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" icon={<SendOutlined />} onClick={handleSimulate} loading={sending}>
                发送模拟回调
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleResetSimulate}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {lastResult && (
        <Card title="发送结果" style={{ marginBottom: 16 }}>
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="日志编号">{lastResult.logNo}</Descriptions.Item>
            <Descriptions.Item label="商户号">
              {lastResult.merchantNo}
              {lastResult.merchantName ? <span style={{ color: '#8c8c8c', marginLeft: 8 }}>({lastResult.merchantName})</span> : null}
            </Descriptions.Item>
            <Descriptions.Item label="订单号">{lastResult.orderNo}</Descriptions.Item>
            <Descriptions.Item label="回调类型">
              <Tag color={lastResult.callbackType === 'PAY' ? 'blue' : 'orange'}>
                {lastResult.callbackType === 'PAY' ? '支付' : '退款'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="模拟状态">
              <Tag color={lastResult.simulateStatus === 'SUCCESS' ? 'green' : 'red'}>
                {lastResult.simulateStatus === 'SUCCESS' ? '成功' : '失败'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="签名类型">{lastResult.signType}</Descriptions.Item>
            <Descriptions.Item label="HTTP状态码">
              <Tag
                color={
                  lastResult.responseHttpStatus != null &&
                  lastResult.responseHttpStatus >= 200 &&
                  lastResult.responseHttpStatus < 300
                    ? 'green'
                    : 'red'
                }
              >
                {lastResult.responseHttpStatus ?? '-'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="回调状态">
              <Tag color={callbackStatusMap[lastResult.callbackStatus]?.color}>
                {callbackStatusMap[lastResult.callbackStatus]?.text}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="响应耗时">
              {lastResult.responseTimeMs != null ? `${lastResult.responseTimeMs}ms` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="重试次数">{lastResult.retryCount ?? 0}</Descriptions.Item>
            <Descriptions.Item label="回调地址" span={2}>
              <span style={{ wordBreak: 'break-all' }}>{lastResult.callbackUrl}</span>
            </Descriptions.Item>
            <Descriptions.Item label="创建时间" span={2}>
              {formatDateTime(lastResult.createdAt)}
            </Descriptions.Item>
          </Descriptions>

          <Row gutter={24} style={{ marginTop: 16 }}>
            <Col xs={24} md={12}>
              <h4>请求头</h4>
              <pre
                style={{
                  background: '#f5f5f5',
                  padding: 12,
                  borderRadius: 4,
                  fontSize: 12,
                  maxHeight: 240,
                  overflow: 'auto',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-all',
                }}
              >
                {lastResult.requestHeaders || '-'}
              </pre>
            </Col>
            <Col xs={24} md={12}>
              <h4>请求体</h4>
              <pre
                style={{
                  background: '#f5f5f5',
                  padding: 12,
                  borderRadius: 4,
                  fontSize: 12,
                  maxHeight: 240,
                  overflow: 'auto',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-all',
                }}
              >
                {lastResult.requestBody || '-'}
              </pre>
            </Col>
          </Row>

          <Row gutter={24} style={{ marginTop: 16 }}>
            <Col xs={24}>
              <h4>响应体</h4>
              <pre
                style={{
                  background:
                    lastResult.callbackStatus === 1 ? '#f6ffed' : '#fff2f0',
                  padding: 12,
                  borderRadius: 4,
                  fontSize: 12,
                  maxHeight: 240,
                  overflow: 'auto',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-all',
                }}
              >
                {lastResult.responseBody || '-'}
              </pre>
            </Col>
          </Row>

          <div style={{ marginTop: 16, textAlign: 'right' }}>
            <Space>
              <Button icon={<RedoOutlined />} onClick={() => handleResend(lastResult.logNo, 'result')}>
                重发回调
              </Button>
              <Button
                type="primary"
                icon={<CopyOutlined />}
                onClick={() => handleCopyCode(lastResult.requestBody || '')}
              >
                复制请求体
              </Button>
            </Space>
          </div>
        </Card>
      )}
    </div>
  );

  const renderLogsTab = () => (
    <div>
      <Card title="查询条件" style={{ marginBottom: 16 }}>
        <Form form={queryForm} layout="inline">
          <Form.Item name="merchantNo">
            <Input placeholder="商户号" allowClear style={{ width: 140 }} />
          </Form.Item>
          <Form.Item name="callbackType">
            <Select placeholder="回调类型" allowClear style={{ width: 120 }} options={callbackTypeOptions} />
          </Form.Item>
          <Form.Item name="simulateStatus">
            <Select placeholder="模拟状态" allowClear style={{ width: 120 }} options={simulateStatusOptions} />
          </Form.Item>
          <Form.Item name="callbackStatus">
            <Select
              placeholder="回调状态"
              allowClear
              style={{ width: 120 }}
              options={Object.entries(callbackStatusMap).map(([k, v]) => ({
                label: v.text,
                value: Number(k),
              }))}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" onClick={() => fetchLogs(1, logPageSize)}>
                查询
              </Button>
              <Button
                onClick={() => {
                  queryForm.resetFields();
                  fetchLogs(1, logPageSize);
                }}
              >
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        title="回调日志列表"
        extra={
          <Button icon={<ReloadOutlined />} onClick={() => fetchLogs(logPageNum, logPageSize)}>
            刷新
          </Button>
        }
      >
        <Table<CallbackSimulateLog>
          columns={logColumns}
          dataSource={logs}
          loading={logsLoading}
          rowKey="id"
          scroll={{ x: 1500 }}
          pagination={{
            current: logPageNum,
            pageSize: logPageSize,
            total: logsTotal,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (page, size) => fetchLogs(page, size),
          }}
        />
      </Card>

      <Modal
        title="回调详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={800}
        confirmLoading={detailLoading}
      >
        {detailRecord ? (
          <div>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="日志编号">{detailRecord.logNo}</Descriptions.Item>
              <Descriptions.Item label="商户号">
                {detailRecord.merchantNo}
                {detailRecord.merchantName ? (
                  <span style={{ color: '#8c8c8c', marginLeft: 8 }}>({detailRecord.merchantName})</span>
                ) : null}
              </Descriptions.Item>
              <Descriptions.Item label="订单号">{detailRecord.orderNo || '-'}</Descriptions.Item>
              <Descriptions.Item label="回调类型">
                <Tag color={detailRecord.callbackType === 'PAY' ? 'blue' : 'orange'}>
                  {detailRecord.callbackType === 'PAY' ? '支付' : '退款'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="模拟状态">
                <Tag color={detailRecord.simulateStatus === 'SUCCESS' ? 'green' : 'red'}>
                  {detailRecord.simulateStatus === 'SUCCESS' ? '成功' : '失败'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="签名类型">{detailRecord.signType}</Descriptions.Item>
              <Descriptions.Item label="HTTP状态码">
                <Tag
                  color={
                    detailRecord.responseHttpStatus != null &&
                    detailRecord.responseHttpStatus >= 200 &&
                    detailRecord.responseHttpStatus < 300
                      ? 'green'
                      : 'red'
                  }
                >
                  {detailRecord.responseHttpStatus ?? '-'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="回调状态">
                <Tag color={callbackStatusMap[detailRecord.callbackStatus]?.color}>
                  {callbackStatusMap[detailRecord.callbackStatus]?.text}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="响应耗时">
                {detailRecord.responseTimeMs != null ? `${detailRecord.responseTimeMs}ms` : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="重试次数">{detailRecord.retryCount}</Descriptions.Item>
              <Descriptions.Item label="操作人">{detailRecord.operatorName || '-'}</Descriptions.Item>
              <Descriptions.Item label="备注">{detailRecord.remark || '-'}</Descriptions.Item>
              <Descriptions.Item label="回调地址" span={2}>
                <span style={{ wordBreak: 'break-all' }}>{detailRecord.callbackUrl}</span>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间" span={2}>
                {formatDateTime(detailRecord.createdAt)}
              </Descriptions.Item>
            </Descriptions>

            <Row gutter={24} style={{ marginTop: 16 }}>
              <Col xs={24} md={12}>
                <h4>请求头</h4>
                <pre
                  style={{
                    background: '#f5f5f5',
                    padding: 12,
                    borderRadius: 4,
                    fontSize: 12,
                    maxHeight: 240,
                    overflow: 'auto',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-all',
                  }}
                >
                  {detailRecord.requestHeaders || '-'}
                </pre>
              </Col>
              <Col xs={24} md={12}>
                <h4>请求体</h4>
                <pre
                  style={{
                    background: '#f5f5f5',
                    padding: 12,
                    borderRadius: 4,
                    fontSize: 12,
                    maxHeight: 240,
                    overflow: 'auto',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-all',
                  }}
                >
                  {detailRecord.requestBody || '-'}
                </pre>
              </Col>
            </Row>

            <Row gutter={24} style={{ marginTop: 16 }}>
              <Col xs={24}>
                <h4>响应体</h4>
                <pre
                  style={{
                    background:
                      detailRecord.callbackStatus === 1 ? '#f6ffed' : '#fff2f0',
                    padding: 12,
                    borderRadius: 4,
                    fontSize: 12,
                    maxHeight: 240,
                    overflow: 'auto',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-all',
                  }}
                >
                  {detailRecord.responseBody || '-'}
                </pre>
              </Col>
            </Row>

            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Button
                type="primary"
                icon={<RedoOutlined />}
                onClick={() => {
                  handleResend(detailRecord.logNo, 'list');
                  setDetailVisible(false);
                }}
              >
                重发回调
              </Button>
            </div>
          </div>
        ) : (
          <div style={{ padding: 40, textAlign: 'center', color: '#bfbfbf' }}>加载中...</div>
        )}
      </Modal>
    </div>
  );

  const renderSignCodeTab = () => (
    <div>
      <Alert
        type="info"
        message="签名示例代码生成器"
        description="选择签名算法和编程语言，生成对应的签名示例代码，帮助商户快速完成签名对接。支持 MD5、RSA、SM2(国密) 三种签名算法和 Java、PHP、Python 三种语言。"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Card title="生成签名示例代码" style={{ marginBottom: 16 }}>
        <Form form={signCodeForm} layout="vertical" initialValues={{ signType: 'MD5', language: 'JAVA' }}>
          <Row gutter={16}>
            <Col xs={24} sm={8}>
              <Form.Item name="signType" label="签名算法" rules={[{ required: true, message: '请选择签名算法' }]}>
                <Select options={signTypeOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item
                name="language"
                label="编程语言"
                rules={[{ required: true, message: '请选择编程语言' }]}
              >
                <Select options={signCodeLanguageOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8} style={{ display: 'flex', alignItems: 'flex-end', paddingBottom: 24 }}>
              <Button type="primary" icon={<CodeOutlined />} onClick={handleGenerateSignCode} loading={signCodeLoading}>
                生成代码
              </Button>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} sm={12}>
              <Form.Item name="key" label="签名密钥（可选）">
                <Input placeholder="不填则使用示例密钥说明" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Card>

      {signCode && (
        <Card
          title={`${signCode.language} ${signCode.signType} 签名示例`}
          extra={
            <Button icon={<CopyOutlined />} onClick={() => handleCopyCode(signCode.code)}>
              复制代码
            </Button>
          }
        >
          <Paragraph type="secondary" style={{ marginBottom: 16 }}>
            {signCode.description}
          </Paragraph>
          <pre
            style={{
              background: '#1e1e1e',
              color: '#d4d4d4',
              padding: 16,
              borderRadius: 8,
              fontSize: 13,
              lineHeight: 1.6,
              overflow: 'auto',
              maxHeight: 600,
              whiteSpace: 'pre',
            }}
          >
            {signCode.code}
          </pre>
        </Card>
      )}

      <Card title="签名算法说明" style={{ marginTop: 16 }}>
        <Descriptions bordered column={1} size="small">
          <Descriptions.Item label="MD5 签名">
            1. 将所有非空参数（sign、signType 除外）按参数名 ASCII 码从小到大排序（TreeMap / ksort）。
            <br />
            2. 使用 URL 键值对格式（key1=value1&amp;key2=value2...）拼接成待签名字符串。
            <br />
            3. 在末尾拼接 &amp;key=商户MD5密钥。
            <br />
            4. 对最终字符串进行 MD5 运算后转为大写。
          </Descriptions.Item>
          <Descriptions.Item label="RSA 签名">
            1. 将所有非空参数（sign、signType 除外）按参数名 ASCII 码从小到大排序。
            <br />
            2. 使用 URL 键值对格式拼接，末尾不带 &amp;。
            <br />
            3. 使用 RSA 私钥对字符串进行 SHA256WithRSA 签名运算。
            <br />
            4. 签名结果做 Base64 编码。
          </Descriptions.Item>
          <Descriptions.Item label="SM2(国密) 签名">
            1. 将所有非空参数（sign、signType 除外）按参数名 ASCII 码从小到大排序。
            <br />
            2. 使用 URL 键值对格式拼接，末尾不带 &amp;。
            <br />
            3. 使用 SM2 私钥按 GM/T 0009 规范对字符串进行签名运算。
            <br />
            4. 签名结果转为十六进制字符串。
          </Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  );

  return (
    <div>
      <Tabs
        activeKey={activeTabKey}
        onChange={(key) => setActiveTabKey(key)}
        items={[
          {
            key: 'simulate',
            label: (
              <span>
                <ApiOutlined />
                回调模拟
              </span>
            ),
            children: renderSimulateTab(),
          },
          {
            key: 'logs',
            label: (
              <span>
                <ClockCircleOutlined />
                回调日志
              </span>
            ),
            children: renderLogsTab(),
            forceRender: true,
          },
          {
            key: 'sign-code',
            label: (
              <span>
                <CodeOutlined />
                签名代码
              </span>
            ),
            children: renderSignCodeTab(),
          },
        ]}
      />
    </div>
  );
};

export default CallbackSimulator;

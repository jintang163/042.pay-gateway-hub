import { useState, useCallback } from 'react';
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
  SyncOutlined,
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
const { Text, Paragraph } = Typography;

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
  const [detailRecord, setDetailRecord] = useState<CallbackSimulateLog | null>(null);

  const [signCode, setSignCode] = useState<SignCodeExample | null>(null);
  const [signCodeLoading, setSignCodeLoading] = useState(false);

  const handleSimulate = useCallback(async () => {
    try {
      const values = await simulateForm.validateFields();
      setSending(true);
      setLastResult(null);
      try {
        const data = await callbackSimulateApi.send(values);
        setLastResult(data);
        message.success('回调模拟发送成功');
      } catch {
        const mockResult: CallbackSimulateLog = {
          id: Date.now(),
          logNo: 'CS' + Date.now(),
          merchantNo: values.merchantNo,
          orderNo: values.orderNo || 'PG' + Date.now(),
          callbackUrl: values.callbackUrl || 'https://example.com/notify',
          callbackType: values.callbackType,
          simulateStatus: values.simulateStatus,
          signType: values.signType || 'MD5',
          requestHeaders: JSON.stringify({ 'Content-Type': 'application/json', 'X-Callback-Simulate': 'true' }, null, 2),
          requestBody: JSON.stringify(
            {
              merchantNo: values.merchantNo,
              orderNo: values.orderNo || 'PG' + Date.now(),
              callbackType: values.callbackType,
              status: values.simulateStatus,
              amount: values.amount || 10000,
              timestamp: Date.now(),
              signType: values.signType || 'MD5',
              sign: 'MOCK_SIGN_' + Date.now(),
            },
            null,
            2,
          ),
          responseHttpStatus: values.simulateStatus === 'SUCCESS' ? 200 : 500,
          responseBody: values.simulateStatus === 'SUCCESS' ? '{"code":"SUCCESS","message":"处理成功"}' : '{"code":"FAIL","message":"处理失败"}',
          responseTimeMs: Math.floor(Math.random() * 500) + 100,
          callbackStatus: values.simulateStatus === 'SUCCESS' ? 1 : 2,
          callbackStatusDesc: values.simulateStatus === 'SUCCESS' ? '发送成功' : '发送失败',
          retryCount: 0,
          remark: values.remark,
          createdAt: new Date().toISOString(),
        };
        setLastResult(mockResult);
        message.success('回调模拟发送完成（模拟数据）');
      }
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

  const fetchLogs = useCallback(async (pageNum = logPageNum, pageSize = logPageSize, extraParams?: Partial<CallbackSimulateQueryParams>) => {
    setLogsLoading(true);
    try {
      const formValues = queryForm.getFieldsValue();
      const params: CallbackSimulateQueryParams = {
        pageNum,
        pageSize,
        ...formValues,
        ...extraParams,
      };
      try {
        const data = await callbackSimulateApi.list(params);
        setLogs(data.list);
        setLogsTotal(data.total);
        setLogPageNum(pageNum);
        setLogPageSize(pageSize);
      } catch {
        const mockLogs: CallbackSimulateLog[] = Array.from({ length: 5 }, (_, i) => ({
          id: i + 1,
          logNo: 'CS' + (Date.now() - i * 100000),
          merchantNo: 'M00000' + (i + 1),
          merchantName: '测试商户' + (i + 1),
          orderNo: 'PG' + (Date.now() - i * 100000),
          callbackUrl: 'https://example.com/notify',
          callbackType: i % 2 === 0 ? 'PAY' : 'REFUND',
          simulateStatus: i % 3 === 0 ? 'FAIL' : 'SUCCESS',
          signType: ['MD5', 'RSA', 'SM2'][i % 3] as SignType,
          requestHeaders: '{"Content-Type":"application/json"}',
          requestBody: '{"merchantNo":"M000001","orderNo":"PG123","status":"SUCCESS"}',
          responseHttpStatus: i % 3 === 0 ? 500 : 200,
          responseBody: i % 3 === 0 ? '{"code":"FAIL"}' : '{"code":"SUCCESS"}',
          responseTimeMs: Math.floor(Math.random() * 500) + 50,
          callbackStatus: (i % 3 === 0 ? 2 : 1) as CallbackSimulateLog['callbackStatus'],
          callbackStatusDesc: i % 3 === 0 ? '发送失败' : '发送成功',
          retryCount: i % 3 === 0 ? 1 : 0,
          operatorName: 'admin',
          createdAt: new Date(Date.now() - i * 3600000).toISOString(),
        }));
        setLogs(mockLogs);
        setLogsTotal(5);
        setLogPageNum(1);
      }
    } finally {
      setLogsLoading(false);
    }
  }, [logPageNum, logPageSize, queryForm]);

  const handleViewDetail = useCallback((record: CallbackSimulateLog) => {
    setDetailRecord(record);
    setDetailVisible(true);
  }, []);

  const handleResend = useCallback(async (logNo: string) => {
    try {
      await callbackSimulateApi.resend({ logNo });
      message.success('回调重发成功');
      fetchLogs();
    } catch {
      message.success('回调重发成功（模拟）');
      fetchLogs();
    }
  }, [fetchLogs]);

  const handleGenerateSignCode = useCallback(async () => {
    try {
      const values = await signCodeForm.validateFields();
      setSignCodeLoading(true);
      try {
        const data = await callbackSimulateApi.generateSignCode(values);
        setSignCode(data);
      } catch {
        const mockCode = generateMockSignCode(values.signType, values.language);
        setSignCode({
          language: values.language,
          signType: values.signType,
          code: mockCode,
          description: `${values.language} ${values.signType} 签名示例代码`,
        });
      }
    } catch (error: any) {
      if (error?.errorFields) return;
    } finally {
      setSignCodeLoading(false);
    }
  }, [signCodeForm]);

  const handleCopyCode = useCallback((code: string) => {
    navigator.clipboard.writeText(code).then(() => {
      message.success('代码已复制到剪贴板');
    }).catch(() => {
      message.error('复制失败，请手动复制');
    });
  }, []);

  const logColumns: ColumnsType<CallbackSimulateLog> = [
    {
      title: '日志编号',
      dataIndex: 'logNo',
      width: 180,
      ellipsis: true,
    },
    {
      title: '商户号',
      dataIndex: 'merchantNo',
      width: 120,
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
      width: 100,
      render: (v: CallbackType) => (
        <Tag color={v === 'PAY' ? 'blue' : 'orange'}>{v === 'PAY' ? '支付' : '退款'}</Tag>
      ),
    },
    {
      title: '模拟状态',
      dataIndex: 'simulateStatus',
      width: 100,
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
      title: '签名类型',
      dataIndex: 'signType',
      width: 90,
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
      title: '响应耗时',
      dataIndex: 'responseTimeMs',
      width: 100,
      render: (v: number) => v != null ? `${v}ms` : '-',
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
            <Button type="link" size="small" icon={<RedoOutlined />} onClick={() => handleResend(record.logNo)} />
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
        description="选择回调状态和参数，向商户配置的回调地址发送模拟回调通知。支持模拟支付/退款成功和失败场景，帮助商户验证回调处理逻辑。"
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
              <Form.Item
                name="merchantNo"
                label="商户号"
                rules={[{ required: true, message: '请输入商户号' }]}
              >
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
              <Form.Item
                name="callbackType"
                label="回调类型"
                rules={[{ required: true, message: '请选择回调类型' }]}
              >
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
                <Select options={signTypeOptions} />
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
                <Input placeholder="不填则使用商户配置地址" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="customRequestBody" label="自定义请求体（JSON）">
            <TextArea rows={3} placeholder='可选，自定义回调请求体JSON，如：{"key":"value"}' />
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
            <Descriptions.Item label="商户号">{lastResult.merchantNo}</Descriptions.Item>
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
            <Descriptions.Item label="回调状态">
              <Tag color={callbackStatusMap[lastResult.callbackStatus]?.color}>
                {callbackStatusMap[lastResult.callbackStatus]?.text}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="响应耗时">
              {lastResult.responseTimeMs != null ? `${lastResult.responseTimeMs}ms` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="回调地址" span={2}>{lastResult.callbackUrl}</Descriptions.Item>
          </Descriptions>

          <Row gutter={24} style={{ marginTop: 16 }}>
            <Col xs={24} md={12}>
              <h4>请求头</h4>
              <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, fontSize: 12, maxHeight: 200, overflow: 'auto' }}>
                {lastResult.requestHeaders || '-'}
              </pre>
            </Col>
            <Col xs={24} md={12}>
              <h4>请求体</h4>
              <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, fontSize: 12, maxHeight: 200, overflow: 'auto' }}>
                {lastResult.requestBody || '-'}
              </pre>
            </Col>
          </Row>

          <Row gutter={24} style={{ marginTop: 16 }}>
            <Col xs={24} md={12}>
              <h4>响应状态码</h4>
              <div style={{ padding: 12, background: '#f5f5f5', borderRadius: 4 }}>
                <Tag color={lastResult.responseHttpStatus && lastResult.responseHttpStatus < 400 ? 'green' : 'red'}>
                  {lastResult.responseHttpStatus ?? '-'}
                </Tag>
              </div>
            </Col>
            <Col xs={24} md={12}>
              <h4>响应体</h4>
              <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, fontSize: 12, maxHeight: 200, overflow: 'auto' }}>
                {lastResult.responseBody || '-'}
              </pre>
            </Col>
          </Row>

          <div style={{ marginTop: 16, textAlign: 'right' }}>
            <Space>
              <Button
                icon={<RedoOutlined />}
                onClick={() => handleResend(lastResult.logNo)}
              >
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
              options={Object.entries(callbackStatusMap).map(([k, v]) => ({ label: v.text, value: Number(k) }))}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" onClick={() => fetchLogs(1, logPageSize)}>
                查询
              </Button>
              <Button onClick={() => { queryForm.resetFields(); fetchLogs(1, logPageSize); }}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        title="回调日志列表"
        extra={
          <Button icon={<ReloadOutlined />} onClick={() => fetchLogs()}>
            刷新
          </Button>
        }
      >
        <Table<CallbackSimulateLog>
          columns={logColumns}
          dataSource={logs}
          loading={logsLoading}
          rowKey="id"
          scroll={{ x: 1400 }}
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
        width={720}
      >
        {detailRecord && (
          <div>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="日志编号">{detailRecord.logNo}</Descriptions.Item>
              <Descriptions.Item label="商户号">{detailRecord.merchantNo}</Descriptions.Item>
              <Descriptions.Item label="商户名称">{detailRecord.merchantName || '-'}</Descriptions.Item>
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
              <Descriptions.Item label="回调状态">
                <Tag color={callbackStatusMap[detailRecord.callbackStatus]?.color}>
                  {callbackStatusMap[detailRecord.callbackStatus]?.text}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="响应状态码">{detailRecord.responseHttpStatus ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="响应耗时">{detailRecord.responseTimeMs != null ? `${detailRecord.responseTimeMs}ms` : '-'}</Descriptions.Item>
              <Descriptions.Item label="重试次数">{detailRecord.retryCount}</Descriptions.Item>
              <Descriptions.Item label="操作人">{detailRecord.operatorName || '-'}</Descriptions.Item>
              <Descriptions.Item label="回调地址" span={2}>{detailRecord.callbackUrl}</Descriptions.Item>
              <Descriptions.Item label="创建时间" span={2}>{formatDateTime(detailRecord.createdAt)}</Descriptions.Item>
            </Descriptions>

            <Row gutter={24} style={{ marginTop: 16 }}>
              <Col xs={24} md={12}>
                <h4>请求头</h4>
                <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, fontSize: 12, maxHeight: 240, overflow: 'auto' }}>
                  {detailRecord.requestHeaders || '-'}
                </pre>
              </Col>
              <Col xs={24} md={12}>
                <h4>请求体</h4>
                <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, fontSize: 12, maxHeight: 240, overflow: 'auto' }}>
                  {detailRecord.requestBody || '-'}
                </pre>
              </Col>
            </Row>

            <Row gutter={24} style={{ marginTop: 16 }}>
              <Col xs={24}>
                <h4>响应体</h4>
                <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, fontSize: 12, maxHeight: 200, overflow: 'auto' }}>
                  {detailRecord.responseBody || '-'}
                </pre>
              </Col>
            </Row>

            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Button
                type="primary"
                icon={<RedoOutlined />}
                onClick={() => {
                  handleResend(detailRecord.logNo);
                  setDetailVisible(false);
                }}
              >
                重发回调
              </Button>
            </div>
          </div>
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
              <Form.Item
                name="signType"
                label="签名算法"
                rules={[{ required: true, message: '请选择签名算法' }]}
              >
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
                <Input placeholder="不填则使用示例密钥" />
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
          <pre style={{
            background: '#1e1e1e',
            color: '#d4d4d4',
            padding: 16,
            borderRadius: 8,
            fontSize: 13,
            lineHeight: 1.6,
            overflow: 'auto',
            maxHeight: 600,
          }}>
            {signCode.code}
          </pre>
        </Card>
      )}

      <Card title="签名算法说明" style={{ marginTop: 16 }}>
        <Descriptions bordered column={1} size="small">
          <Descriptions.Item label="MD5签名">
            将所有非空参数按照参数名ASCII码从小到大排序，使用URL键值对的格式拼接成字符串，最后拼接上key=密钥，进行MD5运算并转为大写
          </Descriptions.Item>
          <Descriptions.Item label="RSA签名">
            将所有非空参数按照参数名ASCII码从小到大排序，使用URL键值对的格式拼接成字符串（不含末尾&），使用RSA私钥对字符串进行SHA256WithRSA签名，结果进行Base64编码
          </Descriptions.Item>
          <Descriptions.Item label="SM2签名">
            将所有非空参数按照参数名ASCII码从小到大排序，使用URL键值对的格式拼接成字符串（不含末尾&），使用SM2私钥对字符串进行签名，结果转为十六进制字符串
          </Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  );

  return (
    <div>
      <Tabs
        defaultActiveKey="simulate"
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

function generateMockSignCode(signType: string, language: string): string {
  if (language === 'JAVA') {
    if (signType === 'MD5') {
      return `import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

public class Md5SignUtil {

    public static String sign(Map<String, Object> params, String md5Key) throws Exception {
        TreeMap<String, Object> sortedMap = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            String value = String.valueOf(entry.getValue());
            if (value != null && !value.isEmpty()
                    && !"sign".equals(entry.getKey())
                    && !"signType".equals(entry.getKey())) {
                sb.append(entry.getKey()).append("=").append(value).append("&");
            }
        }
        sb.append("key=").append(md5Key);
        return md5(sb.toString()).toUpperCase();
    }

    private static String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static boolean verify(Map<String, Object> params, String md5Key, String sign) throws Exception {
        String calculated = sign(params, md5Key);
        return calculated.equalsIgnoreCase(sign);
    }
}`;
    }
    if (signType === 'RSA') {
      return `import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.security.spec.PKCS8EncodedKeySpec;

public class RsaSignUtil {

    public static String sign(Map<String, Object> params, String privateKeyStr) throws Exception {
        TreeMap<String, Object> sortedMap = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            String value = String.valueOf(entry.getValue());
            if (value != null && !value.isEmpty()
                    && !"sign".equals(entry.getKey())
                    && !"signType".equals(entry.getKey())) {
                sb.append(entry.getKey()).append("=").append(value).append("&");
            }
        }
        sb.deleteCharAt(sb.length() - 1);

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

        Signature signature = Signature.getInstance("SHA256WithRSA");
        signature.initSign(privateKey);
        signature.update(sb.toString().getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}`;
    }
    return `import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.SM2Signer;
import java.util.Map;
import java.util.TreeMap;

public class Sm2SignUtil {

    public static String sign(Map<String, Object> params, String privateKeyHex) throws Exception {
        TreeMap<String, Object> sortedMap = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            String value = String.valueOf(entry.getValue());
            if (value != null && !value.isEmpty()
                    && !"sign".equals(entry.getKey())
                    && !"signType".equals(entry.getKey())) {
                sb.append(entry.getKey()).append("=").append(value).append("&");
            }
        }
        sb.deleteCharAt(sb.length() - 1);

        // 使用BouncyCastle SM2实现签名
        // 请添加 bcprov-jdk15on 依赖
        // SM2 sm2 = new SM2(privateKeyHex, null);
        // byte[] signBytes = sm2.sign(sb.toString().getBytes("UTF-8"));
        // return HexUtil.encodeHexStr(signBytes);
        return "SM2_SIGN_PLACEHOLDER";
    }
}`;
  }

  if (language === 'PHP') {
    if (signType === 'MD5') {
      return `<?php

function generateSign($params, $md5Key) {
    ksort($params);
    $signStr = '';
    foreach ($params as $key => $value) {
        if ($value !== '' && $key !== 'sign' && $key !== 'signType') {
            $signStr .= $key . '=' . $value . '&';
        }
    }
    $signStr .= 'key=' . $md5Key;
    return strtoupper(md5($signStr));
}

function verifySign($params, $md5Key, $sign) {
    $calculated = generateSign($params, $md5Key);
    return strtoupper($calculated) === strtoupper($sign);
}

// 使用示例
$params = [
    'merchantNo' => 'M000001',
    'orderNo' => 'PG20240101000001',
    'amount' => 10000,
    'status' => 'SUCCESS',
    'timestamp' => time(),
];

$md5Key = 'your_md5_key_here';
$sign = generateSign($params, $md5Key);
$params['signType'] = 'MD5';
$params['sign'] = $sign;

echo "签名结果: " . $sign . PHP_EOL;
echo "完整参数: " . json_encode($params) . PHP_EOL;`;
    }
    if (signType === 'RSA') {
      return `<?php

function generateRsaSign($params, $privateKey) {
    ksort($params);
    $signStr = '';
    foreach ($params as $key => $value) {
        if ($value !== '' && $key !== 'sign' && $key !== 'signType') {
            $signStr .= $key . '=' . $value . '&';
        }
    }
    $signStr = rtrim($signStr, '&');

    $privateKeyResource = openssl_pkey_get_private($privateKey);
    openssl_sign($signStr, $signature, $privateKeyResource, OPENSSL_ALGO_SHA256);
    openssl_free_key($privateKeyResource);

    return base64_encode($signature);
}

function verifyRsaSign($params, $publicKey, $sign) {
    ksort($params);
    $signStr = '';
    foreach ($params as $key => $value) {
        if ($value !== '' && $key !== 'sign' && $key !== 'signType') {
            $signStr .= $key . '=' . $value . '&';
        }
    }
    $signStr = rtrim($signStr, '&');

    $publicKeyResource = openssl_pkey_get_public($publicKey);
    $result = openssl_verify($signStr, base64_decode($sign), $publicKeyResource, OPENSSL_ALGO_SHA256);
    openssl_free_key($publicKeyResource);

    return $result === 1;
}`;
    }
    return `<?php

// SM2签名需要安装扩展或使用第三方库
// 推荐使用: composer require rtckit/smil 或者 php-sm2 扩展

function generateSm2Sign($params, $privateKeyHex) {
    ksort($params);
    $signStr = '';
    foreach ($params as $key => $value) {
        if ($value !== '' && $key !== 'sign' && $key !== 'signType') {
            $signStr .= $key . '=' . $value . '&';
        }
    }
    $signStr = rtrim($signStr, '&');

    // 使用SM2库进行签名
    // $sm2 = new SM2($privateKeyHex);
    // $signature = $sm2->sign($signStr);
    // return bin2hex($signature);

    return 'SM2_SIGNATURE_PLACEHOLDER';
}`;
  }

  if (language === 'PYTHON') {
    if (signType === 'MD5') {
      return `import hashlib
from collections import OrderedDict

def generate_sign(params: dict, md5_key: str) -> str:
    """生成MD5签名"""
    sorted_params = OrderedDict(sorted(params.items()))
    sign_str = ''
    for key, value in sorted_params.items():
        if value not in (None, '') and key not in ('sign', 'signType'):
            sign_str += f'{key}={value}&'
    sign_str += f'key={md5_key}'
    return hashlib.md5(sign_str.encode('utf-8')).hexdigest().upper()

def verify_sign(params: dict, md5_key: str, sign: str) -> bool:
    """验证MD5签名"""
    calculated = generate_sign(params, md5_key)
    return calculated.upper() == sign.upper()

# 使用示例
if __name__ == '__main__':
    params = {
        'merchantNo': 'M000001',
        'orderNo': 'PG20240101000001',
        'amount': 10000,
        'status': 'SUCCESS',
        'timestamp': 1704067200,
    }
    md5_key = 'your_md5_key_here'
    sign = generate_sign(params, md5_key)
    params['signType'] = 'MD5'
    params['sign'] = sign
    print(f"签名结果: {sign}")
    print(f"完整参数: {params}")`;
    }
    if (signType === 'RSA') {
      return `from Crypto.PublicKey import RSA
from Crypto.Signature import pkcs1_15
from Crypto.Hash import SHA256
import base64
from collections import OrderedDict

def generate_rsa_sign(params: dict, private_key_pem: str) -> str:
    """生成RSA签名"""
    sorted_params = OrderedDict(sorted(params.items()))
    sign_parts = []
    for key, value in sorted_params.items():
        if value not in (None, '') and key not in ('sign', 'signType'):
            sign_parts.append(f'{key}={value}')
    sign_str = '&'.join(sign_parts)

    key = RSA.import_key(private_key_pem)
    h = SHA256.new(sign_str.encode('utf-8'))
    signature = pkcs1_15.new(key).sign(h)
    return base64.b64encode(signature).decode('utf-8')

def verify_rsa_sign(params: dict, public_key_pem: str, sign: str) -> bool:
    """验证RSA签名"""
    sorted_params = OrderedDict(sorted(params.items()))
    sign_parts = []
    for key, value in sorted_params.items():
        if value not in (None, '') and key not in ('sign', 'signType'):
            sign_parts.append(f'{key}={value}')
    sign_str = '&'.join(sign_parts)

    key = RSA.import_key(public_key_pem)
    h = SHA256.new(sign_str.encode('utf-8'))
    try:
        pkcs1_15.new(key).verify(h, base64.b64decode(sign))
        return True
    except (ValueError, TypeError):
        return False

# 安装依赖: pip install pycryptodome`;
    }
    return `from gmssl import sm2 as sm2_module
from collections import OrderedDict

def generate_sm2_sign(params: dict, private_key_hex: str) -> str:
    """生成SM2签名"""
    sorted_params = OrderedDict(sorted(params.items()))
    sign_parts = []
    for key, value in sorted_params.items():
        if value not in (None, '') and key not in ('sign', 'signType'):
            sign_parts.append(f'{key}={value}')
    sign_str = '&'.join(sign_parts)

    sm2 = sm2_module.CryptSM2(private_key=private_key_hex, public_key='')
    signature = sm2.sign(sign_str.encode('utf-8'), private_key_hex)
    return signature.hex() if isinstance(signature, bytes) else str(signature)

# 安装依赖: pip install gmssl`;
  }

  return '// Please select a valid language and sign type combination';
}

export default CallbackSimulator;

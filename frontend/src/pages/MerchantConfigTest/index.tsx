import { useState, useMemo } from 'react';
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  Space,
  Row,
  Col,
  Tag,
  Progress,
  Descriptions,
  Divider,
  Collapse,
  Alert,
  Typography,
  Tooltip,
  Spin,
  message,
  Statistic,
} from 'antd';
import {
  RocketOutlined,
  ReloadOutlined,
  CheckCircleFilled,
  CloseCircleFilled,
  ExclamationCircleFilled,
  DashboardOutlined,
  ApiOutlined,
  SafetyCertificateOutlined,
  GlobalOutlined,
  FileTextOutlined,
  CheckOutlined,
  CloseOutlined,
  WarningOutlined,
  CopyOutlined,
} from '@ant-design/icons';
import { merchantConfigTestApi, merchantApi } from '@/api';
import type {
  MerchantConfigTestReport,
  TestItemResult,
  MerchantConfigTestRequest,
} from '@/types/merchantConfigTest';
import type { Merchant } from '@/types';
import { useEffect } from 'react';

const { Panel } = Collapse;
const { Title, Paragraph, Text } = Typography;

const statusColorMap: Record<string, string> = {
  PASS: '#52c41a',
  FAIL: '#ff4d4f',
  WARN: '#faad14',
  ERROR: '#ff4d4f',
  PENDING: '#bfbfbf',
};

const statusIconMap: Record<string, React.ReactNode> = {
  PASS: <CheckCircleFilled style={{ color: '#52c41a' }} />,
  FAIL: <CloseCircleFilled style={{ color: '#ff4d4f' }} />,
  WARN: <ExclamationCircleFilled style={{ color: '#faad14' }} />,
  ERROR: <CloseCircleFilled style={{ color: '#ff4d4f' }} />,
  PENDING: <Spin size="small" />,
};

const statusTextMap: Record<string, string> = {
  PASS: '通过',
  FAIL: '失败',
  WARN: '警告',
  ERROR: '异常',
  PENDING: '进行中',
};

const categoryIconMap: Record<string, React.ReactNode> = {
  '基础配置': <DashboardOutlined />,
  '签名配置': <SafetyCertificateOutlined />,
  '网络连通性': <GlobalOutlined />,
  '回调功能': <ApiOutlined />,
  '签名安全': <SafetyCertificateOutlined />,
};

const signTypeOptions = [
  { label: '自动选择', value: '' },
  { label: 'MD5', value: 'MD5' },
  { label: 'RSA', value: 'RSA' },
  { label: 'SM2(国密)', value: 'SM2' },
];

const MerchantConfigTest = () => {
  const [form] = Form.useForm<MerchantConfigTestRequest>();
  const [testing, setTesting] = useState(false);
  const [report, setReport] = useState<MerchantConfigTestReport | null>(null);
  const [merchants, setMerchants] = useState<Merchant[]>([]);

  useEffect(() => {
    loadMerchants();
  }, []);

  const loadMerchants = async () => {
    try {
      const result = await merchantApi.list({ current: 1, size: 100, auditStatus: 1 });
      if (result?.list?.length) {
        setMerchants(result.list);
      }
    } catch {
      const mock: Merchant[] = Array.from({ length: 10 }, (_, i) => ({
        id: i + 1,
        merchantNo: `M${String(10000 + i).padStart(6, '0')}`,
        merchantName: `示例商户${i + 1}`,
        businessLicenseNo: '91310000MA1FL' + (1000 + i),
        legalPersonName: '张三',
        contactPhone: '13800138000',
        contactEmail: `merchant${i + 1}@example.com`,
        auditStatus: 1,
        status: 1,
      }));
      setMerchants(mock);
    }
  };

  const handleRunTest = async () => {
    try {
      const values = await form.validateFields();
      setTesting(true);
      setReport(null);
      try {
        const result = await merchantConfigTestApi.runTest(values);
        setReport(result);
        message.success('测试完成');
      } catch (err: any) {
        message.error(err?.message || '测试失败');
      }
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('测试失败');
    } finally {
      setTesting(false);
    }
  };

  const groupedItems = useMemo(() => {
    if (!report) return {};
    const groups: Record<string, TestItemResult[]> = {};
    report.items.forEach((item) => {
      if (!groups[item.itemCategory]) {
        groups[item.itemCategory] = [];
      }
      groups[item.itemCategory].push(item);
    });
    return groups;
  }, [report]);

  const passRate = useMemo(() => {
    if (!report) return 0;
    return Math.round((report.passedTests / report.totalTests) * 100);
  }, [report]);

  const renderItemCard = (item: TestItemResult) => (
    <div
      key={item.itemCode}
      style={{
        padding: 16,
        borderRadius: 8,
        border: `1px solid ${statusColorMap[item.status]}40`,
        backgroundColor: `${statusColorMap[item.status]}08`,
        marginBottom: 12,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
        <div style={{ marginTop: 2, flexShrink: 0 }}>{statusIconMap[item.status]}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
            <span style={{ fontWeight: 600, fontSize: 14 }}>{item.itemName}</span>
            <Tag
              color={statusColorMap[item.status]}
              style={{ margin: 0 }}
            >
              {statusTextMap[item.status]}
            </Tag>
            <span style={{ fontSize: 12, color: '#999' }}>
              耗时: {item.durationMs || 0}ms
            </span>
          </div>
          <p style={{ margin: '0 0 8px 0', fontSize: 13, color: '#666' }}>
            {item.message}
          </p>
          {item.expectedValue && (
            <div style={{ fontSize: 12, marginBottom: 4 }}>
              <Text type="secondary">预期: </Text>
              <Text>{item.expectedValue}</Text>
            </div>
          )}
          {item.actualValue && (
            <div style={{ fontSize: 12, marginBottom: 4 }}>
              <Text type="secondary">实际: </Text>
              <Text>{item.actualValue}</Text>
            </div>
          )}
          {item.suggestion && (
            <Alert
              type="warning"
              showIcon
              message={
                <span style={{ fontSize: 12 }}>
                  <strong>建议: </strong>{item.suggestion}
                </span>
              }
              style={{ marginTop: 8, padding: '6px 12px' }}
            />
          )}
          {item.detail && (
            <Collapse
              size="small"
              style={{ marginTop: 8, background: 'transparent', border: 'none' }}
            >
              <Panel
                header={<span style={{ fontSize: 12 }}>查看详细信息</span>}
                key="detail"
              >
                <pre
                  style={{
                    fontSize: 12,
                    background: '#fff',
                    padding: 12,
                    borderRadius: 4,
                    border: '1px solid #f0f0f0',
                    maxHeight: 200,
                    overflow: 'auto',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-all',
                    margin: 0,
                  }}
                >
                  {item.detail}
                </pre>
              </Panel>
            </Collapse>
          )}
        </div>
      </div>
    </div>
  );

  const handleCopySummary = () => {
    if (report) {
      navigator.clipboard.writeText(report.summary).then(() => {
        message.success('测试摘要已复制');
      }).catch(() => {
        message.error('复制失败');
      });
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Card>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
          <RocketOutlined style={{ fontSize: 24, color: '#1677ff' }} />
          <div>
            <Title level={4} style={{ margin: 0 }}>商户配置一键测试</Title>
            <Paragraph type="secondary" style={{ margin: '4px 0 0 0' }}>
              自动检测商户配置状态，包括基本信息、签名算法、回调连通性等，输出完整测试报告
            </Paragraph>
          </div>
        </div>

        <Form
          form={form}
          layout="vertical"
          initialValues={{ signType: '' }}
        >
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item
                name="merchantNo"
                label="选择商户"
                rules={[{ required: true, message: '请选择要测试的商户' }]}
              >
                <Select
                  placeholder="请选择要测试的商户"
                  showSearch
                  optionFilterProp="label"
                  options={merchants.map((m) => ({
                    label: `${m.merchantName} (${m.merchantNo})`,
                    value: m.merchantNo,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="callbackUrl" label="回调地址（可选）">
                <Input placeholder="不填则从商户支付配置的异步通知地址(notify_url)自动获取" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item name="signType" label="签名算法（可选）">
                <Select options={signTypeOptions} placeholder="自动选择商户已配置的算法" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="testAmount" label="测试金额（可选）">
                <Input placeholder="默认 100.00 元" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item>
            <Space>
              <Button
                type="primary"
                icon={<RocketOutlined />}
                onClick={handleRunTest}
                loading={testing}
                size="large"
              >
                {testing ? '测试中...' : '开始测试'}
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  form.resetFields();
                  setReport(null);
                }}
                size="large"
              >
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {testing && (
        <Card style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin size="large" />
          <p style={{ marginTop: 16, color: '#666' }}>正在执行配置检测，请稍候...</p>
        </Card>
      )}

      {report && !testing && (
        <>
          <Card
            title={
              <Space>
                <FileTextOutlined />
                <span>测试报告</span>
                <Tag color={statusColorMap[report.overallStatus]}>
                  {report.overallStatusDesc}
                </Tag>
              </Space>
            }
            extra={
              <Button
                icon={<CopyOutlined />}
                size="small"
                onClick={handleCopySummary}
              >
                复制摘要
              </Button>
            }
          >
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={6}>
                <Statistic
                  title="测试项总数"
                  value={report.totalTests}
                  prefix={<DashboardOutlined />}
                />
              </Col>
              <Col span={6}>
                <Statistic
                  title="通过"
                  value={report.passedTests}
                  valueStyle={{ color: '#52c41a' }}
                  prefix={<CheckOutlined />}
                />
              </Col>
              <Col span={6}>
                <Statistic
                  title="失败/警告"
                  value={report.failedTests}
                  valueStyle={{ color: '#faad14' }}
                  prefix={<WarningOutlined />}
                />
              </Col>
              <Col span={6}>
                <Statistic
                  title="总耗时"
                  value={report.totalTimeMs}
                  suffix="ms"
                  precision={0}
                />
              </Col>
            </Row>

            <div style={{ marginBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span>通过率</span>
                <span style={{ fontWeight: 600, color: passRate === 100 ? '#52c41a' : '#faad14' }}>
                  {passRate}%
                </span>
              </div>
              <Progress
                percent={passRate}
                showInfo={false}
                strokeColor={passRate === 100 ? '#52c41a' : '#faad14'}
                strokeWidth={12}
              />
            </div>

            <Descriptions column={2} size="small" bordered>
              <Descriptions.Item label="商户号">
                {report.merchantNo}
              </Descriptions.Item>
              <Descriptions.Item label="商户名称">
                {report.merchantName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="测试时间">
                {report.testTime}
              </Descriptions.Item>
              <Descriptions.Item label="总耗时">
                {report.totalTimeMs}ms
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title="测试详情">
            {Object.entries(groupedItems).map(([category, items]) => (
              <div key={category} style={{ marginBottom: 20 }}>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    marginBottom: 12,
                    paddingBottom: 8,
                    borderBottom: '1px solid #f0f0f0',
                  }}
                >
                  {categoryIconMap[category] || <DashboardOutlined />}
                  <span style={{ fontWeight: 600, fontSize: 15 }}>{category}</span>
                  <Tag>
                    {items.filter((i) => i.status === 'PASS').length}/{items.length} 通过
                  </Tag>
                </div>
                {items.map(renderItemCard)}
              </div>
            ))}
          </Card>

          <Card title="测试总结">
            <Alert
              type={report.overallStatus === 'PASS' ? 'success' : report.overallStatus === 'FAIL' ? 'error' : 'warning'}
              showIcon
              message="测试摘要"
              description={
                <pre
                  style={{
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-all',
                    margin: 0,
                    background: 'transparent',
                    border: 'none',
                    padding: 0,
                    fontSize: 13,
                  }}
                >
                  {report.summary}
                </pre>
              }
            />
          </Card>
        </>
      )}
    </div>
  );
};

export default MerchantConfigTest;

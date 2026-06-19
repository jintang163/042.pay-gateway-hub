import { useState, useEffect } from 'react';
import {
  Card,
  Form,
  Select,
  Input,
  InputNumber,
  Button,
  Space,
  Steps,
  Alert,
  Result,
  Tag,
  Descriptions,
  Divider,
  message,
  Row,
  Col,
  Spin,
} from 'antd';
import {
  PlayCircleOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  ExperimentOutlined,
} from '@ant-design/icons';
import type { SandboxScene, SandboxSceneOption } from '@/types/sandbox';
import { sandboxApi } from '@/api';
import type { SandboxExecuteRequest } from '@/types/sandbox';
import type { SandboxTestResultVO } from '@/types/sandbox';

const defaultSceneOptions: { label: string; value: SandboxScene; description: string; icon: React.ReactNode }[] = [
  {
    label: '支付成功',
    value: 'success',
    description: '模拟正常支付成功流程，含回调通知',
    icon: <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 24 }} />,
  },
  {
    label: '支付失败',
    value: 'failed',
    description: '模拟支付失败，验证异常处理',
    icon: <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: 24 }} />,
  },
  {
    label: '支付超时',
    value: 'timeout',
    description: '模拟支付超时，验证订单状态处理',
    icon: <WarningOutlined style={{ color: '#faad14', fontSize: 24 }} />,
  },
  {
    label: '余额不足',
    value: 'insufficient_balance',
    description: '模拟账户余额不足场景',
    icon: <WarningOutlined style={{ color: '#fa541c', fontSize: 24 }} />,
  },
  {
    label: '重复通知',
    value: 'repeat_notify',
    description: '模拟重复异步通知，验证幂等性',
    icon: <ExperimentOutlined style={{ color: '#1677ff', fontSize: 24 }} />,
  },
  {
    label: '签名错误',
    value: 'sign_error',
    description: '模拟签名校验失败场景',
    icon: <WarningOutlined style={{ color: '#eb2f96', fontSize: 24 }} />,
  },
  {
    label: '金额不匹配',
    value: 'amount_mismatch',
    description: '模拟回调金额与订单不一致',
    icon: <WarningOutlined style={{ color: '#722ed1', fontSize: 24 }} />,
  },
  {
    label: '退款成功',
    value: 'refund_success',
    description: '模拟正常退款成功流程',
    icon: <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 24 }} />,
  },
  {
    label: '退款失败',
    value: 'refund_failed',
    description: '模拟退款失败场景',
    icon: <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: 24 }} />,
  },
  {
    label: '通道异常',
    value: 'channel_error',
    description: '模拟通道系统异常场景',
    icon: <WarningOutlined style={{ color: '#8c8c8c', fontSize: 24 }} />,
  },
];

const SandboxTest = () => {
  const [form] = Form.useForm<SandboxExecuteRequest>();
  const [executing, setExecuting] = useState(false);
  const [result, setResult] = useState<SandboxTestResultVO | null>(null);
  const [currentScene, setCurrentScene] = useState<SandboxScene | null>(null);
  const [sceneOptions, setSceneOptions] = useState(defaultSceneOptions);
  const [loadingScenes, setLoadingScenes] = useState(false);

  useEffect(() => {
    loadScenes();
  }, []);

  const loadScenes = async () => {
    setLoadingScenes(true);
    try {
      const data = await sandboxApi.getScenes();
      if (data && data.length > 0) {
        const merged = defaultSceneOptions.map((opt) => {
          const remote = data.find((s: SandboxSceneOption) => s.code === opt.value);
          return {
            ...opt,
            label: remote?.name || opt.label,
            description: remote?.description || opt.description,
          };
        });
        setSceneOptions(merged);
      }
    } catch {
      // use default
    } finally {
      setLoadingScenes(false);
    }
  };

  const handleExecute = async () => {
    try {
      const values = await form.validateFields();
      setExecuting(true);
      setResult(null);
      setCurrentScene(values.testScene as SandboxScene);

      const data = await sandboxApi.execute(values);
      setResult(data);
      if (data.success) {
        message.success('测试通过');
      } else {
        message.warning('测试未通过：' + (data.errorMsg || '预期结果与实际不符'));
      }
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('测试执行失败：' + (error?.message || '未知错误'));
    } finally {
      setExecuting(false);
    }
  };

  const handleReset = () => {
    form.resetFields();
    setResult(null);
    setCurrentScene(null);
  };

  const renderResult = () => {
    if (!result) return null;

    const currentSceneInfo = sceneOptions.find((s) => s.value === result.testScene);

    return (
      <Card title="测试结果" style={{ marginTop: 24 }}>
        <Result
          status={result.success ? 'success' : 'error'}
          title={result.success ? '测试通过' : '测试未通过'}
          subTitle={result.errorMsg || `${currentSceneInfo?.label || result.testScene} 场景测试完成`}
        />

        <Descriptions bordered column={2} size="small" style={{ marginTop: 16 }}>
          <Descriptions.Item label="测试ID">{result.testId}</Descriptions.Item>
          <Descriptions.Item label="测试场景">{currentSceneInfo?.label || result.testScene}</Descriptions.Item>
          <Descriptions.Item label="商户号">{result.merchantNo}</Descriptions.Item>
          <Descriptions.Item label="支付通道">{result.payChannel}</Descriptions.Item>
          <Descriptions.Item label="支付方式">{result.payType}</Descriptions.Item>
          <Descriptions.Item label="支付金额">{result.payAmount} 元</Descriptions.Item>
          <Descriptions.Item label="预期结果">
            <Tag color={result.expectResult === 1 ? 'green' : 'red'}>
              {result.expectResult === 1 ? '成功' : '失败'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="实际结果">
            <Tag color={result.actualResult === 1 ? 'green' : 'red'}>
              {result.actualResult === 1 ? '成功' : '失败'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="执行耗时">{result.costTime}ms</Descriptions.Item>
          <Descriptions.Item label="通知结果">
            {result.notifyResult ? <Tag color="blue">{result.notifyResult}</Tag> : <Tag>无通知</Tag>}
          </Descriptions.Item>
        </Descriptions>

        {result.responseData && (
          <>
            <Divider />
            <Row gutter={24}>
              <Col span={24}>
                <h4>响应数据</h4>
                <pre
                  style={{
                    background: '#f5f5f5',
                    padding: 12,
                    borderRadius: 4,
                    fontSize: 12,
                    maxHeight: 300,
                    overflow: 'auto',
                  }}
                >
                  {typeof result.responseData === 'string'
                    ? JSON.stringify(JSON.parse(result.responseData), null, 2)
                    : JSON.stringify(result.responseData, null, 2)}
                </pre>
              </Col>
            </Row>
          </>
        )}
      </Card>
    );
  };

  return (
    <div>
      <Alert
        type="info"
        message="沙箱测试说明"
        description="沙箱环境与生产完全隔离，所有支付请求走模拟通道，不会产生真实扣款。选择测试场景，填写参数后执行测试。"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Card title="测试场景选择" style={{ marginBottom: 16 }}>
        <Spin spinning={loadingScenes}>
          <Row gutter={[16, 16]}>
            {sceneOptions.map((scene) => (
              <Col xs={24} sm={12} lg={8} xl={6} key={scene.value}>
                <Card
                  hoverable
                  size="small"
                  onClick={() => {
                    form.setFieldsValue({ testScene: scene.value });
                    setCurrentScene(scene.value);
                  }}
                  bordered={currentScene === scene.value}
                  style={{
                    borderColor: currentScene === scene.value ? '#1677ff' : '#f0f0f0',
                    borderWidth: currentScene === scene.value ? 2 : 1,
                  }}
                >
                  <Space align="start">
                    {scene.icon}
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600, marginBottom: 4 }}>{scene.label}</div>
                      <div style={{ fontSize: 12, color: '#888' }}>{scene.description}</div>
                    </div>
                  </Space>
                </Card>
              </Col>
            ))}
          </Row>
        </Spin>
      </Card>

      <Card title="测试参数">
        <Form
          form={form}
          layout="vertical"
          onFinish={handleExecute}
          initialValues={{ testScene: 'success', payChannel: 'ALIPAY', payType: 'NATIVE', payAmount: 100, merchantNo: 'M000001' }}
        >
          <Form.Item
            name="testScene"
            label="测试场景"
            rules={[{ required: true, message: '请选择测试场景' }]}
          >
            <Select
              placeholder="请选择测试场景"
              options={sceneOptions.map((s) => ({ label: s.label, value: s.value }))}
              onChange={(val) => setCurrentScene(val)}
            />
          </Form.Item>

          <Row gutter={16}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="merchantNo"
                label="商户号"
                rules={[{ required: true, message: '请输入商户号' }]}
              >
                <Input placeholder="沙箱测试商户号，如：M000001" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="testName"
                label="测试名称"
                rules={[{ required: true, message: '请输入测试名称' }]}
              >
                <Input placeholder="如：支付成功场景测试" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} sm={8}>
              <Form.Item
                name="payChannel"
                label="支付通道"
                rules={[{ required: true, message: '请选择支付通道' }]}
              >
                <Select
                  placeholder="请选择支付通道"
                  options={[
                    { label: '支付宝', value: 'ALIPAY' },
                    { label: '微信支付', value: 'WECHAT_PAY' },
                    { label: '银联支付', value: 'UNION_PAY' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item
                name="payType"
                label="支付方式"
                rules={[{ required: true, message: '请选择支付方式' }]}
              >
                <Select
                  placeholder="请选择支付方式"
                  options={[
                    { label: '扫码支付', value: 'NATIVE' },
                    { label: 'H5支付', value: 'H5' },
                    { label: '公众号支付', value: 'JSAPI' },
                    { label: 'APP支付', value: 'APP' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item
                name="payAmount"
                label="支付金额(元)"
                rules={[{ required: true, message: '请输入支付金额' }]}
              >
                <InputNumber min={0.01} step={0.01} style={{ width: '100%' }} placeholder="请输入支付金额" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="notifyUrl" label="异步通知地址">
            <Input placeholder="https://example.com/api/notify（可选）" />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<PlayCircleOutlined />} loading={executing}>
                执行测试
              </Button>
              <Button onClick={handleReset} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {renderResult()}
    </div>
  );
};

export default SandboxTest;

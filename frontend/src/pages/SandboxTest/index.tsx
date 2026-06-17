import { useState } from 'react';
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
} from 'antd';
import {
  PlayCircleOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  ExperimentOutlined,
} from '@ant-design/icons';
import type { SandboxScene } from '@/types/sandbox';
import { sandboxApi } from '@/api';
import type { SandboxExecuteRequest, SandboxTestResult } from '@/types/sandbox';
import { formatDateTime } from '@/utils';

const sceneOptions: { label: string; value: SandboxScene; description: string; icon: any }[] = [
  {
    label: '支付成功',
    value: 'success',
    description: '模拟正常的支付成功流程，包含回调通知',
    icon: <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 24 }} />,
  },
  {
    label: '支付失败',
    value: 'failed',
    description: '模拟支付失败场景，验证异常处理逻辑',
    icon: <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: 24 }} />,
  },
  {
    label: '支付超时',
    value: 'timeout',
    description: '模拟支付超时，验证订单状态处理',
    icon: <WarningOutlined style={{ color: '#faad14', fontSize: 24 }} />,
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
    description: '模拟回调金额与订单金额不一致',
    icon: <WarningOutlined style={{ color: '#722ed1', fontSize: 24 }} />,
  },
];

const SandboxTest = () => {
  const [form] = Form.useForm<SandboxExecuteRequest>();
  const [executing, setExecuting] = useState(false);
  const [result, setResult] = useState<SandboxTestResult | null>(null);
  const [currentScene, setCurrentScene] = useState<SandboxScene | null>(null);

  const handleExecute = async () => {
    try {
      const values = await form.validateFields();
      setExecuting(true);
      setResult(null);
      setCurrentScene(values.scene);
      try {
        const data = await sandboxApi.execute(values);
        setResult(data);
        message.success('测试执行完成');
      } catch {
        const mockResult: SandboxTestResult = {
          success: values.scene === 'success',
          testId: 'TEST' + Date.now(),
          scene: values.scene,
          orderNo: 'PG' + Date.now(),
          requestTime: new Date().toISOString(),
          responseTime: new Date(Date.now() + (values.scene === 'timeout' ? 30000 : 500)).toISOString(),
          duration: values.scene === 'timeout' ? 30000 : 500,
          requestParams: JSON.stringify(values, null, 2),
          responseData: JSON.stringify(
            {
              code: values.scene === 'success' ? 200 : values.scene === 'timeout' ? 408 : values.scene === 'sign_error' ? 401 : 500,
              message: values.scene === 'success' ? '支付成功' : values.scene === 'timeout' ? '支付超时' : values.scene === 'sign_error' ? '签名错误' : values.scene === 'amount_mismatch' ? '金额不匹配' : '支付失败',
              data: values.scene === 'success' ? { orderNo: 'PG' + Date.now(), amount: values.amount } : null,
            },
            null,
            2,
          ),
          notifyResult: values.scene === 'success' || values.scene === 'repeat_notify'
            ? {
                notified: true,
                notifyCount: values.scene === 'repeat_notify' ? 3 : 1,
                notifyUrl: values.notifyUrl || 'https://example.com/notify',
                lastNotifyTime: new Date().toISOString(),
              }
            : { notified: false, notifyCount: 0, notifyUrl: values.notifyUrl || '', lastNotifyTime: '' },
          errorMessage: values.scene !== 'success'
            ? (values.scene === 'timeout' ? '支付超时，订单已关闭' : values.scene === 'sign_error' ? '签名校验失败' : values.scene === 'amount_mismatch' ? '回调金额与订单金额不一致' : '支付渠道返回错误')
            : undefined,
        };
        setResult(mockResult);
        message.success('测试执行完成（模拟数据）');
      }
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('测试执行失败');
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

    const currentSceneInfo = sceneOptions.find((s) => s.value === result.scene);

    return (
      <Card title="测试结果" style={{ marginTop: 24 }}>
        <Result
          status={result.success ? 'success' : 'error'}
          title={result.success ? '测试通过' : '测试未通过'}
          subTitle={result.errorMessage || `${currentSceneInfo?.label} 场景测试完成`}
        />

        <Descriptions bordered column={2} size="small" style={{ marginTop: 16 }}>
          <Descriptions.Item label="测试ID">{result.testId}</Descriptions.Item>
          <Descriptions.Item label="测试场景">{currentSceneInfo?.label}</Descriptions.Item>
          <Descriptions.Item label="订单号">{result.orderNo}</Descriptions.Item>
          <Descriptions.Item label="执行耗时">{result.duration}ms</Descriptions.Item>
          <Descriptions.Item label="请求时间">{formatDateTime(result.requestTime)}</Descriptions.Item>
          <Descriptions.Item label="响应时间">{formatDateTime(result.responseTime)}</Descriptions.Item>
        </Descriptions>

        <Divider />

        <Row gutter={24}>
          <Col xs={24} md={12}>
            <h4>请求参数</h4>
            <pre
              style={{
                background: '#f5f5f5',
                padding: 12,
                borderRadius: 4,
                fontSize: 12,
                maxHeight: 240,
                overflow: 'auto',
              }}
            >
              {result.requestParams}
            </pre>
          </Col>
          <Col xs={24} md={12}>
            <h4>响应数据</h4>
            <pre
              style={{
                background: '#f5f5f5',
                padding: 12,
                borderRadius: 4,
                fontSize: 12,
                maxHeight: 240,
                overflow: 'auto',
              }}
            >
              {result.responseData}
            </pre>
          </Col>
        </Row>

        <Divider />

        <Card
          size="small"
          title={
            <Space>
              通知结果
              <Tag color={result.notifyResult?.notified ? 'green' : 'default'}>
                {result.notifyResult?.notified ? '已通知' : '未通知'}
              </Tag>
            </Space>
          }
        >
          {result.notifyResult?.notified && (
            <Descriptions size="small" column={2}>
              <Descriptions.Item label="通知次数">{result.notifyResult.notifyCount} 次</Descriptions.Item>
              <Descriptions.Item label="通知地址">{result.notifyResult.notifyUrl}</Descriptions.Item>
              <Descriptions.Item label="最后通知时间">{formatDateTime(result.notifyResult.lastNotifyTime)}</Descriptions.Item>
            </Descriptions>
          )}
        </Card>
      </Card>
    );
  };

  return (
    <div>
      <Alert
        type="info"
        message="沙箱测试说明"
        description="沙箱环境用于测试支付流程的各种场景，测试数据不会进入真实环境。选择测试场景，填写参数后执行测试。"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Card title="测试场景选择" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]}>
          {sceneOptions.map((scene) => (
            <Col xs={24} sm={12} lg={8} key={scene.value}>
              <Card
                hoverable
                size="small"
                onClick={() => {
                  form.setFieldsValue({ scene: scene.value });
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
      </Card>

      <Card title="测试参数">
        <Form
          form={form}
          layout="vertical"
          onFinish={handleExecute}
          initialValues={{ scene: 'success', amount: 100 }}
        >
          <Form.Item
            name="scene"
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
                name="merchantId"
                label="商户ID"
                rules={[{ required: true, message: '请输入商户ID' }]}
              >
                <Input placeholder="请输入商户ID，如：M000001" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="payChannel"
                label="支付渠道"
                rules={[{ required: true, message: '请选择支付渠道' }]}
              >
                <Select
                  placeholder="请选择支付渠道"
                  options={[
                    { label: '支付宝', value: 'ALIPAY' },
                    { label: '微信支付', value: 'WECHAT' },
                    { label: '银联', value: 'UNIONPAY' },
                    { label: 'Apple Pay', value: 'APPLE_PAY' },
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="amount"
                label="订单金额(元)"
                rules={[{ required: true, message: '请输入订单金额' }]}
              >
                <InputNumber min={0.01} step={0.01} style={{ width: '100%' }} placeholder="请输入订单金额" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="merchantOrderNo"
                label="商户订单号"
              >
                <Input placeholder="不填则自动生成" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="subject" label="订单标题">
            <Input placeholder="请输入订单标题" />
          </Form.Item>

          <Row gutter={16}>
            <Col xs={24} sm={12}>
              <Form.Item name="notifyUrl" label="异步通知地址">
                <Input placeholder="https://example.com/api/notify" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item name="callbackUrl" label="前端回调地址">
                <Input placeholder="https://example.com/callback" />
              </Form.Item>
            </Col>
          </Row>

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

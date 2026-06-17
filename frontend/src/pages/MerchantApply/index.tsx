import { useState } from 'react';
import {
  Form,
  Input,
  Button,
  Card,
  Row,
  Col,
  InputNumber,
  message,
  Steps,
  Result,
  Alert,
} from 'antd';
import { ShopOutlined, TeamOutlined, BankOutlined } from '@ant-design/icons';
import { merchantApi } from '@/api';
import type { MerchantApplyRequest } from '@/types/merchant';

const MerchantApply = () => {
  const [form] = Form.useForm<MerchantApplyRequest>();
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);

  const steps = [
    { title: '基本信息', icon: <ShopOutlined /> },
    { title: '法人信息', icon: <TeamOutlined /> },
    { title: '结算信息', icon: <BankOutlined /> },
  ];

  const handleSubmit = async (values: MerchantApplyRequest) => {
    try {
      setSubmitting(true);
      await merchantApi.apply(values);
      setSubmitted(true);
      message.success('商户入驻申请提交成功，请等待审核');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleNext = async () => {
    try {
      if (currentStep === 0) {
        await form.validateFields([
          'merchantName',
          'shortName',
          'businessLicense',
          'contactPhone',
          'contactEmail',
          'address',
        ]);
      } else if (currentStep === 1) {
        await form.validateFields(['legalPerson', 'legalPersonIdCard']);
      }
      setCurrentStep(currentStep + 1);
    } catch {
      // validation failed, stay on current step
    }
  };

  const handlePrev = () => {
    setCurrentStep(currentStep - 1);
  };

  const handleReset = () => {
    form.resetFields();
    setSubmitted(false);
    setCurrentStep(0);
  };

  if (submitted) {
    return (
      <Card>
        <Result
          status="success"
          title="商户入驻申请提交成功"
          subTitle="我们将在1-3个工作日内完成审核，审核结果将通过邮件通知您。"
          extra={[
            <Button type="primary" onClick={handleReset}>
              继续申请新商户
            </Button>,
          ]}
        />
      </Card>
    );
  }

  return (
    <div>
      <Card>
        <Alert
          type="info"
          message="入驻须知"
          description="请如实填写商户信息，提交虚假信息将导致审核不通过。所有信息我们将严格保密。"
          showIcon
          style={{ marginBottom: 24 }}
        />

        <Steps current={currentStep} items={steps} style={{ marginBottom: 32 }} />

        <Form
          form={form}
          layout="vertical"
          name="merchant_apply"
          onFinish={handleSubmit}
          requiredMark="optional"
          style={{ maxWidth: 800, margin: '0 auto' }}
        >
          {currentStep === 0 && (
            <>
              <Form.Item
                name="merchantName"
                label="商户名称"
                rules={[
                  { required: true, message: '请输入商户名称' },
                  { min: 2, max: 100, message: '商户名称长度在2-100个字符之间' },
                ]}
              >
                <Input placeholder="请输入营业执照上的完整商户名称" />
              </Form.Item>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="shortName" label="商户简称">
                    <Input placeholder="请输入商户简称（选填）" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    name="businessLicense"
                    label="营业执照号"
                    rules={[
                      { required: true, message: '请输入营业执照号' },
                      { pattern: /^[0-9A-Z]{15,18}$/, message: '营业执照号格式不正确' },
                    ]}
                  >
                    <Input placeholder="请输入15或18位营业执照号" />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="contactPhone"
                    label="联系电话"
                    rules={[
                      { required: true, message: '请输入联系电话' },
                      { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
                    ]}
                  >
                    <Input placeholder="请输入手机号码" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    name="contactEmail"
                    label="联系邮箱"
                    rules={[
                      { required: true, message: '请输入联系邮箱' },
                      { type: 'email', message: '邮箱格式不正确' },
                    ]}
                  >
                    <Input placeholder="请输入邮箱地址" />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item name="address" label="经营地址">
                <Input.TextArea rows={3} placeholder="请输入详细经营地址" />
              </Form.Item>
            </>
          )}

          {currentStep === 1 && (
            <>
              <Form.Item
                name="legalPerson"
                label="法人姓名"
                rules={[
                  { required: true, message: '请输入法人姓名' },
                  { min: 2, max: 20, message: '姓名长度在2-20个字符之间' },
                ]}
              >
                <Input placeholder="请输入营业执照上的法人姓名" />
              </Form.Item>

              <Form.Item
                name="legalPersonIdCard"
                label="法人身份证号"
                rules={[
                  { required: true, message: '请输入法人身份证号' },
                  { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确' },
                ]}
              >
                <Input placeholder="请输入18位身份证号" maxLength={18} />
              </Form.Item>
            </>
          )}

          {currentStep === 2 && (
            <>
              <Form.Item
                name="settleBankName"
                label="结算银行名称"
                rules={[
                  { required: true, message: '请输入结算银行名称' },
                  { min: 2, max: 50, message: '银行名称长度在2-50个字符之间' },
                ]}
              >
                <Input placeholder="如：中国工商银行北京市分行营业部" />
              </Form.Item>

              <Form.Item
                name="settleBankAccount"
                label="结算银行账号"
                rules={[
                  { required: true, message: '请输入结算银行账号' },
                  { pattern: /^\d{12,30}$/, message: '银行账号格式不正确' },
                ]}
              >
                <Input placeholder="请输入银行卡号" maxLength={30} />
              </Form.Item>

              <Form.Item
                name="settleAccountName"
                label="结算账户名称"
                rules={[
                  { required: true, message: '请输入结算账户名称' },
                  { min: 2, max: 50, message: '账户名称长度在2-50个字符之间' },
                ]}
              >
                <Input placeholder="需与商户名称或法人姓名一致" />
              </Form.Item>

              <Form.Item
                label="结算费率"
                tooltip="商户入驻后由管理员配置"
              >
                <InputNumber
                  style={{ width: 200 }}
                  disabled
                  min={0}
                  max={10}
                  step={0.01}
                  placeholder="管理员配置"
                  addonAfter="%"
                />
              </Form.Item>
            </>
          )}

          <Form.Item style={{ marginTop: 32 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <Button disabled={currentStep === 0} onClick={handlePrev}>
                上一步
              </Button>
              {currentStep < steps.length - 1 ? (
                <Button type="primary" onClick={handleNext}>
                  下一步
                </Button>
              ) : (
                <Button type="primary" htmlType="submit" loading={submitting}>
                  提交申请
                </Button>
              )}
            </div>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default MerchantApply;

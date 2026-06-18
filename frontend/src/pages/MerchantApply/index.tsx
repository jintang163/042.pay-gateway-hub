import { useState, useEffect, useCallback } from 'react';
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
  Tag,
  Descriptions,
  Statistic,
  Space,
  Typography,
  Spin,
} from 'antd';
import {
  ShopOutlined,
  TeamOutlined,
  BankOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  SafetyOutlined,
  ReloadOutlined,
  FileSearchOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { merchantApi } from '@/api';
import type { MerchantApplyRequest, AuditProgress, AuditStepItem, RiskLevel } from '@/types/merchant';

const { Title, Text, Paragraph } = Typography;

const MerchantApply = () => {
  const [form] = Form.useForm<MerchantApplyRequest>();
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);
  const [merchantNo, setMerchantNo] = useState<string | null>(null);
  const [auditProgress, setAuditProgress] = useState<AuditProgress | null>(null);
  const [loadingProgress, setLoadingProgress] = useState(false);

  const formSteps = [
    { title: '基本信息', icon: <ShopOutlined /> },
    { title: '法人信息', icon: <TeamOutlined /> },
    { title: '结算信息', icon: <BankOutlined /> },
  ];

  const getRiskLevelColor = (level?: RiskLevel) => {
    switch (level) {
      case 'LOW':
        return 'success';
      case 'MEDIUM':
        return 'warning';
      case 'HIGH':
        return 'error';
      default:
        return 'default';
    }
  };

  const getRiskLevelText = (level?: RiskLevel) => {
    switch (level) {
      case 'LOW':
        return '低风险';
      case 'MEDIUM':
        return '中风险';
      case 'HIGH':
        return '高风险';
      default:
        return '评估中';
    }
  };

  const getStepIcon = (status: string) => {
    switch (status) {
      case 'done':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
      case 'active':
        return <ClockCircleOutlined style={{ color: '#1890ff' }} spin />;
      default:
        return <ClockCircleOutlined style={{ color: '#d9d9d9' }} />;
    }
  };

  const fetchAuditProgress = useCallback(async () => {
    if (!merchantNo) return;
    try {
      setLoadingProgress(true);
      const progress = await merchantApi.getAuditProgress(merchantNo);
      setAuditProgress(progress);

      if (progress.auditStatus === 1 || progress.auditStatus === 2) {
        message.info(
          progress.auditStatus === 1 ? '恭喜！您的商户审核已通过' : '很遗憾，您的商户审核未通过'
        );
        return true;
      }
      return false;
    } catch (error) {
      message.error('获取审核进度失败');
      return false;
    } finally {
      setLoadingProgress(false);
    }
  }, [merchantNo]);

  useEffect(() => {
    if (merchantNo && submitted) {
      fetchAuditProgress();

      const interval = setInterval(async () => {
        const finished = await fetchAuditProgress();
        if (finished) {
          clearInterval(interval);
        }
      }, 3000);

      return () => clearInterval(interval);
    }
  }, [merchantNo, submitted, fetchAuditProgress]);

  const handleSubmit = async (values: MerchantApplyRequest) => {
    try {
      setSubmitting(true);
      const requestData = {
        merchantName: values.merchantName,
        businessLicenseNo: values.businessLicense,
        legalPersonName: values.legalPerson,
        legalPersonIdNo: values.legalPersonIdCard,
        contactPhone: values.contactPhone,
        contactEmail: values.contactEmail,
        settlementBankName: values.settleBankName,
        settlementBankAccount: values.settleBankAccount,
        settlementAccountName: values.settleAccountName,
      };
      const result = await merchantApi.apply(requestData);
      const mchNo = result?.merchantNo;
      if (mchNo) {
        setMerchantNo(mchNo);
      }
      setSubmitted(true);
      message.success('商户入驻申请提交成功，正在进行自动审核...');
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
    setMerchantNo(null);
    setAuditProgress(null);
  };

  const renderAuditSteps = (steps: AuditStepItem[]) => {
    return (
      <Steps
        direction="vertical"
        current={steps.findIndex((s) => s.status === 'active')}
        items={steps.map((step) => ({
          title: step.name,
          description: (
            <div>
              <div style={{ color: '#666', fontSize: 13 }}>{step.description}</div>
              {step.time && <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>{step.time}</div>}
              {step.remark && (
                <div style={{ color: '#1890ff', fontSize: 12, marginTop: 4, fontWeight: 500 }}>
                  {step.remark}
                </div>
              )}
            </div>
          ),
          icon: getStepIcon(step.status),
        }))}
        style={{ marginTop: 16 }}
      />
    );
  };

  if (submitted) {
    const isApproved = auditProgress?.auditStatus === 1;
    const isRejected = auditProgress?.auditStatus === 2;
    const isHighRisk = auditProgress?.riskLevel === 'HIGH';

    return (
      <div>
        <Card>
          {isApproved && (
            <Result
              status="success"
              title="商户入驻审核通过"
              subTitle={`商户编号: ${merchantNo}`}
              extra={[
                <Button type="primary" onClick={handleReset}>
                  继续申请新商户
                </Button>,
              ]}
            />
          )}

          {isRejected && (
            <Result
              status="error"
              title="商户入驻审核未通过"
              subTitle={auditProgress?.auditRemark || '请检查您的申请资料'}
              extra={[
                <Button type="primary" onClick={handleReset}>
                  重新申请
                </Button>,
              ]}
            />
          )}

          {!isApproved && !isRejected && (
            <Result
              icon={<FileSearchOutlined style={{ color: '#1890ff', fontSize: 72 }} />}
              title="正在审核中"
              subTitle="系统正在自动核验您的信息，请稍候..."
              extra={
                <Space>
                  <Button icon={<ReloadOutlined spin={loadingProgress} />} onClick={fetchAuditProgress}>
                    刷新进度
                  </Button>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    每3秒自动刷新
                  </Text>
                </Space>
              }
            />
          )}

          {auditProgress && (
            <div style={{ marginTop: 24 }}>
              <Row gutter={24}>
                <Col xs={24} sm={12} md={6}>
                  <Card>
                    <Statistic
                      title="商户编号"
                      value={auditProgress.merchantNo}
                      valueStyle={{ fontSize: 16 }}
                      prefix={<UserOutlined />}
                    />
                  </Card>
                </Col>
                <Col xs={24} sm={12} md={6}>
                  <Card>
                    <Statistic
                      title="审核状态"
                      value={auditProgress.auditStatusDesc}
                      valueStyle={{ fontSize: 16, color: isApproved ? '#52c41a' : isRejected ? '#f5222d' : '#1890ff' }}
                      prefix={
                        isApproved ? (
                          <CheckCircleOutlined />
                        ) : isRejected ? (
                          <CloseCircleOutlined />
                        ) : (
                          <ClockCircleOutlined />
                        )
                      }
                    />
                  </Card>
                </Col>
                <Col xs={24} sm={12} md={6}>
                  <Card>
                    <Statistic
                      title="风险等级"
                      value={getRiskLevelText(auditProgress.riskLevel)}
                      valueStyle={{ fontSize: 16 }}
                      prefix={<SafetyOutlined />}
                      suffix={
                        auditProgress.riskScore !== undefined && (
                          <Tag color={getRiskLevelColor(auditProgress.riskLevel)} style={{ marginLeft: 8 }}>
                            {auditProgress.riskScore}分
                          </Tag>
                        )
                      }
                    />
                  </Card>
                </Col>
                <Col xs={24} sm={12} md={6}>
                  <Card>
                    <Statistic
                      title="当前步骤"
                      value={auditProgress.auditStepName || '-'}
                      valueStyle={{ fontSize: 16 }}
                      prefix={<ExclamationCircleOutlined />}
                    />
                  </Card>
                </Col>
              </Row>

              {isHighRisk && (
                <Alert
                  type="warning"
                  message="高风险商户"
                  description="您的商户风险等级为高风险，已转人工审核，我们将在1-2个工作日内完成审核。"
                  showIcon
                  style={{ marginTop: 24 }}
                />
              )}

              <Row gutter={24} style={{ marginTop: 24 }}>
                <Col xs={24} lg={12}>
                  <Card title="审核进度" bordered={false}>
                    <Spin spinning={loadingProgress}>
                      {renderAuditSteps(auditProgress.steps)}
                    </Spin>
                  </Card>
                </Col>
                <Col xs={24} lg={12}>
                  <Card title="审核详情" bordered={false}>
                    <Descriptions column={1} size="small">
                      <Descriptions.Item label="商户名称">{auditProgress.merchantName}</Descriptions.Item>
                      <Descriptions.Item label="审核步骤">{auditProgress.auditStepName}</Descriptions.Item>
                      <Descriptions.Item label="步骤说明">
                        {auditProgress.auditStepDescription}
                      </Descriptions.Item>
                      <Descriptions.Item label="工商核验">
                        {auditProgress.businessVerifyPassed === 1 ? (
                          <Tag color="success">核验通过</Tag>
                        ) : auditProgress.businessVerifyPassed === 0 ? (
                          <Tag color="error">核验未通过</Tag>
                        ) : (
                          <Tag color="default">核验中</Tag>
                        )}
                      </Descriptions.Item>
                      <Descriptions.Item label="风险评分">
                        {auditProgress.riskScore !== undefined ? (
                          <span>
                            {auditProgress.riskScore}分
                            <Tag color={getRiskLevelColor(auditProgress.riskLevel)} style={{ marginLeft: 8 }}>
                              {getRiskLevelText(auditProgress.riskLevel)}
                            </Tag>
                          </span>
                        ) : (
                          <Text type="secondary">评估中</Text>
                        )}
                      </Descriptions.Item>
                      <Descriptions.Item label="自动审核">
                        {auditProgress.autoAuditPassed === 1 ? (
                          <Tag color="success">自动通过</Tag>
                        ) : auditProgress.autoAuditPassed === 0 ? (
                          <Tag color="warning">转人工</Tag>
                        ) : (
                          <Tag color="default">审核中</Tag>
                        )}
                      </Descriptions.Item>
                      {auditProgress.autoAuditRemark && (
                        <Descriptions.Item label="审核备注">
                          {auditProgress.autoAuditRemark}
                        </Descriptions.Item>
                      )}
                      {auditProgress.businessVerifyTime && (
                        <Descriptions.Item label="工商核验时间">
                          {auditProgress.businessVerifyTime}
                        </Descriptions.Item>
                      )}
                      {auditProgress.autoAuditTime && (
                        <Descriptions.Item label="自动审核时间">
                          {auditProgress.autoAuditTime}
                        </Descriptions.Item>
                      )}
                      {auditProgress.manualAuditUser && (
                        <Descriptions.Item label="人工审核人">
                          {auditProgress.manualAuditUser}
                        </Descriptions.Item>
                      )}
                      {auditProgress.manualAuditTime && (
                        <Descriptions.Item label="人工审核时间">
                          {auditProgress.manualAuditTime}
                        </Descriptions.Item>
                      )}
                    </Descriptions>
                  </Card>
                </Col>
              </Row>
            </div>
          )}
        </Card>
      </div>
    );
  }

  return (
    <div>
      <Card>
        <Alert
          type="info"
          message="入驻须知"
          description="请如实填写商户信息，系统将自动核验营业执照真伪。低风险商户自动通过审核，高风险商户将转人工审核。所有信息我们将严格保密。"
          showIcon
          style={{ marginBottom: 24 }}
        />

        <Steps current={currentStep} items={formSteps} style={{ marginBottom: 32 }} />

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
                    extra="系统将自动对接工商数据库核验营业执照真伪"
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
              <Alert
                type="info"
                message="隐私保护"
                description="法人身份证号将使用国密SM4算法加密存储，仅用于工商核验，绝不外泄。"
                showIcon
                style={{ marginBottom: 24 }}
              />
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

              <Form.Item label="结算费率" tooltip="商户入驻后由管理员配置">
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

              <Alert
                type="warning"
                message="提交须知"
                description="点击提交后，系统将自动对接工商信息数据库核验您的营业执照信息。核验通过后将进行风险评估，低风险商户自动开通。"
                showIcon
                style={{ marginTop: 24 }}
              />
            </>
          )}

          <Form.Item style={{ marginTop: 32 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <Button disabled={currentStep === 0} onClick={handlePrev}>
                上一步
              </Button>
              {currentStep < formSteps.length - 1 ? (
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

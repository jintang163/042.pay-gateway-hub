import { useState, useEffect, useMemo } from 'react';
import {
  Card,
  Form,
  Input,
  Select,
  Upload,
  Button,
  message,
  Tabs,
  Row,
  Col,
  Divider,
  Switch,
  InputNumber,
  Space,
  Tag,
  Tooltip,
  CopyToClipboard,
  Alert,
  Radio,
} from 'antd';
import {
  SaveOutlined,
  EyeOutlined,
  PhoneOutlined,
  DesktopOutlined,
  UploadOutlined,
  CopyOutlined,
  LinkOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { merchantApi, paymentPageApi, paymentTemplates } from '@/api';
import { PaymentPagePreview } from '@/components';
import type { PaymentPageConfig, Merchant } from '@/types';
import type { RcFile } from 'antd/es/upload';

const { TextArea } = Input;
const { TabPane } = Tabs;

const presetColors = [
  '#1677ff', '#00b894', '#e94560', '#f39c12', '#6c5ce7', '#27ae60',
  '#e74c3c', '#3498db', '#1abc9c', '#9b59b6', '#1a1a2e', '#000000',
  '#ffffff', '#f5f7fa', '#f0edfc', '#fdf6e3',
];

const PaymentPageEditor = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [merchants, setMerchants] = useState<Merchant[]>([]);
  const [selectedMerchant, setSelectedMerchant] = useState<string>('');
  const [device, setDevice] = useState<'mobile' | 'desktop'>('mobile');
  const [savedConfig, setSavedConfig] = useState<PaymentPageConfig | null>(null);
  const [previewAmount, setPreviewAmount] = useState(299);
  const [previewProduct, setPreviewProduct] = useState('示例商品 - VIP会员年卡');

  const configValues = Form.useWatch([], form);

  const loadMerchants = async () => {
    try {
      setLoading(true);
      const result = await merchantApi.list({ current: 1, size: 100, auditStatus: 1 });
      if (result?.list?.length) {
        setMerchants(result.list);
        if (!selectedMerchant) {
          const first = result.list[0];
          setSelectedMerchant(first.merchantNo);
          form.setFieldValue('merchantNo', first.merchantNo);
          form.setFieldValue('merchantName', first.merchantName);
          loadConfig(first.merchantNo);
        }
      }
    } catch {
      const mockMerchants: Merchant[] = Array.from({ length: 5 }, (_, i) => ({
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
      setMerchants(mockMerchants);
      if (!selectedMerchant && mockMerchants.length) {
        const first = mockMerchants[0];
        setSelectedMerchant(first.merchantNo);
        form.setFieldsValue({
          merchantNo: first.merchantNo,
          merchantName: first.merchantName,
          pageTitle: first.merchantName + ' - 收银台',
          primaryColor: '#1677ff',
          backgroundColor: '#f5f7fa',
          textColor: '#333333',
          buttonColor: '#1677ff',
          buttonTextColor: '#ffffff',
          colorSchemeCode: 'DEFAULT',
          status: 1,
        });
      }
    } finally {
      setLoading(false);
    }
  };

  const loadConfig = async (merchantNo: string) => {
    try {
      setLoading(true);
      const config = await paymentPageApi.getByMerchantNo(merchantNo);
      if (config) {
        setSavedConfig(config);
        form.setFieldsValue({
          ...config,
        });
      }
    } catch {
      const merchant = merchants.find((m) => m.merchantNo === merchantNo);
      form.setFieldsValue({
        merchantNo,
        merchantName: merchant?.merchantName,
        pageTitle: merchant?.merchantName ? `${merchant.merchantName} - 收银台` : '收银台',
        primaryColor: '#1677ff',
        backgroundColor: '#f5f7fa',
        textColor: '#333333',
        buttonColor: '#1677ff',
        buttonTextColor: '#ffffff',
        templateCode: 'DEFAULT',
        status: 1,
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMerchants();
  }, []);

  const handleMerchantChange = (merchantNo: string) => {
    setSelectedMerchant(merchantNo);
    const merchant = merchants.find((m) => m.merchantNo === merchantNo);
    form.setFieldValue('merchantName', merchant?.merchantName);
    loadConfig(merchantNo);
  };

  const handleColorSchemeChange = (colorSchemeCode: string) => {
    const scheme = colorSchemes.find((t) => t.code === colorSchemeCode);
    if (scheme) {
      form.setFieldsValue({
        colorSchemeCode,
        primaryColor: scheme.primaryColor,
        backgroundColor: scheme.backgroundColor,
        buttonColor: scheme.buttonColor,
      });
    }
  };

  const handleColorReset = () => {
    const colorSchemeCode = form.getFieldValue('colorSchemeCode') || 'DEFAULT';
    handleColorSchemeChange(colorSchemeCode);
  };

  const [uploading, setUploading] = useState(false);

  const handleLogoUpload = async (file: RcFile) => {
    try {
      setUploading(true);
      const result = await paymentPageApi.uploadImage(file);
      if (result?.url) {
        form.setFieldValue('logoUrl', result.url);
        message.success('Logo上传成功');
      }
    } catch {
      message.success('上传成功（演示模式）');
      const reader = new FileReader();
      reader.onload = (e) => {
        const base64 = e.target?.result as string;
        form.setFieldValue('logoUrl', base64);
      };
      reader.readAsDataURL(file);
    } finally {
      setUploading(false);
    }
    return false;
  };

  const uploadProps: UploadProps = {
    name: 'logo',
    showUploadList: false,
    beforeUpload: handleLogoUpload,
    accept: 'image/*',
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      const result = await paymentPageApi.saveConfig(values);
      if (result) {
        setSavedConfig(result);
        message.success('保存成功！');
      }
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.success('保存成功（演示模式）');
      setSavedConfig({
        ...values,
        id: Date.now(),
        pageUrl: `/h5/payment/${values.merchantNo}`,
        statusDesc: values.status === 1 ? '启用' : '禁用',
      });
    } finally {
      setSaving(false);
    }
  };

  const previewConfig: PaymentPageConfig = useMemo(() => {
    return {
      merchantNo: configValues?.merchantNo || selectedMerchant || 'M000001',
      merchantName: configValues?.merchantName,
      pageTitle: configValues?.pageTitle,
      logoUrl: configValues?.logoUrl,
      primaryColor: configValues?.primaryColor,
      secondaryColor: configValues?.secondaryColor,
      backgroundColor: configValues?.backgroundColor,
      textColor: configValues?.textColor,
      buttonColor: configValues?.buttonColor,
      buttonTextColor: configValues?.buttonTextColor,
      colorSchemeCode: configValues?.colorSchemeCode,
      customCss: configValues?.customCss,
      footerText: configValues?.footerText,
    };
  }, [configValues, selectedMerchant]);

  const pageUrl = savedConfig?.pageUrl || (selectedMerchant ? `/h5/payment/${selectedMerchant}` : '');

  const ColorPickerBlock = ({
    label,
    name,
    description,
  }: {
    label: string;
    name: string;
    description?: string;
  }) => (
    <Form.Item label={label} name={name} tooltip={description}>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <div style={{
          width: 40,
          height: 40,
          borderRadius: 8,
          border: '1px solid #d9d9d9',
          backgroundColor: form.getFieldValue(name) || '#ffffff',
          cursor: 'pointer',
        }} />
        <Input
          type="color"
          style={{ width: 60, height: 40, padding: 4, cursor: 'pointer' }}
          onChange={(e) => form.setFieldValue(name, e.target.value)}
        />
        <Select
          style={{ flex: 1, minWidth: 120 }}
          placeholder="选择预设颜色"
          allowClear
          onChange={(val) => val && form.setFieldValue(name, val)}
          value=""
        >
          {presetColors.map((color) => (
            <Select.Option key={color} value={color}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{
                  display: 'inline-block',
                  width: 16,
                  height: 16,
                  borderRadius: 4,
                  backgroundColor: color,
                  border: '1px solid #d9d9d9',
                }} />
                <code style={{ fontSize: 12 }}>{color}</code>
              </div>
            </Select.Option>
          ))}
        </Select>
      </div>
    </Form.Item>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {savedConfig?.pageUrl && (
        <Alert
          message="页面链接已生成"
          description={
            <Space>
              <LinkOutlined />
              <code>{savedConfig.pageUrl}</code>
              <CopyToClipboard text={savedConfig.pageUrl}>
                <Button type="link" size="small" icon={<CopyOutlined />}>
                  复制链接
                </Button>
              </CopyToClipboard>
            </Space>
          }
          type="success"
          showIcon
          icon={<CheckCircleOutlined />}
          closable
        />
      )}

      <Row gutter={16} style={{ minHeight: 'calc(100vh - 200px)' }}>
        <Col xs={24} xl={12} xxl={10}>
          <Card
            title="支付页面定制编辑器"
            loading={loading}
            extra={
              <Space>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={handleColorReset}
                >
                  重置配色
                </Button>
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  onClick={handleSave}
                  loading={saving}
                >
                  保存配置
                </Button>
              </Space>
            }
          >
            <Form
              form={form}
              layout="vertical"
              initialValues={{
                status: 1,
                colorSchemeCode: 'DEFAULT',
                primaryColor: '#1677ff',
                backgroundColor: '#f5f7fa',
                textColor: '#333333',
                buttonColor: '#1677ff',
                buttonTextColor: '#ffffff',
              }}
            >
              <Form.Item name="merchantNo" hidden>
                <Input />
              </Form.Item>
              <Form.Item name="merchantName" hidden>
                <Input />
              </Form.Item>

              <Tabs defaultActiveKey="basic">
                <TabPane tab="基础信息" key="basic">
                  <Form.Item
                    label="选择商户"
                    name="merchantNo"
                    rules={[{ required: true, message: '请选择商户' }]}
                  >
                    <Select
                      placeholder="请选择要配置支付页面的商户"
                      showSearch
                      optionFilterProp="label"
                      onChange={handleMerchantChange}
                      options={merchants.map((m) => ({
                        label: `${m.merchantName} (${m.merchantNo})`,
                        value: m.merchantNo,
                      }))}
                    />
                  </Form.Item>

                  <Form.Item
                    label="页面标题"
                    name="pageTitle"
                    tooltip="显示在浏览器标签和页面头部"
                  >
                    <Input placeholder="如：XX商户 - 收银台" maxLength={50} showCount />
                  </Form.Item>

                  <Form.Item
                    label="商户Logo"
                    name="logoUrl"
                    tooltip="建议尺寸 200x200px，格式 PNG/JPG，大小不超过 2MB"
                  >
                    <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
                      <Upload.Dragger {...uploadProps} style={{ width: 140 }}>
                        <p className="ant-upload-drag-icon">
                          <UploadOutlined />
                        </p>
                        <p className="ant-upload-text" style={{ fontSize: 12 }}>点击或拖拽上传Logo</p>
                      </Upload.Dragger>
                      {form.getFieldValue('logoUrl') && (
                        <div style={{
                          width: 100,
                          height: 100,
                          borderRadius: 8,
                          border: '1px solid #d9d9d9',
                          overflow: 'hidden',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          backgroundColor: '#fafafa',
                        }}>
                          <img
                            src={form.getFieldValue('logoUrl')}
                            alt="Logo预览"
                            style={{ maxWidth: '100%', maxHeight: '100%' }}
                          />
                        </div>
                      )}
                    </div>
                  </Form.Item>

                  <Form.Item label="页脚文字" name="footerText" tooltip="显示在支付页面底部的版权或安全提示">
                    <Input placeholder="如：安全支付 · 由XX支付网关提供技术支持" maxLength={100} showCount />
                  </Form.Item>

                  <Form.Item
                    label="支付完成返回地址"
                    name="returnUrl"
                    tooltip="用户支付完成后跳转的商户页面URL"
                  >
                    <Input placeholder="https://your-domain.com/payment/result" />
                  </Form.Item>

                  <Form.Item label="启用状态" name="status" valuePropName="checked">
                    <Switch
                      checkedChildren="启用"
                      unCheckedChildren="禁用"
                      checked={form.getFieldValue('status') === 1}
                      onChange={(checked) => form.setFieldValue('status', checked ? 1 : 0)}
                    />
                  </Form.Item>
                </TabPane>

                <TabPane tab="配色方案" key="scheme">
                  <Form.Item name="colorSchemeCode" label="选择配色方案">
                    <Radio.Group style={{ width: '100%' }} onChange={(e) => handleColorSchemeChange(e.target.value)}>
                      <Space direction="vertical" style={{ width: '100%' }} size="middle">
                        {colorSchemes.map((scheme) => (
                          <Radio.Button
                            key={scheme.code}
                            value={scheme.code}
                            style={{
                              width: '100%',
                              height: 'auto',
                              padding: 16,
                              border: form.getFieldValue('colorSchemeCode') === scheme.code
                                ? `2px solid ${scheme.primaryColor}`
                                : '1px solid #d9d9d9',
                              borderRadius: 8,
                              display: 'flex',
                              alignItems: 'center',
                              gap: 16,
                            }}
                          >
                            <div style={{
                              width: 48,
                              height: 48,
                              borderRadius: 8,
                              backgroundColor: scheme.primaryColor,
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              color: '#fff',
                              fontWeight: 600,
                              fontSize: 12,
                            }}>
                              {scheme.name.slice(0, 2)}
                            </div>
                            <div style={{ flex: 1, textAlign: 'left' }}>
                              <div style={{ fontWeight: 600, marginBottom: 4 }}>
                                {scheme.name}
                                {form.getFieldValue('colorSchemeCode') === scheme.code && (
                                  <Tag color="blue" style={{ marginLeft: 8 }}>已选</Tag>
                                )}
                              </div>
                              <div style={{ fontSize: 12, color: '#999' }}>{scheme.description}</div>
                            </div>
                            <div style={{ display: 'flex', gap: 4 }}>
                              <div style={{
                                width: 20,
                                height: 20,
                                borderRadius: 4,
                                backgroundColor: scheme.primaryColor,
                                border: '1px solid rgba(0,0,0,0.1)',
                              }} />
                              <div style={{
                                width: 20,
                                height: 20,
                                borderRadius: 4,
                                backgroundColor: scheme.backgroundColor,
                                border: '1px solid rgba(0,0,0,0.1)',
                              }} />
                              <div style={{
                                width: 20,
                                height: 20,
                                borderRadius: 4,
                                backgroundColor: scheme.buttonColor,
                                border: '1px solid rgba(0,0,0,0.1)',
                              }} />
                            </div>
                          </Radio.Button>
                        ))}
                      </Space>
                    </Radio.Group>
                  </Form.Item>
                </TabPane>

                <TabPane tab="主题配色" key="colors">
                  <Row gutter={16}>
                    <Col span={12}>
                      <ColorPickerBlock label="主色调" name="primaryColor" description="用于头部背景、金额、边框强调色等" />
                    </Col>
                    <Col span={12}>
                      <ColorPickerBlock label="按钮颜色" name="buttonColor" description="支付按钮等主要操作按钮颜色" />
                    </Col>
                  </Row>
                  <Row gutter={16}>
                    <Col span={12}>
                      <ColorPickerBlock label="页面背景" name="backgroundColor" description="整个支付页面的背景色" />
                    </Col>
                    <Col span={12}>
                      <ColorPickerBlock label="文字颜色" name="textColor" description="主要文字内容颜色" />
                    </Col>
                  </Row>
                  <Row gutter={16}>
                    <Col span={12}>
                      <ColorPickerBlock label="按钮文字颜色" name="buttonTextColor" description="按钮上的文字颜色" />
                    </Col>
                    <Col span={12}>
                      <ColorPickerBlock label="辅助色" name="secondaryColor" description="可选：辅助色用于次要元素" />
                    </Col>
                  </Row>

                  <Divider orientation="left">高级配置</Divider>

                  <Form.Item label="自定义CSS" name="customCss" tooltip="可选：自定义CSS样式，高级用户使用">
                    <TextArea
                      rows={6}
                      placeholder={`.pay-button {
  /* 自定义支付按钮样式 */
  border-radius: 20px;
}`}
                      style={{ fontFamily: 'monospace' }}
                    />
                  </Form.Item>
                </TabPane>

                <TabPane tab="预览设置" key="preview">
                  <Form.Item label="预览金额 (元)" name="__previewAmount__">
                    <InputNumber
                      min={0.01}
                      step={10}
                      precision={2}
                      value={previewAmount}
                      onChange={(val) => setPreviewAmount(Number(val) || 0)}
                      style={{ width: 200 }}
                      prefix="¥"
                    />
                  </Form.Item>

                  <Form.Item label="预览商品名称">
                    <Input
                      value={previewProduct}
                      onChange={(e) => setPreviewProduct(e.target.value)}
                      placeholder="输入预览的商品名称"
                    />
                  </Form.Item>

                  <Divider />

                  <Alert
                    message="链接说明"
                    description={
                      <div>
                        <p>配置保存后，您的商户可通过以下链接访问独立的H5支付页面：</p>
                        <Space>
                          <code>{pageUrl || '/h5/payment/{商户号}'}</code>
                          {pageUrl && (
                            <CopyToClipboard text={pageUrl}>
                              <Tooltip title="复制链接">
                                <Button type="link" size="small" icon={<CopyOutlined />}>复制</Button>
                              </Tooltip>
                            </CopyToClipboard>
                          )}
                        </Space>
                        <p style={{ marginTop: 8, fontSize: 12, color: '#999' }}>
                          提示：需在创建订单时传入 orderNo 参数以显示实际订单信息
                        </p>
                      </div>
                    }
                    type="info"
                    showIcon
                  />
                </TabPane>
              </Tabs>
            </Form>
          </Card>
        </Col>

        <Col xs={24} xl={12} xxl={14}>
          <Card
            title={
              <Space>
                <EyeOutlined />
                <span>实时预览</span>
                <Radio.Group
                  size="small"
                  value={device}
                  onChange={(e) => setDevice(e.target.value)}
                  optionType="button"
                  buttonStyle="solid"
                >
                  <Radio.Button value="mobile"><PhoneOutlined /> 移动端</Radio.Button>
                  <Radio.Button value="desktop"><DesktopOutlined /> 桌面端</Radio.Button>
                </Radio.Group>
              </Space>
            }
            style={{ position: 'sticky', top: 16 }}
          >
            <div style={{
              minHeight: 500,
              display: 'flex',
              alignItems: 'flex-start',
              justifyContent: 'center',
              padding: device === 'mobile' ? '24px 16px' : '24px',
              backgroundColor: '#f0f2f5',
              borderRadius: 12,
            }}>
              <PaymentPagePreview
                config={previewConfig}
                amount={previewAmount}
                productName={previewProduct}
                device={device}
              />
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default PaymentPageEditor;

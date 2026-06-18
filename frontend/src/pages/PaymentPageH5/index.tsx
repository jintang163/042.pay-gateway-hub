import { useState, useEffect, useMemo } from 'react';
import {
  Avatar,
  Spin,
  message,
  Modal,
  Result,
  Button,
} from 'antd';
import {
  AlipayCircleOutlined,
  WechatOutlined,
  CreditCardOutlined,
  AppleOutlined,
  CheckCircleFilled,
  LoadingOutlined,
  ArrowLeftOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { paymentPageApi, orderApi } from '@/api';
import type { PaymentPageConfig } from '@/types/paymentPage';
import { formatAmount, formatDateTime } from '@/utils';

interface OrderInfo {
  orderNo: string;
  merchantOrderNo: string;
  amount: number;
  productName: string;
  productDescription?: string;
  createTime?: string;
  payStatus?: number;
}

const defaultConfig: Required<Pick<
  PaymentPageConfig,
  'primaryColor' | 'backgroundColor' | 'textColor' | 'buttonColor' | 'buttonTextColor'
>> = {
  primaryColor: '#1677ff',
  backgroundColor: '#f5f7fa',
  textColor: '#333333',
  buttonColor: '#1677ff',
  buttonTextColor: '#ffffff',
};

const payChannels = [
  { code: 'ALIPAY', name: '支付宝', desc: '推荐使用', icon: <AlipayCircleOutlined style={{ color: '#1677ff', fontSize: 28 }} /> },
  { code: 'WECHAT', name: '微信支付', desc: '微信扫码支付', icon: <WechatOutlined style={{ color: '#07c160', fontSize: 28 }} /> },
  { code: 'UNIONPAY', name: '银联支付', desc: '银行卡快捷支付', icon: <CreditCardOutlined style={{ color: '#e60012', fontSize: 28 }} /> },
  { code: 'APPLE_PAY', name: 'Apple Pay', desc: 'Touch ID / Face ID', icon: <AppleOutlined style={{ color: '#000000', fontSize: 28 }} /> },
];

const PaymentPageH5 = () => {
  const { merchantNo } = useParams<{ merchantNo: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const orderNo = searchParams.get('orderNo');

  const [loading, setLoading] = useState(true);
  const [config, setConfig] = useState<PaymentPageConfig | null>(null);
  const [orderInfo, setOrderInfo] = useState<OrderInfo | null>(null);
  const [selectedChannel, setSelectedChannel] = useState<string>('ALIPAY');
  const [paying, setPaying] = useState(false);
  const [payResult, setPayResult] = useState<'success' | 'fail' | null>(null);
  const [countdown, setCountdown] = useState(300);

  const mergedConfig = useMemo(() => {
    return { ...defaultConfig, ...(config || {}) };
  }, [config]);

  useEffect(() => {
    if (!merchantNo) return;
    loadPageData();
  }, [merchantNo, orderNo]);

  useEffect(() => {
    if (paying || payResult) return;
    if (countdown <= 0) {
      message.warning('订单已超时，请重新发起支付');
      return;
    }
    const timer = setInterval(() => {
      setCountdown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [countdown, paying, payResult]);

  const loadPageData = async () => {
    try {
      setLoading(true);
      const [configData] = await Promise.all([
        paymentPageApi.getPublicConfig(merchantNo!),
        orderNo ? loadOrderInfo(orderNo) : Promise.resolve(null),
      ]);
      setConfig(configData);
      if (orderNo) {
        setOrderInfo(orderNo ? await loadOrderInfo(orderNo) : null);
      } else {
        setOrderInfo({
          orderNo: `DEMO${Date.now()}`,
          merchantOrderNo: `MOCK${Date.now()}`,
          amount: 299.0,
          productName: '示例商品 - VIP会员年卡',
          productDescription: '包含全部VIP专属内容，有效期12个月',
          createTime: new Date().toISOString(),
          payStatus: 0,
        });
      }
      if (configData?.pageTitle) {
        document.title = configData.pageTitle;
      }
    } catch (error) {
      message.error('加载页面配置失败');
      setConfig({
        merchantNo: merchantNo!,
        primaryColor: '#1677ff',
        backgroundColor: '#f5f7fa',
        textColor: '#333333',
        buttonColor: '#1677ff',
        buttonTextColor: '#ffffff',
        pageTitle: '收银台',
        templateCode: 'DEFAULT',
      });
      setOrderInfo({
        orderNo: `DEMO${Date.now()}`,
        merchantOrderNo: `MOCK${Date.now()}`,
        amount: 299.0,
        productName: '示例商品 - VIP会员年卡',
        productDescription: '包含全部VIP专属内容，有效期12个月',
        createTime: new Date().toISOString(),
        payStatus: 0,
      });
    } finally {
      setLoading(false);
    }
  };

  const loadOrderInfo = async (orderNo: string): Promise<OrderInfo | null> => {
    try {
      const result = await orderApi.detail(orderNo);
      if (result) {
        return {
          orderNo: result.orderNo || orderNo,
          merchantOrderNo: result.merchantOrderNo || '-',
          amount: result.amount ? Number(result.amount) / 100 : 0,
          productName: result.productName || '商品支付',
          productDescription: result.productDescription,
          createTime: result.createTime,
          payStatus: result.payStatus,
        };
      }
    } catch {
      return null;
    }
    return null;
  };

  const formatCountdown = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handlePay = async () => {
    if (!orderInfo || !selectedChannel) return;
    try {
      setPaying(true);
      await new Promise((resolve) => setTimeout(resolve, 1500));
      setPayResult('success');
      message.success('支付成功！');
      if (mergedConfig.returnUrl) {
        setTimeout(() => {
          window.location.href = mergedConfig.returnUrl!;
        }, 2000);
      }
    } catch {
      setPayResult('fail');
      message.error('支付失败，请重试');
    } finally {
      setPaying(false);
    }
  };

  const handleRetry = () => {
    setPayResult(null);
  };

  if (loading) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: mergedConfig.backgroundColor,
        }}
      >
        <Spin size="large" />
      </div>
    );
  }

  if (payResult) {
    return (
      <div
        style={{
          minHeight: '100vh',
          backgroundColor: mergedConfig.backgroundColor,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 24,
        }}
      >
        <div
          style={{
            width: '100%',
            maxWidth: 400,
            backgroundColor: '#ffffff',
            borderRadius: 16,
            overflow: 'hidden',
            boxShadow: '0 4px 24px rgba(0, 0, 0, 0.08)',
          }}
        >
          <Result
            status={payResult === 'success' ? 'success' : 'error'}
            title={payResult === 'success' ? '支付成功' : '支付失败'}
            subTitle={
              payResult === 'success'
                ? `订单金额：${formatAmount(orderInfo?.amount || 0)}`
                : '请检查支付信息后重试'
            }
          />
          <div style={{ padding: '0 24px 24px' }}>
            {orderInfo && (
              <div
                style={{
                  padding: 16,
                  backgroundColor: '#fafafa',
                  borderRadius: 8,
                  marginBottom: 16,
                  fontSize: 14,
                  color: '#666',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>商品名称</span>
                  <span style={{ color: '#333' }}>{orderInfo.productName}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>订单号</span>
                  <span style={{ color: '#333', fontFamily: 'monospace' }}>{orderInfo.orderNo}</span>
                </div>
              </div>
            )}
            <div style={{ display: 'flex', gap: 12 }}>
              {payResult === 'fail' && (
                <Button
                  block
                  onClick={handleRetry}
                  style={{
                    flex: 1,
                    height: 48,
                    borderRadius: 8,
                  }}
                >
                  重新支付
                </Button>
              )}
              <Button
                block
                type="primary"
                onClick={() => {
                  if (mergedConfig.returnUrl) {
                    window.location.href = mergedConfig.returnUrl!;
                  } else {
                    navigate(-1);
                  }
                }}
                style={{
                  flex: 1,
                  height: 48,
                  borderRadius: 8,
                  backgroundColor: mergedConfig.buttonColor,
                  borderColor: mergedConfig.buttonColor,
                  color: mergedConfig.buttonTextColor,
                }}
              >
                {payResult === 'success' ? '完成' : '返回商户'}
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: mergedConfig.backgroundColor,
        paddingBottom: 100,
      }}
    >
      <style>{mergedConfig.customCss || ''}</style>

      <div
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 100,
          backgroundColor: mergedConfig.primaryColor,
          color: '#ffffff',
          padding: '16px 16px',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
        }}
      >
        <button
          onClick={() => navigate(-1)}
          style={{
            background: 'none',
            border: 'none',
            color: '#ffffff',
            fontSize: 20,
            cursor: 'pointer',
            padding: '4px 8px',
            marginLeft: -8,
          }}
        >
          <ArrowLeftOutlined />
        </button>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
          {mergedConfig.logoUrl ? (
            <Avatar
              size={36}
              src={mergedConfig.logoUrl}
              shape="square"
              style={{ backgroundColor: '#ffffff', borderRadius: 8, flexShrink: 0 }}
            />
          ) : (
            <Avatar
              size={36}
              shape="square"
              style={{
                backgroundColor: 'rgba(255,255,255,0.2)',
                fontSize: 16,
                borderRadius: 8,
                flexShrink: 0,
              }}
            >
              {(mergedConfig.merchantName || 'M').charAt(0)}
            </Avatar>
          )}
          <div style={{ flex: 1, minWidth: 0 }}>
            <div
              style={{
                fontSize: 16,
                fontWeight: 600,
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              {mergedConfig.pageTitle || mergedConfig.merchantName || '收银台'}
            </div>
            <div style={{ fontSize: 11, opacity: 0.85 }}>
              {mergedConfig.merchantName || '安全支付'}
            </div>
          </div>
        </div>
        <SafetyOutlined style={{ fontSize: 20, opacity: 0.9 }} />
      </div>

      <div
        style={{
          backgroundColor: mergedConfig.primaryColor,
          color: '#ffffff',
          padding: '8px 16px 40px',
          textAlign: 'center',
        }}
      >
        <div style={{ fontSize: 12, opacity: 0.85, marginBottom: 8 }}>支付金额</div>
        <div style={{ fontSize: 42, fontWeight: 700, lineHeight: 1.2 }}>
          <span style={{ fontSize: 22, fontWeight: 500, marginRight: 4 }}>¥</span>
          {formatAmount(orderInfo?.amount || 0).replace('¥', '').replace('CNY', '').trim()}
        </div>
        {countdown > 0 && (
          <div style={{ marginTop: 12, fontSize: 13, opacity: 0.9 }}>
            <LoadingOutlined style={{ marginRight: 4 }} />
            请在 <span style={{ fontWeight: 600 }}>{formatCountdown(countdown)}</span> 内完成支付
          </div>
        )}
      </div>

      <div style={{ padding: '0 12px', marginTop: -24 }}>
        <div
          style={{
            backgroundColor: '#ffffff',
            borderRadius: 12,
            padding: 16,
            boxShadow: '0 2px 12px rgba(0, 0, 0, 0.06)',
            color: mergedConfig.textColor,
          }}
        >
          <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>订单信息</div>
          <div style={{ fontSize: 16, fontWeight: 500, marginBottom: 8, color: mergedConfig.textColor }}>
            {orderInfo?.productName || '商品支付'}
          </div>
          {orderInfo?.productDescription && (
            <div style={{ fontSize: 13, color: `${mergedConfig.textColor}99`, marginBottom: 12 }}>
              {orderInfo.productDescription}
            </div>
          )}
          <div
            style={{
              borderTop: '1px solid #f0f0f0',
              paddingTop: 12,
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
              fontSize: 13,
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: `${mergedConfig.textColor}99` }}>商户订单号</span>
              <span style={{ color: mergedConfig.textColor, fontFamily: 'monospace' }}>
                {orderInfo?.merchantOrderNo || '-'}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: `${mergedConfig.textColor}99` }}>系统订单号</span>
              <span style={{ color: mergedConfig.textColor, fontFamily: 'monospace' }}>
                {orderInfo?.orderNo || '-'}
              </span>
            </div>
            {orderInfo?.createTime && (
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: `${mergedConfig.textColor}99` }}>创建时间</span>
                <span style={{ color: mergedConfig.textColor }}>
                  {formatDateTime(orderInfo.createTime)}
                </span>
              </div>
            )}
          </div>
        </div>
      </div>

      <div style={{ padding: '16px 12px 0' }}>
        <div
          style={{
            backgroundColor: '#ffffff',
            borderRadius: 12,
            padding: 16,
            boxShadow: '0 2px 12px rgba(0, 0, 0, 0.06)',
            color: mergedConfig.textColor,
          }}
        >
          <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>选择支付方式</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {payChannels.map((channel) => {
              const isSelected = selectedChannel === channel.code;
              return (
                <div
                  key={channel.code}
                  onClick={() => setSelectedChannel(channel.code)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    padding: '14px 12px',
                    borderRadius: 10,
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    border: `1.5px solid ${isSelected ? mergedConfig.primaryColor : '#f0f0f0'}`,
                    backgroundColor: isSelected ? `${mergedConfig.primaryColor}08` : '#fafafa',
                  }}
                >
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      borderRadius: 10,
                      backgroundColor: '#f5f7fa',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    {channel.icon}
                  </div>
                  <div style={{ flex: 1, marginLeft: 12 }}>
                    <div style={{ fontSize: 15, fontWeight: 500, color: mergedConfig.textColor }}>
                      {channel.name}
                    </div>
                    <div style={{ fontSize: 12, color: `${mergedConfig.textColor}99`, marginTop: 2 }}>
                      {channel.desc}
                    </div>
                  </div>
                  {isSelected && (
                    <CheckCircleFilled
                      style={{
                        fontSize: 22,
                        color: mergedConfig.primaryColor,
                      }}
                    />
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {mergedConfig.footerText && (
        <div
          style={{
            padding: '20px 24px 0',
            textAlign: 'center',
            fontSize: 12,
            color: `${mergedConfig.textColor}66`,
          }}
        >
          <SafetyOutlined style={{ marginRight: 4 }} />
          {mergedConfig.footerText}
        </div>
      )}

      <div
        style={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          padding: '12px 16px calc(12px + env(safe-area-inset-bottom))',
          backgroundColor: '#ffffff',
          boxShadow: '0 -2px 12px rgba(0, 0, 0, 0.06)',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
        }}
      >
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 12, color: '#999' }}>实付金额</div>
          <div
            style={{
              fontSize: 22,
              fontWeight: 700,
              color: mergedConfig.primaryColor,
              lineHeight: 1.2,
            }}
          >
            ¥{formatAmount(orderInfo?.amount || 0).replace('¥', '').replace('CNY', '').trim()}
          </div>
        </div>
        <Button
          block
          type="primary"
          size="large"
          loading={paying}
          onClick={handlePay}
          style={{
            flex: 1.2,
            height: 50,
            borderRadius: 10,
            fontSize: 17,
            fontWeight: 600,
            backgroundColor: mergedConfig.buttonColor,
            borderColor: mergedConfig.buttonColor,
            color: mergedConfig.buttonTextColor,
          }}
        >
          {paying ? '支付处理中...' : '立即支付'}
        </Button>
      </div>

      <Modal
        open={paying}
        centered
        footer={null}
        closable={false}
        maskClosable={false}
        width={280}
        styles={{ body: { textAlign: 'center', padding: '32px 24px' } }}
      >
        <Spin
          size="large"
          indicator={<LoadingOutlined style={{ fontSize: 48, color: mergedConfig.primaryColor }} spin />}
          style={{ marginBottom: 16 }}
        />
        <div style={{ fontSize: 16, fontWeight: 600, color: mergedConfig.textColor, marginBottom: 8 }}>
          支付处理中
        </div>
        <div style={{ fontSize: 13, color: '#999' }}>
          请稍候，正在处理您的支付请求...
        </div>
      </Modal>
    </div>
  );
};

export default PaymentPageH5;

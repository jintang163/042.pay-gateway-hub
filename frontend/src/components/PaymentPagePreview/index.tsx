import { useMemo } from 'react';
import { Card, Avatar } from 'antd';
import {
  AlipayCircleOutlined,
  WechatOutlined,
  CreditCardOutlined,
  AppleOutlined,
} from '@ant-design/icons';
import type { PaymentPageConfig } from '@/types/paymentPage';
import { formatAmount } from '@/utils';

interface PaymentPagePreviewProps {
  config: PaymentPageConfig;
  amount?: number;
  productName?: string;
  device?: 'mobile' | 'desktop';
}

const defaultConfig: Required<Pick<
  PaymentPageConfig,
  'primaryColor' | 'backgroundColor' | 'textColor' | 'buttonColor' | 'buttonTextColor' | 'colorSchemeCode'
>> = {
  primaryColor: '#1677ff',
  backgroundColor: '#f5f7fa',
  textColor: '#333333',
  buttonColor: '#1677ff',
  buttonTextColor: '#ffffff',
  colorSchemeCode: 'DEFAULT',
};

const payChannels = [
  { code: 'ALIPAY', name: '支付宝', icon: <AlipayCircleOutlined style={{ color: '#1677ff', fontSize: 24 }} /> },
  { code: 'WECHAT', name: '微信支付', icon: <WechatOutlined style={{ color: '#07c160', fontSize: 24 }} /> },
  { code: 'UNIONPAY', name: '银联支付', icon: <CreditCardOutlined style={{ color: '#e60012', fontSize: 24 }} /> },
  { code: 'APPLE_PAY', name: 'Apple Pay', icon: <AppleOutlined style={{ color: '#000000', fontSize: 24 }} /> },
];

const PaymentPagePreview = ({
  config,
  amount = 299.0,
  productName = '示例商品名称',
  device = 'mobile',
}: PaymentPagePreviewProps) => {
  const mergedConfig = useMemo(() => {
    return { ...defaultConfig, ...config };
  }, [config]);

  const isMobile = device === 'mobile';

  const containerStyle: React.CSSProperties = useMemo(() => ({
    width: isMobile ? 375 : '100%',
    maxWidth: isMobile ? 375 : 500,
    margin: '0 auto',
    backgroundColor: mergedConfig.backgroundColor,
    borderRadius: 12,
    overflow: 'hidden',
    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.1)',
    border: '1px solid rgba(0, 0, 0, 0.06)',
  }), [isMobile, mergedConfig.backgroundColor]);

  const headerStyle: React.CSSProperties = useMemo(() => ({
    backgroundColor: mergedConfig.primaryColor,
    color: '#ffffff',
    padding: isMobile ? '20px 16px' : '24px 24px',
    display: 'flex',
    alignItems: 'center',
    gap: 12,
  }), [mergedConfig.primaryColor, isMobile]);

  const amountStyle: React.CSSProperties = useMemo(() => ({
    color: mergedConfig.primaryColor,
    fontSize: isMobile ? 36 : 42,
    fontWeight: 700,
    textAlign: 'center',
    padding: isMobile ? '24px 16px 8px' : '32px 24px 12px',
    lineHeight: 1.2,
  }), [mergedConfig.primaryColor, isMobile]);

  const buttonStyle: React.CSSProperties = useMemo(() => ({
    backgroundColor: mergedConfig.buttonColor,
    color: mergedConfig.buttonTextColor,
    border: 'none',
    width: '100%',
    padding: isMobile ? '14px 0' : '16px 0',
    fontSize: isMobile ? 16 : 18,
    fontWeight: 600,
    borderRadius: 8,
    cursor: 'pointer',
    transition: 'all 0.2s',
  }), [mergedConfig.buttonColor, mergedConfig.buttonTextColor, isMobile]);

  const textStyle: React.CSSProperties = {
    color: mergedConfig.textColor,
  };

  const secondaryTextStyle: React.CSSProperties = {
    color: `${mergedConfig.textColor}99`,
    fontSize: 13,
  };

  return (
    <div style={containerStyle}>
      <style>{mergedConfig.customCss || ''}</style>

      <div style={headerStyle}>
        {mergedConfig.logoUrl ? (
          <Avatar
            size={isMobile ? 40 : 48}
            src={mergedConfig.logoUrl}
            shape="square"
            style={{ backgroundColor: '#ffffff', borderRadius: 8 }}
          />
        ) : (
          <Avatar
            size={isMobile ? 40 : 48}
            shape="square"
            style={{ backgroundColor: 'rgba(255,255,255,0.2)', fontSize: 18, borderRadius: 8 }}
          >
            {(mergedConfig.merchantName || 'M').charAt(0)}
          </Avatar>
        )}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: isMobile ? 15 : 17, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {mergedConfig.pageTitle || mergedConfig.merchantName || '收银台'}
          </div>
          <div style={{ fontSize: isMobile ? 11 : 12, opacity: 0.85, marginTop: 2 }}>
            {mergedConfig.merchantName || '商户名称'}
          </div>
        </div>
      </div>

      <div style={amountStyle}>
        <span style={{ fontSize: isMobile ? 18 : 22, fontWeight: 500, marginRight: 4 }}>¥</span>
        {formatAmount(amount).replace('¥', '').replace('CNY', '').trim()}
      </div>

      <div style={{ textAlign: 'center', padding: '0 16px 20px' }}>
        <div style={{ ...textStyle, fontSize: isMobile ? 14 : 15, fontWeight: 500 }}>
          {productName}
        </div>
        <div style={{ ...secondaryTextStyle, marginTop: 4 }}>
          订单号: TEST{Date.now().toString().slice(-10)}
        </div>
      </div>

      <Card
        style={{
          margin: isMobile ? '0 12px' : '0 16px',
          borderRadius: 12,
          border: 'none',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
        }}
        bodyStyle={{ padding: isMobile ? 12 : 16 }}
      >
        <div style={{ ...textStyle, fontWeight: 600, marginBottom: 12, fontSize: isMobile ? 14 : 15 }}>
          选择支付方式
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          {payChannels.slice(0, isMobile ? 3 : 4).map((channel) => (
            <div
              key={channel.code}
              style={{
                display: 'flex',
                alignItems: 'center',
                padding: isMobile ? '12px 8px' : '14px 12px',
                borderRadius: 8,
                cursor: 'pointer',
                transition: 'background-color 0.2s',
                border: `1px solid ${channel.code === 'ALIPAY' ? mergedConfig.primaryColor + '40' : 'transparent'}`,
                backgroundColor: channel.code === 'ALIPAY' ? mergedConfig.primaryColor + '08' : 'transparent',
              }}
            >
              <div style={{
                width: isMobile ? 36 : 40,
                height: isMobile ? 36 : 40,
                borderRadius: 8,
                backgroundColor: '#f5f7fa',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}>
                {channel.icon}
              </div>
              <div style={{ flex: 1, marginLeft: 12 }}>
                <div style={{ ...textStyle, fontSize: isMobile ? 14 : 15, fontWeight: 500 }}>
                  {channel.name}
                </div>
                <div style={secondaryTextStyle}>推荐使用</div>
              </div>
              {channel.code === 'ALIPAY' && (
                <div style={{
                  width: 20,
                  height: 20,
                  borderRadius: '50%',
                  backgroundColor: mergedConfig.primaryColor,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}>
                  <span style={{ color: '#fff', fontSize: 12 }}>✓</span>
                </div>
              )}
            </div>
          ))}
        </div>
      </Card>

      <div style={{ padding: isMobile ? '20px 12px 16px' : '24px 16px 20px' }}>
        <button style={buttonStyle}>
          确认支付
        </button>
      </div>

      {mergedConfig.footerText && (
        <div style={{
          textAlign: 'center',
          padding: '0 16px 20px',
          ...secondaryTextStyle,
          fontSize: 12,
        }}>
          {mergedConfig.footerText}
        </div>
      )}
    </div>
  );
};

export default PaymentPagePreview;

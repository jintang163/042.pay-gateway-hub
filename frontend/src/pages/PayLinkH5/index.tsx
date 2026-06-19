import { useState, useEffect, useMemo } from 'react';
import {
  InputNumber,
  Button,
  Spin,
  message,
  Result,
  Input,
  Tag,
  Space,
  Card,
  Divider,
} from 'antd';
import {
  ArrowLeftOutlined,
  SafetyOutlined,
  CheckCircleOutlined,
  GiftOutlined,
  LoadingOutlined,
} from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { payLinkPublicApi, couponApi } from '@/api';
import type { PayLink, CouponDiscountCalcResult } from '@/types/marketing';
import { formatAmount, formatDateTime } from '@/utils';
import { paymentPageApi } from '@/api/paymentPage';
import type { PaymentPageConfig } from '@/types/paymentPage';

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

const PayLinkH5Page = () => {
  const { linkCode } = useParams<{ linkCode: string }>();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [linkInfo, setLinkInfo] = useState<PayLink | null>(null);
  const [config, setConfig] = useState<PaymentPageConfig | null>(null);
  const [payAmount, setPayAmount] = useState<number | undefined>();
  const [couponCode, setCouponCode] = useState<string>('');
  const [couponResult, setCouponResult] = useState<CouponDiscountCalcResult | null>(null);
  const [couponError, setCouponError] = useState<string>('');
  const [applyingCoupon, setApplyingCoupon] = useState(false);

  const [orderCreating, setOrderCreating] = useState(false);
  const [orderNo, setOrderNo] = useState<string>('');
  const [paying, setPaying] = useState(false);
  const [payResult, setPayResult] = useState<'success' | 'fail' | null>(null);

  const mergedConfig = useMemo(() => {
    return { ...defaultConfig, ...(config || {}) };
  }, [config]);

  const finalAmount = useMemo(() => {
    const base = payAmount || (linkInfo?.fixedAmount ? Number(linkInfo.fixedAmount) : 0);
    if (couponResult) {
      return Math.max(0, base - Number(couponResult.discountAmount));
    }
    return base;
  }, [payAmount, linkInfo, couponResult]);

  useEffect(() => {
    if (linkCode) {
      loadLinkData();
    }
  }, [linkCode]);

  const loadLinkData = async () => {
    try {
      setLoading(true);
      const link = await payLinkPublicApi.getInfo(linkCode!);
      setLinkInfo(link);
      if (link.fixedAmount) {
        setPayAmount(Number(link.fixedAmount));
      }

      try {
        const pageConfig = await paymentPageApi.getPublicConfig(link.merchantNo);
        setConfig(pageConfig);
        if (pageConfig?.pageTitle) {
          document.title = pageConfig.pageTitle;
        }
      } catch {
        // ignore config load error
      }
    } catch (e: any) {
      message.error(e?.message || '支付链接无效或已失效');
    } finally {
      setLoading(false);
    }
  };

  const handleApplyCoupon = async () => {
    if (!couponCode.trim()) {
      setCouponError('请输入优惠券编码');
      return;
    }
    if (!payAmount && !linkInfo?.fixedAmount) {
      setCouponError('请先输入支付金额');
      return;
    }
    try {
      setApplyingCoupon(true);
      setCouponError('');
      const result = await couponApi.calculateDiscount({
        couponCode: couponCode.trim(),
        orderAmount: payAmount || Number(linkInfo?.fixedAmount || 0),
        merchantNo: linkInfo?.merchantNo,
      });
      setCouponResult(result);
      message.success('优惠券应用成功');
    } catch (e: any) {
      setCouponError(e?.message || '优惠券不可用');
      setCouponResult(null);
    } finally {
      setApplyingCoupon(false);
    }
  };

  const handleRemoveCoupon = () => {
    setCouponCode('');
    setCouponResult(null);
    setCouponError('');
  };

  const handleCreateOrder = async () => {
    if (!linkInfo) return;
    if (!linkInfo.amountEditable && !linkInfo.fixedAmount) {
      message.error('支付金额无效');
      return;
    }
    if (linkInfo.amountEditable && (!payAmount || payAmount <= 0)) {
      message.error('请输入有效的支付金额');
      return;
    }
    if (linkInfo.minAmount && payAmount && payAmount < Number(linkInfo.minAmount)) {
      message.error(`支付金额不能低于 ${formatAmount(Number(linkInfo.minAmount))}`);
      return;
    }
    if (linkInfo.maxAmount && payAmount && payAmount > Number(linkInfo.maxAmount)) {
      message.error(`支付金额不能超过 ${formatAmount(Number(linkInfo.maxAmount))}`);
      return;
    }

    try {
      setOrderCreating(true);
      const result = await payLinkPublicApi.createOrder({
        linkCode: linkCode!,
        payAmount: payAmount ? payAmount : undefined,
        couponCode: couponResult?.couponCode || undefined,
      });
      setOrderNo(result.orderNo || '');
      setPaying(true);
      setTimeout(() => {
        setPayResult('success');
        message.success('支付成功');
      }, 2000);
    } catch (e: any) {
      message.error(e?.message || '创建订单失败');
    } finally {
      setOrderCreating(false);
    }
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

  if (!linkInfo) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: mergedConfig.backgroundColor,
          padding: 24,
        }}
      >
        <Result
          status="warning"
          title="链接无效"
          subTitle="该支付链接不存在或已失效，请联系商家获取有效链接"
          extra={
            <Button type="primary" onClick={() => navigate(-1)}>
              返回
            </Button>
          }
        />
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
                ? `实付金额：¥${formatAmount(finalAmount).replace('¥', '')}`
                : '请检查支付信息后重试'
            }
          />
          {payResult === 'success' && couponResult && (
            <div style={{ padding: '0 24px 16px' }}>
              <Card size="small" style={{ backgroundColor: '#fff7e6', borderColor: '#ffd591' }}>
                <Space>
                  <GiftOutlined style={{ color: '#fa8c16', fontSize: 20 }} />
                  <span>
                    已使用优惠券 <b>{couponResult.couponName}</b>，省 <b style={{ color: '#cf1322' }}>¥{formatAmount(Number(couponResult.discountAmount)).replace('¥', '')}</b>
                  </span>
                </Space>
              </Card>
            </div>
          )}
          <div style={{ padding: '0 24px 24px' }}>
            <Button
              block
              type="primary"
              size="large"
              onClick={() => {
                if (linkInfo.redirectUrl) {
                  window.location.href = linkInfo.redirectUrl;
                } else {
                  navigate(-1);
                }
              }}
              style={{
                height: 48,
                borderRadius: 8,
                backgroundColor: mergedConfig.buttonColor,
                borderColor: mergedConfig.buttonColor,
                color: mergedConfig.buttonTextColor,
              }}
            >
              {linkInfo.redirectUrl ? '返回商户' : '完成'}
            </Button>
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
        paddingBottom: 120,
      }}
    >
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
        <div style={{ flex: 1, fontSize: 16, fontWeight: 600 }}>
          {linkInfo.title}
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
          {linkInfo.amountEditable ? (
            <InputNumber
              size="large"
              min={linkInfo.minAmount ? Number(linkInfo.minAmount) : 0.01}
              max={linkInfo.maxAmount ? Number(linkInfo.maxAmount) : undefined}
              step={0.01}
              precision={2}
              value={payAmount}
              onChange={(val) => {
                setPayAmount(val ?? undefined);
                setCouponResult(null);
                setCouponError('');
              }}
              placeholder="请输入金额"
              style={{
                width: '60%',
                fontSize: 28,
                fontWeight: 700,
                textAlign: 'center',
              }}
              controls={false}
            />
          ) : (
            formatAmount(Number(linkInfo.fixedAmount || 0)).replace('¥', '')
          )}
        </div>
        {linkInfo.singleUse && (
          <Tag color="warning" style={{ marginTop: 12 }}>
            单次使用链接
          </Tag>
        )}
        {linkInfo.expireTime && (
          <div style={{ marginTop: 8, fontSize: 12, opacity: 0.9 }}>
            有效期至 {formatDateTime(linkInfo.expireTime)}
          </div>
        )}
      </div>

      <div style={{ padding: '0 12px', marginTop: -24 }}>
        <Card
          style={{
            borderRadius: 12,
            boxShadow: '0 2px 12px rgba(0, 0, 0, 0.06)',
          }}
          bodyStyle={{ padding: 16 }}
        >
          <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 12, color: mergedConfig.textColor }}>
            订单信息
          </div>
          <div style={{ fontSize: 15, fontWeight: 500, marginBottom: 8, color: mergedConfig.textColor }}>
            {linkInfo.productSubject || linkInfo.title}
          </div>
          {linkInfo.productDetail && (
            <div style={{ fontSize: 13, color: `${mergedConfig.textColor}99`, marginBottom: 12 }}>
              {linkInfo.productDetail}
            </div>
          )}

          <Divider style={{ margin: '12px 0' }} />

          <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 12, color: mergedConfig.textColor }}>
            优惠券
          </div>
          {couponResult ? (
            <div
              style={{
                padding: 12,
                backgroundColor: '#fff7e6',
                border: '1px solid #ffd591',
                borderRadius: 8,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <Space>
                <GiftOutlined style={{ color: '#fa8c16' }} />
                <div>
                  <div style={{ fontWeight: 600, color: '#d46b08' }}>{couponResult.couponName}</div>
                  <div style={{ fontSize: 12, color: '#ad6800' }}>{couponResult.calcDetail}</div>
                </div>
              </Space>
              <div style={{ textAlign: 'right' }}>
                <div style={{ color: '#cf1322', fontWeight: 700, fontSize: 18 }}>
                  -¥{formatAmount(Number(couponResult.discountAmount)).replace('¥', '')}
                </div>
                <a
                  onClick={handleRemoveCoupon}
                  style={{ fontSize: 12, cursor: 'pointer' }}
                >
                  移除
                </a>
              </div>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: 8 }}>
              <Input
                placeholder="输入优惠券编码"
                value={couponCode}
                onChange={(e) => {
                  setCouponCode(e.target.value);
                  setCouponError('');
                }}
                onPressEnter={handleApplyCoupon}
                style={{ flex: 1 }}
                status={couponError ? 'error' : ''}
              />
              <Button
                type="primary"
                onClick={handleApplyCoupon}
                loading={applyingCoupon}
                style={{
                  backgroundColor: mergedConfig.buttonColor,
                  borderColor: mergedConfig.buttonColor,
                  color: mergedConfig.buttonTextColor,
                }}
              >
                使用
              </Button>
            </div>
          )}
          {couponError && (
            <div style={{ color: '#ff4d4f', fontSize: 12, marginTop: 6 }}>
              {couponError}
            </div>
          )}
        </Card>
      </div>

      {couponResult && (
        <div style={{ padding: '12px 12px 0' }}>
          <Card
            size="small"
            style={{ borderRadius: 12, backgroundColor: '#fffbe6' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: '#8c6f00' }}>优惠合计</span>
              <span style={{ color: '#cf1322', fontSize: 20, fontWeight: 700 }}>
                -¥{formatAmount(Number(couponResult.discountAmount)).replace('¥', '')}
              </span>
            </div>
          </Card>
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
          zIndex: 100,
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
            ¥{formatAmount(finalAmount).replace('¥', '')}
          </div>
        </div>
        <Button
          block
          type="primary"
          size="large"
          loading={orderCreating || paying}
          onClick={handleCreateOrder}
          style={{
            flex: 1.5,
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

      {paying && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
          }}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: 12,
              padding: '32px 48px',
              textAlign: 'center',
            }}
          >
            <Spin
              size="large"
              indicator={<LoadingOutlined style={{ fontSize: 40, color: mergedConfig.primaryColor }} spin />}
              style={{ marginBottom: 16 }}
            />
            <div style={{ fontSize: 16, fontWeight: 600, color: mergedConfig.textColor, marginBottom: 8 }}>
              支付处理中
            </div>
            <div style={{ fontSize: 13, color: '#999' }}>
              请稍候，正在处理您的支付请求...
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PayLinkH5Page;

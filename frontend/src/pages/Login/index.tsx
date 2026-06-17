import { useState, useEffect } from 'react';
import { Form, Input, Button, Card, Checkbox, message, Tabs, type TabsProps } from 'antd';
import { UserOutlined, LockOutlined, SafetyOutlined, LoginOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { authApi } from '@/api';
import { useUserStore } from '@/store';
import type { LoginRequest } from '@/types/user';

const generateCaptcha = (): string => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let result = '';
  for (let i = 0; i < 4; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
};

const Login = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useUserStore();
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm<LoginRequest>();
  const [captchaCode, setCaptchaCode] = useState('');

  useEffect(() => {
    setCaptchaCode(generateCaptcha());
  }, []);

  const refreshCaptcha = () => {
    setCaptchaCode(generateCaptcha());
  };

  const handleLogin = async (values: LoginRequest) => {
    try {
      if (values.captcha?.toLowerCase() !== captchaCode.toLowerCase()) {
        message.error('验证码错误');
        refreshCaptcha();
        return;
      }
      setLoading(true);
      const result = await authApi.login(values);
      login(result);
      message.success('登录成功');
      const from = (location.state as { from?: string })?.from || '/dashboard';
      navigate(from, { replace: true });
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败');
    } finally {
      setLoading(false);
    }
  };

  const items: TabsProps['items'] = [
    {
      key: 'account',
      label: '账号登录',
      children: (
        <Form
          form={form}
          name="login_form"
          initialValues={{ remember: true }}
          onFinish={handleLogin}
          autoComplete="off"
          size="large"
          layout="vertical"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" allowClear />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" allowClear />
          </Form.Item>

          <Form.Item
            name="captcha"
            rules={[{ required: true, message: '请输入验证码' }]}
          >
            <div style={{ display: 'flex', gap: 8 }}>
              <Input
                prefix={<SafetyOutlined />}
                placeholder="验证码"
                allowClear
                style={{ flex: 1 }}
                maxLength={4}
              />
              <div
                onClick={refreshCaptcha}
                style={{
                  width: 120,
                  height: 40,
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderRadius: 6,
                  cursor: 'pointer',
                  fontWeight: 700,
                  letterSpacing: 4,
                  fontSize: 18,
                  color: '#fff',
                  userSelect: 'none',
                  fontStyle: 'italic',
                  textShadow: '1px 1px 2px rgba(0,0,0,0.3)',
                }}
                title="点击刷新验证码"
              >
                {captchaCode}
              </div>
            </div>
          </Form.Item>

          <Form.Item>
            <Form.Item name="remember" valuePropName="checked" noStyle>
              <Checkbox>记住我</Checkbox>
            </Form.Item>
            <a style={{ float: 'right' }} href="#forgot">
              忘记密码？
            </a>
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              icon={<LoginOutlined />}
            >
              登录
            </Button>
          </Form.Item>
        </Form>
      ),
    },
  ];

  return (
    <div
      style={{
        minHeight: '100vh',
        width: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      <div
        style={{
          position: 'absolute',
          top: '-50%',
          right: '-10%',
          width: 600,
          height: 600,
          borderRadius: '50%',
          background: 'rgba(255,255,255,0.1)',
        }}
      />
      <div
        style={{
          position: 'absolute',
          bottom: '-30%',
          left: '-10%',
          width: 500,
          height: 500,
          borderRadius: '50%',
          background: 'rgba(255,255,255,0.08)',
        }}
      />

      <Card
        style={{
          width: 420,
          borderRadius: 12,
          boxShadow: '0 20px 60px rgba(0,0,0,0.3)',
          position: 'relative',
          zIndex: 1,
        }}
        styles={{ body: { padding: 40 } }}
      >
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <h1 style={{ fontSize: 28, marginBottom: 8, fontWeight: 700, color: '#1677ff' }}>
            支付网关管理平台
          </h1>
          <p style={{ color: '#999', margin: 0 }}>安全、稳定、高效的支付管理系统</p>
        </div>

        <Tabs items={items} centered />
      </Card>
    </div>
  );
};

export default Login;

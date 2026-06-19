import { Routes, Route, Navigate } from 'react-router-dom';
import { lazy, Suspense, useEffect } from 'react';
import { Spin } from 'antd';
import { MainLayout } from '@/layouts';
import Login from '@/pages/Login';
import { useUserStore } from '@/store';
import { useLocation, useNavigate } from 'react-router-dom';

const Dashboard = lazy(() => import('@/pages/Dashboard'));
const MerchantApply = lazy(() => import('@/pages/MerchantApply'));
const MerchantAudit = lazy(() => import('@/pages/MerchantAudit'));
const PayConfig = lazy(() => import('@/pages/PayConfig'));
const OrderList = lazy(() => import('@/pages/OrderList'));
const RefundList = lazy(() => import('@/pages/RefundList'));
const Settlement = lazy(() => import('@/pages/Settlement'));
const Reconcile = lazy(() => import('@/pages/Reconcile'));
const RiskMonitor = lazy(() => import('@/pages/RiskMonitor'));
const RiskRuleConfig = lazy(() => import('@/pages/RiskRuleConfig'));
const RiskListManage = lazy(() => import('@/pages/RiskListManage'));
const RiskAudit = lazy(() => import('@/pages/RiskAudit'));
const SandboxTest = lazy(() => import('@/pages/SandboxTest'));
const ApiStats = lazy(() => import('@/pages/ApiStats'));
const CallbackSimulator = lazy(() => import('@/pages/CallbackSimulator'));
const FeeConfig = lazy(() => import('@/pages/FeeConfig'));
const PaymentPageEditor = lazy(() => import('@/pages/PaymentPageEditor'));
const PaymentPageH5 = lazy(() => import('@/pages/PaymentPageH5'));
const MerchantConfigTest = lazy(() => import('@/pages/MerchantConfigTest'));
const AgentTree = lazy(() => import('@/pages/AgentTree'));
const SubordinateMerchant = lazy(() => import('@/pages/SubordinateMerchant'));
const AgentProfitRule = lazy(() => import('@/pages/AgentProfitRule'));
const AgentProfit = lazy(() => import('@/pages/AgentProfit'));
const AgentWithdraw = lazy(() => import('@/pages/AgentWithdraw'));
const InvoiceList = lazy(() => import('@/pages/InvoiceList'));
const ReportSubscription = lazy(() => import('@/pages/ReportSubscription'));
const PayLinkManage = lazy(() => import('@/pages/PayLinkManage'));
const CouponManage = lazy(() => import('@/pages/CouponManage'));
const ActivityConfig = lazy(() => import('@/pages/ActivityConfig'));

export interface RouteConfigItem {
  path: string;
  name?: string;
  icon?: string;
  element: React.ReactNode;
  children?: RouteConfigItem[];
  hidden?: boolean;
  roles?: string[];
}

export const routesConfig: RouteConfigItem[] = [
  {
    path: 'dashboard',
    name: '仪表盘',
    icon: 'DashboardOutlined',
    element: <Dashboard />,
  },
  {
    path: 'merchant',
    name: '商户管理',
    icon: 'ShopOutlined',
    children: [
      {
        path: 'apply',
        name: '商户入驻',
        element: <MerchantApply />,
      },
      {
        path: 'audit',
        name: '人工审核',
        element: <MerchantAudit />,
      },
    ],
  },
  {
    path: 'pay-config',
    name: '支付配置',
    icon: 'KeyOutlined',
    element: <PayConfig />,
  },
  {
    path: 'order',
    name: '订单管理',
    icon: 'FileTextOutlined',
    children: [
      {
        path: 'list',
        name: '订单列表',
        element: <OrderList />,
      },
      {
        path: 'refund',
        name: '退款列表',
        element: <RefundList />,
      },
      {
        path: 'invoice',
        name: '发票管理',
        element: <InvoiceList />,
      },
    ],
  },
  {
    path: 'settlement',
    name: '结算管理',
    icon: 'WalletOutlined',
    children: [
      {
        path: 'list',
        name: '结算记录',
        element: <Settlement />,
      },
      {
        path: 'reconcile',
        name: '对账与差错',
        element: <Reconcile />,
      },
      {
        path: 'report',
        name: '报表订阅',
        element: <ReportSubscription />,
      },
    ],
  },
  {
    path: 'risk',
    name: '风控管理',
    icon: 'AlertOutlined',
    children: [
      {
        path: 'monitor',
        name: '风控监控',
        element: <RiskMonitor />,
      },
      {
        path: 'rules',
        name: '规则配置',
        element: <RiskRuleConfig />,
      },
      {
        path: 'lists',
        name: '黑白名单',
        element: <RiskListManage />,
      },
      {
        path: 'audit',
        name: '风控审核',
        element: <RiskAudit />,
      },
    ],
  },
  {
    path: 'sandbox',
    name: '沙箱测试',
    icon: 'ExperimentOutlined',
    element: <SandboxTest />,
  },
  {
    path: 'merchant-config-test',
    name: '一键测试',
    icon: 'RocketOutlined',
    element: <MerchantConfigTest />,
  },
  {
    path: 'callback-simulator',
    name: '回调模拟',
    icon: 'BulbOutlined',
    element: <CallbackSimulator />,
  },
  {
    path: 'fee-config',
    name: '手续费配置',
    icon: 'MoneyCollectOutlined',
    element: <FeeConfig />,
  },
  {
    path: 'api-stats',
    name: '接口统计',
    icon: 'BarChartOutlined',
    element: <ApiStats />,
  },
  {
    path: 'payment-page-editor',
    name: '支付页面定制',
    icon: 'PaletteOutlined',
    element: <PaymentPageEditor />,
  },
  {
    path: 'marketing',
    name: '营销工具',
    icon: 'GiftOutlined',
    children: [
      {
        path: 'pay-link',
        name: '支付链接',
        element: <PayLinkManage />,
      },
      {
        path: 'coupon',
        name: '优惠券管理',
        element: <CouponManage />,
      },
      {
        path: 'activity',
        name: '活动配置',
        element: <ActivityConfig />,
      },
    ],
  },
  {
    path: 'agent',
    name: '代理管理',
    icon: 'TeamOutlined',
    children: [
      {
        path: 'tree',
        name: '代理关系图',
        element: <AgentTree />,
      },
      {
        path: 'subordinate',
        name: '下级商户管理',
        element: <SubordinateMerchant />,
      },
      {
        path: 'profit-rule',
        name: '分润规则管理',
        element: <AgentProfitRule />,
      },
      {
        path: 'profit',
        name: '分润记录',
        element: <AgentProfit />,
      },
      {
        path: 'withdraw',
        name: '佣金提现',
        element: <AgentWithdraw />,
      },
    ],
  },
];

export const publicRoutes: RouteConfigItem[] = [
  {
    path: 'login',
    name: '登录',
    element: <Login />,
  },
  {
    path: 'h5/payment/:merchantNo',
    name: 'H5支付',
    element: <PaymentPageH5 />,
    hidden: true,
  },
];

export const findBreadcrumb = (
  pathname: string,
  items: RouteConfigItem[] = routesConfig,
  prefix = '',
  parents: { title: string }[] = []
): { title: string }[] => {
  for (const item of items) {
    const fullPath = prefix + item.path;
    if (item.name) {
      const currentParents = [...parents, { title: item.name }];
      if (pathname === '/' + fullPath || pathname.replace(/\/$/, '') === '/' + fullPath) {
        return currentParents;
      }
      if (item.children) {
        const result = findBreadcrumb(pathname, item.children, fullPath + '/', currentParents);
        if (result.length > 0) return result;
      }
    } else if (item.children) {
      const result = findBreadcrumb(pathname, item.children, fullPath + '/', parents);
      if (result.length > 0) return result;
    }
  }
  return parents.length > 0 ? parents : [{ title: '首页' }];
};

const AuthGuard = ({ children }: { children: React.ReactNode }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { isLoggedIn, checkAuth } = useUserStore();

  useEffect(() => {
    if (!isLoggedIn && !checkAuth()) {
      navigate('/login', { replace: true, state: { from: location.pathname } });
    }
  }, [isLoggedIn, checkAuth, navigate, location.pathname]);

  if (!isLoggedIn && !checkAuth()) {
    return null;
  }

  return <>{children}</>;
};

const PublicGuard = ({ children }: { children: React.ReactNode }) => {
  const navigate = useNavigate();
  const { isLoggedIn, checkAuth } = useUserStore();

  useEffect(() => {
    if (isLoggedIn || checkAuth()) {
      navigate('/dashboard', { replace: true });
    }
  }, [isLoggedIn, checkAuth, navigate]);

  return <>{children}</>;
};

const LoadingFallback = () => (
  <div
    style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      height: '100%',
      minHeight: 300,
    }}
  >
    <Spin size="large" tip="加载中..." />
  </div>
);

const AppRouter = () => {
  return (
    <Suspense fallback={<LoadingFallback />}>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route
          path="/login"
          element={
            <PublicGuard>
              <Login />
            </PublicGuard>
          }
        />
        <Route
          path="/h5/payment/:merchantNo"
          element={<PaymentPageH5 />}
        />
        <Route
          path="/*"
          element={
            <AuthGuard>
              <MainLayout />
            </AuthGuard>
          }
        />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  );
};

export default AppRouter;

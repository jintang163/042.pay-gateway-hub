import { Routes, Route, Navigate } from 'react-router-dom';
import { lazy, Suspense, useEffect } from 'react';
import { Spin } from 'antd';
import { MainLayout } from '@/layouts';
import Login from '@/pages/Login';
import { useUserStore } from '@/store';
import { useLocation, useNavigate } from 'react-router-dom';

const Dashboard = lazy(() => import('@/pages/Dashboard'));
const MerchantApply = lazy(() => import('@/pages/MerchantApply'));
const PayConfig = lazy(() => import('@/pages/PayConfig'));
const OrderList = lazy(() => import('@/pages/OrderList'));
const RefundList = lazy(() => import('@/pages/RefundList'));
const Settlement = lazy(() => import('@/pages/Settlement'));
const RiskMonitor = lazy(() => import('@/pages/RiskMonitor'));
const SandboxTest = lazy(() => import('@/pages/SandboxTest'));
const ApiStats = lazy(() => import('@/pages/ApiStats'));

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
    ],
  },
  {
    path: 'settlement',
    name: '结算管理',
    icon: 'WalletOutlined',
    element: <Settlement />,
  },
  {
    path: 'risk',
    name: '风控监控',
    icon: 'AlertOutlined',
    element: <RiskMonitor />,
  },
  {
    path: 'sandbox',
    name: '沙箱测试',
    icon: 'ExperimentOutlined',
    element: <SandboxTest />,
  },
  {
    path: 'api-stats',
    name: '接口统计',
    icon: 'BarChartOutlined',
    element: <ApiStats />,
  },
];

export const publicRoutes: RouteConfigItem[] = [
  {
    path: 'login',
    name: '登录',
    element: <Login />,
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

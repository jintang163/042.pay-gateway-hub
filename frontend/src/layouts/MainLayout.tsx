import { useState } from 'react';
import { Layout, Menu, Breadcrumb, Dropdown, Avatar, Button, theme, Tooltip, Switch, Tag, Modal } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  DashboardOutlined,
  ShopOutlined,
  KeyOutlined,
  FileTextOutlined,
  RollbackOutlined,
  WalletOutlined,
  AlertOutlined,
  ExperimentOutlined,
  BarChartOutlined,
  BulbOutlined,
  MoneyCollectOutlined,
  PaletteOutlined,
  RocketOutlined,
  TeamOutlined,
  ExclamationCircleOutlined,
  GiftOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation, Routes, Route } from 'react-router-dom';
import type { MenuProps } from 'antd';
import { useUserStore, useAppStore } from '@/store';
import { routesConfig, findBreadcrumb } from '@/router';

const { Header, Sider, Content } = Layout;

type MenuItem = Required<MenuProps>['items'][number];

const iconMap: Record<string, React.ReactNode> = {
  DashboardOutlined: <DashboardOutlined />,
  ShopOutlined: <ShopOutlined />,
  KeyOutlined: <KeyOutlined />,
  FileTextOutlined: <FileTextOutlined />,
  RollbackOutlined: <RollbackOutlined />,
  WalletOutlined: <WalletOutlined />,
  AlertOutlined: <AlertOutlined />,
  ExperimentOutlined: <ExperimentOutlined />,
  BarChartOutlined: <BarChartOutlined />,
  BulbOutlined: <BulbOutlined />,
  MoneyCollectOutlined: <MoneyCollectOutlined />,
  PaletteOutlined: <PaletteOutlined />,
  RocketOutlined: <RocketOutlined />,
  TeamOutlined: <TeamOutlined />,
  GiftOutlined: <GiftOutlined />,
};

const buildMenuItems = (items: typeof routesConfig, prefix = ''): MenuItem[] => {
  return items
    .filter((item) => !item.hidden)
    .map((item) => {
      const fullPath = prefix + item.path;
      const icon = item.icon ? iconMap[item.icon] : undefined;
      const baseItem: MenuItem = {
        key: fullPath,
        icon,
        label: item.name,
      };
      if (item.children && item.children.length > 0) {
        return {
          ...baseItem,
          children: buildMenuItems(item.children, fullPath + '/'),
        };
      }
      return baseItem;
    })
    .filter(Boolean) as MenuItem[];
};

const MainLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useUserStore();
  const { collapsed, toggleCollapsed, theme: appTheme, toggleTheme, sandboxMode, toggleSandboxMode } = useAppStore();
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();
  const [openKeys, setOpenKeys] = useState<string[]>(['']);

  const handleSandboxToggle = (checked: boolean) => {
    if (checked) {
      Modal.confirm({
        title: '启用沙箱环境',
        icon: <ExclamationCircleOutlined />,
        content: '沙箱环境将使用独立的测试数据库，所有操作不会影响真实数据。数据每日凌晨4点自动清理。',
        okText: '确认启用',
        cancelText: '取消',
        onOk: () => {
          toggleSandboxMode();
        },
      });
    } else {
      toggleSandboxMode();
    }
  };

  const menuItems = buildMenuItems(routesConfig);
  const breadcrumbItems = findBreadcrumb(location.pathname);

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    navigate(key);
  };

  const handleOpenChange: MenuProps['onOpenChange'] = (keys) => {
    setOpenKeys(keys.length > 0 ? keys : ['']);
  };

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '设置',
      onClick: () => navigate('/settings'),
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      danger: true,
      onClick: () => {
        logout();
        navigate('/login', { replace: true });
      },
    },
  ];

  const renderRoutes = (items: typeof routesConfig, prefix = ''): React.ReactNode => {
    return items.map((item) => {
      const fullPath = prefix + item.path;
      if (item.children && item.children.length > 0) {
        return renderRoutes(item.children, fullPath + '/');
      }
      return <Route key={fullPath} path={fullPath} element={item.element} />;
    });
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={240}
        theme={appTheme}
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'sticky',
          top: 0,
          left: 0,
        }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            padding: collapsed ? 0 : '0 24px',
            color: '#fff',
            fontSize: collapsed ? 16 : 18,
            fontWeight: 600,
            borderBottom: '1px solid rgba(255,255,255,0.1)',
          }}
        >
          {collapsed ? 'PG' : '支付网关管理平台'}
        </div>
        <Menu
          theme={appTheme}
          mode="inline"
          selectedKeys={[location.pathname]}
          openKeys={openKeys}
          onOpenChange={handleOpenChange}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 16px',
            background: colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={toggleCollapsed}
              style={{ fontSize: '16px', width: 48, height: 48 }}
            />
            <Breadcrumb items={breadcrumbItems} />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Tooltip title="沙箱环境开关">
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  padding: '0 12px',
                  height: 32,
                  borderRadius: 16,
                  background: sandboxMode ? '#fffbe6' : 'transparent',
                  border: sandboxMode ? '1px solid #ffe58f' : '1px solid transparent',
                }}
              >
                <ExperimentOutlined style={{ color: sandboxMode ? '#faad14' : '#999' }} />
                <span style={{ fontSize: 13, color: sandboxMode ? '#d48806' : '#666' }}>沙箱</span>
                <Switch
                  size="small"
                  checked={sandboxMode}
                  onChange={handleSandboxToggle}
                />
              </div>
            </Tooltip>
            {sandboxMode && (
              <Tag color="warning" icon={<ExperimentOutlined />}>
                沙箱环境
              </Tag>
            )}
            <Tooltip title={appTheme === 'light' ? '切换深色模式' : '切换浅色模式'}>
              <Button
                type="text"
                icon={<BulbOutlined />}
                onClick={toggleTheme}
                style={{ width: 48, height: 48 }}
              />
            </Tooltip>
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" arrow>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  cursor: 'pointer',
                  padding: '0 8px',
                  borderRadius: 4,
                }}
              >
                <Avatar size="small" icon={<UserOutlined />} src={user?.avatar} />
                <span>{user?.nickname || user?.username || '管理员'}</span>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content
          style={{
            margin: '16px',
            padding: 24,
            minHeight: 'calc(100vh - 64px - 32px)',
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
          }}
        >
          <Routes>{renderRoutes(routesConfig)}</Routes>
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;

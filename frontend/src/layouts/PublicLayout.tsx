import { Layout } from 'antd';
import { Routes, Route } from 'react-router-dom';
import { publicRoutes } from '@/router';

const { Content } = Layout;

const PublicLayout = () => {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Content style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Routes>
          {publicRoutes.map((route) => (
            <Route key={route.path} path={route.path} element={route.element} />
          ))}
        </Routes>
      </Content>
    </Layout>
  );
};

export default PublicLayout;

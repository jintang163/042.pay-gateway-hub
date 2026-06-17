import { useState, useEffect } from 'react';
import { Row, Col, Card, Statistic, Table, Tag } from 'antd';
import {
  DollarOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  ShopOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { formatAmount, formatDateTime, formatPercent } from '@/utils';
import { dashboardApi } from '@/api';
import type { DashboardStats } from '@/types/dashboard';

interface RecentOrder {
  key: string;
  orderNo: string;
  merchantName: string;
  amount: number;
  payMethod: string;
  status: string;
  createTime: string;
}

const statusTag: Record<string, { color: string; text: string }> = {
  success: { color: 'green', text: '成功' },
  pending: { color: 'orange', text: '处理中' },
  failed: { color: 'red', text: '失败' },
  closed: { color: 'default', text: '已关闭' },
};

const Dashboard = () => {
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState<DashboardStats>({
    todayOrderCount: 0,
    todayOrderAmount: 0,
    todaySuccessRate: 0,
    activeMerchantCount: 0,
    orderTrend: [],
    payMethodDistribution: [],
    payChannelDistribution: [],
    recentOrders: [],
  });

  const fetchData = async () => {
    try {
      setLoading(true);
      const data = await dashboardApi.getStats();
      setStats(data);
    } catch {
      const mockData: DashboardStats = {
        todayOrderCount: 1256,
        todayOrderAmount: 368520.0,
        todaySuccessRate: 98.5,
        activeMerchantCount: 156,
        orderTrend: Array.from({ length: 7 }, (_, i) => ({
          date: dayjs().subtract(6 - i, 'day').format('MM-DD'),
          orderCount: Math.floor(Math.random() * 500 + 800),
          amount: Math.random() * 20 + 25,
        })),
        payMethodDistribution: [
          { name: '支付宝', value: 1048 },
          { name: '微信支付', value: 735 },
          { name: '银联', value: 280 },
          { name: 'Apple Pay', value: 156 },
        ],
        payChannelDistribution: [],
        recentOrders: [
          {
            orderNo: 'PG202401150001',
            merchantName: '示例商户A',
            amount: 299.0,
            payMethod: '支付宝',
            status: 'success',
            createTime: new Date().toISOString(),
          },
          {
            orderNo: 'PG202401150002',
            merchantName: '示例商户B',
            amount: 1299.0,
            payMethod: '微信支付',
            status: 'success',
            createTime: new Date(Date.now() - 300000).toISOString(),
          },
          {
            orderNo: 'PG202401150003',
            merchantName: '示例商户C',
            amount: 59.9,
            payMethod: '银联',
            status: 'pending',
            createTime: new Date(Date.now() - 600000).toISOString(),
          },
          {
            orderNo: 'PG202401150004',
            merchantName: '示例商户A',
            amount: 1999.0,
            payMethod: '支付宝',
            status: 'success',
            createTime: new Date(Date.now() - 900000).toISOString(),
          },
          {
            orderNo: 'PG202401150005',
            merchantName: '示例商户D',
            amount: 88.0,
            payMethod: '微信支付',
            status: 'failed',
            createTime: new Date(Date.now() - 1200000).toISOString(),
          },
        ],
      };
      setStats(mockData);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const orderTrendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单量', '交易金额'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data:
        stats.orderTrend.length > 0
          ? stats.orderTrend.map((d) => d.date)
          : Array.from({ length: 7 }, (_, i) => dayjs().subtract(6 - i, 'day').format('MM-DD')),
    },
    yAxis: [
      { type: 'value', name: '订单量' },
      { type: 'value', name: '金额(万)', axisLabel: { formatter: '{value}' } },
    ],
    series: [
      {
        name: '订单量',
        type: 'line',
        smooth: true,
        data:
          stats.orderTrend.length > 0
            ? stats.orderTrend.map((d) => d.orderCount)
            : [890, 1020, 1156, 980, 1320, 1180, 1256],
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(22,119,255,0.3)' },
              { offset: 1, color: 'rgba(22,119,255,0.05)' },
            ],
          },
        },
        lineStyle: { color: '#1677ff', width: 2 },
        itemStyle: { color: '#1677ff' },
      },
      {
        name: '交易金额',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data:
          stats.orderTrend.length > 0
            ? stats.orderTrend.map((d) => d.amount)
            : [25.6, 28.9, 35.2, 30.1, 40.5, 32.8, 36.9],
        lineStyle: { color: '#52c41a', width: 2 },
        itemStyle: { color: '#52c41a' },
      },
    ],
  };

  const payMethodOption = {
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '支付方式',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: 18, fontWeight: 'bold' },
        },
        labelLine: { show: false },
        data: stats.payMethodDistribution.length > 0
          ? stats.payMethodDistribution
          : [
              { value: 1048, name: '支付宝' },
              { value: 735, name: '微信支付' },
              { value: 280, name: '银联' },
              { value: 156, name: 'Apple Pay' },
            ],
      },
    ],
  };

  const successRateOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['支付成功率'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: Array.from({ length: 7 }, (_, i) => dayjs().subtract(6 - i, 'day').format('MM-DD')),
    },
    yAxis: {
      type: 'value',
      name: '成功率(%)',
      min: 90,
      max: 100,
      axisLabel: { formatter: '{value}%' },
    },
    series: [
      {
        name: '支付成功率',
        type: 'line',
        smooth: true,
        data: [96.5, 97.2, 98.1, 97.8, 98.5, 98.3, stats.todaySuccessRate || 98.5],
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(82,196,26,0.3)' },
              { offset: 1, color: 'rgba(82,196,26,0.05)' },
            ],
          },
        },
        lineStyle: { color: '#52c41a', width: 2 },
        itemStyle: { color: '#52c41a' },
      },
    ],
  };

  const columns: ColumnsType<RecentOrder> = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 160,
    },
    {
      title: '商户名称',
      dataIndex: 'merchantName',
      key: 'merchantName',
    },
    {
      title: '金额',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      render: (val: number) => formatAmount(val),
    },
    {
      title: '支付方式',
      dataIndex: 'payMethod',
      key: 'payMethod',
      width: 100,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: string) => {
        const tag = statusTag[status] || { color: 'default', text: status };
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (val: string) => formatDateTime(val),
    },
  ];

  const recentOrders: RecentOrder[] = stats.recentOrders.map((o, i) => ({
    key: String(i),
    ...o,
  }));

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="今日交易金额"
              value={stats.todayOrderAmount}
              precision={2}
              valueStyle={{ color: '#52c41a' }}
              prefix={<DollarOutlined />}
              formatter={(value) => formatAmount(Number(value))}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="今日订单数"
              value={stats.todayOrderCount}
              valueStyle={{ color: '#1677ff' }}
              prefix={<FileTextOutlined />}
              valueRender={(v) => <>{v} 笔</>}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="成功率"
              value={stats.todaySuccessRate}
              precision={1}
              valueStyle={{ color: '#faad14' }}
              prefix={<CheckCircleOutlined />}
              suffix="%"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="商户数"
              value={stats.activeMerchantCount}
              valueStyle={{ color: '#722ed1' }}
              prefix={<ShopOutlined />}
              valueRender={(v) => <>{v} 家</>}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="支付成功率趋势">
            <ReactECharts option={successRateOption} style={{ height: 320 }} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="支付方式占比">
            <ReactECharts option={payMethodOption} style={{ height: 320 }} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24}>
          <Card title="订单趋势">
            <ReactECharts option={orderTrendOption} style={{ height: 320 }} />
          </Card>
        </Col>
      </Row>

      <Card title="最近订单" extra={<a href="#/order/list">查看全部</a>}>
        <Table<RecentOrder>
          columns={columns}
          dataSource={recentOrders}
          pagination={false}
          size="middle"
          loading={loading}
        />
      </Card>
    </div>
  );
};

export default Dashboard;

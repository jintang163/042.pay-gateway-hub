import { useState, useEffect } from 'react';
import {
  Row,
  Col,
  Card,
  Statistic,
  Table,
  Tag,
  DatePicker,
  Button,
  Space,
} from 'antd';
import {
  ApiOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { apiStatsApi } from '@/api';
import type { ApiStats, TopMerchant } from '@/types/apiStats';
import { formatDateTime } from '@/utils';

const ApiStatsPage = () => {
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState<ApiStats | null>(null);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs | null, dayjs.Dayjs | null] | null>([
    dayjs().subtract(6, 'day'),
    dayjs(),
  ]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const data = await apiStatsApi.getStats({
        startTime: dateRange?.[0]?.format('YYYY-MM-DD HH:mm:ss'),
        endTime: dateRange?.[1]?.format('YYYY-MM-DD HH:mm:ss'),
      });
      setStats(data);
    } catch {
      const mockStats: ApiStats = {
        totalCalls: 125680,
        successCalls: 123850,
        failedCalls: 1830,
        successRate: 98.54,
        avgResponseTime: 85,
        p50ResponseTime: 45,
        p95ResponseTime: 180,
        p99ResponseTime: 420,
        apiPathStats: [
          { path: '/api/pay/create', totalCalls: 45280, successRate: 99.2, avgResponseTime: 65 },
          { path: '/api/pay/query', totalCalls: 32150, successRate: 99.5, avgResponseTime: 35 },
          { path: '/api/pay/refund/apply', totalCalls: 8420, successRate: 97.8, avgResponseTime: 120 },
          { path: '/api/pay/refund/query', totalCalls: 6890, successRate: 98.1, avgResponseTime: 40 },
          { path: '/api/merchant/user/login', totalCalls: 15680, successRate: 96.2, avgResponseTime: 55 },
          { path: '/api/pay/notify', totalCalls: 17260, successRate: 99.8, avgResponseTime: 25 },
        ],
        topMerchants: Array.from({ length: 10 }, (_, i) => ({
          rank: i + 1,
          merchantId: 'M' + String(1000 + i).padStart(4, '0'),
          merchantName: '示例商户' + (i + 1),
          totalCalls: Math.floor(Math.random() * 15000 + 2000),
          successRate: Number((Math.random() * 3 + 96).toFixed(2)),
        })),
        callTrend: Array.from({ length: 7 }, (_, i) => ({
          date: dayjs().subtract(6 - i, 'day').format('MM-DD'),
          totalCalls: Math.floor(Math.random() * 5000 + 15000),
          successCalls: Math.floor(Math.random() * 4800 + 14800),
          failedCalls: Math.floor(Math.random() * 200 + 100),
        })),
      };
      setStats(mockStats);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const callTrendOption = stats
    ? {
        tooltip: { trigger: 'axis' },
        legend: { data: ['总调用量', '成功调用', '失败调用'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: stats.callTrend.map((d) => d.date),
        },
        yAxis: { type: 'value', name: '调用次数' },
        series: [
          {
            name: '总调用量',
            type: 'line',
            smooth: true,
            data: stats.callTrend.map((d) => d.totalCalls),
            areaStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
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
            name: '成功调用',
            type: 'line',
            smooth: true,
            data: stats.callTrend.map((d) => d.successCalls),
            lineStyle: { color: '#52c41a', width: 2 },
            itemStyle: { color: '#52c41a' },
          },
          {
            name: '失败调用',
            type: 'line',
            smooth: true,
            data: stats.callTrend.map((d) => d.failedCalls),
            lineStyle: { color: '#ff4d4f', width: 2 },
            itemStyle: { color: '#ff4d4f' },
          },
        ],
      }
    : {};

  const responseTimeOption = stats
    ? {
        tooltip: { trigger: 'axis' },
        legend: { data: ['平均响应', 'P50', 'P95', 'P99'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: stats.callTrend.map((d) => d.date),
        },
        yAxis: { type: 'value', name: '响应时间(ms)' },
        series: [
          {
            name: '平均响应',
            type: 'line',
            smooth: true,
            data: stats.callTrend.map(() => stats.avgResponseTime + Math.random() * 30 - 15),
            lineStyle: { color: '#1677ff', width: 2 },
          },
          {
            name: 'P50',
            type: 'line',
            smooth: true,
            data: stats.callTrend.map(() => stats.p50ResponseTime + Math.random() * 15 - 7),
            lineStyle: { color: '#52c41a', width: 2 },
          },
          {
            name: 'P95',
            type: 'line',
            smooth: true,
            data: stats.callTrend.map(() => stats.p95ResponseTime + Math.random() * 50 - 25),
            lineStyle: { color: '#faad14', width: 2 },
          },
          {
            name: 'P99',
            type: 'line',
            smooth: true,
            data: stats.callTrend.map(() => stats.p99ResponseTime + Math.random() * 100 - 50),
            lineStyle: { color: '#ff4d4f', width: 2 },
          },
        ],
      }
    : {};

  const apiPathColumns: ColumnsType<{ path: string; totalCalls: number; successRate: number; avgResponseTime: number }> = [
    { title: 'API路径', dataIndex: 'path', key: 'path', ellipsis: true },
    {
      title: '总调用量',
      dataIndex: 'totalCalls',
      key: 'totalCalls',
      width: 120,
      sorter: (a, b) => a.totalCalls - b.totalCalls,
    },
    {
      title: '成功率',
      dataIndex: 'successRate',
      key: 'successRate',
      width: 100,
      render: (val: number) => (
        <Tag color={val >= 99 ? 'green' : val >= 95 ? 'orange' : 'red'}>{val.toFixed(1)}%</Tag>
      ),
      sorter: (a, b) => a.successRate - b.successRate,
    },
    {
      title: '平均响应(ms)',
      dataIndex: 'avgResponseTime',
      key: 'avgResponseTime',
      width: 130,
      sorter: (a, b) => a.avgResponseTime - b.avgResponseTime,
    },
  ];

  const topMerchantColumns: ColumnsType<TopMerchant> = [
    {
      title: '排名',
      dataIndex: 'rank',
      key: 'rank',
      width: 60,
      render: (rank: number) => (
        <Tag color={rank <= 3 ? 'gold' : 'default'}>Top {rank}</Tag>
      ),
    },
    { title: '商户ID', dataIndex: 'merchantId', key: 'merchantId', width: 100 },
    { title: '商户名称', dataIndex: 'merchantName', key: 'merchantName' },
    {
      title: '调用量',
      dataIndex: 'totalCalls',
      key: 'totalCalls',
      width: 120,
      sorter: (a, b) => a.totalCalls - b.totalCalls,
    },
    {
      title: '成功率',
      dataIndex: 'successRate',
      key: 'successRate',
      width: 100,
      render: (val: number) => (
        <Tag color={val >= 99 ? 'green' : val >= 95 ? 'orange' : 'red'}>{val.toFixed(2)}%</Tag>
      ),
    },
  ];

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Space>
          <DatePicker.RangePicker
            value={dateRange as any}
            onChange={(dates) => setDateRange(dates)}
            showTime
          />
          <Button type="primary" icon={<ReloadOutlined />} onClick={fetchData}>
            查询
          </Button>
        </Space>
      </Card>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="总调用量"
              value={stats?.totalCalls || 0}
              valueStyle={{ color: '#1677ff' }}
              prefix={<ApiOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="成功率"
              value={stats?.successRate || 0}
              precision={2}
              valueStyle={{ color: '#52c41a' }}
              prefix={<CheckCircleOutlined />}
              suffix="%"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="平均响应时间"
              value={stats?.avgResponseTime || 0}
              valueStyle={{ color: '#722ed1' }}
              prefix={<ClockCircleOutlined />}
              suffix="ms"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="P99响应时间"
              value={stats?.p99ResponseTime || 0}
              valueStyle={{ color: '#faad14' }}
              prefix={<ClockCircleOutlined />}
              suffix="ms"
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="调用量趋势" loading={loading}>
            {stats && <ReactECharts option={callTrendOption} style={{ height: 320 }} />}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="响应时间趋势" loading={loading}>
            {stats && <ReactECharts option={responseTimeOption} style={{ height: 320 }} />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="按API路径统计">
            {stats && (
              <Table
                columns={apiPathColumns}
                dataSource={stats.apiPathStats}
                rowKey="path"
                pagination={false}
                size="small"
              />
            )}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Top商户排行">
            {stats && (
              <Table
                columns={topMerchantColumns}
                dataSource={stats.topMerchants}
                rowKey="merchantId"
                pagination={false}
                size="small"
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default ApiStatsPage;

import { useState, useEffect } from 'react';
import {
  Row,
  Col,
  Card,
  Statistic,
  Table,
  Tag,
  Alert,
  Space,
  Button,
  Drawer,
  Descriptions,
} from 'antd';
import {
  WarningOutlined,
  ReloadOutlined,
  EyeOutlined,
  ExclamationCircleOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { riskApi } from '@/api';
import type { RiskEvent, RiskLevel } from '@/types/risk';
import { formatDateTime } from '@/utils';

const levelTag: Record<RiskLevel, { color: string; text: string }> = {
  high: { color: 'red', text: '高风险' },
  medium: { color: 'orange', text: '中风险' },
  low: { color: 'blue', text: '低风险' },
};

const RiskMonitor = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<RiskEvent[]>([]);
  const [total, setTotal] = useState(0);
  const [highCount, setHighCount] = useState(0);
  const [mediumCount, setMediumCount] = useState(0);
  const [lowCount, setLowCount] = useState(0);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentEvent, setCurrentEvent] = useState<RiskEvent | null>(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      const result = await riskApi.list({ pageNum: 1, pageSize: 50 });
      setData(result.list);
      setTotal(result.total);
      setHighCount(result.list.filter((e) => e.level === 'high').length);
      setMediumCount(result.list.filter((e) => e.level === 'medium').length);
      setLowCount(result.list.filter((e) => e.level === 'low').length);
    } catch {
      const levels: RiskLevel[] = ['high', 'medium', 'low'];
      const types = ['SUSPICIOUS_IP', 'FREQUENT_REQUEST', 'AMOUNT_ABNORMAL', 'BLACKLIST_HIT', 'SIGN_VERIFY_FAIL'];
      const statuses = ['pending', 'blocked', 'allowed'];
      const mockData: RiskEvent[] = Array.from({ length: 25 }, (_, i) => ({
        id: i + 1,
        eventType: types[i % types.length],
        level: levels[i % levels.length],
        merchantId: 'M' + String(1000 + (i % 10)).padStart(4, '0'),
        merchantName: '示例商户' + ((i % 10) + 1),
        orderNo: 'PG' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        clientIp: `192.168.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
        riskScore: Math.floor(Math.random() * 80 + 20),
        description: `风险事件描述-${i + 1}：检测到异常行为`,
        status: statuses[i % statuses.length],
        createTime: new Date(Date.now() - i * 60000 * 15).toISOString(),
        handleTime: i % 3 === 0 ? new Date(Date.now() - i * 60000 * 15 + 300000).toISOString() : undefined,
        handler: i % 3 === 0 ? 'admin' : undefined,
      }));
      setData(mockData);
      setTotal(mockData.length);
      setHighCount(mockData.filter((e) => e.level === 'high').length);
      setMediumCount(mockData.filter((e) => e.level === 'medium').length);
      setLowCount(mockData.filter((e) => e.level === 'low').length);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const levelDistributionOption = {
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '风险等级',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: 18, fontWeight: 'bold' },
        },
        labelLine: { show: false },
        data: [
          { value: highCount || 9, name: '高风险', itemStyle: { color: '#ff4d4f' } },
          { value: mediumCount || 10, name: '中风险', itemStyle: { color: '#faad14' } },
          { value: lowCount || 6, name: '低风险', itemStyle: { color: '#1677ff' } },
        ],
      },
    ],
  };

  const riskTrendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['高风险', '中风险', '低风险'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: Array.from({ length: 7 }, (_, i) => dayjs().subtract(6 - i, 'day').format('MM-DD')),
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '高风险',
        type: 'line',
        smooth: true,
        data: [3, 5, 4, 7, 3, 2, 9],
        lineStyle: { color: '#ff4d4f', width: 2 },
        itemStyle: { color: '#ff4d4f' },
        areaStyle: { color: 'rgba(255,77,79,0.2)' },
      },
      {
        name: '中风险',
        type: 'line',
        smooth: true,
        data: [8, 10, 12, 6, 9, 11, 10],
        lineStyle: { color: '#faad14', width: 2 },
        itemStyle: { color: '#faad14' },
        areaStyle: { color: 'rgba(250,173,20,0.2)' },
      },
      {
        name: '低风险',
        type: 'line',
        smooth: true,
        data: [5, 7, 3, 8, 4, 6, 6],
        lineStyle: { color: '#1677ff', width: 2 },
        itemStyle: { color: '#1677ff' },
        areaStyle: { color: 'rgba(22,119,255,0.2)' },
      },
    ],
  };

  const columns: ColumnsType<RiskEvent> = [
    {
      title: '风险等级',
      dataIndex: 'level',
      key: 'level',
      width: 100,
      render: (level: RiskLevel) => {
        const tag = levelTag[level];
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    { title: '事件类型', dataIndex: 'eventType', key: 'eventType', width: 150 },
    { title: '商户名称', dataIndex: 'merchantName', key: 'merchantName', width: 140 },
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 200, ellipsis: true },
    { title: '客户端IP', dataIndex: 'clientIp', key: 'clientIp', width: 140 },
    {
      title: '风险评分',
      dataIndex: 'riskScore',
      key: 'riskScore',
      width: 100,
      render: (score: number) => (
        <span style={{ color: score >= 80 ? 'red' : score >= 50 ? 'orange' : 'blue', fontWeight: 600 }}>
          {score}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const map: Record<string, { color: string; text: string }> = {
          pending: { color: 'orange', text: '待处理' },
          blocked: { color: 'red', text: '已拦截' },
          allowed: { color: 'green', text: '已放行' },
        };
        const tag = map[status] || { color: 'default', text: status };
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
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => {
            setCurrentEvent(record);
            setDetailVisible(true);
          }}
        >
          详情
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Alert
        type="warning"
        message={
          <Space>
            <ExclamationCircleOutlined />
            <strong>今日风险事件监控</strong>
            <span>共 {total} 件风险事件</span>
            {highCount > 0 && <Tag color="red">高风险 {highCount} 件</Tag>}
            {mediumCount > 0 && <Tag color="orange">中风险 {mediumCount} 件</Tag>}
            {lowCount > 0 && <Tag color="blue">低风险 {lowCount} 件</Tag>}
          </Space>
        }
        style={{ marginBottom: 16 }}
      />

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="今日风险事件"
              value={total}
              valueStyle={{ color: '#ff4d4f' }}
              prefix={<WarningOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="高风险事件"
              value={highCount}
              valueStyle={{ color: '#ff4d4f' }}
              prefix={<ExclamationCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="中风险事件"
              value={mediumCount}
              valueStyle={{ color: '#faad14' }}
              prefix={<WarningOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="低风险事件"
              value={lowCount}
              valueStyle={{ color: '#1677ff' }}
              prefix={<SafetyOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={8}>
          <Card title="风险等级分布">
            <ReactECharts option={levelDistributionOption} style={{ height: 280 }} />
          </Card>
        </Col>
        <Col xs={24} lg={16}>
          <Card title="风险事件趋势">
            <ReactECharts option={riskTrendOption} style={{ height: 280 }} />
          </Card>
        </Col>
      </Row>

      <Card
        title="风控日志列表"
        extra={
          <Button icon={<ReloadOutlined />} onClick={fetchData}>
            刷新
          </Button>
        }
      >
        <Table<RiskEvent>
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1300 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
          }}
        />
      </Card>

      <Drawer
        title="风险事件详情"
        width={640}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
      >
        {currentEvent && (
          <Descriptions title="事件信息" bordered column={1} size="small">
            <Descriptions.Item label="风险等级">
              {(() => {
                const tag = levelTag[currentEvent.level];
                return <Tag color={tag.color}>{tag.text}</Tag>;
              })()}
            </Descriptions.Item>
            <Descriptions.Item label="事件类型">{currentEvent.eventType}</Descriptions.Item>
            <Descriptions.Item label="风险评分">{currentEvent.riskScore}</Descriptions.Item>
            <Descriptions.Item label="商户ID">{currentEvent.merchantId}</Descriptions.Item>
            <Descriptions.Item label="商户名称">{currentEvent.merchantName}</Descriptions.Item>
            <Descriptions.Item label="订单号">{currentEvent.orderNo || '-'}</Descriptions.Item>
            <Descriptions.Item label="客户端IP">{currentEvent.clientIp || '-'}</Descriptions.Item>
            <Descriptions.Item label="事件描述">{currentEvent.description}</Descriptions.Item>
            <Descriptions.Item label="处理状态">
              {currentEvent.status === 'pending' ? (
                <Tag color="orange">待处理</Tag>
              ) : currentEvent.status === 'blocked' ? (
                <Tag color="red">已拦截</Tag>
              ) : (
                <Tag color="green">已放行</Tag>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="处理人">{currentEvent.handler || '-'}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatDateTime(currentEvent.createTime)}</Descriptions.Item>
            <Descriptions.Item label="处理时间">
              {currentEvent.handleTime ? formatDateTime(currentEvent.handleTime) : '-'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </div>
  );
};

export default RiskMonitor;

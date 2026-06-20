import { useState, useEffect, useMemo } from 'react';
import {
  Card,
  Row,
  Col,
  Statistic,
  DatePicker,
  Table,
  Select,
  Button,
  Space,
  Empty,
  Typography,
  Tag,
} from 'antd';
import {
  EyeOutlined,
  RiseOutlined,
  DollarOutlined,
  ShoppingCartOutlined,
  ReloadOutlined,
  BarChartOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adApi } from '@/api/marketing';
import type { AdDailyStatsVO, AdItemStatsVO, AdStatsOverviewVO } from '@/types/ad';
import { formatAmount } from '@/utils';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const { Text } = Typography;

const positionOptions = [
  { label: '全部位置', value: '' },
  { label: '支付成功页', value: 'PAY_SUCCESS' },
];

const AdStatsPage = () => {
  const [loading, setLoading] = useState(false);
  const [overview, setOverview] = useState<AdStatsOverviewVO | null>(null);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().subtract(29, 'day'),
    dayjs(),
  ]);
  const [filterPosition, setFilterPosition] = useState('');
  const [filterAdCode, setFilterAdCode] = useState<string | undefined>();

  const fetchData = async () => {
    setLoading(true);
    try {
      const resp = await adApi.statsOverview({
        startDate: dateRange[0].format('YYYY-MM-DD'),
        endDate: dateRange[1].format('YYYY-MM-DD'),
        position: filterPosition || undefined,
        adCode: filterAdCode,
      });
      setOverview(resp);
    } catch (e: any) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const dailyColumns: ColumnsType<AdDailyStatsVO> = useMemo(() => [
    {
      title: '统计日期',
      dataIndex: 'statsDate',
      width: 130,
      fixed: 'left',
    },
    {
      title: '曝光量',
      dataIndex: 'impressionCount',
      width: 100,
      align: 'right',
      render: (v) => <strong>{v}</strong>,
    },
    {
      title: '点击量',
      dataIndex: 'clickCount',
      width: 100,
      align: 'right',
      render: (v) => <strong style={{ color: '#1677ff' }}>{v}</strong>,
    },
    {
      title: '有效点击',
      dataIndex: 'validClickCount',
      width: 100,
      align: 'right',
      render: (v) => <span style={{ color: '#52c41a' }}>{v}</span>,
    },
    {
      title: '无效点击',
      dataIndex: 'invalidClickCount',
      width: 100,
      align: 'right',
      render: (v) => <span style={{ color: '#8c8c8c' }}>{v}</span>,
    },
    {
      title: 'CTR',
      dataIndex: 'ctr',
      width: 100,
      align: 'right',
      render: (v: number) => {
        const pct = Number(v || 0);
        return (
          <Tag color={pct >= 3 ? 'green' : pct >= 1 ? 'orange' : 'red'}>
            {pct.toFixed(2)}%
          </Tag>
        );
      },
    },
    {
      title: '平均CPC',
      dataIndex: 'avgCpc',
      width: 120,
      align: 'right',
      render: (v: number) => `¥${Number(v || 0).toFixed(4)}`,
    },
    {
      title: '当日消耗',
      dataIndex: 'totalCost',
      width: 130,
      align: 'right',
      render: (v: number) => <strong style={{ color: '#cf1322' }}>¥{formatAmount(v || 0)}</strong>,
    },
    {
      title: '关联订单',
      dataIndex: 'orderCount',
      width: 100,
      align: 'right',
    },
    {
      title: '订单金额',
      dataIndex: 'orderAmount',
      width: 130,
      align: 'right',
      render: (v: number) => `¥${formatAmount(v || 0)}`,
      fixed: 'right',
    },
  ], []);

  const adColumns: ColumnsType<AdItemStatsVO> = useMemo(() => [
    {
      title: '广告',
      dataIndex: 'adTitle',
      width: 200,
      fixed: 'left',
      render: (t, r) => (
        <Space direction="vertical" size={2}>
          <Text strong>{t}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>{r.adCode}</Text>
        </Space>
      ),
    },
    {
      title: '位置',
      dataIndex: 'positionDesc',
      width: 120,
      render: (d, r) => <Tag color="blue">{d || r.position}</Tag>,
    },
    {
      title: 'CPC单价',
      dataIndex: 'cpcPrice',
      width: 100,
      align: 'right',
      render: (v: number) => `¥${Number(v || 0).toFixed(4)}`,
    },
    {
      title: '曝光量',
      dataIndex: 'impressionCount',
      width: 100,
      align: 'right',
      render: (v) => <strong>{v}</strong>,
    },
    {
      title: '点击量',
      dataIndex: 'clickCount',
      width: 100,
      align: 'right',
      render: (v) => <strong style={{ color: '#1677ff' }}>{v}</strong>,
    },
    {
      title: '有效点击',
      dataIndex: 'validClickCount',
      width: 100,
      align: 'right',
      render: (v) => <span style={{ color: '#52c41a' }}>{v}</span>,
    },
    {
      title: 'CTR',
      dataIndex: 'ctr',
      width: 100,
      align: 'right',
      render: (v: number) => {
        const pct = Number(v || 0);
        return (
          <Tag color={pct >= 3 ? 'green' : pct >= 1 ? 'orange' : 'red'}>
            {pct.toFixed(2)}%
          </Tag>
        );
      },
    },
    {
      title: '平均CPC',
      dataIndex: 'avgCpc',
      width: 120,
      align: 'right',
      render: (v: number) => `¥${Number(v || 0).toFixed(4)}`,
    },
    {
      title: '累计消耗',
      dataIndex: 'totalCost',
      width: 130,
      align: 'right',
      render: (v: number) => <strong style={{ color: '#cf1322' }}>¥{formatAmount(v || 0)}</strong>,
      fixed: 'right',
    },
    {
      title: '状态',
      dataIndex: 'statusDesc',
      width: 100,
      render: (d, r) => (
        <Tag color={r.status === 1 ? 'green' : 'default'}>{d || '-'}</Tag>
      ),
      fixed: 'right',
    },
  ], []);

  const dailyData = overview?.dailyStats || [];
  const adData = overview?.adStats || [];

  return (
    <div>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <RangePicker
            value={dateRange}
            onChange={(v) => v && setDateRange(v as [dayjs.Dayjs, dayjs.Dayjs])}
            allowClear={false}
          />
          <Select
            style={{ width: 160 }}
            value={filterPosition}
            onChange={setFilterPosition}
            options={positionOptions}
          />
          <Select
            allowClear
            style={{ width: 200 }}
            placeholder="选择广告"
            value={filterAdCode}
            onChange={setFilterAdCode}
            options={adData.map((a) => ({ label: `${a.adTitle}(${a.adCode})`, value: a.adCode }))}
            showSearch
            optionFilterProp="label"
          />
          <Button type="primary" icon={<BarChartOutlined />} onClick={fetchData}>
            查询
          </Button>
          <Button icon={<ReloadOutlined />} onClick={fetchData}>刷新</Button>
        </Space>
      </Card>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="广告曝光总量"
              value={overview?.totalImpression ?? 0}
              prefix={<EyeOutlined style={{ color: '#1677ff' }} />}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="有效点击数"
              value={overview?.totalValidClick ?? 0}
              prefix={<RiseOutlined style={{ color: '#52c41a' }} />}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="整体CTR"
              value={overview?.overallCtr ?? 0}
              precision={2}
              suffix="%"
              loading={loading}
              valueStyle={{ color: (overview?.overallCtr ?? 0) >= 3 ? '#52c41a' : '#cf1322' }}
              prefix={<BarChartOutlined />}
            />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="累计消耗金额"
              value={overview?.totalCost ?? 0}
              precision={2}
              prefix="¥"
              loading={loading}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="平均点击成本"
              value={overview?.overallAvgCpc ?? 0}
              precision={4}
              prefix="¥"
              loading={loading}
              prefixCls=""
            />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="无效点击"
              value={overview?.totalInvalidClick ?? 0}
              loading={loading}
              valueStyle={{ color: '#8c8c8c' }}
            />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="关联订单数"
              value={overview?.totalOrder ?? 0}
              prefix={<ShoppingCartOutlined style={{ color: '#eb2f96' }} />}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="关联订单金额"
              value={overview?.totalOrderAmount ?? 0}
              precision={2}
              prefix={<DollarOutlined style={{ color: '#fa8c16' }} />}
              loading={loading}
            />
          </Card>
        </Col>
      </Row>

      <Card
        size="small"
        title={<Space><BarChartOutlined />按日期统计趋势</Space>}
        style={{ marginBottom: 16 }}
      >
        {dailyData.length === 0
          ? <Empty description="暂无数据" />
          : (
            <Table<AdDailyStatsVO>
              rowKey="statsDate"
              size="small"
              columns={dailyColumns}
              dataSource={dailyData}
              loading={loading}
              pagination={{ pageSize: 15, showSizeChanger: false }}
              scroll={{ x: 1200 }}
            />
          )}
      </Card>

      <Card
        size="small"
        title={<Space><BarChartOutlined />按广告维度统计</Space>}
      >
        {adData.length === 0
          ? <Empty description="暂无数据" />
          : (
            <Table<AdItemStatsVO>
              rowKey="adCode"
              size="small"
              columns={adColumns}
              dataSource={adData}
              loading={loading}
              pagination={{ pageSize: 15, showSizeChanger: false }}
              scroll={{ x: 1300 }}
            />
          )}
      </Card>
    </div>
  );
};

export default AdStatsPage;

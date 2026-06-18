import { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Card,
  Form,
  Input,
  Select,
  DatePicker,
  message,
  Row,
  Col,
  Statistic,
  Tooltip,
} from 'antd';
import {
  ReloadOutlined,
  SearchOutlined,
  DollarOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { agentProfitApi } from '@/api';
import type { AgentProfitRecord } from '@/types/agent';
import { useUserStore } from '@/store';
import { formatDateTime, formatAmount } from '@/utils';

const { RangePicker } = DatePicker;

const AgentProfitPage = () => {
  const { user } = useUserStore();
  const isAdmin = user?.role === 'admin' || user?.role === 'operator';

  const [queryForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AgentProfitRecord[]>([]);
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });

  const [stats, setStats] = useState({
    totalProfit: 0,
    pendingCount: 0,
    settledCount: 0,
  });

  const currentMerchantNo = isAdmin ? '' : user?.username || '';

  const fetchData = async (page = pagination.current, size = pagination.size) => {
    try {
      setLoading(true);
      const values = queryForm.getFieldsValue();
      const params: any = {
        pageNum: page,
        pageSize: size,
        ...values,
      };
      if (values.dateRange && values.dateRange.length === 2) {
        params.startDate = values.dateRange[0].format('YYYY-MM-DD');
        params.endDate = values.dateRange[1].format('YYYY-MM-DD');
      }
      if (!isAdmin) {
        params.agentMerchantNo = currentMerchantNo;
      }
      const result = await agentProfitApi.list(params);
      setData(result.records);
      setPagination({ current: result.current, size: result.size, total: result.total });
      const totalProfit = result.records.reduce((sum: number, r: AgentProfitRecord) => sum + r.profitAmount, 0);
      const pendingCount = result.records.filter((r: AgentProfitRecord) => r.profitStatus === 0).length;
      const settledCount = result.records.filter((r: AgentProfitRecord) => r.profitStatus === 1).length;
      setStats({ totalProfit, pendingCount, settledCount });
    } catch (e: any) {
      message.error(e?.message || '加载分润记录失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSearch = () => {
    fetchData(1, pagination.size);
  };

  const handleReset = () => {
    queryForm.resetFields();
    fetchData(1, pagination.size);
  };

  const columns: ColumnsType<AgentProfitRecord> = [
    {
      title: '分润单号',
      dataIndex: 'profitNo',
      key: 'profitNo',
      width: 160,
    },
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 180,
    },
    {
      title: '下级商户',
      key: 'merchant',
      width: 180,
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 500 }}>{r.merchantName}</span>
          <span style={{ color: '#999', fontSize: 12 }}>{r.merchantNo}</span>
        </Space>
      ),
    },
    {
      title: '代理层级',
      dataIndex: 'agentLevel',
      key: 'agentLevel',
      width: 90,
      render: (level: number) => <Tag color="blue">L{level}</Tag>,
    },
    {
      title: '订单金额',
      dataIndex: 'orderAmount',
      key: 'orderAmount',
      width: 120,
      render: (val: number) => <span>¥{formatAmount(val)}</span>,
    },
    {
      title: '手续费',
      dataIndex: 'feeAmount',
      key: 'feeAmount',
      width: 100,
      render: (val: number) => <span style={{ color: '#ff4d4f' }}>¥{formatAmount(val)}</span>,
    },
    {
      title: '分润比例',
      dataIndex: 'commissionRate',
      key: 'commissionRate',
      width: 100,
      render: (rate: number) => <Tag color="gold">{rate || 0}%</Tag>,
    },
    {
      title: '分润金额',
      dataIndex: 'profitAmount',
      key: 'profitAmount',
      width: 120,
      render: (val: number) => (
        <span style={{ color: '#52c41a', fontWeight: 600 }}>¥{formatAmount(val)}</span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'profitStatus',
      key: 'profitStatus',
      width: 100,
      render: (status: number) => {
        let color = 'default';
        let text = '';
        if (status === 0) {
          color = 'orange';
          text = '待结算';
        } else if (status === 1) {
          color = 'green';
          text = '已结算';
        } else if (status === 2) {
          color = 'red';
          text = '结算失败';
        }
        return <Tag color={color}>{text}</Tag>;
      },
    },
    {
      title: '结算日期',
      dataIndex: 'settleDate',
      key: 'settleDate',
      width: 120,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (val: string) => formatDateTime(val),
    },
  ];

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card>
            <Statistic
              title="分润总金额"
              value={stats.totalProfit}
              precision={2}
              suffix="元"
              prefix={<DollarOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
              formatter={(value: number) => formatAmount(value)}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="待结算笔数"
              value={stats.pendingCount}
              prefix={<ClockCircleOutlined style={{ color: '#fa8c16' }} />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="已结算笔数"
              value={stats.settledCount}
              prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      <Card
        title="分润记录"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>
              刷新
            </Button>
          </Space>
        }
      >
        <Form
          form={queryForm}
          layout="inline"
          style={{ marginBottom: 16 }}
          onFinish={handleSearch}
        >
          <Form.Item name="profitNo" label="分润单号">
            <Input placeholder="请输入分润单号" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="orderNo" label="订单号">
            <Input placeholder="请输入订单号" allowClear style={{ width: 180 }} />
          </Form.Item>
          {isAdmin && (
            <Form.Item name="agentMerchantNo" label="代理商号">
              <Input placeholder="请输入代理商号" allowClear style={{ width: 160 }} />
            </Form.Item>
          )}
          <Form.Item name="agentLevel" label="代理层级">
            <Select
              placeholder="全部层级"
              allowClear
              style={{ width: 120 }}
              options={[
                { label: '一级', value: 1 },
                { label: '二级', value: 2 },
                { label: '三级', value: 3 },
                { label: '四级', value: 4 },
                { label: '五级', value: 5 },
              ]}
            />
          </Form.Item>
          <Form.Item name="profitStatus" label="状态">
            <Select
              placeholder="全部状态"
              allowClear
              style={{ width: 120 }}
              options={[
                { label: '待结算', value: 0 },
                { label: '已结算', value: 1 },
                { label: '结算失败', value: 2 },
              ]}
            />
          </Form.Item>
          <Form.Item name="dateRange" label="日期范围">
            <RangePicker style={{ width: 260 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} htmlType="submit">
                查询
              </Button>
              <Button onClick={handleReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>

        <Table<AgentProfitRecord>
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.size,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchData(page, pageSize),
          }}
          scroll={{ x: 1600 }}
        />
      </Card>
    </div>
  );
};

export default AgentProfitPage;

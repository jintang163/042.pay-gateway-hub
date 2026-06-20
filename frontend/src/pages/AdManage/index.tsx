import { useState, useEffect, useMemo } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  DatePicker,
  Descriptions,
  Image,
  Switch,
  Statistic,
  Tooltip,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  LinkOutlined,
  EyeOutlined,
  RiseOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adApi } from '@/api/marketing';
import type { MerchantAd, MerchantAdSaveRequest, AdStatsOverviewVO } from '@/types/ad';
import { formatDateTime, formatAmount } from '@/utils';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;

const positionOptions = [
  { label: '支付成功页', value: 'PAY_SUCCESS' },
];

const AdManagePage = () => {
  const [form] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<MerchantAd[]>([]);
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });

  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<MerchantAd | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailAd, setDetailAd] = useState<MerchantAd | null>(null);

  const [queryTitle, setQueryTitle] = useState('');
  const [queryPosition, setQueryPosition] = useState<string | undefined>();
  const [queryStatus, setQueryStatus] = useState<number | undefined>();

  const [overview, setOverview] = useState<AdStatsOverviewVO | null>(null);
  const [overviewLoading, setOverviewLoading] = useState(false);

  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().subtract(6, 'day'),
    dayjs(),
  ]);

  const fetchOverview = async () => {
    setOverviewLoading(true);
    try {
      const resp = await adApi.statsOverview({
        startDate: dateRange[0].format('YYYY-MM-DD'),
        endDate: dateRange[1].format('YYYY-MM-DD'),
      });
      setOverview(resp);
    } catch (e: any) {
      console.error(e);
    } finally {
      setOverviewLoading(false);
    }
  };

  const fetchData = async (page = pagination.current, size = pagination.size) => {
    try {
      setLoading(true);
      const result = await adApi.list({
        current: page,
        size,
        adTitle: queryTitle || undefined,
        position: queryPosition,
        status: queryStatus,
      });
      setData(result.records);
      setPagination({ current: result.current, size: result.size, total: result.total });
    } catch (e: any) {
      message.error(e?.message || '加载广告列表失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    fetchOverview();
  }, []);

  const handleAdd = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      position: 'PAY_SUCCESS',
      cpcPrice: 0.5,
      sortOrder: 0,
      status: 1,
      dailyBudget: 0,
    });
    setModalVisible(true);
  };

  const handleEdit = (record: MerchantAd) => {
    setEditing(record);
    form.setFieldsValue({
      id: record.id,
      adTitle: record.adTitle,
      adDescription: record.adDescription,
      adImageUrl: record.adImageUrl,
      targetUrl: record.targetUrl,
      position: record.position,
      cpcPrice: record.cpcPrice,
      sortOrder: record.sortOrder,
      status: record.status,
      startTime: record.startTime ? dayjs(record.startTime) : undefined,
      endTime: record.endTime ? dayjs(record.endTime) : undefined,
      dailyBudget: record.dailyBudget,
      remark: record.remark,
    });
    setModalVisible(true);
  };

  const handleDetail = (record: MerchantAd) => {
    setDetailAd(record);
    setDetailVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload: MerchantAdSaveRequest = {
        ...values,
        startTime: values.startTime ? values.startTime.format('YYYY-MM-DD HH:mm:ss') : undefined,
        endTime: values.endTime ? values.endTime.format('YYYY-MM-DD HH:mm:ss') : undefined,
      };
      await adApi.save(payload);
      message.success(editing ? '修改成功' : '创建成功');
      setModalVisible(false);
      fetchData();
      fetchOverview();
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.message || '保存失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggle = async (record: MerchantAd) => {
    try {
      await adApi.toggle(record.id!);
      message.success(record.status === 1 ? '已下架' : '已上架');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '操作失败');
    }
  };

  const handleDelete = async (record: MerchantAd) => {
    try {
      await adApi.remove(record.id!);
      message.success('删除成功');
      fetchData();
      fetchOverview();
    } catch (e: any) {
      message.error(e?.message || '删除失败');
    }
  };

  const columns: ColumnsType<MerchantAd> = useMemo(() => [
    {
      title: '广告标题',
      dataIndex: 'adTitle',
      width: 200,
      render: (t: string, r) => (
        <Space direction="vertical" size={2}>
          <span>{t}</span>
          <span style={{ color: '#8c8c8c', fontSize: 12 }}>{r.adCode}</span>
        </Space>
      ),
    },
    {
      title: '图片',
      dataIndex: 'adImageUrl',
      width: 90,
      render: (url: string) => url
        ? <Image src={url} width={60} height={40} style={{ objectFit: 'cover', borderRadius: 4 }} />
        : <span style={{ color: '#bfbfbf' }}>无图</span>,
    },
    {
      title: '位置',
      dataIndex: 'positionDesc',
      width: 110,
      render: (d, r) => <Tag color="blue">{d || r.position}</Tag>,
    },
    {
      title: 'CPC单价',
      dataIndex: 'cpcPrice',
      width: 100,
      render: (v: number) => <strong>¥{Number(v || 0).toFixed(4)}</strong>,
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 70,
      align: 'center',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (s: number, r) => (
        <Space>
          <Tag color={s === 1 ? 'green' : 'default'}>
            {r.statusDesc || (s === 1 ? '已上架' : '已下架')}
          </Tag>
        </Space>
      ),
    },
    {
      title: '投放时间',
      dataIndex: 'startTime',
      width: 200,
      render: (_: string, r) => (
        <Space direction="vertical" size={0} style={{ fontSize: 12 }}>
          <span>开始: {r.startTime ? formatDateTime(r.startTime) : '立即'}</span>
          <span style={{ color: '#8c8c8c' }}>结束: {r.endTime ? formatDateTime(r.endTime) : '长期'}</span>
        </Space>
      ),
    },
    {
      title: '曝光/点击',
      width: 130,
      render: (_: unknown, r) => (
        <Space direction="vertical" size={0} style={{ fontSize: 12 }}>
          <span>曝光: <strong>{r.impressionCount ?? 0}</strong></span>
          <span>点击: <strong style={{ color: '#1677ff' }}>{r.clickCount ?? 0}</strong></span>
          {r.ctr != null && (
            <span style={{ color: '#8c8c8c' }}>CTR: {Number(r.ctr).toFixed(2)}%</span>
          )}
        </Space>
      ),
    },
    {
      title: '消耗',
      dataIndex: 'totalCost',
      width: 100,
      render: (v: number) => <strong style={{ color: '#cf1322' }}>¥{formatAmount(v || 0)}</strong>,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (t: string) => formatDateTime(t),
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      fixed: 'right',
      render: (_: unknown, r) => (
        <Space size={4}>
          <Button size="small" type="link" icon={<EyeOutlined />} onClick={() => handleDetail(r)}>详情</Button>
          <Button size="small" type="link" icon={<EditOutlined />} onClick={() => handleEdit(r)}>编辑</Button>
          <Button size="small" type="link" onClick={() => handleToggle(r)}>
            {r.status === 1 ? '下架' : '上架'}
          </Button>
          <Popconfirm title="确认删除此广告？" onConfirm={() => handleDelete(r)} okButtonProps={{ danger: true }}>
            <Button size="small" type="link" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ], []);

  return (
    <div>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 12]} align="middle">
          <Col flex="auto">
            <Space wrap>
              <Input
                allowClear
                placeholder="搜索广告标题"
                prefix={<SearchOutlined />}
                value={queryTitle}
                onChange={(e) => setQueryTitle(e.target.value)}
                style={{ width: 200 }}
              />
              <Select
                allowClear
                placeholder="展示位置"
                value={queryPosition}
                onChange={setQueryPosition}
                style={{ width: 140 }}
                options={positionOptions}
              />
              <Select
                allowClear
                placeholder="状态"
                value={queryStatus}
                onChange={setQueryStatus}
                style={{ width: 120 }}
                options={[
                  { label: '已上架', value: 1 },
                  { label: '已下架', value: 0 },
                ]}
              />
              <RangePicker
                value={dateRange}
                onChange={(v) => v && setDateRange(v as [dayjs.Dayjs, dayjs.Dayjs])}
                allowClear={false}
              />
              <Button type="primary" icon={<SearchOutlined />} onClick={() => { fetchData(1); fetchOverview(); }}>
                查询
              </Button>
              <Button icon={<ReloadOutlined />} onClick={() => { fetchData(); fetchOverview(); }}>
                刷新
              </Button>
            </Space>
          </Col>
          <Col>
            <Space>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
                新建广告
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="广告曝光总量"
              value={overview?.totalImpression ?? 0}
              prefix={<EyeOutlined style={{ color: '#1677ff' }} />}
              loading={overviewLoading}
            />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="有效点击数"
              value={overview?.totalValidClick ?? 0}
              prefix={<RiseOutlined style={{ color: '#52c41a' }} />}
              loading={overviewLoading}
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
              loading={overviewLoading}
              valueStyle={{ color: (overview?.overallCtr ?? 0) >= 3 ? '#52c41a' : '#cf1322' }}
            />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic
              title="累计消耗"
              value={overview?.totalCost ?? 0}
              precision={2}
              prefix="¥"
              loading={overviewLoading}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>

      <Card size="small">
        <Table<MerchantAd>
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={data}
          pagination={{
            current: pagination.current,
            pageSize: pagination.size,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p, s) => fetchData(p, s),
          }}
          scroll={{ x: 1500 }}
        />
      </Card>

      <Modal
        title={editing ? '编辑广告' : '新建广告'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={submitting}
        okText="保存"
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Row gutter={16}>
            <Col span={16}>
              <Form.Item
                label="广告标题"
                name="adTitle"
                rules={[{ required: true, message: '请输入广告标题' }, { max: 100, message: '最多100字' }]}
              >
                <Input placeholder="简短吸引人的推广标题" maxLength={100} showCount />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="展示位置"
                name="position"
                rules={[{ required: true }]}
              >
                <Select options={positionOptions} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="广告描述" name="adDescription" rules={[{ max: 500, message: '最多500字' }]}>
            <Input.TextArea placeholder="产品描述或卖点(选填)" rows={2} maxLength={500} showCount />
          </Form.Item>
          <Row gutter={16}>
            <Col span={16}>
              <Form.Item label="广告图片URL" name="adImageUrl" rules={[{ max: 500 }]}>
                <Input placeholder="https:// 图片链接 (选填)" prefix={<EyeOutlined />} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="跳转链接"
                name="targetUrl"
                rules={[{ required: true, message: '请输入跳转链接' }, { max: 500 }]}
              >
                <Input placeholder="https://..." prefix={<LinkOutlined />} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="CPC单价(元/次)"
                name="cpcPrice"
                rules={[{ required: true, message: '请输入CPC单价' }]}
              >
                <InputNumber min={0} max={1000} step={0.01} precision={4} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item label="每日预算(元)" name="dailyBudget" extra="0=不限">
                <InputNumber min={0} step={10} precision={2} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item label="排序" name="sortOrder" extra="越大越靠前">
                <InputNumber min={0} max={999} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="开始时间" name="startTime">
                <DatePicker showTime style={{ width: '100%' }} placeholder="不填=立即" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="结束时间" name="endTime">
                <DatePicker showTime style={{ width: '100%' }} placeholder="不填=长期有效" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16} align="middle">
            <Col span={12}>
              <Form.Item
                label="状态"
                name="status"
                valuePropName="checked"
                extra="上架后符合条件会在收银台展示"
              >
                <Switch
                  checkedChildren="上架"
                  unCheckedChildren="下架"
                  checked={form.getFieldValue('status') === 1}
                  onChange={(c) => form.setFieldsValue({ status: c ? 1 : 0 })}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="备注" name="remark" rules={[{ max: 500 }]}>
            <Input.TextArea placeholder="内部备注(选填)" rows={2} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="广告详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={680}
      >
        {detailAd && (
          <div>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="广告编号">{detailAd.adCode}</Descriptions.Item>
              <Descriptions.Item label="广告标题">{detailAd.adTitle}</Descriptions.Item>
              <Descriptions.Item label="展示位置" span={2}>
                <Tag color="blue">{detailAd.positionDesc || detailAd.position}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="广告描述" span={2}>
                {detailAd.adDescription || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="跳转链接" span={2}>
                <a href={detailAd.targetUrl} target="_blank" rel="noreferrer">
                  {detailAd.targetUrl}
                </a>
              </Descriptions.Item>
              <Descriptions.Item label="CPC单价">¥{Number(detailAd.cpcPrice || 0).toFixed(4)}</Descriptions.Item>
              <Descriptions.Item label="每日预算">
                {detailAd.dailyBudget && detailAd.dailyBudget > 0
                  ? `¥${formatAmount(detailAd.dailyBudget)}`
                  : '不限'}
              </Descriptions.Item>
              <Descriptions.Item label="开始时间">{detailAd.startTime ? formatDateTime(detailAd.startTime) : '立即'}</Descriptions.Item>
              <Descriptions.Item label="结束时间">{detailAd.endTime ? formatDateTime(detailAd.endTime) : '长期'}</Descriptions.Item>
              <Descriptions.Item label="排序">{detailAd.sortOrder}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={detailAd.status === 1 ? 'green' : 'default'}>
                  {detailAd.statusDesc || (detailAd.status === 1 ? '已上架' : '已下架')}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="累计曝光">{detailAd.impressionCount ?? 0}</Descriptions.Item>
              <Descriptions.Item label="累计点击">{detailAd.clickCount ?? 0}</Descriptions.Item>
              <Descriptions.Item label="CTR">
                {detailAd.ctr != null ? `${Number(detailAd.ctr).toFixed(2)}%` : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="累计消耗" span={2}>
                <strong style={{ color: '#cf1322' }}>¥{formatAmount(detailAd.totalCost || 0)}</strong>
              </Descriptions.Item>
              <Descriptions.Item label="备注" span={2}>{detailAd.remark || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{detailAd.createdAt ? formatDateTime(detailAd.createdAt) : '-'}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{detailAd.updatedAt ? formatDateTime(detailAd.updatedAt) : '-'}</Descriptions.Item>
            </Descriptions>
            {detailAd.adImageUrl && (
              <div style={{ marginTop: 16, textAlign: 'center' }}>
                <div style={{ color: '#8c8c8c', fontSize: 12, marginBottom: 8 }}>广告预览图</div>
                <Image
                  src={detailAd.adImageUrl}
                  alt="广告图"
                  style={{ maxHeight: 240, borderRadius: 8, border: '1px solid #f0f0f0' }}
                />
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default AdManagePage;

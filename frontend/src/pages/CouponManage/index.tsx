import { useState, useEffect } from 'react';
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
  Switch,
  message,
  Popconfirm,
  Card,
  Tooltip,
  Row,
  Col,
  Descriptions,
  Divider,
  Statistic,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  CalculatorOutlined,
  CopyOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { couponApi } from '@/api/marketing';
import type { Coupon, CouponSaveRequest, CouponDiscountCalcResult } from '@/types/marketing';
import { useUserStore } from '@/store';
import { formatDateTime, formatAmount } from '@/utils';

const couponTypeOptions = [
  { label: '固定金额抵扣', value: 1 },
  { label: '折扣率抵扣', value: 2 },
];

const statusColorMap: Record<number, string> = {
  0: 'default',
  1: 'green',
  2: 'orange',
  3: 'gray',
  4: 'red',
};

const CouponManagePage = () => {
  const { user } = useUserStore();
  const [form] = Form.useForm();
  const [calcForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Coupon[]>([]);
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });

  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<Coupon | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [calcVisible, setCalcVisible] = useState(false);
  const [calcResult, setCalcResult] = useState<CouponDiscountCalcResult | null>(null);
  const [calcLoading, setCalcLoading] = useState(false);

  const [queryStatus, setQueryStatus] = useState<number | undefined>();
  const [queryCouponType, setQueryCouponType] = useState<number | undefined>();

  const couponType = Form.useWatch('couponType', form);

  const fetchData = async (page = pagination.current, size = pagination.size) => {
    try {
      setLoading(true);
      const result = await couponApi.list({
        current: page,
        size,
        status: queryStatus,
        couponType: queryCouponType,
      });
      setData(result.records);
      setPagination({ current: result.current, size: result.size, total: result.total });
    } catch (e: any) {
      message.error(e?.message || '加载优惠券列表失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleAdd = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      couponType: 1,
      totalQuantity: 100,
      merchantNo: user?.username || '',
    });
    setModalVisible(true);
  };

  const handleEdit = (record: Coupon) => {
    setEditing(record);
    form.setFieldsValue({
      ...record,
      startTime: record.startTime ? record.startTime.replace('T', ' ').substring(0, 19) : undefined,
      endTime: record.endTime ? record.endTime.replace('T', ' ').substring(0, 19) : undefined,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await couponApi.remove(id);
      message.success('删除成功');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '删除失败');
    }
  };

  const handleToggleStatus = async (record: Coupon) => {
    try {
      await couponApi.toggle(record.id);
      message.success('状态更新成功');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '状态更新失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload: CouponSaveRequest = {
        ...values,
        id: editing?.id,
        minOrderAmount: values.minOrderAmount || undefined,
        maxDiscount: values.maxDiscount || undefined,
      };
      await couponApi.save(payload);
      message.success(editing ? '更新成功' : '创建成功');
      setModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error?.message || (editing ? '更新失败' : '创建失败'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleCopyCode = (code: string) => {
    navigator.clipboard.writeText(code).then(() => {
      message.success('优惠券编码已复制');
    }).catch(() => {
      message.info(`编码: ${code}`);
    });
  };

  const handleCalc = async () => {
    try {
      const values = await calcForm.validateFields();
      setCalcLoading(true);
      const result = await couponApi.calculateDiscount({
        couponCode: values.couponCode,
        orderAmount: values.orderAmount,
      });
      setCalcResult(result);
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.message || '计算失败');
    } finally {
      setCalcLoading(false);
    }
  };

  const columns: ColumnsType<Coupon> = [
    {
      title: '优惠券编码',
      dataIndex: 'couponCode',
      key: 'couponCode',
      width: 190,
      render: (code: string) => (
        <Space>
          <span style={{ fontFamily: 'monospace' }}>{code}</span>
          <Tooltip title="复制">
            <CopyOutlined style={{ color: '#1677ff', cursor: 'pointer' }} onClick={() => handleCopyCode(code)} />
          </Tooltip>
        </Space>
      ),
    },
    {
      title: '名称',
      dataIndex: 'couponName',
      key: 'couponName',
      width: 140,
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'couponType',
      key: 'couponType',
      width: 120,
      render: (type: number) => (
        <Tag color={type === 1 ? 'blue' : 'purple'}>
          {type === 1 ? '固定抵扣' : '折扣抵扣'}
        </Tag>
      ),
    },
    {
      title: '优惠值',
      key: 'discountValue',
      width: 120,
      render: (_, r) => (
        r.couponType === 1
          ? <span style={{ color: '#cf1322', fontWeight: 600 }}>¥{r.discountValue}</span>
          : <span style={{ color: '#722ed1', fontWeight: 600 }}>{r.discountValue}折</span>
      ),
    },
    {
      title: '门槛',
      dataIndex: 'minOrderAmount',
      key: 'minOrderAmount',
      width: 100,
      render: (val?: number) => val ? `满${formatAmount(val)}` : <Tag>无门槛</Tag>,
    },
    {
      title: '发放/已用',
      key: 'usage',
      width: 120,
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          <span>总量 {r.totalQuantity} / 已发 {r.issuedCount}</span>
          <span style={{ color: '#52c41a' }}>已核销 {r.usedCount}</span>
        </Space>
      ),
    },
    {
      title: '有效期',
      key: 'timeRange',
      width: 170,
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontSize: 12 }}>{r.startTime ? formatDateTime(r.startTime) : '-'}</span>
          <span style={{ fontSize: 12, color: '#999' }}>~ {r.endTime ? formatDateTime(r.endTime) : '-'}</span>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (status: number) => (
        <Tag color={statusColorMap[status] || 'default'}>
          {status === 0 ? '未开始' : status === 1 ? '发放中' : status === 2 ? '已暂停' : status === 3 ? '已发完' : status === 4 ? '已过期' : '未知'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Tooltip title="编辑">
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          </Tooltip>
          <Switch
            size="small"
            checked={record.status === 1}
            checkedChildren="开"
            unCheckedChildren="停"
            onChange={() => handleToggleStatus(record)}
          />
          <Popconfirm
            title="确定删除该优惠券？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card
        title="优惠券管理"
        extra={
          <Space>
            <Select
              placeholder="类型筛选"
              value={queryCouponType}
              onChange={(v) => { setQueryCouponType(v); fetchData(1); }}
              style={{ width: 140 }}
              allowClear
              options={couponTypeOptions}
            />
            <Select
              placeholder="状态筛选"
              value={queryStatus}
              onChange={(v) => { setQueryStatus(v); fetchData(1); }}
              style={{ width: 120 }}
              allowClear
              options={[
                { label: '未开始', value: 0 },
                { label: '发放中', value: 1 },
                { label: '已暂停', value: 2 },
                { label: '已发完', value: 3 },
                { label: '已过期', value: 4 },
              ]}
            />
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>刷新</Button>
            <Button icon={<CalculatorOutlined />} onClick={() => { setCalcVisible(true); setCalcResult(null); }}>模拟计算</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>创建优惠券</Button>
          </Space>
        }
      >
        <Table<Coupon>
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
          scroll={{ x: 1400 }}
        />
      </Card>

      <Modal
        title={editing ? '编辑优惠券' : '创建优惠券'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        width={680}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="couponName" label="优惠券名称" rules={[{ required: true, message: '请输入名称' }]}>
                <Input placeholder="如：新人立减券" maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="couponType" label="优惠类型" rules={[{ required: true }]}>
                <Select options={couponTypeOptions} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="discountValue" label={couponType === 2 ? '折扣率(如8.5)' : '抵扣金额(元)'} rules={[{ required: true }]}>
                <InputNumber
                  min={couponType === 2 ? 0.1 : 0.01}
                  max={couponType === 2 ? 9.9 : 99999}
                  step={couponType === 2 ? 0.1 : 1}
                  precision={couponType === 2 ? 1 : 2}
                  style={{ width: '100%' }}
                  placeholder={couponType === 2 ? '如8.5表示85折' : '如10'}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="minOrderAmount" label="最低消费(元)">
                <InputNumber min={0} step={1} precision={2} style={{ width: '100%' }} placeholder="无门槛留空" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="maxDiscount" label="最大优惠(元)">
                <InputNumber min={0} step={1} precision={2} style={{ width: '100%' }} placeholder="折扣券可设上限" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="totalQuantity" label="发放总量" rules={[{ required: true }]}>
                <InputNumber min={1} style={{ width: '100%' }} placeholder="如100" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="startTime" label="开始时间">
                <Input placeholder="如：2026-01-01 00:00:00" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="endTime" label="结束时间">
                <Input placeholder="如：2026-12-31 23:59:59" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="优惠券抵扣模拟计算"
        open={calcVisible}
        onCancel={() => setCalcVisible(false)}
        footer={null}
        width={700}
        destroyOnClose
      >
        <Row gutter={24}>
          <Col span={10}>
            <Form form={calcForm} layout="vertical" initialValues={{ orderAmount: 100 }}>
              <Form.Item name="couponCode" label="优惠券编码" rules={[{ required: true, message: '请输入编码' }]}>
                <Input placeholder="输入优惠券编码" />
              </Form.Item>
              <Form.Item name="orderAmount" label="订单金额(元)" rules={[{ required: true, message: '请输入金额' }]}>
                <InputNumber min={0.01} step={1} precision={2} style={{ width: '100%' }} placeholder="如100" />
              </Form.Item>
              <Form.Item>
                <Button type="primary" icon={<CalculatorOutlined />} loading={calcLoading} onClick={handleCalc} block>
                  计算优惠
                </Button>
              </Form.Item>
            </Form>
          </Col>
          <Col span={14}>
            {calcResult ? (
              <Card title="计算结果" size="small">
                <Row gutter={16}>
                  <Col span={8}>
                    <Statistic title="订单金额" value={calcResult.orderAmount} precision={2} suffix="元" formatter={(v: number) => formatAmount(v)} />
                  </Col>
                  <Col span={8}>
                    <Statistic title="优惠金额" value={calcResult.discountAmount} precision={2} suffix="元" valueStyle={{ color: '#cf1322' }} formatter={(v: number) => formatAmount(v)} />
                  </Col>
                  <Col span={8}>
                    <Statistic title="实付金额" value={calcResult.actualAmount} precision={2} suffix="元" valueStyle={{ color: '#3f8600' }} formatter={(v: number) => formatAmount(v)} />
                  </Col>
                </Row>
                <Divider />
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="优惠券">{calcResult.couponName} ({calcResult.couponCode})</Descriptions.Item>
                  <Descriptions.Item label="类型">{calcResult.couponTypeDesc}</Descriptions.Item>
                  <Descriptions.Item label="计算说明"><Tag color="blue">{calcResult.calcDetail}</Tag></Descriptions.Item>
                </Descriptions>
              </Card>
            ) : (
              <div style={{ textAlign: 'center', color: '#999', padding: '60px 0' }}>
                请在左侧输入条件后点击"计算优惠"
              </div>
            )}
          </Col>
        </Row>
      </Modal>
    </div>
  );
};

export default CouponManagePage;

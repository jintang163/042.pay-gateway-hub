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
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { activityApi } from '@/api/marketing';
import type { Activity, ActivitySaveRequest } from '@/types/marketing';
import { useUserStore } from '@/store';
import { formatDateTime, formatAmount } from '@/utils';

const activityTypeOptions = [
  { label: '满减活动', value: 1 },
  { label: '折扣活动', value: 2 },
];

const statusColorMap: Record<number, string> = {
  0: 'default',
  1: 'green',
  2: 'orange',
  3: 'gray',
};

const ActivityConfigPage = () => {
  const { user } = useUserStore();
  const [form] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Activity[]>([]);
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });

  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<Activity | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [queryStatus, setQueryStatus] = useState<number | undefined>();
  const [queryActivityType, setQueryActivityType] = useState<number | undefined>();

  const activityType = Form.useWatch('activityType', form);

  const fetchData = async (page = pagination.current, size = pagination.size) => {
    try {
      setLoading(true);
      const result = await activityApi.list({
        current: page,
        size,
        status: queryStatus,
        activityType: queryActivityType,
      });
      setData(result.records);
      setPagination({ current: result.current, size: result.size, total: result.total });
    } catch (e: any) {
      message.error(e?.message || '加载活动列表失败');
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
      activityType: 1,
      merchantNo: user?.username || '',
    });
    setModalVisible(true);
  };

  const handleEdit = (record: Activity) => {
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
      await activityApi.remove(id);
      message.success('删除成功');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '删除失败');
    }
  };

  const handleToggleStatus = async (record: Activity) => {
    try {
      await activityApi.toggle(record.id);
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
      const payload: ActivitySaveRequest = {
        ...values,
        id: editing?.id,
        thresholdAmount: values.thresholdAmount || undefined,
        discountAmount: activityType === 1 ? values.discountAmount : undefined,
        discountRate: activityType === 2 ? values.discountRate : undefined,
        maxDiscount: values.maxDiscount || undefined,
      };
      await activityApi.save(payload);
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

  const renderDiscountRule = (_: unknown, r: Activity) => {
    if (r.activityType === 1) {
      return (
        <Space direction="vertical" size={0}>
          <span>满 <b style={{ color: '#1677ff' }}>{formatAmount(r.thresholdAmount || 0)}</b> 元</span>
          <span>减 <b style={{ color: '#cf1322' }}>{formatAmount(r.discountAmount || 0)}</b> 元</span>
        </Space>
      );
    }
    return (
      <Space direction="vertical" size={0}>
        <span>满 <b style={{ color: '#1677ff' }}>{formatAmount(r.thresholdAmount || 0)}</b> 元</span>
        <span>享 <b style={{ color: '#722ed1' }}>{r.discountRate}折</b></span>
        {r.maxDiscount && <span style={{ fontSize: 12, color: '#999' }}>最多优惠 ¥{formatAmount(r.maxDiscount)}</span>}
      </Space>
    );
  };

  const columns: ColumnsType<Activity> = [
    {
      title: '活动编码',
      dataIndex: 'activityCode',
      key: 'activityCode',
      width: 190,
      render: (code: string) => (
        <span style={{ fontFamily: 'monospace' }}>{code}</span>
      ),
    },
    {
      title: '活动名称',
      dataIndex: 'activityName',
      key: 'activityName',
      width: 160,
      ellipsis: true,
    },
    {
      title: '活动类型',
      dataIndex: 'activityType',
      key: 'activityType',
      width: 100,
      render: (type: number) => (
        <Tag color={type === 1 ? 'blue' : 'purple'}>
          {type === 1 ? '满减' : '折扣'}
        </Tag>
      ),
    },
    {
      title: '优惠规则',
      key: 'rule',
      width: 160,
      render: renderDiscountRule,
    },
    {
      title: '活动时间',
      key: 'timeRange',
      width: 180,
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
          {status === 0 ? '未开始' : status === 1 ? '进行中' : status === 2 ? '已暂停' : status === 3 ? '已结束' : '未知'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (val: string) => formatDateTime(val),
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
            title="确定删除该活动？"
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
        title="活动配置"
        extra={
          <Space>
            <Select
              placeholder="类型筛选"
              value={queryActivityType}
              onChange={(v) => { setQueryActivityType(v); fetchData(1); }}
              style={{ width: 130 }}
              allowClear
              options={activityTypeOptions}
            />
            <Select
              placeholder="状态筛选"
              value={queryStatus}
              onChange={(v) => { setQueryStatus(v); fetchData(1); }}
              style={{ width: 120 }}
              allowClear
              options={[
                { label: '未开始', value: 0 },
                { label: '进行中', value: 1 },
                { label: '已暂停', value: 2 },
                { label: '已结束', value: 3 },
              ]}
            />
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>刷新</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>创建活动</Button>
          </Space>
        }
      >
        <Table<Activity>
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
          scroll={{ x: 1300 }}
        />
      </Card>

      <Modal
        title={editing ? '编辑活动' : '创建活动'}
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
              <Form.Item name="activityName" label="活动名称" rules={[{ required: true, message: '请输入活动名称' }]}>
                <Input placeholder="如：春节满减活动" maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="activityType" label="活动类型" rules={[{ required: true }]}>
                <Select options={activityTypeOptions} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="thresholdAmount" label="门槛金额(元)" rules={[{ required: true, message: '请输入门槛金额' }]}>
                <InputNumber min={0.01} step={1} precision={2} style={{ width: '100%' }} placeholder="满XX元" />
              </Form.Item>
            </Col>
            <Col span={8}>
              {activityType === 1 ? (
                <Form.Item name="discountAmount" label="减免金额(元)" rules={[{ required: true, message: '请输入减免金额' }]}>
                  <InputNumber min={0.01} step={1} precision={2} style={{ width: '100%' }} placeholder="减XX元" />
                </Form.Item>
              ) : (
                <Form.Item name="discountRate" label="折扣率(如8.5)" rules={[{ required: true, message: '请输入折扣率' }]}>
                  <InputNumber min={0.1} max={9.9} step={0.1} precision={1} style={{ width: '100%' }} placeholder="8.5=85折" />
                </Form.Item>
              )}
            </Col>
            <Col span={8}>
              <Form.Item name="maxDiscount" label="最大优惠(元)">
                <InputNumber min={0} step={1} precision={2} style={{ width: '100%' }} placeholder="折扣活动可设上限" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
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
            <Col span={8}>
              <Form.Item name="remark" label="备注">
                <Input placeholder="可选" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
};

export default ActivityConfigPage;

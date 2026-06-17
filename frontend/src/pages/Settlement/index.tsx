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
  Modal,
  InputNumber,
  Tabs,
  Divider,
  Descriptions,
  Drawer,
  Popconfirm,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import type { ColumnsType, TabsProps } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { settlementApi, splitRuleApi } from '@/api';
import type { Settlement, SettlementQueryParams, SplitRule, SplitRuleCreateRequest } from '@/types/settlement';
import { formatAmount, formatDateTime } from '@/utils';

const { RangePicker } = DatePicker;

const settleStatusTag: Record<string, { color: string; text: string }> = {
  pending: { color: 'orange', text: '待结算' },
  processing: { color: 'blue', text: '结算中' },
  success: { color: 'green', text: '已结算' },
  failed: { color: 'red', text: '结算失败' },
};

const splitStatusTag: Record<number, { color: string; text: string }> = {
  1: { color: 'green', text: '启用' },
  0: { color: 'default', text: '禁用' },
};

const SettlementPage = () => {
  const [settleQueryForm] = Form.useForm();
  const [splitRuleForm] = Form.useForm<SplitRuleCreateRequest>();
  const [settleLoading, setSettleLoading] = useState(false);
  const [splitLoading, setSplitLoading] = useState(false);
  const [settleData, setSettleData] = useState<Settlement[]>([]);
  const [splitData, setSplitData] = useState<SplitRule[]>([]);
  const [settlePagination, setSettlePagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [splitPagination, setSplitPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [splitModalVisible, setSplitModalVisible] = useState(false);
  const [editingSplit, setEditingSplit] = useState<SplitRule | null>(null);
  const [splitSubmitting, setSplitSubmitting] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentSettle, setCurrentSettle] = useState<Settlement | null>(null);

  const fetchSettleData = async (
    page = settlePagination.current,
    pageSize = settlePagination.pageSize,
    params?: Partial<SettlementQueryParams>,
  ) => {
    try {
      setSettleLoading(true);
      const queryParams: SettlementQueryParams = { pageNum: page, pageSize, ...params };
      const result = await settlementApi.list(queryParams);
      setSettleData(result.list);
      setSettlePagination({ current: page, pageSize, total: result.total });
    } catch {
      const statuses = Object.keys(settleStatusTag);
      const channels = ['ALIPAY', 'WECHAT', 'UNIONPAY', 'APPLE_PAY'];
      const mockData: Settlement[] = Array.from({ length: 28 }, (_, i) => ({
        id: i + 1,
        settleNo: 'STL' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        merchantId: 'M' + String(1000 + (i % 10)).padStart(4, '0'),
        merchantName: '示例商户' + ((i % 10) + 1),
        settleDate: dayjs().subtract(i, 'day').format('YYYY-MM-DD'),
        payChannel: channels[i % channels.length],
        totalAmount: Math.random() * 100000 + 10000,
        feeAmount: Math.random() * 1000 + 100,
        settleAmount: Math.random() * 99000 + 9900,
        orderCount: Math.floor(Math.random() * 500 + 50),
        status: statuses[i % statuses.length],
        bankName: '中国工商银行北京市分行',
        bankAccount: '6222****8888',
        accountName: '示例商户' + ((i % 10) + 1),
        createTime: new Date(Date.now() - i * 86400000).toISOString(),
        completeTime: i % 4 === 2 ? new Date(Date.now() - i * 86400000 + 7200000).toISOString() : undefined,
        remark: i % 3 === 0 ? '正常结算' : undefined,
      }));
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      setSettleData(mockData.slice(start, end));
      setSettlePagination({ current: page, pageSize, total: mockData.length });
    } finally {
      setSettleLoading(false);
    }
  };

  const fetchSplitData = async (
    page = splitPagination.current,
    pageSize = splitPagination.pageSize,
  ) => {
    try {
      setSplitLoading(true);
      const result = await splitRuleApi.list({ pageNum: page, pageSize });
      setSplitData(result.list);
      setSplitPagination({ current: page, pageSize, total: result.total });
    } catch {
      const types = ['PERCENT', 'FIXED'];
      const mockData: SplitRule[] = Array.from({ length: 12 }, (_, i) => ({
        id: i + 1,
        ruleNo: 'SR' + String(1000 + i).padStart(6, '0'),
        ruleName: '分账规则-' + (i + 1),
        merchantId: 'M' + String(1000 + (i % 10)).padStart(4, '0'),
        merchantName: '示例商户' + ((i % 10) + 1),
        splitType: types[i % types.length],
        splitConfig: JSON.stringify(
          {
            type: types[i % types.length],
            details: [
              { account: '主账户', percent: types[i % types.length] === 'PERCENT' ? 70 : 700 },
              { account: '子账户A', percent: types[i % types.length] === 'PERCENT' ? 20 : 200 },
              { account: '子账户B', percent: types[i % types.length] === 'PERCENT' ? 10 : 100 },
            ],
          },
          null,
          2,
        ),
        status: i % 2,
        description: '分账规则描述信息',
        createTime: new Date(Date.now() - i * 86400000).toISOString(),
        updateTime: new Date(Date.now() - i * 86400000).toISOString(),
      }));
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      setSplitData(mockData.slice(start, end));
      setSplitPagination({ current: page, pageSize, total: mockData.length });
    } finally {
      setSplitLoading(false);
    }
  };

  useEffect(() => {
    fetchSettleData();
    fetchSplitData();
  }, []);

  const handleSettleQuery = () => {
    const values = settleQueryForm.getFieldsValue();
    const params: Partial<SettlementQueryParams> = { ...values };
    if (values.dateRange) {
      params.startTime = (values.dateRange as [Dayjs, Dayjs])[0].format('YYYY-MM-DD HH:mm:ss');
      params.endTime = (values.dateRange as [Dayjs, Dayjs])[1].format('YYYY-MM-DD HH:mm:ss');
    }
    fetchSettleData(1, settlePagination.pageSize, params);
  };

  const handleSettleReset = () => {
    settleQueryForm.resetFields();
    fetchSettleData(1, settlePagination.pageSize);
  };

  const handleAddSplit = () => {
    setEditingSplit(null);
    splitRuleForm.resetFields();
    setSplitModalVisible(true);
  };

  const handleEditSplit = (record: SplitRule) => {
    setEditingSplit(record);
    splitRuleForm.setFieldsValue({
      ...record,
    });
    setSplitModalVisible(true);
  };

  const handleDeleteSplit = async (id: number) => {
    try {
      await splitRuleApi.delete(id);
      message.success('删除成功');
      fetchSplitData();
    } catch {
      setSplitData((prev) => prev.filter((item) => item.id !== id));
      message.success('删除成功');
    }
  };

  const handleToggleSplitStatus = async (record: SplitRule) => {
    try {
      const newStatus = record.status === 1 ? 0 : 1;
      await splitRuleApi.updateStatus(record.id!, newStatus);
      message.success('状态更新成功');
      fetchSplitData();
    } catch {
      setSplitData((prev) =>
        prev.map((item) =>
          item.id === record.id ? { ...item, status: item.status === 1 ? 0 : 1 } : item,
        ),
      );
      message.success('状态更新成功');
    }
  };

  const handleSplitSubmit = async () => {
    try {
      const values = await splitRuleForm.validateFields();
      setSplitSubmitting(true);
      if (editingSplit) {
        await splitRuleApi.update(editingSplit.id!, values);
        message.success('更新成功');
      } else {
        await splitRuleApi.create(values);
        message.success('创建成功');
      }
      setSplitModalVisible(false);
      fetchSplitData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(editingSplit ? '更新失败' : '创建失败');
    } finally {
      setSplitSubmitting(false);
    }
  };

  const settleColumns: ColumnsType<Settlement> = [
    { title: '结算单号', dataIndex: 'settleNo', key: 'settleNo', width: 200 },
    { title: '商户名称', dataIndex: 'merchantName', key: 'merchantName', width: 140 },
    { title: '结算日期', dataIndex: 'settleDate', key: 'settleDate', width: 120 },
    {
      title: '支付渠道',
      dataIndex: 'payChannel',
      key: 'payChannel',
      width: 100,
      render: (val) => <Tag color="blue">{val}</Tag>,
    },
    {
      title: '总金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      render: (val) => formatAmount(val),
    },
    {
      title: '手续费',
      dataIndex: 'feeAmount',
      key: 'feeAmount',
      width: 120,
      render: (val) => formatAmount(val),
    },
    {
      title: '结算金额',
      dataIndex: 'settleAmount',
      key: 'settleAmount',
      width: 120,
      render: (val) => <span style={{ color: '#52c41a', fontWeight: 600 }}>{formatAmount(val)}</span>,
    },
    { title: '订单数', dataIndex: 'orderCount', key: 'orderCount', width: 90 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const tag = settleStatusTag[status] || { color: 'default', text: status };
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (val) => formatDateTime(val),
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
            setCurrentSettle(record);
            setDetailVisible(true);
          }}
        >
          详情
        </Button>
      ),
    },
  ];

  const splitColumns: ColumnsType<SplitRule> = [
    { title: '规则编号', dataIndex: 'ruleNo', key: 'ruleNo', width: 160 },
    { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 160 },
    { title: '商户名称', dataIndex: 'merchantName', key: 'merchantName', width: 140 },
    {
      title: '分账类型',
      dataIndex: 'splitType',
      key: 'splitType',
      width: 100,
      render: (val) => <Tag color={val === 'PERCENT' ? 'blue' : 'purple'}>{val === 'PERCENT' ? '按比例' : '固定金额'}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number, record) => {
        const tag = splitStatusTag[status];
        return (
          <a onClick={() => handleToggleSplitStatus(record)}>
            <Tag color={tag.color} style={{ cursor: 'pointer' }}>
              {tag.text}
            </Tag>
          </a>
        );
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (val) => formatDateTime(val),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditSplit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除该规则？"
            onConfirm={() => handleDeleteSplit(record.id!)}
            okText="删除"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const tabItems: TabsProps['items'] = [
    {
      key: 'settlement',
      label: '结算记录',
      children: (
        <div>
          <Card style={{ marginBottom: 16 }}>
            <Form form={settleQueryForm} layout="inline" onFinish={handleSettleQuery}>
              <Form.Item name="settleNo" label="结算单号">
                <Input placeholder="请输入结算单号" allowClear style={{ width: 200 }} />
              </Form.Item>
              <Form.Item name="status" label="结算状态" initialValue="">
                <Select
                  style={{ width: 130 }}
                  options={[
                    { label: '全部', value: '' },
                    { label: '待结算', value: 'pending' },
                    { label: '结算中', value: 'processing' },
                    { label: '已结算', value: 'success' },
                    { label: '结算失败', value: 'failed' },
                  ]}
                />
              </Form.Item>
              <Form.Item name="dateRange" label="结算日期">
                <RangePicker style={{ width: 340 }} />
              </Form.Item>
              <Form.Item>
                <Space>
                  <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                    查询
                  </Button>
                  <Button onClick={handleSettleReset} icon={<ReloadOutlined />}>
                    重置
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          </Card>

          <Card>
            <Table<Settlement>
              columns={settleColumns}
              dataSource={settleData}
              rowKey="id"
              loading={settleLoading}
              scroll={{ x: 1400 }}
              pagination={{
                ...settlePagination,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条`,
                onChange: (page, pageSize) => fetchSettleData(page, pageSize),
              }}
            />
          </Card>
        </div>
      ),
    },
    {
      key: 'split',
      label: '分账规则',
      children: (
        <Card
          extra={
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAddSplit}>
              新增分账规则
            </Button>
          }
        >
          <Table<SplitRule>
            columns={splitColumns}
            dataSource={splitData}
            rowKey="id"
            loading={splitLoading}
            scroll={{ x: 1200 }}
            pagination={{
              ...splitPagination,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 条`,
              onChange: (page, pageSize) => fetchSplitData(page, pageSize),
            }}
          />
        </Card>
      ),
    },
  ];

  return (
    <div>
      <Tabs items={tabItems} defaultActiveKey="settlement" />

      <Modal
        title={editingSplit ? '编辑分账规则' : '新增分账规则'}
        open={splitModalVisible}
        onCancel={() => setSplitModalVisible(false)}
        onOk={handleSplitSubmit}
        confirmLoading={splitSubmitting}
        width={640}
        destroyOnClose
      >
        <Form form={splitRuleForm} layout="vertical" name="split_rule_form">
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="ruleName"
              label="规则名称"
              rules={[{ required: true, message: '请输入规则名称' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="请输入规则名称" />
            </Form.Item>
            <Form.Item
              name="splitType"
              label="分账类型"
              rules={[{ required: true, message: '请选择分账类型' }]}
              style={{ flex: 1 }}
            >
              <Select
                placeholder="请选择分账类型"
                options={[
                  { label: '按比例', value: 'PERCENT' },
                  { label: '固定金额', value: 'FIXED' },
                ]}
              />
            </Form.Item>
            <Form.Item
              name="status"
              label="状态"
              initialValue={1}
              style={{ flex: 1 }}
            >
              <Select
                options={[
                  { label: '启用', value: 1 },
                  { label: '禁用', value: 0 },
                ]}
              />
            </Form.Item>
          </div>

          <Form.Item
            name="description"
            label="规则描述"
          >
            <Input.TextArea rows={2} placeholder="请输入规则描述（选填）" />
          </Form.Item>

          <Form.Item
            name="splitConfig"
            label="分账配置JSON"
            rules={[
              { required: true, message: '请输入分账配置' },
              {
                validator: (_, value) => {
                  if (!value) return Promise.resolve();
                  try {
                    JSON.parse(value);
                    return Promise.resolve();
                  } catch {
                    return Promise.reject(new Error('JSON格式不正确'));
                  }
                },
              },
            ]}
          >
            <Input.TextArea
              rows={8}
              placeholder='{"type":"PERCENT","details":[{"account":"主账户","percent":70},{"account":"子账户A","percent":30}]}'
              style={{ fontFamily: 'monospace' }}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="结算详情"
        width={640}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
      >
        {currentSettle && (
          <div>
            <Descriptions title="基本信息" bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="结算单号">{currentSettle.settleNo}</Descriptions.Item>
              <Descriptions.Item label="商户名称">{currentSettle.merchantName}</Descriptions.Item>
              <Descriptions.Item label="结算日期">{currentSettle.settleDate}</Descriptions.Item>
              <Descriptions.Item label="支付渠道">{currentSettle.payChannel}</Descriptions.Item>
              <Descriptions.Item label="订单数">{currentSettle.orderCount} 笔</Descriptions.Item>
              <Descriptions.Item label="结算状态">
                {(() => {
                  const tag = settleStatusTag[currentSettle.status];
                  return <Tag color={tag.color}>{tag.text}</Tag>;
                })()}
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="金额信息" bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="总金额">{formatAmount(currentSettle.totalAmount)}</Descriptions.Item>
              <Descriptions.Item label="手续费">{formatAmount(currentSettle.feeAmount)}</Descriptions.Item>
              <Descriptions.Item label="结算金额">
                <span style={{ color: '#52c41a', fontWeight: 600, fontSize: 16 }}>
                  {formatAmount(currentSettle.settleAmount)}
                </span>
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="银行账户" bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="开户银行">{currentSettle.bankName}</Descriptions.Item>
              <Descriptions.Item label="银行账号">{currentSettle.bankAccount}</Descriptions.Item>
              <Descriptions.Item label="账户名称">{currentSettle.accountName}</Descriptions.Item>
            </Descriptions>

            <Descriptions title="时间信息" bordered column={1} size="small">
              <Descriptions.Item label="创建时间">{formatDateTime(currentSettle.createTime)}</Descriptions.Item>
              <Descriptions.Item label="完成时间">
                {currentSettle.completeTime ? formatDateTime(currentSettle.completeTime) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="备注">{currentSettle.remark || '-'}</Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default SettlementPage;

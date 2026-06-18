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
  Tabs,
  Tooltip,
  Descriptions,
  Row,
  Col,
  Statistic,
  Divider,
  Alert,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  CalculatorOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { feeRuleApi, merchantApi } from '@/api';
import type {
  FeeRule,
  FeeRuleSaveRequest,
  IndustryOption,
} from '@/types/feeRule';
import type { Merchant } from '@/types/merchant';
import { useUserStore } from '@/store';
import { formatDateTime, formatPercent, formatAmount } from '@/utils';

const channelOptions = [
  { label: '全部渠道', value: '' },
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信支付', value: 'WECHAT' },
  { label: '银联', value: 'UNIONPAY' },
  { label: 'Apple Pay', value: 'APPLE_PAY' },
  { label: 'Google Pay', value: 'GOOGLE_PAY' },
  { label: 'PayPal', value: 'PAYPAL' },
];

const commonIndustryOptions: IndustryOption[] = [
  { code: 'RETAIL', name: '零售百货' },
  { code: 'FOOD', name: '餐饮美食' },
  { code: 'HOTEL', name: '酒店住宿' },
  { code: 'TRAVEL', name: '旅游出行' },
  { code: 'EDUCATION', name: '教育培训' },
  { code: 'HEALTHCARE', name: '医疗健康' },
  { code: 'ONLINE_SHOP', name: '电子商务' },
  { code: 'SERVICE', name: '生活服务' },
  { code: 'ENTERTAINMENT', name: '娱乐休闲' },
  { code: 'OTHER', name: '其他行业' },
];

const renderAmountRange = (min: number, max: number): JSX.Element => {
  const minYuan = min;
  const maxYuan = max >= 99999999 ? Infinity : max;
  const minText = minYuan === 0 ? '0' : formatAmount(minYuan);
  const maxText = !isFinite(maxYuan) ? '∞' : formatAmount(maxYuan);
  return (
    <span>
      {minText} ~ {maxText}
    </span>
  );
};

const FeeConfigPage = () => {
  const { user } = useUserStore();
  const isAdmin = user?.role === 'admin' || user?.role === 'operator';

  const [activeTab, setActiveTab] = useState<string>('ladder');
  const [form] = Form.useForm();
  const [calcForm] = Form.useForm();
  const [ladderForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<FeeRule[]>([]);
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });
  const [industries, setIndustries] = useState<IndustryOption[]>(commonIndustryOptions);
  const [currentMerchant, setCurrentMerchant] = useState<Merchant | null>(null);

  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<FeeRule | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [ladderLoading, setLadderLoading] = useState(false);
  const [ladderData, setLadderData] = useState<FeeRule[]>([]);

  const [calcResult, setCalcResult] = useState<any>(null);
  const [calcLoading, setCalcLoading] = useState(false);

  const fetchData = async (page = pagination.current, size = pagination.size) => {
    try {
      setLoading(true);
      const result = await feeRuleApi.list({ current: page, size });
      setData(result.records);
      setPagination({ current: result.current, size: result.size, total: result.total });
    } catch (e: any) {
      message.error(e?.message || '加载费率规则列表失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchIndustries = async () => {
    try {
      const list = await feeRuleApi.industries();
      if (list && list.length > 0) {
        setIndustries(list);
      }
    } catch {
    }
  };

  const fetchLadder = async (industryCode: string, payChannel?: string) => {
    try {
      setLadderLoading(true);
      const list = await feeRuleApi.listByIndustry(industryCode, payChannel || undefined);
      setLadderData(list);
    } catch (e: any) {
      message.error(e?.message || '加载阶梯费率失败');
      setLadderData([]);
    } finally {
      setLadderLoading(false);
    }
  };

  const fetchCurrentMerchant = async () => {
    if (!isAdmin) {
      try {
        const merchantNo = user?.username || '';
        const m = await merchantApi.detail(merchantNo);
        setCurrentMerchant(m as any);
      } catch {
      }
    }
  };

  useEffect(() => {
    fetchIndustries();
    if (isAdmin) {
      fetchData();
    } else {
      fetchCurrentMerchant();
    }
  }, [isAdmin]);

  useEffect(() => {
    if (activeTab === 'ladder') {
      const vals = ladderForm.getFieldsValue();
      let defaultIndustry = vals.industryCode;
      if (!defaultIndustry && !isAdmin && currentMerchant?.industryCode) {
        defaultIndustry = currentMerchant.industryCode;
        ladderForm.setFieldsValue({ industryCode: defaultIndustry });
      }
      if (defaultIndustry) {
        fetchLadder(defaultIndustry, vals.payChannel || undefined);
      }
    }
  }, [activeTab, currentMerchant]);

  const handleAdd = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      status: 1,
      priority: 0,
      minFee: 0,
      minAmount: 0,
      maxAmount: 99999999.99,
    });
    setModalVisible(true);
  };

  const handleEdit = (record: FeeRule) => {
    setEditing(record);
    form.setFieldsValue({
      ...record,
      feeRate: record.feeRate,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await feeRuleApi.remove(id);
      message.success('删除成功');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '删除失败');
    }
  };

  const handleToggleStatus = async (record: FeeRule) => {
    try {
      await feeRuleApi.toggle(record.id);
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
      const payload: FeeRuleSaveRequest = {
        ...values,
        payChannel: values.payChannel || undefined,
      };
      await feeRuleApi.save(payload);
      message.success(editing ? '更新成功' : '创建成功');
      setModalVisible(false);
      fetchData();
      fetchIndustries();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error?.message || (editing ? '更新失败' : '创建失败'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleCalc = async () => {
    try {
      const values = await calcForm.validateFields();
      setCalcLoading(true);
      const result = await feeRuleApi.calculate({
        industryCode: values.industryCode,
        payChannel: values.payChannel || undefined,
        amount: values.amountYuan,
      });
      setCalcResult(result);
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.message || '计算失败');
    } finally {
      setCalcLoading(false);
    }
  };

  const manageColumns: ColumnsType<FeeRule> = [
    {
      title: '规则编号',
      dataIndex: 'ruleNo',
      key: 'ruleNo',
      width: 160,
    },
    {
      title: '行业',
      key: 'industry',
      width: 140,
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 500 }}>{r.industryName}</span>
          <span style={{ color: '#999', fontSize: 12 }}>{r.industryCode}</span>
        </Space>
      ),
    },
    {
      title: '支付渠道',
      dataIndex: 'payChannelDesc',
      key: 'payChannelDesc',
      width: 100,
      render: (desc, r) => {
        const color = r.payChannel ? 'blue' : 'default';
        return <Tag color={color}>{desc}</Tag>;
      },
    },
    {
      title: '金额区间（元）',
      key: 'amountRange',
      width: 180,
      render: (_, r) => renderAmountRange(r.minAmount, r.maxAmount),
    },
    {
      title: '费率',
      dataIndex: 'feeRate',
      key: 'feeRate',
      width: 100,
      render: (rate: number) => (
        <Tag color="geekblue" style={{ fontSize: 14, padding: '2px 10px' }}>
          {formatPercent(rate / 100)}
        </Tag>
      ),
    },
    {
      title: '单笔最低/最高（元）',
      key: 'feeRange',
      width: 160,
      render: (_, r) => (
        <span>
          {r.minFee ? formatAmount(r.minFee) : '0'}
          {r.maxFee ? ` / ${formatAmount(r.maxFee)}` : ' / ∞'}
        </span>
      ),
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
      sorter: (a, b) => a.priority - b.priority,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (status: number, record) => (
        <Switch
          checked={status === 1}
          checkedChildren="启用"
          unCheckedChildren="禁用"
          onClick={() => handleToggleStatus(record)}
        />
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
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            >
              编辑
            </Button>
          </Tooltip>
          <Popconfirm
            title="确定删除该规则？"
            description="删除后不可恢复"
            onConfirm={() => handleDelete(record.id)}
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

  const ladderColumns: ColumnsType<FeeRule> = [
    {
      title: '金额区间（元）',
      key: 'amountRange',
      width: 200,
      render: (_, r) => renderAmountRange(r.minAmount, r.maxAmount),
    },
    {
      title: '费率',
      dataIndex: 'feeRate',
      key: 'feeRate',
      width: 120,
      render: (rate: number) => (
        <span style={{ fontWeight: 600, fontSize: 16, color: '#1677ff' }}>
          {formatPercent(rate / 100)}
        </span>
      ),
    },
    {
      title: '单笔最低手续费',
      dataIndex: 'minFee',
      key: 'minFee',
      width: 140,
      render: (fee: number) => (fee ? formatAmount(fee) : <Tag color="default">无限制</Tag>),
    },
    {
      title: '单笔最高手续费',
      dataIndex: 'maxFee',
      key: 'maxFee',
      width: 140,
      render: (fee?: number) => (fee ? formatAmount(fee) : <Tag color="default">无限制</Tag>),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      render: (val?: string) => val || '-',
    },
  ];

  const defaultCalcIndustry = () => {
    if (!isAdmin && currentMerchant?.industryCode) return currentMerchant.industryCode;
    return 'RETAIL';
  };

  const ladderTab = (
    <div>
      <Card title="阶梯费率查询" style={{ marginBottom: 16 }}>
        <Form
          form={ladderForm}
          layout="inline"
          initialValues={{ industryCode: defaultCalcIndustry() }}
          onValuesChange={(changed) => {
            if ('industryCode' in changed || 'payChannel' in changed) {
              const vals = ladderForm.getFieldsValue();
              if (vals.industryCode) {
                fetchLadder(vals.industryCode, vals.payChannel || undefined);
              }
            }
          }}
        >
          <Form.Item label="行业类别" name="industryCode" rules={[{ required: true }]}>
            <Select
              placeholder="选择行业"
              style={{ width: 200 }}
              disabled={!isAdmin && !!currentMerchant?.industryCode}
              options={industries.map((i) => ({ label: i.name, value: i.code }))}
            />
          </Form.Item>
          <Form.Item label="支付渠道" name="payChannel">
            <Select
              placeholder="全部渠道"
              style={{ width: 180 }}
              allowClear
              options={channelOptions.filter((c) => c.value !== '')}
            />
          </Form.Item>
          <Form.Item>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                const vals = ladderForm.getFieldsValue();
                if (vals.industryCode) fetchLadder(vals.industryCode, vals.payChannel || undefined);
              }}
            >
              刷新
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {!isAdmin && currentMerchant && (
        <Alert
          message={`您当前所属行业：${currentMerchant.industryName || currentMerchant.industryCode || '未设置'}`}
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Card title="阶梯费率表">
        <Table<FeeRule>
          columns={ladderColumns}
          dataSource={ladderData}
          rowKey="ruleNo"
          loading={ladderLoading}
          pagination={false}
          locale={{ emptyText: '暂无匹配的费率规则' }}
        />
      </Card>
    </div>
  );

  const calcTab = (
    <div>
      <Row gutter={24}>
        <Col span={12}>
          <Card title="手续费模拟计算" extra={<CalculatorOutlined style={{ color: '#1677ff', fontSize: 18 }} />}>
            <Form
              form={calcForm}
              layout="vertical"
              initialValues={{ industryCode: defaultCalcIndustry(), amountYuan: 100 }}
            >
              <Form.Item
                label="行业类别"
                name="industryCode"
                rules={[{ required: true, message: '请选择行业' }]}
              >
                <Select
                  placeholder="选择行业"
                  disabled={!isAdmin && !!currentMerchant?.industryCode}
                  options={industries.map((i) => ({ label: i.name, value: i.code }))}
                />
              </Form.Item>
              <Form.Item label="支付渠道" name="payChannel">
                <Select
                  placeholder="全部渠道"
                  allowClear
                  options={channelOptions.filter((c) => c.value !== '')}
                />
              </Form.Item>
              <Form.Item
                label="交易金额（元）"
                name="amountYuan"
                rules={[{ required: true, message: '请输入交易金额' }]}
              >
                <InputNumber min={0.01} step={1} precision={2} style={{ width: '100%' }} placeholder="请输入金额，单位：元" />
              </Form.Item>
              <Form.Item>
                <Button
                  type="primary"
                  icon={<CalculatorOutlined />}
                  loading={calcLoading}
                  onClick={handleCalc}
                  block
                  size="large"
                >
                  计算手续费
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>
        <Col span={12}>
          <Card title="计算结果">
            {calcResult ? (
              <div>
                <Row gutter={[0, 16]}>
                  <Col span={12}>
                    <Statistic title="交易金额" value={calcResult.amount} precision={2} suffix="元" formatter={(v: number) => formatAmount(v)} />
                  </Col>
                  <Col span={12}>
                    <Statistic
                      title="手续费"
                      value={calcResult.feeAmount}
                      precision={2}
                      suffix="元"
                      valueStyle={{ color: '#cf1322' }}
                      formatter={(v: number) => formatAmount(v)}
                    />
                  </Col>
                </Row>
                <Row gutter={[0, 16]} style={{ marginTop: 8 }}>
                  <Col span={12}>
                    <Statistic title="适用费率" value={calcResult.feeRate} formatter={(v: number) => formatPercent(v / 100)} />
                  </Col>
                  <Col span={12}>
                    <Statistic
                      title="实际到账"
                      value={calcResult.amount - calcResult.feeAmount}
                      precision={2}
                      suffix="元"
                      valueStyle={{ color: '#3f8600' }}
                      formatter={(v: number) => formatAmount(v)}
                    />
                  </Col>
                </Row>
                <Divider />
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="规则编号">{calcResult.ruleNo || '-'}</Descriptions.Item>
                  <Descriptions.Item label="行业">
                    {calcResult.industryName || calcResult.industryCode}
                  </Descriptions.Item>
                  <Descriptions.Item label="支付渠道">
                    {calcResult.payChannel || '全部渠道'}
                  </Descriptions.Item>
                  <Descriptions.Item label="计算说明">
                    <Tag color="blue">{calcResult.calcDetail}</Tag>
                  </Descriptions.Item>
                </Descriptions>
              </div>
            ) : (
              <div style={{ textAlign: 'center', color: '#999', padding: '60px 0' }}>
                请在左侧输入条件后点击"计算手续费"
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );

  const manageTab = (
    <Card
      title="费率规则管理"
      extra={
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增规则
          </Button>
        </Space>
      }
    >
      <Table<FeeRule>
        columns={manageColumns}
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
        scroll={{ x: 1500 }}
      />
    </Card>
  );

  const tabItems = [
    { key: 'ladder', label: '阶梯费率表', children: ladderTab },
    { key: 'calc', label: '手续费计算', children: calcTab },
  ];
  if (isAdmin) {
    tabItems.push({ key: 'manage', label: '规则管理', children: manageTab });
  }

  return (
    <div>
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={tabItems}
      />

      <Modal
        title={editing ? '编辑费率规则' : '新增费率规则'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        width={720}
        destroyOnClose
      >
        <Form form={form} layout="vertical" name="fee_rule_form">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="industryCode"
                label="行业编码"
                rules={[{ required: true, message: '请选择行业编码' }]}
              >
                <Select
                  placeholder="选择行业"
                  options={industries.map((i) => ({ label: `${i.name} (${i.code})`, value: i.code }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="industryName"
                label="行业名称"
                rules={[{ required: true, message: '请输入行业名称' }]}
              >
                <Input placeholder="如：零售百货" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="payChannel" label="支付渠道">
                <Select
                  placeholder="留空=全部渠道"
                  allowClear
                  options={channelOptions}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="priority"
                label="优先级"
                tooltip="数字越大优先级越高，匹配相同区间时取高优先级规则"
              >
                <InputNumber min={0} max={999} style={{ width: '100%' }} placeholder="默认0" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                name="minAmount"
                label="金额区间最小值（元）"
                rules={[{ required: true }]}
              >
                <InputNumber min={0} step={0.01} precision={2} style={{ width: '100%' }} placeholder="如：0" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="maxAmount"
                label="金额区间最大值（元）"
                rules={[{ required: true }]}
              >
                <InputNumber min={0.01} step={0.01} precision={2} style={{ width: '100%' }} placeholder="如：99999999.99" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="feeRate"
                label="费率(%)"
                rules={[{ required: true, message: '请输入费率' }]}
              >
                <InputNumber min={0} max={100} step={0.01} precision={4} style={{ width: '100%' }} placeholder="如：0.6" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="minFee" label="单笔最低手续费（元）">
                <InputNumber min={0} step={0.01} precision={2} style={{ width: '100%' }} placeholder="默认0" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="maxFee" label="单笔最高手续费（元）">
                <InputNumber min={0} step={0.01} precision={2} style={{ width: '100%' }} placeholder="留空=不限制" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="status" label="状态" initialValue={1}>
                <Select
                  options={[
                    { label: '启用', value: 1 },
                    { label: '禁用', value: 0 },
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default FeeConfigPage;

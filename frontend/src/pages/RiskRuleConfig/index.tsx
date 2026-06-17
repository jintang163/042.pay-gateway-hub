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
  Switch,
  Popconfirm,
  Row,
  Col,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  SwitcherOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { formatDateTime } from '@/utils';

const { RangePicker } = DatePicker;
const { TextArea } = Input;

type RuleType = 'AMOUNT' | 'FREQUENCY' | 'IP_BLACKLIST' | 'DEVICE' | 'BEHAVIOR';
type ActionType = 'PASS' | 'BLOCK' | 'SMS' | 'MANUAL';

interface RiskRule {
  id: number;
  ruleCode: string;
  ruleName: string;
  ruleType: RuleType;
  riskLevel: 1 | 2 | 3;
  actionType: ActionType;
  smsTemplateId?: string;
  priority: number;
  status: number;
  ruleCondition: string;
  effectStartTime: string;
  effectEndTime: string;
  remark?: string;
  createdAt: string;
}

const ruleTypeMap: Record<RuleType, { label: string; color: string }> = {
  AMOUNT: { label: '金额', color: 'blue' },
  FREQUENCY: { label: '频率', color: 'cyan' },
  IP_BLACKLIST: { label: 'IP黑名单', color: 'magenta' },
  DEVICE: { label: '设备', color: 'purple' },
  BEHAVIOR: { label: '行为', color: 'geekblue' },
};

const riskLevelMap: Record<number, { color: string; text: string }> = {
  1: { color: 'blue', text: '低' },
  2: { color: 'orange', text: '中' },
  3: { color: 'red', text: '高' },
};

const actionTypeMap: Record<ActionType, { label: string; color: string }> = {
  PASS: { label: '放行', color: 'green' },
  BLOCK: { label: '拦截', color: 'red' },
  SMS: { label: '短信验证', color: 'orange' },
  MANUAL: { label: '人工审核', color: 'purple' },
};

const ruleTypeOptions = [
  { label: '金额', value: 'AMOUNT' },
  { label: '频率', value: 'FREQUENCY' },
  { label: 'IP黑名单', value: 'IP_BLACKLIST' },
  { label: '设备', value: 'DEVICE' },
  { label: '行为', value: 'BEHAVIOR' },
];

const riskLevelOptions = [
  { label: '1 - 低', value: 1 },
  { label: '2 - 中', value: 2 },
  { label: '3 - 高', value: 3 },
];

const actionTypeOptions = [
  { label: '放行', value: 'PASS' },
  { label: '拦截', value: 'BLOCK' },
  { label: '短信验证', value: 'SMS' },
  { label: '人工审核', value: 'MANUAL' },
];

const conditionTemplateMap: Record<RuleType, string> = {
  AMOUNT: `{\n  "minAmount": 1000,\n  "maxAmount": 5000000\n}`,
  FREQUENCY: `{\n  "windowSeconds": 60,\n  "maxCount": 100\n}`,
  IP_BLACKLIST: `{}`,
  DEVICE: `{\n  "minRiskScore": 60\n}`,
  BEHAVIOR: `{\n  "customCondition": "...自定义DRL条件..."\n}`,
};

const generateDRL = (rule: Partial<RiskRule>): string => {
  const ruleName = rule.ruleName || 'UnknownRule';
  const ruleCode = rule.ruleCode || 'UNKNOWN';
  const ruleType = rule.ruleType || 'AMOUNT';
  const riskLevel = rule.riskLevel || 1;
  const actionType = rule.actionType || 'PASS';
  const priority = rule.priority || 100;
  let conditionStr = '';
  try {
    const cond = JSON.parse(rule.ruleCondition || '{}');
    conditionStr = JSON.stringify(cond, null, 8).replace(/\n/g, '\n        ');
  } catch {
    conditionStr = rule.ruleCondition || '{}';
  }

  return `package com.payment.risk.rules;

import com.payment.risk.model.RiskContext;
import com.payment.risk.model.RiskResult;
import com.payment.risk.model.RiskLevel;
import com.payment.risk.model.ActionType;

dialect "mvel"

/**
 * 规则编码: ${ruleCode}
 * 规则类型: ${ruleType}
 * 风险等级: ${riskLevel}
 * 优先级: ${priority}
 */
rule "${ruleName}"
    salience ${priority}
    no-loop true
    when
        \$ctx: RiskContext()
        // 规则条件
        eval(${ruleType}ConditionCheck(\$ctx, ${conditionStr}))
    then
        RiskResult result = new RiskResult();
        result.setRuleCode("${ruleCode}");
        result.setRuleName("${ruleName}");
        result.setRiskLevel(RiskLevel.LEVEL_${riskLevel});
        result.setActionType(ActionType.${actionType});
        result.setHit(true);
${actionType === 'SMS' && rule.smsTemplateId ? `        result.setSmsTemplateId("${rule.smsTemplateId}");` : ''}
        \$ctx.addRiskResult(result);
end`;
};

const RiskRuleConfig = () => {
  const [queryForm] = Form.useForm();
  const [ruleForm] = Form.useForm();

  const selectedActionType = Form.useWatch('actionType', ruleForm);

  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [data, setData] = useState<RiskRule[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const [modalVisible, setModalVisible] = useState(false);
  const [editingRule, setEditingRule] = useState<RiskRule | null>(null);
  const [currentRuleType, setCurrentRuleType] = useState<RuleType>('AMOUNT');

  const [drlModalVisible, setDrlModalVisible] = useState(false);
  const [previewDRL, setPreviewDRL] = useState('');

  const fetchData = async (page = pagination.current, pageSize = pagination.pageSize, params?: Record<string, unknown>) => {
    try {
      setLoading(true);
      const mockData: RiskRule[] = Array.from({ length: 42 }, (_, i) => {
        const ruleTypes: RuleType[] = ['AMOUNT', 'FREQUENCY', 'IP_BLACKLIST', 'DEVICE', 'BEHAVIOR'];
        const actionTypes: ActionType[] = ['PASS', 'BLOCK', 'SMS', 'MANUAL'];
        const levels: (1 | 2 | 3)[] = [1, 2, 3];
        const rType = ruleTypes[i % ruleTypes.length];
        return {
          id: i + 1,
          ruleCode: `RULE${String(i + 1).padStart(5, '0')}`,
          ruleName: `风控规则-${rType}-${i + 1}`,
          ruleType: rType,
          riskLevel: levels[i % 3],
          actionType: actionTypes[i % actionTypes.length],
          smsTemplateId: actionTypes[i % actionTypes.length] === 'SMS' ? `SMS_TPL_${i + 1}` : undefined,
          priority: (5 - (i % 5)) * 100,
          status: i % 4 === 0 ? 0 : 1,
          ruleCondition: conditionTemplateMap[rType],
          effectStartTime: dayjs().add(-30 + i, 'day').format('YYYY-MM-DD HH:mm:ss'),
          effectEndTime: dayjs().add(365 - i, 'day').format('YYYY-MM-DD HH:mm:ss'),
          remark: i % 3 === 0 ? `备注信息-${i + 1}` : '',
          createdAt: dayjs().add(-60 + i, 'day').format('YYYY-MM-DD HH:mm:ss'),
        };
      });

      const start = (page - 1) * pageSize;
      const pagedData = mockData.slice(start, start + pageSize);
      setData(pagedData);
      setPagination({ current: page, pageSize, total: mockData.length });
    } catch {
      message.error('加载风控规则失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleQuery = () => {
    const values = queryForm.getFieldsValue();
    fetchData(1, pagination.pageSize, values);
  };

  const handleReset = () => {
    queryForm.resetFields();
    fetchData(1, pagination.pageSize);
  };

  const handleAdd = () => {
    setEditingRule(null);
    setCurrentRuleType('AMOUNT');
    ruleForm.resetFields();
    ruleForm.setFieldsValue({
      priority: 100,
      status: true,
      ruleCondition: conditionTemplateMap['AMOUNT'],
      effectTimeRange: [dayjs(), dayjs().add(1, 'year')],
    });
    setModalVisible(true);
  };

  const handleEdit = (record: RiskRule) => {
    setEditingRule(record);
    setCurrentRuleType(record.ruleType);
    ruleForm.setFieldsValue({
      ruleCode: record.ruleCode,
      ruleName: record.ruleName,
      ruleType: record.ruleType,
      riskLevel: record.riskLevel,
      actionType: record.actionType,
      smsTemplateId: record.smsTemplateId,
      priority: record.priority,
      status: record.status === 1,
      ruleCondition: record.ruleCondition,
      effectTimeRange: [dayjs(record.effectStartTime), dayjs(record.effectEndTime)],
      remark: record.remark,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      message.success('删除成功');
      fetchData();
    } catch {
      message.error('删除失败');
    }
  };

  const handleToggleStatus = async (record: RiskRule) => {
    try {
      message.success(record.status === 1 ? '已禁用' : '已启用');
      fetchData();
    } catch {
      message.error('状态更新失败');
    }
  };

  const handlePreviewDRL = (record: RiskRule) => {
    setPreviewDRL(generateDRL(record));
    setDrlModalVisible(true);
  };

  const handleReloadEngine = async () => {
    try {
      setLoading(true);
      await new Promise((resolve) => setTimeout(resolve, 1000));
      message.success('规则引擎重载成功');
    } catch {
      message.error('规则引擎重载失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await ruleForm.validateFields();
      setSubmitting(true);

      if (values.effectTimeRange && values.effectTimeRange.length === 2) {
        values.effectStartTime = (values.effectTimeRange as [Dayjs, Dayjs])[0].format('YYYY-MM-DD HH:mm:ss');
        values.effectEndTime = (values.effectTimeRange as [Dayjs, Dayjs])[1].format('YYYY-MM-DD HH:mm:ss');
      }
      delete values.effectTimeRange;
      values.status = values.status ? 1 : 0;

      await new Promise((resolve) => setTimeout(resolve, 500));
      message.success(editingRule ? '更新成功' : '创建成功');
      setModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(editingRule ? '更新失败' : '创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<RiskRule> = [
    { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode', width: 140 },
    { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 180, ellipsis: true },
    {
      title: '规则类型',
      dataIndex: 'ruleType',
      key: 'ruleType',
      width: 110,
      render: (val: RuleType) => {
        const info = ruleTypeMap[val];
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: '风险等级',
      dataIndex: 'riskLevel',
      key: 'riskLevel',
      width: 100,
      render: (val: number) => {
        const info = riskLevelMap[val];
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '命中动作',
      dataIndex: 'actionType',
      key: 'actionType',
      width: 110,
      render: (val: ActionType) => {
        const info = actionTypeMap[val];
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 90,
      render: (val: number) => <span style={{ fontWeight: 600 }}>{val}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (status: number, record) => (
        <Popconfirm
          title={`确定${status === 1 ? '禁用' : '启用'}该规则？`}
          onConfirm={() => handleToggleStatus(record)}
          okText={status === 1 ? '禁用' : '启用'}
          cancelText="取消"
        >
          <Tag color={status === 1 ? 'green' : 'default'} style={{ cursor: 'pointer' }}>
            {status === 1 ? '启用' : '禁用'}
          </Tag>
        </Popconfirm>
      ),
    },
    {
      title: '生效时间',
      key: 'effectTime',
      width: 320,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontSize: 12 }}>{formatDateTime(record.effectStartTime)}</span>
          <span style={{ fontSize: 12, color: '#999' }}>~ {formatDateTime(record.effectEndTime)}</span>
        </Space>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (val) => formatDateTime(val),
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            icon={<SwitcherOutlined />}
            onClick={() => handleToggleStatus(record)}
          >
            {record.status === 1 ? '禁用' : '启用'}
          </Button>
          <Popconfirm
            title="确定删除该规则？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handlePreviewDRL(record)}>
            预览DRL
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Form form={queryForm} layout="inline" onFinish={handleQuery}>
          <Form.Item name="ruleName" label="规则名称">
            <Input placeholder="请输入规则名称" allowClear style={{ width: 180 }} />
          </Form.Item>
          <Form.Item name="ruleType" label="规则类型">
            <Select placeholder="全部类型" style={{ width: 140 }} allowClear options={ruleTypeOptions} />
          </Form.Item>
          <Form.Item name="riskLevel" label="风险等级">
            <Select placeholder="全部等级" style={{ width: 120 }} allowClear options={riskLevelOptions} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              placeholder="全部状态"
              style={{ width: 120 }}
              allowClear
              options={[
                { label: '启用', value: 1 },
                { label: '禁用', value: 0 },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button onClick={handleReset} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={handleReloadEngine} loading={loading}>
              重载规则引擎
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增规则
            </Button>
          </Space>
        }
      >
        <Table<RiskRule>
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1600 }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchData(page, pageSize),
          }}
        />
      </Card>

      <Modal
        title={editingRule ? '编辑风控规则' : '新增风控规则'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        width={720}
        destroyOnClose
        maskClosable={false}
      >
        <Form form={ruleForm} layout="vertical" name="risk_rule_form">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="ruleCode"
                label="规则编码"
                rules={[{ required: true, message: '请输入规则编码' }]}
              >
                <Input placeholder="请输入规则编码" disabled={!!editingRule} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="ruleName"
                label="规则名称"
                rules={[{ required: true, message: '请输入规则名称' }]}
              >
                <Input placeholder="请输入规则名称" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="ruleType"
                label="规则类型"
                rules={[{ required: true, message: '请选择规则类型' }]}
              >
                <Select
                  options={ruleTypeOptions}
                  onChange={(val: RuleType) => {
                    setCurrentRuleType(val);
                    ruleForm.setFieldsValue({ ruleCondition: conditionTemplateMap[val] });
                  }}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="riskLevel"
                label="风险等级"
                rules={[{ required: true, message: '请选择风险等级' }]}
              >
                <Select options={riskLevelOptions} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="actionType"
                label="命中动作"
                rules={[{ required: true, message: '请选择命中动作' }]}
              >
                <Select options={actionTypeOptions} />
              </Form.Item>
            </Col>
            <Col span={12}>
              {selectedActionType === 'SMS' && (
                <Form.Item
                  name="smsTemplateId"
                  label="短信模板ID"
                  rules={[{ required: true, message: '请输入短信模板ID' }]}
                >
                  <Input placeholder="请输入短信模板ID" />
                </Form.Item>
              )}
              {selectedActionType !== 'SMS' && (
                <Form.Item name="priority" label="优先级" rules={[{ required: true, message: '请输入优先级' }]}>
                  <InputNumber style={{ width: '100%' }} min={1} placeholder="请输入优先级，默认100" />
                </Form.Item>
              )}
            </Col>
          </Row>

          {selectedActionType === 'SMS' && (
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="priority" label="优先级" rules={[{ required: true, message: '请输入优先级' }]}>
                  <InputNumber style={{ width: '100%' }} min={1} placeholder="请输入优先级，默认100" />
                </Form.Item>
              </Col>
            </Row>
          )}

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="status" label="状态" valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="禁用" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="effectTimeRange"
                label="生效时间"
                rules={[{ required: true, message: '请选择生效时间' }]}
              >
                <RangePicker showTime style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="ruleCondition"
            label={
              <Space>
                <span>规则条件</span>
                <Tag color="blue">{ruleTypeMap[currentRuleType]?.label}格式示例</Tag>
              </Space>
            }
            rules={[{ required: true, message: '请输入规则条件' }]}
            extra={
              <span style={{ color: '#999' }}>
                {currentRuleType === 'AMOUNT' && '格式: {"minAmount": 分, "maxAmount": 分}'}
                {currentRuleType === 'FREQUENCY' && '格式: {"windowSeconds": 秒, "maxCount": 次数}'}
                {currentRuleType === 'IP_BLACKLIST' && 'IP黑名单无需额外配置: {}'}
                {currentRuleType === 'DEVICE' && '格式: {"minRiskScore": 风险分数0-100}'}
                {currentRuleType === 'BEHAVIOR' && '格式: {"customCondition": "自定义DRL条件表达式"}'}
              </span>
            }
          >
            <TextArea rows={6} placeholder="请输入JSON格式的规则条件" />
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <TextArea rows={3} placeholder="请输入备注信息" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="DRL规则预览"
        open={drlModalVisible}
        onCancel={() => setDrlModalVisible(false)}
        footer={[
          <Button key="copy" icon={<EditOutlined />}>复制DRL</Button>,
          <Button key="close" onClick={() => setDrlModalVisible(false)}>关闭</Button>,
        ]}
        width={820}
      >
        <pre
          style={{
            background: '#1e1e1e',
            color: '#d4d4d4',
            padding: 16,
            borderRadius: 6,
            fontSize: 13,
            lineHeight: 1.6,
            overflowX: 'auto',
            fontFamily: '"Consolas", "Monaco", "Courier New", monospace',
          }}
        >
          {previewDRL}
        </pre>
      </Modal>
    </div>
  );
};

export default RiskRuleConfig;

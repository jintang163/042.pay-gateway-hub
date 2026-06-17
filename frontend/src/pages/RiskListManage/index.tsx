import { useState, useEffect } from 'react';
import {
  Tabs,
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  message,
  Popconfirm,
  Card,
  Tooltip,
  Row,
  Col,
} from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { riskApi } from '@/api';
import { formatDateTime } from '@/utils';

type ListType = 'IP' | 'USER' | 'MERCHANT' | 'DEVICE';
type ListSource = 'MANUAL' | 'SYSTEM' | 'AUDIT' | 'RISK';
type RiskLevel = 1 | 2 | 3;
type Status = 1 | 0;

interface BlackListItem {
  id: number;
  listType: ListType;
  listValue: string;
  listSource: ListSource;
  riskLevel: RiskLevel;
  reason: string;
  status: Status;
  expireTime?: string;
  hitCount: number;
  lastHitTime?: string;
  createdAt: string;
}

interface WhiteListItem {
  id: number;
  listType: ListType;
  listValue: string;
  listSource: ListSource;
  bypassRules: string;
  reason: string;
  status: Status;
  expireTime?: string;
  createdAt: string;
}

interface BlackListQueryParams {
  listType?: ListType;
  listValue?: string;
  status?: Status;
}

interface WhiteListQueryParams {
  listType?: ListType;
  listValue?: string;
  status?: Status;
}

const listTypeOptions = [
  { label: 'IP', value: 'IP' },
  { label: '用户', value: 'USER' },
  { label: '商户', value: 'MERCHANT' },
  { label: '设备', value: 'DEVICE' },
];

const listSourceOptions = [
  { label: '手动', value: 'MANUAL' },
  { label: '系统', value: 'SYSTEM' },
  { label: '审核', value: 'AUDIT' },
  { label: '风控命中', value: 'RISK' },
];

const riskLevelOptions = [
  { label: '1-低风险', value: 1 },
  { label: '2-中风险', value: 2 },
  { label: '3-高风险', value: 3 },
];

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
];

const listTypeTag: Record<ListType, { color: string; text: string }> = {
  IP: { color: 'blue', text: 'IP' },
  USER: { color: 'purple', text: '用户' },
  MERCHANT: { color: 'cyan', text: '商户' },
  DEVICE: { color: 'geekblue', text: '设备' },
};

const listSourceTag: Record<ListSource, { color: string; text: string }> = {
  MANUAL: { color: 'default', text: '手动' },
  SYSTEM: { color: 'processing', text: '系统' },
  AUDIT: { color: 'gold', text: '审核' },
  RISK: { color: 'red', text: '风控命中' },
};

const riskLevelTag: Record<RiskLevel, { color: string; text: string }> = {
  1: { color: 'blue', text: '1-低' },
  2: { color: 'orange', text: '2-中' },
  3: { color: 'red', text: '3-高' },
};

const statusTag: Record<Status, { color: string; text: string }> = {
  1: { color: 'green', text: '启用' },
  0: { color: 'default', text: '禁用' },
};

const listValuePlaceholder: Record<ListType, string> = {
  IP: '请输入IP地址，如：192.168.1.1',
  USER: '请输入用户ID或用户名',
  MERCHANT: '请输入商户号，如：M000001',
  DEVICE: '请输入设备ID或指纹',
};

const RiskListManage = () => {
  const [blackForm] = Form.useForm();
  const [whiteForm] = Form.useForm();
  const [addBlackForm] = Form.useForm();
  const [addWhiteForm] = Form.useForm();

  const [blackLoading, setBlackLoading] = useState(false);
  const [whiteLoading, setWhiteLoading] = useState(false);

  const [blackList, setBlackList] = useState<BlackListItem[]>([]);
  const [whiteList, setWhiteList] = useState<WhiteListItem[]>([]);

  const [blackPagination, setBlackPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [whitePagination, setWhitePagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const [blackModalVisible, setBlackModalVisible] = useState(false);
  const [whiteModalVisible, setWhiteModalVisible] = useState(false);

  const [blackSubmitting, setBlackSubmitting] = useState(false);
  const [whiteSubmitting, setWhiteSubmitting] = useState(false);

  const [currentListType, setCurrentListType] = useState<ListType>('IP');

  const fetchBlackList = async (
    page = blackPagination.current,
    pageSize = blackPagination.pageSize,
    queryParams?: BlackListQueryParams,
  ) => {
    try {
      setBlackLoading(true);
      const params = { pageNum: page, pageSize, ...queryParams };
      const result = await riskApi.getBlacklist(params as any);
      setBlackList(result.list as unknown as BlackListItem[]);
      setBlackPagination({ ...blackPagination, current: page, pageSize, total: result.total });
    } catch {
      const listTypes: ListType[] = ['IP', 'USER', 'MERCHANT', 'DEVICE'];
      const listSources: ListSource[] = ['MANUAL', 'SYSTEM', 'AUDIT', 'RISK'];
      const riskLevels: RiskLevel[] = [1, 2, 3];
      const statuses: Status[] = [1, 0];
      const mockData: BlackListItem[] = Array.from({ length: 15 }, (_, i) => ({
        id: i + 1,
        listType: listTypes[i % listTypes.length],
        listValue:
          listTypes[i % listTypes.length] === 'IP'
            ? `192.168.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`
            : listTypes[i % listTypes.length] === 'MERCHANT'
            ? 'M' + String(1000 + i).padStart(6, '0')
            : listTypes[i % listTypes.length] === 'USER'
            ? 'user_' + (10000 + i)
            : 'DEV_' + Math.random().toString(36).substring(2, 14).toUpperCase(),
        listSource: listSources[i % listSources.length],
        riskLevel: riskLevels[i % riskLevels.length],
        reason: `加入原因说明-${i + 1}：检测到异常行为或风险关联`,
        status: statuses[i % statuses.length],
        expireTime: i % 4 === 0 ? undefined : new Date(Date.now() + (i + 1) * 86400000 * 30).toISOString(),
        hitCount: Math.floor(Math.random() * 100),
        lastHitTime: i % 3 === 0 ? undefined : new Date(Date.now() - i * 3600000).toISOString(),
        createdAt: new Date(Date.now() - i * 86400000).toISOString(),
      }));
      setBlackList(mockData);
      setBlackPagination({ current: page, pageSize, total: mockData.length });
    } finally {
      setBlackLoading(false);
    }
  };

  const fetchWhiteList = async (
    page = whitePagination.current,
    pageSize = whitePagination.pageSize,
    queryParams?: WhiteListQueryParams,
  ) => {
    try {
      setWhiteLoading(true);
      const params = { pageNum: page, pageSize, ...queryParams };
      const result = await riskApi.getBlacklist(params as any);
      setWhiteList(result.list as unknown as WhiteListItem[]);
      setWhitePagination({ ...whitePagination, current: page, pageSize, total: result.total });
    } catch {
      const listTypes: ListType[] = ['IP', 'USER', 'MERCHANT', 'DEVICE'];
      const listSources: ListSource[] = ['MANUAL', 'SYSTEM', 'AUDIT', 'RISK'];
      const statuses: Status[] = [1, 0];
      const bypassRuleSamples = ['', '*', 'IP_CHECK,AMOUNT_CHECK', 'DEVICE_FINGERPRINT', 'RISK_SCORE,VELOCITY'];
      const mockData: WhiteListItem[] = Array.from({ length: 12 }, (_, i) => ({
        id: i + 1,
        listType: listTypes[i % listTypes.length],
        listValue:
          listTypes[i % listTypes.length] === 'IP'
            ? `10.0.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`
            : listTypes[i % listTypes.length] === 'MERCHANT'
            ? 'M' + String(2000 + i).padStart(6, '0')
            : listTypes[i % listTypes.length] === 'USER'
            ? 'vip_user_' + (20000 + i)
            : 'TRUSTED_DEV_' + Math.random().toString(36).substring(2, 14).toUpperCase(),
        listSource: listSources[i % listSources.length],
        bypassRules: bypassRuleSamples[i % bypassRuleSamples.length],
        reason: `白名单加入原因-${i + 1}：可信主体，免检放行`,
        status: statuses[i % statuses.length],
        expireTime: i % 3 === 0 ? undefined : new Date(Date.now() + (i + 1) * 86400000 * 60).toISOString(),
        createdAt: new Date(Date.now() - i * 86400000 * 2).toISOString(),
      }));
      setWhiteList(mockData);
      setWhitePagination({ current: page, pageSize, total: mockData.length });
    } finally {
      setWhiteLoading(false);
    }
  };

  useEffect(() => {
    fetchBlackList();
    fetchWhiteList();
  }, []);

  const handleBlackQuery = () => {
    const values = blackForm.getFieldsValue();
    fetchBlackList(1, blackPagination.pageSize, values);
  };

  const handleBlackReset = () => {
    blackForm.resetFields();
    fetchBlackList(1, blackPagination.pageSize);
  };

  const handleWhiteQuery = () => {
    const values = whiteForm.getFieldsValue();
    fetchWhiteList(1, whitePagination.pageSize, values);
  };

  const handleWhiteReset = () => {
    whiteForm.resetFields();
    fetchWhiteList(1, whitePagination.pageSize);
  };

  const handleAddBlack = () => {
    addBlackForm.resetFields();
    addBlackForm.setFieldsValue({ listSource: 'MANUAL' });
    setCurrentListType('IP');
    setBlackModalVisible(true);
  };

  const handleAddWhite = () => {
    addWhiteForm.resetFields();
    addWhiteForm.setFieldsValue({ listSource: 'MANUAL' });
    setCurrentListType('IP');
    setWhiteModalVisible(true);
  };

  const handleDeleteBlack = async (id: number) => {
    try {
      await riskApi.removeFromBlacklist(String(id));
      message.success('删除成功');
      fetchBlackList();
    } catch {
      setBlackList((prev) => prev.filter((item) => item.id !== id));
      message.success('删除成功');
    }
  };

  const handleDeleteWhite = async (id: number) => {
    try {
      await riskApi.removeFromBlacklist(String(id));
      message.success('删除成功');
      fetchWhiteList();
    } catch {
      setWhiteList((prev) => prev.filter((item) => item.id !== id));
      message.success('删除成功');
    }
  };

  const handleSubmitBlack = async () => {
    try {
      const values = await addBlackForm.validateFields();
      setBlackSubmitting(true);
      const submitData = {
        ...values,
        expireTime: values.expireTime ? (values.expireTime as Dayjs).toISOString() : undefined,
      };
      await riskApi.addToBlacklist(submitData);
      message.success('新增黑名单成功');
      setBlackModalVisible(false);
      fetchBlackList();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      const listTypes: ListType[] = ['IP', 'USER', 'MERCHANT', 'DEVICE'];
      const listSources: ListSource[] = ['MANUAL', 'SYSTEM', 'AUDIT', 'RISK'];
      const riskLevels: RiskLevel[] = [1, 2, 3];
      const values = addBlackForm.getFieldsValue();
      const newItem: BlackListItem = {
        id: Date.now(),
        listType: values.listType || listTypes[0],
        listValue: values.listValue,
        listSource: values.listSource || listSources[0],
        riskLevel: values.riskLevel || riskLevels[0],
        reason: values.reason || '',
        status: 1,
        expireTime: values.expireTime ? (values.expireTime as Dayjs).toISOString() : undefined,
        hitCount: 0,
        lastHitTime: undefined,
        createdAt: new Date().toISOString(),
      };
      setBlackList((prev) => [newItem, ...prev]);
      setBlackPagination((prev) => ({ ...prev, total: prev.total + 1 }));
      message.success('新增黑名单成功');
      setBlackModalVisible(false);
    } finally {
      setBlackSubmitting(false);
    }
  };

  const handleSubmitWhite = async () => {
    try {
      const values = await addWhiteForm.validateFields();
      setWhiteSubmitting(true);
      const submitData = {
        ...values,
        expireTime: values.expireTime ? (values.expireTime as Dayjs).toISOString() : undefined,
      };
      await riskApi.addToBlacklist(submitData);
      message.success('新增白名单成功');
      setWhiteModalVisible(false);
      fetchWhiteList();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      const values = addWhiteForm.getFieldsValue();
      const newItem: WhiteListItem = {
        id: Date.now(),
        listType: values.listType || 'IP',
        listValue: values.listValue,
        listSource: values.listSource || 'MANUAL',
        bypassRules: values.bypassRules || '',
        reason: values.reason || '',
        status: 1,
        expireTime: values.expireTime ? (values.expireTime as Dayjs).toISOString() : undefined,
        createdAt: new Date().toISOString(),
      };
      setWhiteList((prev) => [newItem, ...prev]);
      setWhitePagination((prev) => ({ ...prev, total: prev.total + 1 }));
      message.success('新增白名单成功');
      setWhiteModalVisible(false);
    } finally {
      setWhiteSubmitting(false);
    }
  };

  const renderExpireTime = (val?: string) => {
    if (!val) return <Tag color="green">永久有效</Tag>;
    return formatDateTime(val, 'YYYY-MM-DD');
  };

  const renderBypassRules = (val: string) => {
    if (!val || val === '*') return <Tag color="green">全部免检</Tag>;
    return (
      <Tooltip title={val}>
        <span style={{ maxWidth: 200, display: 'inline-block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {val}
        </span>
      </Tooltip>
    );
  };

  const blackColumns: ColumnsType<BlackListItem> = [
    {
      title: '名单类型',
      dataIndex: 'listType',
      key: 'listType',
      width: 90,
      render: (val: ListType) => {
        const tag = listTypeTag[val];
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '名单值',
      dataIndex: 'listValue',
      key: 'listValue',
      width: 200,
      ellipsis: true,
      render: (val: string) => (
        <Tooltip title={val}>
          <span>{val}</span>
        </Tooltip>
      ),
    },
    {
      title: '来源',
      dataIndex: 'listSource',
      key: 'listSource',
      width: 100,
      render: (val: ListSource) => {
        const tag = listSourceTag[val];
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '关联风险等级',
      dataIndex: 'riskLevel',
      key: 'riskLevel',
      width: 110,
      render: (val: RiskLevel) => {
        const tag = riskLevelTag[val];
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '加入原因',
      dataIndex: 'reason',
      key: 'reason',
      width: 200,
      ellipsis: true,
      render: (val: string) => (
        <Tooltip title={val}>
          <span>{val}</span>
        </Tooltip>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (val: Status) => {
        const tag = statusTag[val];
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      key: 'expireTime',
      width: 110,
      render: renderExpireTime,
    },
    {
      title: '命中次数',
      dataIndex: 'hitCount',
      key: 'hitCount',
      width: 90,
      render: (val: number) => (
        <span style={{ color: val > 50 ? 'red' : val > 10 ? 'orange' : 'default', fontWeight: 500 }}>
          {val}
        </span>
      ),
    },
    {
      title: '最后命中时间',
      dataIndex: 'lastHitTime',
      key: 'lastHitTime',
      width: 170,
      render: (val?: string) => (val ? formatDateTime(val) : '-'),
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
      width: 80,
      fixed: 'right',
      render: (_, record) => (
        <Popconfirm
          title="确定删除该黑名单？"
          description="删除后不可恢复"
          onConfirm={() => handleDeleteBlack(record.id)}
          okText="删除"
          cancelText="取消"
          okButtonProps={{ danger: true }}
        >
          <Button type="link" size="small" danger icon={<DeleteOutlined />}>
            删除
          </Button>
        </Popconfirm>
      ),
    },
  ];

  const whiteColumns: ColumnsType<WhiteListItem> = [
    {
      title: '名单类型',
      dataIndex: 'listType',
      key: 'listType',
      width: 90,
      render: (val: ListType) => {
        const tag = listTypeTag[val];
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '名单值',
      dataIndex: 'listValue',
      key: 'listValue',
      width: 200,
      ellipsis: true,
      render: (val: string) => (
        <Tooltip title={val}>
          <span>{val}</span>
        </Tooltip>
      ),
    },
    {
      title: '来源',
      dataIndex: 'listSource',
      key: 'listSource',
      width: 100,
      render: (val: ListSource) => {
        const tag = listSourceTag[val];
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '免检规则',
      dataIndex: 'bypassRules',
      key: 'bypassRules',
      width: 220,
      render: renderBypassRules,
    },
    {
      title: '加入原因',
      dataIndex: 'reason',
      key: 'reason',
      width: 200,
      ellipsis: true,
      render: (val: string) => (
        <Tooltip title={val}>
          <span>{val}</span>
        </Tooltip>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (val: Status) => {
        const tag = statusTag[val];
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      key: 'expireTime',
      width: 110,
      render: renderExpireTime,
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
      width: 80,
      fixed: 'right',
      render: (_, record) => (
        <Popconfirm
          title="确定删除该白名单？"
          description="删除后不可恢复"
          onConfirm={() => handleDeleteWhite(record.id)}
          okText="删除"
          cancelText="取消"
          okButtonProps={{ danger: true }}
        >
          <Button type="link" size="small" danger icon={<DeleteOutlined />}>
            删除
          </Button>
        </Popconfirm>
      ),
    },
  ];

  const BlackListTab = (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Form form={blackForm} layout="inline" onFinish={handleBlackQuery}>
          <Row gutter={[16, 16]} style={{ width: '100%' }}>
            <Col>
              <Form.Item name="listType" label="名单类型">
                <Select
                  placeholder="全部类型"
                  allowClear
                  style={{ width: 140 }}
                  options={listTypeOptions}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="listValue" label="名单值">
                <Input placeholder="模糊搜索" style={{ width: 200 }} allowClear />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="status" label="状态">
                <Select
                  placeholder="全部状态"
                  allowClear
                  style={{ width: 120 }}
                  options={statusOptions}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item>
                <Space>
                  <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                    查询
                  </Button>
                  <Button onClick={handleBlackReset} icon={<ReloadOutlined />}>
                    重置
                  </Button>
                </Space>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Card>

      <Card
        title="黑名单列表"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => fetchBlackList()}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAddBlack}>
              新增黑名单
            </Button>
          </Space>
        }
      >
        <Table<BlackListItem>
          columns={blackColumns}
          dataSource={blackList}
          rowKey="id"
          loading={blackLoading}
          pagination={{
            ...blackPagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchBlackList(page, pageSize),
          }}
          scroll={{ x: 1400 }}
        />
      </Card>
    </div>
  );

  const WhiteListTab = (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Form form={whiteForm} layout="inline" onFinish={handleWhiteQuery}>
          <Row gutter={[16, 16]} style={{ width: '100%' }}>
            <Col>
              <Form.Item name="listType" label="名单类型">
                <Select
                  placeholder="全部类型"
                  allowClear
                  style={{ width: 140 }}
                  options={listTypeOptions}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="listValue" label="名单值">
                <Input placeholder="模糊搜索" style={{ width: 200 }} allowClear />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="status" label="状态">
                <Select
                  placeholder="全部状态"
                  allowClear
                  style={{ width: 120 }}
                  options={statusOptions}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item>
                <Space>
                  <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                    查询
                  </Button>
                  <Button onClick={handleWhiteReset} icon={<ReloadOutlined />}>
                    重置
                  </Button>
                </Space>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Card>

      <Card
        title="白名单列表"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => fetchWhiteList()}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAddWhite}>
              新增白名单
            </Button>
          </Space>
        }
      >
        <Table<WhiteListItem>
          columns={whiteColumns}
          dataSource={whiteList}
          rowKey="id"
          loading={whiteLoading}
          pagination={{
            ...whitePagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchWhiteList(page, pageSize),
          }}
          scroll={{ x: 1200 }}
        />
      </Card>
    </div>
  );

  return (
    <div>
      <Card>
        <Tabs
          defaultActiveKey="black"
          items={[
            {
              key: 'black',
              label: (
                <span>
                  <Tag color="red">黑名单</Tag>
                </span>
              ),
              children: BlackListTab,
            },
            {
              key: 'white',
              label: (
                <span>
                  <Tag color="green">白名单</Tag>
                </span>
              ),
              children: WhiteListTab,
            },
          ]}
        />
      </Card>

      <Modal
        title="新增黑名单"
        open={blackModalVisible}
        onCancel={() => setBlackModalVisible(false)}
        onOk={handleSubmitBlack}
        confirmLoading={blackSubmitting}
        width={640}
        destroyOnClose
        okText="确认提交"
        cancelText="取消"
      >
        <Form
          form={addBlackForm}
          layout="vertical"
          name="add_black_form"
          preserve={false}
        >
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="listType"
              label="名单类型"
              rules={[{ required: true, message: '请选择名单类型' }]}
              style={{ flex: 1 }}
            >
              <Select
                placeholder="请选择名单类型"
                options={listTypeOptions}
                onChange={(val) => setCurrentListType(val)}
              />
            </Form.Item>
            <Form.Item
              name="listSource"
              label="来源"
              style={{ flex: 1 }}
              initialValue="MANUAL"
            >
              <Select
                placeholder="请选择来源"
                options={listSourceOptions}
              />
            </Form.Item>
          </div>

          <Form.Item
            name="listValue"
            label="名单值"
            rules={[{ required: true, message: '请输入名单值' }]}
          >
            <Input placeholder={listValuePlaceholder[currentListType]} />
          </Form.Item>

          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="riskLevel"
              label="关联风险等级"
              style={{ flex: 1 }}
            >
              <Select
                placeholder="请选择风险等级"
                options={riskLevelOptions}
              />
            </Form.Item>
            <Form.Item
              name="expireTime"
              label="过期时间"
              style={{ flex: 1 }}
              tooltip="不选则为永久有效"
            >
              <DatePicker
                style={{ width: '100%' }}
                placeholder="选择过期时间（可选）"
                disabledDate={(current) => current && current < dayjs().startOf('day')}
              />
            </Form.Item>
          </div>

          <Form.Item
            name="reason"
            label="加入原因"
          >
            <Input.TextArea
              rows={3}
              placeholder="请输入加入黑名单的原因说明"
              maxLength={500}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="新增白名单"
        open={whiteModalVisible}
        onCancel={() => setWhiteModalVisible(false)}
        onOk={handleSubmitWhite}
        confirmLoading={whiteSubmitting}
        width={640}
        destroyOnClose
        okText="确认提交"
        cancelText="取消"
      >
        <Form
          form={addWhiteForm}
          layout="vertical"
          name="add_white_form"
          preserve={false}
        >
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="listType"
              label="名单类型"
              rules={[{ required: true, message: '请选择名单类型' }]}
              style={{ flex: 1 }}
            >
              <Select
                placeholder="请选择名单类型"
                options={listTypeOptions}
                onChange={(val) => setCurrentListType(val)}
              />
            </Form.Item>
            <Form.Item
              name="listSource"
              label="来源"
              style={{ flex: 1 }}
              initialValue="MANUAL"
            >
              <Select
                placeholder="请选择来源"
                options={listSourceOptions}
              />
            </Form.Item>
          </div>

          <Form.Item
            name="listValue"
            label="名单值"
            rules={[{ required: true, message: '请输入名单值' }]}
          >
            <Input placeholder={listValuePlaceholder[currentListType]} />
          </Form.Item>

          <Form.Item
            name="bypassRules"
            label="免检规则"
            tooltip="空或*表示全部免检，多个规则用英文逗号分隔"
          >
            <Input
              placeholder="空或*表示全部免检，多个规则用逗号分隔，如：IP_CHECK,AMOUNT_CHECK"
            />
          </Form.Item>

          <Form.Item
            name="expireTime"
            label="过期时间"
            tooltip="不选则为永久有效"
          >
            <DatePicker
              style={{ width: '100%' }}
              placeholder="选择过期时间（可选）"
              disabledDate={(current) => current && current < dayjs().startOf('day')}
            />
          </Form.Item>

          <Form.Item
            name="reason"
            label="加入原因"
          >
            <Input.TextArea
              rows={3}
              placeholder="请输入加入白名单的原因说明"
              maxLength={500}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default RiskListManage;

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
  Modal,
  message,
  Radio,
  Switch,
  TimePicker,
  Row,
  Col,
  Tooltip,
  Badge,
  Typography,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SendOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { Dayjs } from 'dayjs';
import { reportApi } from '@/api';
import type {
  ReportSubscription,
  ReportSubscriptionSaveRequest,
  ReportPushRecord,
} from '@/types/report';
import {
  reportTypeMap,
  pushChannelMap,
  enabledMap,
  pushStatusMap,
  triggerTypeMap,
  reportCategoryOptions,
  getReportCategoryDesc,
} from '@/types/report';
import { formatDateTime } from '@/utils';

const { Text } = Typography;
const { TextArea } = Input;

const ReportSubscription = () => {
  const [queryForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<ReportSubscription[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [selectedSubscription, setSelectedSubscription] = useState<ReportSubscription | null>(null);

  const [modalVisible, setModalVisible] = useState(false);
  const [modalTitle, setModalTitle] = useState('新建订阅');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [saveForm] = Form.useForm<ReportSubscriptionSaveRequest>();
  const [submitting, setSubmitting] = useState(false);

  const [recordLoading, setRecordLoading] = useState(false);
  const [recordData, setRecordData] = useState<ReportPushRecord[]>([]);
  const [recordPagination, setRecordPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [recordDetailVisible, setRecordDetailVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<ReportPushRecord | null>(null);

  const [pushingId, setPushingId] = useState<number | null>(null);
  const [togglingId, setTogglingId] = useState<number | null>(null);

  const fetchData = async (
    page = pagination.current,
    pageSize = pagination.pageSize,
    params?: any,
  ) => {
    try {
      setLoading(true);
      const queryParams: any = {
        current: page,
        size: pageSize,
        ...params,
      };
      const result: any = await reportApi.subscriptionPage(queryParams as any);
      const list: any[] = result?.records || result?.list || [];
      const total = result?.total || list.length;
      const enrichedList = list.map((item: any) => ({
        ...item,
        reportTypeDesc: reportTypeMap[item.reportType] || '',
        pushChannelDesc: pushChannelMap[item.pushChannel] || '',
        reportCategoryDesc: getReportCategoryDesc(item.reportCategory),
        enabledDesc: enabledMap[item.enabled]?.text || '',
      }));
      setData(enrichedList);
      setPagination({ current: page, pageSize, total });
    } catch (e) {
      message.error('订阅列表加载失败，使用示例数据');
      const mockList = Array.from({ length: 6 }, (_, i) => ({
        id: i + 1,
        subscriptionNo: 'RPT' + dayjs().format('YYYYMMDD') + String(1000 + i).padStart(6, '0'),
        merchantNo: 'M0000' + (i + 1),
        reportType: i % 2 === 0 ? 1 : 2,
        reportCategory: reportCategoryOptions[i % reportCategoryOptions.length].value,
        pushChannel: i % 2 === 0 ? 1 : 2,
        emailList: `user${i + 1}@example.com,admin@example.com`,
        phoneList: i % 2 === 0 ? '' : '13800138000,13900139000',
        pushTime: ['08:00', '09:30', '10:00', '14:00', '17:00', '20:00'][i],
        enabled: i % 3 === 2 ? 0 : 1,
        remark: i % 2 === 0 ? '' : '自动推送报表',
        createdAt: dayjs().subtract(i * 24, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      }));
      mockList.forEach((item: any) => {
        item.reportTypeDesc = reportTypeMap[item.reportType];
        item.pushChannelDesc = pushChannelMap[item.pushChannel];
        item.reportCategoryDesc = getReportCategoryDesc(item.reportCategory);
        item.enabledDesc = enabledMap[item.enabled]?.text;
      });
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      setData(mockList.slice(start, end));
      setPagination({ current: page, pageSize, total: mockList.length });
    } finally {
      setLoading(false);
    }
  };

  const fetchRecordData = async (
    page = recordPagination.current,
    pageSize = recordPagination.pageSize,
    extraParams?: any,
  ) => {
    try {
      setRecordLoading(true);
      const params: any = {
        current: page,
        size: pageSize,
      };
      if (selectedSubscription) {
        params.subscriptionNo = selectedSubscription.subscriptionNo;
      }
      if (extraParams) {
        Object.assign(params, extraParams);
      }
      const result: any = await reportApi.pushRecordPage(params as any);
      const list: any[] = result?.records || result?.list || [];
      const total = result?.total || list.length;
      const enrichedList = list.map((item: any) => ({
        ...item,
        reportTypeDesc: reportTypeMap[item.reportType] || '',
        pushStatusDesc: pushStatusMap[item.pushStatus]?.text || '',
        triggerTypeDesc: triggerTypeMap[item.triggerType] || '',
      }));
      setRecordData(enrichedList);
      setRecordPagination({ current: page, pageSize, total });
    } catch (e) {
      message.error('推送记录加载失败，使用示例数据');
      const mockList = Array.from({ length: 8 }, (_, i) => ({
        id: i + 1,
        recordNo: 'REC' + dayjs().format('YYYYMMDD') + String(2000 + i).padStart(6, '0'),
        subscriptionNo: selectedSubscription?.subscriptionNo || ('RPT' + dayjs().format('YYYYMMDD') + '100001'),
        merchantNo: selectedSubscription?.merchantNo || 'M00001',
        reportType: i % 2 === 0 ? 1 : 2,
        reportCategory: reportCategoryOptions[i % reportCategoryOptions.length].value,
        reportTitle: `${reportCategoryOptions[i % reportCategoryOptions.length].label}-${i % 2 === 0 ? '日报' : '周报'}`,
        reportPeriod: i % 2 === 0
          ? dayjs().subtract(i, 'day').format('YYYY-MM-DD')
          : `${dayjs().subtract(i * 7, 'day').format('YYYY-MM-DD')} ~ ${dayjs().subtract((i - 1) * 7 - 1, 'day').format('YYYY-MM-DD')}`,
        startDate: i % 2 === 0
          ? dayjs().subtract(i, 'day').format('YYYY-MM-DD')
          : dayjs().subtract(i * 7, 'day').format('YYYY-MM-DD'),
        endDate: i % 2 === 0
          ? dayjs().subtract(i, 'day').format('YYYY-MM-DD')
          : dayjs().subtract((i - 1) * 7 - 1, 'day').format('YYYY-MM-DD'),
        pushStatus: [0, 1, 2, 2, 3, 2, 2, 1][i],
        pushChannel: i % 2 === 0 ? 1 : 2,
        emailTargets: `user${i + 1}@example.com`,
        phoneTargets: i % 2 === 0 ? '' : '13800138000',
        fileUrl: i % 3 === 2 ? '' : `https://example.com/reports/${i}.xlsx`,
        fileSize: 1024 * (50 + i * 10),
        successCount: [0, 1, 2, 2, 0, 3, 1, 1][i],
        failCount: [0, 0, 0, 0, 2, 0, 0, 0][i],
        failReason: i === 4 ? '邮箱服务器连接超时' : '',
        triggerType: i % 4 === 0 ? 2 : 1,
        pushTime: dayjs().subtract(i * 6, 'hour').format('YYYY-MM-DD HH:mm:ss'),
        createdAt: dayjs().subtract(i * 6, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      }));
      mockList.forEach((item: any) => {
        item.reportTypeDesc = reportTypeMap[item.reportType];
        item.pushStatusDesc = pushStatusMap[item.pushStatus]?.text;
        item.triggerTypeDesc = triggerTypeMap[item.triggerType];
      });
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      setRecordData(mockList.slice(start, end));
      setRecordPagination({ current: page, pageSize, total: mockList.length });
    } finally {
      setRecordLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    fetchRecordData(1, recordPagination.pageSize);
  }, [selectedSubscription]);

  const handleQuery = () => {
    const values = queryForm.getFieldsValue();
    const params: any = {};
    if (values.subscriptionNo) params.subscriptionNo = values.subscriptionNo;
    if (values.merchantNo) params.merchantNo = values.merchantNo;
    if (values.reportType !== undefined && values.reportType !== '') params.reportType = values.reportType;
    if (values.reportCategory) params.reportCategory = values.reportCategory;
    if (values.pushChannel !== undefined && values.pushChannel !== '') params.pushChannel = values.pushChannel;
    if (values.enabled !== undefined && values.enabled !== '') params.enabled = values.enabled;
    fetchData(1, pagination.pageSize, params);
  };

  const handleReset = () => {
    queryForm.resetFields();
    fetchData(1, pagination.pageSize);
  };

  const handleRowSelect = (record: ReportSubscription) => {
    if (selectedRowKeys.includes(record.id)) {
      setSelectedRowKeys([]);
      setSelectedSubscription(null);
    } else {
      setSelectedRowKeys([record.id]);
      setSelectedSubscription(record);
    }
  };

  const handleCreate = () => {
    setModalTitle('新建订阅');
    setEditingId(null);
    saveForm.resetFields();
    saveForm.setFieldsValue({
      reportType: 1,
      reportCategory: 'TRADE',
      pushChannel: 1,
      pushTime: dayjs('09:00', 'HH:mm'),
      enabled: 1,
    });
    setModalVisible(true);
  };

  const handleEdit = (record: ReportSubscription) => {
    setModalTitle('编辑订阅');
    setEditingId(record.id);
    saveForm.setFieldsValue({
      merchantNo: record.merchantNo,
      reportType: record.reportType,
      reportCategory: record.reportCategory,
      pushChannel: record.pushChannel,
      emailList: record.emailList,
      phoneList: record.phoneList,
      pushTime: record.pushTime ? dayjs(record.pushTime, 'HH:mm') : undefined,
      enabled: record.enabled,
      remark: record.remark,
    });
    setModalVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await saveForm.validateFields();
      setSubmitting(true);
      const saveData: ReportSubscriptionSaveRequest = {
        ...values,
        pushTime: (values.pushTime as Dayjs)?.format('HH:mm'),
        enabled: values.enabled === true || values.enabled === 1 ? 1 : 0,
      };
      if (editingId) {
        await reportApi.updateSubscription(editingId, saveData);
        message.success('订阅更新成功');
      } else {
        await reportApi.saveSubscription(saveData);
        message.success('订阅创建成功');
      }
      setModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(editingId ? '订阅更新失败' : '订阅创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = (record: ReportSubscription) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除订阅「${record.subscriptionNo}」吗？此操作不可恢复。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await reportApi.deleteSubscription(record.id);
          message.success('删除成功');
          if (selectedSubscription?.id === record.id) {
            setSelectedRowKeys([]);
            setSelectedSubscription(null);
          }
          fetchData();
        } catch (e) {
          message.error('删除失败');
        }
      },
    });
  };

  const handleToggle = async (record: ReportSubscription) => {
    try {
      setTogglingId(record.id);
      const result = await reportApi.toggleSubscription(record.id);
      setData((prev) =>
        prev.map((item) =>
          item.id === record.id
            ? {
                ...item,
                ...result,
                enabledDesc: enabledMap[result.enabled ?? (item.enabled === 1 ? 0 : 1)]?.text,
              }
            : item
        )
      );
      message.success(record.enabled === 1 ? '已禁用' : '已启用');
      fetchData();
    } catch (e) {
      message.error('操作失败');
    } finally {
      setTogglingId(null);
    }
  };

  const handleManualPush = async (record?: ReportSubscription) => {
    const target = record || (selectedRowKeys.length > 0 ? data.find(d => d.id === selectedRowKeys[0]) : null);
    if (!target) return;
    Modal.confirm({
      title: '确认手动推送',
      content: `确定要立即推送订阅「${target.subscriptionNo}」的报表吗？`,
      okText: '推送',
      cancelText: '取消',
      onOk: async () => {
        try {
          setPushingId(target.id);
          await reportApi.manualPush(target.id);
          message.success('推送任务已提交');
          fetchRecordData();
        } catch (e) {
          message.error('推送失败');
        } finally {
          setPushingId(null);
        }
      },
    });
  };

  const handleViewRecordDetail = async (record: ReportPushRecord) => {
    try {
      const detail = await reportApi.getPushRecord(record.id);
      setCurrentRecord(detail || record);
    } catch (e) {
      setCurrentRecord(record);
    }
    setRecordDetailVisible(true);
  };

  const renderEnabledTag = (enabled: number) => {
    const tag = enabledMap[enabled];
    if (!tag) return <Tag>-</Tag>;
    return <Tag color={tag.color}>{tag.text}</Tag>;
  };

  const renderPushStatusBadge = (status: number) => {
    const tag = pushStatusMap[status];
    if (!tag) return <Tag>-</Tag>;
    return (
      <Badge status={tag.color as any} text={<span style={{ color: tag.color === 'processing' ? '#1677ff' : undefined }}>{tag.text}</span>} />
    );
  };

  const columns: ColumnsType<ReportSubscription> = [
    {
      title: '订阅编号',
      dataIndex: 'subscriptionNo',
      key: 'subscriptionNo',
      width: 180,
      ellipsis: true,
    },
    {
      title: '商户号',
      dataIndex: 'merchantNo',
      key: 'merchantNo',
      width: 120,
      ellipsis: true,
    },
    {
      title: '报表类型',
      dataIndex: 'reportType',
      key: 'reportType',
      width: 80,
      render: (val) => <Tag color={val === 1 ? 'blue' : 'purple'}>{reportTypeMap[val]}</Tag>,
    },
    {
      title: '报表分类',
      dataIndex: 'reportCategoryDesc',
      key: 'reportCategoryDesc',
      width: 100,
    },
    {
      title: '推送渠道',
      dataIndex: 'pushChannel',
      key: 'pushChannel',
      width: 100,
      render: (val) => pushChannelMap[val] || '-',
    },
    {
      title: '接收邮箱',
      dataIndex: 'emailList',
      key: 'emailList',
      width: 200,
      ellipsis: true,
      render: (val) => (
        <Tooltip title={val}>
          <span>{val}</span>
        </Tooltip>
      ),
    },
    {
      title: '推送时间',
      dataIndex: 'pushTime',
      key: 'pushTime',
      width: 90,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (val) => renderEnabledTag(val),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (val) => formatDateTime(val),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
          <Button
            type="link"
            size="small"
            loading={togglingId === record.id}
            onClick={() => handleToggle(record)}
          >
            {record.enabled === 1 ? '禁用' : '启用'}
          </Button>
          <Button
            type="link"
            size="small"
            icon={<SendOutlined />}
            loading={pushingId === record.id}
            onClick={() => handleManualPush(record)}
          >
            推送
          </Button>
        </Space>
      ),
    },
  ];

  const recordColumns: ColumnsType<ReportPushRecord> = [
    {
      title: '记录编号',
      dataIndex: 'recordNo',
      key: 'recordNo',
      width: 180,
      ellipsis: true,
    },
    {
      title: '报表类型',
      dataIndex: 'reportType',
      key: 'reportType',
      width: 80,
      render: (val) => <Tag color={val === 1 ? 'blue' : 'purple'}>{reportTypeMap[val]}</Tag>,
    },
    {
      title: '报表标题',
      dataIndex: 'reportTitle',
      key: 'reportTitle',
      width: 160,
      ellipsis: true,
    },
    {
      title: '周期',
      dataIndex: 'reportPeriod',
      key: 'reportPeriod',
      width: 200,
      ellipsis: true,
    },
    {
      title: '推送状态',
      dataIndex: 'pushStatus',
      key: 'pushStatus',
      width: 90,
      render: (val) => renderPushStatusBadge(val),
    },
    {
      title: '触发方式',
      dataIndex: 'triggerType',
      key: 'triggerType',
      width: 90,
      render: (val) => (
        <Tag color={val === 1 ? 'cyan' : 'orange'}>
          {triggerTypeMap[val]}
        </Tag>
      ),
    },
    {
      title: '推送时间',
      dataIndex: 'pushTime',
      key: 'pushTime',
      width: 160,
      render: (val) => formatDateTime(val),
    },
    {
      title: '成功数',
      dataIndex: 'successCount',
      key: 'successCount',
      width: 70,
      render: (val) => <Text type="success">{val}</Text>,
    },
    {
      title: '失败数',
      dataIndex: 'failCount',
      key: 'failCount',
      width: 70,
      render: (val) => val > 0 ? <Text type="danger">{val}</Text> : val,
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      fixed: 'right',
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => handleViewRecordDetail(record)}
        >
          详情
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Row gutter={16}>
        <Col span={14}>
          <Card
            title="订阅查询"
            style={{ marginBottom: 16 }}
            extra={
              <Space>
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={handleCreate}
                >
                  新建订阅
                </Button>
                <Button
                  icon={<SendOutlined />}
                  disabled={selectedRowKeys.length === 0}
                  onClick={() => handleManualPush()}
                >
                  手动推送
                </Button>
              </Space>
            }
          >
            <Form form={queryForm} layout="inline" onFinish={handleQuery}>
              <Form.Item name="subscriptionNo" label="订阅编号">
                <Input placeholder="请输入订阅编号" allowClear style={{ width: 150 }} />
              </Form.Item>
              <Form.Item name="merchantNo" label="商户号">
                <Input placeholder="请输入商户号" allowClear style={{ width: 130 }} />
              </Form.Item>
              <Form.Item name="reportType" label="报表类型" initialValue="">
                <Select
                  options={[
                    { label: '全部', value: '' },
                    { label: '日报', value: 1 },
                    { label: '周报', value: 2 },
                  ]}
                  style={{ width: 90 }}
                />
              </Form.Item>
              <Form.Item name="reportCategory" label="报表分类">
                <Select
                  placeholder="请选择"
                  allowClear
                  options={reportCategoryOptions}
                  style={{ width: 120 }}
                />
              </Form.Item>
              <Form.Item name="pushChannel" label="推送渠道" initialValue="">
                <Select
                  options={[
                    { label: '全部', value: '' },
                    { label: '邮件', value: 1 },
                    { label: '邮件+短信', value: 2 },
                  ]}
                  style={{ width: 110 }}
                />
              </Form.Item>
              <Form.Item name="enabled" label="状态" initialValue="">
                <Select
                  options={[
                    { label: '全部', value: '' },
                    { label: '启用', value: 1 },
                    { label: '禁用', value: 0 },
                  ]}
                  style={{ width: 90 }}
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
            title={
              <Space>
                <span>订阅列表</span>
                {selectedSubscription && (
                  <Tag color="blue">已选中：{selectedSubscription.subscriptionNo}</Tag>
                )}
              </Space>
            }
          >
            <Table<ReportSubscription>
              columns={columns}
              dataSource={data}
              rowKey="id"
              loading={loading}
              scroll={{ x: 1400 }}
              rowSelection={{
                type: 'radio',
                selectedRowKeys,
                onChange: (keys, rows) => {
                  setSelectedRowKeys(keys);
                  setSelectedSubscription(rows.length > 0 ? rows[0] : null);
                },
              }}
              onRow={(record) => ({
                onClick: () => handleRowSelect(record),
                style: { cursor: 'pointer' },
              })}
              pagination={{
                ...pagination,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条`,
                onChange: (page, pageSize) => fetchData(page, pageSize),
              }}
            />
          </Card>
        </Col>

        <Col span={10}>
          <Card
            title={
              <Space>
                <span>推送记录</span>
                {selectedSubscription && (
                  <Tag color="geekblue">过滤：{selectedSubscription.subscriptionNo}</Tag>
                )}
              </Space>
            }
            extra={
              <Button
                type="link"
                size="small"
                onClick={() => {
                  setSelectedRowKeys([]);
                  setSelectedSubscription(null);
                }}
                disabled={!selectedSubscription}
              >
                清除过滤
              </Button>
            }
          >
            <Table<ReportPushRecord>
              columns={recordColumns}
              dataSource={recordData}
              rowKey="id"
              loading={recordLoading}
              scroll={{ x: 1100 }}
              size="small"
              pagination={{
                ...recordPagination,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条`,
                onChange: (page, pageSize) => fetchRecordData(page, pageSize),
              }}
            />
          </Card>
        </Col>
      </Row>

      <Modal
        title={modalTitle}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={680}
        okText="保存"
      >
        <Form form={saveForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="merchantNo"
                label="商户号"
              >
                <Input placeholder="请输入商户号，留空则为全局" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="reportCategory"
                label="报表分类"
                rules={[{ required: true, message: '请选择报表分类' }]}
              >
                <Select options={reportCategoryOptions} placeholder="请选择报表分类" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="reportType"
                label="报表类型"
                rules={[{ required: true, message: '请选择报表类型' }]}
              >
                <Radio.Group>
                  <Radio value={1}>日报</Radio>
                  <Radio value={2}>周报</Radio>
                </Radio.Group>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="pushChannel"
                label="推送渠道"
                rules={[{ required: true, message: '请选择推送渠道' }]}
              >
                <Select
                  options={[
                    { label: '邮件', value: 1 },
                    { label: '邮件+短信', value: 2 },
                  ]}
                  placeholder="请选择推送渠道"
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={14}>
              <Form.Item
                name="pushTime"
                label="推送时间"
                rules={[{ required: true, message: '请选择推送时间' }]}
              >
                <TimePicker format="HH:mm" placeholder="请选择推送时间" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={10}>
              <Form.Item
                name="enabled"
                label="是否启用"
                valuePropName="checked"
                getValueFromEvent={(checked) => (checked ? 1 : 0)}
                getValueProps={(value) => ({ checked: value === 1 || value === true })}
              >
                <Switch checkedChildren="启用" unCheckedChildren="禁用" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="emailList"
            label="接收邮箱"
            rules={[{ required: true, message: '请输入接收邮箱' }]}
            extra="多个邮箱请用英文逗号分隔"
          >
            <TextArea
              rows={2}
              placeholder="例如：user1@example.com,user2@example.com"
            />
          </Form.Item>
          <Form.Item
            name="phoneList"
            label="接收手机号"
            extra="多个手机号请用英文逗号分隔（仅当推送渠道包含短信时生效）"
          >
            <TextArea
              rows={2}
              placeholder="例如：13800138000,13900139000"
            />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <TextArea rows={3} placeholder="请输入备注信息（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="推送记录详情"
        open={recordDetailVisible}
        onCancel={() => setRecordDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setRecordDetailVisible(false)}>
            关闭
          </Button>,
          currentRecord?.fileUrl && (
            <Button
              key="download"
              type="primary"
              icon={<EyeOutlined />}
              onClick={() => window.open(currentRecord.fileUrl, '_blank')}
            >
              下载报表文件
            </Button>
          ),
        ]}
        destroyOnClose
        width={720}
      >
        {currentRecord && (
          <div style={{ fontSize: 14 }}>
            <Row gutter={[16, 12]}>
              <Col span={12}>
                <Text type="secondary">记录编号：</Text>
                <Text copyable>{currentRecord.recordNo}</Text>
              </Col>
              <Col span={12}>
                <Text type="secondary">订阅编号：</Text>
                <Text>{currentRecord.subscriptionNo || '-'}</Text>
              </Col>
              <Col span={12}>
                <Text type="secondary">商户号：</Text>
                <Text>{currentRecord.merchantNo}</Text>
              </Col>
              <Col span={12}>
                <Text type="secondary">报表类型：</Text>
                <Tag color={currentRecord.reportType === 1 ? 'blue' : 'purple'}>
                  {reportTypeMap[currentRecord.reportType]}
                </Tag>
              </Col>
              <Col span={12}>
                <Text type="secondary">报表标题：</Text>
                <Text>{currentRecord.reportTitle}</Text>
              </Col>
              <Col span={12}>
                <Text type="secondary">报表分类：</Text>
                <Text>{getReportCategoryDesc(currentRecord.reportCategory)}</Text>
              </Col>
              <Col span={24}>
                <Text type="secondary">报表周期：</Text>
                <Text>{currentRecord.reportPeriod}</Text>
              </Col>
              <Col span={12}>
                <Text type="secondary">推送状态：</Text>
                {renderPushStatusBadge(currentRecord.pushStatus)}
              </Col>
              <Col span={12}>
                <Text type="secondary">触发方式：</Text>
                <Tag color={currentRecord.triggerType === 1 ? 'cyan' : 'orange'}>
                  {triggerTypeMap[currentRecord.triggerType]}
                </Tag>
              </Col>
              <Col span={12}>
                <Text type="secondary">推送渠道：</Text>
                <Text>{pushChannelMap[currentRecord.pushChannel] || '-'}</Text>
              </Col>
              <Col span={12}>
                <Text type="secondary">推送时间：</Text>
                <Text>{formatDateTime(currentRecord.pushTime)}</Text>
              </Col>
              <Col span={12}>
                <Text type="success">成功数：{currentRecord.successCount}</Text>
              </Col>
              <Col span={12}>
                <Text type={currentRecord.failCount > 0 ? 'danger' : undefined}>
                  失败数：{currentRecord.failCount}
                </Text>
              </Col>
              <Col span={24}>
                <Text type="secondary">接收邮箱：</Text>
                <Text>{currentRecord.emailTargets || '-'}</Text>
              </Col>
              {currentRecord.phoneTargets && (
                <Col span={24}>
                  <Text type="secondary">接收手机号：</Text>
                  <Text>{currentRecord.phoneTargets}</Text>
                </Col>
              )}
              {currentRecord.fileUrl && (
                <Col span={24}>
                  <Text type="secondary">报表文件：</Text>
                  <a onClick={() => window.open(currentRecord.fileUrl, '_blank')}>
                    点击下载
                    {currentRecord.fileSize && (
                      <Text type="secondary"> ({(currentRecord.fileSize / 1024).toFixed(2)} KB)</Text>
                    )}
                  </a>
                </Col>
              )}
              {currentRecord.failReason && (
                <Col span={24}>
                  <Text type="danger">失败原因：{currentRecord.failReason}</Text>
                </Col>
              )}
              <Col span={24}>
                <Text type="secondary">创建时间：</Text>
                <Text>{formatDateTime(currentRecord.createdAt)}</Text>
              </Col>
            </Row>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default ReportSubscription;

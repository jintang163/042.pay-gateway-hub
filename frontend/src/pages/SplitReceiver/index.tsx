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
  message,
  Modal,
  Row,
  Col,
  Tooltip,
  Popconfirm,
  Descriptions,
  Drawer,
  Divider,
  Alert,
  Upload,
  Switch,
} from 'antd';
import type { UploadProps } from 'antd';
import {
  ReloadOutlined,
  SearchOutlined,
  PlusOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  SafetyCertificateOutlined,
  UploadOutlined,
  FileTextOutlined,
  SwapOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { splitReceiverApi } from '@/api';
import type {
  SplitReceiver,
  SplitReceiverSaveRequest,
  SplitReceiverVerifyRequest,
  SplitReceiverBatchImportItem,
  SplitReceiverVerifyLog,
} from '@/types/splitReceiver';
import { formatDateTime } from '@/utils';

const maskIdCard = (idCard: string): string => {
  if (!idCard) return '-';
  if (idCard.length <= 10) return idCard;
  return idCard.slice(0, 6) + '********' + idCard.slice(-4);
};

const maskBankCard = (bankCard: string): string => {
  if (!bankCard) return '-';
  if (bankCard.length <= 8) return bankCard;
  return bankCard.slice(0, 4) + ' **** **** ' + bankCard.slice(-4);
};

const renderVerifyStatus = (status: number) => {
  const statusMap: Record<number, { color: string; text: string }> = {
    0: { color: 'default', text: '未认证' },
    1: { color: 'blue', text: '认证中' },
    2: { color: 'green', text: '已认证' },
    3: { color: 'red', text: '认证失败' },
  };
  const info = statusMap[status] || { color: 'default', text: '未知' };
  return <Tag color={info.color}>{info.text}</Tag>;
};

const renderReceiverType = (type: number, desc?: string) => {
  const colorMap: Record<number, string> = {
    1: 'blue',
    2: 'purple',
    3: 'cyan',
  };
  return <Tag color={colorMap[type] || 'default'}>{desc || '未知'}</Tag>;
};

const renderStatus = (status: number, desc?: string) => {
  const colorMap: Record<number, string> = {
    0: 'red',
    1: 'green',
  };
  return <Tag color={colorMap[status] || 'default'}>{desc || (status === 1 ? '启用' : '禁用')}</Tag>;
};

const verifyLogStatus = (status: number) => {
  const statusMap: Record<number, { color: string; text: string }> = {
    0: { color: 'default', text: '未认证' },
    1: { color: 'blue', text: '认证中' },
    2: { color: 'green', text: '认证成功' },
    3: { color: 'red', text: '认证失败' },
  };
  const info = statusMap[status] || { color: 'default', text: '未知' };
  return <Tag color={info.color}>{info.text}</Tag>;
};

const SAMPLE_JSON_TEMPLATE = `[
  {
    "receiverName": "张三",
    "receiverType": 1,
    "idCardNo": "110101199001011234",
    "idCardName": "张三",
    "bankCardNo": "6222021234567890123",
    "bankPhone": "13800138000",
    "bankName": "中国工商银行",
    "bankBranchName": "北京朝阳支行",
    "contactName": "张三",
    "contactPhone": "13800138000",
    "contactEmail": "zhangsan@example.com",
    "remark": "测试导入"
  }
]`;

const SplitReceiverPage = () => {
  const [queryForm] = Form.useForm();
  const [saveForm] = Form.useForm();
  const [verifyForm] = Form.useForm();
  const [logQueryForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<SplitReceiver[]>([]);
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });

  const [saveModalVisible, setSaveModalVisible] = useState(false);
  const [verifyModalVisible, setVerifyModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [importModalVisible, setImportModalVisible] = useState(false);
  const [logDrawerVisible, setLogDrawerVisible] = useState(false);

  const [currentRecord, setCurrentRecord] = useState<SplitReceiver | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [verifySubmitting, setVerifySubmitting] = useState(false);
  const [importSubmitting, setImportSubmitting] = useState(false);
  const [importJsonText, setImportJsonText] = useState('');
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [autoVerify, setAutoVerify] = useState(false);
  const [batchVerifySubmitting, setBatchVerifySubmitting] = useState(false);
  const [importResult, setImportResult] = useState<{ successCount: number; failCount: number; failDetails: any[] } | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const [logLoading, setLogLoading] = useState(false);
  const [logData, setLogData] = useState<SplitReceiverVerifyLog[]>([]);
  const [logPagination, setLogPagination] = useState({ current: 1, size: 10, total: 0 });

  const [isEditMode, setIsEditMode] = useState(false);

  const fetchData = async (page = pagination.current, size = pagination.size) => {
    try {
      setLoading(true);
      const values = queryForm.getFieldsValue();
      const params: any = {
        current: page,
        size,
        ...values,
      };
      const result = await splitReceiverApi.list(params);
      setData(result.records);
      setPagination({ current: result.current, size: result.size, total: result.total });
    } catch (e: any) {
      message.error(e?.message || '加载接收方列表失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchLogs = async (page = logPagination.current, size = logPagination.size) => {
    try {
      setLogLoading(true);
      const values = logQueryForm.getFieldsValue();
      const params: any = {
        current: page,
        size,
        receiverNo: currentRecord?.receiverNo,
        ...values,
      };
      const result = await splitReceiverApi.verifyLogs(params);
      setLogData(result.records);
      setLogPagination({ current: result.current, size: result.size, total: result.total });
    } catch (e: any) {
      message.error(e?.message || '加载认证记录失败');
      setLogData([]);
    } finally {
      setLogLoading(false);
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

  const handleAdd = () => {
    setIsEditMode(false);
    setCurrentRecord(null);
    saveForm.resetFields();
    saveForm.setFieldsValue({ status: 1, receiverType: 1 });
    setSaveModalVisible(true);
  };

  const handleEdit = (record: SplitReceiver) => {
    setIsEditMode(true);
    setCurrentRecord(record);
    saveForm.resetFields();
    saveForm.setFieldsValue({
      receiverNo: record.receiverNo,
      receiverName: record.receiverName,
      receiverType: record.receiverType,
      idCardNo: record.idCardNo,
      idCardName: record.idCardName,
      bankCardNo: record.bankCardNo,
      bankPhone: record.bankPhone,
      bankName: record.bankName,
      bankBranchName: record.bankBranchName,
      contactName: record.contactName,
      contactPhone: record.contactPhone,
      contactEmail: record.contactEmail,
      remark: record.remark,
      status: record.status,
    });
    setSaveModalVisible(true);
  };

  const handleSaveSubmit = async () => {
    try {
      const values = await saveForm.validateFields();
      setSubmitting(true);
      const payload: SplitReceiverSaveRequest = {
        ...values,
      };
      await splitReceiverApi.save(payload);
      message.success(isEditMode ? '编辑成功' : '新增成功');
      setSaveModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error?.message || '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleViewDetail = (record: SplitReceiver) => {
    setCurrentRecord(record);
    setDetailModalVisible(true);
  };

  const handleToggle = async (record: SplitReceiver) => {
    try {
      await splitReceiverApi.toggle(record.id);
      message.success(record.status === 1 ? '已禁用' : '已启用');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '操作失败');
    }
  };

  const handleDelete = async (record: SplitReceiver) => {
    try {
      await splitReceiverApi.remove(record.id);
      message.success('删除成功');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '删除失败');
    }
  };

  const handleVerify = (record: SplitReceiver) => {
    setCurrentRecord(record);
    verifyForm.resetFields();
    verifyForm.setFieldsValue({ verifyChannel: 'BANK_FOUR' });
    setVerifyModalVisible(true);
  };

  const handleVerifySubmit = async () => {
    try {
      const values = await verifyForm.validateFields();
      setVerifySubmitting(true);
      const payload: SplitReceiverVerifyRequest = {
        receiverNo: currentRecord!.receiverNo,
        verifyChannel: values.verifyChannel,
      };
      await splitReceiverApi.verify(payload);
      message.success('实名认证请求已提交');
      setVerifyModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error?.message || '提交失败');
    } finally {
      setVerifySubmitting(false);
    }
  };

  const handleViewLogs = (record?: SplitReceiver) => {
    if (record) {
      setCurrentRecord(record);
    }
    logQueryForm.resetFields();
    setLogDrawerVisible(true);
    setTimeout(() => {
      fetchLogs(1, logPagination.size);
    }, 100);
  };

  const handleImport = () => {
    setImportJsonText('');
    setAutoVerify(false);
    setImportResult(null);
    setSelectedFile(null);
    setImportModalVisible(true);
  };

  const handleDownloadTemplate = () => {
    const headers = ['接收方名称', '接收方类型(1个人2企业3个体)', '证件姓名', '身份证号', '银行卡号', '银行预留手机号', '开户银行', '开户支行', '联系人姓名', '联系人电话', '联系人邮箱', '备注'];
    const sampleRow = ['张三', '1', '张三', '110101199001011234', '6222021234567890123', '13800138000', '中国工商银行', '北京朝阳支行', '张三', '13800138000', 'zhangsan@example.com', '测试导入'];
    const csvContent = [headers.join(','), sampleRow.join(',')].join('\n');
    const BOM = '\uFEFF';
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', '分账接收方导入模板.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    message.success('模板下载成功');
  };

  const handleBatchVerify = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要认证的接收方');
      return;
    }
    Modal.confirm({
      title: '确认批量认证',
      content: `确定对选中的 ${selectedRowKeys.length} 个接收方发起实名认证？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          setBatchVerifySubmitting(true);
          const selectedReceivers = data.filter((item) => selectedRowKeys.includes(item.id as React.Key));
          const receiverNos = selectedReceivers.map((item) => item.receiverNo);
          const result = await splitReceiverApi.batchVerify(receiverNos);
          if (result) {
            const success = result.successCount || 0;
            const fail = result.failCount || 0;
            if (fail > 0 && result.failDetails?.length > 0) {
              Modal.error({
                title: '批量认证完成',
                content: (
                  <div>
                    <p>成功：{success} 条</p>
                    <p>失败：{fail} 条</p>
                    <Divider>失败详情</Divider>
                    <div style={{ maxHeight: 300, overflow: 'auto' }}>
                      {result.failDetails.map((item: any, idx: number) => (
                        <div key={idx} style={{ marginBottom: 8 }}>
                          <span style={{ color: '#ff4d4f' }}>
                            [{item.receiverNo || item.receiverName}] {item.failReason || '未知错误'}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                ),
              });
            } else {
              message.success(`批量认证成功，共 ${success} 条`);
            }
          } else {
            message.success('批量认证请求已提交');
          }
          setSelectedRowKeys([]);
          fetchData();
        } catch (e: any) {
          message.error(e?.message || '批量认证失败');
        } finally {
          setBatchVerifySubmitting(false);
        }
      },
    });
  };

  const handleImportSubmit = async () => {
    try {
      if (!selectedFile) {
        message.error('请选择要导入的文件');
        return;
      }
      setImportSubmitting(true);
      const result = await splitReceiverApi.batchImportFile(selectedFile, autoVerify);
      if (result) {
        const success = result.successCount || 0;
        const fail = result.failCount || 0;
        setImportResult(result);
        if (fail > 0 && result.failDetails?.length > 0) {
          message.warning(`导入完成：成功 ${success} 条，失败 ${fail} 条`);
        } else {
          message.success(`导入成功，共 ${success} 条记录`);
          setTimeout(() => {
            setImportModalVisible(false);
          }, 1500);
        }
      } else {
        message.success('导入成功');
        setTimeout(() => {
          setImportModalVisible(false);
        }, 1500);
      }
      fetchData();
    } catch (error: any) {
      message.error(error?.message || '导入失败');
    } finally {
      setImportSubmitting(false);
    }
  };

  const columns: ColumnsType<SplitReceiver> = [
    {
      title: '接收方编号',
      dataIndex: 'receiverNo',
      key: 'receiverNo',
      width: 140,
      fixed: 'left',
    },
    {
      title: '接收方名称',
      dataIndex: 'receiverName',
      key: 'receiverName',
      width: 120,
    },
    {
      title: '类型',
      dataIndex: 'receiverType',
      key: 'receiverType',
      width: 80,
      render: (val: number, record) => renderReceiverType(val, record.receiverTypeDesc),
    },
    {
      title: '证件姓名',
      dataIndex: 'idCardName',
      key: 'idCardName',
      width: 100,
    },
    {
      title: '证件号',
      dataIndex: 'idCardNo',
      key: 'idCardNo',
      width: 180,
      render: (val: string) => maskIdCard(val),
    },
    {
      title: '银行卡号',
      dataIndex: 'bankCardNo',
      key: 'bankCardNo',
      width: 200,
      render: (val: string) => maskBankCard(val),
    },
    {
      title: '预留手机号',
      dataIndex: 'bankPhone',
      key: 'bankPhone',
      width: 120,
    },
    {
      title: '开户银行',
      dataIndex: 'bankName',
      key: 'bankName',
      width: 120,
    },
    {
      title: '认证状态',
      dataIndex: 'verifyStatus',
      key: 'verifyStatus',
      width: 100,
      render: (val: number) => renderVerifyStatus(val),
    },
    {
      title: '认证渠道',
      dataIndex: 'verifyChannelDesc',
      key: 'verifyChannelDesc',
      width: 120,
      render: (val: string) => val || '-',
    },
    {
      title: '认证时间',
      dataIndex: 'verifyTime',
      key: 'verifyTime',
      width: 170,
      render: (val: string) => formatDateTime(val),
    },
    {
      title: '启用状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (val: number, record) => renderStatus(val, record.statusDesc),
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small" wrap>
          <Tooltip title="查看详情">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
            >
              详情
            </Button>
          </Tooltip>
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
          <Tooltip title={record.status === 1 ? '禁用' : '启用'}>
            <Button
              type="link"
              size="small"
              icon={<SwapOutlined />}
              onClick={() => handleToggle(record)}
            >
              {record.status === 1 ? '禁用' : '启用'}
            </Button>
          </Tooltip>
          <Tooltip title="实名认证">
            <Button
              type="link"
              size="small"
              icon={<SafetyCertificateOutlined />}
              onClick={() => handleVerify(record)}
            >
              认证
            </Button>
          </Tooltip>
          <Popconfirm
            title="确认删除该接收方？"
            description="删除后不可恢复"
            onConfirm={() => handleDelete(record)}
            okText="确认"
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

  const logColumns: ColumnsType<SplitReceiverVerifyLog> = [
    {
      title: '日志编号',
      dataIndex: 'logNo',
      key: 'logNo',
      width: 160,
    },
    {
      title: '接收方编号',
      dataIndex: 'receiverNo',
      key: 'receiverNo',
      width: 140,
    },
    {
      title: '认证渠道',
      dataIndex: 'verifyChannelDesc',
      key: 'verifyChannelDesc',
      width: 120,
      render: (val: string) => val || '-',
    },
    {
      title: '请求ID',
      dataIndex: 'verifyRequestId',
      key: 'verifyRequestId',
      width: 180,
      render: (val: string) => val || '-',
    },
    {
      title: '证件姓名',
      dataIndex: 'idCardName',
      key: 'idCardName',
      width: 100,
    },
    {
      title: '证件号',
      dataIndex: 'idCardNo',
      key: 'idCardNo',
      width: 160,
      render: (val: string) => maskIdCard(val),
    },
    {
      title: '银行卡号',
      dataIndex: 'bankCardNo',
      key: 'bankCardNo',
      width: 180,
      render: (val: string) => maskBankCard(val),
    },
    {
      title: '认证状态',
      dataIndex: 'verifyStatus',
      key: 'verifyStatus',
      width: 100,
      render: (val: number) => verifyLogStatus(val),
    },
    {
      title: '失败原因',
      dataIndex: 'verifyFailReason',
      key: 'verifyFailReason',
      width: 160,
      render: (val: string) => val || '-',
    },
    {
      title: '认证时间',
      dataIndex: 'verifyTime',
      key: 'verifyTime',
      width: 170,
      render: (val: string) => formatDateTime(val),
    },
    {
      title: '操作人',
      dataIndex: 'operatorName',
      key: 'operatorName',
      width: 100,
      render: (val: string) => val || '-',
    },
  ];

  return (
    <div>
      <Card
        title="分账接收方管理"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>
              刷新
            </Button>
            <Button icon={<FileTextOutlined />} onClick={() => handleViewLogs()}>
              认证记录
            </Button>
            <Button
              icon={<SafetyCertificateOutlined />}
              onClick={handleBatchVerify}
              disabled={selectedRowKeys.length === 0}
              loading={batchVerifySubmitting}
            >
              批量认证{selectedRowKeys.length > 0 ? `(${selectedRowKeys.length})` : ''}
            </Button>
            <Button icon={<UploadOutlined />} onClick={handleImport}>
              批量导入
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增接收方
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
          <Row gutter={[16, 16]} style={{ width: '100%' }}>
            <Col>
              <Form.Item name="receiverNo" label="接收方编号">
                <Input placeholder="请输入编号" allowClear style={{ width: 160 }} />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="receiverName" label="姓名">
                <Input placeholder="请输入姓名" allowClear style={{ width: 140 }} />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="receiverType" label="类型">
                <Select
                  placeholder="全部类型"
                  allowClear
                  style={{ width: 120 }}
                  options={[
                    { label: '个人', value: 1 },
                    { label: '企业', value: 2 },
                    { label: '个体工商户', value: 3 },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="verifyStatus" label="认证状态">
                <Select
                  placeholder="全部状态"
                  allowClear
                  style={{ width: 120 }}
                  options={[
                    { label: '未认证', value: 0 },
                    { label: '认证中', value: 1 },
                    { label: '已认证', value: 2 },
                    { label: '认证失败', value: 3 },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="status" label="启用状态">
                <Select
                  placeholder="全部状态"
                  allowClear
                  style={{ width: 120 }}
                  options={[
                    { label: '启用', value: 1 },
                    { label: '禁用', value: 0 },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="idCardNo" label="身份证号">
                <Input placeholder="请输入身份证号" allowClear style={{ width: 180 }} />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="bankCardNo" label="银行卡号">
                <Input placeholder="请输入银行卡号" allowClear style={{ width: 180 }} />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item>
                <Space>
                  <Button type="primary" icon={<SearchOutlined />} htmlType="submit">
                    查询
                  </Button>
                  <Button onClick={handleReset}>重置</Button>
                </Space>
              </Form.Item>
            </Col>
          </Row>
        </Form>

        <Table<SplitReceiver>
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          pagination={{
            current: pagination.current,
            pageSize: pagination.size,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchData(page, pageSize),
          }}
          scroll={{ x: 1900 }}
        />
      </Card>

      <Modal
        title={isEditMode ? '编辑接收方' : '新增接收方'}
        open={saveModalVisible}
        onCancel={() => setSaveModalVisible(false)}
        onOk={handleSaveSubmit}
        confirmLoading={submitting}
        width={720}
        destroyOnClose
        maskClosable={false}
      >
        <Form form={saveForm} layout="vertical" name="split_receiver_save_form">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="receiverName"
                label="接收方名称"
                rules={[{ required: true, message: '请输入接收方名称' }]}
              >
                <Input placeholder="请输入接收方名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="receiverType"
                label="接收方类型"
                rules={[{ required: true, message: '请选择接收方类型' }]}
              >
                <Select
                  placeholder="请选择类型"
                  options={[
                    { label: '个人', value: 1 },
                    { label: '企业', value: 2 },
                    { label: '个体工商户', value: 3 },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="idCardName"
                label="证件姓名"
                rules={[{ required: true, message: '请输入证件姓名' }]}
              >
                <Input placeholder="请输入证件姓名" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="idCardNo"
                label="身份证号"
                rules={[
                  { required: true, message: '请输入身份证号' },
                  {
                    pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
                    message: '请输入正确的身份证号',
                  },
                ]}
              >
                <Input placeholder="请输入身份证号" maxLength={18} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="bankName"
                label="开户银行"
                rules={[{ required: true, message: '请输入开户银行' }]}
              >
                <Input placeholder="如：中国工商银行" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="bankBranchName"
                label="开户支行"
              >
                <Input placeholder="如：北京朝阳支行（可选）" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="bankCardNo"
                label="银行卡号"
                rules={[
                  { required: true, message: '请输入银行卡号' },
                  { min: 12, max: 25, message: '银行卡号长度不正确' },
                ]}
              >
                <Input placeholder="请输入银行卡号" maxLength={25} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="bankPhone"
                label="银行预留手机号"
                rules={[
                  { required: true, message: '请输入预留手机号' },
                  { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' },
                ]}
              >
                <Input placeholder="请输入预留手机号" maxLength={11} />
              </Form.Item>
            </Col>
            <Divider style={{ margin: '8px 0' }} />
            <Col span={12}>
              <Form.Item name="contactName" label="联系人姓名">
                <Input placeholder="请输入联系人姓名（可选）" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="contactPhone"
                label="联系人电话"
                rules={[{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }]}
              >
                <Input placeholder="请输入联系人电话（可选）" maxLength={11} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="contactEmail"
                label="联系人邮箱"
                rules={[{ type: 'email', message: '请输入正确的邮箱' }]}
              >
                <Input placeholder="请输入联系人邮箱（可选）" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="status"
                label="状态"
                rules={[{ required: true, message: '请选择状态' }]}
              >
                <Select
                  options={[
                    { label: '启用', value: 1 },
                    { label: '禁用', value: 0 },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="备注">
                <Input.TextArea rows={2} placeholder="请输入备注（可选）" maxLength={200} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="接收方详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
        ]}
        width={720}
        destroyOnClose
      >
        {currentRecord && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="接收方编号" span={2}>
              {currentRecord.receiverNo}
            </Descriptions.Item>
            <Descriptions.Item label="接收方名称">
              {currentRecord.receiverName}
            </Descriptions.Item>
            <Descriptions.Item label="类型">
              {renderReceiverType(currentRecord.receiverType, currentRecord.receiverTypeDesc)}
            </Descriptions.Item>
            <Descriptions.Item label="证件姓名">
              {currentRecord.idCardName}
            </Descriptions.Item>
            <Descriptions.Item label="身份证号">
              {maskIdCard(currentRecord.idCardNo)}
            </Descriptions.Item>
            <Descriptions.Item label="开户银行">
              {currentRecord.bankName}
            </Descriptions.Item>
            <Descriptions.Item label="开户支行">
              {currentRecord.bankBranchName || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="银行卡号">
              {maskBankCard(currentRecord.bankCardNo)}
            </Descriptions.Item>
            <Descriptions.Item label="预留手机号">
              {currentRecord.bankPhone}
            </Descriptions.Item>
            <Descriptions.Item label="认证状态">
              {renderVerifyStatus(currentRecord.verifyStatus)}
            </Descriptions.Item>
            <Descriptions.Item label="认证渠道">
              {currentRecord.verifyChannelDesc || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="认证时间">
              {formatDateTime(currentRecord.verifyTime)}
            </Descriptions.Item>
            <Descriptions.Item label="认证请求ID">
              {currentRecord.verifyRequestId || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="失败原因">
              {currentRecord.verifyFailReason || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="联系人姓名">
              {currentRecord.contactName || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="联系人电话">
              {currentRecord.contactPhone || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="联系人邮箱">
              {currentRecord.contactEmail || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="启用状态">
              {renderStatus(currentRecord.status, currentRecord.statusDesc)}
            </Descriptions.Item>
            <Descriptions.Item label="操作人">
              {currentRecord.operatorName || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {formatDateTime(currentRecord.createdAt)}
            </Descriptions.Item>
            <Descriptions.Item label="更新时间">
              {formatDateTime(currentRecord.updatedAt)}
            </Descriptions.Item>
            <Descriptions.Item label="备注" span={2}>
              {currentRecord.remark || '-'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      <Modal
        title="发起实名认证"
        open={verifyModalVisible}
        onCancel={() => setVerifyModalVisible(false)}
        onOk={handleVerifySubmit}
        confirmLoading={verifySubmitting}
        width={520}
        destroyOnClose
      >
        <Form form={verifyForm} layout="vertical" name="split_receiver_verify_form">
          {currentRecord && (
            <>
              <Alert
                type="info"
                showIcon
                message="即将对以下接收方发起实名认证"
                style={{ marginBottom: 16 }}
              />
              <Descriptions column={1} size="small" bordered style={{ marginBottom: 16 }}>
                <Descriptions.Item label="接收方编号">
                  {currentRecord.receiverNo}
                </Descriptions.Item>
                <Descriptions.Item label="接收方名称">
                  {currentRecord.receiverName}
                </Descriptions.Item>
                <Descriptions.Item label="证件姓名">
                  {currentRecord.idCardName}
                </Descriptions.Item>
                <Descriptions.Item label="身份证号">
                  {maskIdCard(currentRecord.idCardNo)}
                </Descriptions.Item>
                <Descriptions.Item label="银行卡号">
                  {maskBankCard(currentRecord.bankCardNo)}
                </Descriptions.Item>
                <Descriptions.Item label="预留手机号">
                  {currentRecord.bankPhone}
                </Descriptions.Item>
              </Descriptions>
            </>
          )}
          <Form.Item
            name="verifyChannel"
            label="认证渠道"
            rules={[{ required: true, message: '请选择认证渠道' }]}
          >
            <Select
              placeholder="请选择认证渠道"
              options={[
                { label: '银行卡四要素', value: 'BANK_FOUR' },
                { label: '身份证二要素', value: 'ID_TWO' },
                { label: '银行卡三要素', value: 'BANK_THREE' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="批量导入接收方"
        open={importModalVisible}
        onCancel={() => setImportModalVisible(false)}
        onOk={handleImportSubmit}
        confirmLoading={importSubmitting}
        width={640}
        destroyOnClose
        okText="导入"
      >
        <Alert
          type="info"
          showIcon
          message={
            <Space direction="vertical">
              <span>请上传 .xlsx/.xls/.csv 格式的导入文件</span>
              <Button
                type="link"
                size="small"
                icon={<DownloadOutlined />}
                style={{ padding: 0 }}
                onClick={handleDownloadTemplate}
              >
                下载导入模板
              </Button>
            </Space>
          }
          style={{ marginBottom: 16 }}
        />

        <div style={{ marginBottom: 16 }}>
          <Upload
            accept=".xlsx,.xls,.csv"
            maxCount={1}
            customRequest={({ file, onSuccess, onError }) => {
              setSelectedFile(file as File);
              setImportResult(null);
              onSuccess?.(file);
            }}
            onRemove={() => {
              setSelectedFile(null);
              setImportResult(null);
            }}
            fileList={selectedFile ? [{
              uid: '-1',
              name: selectedFile.name,
              status: 'done',
              size: selectedFile.size,
            }] : []}
          >
            <Button icon={<UploadOutlined />}>
              {selectedFile ? '重新选择文件' : '选择文件'}
            </Button>
          </Upload>
        </div>

        <div style={{ marginBottom: 16 }}>
          <Space>
            <span>导入后自动发起实名认证：</span>
            <Switch
              checked={autoVerify}
              onChange={setAutoVerify}
            />
          </Space>
        </div>

        {importResult && (
          <Alert
            type={importResult.failCount > 0 ? 'warning' : 'success'}
            showIcon
            message="导入结果"
            description={
              <div>
                <p>成功：{importResult.successCount || 0} 条</p>
                <p>失败：{importResult.failCount || 0} 条</p>
                {importResult.failDetails && importResult.failDetails.length > 0 && (
                  <>
                    <Divider style={{ margin: '8px 0' }}>失败详情</Divider>
                    <div style={{ maxHeight: 200, overflow: 'auto' }}>
                      {importResult.failDetails.map((item: any, idx: number) => (
                        <div key={idx} style={{ marginBottom: 6 }}>
                          <span style={{ color: '#ff4d4f' }}>
                            第{item.rowNum ? `${item.rowNum}行 - ` : ''}
                            [{item.receiverName || item.receiverNo || '未知'}] {item.failReason || '未知错误'}
                          </span>
                        </div>
                      ))}
                    </div>
                  </>
                )}
              </div>
            }
            style={{ marginTop: 16 }}
          />
        )}
      </Modal>

      <Drawer
        title="实名认证记录"
        placement="right"
        width={1400}
        onClose={() => setLogDrawerVisible(false)}
        open={logDrawerVisible}
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => fetchLogs()}>
              刷新
            </Button>
          </Space>
        }
      >
        <Form
          form={logQueryForm}
          layout="inline"
          style={{ marginBottom: 16 }}
          onFinish={() => fetchLogs(1, logPagination.size)}
        >
          <Form.Item name="receiverNo" label="接收方编号">
            <Input placeholder="请输入编号" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="verifyChannel" label="认证渠道">
            <Select
              placeholder="全部渠道"
              allowClear
              style={{ width: 140 }}
              options={[
                { label: '银行卡四要素', value: 'BANK_FOUR' },
                { label: '身份证二要素', value: 'ID_TWO' },
                { label: '银行卡三要素', value: 'BANK_THREE' },
              ]}
            />
          </Form.Item>
          <Form.Item name="verifyStatus" label="认证状态">
            <Select
              placeholder="全部状态"
              allowClear
              style={{ width: 120 }}
              options={[
                { label: '未认证', value: 0 },
                { label: '认证中', value: 1 },
                { label: '认证成功', value: 2 },
                { label: '认证失败', value: 3 },
              ]}
            />
          </Form.Item>
          <Form.Item name="verifyRequestId" label="请求ID">
            <Input placeholder="请输入请求ID" allowClear style={{ width: 180 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} htmlType="submit">
                查询
              </Button>
              <Button
                onClick={() => {
                  logQueryForm.resetFields();
                  fetchLogs(1, logPagination.size);
                }}
              >
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>

        <Table<SplitReceiverVerifyLog>
          columns={logColumns}
          dataSource={logData}
          rowKey="id"
          loading={logLoading}
          pagination={{
            current: logPagination.current,
            pageSize: logPagination.size,
            total: logPagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchLogs(page, pageSize),
          }}
          scroll={{ x: 1400 }}
        />
      </Drawer>
    </div>
  );
};

export default SplitReceiverPage;

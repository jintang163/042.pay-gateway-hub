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
  Row,
  Col,
  Statistic,
  Tooltip,
  Popconfirm,
  Descriptions,
} from 'antd';
import {
  ReloadOutlined,
  SearchOutlined,
  WalletOutlined,
  PlusOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  EyeOutlined,
  SendOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { agentWithdrawApi } from '@/api';
import type {
  AgentWithdraw,
  AgentWithdrawApplyRequest,
  AgentWithdrawAuditRequest,
} from '@/types/agent';
import { useUserStore } from '@/store';
import { formatDateTime, formatAmount } from '@/utils';

const { RangePicker } = DatePicker;

const AgentWithdrawPage = () => {
  const { user } = useUserStore();
  const isAdmin = user?.role === 'admin' || user?.role === 'operator';

  const [queryForm] = Form.useForm();
  const [applyForm] = Form.useForm();
  const [auditForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AgentWithdraw[]>([]);
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });

  const [applyModalVisible, setApplyModalVisible] = useState(false);
  const [auditModalVisible, setAuditModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<AgentWithdraw | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [auditSubmitting, setAuditSubmitting] = useState(false);

  const [balance, setBalance] = useState(0);

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
        params.merchantNo = currentMerchantNo;
      }
      const result = await agentWithdrawApi.list(params);
      setData(result.records);
      setPagination({ current: result.current, size: result.size, total: result.total });
    } catch (e: any) {
      message.error(e?.message || '加载提现记录失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchBalance = async () => {
    if (!currentMerchantNo) return;
    try {
      const result = await agentWithdrawApi.getBalance(currentMerchantNo);
      setBalance(result as any);
    } catch {
    }
  };

  useEffect(() => {
    fetchData();
    if (!isAdmin) {
      fetchBalance();
    }
  }, []);

  const handleSearch = () => {
    fetchData(1, pagination.size);
  };

  const handleReset = () => {
    queryForm.resetFields();
    fetchData(1, pagination.size);
  };

  const handleApply = () => {
    applyForm.resetFields();
    setApplyModalVisible(true);
  };

  const handleApplySubmit = async () => {
    try {
      const values = await applyForm.validateFields();
      setSubmitting(true);
      const payload: AgentWithdrawApplyRequest = {
        merchantNo: currentMerchantNo,
        ...values,
      };
      await agentWithdrawApi.apply(payload);
      message.success('提现申请提交成功');
      setApplyModalVisible(false);
      fetchData();
      fetchBalance();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error?.message || '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAudit = (record: AgentWithdraw) => {
    setCurrentRecord(record);
    auditForm.resetFields();
    setAuditModalVisible(true);
  };

  const handleAuditSubmit = async () => {
    try {
      const values = await auditForm.validateFields();
      setAuditSubmitting(true);
      const payload: AgentWithdrawAuditRequest = {
        id: currentRecord!.id,
        ...values,
      };
      await agentWithdrawApi.audit(payload);
      message.success('审核成功');
      setAuditModalVisible(false);
      fetchData();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error?.message || '审核失败');
    } finally {
      setAuditSubmitting(false);
    }
  };

  const handleViewDetail = (record: AgentWithdraw) => {
    setCurrentRecord(record);
    setDetailModalVisible(true);
  };

  const handleExecuteTransfer = async (record: AgentWithdraw) => {
    try {
      await agentWithdrawApi.execute(record.id);
      message.success('转账执行成功');
      fetchData();
    } catch (e: any) {
      message.error(e?.message || '转账执行失败');
    }
  };

  const renderStatus = (status: number) => {
    const statusMap: Record<number, { color: string; text: string }> = {
      0: { color: 'orange', text: '待审核' },
      1: { color: 'blue', text: '审核通过' },
      2: { color: 'red', text: '审核拒绝' },
      3: { color: 'processing', text: '转账中' },
      4: { color: 'green', text: '提现成功' },
      5: { color: 'red', text: '提现失败' },
    };
    const info = statusMap[status] || { color: 'default', text: '未知' };
    return <Tag color={info.color}>{info.text}</Tag>;
  };

  const columns: ColumnsType<AgentWithdraw> = [
    {
      title: '提现单号',
      dataIndex: 'withdrawNo',
      key: 'withdrawNo',
      width: 160,
    },
    {
      title: '商户信息',
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
      title: '提现金额',
      dataIndex: 'withdrawAmount',
      key: 'withdrawAmount',
      width: 120,
      render: (val: number) => (
        <span style={{ color: '#fa8c16', fontWeight: 600 }}>¥{formatAmount(val)}</span>
      ),
    },
    {
      title: '手续费',
      dataIndex: 'feeAmount',
      key: 'feeAmount',
      width: 100,
      render: (val: number) => <span>¥{formatAmount(val || 0)}</span>,
    },
    {
      title: '实际到账',
      dataIndex: 'actualAmount',
      key: 'actualAmount',
      width: 120,
      render: (val: number) => (
        <span style={{ color: '#52c41a', fontWeight: 600 }}>¥{formatAmount(val)}</span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'withdrawStatus',
      key: 'withdrawStatus',
      width: 100,
      render: (status: number) => renderStatus(status),
    },
    {
      title: '开户银行',
      dataIndex: 'bankName',
      key: 'bankName',
      width: 120,
    },
    {
      title: '银行账号',
      dataIndex: 'bankAccount',
      key: 'bankAccount',
      width: 160,
    },
    {
      title: '申请时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (val: string) => formatDateTime(val),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space>
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
          {isAdmin && record.withdrawStatus === 0 && (
            <Tooltip title="审核">
              <Button
                type="link"
                size="small"
                icon={<CheckCircleOutlined />}
                onClick={() => handleAudit(record)}
              >
                审核
              </Button>
            </Tooltip>
          )}
          {isAdmin && (record.withdrawStatus === 1 || record.withdrawStatus === 5) && (
            <Tooltip title="执行转账">
              <Button
                type="link"
                size="small"
                icon={<SendOutlined />}
                onClick={() => handleExecuteTransfer(record)}
              >
                转账
              </Button>
            </Tooltip>
          )}
        </Space>
      ),
    },
  ];

  const pendingCount = data.filter((d) => d.withdrawStatus === 0).length;
  const successCount = data.filter((d) => d.withdrawStatus === 4).length;
  const totalAmount = data.reduce((sum, r) => sum + r.withdrawAmount, 0);

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card>
            <Statistic
              title="可提现余额"
              value={balance}
              precision={2}
              suffix="元"
              prefix={<WalletOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
              formatter={(value: number) => formatAmount(value)}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="待审核笔数"
              value={pendingCount}
              prefix={<ClockCircleOutlined style={{ color: '#fa8c16' }} />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="提现总金额"
              value={totalAmount}
              precision={2}
              suffix="元"
              prefix={<WalletOutlined style={{ color: '#1677ff' }} />}
              valueStyle={{ color: '#1677ff' }}
              formatter={(value: number) => formatAmount(value)}
            />
          </Card>
        </Col>
      </Row>

      <Card
        title="佣金提现"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>
              刷新
            </Button>
            {!isAdmin && (
              <Button type="primary" icon={<PlusOutlined />} onClick={handleApply}>
                申请提现
              </Button>
            )}
          </Space>
        }
      >
        <Form
          form={queryForm}
          layout="inline"
          style={{ marginBottom: 16 }}
          onFinish={handleSearch}
        >
          <Form.Item name="withdrawNo" label="提现单号">
            <Input placeholder="请输入提现单号" allowClear style={{ width: 160 }} />
          </Form.Item>
          {isAdmin && (
            <>
              <Form.Item name="merchantNo" label="商户号">
                <Input placeholder="请输入商户号" allowClear style={{ width: 160 }} />
              </Form.Item>
              <Form.Item name="merchantName" label="商户名称">
                <Input placeholder="请输入商户名称" allowClear style={{ width: 160 }} />
              </Form.Item>
            </>
          )}
          <Form.Item name="withdrawStatus" label="状态">
            <Select
              placeholder="全部状态"
              allowClear
              style={{ width: 120 }}
              options={[
                { label: '待审核', value: 0 },
                { label: '审核通过', value: 1 },
                { label: '审核拒绝', value: 2 },
                { label: '转账中', value: 3 },
                { label: '提现成功', value: 4 },
                { label: '提现失败', value: 5 },
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

        <Table<AgentWithdraw>
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

      <Modal
        title="申请提现"
        open={applyModalVisible}
        onCancel={() => setApplyModalVisible(false)}
        onOk={handleApplySubmit}
        confirmLoading={submitting}
        width={520}
        destroyOnClose
      >
        <Form form={applyForm} layout="vertical" name="withdraw_apply_form">
          <Card size="small" style={{ marginBottom: 16, background: '#f5f5f5' }}>
            <Statistic
              title="当前可提现余额"
              value={balance}
              precision={2}
              suffix="元"
              valueStyle={{ color: '#52c41a', fontSize: 24 }}
              formatter={(value: number) => formatAmount(value)}
            />
          </Card>
          <Form.Item
            name="withdrawAmount"
            label="提现金额(元)"
            rules={[
              { required: true, message: '请输入提现金额' },
              {
                validator: (_, value) => {
                  if (value > balance) {
                    return Promise.reject(new Error('提现金额不能大于可提现余额'));
                  }
                  if (value < 10) {
                    return Promise.reject(new Error('最低提现金额为10元'));
                  }
                  return Promise.resolve();
                },
              },
            ]}
          >
            <InputNumber
              min={10}
              step={100}
              precision={2}
              style={{ width: '100%' }}
              placeholder="请输入提现金额，最低10元"
              addonAfter={
                <Button
                  type="link"
                  size="small"
                  onClick={() => applyForm.setFieldsValue({ withdrawAmount: balance })}
                >
                  全部提现
                </Button>
              }
            />
          </Form.Item>
          <Form.Item
            name="bankName"
            label="开户银行"
            rules={[{ required: true, message: '请输入开户银行' }]}
          >
            <Input placeholder="如：中国工商银行" />
          </Form.Item>
          <Form.Item
            name="bankAccount"
            label="银行账号"
            rules={[{ required: true, message: '请输入银行账号' }]}
          >
            <Input placeholder="请输入银行账号" />
          </Form.Item>
          <Form.Item
            name="accountName"
            label="开户名"
            rules={[{ required: true, message: '请输入开户名' }]}
          >
            <Input placeholder="请输入开户名（姓名或公司名称）" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="审核提现"
        open={auditModalVisible}
        onCancel={() => setAuditModalVisible(false)}
        onOk={handleAuditSubmit}
        confirmLoading={auditSubmitting}
        width={480}
        destroyOnClose
      >
        <Form form={auditForm} layout="vertical" name="withdraw_audit_form">
          {currentRecord && (
            <Descriptions column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="提现单号">{currentRecord.withdrawNo}</Descriptions.Item>
              <Descriptions.Item label="提现金额">
                ¥{formatAmount(currentRecord.withdrawAmount)}
              </Descriptions.Item>
            </Descriptions>
          )}
          <Form.Item
            name="auditStatus"
            label="审核结果"
            rules={[{ required: true, message: '请选择审核结果' }]}
          >
            <Select
              placeholder="请选择审核结果"
              options={[
                { label: '审核通过', value: 1 },
                { label: '审核拒绝', value: 2 },
              ]}
            />
          </Form.Item>
          <Form.Item name="auditRemark" label="审核备注">
            <Input.TextArea rows={3} placeholder="请输入审核备注" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="提现详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
        ]}
        width={640}
        destroyOnClose
      >
        {currentRecord && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="提现单号" span={2}>
              {currentRecord.withdrawNo}
            </Descriptions.Item>
            <Descriptions.Item label="商户名称">{currentRecord.merchantName}</Descriptions.Item>
            <Descriptions.Item label="商户号">{currentRecord.merchantNo}</Descriptions.Item>
            <Descriptions.Item label="提现金额">
              <span style={{ color: '#fa8c16', fontWeight: 600 }}>
                ¥{formatAmount(currentRecord.withdrawAmount)}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="手续费">
              ¥{formatAmount(currentRecord.feeAmount || 0)}
            </Descriptions.Item>
            <Descriptions.Item label="实际到账">
              <span style={{ color: '#52c41a', fontWeight: 600 }}>
                ¥{formatAmount(currentRecord.actualAmount)}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              {renderStatus(currentRecord.withdrawStatus)}
            </Descriptions.Item>
            <Descriptions.Item label="开户银行">{currentRecord.bankName}</Descriptions.Item>
            <Descriptions.Item label="银行账号">{currentRecord.bankAccount}</Descriptions.Item>
            <Descriptions.Item label="开户名" span={2}>
              {currentRecord.accountName}
            </Descriptions.Item>
            <Descriptions.Item label="审核人">{currentRecord.auditUser || '-'}</Descriptions.Item>
            <Descriptions.Item label="审核时间">
              {currentRecord.auditTime ? formatDateTime(currentRecord.auditTime) : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="审核备注" span={2}>
              {currentRecord.auditRemark || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="转账单号">{currentRecord.transferNo || '-'}</Descriptions.Item>
            <Descriptions.Item label="转账时间">
              {currentRecord.transferTime ? formatDateTime(currentRecord.transferTime) : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="申请时间" span={2}>
              {formatDateTime(currentRecord.createdAt)}
            </Descriptions.Item>
            <Descriptions.Item label="备注" span={2}>
              {currentRecord.remark || '-'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default AgentWithdrawPage;

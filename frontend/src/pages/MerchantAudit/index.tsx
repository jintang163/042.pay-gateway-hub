import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Input,
  Select,
  Form,
  Modal,
  Descriptions,
  Row,
  Col,
  Statistic,
  Steps,
  Alert,
  Empty,
  Divider,
  Tabs,
  Typography,
  message,
  Badge,
  Popconfirm,
  Spin,
} from 'antd';
import {
  SearchOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  EyeOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined,
  SafetyCertificateOutlined,
  WarningOutlined,
  MinusCircleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { TabsProps } from 'antd';
import { merchantApi } from '@/api/merchant';
import type { Merchant, AuditProgress, MerchantQueryParams, RiskLevel, VerifyDetail } from '@/types/merchant';

const { Option } = Select;
const { Text, Title, Paragraph } = Typography;

const riskLevelColors: Record<RiskLevel | 'UNKNOWN', string> = {
  LOW: 'green',
  MEDIUM: 'gold',
  HIGH: 'red',
  UNKNOWN: 'default',
};

const riskLevelText: Record<RiskLevel | 'UNKNOWN', string> = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
  UNKNOWN: '未知',
};

const auditStatusColor: Record<number, string> = {
  0: 'blue',
  1: 'green',
  2: 'red',
};

const auditStatusText: Record<number, string> = {
  0: '待审核',
  1: '已通过',
  2: '已拒绝',
};

interface ManualAuditStats {
  totalPending: number;
  highRisk: number;
  mediumRisk: number;
  lowRisk: number;
  needManual: number;
  businessFail: number;
}

const MerchantAudit: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [dataList, setDataList] = useState<Merchant[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [searchForm] = Form.useForm();
  const [auditForm] = Form.useForm();

  const [stats, setStats] = useState<ManualAuditStats | null>(null);
  const [statsLoading, setStatsLoading] = useState(false);

  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [auditModalOpen, setAuditModalOpen] = useState(false);
  const [currentMerchant, setCurrentMerchant] = useState<Merchant | null>(null);
  const [auditProgress, setAuditProgress] = useState<AuditProgress | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [activeTabKey, setActiveTabKey] = useState('pending');

  const fetchStats = useCallback(async () => {
    setStatsLoading(true);
    try {
      const resp = await merchantApi.getManualAuditStats();
      setStats(resp);
    } catch (e) {
      console.error('获取统计数据失败', e);
    } finally {
      setStatsLoading(false);
    }
  }, []);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const query: MerchantQueryParams = {
        current: currentPage,
        size: pageSize,
        merchantName: values.merchantName || undefined,
        merchantNo: values.merchantNo || undefined,
        auditStatus: values.auditStatus ?? undefined,
        riskLevel: values.riskLevel || undefined,
      };
      if (activeTabKey === 'pending') {
        query.auditStatus = 0;
      } else if (activeTabKey === 'approved') {
        query.auditStatus = 1;
      } else if (activeTabKey === 'rejected') {
        query.auditStatus = 2;
      } else if (activeTabKey === 'highRisk') {
        query.riskLevel = 'HIGH';
        query.auditStatus = 0;
      } else if (activeTabKey === 'businessFail') {
        query.auditStatus = 0;
      }
      const resp = await merchantApi.list(query);
      setDataList(resp.records || []);
      setTotal(resp.total || 0);
    } catch (e) {
      console.error('获取商户列表失败', e);
      message.error('获取商户列表失败');
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, activeTabKey, searchForm]);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleTabChange = (key: string) => {
    setActiveTabKey(key);
    setCurrentPage(1);
  };

  const handleDetail = async (record: Merchant) => {
    setCurrentMerchant(record);
    setDetailModalOpen(true);
    setDetailLoading(true);
    setAuditProgress(null);
    try {
      const resp = await merchantApi.getAuditProgress(record.merchantNo);
      setAuditProgress(resp);
    } catch (e) {
      console.error('获取审核进度失败', e);
      message.error('获取审核进度失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const handleAudit = (record: Merchant, action: 'approve' | 'reject') => {
    setCurrentMerchant(record);
    setAuditModalOpen(true);
    auditForm.setFieldsValue({
      auditUserName: '',
      remark: '',
      action,
    });
  };

  const handleAuditSubmit = async () => {
    try {
      const values = await auditForm.validateFields();
      setSubmitLoading(true);
      if (!currentMerchant) return;
      await merchantApi.audit({
        merchantNo: currentMerchant.merchantNo,
        status: values.action,
        remark: values.remark,
        auditUserName: values.auditUserName,
      });
      message.success(values.action === 'approved' ? '审核通过成功' : '审核驳回成功');
      setAuditModalOpen(false);
      auditForm.resetFields();
      setCurrentMerchant(null);
      fetchStats();
      fetchData();
    } catch (e) {
      console.error('审核提交失败', e);
    } finally {
      setSubmitLoading(false);
    }
  };

  const formatRiskLevel = (level?: RiskLevel) => {
    const key: RiskLevel | 'UNKNOWN' = level || 'UNKNOWN';
    return (
      <Tag color={riskLevelColors[key]}>
        {level === 'HIGH' && <ExclamationCircleOutlined style={{ marginRight: 4 }} />}
        {level === 'MEDIUM' && <WarningOutlined style={{ marginRight: 4 }} />}
        {level === 'LOW' && <SafetyCertificateOutlined style={{ marginRight: 4 }} />}
        {riskLevelText[key]}
      </Tag>
    );
  };

  const riskScoreColor = (score?: number) => {
    if (!score) return '#8c8c8c';
    if (score >= 61) return '#cf1322';
    if (score >= 31) return '#d48806';
    return '#389e0d';
  };

  const columns: ColumnsType<Merchant> = [
    {
      title: '商户号',
      dataIndex: 'merchantNo',
      width: 160,
      render: (t: string) => <Text code>{t}</Text>,
    },
    {
      title: '商户名称',
      dataIndex: 'merchantName',
      width: 200,
      ellipsis: true,
    },
    {
      title: '法人',
      dataIndex: 'legalPersonName',
      width: 100,
    },
    {
      title: '手机号',
      dataIndex: 'contactPhone',
      width: 130,
    },
    {
      title: '风险等级',
      dataIndex: 'riskLevel',
      width: 120,
      render: (l: RiskLevel, r) => (
        <Space direction="vertical" size={2}>
          {formatRiskLevel(l)}
          {r.riskScore != null && (
            <Text type="secondary" style={{ fontSize: 12, color: riskScoreColor(r.riskScore) }}>
              评分: {r.riskScore}分
            </Text>
          )}
        </Space>
      ),
    },
    {
      title: '工商核验',
      dataIndex: 'businessVerifyPassed',
      width: 110,
      render: (v: number) => {
        if (v == null) return <Tag icon={<ClockCircleOutlined />} color="default">待核验</Tag>;
        return v === 1
          ? <Tag icon={<CheckCircleOutlined />} color="green">已通过</Tag>
          : <Tag icon={<CloseCircleOutlined />} color="red">未通过</Tag>;
      },
    },
    {
      title: '审核状态',
      dataIndex: 'auditStatus',
      width: 110,
      render: (s: number) => (
        <Badge status={s === 0 ? 'processing' : s === 1 ? 'success' : 'error'} text={auditStatusText[s] || '-'} />
      ),
    },
    {
      title: '当前步骤',
      dataIndex: 'auditStepName',
      width: 120,
      render: (n: string) => <Text type="secondary">{n || '-'}</Text>,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      fixed: 'right',
      render: (_: unknown, record: Merchant) => (
        <Space>
          <Button size="small" type="link" icon={<EyeOutlined />} onClick={() => handleDetail(record)}>
            详情
          </Button>
          {record.auditStatus === 0 && (
            <>
              <Popconfirm
                title="确认通过该商户审核？"
                description={record.riskLevel === 'HIGH' ? '该商户为高风险，通过后将自动生成接入密钥' : '通过后将自动生成接入密钥'}
                okText="确认通过"
                cancelText="取消"
                okButtonProps={{ danger: false, type: 'primary' }}
                onConfirm={() => handleAudit(record, 'approve')}
              >
                <Button size="small" type="primary" icon={<CheckCircleOutlined />}>
                  通过
                </Button>
              </Popconfirm>
              <Popconfirm
                title="确认驳回该商户审核？"
                okText="确认驳回"
                cancelText="取消"
                okButtonProps={{ danger: true }}
                onConfirm={() => handleAudit(record, 'reject')}
              >
                <Button size="small" danger icon={<CloseCircleOutlined />}>
                  驳回
                </Button>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ];

  const tabItems: TabsProps['items'] = [
    {
      key: 'pending',
      label: (
        <Badge count={stats?.totalPending || 0} size="small" offset={[4, 0]}>
          待审核({stats?.totalPending ?? 0})
        </Badge>
      ),
    },
    {
      key: 'highRisk',
      label: (
        <Badge count={stats?.highRisk || 0} size="small" offset={[4, 0]} color="red">
          高风险({stats?.highRisk ?? 0})
        </Badge>
      ),
    },
    {
      key: 'businessFail',
      label: (
        <Badge count={stats?.businessFail || 0} size="small" offset={[4, 0]} color="orange">
          工商异常({stats?.businessFail ?? 0})
        </Badge>
      ),
    },
    {
      key: 'approved',
      label: `已通过(${stats?.lowRisk ?? 0}+)`,
    },
    {
      key: 'rejected',
      label: '已拒绝',
    },
    {
      key: 'all',
      label: '全部',
    },
  ];

  const renderVerifyDetail = (verifyDetail: VerifyDetail | undefined) => {
    if (!verifyDetail) return <Empty description="暂无核验详情" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
    return (
      <div>
        <Alert
          type={verifyDetail.failReason ? 'error' : 'success'}
          showIcon
          message={verifyDetail.failReason ? `核验未通过: ${verifyDetail.failReason}` : '核验已通过'}
          description={
            <Space direction="vertical" size={4}>
              <span><Text strong>核验ID:</Text> <Text code>{verifyDetail.verifyId}</Text></span>
              <span><Text strong>来源:</Text> {verifyDetail.verifySource} / {verifyDetail.verifyVendor}</span>
              <span><Text strong>请求ID:</Text> {verifyDetail.verifyRequestId}</span>
              {verifyDetail.fallbackUsed && <Tag color="warning">真实接口不可用，已降级为沙箱</Tag>}
              <span><Text strong>核验人:</Text> {verifyDetail.verifiedBy} · <Text type="secondary">{verifyDetail.verifyTime}</Text></span>
            </Space>
          }
          style={{ marginBottom: 16 }}
        />
        <Card size="small" title={`综合匹配度: ${verifyDetail.matchOverallScore}分`} style={{ marginBottom: 16 }}>
          <Title level={5} style={{ marginTop: 0 }}>决策理由:</Title>
          <ul style={{ paddingLeft: 20, margin: 0 }}>
            {verifyDetail.decisionReasons?.map((r, idx) => (
              <li key={idx}>
                <Text>{r}</Text>
              </li>
            ))}
          </ul>
        </Card>
        <Row gutter={16}>
          <Col span={12}>
            <Card
              size="small"
              title="原始请求报文"
              extra={<Text type="secondary" style={{ fontSize: 12 }}>Raw Request</Text>}
            >
              <Paragraph
                style={{
                  background: '#f6f8fa',
                  padding: 12,
                  borderRadius: 6,
                  fontFamily: 'monospace',
                  fontSize: 12,
                  maxHeight: 280,
                  overflow: 'auto',
                  margin: 0,
                }}
              >
                {verifyDetail.rawRequest || '-'}
              </Paragraph>
            </Card>
          </Col>
          <Col span={12}>
            <Card
              size="small"
              title="原始响应报文"
              extra={<Text type="secondary" style={{ fontSize: 12 }}>Raw Response</Text>}
            >
              <Paragraph
                style={{
                  background: '#f6f8fa',
                  padding: 12,
                  borderRadius: 6,
                  fontFamily: 'monospace',
                  fontSize: 12,
                  maxHeight: 280,
                  overflow: 'auto',
                  margin: 0,
                }}
              >
                {verifyDetail.rawResponse || '-'}
              </Paragraph>
            </Card>
          </Col>
        </Row>
      </div>
    );
  };

  const renderAuditProgressSteps = (progress: AuditProgress) => {
    const items = progress.steps?.map((s) => ({
      title: s.name,
      description: (
        <Space direction="vertical" size={0} style={{ fontSize: 12 }}>
          <Text type="secondary">{s.description}</Text>
          {s.time && <Text type="secondary" style={{ fontSize: 11 }}>时间: {String(s.time)}</Text>}
          {s.remark && <Text strong style={{ fontSize: 11 }}>备注: {s.remark}</Text>}
        </Space>
      ),
      status: s.status === 'done' ? 'finish' : s.status === 'active' ? 'process' : 'wait',
      icon:
        s.status === 'done' ? <CheckCircleOutlined style={{ color: '#52c41a' }} />
        : s.status === 'active' ? <ClockCircleOutlined spin style={{ color: '#1890ff' }} />
        : <MinusCircleOutlined style={{ color: '#bfbfbf' }} />,
    }));
    return <Steps direction="vertical" current={-1} items={items || []} size="small" />;
  };

  return (
    <div>
      <Spin spinning={statsLoading}>
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={12} md={6}>
            <Card size="small">
              <Statistic
                title="待审核商户"
                value={stats?.totalPending || 0}
                prefix={<ClockCircleOutlined style={{ color: '#1890ff' }} />}
              />
            </Card>
          </Col>
          <Col xs={12} md={6}>
            <Card size="small">
              <Statistic
                title="高风险(需人工)"
                value={stats?.highRisk || 0}
                valueStyle={{ color: '#cf1322' }}
                prefix={<ExclamationCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} md={6}>
            <Card size="small">
              <Statistic
                title="工商核验未通过"
                value={stats?.businessFail || 0}
                valueStyle={{ color: '#d48806' }}
                prefix={<WarningOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} md={6}>
            <Card size="small">
              <Statistic
                title="已转人工审核"
                value={stats?.needManual || 0}
                valueStyle={{ color: '#1890ff' }}
                prefix={<EyeOutlined />}
              />
            </Card>
          </Col>
        </Row>
      </Spin>

      <Card
        size="small"
        style={{ marginBottom: 16 }}
        title="筛选条件"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => { searchForm.resetFields(); setCurrentPage(1); fetchData(); fetchStats(); }}>
              重置/刷新
            </Button>
          </Space>
        }
      >
        <Form form={searchForm} layout="inline" onFinish={() => { setCurrentPage(1); fetchData(); }}>
          <Form.Item name="merchantName" label="商户名称">
            <Input allowClear placeholder="请输入商户名称" prefix={<SearchOutlined />} style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="merchantNo" label="商户号">
            <Input allowClear placeholder="请输入商户号" style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="riskLevel" label="风险等级" initialValue="">
            <Select allowClear style={{ width: 140 }} placeholder="全部">
              <Option value="LOW">低风险</Option>
              <Option value="MEDIUM">中风险</Option>
              <Option value="HIGH">高风险</Option>
            </Select>
          </Form.Item>
          {activeTabKey === 'all' && (
            <Form.Item name="auditStatus" label="审核状态" initialValue={undefined}>
              <Select allowClear style={{ width: 140 }} placeholder="全部">
                <Option value={0}>待审核</Option>
                <Option value={1}>已通过</Option>
                <Option value={2}>已拒绝</Option>
              </Select>
            </Form.Item>
          )}
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card size="small">
        <Tabs
          activeKey={activeTabKey}
          onChange={handleTabChange}
          items={tabItems}
          style={{ marginBottom: 0 }}
        />

        <Table<Merchant>
          rowKey="merchantNo"
          columns={columns}
          dataSource={dataList}
          loading={loading}
          pagination={{
            current: currentPage,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p, s) => { setCurrentPage(p); setPageSize(s); },
          }}
          scroll={{ x: 1400 }}
        />
      </Card>

      <Modal
        title={
          <Space>
            <EyeOutlined />
            <span>商户审核详情 - {currentMerchant?.merchantName}</span>
          </Space>
        }
        open={detailModalOpen}
        onCancel={() => { setDetailModalOpen(false); setCurrentMerchant(null); setAuditProgress(null); }}
        width={1100}
        destroyOnClose
        footer={
          <Space>
            <Button onClick={() => { setDetailModalOpen(false); }}>关闭</Button>
            {currentMerchant?.auditStatus === 0 && auditProgress && (
              <>
                <Button
                  danger
                  icon={<CloseCircleOutlined />}
                  onClick={() => {
                    if (currentMerchant) {
                      setDetailModalOpen(false);
                      handleAudit(currentMerchant, 'reject');
                    }
                  }}
                >
                  驳回
                </Button>
                <Button
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  onClick={() => {
                    if (currentMerchant) {
                      setDetailModalOpen(false);
                      handleAudit(currentMerchant, 'approve');
                    }
                  }}
                >
                  通过
                </Button>
              </>
            )}
          </Space>
        }
      >
        {detailLoading && (
          <div style={{ textAlign: 'center', padding: 60 }}>
            <Spin tip="加载审核详情中..." />
          </div>
        )}

        {!detailLoading && auditProgress && (
          <div>
            {auditProgress.riskLevel === 'HIGH' && (
              <Alert
                type="warning"
                showIcon
                icon={<ExclamationCircleOutlined />}
                message="高风险商户提示"
                description={`风险评分: ${auditProgress.riskScore}分, 等级: ${auditProgress.riskLevelDesc || '高风险'}。请人工复核工商信息和业务真实性。`}
                style={{ marginBottom: 16 }}
                closable
              />
            )}
            {auditProgress.businessVerifyPassed === 0 && (
              <Alert
                type="error"
                showIcon
                message="工商核验未通过"
                description={auditProgress.autoAuditRemark || '请人工复核营业执照真伪'}
                style={{ marginBottom: 16 }}
                closable
              />
            )}

            <Descriptions
              size="small"
              column={2}
              bordered
              title="商户基本信息"
              style={{ marginBottom: 16 }}
            >
              <Descriptions.Item label="商户号"><Text code>{auditProgress.merchantNo}</Text></Descriptions.Item>
              <Descriptions.Item label="商户名称">{auditProgress.merchantName}</Descriptions.Item>
              <Descriptions.Item label="审核状态">
                <Tag color={auditStatusColor[auditProgress.auditStatus]}>{auditProgress.auditStatusDesc}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="当前步骤">
                {auditProgress.auditStepName}
                {auditProgress.auditStepDescription && <Text type="secondary"> ({auditProgress.auditStepDescription})</Text>}
              </Descriptions.Item>
              <Descriptions.Item label="风险等级">
                <Space>
                  {formatRiskLevel(auditProgress.riskLevel)}
                  {auditProgress.riskScore != null && (
                    <Text strong style={{ color: riskScoreColor(auditProgress.riskScore) }}>
                      {auditProgress.riskScore}分
                    </Text>
                  )}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="人工审核人">
                {auditProgress.manualAuditUser
                  ? `${auditProgress.manualAuditUser} · ${auditProgress.manualAuditTime || ''}`
                  : <Text type="secondary">暂未处理</Text>}
              </Descriptions.Item>
              <Descriptions.Item label="审核备注" span={2}>
                {auditProgress.auditRemark || <Text type="secondary">-</Text>}
              </Descriptions.Item>
            </Descriptions>

            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={10}>
                <Card size="small" title="审核流程进度">
                  {renderAuditProgressSteps(auditProgress)}
                </Card>
              </Col>
              <Col span={14}>
                <Card size="small" title="自动审核记录">
                  <Descriptions size="small" column={1} bordered>
                    <Descriptions.Item label="工商核验">
                      {auditProgress.businessVerifyPassed === 1
                        ? <Tag color="green">已通过</Tag>
                        : auditProgress.businessVerifyPassed === 0
                          ? <Tag color="red">未通过</Tag>
                          : <Tag>待核验</Tag>}
                      <Text type="secondary" style={{ marginLeft: 8 }}>
                        {auditProgress.businessVerifyTime || '-'}
                      </Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="自动审核">
                      {auditProgress.autoAuditPassed === 1
                        ? <Tag color="green">自动通过</Tag>
                        : auditProgress.autoAuditPassed === 0
                          ? <Tag color="orange">转人工</Tag>
                          : <Tag>进行中</Tag>}
                      <Text type="secondary" style={{ marginLeft: 8 }}>
                        {auditProgress.autoAuditTime || '-'}
                      </Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="自动审核备注">
                      {auditProgress.autoAuditRemark || <Text type="secondary">-</Text>}
                    </Descriptions.Item>
                  </Descriptions>
                </Card>
              </Col>
            </Row>

            <Divider orientation="left" orientationMargin={0}>
              <Space>
                <SafetyCertificateOutlined />
                <span>工商核验详情(可复核)</span>
              </Space>
            </Divider>
            {renderVerifyDetail(auditProgress.verifyDetail)}
          </div>
        )}
      </Modal>

      <Modal
        title={
          <Space>
            {auditForm.getFieldValue('action') === 'approved'
              ? <><CheckCircleOutlined style={{ color: '#52c41a' }} /><span>人工审核通过确认</span></>
              : <><CloseCircleOutlined style={{ color: '#cf1322' }} /><span>人工审核驳回确认</span></>}
          </Space>
        }
        open={auditModalOpen}
        onCancel={() => { setAuditModalOpen(false); auditForm.resetFields(); setCurrentMerchant(null); }}
        onOk={handleAuditSubmit}
        confirmLoading={submitLoading}
        okText={auditForm.getFieldValue('action') === 'approved' ? '确认通过' : '确认驳回'}
        okButtonProps={auditForm.getFieldValue('action') === 'approved' ? { type: 'primary' } : { danger: true }}
        destroyOnClose
        width={560}
      >
        {currentMerchant && (
          <Alert
            style={{ marginBottom: 16 }}
            type={auditForm.getFieldValue('action') === 'approved' ? 'info' : 'warning'}
            showIcon
            message={
              <Space>
                <Text>商户:</Text>
                <Text strong>{currentMerchant.merchantName}</Text>
                <Text code>{currentMerchant.merchantNo}</Text>
                {formatRiskLevel(currentMerchant.riskLevel)}
              </Space>
            }
            description={
              auditForm.getFieldValue('action') === 'approved'
                ? '审核通过后，系统将自动生成 MD5 / RSA / SM2 接入密钥，商户状态变为启用。'
                : '审核驳回后，商户将无法接入本系统，请谨慎操作！'
            }
          />
        )}
        <Form form={auditForm} layout="vertical" preserve={false}>
          <Form.Item name="action" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            label="审核人姓名"
            name="auditUserName"
            rules={[{ required: true, message: '请输入审核人姓名' }, { min: 2, message: '至少2个字符' }]}
          >
            <Input placeholder="请输入您的姓名(用于留痕)" maxLength={32} showCount />
          </Form.Item>
          <Form.Item
            label="审核意见"
            name="remark"
            rules={[
              {
                validator: (_, v) => {
                  if (auditForm.getFieldValue('action') === 'reject' && (!v || String(v).trim().length < 5)) {
                    return Promise.reject(new Error('驳回必须填写至少5字原因'));
                  }
                  return Promise.resolve();
                },
              },
            ]}
          >
            <Input.TextArea
              placeholder={auditForm.getFieldValue('action') === 'reject' ? '请详细填写驳回原因(至少5字)' : '请填写审核意见(可选)'}
              rows={4}
              maxLength={500}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default MerchantAudit;

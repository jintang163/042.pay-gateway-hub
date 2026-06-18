import { useState, useEffect } from 'react';
import {
  Card,
  Row,
  Col,
  Statistic,
  Tree,
  Tag,
  Button,
  Space,
  Input,
  Select,
  Form,
  message,
  Modal,
  InputNumber,
  Descriptions,
  Tooltip,
  Divider,
} from 'antd';
import {
  ReloadOutlined,
  TeamOutlined,
  DollarOutlined,
  WalletOutlined,
  PlusOutlined,
  EditOutlined,
  UserAddOutlined,
} from '@ant-design/icons';
import type { DataNode } from 'antd/es/tree';
import { agentRelationApi, merchantApi } from '@/api';
import type { AgentTree, AgentStats, AgentRelation, AgentRelationSaveRequest } from '@/types/agent';
import type { Merchant } from '@/types/merchant';
import { useUserStore } from '@/store';
import { formatAmount, formatDateTime, formatPercent } from '@/utils';

const AgentTreePage = () => {
  const { user } = useUserStore();
  const isAdmin = user?.role === 'admin' || user?.role === 'operator';

  const [form] = Form.useForm();
  const [stats, setStats] = useState<AgentStats | null>(null);
  const [treeData, setTreeData] = useState<DataNode[]>([]);
  const [loading, setLoading] = useState(false);
  const [statsLoading, setStatsLoading] = useState(false);
  const [currentMerchantNo, setCurrentMerchantNo] = useState<string>('');
  const [selectedNode, setSelectedNode] = useState<AgentTree | null>(null);

  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<AgentRelation | null>(null);
  const [isAddSub, setIsAddSub] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [merchantOptions, setMerchantOptions] = useState<{ label: string; value: string }[]>([]);

  const fetchStats = async (merchantNo: string) => {
    if (!merchantNo) return;
    try {
      setStatsLoading(true);
      const result = await agentRelationApi.getStats(merchantNo);
      setStats(result as any);
    } catch (e: any) {
      message.error(e?.message || '加载统计数据失败');
    } finally {
      setStatsLoading(false);
    }
  };

  const fetchTree = async (merchantNo: string) => {
    if (!merchantNo) return;
    try {
      setLoading(true);
      const result = await agentRelationApi.getTree(merchantNo);
      setTreeData(convertToTreeData(result as any));
    } catch (e: any) {
      message.error(e?.message || '加载代理树失败');
      setTreeData([]);
    } finally {
      setLoading(false);
    }
  };

  const convertToTreeData = (nodes: AgentTree[]): DataNode[] => {
    return nodes.map((node) => ({
      key: node.merchantNo,
      title: (
        <Space>
          <span style={{ fontWeight: 500 }}>{node.merchantName}</span>
          <Tag color={node.status === 1 ? 'green' : 'default'}>
            L{node.agentLevel}
          </Tag>
          {node.commissionRate !== undefined && node.commissionRate !== null && (
            <Tag color="gold">{node.commissionRate}%</Tag>
          )}
        </Space>
      ),
      children: node.children ? convertToTreeData(node.children) : undefined,
    }));
  };

  const fetchMerchantOptions = async (keyword?: string) => {
    try {
      const result = await merchantApi.list({
        pageNum: 1,
        pageSize: 50,
        merchantName: keyword || undefined,
        auditStatus: 1,
      } as any);
      const options = (result.records as Merchant[]).map((m) => ({
        label: `${m.merchantName} (${m.merchantNo})`,
        value: m.merchantNo,
      }));
      setMerchantOptions(options);
    } catch {
    }
  };

  const initData = async () => {
    let merchantNo = '';
    if (isAdmin) {
      merchantNo = 'M000001';
    } else {
      merchantNo = user?.username || '';
    }
    setCurrentMerchantNo(merchantNo);
    fetchStats(merchantNo);
    fetchTree(merchantNo);
  };

  useEffect(() => {
    initData();
    if (isAdmin) {
      fetchMerchantOptions();
    }
  }, [isAdmin]);

  const handleSelect = (selectedKeys: React.Key[]) => {
    if (selectedKeys.length > 0) {
      const key = selectedKeys[0] as string;
      findNodeByKey(treeData, key);
    }
  };

  const findNodeByKey = (nodes: DataNode[], key: string) => {
    for (const node of nodes) {
      if (node.key === key) {
        setSelectedNode(node as any);
        return;
      }
      if (node.children) {
        findNodeByKey(node.children as DataNode[], key);
      }
    }
  };

  const handleAddSubAgent = () => {
    if (!selectedNode) {
      message.warning('请先选择一个节点');
      return;
    }
    setIsAddSub(true);
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      parentMerchantNo: selectedNode.merchantNo,
      parentMerchantName: selectedNode.merchantName,
      status: 1,
      commissionRate: 10,
    });
    setModalVisible(true);
  };

  const handleEditAgent = async () => {
    if (!selectedNode) {
      message.warning('请先选择一个节点');
      return;
    }
    try {
      const relation = await agentRelationApi.getByMerchantNo(selectedNode.merchantNo);
      setEditing(relation as any);
      setIsAddSub(false);
      form.setFieldsValue({
        ...relation,
      });
      setModalVisible(true);
    } catch (e: any) {
      message.error(e?.message || '获取代理信息失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload: AgentRelationSaveRequest = {
        ...values,
      };
      if (isAddSub) {
        payload.parentMerchantNo = selectedNode?.merchantNo;
      }
      await agentRelationApi.save(payload);
      message.success(editing ? '更新成功' : '添加成功');
      setModalVisible(false);
      fetchTree(currentMerchantNo);
      fetchStats(currentMerchantNo);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error?.message || (editing ? '更新失败' : '添加失败'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleRefresh = () => {
    fetchTree(currentMerchantNo);
    fetchStats(currentMerchantNo);
  };

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card loading={statsLoading}>
            <Statistic
              title="总代理数"
              value={stats?.totalAgentCount || 0}
              prefix={<TeamOutlined style={{ color: '#1677ff' }} />}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={statsLoading}>
            <Statistic
              title="活跃代理数"
              value={stats?.activeAgentCount || 0}
              prefix={<TeamOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={statsLoading}>
            <Statistic
              title="累计分润"
              value={stats?.totalProfitAmount || 0}
              precision={2}
              suffix="元"
              prefix={<DollarOutlined style={{ color: '#fa8c16' }} />}
              valueStyle={{ color: '#fa8c16' }}
              formatter={(value: number) => formatAmount(value)}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={statsLoading}>
            <Statistic
              title="可提现余额"
              value={stats?.availableBalance || 0}
              precision={2}
              suffix="元"
              prefix={<WalletOutlined style={{ color: '#722ed1' }} />}
              valueStyle={{ color: '#722ed1' }}
              formatter={(value: number) => formatAmount(value)}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={10}>
          <Card
            title="代理关系树"
            extra={
              <Space>
                {isAdmin && (
                  <Select
                    placeholder="选择商户"
                    style={{ width: 200 }}
                    showSearch
                    filterOption={false}
                    onSearch={fetchMerchantOptions}
                    options={merchantOptions}
                    value={currentMerchantNo}
                    onChange={(val) => {
                      setCurrentMerchantNo(val);
                      fetchStats(val);
                      fetchTree(val);
                    }}
                  />
                )}
                <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
                  刷新
                </Button>
              </Space>
            }
            style={{ height: 'calc(100vh - 280px)', overflow: 'auto' }}
          >
            <Tree
              showLine
              defaultExpandAll
              onSelect={handleSelect}
              treeData={treeData}
              loading={loading}
            />
          </Card>
        </Col>
        <Col span={14}>
          <Card
            title="代理详情"
            extra={
              selectedNode && (
                <Space>
                  <Tooltip title="添加下级">
                    <Button
                      type="primary"
                      icon={<UserAddOutlined />}
                      onClick={handleAddSubAgent}
                    >
                      添加下级
                    </Button>
                  </Tooltip>
                  <Tooltip title="编辑">
                    <Button icon={<EditOutlined />} onClick={handleEditAgent}>
                      编辑
                    </Button>
                  </Tooltip>
                </Space>
              )
            }
            style={{ height: 'calc(100vh - 280px)', overflow: 'auto' }}
          >
            {selectedNode ? (
              <div>
                <Descriptions column={2} bordered size="middle">
                  <Descriptions.Item label="商户名称" span={2}>
                    {selectedNode.merchantName}
                  </Descriptions.Item>
                  <Descriptions.Item label="商户号">
                    {selectedNode.merchantNo}
                  </Descriptions.Item>
                  <Descriptions.Item label="代理层级">
                    <Tag color="blue">L{selectedNode.agentLevel}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="分润比例">
                    <Tag color="gold">{selectedNode.commissionRate || 0}%</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="状态">
                    <Tag color={selectedNode.status === 1 ? 'green' : 'default'}>
                      {selectedNode.status === 1 ? '启用' : '禁用'}
                    </Tag>
                  </Descriptions.Item>
                </Descriptions>
                <Divider orientation="left">下级统计</Divider>
                <Row gutter={16}>
                  <Col span={8}>
                    <Statistic
                      title="直接下级数"
                      value={selectedNode.children?.length || 0}
                      valueStyle={{ fontSize: 20 }}
                    />
                  </Col>
                </Row>
              </div>
            ) : (
              <div style={{ textAlign: 'center', color: '#999', padding: '80px 0' }}>
                请在左侧选择代理节点查看详情
              </div>
            )}
          </Card>
        </Col>
      </Row>

      <Modal
        title={isAddSub ? '添加下级代理' : '编辑代理关系'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical" name="agent_relation_form">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="merchantNo"
                label="商户号"
                rules={[{ required: true, message: '请输入商户号' }]}
              >
                <Input placeholder="请输入商户号" disabled={!!editing} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="merchantName"
                label="商户名称"
                rules={[{ required: true, message: '请输入商户名称' }]}
              >
                <Input placeholder="请输入商户名称" />
              </Form.Item>
            </Col>
          </Row>
          {isAddSub && (
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="parentMerchantNo" label="上级商户号">
                  <Input disabled />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="parentMerchantName" label="上级商户名称">
                  <Input disabled />
                </Form.Item>
              </Col>
            </Row>
          )}
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="commissionRate"
                label="分润比例(%)"
                rules={[{ required: true, message: '请输入分润比例' }]}
              >
                <InputNumber min={0} max={100} step={0.1} precision={2} style={{ width: '100%' }} placeholder="如：10" />
              </Form.Item>
            </Col>
            <Col span={12}>
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

export default AgentTreePage;

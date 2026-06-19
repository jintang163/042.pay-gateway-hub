import { useState, useEffect } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Button,
  Table,
  Space,
  Row,
  Col,
  Divider,
  message,
  InputNumber,
  Alert,
  Typography,
} from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import type { InvoiceApplyRequest } from '@/types/invoice';

const { Text } = Typography;

interface BatchInvoiceApplyModalProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (invoices: InvoiceApplyRequest[]) => Promise<void>;
  submitting: boolean;
}

interface BatchInvoiceItem extends InvoiceApplyRequest {
  key: string;
}

const BatchInvoiceApplyModal = ({ visible, onClose, onSubmit, submitting }: BatchInvoiceApplyModalProps) => {
  const [form] = Form.useForm();
  const [items, setItems] = useState<BatchInvoiceItem[]>([]);

  useEffect(() => {
    if (visible) {
      form.resetFields();
      setItems([{
        key: Math.random().toString(36).substr(2, 9),
        orderNo: '',
        titleType: 2,
        buyerTitle: '',
        channelCode: 'NUONUO',
        invoiceContent: '商品明细',
      }]);
    }
  }, [visible]);

  const handleAddRow = () => {
    const newItem: BatchInvoiceItem = {
      key: Math.random().toString(36).substr(2, 9),
      orderNo: '',
      titleType: 2,
      buyerTitle: '',
      channelCode: form.getFieldValue('commonChannelCode') || 'NUONUO',
      invoiceContent: form.getFieldValue('commonInvoiceContent') || '商品明细',
    };
    const commonTitleType = form.getFieldValue('commonTitleType');
    const commonBuyerTitle = form.getFieldValue('commonBuyerTitle');
    const commonBuyerTaxNo = form.getFieldValue('commonBuyerTaxNo');
    const commonBuyerEmail = form.getFieldValue('commonBuyerEmail');
    if (commonTitleType) newItem.titleType = commonTitleType;
    if (commonBuyerTitle) newItem.buyerTitle = commonBuyerTitle;
    if (commonBuyerTaxNo) newItem.buyerTaxNo = commonBuyerTaxNo;
    if (commonBuyerEmail) newItem.buyerEmail = commonBuyerEmail;
    setItems([...items, newItem]);
  };

  const handleDeleteRow = (key: string) => {
    if (items.length <= 1) {
      message.warning('至少保留一条记录');
      return;
    }
    setItems(items.filter(item => item.key !== key));
  };

  const handleFieldChange = (key: string, field: keyof BatchInvoiceItem, value: any) => {
    setItems(items.map(item =>
      item.key === key ? { ...item, [field]: value } : item
    ));
  };

  const handleApplyCommonToAll = () => {
    const commonValues = form.getFieldsValue();
    const updatedItems = items.map(item => ({
      ...item,
      channelCode: commonValues.commonChannelCode || item.channelCode,
      titleType: commonValues.commonTitleType || item.titleType,
      buyerTitle: commonValues.commonBuyerTitle || item.buyerTitle,
      buyerTaxNo: commonValues.commonBuyerTaxNo || item.buyerTaxNo,
      buyerEmail: commonValues.commonBuyerEmail || item.buyerEmail,
      invoiceContent: commonValues.commonInvoiceContent || item.invoiceContent,
    }));
    setItems(updatedItems);
    message.success('已将通用信息应用到所有发票');
  };

  const handleSubmit = async () => {
    try {
      const commonValues = form.getFieldsValue();

      const invoices = items.map(item => {
        if (!item.orderNo) {
          throw new Error('请填写所有订单号');
        }
        if (!item.buyerTitle) {
          throw new Error('请填写所有发票抬头');
        }
        return {
          orderNo: item.orderNo,
          channelCode: item.channelCode || commonValues.commonChannelCode || 'NUONUO',
          titleType: item.titleType || commonValues.commonTitleType || 2,
          buyerTitle: item.buyerTitle || commonValues.commonBuyerTitle,
          buyerTaxNo: item.buyerTaxNo || commonValues.commonBuyerTaxNo,
          buyerEmail: item.buyerEmail || commonValues.commonBuyerEmail,
          buyerPhone: item.buyerPhone || commonValues.commonBuyerPhone,
          invoiceAmount: item.invoiceAmount,
          invoiceContent: item.invoiceContent || commonValues.commonInvoiceContent,
          remark: item.remark || commonValues.commonRemark,
        } as InvoiceApplyRequest;
      });

      const validateResult = invoices.every((inv, idx) => {
        if (!inv.orderNo) {
          message.error(`第 ${idx + 1} 条：订单号不能为空`);
          return false;
        }
        if (!inv.buyerTitle) {
          message.error(`第 ${idx + 1} 条：发票抬头不能为空`);
          return false;
        }
        if (inv.titleType === 2 && !inv.buyerTaxNo) {
          message.error(`第 ${idx + 1} 条：企业抬头必须填写税号`);
          return false;
        }
        if (!inv.buyerEmail) {
          message.error(`第 ${idx + 1} 条：接收邮箱不能为空`);
          return false;
        }
        return true;
      });

      if (!validateResult) return;
      await onSubmit(invoices);
    } catch (error: any) {
      if (error?.message) {
        message.error(error.message);
      }
    }
  };

  const columns = [
    {
      title: '序号',
      key: 'index',
      width: 60,
      render: (_: any, __: any, index: number) => index + 1,
    },
    {
      title: '订单号 *',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 200,
      render: (_: any, record: BatchInvoiceItem) => (
        <Input
          placeholder="请输入订单号"
          value={record.orderNo}
          onChange={(e) => handleFieldChange(record.key, 'orderNo', e.target.value)}
        />
      ),
    },
    {
      title: '开票金额',
      dataIndex: 'invoiceAmount',
      key: 'invoiceAmount',
      width: 120,
      render: (_: any, record: BatchInvoiceItem) => (
        <InputNumber
          style={{ width: '100%' }}
          min={0.01}
          step={0.01}
          placeholder="默认订单金额"
          value={record.invoiceAmount}
          onChange={(val) => handleFieldChange(record.key, 'invoiceAmount', val)}
        />
      ),
    },
    {
      title: '抬头类型 *',
      dataIndex: 'titleType',
      key: 'titleType',
      width: 100,
      render: (_: any, record: BatchInvoiceItem) => (
        <Select
          value={record.titleType}
          onChange={(val) => handleFieldChange(record.key, 'titleType', val)}
          options={[
            { label: '个人', value: 1 },
            { label: '企业', value: 2 },
          ]}
        />
      ),
    },
    {
      title: '发票抬头 *',
      dataIndex: 'buyerTitle',
      key: 'buyerTitle',
      width: 160,
      render: (_: any, record: BatchInvoiceItem) => (
        <Input
          placeholder="请输入发票抬头"
          value={record.buyerTitle}
          onChange={(e) => handleFieldChange(record.key, 'buyerTitle', e.target.value)}
        />
      ),
    },
    {
      title: '企业税号',
      dataIndex: 'buyerTaxNo',
      key: 'buyerTaxNo',
      width: 160,
      render: (_: any, record: BatchInvoiceItem) => (
        <Input
          placeholder={record.titleType === 2 ? '必填' : '可选'}
          value={record.buyerTaxNo}
          onChange={(e) => handleFieldChange(record.key, 'buyerTaxNo', e.target.value)}
        />
      ),
    },
    {
      title: '接收邮箱 *',
      dataIndex: 'buyerEmail',
      key: 'buyerEmail',
      width: 180,
      render: (_: any, record: BatchInvoiceItem) => (
        <Input
          placeholder="请输入接收邮箱"
          value={record.buyerEmail}
          onChange={(e) => handleFieldChange(record.key, 'buyerEmail', e.target.value)}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 60,
      fixed: 'right',
      render: (_: any, record: BatchInvoiceItem) => (
        <Button
          type="text"
          danger
          icon={<MinusCircleOutlined />}
          onClick={() => handleDeleteRow(record.key)}
          disabled={items.length <= 1}
        />
      ),
    },
  ];

  return (
    <Modal
      title="批量申请电子发票"
      open={visible}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={submitting}
      width={1200}
      destroyOnClose
      okText="提交批量开票"
      bodyStyle={{ maxHeight: '70vh', overflowY: 'auto' }}
    >
      <Alert
        type="info"
        showIcon
        message="批量开票说明"
        description='请先填写下方通用信息，点击"应用到所有"可快速填充。然后在表格中填写各订单的开票信息。'
        style={{ marginBottom: 16 }}
      />

      <Form form={form} layout="vertical">
        <Divider orientation="left">通用信息 <Text type="secondary">(可选，将应用到所有发票)</Text></Divider>
        <Row gutter={16}>
          <Col span={6}>
            <Form.Item name="commonChannelCode" label="开票渠道" initialValue="NUONUO">
              <Select
                options={[
                  { label: '诺诺发票', value: 'NUONUO' },
                  { label: '百望发票', value: 'BAIWANG' },
                ]}
              />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="commonTitleType" label="抬头类型" initialValue={2}>
              <Select
                options={[
                  { label: '个人', value: 1 },
                  { label: '企业', value: 2 },
                ]}
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="commonBuyerTitle" label="发票抬头">
              <Input placeholder="请输入发票抬头" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="commonBuyerTaxNo" label="企业税号">
              <Input placeholder="企业抬头必填" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="commonBuyerEmail" label="接收邮箱">
              <Input placeholder="电子发票将发送至该邮箱" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="commonInvoiceContent" label="发票内容" initialValue="商品明细">
              <Input placeholder="默认：商品明细" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label=" ">
              <Button type="dashed" block onClick={handleApplyCommonToAll}>
                <PlusOutlined /> 应用通用信息到所有发票
              </Button>
            </Form.Item>
          </Col>
        </Row>

        <Divider orientation="left">开票明细</Divider>
        <div style={{ marginBottom: 12 }}>
          <Space>
            <Button type="dashed" icon={<PlusOutlined />} onClick={handleAddRow}>
              新增开票记录
            </Button>
            <Text type="secondary">共 {items.length} 条记录</Text>
          </Space>
        </div>

        <Table<BatchInvoiceItem>
          columns={columns}
          dataSource={items}
          rowKey="key"
          pagination={false}
          scroll={{ x: 1100 }}
          size="small"
        />
      </Form>
    </Modal>
  );
};

export default BatchInvoiceApplyModal;

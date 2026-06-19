import { useState, useEffect } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Row,
  Col,
  Alert,
  Space,
  Typography,
} from 'antd';
import { invoiceApi } from '@/api';
import type { InvoiceApplyRequest } from '@/types/invoice';
import { formatAmount } from '@/utils';

const { InputNumber } = Form;
const { Text } = Typography;

interface InvoiceApplyModalProps {
  visible: boolean;
  onClose: () => void;
  onSuccess?: () => void;
  orderInfo?: {
    orderNo: string;
    payAmount: number;
    productSubject?: string;
  };
}

const InvoiceApplyModal = ({ visible, onClose, onSuccess, orderInfo }: InvoiceApplyModalProps) => {
  const [form] = Form.useForm<InvoiceApplyRequest>();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (visible && orderInfo) {
      form.setFieldsValue({
        orderNo: orderInfo.orderNo,
        invoiceAmount: orderInfo.payAmount,
        invoiceContent: orderInfo.productSubject || '商品明细',
        channelCode: 'NUONUO',
        titleType: 2,
      });
    } else if (visible) {
      form.resetFields();
      form.setFieldsValue({
        channelCode: 'NUONUO',
        titleType: 2,
      });
    }
  }, [visible, orderInfo]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      await invoiceApi.apply(values as any);
      if (onSuccess) onSuccess();
      onClose();
    } catch (error: any) {
      if (error?.errorFields) return;
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="申请电子发票"
      open={visible}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={submitting}
      width={600}
      destroyOnClose
      okText="提交申请"
    >
      <Form form={form} layout="vertical">
        {orderInfo && (
          <Alert
            type="info"
            showIcon
            message={
              <Space>
                <Text>订单号: {orderInfo.orderNo}</Text>
                <Text strong type="danger">
                  开票金额: {formatAmount(orderInfo.payAmount)}
                </Text>
              </Space>
            }
            style={{ marginBottom: 16 }}
          />
        )}
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="orderNo"
              label="关联订单号"
              rules={[{ required: true, message: '请输入订单号' }]}
            >
              <Input placeholder="请输入订单号" disabled={!!orderInfo} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="channelCode"
              label="开票渠道"
              rules={[{ required: true, message: '请选择开票渠道' }]}
            >
              <Select
                options={[
                  { label: '诺诺发票', value: 'NUONUO' },
                  { label: '百望发票', value: 'BAIWANG' },
                ]}
              />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="titleType"
              label="抬头类型"
              rules={[{ required: true, message: '请选择抬头类型' }]}
            >
              <Select
                options={[
                  { label: '个人', value: 1 },
                  { label: '企业', value: 2 },
                ]}
                onChange={(val) => {
                  if (val === 1) {
                    form.setFieldsValue({ buyerTaxNo: undefined });
                  }
                }}
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="buyerTitle"
              label="发票抬头"
              rules={[{ required: true, message: '请输入发票抬头' }]}
            >
              <Input placeholder="请输入发票抬头" />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item noStyle shouldUpdate={(prev, cur) => prev.titleType !== cur.titleType}>
          {({ getFieldValue }) =>
            getFieldValue('titleType') === 2 ? (
              <Form.Item
                name="buyerTaxNo"
                label="企业税号"
                rules={[{ required: true, message: '请输入企业税号' }]}
              >
                <Input placeholder="请输入企业纳税人识别号" />
              </Form.Item>
            ) : null
          }
        </Form.Item>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="buyerEmail"
              label="接收邮箱"
              rules={[
                { type: 'email', message: '请输入正确的邮箱地址' },
                { required: true, message: '请输入接收邮箱' },
              ]}
            >
              <Input placeholder="电子发票将发送至该邮箱" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="buyerPhone" label="接收手机号">
              <Input placeholder="可选，短信提醒" />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="buyerAddress" label="企业地址" extra="企业抬头可填写">
          <Input placeholder="可选" />
        </Form.Item>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="buyerBankName" label="开户银行">
              <Input placeholder="可选" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="buyerBankAccount" label="银行账号">
              <Input placeholder="可选" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="invoiceAmount"
              label="开票金额"
              rules={[{ required: true, message: '请输入开票金额' }]}
            >
              <InputNumber style={{ width: '100%' }} min={0.01} step={0.01} prefix="¥" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="invoiceContent" label="发票内容">
              <Input placeholder="默认：商品明细" />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="remark" label="备注">
          <Input.TextArea rows={3} placeholder="选填" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default InvoiceApplyModal;

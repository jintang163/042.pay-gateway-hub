import { useState, useEffect, useRef, useCallback } from 'react';
import { Button, Form, Input, Select, DatePicker, Space, type FormInstance } from 'antd';
import type { FormItemProps } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';

export interface QueryField {
  name: string;
  label: string;
  type: 'input' | 'select' | 'date' | 'dateRange' | 'textarea' | 'inputNumber';
  placeholder?: string;
  options?: { label: string; value: string | number | boolean }[];
  width?: number | string;
  allowClear?: boolean;
  rules?: FormItemProps['rules'];
  props?: Record<string, unknown>;
}

interface QueryFormProps {
  fields: QueryField[];
  onQuery: (values: Record<string, unknown>) => void;
  onReset?: () => void;
  initialValues?: Record<string, unknown>;
  form?: FormInstance;
  inline?: boolean;
  showReset?: boolean;
  extra?: React.ReactNode;
}

const QueryForm = ({
  fields,
  onQuery,
  onReset,
  initialValues,
  form: externalForm,
  inline = true,
  showReset = true,
  extra,
}: QueryFormProps) => {
  const [form] = Form.useForm(externalForm);
  const [loading, setLoading] = useState(false);
  const mountedRef = useRef(true);

  useEffect(() => {
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const handleQuery = useCallback(async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await onQuery(values);
    } catch {
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, [form, onQuery]);

  const handleReset = useCallback(() => {
    form.resetFields();
    onReset?.();
  }, [form, onReset]);

  const renderField = (field: QueryField) => {
    const commonProps = {
      placeholder: field.placeholder || `请输入${field.label}`,
      allowClear: field.allowClear ?? true,
      style: { width: field.width || 180 },
      ...field.props,
    };

    switch (field.type) {
      case 'input':
        return <Input {...commonProps} />;
      case 'textarea':
        return <Input.TextArea rows={2} {...commonProps} />;
      case 'select':
        return (
          <Select
            placeholder={field.placeholder || `请选择${field.label}`}
            options={field.options || []}
            {...commonProps}
          />
        );
      case 'date':
        return <DatePicker showTime {...commonProps} />;
      case 'dateRange':
        return <DatePicker.RangePicker showTime style={{ width: field.width || 300 }} {...field.props} />;
      case 'inputNumber':
        return <Input {...commonProps} type="number" />;
      default:
        return <Input {...commonProps} />;
    }
  };

  return (
    <Form
      form={form}
      layout={inline ? 'inline' : 'vertical'}
      initialValues={initialValues}
      style={{ rowGap: 12, marginBottom: inline ? 16 : 0 }}
      onFinish={handleQuery}
    >
      {fields.map((field) => (
        <Form.Item key={field.name} name={field.name} label={field.label} rules={field.rules}>
          {renderField(field)}
        </Form.Item>
      ))}
      <Form.Item>
        <Space>
          <Button
            type="primary"
            htmlType="submit"
            icon={<SearchOutlined />}
            loading={loading}
          >
            查询
          </Button>
          {showReset && (
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              重置
            </Button>
          )}
          {extra}
        </Space>
      </Form.Item>
    </Form>
  );
};

export default QueryForm;

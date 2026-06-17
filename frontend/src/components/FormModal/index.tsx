import { Modal, Form, type FormInstance, type ModalProps, type FormProps } from 'antd';
import { useState, useEffect, useImperativeHandle, forwardRef } from 'react';

export interface FormModalHandle {
  open: (initialValues?: Record<string, unknown>) => void;
  close: () => void;
  getForm: () => FormInstance;
}

export interface FormField {
  name: string;
  label: string;
  type: 'input' | 'password' | 'number' | 'select' | 'textarea' | 'date' | 'dateRange' | 'switch' | 'upload';
  required?: boolean;
  placeholder?: string;
  options?: { label: string; value: string | number | boolean }[];
  span?: number;
  rules?: unknown[];
  props?: Record<string, unknown>;
}

export interface FormSection {
  title?: React.ReactNode;
  fields: FormField[];
  columns?: number;
}

interface FormModalProps extends Omit<ModalProps, 'open' | 'onOk' | 'onCancel' | 'footer' | 'title'> {
  title: React.ReactNode;
  sections: FormSection[];
  onSubmit: (values: Record<string, unknown>) => Promise<void> | void;
  formProps?: Omit<FormProps, 'form' | 'layout'>;
  layout?: 'horizontal' | 'vertical' | 'inline';
  columns?: number;
  width?: number;
  okText?: string;
  cancelText?: string;
  showFooter?: boolean;
  initialValues?: Record<string, unknown>;
  onOpenChange?: (open: boolean) => void;
}

const FormModal = forwardRef<FormModalHandle, FormModalProps>(
  (
    {
      title,
      sections,
      onSubmit,
      formProps,
      layout = 'vertical',
      columns = 2,
      width = 640,
      okText = '确定',
      cancelText = '取消',
      showFooter = true,
      initialValues: externalInitialValues,
      onOpenChange,
      ...modalProps
    },
    ref
  ) => {
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [form] = Form.useForm();
    const [initialValues, setInitialValues] = useState<Record<string, unknown> | undefined>(
      externalInitialValues
    );

    useImperativeHandle(ref, () => ({
      open: (values?: Record<string, unknown>) => {
        if (values) {
          setInitialValues(values);
          form.setFieldsValue(values);
        } else if (externalInitialValues) {
          form.setFieldsValue(externalInitialValues);
        }
        setOpen(true);
        onOpenChange?.(true);
      },
      close: () => {
        setOpen(false);
        form.resetFields();
        onOpenChange?.(false);
      },
      getForm: () => form,
    }));

    useEffect(() => {
      if (externalInitialValues && !open) {
        setInitialValues(externalInitialValues);
      }
    }, [externalInitialValues, open]);

    const handleCancel = () => {
      setOpen(false);
      form.resetFields();
      onOpenChange?.(false);
    };

    const handleOk = async () => {
      try {
        const values = await form.validateFields();
        setLoading(true);
        await onSubmit(values);
        setOpen(false);
        form.resetFields();
        onOpenChange?.(false);
      } catch {
      } finally {
        setLoading(false);
      }
    };

    const renderField = (field: FormField) => {
      const rules = field.required
        ? [{ required: true, message: `请${field.type === 'select' ? '选择' : '输入'}${field.label}` }]
        : [];

      const commonProps = {
        placeholder: field.placeholder || `请${field.type === 'select' ? '选择' : '输入'}${field.label}`,
        allowClear: true,
        style: { width: '100%' },
        ...field.props,
      };

      let component: React.ReactNode;

      switch (field.type) {
        case 'input':
          component = <Form.Item name={field.name} label={field.label} rules={rules} {...(field as unknown as object)}>
            <Form.Item name={field.name} noStyle rules={rules}>
              {/* 这里是为了兼容 */}
            </Form.Item>
          </Form.Item>;
          break;
        default:
          break;
      }

      return component;
    };

    return (
      <Modal
        {...modalProps}
        title={title}
        open={open}
        onOk={handleOk}
        onCancel={handleCancel}
        confirmLoading={loading}
        width={width}
        destroyOnClose
        okText={okText}
        cancelText={cancelText}
        footer={
          showFooter
            ? undefined
            : null
        }
      >
        <Form
          form={form}
          layout={layout}
          initialValues={initialValues}
          {...formProps}
        >
          {sections.map((section, sIdx) => (
            <div key={sIdx}>
              {section.title && (
                <div style={{ margin: sIdx > 0 ? '16px 0 12px' : '0 0 12px', fontWeight: 600 }}>
                  {section.title}
                </div>
              )}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: `repeat(${section.columns || columns}, 1fr)`,
                  gap: '0 16px',
                }}
              >
                {section.fields.map((field) => {
                  const rules = field.required
                    ? [{ required: true, message: `请${field.type === 'select' ? '选择' : '输入'}${field.label}` }]
                    : [];

                  let component: React.ReactNode;
                  const Input = require('antd').Input;
                  const Select = require('antd').Select;
                  const InputNumber = require('antd').InputNumber;
                  const DatePicker = require('antd').DatePicker;
                  const Switch = require('antd').Switch;
                  const { TextArea } = Input;

                  const commonProps = {
                    placeholder: field.placeholder || `请${field.type === 'select' ? '选择' : '输入'}${field.label}`,
                    allowClear: true,
                    style: { width: '100%' },
                    ...field.props,
                  };

                  switch (field.type) {
                    case 'input':
                      component = <Input {...commonProps} />;
                      break;
                    case 'password':
                      component = <Input.Password {...commonProps} />;
                      break;
                    case 'number':
                      component = <InputNumber {...commonProps} />;
                      break;
                    case 'select':
                      component = (
                        <Select
                          placeholder={field.placeholder || `请选择${field.label}`}
                          options={field.options || []}
                          {...commonProps}
                        />
                      );
                      break;
                    case 'textarea':
                      component = <TextArea rows={3} {...commonProps} />;
                      break;
                    case 'date':
                      component = <DatePicker showTime {...commonProps} />;
                      break;
                    case 'dateRange':
                      component = <DatePicker.RangePicker showTime {...commonProps} />;
                      break;
                    case 'switch':
                      component = <Switch {...commonProps} />;
                      break;
                    default:
                      component = <Input {...commonProps} />;
                  }

                  return (
                    <Form.Item
                      key={field.name}
                      name={field.name}
                      label={field.label}
                      rules={rules}
                      style={{
                        gridColumn: field.span ? `span ${field.span}` : undefined,
                      }}
                    >
                      {component}
                    </Form.Item>
                  );
                })}
              </div>
            </div>
          ))}
        </Form>
      </Modal>
    );
  }
);

FormModal.displayName = 'FormModal';

export default FormModal;

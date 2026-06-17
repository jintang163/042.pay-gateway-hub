import { Drawer, Descriptions, Tag, Space, Button, Divider, Spin } from 'antd';
import type { DescriptionsProps } from 'antd';
import { useState } from 'react';
import { EditOutlined, CloseOutlined } from '@ant-design/icons';

export interface DetailField {
  label: string;
  key: string;
  value?: React.ReactNode;
  span?: number;
  render?: (value: unknown, record: Record<string, unknown>) => React.ReactNode;
  tag?: {
    color?: string;
    icon?: React.ReactNode;
    text?: string;
  };
}

export interface DetailSection {
  title?: React.ReactNode;
  fields: DetailField[];
  column?: number;
}

interface DetailDrawerProps {
  open: boolean;
  onClose: () => void;
  title?: React.ReactNode;
  width?: number | string;
  sections: DetailSection[];
  loading?: boolean;
  onEdit?: () => void;
  showEdit?: boolean;
  footer?: React.ReactNode;
  descriptionsProps?: Omit<DescriptionsProps, 'title' | 'items' | 'children' | 'column'>;
  record?: Record<string, unknown>;
  editText?: string;
}

const DetailDrawer = ({
  open,
  onClose,
  title = '详情',
  width = 720,
  sections,
  loading = false,
  onEdit,
  showEdit = false,
  footer,
  descriptionsProps = { bordered: true, size: 'small' },
  record = {},
  editText = '编辑',
}: DetailDrawerProps) => {
  const [spinTip] = useState('加载中...');

  const renderValue = (field: DetailField) => {
    const rawValue = record[field.key] ?? field.value;

    if (field.render) {
      return field.render(rawValue, record);
    }

    if (field.tag) {
      return (
        <Tag color={field.tag.color} icon={field.tag.icon}>
          {field.tag.text ?? (rawValue as string)}
        </Tag>
      );
    }

    return rawValue ?? '-';
  };

  const renderExtra = () => {
    if (!showEdit || !onEdit) return undefined;
    return (
      <Space>
        <Button icon={<EditOutlined />} onClick={onEdit}>
          {editText}
        </Button>
        <Button icon={<CloseOutlined />} onClick={onClose}>
          关闭
        </Button>
      </Space>
    );
  };

  const renderFooter = () => {
    if (footer) return footer;
    if (showEdit && onEdit) {
      return (
        <div style={{ textAlign: 'right' }}>
          <Space>
            <Button onClick={onClose}>关闭</Button>
            <Button type="primary" icon={<EditOutlined />} onClick={onEdit}>
              {editText}
            </Button>
          </Space>
        </div>
      );
    }
    return null;
  };

  return (
    <Drawer
      title={title}
      placement="right"
      width={width}
      open={open}
      onClose={onClose}
      extra={renderExtra()}
      footer={renderFooter()}
      destroyOnClose
    >
      <Spin spinning={loading} tip={spinTip}>
        {sections.map((section, sectionIndex) => (
          <div key={sectionIndex} style={{ marginBottom: sectionIndex < sections.length - 1 ? 0 : 0 }}>
            {section.title && (
              <>
                {sectionIndex > 0 && <Divider style={{ margin: '16px 0' }} />}
                <Descriptions
                  title={section.title}
                  column={section.column ?? 2}
                  {...descriptionsProps}
                  items={section.fields.map((field) => ({
                    key: field.key,
                    label: field.label,
                    children: renderValue(field),
                    span: field.span,
                  }))}
                />
              </>
            )}
            {!section.title && (
              <>
                {sectionIndex > 0 && <Divider style={{ margin: '16px 0' }} />}
                <Descriptions
                  column={section.column ?? 2}
                  {...descriptionsProps}
                  items={section.fields.map((field) => ({
                    key: field.key,
                    label: field.label,
                    children: renderValue(field),
                    span: field.span,
                  }))}
                />
              </>
            )}
          </div>
        ))}
      </Spin>
    </Drawer>
  );
};

export default DetailDrawer;

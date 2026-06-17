import { Table, type TableProps, Card, type CardProps, Space, Button, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined, DownloadOutlined, PlusOutlined } from '@ant-design/icons';
import { useState } from 'react';

interface DataTableProps<T> extends Omit<TableProps<T>, 'loading'> {
  title?: React.ReactNode;
  cardProps?: CardProps;
  loading?: boolean;
  data: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  onPageChange?: (page: number, pageSize: number) => void;
  onRefresh?: () => void;
  onExport?: () => void;
  onCreate?: () => void;
  showRefresh?: boolean;
  showExport?: boolean;
  showCreate?: boolean;
  extra?: React.ReactNode;
  createText?: string;
  exportText?: string;
  columns: ColumnsType<T>;
}

function DataTable<T extends object>({
  title,
  cardProps,
  loading = false,
  data,
  total,
  pageNum,
  pageSize,
  onPageChange,
  onRefresh,
  onExport,
  onCreate,
  showRefresh = true,
  showExport = false,
  showCreate = false,
  extra,
  createText = '新增',
  exportText = '导出',
  columns,
  ...tableProps
}: DataTableProps<T>) {
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = async () => {
    if (!onRefresh) return;
    try {
      setRefreshing(true);
      await onRefresh();
    } finally {
      setRefreshing(false);
    }
  };

  const renderExtra = () => {
    const actions: React.ReactNode[] = [];

    if (showCreate && onCreate) {
      actions.push(
        <Button key="create" type="primary" icon={<PlusOutlined />} onClick={onCreate}>
          {createText}
        </Button>
      );
    }

    if (showExport && onExport) {
      actions.push(
        <Button key="export" icon={<DownloadOutlined />} onClick={onExport}>
          {exportText}
        </Button>
      );
    }

    if (showRefresh && onRefresh) {
      actions.push(
        <Tooltip key="refresh" title="刷新">
          <Button
            icon={<ReloadOutlined spin={refreshing} />}
            onClick={handleRefresh}
          />
        </Tooltip>
      );
    }

    if (extra) {
      actions.push(extra);
    }

    return actions.length > 0 ? <Space>{actions}</Space> : undefined;
  };

  return (
    <Card title={title} extra={renderExtra()} {...cardProps}>
      <Table<T>
        {...tableProps}
        columns={columns}
        dataSource={data}
        loading={loading || refreshing}
        rowKey={(record, index) => {
          const key = (record as { id?: string }).id || (record as { key?: string }).key;
          return key ?? String(index);
        }}
        scroll={{ x: 'max-content', ...tableProps.scroll }}
        pagination={{
          current: pageNum,
          pageSize,
          total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: onPageChange,
          ...tableProps.pagination,
        }}
      />
    </Card>
  );
}

export default DataTable;

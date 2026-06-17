import ReactECharts from 'echarts-for-react';
import { Card, type CardProps, Space, Button, Dropdown } from 'antd';
import type { EChartsOption } from 'echarts';
import { useState, useRef } from 'react';
import {
  ReloadOutlined,
  DownloadOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
} from '@ant-design/icons';

interface ChartCardProps {
  title?: React.ReactNode;
  option: EChartsOption;
  height?: number | string;
  style?: React.CSSProperties;
  cardProps?: CardProps;
  extra?: React.ReactNode;
  onRefresh?: () => void;
  showRefresh?: boolean;
  showDownload?: boolean;
  showFullscreen?: boolean;
  loading?: boolean;
  notMerge?: boolean;
  lazyUpdate?: boolean;
}

const ChartCard = ({
  title,
  option,
  height = 320,
  style,
  cardProps,
  extra,
  onRefresh,
  showRefresh = true,
  showDownload = true,
  showFullscreen = true,
  loading = false,
  notMerge = false,
  lazyUpdate = false,
}: ChartCardProps) => {
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const chartRef = useRef<ReactECharts>(null);

  const handleRefresh = async () => {
    if (!onRefresh) return;
    try {
      setRefreshing(true);
      await onRefresh();
    } finally {
      setRefreshing(false);
    }
  };

  const handleDownload = (type: 'png' | 'jpg') => {
    const instance = chartRef.current?.getEchartsInstance();
    if (!instance) return;
    const url = instance.getDataURL({
      type,
      pixelRatio: 2,
      backgroundColor: '#fff',
    });
    const link = document.createElement('a');
    link.download = `chart-${Date.now()}.${type}`;
    link.href = url;
    link.click();
  };

  const downloadMenu = {
    items: [
      { key: 'png', label: '下载为 PNG' },
      { key: 'jpg', label: '下载为 JPG' },
    ],
    onClick: ({ key }: { key: string }) => {
      handleDownload(key as 'png' | 'jpg');
    },
  };

  const renderExtra = () => {
    const actions: React.ReactNode[] = [];

    if (extra) {
      actions.push(extra);
    }

    if (showRefresh && onRefresh) {
      actions.push(
        <Button
          key="refresh"
          icon={<ReloadOutlined spin={refreshing} />}
          onClick={handleRefresh}
        />
      );
    }

    if (showDownload) {
      actions.push(
        <Dropdown key="download" menu={downloadMenu} placement="bottomRight" trigger={['click']}>
          <Button icon={<DownloadOutlined />} />
        </Dropdown>
      );
    }

    if (showFullscreen) {
      actions.push(
        <Button
          key="fullscreen"
          icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
          onClick={() => setIsFullscreen(!isFullscreen)}
        />
      );
    }

    return actions.length > 0 ? <Space>{actions}</Space> : undefined;
  };

  const containerStyle: React.CSSProperties = isFullscreen
    ? {
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 9999,
        background: '#fff',
        margin: 0,
        borderRadius: 0,
        display: 'flex',
        flexDirection: 'column',
      }
    : {};

  const chartStyle: React.CSSProperties = isFullscreen
    ? { flex: 1, height: 'auto' }
    : { height, ...style };

  return (
    <Card title={title} extra={renderExtra()} style={containerStyle} {...cardProps}>
      <ReactECharts
        ref={chartRef}
        option={option}
        style={chartStyle}
        notMerge={notMerge}
        lazyUpdate={lazyUpdate}
        showLoading={loading || refreshing}
      />
    </Card>
  );
};

export default ChartCard;

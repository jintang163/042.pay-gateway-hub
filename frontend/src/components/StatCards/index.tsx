import { Statistic, Card, Row, Col, type RowProps } from 'antd';
import type { StatisticProps } from 'antd';
import type { ReactNode } from 'react';

export interface StatCardItem extends StatisticProps {
  key?: string;
  span?: number;
  cardStyle?: React.CSSProperties;
}

interface StatCardsProps {
  items: StatCardItem[];
  gutter?: RowProps['gutter'];
  cols?: { xs?: number; sm?: number; md?: number; lg?: number; xl?: number };
}

const StatCards = ({
  items,
  gutter = [16, 16],
  cols = { xs: 24, sm: 12, lg: 6 },
}: StatCardsProps) => {
  return (
    <Row gutter={gutter} style={{ marginBottom: 16 }}>
      {items.map((item, index) => {
        const { key, span, cardStyle, ...statisticProps } = item;
        const itemKey = key || String(index);
        const spanValue = span || cols.xs;
        return (
          <Col key={itemKey} xs={cols.xs} sm={cols.sm} md={cols.md} lg={cols.lg} xl={cols.xl} span={spanValue}>
            <Card style={cardStyle}>
              <Statistic {...statisticProps} />
            </Card>
          </Col>
        );
      })}
    </Row>
  );
};

export default StatCards;

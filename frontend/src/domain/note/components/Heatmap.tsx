import { useMemo } from 'react';
import { Tooltip } from 'antd';
import dayjs from 'dayjs';
import type { HeatmapDay } from '@/domain/note/types';
import { buildHeatmapGrid, heatLevel, HEATMAP_COLORS } from '@/utils/heatmap';

interface Props {
  year: number;
  days: HeatmapDay[];
}

const CELL = 12;
const GAP = 3;
const CELL_STEP = CELL + GAP;
const LEFT_LABEL_WIDTH = 28;

/** GitHub 风格刷题热力图: 纯 CSS 网格, 不引入图表库 */
export default function Heatmap({ year, days }: Props) {
  const { weeks, monthLabels } = useMemo(() => buildHeatmapGrid(year, days), [year, days]);
  const gridWidth = weeks.length * CELL_STEP;

  return (
    <div style={{ overflowX: 'auto', paddingBottom: 4 }}>
      <div style={{ minWidth: gridWidth + LEFT_LABEL_WIDTH }}>
        <div
          style={{
            position: 'relative',
            height: 16,
            marginLeft: LEFT_LABEL_WIDTH,
            width: gridWidth,
          }}
        >
          {monthLabels.map((month) => (
            <span
              key={month.label}
              style={{
                position: 'absolute',
                left: month.weekIndex * CELL_STEP,
                fontSize: 11,
                color: '#8c8c8c',
                lineHeight: '16px',
              }}
            >
              {month.label}
            </span>
          ))}
        </div>

        <div style={{ display: 'flex' }}>
          <div
            style={{
              width: LEFT_LABEL_WIDTH,
              display: 'grid',
              gridTemplateRows: `repeat(7, ${CELL}px)`,
              rowGap: GAP,
              marginTop: 0,
            }}
          >
            {['日', '', '一', '', '三', '', '五'].map((label, index) => (
              <div
                key={index}
                style={{ height: CELL, lineHeight: `${CELL}px`, fontSize: 10, color: '#8c8c8c' }}
              >
                {label}
              </div>
            ))}
          </div>

          <div
            role="img"
            aria-label={`${year} 年刷题热力图, 共 ${days.reduce((sum, d) => sum + d.count, 0)} 篇笔记`}
            style={{
              display: 'grid',
              gridAutoFlow: 'column',
              gridTemplateRows: `repeat(7, ${CELL}px)`,
              gridAutoColumns: `${CELL}px`,
              gap: GAP,
            }}
          >
            {weeks.flat().map((cell, index) =>
              cell.date ? (
                <Tooltip
                  key={cell.date}
                  title={`${dayjs(cell.date).format('YYYY-MM-DD')} · ${cell.count} 篇笔记`}
                >
                  <div
                    aria-label={`${cell.date} ${cell.count}篇`}
                    data-level={heatLevel(cell.count)}
                    style={{
                      width: CELL,
                      height: CELL,
                      borderRadius: 2,
                      background: HEATMAP_COLORS[heatLevel(cell.count)],
                      opacity: cell.inYear ? 1 : 0.25,
                    }}
                  />
                </Tooltip>
              ) : (
                <div key={`pad-${index}`} style={{ width: CELL, height: CELL }} />
              ),
            )}
          </div>
        </div>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: 4,
            marginTop: 10,
            fontSize: 11,
            color: '#8c8c8c',
          }}
        >
          <span>少</span>
          {HEATMAP_COLORS.map((color) => (
            <span
              key={color}
              style={{ width: CELL, height: CELL, borderRadius: 2, background: color }}
            />
          ))}
          <span>多</span>
        </div>
      </div>
    </div>
  );
}

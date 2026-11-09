import { Tag } from 'antd';

const DIFFICULTY_TEXT: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' };
const DIFFICULTY_COLOR: Record<number, string> = {
  1: 'green',
  2: 'orange',
  3: 'red',
};

/** 难度标签: 1简单/2中等/3困难 */
export default function DifficultyTag({ difficulty }: { difficulty: number }) {
  return (
    <Tag color={DIFFICULTY_COLOR[difficulty] ?? 'default'} style={{ marginInlineEnd: 0 }}>
      {DIFFICULTY_TEXT[difficulty] ?? '未知'}
    </Tag>
  );
}

import { Drawer } from 'antd';
import CommentsPanel from './CommentsPanel';

interface Props {
  noteId: number | null;
  onClose: () => void;
  /** 评论增删后触发; 调用方只刷新当前笔记或列表, 不要卸载抽屉 */
  onCountChange?: () => void;
}

/** 笔记评论抽屉: 列表页/题目页共用 */
export default function NoteCommentsDrawer({ noteId, onClose, onCountChange }: Props) {
  return (
    <Drawer
      open={noteId != null}
      onClose={onClose}
      title="评论"
      width={560}
      destroyOnHidden
    >
      {noteId != null && <CommentsPanel noteId={noteId} onCountChange={onCountChange} />}
    </Drawer>
  );
}

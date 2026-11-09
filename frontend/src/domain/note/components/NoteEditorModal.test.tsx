import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/userSlice';
import NoteEditorModal from './NoteEditorModal';

vi.mock('@/domain/note/api', () => ({
  createNote: vi.fn(),
  updateNote: vi.fn(),
}));
vi.mock('@/domain/tag/api', () => ({
  getTags: vi.fn(),
}));

import { createNote, updateNote } from '@/domain/note/api';
import { getTags } from '@/domain/tag/api';

function renderModal(props: Partial<React.ComponentProps<typeof NoteEditorModal>> = {}) {
  const onSaved = vi.fn();
  const onClose = vi.fn();
  const store = configureStore({ reducer: { user: userReducer } });
  render(
    <Provider store={store}>
      <ConfigProvider locale={zhCN}>
        <AntApp>
          <NoteEditorModal
            open
            questionId={2}
            onSaved={onSaved}
            onClose={onClose}
            {...props}
          />
        </AntApp>
      </ConfigProvider>
    </Provider>,
  );
  return { onSaved, onClose };
}

describe('NoteEditorModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(createNote).mockResolvedValue({ noteId: 99 });
    vi.mocked(updateNote).mockResolvedValue(undefined);
    vi.mocked(getTags).mockResolvedValue([]);
  });

  it('新建: 内容为空时拦截, 填写后调用 createNote', async () => {
    const user = userEvent.setup();
    const { onSaved } = renderModal();

    await user.click(screen.getByRole('button', { name: '发布笔记' }));
    expect(createNote).not.toHaveBeenCalled();

    await user.type(screen.getByLabelText('笔记内容'), '# 哈希表题解');
    await user.click(screen.getByRole('button', { name: '发布笔记' }));

    await waitFor(() => expect(createNote).toHaveBeenCalledWith({ questionId: 2, content: '# 哈希表题解' }));
    expect(onSaved).toHaveBeenCalled();
  });

  it('新建: 预填题目标签并在保存时携带', async () => {
    const user = userEvent.setup();
    renderModal({ initialTags: ['链表', '递归'] });

    await user.type(screen.getByLabelText('笔记内容'), '# 链表题解');
    await user.click(screen.getByRole('button', { name: '发布笔记' }));

    await waitFor(() =>
      expect(createNote).toHaveBeenCalledWith({
        questionId: 2,
        content: '# 链表题解',
        tags: ['链表', '递归'],
      }),
    );
  });

  it('编辑: 有 noteId 时调用 updateNote 且按钮文案为保存修改', async () => {
    const user = userEvent.setup();
    renderModal({ noteId: 7, initialContent: '旧内容' });

    expect(screen.getByRole('button', { name: '保存修改' })).toBeInTheDocument();
    await user.clear(screen.getByLabelText('笔记内容'));
    await user.type(screen.getByLabelText('笔记内容'), '新内容');
    await user.click(screen.getByRole('button', { name: '保存修改' }));

    await waitFor(() => expect(updateNote).toHaveBeenCalledWith(7, '新内容', []));
    expect(createNote).not.toHaveBeenCalled();
  });
});

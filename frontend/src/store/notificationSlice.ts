import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { UnreadCount } from '@/domain/note/types';

interface NotificationState {
  /** 未读消息计数(与 GET /api/messages/unread/count 同构) */
  unread: UnreadCount;
  /** WebSocket 是否已连接 */
  connected: boolean;
}

const emptyUnread: UnreadCount = { total: 0, like: 0, comment: 0, system: 0 };

const initialState: NotificationState = {
  unread: { ...emptyUnread },
  connected: false,
};

/** I3: WebSocket 推送 + 页面操作共同维护未读角标 */
const notificationSlice = createSlice({
  name: 'notification',
  initialState,
  reducers: {
    setUnread(state, action: PayloadAction<UnreadCount>) {
      state.unread = action.payload;
    },
    setConnected(state, action: PayloadAction<boolean>) {
      state.connected = action.payload;
    },
    resetNotification(state) {
      state.unread = { ...emptyUnread };
      state.connected = false;
    },
  },
});

export const { setUnread, setConnected, resetNotification } = notificationSlice.actions;
export default notificationSlice.reducer;

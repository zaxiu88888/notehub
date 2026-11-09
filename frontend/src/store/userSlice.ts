import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import * as userApi from '@/domain/user/api';
import type { LoginBody, RegisterBody, UserVO } from '@/domain/user/types';
import { getToken, setToken } from '@/request/http';

interface UserState {
  token: string | null;
  user: UserVO | null;
  /** 登录/注册/拉取资料进行中 */
  loading: boolean;
}

const initialState: UserState = {
  token: getToken(),
  user: null,
  loading: false,
};

export const loginThunk = createAsyncThunk(
  'user/login',
  async (body: LoginBody) => {
    const auth = await userApi.login(body);
    setToken(auth.token);
    return auth;
  },
);

export const registerThunk = createAsyncThunk(
  'user/register',
  async (body: RegisterBody) => {
    const auth = await userApi.register(body);
    setToken(auth.token);
    return auth;
  },
);

export const fetchWhoamiThunk = createAsyncThunk('user/whoami', () => userApi.whoami());

const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    logout(state) {
      state.token = null;
      state.user = null;
      setToken(null);
    },
    /** 本地会话失效(收到 40101) */
    clearAuth(state) {
      state.token = null;
      state.user = null;
      setToken(null);
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginThunk.pending, (state) => {
        state.loading = true;
      })
      .addCase(loginThunk.fulfilled, (state, action) => {
        state.loading = false;
        state.token = action.payload.token;
      })
      .addCase(loginThunk.rejected, (state) => {
        state.loading = false;
      })
      .addCase(registerThunk.pending, (state) => {
        state.loading = true;
      })
      .addCase(registerThunk.fulfilled, (state, action) => {
        state.loading = false;
        state.token = action.payload.token;
      })
      .addCase(registerThunk.rejected, (state) => {
        state.loading = false;
      })
      .addCase(fetchWhoamiThunk.fulfilled, (state, action) => {
        state.user = action.payload;
      })
      .addCase(fetchWhoamiThunk.rejected, (state) => {
        // token 失效: 清理本地会话
        state.token = null;
        state.user = null;
        setToken(null);
      });
  },
});

export const { logout, clearAuth } = userSlice.actions;
export default userSlice.reducer;

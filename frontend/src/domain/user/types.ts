/** 用户领域类型 */
export interface UserVO {
  userId: number;
  account: string | null;
  username: string;
  email: string | null;
  gender: number;
  birthday: string | null;
  avatarUrl: string | null;
  school: string | null;
  signature: string | null;
  isAdmin: number;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface LoginUserVO {
  userId: number;
  token: string;
}

export interface LoginBody {
  account: string;
  password: string;
}

export interface RegisterBody {
  email: string;
  code: string;
  account: string;
  username: string;
  password: string;
}

export interface UpdateUserBody {
  username?: string;
  gender?: number;
  birthday?: string;
  school?: string;
  signature?: string;
}

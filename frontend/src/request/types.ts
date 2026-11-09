/** 后端统一响应体 (docs/api-contract.md) */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

/** 分页响应 data */
export interface PageData<T> {
  list: T[];
  page: number;
  pageSize: number;
  total: number;
}

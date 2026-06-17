export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
  requestId?: string;
}

export interface PageParams {
  pageNum: number;
  pageSize: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

export interface SortParams {
  field?: string;
  order?: 'asc' | 'desc';
}

export interface DateRangeParams {
  startTime?: string;
  endTime?: string;
}

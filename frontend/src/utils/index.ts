import dayjs from 'dayjs';

export const formatAmount = (amount: number, currency: string = 'CNY'): string => {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount);
};

export const formatPercent = (value: number, fractionDigits: number = 2): string => {
  return `${(value * 100).toFixed(fractionDigits)}%`;
};

export const formatNumber = (value: number, fractionDigits: number = 0): string => {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value);
};

export const formatDate = (value: string | number | Date, format: string = 'YYYY-MM-DD'): string => {
  if (!value) return '-';
  return dayjs(value).format(format);
};

export const formatDateTime = (value: string | number | Date, format: string = 'YYYY-MM-DD HH:mm:ss'): string => {
  if (!value) return '-';
  return dayjs(value).format(format);
};

export const formatRelativeTime = (value: string | number | Date): string => {
  if (!value) return '-';
  return dayjs(value).fromNow();
};

export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`;
};

export const parseQueryString = (query: string): Record<string, string> => {
  const result: Record<string, string> = {};
  if (!query) return result;
  query.replace(/^[?#]/, '').split('&').forEach((param) => {
    const [key, value] = param.split('=');
    if (key) {
      result[decodeURIComponent(key)] = decodeURIComponent(value || '');
    }
  });
  return result;
};

export const buildQueryString = (params: Record<string, unknown>): string => {
  const searchParams = new URLSearchParams();
  Object.keys(params).forEach((key) => {
    const value = params[key];
    if (value !== undefined && value !== null && value !== '') {
      searchParams.append(key, String(value));
    }
  });
  const result = searchParams.toString();
  return result ? `?${result}` : '';
};

export const uuid = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

export const debounce = <T extends (...args: unknown[]) => unknown>(
  func: T,
  wait: number = 300
): ((...args: Parameters<T>) => void) => {
  let timeout: ReturnType<typeof setTimeout> | null = null;
  return (...args: Parameters<T>) => {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
};

export const throttle = <T extends (...args: unknown[]) => unknown>(
  func: T,
  wait: number = 300
): ((...args: Parameters<T>) => void) => {
  let last = 0;
  return (...args: Parameters<T>) => {
    const now = Date.now();
    if (now - last >= wait) {
      last = now;
      func(...args);
    }
  };
};

export const deepClone = <T>(obj: T): T => {
  if (obj === null || typeof obj !== 'object') return obj;
  if (obj instanceof Date) return new Date(obj.getTime()) as unknown as T;
  if (obj instanceof Array) return obj.map((item) => deepClone(item)) as unknown as T;
  if (obj instanceof Object) {
    const result = {} as T;
    Object.keys(obj).forEach((key) => {
      (result as Record<string, unknown>)[key] = deepClone((obj as Record<string, unknown>)[key]);
    });
    return result;
  }
  return obj;
};

export const downloadFile = (blob: Blob, filename: string): void => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

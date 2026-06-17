const PREFIX = 'pay_gateway_hub_';

export const storage = {
  set(key: string, value: unknown, expires?: number): void {
    const prefixedKey = PREFIX + key;
    const data = {
      value,
      timestamp: Date.now(),
      expires: expires ? Date.now() + expires * 1000 : null,
    };
    try {
      localStorage.setItem(prefixedKey, JSON.stringify(data));
    } catch (e) {
      console.error('Storage set error:', e);
    }
  },

  get<T = unknown>(key: string, defaultValue?: T): T | null {
    const prefixedKey = PREFIX + key;
    try {
      const item = localStorage.getItem(prefixedKey);
      if (!item) return defaultValue ?? null;
      const data = JSON.parse(item) as { value: T; expires: number | null };
      if (data.expires && Date.now() > data.expires) {
        this.remove(key);
        return defaultValue ?? null;
      }
      return data.value;
    } catch {
      return defaultValue ?? null;
    }
  },

  remove(key: string): void {
    const prefixedKey = PREFIX + key;
    try {
      localStorage.removeItem(prefixedKey);
    } catch (e) {
      console.error('Storage remove error:', e);
    }
  },

  clear(): void {
    try {
      Object.keys(localStorage).forEach((key) => {
        if (key.startsWith(PREFIX)) {
          localStorage.removeItem(key);
        }
      });
    } catch (e) {
      console.error('Storage clear error:', e);
    }
  },

  has(key: string): boolean {
    const prefixedKey = PREFIX + key;
    return localStorage.getItem(prefixedKey) !== null;
  },
};

export const sessionStorage = {
  set(key: string, value: unknown): void {
    const prefixedKey = PREFIX + key;
    try {
      sessionStorage.setItem(prefixedKey, JSON.stringify(value));
    } catch (e) {
      console.error('SessionStorage set error:', e);
    }
  },

  get<T = unknown>(key: string, defaultValue?: T): T | null {
    const prefixedKey = PREFIX + key;
    try {
      const item = sessionStorage.getItem(prefixedKey);
      return item ? (JSON.parse(item) as T) : defaultValue ?? null;
    } catch {
      return defaultValue ?? null;
    }
  },

  remove(key: string): void {
    const prefixedKey = PREFIX + key;
    try {
      sessionStorage.removeItem(prefixedKey);
    } catch (e) {
      console.error('SessionStorage remove error:', e);
    }
  },

  clear(): void {
    try {
      Object.keys(sessionStorage).forEach((key) => {
        if (key.startsWith(PREFIX)) {
          sessionStorage.removeItem(key);
        }
      });
    } catch (e) {
      console.error('SessionStorage clear error:', e);
    }
  },
};

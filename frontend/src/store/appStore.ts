import { create } from 'zustand';
import type { TabItem } from '@/types/router';
import { sessionStorage } from '@/utils/storage';

interface AppState {
  collapsed: boolean;
  theme: 'light' | 'dark';
  tabs: TabItem[];
  activeTabKey: string;
  sandboxMode: boolean;

  toggleCollapsed: () => void;
  setCollapsed: (collapsed: boolean) => void;
  toggleTheme: () => void;
  setTheme: (theme: 'light' | 'dark') => void;
  addTab: (tab: TabItem) => void;
  removeTab: (key: string) => void;
  setActiveTab: (key: string) => void;
  clearTabs: () => void;
  closeOtherTabs: (key: string) => void;
  toggleSandboxMode: () => void;
  setSandboxMode: (enabled: boolean) => void;
}

const initialCollapsed = sessionStorage.get<boolean>('sidebar_collapsed', false);
const initialTheme = sessionStorage.get<'light' | 'dark'>('app_theme', 'light');
const initialTabs = sessionStorage.get<TabItem[]>('app_tabs', [{ key: '/dashboard', label: '仪表盘', closable: false }]);
const initialActiveTab = sessionStorage.get<string>('active_tab', '/dashboard');
const initialSandboxMode = sessionStorage.get<boolean>('sandbox_mode', false);

export const useAppStore = create<AppState>((set, get) => ({
  collapsed: initialCollapsed,
  theme: initialTheme,
  tabs: initialTabs,
  activeTabKey: initialActiveTab,
  sandboxMode: initialSandboxMode,

  toggleCollapsed: () => {
    const collapsed = !get().collapsed;
    sessionStorage.set('sidebar_collapsed', collapsed);
    set({ collapsed });
  },

  setCollapsed: (collapsed: boolean) => {
    sessionStorage.set('sidebar_collapsed', collapsed);
    set({ collapsed });
  },

  toggleTheme: () => {
    const theme = get().theme === 'light' ? 'dark' : 'light';
    sessionStorage.set('app_theme', theme);
    set({ theme });
  },

  setTheme: (theme: 'light' | 'dark') => {
    sessionStorage.set('app_theme', theme);
    set({ theme });
  },

  addTab: (tab: TabItem) => {
    set((state) => {
      const exists = state.tabs.some((t) => t.key === tab.key);
      const newTabs = exists ? state.tabs : [...state.tabs, tab];
      sessionStorage.set('app_tabs', newTabs);
      sessionStorage.set('active_tab', tab.key);
      return { tabs: newTabs, activeTabKey: tab.key };
    });
  },

  removeTab: (key: string) => {
    set((state) => {
      const targetIndex = state.tabs.findIndex((t) => t.key === key);
      if (targetIndex === -1) return state;

      const newTabs = state.tabs.filter((t) => t.key !== key);
      let newActiveKey = state.activeTabKey;

      if (state.activeTabKey === key && newTabs.length > 0) {
        newActiveKey = newTabs[Math.min(targetIndex, newTabs.length - 1)].key;
      } else if (newTabs.length === 0) {
        newActiveKey = '/dashboard';
        newTabs.push({ key: '/dashboard', label: '仪表盘', closable: false });
      }

      sessionStorage.set('app_tabs', newTabs);
      sessionStorage.set('active_tab', newActiveKey);
      return { tabs: newTabs, activeTabKey: newActiveKey };
    });
  },

  setActiveTab: (key: string) => {
    sessionStorage.set('active_tab', key);
    set({ activeTabKey: key });
  },

  clearTabs: () => {
    const defaultTabs = [{ key: '/dashboard', label: '仪表盘', closable: false }];
    sessionStorage.set('app_tabs', defaultTabs);
    sessionStorage.set('active_tab', '/dashboard');
    set({ tabs: defaultTabs, activeTabKey: '/dashboard' });
  },

  closeOtherTabs: (key: string) => {
    set((state) => {
      const targetTab = state.tabs.find((t) => t.key === key);
      const newTabs = [
        { key: '/dashboard', label: '仪表盘', closable: false },
        ...(targetTab && key !== '/dashboard' ? [targetTab] : []),
      ];
      sessionStorage.set('app_tabs', newTabs);
      sessionStorage.set('active_tab', key);
      return { tabs: newTabs, activeTabKey: key };
    });
  },

  toggleSandboxMode: () => {
    const sandboxMode = !get().sandboxMode;
    sessionStorage.set('sandbox_mode', sandboxMode);
    set({ sandboxMode });
  },

  setSandboxMode: (enabled: boolean) => {
    sessionStorage.set('sandbox_mode', enabled);
    set({ sandboxMode: enabled });
  },
}));

export interface RouteConfig {
  path: string;
  name?: string;
  icon?: string;
  element: React.ReactNode;
  children?: RouteConfig[];
  hidden?: boolean;
  roles?: string[];
}

export interface TabItem {
  key: string;
  label: string;
  closable?: boolean;
}

import { create } from 'zustand';
import type { User, LoginRequest, LoginResponse } from '@/types/user';
import { storage } from '@/utils/storage';

interface UserState {
  user: User | null;
  token: string | null;
  isLoggedIn: boolean;
  permissions: string[];

  login: (data: LoginResponse) => void;
  logout: () => void;
  setUser: (user: Partial<User>) => void;
  setPermissions: (permissions: string[]) => void;
  checkAuth: () => boolean;
}

const initialToken = storage.get<string>('access_token');
const initialUser = storage.get<User>('user_info');

export const useUserStore = create<UserState>((set, get) => ({
  user: initialUser,
  token: initialToken,
  isLoggedIn: !!initialToken && !!initialUser,
  permissions: [],

  login: (data: LoginResponse) => {
    storage.set('access_token', data.token);
    if (data.refreshToken) {
      storage.set('refresh_token', data.refreshToken);
    }
    storage.set('user_info', data.user);
    storage.set('token_expires', data.expiresIn, data.expiresIn);

    set({
      user: data.user,
      token: data.token,
      isLoggedIn: true,
    });
  },

  logout: () => {
    storage.remove('access_token');
    storage.remove('refresh_token');
    storage.remove('user_info');
    storage.remove('token_expires');
    storage.remove('permissions');

    set({
      user: null,
      token: null,
      isLoggedIn: false,
      permissions: [],
    });
  },

  setUser: (user: Partial<User>) => {
    set((state) => {
      const updatedUser = state.user ? { ...state.user, ...user } : (user as User);
      storage.set('user_info', updatedUser);
      return { user: updatedUser };
    });
  },

  setPermissions: (permissions: string[]) => {
    storage.set('permissions', permissions);
    set({ permissions });
  },

  checkAuth: () => {
    const { token, user } = get();
    if (!token || !user) return false;
    const expires = storage.get<number>('token_expires');
    if (expires && Date.now() > expires) {
      get().logout();
      return false;
    }
    return true;
  },
}));

export const useAuthStore = useUserStore;

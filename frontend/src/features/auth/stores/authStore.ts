import { create } from "zustand";
import { apiClient } from "@/lib/axios";

interface User {
  publicId: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  fetchProfile: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,

  login: async (email, password) => {
    const response = await apiClient.post("/v1/auth/login", {
      email,
      password,
    });
    const { accessToken, refreshToken } = response.data;
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", refreshToken);
    set({ isAuthenticated: true });
    await useAuthStore.getState().fetchProfile();
  },

  logout: async () => {
    const refreshToken = localStorage.getItem("refreshToken");
    try {
      if (refreshToken)
        await apiClient.post("/v1/auth/logout", { refreshToken });
    } finally {
      localStorage.clear();
      set({ user: null, isAuthenticated: false });
      window.location.href = "/login";
    }
  },

  fetchProfile: async () => {
    try {
      set({ isLoading: true });
      const response = await apiClient.get("/v1/users/me");
      set({ user: response.data, isAuthenticated: true, isLoading: false });
    } catch {
      localStorage.clear();
      set({ user: null, isAuthenticated: false, isLoading: false });
    }
  },
}));

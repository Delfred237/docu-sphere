import { apiClient } from "@/lib/axios";
import type {
  LoginInput,
  RegisterInput,
  VerifyEmailInput,
} from "../schemas/auth.schemas";

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  errors?: Array<{
    field: string;
    message: string;
    rejectedValue: unknown;
  }>;
}

export const authService = {
  async login(data: LoginInput): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>("/v1/auth/login", data);
    return response.data;
  },

  async register(data: Omit<RegisterInput, "confirmPassword">): Promise<void> {
    await apiClient.post("/v1/auth/register", {
      firstName: data.firstName,
      lastName: data.lastName,
      email: data.email,
      password: data.password,
    });
  },

  async verifyEmail(data: VerifyEmailInput): Promise<void> {
    await apiClient.post("/v1/auth/verify-email", data);
  },

  async resendVerification(email: string): Promise<void> {
    await apiClient.post("/v1/auth/resend-verification", { email });
  },

  async forgotPassword(email: string): Promise<void> {
    await apiClient.post("/v1/auth/forgot-password", { email });
  },

  async resetPassword(
    email: string,
    code: string,
    newPassword: string,
  ): Promise<void> {
    await apiClient.post("/v1/auth/reset-password", {
      email,
      code,
      newPassword,
    });
  },

  async refresh(refreshToken: string): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>("/v1/auth/refresh", {
      refreshToken,
    });
    return response.data;
  },

  async logout(refreshToken: string): Promise<void> {
    await apiClient.post("/v1/auth/logout", { refreshToken });
  },
};

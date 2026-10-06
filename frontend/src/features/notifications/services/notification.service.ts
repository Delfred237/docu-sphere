import { apiClient } from "@/lib/axios";

export type NotificationType =
  | "DOCUMENT_SUBMITTED"
  | "DOCUMENT_APPROVED"
  | "DOCUMENT_REJECTED"
  | "DOCUMENT_SHARED"
  | "SYSTEM";

export interface Notification {
  publicId: string;
  type: NotificationType;
  title: string;
  message: string;
  targetUrl?: string;
  targetId?: string;
  read: boolean;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export const notificationService = {
  async getNotifications(
    unreadOnly = false,
    page = 0,
    size = 20,
  ): Promise<Page<Notification>> {
    const response = await apiClient.get<Page<Notification>>(
      "/v1/notifications",
      {
        params: { unreadOnly, page, size, sort: "createdAt,desc" },
      },
    );
    return response.data;
  },

  async getUnreadCount(): Promise<number> {
    const response = await apiClient.get<number>(
      "/v1/notifications/unread-count",
    );
    return response.data;
  },

  async markAsRead(publicId: string): Promise<void> {
    await apiClient.post(`/v1/notifications/${publicId}/read`);
  },

  async markAllAsRead(): Promise<void> {
    await apiClient.post("/v1/notifications/read-all");
  },

  async deleteNotification(publicId: string): Promise<void> {
    await apiClient.delete(`/v1/notifications/${publicId}`);
  },
};

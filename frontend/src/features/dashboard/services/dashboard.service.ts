import { apiClient } from "@/lib/axios";

export interface DashboardOverview {
  totalDocuments: number;
  totalFolders: number;
  pendingReviews: number;
  sharedLinks: number;
  storageUsedBytes: number;
}

export interface DailyActivity {
  date: string;
  uploads: number;
  approvals: number;
}

export interface RecentDocument {
  publicId: string;
  name: string;
  mimeType: string;
  size: number;
  status: string;
  uploadedAt: string;
  ownerName: string;
}

export interface DashboardData {
  overview: DashboardOverview;
  activity: DailyActivity[];
  byStatus: Record<string, number>;
  byType: Record<string, number>;
  recentDocuments: RecentDocument[];
}

export const dashboardService = {
  async getDashboard(): Promise<DashboardData> {
    const response = await apiClient.get<DashboardData>("/v1/dashboard");
    return response.data;
  },
};

import { useEffect, useState } from "react";
import { FileText, FolderTree, Clock, Share2, HardDrive } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { StatCard } from "../components/StatCard";
import { ActivityChart } from "../components/ActivityChart";
import { StatusChart } from "../components/StatusChart";
import { TypeChart } from "../components/TypeChart";
import { RecentDocuments } from "../components/RecentDocuments";
import {
  dashboardService,
  type DashboardData,
} from "../services/dashboard.service";

function formatBytes(bytes: number): string {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${Number.parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
}

export function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const dashboardData = await dashboardService.getDashboard();
        setData(dashboardData);
      } catch (error) {
        console.error("Failed to fetch dashboard data", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, []);

  if (isLoading) {
    return (
      <div className="p-6 space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Skeleton className="h-80" />
          <Skeleton className="h-80" />
        </div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="p-6 text-center text-slate-500">
        Failed to load dashboard data
      </div>
    );
  }

  const { overview, activity, byStatus, byType, recentDocuments } = data;

  return (
    <div className="p-6 space-y-6 mx-auto">
      {/* Page header */}
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Dashboard</h1>
        <p className="text-sm text-slate-500 mt-1">
          Overview of your document workspace
        </p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Documents"
          value={overview.totalDocuments}
          icon={FileText}
          iconBgColor="bg-blue-50"
          iconColor="text-blue-600"
        />
        <StatCard
          title="Folders"
          value={overview.totalFolders}
          icon={FolderTree}
          iconBgColor="bg-purple-50"
          iconColor="text-purple-600"
        />
        <StatCard
          title="Pending Review"
          value={overview.pendingReviews}
          icon={Clock}
          iconBgColor="bg-amber-50"
          iconColor="text-amber-600"
        />
        <StatCard
          title="Shared Links"
          value={overview.sharedLinks}
          icon={Share2}
          iconBgColor="bg-green-50"
          iconColor="text-green-600"
        />
      </div>

      {/* Storage indicator */}
      <Card className="bg-white border-slate-200 shadow-xs">
        <CardContent className="p-4 flex items-center gap-4">
          <div className="h-10 w-10 rounded-lg bg-slate-100 flex items-center justify-center">
            <HardDrive className="h-5 w-5 text-slate-600" />
          </div>
          <div className="flex-1">
            <div className="flex items-center justify-between mb-1">
              <span className="text-sm font-medium text-slate-900">
                Storage used
              </span>
              <span className="text-sm text-slate-600">
                {formatBytes(overview.storageUsedBytes)}
              </span>
            </div>
            <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full bg-primary-600 rounded-full transition-all"
                style={{
                  width: `${Math.min((overview.storageUsedBytes / (5 * 1024 * 1024 * 1024)) * 100, 100)}%`,
                }}
              />
            </div>
            <p className="text-xs text-slate-500 mt-1">of 5 GB available</p>
          </div>
        </CardContent>
      </Card>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Activity chart */}
        <Card className="bg-white border-slate-200 shadow-xs">
          <CardHeader className="pb-2">
            <CardTitle className="text-base font-semibold text-slate-900">
              Upload Activity
            </CardTitle>
            <p className="text-sm text-slate-500">Last 14 days</p>
          </CardHeader>
          <CardContent>
            <ActivityChart data={activity} />
          </CardContent>
        </Card>

        {/* Status distribution */}
        <Card className="bg-white border-slate-200 shadow-xs">
          <CardHeader className="pb-2">
            <CardTitle className="text-base font-semibold text-slate-900">
              Documents by Status
            </CardTitle>
            <p className="text-sm text-slate-500">Current distribution</p>
          </CardHeader>
          <CardContent>
            <StatusChart data={byStatus} />
          </CardContent>
        </Card>
      </div>

      {/* Bottom row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Type distribution */}
        <Card className="bg-white border-slate-200 shadow-xs lg:col-span-1">
          <CardHeader className="pb-2">
            <CardTitle className="text-base font-semibold text-slate-900">
              By File Type
            </CardTitle>
          </CardHeader>
          <CardContent>
            <TypeChart data={byType} />
          </CardContent>
        </Card>

        {/* Recent documents */}
        <Card className="bg-white border-slate-200 shadow-xs lg:col-span-2">
          <CardHeader className="pb-2">
            <CardTitle className="text-base font-semibold text-slate-900">
              Recent Documents
            </CardTitle>
          </CardHeader>
          <CardContent>
            <RecentDocuments documents={recentDocuments} />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

import type { LucideIcon } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';

interface StatCardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  trend?: {
    value: number;
    isPositive: boolean;
  };
  iconBgColor?: string;
  iconColor?: string;
}

export function StatCard({
  title,
  value,
  icon: Icon,
  trend,
  iconBgColor = 'bg-primary-50',
  iconColor = 'text-primary-600',
}: Readonly<StatCardProps>) {
  return (
    <Card className="bg-white border-slate-200 shadow-xs">
      <CardContent className="p-5">
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-slate-500 truncate">{title}</p>
            <p className="text-2xl font-semibold text-slate-900 mt-1">{value}</p>
            {trend && (
              <p className={`text-xs mt-1 ${trend.isPositive ? 'text-green-600' : 'text-red-600'}`}>
                {trend.isPositive ? '↑' : '↓'} {Math.abs(trend.value)}% vs last month
              </p>
            )}
          </div>
          <div className={`h-10 w-10 rounded-lg flex items-center justify-center shrink-0 ${iconBgColor}`}>
            <Icon className={`h-5 w-5 ${iconColor}`} />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
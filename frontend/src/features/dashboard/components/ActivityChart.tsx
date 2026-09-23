import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { format, parseISO } from "date-fns";
import type { DailyActivity } from "../services/dashboard.service";

interface ActivityChartProps {
  data: DailyActivity[];
}

export function ActivityChart({ data }: Readonly<ActivityChartProps>) {
  const formattedData = data.map((item) => ({
    ...item,
    dateLabel: format(parseISO(item.date), "MMM d"),
  }));

  return (
    <ResponsiveContainer width="100%" height={280}>
      <LineChart
        data={formattedData}
        margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
      >
        <CartesianGrid
          strokeDasharray="3 3"
          stroke="#e2e8f0"
          vertical={false}
        />
        <XAxis
          dataKey="dateLabel"
          axisLine={false}
          tickLine={false}
          tick={{ fontSize: 12, fill: "#64748b" }}
        />
        <YAxis
          axisLine={false}
          tickLine={false}
          tick={{ fontSize: 12, fill: "#64748b" }}
          allowDecimals={false}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: "#ffffff",
            border: "1px solid #e2e8f0",
            borderRadius: "8px",
            boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
          }}
          labelStyle={{ color: "#0f172a", fontWeight: 600 }}
        />
        <Line
          type="monotone"
          dataKey="uploads"
          stroke="#2563eb"
          strokeWidth={2}
          dot={{ fill: "#2563eb", r: 4 }}
          activeDot={{ r: 6 }}
          name="Uploads"
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";

interface StatusChartProps {
  data: Record<string, number>;
}

const STATUS_COLORS: Record<string, string> = {
  DRAFT: "#94a3b8",
  PENDING_REVIEW: "#f59e0b",
  APPROVED: "#22c55e",
  REJECTED: "#ef4444",
};

const STATUS_LABELS: Record<string, string> = {
  DRAFT: "Draft",
  PENDING_REVIEW: "Pending",
  APPROVED: "Approved",
  REJECTED: "Rejected",
};

export function StatusChart({ data }: Readonly<StatusChartProps>) {
  const chartData = Object.entries(data).map(([key, value]) => ({
    name: STATUS_LABELS[key] || key,
    value,
    color: STATUS_COLORS[key] || "#94a3b8",
  }));

  const total = chartData.reduce((sum, item) => sum + item.value, 0);

  if (total === 0) {
    return (
      <div className="h-70 flex items-center justify-center text-slate-500 text-sm">
        No documents yet
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={280}>
      <PieChart>
        <Pie
          data={chartData}
          cx="50%"
          cy="50%"
          innerRadius={60}
          outerRadius={90}
          paddingAngle={2}
          dataKey="value"
        >
          {chartData.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={{
            backgroundColor: "#ffffff",
            border: "1px solid #e2e8f0",
            borderRadius: "8px",
          }}
        />
        <Legend
          verticalAlign="bottom"
          height={36}
          iconType="circle"
          iconSize={8}
          formatter={(value) => (
            <span className="text-sm text-slate-600">{value}</span>
          )}
        />
      </PieChart>
    </ResponsiveContainer>
  );
}

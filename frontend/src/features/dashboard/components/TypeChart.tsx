import {
  BarChart,
  Cell,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

interface TypeChartProps {
  data: Record<string, number>;
}

const TYPE_COLORS: Record<string, string> = {
  PDF: "#ef4444",
  Image: "#22c55e",
  Word: "#3b82f6",
  Excel: "#10b981",
  Text: "#64748b",
  Other: "#94a3b8",
};

export function TypeChart({ data }: Readonly<TypeChartProps>) {
  const chartData = Object.entries(data).map(([key, value]) => ({
    type: key,
    count: value,
    color: TYPE_COLORS[key] || "#94a3b8",
  }));

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart
        data={chartData}
        margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
      >
        <CartesianGrid
          strokeDasharray="3 3"
          stroke="#e2e8f0"
          vertical={false}
        />
        <XAxis
          dataKey="type"
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
          }}
          cursor={{ fill: "#f8fafc" }}
        />
        <Bar dataKey="count" radius={[6, 6, 0, 0]}>
          {chartData.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={entry.color} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

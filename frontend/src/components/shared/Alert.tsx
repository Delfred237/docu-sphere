import { AlertCircle } from "lucide-react";

interface AlertProps {
  variant: "error" | "success" | "info";
  message: string;
}

const variants = {
  error: "bg-red-50 border-red-200 text-red-800",
  success: "bg-green-50 border-green-200 text-green-800",
  info: "bg-blue-50 border-blue-200 text-blue-800",
};

export function Alert({ variant, message }: Readonly<AlertProps>) {
  return (
    <div
      className={`flex items-center gap-2 p-3 rounded-md border text-sm ${variants[variant]}`}
      role="alert"
    >
      {variant === "error" && <AlertCircle className="h-4 w-4 shrink-0" />}
      <p>{message}</p>
    </div>
  );
}

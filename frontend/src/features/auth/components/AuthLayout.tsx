import { Logo } from "@/components/shared/Logo";
import type { ReactNode } from "react";

interface AuthLayoutProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
}

export function AuthLayout({
  title,
  subtitle,
  children,
}: Readonly<AuthLayoutProps>) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="flex justify-center mb-8">
          <Logo size="lg" />
        </div>

        {/* Card */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-xs p-8">
          <div className="mb-6">
            <h2 className="text-xl font-semibold text-slate-900">{title}</h2>
            {subtitle && (
              <p className="text-sm text-slate-500 mt-1">{subtitle}</p>
            )}
          </div>
          {children}
        </div>
      </div>
    </div>
  );
}

import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, KeyRound, ArrowLeft } from "lucide-react";
import { z } from "zod";

import { AuthLayout } from "../components/AuthLayout";
import { authService, type ApiError } from "../services/auth.service";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FormError } from "@/components/shared/FormError";
import { Alert } from "@/components/shared/Alert";

const resetPasswordSchema = z
  .object({
    email: z.string().min(1, "Email is required").email("Invalid email format"),
    code: z
      .string()
      .length(6, "Code must be 6 digits")
      .regex(/^\d+$/, "Code must contain only digits"),
    newPassword: z
      .string()
      .min(8, "Minimum 8 characters")
      .regex(/[A-Z]/, "At least one uppercase letter")
      .regex(/[a-z]/, "At least one lowercase letter")
      .regex(/[0-9]/, "At least one number"),
    confirmPassword: z.string().min(1, "Please confirm your password"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

type ResetPasswordInput = z.infer<typeof resetPasswordSchema>;

export function ResetPasswordPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const emailFromState = (location.state as { email?: string })?.email || "";

  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [globalError, setGlobalError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordInput>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { email: emailFromState },
  });

  const onSubmit = async (data: ResetPasswordInput) => {
    setGlobalError(null);
    setSuccessMessage(null);

    try {
      await authService.resetPassword(data.email, data.code, data.newPassword);
      setSuccessMessage("Password reset successfully! You can now sign in.");
      setTimeout(() => navigate("/login"), 2000);
    } catch (error) {
      const apiError = error as { response?: { data?: ApiError } };
      const errorData = apiError.response?.data;

      if (errorData?.code === "INVALID_CODE") {
        setError("code", { message: "Invalid reset code" });
      } else if (errorData?.code === "CODE_EXPIRED") {
        setError("code", {
          message: "Code has expired. Please request a new one.",
        });
      } else {
        setGlobalError(
          errorData?.message || "Failed to reset password. Please try again.",
        );
      }
    }
  };

  return (
    <AuthLayout
      title="Reset your password"
      subtitle="Enter the code we sent to your email and choose a new password"
    >
      <div className="mb-6 flex justify-center">
        <div className="h-12 w-12 rounded-full bg-primary-50 flex items-center justify-center">
          <KeyRound className="h-6 w-6 text-primary-600" />
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {globalError && <Alert variant="error" message={globalError} />}
        {successMessage && <Alert variant="success" message={successMessage} />}

        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            type="email"
            placeholder="you@company.com"
            {...register("email")}
          />
          <FormError message={errors.email?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="code">Reset code</Label>
          <Input
            id="code"
            type="text"
            inputMode="numeric"
            maxLength={6}
            placeholder="000000"
            className="text-center text-2xl tracking-[0.5em] font-mono"
            {...register("code")}
          />
          <FormError message={errors.code?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="newPassword">New password</Label>
          <Input
            id="newPassword"
            type="password"
            placeholder="••••••••"
            autoComplete="new-password"
            {...register("newPassword")}
          />
          <FormError message={errors.newPassword?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="confirmPassword">Confirm new password</Label>
          <Input
            id="confirmPassword"
            type="password"
            placeholder="••••••••"
            autoComplete="new-password"
            {...register("confirmPassword")}
          />
          <FormError message={errors.confirmPassword?.message} />
        </div>

        <Button
          type="submit"
          className="w-full bg-primary-600 hover:bg-primary-700 text-white"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Resetting...
            </>
          ) : (
            "Reset password"
          )}
        </Button>
      </form>

      <p className="text-sm text-center text-slate-500 mt-6">
        <Link
          to="/login"
          className="text-primary-600 hover:text-primary-700 font-medium inline-flex items-center gap-1"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to sign in
        </Link>
      </p>
    </AuthLayout>
  );
}

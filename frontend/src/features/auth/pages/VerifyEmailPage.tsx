import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, Mail } from "lucide-react";

import { AuthLayout } from "../components/AuthLayout";
import {
  verifyEmailSchema,
  type VerifyEmailInput,
} from "../schemas/auth.schemas";
import { authService, type ApiError } from "../services/auth.service";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FormError } from "@/components/shared/FormError";
import { Alert } from "@/components/shared/Alert";

export function VerifyEmailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const emailFromState = (location.state as { email?: string })?.email || "";

  const [globalError, setGlobalError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isResending, setIsResending] = useState(false);

  const {
    register,
    handleSubmit,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<VerifyEmailInput>({
    resolver: zodResolver(verifyEmailSchema),
    defaultValues: { email: emailFromState },
  });

  const currentEmail = watch("email");

  const onSubmit = async (data: VerifyEmailInput) => {
    setGlobalError(null);
    setSuccessMessage(null);

    try {
      await authService.verifyEmail(data);
      setSuccessMessage("Email verified successfully! You can now sign in.");
      setTimeout(() => navigate("/login"), 2000);
    } catch (error) {
      const apiError = error as { response?: { data?: ApiError } };
      const errorData = apiError.response?.data;

      if (errorData?.code === "INVALID_CODE") {
        setError("code", { message: "Invalid verification code" });
      } else if (errorData?.code === "CODE_EXPIRED") {
        setError("code", {
          message: "Code has expired. Please request a new one.",
        });
      } else {
        setGlobalError(
          errorData?.message || "Verification failed. Please try again.",
        );
      }
    }
  };

  const handleResend = async () => {
    if (!currentEmail) return;

    setIsResending(true);
    setGlobalError(null);

    try {
      await authService.resendVerification(currentEmail);
      setSuccessMessage("Verification code sent. Check your inbox.");
    } catch (error) {
      const apiError = error as { response?: { data?: ApiError } };
      setGlobalError(
        apiError.response?.data?.message || "Failed to resend code.",
      );
    } finally {
      setIsResending(false);
    }
  };

  return (
    <AuthLayout
      title="Verify your email"
      subtitle="Enter the 6-digit code we sent to your email address"
    >
      <div className="mb-6 flex justify-center">
        <div className="h-12 w-12 rounded-full bg-primary-50 flex items-center justify-center">
          <Mail className="h-6 w-6 text-primary-600" />
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
          <Label htmlFor="code">Verification code</Label>
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

        <Button
          type="submit"
          className="w-full bg-primary-600 hover:bg-primary-700 text-white"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Verifying...
            </>
          ) : (
            "Verify email"
          )}
        </Button>

        <Button
          type="button"
          variant="outline"
          className="w-full"
          onClick={handleResend}
          disabled={isResending || !currentEmail}
        >
          {isResending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Resending...
            </>
          ) : (
            "Resend code"
          )}
        </Button>
      </form>

      <p className="text-sm text-center text-slate-500 mt-6">
        <Link
          to="/login"
          className="text-primary-600 hover:text-primary-700 font-medium"
        >
          Back to sign in
        </Link>
      </p>
    </AuthLayout>
  );
}

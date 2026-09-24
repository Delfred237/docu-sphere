import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, Mail, ArrowLeft } from "lucide-react";
import { z } from "zod";

import { AuthLayout } from "../components/AuthLayout";
import { authService, type ApiError } from "../services/auth.service";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FormError } from "@/components/shared/FormError";
import { Alert } from "@/components/shared/Alert";

const forgotPasswordSchema = z.object({
  email: z.string().min(1, "Email is required").email("Invalid email format"),
});

type ForgotPasswordInput = z.infer<typeof forgotPasswordSchema>;

export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [globalError, setGlobalError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordInput>({
    resolver: zodResolver(forgotPasswordSchema),
  });

  const onSubmit = async (data: ForgotPasswordInput) => {
    setGlobalError(null);
    setSuccessMessage(null);

    try {
      await authService.forgotPassword(data.email);
      setSuccessMessage(
        "If an account exists with this email, a reset code has been sent.",
      );
      setTimeout(() => {
        navigate("/reset-password", { state: { email: data.email } });
      }, 2000);
    } catch (error) {
      const apiError = error as { response?: { data?: ApiError } };
      setGlobalError(
        apiError.response?.data?.message ||
          "Something went wrong. Please try again.",
      );
    }
  };

  return (
    <AuthLayout
      title="Forgot your password?"
      subtitle="Enter your email and we'll send you a reset code"
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
          <Label htmlFor="email">Email address</Label>
          <Input
            id="email"
            type="email"
            placeholder="you@company.com"
            autoComplete="email"
            {...register("email")}
          />
          <FormError message={errors.email?.message} />
        </div>

        <Button
          type="submit"
          className="w-full bg-primary-600 hover:bg-primary-700 text-white"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Sending...
            </>
          ) : (
            "Send reset code"
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

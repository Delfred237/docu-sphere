import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";

import { AuthLayout } from "../components/AuthLayout";
import { loginSchema, type LoginInput } from "../schemas/auth.schemas";
import { useAuthStore } from "../stores/authStore";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FormError } from "@/components/shared/FormError";
import { Alert } from "@/components/shared/Alert";
import type { ApiError } from "../services/auth.service";

export function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [globalError, setGlobalError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginInput) => {
    setGlobalError(null);

    try {
      await login(data.email, data.password);
      navigate("/");
    } catch (error) {
      const apiError = error as { response?: { data?: ApiError } };
      const errorData = apiError.response?.data;

      if (errorData?.errors) {
        errorData.errors.forEach((fieldError) => {
          setError(fieldError.field as keyof LoginInput, {
            message: fieldError.message,
          });
        });
      } else if (errorData?.code === "EMAIL_NOT_VERIFIED") {
        navigate("/verify-email", { state: { email: data.email } });
      } else {
        setGlobalError(errorData?.message || "Invalid email or password");
      }
    }
  };

  return (
    <AuthLayout
      title="Sign in"
      subtitle="Enter your credentials to access your workspace"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {globalError && <Alert variant="error" message={globalError} />}

        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            type="email"
            placeholder="you@company.com"
            autoComplete="email"
            {...register("email")}
          />
          <FormError message={errors.email?.message} />
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <Label htmlFor="password">Password</Label>
            <Link
              to="/forgot-password"
              className="text-sm text-primary-600 hover:text-primary-700"
            >
              Forgot password?
            </Link>
          </div>
          <Input
            id="password"
            type="password"
            placeholder="••••••••"
            autoComplete="current-password"
            {...register("password")}
          />
          <FormError message={errors.password?.message} />
        </div>

        <Button
          type="submit"
          className="w-full bg-primary-600 hover:bg-primary-700 text-white"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Signing in...
            </>
          ) : (
            "Sign in"
          )}
        </Button>
      </form>

      <p className="text-sm text-center text-slate-500 mt-6">
        Don&apos;t have an account?{" "}
        <Link
          to="/register"
          className="text-primary-600 hover:text-primary-700 font-medium"
        >
          Sign up
        </Link>
      </p>
    </AuthLayout>
  );
}

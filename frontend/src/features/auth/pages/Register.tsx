import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";

import { AuthLayout } from "../components/AuthLayout";
import { registerSchema, type RegisterInput } from "../schemas/auth.schemas";
import { authService, type ApiError } from "../services/auth.service";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FormError } from "@/components/shared/FormError";
import { Alert } from "@/components/shared/Alert";

export function RegisterPage() {
  const navigate = useNavigate();
  const [globalError, setGlobalError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterInput>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterInput) => {
    setGlobalError(null);

    try {
      await authService.register(data);
      navigate("/verify-email", { state: { email: data.email } });
    } catch (error) {
      const apiError = error as { response?: { data?: ApiError } };
      const errorData = apiError.response?.data;

      if (errorData?.errors) {
        errorData.errors.forEach((fieldError) => {
          setError(fieldError.field as keyof RegisterInput, {
            message: fieldError.message,
          });
        });
      } else if (errorData?.code === "DUPLICATE_RESOURCE") {
        setError("email", { message: "This email is already registered" });
      } else {
        setGlobalError(
          errorData?.message || "Registration failed. Please try again.",
        );
      }
    }
  };

  return (
    <AuthLayout
      title="Create an account"
      subtitle="Start managing your documents with DocuSphere"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {globalError && <Alert variant="error" message={globalError} />}

        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="firstName">First name</Label>
            <Input
              id="firstName"
              placeholder="John"
              autoComplete="given-name"
              {...register("firstName")}
            />
            <FormError message={errors.firstName?.message} />
          </div>

          <div className="space-y-2">
            <Label htmlFor="lastName">Last name</Label>
            <Input
              id="lastName"
              placeholder="Doe"
              autoComplete="family-name"
              {...register("lastName")}
            />
            <FormError message={errors.lastName?.message} />
          </div>
        </div>

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
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            placeholder="••••••••"
            autoComplete="new-password"
            {...register("password")}
          />
          <FormError message={errors.password?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="confirmPassword">Confirm password</Label>
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
              Creating account...
            </>
          ) : (
            "Create account"
          )}
        </Button>
      </form>

      <p className="text-sm text-center text-slate-500 mt-6">
        Already have an account?{" "}
        <Link
          to="/login"
          className="text-primary-600 hover:text-primary-700 font-medium"
        >
          Sign in
        </Link>
      </p>
    </AuthLayout>
  );
}

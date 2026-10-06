import {
  Unlink,
  Link2Off,
  FileQuestionIcon,
  AlertTriangleIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Logo } from "@/components/shared/Logo";
import type { ShareLinkError } from "../shareDocument.service";

interface ShareErrorStateProps {
  errorType: ShareLinkError;
}

const errorConfig = {
  NOT_FOUND: {
    icon: FileQuestionIcon,
    title: "Link not found",
    description:
      "This share link does not exist or has been revoked by the owner.",
  },
  EXPIRED: {
    icon: Unlink,
    title: "Link expired",
    description:
      "This share link has expired. Please ask the document owner for a new link.",
  },
  REVOKED: {
    icon: Link2Off,
    title: "Link revoked",
    description: "This share link has been revoked by the owner.",
  },
  UNKNOWN: {
    icon: AlertTriangleIcon,
    title: "Something went wrong",
    description: "An unexpected error occurred. Please try again later.",
  },
};

export function ShareErrorState({ errorType }: Readonly<ShareErrorStateProps>) {
  const config = errorConfig[errorType];
  const Icon = config.icon;

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="flex justify-center mb-8">
          <Logo size="md" />
        </div>

        {/* Error card */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-xs p-8 text-center">
          <div className="h-16 w-16 rounded-full bg-red-50 flex items-center justify-center mx-auto mb-4">
            <Icon className="h-8 w-8 text-red-600" />
          </div>
          <h1 className="text-xl font-semibold text-slate-900 mb-2">
            {config.title}
          </h1>
          <p className="text-sm text-slate-500 mb-6">{config.description}</p>
          <Button
            variant="outline"
            onClick={() => (window.location.href = "/")}
            className="w-full"
          >
            Go to DocuSphere
          </Button>
        </div>

        {/* Footer */}
        <p className="text-xs text-center text-slate-400 mt-8">
          © 2026 DocuSphere. All rights reserved.
        </p>
      </div>
    </div>
  );
}

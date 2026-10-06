import { useState, useMemo } from "react";
import { useParams } from "react-router-dom";
import { useQuery, useMutation } from "@tanstack/react-query";
import {
  Download,
  Loader2,
  FileText,
  FileImage,
  FileSpreadsheet,
  File,
  Calendar,
  HardDrive,
  Lock,
  ShieldCheck,
} from "lucide-react";
import { format, parseISO } from "date-fns";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Logo } from "@/components/shared/Logo";
import { ShareErrorState } from "../components/ShareErrorState";
import {
  sharedDocumentService,
  type ShareLinkError,
  getShareLinkError,
} from "../shareDocument.service";

export function SharedDocumentPage() {
  const { token } = useParams<{ token: string }>();
  const [downloading, setDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  const {
    data: document,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["shared-document", token],
    queryFn: () => sharedDocumentService.getSharedDocumentInfo(token!),
    enabled: !!token,
    retry: false,
  });

  // Dériver le type d'erreur directement depuis l'erreur de la query
  const errorType = useMemo<ShareLinkError | null>(() => {
    if (isError && error) {
      return getShareLinkError(error);
    }
    return null;
  }, [isError, error]);

  const downloadMutation = useMutation({
    mutationFn: () => {
      if (!document) throw new Error("No document");
      return sharedDocumentService.downloadSharedDocument(
        token!,
        document.originalFilename,
      );
    },
    onMutate: () => {
      setDownloading(true);
      setDownloadError(null);
    },
    onSuccess: () => {
      setDownloading(false);
    },
    onError: (err: unknown) => {
      setDownloading(false);
      if (getShareLinkError(err) === "REVOKED") {
        setDownloadError("Download is not allowed for this link.");
      } else {
        setDownloadError("Failed to download the file. Please try again.");
      }
    },
  });

  // Loading state
  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin text-primary-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading shared document...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (isError && errorType) {
    return <ShareErrorState errorType={errorType} />;
  }

  if (!document) {
    return <ShareErrorState errorType="UNKNOWN" />;
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Header */}
      <header className="bg-white border-b border-slate-200 py-4">
        <div className="max-w-4xl mx-auto px-4 flex items-center justify-between">
          <Logo size="sm" />
          <Badge
            variant="outline"
            className="bg-slate-50 text-slate-600 border-slate-200"
          >
            <ShieldCheck className="h-3.5 w-3.5 mr-1" />
            Shared Document
          </Badge>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 flex items-center justify-center p-4 py-12">
        <div className="w-full max-w-2xl">
          <Card className="bg-white border-slate-200 shadow-xs overflow-hidden">
            {/* Document preview area */}
            <div className="bg-slate-50 border-b border-slate-200 p-8 md:p-12">
              <div className="flex justify-center">
                <FileIconLarge mimeType={document.mimeType} />
              </div>
            </div>

            <CardContent className="p-6 md:p-8">
              {/* Document name */}
              <div className="text-center mb-6">
                <h1 className="text-xl md:text-2xl font-semibold text-slate-900 mb-2 wrap-break-word">
                  {document.name}
                </h1>
                <p className="text-sm text-slate-500">Shared via DocuSphere</p>
              </div>

              {/* Details */}
              <div className="grid grid-cols-2 gap-4 mb-6">
                <DetailItem
                  icon={HardDrive}
                  label="Size"
                  value={formatSize(document.size)}
                />
                <DetailItem
                  icon={Calendar}
                  label="Uploaded"
                  value={format(parseISO(document.createdAt), "MMM d, yyyy")}
                />
                <DetailItem
                  icon={File}
                  label="Type"
                  value={
                    document.mimeType.split("/")[1]?.toUpperCase() || "File"
                  }
                />
                <DetailItem icon={Lock} label="Access" value="Link only" />
              </div>

              {/* Download button */}
              <Button
                onClick={() => downloadMutation.mutate()}
                disabled={downloading}
                className="w-full bg-primary-600 hover:bg-primary-700 text-white py-6 text-base"
              >
                {downloading ? (
                  <>
                    <Loader2 className="h-5 w-5 mr-2 animate-spin" />
                    Downloading...
                  </>
                ) : (
                  <>
                    <Download className="h-5 w-5 mr-2" />
                    Download Document
                  </>
                )}
              </Button>

              {/* Download error */}
              {downloadError && (
                <p className="text-sm text-red-600 text-center mt-3">
                  {downloadError}
                </p>
              )}

              {/* Security notice */}
              <div className="mt-6 p-3 rounded-md bg-slate-50 border border-slate-200">
                <p className="text-xs text-slate-500 text-center">
                  This document is shared securely via DocuSphere. The link may
                  expire or be revoked by the owner at any time.
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>

      {/* Footer */}
      <footer className="py-6 border-t border-slate-200 bg-white">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <p className="text-xs text-slate-400">
            © 2026 DocuSphere. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}

function DetailItem({
  icon: Icon,
  label,
  value,
}: Readonly<{
  icon: React.ElementType;
  label: string;
  value: string;
}>) {
  return (
    <div className="flex items-center gap-3 p-3 rounded-md bg-slate-50 border border-slate-200">
      <Icon className="h-5 w-5 text-slate-400 shrink-0" />
      <div className="min-w-0">
        <p className="text-xs text-slate-500">{label}</p>
        <p className="text-sm font-medium text-slate-900 truncate">{value}</p>
      </div>
    </div>
  );
}

function FileIconLarge({ mimeType }: Readonly<{ mimeType: string }>) {
  if (mimeType.includes("pdf"))
    return <FileText className="h-24 w-24 text-red-500" />;
  if (mimeType.includes("image"))
    return <FileImage className="h-24 w-24 text-green-500" />;
  if (mimeType.includes("word"))
    return <FileText className="h-24 w-24 text-blue-500" />;
  if (mimeType.includes("excel") || mimeType.includes("sheet"))
    return <FileSpreadsheet className="h-24 w-24 text-emerald-500" />;
  return <File className="h-24 w-24 text-slate-500" />;
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

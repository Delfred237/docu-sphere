import { useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  Download,
  Trash2,
  Pencil,
  Share2,
  Send,
  CheckCircle,
  XCircle,
  Loader2,
  FileText,
  FileImage,
  FileSpreadsheet,
  File,
  Calendar,
  HardDrive,
  Folder,
} from "lucide-react";
import { format, parseISO } from "date-fns";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { documentService } from "../services/document.service";
import { StatusBadge } from "../components/StatusBadge";
import { ShareDocumentDialog } from "@/features/sharing/components/ShareDocumentDialog";
import { RenameDialog } from "@/components/shared/RenameDialog";
import { ConfirmationDialog } from "@/components/shared/ConfirmationDialog";
import { Alert } from "@/components/shared/Alert";

export function DocumentDetailPage() {
  const { publicId } = useParams<{ publicId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [error, setError] = useState<string | null>(null);
  const [shareDialogOpen, setShareDialogOpen] = useState(false);
  const [renameDialogOpen, setRenameDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const { data: document, isLoading } = useQuery({
    queryKey: ["document", publicId],
    queryFn: () => documentService.getDocument(publicId!),
    enabled: !!publicId,
  });

  const renameMutation = useMutation({
    mutationFn: (newName: string) =>
      documentService.renameDocument(publicId!, newName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["document", publicId] });
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
    onError: () => setError("Failed to rename document"),
  });

  const deleteMutation = useMutation({
    mutationFn: () => documentService.deleteDocument(publicId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      navigate("/documents");
    },
    onError: () => setError("Failed to delete document"),
  });

  const submitMutation = useMutation({
    mutationFn: () => documentService.submitForReview(publicId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["document", publicId] });
    },
  });

  const approveMutation = useMutation({
    mutationFn: () => documentService.approveDocument(publicId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["document", publicId] });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => documentService.rejectDocument(publicId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["document", publicId] });
    },
  });

  const handleDownload = async () => {
    if (!document) return;
    setDownloading(true);
    try {
      await documentService.downloadDocument(
        document.publicId,
        document.originalFilename,
      );
    } catch {
      setError("Failed to download file");
    } finally {
      setDownloading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
      </div>
    );
  }

  if (!document) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4">
        <p className="text-slate-500">Document not found</p>
        <Link to="/documents">
          <Button variant="outline">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to documents
          </Button>
        </Link>
      </div>
    );
  }

  const hasFile = document.size > 0;

  return (
    <div className="p-4 md:p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link to="/documents">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-5 w-5" />
          </Button>
        </Link>
        <div className="flex-1 min-w-0">
          <h1 className="text-xl md:text-2xl font-semibold text-slate-900 truncate">
            {document.name}
          </h1>
          <div className="flex items-center gap-2 mt-1">
            <StatusBadge status={document.status} />
            <span className="text-sm text-slate-500">
              {format(parseISO(document.createdAt), "MMM d, yyyy")}
            </span>
          </div>
        </div>
      </div>

      {/* Error */}
      {error && <Alert variant="error" message={error} />}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Preview */}
        <div className="lg:col-span-2">
          <Card className="bg-white border-slate-200">
            <CardHeader>
              <CardTitle>Preview</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center justify-center min-h-75 bg-slate-50 rounded-lg">
                {hasFile ? (
                  document.mimeType.includes("image") ? (
                    <img
                      src={URL.createObjectURL(new Blob())} // À remplacer par l'URL réelle
                      alt={document.name}
                      className="max-w-full max-h-100 object-contain rounded"
                    />
                  ) : (
                    <div className="text-center">
                      <FileIconLarge mimeType={document.mimeType} />
                      <p className="text-sm text-slate-500 mt-4">
                        Preview not available for this file type
                      </p>
                      <p className="text-xs text-slate-400 mt-1">
                        Download the file to view its contents
                      </p>
                    </div>
                  )
                ) : (
                  <div className="text-center">
                    <FileText className="h-16 w-16 text-slate-300 mx-auto" />
                    <p className="text-sm text-slate-500 mt-4">
                      No file associated with this document
                    </p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Details sidebar */}
        <div className="space-y-6">
          {/* Actions */}
          <Card className="bg-white border-slate-200">
            <CardHeader>
              <CardTitle>Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {hasFile && (
                <Button
                  onClick={handleDownload}
                  disabled={downloading}
                  className="w-full bg-primary-600 hover:bg-primary-700 text-white"
                >
                  {downloading ? (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  ) : (
                    <Download className="h-4 w-4 mr-2" />
                  )}
                  Download
                </Button>
              )}

              <Button
                variant="outline"
                onClick={() => setShareDialogOpen(true)}
                className="w-full"
              >
                <Share2 className="h-4 w-4 mr-2" />
                Share
              </Button>

              <Button
                variant="outline"
                onClick={() => setRenameDialogOpen(true)}
                className="w-full"
              >
                <Pencil className="h-4 w-4 mr-2" />
                Rename
              </Button>

              {document.status === "DRAFT" && (
                <Button
                  variant="outline"
                  onClick={() => submitMutation.mutate()}
                  disabled={submitMutation.isPending}
                  className="w-full"
                >
                  <Send className="h-4 w-4 mr-2" />
                  Submit for review
                </Button>
              )}

              {document.status === "PENDING_REVIEW" && (
                <div className="grid grid-cols-2 gap-2">
                  <Button
                    variant="outline"
                    onClick={() => approveMutation.mutate()}
                    disabled={approveMutation.isPending}
                    className="text-green-600 hover:text-green-700 hover:bg-green-50"
                  >
                    <CheckCircle className="h-4 w-4 mr-1" />
                    Approve
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => rejectMutation.mutate()}
                    disabled={rejectMutation.isPending}
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    <XCircle className="h-4 w-4 mr-1" />
                    Reject
                  </Button>
                </div>
              )}

              <Button
                variant="outline"
                onClick={() => setDeleteDialogOpen(true)}
                className="w-full text-red-600 hover:text-red-700 hover:bg-red-50"
              >
                <Trash2 className="h-4 w-4 mr-2" />
                Delete
              </Button>
            </CardContent>
          </Card>

          {/* Details */}
          <Card className="bg-white border-slate-200">
            <CardHeader>
              <CardTitle>Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-sm">
              <DetailRow icon={File} label="Type" value={document.mimeType} />
              <DetailRow
                icon={HardDrive}
                label="Size"
                value={formatSize(document.size)}
              />
              <DetailRow
                icon={Calendar}
                label="Uploaded"
                value={format(parseISO(document.createdAt), "PPP p")}
              />
              {document.folderName && (
                <DetailRow
                  icon={Folder}
                  label="Folder"
                  value={document.folderName}
                />
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Dialogs */}
      <ShareDocumentDialog
        doc={document}
        open={shareDialogOpen}
        onOpenChange={setShareDialogOpen}
      />

      <RenameDialog
        open={renameDialogOpen}
        onOpenChange={setRenameDialogOpen}
        currentName={document.name}
        onRename={async (newName) => {
          await renameMutation.mutateAsync(newName);
        }}
        title="Rename document"
        description="Enter a new name for this document"
      />

      <ConfirmationDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        title="Delete document"
        description={`Are you sure you want to delete "${document.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        onConfirm={() => deleteMutation.mutate()}
        isLoading={deleteMutation.isPending}
        variant="destructive"
      />
    </div>
  );
}

function DetailRow({
  icon: Icon,
  label,
  value,
}: Readonly<{
  icon: React.ElementType;
  label: string;
  value: string;
}>) {
  return (
    <div className="flex items-start gap-3">
      <Icon className="h-4 w-4 text-slate-400 mt-0.5 shrink-0" />
      <div className="flex-1 min-w-0">
        <p className="text-slate-500">{label}</p>
        <p className="font-medium text-slate-900 truncate">{value}</p>
      </div>
    </div>
  );
}

function FileIconLarge({ mimeType }: Readonly<{ mimeType: string }>) {
  if (mimeType.includes("pdf"))
    return <FileText className="h-16 w-16 text-red-500" />;
  if (mimeType.includes("image"))
    return <FileImage className="h-16 w-16 text-green-500" />;
  if (mimeType.includes("word"))
    return <FileText className="h-16 w-16 text-blue-500" />;
  if (mimeType.includes("excel") || mimeType.includes("sheet"))
    return <FileSpreadsheet className="h-16 w-16 text-emerald-500" />;
  return <File className="h-16 w-16 text-slate-500" />;
}

function formatSize(bytes: number): string {
  if (bytes === 0) return "No file";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

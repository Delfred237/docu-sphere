import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2, RefreshCw, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { UploadDropzone } from "../components/UploadDropzone";
import { DocumentsTable } from "../components/DocumentsTable";
import { documentService, type Document } from "../services/document.service";
import { Alert } from "@/components/shared/Alert";

export function DocumentsPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [uploadingCount, setUploadingCount] = useState(0);

  // Fetch documents
  const { data, isLoading, refetch, isFetching } = useQuery({
    queryKey: ["documents", searchQuery],
    queryFn: () =>
      documentService.searchDocuments({ name: searchQuery, size: 50 }),
    placeholderData: (previousData) => previousData,
  });

  // Upload mutation
  const uploadMutation = useMutation({
    mutationFn: (file: File) => documentService.uploadDocument(file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
    onError: () => {
      setError("Failed to upload file");
    },
    onSettled: () => {
      setUploadingCount((count) => count - 1);
    },
  });

  // Submit for review mutation
  const submitMutation = useMutation({
    mutationFn: (publicId: string) => documentService.submitForReview(publicId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  // Approve mutation
  const approveMutation = useMutation({
    mutationFn: (publicId: string) => documentService.approveDocument(publicId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  // Reject mutation
  const rejectMutation = useMutation({
    mutationFn: (publicId: string) => documentService.rejectDocument(publicId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (publicId: string) => documentService.deleteDocument(publicId),
    onSuccess: async () => {
      // Force le refetch de toutes les queries 'documents'
      await queryClient.refetchQueries({ queryKey: ["documents"] });
      // Met à jour aussi le dashboard
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
    onError: () => {
      setError("Failed to delete document");
    },
  });

  const handleUpload = async (files: File[]) => {
    setError(null);
    setUploadingCount((count) => count + files.length);

    for (const file of files) {
      await uploadMutation.mutateAsync(file).catch(() => {});
    }
  };

  const handleDownload = async (doc: Document) => {
    try {
      await documentService.downloadDocument(
        doc.publicId,
        doc.originalFilename,
      );
    } catch {
      setError("Failed to download file");
    }
  };

  const handleSubmit = (doc: Document) => {
    submitMutation.mutate(doc.publicId);
  };

  const handleApprove = (doc: Document) => {
    approveMutation.mutate(doc.publicId);
  };

  const handleReject = (doc: Document) => {
    rejectMutation.mutate(doc.publicId);
  };

  const handleDelete = (doc: Document) => {
    deleteMutation.mutate(doc.publicId);
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl md:text-2xl font-semibold text-slate-900">
            Documents
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Manage and review your documents
          </p>
        </div>
        <Button
          variant="outline"
          onClick={() => refetch()}
          disabled={isFetching}
          className="self-start sm:self-auto"
        >
          <RefreshCw
            className={`h-4 w-4 mr-2 ${isFetching ? "animate-spin" : ""}`}
          />
          Refresh
        </Button>
      </div>

      {/* Error alert */}
      {error && (
        <div className="flex items-start justify-between gap-4">
          <Alert variant="error" message={error} />
          <button
            onClick={() => setError(null)}
            className="text-sm text-slate-400 hover:text-slate-600 flex-shrink-0"
          >
            ✕
          </button>
        </div>
      )}

      {/* Upload zone */}
      <UploadDropzone
        onUpload={handleUpload}
        isUploading={uploadingCount > 0}
      />

      {/* Uploading indicator */}
      {uploadingCount > 0 && (
        <div className="flex items-center gap-2 text-sm text-slate-600">
          <Loader2 className="h-4 w-4 animate-spin" />
          Uploading {uploadingCount} file{uploadingCount > 1 ? "s" : ""}...
        </div>
      )}

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
        <Input
          placeholder="Search documents..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="pl-9"
        />
      </div>

      {/* Documents table */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
        </div>
      ) : (
        <DocumentsTable
          documents={data?.content || []}
          onDownload={handleDownload}
          onSubmit={handleSubmit}
          onApprove={handleApprove}
          onReject={handleReject}
          onDelete={handleDelete}
        />
      )}

      {/* Pagination info */}
      {data && (
        <div className="text-sm text-slate-500">
          Showing {data.content.length} of {data.totalElements} documents
        </div>
      )}
    </div>
  );
}

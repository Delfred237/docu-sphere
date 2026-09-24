import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { FolderTree } from "../components/FolderTree";
import { FolderBreadcrumb } from "../components/FolderBreadcrumb";
import { CreateFolderDialog } from "../components/CreateFolderDialog";
import { DocumentsTable } from "@/features/documents/components/DocumentsTable";
import { UploadDropzone } from "@/features/documents/components/UploadDropzone";
import {
  documentService,
  type Document,
} from "@/features/documents/services/document.service";
import { folderService, type Folder } from "../services/folder.service";
import { Alert } from "@/components/shared/Alert";
import { Button } from "@/components/ui/button";
import { FolderIcon, X } from "lucide-react";

export function FoldersPage() {
  const queryClient = useQueryClient();
  const [selectedFolder, setSelectedFolder] = useState<Folder | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  // Fetch all folders for breadcrumb
  const { data: allFolders = [] } = useQuery({
    queryKey: ["all-folders"],
    queryFn: async () => {
      const root = await folderService.getRootFolders();
      return root;
    },
  });

  // Fetch documents in selected folder
  const {
    data: documentsData,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ["documents", selectedFolder?.publicId],
    queryFn: () =>
      documentService.searchDocuments({
        folderId: selectedFolder?.publicId || undefined,
        rootOnly: !selectedFolder,
        size: 100,
      }),
  });

  // Delete document mutation
  const deleteMutation = useMutation({
    mutationFn: (publicId: string) => documentService.deleteDocument(publicId),
    onSuccess: async () => {
      await queryClient.refetchQueries({
        queryKey: ["documents", selectedFolder?.publicId],
      });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
    onError: () => {
      setError("Failed to delete document");
    },
  });

  const handleFolderSelect = (folder: Folder | null) => {
    setSelectedFolder(folder);
    setError(null);
    setIsSidebarOpen(false);
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

  const handleSubmit = async (doc: Document) => {
    try {
      await documentService.submitForReview(doc.publicId);
      refetch();
    } catch {
      setError("Failed to submit document");
    }
  };

  const handleApprove = async (doc: Document) => {
    try {
      await documentService.approveDocument(doc.publicId);
      refetch();
    } catch {
      setError("Failed to approve document");
    }
  };

  const handleReject = async (doc: Document) => {
    try {
      await documentService.rejectDocument(doc.publicId);
      refetch();
    } catch {
      setError("Failed to reject document");
    }
  };

  const handleDelete = (doc: Document) => {
    deleteMutation.mutate(doc.publicId);
  };

  const handleUpload = async (files: File[]) => {
    setError(null);
    try {
      for (const file of files) {
        await documentService.uploadDocument(file, selectedFolder?.publicId);
      }
      refetch();
    } catch {
      setError("Failed to upload files");
    }
  };

  return (
    <div className="flex h-full relative">
      {/* Mobile overlay */}
      {isSidebarOpen && (
        <div
          role="presentation"
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* Sidebar - Desktop */}
      <aside className="hidden lg:flex lg:w-64 border-r border-slate-200 bg-white p-4 overflow-y-auto flex-shrink-0">
        <div className="w-full">
          <div className="mb-4">
            <h3 className="text-sm font-semibold text-slate-900 mb-3">
              Folders
            </h3>
            <CreateFolderDialog parentId={selectedFolder?.publicId} />
          </div>
          <FolderTree
            selectedFolderId={selectedFolder?.publicId || null}
            onFolderSelect={handleFolderSelect}
          />
        </div>
      </aside>

      {/* Sidebar - Mobile Drawer */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-72 bg-white border-r border-slate-200 flex flex-col transform transition-transform duration-300 ease-in-out lg:hidden ${
          isSidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="h-16 flex items-center justify-between px-4 border-b border-slate-200">
          <h3 className="text-sm font-semibold text-slate-900">Folders</h3>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setIsSidebarOpen(false)}
          >
            <X className="h-5 w-5 text-slate-600" />
            <span className="sr-only">Close folders</span>
          </Button>
        </div>
        <div className="flex-1 p-4 overflow-y-auto">
          <div className="mb-4">
            <CreateFolderDialog parentId={selectedFolder?.publicId} />
          </div>
          <FolderTree
            selectedFolderId={selectedFolder?.publicId || null}
            onFolderSelect={handleFolderSelect}
          />
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 p-4 md:p-6 overflow-y-auto">
        <div className="space-y-6">
          {/* Mobile button to open folders */}
          <div className="lg:hidden">
            <Button
              variant="outline"
              onClick={() => setIsSidebarOpen(true)}
              className="w-full"
            >
              <FolderIcon className="h-4 w-4 mr-2" />
              Browse Folders
            </Button>
          </div>

          {/* Header with breadcrumb */}
          <div>
            <FolderBreadcrumb
              folders={allFolders}
              currentFolder={selectedFolder}
              onFolderClick={handleFolderSelect}
            />
            <h1 className="text-xl md:text-2xl font-semibold text-slate-900 mt-2">
              {selectedFolder ? selectedFolder.name : "All Documents"}
            </h1>
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
          <UploadDropzone onUpload={handleUpload} />

          {/* Documents table */}
          {isLoading ? (
            <div className="flex justify-center py-12">
              <div className="w-8 h-8 border-4 border-slate-300 border-t-primary-600 rounded-full animate-spin" />
            </div>
          ) : (
            <DocumentsTable
              documents={documentsData?.content || []}
              onDownload={handleDownload}
              onSubmit={handleSubmit}
              onApprove={handleApprove}
              onReject={handleReject}
              onDelete={handleDelete}
            />
          )}
        </div>
      </main>
    </div>
  );
}

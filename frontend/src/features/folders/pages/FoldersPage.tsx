import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
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

export function FoldersPage() {
  const [selectedFolder, setSelectedFolder] = useState<Folder | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Fetch all folders for breadcrumb
  const { data: allFolders = [] } = useQuery({
    queryKey: ["all-folders"],
    queryFn: async () => {
      const root = await folderService.getRootFolders();
      // Pour simplifier, on ne charge que les root folders pour le breadcrumb
      // Une vraie implémentation chargerait récursivement
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

  const handleFolderSelect = (folder: Folder | null) => {
    setSelectedFolder(folder);
    setError(null);
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

  return (
    <div className="flex h-full">
      {/* Sidebar with folder tree */}
      <aside className="w-64 border-r border-slate-200 bg-white p-4 overflow-y-auto">
        <div className="mb-4">
          <h3 className="text-sm font-semibold text-slate-900 mb-3">Folders</h3>
          <CreateFolderDialog parentId={selectedFolder?.publicId} />
        </div>
        <FolderTree
          selectedFolderId={selectedFolder?.publicId || null}
          onFolderSelect={handleFolderSelect}
        />
      </aside>

      {/* Main content */}
      <main className="flex-1 p-6 overflow-y-auto">
        <div className="max-w-6xl mx-auto space-y-6">
          {/* Header with breadcrumb */}
          <div>
            <FolderBreadcrumb
              folders={allFolders}
              currentFolder={selectedFolder}
              onFolderClick={handleFolderSelect}
            />
            <h1 className="text-2xl font-semibold text-slate-900 mt-2">
              {selectedFolder ? selectedFolder.name : "All Documents"}
            </h1>
          </div>

          {/* Error alert */}
          {error && <Alert variant="error" message={error} />}

          {/* Upload zone */}
          <UploadDropzone
            onUpload={async (files) => {
              setError(null);
              try {
                for (const file of files) {
                  await documentService.uploadDocument(
                    file,
                    selectedFolder?.publicId,
                  );
                }
                refetch();
              } catch {
                setError("Failed to upload files");
              }
            }}
          />

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
            />
          )}
        </div>
      </main>
    </div>
  );
}

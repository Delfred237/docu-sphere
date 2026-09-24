import { useState, useMemo } from "react";
import {
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Download,
  CheckCircle,
  Send,
  XCircle,
  Trash2,
  Pencil,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { FileIcon } from "./FileIcon";
import { StatusBadge } from "./StatusBadge";
import { DocumentCard } from "./DocumentCard";
import { ConfirmationDialog } from "@/components/shared/ConfirmationDialog";
import { formatDistanceToNow, parseISO } from "date-fns";
import type { Document } from "../services/document.service";
import { RenameDialog } from "@/components/shared/RenameDialog";

interface DocumentsTableProps {
  documents: Document[];
  onDownload: (doc: Document) => void;
  onSubmit: (doc: Document) => void;
  onApprove: (doc: Document) => void;
  onReject: (doc: Document) => void;
  onRename: (doc: Document, newName: string) => Promise<void>;
  onDelete: (doc: Document) => void;
}

type SortField = "name" | "size" | "createdAt" | null;
type SortDirection = "asc" | "desc";

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function renderSortIcon(
  activeField: SortField,
  sortField: SortField,
  sortDirection: SortDirection,
) {
  if (activeField !== sortField) {
    return <ArrowUpDown className="ml-2 h-4 w-4 text-slate-400" />;
  }
  return sortDirection === "asc" ? (
    <ArrowUp className="ml-2 h-4 w-4 text-slate-600" />
  ) : (
    <ArrowDown className="ml-2 h-4 w-4 text-slate-600" />
  );
}

export function DocumentsTable({
  documents,
  onDownload,
  onSubmit,
  onApprove,
  onReject,
  onRename,
  onDelete,
}: Readonly<DocumentsTableProps>) {
  const [sortField, setSortField] = useState<SortField>("createdAt");
  const [sortDirection, setSortDirection] = useState<SortDirection>("desc");
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [documentToDelete, setDocumentToDelete] = useState<Document | null>(
    null,
  );
  const [renameDialogOpen, setRenameDialogOpen] = useState(false);
  const [documentToRename, setDocumentToRename] = useState<Document | null>(
    null,
  );

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDirection("asc");
    }
  };

  const handleRenameClick = (doc: Document) => {
    setDocumentToRename(doc);
    setRenameDialogOpen(true);
  };

  const sortedDocuments = useMemo(() => {
    if (!sortField) return documents;

    return [...documents].sort((a, b) => {
      let comparison = 0;

      if (sortField === "name") {
        comparison = a.name.localeCompare(b.name);
      } else if (sortField === "size") {
        comparison = a.size - b.size;
      } else if (sortField === "createdAt") {
        if (!a.createdAt && !b.createdAt) return 0;
        if (!a.createdAt) return 1;
        if (!b.createdAt) return -1;
        comparison =
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      }

      return sortDirection === "asc" ? comparison : -comparison;
    });
  }, [documents, sortField, sortDirection]);

  const toggleSelectAll = () => {
    if (selectedIds.size === documents.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(documents.map((d) => d.publicId)));
    }
  };

  const toggleSelect = (id: string) => {
    const newSelected = new Set(selectedIds);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    setSelectedIds(newSelected);
  };

  const handleDeleteClick = (doc: Document) => {
    setDocumentToDelete(doc);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = () => {
    if (documentToDelete) {
      onDelete(documentToDelete);
      setDeleteDialogOpen(false);
      setDocumentToDelete(null);
    }
  };

  return (
    <>
      {/* Desktop Table - visible on md and up */}
      <div className="hidden md:block rounded-lg border border-slate-200 bg-white overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="px-4 py-3 w-12">
                  <Checkbox
                    checked={
                      selectedIds.size === documents.length &&
                      documents.length > 0
                    }
                    onCheckedChange={toggleSelectAll}
                    aria-label="Select all"
                  />
                </th>
                <th className="px-4 py-3 text-left">
                  <Button
                    variant="ghost"
                    onClick={() => handleSort("name")}
                    className="-ml-3 font-semibold text-xs text-slate-500 uppercase"
                  >
                    Name
                    {renderSortIcon("name", sortField, sortDirection)}
                  </Button>
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">
                  Status
                </th>
                <th className="px-4 py-3 text-left">
                  <Button
                    variant="ghost"
                    onClick={() => handleSort("size")}
                    className="-ml-3 font-semibold text-xs text-slate-500 uppercase"
                  >
                    Size
                    {renderSortIcon("size", sortField, sortDirection)}
                  </Button>
                </th>
                <th className="px-4 py-3 text-left">
                  <Button
                    variant="ghost"
                    onClick={() => handleSort("createdAt")}
                    className="-ml-3 font-semibold text-xs text-slate-500 uppercase"
                  >
                    Uploaded
                    {renderSortIcon("createdAt", sortField, sortDirection)}
                  </Button>
                </th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {sortedDocuments.map((doc) => (
                <tr
                  key={doc.publicId}
                  className="hover:bg-slate-50 transition-colors"
                >
                  <td className="px-4 py-3">
                    <Checkbox
                      checked={selectedIds.has(doc.publicId)}
                      onCheckedChange={() => toggleSelect(doc.publicId)}
                      aria-label={`Select ${doc.name}`}
                    />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <FileIcon mimeType={doc.mimeType} />
                      <span className="font-medium text-slate-900 truncate max-w-[200px]">
                        {doc.name}
                      </span>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={doc.status} />
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-600">
                    {formatSize(doc.size)}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-500">
                    {doc.createdAt ? (
                      formatDistanceToNow(parseISO(doc.createdAt), {
                        addSuffix: true,
                      })
                    ) : (
                      <span className="text-slate-400">Just now</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => onDownload(doc)}
                        title="Download"
                      >
                        <Download className="h-4 w-4 text-slate-600" />
                      </Button>

                      {doc.status === "DRAFT" && (
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => onSubmit(doc)}
                          title="Submit for review"
                        >
                          <Send className="h-4 w-4 text-slate-600" />
                        </Button>
                      )}

                      {doc.status === "PENDING_REVIEW" && (
                        <>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => onApprove(doc)}
                            title="Approve"
                          >
                            <CheckCircle className="h-4 w-4 text-green-600" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => onReject(doc)}
                            title="Reject"
                          >
                            <XCircle className="h-4 w-4 text-red-600" />
                          </Button>
                        </>
                      )}
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleRenameClick(doc)}
                        title="Rename"
                      >
                        <Pencil className="h-4 w-4 text-slate-600" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleDeleteClick(doc)}
                        title="Delete"
                      >
                        <Trash2 className="h-4 w-4 text-red-600" />
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {documents.length === 0 && (
          <div className="py-12 text-center text-slate-500 text-sm">
            No documents found
          </div>
        )}
      </div>

      {/* Mobile Cards - visible on mobile only */}
      <div className="md:hidden space-y-3">
        {sortedDocuments.map((doc) => (
          <DocumentCard
            key={doc.publicId}
            doc={doc}
            isSelected={selectedIds.has(doc.publicId)}
            onSelect={toggleSelect}
            onDownload={onDownload}
            onSubmit={onSubmit}
            onApprove={onApprove}
            onReject={onReject}
            onDelete={handleDeleteClick}
          />
        ))}

        {documents.length === 0 && (
          <div className="py-12 text-center text-slate-500 text-sm bg-white rounded-lg border border-slate-200">
            No documents found
          </div>
        )}
      </div>

      {/* Rename confirmation dialog */}
      <RenameDialog
        open={renameDialogOpen}
        onOpenChange={setRenameDialogOpen}
        currentName={documentToRename?.name || ""}
        onRename={async (newName) => {
          if (documentToRename) {
            await onRename(documentToRename, newName);
          }
        }}
        title="Rename document"
        description="Enter a new name for this document"
      />

      {/* Delete confirmation dialog */}
      <ConfirmationDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        title="Delete document"
        description={`Are you sure you want to delete "${documentToDelete?.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        onConfirm={handleDeleteConfirm}
        variant="destructive"
      />
    </>
  );
}

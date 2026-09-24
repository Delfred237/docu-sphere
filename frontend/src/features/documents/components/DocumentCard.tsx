import { Download, CheckCircle, XCircle, Send, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { FileIcon } from "./FileIcon";
import { StatusBadge } from "./StatusBadge";
import { formatDistanceToNow, parseISO } from "date-fns";
import type { Document } from "../services/document.service";

interface DocumentCardProps {
  doc: Document;
  isSelected: boolean;
  onSelect: (id: string) => void;
  onDownload: (doc: Document) => void;
  onSubmit: (doc: Document) => void;
  onApprove: (doc: Document) => void;
  onReject: (doc: Document) => void;
  onDelete: (doc: Document) => void;
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function DocumentCard({
  doc,
  isSelected,
  onSelect,
  onDownload,
  onSubmit,
  onApprove,
  onReject,
  onDelete,
}: Readonly<DocumentCardProps>) {
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4 hover:shadow-sm transition-shadow">
      <div className="flex items-start gap-3">
        {/* Checkbox */}
        <div className="flex-shrink-0 mt-1">
          <Checkbox
            checked={isSelected}
            onCheckedChange={() => onSelect(doc.publicId)}
            aria-label={`Select ${doc.name}`}
          />
        </div>

        {/* Icon and info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start gap-3">
            <FileIcon
              mimeType={doc.mimeType}
              className="h-6 w-6 flex-shrink-0"
            />
            <div className="flex-1 min-w-0">
              <p className="font-medium text-slate-900 truncate">{doc.name}</p>
              <div className="flex items-center gap-2 mt-1 flex-wrap">
                <span className="text-xs text-slate-500">
                  {formatSize(doc.size)}
                </span>
                {doc.createdAt && (
                  <>
                    <span className="text-slate-300">•</span>
                    <span className="text-xs text-slate-500">
                      {formatDistanceToNow(parseISO(doc.createdAt), {
                        addSuffix: true,
                      })}
                    </span>
                  </>
                )}
              </div>
            </div>
          </div>

          {/* Status and actions */}
          <div className="flex items-center justify-between mt-3">
            <StatusBadge status={doc.status} />
            <div className="flex items-center gap-1">
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                onClick={() => onDownload(doc)}
                title="Download"
              >
                <Download className="h-4 w-4 text-slate-600" />
              </Button>

              {doc.status === "DRAFT" && (
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
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
                    className="h-8 w-8"
                    onClick={() => onApprove(doc)}
                    title="Approve"
                  >
                    <CheckCircle className="h-4 w-4 text-green-600" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
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
                className="h-8 w-8"
                onClick={() => onDelete(doc)}
                title="Delete"
              >
                <Trash2 className="h-4 w-4 text-red-600" />
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

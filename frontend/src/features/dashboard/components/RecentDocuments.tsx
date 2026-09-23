import { FileText, FileImage, FileSpreadsheet, File } from "lucide-react";
import { formatDistanceToNow, parseISO } from "date-fns";
import type { RecentDocument } from "../services/dashboard.service";
import { Badge } from "@/components/ui/badge";

interface RecentDocumentsProps {
  documents: RecentDocument[];
}

const statusStyles: Record<string, string> = {
  DRAFT: "bg-slate-100 text-slate-700",
  PENDING_REVIEW: "bg-amber-50 text-amber-700",
  APPROVED: "bg-green-50 text-green-700",
  REJECTED: "bg-red-50 text-red-700",
};

const statusLabels: Record<string, string> = {
  DRAFT: "Draft",
  PENDING_REVIEW: "Pending",
  APPROVED: "Approved",
  REJECTED: "Rejected",
};

function getFileIcon(mimeType: string) {
  if (mimeType.includes("pdf"))
    return <FileText className="h-4 w-4 text-red-500" />;
  if (mimeType.includes("image"))
    return <FileImage className="h-4 w-4 text-green-500" />;
  if (mimeType.includes("word"))
    return <FileText className="h-4 w-4 text-blue-500" />;
  if (mimeType.includes("excel") || mimeType.includes("sheet"))
    return <FileSpreadsheet className="h-4 w-4 text-emerald-500" />;
  return <File className="h-4 w-4 text-slate-500" />;
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function RecentDocuments({ documents }: Readonly<RecentDocumentsProps>) {
  if (documents.length === 0) {
    return (
      <div className="py-8 text-center text-sm text-slate-500">
        No documents yet
      </div>
    );
  }

  return (
    <div className="divide-y divide-slate-100">
      {documents.map((doc) => (
        <div
          key={doc.publicId}
          className="flex items-center gap-3 py-3 hover:bg-slate-50 -mx-2 px-2 rounded-md transition-colors"
        >
          <div className="h-8 w-8 rounded-md bg-slate-50 flex items-center justify-center shrink-0">
            {getFileIcon(doc.mimeType)}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-slate-900 truncate">
              {doc.name}
            </p>
            <p className="text-xs text-slate-500">
              {formatSize(doc.size)} ·{" "}
              {formatDistanceToNow(parseISO(doc.uploadedAt), {
                addSuffix: true,
              })}
            </p>
          </div>
          <Badge
            className={`${statusStyles[doc.status]} border-0 text-xs font-medium`}
          >
            {statusLabels[doc.status]}
          </Badge>
        </div>
      ))}
    </div>
  );
}

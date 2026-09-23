import { ChevronRight, Home } from "lucide-react";
import type { Folder } from "../services/folder.service";

interface FolderBreadcrumbProps {
  folders: Folder[];
  currentFolder: Folder | null;
  onFolderClick: (folder: Folder | null) => void;
}

export function FolderBreadcrumb({
  folders,
  currentFolder,
  onFolderClick,
}: Readonly<FolderBreadcrumbProps>) {
  // Build breadcrumb path
  const breadcrumbPath: Folder[] = [];
  let current = currentFolder;

  while (current) {
    breadcrumbPath.unshift(current);
    if (current.parentPublicId) {
      current =
        folders.find((f) => f.publicId === current!.parentPublicId) || null;
    } else {
      current = null;
    }
  }

  return (
    <nav className="flex items-center gap-1 text-sm" aria-label="Breadcrumb">
      <button
        onClick={() => onFolderClick(null)}
        className={`flex items-center gap-1 hover:text-primary-600 transition-colors ${
          !currentFolder ? "text-slate-900 font-medium" : "text-slate-500"
        }`}
      >
        <Home className="w-4 h-4" />
        <span>Documents</span>
      </button>

      {breadcrumbPath.map((folder, index) => (
        <div key={folder.publicId} className="flex items-center gap-1">
          <ChevronRight className="w-4 h-4 text-slate-400" />
          <button
            onClick={() => onFolderClick(folder)}
            className={`hover:text-primary-600 transition-colors ${
              index === breadcrumbPath.length - 1
                ? "text-slate-900 font-medium"
                : "text-slate-500"
            }`}
          >
            {folder.name}
          </button>
        </div>
      ))}
    </nav>
  );
}

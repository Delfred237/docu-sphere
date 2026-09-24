import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ChevronRight,
  ChevronDown,
  Folder as FolderIcon,
  MoreVertical,
  Trash2,
  Pencil,
} from "lucide-react";
import { folderService, type Folder } from "../services/folder.service";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { ConfirmationDialog } from "@/components/shared/ConfirmationDialog";
import { RenameDialog } from "@/components/shared/RenameDialog";

interface FolderTreeProps {
  selectedFolderId: string | null;
  onFolderSelect: (folder: Folder | null) => void;
}

interface FolderNodeProps {
  folder: Folder;
  level: number;
  selectedFolderId: string | null;
  onFolderSelect: (folder: Folder) => void;
}

function FolderNode({
  folder,
  level,
  selectedFolderId,
  onFolderSelect,
}: Readonly<FolderNodeProps>) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [renameDialogOpen, setRenameDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const queryClient = useQueryClient();

  // Ajoute cette mutation :
  const renameMutation = useMutation({
    mutationFn: (newName: string) =>
      folderService.renameFolder(folder.publicId, newName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["root-folders"] });
      queryClient.invalidateQueries({ queryKey: ["folder-children"] });
      queryClient.invalidateQueries({ queryKey: ["all-folders"] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => folderService.deleteFolder(folder.publicId),
    onSuccess: async () => {
      // Force le refetch des dossiers racine
      await queryClient.refetchQueries({ queryKey: ["root-folders"] });
      // Force le refetch des enfants si le dossier était ouvert
      await queryClient.refetchQueries({ queryKey: ["folder-children"] });
    },
    onError: (error: { response?: { data?: { message?: string } } } | null) => {
      alert(error?.response?.data?.message || "Failed to delete folder");
    },
  });

  const { data: children, isLoading } = useQuery({
    queryKey: ["folder-children", folder.publicId],
    queryFn: () => folderService.getSubFolders(folder.publicId),
    enabled: isExpanded,
  });

  const isSelected = selectedFolderId === folder.publicId;
  const hasChildren = children && children.length > 0;

  const handleClick = () => {
    onFolderSelect(folder);
  };

  const handleToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsExpanded(!isExpanded);
  };

  return (
    <div>
      <div
        role="presentation"
        onClick={handleClick}
        className={`flex items-center gap-2 px-3 py-2 rounded-md cursor-pointer transition-colors ${
          isSelected
            ? "bg-primary-50 text-primary-700"
            : "text-slate-700 hover:bg-slate-100"
        }`}
        style={{ paddingLeft: `${level * 16 + 12}px` }}
      >
        <button
          onClick={handleToggle}
          className="shrink-0 w-4 h-4 flex items-center justify-center text-slate-400 hover:text-slate-600"
          aria-label={isExpanded ? "Collapse folder" : "Expand folder"}
        >
          {isLoading ? (
            <div className="w-3 h-3 border-2 border-slate-300 border-t-primary-600 rounded-full animate-spin" />
          ) : (
            <>
              {isExpanded ? (
                <ChevronDown className="w-4 h-4" />
              ) : (
                <ChevronRight className="w-4 h-4" />
              )}
            </>
          )}
        </button>

        <FolderIcon
          className={`w-4 h-4 shrink-0 ${
            isSelected ? "text-primary-600" : "text-slate-400"
          }`}
          fill={isSelected ? "currentColor" : "none"}
        />

        <div className="group flex items-center justify-between flex-1">
          <span className="text-sm font-medium truncate">{folder.name}</span>
          <DropdownMenu open={menuOpen} onOpenChange={setMenuOpen}>
            <DropdownMenuTrigger
              render={
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-6 w-6 group-hover:opacity-100 transition-opacity"
                  onClick={(e) => e.stopPropagation()}
                >
                  <MoreVertical className="h-4 w-4 text-slate-400" />
                </Button>
              }
            />
            <DropdownMenuContent align="end">
              <DropdownMenuItem
                onClick={(e) => {
                  e.stopPropagation();
                  setMenuOpen(false);
                  setRenameDialogOpen(true);
                }}
              >
                <Pencil className="mr-2 h-4 w-4" />
                Rename
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                onClick={(e) => {
                  e.stopPropagation();
                  setMenuOpen(false);
                  setDeleteDialogOpen(true);
                }}
                className="text-red-600 focus:text-red-600 focus:bg-red-50"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Delete folder
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {isExpanded && hasChildren && (
        <div>
          {children.map((child) => (
            <FolderNode
              key={child.publicId}
              folder={child}
              level={level + 1}
              selectedFolderId={selectedFolderId}
              onFolderSelect={onFolderSelect}
            />
          ))}
        </div>
      )}

      <RenameDialog
        open={renameDialogOpen}
        onOpenChange={setRenameDialogOpen}
        currentName={folder.name}
        onRename={async (newName) => {
          await renameMutation.mutateAsync(newName);
        }}
        title="Rename folder"
        description="Enter a new name for this folder"
      />
      <ConfirmationDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        title="Delete folder"
        description={`Are you sure you want to delete "${folder.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        onConfirm={() => {
          deleteMutation.mutate();
          setDeleteDialogOpen(false);
        }}
        variant="destructive"
      />
    </div>
  );
}

export function FolderTree({
  selectedFolderId,
  onFolderSelect,
}: Readonly<FolderTreeProps>) {
  const { data: rootFolders, isLoading } = useQuery({
    queryKey: ["root-folders"],
    queryFn: folderService.getRootFolders,
  });

  const handleRootSelect = () => {
    onFolderSelect(null);
  };

  return (
    <div className="space-y-1">
      {/* Root node */}
      <div
        role="presentation"
        onClick={handleRootSelect}
        className={`flex items-center gap-2 px-3 py-2 rounded-md cursor-pointer transition-colors ${
          selectedFolderId === null
            ? "bg-primary-50 text-primary-700"
            : "text-slate-700 hover:bg-slate-100"
        }`}
      >
        <FolderIcon
          className={`w-4 h-4 shrink-0 ${
            selectedFolderId === null ? "text-primary-600" : "text-slate-400"
          }`}
          fill={selectedFolderId === null ? "currentColor" : "none"}
        />
        <span className="text-sm font-medium">All Documents</span>
      </div>

      {/* Loading state */}
      {isLoading && (
        <div className="px-3 py-2">
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 border-2 border-slate-300 border-t-primary-600 rounded-full animate-spin" />
            <span className="text-sm text-slate-500">Loading folders...</span>
          </div>
        </div>
      )}

      {/* Folder tree */}
      {rootFolders?.map((folder) => (
        <FolderNode
          key={folder.publicId}
          folder={folder}
          level={0}
          selectedFolderId={selectedFolderId}
          onFolderSelect={onFolderSelect}
        />
      ))}

      {/* Empty state */}
      {!isLoading && rootFolders?.length === 0 && (
        <div className="px-3 py-2 text-sm text-slate-500">No folders yet</div>
      )}
    </div>
  );
}

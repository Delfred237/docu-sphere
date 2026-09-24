import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface RenameDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  currentName: string;
  onRename: (newName: string) => Promise<void>;
  title?: string;
  description?: string;
}

export function RenameDialog({
  open,
  onOpenChange,
  currentName,
  onRename,
  title = "Rename",
  description = "Enter a new name",
}: Readonly<RenameDialogProps>) {
  const [name, setName] = useState(currentName);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setName(currentName);
      setError(null);
    }
  }, [open, currentName]);

  const renameMutation = useMutation({
    mutationFn: () => onRename(name),
    onSuccess: () => {
      onOpenChange(false);
    },
    onError: (error: { response?: { data?: { message?: string } } } | null) => {
      alert(error?.response?.data?.message || "Failed to delete folder");
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (name.trim() && name.trim() !== currentName) {
      renameMutation.mutate();
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            <DialogDescription>{description}</DialogDescription>
          </DialogHeader>

          <div className="py-4">
            <Label htmlFor="rename-input" className="mb-3">
              Name
            </Label>
            <Input
              id="rename-input"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                setError(null);
              }}
              autoFocus
              maxLength={255}
            />
            {error && <p className="text-sm text-red-600 mt-1">{error}</p>}
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={
                !name.trim() ||
                name.trim() === currentName ||
                renameMutation.isPending
              }
              className="bg-primary-600 hover:bg-primary-700 text-white"
            >
              {renameMutation.isPending ? "Saving..." : "Save"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

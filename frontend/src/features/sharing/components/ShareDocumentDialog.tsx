import { useState, useEffect } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Share2, Link2, Copy, Check, Download, Loader2 } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  shareLinkService,
  type ShareLink,
  type CreateShareLinkRequest,
} from "../services/shareLink.service";

interface ShareDocumentDialogProps {
  doc: { publicId: string; name: string } | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const EXPIRATION_OPTIONS = [
  { value: 1, label: "1 day" },
  { value: 7, label: "7 days" },
  { value: 30, label: "30 days" },
  { value: 90, label: "90 days" },
];

export function ShareDocumentDialog({
  doc,
  open,
  onOpenChange,
}: Readonly<ShareDocumentDialogProps>) {
  const queryClient = useQueryClient();
  const [expirationDays, setExpirationDays] = useState<number>(7);
  const [allowDownload, setAllowDownload] = useState<boolean>(true);
  const [createdLink, setCreatedLink] = useState<ShareLink | null>(null);
  const [copied, setCopied] = useState(false);
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string | null>(null);
  const [qrLoading, setQrLoading] = useState(false);
  const [qrError, setQrError] = useState(false);

  const createMutation = useMutation({
    mutationFn: (request: CreateShareLinkRequest) =>
      doc
        ? shareLinkService.createShareLink(doc.publicId, request)
        : Promise.reject(),
    onSuccess: async (link) => {
      setCreatedLink(link);
      await queryClient.invalidateQueries({ queryKey: ["share-links"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  // Fetch QR code when link is created (legitimate side effect)
  useEffect(() => {
    if (createdLink) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setQrLoading(true);
      setQrError(false);
      const token = localStorage.getItem("accessToken");
      fetch(shareLinkService.getQrCodeUrl(createdLink.publicId), {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })
        .then((res) => res.blob())
        .then((blob) => {
          const reader = new FileReader();
          reader.onloadend = () => setQrCodeDataUrl(reader.result as string);
          reader.readAsDataURL(blob);
        })
        .catch(() => setQrError(true))
        .finally(() => setQrLoading(false));
    }
  }, [createdLink]);

  const handleCreate = () => {
    createMutation.mutate({ allowDownload, expirationDays });
  };

  const handleCopy = async () => {
    if (!createdLink) return;
    try {
      await navigator.clipboard.writeText(
        shareLinkService.getShareUrl(createdLink.token),
      );
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error("Failed to copy:", error);
    }
  };

  const handleDownloadQr = () => {
    if (!qrCodeDataUrl || !createdLink) return;
    const a = window.document.createElement("a");
    a.href = qrCodeDataUrl;
    a.download = `qrcode-${createdLink.documentName.replace(/[^a-z0-9]/gi, "-").toLowerCase()}.png`;
    window.document.body.appendChild(a);
    a.click();
    a.remove();
  };

  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) {
      // Reset state when closing
      setCreatedLink(null);
      setCopied(false);
      setQrCodeDataUrl(null);
      setQrError(false);
      setExpirationDays(7);
      setAllowDownload(true);
      createMutation.reset();
    }
    onOpenChange(newOpen);
  };

  if (!doc) return null;

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Share2 className="h-5 w-5 text-primary-600" />
            {createdLink ? "Link created!" : "Share document"}
          </DialogTitle>
          <DialogDescription className="truncate">{doc.name}</DialogDescription>
        </DialogHeader>

        {!createdLink ? (
          /* === Configuration view === */
          <div className="space-y-5">
            {/* Expiration */}
            <div className="space-y-2">
              <Label>Link expiration</Label>
              <div className="grid grid-cols-4 gap-2">
                {EXPIRATION_OPTIONS.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setExpirationDays(option.value)}
                    className={`px-3 py-2 rounded-md text-sm font-medium border transition-colors ${
                      expirationDays === option.value
                        ? "bg-primary-50 border-primary-500 text-primary-700"
                        : "bg-white border-slate-200 text-slate-700 hover:bg-slate-50"
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Download toggle */}
            <div className="flex items-center justify-between p-3 rounded-md border border-slate-200 bg-slate-50">
              <div className="flex-1">
                <Label htmlFor="allow-download" className="cursor-pointer">
                  Allow download
                </Label>
                <p className="text-xs text-slate-500 mt-0.5">
                  Recipients can download the original file
                </p>
              </div>
              <button
                type="button"
                id="allow-download"
                role="switch"
                aria-checked={allowDownload}
                onClick={() => setAllowDownload(!allowDownload)}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                  allowDownload ? "bg-primary-600" : "bg-slate-300"
                }`}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                    allowDownload ? "translate-x-6" : "translate-x-1"
                  }`}
                />
              </button>
            </div>

            {/* Summary */}
            <div className="p-3 rounded-md bg-slate-50 border border-slate-200 text-sm space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-slate-600">Expires in</span>
                <span className="font-medium text-slate-900">
                  {expirationDays} day{expirationDays > 1 ? "s" : ""}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-600">Download allowed</span>
                <Badge
                  variant="outline"
                  className={
                    allowDownload
                      ? "bg-green-50 text-green-700 border-green-200"
                      : "bg-slate-100 text-slate-600 border-slate-200"
                  }
                >
                  {allowDownload ? "Yes" : "No"}
                </Badge>
              </div>
            </div>

            {createMutation.isError && (
              <div className="p-3 rounded-md bg-red-50 border border-red-200 text-sm text-red-700">
                Failed to create share link. Please try again.
              </div>
            )}

            <div className="flex gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => handleOpenChange(false)}
                disabled={createMutation.isPending}
                className="flex-1"
              >
                Cancel
              </Button>
              <Button
                type="button"
                onClick={handleCreate}
                disabled={createMutation.isPending}
                className="flex-1 bg-primary-600 hover:bg-primary-700 text-white"
              >
                {createMutation.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Creating...
                  </>
                ) : (
                  <>
                    <Share2 className="h-4 w-4 mr-2" />
                    Create link
                  </>
                )}
              </Button>
            </div>
          </div>
        ) : (
          /* === Success view === */
          <div className="space-y-5 overflow-hidden">
            {/* Share URL */}
            <div className="space-y-2">
              <Label>Share link</Label>
              <div className="flex items-center gap-2 w-full">
                <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-slate-50 rounded-md border border-slate-200 min-w-0 overflow-hidden">
                  <Link2 className="h-3.5 w-3.5 text-slate-400 flex-shrink-0" />
                  <code className="text-xs text-slate-700 break-all font-mono">
                    {shareLinkService.getShareUrl(createdLink.token)}
                  </code>
                </div>
                <Button
                  variant="outline"
                  size="icon"
                  onClick={handleCopy}
                  className="flex-shrink-0 h-10 w-10"
                >
                  {copied ? (
                    <Check className="h-4 w-4 text-green-600" />
                  ) : (
                    <Copy className="h-4 w-4" />
                  )}
                </Button>
              </div>
            </div>

            {/* QR Code */}
            <div className="space-y-2">
              <Label>QR Code</Label>
              <div className="flex items-center justify-center bg-white p-4 rounded-md border border-slate-200 w-full">
                {qrLoading ? (
                  <div className="w-32 h-32 flex items-center justify-center">
                    <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
                  </div>
                ) : qrError ? (
                  <div className="w-32 h-32 flex items-center justify-center text-sm text-slate-500">
                    Failed to load QR code
                  </div>
                ) : qrCodeDataUrl ? (
                  <img
                    src={qrCodeDataUrl}
                    alt="QR Code"
                    width={132}
                    height={132}
                    className="rounded max-w-full h-auto"
                  />
                ) : null}
              </div>
              {qrCodeDataUrl && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleDownloadQr}
                  className="w-full"
                >
                  <Download className="h-4 w-4 mr-2" />
                  Download QR Code
                </Button>
              )}
            </div>

            {/* Info */}
            <div className="p-3 rounded-md bg-slate-50 border border-slate-200 text-xs text-slate-600 space-y-1">
              <p>✓ Link created successfully</p>
              <p>✓ Anyone with the link can access the document</p>
              <p>✓ You can revoke this link anytime from the Shared page</p>
            </div>

            <Button
              type="button"
              onClick={() => handleOpenChange(false)}
              className="w-full bg-primary-600 hover:bg-primary-700 text-white"
            >
              Done
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

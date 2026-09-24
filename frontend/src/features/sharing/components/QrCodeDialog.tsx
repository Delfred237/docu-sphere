import { useState, useEffect } from "react";
import { Download, X } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
  shareLinkService,
  type ShareLink,
} from "../services/shareLink.service";

interface QrCodeDialogProps {
  link: ShareLink | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function QrCodeDialog({
  link,
  open,
  onOpenChange,
}: Readonly<QrCodeDialogProps>) {
  const [imageError, setImageError] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string | null>(null);

  //   useEffect(() => {
  //     if (open) {
  //       // eslint-disable-next-line react-hooks/set-state-in-effect
  //       setImageError(false);
  //       setIsLoading(true);
  //     }
  //   }, [open]);

  useEffect(() => {
    if (open && link) {
      const token = localStorage.getItem("accessToken");
      fetch(shareLinkService.getQrCodeUrl(link.publicId), {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })
        .then((res) => res.blob())
        .then((blob) => {
          const reader = new FileReader();
          reader.onloadend = () => setQrCodeDataUrl(reader.result as string);
          reader.readAsDataURL(blob);
        })
        .catch(() => setImageError(true))
        .finally(() => setIsLoading(false));
    }
  }, [open, link]);

  if (!link) return null;

  const qrCodeUrl = shareLinkService.getQrCodeUrl(link.publicId);
  const shareUrl = shareLinkService.getShareUrl(link.token);

  const handleDownloadQr = async () => {
    try {
      // Fetch avec le token d'auth
      const token = localStorage.getItem("accessToken");
      const response = await fetch(qrCodeUrl, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `qrcode-${link.documentName.replace(/[^a-z0-9]/gi, "-").toLowerCase()}.png`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Failed to download QR code:", error);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>QR Code</DialogTitle>
          <DialogDescription>
            Scan this code to access "{link.documentName}"
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col items-center py-4">
          <div className="relative bg-white p-4 rounded-xl border border-slate-200">
            {isLoading && (
              <div className="absolute inset-0 flex items-center justify-center bg-white/80 rounded-xl">
                <div className="w-6 h-6 border-2 border-slate-300 border-t-primary-600 rounded-full animate-spin" />
              </div>
            )}
            {qrCodeDataUrl ? (
              <img
                src={qrCodeDataUrl}
                alt="QR Code"
                width={200}
                height={200}
                className="rounded"
              />
            ) : (
              <div className="w-[200px] h-[200px] flex items-center justify-center">
                <div className="w-6 h-6 border-2 border-slate-300 border-t-primary-600 rounded-full animate-spin" />
              </div>
            )}
          </div>

          <div className="mt-4 text-center">
            <p className="text-xs text-slate-500 mb-1">Scanning URL</p>
            <code className="text-xs text-slate-700 font-mono break-all px-2 py-1 bg-slate-50 rounded">
              {shareUrl}
            </code>
          </div>
        </div>

        <div className="flex gap-2">
          <Button
            variant="outline"
            className="flex-1"
            onClick={handleDownloadQr}
            disabled={imageError}
          >
            <Download className="h-4 w-4 mr-2" />
            Download PNG
          </Button>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            <X className="h-4 w-4 mr-2" />
            Close
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

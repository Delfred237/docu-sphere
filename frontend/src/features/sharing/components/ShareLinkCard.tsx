import { useState } from "react";
import {
  Link2,
  Copy,
  Check,
  QrCode,
  Trash2,
  Download,
  Clock,
  ExternalLink,
} from "lucide-react";
import { formatDistanceToNow, parseISO } from "date-fns";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { FileIcon } from "@/features/documents/components/FileIcon";
import {
  shareLinkService,
  type ShareLink,
} from "../services/shareLink.service";

interface ShareLinkCardProps {
  link: ShareLink;
  onShowQrCode: (link: ShareLink) => void;
  onRevoke: (link: ShareLink) => void;
}

export function ShareLinkCard({
  link,
  onShowQrCode,
  onRevoke,
}: Readonly<ShareLinkCardProps>) {
  const [copied, setCopied] = useState(false);
  const shareUrl = shareLinkService.getShareUrl(link.token);

  const isExpired = new Date(link.expiresAt) < new Date();
  const expiresIn = formatDistanceToNow(parseISO(link.expiresAt), {
    addSuffix: true,
  });

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error("Failed to copy:", error);
    }
  };

  const handleOpenLink = () => {
    window.open(shareUrl, "_blank");
  };

  return (
    <Card className="bg-white border-slate-200 shadow-xs hover:shadow-sm transition-shadow">
      <CardContent className="p-5">
        <div className="flex flex-col sm:flex-row sm:items-start gap-4">
          {/* Icon */}
          <div className="h-12 w-12 rounded-lg bg-slate-50 border border-slate-200 flex items-center justify-center flex-shrink-0">
            <FileIcon mimeType={link.documentMimeType} className="h-6 w-6" />
          </div>

          {/* Info */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2 mb-1">
              <h3 className="font-semibold text-slate-900 truncate">
                {link.documentName}
              </h3>
              <Badge
                variant="outline"
                className={
                  isExpired
                    ? "bg-red-50 text-red-700 border-red-200 flex-shrink-0"
                    : "bg-green-50 text-green-700 border-green-200 flex-shrink-0"
                }
              >
                {isExpired ? "Expired" : "Active"}
              </Badge>
            </div>

            {/* Stats */}
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-slate-500 mb-3">
              <span className="flex items-center gap-1">
                <Clock className="h-3.5 w-3.5" />
                {isExpired ? `Expired ${expiresIn}` : `Expires ${expiresIn}`}
              </span>
              <span className="flex items-center gap-1">
                <Download className="h-3.5 w-3.5" />
                {link.downloadCount} download
                {link.downloadCount !== 1 ? "s" : ""}
              </span>
              {link.allowDownload && (
                <Badge variant="outline" className="text-xs">
                  Download enabled
                </Badge>
              )}
            </div>

            {/* URL */}
            <div className="flex items-center gap-2 mb-3">
              <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-slate-50 rounded-md border border-slate-200 min-w-0">
                <Link2 className="h-3.5 w-3.5 text-slate-400 flex-shrink-0" />
                <code className="text-xs text-slate-600 truncate font-mono">
                  {shareUrl}
                </code>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={handleCopy}
                className="flex-shrink-0"
              >
                {copied ? (
                  <>
                    <Check className="h-4 w-4 mr-1.5 text-green-600" />
                    Copied
                  </>
                ) : (
                  <>
                    <Copy className="h-4 w-4 mr-1.5" />
                    Copy
                  </>
                )}
              </Button>
            </div>

            {/* Actions */}
            <div className="flex flex-wrap items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => onShowQrCode(link)}
                disabled={isExpired}
              >
                <QrCode className="h-4 w-4 mr-1.5" />
                QR Code
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleOpenLink}
                disabled={isExpired}
              >
                <ExternalLink className="h-4 w-4 mr-1.5" />
                Open
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => onRevoke(link)}
                className="text-red-600 hover:text-red-700 hover:bg-red-50"
              >
                <Trash2 className="h-4 w-4 mr-1.5" />
                Revoke
              </Button>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

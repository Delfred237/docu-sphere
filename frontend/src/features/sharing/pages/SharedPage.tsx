import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Share2, Link2, Plus, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ShareLinkCard } from "../components/ShareLinkCard";
import { QrCodeDialog } from "../components/QrCodeDialog";
import { ConfirmationDialog } from "@/components/shared/ConfirmationDialog";
import { Alert } from "@/components/shared/Alert";
import {
  shareLinkService,
  type ShareLink,
} from "../services/shareLink.service";

export function SharedPage() {
  const queryClient = useQueryClient();
  const [qrCodeLink, setQrCodeLink] = useState<ShareLink | null>(null);
  const [revokeLink, setRevokeLink] = useState<ShareLink | null>(null);
  const [error, setError] = useState<string | null>(null);

  const {
    data: links = [],
    isLoading,
    isFetching,
    refetch,
  } = useQuery({
    queryKey: ["share-links"],
    queryFn: shareLinkService.getUserShareLinks,
  });

  const revokeMutation = useMutation({
    mutationFn: (publicId: string) =>
      shareLinkService.revokeShareLink(publicId),
    onSuccess: async () => {
      await queryClient.refetchQueries({ queryKey: ["share-links"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      setRevokeLink(null);
    },
    onError: () => {
      setError("Failed to revoke share link");
    },
  });

  const handleRevokeConfirm = () => {
    if (revokeLink) {
      revokeMutation.mutate(revokeLink.publicId);
    }
  };

  // Séparer les liens actifs et expirés
  const activeLinks = links.filter((l) => new Date(l.expiresAt) >= new Date());
  const expiredLinks = links.filter((l) => new Date(l.expiresAt) < new Date());

  return (
    <div className="p-4 md:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl md:text-2xl font-semibold text-slate-900">
            Shared Links
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Manage links shared with external collaborators
          </p>
        </div>
        <Button
          variant="outline"
          onClick={() => refetch()}
          disabled={isFetching} // ← Changé de isLoading à isFetching
          className="self-start sm:self-auto"
        >
          <Loader2
            className={`h-4 w-4 mr-2 ${isFetching ? "animate-spin" : ""}`}
          />
          Refresh
        </Button>
      </div>

      {/* Error */}
      {error && (
        <div className="flex items-start justify-between gap-4">
          <Alert variant="error" message={error} />
          <button
            onClick={() => setError(null)}
            className="text-sm text-slate-400 hover:text-slate-600 flex-shrink-0"
          >
            ✕
          </button>
        </div>
      )}

      {/* Stats summary */}
      {!isLoading && links.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white border border-slate-200 rounded-lg p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-primary-50 flex items-center justify-center">
                <Link2 className="h-5 w-5 text-primary-600" />
              </div>
              <div>
                <p className="text-xs text-slate-500 font-medium uppercase">
                  Total Links
                </p>
                <p className="text-2xl font-semibold text-slate-900">
                  {links.length}
                </p>
              </div>
            </div>
          </div>
          <div className="bg-white border border-slate-200 rounded-lg p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-green-50 flex items-center justify-center">
                <Share2 className="h-5 w-5 text-green-600" />
              </div>
              <div>
                <p className="text-xs text-slate-500 font-medium uppercase">
                  Active
                </p>
                <p className="text-2xl font-semibold text-slate-900">
                  {activeLinks.length}
                </p>
              </div>
            </div>
          </div>
          <div className="bg-white border border-slate-200 rounded-lg p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-slate-100 flex items-center justify-center">
                <Share2 className="h-5 w-5 text-slate-600" />
              </div>
              <div>
                <p className="text-xs text-slate-500 font-medium uppercase">
                  Downloads
                </p>
                <p className="text-2xl font-semibold text-slate-900">
                  {links.reduce((sum, l) => sum + l.downloadCount, 0)}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Loading state */}
      {isLoading && (
        <div className="flex justify-center py-16">
          <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
        </div>
      )}

      {/* Empty state */}
      {!isLoading && links.length === 0 && (
        <div className="bg-white border border-slate-200 rounded-xl p-12 text-center">
          <div className="h-16 w-16 rounded-full bg-slate-100 flex items-center justify-center mx-auto mb-4">
            <Share2 className="h-8 w-8 text-slate-400" />
          </div>
          <h3 className="text-lg font-semibold text-slate-900 mb-2">
            No shared links yet
          </h3>
          <p className="text-sm text-slate-500 mb-6 max-w-md mx-auto">
            Share your documents with external collaborators by creating secure,
            time-limited links from the Documents page.
          </p>
          <Button
            onClick={() => (window.location.href = "/documents")}
            className="bg-primary-600 hover:bg-primary-700 text-white"
          >
            <Plus className="h-4 w-4 mr-2" />
            Go to Documents
          </Button>
        </div>
      )}

      {/* Active links */}
      {!isLoading && activeLinks.length > 0 && (
        <div className="space-y-4">
          <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">
            Active Links ({activeLinks.length})
          </h2>
          <div className="space-y-3">
            {activeLinks.map((link) => (
              <ShareLinkCard
                key={link.publicId}
                link={link}
                onShowQrCode={setQrCodeLink}
                onRevoke={setRevokeLink}
              />
            ))}
          </div>
        </div>
      )}

      {/* Expired links */}
      {!isLoading && expiredLinks.length > 0 && (
        <div className="space-y-4">
          <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wide">
            Expired Links ({expiredLinks.length})
          </h2>
          <div className="space-y-3 opacity-75">
            {expiredLinks.map((link) => (
              <ShareLinkCard
                key={link.publicId}
                link={link}
                onShowQrCode={setQrCodeLink}
                onRevoke={setRevokeLink}
              />
            ))}
          </div>
        </div>
      )}

      {/* QR Code Dialog */}
      <QrCodeDialog
        link={qrCodeLink}
        open={!!qrCodeLink}
        onOpenChange={(open) => !open && setQrCodeLink(null)}
      />

      {/* Revoke confirmation */}
      <ConfirmationDialog
        open={!!revokeLink}
        onOpenChange={(open) => !open && setRevokeLink(null)}
        title="Revoke share link"
        description={`Are you sure you want to revoke this link? Anyone with this link will no longer be able to access "${revokeLink?.documentName}". This action cannot be undone.`}
        confirmLabel="Revoke link"
        onConfirm={handleRevokeConfirm}
        isLoading={revokeMutation.isPending}
        variant="destructive"
      />
    </div>
  );
}

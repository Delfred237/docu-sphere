import { apiClient } from "@/lib/axios";

export interface ShareLink {
  publicId: string;
  token: string;
  documentPublicId: string;
  documentName: string;
  documentMimeType: string;
  expiresAt: string;
  allowDownload: boolean;
  downloadCount: number;
  createdAt: string;
}

export interface CreateShareLinkRequest {
  allowDownload: boolean;
  expirationDays: number;
}

export const shareLinkService = {
  async getUserShareLinks(): Promise<ShareLink[]> {
    const response = await apiClient.get<ShareLink[]>("/v1/share-links");
    return response.data;
  },

  async revokeShareLink(publicId: string): Promise<void> {
    await apiClient.delete(`/v1/share-links/${publicId}`);
  },

  /**
   * Retourne l'URL publique de partage (pour copier dans le presse-papier)
   */
  getShareUrl(token: string): string {
    const baseUrl = window.location.origin;
    return `${baseUrl}/share/${token}`;
  },

  /**
   * Retourne l'URL du QR code (utilisée comme src d'une balise img)
   */
  getQrCodeUrl(linkPublicId: string): string {
    const baseUrl = import.meta.env.VITE_API_URL || "http://localhost:8080/api";
    return `${baseUrl}/v1/shared/share/${linkPublicId}/qrcode`;
  },

  // Ajoute cette méthode dans shareLinkService :
  async createShareLink(
    documentPublicId: string,
    request: CreateShareLinkRequest,
  ): Promise<ShareLink> {
    const response = await apiClient.post<ShareLink>(
      `/v1/shared/${documentPublicId}/share`,
      request,
    );
    return response.data;
  },
};

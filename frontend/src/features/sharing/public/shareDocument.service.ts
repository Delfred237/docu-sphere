import axios from "axios";

const API_BASE_URL =
  import.meta.env.VITE_API_URL || "http://localhost:8080/api";

export interface SharedDocumentInfo {
  publicId: string;
  name: string;
  originalFilename: string;
  mimeType: string;
  size: number;
  status: string;
  createdAt: string;
}

// Client axios SANS token d'authentification (endpoints publics)
const publicClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

export const sharedDocumentService = {
  async getSharedDocumentInfo(token: string): Promise<SharedDocumentInfo> {
    const response = await publicClient.get<SharedDocumentInfo>(
      `/v1/shared/${token}/info`,
    );
    return response.data;
  },

  async downloadSharedDocument(token: string, filename: string): Promise<void> {
    const response = await publicClient.get(`/v1/shared/${token}/download`, {
      responseType: "blob",
    });

    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = window.document.createElement("a");
    link.href = url;
    link.setAttribute("download", filename);
    window.document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};

// Types d'erreur pour un meilleur affichage
export type ShareLinkError = "NOT_FOUND" | "EXPIRED" | "REVOKED" | "UNKNOWN";

export function getShareLinkError(error: unknown): ShareLinkError {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 404) return "NOT_FOUND";
    if (error.response?.status === 410) return "EXPIRED";
    if (error.response?.data?.code === "LINK_EXPIRED") return "EXPIRED";
    if (error.response?.status === 403) return "REVOKED";
  }
  return "UNKNOWN";
}

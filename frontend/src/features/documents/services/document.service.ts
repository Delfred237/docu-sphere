import { apiClient } from "@/lib/axios";

export interface Document {
  publicId: string;
  name: string;
  originalFilename: string;
  mimeType: string;
  size: number;
  status: "DRAFT" | "PENDING_REVIEW" | "APPROVED" | "REJECTED";
  createdAt: string;
  updatedAt: string;
  folderPublicId?: string;
  folderName?: string;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface DocumentSearchParams {
  name?: string;
  status?: string;
  mimeType?: string;
  folderId?: string;
  rootOnly?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export const documentService = {
  async searchDocuments(params: DocumentSearchParams): Promise<Page<Document>> {
    const response = await apiClient.get<Page<Document>>("/v1/documents", {
      params,
    });
    return response.data;
  },

  async uploadDocument(file: File, folderId?: string): Promise<Document> {
    const formData = new FormData();
    formData.append("file", file);
    if (folderId) {
      formData.append("folderId", folderId);
    }

    const response = await apiClient.post<Document>("/v1/documents", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },

  async downloadDocument(publicId: string, filename: string): Promise<void> {
    const response = await apiClient.get(`/v1/documents/${publicId}/download`, {
      responseType: "blob",
    });

    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", filename);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async submitForReview(publicId: string): Promise<Document> {
    const response = await apiClient.post<Document>(
      `/v1/documents/${publicId}/submit`,
    );
    return response.data;
  },

  async approveDocument(publicId: string, comment?: string): Promise<Document> {
    const response = await apiClient.post<Document>(
      `/v1/documents/${publicId}/approve`,
      { comment },
    );
    return response.data;
  },

  async rejectDocument(publicId: string, comment?: string): Promise<Document> {
    const response = await apiClient.post<Document>(
      `/v1/documents/${publicId}/reject`,
      { comment },
    );
    return response.data;
  },

  async renameDocument(publicId: string, name: string): Promise<Document> {
    const response = await apiClient.patch<Document>(
      `/v1/documents/${publicId}/rename`,
      { name },
    );
    return response.data;
  },

  async deleteDocument(publicId: string): Promise<void> {
    console.log("Deleting document:", publicId);
    console.log("URL:", `/v1/documents/${publicId}`);
    const response = await apiClient.delete(`/v1/documents/${publicId}`);
    console.log("Response:", response);
    return response.data;
  },
};

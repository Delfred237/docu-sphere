import { apiClient } from "@/lib/axios";

export interface Folder {
  publicId: string;
  name: string;
  isRoot: boolean;
  parentPublicId: string | null;
  createdAt: string;
}

export interface CreateFolderRequest {
  name: string;
  parentId?: string;
}

export const folderService = {
  async getRootFolders(): Promise<Folder[]> {
    const response = await apiClient.get<Folder[]>("/v1/folders");
    return response.data;
  },

  async getSubFolders(parentId: string): Promise<Folder[]> {
    const response = await apiClient.get<Folder[]>(
      `/v1/folders/${parentId}/children`,
    );
    return response.data;
  },

  async createFolder(data: CreateFolderRequest): Promise<Folder> {
    const response = await apiClient.post<Folder>("/v1/folders", data);
    return response.data;
  },

  async renameFolder(publicId: string, name: string): Promise<Folder> {
    const response = await apiClient.patch<Folder>(
      `/v1/folders/${publicId}/rename`,
      { name },
    );
    return response.data;
  },

  async deleteFolder(publicId: string): Promise<void> {
    console.log("Deleting folder:", publicId);
    console.log("URL:", `/v1/folders/${publicId}`);
    const response = await apiClient.delete(`/v1/folders/${publicId}`);
    console.log("Response:", response);
    return response.data;
  },
};

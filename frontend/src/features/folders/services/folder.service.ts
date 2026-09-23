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
};

/**
 * Import API endpoints
 */
import apiClient from './client';
import { ImportJob, ApiResponse } from '../types';

export const importApi = {
  /**
   * Upload and import file
   */
  upload: async (
    file: File,
    projectId: number,
    dryRun: boolean = false
  ): Promise<ImportJob> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectId', projectId.toString());
    formData.append('dryRun', dryRun.toString());

    const response = await apiClient.post<ApiResponse<ImportJob>>(
      '/import-jobs',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data.data;
  },

  /**
   * Get import job status
   */
  getJob: async (id: number): Promise<ImportJob> => {
    const response = await apiClient.get<ApiResponse<ImportJob>>(`/import-jobs/${id}`);
    return response.data.data;
  },

  /**
   * Download error report CSV
   */
  downloadErrors: async (id: number): Promise<Blob> => {
    const response = await apiClient.get(`/import-jobs/${id}/errors`, {
      responseType: 'blob',
    });
    return response.data;
  },
};

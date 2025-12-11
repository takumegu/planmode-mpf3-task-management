/**
 * Project API endpoints
 */
import apiClient from './client';
import { Project, ApiResponse } from '../types';

export const projectsApi = {
  /**
   * Get all projects
   */
  getAll: async (status?: string): Promise<Project[]> => {
    const params = status ? { status } : {};
    const response = await apiClient.get<ApiResponse<Project[]>>('/projects', { params });
    return response.data.data;
  },

  /**
   * Get project by ID
   */
  getById: async (id: number): Promise<Project> => {
    const response = await apiClient.get<ApiResponse<Project>>(`/projects/${id}`);
    return response.data.data;
  },

  /**
   * Create new project
   */
  create: async (data: Partial<Project>): Promise<Project> => {
    const response = await apiClient.post<ApiResponse<Project>>('/projects', data);
    return response.data.data;
  },

  /**
   * Update project
   */
  update: async (id: number, data: Partial<Project>): Promise<Project> => {
    const response = await apiClient.patch<ApiResponse<Project>>(`/projects/${id}`, data);
    return response.data.data;
  },

  /**
   * Delete project
   */
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/projects/${id}`);
  },

  /**
   * Search projects by name
   */
  search: async (name: string): Promise<Project[]> => {
    const response = await apiClient.get<ApiResponse<Project[]>>('/projects/search', {
      params: { name },
    });
    return response.data.data;
  },
};

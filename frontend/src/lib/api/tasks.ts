/**
 * Task API endpoints
 */
import apiClient from './client';
import { Task, TaskDependency, ApiResponse } from '../types';

export const tasksApi = {
  /**
   * Get tasks by project
   */
  getByProject: async (
    projectId: number,
    params?: {
      from?: string;
      to?: string;
      status?: string;
    }
  ): Promise<Task[]> => {
    const response = await apiClient.get<ApiResponse<Task[]>>(
      `/projects/${projectId}/tasks`,
      { params }
    );
    return response.data.data;
  },

  /**
   * Get task by ID
   */
  getById: async (id: number): Promise<Task> => {
    const response = await apiClient.get<ApiResponse<Task>>(`/tasks/${id}`);
    return response.data.data;
  },

  /**
   * Create new task
   */
  create: async (projectId: number, data: Partial<Task>): Promise<Task> => {
    const response = await apiClient.post<ApiResponse<Task>>(
      `/projects/${projectId}/tasks`,
      data
    );
    return response.data.data;
  },

  /**
   * Update task
   */
  update: async (id: number, data: Partial<Task>): Promise<Task> => {
    const response = await apiClient.patch<ApiResponse<Task>>(`/tasks/${id}`, data);
    return response.data.data;
  },

  /**
   * Delete task
   */
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/tasks/${id}`);
  },
};

export const dependenciesApi = {
  /**
   * Get dependencies for a task
   */
  getByTask: async (taskId: number): Promise<TaskDependency[]> => {
    const response = await apiClient.get<ApiResponse<TaskDependency[]>>(
      `/tasks/${taskId}/dependencies`
    );
    return response.data.data;
  },

  /**
   * Create dependency
   */
  create: async (
    taskId: number,
    predecessorTaskId: number,
    type: string = 'FS'
  ): Promise<TaskDependency> => {
    const response = await apiClient.post<ApiResponse<TaskDependency>>(
      `/tasks/${taskId}/dependencies`,
      { predecessorTaskId, type }
    );
    return response.data.data;
  },

  /**
   * Delete dependency
   */
  delete: async (taskId: number, dependencyId: number): Promise<void> => {
    await apiClient.delete(`/tasks/${taskId}/dependencies/${dependencyId}`);
  },
};

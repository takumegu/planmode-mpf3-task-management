/**
 * Domain model type definitions matching backend DTOs
 */

export interface Project {
  id: number;
  name: string;
  startDate: string; // ISO date string
  endDate: string;
  status: 'active' | 'archived';
  createdAt: string;
  updatedAt: string;
}

export interface Task {
  id: number;
  projectId: number;
  taskCode?: string;
  name: string;
  assignee?: string;
  startDate: string; // ISO date string
  endDate: string;
  progress: number; // 0-100
  status: 'planned' | 'in_progress' | 'done' | 'blocked' | 'on_hold';
  parentTaskId?: number;
  isMilestone: boolean;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TaskDependency {
  id: number;
  taskId: number;
  predecessorTaskId: number;
  type: 'FS' | 'SS' | 'FF' | 'SF';
}

export interface ImportJob {
  id: number;
  sourceType: 'CSV' | 'Excel';
  status: 'PENDING' | 'DRY_RUN' | 'SUCCESS' | 'PARTIAL' | 'FAILED';
  executedAt: string;
  summary: ImportSummary;
  errors?: ValidationError[];
}

export interface ImportSummary {
  totalRows: number;
  successfulRows: number;
  failedRows: number;
  tasksCreated: number;
  tasksUpdated: number;
  dependenciesCreated: number;
}

export interface ValidationError {
  lineNumber?: number;
  field?: string;
  value?: string;
  errorCode?: string;
  errorMessage?: string;
}

export interface ApiResponse<T> {
  data: T;
  meta?: {
    requestId?: string;
    timestamp?: string;
  };
  errors?: Array<{
    code: string;
    field?: string;
    message: string;
  }>;
}

// Gantt-specific types
export interface GanttTask {
  id: string;
  name: string;
  start: Date;
  end: Date;
  progress: number;
  dependencies?: string; // Comma-separated task IDs
  type?: 'task' | 'milestone';
  assignee?: string;
  status?: string;
}

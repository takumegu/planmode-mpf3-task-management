/**
 * Adapter to convert between backend Task model and TOAST UI Gantt format
 */
import { Task, TaskDependency } from '../types';

export interface GanttData {
  id: string;
  title: string;
  start: string; // YYYY-MM-DD format
  end: string;
  progress: number; // 0-100
  type: 'task' | 'milestone';
  dependencies?: string;
  assignee?: string;
  status?: string;
}

/**
 * Convert Task to TOAST UI Gantt format
 */
export function taskToGanttData(task: Task, dependencies?: TaskDependency[]): GanttData {
  // Find dependencies where this task depends on others
  const taskDeps = dependencies?.filter(dep => dep.taskId === task.id) || [];
  const depString = taskDeps.length > 0
    ? taskDeps.map(dep => `${dep.predecessorTaskId}:${dep.type}`).join(',')
    : undefined;

  return {
    id: task.id.toString(),
    title: task.name,
    start: task.startDate,
    end: task.endDate,
    progress: task.progress,
    type: task.isMilestone ? 'milestone' : 'task',
    dependencies: depString,
    assignee: task.assignee,
    status: task.status,
  };
}

/**
 * Convert array of Tasks to TOAST UI Gantt format
 */
export function tasksToGanttData(tasks: Task[], dependencies: TaskDependency[]): GanttData[] {
  return tasks.map(task => taskToGanttData(task, dependencies));
}

/**
 * Parse dependency string back to TaskDependency format
 * Format: "taskId:type,taskId:type"
 */
export function parseDependencyString(
  taskId: number,
  depString: string
): Partial<TaskDependency>[] {
  if (!depString) return [];

  return depString.split(',').map(dep => {
    const [predecessorId, type = 'FS'] = dep.split(':');
    return {
      taskId,
      predecessorTaskId: parseInt(predecessorId, 10),
      type: type as 'FS' | 'SS' | 'FF' | 'SF',
    };
  });
}

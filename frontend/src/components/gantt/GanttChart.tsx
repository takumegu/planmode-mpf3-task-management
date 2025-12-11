'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Task, TaskDependency } from '@/lib/types';
import { tasksApi, dependenciesApi } from '@/lib/api';
import { tasksToGanttData, GanttData } from '@/lib/utils/ganttAdapter';

// Note: TOAST UI Gantt will be imported dynamically to avoid SSR issues
// import Gantt from '@toast-ui/react-gantt';

interface GanttChartProps {
  projectId: number;
  onTaskUpdate?: (task: Task) => void;
  onTaskCreate?: (task: Partial<Task>) => void;
  onDependencyCreate?: (taskId: number, predecessorId: number, type: string) => void;
}

export default function GanttChart({
  projectId,
  onTaskUpdate,
  onTaskCreate,
  onDependencyCreate,
}: GanttChartProps) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [dependencies, setDependencies] = useState<TaskDependency[]>([]);
  const [ganttData, setGanttData] = useState<GanttData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const ganttRef = useRef<any>(null);

  // Load tasks and dependencies
  useEffect(() => {
    loadData();
  }, [projectId]);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Load tasks
      const tasksData = await tasksApi.getByProject(projectId);
      setTasks(tasksData);

      // Load all dependencies for these tasks
      const allDeps: TaskDependency[] = [];
      for (const task of tasksData) {
        const deps = await dependenciesApi.getByTask(task.id);
        allDeps.push(...deps);
      }
      setDependencies(allDeps);

      // Convert to Gantt format
      const ganttTasks = tasksToGanttData(tasksData, allDeps);
      setGanttData(ganttTasks);
    } catch (err) {
      console.error('Failed to load Gantt data:', err);
      setError('Failed to load tasks');
    } finally {
      setLoading(false);
    }
  };

  const handleTaskUpdate = async (updatedGanttTask: GanttData) => {
    try {
      const taskId = parseInt(updatedGanttTask.id, 10);
      const updates: Partial<Task> = {
        name: updatedGanttTask.title,
        startDate: updatedGanttTask.start,
        endDate: updatedGanttTask.end,
        progress: updatedGanttTask.progress,
      };

      const updated = await tasksApi.update(taskId, updates);

      // Update local state
      setTasks(prev => prev.map(t => t.id === taskId ? updated : t));

      if (onTaskUpdate) {
        onTaskUpdate(updated);
      }
    } catch (err) {
      console.error('Failed to update task:', err);
      alert('Failed to update task');
      // Reload data to revert changes
      loadData();
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-lg text-gray-600">Loading Gantt chart...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-lg text-red-600">{error}</div>
      </div>
    );
  }

  // Placeholder for TOAST UI Gantt (will be implemented with actual library)
  return (
    <div className="gantt-container border rounded-lg bg-white shadow-sm">
      <div className="p-4 border-b bg-gray-50">
        <h3 className="text-lg font-semibold">Gantt Chart</h3>
        <p className="text-sm text-gray-600 mt-1">
          {tasks.length} tasks loaded
        </p>
      </div>
      <div className="p-4">
        <div className="bg-blue-50 border border-blue-200 rounded p-4 mb-4">
          <p className="text-sm text-blue-800">
            <strong>Note:</strong> TOAST UI Gantt integration placeholder.
            The actual Gantt chart will render here with drag-and-drop functionality,
            task bars, dependency lines, and timeline controls.
          </p>
        </div>

        {/* Simple table view as fallback */}
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Task</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Start</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">End</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Progress</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {ganttData.map((task) => (
                <tr key={task.id}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {task.title}
                    {task.type === 'milestone' && (
                      <span className="ml-2 px-2 py-1 text-xs bg-purple-100 text-purple-800 rounded">
                        Milestone
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{task.start}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{task.end}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div className="flex items-center">
                      <div className="w-16 bg-gray-200 rounded-full h-2 mr-2">
                        <div
                          className="bg-blue-600 h-2 rounded-full"
                          style={{ width: `${task.progress}%` }}
                        />
                      </div>
                      <span>{task.progress}%</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <span className={`px-2 py-1 text-xs rounded ${
                      task.status === 'done' ? 'bg-green-100 text-green-800' :
                      task.status === 'in_progress' ? 'bg-blue-100 text-blue-800' :
                      task.status === 'blocked' ? 'bg-red-100 text-red-800' :
                      'bg-gray-100 text-gray-800'
                    }`}>
                      {task.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

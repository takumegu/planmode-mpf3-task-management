'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { projectsApi } from '@/lib/api';
import { Project, Task } from '@/lib/types';
import GanttChart from '@/components/gantt/GanttChart';
import GanttToolbar from '@/components/gantt/GanttToolbar';
import TaskCreateDialog from '@/components/tasks/TaskCreateDialog';

export default function ProjectGanttPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = parseInt(params?.id as string, 10);

  const [project, setProject] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [ganttKey, setGanttKey] = useState(0);

  useEffect(() => {
    if (projectId) {
      loadProject();
    }
  }, [projectId]);

  const loadProject = async () => {
    try {
      setLoading(true);
      const data = await projectsApi.getById(projectId);
      setProject(data);
    } catch (err) {
      console.error('Failed to load project:', err);
      setError('Failed to load project');
    } finally {
      setLoading(false);
    }
  };

  const handleZoomChange = (level: 'day' | 'week' | 'month') => {
    console.log('Zoom changed to:', level);
    // TODO: Implement zoom functionality
  };

  const handleFilterChange = (status: string) => {
    console.log('Filter changed to:', status);
    // TODO: Implement filter functionality
  };

  const handleAddTask = () => {
    setIsCreateDialogOpen(true);
  };

  const handleTaskCreated = (task: Task) => {
    console.log('Task created:', task);
    // Force Gantt chart to reload by changing its key
    setGanttKey(prev => prev + 1);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-xl text-gray-600">Loading project...</div>
      </div>
    );
  }

  if (error || !project) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen">
        <div className="text-xl text-red-600 mb-4">{error || 'Project not found'}</div>
        <button
          onClick={() => router.push('/')}
          className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
        >
          Back to Projects
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <button
                onClick={() => router.push('/')}
                className="text-gray-500 hover:text-gray-700"
              >
                ← Back
              </button>
              <h1 className="text-2xl font-bold text-gray-900">{project.name}</h1>
            </div>
            <div className="flex items-center space-x-2">
              <button
                onClick={() => router.push(`/projects/${projectId}/tasks`)}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                Task List
              </button>
              <button
                onClick={() => router.push(`/projects/${projectId}/import`)}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                Import
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Gantt Chart */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="bg-white rounded-lg shadow">
          <GanttToolbar
            onZoomChange={handleZoomChange}
            onFilterChange={handleFilterChange}
            onAddTask={handleAddTask}
          />
          <GanttChart key={ganttKey} projectId={projectId} />
        </div>
      </div>

      {/* Task Create Dialog */}
      <TaskCreateDialog
        projectId={projectId}
        isOpen={isCreateDialogOpen}
        onClose={() => setIsCreateDialogOpen(false)}
        onTaskCreated={handleTaskCreated}
      />
    </div>
  );
}

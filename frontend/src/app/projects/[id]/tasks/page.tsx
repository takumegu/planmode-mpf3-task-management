'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { projectsApi } from '@/lib/api';
import { Project } from '@/lib/types';
import TaskList from '@/components/tasks/TaskList';

export default function TaskListPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = parseInt(params?.id as string, 10);

  const [project, setProject] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (projectId) {
      loadProject();
    }
  }, [projectId]);

  const loadProject = async () => {
    try {
      const data = await projectsApi.getById(projectId);
      setProject(data);
    } catch (err) {
      console.error('Failed to load project:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading || !project) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-white border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <button
                onClick={() => router.push(`/projects/${projectId}`)}
                className="text-gray-500 hover:text-gray-700"
              >
                ← Back to Gantt
              </button>
              <h1 className="text-2xl font-bold text-gray-900">{project.name} - Tasks</h1>
            </div>
            <button
              onClick={() => router.push(`/projects/${projectId}/import`)}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
            >
              Import Tasks
            </button>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <TaskList projectId={projectId} />
      </div>
    </div>
  );
}

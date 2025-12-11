'use client';

import React from 'react';

interface GanttToolbarProps {
  onZoomChange?: (level: 'day' | 'week' | 'month') => void;
  onFilterChange?: (status: string) => void;
  onAddTask?: () => void;
}

export default function GanttToolbar({
  onZoomChange,
  onFilterChange,
  onAddTask,
}: GanttToolbarProps) {
  return (
    <div className="flex items-center justify-between p-4 bg-white border-b">
      {/* Left side: Zoom controls */}
      <div className="flex items-center space-x-2">
        <label className="text-sm font-medium text-gray-700">View:</label>
        <div className="inline-flex rounded-md shadow-sm" role="group">
          <button
            onClick={() => onZoomChange?.('day')}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-l-lg hover:bg-gray-50"
          >
            Day
          </button>
          <button
            onClick={() => onZoomChange?.('week')}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border-t border-b border-gray-300 hover:bg-gray-50"
          >
            Week
          </button>
          <button
            onClick={() => onZoomChange?.('month')}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-r-lg hover:bg-gray-50"
          >
            Month
          </button>
        </div>
      </div>

      {/* Center: Filter controls */}
      <div className="flex items-center space-x-2">
        <label className="text-sm font-medium text-gray-700">Status:</label>
        <select
          onChange={(e) => onFilterChange?.(e.target.value)}
          className="px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">All</option>
          <option value="planned">Planned</option>
          <option value="in_progress">In Progress</option>
          <option value="done">Done</option>
          <option value="blocked">Blocked</option>
          <option value="on_hold">On Hold</option>
        </select>
      </div>

      {/* Right side: Action buttons */}
      <div className="flex items-center space-x-2">
        <button
          onClick={onAddTask}
          className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          + Add Task
        </button>
      </div>
    </div>
  );
}

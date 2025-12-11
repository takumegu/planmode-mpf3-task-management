'use client';

import React, { useRef } from 'react';

interface FileUploadProps {
  onFileSelect: (file: File) => void;
  accept?: string;
}

export default function FileUpload({ onFileSelect, accept = '.csv,.xlsx' }: FileUploadProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onFileSelect(file);
    }
  };

  const handleClick = () => {
    fileInputRef.current?.click();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0];
    if (file) {
      onFileSelect(file);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
  };

  return (
    <div
      onDrop={handleDrop}
      onDragOver={handleDragOver}
      onClick={handleClick}
      className="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center cursor-pointer hover:border-blue-400 transition-colors"
    >
      <input
        ref={fileInputRef}
        type="file"
        accept={accept}
        onChange={handleFileChange}
        className="hidden"
      />
      <div className="space-y-4">
        <div className="text-6xl text-gray-400">📁</div>
        <div>
          <p className="text-lg font-medium text-gray-900">
            Drop your CSV or Excel file here
          </p>
          <p className="text-sm text-gray-500 mt-1">or click to browse</p>
        </div>
        <p className="text-xs text-gray-400">Supported formats: .csv, .xlsx (max 10MB)</p>
      </div>
    </div>
  );
}

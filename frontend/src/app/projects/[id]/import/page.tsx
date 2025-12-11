'use client';

import React, { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { importApi } from '@/lib/api';
import { ImportJob } from '@/lib/types';
import FileUpload from '@/components/import/FileUpload';
import DryRunResults from '@/components/import/DryRunResults';

export default function ImportPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = parseInt(params?.id as string, 10);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [dryRunResult, setDryRunResult] = useState<ImportJob | null>(null);
  const [loading, setLoading] = useState(false);
  const [importComplete, setImportComplete] = useState(false);

  const handleFileSelect = async (file: File) => {
    setSelectedFile(file);
    setDryRunResult(null);
    setImportComplete(false);

    // Automatically run dry-run validation
    try {
      setLoading(true);
      const result = await importApi.upload(file, projectId, true);
      setDryRunResult(result);
    } catch (err) {
      console.error('Dry-run failed:', err);
      alert('Failed to validate file');
    } finally {
      setLoading(false);
    }
  };

  const handleProceedWithImport = async () => {
    if (!selectedFile) return;

    try {
      setLoading(true);
      const result = await importApi.upload(selectedFile, projectId, false);
      setImportComplete(true);
      alert(
        `Import completed!\n\nTasks created: ${result.summary.tasksCreated}\nTasks updated: ${result.summary.tasksUpdated}\nDependencies created: ${result.summary.dependenciesCreated}`
      );
      router.push(`/projects/${projectId}/tasks`);
    } catch (err) {
      console.error('Import failed:', err);
      alert('Import failed');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setSelectedFile(null);
    setDryRunResult(null);
    setImportComplete(false);
  };

  const downloadSampleTemplate = () => {
    window.open('/sample-import-template.csv', '_blank');
  };

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
                ← Back
              </button>
              <h1 className="text-2xl font-bold text-gray-900">Import Tasks</h1>
            </div>
            <button
              onClick={downloadSampleTemplate}
              className="px-4 py-2 text-sm font-medium text-blue-600 bg-white border border-blue-600 rounded-md hover:bg-blue-50"
            >
              Download Sample Template
            </button>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {loading && (
          <div className="text-center py-12">
            <div className="text-xl text-gray-600">Processing...</div>
          </div>
        )}

        {!loading && !dryRunResult && (
          <div className="space-y-6">
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <h3 className="font-medium text-blue-900">Import Instructions</h3>
              <ul className="mt-2 text-sm text-blue-800 space-y-1">
                <li>• Upload a CSV or Excel file containing your tasks</li>
                <li>• The file will be validated before import</li>
                <li>• Tasks with matching task_code will be updated</li>
                <li>• New tasks will be created</li>
              </ul>
            </div>

            <FileUpload onFileSelect={handleFileSelect} />
          </div>
        )}

        {!loading && dryRunResult && (
          <DryRunResults
            importJob={dryRunResult}
            onProceed={handleProceedWithImport}
            onCancel={handleCancel}
          />
        )}
      </div>
    </div>
  );
}

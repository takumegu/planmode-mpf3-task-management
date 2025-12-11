'use client';

import React from 'react';
import { ImportJob } from '@/lib/types';

interface DryRunResultsProps {
  importJob: ImportJob;
  onProceed: () => void;
  onCancel: () => void;
}

export default function DryRunResults({ importJob, onProceed, onCancel }: DryRunResultsProps) {
  const hasErrors = importJob.errors && importJob.errors.length > 0;

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-semibold mb-4">Import Validation Results</h2>

      {/* Summary */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="bg-blue-50 rounded-lg p-4">
          <div className="text-sm text-gray-600">Total Rows</div>
          <div className="text-2xl font-bold text-blue-600">{importJob.summary.totalRows}</div>
        </div>
        <div className="bg-green-50 rounded-lg p-4">
          <div className="text-sm text-gray-600">Valid Rows</div>
          <div className="text-2xl font-bold text-green-600">
            {importJob.summary.totalRows - importJob.summary.failedRows}
          </div>
        </div>
        <div className="bg-red-50 rounded-lg p-4">
          <div className="text-sm text-gray-600">Errors</div>
          <div className="text-2xl font-bold text-red-600">{importJob.summary.failedRows}</div>
        </div>
      </div>

      {/* Errors */}
      {hasErrors && (
        <div className="mb-6">
          <h3 className="text-lg font-medium mb-2 text-red-600">Validation Errors</h3>
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 max-h-96 overflow-y-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b border-red-200">
                  <th className="text-left py-2 px-2 font-medium">Line</th>
                  <th className="text-left py-2 px-2 font-medium">Field</th>
                  <th className="text-left py-2 px-2 font-medium">Value</th>
                  <th className="text-left py-2 px-2 font-medium">Error</th>
                </tr>
              </thead>
              <tbody>
                {importJob.errors?.map((error, index) => (
                  <tr key={index} className="border-b border-red-100">
                    <td className="py-2 px-2">{error.lineNumber}</td>
                    <td className="py-2 px-2 font-mono text-xs">{error.field}</td>
                    <td className="py-2 px-2 text-gray-600">{error.value}</td>
                    <td className="py-2 px-2 text-red-700">{error.errorMessage}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Success message */}
      {!hasErrors && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
          <p className="text-green-800">
            ✓ All rows are valid! You can proceed with the import.
          </p>
        </div>
      )}

      {/* Actions */}
      <div className="flex justify-end space-x-4">
        <button
          onClick={onCancel}
          className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
        >
          Cancel
        </button>
        {!hasErrors && (
          <button
            onClick={onProceed}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
          >
            Proceed with Import
          </button>
        )}
      </div>
    </div>
  );
}

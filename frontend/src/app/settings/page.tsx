'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';

export default function SettingsPage() {
  const router = useRouter();
  const [workingDays, setWorkingDays] = useState({
    monday: true,
    tuesday: true,
    wednesday: true,
    thursday: true,
    friday: true,
    saturday: false,
    sunday: false,
  });

  const handleWorkingDayChange = (day: keyof typeof workingDays) => {
    setWorkingDays(prev => ({ ...prev, [day]: !prev[day] }));
  };

  const handleSaveSettings = () => {
    // TODO: Implement API call to save settings
    alert('Settings saved (API integration pending)');
  };

  const handleHolidayFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // TODO: Implement holiday CSV upload
      alert(`Holiday file selected: ${file.name} (API integration pending)`);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
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
              <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Working Days Configuration */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4">Working Days Configuration</h2>
          <p className="text-sm text-gray-600 mb-4">
            Select which days are considered working days for date calculations
          </p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {Object.entries(workingDays).map(([day, isWorking]) => (
              <label key={day} className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={isWorking}
                  onChange={() => handleWorkingDayChange(day as keyof typeof workingDays)}
                  className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                />
                <span className="text-sm capitalize">{day}</span>
              </label>
            ))}
          </div>
        </div>

        {/* Holiday Import */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4">Holiday Configuration</h2>
          <p className="text-sm text-gray-600 mb-4">
            Upload a CSV file containing holiday dates (format: YYYY-MM-DD)
          </p>
          <div className="space-y-4">
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-6">
              <label className="flex flex-col items-center cursor-pointer">
                <span className="text-4xl mb-2">📅</span>
                <span className="text-sm text-gray-600">Click to upload holidays CSV</span>
                <input
                  type="file"
                  accept=".csv"
                  onChange={handleHolidayFileUpload}
                  className="hidden"
                />
              </label>
            </div>
            <p className="text-xs text-gray-500">
              CSV format: One date per line in YYYY-MM-DD format
            </p>
          </div>
        </div>

        {/* Save Button */}
        <div className="flex justify-end">
          <button
            onClick={handleSaveSettings}
            className="px-6 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
          >
            Save Settings
          </button>
        </div>

        {/* Info Box */}
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <p className="text-sm text-yellow-800">
            <strong>Note:</strong> Settings API integration is pending. This is a UI placeholder
            that demonstrates the intended functionality. Backend endpoints need to be implemented
            in Week 9-10 or Phase 1.1.
          </p>
        </div>
      </div>
    </div>
  );
}

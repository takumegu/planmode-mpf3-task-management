import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Task Management - Gantt Chart',
  description: 'Project and task management with Gantt chart visualization',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}

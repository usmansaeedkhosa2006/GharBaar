'use client';
// [PAGE] /admin/notifications - admin notifications (new listings, claims, reports)
import { AppShell } from '@/ui/components/AppShell';
import { NotificationsScreen } from '@/ui/screens/NotificationsScreen';

export default function Page() {
  return <AppShell portal="admin" title="Notifications"><NotificationsScreen /></AppShell>;
}

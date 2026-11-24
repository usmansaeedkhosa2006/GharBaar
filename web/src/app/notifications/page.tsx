'use client';
// [PAGE] /notifications - customers and agents
import { AppShell } from '@/ui/components/AppShell';
import { NotificationsScreen } from '@/ui/screens/NotificationsScreen';

const ALLOW = ['agent' as const];

export default function Page() {
  return <AppShell portal="customer" allow={ALLOW} title="Notifications"><NotificationsScreen /></AppShell>;
}

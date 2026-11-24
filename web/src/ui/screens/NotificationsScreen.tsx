'use client';
// =====================================================================
// [UI] NotificationsScreen - messages sent through the Notifier
// (listing verified/rejected, visit requested/answered, report resolved)
// =====================================================================
import { useEffect, useState } from 'react';
import { Bell } from 'lucide-react';
import { useMe } from '../components/AppShell';
import { Empty, Loading } from '../components/ui';
import { api, fmtDateTime } from '../api';

export function NotificationsScreen() {
  const { refreshBell } = useMe();
  const [items, setItems] = useState<any[] | null>(null);

  useEffect(() => {
    api<{ items: any[] }>('/api/notifications').then(async (r) => {
      setItems(r.items);
      await api('/api/notifications', { method: 'POST' });        // mark as read once shown
      refreshBell();
    }).catch(() => setItems([]));
  }, [refreshBell]);

  return (
    <section className="card col">
      <h3>Recent notifications</h3>
      {!items ? <Loading /> : items.length === 0 ? <Empty title="You're all caught up" sub="No notifications yet." />
        : items.map((n) => (
          <div key={n.notificationID} className="row" style={{ alignItems: 'flex-start' }}>
            <span className="icon-btn" style={{ background: n.read ? '#f3f4f6' : undefined, color: n.read ? '#9ca3af' : undefined }}><Bell size={16} /></span>
            <div className="col" style={{ gap: 2 }}>
              <span style={{ fontWeight: n.read ? 400 : 600 }}>{n.message}</span>
              <span className="muted small">{fmtDateTime(n.createdAt)}</span>
            </div>
          </div>
        ))}
    </section>
  );
}

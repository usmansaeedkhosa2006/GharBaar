'use client';
// =====================================================================
// [PAGE] /agent/visits - visit requests from customers (UC-12);
// the agent accepts or declines and the customer is notified.
// =====================================================================
import { useCallback, useEffect, useState } from 'react';
import { AppShell } from '@/ui/components/AppShell';
import { Empty, Loading, StatusBadge } from '@/ui/components/ui';
import { useToast } from '@/ui/components/Toast';
import { api, fmtDateTime, photoUrl } from '@/ui/api';

export default function Page() {
  return <AppShell portal="agent" title="Visit Requests"><Visits /></AppShell>;
}

function Visits() {
  const toast = useToast();
  const [list, setList] = useState<any[] | null>(null);
  const load = useCallback(() => api<{ visits: any[] }>('/api/agent/visits').then((r) => setList(r.visits)).catch((e) => toast(e.message)), [toast]);
  useEffect(() => { load(); }, [load]);

  async function respond(id: number, accept: boolean) {
    try {
      await api('/api/agent/visits', { body: { requestId: id, accept } });
      toast(`Visit ${accept ? 'accepted' : 'declined'}. The customer has been notified.`);
      load();
    } catch (e: any) { toast(e.message); }
  }

  return (
    <div className="col" style={{ gap: 14 }}>
      <h2>Requests for your properties</h2>
      {!list ? <Loading /> : list.length === 0 ? <Empty title="No visit requests" sub="Customers' visit requests for your listings will appear here." />
        : list.map((v) => (
          <div key={v.requestID} className="card list-row">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={photoUrl(v.listing.photoPath)} alt="" />
            <div className="col" style={{ gap: 4, flex: 1 }}>
              <h3>{v.listing.title}</h3>
              <b>{fmtDateTime(v.proposedDateTime)}</b>
              <span className="muted">Customer: {v.customer.name} · {v.customer.phone}</span>
              <span className="muted small">{v.notes ? `“${v.notes}”` : 'No notes'}</span>
            </div>
            <StatusBadge status={v.status} />
            {v.status === 'PENDING' && (
              <div className="col" style={{ gap: 8 }}>
                <button className="btn btn-success" onClick={() => respond(v.requestID, true)}>Accept</button>
                <button className="btn btn-danger" onClick={() => respond(v.requestID, false)}>Decline</button>
              </div>
            )}
          </div>
        ))}
    </div>
  );
}

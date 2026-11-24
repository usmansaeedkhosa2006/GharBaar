'use client';
// =====================================================================
// [PAGE] /admin/all-listings - every listing with a status filter;
// the Admin can remove fraudulent listings (reason sent to the agent).
// =====================================================================
import { useCallback, useEffect, useState } from 'react';
import { Trash2 } from 'lucide-react';
import { AppShell } from '@/ui/components/AppShell';
import { ChipGroup, Empty, ErrorBox, Field, Loading, Modal, StatusBadge } from '@/ui/components/ui';
import { useToast } from '@/ui/components/Toast';
import { api, photoUrl, pkr } from '@/ui/api';

const FILTERS: Record<string, string> = { Verified: 'VERIFIED', 'Pending Verification': 'PENDING_VERIFICATION', Rejected: 'REJECTED' };

export default function Page() {
  return <AppShell portal="admin" title="All Listings"><AllListings /></AppShell>;
}

function AllListings() {
  const toast = useToast();
  const [list, setList] = useState<any[] | null>(null);
  const [filter, setFilter] = useState('');
  const [removing, setRemoving] = useState<any>(null);
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => api<{ listings: any[] }>('/api/admin/listings?scope=all').then((r) => setList(r.listings)).catch((e) => toast(e.message)), [toast]);
  useEffect(() => { load(); }, [load]);

  async function remove() {
    setError(null);
    try {
      await api(`/api/admin/listings/${removing.listingID}`, { body: { action: 'remove', reason } });
      toast('Listing removed. The agent has been informed.');
      setRemoving(null); setReason('');
      load();
    } catch (e: any) { setError(e.message); }
  }

  const shown = (list ?? []).filter((l) => !filter || l.listingStatus === FILTERS[filter]);

  return (
    <div className="col" style={{ gap: 16 }}>
      <ChipGroup options={Object.keys(FILTERS)} value={filter} onChange={setFilter} anyLabel="All" />
      {!list ? <Loading /> : shown.length === 0 ? <Empty title="No listings" sub="Nothing matches this filter." />
        : shown.map((l) => (
          <div key={l.listingID} className="card list-row">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={photoUrl(l.photoPath)} alt="" />
            <div className="col" style={{ gap: 4, flex: 1 }}>
              <h3>{l.title}</h3>
              <span className="muted">{l.address} · {l.cdaSector} · Agent: {l.owner.name}</span>
              <b>{pkr(l.rent)} /month</b>
            </div>
            <StatusBadge status={l.listingStatus} /><StatusBadge status={l.propertyStatus} />
            <button className="btn btn-danger" onClick={() => setRemoving(l)}><Trash2 size={15} />Remove</button>
          </div>
        ))}

      {removing && (
        <Modal title={`Remove “${removing.title}”?`} onClose={() => setRemoving(null)}
          footer={<><button className="btn btn-ghost" onClick={() => setRemoving(null)}>Cancel</button><button className="btn btn-danger" onClick={remove}>Remove listing</button></>}>
          <Field label="Reason (sent to the agent)"><textarea className="textarea" value={reason} onChange={(e) => setReason(e.target.value)} /></Field>
          <ErrorBox error={error} />
        </Modal>
      )}
    </div>
  );
}

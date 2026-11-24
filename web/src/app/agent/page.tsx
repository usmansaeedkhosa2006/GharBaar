'use client';
// =====================================================================
// [PAGE] /agent - My Listings (Agent home)
// UC-04 step 1: agent opens his listings and selects one to edit.
// Shows verification status and the Admin's rejection reason.
// =====================================================================
import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { Pencil, PlusSquare, Trash2 } from 'lucide-react';
import { AppShell } from '@/ui/components/AppShell';
import { Empty, ErrorBox, Field, Loading, Modal, StatusBadge } from '@/ui/components/ui';
import { useToast } from '@/ui/components/Toast';
import { api, photoUrl, pkr } from '@/ui/api';

export default function Page() {
  return <AppShell portal="agent" title="My Listings"><MyListings /></AppShell>;
}

function MyListings() {
  const toast = useToast();
  const [list, setList] = useState<any[] | null>(null);
  const [del, setDel] = useState<any>(null);

  const load = useCallback(() => api<{ listings: any[] }>('/api/agent/listings').then((r) => setList(r.listings)).catch((e) => toast(e.message)), [toast]);
  useEffect(() => { load(); }, [load]);

  const count = (s: string) => list?.filter((l) => l.listingStatus === s).length ?? 0;

  return (
    <div className="col" style={{ gap: 18 }}>
      <div className="row"><h2 style={{ flex: 1 }}>Your properties</h2>
        <Link className="btn btn-primary" href="/agent/new"><PlusSquare size={16} />Create Property Listing</Link></div>

      <div className="grid-stats">
        {[['Total', list?.length ?? 0, '#2f80ed'], ['Verified', count('VERIFIED'), '#16a34a'],
          ['Pending', count('PENDING_VERIFICATION'), '#d97706'], ['Rejected', count('REJECTED'), '#dc2626']].map(([label, n, c]) => (
          <div key={label as string} className="stat" style={{ cursor: 'default' }}>
            <span className="muted">{label}</span><div className="value" style={{ color: c as string }}>{n}</div></div>
        ))}
      </div>

      {!list ? <Loading /> : list.length === 0
        ? <Empty title="No listings yet" sub="Create your first property listing. The Admin will verify it before it becomes public." />
        : list.map((l) => (
          <div key={l.listingID} className="card list-row">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={photoUrl(l.photoPath)} alt="" style={{ width: 170, height: 110 }} />
            <div className="col" style={{ gap: 6, flex: 1 }}>
              <div className="row wrap"><h3>{l.title}</h3><StatusBadge status={l.listingStatus} /><StatusBadge status={l.propertyStatus} /></div>
              <span className="muted">{l.address} · Sector {l.cdaSector} · {l.propertyType}</span>
              <b>{pkr(l.rent)} /month · {l.facilities.length} facilities</b>
              {l.listingStatus === 'REJECTED' && <span className="error">Rejected by Admin: {l.rejectionReason} - edit the listing to resubmit it.</span>}
              {l.listingStatus === 'PENDING_VERIFICATION' && <span className="muted small">Waiting for Admin verification before it becomes public.</span>}
            </div>
            <div className="col" style={{ gap: 8 }}>
              <Link className="btn btn-outline" href={`/agent/${l.listingID}/edit`}><Pencil size={15} />Edit</Link>
              <button className="btn btn-danger" onClick={() => setDel(l)}><Trash2 size={15} />Delete</button>
            </div>
          </div>
        ))}

      {del && <DeleteModal listing={del} onClose={() => setDel(null)} onDone={() => { setDel(null); load(); }} />}
    </div>
  );
}

// ---------------------------------------------------------------------
// [UC-04 alternative] delete with a reason
// ---------------------------------------------------------------------
function DeleteModal({ listing, onClose, onDone }: { listing: any; onClose: () => void; onDone: () => void }) {
  const toast = useToast();
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  async function submit() {
    try {
      await api(`/api/agent/listings/${listing.listingID}`, { method: 'DELETE', body: { reason } });
      toast('Listing deleted.');
      onDone();
    } catch (e: any) { setError(e.message); }
  }
  return (
    <Modal title={`Delete “${listing.title}”?`} onClose={onClose}
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button><button className="btn btn-danger" onClick={submit}>Delete listing</button></>}>
      <Field label="Reason for deleting"><textarea className="textarea" value={reason} onChange={(e) => setReason(e.target.value)} /></Field>
      <ErrorBox error={error} />
    </Modal>
  );
}

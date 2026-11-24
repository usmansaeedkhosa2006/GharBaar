'use client';
// =====================================================================
// [PAGE] /admin/listings - UC-06 Verify Listing
// Pending listings -> full details -> Approve, or Reject with reason.
// =====================================================================
import { useCallback, useEffect, useState } from 'react';
import { Check, X } from 'lucide-react';
import { AppShell } from '@/ui/components/AppShell';
import { Badge, Empty, ErrorBox, Field, Loading, Modal, StatusBadge } from '@/ui/components/ui';
import { useToast } from '@/ui/components/Toast';
import { api, fmtDateTime, photoUrl, pkr } from '@/ui/api';

export default function Page() {
  return <AppShell portal="admin" title="Verify Listings"><VerifyListings /></AppShell>;
}

function VerifyListings() {
  const toast = useToast();
  const [list, setList] = useState<any[] | null>(null);
  const [sel, setSel] = useState<any>(null);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => api<{ listings: any[] }>('/api/admin/listings').then((r) => setList(r.listings)).catch((e) => toast(e.message)), [toast]);
  useEffect(() => { load(); }, [load]);

  async function act(action: 'approve' | 'reject') {
    setError(null);
    try {
      await api(`/api/admin/listings/${sel.listingID}`, { body: { action, reason } });
      toast(action === 'approve' ? 'Listing verified. The agent has been informed.' : 'Listing rejected. The agent has been informed.');
      setSel(null); setRejecting(false); setReason('');
      load();
    } catch (e: any) { setError(e.message); }
  }

  return (
    <div className="master">
      <section className="card col">
        <h3>Pending Verification</h3>
        {!list ? <Loading /> : list.length === 0 ? <Empty title="No listings waiting" sub="The queue is empty." />
          : list.map((l) => (
            <div key={l.listingID} className={`mrow list-row ${sel?.listingID === l.listingID ? 'on' : ''}`} onClick={() => { setSel(l); setError(null); }}>
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={photoUrl(l.photoPath)} alt="" style={{ width: 80, height: 58 }} />
              <div className="col" style={{ gap: 2 }}><b>{l.title}</b><span className="muted small">{l.owner.name} · {l.cdaSector}</span><b className="small">{pkr(l.rent)}</b></div>
            </div>
          ))}
      </section>

      <section className="card col">
        {!sel ? <Empty title="Nothing selected" sub="Select a listing from the list to review its details." /> : (
          <>
            <div className="row"><h2 style={{ flex: 1 }}>{sel.title}</h2><StatusBadge status={sel.listingStatus} /></div>
            <div className="thumbs">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              {sel.photos.map((p: string) => <img key={p} src={photoUrl(p)} alt="" style={{ width: 200, height: 130, opacity: 1 }} />)}
            </div>
            <div className="facts">
              <span className="muted">Agent</span><b>{sel.owner.name} ({sel.owner.phone})</b>
              <span className="muted">Address</span><b>{sel.address}</b>
              <span className="muted">CDA Sector</span><b>{sel.cdaSector}</b>
              <span className="muted">Type</span><b>{sel.propertyType}</b>
              <span className="muted">Rent</span><b>{pkr(sel.rent)} /month</b>
              <span className="muted">Furnishing</span><b>{sel.furnishing ? 'Furnished' : 'Unfurnished'}</b>
              <span className="muted">Occupancy</span><b>{sel.occupancyType}</b>
              <span className="muted">Submitted</span><b>{fmtDateTime(sel.createdAt)}</b>
            </div>
            <b>Description</b><span className="muted">{sel.description}</span>
            <b>Advertised facilities</b>
            <div className="chips">{sel.facilities.map((f: any) => <Badge key={f.facilityID}>{f.name}</Badge>)}</div>
            <ErrorBox error={error} />
            <div className="row">
              <button className="btn btn-success" onClick={() => act('approve')}><Check size={16} />Approve listing</button>
              <button className="btn btn-danger" onClick={() => setRejecting(true)}><X size={16} />Reject</button>
            </div>
          </>
        )}
      </section>

      {rejecting && (
        <Modal title={`Reject “${sel.title}”`} onClose={() => setRejecting(false)}
          footer={<><button className="btn btn-ghost" onClick={() => setRejecting(false)}>Cancel</button><button className="btn btn-danger" onClick={() => act('reject')}>Reject listing</button></>}>
          <Field label="Reason (sent to the agent)"><textarea className="textarea" value={reason} onChange={(e) => setReason(e.target.value)} /></Field>
          <ErrorBox error={error} />
        </Modal>
      )}
    </div>
  );
}

'use client';
// =====================================================================
// [PAGE] /admin/tenants - UC-07 Verify Tenant
// Unverified tenants -> info (CNIC, phone) + tenancy claims with proof
// documents and overlap flags -> Confirm, or Reject with reason.
// =====================================================================
import { useCallback, useEffect, useState } from 'react';
import { Check, FileText, X } from 'lucide-react';
import { AppShell } from '@/ui/components/AppShell';
import { Avatar, Badge, Empty, ErrorBox, Field, Loading, Modal, StatusBadge } from '@/ui/components/ui';
import { useToast } from '@/ui/components/Toast';
import { api, fmtDate, photoUrl } from '@/ui/api';

export default function Page() {
  return <AppShell portal="admin" title="Verify Tenants"><VerifyTenants /></AppShell>;
}

function VerifyTenants() {
  const toast = useToast();
  const [list, setList] = useState<any[] | null>(null);
  const [sel, setSel] = useState<any>(null);
  const [detail, setDetail] = useState<{ tenant: any; claims: any[] } | null>(null);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => api<{ tenants: any[] }>('/api/admin/tenants').then((r) => setList(r.tenants)).catch((e) => toast(e.message)), [toast]);
  useEffect(() => { load(); }, [load]);

  async function select(t: any) {
    setSel(t); setDetail(null); setError(null);
    try { setDetail(await api(`/api/admin/tenants/${t.userID}`)); } catch (e: any) { toast(e.message); }
  }

  async function act(action: 'approve' | 'reject') {
    setError(null);
    try {
      await api(`/api/admin/tenants/${sel.userID}`, { body: { action, reason } });
      toast(action === 'approve' ? 'Tenant verified and informed.' : 'Tenant rejected and informed.');
      setSel(null); setDetail(null); setRejecting(false); setReason('');
      load();
    } catch (e: any) { setError(e.message); }
  }

  return (
    <div className="master">
      <section className="card col">
        <h3>Tenants waiting for verification</h3>
        {!list ? <Loading /> : list.length === 0 ? <Empty title="No tenants waiting" sub="The queue is empty." />
          : list.map((t) => (
            <div key={t.userID} className={`mrow row ${sel?.userID === t.userID ? 'on' : ''}`} onClick={() => select(t)}>
              <Avatar name={t.name} />
              <div className="col" style={{ gap: 2 }}><b>{t.name}</b><span className="muted small">@{t.username} · CNIC {t.cnic}</span></div>
            </div>
          ))}
      </section>

      <section className="card col">
        {!sel ? <Empty title="Nothing selected" sub="Select a tenant to review their information and claims." /> : !detail ? <Loading /> : (
          <>
            <div className="row"><Avatar name={sel.name} size={52} /><h2>{sel.name}</h2></div>
            <div className="facts">
              <span className="muted">Username</span><b>@{detail.tenant.username}</b>
              <span className="muted">Phone</span><b>{detail.tenant.phone}</b>
              <span className="muted">CNIC</span><b>{detail.tenant.cnic}</b>
              <span className="muted">Status</span><span><StatusBadge status={detail.tenant.verificationStatus} /></span>
            </div>
            <h3>Tenancy claims</h3>
            {detail.claims.map((c) => (
              <div key={c.claimID} className="card-soft col" style={{ gap: 8 }}>
                <div className="row wrap"><b style={{ flex: 1 }}>{c.listing.title}</b>
                  {c.flagged && <Badge color="amber">FLAGGED: overlaps another claim</Badge>}<StatusBadge status={c.status} /></div>
                <span className="muted">{fmtDate(c.startDate)} to {fmtDate(c.endDate)} · Agent: {c.listing.ownerName}</span>
                <a className="btn btn-outline" style={{ alignSelf: 'flex-start' }} href={photoUrl(c.proofDocument)} target="_blank" rel="noreferrer"><FileText size={15} />View proof</a>
              </div>
            ))}
            <ErrorBox error={error} />
            <div className="row">
              <button className="btn btn-success" onClick={() => act('approve')}><Check size={16} />Confirm tenant as valid</button>
              <button className="btn btn-danger" onClick={() => setRejecting(true)}><X size={16} />Reject</button>
            </div>
          </>
        )}
      </section>

      {rejecting && (
        <Modal title={`Reject tenant ${sel.name}`} onClose={() => setRejecting(false)}
          footer={<><button className="btn btn-ghost" onClick={() => setRejecting(false)}>Cancel</button><button className="btn btn-danger" onClick={() => act('reject')}>Reject tenant</button></>}>
          <Field label="Reason (sent to the tenant)"><textarea className="textarea" value={reason} onChange={(e) => setReason(e.target.value)} /></Field>
          <ErrorBox error={error} />
        </Modal>
      )}
    </div>
  );
}

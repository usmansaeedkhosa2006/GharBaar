'use client';
// =====================================================================
// [PAGE] /admin/reports - UC-08 Resolve Report
// Open reports -> details -> remarks -> Resolved (reporter notified)
// =====================================================================
import { useCallback, useEffect, useState } from 'react';
import { Check } from 'lucide-react';
import { AppShell } from '@/ui/components/AppShell';
import { Chip, Empty, ErrorBox, Field, Loading, StatusBadge } from '@/ui/components/ui';
import { useToast } from '@/ui/components/Toast';
import { api, fmtDateTime } from '@/ui/api';

export default function Page() {
  return <AppShell portal="admin" title="Reports"><Reports /></AppShell>;
}

function Reports() {
  const toast = useToast();
  const [status, setStatus] = useState<'OPEN' | 'RESOLVED'>('OPEN');
  const [list, setList] = useState<any[] | null>(null);
  const [sel, setSel] = useState<any>(null);
  const [remarks, setRemarks] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback((s: string) => {
    setList(null);
    api<{ reports: any[] }>(`/api/admin/reports?status=${s}`).then((r) => setList(r.reports)).catch((e) => toast(e.message));
  }, [toast]);
  useEffect(() => { load(status); }, [status, load]);

  async function resolve() {
    setError(null);
    try {
      await api(`/api/admin/reports/${sel.reportID}`, { body: { remarks } });
      toast('Report resolved. The reporter has been notified.');
      setSel(null); setRemarks('');
      load(status);
    } catch (e: any) { setError(e.message); }
  }

  return (
    <div className="col" style={{ gap: 16 }}>
      <div className="chips">
        <Chip on={status === 'OPEN'} onClick={() => { setStatus('OPEN'); setSel(null); }}>Open</Chip>
        <Chip on={status === 'RESOLVED'} onClick={() => { setStatus('RESOLVED'); setSel(null); }}>Resolved</Chip>
      </div>
      <div className="master">
        <section className="card col">
          <h3>{status === 'OPEN' ? 'Open reports' : 'Resolved reports'}</h3>
          {!list ? <Loading /> : list.length === 0 ? <Empty title={status === 'OPEN' ? 'No open reports' : 'No resolved reports'} />
            : list.map((r) => (
              <div key={r.reportID} className={`mrow row ${sel?.reportID === r.reportID ? 'on' : ''}`} onClick={() => { setSel(r); setError(null); }}>
                <div className="col" style={{ gap: 2, flex: 1 }}><b>{r.type}</b>
                  <span className="muted small">{r.reporter.name} ({r.reporter.role.toLowerCase()}) · {fmtDateTime(r.dateTime)}</span></div>
                <StatusBadge status={r.status} />
              </div>
            ))}
        </section>

        <section className="card col">
          {!sel ? <Empty title="Nothing selected" sub="Select a report to review it." /> : (
            <>
              <div className="row"><h2 style={{ flex: 1 }}>{sel.type}</h2><StatusBadge status={sel.status} /></div>
              <span className="muted">Reported by {sel.reporter.name} · @{sel.reporter.username} · {sel.reporter.phone}</span>
              <span className="muted small">{fmtDateTime(sel.dateTime)}</span>
              <b>Description</b><p style={{ margin: 0 }}>{sel.description}</p>
              {sel.status === 'OPEN' ? (
                <>
                  <Field label="Resolution remarks"><textarea className="textarea" value={remarks} onChange={(e) => setRemarks(e.target.value)} placeholder="Action taken / resolution remarks (required)" /></Field>
                  <ErrorBox error={error} />
                  <div><button className="btn btn-success" onClick={resolve}><Check size={16} />Mark as resolved</button></div>
                </>
              ) : (<><b>Resolution remarks</b><span className="muted">{sel.resolutionRemarks}</span></>)}
            </>
          )}
        </section>
      </div>
    </div>
  );
}

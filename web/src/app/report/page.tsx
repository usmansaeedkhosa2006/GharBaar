'use client';
// =====================================================================
// [PAGE] /report - UC-05 Report Issue (Customer & Agent)
// Form (type + description) and my previous reports with the Admin's
// resolution remarks (result of UC-08).
// =====================================================================
import { Suspense, useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { AppShell } from '@/ui/components/AppShell';
import { ErrorBox, Field, Loading, StatusBadge } from '@/ui/components/ui';
import { useToast } from '@/ui/components/Toast';
import { api, fmtDateTime } from '@/ui/api';
import { REPORT_TYPES } from '@/bl/domain/constants';

const ALLOW = ['agent' as const];

export default function Page() {
  return <AppShell portal="customer" allow={ALLOW} title="Report Issue"><Suspense><ReportIssue /></Suspense></AppShell>;
}

function ReportIssue() {
  const params = useSearchParams();
  const toast = useToast();
  const [type, setType] = useState(params.get('type') ?? '');
  const [description, setDescription] = useState(params.get('about') ?? '');
  const [error, setError] = useState<string | null>(null);
  const [mine, setMine] = useState<any[] | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => api<{ reports: any[] }>('/api/reports').then((r) => setMine(r.reports)).catch(() => setMine([])), []);
  useEffect(() => { load(); }, [load]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true); setError(null);
    try {
      await api('/api/reports', { body: { type, description } });
      toast('Report submitted with status Open. The Admin has been informed.');
      setType(''); setDescription('');
      load();
    } catch (err: any) { setError(err.message); }
    finally { setBusy(false); }
  }

  return (
    <div className="col" style={{ gap: 20 }}>
      <form className="card col" onSubmit={submit} style={{ maxWidth: 760 }}>
        <h3>Report a problem</h3>
        <span className="muted">Fake listing, fraud, wrong information or a technical problem? Tell the Admin.</span>
        <Field label="Issue type">
          <select className="select" value={type} onChange={(e) => setType(e.target.value)} style={{ maxWidth: 320 }}>
            <option value="">Select issue type</option>
            {REPORT_TYPES.map((t) => <option key={t}>{t}</option>)}
          </select>
        </Field>
        <Field label="Description"><textarea className="textarea" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Describe the issue (at least 10 characters)" /></Field>
        <ErrorBox error={error} />
        <div><button className="btn btn-primary" disabled={busy}>Submit report</button></div>
      </form>

      <section className="card col">
        <h3>My reports</h3>
        {!mine ? <Loading /> : mine.length === 0 ? <span className="muted">You have not reported any issues.</span>
          : mine.map((r) => (
            <div key={r.reportID} className="row" style={{ alignItems: 'flex-start' }}>
              <div className="col" style={{ gap: 3, flex: 1 }}>
                <b>{r.type} · {fmtDateTime(r.dateTime)}</b>
                <span className="muted">{r.description}</span>
                {r.resolutionRemarks && <b className="small">Admin: {r.resolutionRemarks}</b>}
              </div>
              <StatusBadge status={r.status} />
            </div>
          ))}
      </section>
    </div>
  );
}

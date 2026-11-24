'use client';
// [UI] ReviewModal - used by the property page and My Activity
import { useState } from 'react';
import { Empty, ErrorBox, FacilityIcon, Field, Modal } from './ui';
import { useToast } from './Toast';
import { api } from '../api';

// ---------------------------------------------------------------------
// [UC-14] Submit Facility Review - confirm / deny every facility
// ---------------------------------------------------------------------
export function ReviewModal({ l, facilities, onClose, onSaved }: { l: { listingID: number; title: string }; facilities: any[]; onClose: () => void; onSaved?: () => void }) {
  const toast = useToast();
  const [answers, setAnswers] = useState<Record<number, boolean>>({});
  const [comments, setComments] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit() {
    setBusy(true); setError(null);
    try {
      await api(`/api/tenancy/review/${l.listingID}`, { body: { confirmations: answers, comments } });
      toast('Thank you! Your facility review is now part of the tenant history.');
      onSaved?.();
      onClose();
    } catch (e: any) { setError(e.message); setBusy(false); }
  }

  return (
    <Modal title="Review the facilities" onClose={onClose}
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={busy} onClick={submit}>Submit review</button></>}>
      <span className="muted">Was each facility advertised for “{l.title}” actually provided during your tenancy?</span>
      {facilities.length === 0 && <Empty title="No facilities advertised" />}
      {facilities.map((f) => (
        <div key={f.facilityID} className="row">
          <span className="amenity"><span className="ic"><FacilityIcon name={f.name} /></span><b>{f.name}</b></span>
          <span className="spacer" />
          <button type="button" className={`chip ${answers[f.facilityID] === true ? 'on' : ''}`} onClick={() => setAnswers({ ...answers, [f.facilityID]: true })}>Provided</button>
          <button type="button" className={`chip ${answers[f.facilityID] === false ? 'on' : ''}`} onClick={() => setAnswers({ ...answers, [f.facilityID]: false })}>Not provided</button>
        </div>
      ))}
      <Field label="Comments (optional)"><textarea className="textarea" value={comments} onChange={(e) => setComments(e.target.value)} placeholder="Help future tenants decide" /></Field>
      <span className="muted small">Reviews cannot be edited or deleted after submission.</span>
      <ErrorBox error={error} />
    </Modal>
  );
}

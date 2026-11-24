'use client';
// =====================================================================
// [PAGE] /property/:id
//   UC-10 View Property Details (photos, rent, amenities, location,
//         furnishing, occupancy, status, agent contact)
//   UC-11 View Tenant History        UC-12 Request Property Visit
//   UC-13 Submit Tenancy Claim       UC-14 Submit Facility Review
//   UC-05 Report this listing
// =====================================================================
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, CalendarDays, Flag, History, MapPin, MessageSquare, Phone, Star } from 'lucide-react';
import { AppShell, useMe } from '@/ui/components/AppShell';
import { Avatar, Badge, Empty, ErrorBox, FacilityIcon, Field, Loading, Modal, StatusBadge } from '@/ui/components/ui';
import { useToast } from '@/ui/components/Toast';
import { api, fmtDate, photoUrl, pkr } from '@/ui/api';
import { ReviewModal } from '@/ui/components/ReviewModal';

export default function Page() {
  return <AppShell portal="customer" title="Property Details"><Details /></AppShell>;
}

function Details() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [l, setL] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [photo, setPhoto] = useState<string | null>(null);
  const [modal, setModal] = useState<'visit' | 'claim' | 'review' | 'contact' | null>(null);
  const [facilities, setFacilities] = useState<any[]>([]);
  const [history, setHistory] = useState<any[] | null>(null);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const toast = useToast();

  useEffect(() => {
    api<{ listing: any }>(`/api/listings/${id}`).then((r) => { setL(r.listing); setPhoto(r.listing.photoPath); })
      .catch((e) => setError(e.message));                                         // ext. 1a
  }, [id]);

  // ---------- [UC-11] View Tenant History ----------
  const loadHistory = useCallback(async () => {
    setLoadingHistory(true);
    try { setHistory((await api<{ reviews: any[] }>(`/api/listings/${id}/history`)).reviews); }
    catch (e: any) { toast(e.message); }
    finally { setLoadingHistory(false); }
  }, [id, toast]);

  // ---------- [UC-14 step 1-2] eligibility check, then open review form ----------
  async function startReview() {
    try {
      setFacilities((await api<{ facilities: any[] }>(`/api/tenancy/review/${id}`)).facilities);
      setModal('review');
    } catch (e: any) { toast(e.message); }                                         // ext. 1a / 1b
  }

  if (error) return (
    <div className="card col" style={{ alignItems: 'flex-start' }}>
      <h2>{error}</h2>
      <button className="btn btn-primary" onClick={() => router.push('/home')}>Back to results</button>
    </div>
  );
  if (!l) return <Loading />;

  return (
    <div className="details">
      {/* ================= LEFT COLUMN ================= */}
      <div className="col" style={{ gap: 16, minWidth: 0 }}>
        <Link href="/home" className="row" style={{ fontWeight: 600 }}><ArrowLeft size={16} />Back to results</Link>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img className="gallery-main" src={photoUrl(photo)} alt={l.title} />
        <div className="thumbs">
          {l.photos.map((p: string) => (
            // eslint-disable-next-line @next/next/no-img-element
            <img key={p} src={photoUrl(p)} alt="" className={p === photo ? 'on' : ''} onClick={() => setPhoto(p)} />
          ))}
        </div>

        <section className="card col">
          <div className="row"><h1 style={{ flex: 1 }}>{l.title}</h1><StatusBadge status={l.propertyStatus} /></div>
          <div className="row muted"><MapPin size={16} />{l.address}, Islamabad</div>
          <p className="muted" style={{ margin: 0 }}>{l.description}</p>
        </section>

        <section className="card col">
          <h3>Amenities</h3>
          <div className="amenities">
            {l.facilities.map((f: any) => (
              <div key={f.facilityID} className="amenity"><span className="ic"><FacilityIcon name={f.name} /></span>{f.name}</div>
            ))}
          </div>
        </section>

        <section className="card col">
          <div className="row"><h3 style={{ flex: 1 }}>Tenant History</h3>
            <button className="btn btn-outline" onClick={loadHistory} disabled={loadingHistory}><History size={16} />View Tenant History</button></div>
          <span className="muted small">Feedback from previous tenants with a verified tenancy: were the advertised facilities actually provided?</span>
          {loadingHistory && <Loading />}
          {history && history.length === 0 && <span className="muted">No tenant history available yet.</span>}
          {history?.map((r) => <ReviewCard key={r.reviewID} r={r} />)}
        </section>
      </div>

      {/* ================= RIGHT COLUMN ================= */}
      <div className="col" style={{ gap: 16, marginTop: 38 }}>
        <section className="card col">
          <span className="muted">Monthly rent</span>
          <span className="price-big">{pkr(l.rent)}</span>
          <div className="facts">
            <span className="muted">Type</span><b>{l.propertyType}</b>
            <span className="muted">CDA Sector</span><b>{l.cdaSector}</b>
            <span className="muted">Furnishing</span><b>{l.furnishing ? 'Furnished' : 'Unfurnished'}</b>
            <span className="muted">Occupancy</span><b>{l.occupancyType}</b>
            <span className="muted">Status</span><span><StatusBadge status={l.propertyStatus} /></span>
          </div>
        </section>

        <section className="card col">
          <h3>For Details</h3>
          <div className="row">
            <Avatar name={l.owner.name} size={46} />
            <div className="col" style={{ gap: 0, flex: 1 }}><b>{l.owner.name}</b><span className="muted small">Agent / Owner</span></div>
            <button className="icon-btn" aria-label="Message agent" onClick={() => setModal('contact')}><MessageSquare size={18} /></button>
            <a className="icon-btn" aria-label="Call agent" href={`tel:${l.owner.phone}`}><Phone size={18} /></a>
          </div>
        </section>

        <section className="card col">
          <h3>Actions</h3>
          <button className="btn btn-primary btn-block" disabled={!l.available} onClick={() => setModal('visit')}><CalendarDays size={16} />Request Visit</button>
          {!l.available && <span className="muted small">Visits can only be requested while the property is available.</span>}
          <button className="btn btn-outline btn-block" onClick={() => setModal('claim')}>I lived here - Submit Tenancy Claim</button>
          <button className="btn btn-outline btn-block" onClick={startReview}><Star size={16} />Write Facility Review</button>
          <Link className="btn btn-ghost btn-block" href={`/report?type=Fake%20Listing&about=${encodeURIComponent(`About listing #${l.listingID} "${l.title}": `)}`}>
            <Flag size={16} />Report this listing</Link>
        </section>

        <section className="card col">
          <h3>Location</h3>
          <div className="map"><MapPin size={30} color="#f97316" /><Badge color="red">Sector {l.cdaSector}, ISLAMABAD</Badge></div>
        </section>
      </div>

      {modal === 'visit' && <VisitModal l={l} onClose={() => setModal(null)} />}
      {modal === 'claim' && <ClaimModal l={l} onClose={() => setModal(null)} />}
      {modal === 'review' && <ReviewModal l={l} facilities={facilities} onClose={() => setModal(null)} onSaved={loadHistory} />}
      {modal === 'contact' && (
        <Modal title={`Contact ${l.owner.name}`} onClose={() => setModal(null)}>
          <p>Phone / WhatsApp: <b>{l.owner.phone}</b></p>
          <p className="muted">Mention GharBaar listing #{l.listingID}.</p>
          <div className="row"><a className="btn btn-primary" href={`tel:${l.owner.phone}`}><Phone size={16} />Call</a>
            <a className="btn btn-outline" target="_blank" rel="noreferrer" href={`https://wa.me/92${l.owner.phone.slice(1)}`}>WhatsApp</a></div>
        </Modal>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------
// [UC-11] one review in the tenant history
// ---------------------------------------------------------------------
function ReviewCard({ r }: { r: any }) {
  return (
    <div className="card-soft col" style={{ gap: 10 }}>
      <div className="row">
        <Avatar name={r.tenantName} size={34} />
        <div className="col" style={{ gap: 2, flex: 1 }}>
          <div className="row"><b>{r.tenantName}</b><Badge color="green">Verified tenant</Badge></div>
          {r.tenancyStart && <span className="muted small">Lived here {fmtDate(r.tenancyStart)} - {fmtDate(r.tenancyEnd)}</span>}
        </div>
        <b>{r.confirmedCount}/{r.confirmations.length} facilities confirmed</b>
      </div>
      <div className="chips">
        {r.confirmations.map((c: any) => <Badge key={c.facility} color={c.confirmed ? 'green' : 'red'}>{c.confirmed ? '✓' : '✗'} {c.facility}</Badge>)}
      </div>
      {r.comments && <span className="muted">“{r.comments}”</span>}
      <span className="muted small">Submitted {fmtDate(r.submittedDate)}</span>
    </div>
  );
}

// ---------------------------------------------------------------------
// [UC-12] Request Property Visit
// ---------------------------------------------------------------------
function VisitModal({ l, onClose }: { l: any; onClose: () => void }) {
  const toast = useToast();
  const { refreshBell } = useMe();
  const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10);
  const [date, setDate] = useState(tomorrow);
  const [time, setTime] = useState('17:00');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit() {
    setBusy(true); setError(null);
    try {
      await api('/api/visits', { body: { listingId: l.listingID, dateTime: date && time ? `${date}T${time}:00+05:00` : null, notes } });
      toast(`Visit request sent to ${l.owner.name}.`);
      refreshBell();
      onClose();
    } catch (e: any) { setError(e.message); setBusy(false); }
  }

  return (
    <Modal title={`Request a visit`} onClose={onClose}
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={busy} onClick={submit}>Send request</button></>}>
      <span className="muted">{l.title}</span>
      <div className="grid-2">
        <Field label="Proposed date"><input className="input" type="date" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
        <Field label="Time"><input className="input" type="time" value={time} onChange={(e) => setTime(e.target.value)} /></Field>
      </div>
      <Field label="Notes (optional)"><textarea className="textarea" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="e.g. preferred contact time" /></Field>
      <span className="muted small">The agent is notified immediately and can accept or decline.</span>
      <ErrorBox error={error} />
    </Modal>
  );
}

// ---------------------------------------------------------------------
// [UC-13] Submit Tenancy Claim
// ---------------------------------------------------------------------
function ClaimModal({ l, onClose }: { l: any; onClose: () => void }) {
  const toast = useToast();
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [cnic, setCnic] = useState('');
  const [proof, setProof] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit() {
    setBusy(true); setError(null);
    const fd = new FormData();
    fd.set('listingId', String(l.listingID));
    fd.set('startDate', start);
    fd.set('endDate', end);
    fd.set('cnic', cnic);
    if (proof) fd.set('proof', proof);
    try {
      const r = await api<{ claim: any }>('/api/tenancy/claims', { form: fd });
      toast(r.claim.flagged ? 'Claim submitted. It overlaps another claim, so the Admin will review it closely.'
        : 'Claim submitted with status Pending. The Admin will verify it.');
      onClose();
    } catch (e: any) { setError(e.message); setBusy(false); }
  }

  return (
    <Modal title="Submit tenancy claim" onClose={onClose}
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={busy} onClick={submit}>Submit claim</button></>}>
      <span className="muted">{l.title}</span>
      <div className="grid-2">
        <Field label="Tenancy start"><input className="input" type="date" value={start} onChange={(e) => setStart(e.target.value)} /></Field>
        <Field label="Tenancy end"><input className="input" type="date" value={end} onChange={(e) => setEnd(e.target.value)} /></Field>
      </div>
      <Field label="Your CNIC"><input className="input" value={cnic} onChange={(e) => setCnic(e.target.value)} placeholder="12345-1234567-1" /></Field>
      <Field label="Proof document (rent agreement / receipt: PDF, JPG, PNG, max 4 MB)">
        <input className="input" type="file" accept=".pdf,.jpg,.jpeg,.png" onChange={(e) => setProof(e.target.files?.[0] ?? null)} />
      </Field>
      <span className="muted small">Your proof is only visible to the GharBaar Admin. Once verified, you can review the facilities.</span>
      <ErrorBox error={error} />
    </Modal>
  );
}

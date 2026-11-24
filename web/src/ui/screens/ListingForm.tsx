'use client';
// =====================================================================
// [UI] ListingForm
//   listingId == null -> UC-03 Create Property Listing
//   listingId != null -> UC-04 Edit Property Listing (or delete)
// =====================================================================
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { ImagePlus, Trash2 } from 'lucide-react';
import { ChipGroup, ErrorBox, Field, Loading, Modal } from '../components/ui';
import { useToast } from '../components/Toast';
import { api, compressImage, photoUrl } from '../api';
import { CDA_SECTORS, COMMON_FACILITIES, OCCUPANCY_TYPES, PROPERTY_TYPES } from '@/bl/domain/constants';

const STATUSES = [['AVAILABLE', 'Available'], ['OCCUPIED', 'Occupied'], ['UNDER_MAINTENANCE', 'Under maintenance']];

export function ListingForm({ listingId }: { listingId?: number }) {
  const router = useRouter();
  const toast = useToast();
  const [f, setF] = useState({ title: '', address: '', cdaSector: '', rent: '', propertyType: '', occupancyType: '', furnishing: '', description: '', propertyStatus: 'AVAILABLE' });
  const [facilities, setFacilities] = useState<string[]>([]);
  const [extra, setExtra] = useState<string[]>([]);
  const [custom, setCustom] = useState('');
  const [photos, setPhotos] = useState<File[]>([]);
  const [existing, setExisting] = useState<string[]>([]);
  const [rejection, setRejection] = useState<string | null>(null);
  const [loading, setLoading] = useState(!!listingId);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [del, setDel] = useState(false);
  const [reason, setReason] = useState('');

  // ---------- [UC-04 step 2] load current details ----------
  useEffect(() => {
    if (!listingId) return;
    api<{ listing: any }>(`/api/agent/listings/${listingId}`).then(({ listing: l }) => {
      setF({ title: l.title, address: l.address, cdaSector: l.cdaSector, rent: String(l.rent), propertyType: l.propertyType,
        occupancyType: l.occupancyType, furnishing: l.furnishing ? 'Furnished' : 'Unfurnished', description: l.description, propertyStatus: l.propertyStatus });
      const names = l.facilities.map((x: any) => x.name);
      setFacilities(names);
      setExtra(names.filter((n: string) => !COMMON_FACILITIES.includes(n)));
      setExisting(l.photos);
      if (l.listingStatus === 'REJECTED') setRejection(l.rejectionReason);
      setLoading(false);
    }).catch((e) => { toast(e.message); router.replace('/agent'); });
  }, [listingId, router, toast]);

  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => setF({ ...f, [k]: e.target.value });
  const toggle = (n: string) => setFacilities(facilities.includes(n) ? facilities.filter((x) => x !== n) : [...facilities, n]);
  const addCustom = () => {
    const n = custom.trim();
    if (!n) return;
    if (![...COMMON_FACILITIES, ...extra].some((x) => x.toLowerCase() === n.toLowerCase())) setExtra([...extra, n]);
    if (!facilities.includes(n)) setFacilities([...facilities, n]);
    setCustom('');
  };

  // ---------- [UC-03 / UC-04 steps 3-5] submit ----------
  async function save() {
    setBusy(true); setError(null);
    if (!f.furnishing) { setError('Select whether the property is furnished.'); setBusy(false); return; }
    const fd = new FormData();
    Object.entries(f).forEach(([k, v]) => fd.set(k, k === 'furnishing' ? String(v === 'Furnished') : v));
    fd.set('facilities', JSON.stringify(facilities));
    for (const p of photos) fd.append('photos', await compressImage(p));
    try {
      if (listingId) {
        await api(`/api/agent/listings/${listingId}`, { method: 'PUT', form: fd });
        toast('Listing updated.');
      } else {
        await api('/api/agent/listings', { form: fd });
        toast('Listing submitted as Pending Verification. The Admin will verify it before it becomes public.');
      }
      router.push('/agent');
    } catch (e: any) {
      setError(e.message);
      setBusy(false);
      window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }
  }

  async function remove() {
    try {
      await api(`/api/agent/listings/${listingId}`, { method: 'DELETE', body: { reason } });
      toast('Listing deleted.');
      router.push('/agent');
    } catch (e: any) { setError(e.message); setDel(false); }
  }

  if (loading) return <Loading />;
  const previews = photos.length ? photos.map((p) => URL.createObjectURL(p)) : existing.map(photoUrl);

  return (
    <div className="col" style={{ gap: 18, maxWidth: 980 }}>
      {rejection && <div className="alert">Admin rejected this listing: {rejection}{'\n'}Fix the details and save to resubmit it for verification.</div>}

      <section className="card col">
        <h3>Property details</h3>
        <Field label="Title"><input className="input" value={f.title} onChange={set('title')} placeholder="e.g. Single sunny room apartment" /></Field>
        <div className="grid-2">
          <Field label="Address"><input className="input" value={f.address} onChange={set('address')} placeholder="House / street / block" /></Field>
          <Field label="CDA Sector">
            <select className="select" value={f.cdaSector} onChange={set('cdaSector')}>
              <option value="">Select sector</option>{CDA_SECTORS.map((s) => <option key={s}>{s}</option>)}
            </select>
          </Field>
          <Field label="Monthly rent (PKR)"><input className="input" inputMode="numeric" value={f.rent} onChange={set('rent')} placeholder="e.g. 45000" /></Field>
          <Field label="Property type"><ChipGroup options={PROPERTY_TYPES} value={f.propertyType} onChange={(v) => setF({ ...f, propertyType: v })} /></Field>
          <Field label="Occupancy"><ChipGroup options={OCCUPANCY_TYPES} value={f.occupancyType} onChange={(v) => setF({ ...f, occupancyType: v })} /></Field>
          <Field label="Furnishing"><ChipGroup options={['Furnished', 'Unfurnished']} value={f.furnishing} onChange={(v) => setF({ ...f, furnishing: v })} /></Field>
        </div>
        <Field label="Description"><textarea className="textarea" value={f.description} onChange={set('description')} placeholder="Describe the property (at least 20 characters)" /></Field>
      </section>

      <section className="card col">
        <h3>Facilities advertised</h3>
        <span className="muted small">Tenants will later confirm whether these were really provided.</span>
        <div className="row wrap" style={{ gap: 14 }}>
          {[...COMMON_FACILITIES, ...extra].map((n) => (
            <label key={n} className="check" style={{ width: 170 }}><input type="checkbox" checked={facilities.includes(n)} onChange={() => toggle(n)} />{n}</label>
          ))}
        </div>
        <div className="row" style={{ maxWidth: 420 }}>
          <input className="input" value={custom} onChange={(e) => setCustom(e.target.value)} placeholder="Other facility"
            onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addCustom(); } }} />
          <button className="btn btn-outline" type="button" onClick={addCustom}>Add</button>
        </div>
      </section>

      <section className="card col">
        <h3>Photos</h3>
        <span className="muted small">JPG, PNG or WEBP. The first photo is the cover.{listingId ? ' Choosing new photos replaces the current ones.' : ''}</span>
        <label className="btn btn-outline" style={{ alignSelf: 'flex-start' }}><ImagePlus size={16} />{listingId ? 'Replace photos' : 'Choose photos'}
          <input type="file" accept="image/jpeg,image/png,image/webp" multiple hidden onChange={(e) => setPhotos(Array.from(e.target.files ?? []).slice(0, 8))} /></label>
        <div className="thumbs">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          {previews.map((src, i) => <img key={i} src={src} alt="" style={{ width: 140, height: 95, opacity: 1 }} />)}
        </div>
      </section>

      {listingId && (
        <section className="card col">
          <h3>Current property status</h3>
          <div className="chips">
            {STATUSES.map(([v, label]) => (
              <button key={v} type="button" className={`chip ${f.propertyStatus === v ? 'on' : ''}`} onClick={() => setF({ ...f, propertyStatus: v })}>{label}</button>
            ))}
          </div>
        </section>
      )}

      <ErrorBox error={error} />
      <div className="row">
        <button className="btn btn-ghost" onClick={() => router.push('/agent')}>Cancel</button>
        {listingId && <button className="btn btn-danger" onClick={() => setDel(true)}><Trash2 size={15} />Delete listing</button>}
        <span className="spacer" />
        <button className="btn btn-primary" disabled={busy} onClick={save}>{busy ? 'Saving...' : listingId ? 'Save changes' : 'Submit for verification'}</button>
      </div>

      {del && (
        <Modal title="Delete this listing?" onClose={() => setDel(false)}
          footer={<><button className="btn btn-ghost" onClick={() => setDel(false)}>Cancel</button><button className="btn btn-danger" onClick={remove}>Delete</button></>}>
          <Field label="Reason for deleting"><textarea className="textarea" value={reason} onChange={(e) => setReason(e.target.value)} /></Field>
        </Modal>
      )}
    </div>
  );
}

'use client';
// =====================================================================
// [UI] Small reusable components: Chip, ChipGroup, StatusBadge, Modal,
// Avatar, Field, ErrorBox, Loading, Empty, facility icons, PropertyCard
// =====================================================================
import Link from 'next/link';
import {
  Bolt, Car, Check, Dumbbell, Flame, Shirt, ShieldCheck, Snowflake, Sofa, ArrowUpDown, Tv, Utensils, Wifi, MapPin, X,
} from 'lucide-react';
import { photoUrl, pkr, initials } from '../api';
import { statusLabel } from '@/bl/domain/constants';

// ---------------------------------------------------------------------
// [CHIPS] squircle filter chips
// ---------------------------------------------------------------------
export function Chip({ on, onClick, children, square }: { on?: boolean; onClick?: () => void; children: React.ReactNode; square?: boolean }) {
  return (
    <button type="button" className={`chip ${on ? 'on' : ''} ${square ? 'chip-sq' : ''}`} onClick={onClick} aria-pressed={!!on}>
      {children}
    </button>
  );
}

/** single-select group; value '' = Any / All */
export function ChipGroup({ options, value, onChange, anyLabel }: {
  options: string[]; value: string; onChange: (v: string) => void; anyLabel?: string;
}) {
  return (
    <div className="chips">
      {anyLabel && <Chip on={value === ''} onClick={() => onChange('')} square>{anyLabel}</Chip>}
      {options.map((o) => <Chip key={o} on={value === o} onClick={() => onChange(o)} square={o.length <= 4}>{o}</Chip>)}
    </div>
  );
}

// ---------------------------------------------------------------------
// [BADGES] colour per status
// ---------------------------------------------------------------------
const COLOR: Record<string, string> = {
  VERIFIED: 'green', AVAILABLE: 'green', ACCEPTED: 'green', RESOLVED: 'green',
  PENDING: 'amber', PENDING_VERIFICATION: 'amber', OPEN: 'amber', UNDER_MAINTENANCE: 'amber',
  REJECTED: 'red', DECLINED: 'red', OCCUPIED: 'gray',
};
export function StatusBadge({ status }: { status: string }) {
  return <span className={`badge b-${COLOR[status] ?? 'blue'}`}>{statusLabel(status)}</span>;
}
export const Badge = ({ color = 'blue', children }: { color?: string; children: React.ReactNode }) =>
  <span className={`badge b-${color}`}>{children}</span>;

// ---------------------------------------------------------------------
// [MODAL]
// ---------------------------------------------------------------------
export function Modal({ title, onClose, children, footer }: {
  title: string; onClose: () => void; children: React.ReactNode; footer?: React.ReactNode;
}) {
  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-label={title}>
        <div className="row"><h2 style={{ flex: 1 }}>{title}</h2>
          <button className="btn btn-ghost" onClick={onClose} aria-label="Close"><X size={18} /></button></div>
        {children}
        {footer && <div className="row" style={{ justifyContent: 'flex-end' }}>{footer}</div>}
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------
// [MISC]
// ---------------------------------------------------------------------
export const Avatar = ({ name, size = 40 }: { name: string; size?: number }) =>
  <span className="avatar" style={{ width: size, height: size, fontSize: size * 0.36 }}>{initials(name || '?')}</span>;

export const Field = ({ label, children }: { label: string; children: React.ReactNode }) =>
  <div className="field"><label>{label}</label>{children}</div>;

export const ErrorBox = ({ error }: { error: string | null }) => (error ? <div className="alert">{error}</div> : null);

export const Loading = () => <div className="muted" style={{ padding: 30, textAlign: 'center' }}>Loading...</div>;

export const Empty = ({ title, sub }: { title: string; sub?: string }) => (
  <div style={{ padding: 40, textAlign: 'center' }}><h3>{title}</h3>{sub && <p className="muted">{sub}</p>}</div>
);

/** Icon per facility name (amenity grid) */
export function FacilityIcon({ name, size = 16 }: { name: string; size?: number }) {
  const n = name.toLowerCase();
  const I = n.includes('wi') ? Wifi : n.includes('tv') ? Tv : n.includes('furn') ? Sofa : n.includes('elev') ? ArrowUpDown
    : n.includes('air') ? Snowflake : n.includes('gener') || n.includes('ups') ? Bolt : n.includes('park') ? Car
    : n.includes('gas') ? Flame : n.includes('gym') ? Dumbbell : n.includes('meal') ? Utensils
    : n.includes('laund') ? Shirt : n.includes('secur') ? ShieldCheck : Check;
  return <I size={size} />;
}

// ---------------------------------------------------------------------
// [PROPERTY CARD] like "Nearby Properties" in the design
// ---------------------------------------------------------------------
export function PropertyCard({ l }: { l: any }) {
  return (
    <Link href={`/property/${l.listingID}`} className="pcard">
      <div className="img">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={photoUrl(l.photoPath)} alt={l.title} loading="lazy" />
        <span className="badge b-blue">{l.propertyType}</span>
        {!l.available && <span style={{ position: 'absolute', top: 10, right: 10 }}><StatusBadge status={l.propertyStatus} /></span>}
      </div>
      <div className="row small"><Avatar name={l.owner.name} size={24} /><span>{l.owner.name}</span><span className="muted">/Agent</span></div>
      <div className="title">{l.title}</div>
      <div className="row muted small"><MapPin size={14} />{l.address}</div>
      <div className="row"><span className="price">{pkr(l.rent)}</span><span className="muted small">/month</span>
        <span className="spacer" /><span className="link small">View details &rsaquo;</span></div>
    </Link>
  );
}

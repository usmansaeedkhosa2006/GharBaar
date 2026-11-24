'use client';
// =====================================================================
// [PAGE] /home - UC-09 Search Properties  (Customer home)
// Layout follows the reference design: hero + search bar, category
// chips, the "Filters" sheet as a left panel (price histogram, type,
// sector, furnishing, occupancy, amenities) and property cards.
// =====================================================================
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Search, SlidersHorizontal } from 'lucide-react';
import { AppShell } from '@/ui/components/AppShell';
import { ChipGroup, Empty, ErrorBox, Loading, PropertyCard } from '@/ui/components/ui';
import { api, pkr } from '@/ui/api';
import { CDA_SECTORS, COMMON_FACILITIES, OCCUPANCY_TYPES, PROPERTY_TYPES } from '@/bl/domain/constants';

type Filters = { q: string; type: string; sector: string; furnished: string; occupancy: string; facilities: string[]; min: number; max: number };
const EMPTY: Filters = { q: '', type: '', sector: '', furnished: '', occupancy: '', facilities: [], min: 0, max: 0 };

export default function SearchPage() {
  return <AppShell portal="customer" title="Find Home"><SearchScreen /></AppShell>;
}

function SearchScreen() {
  const [f, setF] = useState<Filters>(EMPTY);
  const [all, setAll] = useState<any[] | null>(null);          // every verified listing (for histogram)
  const [results, setResults] = useState<any[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [moreAmenities, setMoreAmenities] = useState(false);

  // ---------- [UC-09 step 3-5] call the controller with the filters ----------
  const run = useCallback(async (flt: Filters) => {
    setError(null);
    setResults(null);
    const p = new URLSearchParams();
    if (flt.q.trim()) p.set('q', flt.q.trim());
    if (flt.type) p.set('type', flt.type);
    if (flt.sector) p.set('sector', flt.sector);
    if (flt.furnished) p.set('furnished', flt.furnished === 'Furnished' ? 'true' : 'false');
    if (flt.occupancy) p.set('occupancy', flt.occupancy);
    if (flt.facilities.length) p.set('facilities', flt.facilities.join(','));
    if (flt.min > 0) p.set('min', String(flt.min));
    if (flt.max > 0) p.set('max', String(flt.max));
    try {
      setResults((await api<{ results: any[] }>('/api/listings?' + p)).results);
    } catch (e: any) {
      setError(e.message);                                                     // ext. 4b: show error + retry
      setResults([]);
    }
  }, []);

  useEffect(() => {
    api<{ results: any[] }>('/api/listings').then((r) => { setAll(r.results); setResults(r.results); })
      .catch((e) => { setError(e.message); setResults([]); });
  }, []);

  // ---------- [HISTOGRAM] rent distribution like the design ----------
  const maxRent = useMemo(() => Math.ceil(Math.max(50000, ...(all ?? []).map((l) => l.rent)) / 50000) * 50000, [all]);
  const bins = useMemo(() => {
    const b = new Array(24).fill(0);
    (all ?? []).forEach((l) => b[Math.min(23, Math.floor((l.rent / maxRent) * 24))]++);
    const top = Math.max(1, ...b);
    return b.map((n) => ({ n, h: 6 + (46 * n) / top }));
  }, [all, maxRent]);
  const hi = f.max || maxRent;

  const update = (patch: Partial<Filters>, search = false) => {
    const next = { ...f, ...patch };
    setF(next);
    if (search) run(next);
  };
  const toggleFacility = (name: string) =>
    update({ facilities: f.facilities.includes(name) ? f.facilities.filter((x) => x !== name) : [...f.facilities, name] });

  return (
    <div className="col" style={{ gap: 20 }}>
      {/* ---------- [HERO] ---------- */}
      <section className="hero">
        <h1>Find your home where you want to Live</h1>
        <form className="search-bar" onSubmit={(e) => { e.preventDefault(); run(f); }}>
          <Search size={18} color="#6b7280" />
          <input placeholder="Search your properties... (title, sector, address)" value={f.q} onChange={(e) => update({ q: e.target.value })} />
          <button className="icon-btn" aria-label="Apply filters"><SlidersHorizontal size={18} /></button>
        </form>
      </section>

      {/* ---------- [CATEGORIES] ---------- */}
      <section className="col" style={{ gap: 10 }}>
        <h3>Categories</h3>
        <ChipGroup options={PROPERTY_TYPES} value={f.type} onChange={(v) => update({ type: v }, true)} anyLabel="All" />
      </section>

      <div className="search-layout">
        {/* ---------- [FILTER PANEL] ---------- */}
        <aside className="card filters">
          <h3>Filters</h3>
          <div className="col" style={{ gap: 8 }}>
            <span className="label">Price range (monthly rent)</span>
            <b style={{ fontSize: 15 }}>{pkr(f.min)} - {pkr(hi)}</b>
            <div className="histogram">
              {bins.map((b, i) => {
                const inRange = ((i + 0.5) / 24) * maxRent >= f.min && ((i + 0.5) / 24) * maxRent <= hi;
                return <span key={i} className={b.n === 0 ? 'zero' : ''} style={{ height: b.h, opacity: inRange ? 1 : 0.35 }} />;
              })}
            </div>
            <input className="range" type="range" min={0} max={maxRent} step={5000} value={f.min} aria-label="Minimum rent"
              onChange={(e) => { const v = Number(e.target.value); update({ min: v, max: f.max && v > f.max ? v : f.max }); }} />
            <input className="range" type="range" min={0} max={maxRent} step={5000} value={hi} aria-label="Maximum rent"
              onChange={(e) => { const v = Number(e.target.value); update({ max: v >= maxRent ? 0 : v, min: Math.min(f.min, v) }); }} />
            <div className="row small muted"><span>{pkr(0)}</span><span className="spacer" /><span>{pkr(maxRent)}</span></div>
          </div>
          <div className="col" style={{ gap: 8 }}><span className="label">Property Type</span>
            <ChipGroup options={PROPERTY_TYPES} value={f.type} onChange={(v) => update({ type: v })} anyLabel="All" /></div>
          <div className="col" style={{ gap: 8 }}><span className="label">CDA Sector</span>
            <ChipGroup options={CDA_SECTORS} value={f.sector} onChange={(v) => update({ sector: v })} anyLabel="Any" /></div>
          <div className="col" style={{ gap: 8 }}><span className="label">Furnishing</span>
            <ChipGroup options={['Furnished', 'Unfurnished']} value={f.furnished} onChange={(v) => update({ furnished: v })} anyLabel="Any" /></div>
          <div className="col" style={{ gap: 8 }}><span className="label">Occupancy</span>
            <ChipGroup options={OCCUPANCY_TYPES} value={f.occupancy} onChange={(v) => update({ occupancy: v })} anyLabel="Any" /></div>
          <div className="col" style={{ gap: 10 }}>
            <div className="row"><span className="label">Home amenities</span><span className="spacer" />
              <button className="btn-ghost link small" style={{ border: 0, background: 'none' }} onClick={() => setMoreAmenities(!moreAmenities)}>
                {moreAmenities ? 'View less' : 'View more'}</button></div>
            <div className="grid-2" style={{ gap: 10 }}>
              {COMMON_FACILITIES.slice(0, moreAmenities ? undefined : 6).map((a) => (
                <label key={a} className="check"><input type="checkbox" checked={f.facilities.includes(a)} onChange={() => toggleFacility(a)} />{a}</label>
              ))}
            </div>
          </div>
          <div className="row">
            <button className="btn btn-outline" style={{ flex: 1 }} onClick={() => { setF(EMPTY); run(EMPTY); }}>Clear all</button>
            <button className="btn btn-primary" style={{ flex: 1 }} onClick={() => run(f)}>Show result</button>
          </div>
        </aside>

        {/* ---------- [RESULTS] ---------- */}
        <section className="col">
          <div className="row"><h3>Properties for rent</h3>{results && <span className="muted">{results.length} found</span>}</div>
          <ErrorBox error={error} />
          {error && <div><button className="btn btn-primary" onClick={() => run(f)}>Retry</button></div>}
          {!results ? <Loading /> : results.length === 0 && !error
            ? <Empty title="No results found" sub="Try broadening your filters: increase the price range or remove some amenities." />
            : <div className="grid-cards">{results.map((l) => <PropertyCard key={l.listingID} l={l} />)}</div>}
        </section>
      </div>
    </div>
  );
}

'use client';
// =====================================================================
// [PAGE] /activity - My Activity (Customer)
//   - my visit requests (UC-12) with the agent's answer
//   - my tenancy claims (UC-13) + "Write review" when verified (UC-14)
// =====================================================================
import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { Star } from 'lucide-react';
import { AppShell } from '@/ui/components/AppShell';
import { Badge, Loading, StatusBadge } from '@/ui/components/ui';
import { ReviewModal } from '@/ui/components/ReviewModal';
import { useToast } from '@/ui/components/Toast';
import { api, fmtDate, fmtDateTime, photoUrl } from '@/ui/api';

export default function Page() {
  return <AppShell portal="customer" title="My Activity"><Activity /></AppShell>;
}

function Activity() {
  const toast = useToast();
  const [visits, setVisits] = useState<any[] | null>(null);
  const [claims, setClaims] = useState<any[] | null>(null);
  const [review, setReview] = useState<{ l: any; facilities: any[] } | null>(null);

  const loadClaims = useCallback(() => api<{ claims: any[] }>('/api/tenancy/claims').then((r) => setClaims(r.claims)).catch((e) => toast(e.message)), [toast]);
  useEffect(() => {
    api<{ visits: any[] }>('/api/visits').then((r) => setVisits(r.visits)).catch((e) => toast(e.message));
    loadClaims();
  }, [loadClaims, toast]);

  async function startReview(c: any) {
    try {
      const r = await api<{ facilities: any[] }>(`/api/tenancy/review/${c.listing.listingID}`);
      setReview({ l: c.listing, facilities: r.facilities });
    } catch (e: any) { toast(e.message); }
  }

  return (
    <div className="col" style={{ gap: 20 }}>
      <section className="card col">
        <h3>My visit requests</h3>
        {!visits ? <Loading /> : visits.length === 0 ? <span className="muted">You have not requested any visits yet.</span>
          : visits.map((v) => (
            <Link key={v.requestID} href={`/property/${v.listing.listingID}`} className="list-row">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={photoUrl(v.listing.photoPath)} alt="" />
              <div className="col" style={{ gap: 3, flex: 1 }}>
                <b>{v.listing.title}</b>
                <span className="muted">{fmtDateTime(v.proposedDateTime)} · Agent: {v.listing.ownerName}</span>
                <span className="muted small">{v.notes ? `Notes: ${v.notes}` : 'No notes'}</span>
              </div>
              <StatusBadge status={v.status} />
            </Link>
          ))}
      </section>

      <section className="card col">
        <h3>My tenancy claims &amp; reviews</h3>
        <span className="muted small">Lived in a property listed on GharBaar? Open it and choose “Submit Tenancy Claim”. After the Admin verifies it, you can review its facilities here.</span>
        {!claims ? <Loading /> : claims.length === 0 ? <span className="muted">No tenancy claims yet.</span>
          : claims.map((c) => (
            <div key={c.claimID} className="list-row">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={photoUrl(c.listing.photoPath)} alt="" />
              <div className="col" style={{ gap: 3, flex: 1 }}>
                <b>{c.listing.title}</b>
                <span className="muted">{fmtDate(c.startDate)} - {fmtDate(c.endDate)}</span>
                {c.rejectionReason && <span className="error small">Reason: {c.rejectionReason}</span>}
              </div>
              {c.flagged && <Badge color="amber">Flagged: overlapping dates</Badge>}
              <StatusBadge status={c.status} />
              {c.status === 'VERIFIED' && !c.hasReview && <button className="btn btn-primary" onClick={() => startReview(c)}><Star size={16} />Write review</button>}
              {c.hasReview && <Badge>Reviewed</Badge>}
            </div>
          ))}
      </section>

      {review && <ReviewModal l={review.l} facilities={review.facilities} onClose={() => setReview(null)} onSaved={loadClaims} />}
    </div>
  );
}

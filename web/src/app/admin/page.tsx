'use client';
// =====================================================================
// [PAGE] /admin - Admin dashboard (admin sub-domain home)
// =====================================================================
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { FileCheck, Flag, Home, ShieldCheck, UserCheck, Users, X } from 'lucide-react';
import { AppShell } from '@/ui/components/AppShell';
import { Loading } from '@/ui/components/ui';
import { api } from '@/ui/api';

export default function Page() {
  return <AppShell portal="admin" title="Dashboard"><Dashboard /></AppShell>;
}

function Dashboard() {
  const [s, setS] = useState<any>(null);
  useEffect(() => { api('/api/admin/stats').then(setS).catch(() => {}); }, []);

  const cards = s ? [
    ['Listings waiting for verification', s.pendingListings, ShieldCheck, '#d97706', '/admin/listings'],
    ['Tenants waiting for verification', s.pendingTenants, UserCheck, '#d97706', '/admin/tenants'],
    ['Open reports', s.openReports, Flag, '#dc2626', '/admin/reports'],
    ['Verified listings', s.verifiedListings, Home, '#16a34a', '/admin/all-listings'],
    ['Rejected listings', s.rejectedListings, X, '#6b7280', '/admin/all-listings'],
    ['Customers', s.customers, Users, '#2f80ed', '/admin/users'],
    ['Agents', s.agents, Users, '#2f80ed', '/admin/users'],
    ['Pending tenancy claims', s.pendingClaims, FileCheck, '#d97706', '/admin/tenants'],
  ] as const : [];

  return (
    <div className="col" style={{ gap: 20 }}>
      <div><h1>Admin panel</h1><span className="muted">Verify listings and tenants, resolve reports and manage accounts.</span></div>
      {!s ? <Loading /> : (
        <div className="grid-stats">
          {cards.map(([label, n, Icon, color, href]) => (
            <Link key={label} href={href} className="stat col" style={{ gap: 8 }}>
              <span className="icon-btn" style={{ background: color + '1a', color, alignSelf: 'flex-start' }}><Icon size={20} /></span>
              <div className="value">{n}</div><span className="muted">{label}</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

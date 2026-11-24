'use client';
// =====================================================================
// [UI] AppShell - sidebar + top bar for every logged-in page.
// Checks the session and sends the user to the right portal/home.
//   portal 'customer' | 'agent' | 'admin'
// =====================================================================
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import {
  Bell, CalendarDays, Flag, Home, LayoutGrid, List, LogOut, PlusSquare, ShieldCheck, UserCheck, Users,
} from 'lucide-react';
import { api } from '../api';
import { Avatar, Loading } from './ui';

type Portal = 'customer' | 'agent' | 'admin';
export type Me = { userID: number; username: string; name: string; phone: string; role: string; homeView: string };

const MeCtx = createContext<{ me: Me | null; refreshBell: () => void }>({ me: null, refreshBell: () => {} });
export const useMe = () => useContext(MeCtx);

// ---------------------------------------------------------------------
// [MENU] per role
// ---------------------------------------------------------------------
const MENUS: Record<Portal, { href: string; label: string; icon: React.ComponentType<{ size?: number }> }[]> = {
  customer: [
    { href: '/home', label: 'Find Home', icon: Home },
    { href: '/activity', label: 'My Activity', icon: CalendarDays },
    { href: '/notifications', label: 'Notifications', icon: Bell },
    { href: '/report', label: 'Report Issue', icon: Flag },
  ],
  agent: [
    { href: '/agent', label: 'My Listings', icon: List },
    { href: '/agent/new', label: 'Add Listing', icon: PlusSquare },
    { href: '/agent/visits', label: 'Visit Requests', icon: CalendarDays },
    { href: '/notifications', label: 'Notifications', icon: Bell },
    { href: '/report', label: 'Report Issue', icon: Flag },
  ],
  admin: [
    { href: '/admin', label: 'Dashboard', icon: LayoutGrid },
    { href: '/admin/listings', label: 'Verify Listings', icon: ShieldCheck },
    { href: '/admin/tenants', label: 'Verify Tenants', icon: UserCheck },
    { href: '/admin/reports', label: 'Reports', icon: Flag },
    { href: '/admin/users', label: 'Users', icon: Users },
    { href: '/admin/all-listings', label: 'All Listings', icon: List },
    { href: '/admin/notifications', label: 'Notifications', icon: Bell },
  ],
};

const portalOf = (role: string): Portal => (role === 'ADMIN' ? 'admin' : role === 'AGENT' ? 'agent' : 'customer');

export function AppShell({ portal, title, children, allow }: {
  portal: Portal; title: string; children: React.ReactNode; allow?: Portal[];
}) {
  const router = useRouter();
  const path = usePathname();
  const [me, setMe] = useState<Me | null>(null);
  const [unread, setUnread] = useState(0);

  // ---- session check (redirects) ----
  useEffect(() => {
    api<{ user: Me | null }>('/api/auth/me').then(({ user }) => {
      if (!user) { router.replace(portal === 'admin' ? '/admin/login' : '/login'); return; }
      const mine = portalOf(user.role);
      if (mine !== portal && !(allow ?? []).includes(mine)) { router.replace(user.homeView); return; }
      setMe(user);
    }).catch(() => router.replace('/login'));
  }, [portal, router, allow]);

  const refreshBell = useCallback(() => {
    api<{ unread: number }>('/api/notifications').then((d) => setUnread(d.unread)).catch(() => {});
  }, []);
  useEffect(() => { if (me) refreshBell(); }, [me, path, refreshBell]);

  async function logout() {
    await api('/api/auth/logout', { method: 'POST' }).catch(() => {});
    router.replace(portal === 'admin' ? '/admin/login' : '/login');
  }

  if (!me) return <Loading />;
  const menu = MENUS[portalOf(me.role)];
  const roleTag = me.role === 'TENANT' ? 'Customer · Tenant' : me.role === 'ADMIN' ? 'Admin Portal' : me.role === 'AGENT' ? 'Agent Portal' : 'Customer Portal';
  const bellHref = me.role === 'ADMIN' ? '/admin/notifications' : '/notifications';

  return (
    <MeCtx.Provider value={{ me, refreshBell }}>
      <div className="shell">
        {/* ---------- [SIDEBAR] ---------- */}
        <aside className="sidebar">
          <div className="brand">GharBaar</div>
          <span className="badge b-blue" style={{ alignSelf: 'flex-start', marginLeft: 8 }}>{roleTag}</span>
          <nav className="nav">
            {menu.map((m) => {
              const active = path === m.href || (m.href !== '/agent' && m.href !== '/admin' && path.startsWith(m.href + '/'))
                || (m.href === '/home' && path.startsWith('/property'));
              return <Link key={m.href} href={m.href} className={active ? 'active' : ''}><m.icon size={18} />{m.label}</Link>;
            })}
          </nav>
          <div className="spacer" />
          <div className="nav"><button onClick={logout}><LogOut size={18} />Logout</button></div>
        </aside>

        {/* ---------- [TOP BAR + PAGE] ---------- */}
        <div className="main">
          <header className="topbar">
            <h2>{title}</h2>
            <span className="spacer" />
            <Link href={bellHref} className="bell" aria-label="Notifications">
              <Bell size={21} />{unread > 0 && <span className="count">{unread}</span>}
            </Link>
            <div className="col" style={{ gap: 0 }}><b>{me.name}</b><span className="muted small">@{me.username}</span></div>
            <Avatar name={me.name} />
          </header>
          <main className="content">{children}</main>
        </div>
      </div>
    </MeCtx.Provider>
  );
}

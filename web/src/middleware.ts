// =====================================================================
// [ROUTING] Host-based split between the main site and the admin portal
//   main site   : gharbaar-pk.vercel.app        (customers + agents)
//   admin portal: admin-gharbaar-pk.vercel.app  (admin only)
//   locally     : localhost:3000 / admin.localhost:3000
// =====================================================================
import { NextResponse, type NextRequest } from 'next/server';

export function isAdminHost(host: string) {
  const h = host.toLowerCase();
  return h.startsWith('admin.') || h.startsWith('admin-');
}

export function middleware(req: NextRequest) {
  const host = req.headers.get('host') ?? '';
  const { pathname } = req.nextUrl;
  const admin = isAdminHost(host);
  if (/\.\w+$/.test(pathname)) return NextResponse.next();          // static files (images, svg)

  if (admin) {
    // admin host: everything lives under /admin
    if (pathname === '/') return NextResponse.rewrite(new URL('/admin', req.url));
    if (!pathname.startsWith('/admin') && !pathname.startsWith('/api'))
      return NextResponse.redirect(new URL('/admin', req.url));
    const res = NextResponse.next();
    res.headers.set('x-portal', 'admin');
    return res;
  }

  // main host: the admin panel is not reachable here
  if (pathname.startsWith('/admin') || pathname.startsWith('/api/admin'))
    return NextResponse.rewrite(new URL('/not-found-page', req.url));
  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|icon.svg).*)'],
};

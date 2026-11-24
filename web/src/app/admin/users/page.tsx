'use client';
// =====================================================================
// [PAGE] /admin/users - all accounts; suspend / re-activate
// (tackling fraudulent use of the service)
// =====================================================================
import { useCallback, useEffect, useMemo, useState } from 'react';
import { AppShell } from '@/ui/components/AppShell';
import { Badge, Loading } from '@/ui/components/ui';
import { useToast } from '@/ui/components/Toast';
import { api } from '@/ui/api';
import { statusLabel } from '@/bl/domain/constants';

export default function Page() {
  return <AppShell portal="admin" title="Users"><UsersTable /></AppShell>;
}

function UsersTable() {
  const toast = useToast();
  const [users, setUsers] = useState<any[] | null>(null);
  const [q, setQ] = useState('');
  const load = useCallback(() => api<{ users: any[] }>('/api/admin/users').then((r) => setUsers(r.users)).catch((e) => toast(e.message)), [toast]);
  useEffect(() => { load(); }, [load]);

  const shown = useMemo(() => (users ?? []).filter((u) =>
    (u.name + ' ' + u.username).toLowerCase().includes(q.toLowerCase())), [users, q]);

  async function toggle(u: any) {
    if (u.active && !confirm(`Suspend ${u.name}? They will not be able to log in until re-activated.`)) return;
    try {
      await api(`/api/admin/users/${u.userID}`, { body: { active: !u.active } });
      toast(u.active ? 'Account suspended.' : 'Account re-activated.');
      load();
    } catch (e: any) { toast(e.message); }
  }

  return (
    <div className="col" style={{ gap: 16 }}>
      <div className="row"><h2 style={{ flex: 1 }}>All accounts</h2>
        <input className="input" style={{ maxWidth: 320 }} placeholder="Search by name or username" value={q} onChange={(e) => setQ(e.target.value)} /></div>
      {!users ? <Loading /> : (
        <table className="table">
          <thead><tr><th>Name</th><th>Username</th><th>Role</th><th>Phone</th><th>Tenant status</th><th>Account</th><th>Action</th></tr></thead>
          <tbody>
            {shown.map((u) => (
              <tr key={u.userID}>
                <td><b>{u.name}</b></td><td>@{u.username}</td>
                <td>{u.role.charAt(0) + u.role.slice(1).toLowerCase()}</td><td>{u.phone}</td>
                <td>{u.verificationStatus ? statusLabel(u.verificationStatus) : '-'}</td>
                <td>{u.active ? <Badge color="green">Active</Badge> : <Badge color="red">Suspended</Badge>}</td>
                <td>{u.role !== 'ADMIN' && (
                  <button className={`btn ${u.active ? 'btn-danger' : 'btn-success'}`} style={{ padding: '5px 12px' }} onClick={() => toggle(u)}>
                    {u.active ? 'Suspend' : 'Activate'}</button>)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

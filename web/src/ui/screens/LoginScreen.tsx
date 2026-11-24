'use client';
// =====================================================================
// [UI] LoginScreen - UC-02 Login + UC-01 Register
// Register offers TWO separate account types: Customer and Agent.
// On the admin sub-domain only the Admin login form is shown.
// =====================================================================
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { Check, Home, Search, ShieldCheck } from 'lucide-react';
import { api } from '../api';
import { ErrorBox, Field } from '../components/ui';
import { useToast } from '../components/Toast';

export function LoginScreen({ admin = false }: { admin?: boolean }) {
  const router = useRouter();
  const [tab, setTab] = useState<'login' | 'register'>('login');

  // already logged in? go home
  useEffect(() => {
    api<{ user: any }>('/api/auth/me').then(({ user }) => {
      if (user && (admin ? user.role === 'ADMIN' : user.role !== 'ADMIN')) router.replace(user.homeView);
    }).catch(() => {});
  }, [admin, router]);

  return (
    <div className="auth">
      {/* ---------- [LEFT] brand panel ---------- */}
      <section className="auth-left">
        <h1 style={{ fontSize: 38 }}>GharBaar{admin && ' Admin'}</h1>
        <p style={{ fontSize: 16, opacity: 0.9, margin: 0 }}>
          {admin ? 'Verify listings and tenants, resolve reports and keep the platform safe.'
            : <>Find your home where you want to live.<br />Verified rentals across Islamabad&apos;s CDA sectors.</>}
        </p>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src="https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80" alt="Modern house" />
        {['Every listing verified by our Admin team', 'Real tenant history: were the facilities really provided?',
          'Request visits and talk to agents directly'].map((t) => (
          <div key={t} className="row" style={{ opacity: 0.92 }}><Check size={18} />{t}</div>
        ))}
      </section>

      {/* ---------- [RIGHT] forms ---------- */}
      <section className="auth-right">
        <div className="col" style={{ width: '100%', maxWidth: 460, gap: 24, alignItems: 'center' }}>
          {!admin && (
            <div className="tabs">
              <button className={tab === 'login' ? 'on' : ''} onClick={() => setTab('login')}>Login</button>
              <button className={tab === 'register' ? 'on' : ''} onClick={() => setTab('register')}>Register</button>
            </div>
          )}
          {tab === 'login' || admin ? <LoginForm admin={admin} /> : <RegisterForm onDone={() => setTab('login')} />}
        </div>
      </section>
    </div>
  );
}

// ---------------------------------------------------------------------
// [UC-02] Login form
// ---------------------------------------------------------------------
function LoginForm({ admin }: { admin: boolean }) {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const r = await api<{ home: string }>('/api/auth/login', { body: { username, password } });
      router.replace(r.home);                                         // home page per role
    } catch (err: any) {
      setError(err.message);
      setBusy(false);
    }
  }

  return (
    <form className="card col" style={{ width: '100%', padding: 30, gap: 16 }} onSubmit={submit}>
      <h1>{admin ? 'Admin sign in' : 'Welcome back'}</h1>
      <span className="muted">{admin ? 'Restricted to GharBaar administrators.' : 'Log in as a Customer or an Agent.'}</span>
      <Field label="Username"><input className="input" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required /></Field>
      <Field label="Password"><input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required /></Field>
      <ErrorBox error={error} />
      <button className="btn btn-primary btn-block" disabled={busy}>{admin && <ShieldCheck size={16} />}{busy ? 'Signing in...' : 'Login'}</button>
      {!admin && <span className="muted small">Demo: customer_sara / agent_mahmud / tenant_bilal (password Demo@1234)</span>}
    </form>
  );
}

// ---------------------------------------------------------------------
// [UC-01] Register form - choose Customer OR Agent
// ---------------------------------------------------------------------
function RegisterForm({ onDone }: { onDone: () => void }) {
  const toast = useToast();
  const [role, setRole] = useState<'CUSTOMER' | 'AGENT'>('CUSTOMER');
  const [f, setF] = useState({ name: '', phone: '', username: '', password: '', confirm: '' });
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value });

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (f.password !== f.confirm) { setError('Passwords do not match.'); return; }
    setBusy(true);
    try {
      await api('/api/auth/register', { body: { username: f.username, password: f.password, name: f.name, phone: f.phone, role } });
      toast(`Your ${role === 'AGENT' ? 'Agent' : 'Customer'} account is ready. Please log in.`);
      onDone();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const RoleCard = ({ value, title, sub, Icon }: { value: 'CUSTOMER' | 'AGENT'; title: string; sub: string; Icon: typeof Home }) => (
    <button type="button" className={`role-card ${role === value ? 'on' : ''}`} onClick={() => setRole(value)} aria-pressed={role === value}>
      <Icon size={22} color="#2f80ed" /><b>{title}</b><span className="muted small">{sub}</span>
    </button>
  );

  return (
    <form className="card col" style={{ width: '100%', padding: 28, gap: 14 }} onSubmit={submit}>
      <h1>Create your account</h1>
      <span className="muted">Choose the type of account you want:</span>
      <div className="row" style={{ alignItems: 'stretch' }}>
        <RoleCard value="CUSTOMER" title="Customer" sub="Search, visit and rent homes" Icon={Search} />
        <RoleCard value="AGENT" title="Agent" sub="List your properties for rent" Icon={Home} />
      </div>
      <div className="grid-2">
        <Field label="Full name"><input className="input" value={f.name} onChange={set('name')} placeholder="e.g. Ali Khan" required /></Field>
        <Field label="Phone"><input className="input" value={f.phone} onChange={set('phone')} placeholder="03001234567" required /></Field>
      </div>
      <Field label="Username"><input className="input" value={f.username} onChange={set('username')} placeholder="letters, digits, _" required /></Field>
      <div className="grid-2">
        <Field label="Password"><input className="input" type="password" value={f.password} onChange={set('password')} placeholder="min 8, letters + numbers" required /></Field>
        <Field label="Confirm"><input className="input" type="password" value={f.confirm} onChange={set('confirm')} required /></Field>
      </div>
      <ErrorBox error={error} />
      <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Creating...' : `Create ${role === 'AGENT' ? 'Agent' : 'Customer'} account`}</button>
    </form>
  );
}

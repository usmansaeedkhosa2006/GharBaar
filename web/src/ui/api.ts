// =====================================================================
// [UI] api.ts - the ONLY way the UI talks to the server (API routes ->
// controllers). Errors from the business layer come back as messages.
// =====================================================================
export class ApiError extends Error {
  constructor(message: string, readonly status: number) { super(message); }
}

export async function api<T = any>(path: string, opts: { method?: string; body?: unknown; form?: FormData } = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(path, {
      method: opts.method ?? (opts.body || opts.form ? 'POST' : 'GET'),
      headers: opts.form ? undefined : { 'Content-Type': 'application/json' },
      body: opts.form ?? (opts.body !== undefined ? JSON.stringify(opts.body) : undefined),
      cache: 'no-store',
    });
  } catch {
    throw new ApiError('Network error. Check your connection and try again.', 0);
  }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new ApiError(data.error ?? `Request failed (${res.status})`, res.status);
  return data as T;
}

/** "db:12" -> /api/files/12 ; web URLs unchanged */
export const photoUrl = (p?: string | null) =>
  !p ? '/placeholder.svg' : p.startsWith('db:') ? `/api/files/${p.slice(3)}` : p;

export const pkr = (n: number) => 'PKR ' + Math.round(n).toLocaleString('en-US');
export const fmtDate = (d: string | Date) =>
  new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'Asia/Karachi' });
export const fmtDateTime = (d: string | Date) =>
  new Date(d).toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true, timeZone: 'Asia/Karachi' });
export const initials = (name: string) => {
  const p = name.trim().split(/\s+/);
  return ((p[0]?.[0] ?? '') + (p.length > 1 ? p[p.length - 1][0] : '')).toUpperCase();
};

/** Shrinks photos in the browser so uploads stay under Vercel's 4.5 MB limit. */
export async function compressImage(file: File, maxSide = 1600, quality = 0.82): Promise<File> {
  if (!file.type.startsWith('image/') || file.size < 600_000) return file;
  const bmp = await createImageBitmap(file);
  const scale = Math.min(1, maxSide / Math.max(bmp.width, bmp.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bmp.width * scale);
  canvas.height = Math.round(bmp.height * scale);
  canvas.getContext('2d')!.drawImage(bmp, 0, 0, canvas.width, canvas.height);
  const blob: Blob = await new Promise((r) => canvas.toBlob((b) => r(b!), 'image/jpeg', quality));
  return new File([blob], file.name.replace(/\.\w+$/, '') + '.jpg', { type: 'image/jpeg' });
}

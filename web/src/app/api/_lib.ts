// =====================================================================
// [API] shared helpers: turns controller results / exceptions into JSON
//   BusinessException -> 400, AuthException -> 401/403,
//   DataAccessException -> 503 (retry), anything else -> 500
// =====================================================================
import { NextResponse } from 'next/server';
import { AuthException, BusinessException } from '@/bl/BusinessException';
import { DataAccessException } from '@/db/DBConnection';

export async function handle(fn: () => Promise<unknown>) {
  try {
    const data = await fn();
    return NextResponse.json(data ?? { ok: true });
  } catch (e) {
    if (e instanceof BusinessException) return NextResponse.json({ error: e.message, errors: e.errors }, { status: 400 });
    if (e instanceof AuthException) return NextResponse.json({ error: e.message }, { status: e.status });
    if (e instanceof DataAccessException) {
      console.error(e.cause);
      return NextResponse.json({ error: e.message }, { status: 503 });
    }
    console.error(e);
    return NextResponse.json({ error: 'Something went wrong. Please try again.' }, { status: 500 });
  }
}

export const idOf = async (p: Promise<{ id: string }>) => {
  const id = Number((await p).id);
  if (!Number.isInteger(id) || id <= 0) throw new BusinessException('Invalid id.');
  return id;
};

export async function readJson(req: Request): Promise<any> {
  try { return await req.json(); } catch { throw new BusinessException('Invalid request.'); }
}

/** Reads the listing form (multipart) into plain values. */
export function listingForm(fd: FormData) {
  const str = (k: string) => String(fd.get(k) ?? '');
  return {
    title: str('title'), address: str('address'), propertyType: str('propertyType'), cdaSector: str('cdaSector'),
    rent: Number(str('rent').replace(/,/g, '')), description: str('description'), furnishing: str('furnishing') === 'true',
    occupancyType: str('occupancyType'), facilities: JSON.parse(str('facilities') || '[]') as string[],
    propertyStatus: (str('propertyStatus') || 'AVAILABLE') as any,
    photos: fd.getAll('photos').filter((f): f is File => f instanceof File && f.size > 0),
  };
}

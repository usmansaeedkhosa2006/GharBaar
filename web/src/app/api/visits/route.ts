// [API] GET /api/visits -> my visit requests ; POST -> Request Property Visit (UC-12)
import { app } from '@/bl/AppContext';
import { handle, readJson } from '../_lib';

export const GET = () => handle(async () => ({ visits: await app().visits.getMyVisits() }));

export const POST = (req: Request) => handle(async () => {
  const b = await readJson(req);
  const dt = b.dateTime ? new Date(b.dateTime) : null;
  return { visit: await app().visits.requestVisit(Number(b.listingId), dt, b.notes ?? '') };
});

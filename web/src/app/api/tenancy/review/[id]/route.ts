// [API] GET  /api/tenancy/review/:listingId -> facilities to confirm (UC-14 steps 1-2)
//       POST /api/tenancy/review/:listingId -> Submit Facility Review (UC-14)
import { app } from '@/bl/AppContext';
import { handle, idOf, readJson } from '../../../_lib';

type Ctx = { params: Promise<{ id: string }> };

export const GET = (_: Request, ctx: Ctx) => handle(async () => ({ facilities: await app().tenancy.startFacilityReview(await idOf(ctx.params)) }));

export const POST = (req: Request, ctx: Ctx) => handle(async () => {
  const b = await readJson(req);
  return { review: await app().tenancy.submitReview(await idOf(ctx.params), b.confirmations ?? {}, b.comments ?? '') };
});

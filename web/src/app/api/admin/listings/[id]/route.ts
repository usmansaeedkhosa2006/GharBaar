// [API] GET /api/admin/listings/:id ; POST {action: approve | reject | remove, reason}   (UC-06)
import { app } from '@/bl/AppContext';
import { BusinessException } from '@/bl/BusinessException';
import { handle, idOf, readJson } from '../../../_lib';

type Ctx = { params: Promise<{ id: string }> };

export const GET = (_: Request, ctx: Ctx) => handle(async () => ({ listing: await app().verification.getListingDetails(await idOf(ctx.params)) }));

export const POST = (req: Request, ctx: Ctx) => handle(async () => {
  const id = await idOf(ctx.params);
  const b = await readJson(req);
  if (b.action === 'approve') return app().verification.approveListing(id);
  if (b.action === 'reject') return app().verification.rejectListing(id, b.reason);
  if (b.action === 'remove') return app().admin.removeListing(id, b.reason);
  throw new BusinessException('Unknown action.');
});

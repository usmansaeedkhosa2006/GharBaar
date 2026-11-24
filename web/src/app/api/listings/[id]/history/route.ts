// [API] GET /api/listings/:id/history  ->  View Tenant History (UC-11)
import { app } from '@/bl/AppContext';
import { handle, idOf } from '../../../_lib';

export const GET = (_: Request, ctx: { params: Promise<{ id: string }> }) =>
  handle(async () => ({ reviews: await app().search.getTenantHistory(await idOf(ctx.params)) }));

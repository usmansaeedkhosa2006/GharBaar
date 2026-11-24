// [API] GET /api/listings/:id  ->  View Property Details (UC-10)
import { app } from '@/bl/AppContext';
import { handle, idOf } from '../../_lib';

export const GET = (_: Request, ctx: { params: Promise<{ id: string }> }) =>
  handle(async () => ({ listing: await app().search.getPropertyDetails(await idOf(ctx.params)) }));

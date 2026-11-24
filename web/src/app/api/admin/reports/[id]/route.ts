// [API] POST /api/admin/reports/:id {remarks}  -> Resolve Report (UC-08)
import { app } from '@/bl/AppContext';
import { handle, idOf, readJson } from '../../../_lib';

export const POST = (req: Request, ctx: { params: Promise<{ id: string }> }) => handle(async () => {
  const b = await readJson(req);
  await app().reports.resolveReport(await idOf(ctx.params), b.remarks);
});

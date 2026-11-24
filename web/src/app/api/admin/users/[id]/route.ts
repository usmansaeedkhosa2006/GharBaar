// [API] POST /api/admin/users/:id {active}  -> suspend / re-activate
import { app } from '@/bl/AppContext';
import { handle, idOf, readJson } from '../../../_lib';

export const POST = (req: Request, ctx: { params: Promise<{ id: string }> }) => handle(async () => {
  const b = await readJson(req);
  await app().admin.setUserActive(await idOf(ctx.params), !!b.active);
});

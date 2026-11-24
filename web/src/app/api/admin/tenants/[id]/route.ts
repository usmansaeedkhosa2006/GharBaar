// [API] GET /api/admin/tenants/:id -> tenant + claims ; POST {action: approve | reject, reason}  (UC-07)
import { app } from '@/bl/AppContext';
import { BusinessException } from '@/bl/BusinessException';
import { handle, idOf, readJson } from '../../../_lib';

type Ctx = { params: Promise<{ id: string }> };

export const GET = (_: Request, ctx: Ctx) => handle(async () => {
  const id = await idOf(ctx.params);
  return { tenant: await app().verification.getTenant(id), claims: await app().verification.getTenantClaims(id) };
});

export const POST = (req: Request, ctx: Ctx) => handle(async () => {
  const id = await idOf(ctx.params);
  const b = await readJson(req);
  if (b.action === 'approve') return app().verification.approveTenant(id);
  if (b.action === 'reject') return app().verification.rejectTenant(id, b.reason);
  throw new BusinessException('Unknown action.');
});

// [API] POST /api/auth/register  ->  AuthController.register  (UC-01)
import { app } from '@/bl/AppContext';
import { handle, readJson } from '../../_lib';

export const POST = (req: Request) => handle(async () => {
  const b = await readJson(req);
  const u = await app().auth.register(b.username, b.password, b.name, b.phone, b.role);
  return { user: u.toJSON() };
});

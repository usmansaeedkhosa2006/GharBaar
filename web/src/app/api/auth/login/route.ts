// [API] POST /api/auth/login  ->  AuthController.login  (UC-02)
import { app } from '@/bl/AppContext';
import { isAdminHost } from '@/middleware';
import { handle, readJson } from '../../_lib';

export const POST = (req: Request) => handle(async () => {
  const b = await readJson(req);
  const portal = isAdminHost(req.headers.get('host') ?? '') ? 'admin' : 'main';
  const u = await app().auth.login(b.username, b.password, portal);
  return { user: u.toJSON(), home: u.getHomeView() };
});

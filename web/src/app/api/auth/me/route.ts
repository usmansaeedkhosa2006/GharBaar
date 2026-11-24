// [API] GET /api/auth/me  -> current user or null
import { Session } from '@/bl/service/Session';
import { handle } from '../../_lib';

export const GET = () => handle(async () => ({ user: (await Session.getInstance().getCurrentUser())?.toJSON() ?? null }));

// [API] GET /api/admin/users -> all accounts
import { app } from '@/bl/AppContext';
import { handle } from '../../_lib';

export const GET = () => handle(async () => ({ users: await app().admin.getAllUsers() }));

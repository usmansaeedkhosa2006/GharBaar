// [API] POST /api/auth/logout
import { app } from '@/bl/AppContext';
import { handle } from '../../_lib';

export const POST = () => handle(() => app().auth.logout());

// [API] GET /api/admin/stats -> dashboard numbers
import { app } from '@/bl/AppContext';
import { handle } from '../../_lib';

export const GET = () => handle(() => app().admin.getDashboardStats());

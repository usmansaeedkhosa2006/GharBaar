// [API] GET /api/admin/tenants -> tenants waiting for verification (UC-07)
import { app } from '@/bl/AppContext';
import { handle } from '../../_lib';

export const GET = () => handle(async () => ({ tenants: await app().verification.getUnverifiedTenants() }));

// [API] GET /api/admin/reports?status=OPEN|RESOLVED   (UC-08 step 2)
import { app } from '@/bl/AppContext';
import { handle } from '../../_lib';

export const GET = (req: Request) => handle(async () => {
  const s = new URL(req.url).searchParams.get('status') === 'RESOLVED' ? 'RESOLVED' : 'OPEN';
  return { reports: await app().reports.getReports(s) };
});

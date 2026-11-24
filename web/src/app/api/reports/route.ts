// [API] GET /api/reports -> my reports ; POST -> Report Issue (UC-05)
import { app } from '@/bl/AppContext';
import { handle, readJson } from '../_lib';

export const GET = () => handle(async () => ({ reports: await app().reports.getMyReports() }));

export const POST = (req: Request) => handle(async () => {
  const b = await readJson(req);
  return { report: await app().reports.submitReport(b.type, b.description) };
});

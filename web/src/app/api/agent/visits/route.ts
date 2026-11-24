// [API] GET /api/agent/visits -> requests for my listings ; POST {requestId, accept}
import { app } from '@/bl/AppContext';
import { handle, readJson } from '../../_lib';

export const GET = () => handle(async () => ({ visits: await app().visits.getAgentVisitRequests() }));

export const POST = (req: Request) => handle(async () => {
  const b = await readJson(req);
  await app().visits.respondToVisit(Number(b.requestId), !!b.accept);
});

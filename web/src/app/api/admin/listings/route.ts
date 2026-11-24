// [API] GET /api/admin/listings?scope=pending|all   (UC-06 step 2 / all listings)
import { app } from '@/bl/AppContext';
import { handle } from '../../_lib';

export const GET = (req: Request) => handle(async () => {
  const scope = new URL(req.url).searchParams.get('scope');
  return { listings: scope === 'all' ? await app().admin.getAllListings() : await app().verification.getPendingListings() };
});

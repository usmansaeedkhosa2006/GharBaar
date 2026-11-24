// [API] GET /api/listings?q=&type=&sector=&min=&max=&furnished=&occupancy=&facilities=  (UC-09 Search)
import { app } from '@/bl/AppContext';
import { SearchCriteria } from '@/bl/domain/listing';
import { handle } from '../_lib';

export const GET = (req: Request) => handle(async () =>
  ({ results: await app().search.search(SearchCriteria.fromQuery(new URL(req.url).searchParams)) }));

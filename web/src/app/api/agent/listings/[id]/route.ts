// [API] GET / PUT / DELETE /api/agent/listings/:id  -> Edit Property Listing (UC-04)
import { app } from '@/bl/AppContext';
import { ListingDetails } from '@/bl/domain/listing';
import { handle, idOf, listingForm, readJson } from '../../../_lib';

type Ctx = { params: Promise<{ id: string }> };

export const GET = (_: Request, ctx: Ctx) => handle(async () => ({ listing: await app().listings.getListing(await idOf(ctx.params)) }));

export const PUT = (req: Request, ctx: Ctx) => handle(async () => {
  const f = listingForm(await req.formData());
  const d = new ListingDetails(f.title, f.address, f.propertyType, f.cdaSector, f.rent, f.description, f.furnishing,
    f.occupancyType, f.facilities, f.propertyStatus);
  return { listing: await app().listings.updateListing(await idOf(ctx.params), d, f.photos) };
});

export const DELETE = (req: Request, ctx: Ctx) => handle(async () => {
  const b = await readJson(req);
  await app().listings.deleteListing(await idOf(ctx.params), b.reason);
});

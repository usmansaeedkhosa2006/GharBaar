// [API] GET  /api/agent/listings  -> my listings
//       POST /api/agent/listings  -> Create Property Listing (UC-03, multipart form)
import { app } from '@/bl/AppContext';
import { ListingDetails } from '@/bl/domain/listing';
import { handle, listingForm } from '../../_lib';

export const GET = () => handle(async () => ({ listings: await app().listings.getMyListings() }));

export const POST = (req: Request) => handle(async () => {
  const f = listingForm(await req.formData());
  const d = new ListingDetails(f.title, f.address, f.propertyType, f.cdaSector, f.rent, f.description, f.furnishing,
    f.occupancyType, f.facilities);
  return { listing: await app().listings.createListing(d, f.photos) };
});

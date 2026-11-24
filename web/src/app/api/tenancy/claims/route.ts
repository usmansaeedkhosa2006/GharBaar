// [API] GET /api/tenancy/claims -> my claims ; POST (multipart) -> Submit Tenancy Claim (UC-13)
import { app } from '@/bl/AppContext';
import { handle } from '../../_lib';

const day = (s: FormDataEntryValue | null) => (s ? new Date(String(s) + 'T00:00:00+05:00') : null);

export const GET = () => handle(async () => ({ claims: await app().tenancy.getMyClaims() }));

export const POST = (req: Request) => handle(async () => {
  const fd = await req.formData();
  const proof = fd.get('proof');
  const claim = await app().tenancy.submitClaim(Number(fd.get('listingId')), day(fd.get('startDate')), day(fd.get('endDate')),
    proof instanceof File && proof.size > 0 ? proof : null, String(fd.get('cnic') ?? '').trim());
  return { claim };
});

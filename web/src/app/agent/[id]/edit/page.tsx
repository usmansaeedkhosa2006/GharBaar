'use client';
// [PAGE] /agent/:id/edit - UC-04 Edit Property Listing
import { useParams } from 'next/navigation';
import { AppShell } from '@/ui/components/AppShell';
import { ListingForm } from '@/ui/screens/ListingForm';

export default function Page() {
  const { id } = useParams<{ id: string }>();
  return <AppShell portal="agent" title="Edit Property Listing"><ListingForm listingId={Number(id)} /></AppShell>;
}

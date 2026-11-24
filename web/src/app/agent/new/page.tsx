'use client';
// [PAGE] /agent/new - UC-03 Create Property Listing
import { AppShell } from '@/ui/components/AppShell';
import { ListingForm } from '@/ui/screens/ListingForm';

export default function Page() {
  return <AppShell portal="agent" title="Create Property Listing"><ListingForm /></AppShell>;
}

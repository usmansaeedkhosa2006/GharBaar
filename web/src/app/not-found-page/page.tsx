// [PAGE] used by middleware to hide /admin on the main site
import { notFound } from 'next/navigation';

export default function Page() { notFound(); }

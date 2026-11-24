// [PAGE] / - send the user to the login page (middleware sends the admin host to /admin)
import { redirect } from 'next/navigation';

export default function Page() { redirect('/login'); }

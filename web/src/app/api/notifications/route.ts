// [API] GET /api/notifications ; POST -> mark all as read
import { app } from '@/bl/AppContext';
import { handle } from '../_lib';

export const GET = () => handle(() => app().notifications.getMyNotifications());
export const POST = () => handle(() => app().notifications.markAllRead());

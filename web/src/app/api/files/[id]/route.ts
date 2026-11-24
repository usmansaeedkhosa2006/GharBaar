// [API] GET /api/files/:id -> photo (public) or proof document (admin only)
import { app } from '@/bl/AppContext';
import { handle, idOf } from '../../_lib';

export async function GET(_: Request, ctx: { params: Promise<{ id: string }> }) {
  try {
    const f = await app().files.getFile(await idOf(ctx.params));
    return new Response(new Uint8Array(f.data), {
      headers: {
        'Content-Type': f.contentType,
        'Cache-Control': f.adminOnly ? 'private, no-store' : 'public, max-age=31536000, immutable',
        'Content-Disposition': 'inline; filename="' + f.fileName.replace(/"/g, '') + '"',
      },
    });
  } catch (e) {
    return handle(async () => { throw e; });
  }
}

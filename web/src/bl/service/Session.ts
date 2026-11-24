// =====================================================================
// [BL - SERVICE] Session  «pure fabrication, singleton»
// Web version: the logged-in user is kept in a signed, httpOnly cookie
// (JWT). getCurrentUser() reads it for the CURRENT request.
// The admin sub-domain gets its own cookie (cookies are per host).
// =====================================================================
import { cookies } from 'next/headers';
import { SignJWT, jwtVerify } from 'jose';
import { AuthException } from '../BusinessException';
import type { User } from '../domain/users';
import { UserRepository } from '../../db/repositories';

const COOKIE = 'gb_session';

export class Session {
  private static instance: Session | null = null;
  private readonly userRepo = new UserRepository();
  private readonly secret = new TextEncoder().encode(process.env.AUTH_SECRET || 'dev-only-secret-change-me');

  private constructor() {}

  static getInstance(): Session {
    if (!Session.instance) Session.instance = new Session();
    return Session.instance;
  }

  async setCurrentUser(u: User) {
    const token = await new SignJWT({ uid: u.getUserID(), role: u.getRole() })
      .setProtectedHeader({ alg: 'HS256' }).setIssuedAt().setExpirationTime('7d').sign(this.secret);
    (await cookies()).set(COOKIE, token, {
      httpOnly: true, sameSite: 'lax', secure: process.env.NODE_ENV === 'production', path: '/', maxAge: 60 * 60 * 24 * 7,
    });
  }

  async getCurrentUser(): Promise<User | null> {
    const token = (await cookies()).get(COOKIE)?.value;
    if (!token) return null;
    try {
      const { payload } = await jwtVerify(token, this.secret);
      const u = await this.userRepo.findById(Number(payload.uid));
      return u && u.isActive() ? u : null;            // suspended users are logged out
    } catch {
      return null;
    }
  }

  async clear() { (await cookies()).delete(COOKIE); }

  /** Current user as the required role, else 401 / 403. */
  // eslint-disable-next-line @typescript-eslint/no-unsafe-function-type
  async require<T extends User>(type: Function & { prototype: T }, roleName?: string): Promise<T> {
    const u = await this.getCurrentUser();
    if (!u) throw new AuthException('Please log in first.', 401);
    if (!(u instanceof type)) throw new AuthException(`Only a ${roleName ?? type.name} can do this.`, 403);
    return u as T;
  }
}

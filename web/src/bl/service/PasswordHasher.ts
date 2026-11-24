// =====================================================================
// [BL - SERVICE] PasswordHasher «pure fabrication» - BCrypt hashing
// (same $2a$ format as jBCrypt in the desktop app)
// =====================================================================
import bcrypt from 'bcryptjs';

export class PasswordHasher {
  hash(raw: string): Promise<string> { return bcrypt.hash(raw, 10); }
  async matches(raw: string, hash: string): Promise<boolean> {
    try { return !!hash && (await bcrypt.compare(raw, hash)); } catch { return false; }
  }
}

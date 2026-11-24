// =====================================================================
// [BL - DOMAIN] User hierarchy:  User (abstract) <- Customer <- Tenant
//                                                 <- Agent, Admin
// OOP: abstraction + polymorphism (getRole / getHomeView), encapsulation
// (private fields + getters), GRASP Creator / Expert as in the DCD.
// =====================================================================
import { BusinessException } from '../BusinessException';
import type { PasswordHasher } from '../service/PasswordHasher';
import type { Role, VerificationStatus } from './constants';
import { Report } from './Report';
import { VisitRequest } from './VisitRequest';
import type { PropertyListing } from './listing';
import { TenancyClaim } from './tenancy';

// ---------------------------------------------------------------------
// [CLASS] User (abstract)
// ---------------------------------------------------------------------
export abstract class User {
  protected active = true;

  protected constructor(
    protected userID: number,
    protected username: string,
    protected passwordHash: string,
    protected name: string,
    protected phone: string,
  ) {}

  /** [Expert] UC-02 password check */
  verifyPassword(raw: string, h: PasswordHasher): Promise<boolean> {
    return h.matches(raw, this.passwordHash);
  }

  /** [Creator] UC-05: a user files a report */
  fileReport(type: string, description: string): Report {
    return Report.create(this, type, description);
  }

  abstract getRole(): Role;
  abstract getHomeView(): string;

  /** UC-01 input validation */
  static validateRegistration(username: string, password: string, name: string, phone: string): string[] {
    const e: string[] = [];
    if (!/^[A-Za-z0-9_]{3,30}$/.test(username ?? '')) e.push('Username must be 3-30 letters, digits or underscores.');
    if (!password || password.length < 8) e.push('Password must be at least 8 characters.');
    else if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) e.push('Password must contain letters and numbers.');
    const n = (name ?? '').trim();
    if (n.length < 3 || n.length > 80) e.push('Full name must be 3-80 characters.');
    if (!/^03\d{9}$/.test(phone ?? '')) e.push('Phone must be a Pakistani mobile number like 03001234567.');
    return e;
  }

  getUserID() { return this.userID; }
  getUsername() { return this.username; }
  getPasswordHash() { return this.passwordHash; }
  getName() { return this.name; }
  getPhone() { return this.phone; }
  isActive() { return this.active; }
  setActive(a: boolean) { this.active = a; }
  assignId(id: number) { if (this.userID) throw new Error('ID already assigned'); this.userID = id; }

  /** Safe data sent to the browser (never the password hash). */
  toJSON() {
    return { userID: this.userID, username: this.username, name: this.name, phone: this.phone,
      role: this.getRole(), homeView: this.getHomeView(), active: this.active };
  }
}

// ---------------------------------------------------------------------
// [CLASS] Customer extends User
// ---------------------------------------------------------------------
export class Customer extends User {
  constructor(id: number, username: string, hash: string, name: string, phone: string) {
    super(id, username, hash, name, phone);
  }
  /** [Creator] UC-12 */
  makeVisitRequest(l: PropertyListing, dateTime: Date, notes: string): VisitRequest {
    return VisitRequest.create(this, l, dateTime, notes);
  }
  getRole(): Role { return 'CUSTOMER'; }
  getHomeView() { return '/home'; }
}

// ---------------------------------------------------------------------
// [CLASS] Tenant extends Customer   (UC-07, UC-13, UC-14)
// ---------------------------------------------------------------------
export class Tenant extends Customer {
  constructor(id: number, username: string, hash: string, name: string, phone: string,
              private cnic: string | null, private verificationStatus: VerificationStatus = 'PENDING',
              private rejectionReason: string | null = null) {
    super(id, username, hash, name, phone);
  }
  verify() { this.verificationStatus = 'VERIFIED'; this.rejectionReason = null; }
  reject(reason: string) {
    if (!reason?.trim()) throw new BusinessException('A rejection reason is required.');
    this.verificationStatus = 'REJECTED';
    this.rejectionReason = reason.trim();
  }
  isVerified() { return this.verificationStatus === 'VERIFIED'; }
  markPending() { this.verificationStatus = 'PENDING'; }

  /** [Creator] UC-13 */
  submitClaim(l: PropertyListing, start: Date, end: Date, proofPath: string): TenancyClaim {
    this.markPending();
    return TenancyClaim.create(this, l, start, end, proofPath);
  }
  static isValidCnic(c: string) { return /^\d{5}-\d{7}-\d$/.test(c ?? ''); }

  setCnic(c: string) { this.cnic = c; }
  getCnic() { return this.cnic; }
  getVerificationStatus() { return this.verificationStatus; }
  getRejectionReason() { return this.rejectionReason; }
  getRole(): Role { return 'TENANT'; }
  toJSON() { return { ...super.toJSON(), cnic: this.cnic, verificationStatus: this.verificationStatus, rejectionReason: this.rejectionReason }; }
}

// ---------------------------------------------------------------------
// [CLASS] Agent extends User   (UC-03, UC-04)
// ---------------------------------------------------------------------
export class Agent extends User {
  constructor(id: number, username: string, hash: string, name: string, phone: string) {
    super(id, username, hash, name, phone);
  }
  /** [Expert] only the owner may edit */
  ownsListing(l: PropertyListing | null) { return !!l && l.getOwner().getUserID() === this.userID; }
  getRole(): Role { return 'AGENT'; }
  getHomeView() { return '/agent'; }
}

// ---------------------------------------------------------------------
// [CLASS] Admin extends User   (UC-06, UC-07, UC-08)
// ---------------------------------------------------------------------
export class Admin extends User {
  constructor(id: number, username: string, hash: string, name: string, phone: string) {
    super(id, username, hash, name, phone);
  }
  getRole(): Role { return 'ADMIN'; }
  getHomeView() { return '/admin'; }
}

// ---------------------------------------------------------------------
// [CLASS] UserFactory  (GoF Factory) - picks the right subclass
// ---------------------------------------------------------------------
export class UserFactory {
  static createNew(role: string, username: string, hash: string, name: string, phone: string): User {
    if (role === 'CUSTOMER') return new Customer(0, username, hash, name, phone);
    if (role === 'AGENT') return new Agent(0, username, hash, name, phone);
    throw new BusinessException('You can only register as a Customer or an Agent.');
  }

  static fromRecord(r: any): User {
    let u: User;
    if (r.role === 'ADMIN') u = new Admin(r.user_id, r.username, r.password_hash, r.name, r.phone);
    else if (r.role === 'AGENT') u = new Agent(r.user_id, r.username, r.password_hash, r.name, r.phone);
    else if (r.tenant_status) u = new Tenant(r.user_id, r.username, r.password_hash, r.name, r.phone, r.cnic, r.tenant_status, r.tenant_rejection_reason);
    else u = new Customer(r.user_id, r.username, r.password_hash, r.name, r.phone);
    u.setActive(r.is_active);
    return u;
  }
}

// =====================================================================
// [BL - CONTROLLERS]  (GRASP Controller) - same classes and methods as
// the Design Class Diagram / JavaFX version. API routes call these;
// they never touch SQL directly.
//   AuthController            UC-01, UC-02
//   ListingController         UC-03, UC-04
//   PropertySearchController  UC-09, UC-10, UC-11
//   VisitController           UC-12
//   TenancyController         UC-13, UC-14
//   ReportController          UC-05, UC-08
//   VerificationController    UC-06, UC-07
//   AdminController           admin panel (users, all listings, stats)
//   NotificationController    bell icon
// =====================================================================
import { AuthException, BusinessException } from './BusinessException';
import { Admin, Agent, Customer, Tenant, User, UserFactory } from './domain/users';
import { ListingDetails, PropertyListing, SearchCriteria } from './domain/listing';
import type { Facility } from './domain/listing';
import type { TenancyClaim } from './domain/tenancy';
import { Session } from './service/Session';
import type { PasswordHasher } from './service/PasswordHasher';
import type { Notifier } from './service/Notifier';
import type { FileStorage } from './service/FileStorage';
import type {
  FileRepository, ListingRepository, NotificationRepository, ReportRepository, TenancyRepository,
  UserRepository, VisitRequestRepository,
} from '../db/repositories';

const session = () => Session.getInstance();
const fmt = (d: Date) => d.toLocaleString('en-PK', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Karachi' });

// =====================================================================
// [CONTROLLER] AuthController - UC-01 Register, UC-02 Login
// =====================================================================
export class AuthController {
  constructor(private userRepo: UserRepository, private hasher: PasswordHasher) {}

  async register(username: string, password: string, name: string, phone: string, role: string) {
    const errors = User.validateRegistration(username, password, name, phone);
    if (errors.length) throw new BusinessException(errors);                                 // ext. 4b
    if (await this.userRepo.existsByUsername(username.trim()))                               // ext. 3a
      throw new BusinessException('Username already exists. Please choose another one.');
    const u = UserFactory.createNew(role, username.trim(), await this.hasher.hash(password), name.trim(), phone.trim());
    await this.userRepo.save(u);                                                             // step 4
    return u;
  }

  /** portal = 'main' (customers & agents) or 'admin' (admin sub-domain) */
  async login(username: string, password: string, portal: 'main' | 'admin') {
    if (!username?.trim() || !password) throw new BusinessException('Please enter your username and password.');
    const u = await this.userRepo.findByUsername(username.trim());
    if (!u || !(await u.verifyPassword(password, this.hasher)))                              // ext. 4a
      throw new BusinessException('Invalid username or password. Please re-enter the details.');
    if (!u.isActive()) throw new BusinessException('Your account has been suspended by the Admin.');
    if (portal === 'admin' && !(u instanceof Admin)) throw new BusinessException('Only Admin accounts can sign in here.');
    if (portal === 'main' && u instanceof Admin) throw new BusinessException('Admins sign in on the admin portal.');
    await session().setCurrentUser(u);
    return u;                                                                                 // step 5: u.getHomeView()
  }

  logout() { return session().clear(); }
}

// =====================================================================
// [CONTROLLER] ListingController - UC-03 Create, UC-04 Edit / Delete
// =====================================================================
export class ListingController {
  constructor(private listingRepo: ListingRepository, private storage: FileStorage, private notifier: Notifier) {}

  async createListing(d: ListingDetails, photos: File[]) {
    const agent = await session().require(Agent);
    const errors = d.validate();                                                             // step 4
    if (!photos.length) errors.push('Add at least one photo of the property.');
    else if (!this.storage.isSupportedImage(photos)) errors.push('Photos must be JPG, PNG or WEBP and under 4 MB each.');
    if (errors.length) throw new BusinessException(errors);                                  // ext. 4b
    const paths = (await this.storage.store(photos, agent.getUserID())).split(';');
    const l = PropertyListing.create(agent, d, paths[0]);                                    // Creator
    await this.listingRepo.save(l, paths);                                                   // step 5
    await this.notifier.notifyAdmins(`New listing "${l.getTitle()}" is waiting for verification.`);
    return l;
  }

  async getMyListings() {
    const agent = await session().require(Agent);
    return this.listingRepo.findByAgent(agent.getUserID());
  }

  async getListing(id: number) {
    const agent = await session().require(Agent);
    const l = await this.listingRepo.findById(id);
    if (!l) throw new BusinessException('This listing no longer exists.');
    if (!agent.ownsListing(l)) throw new AuthException('You can only edit your own listings.', 403);
    return l;
  }

  async updateListing(id: number, d: ListingDetails, photos: File[]) {
    const l = await this.getListing(id);
    const errors = d.validate();
    if (photos.length && !this.storage.isSupportedImage(photos)) errors.push('Photos must be JPG, PNG or WEBP and under 4 MB each.');
    if (errors.length) throw new BusinessException(errors);
    const wasRejected = l.getListingStatus() === 'REJECTED';
    l.update(d);                                                                              // Expert
    let paths: string[] | null = null;
    if (photos.length) {
      paths = (await this.storage.store(photos, l.getOwner().getUserID())).split(';');
      l.setPhotoPath(paths[0]);
    }
    await this.listingRepo.update(l, paths);
    if (wasRejected) await this.notifier.notifyAdmins(`Listing "${l.getTitle()}" was corrected and resubmitted for verification.`);
    return l;
  }

  async deleteListing(id: number, reason: string) {
    const l = await this.getListing(id);
    if (!reason || reason.trim().length < 5) throw new BusinessException('Please give a reason (at least 5 characters) for deleting the listing.');
    await this.listingRepo.delete(l, reason.trim());
  }
}

// =====================================================================
// [CONTROLLER] PropertySearchController - UC-09, UC-10, UC-11
// =====================================================================
export class PropertySearchController {
  constructor(private listingRepo: ListingRepository, private tenancyRepo: TenancyRepository) {}

  async search(c: SearchCriteria) {
    await session().require(User, 'logged-in user');
    if (c.minRent > 0 && c.maxRent > 0 && c.minRent > c.maxRent)
      throw new BusinessException('Minimum rent cannot be greater than maximum rent.');
    const results = (await this.listingRepo.findVerified()).filter((l) => l.matches(c));   // step 4
    return this.sortByRelevance(results, c);                                                 // step 5
  }

  private sortByRelevance(results: PropertyListing[], c: SearchCriteria) {
    if (c.isEmpty()) return results.sort((a, b) => +b.getCreatedAt() - +a.getCreatedAt());
    const target = c.maxRent > 0 ? (c.minRent + c.maxRent) / 2 : 0;
    const score = (l: PropertyListing) => l.getAdvertisedFacilities().length * 0.5 + (l.isAvailable() ? 5 : 0)
      - (target > 0 ? Math.abs(l.getRent() - target) / Math.max(target, 1) : 0);
    return results.sort((a, b) => score(b) - score(a));
  }

  async getPropertyDetails(id: number) {
    await session().require(User, 'logged-in user');
    const l = await this.listingRepo.findById(id);
    if (!l || !l.isVerified()) throw new BusinessException('This listing is no longer available.');   // ext. 1a
    return l;
  }

  async getTenantHistory(id: number) {
    const l = await this.getPropertyDetails(id);
    l.attachTenantHistory(await this.tenancyRepo.findReviewsByListing(id));
    return l.getTenantHistory();                                                              // empty => ext. 2a
  }
}

// =====================================================================
// [CONTROLLER] VisitController - UC-12 Request Property Visit
// =====================================================================
export class VisitController {
  constructor(private listingRepo: ListingRepository, private visitRepo: VisitRequestRepository, private notifier: Notifier) {}

  async requestVisit(listingId: number, dateTime: Date | null, notes: string) {
    const customer = await session().require(Customer);
    if (!dateTime || isNaN(+dateTime)) throw new BusinessException('Please choose a date and time.');
    if (!this.isFutureDate(dateTime))                                                         // ext. 3a
      throw new BusinessException('The visit date must be in the future. Please choose a valid date.');
    if ((notes ?? '').length > 500) throw new BusinessException('Notes must be under 500 characters.');
    const l = await this.listingRepo.findById(listingId);
    if (!l || !l.isAvailable()) throw new BusinessException('Sorry, this property is no longer available.');   // ext. 4a
    const v = customer.makeVisitRequest(l, dateTime, notes);                                  // Creator
    await this.visitRepo.save(v);                                                             // step 4
    await this.notifier.notify(l.getOwner(), `${customer.getName()} requested a visit to "${l.getTitle()}" on ${fmt(dateTime)}.`);
    return v;
  }

  private isFutureDate(d: Date) { return d.getTime() > Date.now(); }

  async getMyVisits() { return this.visitRepo.findByCustomer((await session().require(Customer)).getUserID()); }
  async getAgentVisitRequests() { return this.visitRepo.findByAgent((await session().require(Agent)).getUserID()); }

  async respondToVisit(id: number, accept: boolean) {
    const agent = await session().require(Agent);
    const v = await this.visitRepo.findById(id);
    if (!v || !agent.ownsListing(v.getListing())) throw new BusinessException('Visit request not found.');
    if (accept) v.accept(); else v.decline();
    await this.visitRepo.update(v);
    await this.notifier.notify(v.getCustomer(), `Your visit request for "${v.getListing().getTitle()}" on ${fmt(v.getProposedDateTime())} was ${accept ? 'ACCEPTED' : 'DECLINED'} by the agent.`);
  }
}

// =====================================================================
// [CONTROLLER] TenancyController - UC-13 Claim, UC-14 Facility Review
// =====================================================================
export class TenancyController {
  constructor(private tenancyRepo: TenancyRepository, private listingRepo: ListingRepository, private userRepo: UserRepository,
              private storage: FileStorage, private notifier: Notifier) {}

  async submitClaim(listingId: number, start: Date | null, end: Date | null, proof: File | null, cnic: string) {
    const customer = await session().require(Customer);
    const e: string[] = [];
    if (!this.checkRequiredFields(start, end, proof)) e.push('Start date, end date and proof document are required.');   // 4a
    if (start && end && end < start) e.push('End date cannot be before start date.');
    if (start && start.getTime() > Date.now()) e.push('Tenancy start date cannot be in the future.');
    if (proof && !this.storage.isSupportedDocument(proof)) e.push('Unsupported file. Accepted formats: PDF, JPG, JPEG, PNG (max 4 MB).'); // 3a
    if (!Tenant.isValidCnic(cnic)) e.push('Enter your CNIC in the format 12345-1234567-1.');
    if (e.length) throw new BusinessException(e);

    const l = await this.listingRepo.findById(listingId);
    if (!l) throw new BusinessException('This property no longer exists.');
    const tenant = customer instanceof Tenant ? customer
      : new Tenant(customer.getUserID(), customer.getUsername(), customer.getPasswordHash(), customer.getName(), customer.getPhone(), cnic);
    tenant.setCnic(cnic);
    const proofPath = await this.storage.storeDocument(proof!, tenant.getUserID());
    const claim = tenant.submitClaim(l, start!, end!, proofPath);                            // Creator
    for (const other of await this.tenancyRepo.findClaimsByListing(listingId))                // ext. 5a
      if (claim.overlapsWith(other)) { claim.flagForReview(); break; }
    await this.userRepo.update(tenant);
    await this.tenancyRepo.saveClaim(claim);                                                  // step 5
    await this.notifier.notifyAdmins(`New tenancy claim by ${tenant.getName()} for "${l.getTitle()}"${claim.isFlagged() ? ' (FLAGGED: overlapping dates)' : ''}.`);
    return claim;
  }

  private checkRequiredFields(start: Date | null, end: Date | null, proof: File | null) {
    return !!start && !isNaN(+start) && !!end && !isNaN(+end) && !!proof && proof.size > 0;
  }

  async getMyClaims() { return this.tenancyRepo.findClaimsByTenant((await session().require(Customer)).getUserID()); }

  /** steps 1-2: eligibility + advertised facilities */
  async startFacilityReview(listingId: number): Promise<Facility[]> {
    return (await this.requireReviewableClaim(listingId)).getListing().getAdvertisedFacilities();
  }

  async submitReview(listingId: number, confirmations: Record<number, boolean>, comments: string) {
    const claim = await this.requireReviewableClaim(listingId);
    const facilities = claim.getListing().getAdvertisedFacilities();
    const missing = facilities.filter((f) => typeof confirmations?.[f.getFacilityID()] !== 'boolean');
    if (missing.length) throw new BusinessException(`Please confirm or deny every facility (${missing.map((f) => f.getName()).join(', ')}).`);
    if ((comments ?? '').length > 1000) throw new BusinessException('Comments must be under 1000 characters.');
    const review = claim.createReview(comments);                                             // Creator
    facilities.forEach((f) => review.addConfirmation(f, confirmations[f.getFacilityID()]));   // step 3
    await this.tenancyRepo.saveReview(review);                                                // step 5
    await this.notifier.notify(claim.getListing().getOwner(), `A verified tenant reviewed the facilities of "${claim.getListing().getTitle()}".`);
    return review;
  }

  private async requireReviewableClaim(listingId: number): Promise<TenancyClaim> {
    const c = await session().require(Customer);
    const claim = await this.tenancyRepo.findVerifiedClaim(c.getUserID(), listingId);
    if (!claim) throw new BusinessException('You must have a verified tenancy to leave a review.');          // 1a
    if (claim.hasReview()) throw new BusinessException('You have already submitted a review for this tenancy. Reviews cannot be changed once submitted.'); // 1b
    return claim;
  }
}

// =====================================================================
// [CONTROLLER] ReportController - UC-05 Report Issue, UC-08 Resolve
// =====================================================================
export class ReportController {
  constructor(private reportRepo: ReportRepository, private notifier: Notifier) {}

  async submitReport(type: string, description: string) {
    const u = await session().require(User, 'logged-in user');
    const r = u.fileReport(type, description);                                               // Creator + validation
    await this.reportRepo.save(r);                                                            // OPEN
    await this.notifier.notifyAdmins(`New ${type} report from ${u.getName()}.`);
    return r;
  }

  async getMyReports() { return this.reportRepo.findByReporter((await session().require(User, 'logged-in user')).getUserID()); }

  async getReports(status: 'OPEN' | 'RESOLVED') {
    await session().require(Admin);
    return this.reportRepo.findByStatus(status);
  }

  async resolveReport(id: number, remarks: string) {
    await session().require(Admin);
    const r = await this.reportRepo.findById(id);
    if (!r) throw new BusinessException('Report not found.');
    r.resolve(remarks);                                                                       // Expert (ext. 5a)
    await this.reportRepo.update(r);
    await this.notifier.notify(r.getReporter(), `Your report "${r.getType()}" was resolved: ${r.getResolutionRemarks()}`);
  }
}

// =====================================================================
// [CONTROLLER] VerificationController - UC-06 Listing, UC-07 Tenant
// =====================================================================
export class VerificationController {
  constructor(private listingRepo: ListingRepository, private userRepo: UserRepository, private tenancyRepo: TenancyRepository,
              private storage: FileStorage, private notifier: Notifier) {}

  private requireAdmin() { return session().require(Admin); }

  async getPendingListings() { await this.requireAdmin(); return this.listingRepo.findByStatus('PENDING_VERIFICATION'); }

  async getListingDetails(id: number) {
    await this.requireAdmin();
    const l = await this.listingRepo.findById(id);
    if (!l) throw new BusinessException('Listing not found.');
    return l;
  }

  async approveListing(id: number) {
    const l = await this.getListingDetails(id);
    l.approve();
    await this.listingRepo.update(l);
    await this.notifier.notify(l.getOwner(), `Your listing "${l.getTitle()}" has been VERIFIED and is now public.`);
  }

  async rejectListing(id: number, reason: string) {
    const l = await this.getListingDetails(id);
    l.reject(reason);
    await this.listingRepo.update(l);
    await this.notifier.notify(l.getOwner(), `Your listing "${l.getTitle()}" was REJECTED. Reason: ${l.getRejectionReason()}`);
  }

  async getUnverifiedTenants() { await this.requireAdmin(); return this.userRepo.findUnverifiedTenants(); }

  async getTenant(id: number) {
    await this.requireAdmin();
    const t = await this.userRepo.findTenantById(id);
    if (!t) throw new BusinessException('Tenant not found.');
    return t;
  }

  async getTenantClaims(id: number) { await this.requireAdmin(); return this.tenancyRepo.findClaimsByTenant(id); }

  async approveTenant(id: number) {
    const t = await this.getTenant(id);
    t.verify();
    await this.userRepo.update(t);
    for (const c of await this.tenancyRepo.findClaimsByTenant(id))
      if (c.getStatus() === 'PENDING') { c.verify(); await this.tenancyRepo.updateClaim(c); }
    await this.notifier.notify(t, 'Your tenancy has been VERIFIED. You can now submit a facility review for the property.');
  }

  async rejectTenant(id: number, reason: string) {
    const t = await this.getTenant(id);
    t.reject(reason);
    await this.userRepo.update(t);
    for (const c of await this.tenancyRepo.findClaimsByTenant(id))
      if (c.getStatus() === 'PENDING') { c.reject(t.getRejectionReason()); await this.tenancyRepo.updateClaim(c); }
    await this.notifier.notify(t, `Your tenancy claim was REJECTED. Reason: ${t.getRejectionReason()}`);
  }
}

// =====================================================================
// [CONTROLLER] AdminController - dashboard, users, all listings
// =====================================================================
export class AdminController {
  constructor(private userRepo: UserRepository, private listingRepo: ListingRepository, private reportRepo: ReportRepository,
              private tenancyRepo: TenancyRepository, private notifier: Notifier) {}

  async getDashboardStats() {
    await session().require(Admin);
    const [customers, agents, verifiedListings, pendingListings, rejectedListings, openReports, pendingClaims, tenants] = await Promise.all([
      this.userRepo.countByRole('CUSTOMER'), this.userRepo.countByRole('AGENT'), this.listingRepo.countByStatus('VERIFIED'),
      this.listingRepo.countByStatus('PENDING_VERIFICATION'), this.listingRepo.countByStatus('REJECTED'),
      this.reportRepo.countByStatus('OPEN'), this.tenancyRepo.countPendingClaims(), this.userRepo.findUnverifiedTenants()]);
    return { customers, agents, verifiedListings, pendingListings, rejectedListings, openReports, pendingClaims, pendingTenants: tenants.length };
  }

  async getAllUsers() { await session().require(Admin); return this.userRepo.findAll(); }

  async setUserActive(id: number, active: boolean) {
    const me = await session().require(Admin);
    if (me.getUserID() === id) throw new BusinessException('You cannot suspend your own account.');
    const u = await this.userRepo.findById(id);
    if (!u) throw new BusinessException('User not found.');
    if (u instanceof Admin) throw new BusinessException('Admin accounts cannot be suspended.');
    u.setActive(active);
    await this.userRepo.update(u);
    await this.notifier.notify(u, active ? 'Your account has been re-activated by the Admin.' : 'Your account has been suspended by the Admin.');
  }

  async getAllListings() { await session().require(Admin); return this.listingRepo.findAll(); }

  async removeListing(id: number, reason: string) {
    await session().require(Admin);
    const l = await this.listingRepo.findById(id);
    if (!l) throw new BusinessException('Listing not found.');
    if (!reason || reason.trim().length < 5) throw new BusinessException('Please enter a reason (at least 5 characters).');
    await this.listingRepo.delete(l, `[Removed by Admin] ${reason.trim()}`);
    await this.notifier.notify(l.getOwner(), `Your listing "${l.getTitle()}" was removed by the Admin. Reason: ${reason.trim()}`);
  }
}

// =====================================================================
// [CONTROLLER] NotificationController + FileController (photo / proof)
// =====================================================================
export class NotificationController {
  constructor(private repo: NotificationRepository) {}
  async getMyNotifications() {
    const u = await session().require(User, 'logged-in user');
    const [items, unread] = await Promise.all([this.repo.findByRecipient(u.getUserID()), this.repo.countUnread(u.getUserID())]);
    return { items, unread };
  }
  async markAllRead() { await this.repo.markAllRead((await session().require(User, 'logged-in user')).getUserID()); }
}

export class FileController {
  constructor(private storage: FileStorage) {}
  /** Photos are public; proof documents only for the Admin (UC-13 special requirement). */
  async getFile(id: number) {
    const f = await this.storage.load(`db:${id}`);
    if (!f) throw new BusinessException('File not found.');
    if (f.adminOnly) await session().require(Admin);
    return f;
  }
}

export type { FileRepository };

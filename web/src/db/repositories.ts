// =====================================================================
// [DB LAYER] Repositories  «pure fabrication»  - all SQL lives here.
// Same classes as the Java version: UserRepository, ListingRepository,
// ReportRepository, VisitRequestRepository, TenancyRepository,
// NotificationRepository, FileRepository.
// =====================================================================
import { BaseRepository } from './BaseRepository';
import { pkDate, pkDateTime } from './DBConnection';
import { Agent, Customer, Tenant, User, UserFactory } from '../bl/domain/users';
import { Facility, ListingDetails, PropertyListing } from '../bl/domain/listing';
import { FacilityReview, TenancyClaim } from '../bl/domain/tenancy';
import { VisitRequest } from '../bl/domain/VisitRequest';
import { Report } from '../bl/domain/Report';
import type { ListingStatus, ReportStatus } from '../bl/domain/constants';

const USER_COLS = `u.user_id, u.username, u.password_hash, u.name, u.phone, u.role, u.cnic,
  u.tenant_status, u.tenant_rejection_reason, u.is_active`;

// =====================================================================
// [REPO] UserRepository   -> users
// =====================================================================
export class UserRepository extends BaseRepository {
  async save(u: User) {
    const role = u.getRole() === 'TENANT' ? 'CUSTOMER' : u.getRole();
    u.assignId(await this.insert(
      'INSERT INTO users (username, password_hash, name, phone, role) VALUES ($1,$2,$3,$4,$5) RETURNING user_id',
      [u.getUsername(), u.getPasswordHash(), u.getName(), u.getPhone(), role]));
  }

  async update(u: User) {
    if (u instanceof Tenant)
      await this.execute(`UPDATE users SET name=$1, phone=$2, is_active=$3, cnic=$4, tenant_status=$5, tenant_rejection_reason=$6
        WHERE user_id=$7`, [u.getName(), u.getPhone(), u.isActive(), u.getCnic(), u.getVerificationStatus(), u.getRejectionReason(), u.getUserID()]);
    else
      await this.execute('UPDATE users SET name=$1, phone=$2, is_active=$3 WHERE user_id=$4',
        [u.getName(), u.getPhone(), u.isActive(), u.getUserID()]);
  }

  async existsByUsername(username: string) {
    return (await this.count('SELECT COUNT(*) FROM users WHERE LOWER(username)=LOWER($1)', [username])) > 0;
  }
  findByUsername(username: string) {
    return this.queryOne(`SELECT ${USER_COLS} FROM users u WHERE LOWER(u.username)=LOWER($1)`, [username], UserFactory.fromRecord);
  }
  findById(id: number) {
    return this.queryOne(`SELECT ${USER_COLS} FROM users u WHERE u.user_id=$1`, [id], UserFactory.fromRecord);
  }
  async findTenantById(id: number) {
    const u = await this.findById(id);
    return u instanceof Tenant ? u : null;
  }
  findUnverifiedTenants() {
    return this.query(`SELECT ${USER_COLS} FROM users u WHERE u.tenant_status='PENDING' ORDER BY u.user_id`, [],
      (r) => UserFactory.fromRecord(r) as Tenant);
  }
  findAll() { return this.query(`SELECT ${USER_COLS} FROM users u ORDER BY u.role, u.name`, [], UserFactory.fromRecord); }
  countByRole(role: string) { return this.count('SELECT COUNT(*) FROM users WHERE role=$1', [role]); }
}

// =====================================================================
// [REPO] ListingRepository  -> property_listings, listing_photos,
//                              facilities, listing_deletions
// =====================================================================
const LISTING_SELECT = `SELECT l.*, a.name AS agent_name, a.phone AS agent_phone, a.username AS agent_username
  FROM property_listings l JOIN users a ON a.user_id = l.agent_id `;

export class ListingRepository extends BaseRepository {
  /** UC-03: listing + photos + facilities in one transaction */
  save(l: PropertyListing, photoPaths: string[]) {
    return this.inTransaction(async (c) => {
      const id = await this.insert(`INSERT INTO property_listings (agent_id,title,address,property_type,cda_sector,rent,
          description,furnishing,occupancy_type,photo_path,listing_status,property_status)
          VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12) RETURNING listing_id`,
        [l.getOwner().getUserID(), l.getTitle(), l.getAddress(), l.getPropertyType(), l.getCdaSector(), l.getRent(),
          l.getDescription(), l.isFurnishing(), l.getOccupancyType(), l.getPhotoPath(), l.getListingStatus(), l.getPropertyStatus()], c);
      l.assignId(id);
      await this.savePhotos(c, id, photoPaths);
      for (const f of l.getAllFacilities())
        f.assignId(await this.insert('INSERT INTO facilities (listing_id, name, is_active) VALUES ($1,$2,$3) RETURNING facility_id',
          [id, f.getName(), f.isActive()], c));
    });
  }

  /** UC-04 / UC-06 */
  update(l: PropertyListing, newPhotoPaths: string[] | null = null) {
    return this.inTransaction(async (c) => {
      await this.execute(`UPDATE property_listings SET title=$1,address=$2,property_type=$3,cda_sector=$4,rent=$5,description=$6,
          furnishing=$7,occupancy_type=$8,photo_path=$9,listing_status=$10,property_status=$11,rejection_reason=$12,updated_at=NOW()
          WHERE listing_id=$13`,
        [l.getTitle(), l.getAddress(), l.getPropertyType(), l.getCdaSector(), l.getRent(), l.getDescription(), l.isFurnishing(),
          l.getOccupancyType(), l.getPhotoPath(), l.getListingStatus(), l.getPropertyStatus(), l.getRejectionReason(), l.getListingID()], c);
      for (const f of l.getAllFacilities()) {
        if (!f.getFacilityID())
          f.assignId(await this.insert('INSERT INTO facilities (listing_id, name, is_active) VALUES ($1,$2,$3) RETURNING facility_id',
            [l.getListingID(), f.getName(), f.isActive()], c));
        else await this.execute('UPDATE facilities SET is_active=$1 WHERE facility_id=$2', [f.isActive(), f.getFacilityID()], c);
      }
      if (newPhotoPaths?.length) {
        await this.execute('DELETE FROM listing_photos WHERE listing_id=$1', [l.getListingID()], c);
        await this.savePhotos(c, l.getListingID(), newPhotoPaths);
      }
    });
  }

  private async savePhotos(c: any, id: number, paths: string[]) {
    for (let i = 0; i < paths.length; i++)
      await this.execute('INSERT INTO listing_photos (listing_id, photo_path, sort_order) VALUES ($1,$2,$3)', [id, paths[i], i], c);
  }

  /** UC-04: log the reason, then remove */
  delete(l: PropertyListing, reason: string) {
    return this.inTransaction(async (c) => {
      await this.execute('INSERT INTO listing_deletions (listing_id, agent_id, title, reason) VALUES ($1,$2,$3,$4)',
        [l.getListingID(), l.getOwner().getUserID(), l.getTitle(), reason], c);
      await this.execute('DELETE FROM property_listings WHERE listing_id=$1', [l.getListingID()], c);
    });
  }

  async findById(id: number) {
    return (await this.load(await this.query(LISTING_SELECT + 'WHERE l.listing_id=$1', [id], map)))[0] ?? null;
  }
  async findByAgent(agentId: number) {
    return this.load(await this.query(LISTING_SELECT + 'WHERE l.agent_id=$1 ORDER BY l.created_at DESC', [agentId], map));
  }
  async findByStatus(s: ListingStatus) {
    return this.load(await this.query(LISTING_SELECT + 'WHERE l.listing_status=$1 ORDER BY l.created_at', [s], map));
  }
  findVerified() { return this.findByStatus('VERIFIED'); }
  async findAll() { return this.load(await this.query(LISTING_SELECT + 'ORDER BY l.created_at DESC', [], map)); }
  countByStatus(s: ListingStatus) { return this.count('SELECT COUNT(*) FROM property_listings WHERE listing_status=$1', [s]); }

  /** facilities + photos for many listings in 2 queries (fast search) */
  private async load(list: PropertyListing[]) {
    if (!list.length) return list;
    const byId = new Map(list.map((l) => [l.getListingID(), l]));
    const ids = [...byId.keys()];
    await this.query('SELECT facility_id, listing_id, name, is_active FROM facilities WHERE listing_id = ANY($1) ORDER BY facility_id', [ids],
      (r) => byId.get(r.listing_id)!.attachFacility(new Facility(r.facility_id, r.name, r.is_active)));
    await this.query('SELECT listing_id, photo_path FROM listing_photos WHERE listing_id = ANY($1) ORDER BY sort_order', [ids],
      (r) => byId.get(r.listing_id)!.attachPhoto(r.photo_path));
    return list;
  }
}

function map(r: any): PropertyListing {
  const owner = new Agent(r.agent_id, r.agent_username, '', r.agent_name, r.agent_phone);
  const d = new ListingDetails(r.title, r.address, r.property_type, r.cda_sector, Number(r.rent), r.description,
    r.furnishing, r.occupancy_type, [], r.property_status);
  return PropertyListing.restore(r.listing_id, owner, d, r.photo_path, r.listing_status, r.rejection_reason, r.created_at);
}

// =====================================================================
// [REPO] ReportRepository   -> reports
// =====================================================================
const REPORT_SELECT = `SELECT r.*, ${USER_COLS} FROM reports r JOIN users u ON u.user_id = r.reporter_id `;
const mapReport = (r: any) => Report.restore(r.report_id, UserFactory.fromRecord(r), r.type, r.description, r.status,
  r.resolution_remarks, r.date_time);

export class ReportRepository extends BaseRepository {
  async save(r: Report) {
    r.assignId(await this.insert('INSERT INTO reports (reporter_id, type, description, status) VALUES ($1,$2,$3,$4) RETURNING report_id',
      [r.getReporter().getUserID(), r.getType(), r.getDescription(), r.getStatus()]));
  }
  update(r: Report) {
    return this.execute(`UPDATE reports SET status=$1, resolution_remarks=$2,
      resolved_at=CASE WHEN $1::varchar='RESOLVED' THEN NOW() ELSE NULL END WHERE report_id=$3`,
    [r.getStatus(), r.getResolutionRemarks(), r.getReportID()]);
  }
  findById(id: number) { return this.queryOne(REPORT_SELECT + 'WHERE r.report_id=$1', [id], mapReport); }
  findByStatus(s: ReportStatus) { return this.query(REPORT_SELECT + 'WHERE r.status=$1 ORDER BY r.date_time DESC', [s], mapReport); }
  findByReporter(id: number) { return this.query(REPORT_SELECT + 'WHERE r.reporter_id=$1 ORDER BY r.date_time DESC', [id], mapReport); }
  countByStatus(s: ReportStatus) { return this.count('SELECT COUNT(*) FROM reports WHERE status=$1', [s]); }
}

// =====================================================================
// [REPO] VisitRequestRepository  -> visit_requests
// =====================================================================
export class VisitRequestRepository extends BaseRepository {
  constructor(private listingRepo: ListingRepository) { super(); }

  async save(v: VisitRequest) {
    v.assignId(await this.insert(`INSERT INTO visit_requests (customer_id, listing_id, proposed_datetime, notes, status)
      VALUES ($1,$2,$3,$4,$5) RETURNING request_id`,
    [v.getCustomer().getUserID(), v.getListing().getListingID(), pkDateTime(v.getProposedDateTime()), v.getNotes(), v.getStatus()]));
  }
  update(v: VisitRequest) { return this.execute('UPDATE visit_requests SET status=$1 WHERE request_id=$2', [v.getStatus(), v.getRequestID()]); }

  async findById(id: number) { return (await this.build(`WHERE v.request_id=$1`, [id]))[0] ?? null; }
  findByAgent(agentId: number) {
    return this.build(`JOIN property_listings l ON l.listing_id = v.listing_id WHERE l.agent_id=$1
      ORDER BY (v.status='PENDING') DESC, v.proposed_datetime`, [agentId]);
  }
  findByCustomer(id: number) { return this.build('WHERE v.customer_id=$1 ORDER BY v.proposed_datetime DESC', [id]); }

  private async build(where: string, params: unknown[]) {
    const rows = await this.query(`SELECT v.*, ${USER_COLS} FROM visit_requests v JOIN users u ON u.user_id = v.customer_id ${where}`, params, (r) => r);
    const cache = new Map<number, PropertyListing | null>();
    const out: VisitRequest[] = [];
    for (const r of rows) {
      if (!cache.has(r.listing_id)) cache.set(r.listing_id, await this.listingRepo.findById(r.listing_id));
      const l = cache.get(r.listing_id);
      if (l) out.push(VisitRequest.restore(r.request_id, UserFactory.fromRecord(r) as Customer, l, r.proposed_datetime, r.notes, r.status, r.created_at));
    }
    return out;
  }
}

// =====================================================================
// [REPO] TenancyRepository  -> tenancy_claims, facility_reviews,
//                              facility_confirmations
// =====================================================================
export class TenancyRepository extends BaseRepository {
  constructor(private listingRepo: ListingRepository) { super(); }

  async saveClaim(c: TenancyClaim) {
    c.assignId(await this.insert(`INSERT INTO tenancy_claims (tenant_id, listing_id, start_date, end_date, proof_document, status, flagged)
      VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING claim_id`,
    [c.getTenant().getUserID(), c.getListing().getListingID(), pkDate(c.getStartDate()), pkDate(c.getEndDate()), c.getProofDocument(), c.getStatus(), c.isFlagged()]));
  }
  updateClaim(c: TenancyClaim) {
    return this.execute('UPDATE tenancy_claims SET status=$1, flagged=$2, rejection_reason=$3 WHERE claim_id=$4',
      [c.getStatus(), c.isFlagged(), c.getRejectionReason(), c.getClaimID()]);
  }
  findClaimsByListing(listingId: number) { return this.build('WHERE c.listing_id=$1 ORDER BY c.start_date', [listingId]); }
  async findClaimsByTenant(tenantId: number) { return this.withReviews(await this.build('WHERE c.tenant_id=$1 ORDER BY c.created_at DESC', [tenantId])); }

  /** UC-14 precondition - prefers a verified claim without a review */
  async findVerifiedClaim(tenantId: number, listingId: number) {
    const list = await this.withReviews(await this.build(
      `WHERE c.tenant_id=$1 AND c.listing_id=$2 AND c.status='VERIFIED' ORDER BY c.end_date DESC`, [tenantId, listingId]));
    return list.find((c) => !c.hasReview()) ?? list[0] ?? null;
  }
  countPendingClaims() { return this.count(`SELECT COUNT(*) FROM tenancy_claims WHERE status='PENDING'`); }

  /** UC-14: review + confirmations atomically */
  saveReview(r: FacilityReview) {
    return this.inTransaction(async (c) => {
      const id = await this.insert('INSERT INTO facility_reviews (claim_id, comments, submitted_date) VALUES ($1,$2,$3) RETURNING review_id',
        [r.getClaim()!.getClaimID(), r.comments, pkDate(r.getSubmittedDate())], c);
      r.assignId(id);
      for (const fc of r.getConfirmations())
        await this.execute('INSERT INTO facility_confirmations (review_id, facility_id, confirmed) VALUES ($1,$2,$3)',
          [id, fc.getFacility().getFacilityID(), fc.isConfirmed()], c);
    });
  }

  /** UC-11: reviews of verified tenancies for one listing */
  async findReviewsByListing(listingId: number) {
    const byId = new Map<number, FacilityReview>();
    const reviews = await this.query(`SELECT r.review_id, r.comments, r.submitted_date, c.start_date, c.end_date, u.name
        FROM facility_reviews r JOIN tenancy_claims c ON c.claim_id = r.claim_id JOIN users u ON u.user_id = c.tenant_id
        WHERE c.listing_id=$1 AND c.status='VERIFIED' ORDER BY r.submitted_date DESC`, [listingId], (r) => {
      const fr = new FacilityReview(r.review_id, null, r.comments, r.submitted_date);
      fr.setDisplayInfo(r.name, r.start_date, r.end_date);
      byId.set(r.review_id, fr);
      return fr;
    });
    if (!reviews.length) return reviews;
    await this.query(`SELECT fc.review_id, fc.confirmed, f.facility_id, f.name, f.is_active FROM facility_confirmations fc
        JOIN facilities f ON f.facility_id = fc.facility_id WHERE f.listing_id=$1 ORDER BY f.facility_id`, [listingId],
    (r) => byId.get(r.review_id)?.addConfirmation(new Facility(r.facility_id, r.name, r.is_active), r.confirmed));
    return reviews;
  }

  private async build(where: string, params: unknown[]) {
    const rows = await this.query(`SELECT c.*, ${USER_COLS} FROM tenancy_claims c JOIN users u ON u.user_id = c.tenant_id ${where}`, params, (r) => r);
    const out: TenancyClaim[] = [];
    const cache = new Map<number, PropertyListing | null>();
    for (const r of rows) {
      if (!cache.has(r.listing_id)) cache.set(r.listing_id, await this.listingRepo.findById(r.listing_id));
      const l = cache.get(r.listing_id);
      if (l) out.push(TenancyClaim.restore(r.claim_id, UserFactory.fromRecord(r) as Tenant, l, r.start_date, r.end_date,
        r.proof_document, r.status, r.flagged, r.rejection_reason, r.created_at));
    }
    return out;
  }

  private async withReviews(claims: TenancyClaim[]) {
    for (const c of claims) {
      const r = await this.queryOne('SELECT review_id, comments, submitted_date FROM facility_reviews WHERE claim_id=$1', [c.getClaimID()],
        (x) => new FacilityReview(x.review_id, c, x.comments, x.submitted_date));
      if (r) c.attachReview(r);
    }
    return claims;
  }
}

// =====================================================================
// [REPO] NotificationRepository  -> notifications
// =====================================================================
export class NotificationRepository extends BaseRepository {
  save(recipientId: number, message: string) {
    return this.execute('INSERT INTO notifications (recipient_id, message) VALUES ($1,$2)', [recipientId, message]);
  }
  saveForRole(role: string, message: string) {
    return this.execute('INSERT INTO notifications (recipient_id, message) SELECT user_id, $1 FROM users WHERE role=$2', [message, role]);
  }
  findByRecipient(id: number) {
    return this.query('SELECT * FROM notifications WHERE recipient_id=$1 ORDER BY created_at DESC LIMIT 50', [id],
      (r) => ({ notificationID: r.notification_id, message: r.message, read: r.is_read, createdAt: r.created_at }));
  }
  countUnread(id: number) { return this.count('SELECT COUNT(*) FROM notifications WHERE recipient_id=$1 AND is_read=FALSE', [id]); }
  markAllRead(id: number) { return this.execute('UPDATE notifications SET is_read=TRUE WHERE recipient_id=$1', [id]); }
}

// =====================================================================
// [REPO] FileRepository  -> stored_files (photos, proof documents)
// =====================================================================
export class FileRepository extends BaseRepository {
  insertFile(name: string, contentType: string, data: Buffer, adminOnly: boolean, uploadedBy: number | null) {
    return this.insert(`INSERT INTO stored_files (file_name, content_type, data, admin_only, uploaded_by)
      VALUES ($1,$2,$3,$4,$5) RETURNING file_id`, [name, contentType, data, adminOnly, uploadedBy]);
  }
  findById(id: number) {
    return this.queryOne('SELECT file_name, content_type, data, admin_only FROM stored_files WHERE file_id=$1', [id],
      (r) => ({ fileName: r.file_name as string, contentType: r.content_type as string, data: r.data as Buffer, adminOnly: r.admin_only as boolean }));
  }
}

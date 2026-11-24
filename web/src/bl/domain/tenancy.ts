// =====================================================================
// [BL - DOMAIN] TenancyClaim, FacilityReview, FacilityConfirmation
// UC-13 Submit Tenancy Claim, UC-14 Submit Facility Review,
// UC-11 View Tenant History
// =====================================================================
import { BusinessException } from '../BusinessException';
import type { ClaimStatus } from './constants';
import type { Facility, PropertyListing } from './listing';
import type { Tenant } from './users';

// ---------------------------------------------------------------------
// [CLASS] TenancyClaim - Expert for overlap check; Creator of review
// ---------------------------------------------------------------------
export class TenancyClaim {
  private claimID = 0;
  private status: ClaimStatus = 'PENDING';
  private flagged = false;
  private rejectionReason: string | null = null;
  private createdAt = new Date();
  private review: FacilityReview | null = null;

  private constructor(private tenant: Tenant, private listing: PropertyListing,
                      private startDate: Date, private endDate: Date, private proofDocument: string) {}

  static create(t: Tenant, l: PropertyListing, start: Date, end: Date, proof: string) {
    return new TenancyClaim(t, l, start, end, proof);
  }

  static restore(id: number, t: Tenant, l: PropertyListing, start: Date, end: Date, proof: string,
                 status: ClaimStatus, flagged: boolean, reason: string | null, createdAt: Date) {
    const c = new TenancyClaim(t, l, start, end, proof);
    Object.assign(c, { claimID: id, status, flagged, rejectionReason: reason, createdAt });
    return c;
  }

  isVerified() { return this.status === 'VERIFIED'; }

  /** extension 5a: same property, intersecting dates, not rejected */
  overlapsWith(o: TenancyClaim): boolean {
    if (o === this || o.status === 'REJECTED') return false;
    if (o.listing.getListingID() !== this.listing.getListingID()) return false;
    return this.endDate >= o.startDate && this.startDate <= o.endDate;
  }

  flagForReview() { this.flagged = true; }
  isFlagged() { return this.flagged; }
  verify() { this.status = 'VERIFIED'; this.rejectionReason = null; }
  reject(reason: string | null) { this.status = 'REJECTED'; this.rejectionReason = reason; }

  hasReview() { return this.review !== null; }
  getReview() { return this.review; }
  attachReview(r: FacilityReview) { this.review = r; }

  /** [Creator] UC-14: one review per verified tenancy */
  createReview(comments: string): FacilityReview {
    if (!this.isVerified()) throw new BusinessException('You must have a verified tenancy to leave a review.');
    if (this.hasReview()) throw new BusinessException('You have already submitted a review for this tenancy. Reviews cannot be changed once submitted.');
    this.review = new FacilityReview(0, this, comments, new Date());
    return this.review;
  }

  assignId(id: number) { if (!this.claimID) this.claimID = id; }
  getClaimID() { return this.claimID; }
  getTenant() { return this.tenant; }
  getListing() { return this.listing; }
  getStartDate() { return this.startDate; }
  getEndDate() { return this.endDate; }
  getProofDocument() { return this.proofDocument; }
  getStatus() { return this.status; }
  getRejectionReason() { return this.rejectionReason; }

  toJSON() {
    return {
      claimID: this.claimID, status: this.status, flagged: this.flagged, rejectionReason: this.rejectionReason,
      startDate: this.startDate, endDate: this.endDate, proofDocument: this.proofDocument, createdAt: this.createdAt,
      hasReview: this.hasReview(),
      listing: { listingID: this.listing.getListingID(), title: this.listing.getTitle(), photoPath: this.listing.getPhotoPath(),
        ownerName: this.listing.getOwner().getName() },
      tenant: { userID: this.tenant.getUserID(), name: this.tenant.getName() },
    };
  }
}

// ---------------------------------------------------------------------
// [CLASS] FacilityReview - Creator of FacilityConfirmation; immutable once saved
// ---------------------------------------------------------------------
export class FacilityReview {
  private confirmations: FacilityConfirmation[] = [];
  private tenantName = '';
  private tenancyStart: Date | null = null;
  private tenancyEnd: Date | null = null;
  readonly comments: string;

  constructor(private reviewID: number, private claim: TenancyClaim | null, comments: string, private submittedDate: Date) {
    this.comments = (comments ?? '').trim();
  }

  addConfirmation(f: Facility, confirmed: boolean) {
    const fc = new FacilityConfirmation(f, confirmed);
    this.confirmations.push(fc);
    return fc;
  }

  getConfirmations() { return [...this.confirmations]; }
  confirmedCount() { return this.confirmations.filter((c) => c.isConfirmed()).length; }
  setDisplayInfo(name: string, start: Date, end: Date) { this.tenantName = name; this.tenancyStart = start; this.tenancyEnd = end; }
  assignId(id: number) { if (!this.reviewID) this.reviewID = id; }
  getReviewID() { return this.reviewID; }
  getClaim() { return this.claim; }
  getSubmittedDate() { return this.submittedDate; }

  toJSON() {
    return {
      reviewID: this.reviewID, comments: this.comments, submittedDate: this.submittedDate,
      tenantName: this.tenantName, tenancyStart: this.tenancyStart, tenancyEnd: this.tenancyEnd,
      confirmedCount: this.confirmedCount(),
      confirmations: this.confirmations.map((c) => ({ facility: c.getFacility().getName(), confirmed: c.isConfirmed() })),
    };
  }
}

// ---------------------------------------------------------------------
// [CLASS] FacilityConfirmation - "was facility X provided?"
// ---------------------------------------------------------------------
export class FacilityConfirmation {
  constructor(private facility: Facility, private confirmed: boolean) {}
  getFacility() { return this.facility; }
  isConfirmed() { return this.confirmed; }
}

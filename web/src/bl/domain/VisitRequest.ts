// =====================================================================
// [BL - DOMAIN] VisitRequest  (UC-12 Request Property Visit)
// =====================================================================
import { BusinessException } from '../BusinessException';
import type { VisitStatus } from './constants';
import type { PropertyListing } from './listing';
import type { Customer } from './users';

export class VisitRequest {
  private requestID = 0;
  private status: VisitStatus = 'PENDING';
  private createdAt = new Date();
  private readonly notes: string;

  private constructor(private customer: Customer, private listing: PropertyListing,
                      private proposedDateTime: Date, notes: string) {
    this.notes = (notes ?? '').trim();
  }

  static create(c: Customer, l: PropertyListing, dt: Date, notes: string) { return new VisitRequest(c, l, dt, notes); }

  static restore(id: number, c: Customer, l: PropertyListing, dt: Date, notes: string, status: VisitStatus, createdAt: Date) {
    const v = new VisitRequest(c, l, dt, notes);
    Object.assign(v, { requestID: id, status, createdAt });
    return v;
  }

  accept() { this.requirePending(); this.status = 'ACCEPTED'; }
  decline() { this.requirePending(); this.status = 'DECLINED'; }
  private requirePending() { if (this.status !== 'PENDING') throw new BusinessException('This visit request was already answered.'); }

  assignId(id: number) { if (!this.requestID) this.requestID = id; }
  getRequestID() { return this.requestID; }
  getCustomer() { return this.customer; }
  getListing() { return this.listing; }
  getProposedDateTime() { return this.proposedDateTime; }
  getNotes() { return this.notes; }
  getStatus() { return this.status; }

  toJSON() {
    return {
      requestID: this.requestID, status: this.status, notes: this.notes, proposedDateTime: this.proposedDateTime,
      createdAt: this.createdAt,
      customer: { name: this.customer.getName(), phone: this.customer.getPhone() },
      listing: { listingID: this.listing.getListingID(), title: this.listing.getTitle(), photoPath: this.listing.getPhotoPath(),
        ownerName: this.listing.getOwner().getName() },
    };
  }
}

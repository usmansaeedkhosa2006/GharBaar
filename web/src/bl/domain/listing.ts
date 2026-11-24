// =====================================================================
// [BL - DOMAIN] Facility, ListingDetails, SearchCriteria, PropertyListing
// =====================================================================
import { BusinessException } from '../BusinessException';
import { CDA_SECTORS, OCCUPANCY_TYPES, PROPERTY_TYPES, type ListingStatus, type PropertyStatus } from './constants';
import type { Agent } from './users';
import type { FacilityReview } from './tenancy';

// ---------------------------------------------------------------------
// [CLASS] Facility
// ---------------------------------------------------------------------
export class Facility {
  constructor(private facilityID: number, private name: string, private active = true) {}
  getFacilityID() { return this.facilityID; }
  getName() { return this.name; }
  isActive() { return this.active; }
  setActive(a: boolean) { this.active = a; }
  assignId(id: number) { if (!this.facilityID) this.facilityID = id; }
  toJSON() { return { facilityID: this.facilityID, name: this.name, active: this.active }; }
}

// ---------------------------------------------------------------------
// [CLASS] ListingDetails - form data; validates itself (Expert)
// ---------------------------------------------------------------------
export class ListingDetails {
  readonly title: string;
  readonly address: string;
  readonly description: string;
  readonly facilityNames: string[];

  constructor(title: string, address: string, readonly propertyType: string, readonly cdaSector: string,
              readonly rent: number, description: string, readonly furnishing: boolean,
              readonly occupancyType: string, facilityNames: string[],
              readonly propertyStatus: PropertyStatus = 'AVAILABLE') {
    this.title = (title ?? '').trim();
    this.address = (address ?? '').trim();
    this.description = (description ?? '').trim();
    const seen = new Set<string>();
    this.facilityNames = (facilityNames ?? []).map((f) => f.trim()).filter((f) => {
      const k = f.toLowerCase();
      if (!f || seen.has(k)) return false;
      seen.add(k);
      return true;
    });
  }

  validate(): string[] {
    const e: string[] = [];
    if (this.title.length < 5 || this.title.length > 100) e.push('Title must be 5-100 characters.');
    if (this.address.length < 5 || this.address.length > 200) e.push('Address must be 5-200 characters.');
    if (!PROPERTY_TYPES.includes(this.propertyType)) e.push('Select a valid property type.');
    if (!CDA_SECTORS.includes(this.cdaSector)) e.push('Select a valid CDA sector.');
    if (!(this.rent > 0 && this.rent <= 10_000_000)) e.push('Rent must be between 1 and 10,000,000 PKR.');
    if (this.description.length < 20) e.push('Description must be at least 20 characters.');
    if (!OCCUPANCY_TYPES.includes(this.occupancyType)) e.push('Select a valid occupancy type.');
    if (this.facilityNames.length === 0) e.push('Add at least one facility.');
    if (this.facilityNames.some((f) => f.length > 50)) e.push('Facility names must be under 50 characters.');
    if (!['AVAILABLE', 'OCCUPIED', 'UNDER_MAINTENANCE'].includes(this.propertyStatus)) e.push('Invalid property status.');
    return e;
  }
}

// ---------------------------------------------------------------------
// [CLASS] SearchCriteria - UC-09 filters (null/0 = any)
// ---------------------------------------------------------------------
export class SearchCriteria {
  keyword: string | null = null;
  propertyType: string | null = null;
  cdaSector: string | null = null;
  minRent = 0;
  maxRent = 0;
  furnishing: boolean | null = null;
  occupancyType: string | null = null;
  facilities: string[] = [];

  /** extension 3a: blank filters = show all verified listings */
  isEmpty() {
    return !this.keyword?.trim() && !this.propertyType && !this.cdaSector && this.minRent <= 0 && this.maxRent <= 0
      && this.furnishing === null && !this.occupancyType && this.facilities.length === 0;
  }

  static fromQuery(q: URLSearchParams): SearchCriteria {
    const c = new SearchCriteria();
    c.keyword = q.get('q') || null;
    c.propertyType = q.get('type') || null;
    c.cdaSector = q.get('sector') || null;
    c.minRent = Math.max(0, Number(q.get('min') || 0)) || 0;
    c.maxRent = Math.max(0, Number(q.get('max') || 0)) || 0;
    const f = q.get('furnished');
    c.furnishing = f === 'true' ? true : f === 'false' ? false : null;
    c.occupancyType = q.get('occupancy') || null;
    c.facilities = (q.get('facilities') || '').split(',').map((s) => s.trim()).filter(Boolean);
    return c;
  }
}

// ---------------------------------------------------------------------
// [CLASS] PropertyListing - Information Expert for verification,
//         editing, search matching, facilities and tenant history
// ---------------------------------------------------------------------
export class PropertyListing {
  private listingID = 0;
  private title!: string;
  private address!: string;
  private propertyType!: string;
  private cdaSector!: string;
  private rent!: number;
  private description!: string;
  private furnishing!: boolean;
  private occupancyType!: string;
  private propertyStatus: PropertyStatus = 'AVAILABLE';
  private listingStatus: ListingStatus = 'PENDING_VERIFICATION';
  private rejectionReason: string | null = null;
  private createdAt = new Date();
  private facilities: Facility[] = [];
  private photos: string[] = [];
  private tenantHistory: FacilityReview[] = [];

  private constructor(private owner: Agent, d: ListingDetails, private photoPath: string | null) {
    this.copyDetails(d);
  }

  /** [Creator] UC-03: new listing starts as Pending Verification */
  static create(owner: Agent, d: ListingDetails, photoPath: string): PropertyListing {
    const l = new PropertyListing(owner, d, photoPath);
    d.facilityNames.forEach((f) => l.addFacility(f));
    return l;
  }

  /** rebuilt from the database */
  static restore(id: number, owner: Agent, d: ListingDetails, photoPath: string | null, ls: ListingStatus,
                 rejectionReason: string | null, createdAt: Date): PropertyListing {
    const l = new PropertyListing(owner, d, photoPath);
    l.listingID = id;
    l.listingStatus = ls;
    l.propertyStatus = d.propertyStatus;
    l.rejectionReason = rejectionReason;
    l.createdAt = createdAt;
    return l;
  }

  private copyDetails(d: ListingDetails) {
    this.title = d.title; this.address = d.address; this.propertyType = d.propertyType;
    this.cdaSector = d.cdaSector; this.rent = d.rent; this.description = d.description;
    this.furnishing = d.furnishing; this.occupancyType = d.occupancyType;
  }

  addFacility(name: string) { const f = new Facility(0, name); this.facilities.push(f); return f; }
  attachFacility(f: Facility) { this.facilities.push(f); }
  attachPhoto(p: string) { this.photos.push(p); }
  attachTenantHistory(r: FacilityReview[]) { this.tenantHistory = r; }

  /** UC-04 edit: update fields, sync facilities by name, resubmit if rejected */
  update(d: ListingDetails) {
    this.copyDetails(d);
    this.propertyStatus = d.propertyStatus;
    const wanted = d.facilityNames.map((f) => f.toLowerCase());
    this.facilities.forEach((f) => f.setActive(wanted.includes(f.getName().toLowerCase())));
    for (const n of d.facilityNames)
      if (!this.facilities.some((f) => f.getName().toLowerCase() === n.toLowerCase())) this.addFacility(n);
    if (this.listingStatus === 'REJECTED') { this.listingStatus = 'PENDING_VERIFICATION'; this.rejectionReason = null; }
  }

  setPhotoPath(p: string) { this.photoPath = p; }

  /** UC-06 */
  approve() {
    if (this.listingStatus === 'VERIFIED') throw new BusinessException('Listing is already verified.');
    this.listingStatus = 'VERIFIED';
    this.rejectionReason = null;
  }
  reject(reason: string) {
    if (!reason?.trim()) throw new BusinessException('Please enter the reason for rejection.');
    this.listingStatus = 'REJECTED';
    this.rejectionReason = reason.trim();
  }

  isVerified() { return this.listingStatus === 'VERIFIED'; }
  isAvailable() { return this.isVerified() && this.propertyStatus === 'AVAILABLE'; }

  /** UC-09: does this listing satisfy the filters? */
  matches(c: SearchCriteria): boolean {
    if (!this.isVerified()) return false;
    if (c.isEmpty()) return true;
    if (c.keyword?.trim()) {
      const hay = `${this.title} ${this.address} ${this.cdaSector} ${this.propertyType} ${this.description}`.toLowerCase();
      if (!hay.includes(c.keyword.trim().toLowerCase())) return false;
    }
    if (c.propertyType && c.propertyType !== this.propertyType) return false;
    if (c.cdaSector && c.cdaSector !== this.cdaSector) return false;
    if (c.minRent > 0 && this.rent < c.minRent) return false;
    if (c.maxRent > 0 && this.rent > c.maxRent) return false;
    if (c.furnishing !== null && c.furnishing !== this.furnishing) return false;
    if (c.occupancyType && c.occupancyType !== this.occupancyType) return false;
    const have = this.getAdvertisedFacilities().map((f) => f.getName().toLowerCase());
    return c.facilities.every((w) => have.includes(w.toLowerCase()));
  }

  getAdvertisedFacilities() { return this.facilities.filter((f) => f.isActive()); }
  getAllFacilities() { return [...this.facilities]; }
  getTenantHistory() { return [...this.tenantHistory]; }
  assignId(id: number) { if (this.listingID) throw new Error('ID already assigned'); this.listingID = id; }

  getListingID() { return this.listingID; }
  getTitle() { return this.title; }
  getAddress() { return this.address; }
  getPropertyType() { return this.propertyType; }
  getCdaSector() { return this.cdaSector; }
  getRent() { return this.rent; }
  getDescription() { return this.description; }
  isFurnishing() { return this.furnishing; }
  getOccupancyType() { return this.occupancyType; }
  getPhotoPath() { return this.photoPath; }
  getPhotos() { return this.photos.length ? [...this.photos] : this.photoPath ? [this.photoPath] : []; }
  getListingStatus() { return this.listingStatus; }
  getPropertyStatus() { return this.propertyStatus; }
  getRejectionReason() { return this.rejectionReason; }
  getCreatedAt() { return this.createdAt; }
  getOwner() { return this.owner; }

  toJSON() {
    return {
      listingID: this.listingID, title: this.title, address: this.address, propertyType: this.propertyType,
      cdaSector: this.cdaSector, rent: this.rent, description: this.description, furnishing: this.furnishing,
      occupancyType: this.occupancyType, photoPath: this.photoPath, photos: this.getPhotos(),
      listingStatus: this.listingStatus, propertyStatus: this.propertyStatus, rejectionReason: this.rejectionReason,
      createdAt: this.createdAt, available: this.isAvailable(),
      facilities: this.getAdvertisedFacilities().map((f) => f.toJSON()),
      owner: { userID: this.owner.getUserID(), name: this.owner.getName(), phone: this.owner.getPhone() },
    };
  }
}

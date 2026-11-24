// =====================================================================
// [BL - DOMAIN] Enums and allowed values (same as the Java version)
// Safe to import from the UI (no server code here).
// =====================================================================
export type ListingStatus = 'PENDING_VERIFICATION' | 'VERIFIED' | 'REJECTED';
export type PropertyStatus = 'AVAILABLE' | 'OCCUPIED' | 'UNDER_MAINTENANCE';
export type VisitStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';
export type ClaimStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';
export type ReportStatus = 'OPEN' | 'RESOLVED';
export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';
export type Role = 'CUSTOMER' | 'AGENT' | 'ADMIN' | 'TENANT';

export const PROPERTY_TYPES = ['Portion', 'Flat', 'Apartment', 'Hostel Room'];
export const OCCUPANCY_TYPES = ['Family', 'Individual', 'Shared'];
export const CDA_SECTORS = ['D-12', 'E-7', 'E-11', 'F-6', 'F-7', 'F-8', 'F-10', 'F-11', 'G-6', 'G-7', 'G-8',
  'G-9', 'G-10', 'G-11', 'G-13', 'G-14', 'G-15', 'H-13', 'I-8', 'I-9', 'I-10', 'B-17'];
export const COMMON_FACILITIES = ['Wi-Fi', 'TV', 'Furniture', 'Elevator', 'Air Conditioning', 'Generator',
  'Parking', 'Gas', 'Security', 'Gym', 'Meals', 'Laundry'];
export const REPORT_TYPES = ['Fake Listing', 'Fraud / Scam', 'Wrong Information', 'Agent Behaviour', 'Technical Problem', 'Other'];

export const statusLabel = (s: string) =>
  s === 'PENDING_VERIFICATION' ? 'Pending Verification'
    : s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, ' ');

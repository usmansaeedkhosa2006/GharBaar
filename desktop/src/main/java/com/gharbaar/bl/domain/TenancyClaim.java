package com.gharbaar.bl.domain;

import com.gharbaar.bl.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * =====================================================================
 * [BL - DOMAIN] TenancyClaim  (UC-13 Submit Tenancy Claim)
 * GRASP Expert: knows if it overlaps another claim (extension 5a)
 *               and whether it already has a review (UC-14 ext. 1b).
 * GRASP Creator: creates the FacilityReview it backs (UC-14).
 * =====================================================================
 */
public class TenancyClaim {

    private int claimID;
    private final Tenant tenant;
    private final PropertyListing listing;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String proofDocument;
    private ClaimStatus status;
    private boolean flagged;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private FacilityReview review;

    public TenancyClaim(Tenant t, PropertyListing l, LocalDate start, LocalDate end, String proofPath) {
        this(0, t, l, start, end, proofPath, ClaimStatus.PENDING, false, null, LocalDateTime.now());
    }

    public TenancyClaim(int id, Tenant t, PropertyListing l, LocalDate start, LocalDate end, String proofPath,
                        ClaimStatus status, boolean flagged, String rejectionReason, LocalDateTime createdAt) {
        this.claimID = id;
        this.tenant = t;
        this.listing = l;
        this.startDate = start;
        this.endDate = end;
        this.proofDocument = proofPath;
        this.status = status;
        this.flagged = flagged;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    public boolean isVerified() { return status == ClaimStatus.VERIFIED; }

    /** Extension 5a: two claims on the same property with intersecting dates. */
    public boolean overlapsWith(TenancyClaim other) {
        if (other == null || other == this || other.status == ClaimStatus.REJECTED) return false;
        if (other.listing.getListingID() != this.listing.getListingID()) return false;
        return !endDate.isBefore(other.startDate) && !startDate.isAfter(other.endDate);
    }

    public void flagForReview() { this.flagged = true; }
    public boolean isFlagged() { return flagged; }

    public void verify() { this.status = ClaimStatus.VERIFIED; this.rejectionReason = null; }
    public void reject(String reason) { this.status = ClaimStatus.REJECTED; this.rejectionReason = reason; }

    public boolean hasReview() { return review != null; }
    public FacilityReview getReview() { return review; }
    public void attachReview(FacilityReview r) { this.review = r; }

    // [Creator] UC-14: one review per verified tenancy
    public FacilityReview createReview(String comments) {
        if (!isVerified()) throw new BusinessException("You must have a verified tenancy to leave a review.");
        if (hasReview()) throw new BusinessException("You have already submitted a review for this tenancy. Reviews cannot be changed once submitted.");
        this.review = new FacilityReview(this, comments);
        return review;
    }

    public void assignId(int id) { if (claimID == 0) claimID = id; }

    public int getClaimID() { return claimID; }
    public Tenant getTenant() { return tenant; }
    public PropertyListing getListing() { return listing; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getProofDocument() { return proofDocument; }
    public ClaimStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

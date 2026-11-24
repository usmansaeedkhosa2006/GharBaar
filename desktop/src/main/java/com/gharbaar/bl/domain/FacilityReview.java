package com.gharbaar.bl.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 * [BL - DOMAIN] FacilityReview  (UC-14 Submit Facility Review)
 * The tenant confirms / denies each advertised facility + comments.
 * Shown in UC-11 View Tenant History. Immutable once saved.
 * GRASP Creator: creates its FacilityConfirmation objects.
 * =====================================================================
 */
public class FacilityReview {

    private int reviewID;
    private final String comments;
    private final LocalDate submittedDate;
    private final TenancyClaim claim;
    private final List<FacilityConfirmation> confirmations = new ArrayList<>();

    // display info filled by the repository for tenant history
    private String tenantName = "";
    private LocalDate tenancyStart;
    private LocalDate tenancyEnd;

    public FacilityReview(TenancyClaim c, String comments) {
        this(0, c, comments, LocalDate.now());
    }

    public FacilityReview(int id, TenancyClaim c, String comments, LocalDate submittedDate) {
        this.reviewID = id;
        this.claim = c;
        this.comments = comments == null ? "" : comments.trim();
        this.submittedDate = submittedDate;
    }

    // [Creator]
    public FacilityConfirmation addConfirmation(Facility f, boolean confirmed) {
        FacilityConfirmation fc = new FacilityConfirmation(f, confirmed);
        confirmations.add(fc);
        return fc;
    }

    public List<FacilityConfirmation> getConfirmations() { return List.copyOf(confirmations); }

    /** e.g. "5 of 6 facilities confirmed" */
    public long confirmedCount() { return confirmations.stream().filter(FacilityConfirmation::isConfirmed).count(); }

    public void setDisplayInfo(String tenantName, LocalDate start, LocalDate end) {
        this.tenantName = tenantName;
        this.tenancyStart = start;
        this.tenancyEnd = end;
    }

    public void assignId(int id) { if (reviewID == 0) reviewID = id; }

    public int getReviewID() { return reviewID; }
    public String getComments() { return comments; }
    public LocalDate getSubmittedDate() { return submittedDate; }
    public TenancyClaim getClaim() { return claim; }
    public String getTenantName() { return tenantName; }
    public LocalDate getTenancyStart() { return tenancyStart; }
    public LocalDate getTenancyEnd() { return tenancyEnd; }
}

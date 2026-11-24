package com.gharbaar.bl.domain;

import com.gharbaar.bl.BusinessException;

import java.time.LocalDate;

/**
 * =====================================================================
 * [BL - DOMAIN] Tenant  extends Customer
 * A Customer who has submitted a tenancy claim. The Admin verifies him
 * (UC-07). A verified tenant can submit facility reviews (UC-14).
 * GRASP: Expert of his own verification state; Creator of TenancyClaim.
 * =====================================================================
 */
public class Tenant extends Customer {

    private VerificationStatus verificationStatus;
    private String rejectionReason;
    private String cnic;

    public Tenant(int userID, String username, String passwordHash, String name, String phone,
                  String cnic, VerificationStatus status, String rejectionReason) {
        super(userID, username, passwordHash, name, phone);
        this.cnic = cnic;
        this.verificationStatus = status == null ? VerificationStatus.PENDING : status;
        this.rejectionReason = rejectionReason;
    }

    // ---- UC-07 Verify Tenant ----
    public void verify() {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) throw new BusinessException("A rejection reason is required.");
        this.verificationStatus = VerificationStatus.REJECTED;
        this.rejectionReason = reason.trim();
    }

    public boolean isVerified() { return verificationStatus == VerificationStatus.VERIFIED; }

    /** A new claim puts the tenant back in the Admin's verification queue. */
    public void markPending() { this.verificationStatus = VerificationStatus.PENDING; }

    // [Creator] UC-13 Submit Tenancy Claim
    public TenancyClaim submitClaim(PropertyListing l, LocalDate start, LocalDate end, String proofPath) {
        markPending();
        return new TenancyClaim(this, l, start, end, proofPath);
    }

    public static boolean isValidCnic(String cnic) {
        return cnic != null && cnic.matches("\\d{5}-\\d{7}-\\d");
    }

    public void setCnic(String cnic) { this.cnic = cnic; }
    public String getCnic() { return cnic; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getRejectionReason() { return rejectionReason; }

    @Override public String getRole() { return "TENANT"; }
    @Override public String getHomeView() { return "CustomerHome"; }
}

package com.gharbaar.bl.controller;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.bl.domain.Customer;
import com.gharbaar.bl.domain.Facility;
import com.gharbaar.bl.domain.FacilityReview;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.TenancyClaim;
import com.gharbaar.bl.domain.Tenant;
import com.gharbaar.bl.domain.VerificationStatus;
import com.gharbaar.bl.service.FileStorage;
import com.gharbaar.bl.service.Notifier;
import com.gharbaar.bl.service.Session;
import com.gharbaar.db.ListingRepository;
import com.gharbaar.db.TenancyRepository;
import com.gharbaar.db.UserRepository;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * =====================================================================
 * [BL - CONTROLLER] TenancyController   (GRASP Controller)
 * UC-13 Submit Tenancy Claim, UC-14 Submit Facility Review
 * =====================================================================
 */
public class TenancyController {

    private final TenancyRepository tenancyRepo;
    private final ListingRepository listingRepo;
    private final UserRepository userRepo;
    private final FileStorage storage;
    private final Notifier notifier;

    public TenancyController(TenancyRepository tenancyRepo, ListingRepository listingRepo, UserRepository userRepo,
                             FileStorage storage, Notifier notifier) {
        this.tenancyRepo = tenancyRepo;
        this.listingRepo = listingRepo;
        this.userRepo = userRepo;
        this.storage = storage;
        this.notifier = notifier;
    }

    // ---------------------------------------------------------------
    // UC-13 Submit Tenancy Claim
    // ---------------------------------------------------------------
    public TenancyClaim submitClaim(int listingId, LocalDate start, LocalDate end, File proof, String cnic) {
        Customer customer = Session.getInstance().require(Customer.class);
        List<String> errors = new ArrayList<>();
        if (!checkRequiredFields(start, end, proof)) errors.add("Start date, end date and proof document are required.");   // ext. 4a
        if (start != null && end != null && end.isBefore(start)) errors.add("End date cannot be before start date.");
        if (start != null && start.isAfter(LocalDate.now())) errors.add("Tenancy start date cannot be in the future.");
        if (proof != null && !storage.isSupportedDocument(proof))                                                            // ext. 3a
            errors.add("Unsupported file. Accepted formats: PDF, JPG, JPEG, PNG (max 4 MB).");
        if (!Tenant.isValidCnic(cnic)) errors.add("Enter your CNIC in the format 12345-1234567-1.");
        if (!errors.isEmpty()) throw new BusinessException(errors);

        PropertyListing l = listingRepo.findById(listingId);
        if (l == null) throw new BusinessException("This property no longer exists.");

        Tenant tenant = asTenant(customer, cnic);
        String proofPath = storage.store(proof);                                       // admin-only storage
        TenancyClaim claim = tenant.submitClaim(l, start, end, proofPath);             // Creator: Tenant

        for (TenancyClaim other : tenancyRepo.findClaimsByListing(listingId))          // ext. 5a
            if (claim.overlapsWith(other)) { claim.flagForReview(); break; }

        userRepo.update(tenant);
        tenancyRepo.saveClaim(claim);                                                  // step 5
        notifier.notifyAdmins("New tenancy claim by " + tenant.getName() + " for \"" + l.getTitle() + "\""
                + (claim.isFlagged() ? " (FLAGGED: overlapping dates)" : "") + ".");
        return claim;
    }

    private boolean checkRequiredFields(LocalDate start, LocalDate end, File proof) {
        return start != null && end != null && proof != null;
    }

    /** A Customer becomes a Tenant the first time he submits a claim. */
    private Tenant asTenant(Customer c, String cnic) {
        Tenant t = c instanceof Tenant existing ? existing
                : new Tenant(c.getUserID(), c.getUsername(), c.getPasswordHash(), c.getName(), c.getPhone(),
                             cnic, VerificationStatus.PENDING, null);
        t.setCnic(cnic);
        Session.getInstance().setCurrentUser(t);
        return t;
    }

    public List<TenancyClaim> getMyClaims() {
        Customer c = Session.getInstance().require(Customer.class);
        return tenancyRepo.findClaimsByTenant(c.getUserID());
    }

    // ---------------------------------------------------------------
    // UC-14 Submit Facility Review
    // ---------------------------------------------------------------
    /** Step 1-2: checks eligibility and returns the advertised facilities. */
    public List<Facility> startFacilityReview(int listingId) {
        TenancyClaim claim = requireReviewableClaim(listingId);
        return claim.getListing().getAdvertisedFacilities();
    }

    public FacilityReview submitReview(int listingId, Map<Facility, Boolean> confirmations, String comments) {
        TenancyClaim claim = requireReviewableClaim(listingId);
        if (confirmations == null || confirmations.isEmpty())
            throw new BusinessException("Please confirm or deny each facility.");
        if (comments != null && comments.length() > 1000) throw new BusinessException("Comments must be under 1000 characters.");
        FacilityReview review = claim.createReview(comments);                          // Creator: TenancyClaim
        confirmations.forEach(review::addConfirmation);                                // step 3
        tenancyRepo.saveReview(review);                                                // step 5
        notifier.notify(claim.getListing().getOwner(), "A verified tenant reviewed the facilities of \""
                + claim.getListing().getTitle() + "\".");
        return review;
    }

    private TenancyClaim requireReviewableClaim(int listingId) {
        Customer c = Session.getInstance().require(Customer.class);
        TenancyClaim claim = tenancyRepo.findVerifiedClaim(c.getUserID(), listingId);
        if (claim == null)                                                             // ext. 1a
            throw new BusinessException("You must have a verified tenancy to leave a review.");
        if (claim.hasReview())                                                         // ext. 1b
            throw new BusinessException("You have already submitted a review for this tenancy. Reviews cannot be changed once submitted.");
        return claim;
    }

    /** Used by the UI to decide which button to show on the details page. */
    public boolean canReview(int listingId) {
        try { requireReviewableClaim(listingId); return true; } catch (BusinessException e) { return false; }
    }
}

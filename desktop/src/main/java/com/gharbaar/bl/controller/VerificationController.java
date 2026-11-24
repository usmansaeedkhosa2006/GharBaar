package com.gharbaar.bl.controller;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.bl.domain.Admin;
import com.gharbaar.bl.domain.ClaimStatus;
import com.gharbaar.bl.domain.ListingStatus;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.TenancyClaim;
import com.gharbaar.bl.domain.Tenant;
import com.gharbaar.bl.service.FileStorage;
import com.gharbaar.bl.service.Notifier;
import com.gharbaar.bl.service.Session;
import com.gharbaar.db.ListingRepository;
import com.gharbaar.db.TenancyRepository;
import com.gharbaar.db.UserRepository;

import java.util.List;

/**
 * =====================================================================
 * [BL - CONTROLLER] VerificationController   (GRASP Controller)
 * UC-06 Verify Listing, UC-07 Verify Tenant   (Admin only)
 * =====================================================================
 */
public class VerificationController {

    private final ListingRepository listingRepo;
    private final UserRepository userRepo;
    private final TenancyRepository tenancyRepo;
    private final FileStorage storage;
    private final Notifier notifier;

    public VerificationController(ListingRepository listingRepo, UserRepository userRepo, TenancyRepository tenancyRepo,
                                  FileStorage storage, Notifier notifier) {
        this.listingRepo = listingRepo;
        this.userRepo = userRepo;
        this.tenancyRepo = tenancyRepo;
        this.storage = storage;
        this.notifier = notifier;
    }

    private void requireAdmin() { Session.getInstance().require(Admin.class); }

    // ---------------------------------------------------------------
    // UC-06 Verify Listing
    // ---------------------------------------------------------------
    public List<PropertyListing> getPendingListings() {
        requireAdmin();
        return listingRepo.findByStatus(ListingStatus.PENDING_VERIFICATION);           // step 2
    }

    public PropertyListing getListingDetails(int listingId) {
        requireAdmin();
        PropertyListing l = listingRepo.findById(listingId);
        if (l == null) throw new BusinessException("Listing not found.");
        return l;                                                                       // step 4
    }

    public void approveListing(int listingId) {
        PropertyListing l = getListingDetails(listingId);
        l.approve();                                                                    // Expert
        listingRepo.update(l);                                                          // step 6
        notifier.notify(l.getOwner(), "Your listing \"" + l.getTitle() + "\" has been VERIFIED and is now public.");
    }

    public void rejectListing(int listingId, String reason) {
        PropertyListing l = getListingDetails(listingId);
        l.reject(reason);                                                               // ext. 5a
        listingRepo.update(l);
        notifier.notify(l.getOwner(), "Your listing \"" + l.getTitle() + "\" was REJECTED. Reason: " + l.getRejectionReason()); // 5b
    }

    // ---------------------------------------------------------------
    // UC-07 Verify Tenant
    // ---------------------------------------------------------------
    public List<Tenant> getUnverifiedTenants() {
        requireAdmin();
        return userRepo.findUnverifiedTenants();                                        // step 2
    }

    public Tenant getTenant(int tenantId) {
        requireAdmin();
        Tenant t = userRepo.findTenantById(tenantId);
        if (t == null) throw new BusinessException("Tenant not found.");
        return t;
    }

    /** The claims (with proof + flag) the admin reviews for this tenant (step 4). */
    public List<TenancyClaim> getTenantClaims(int tenantId) {
        requireAdmin();
        return tenancyRepo.findClaimsByTenant(tenantId);
    }

    public void approveTenant(int tenantId) {
        Tenant t = getTenant(tenantId);
        t.verify();
        userRepo.update(t);                                                             // step 6
        for (TenancyClaim c : tenancyRepo.findClaimsByTenant(tenantId))
            if (c.getStatus() == ClaimStatus.PENDING) { c.verify(); tenancyRepo.updateClaim(c); }
        notifier.notify(t, "Your tenancy has been VERIFIED. You can now submit a facility review for the property.");
    }

    public void rejectTenant(int tenantId, String reason) {
        Tenant t = getTenant(tenantId);
        t.reject(reason);                                                               // ext. 5a
        userRepo.update(t);
        for (TenancyClaim c : tenancyRepo.findClaimsByTenant(tenantId))
            if (c.getStatus() == ClaimStatus.PENDING) { c.reject(t.getRejectionReason()); tenancyRepo.updateClaim(c); }
        notifier.notify(t, "Your tenancy claim was REJECTED. Reason: " + t.getRejectionReason());   // ext. 5b
    }

    /** Proof documents are only accessible to the Admin (UC-13 special requirement). */
    public FileStorage.StoredFile getProofDocument(String path) {
        requireAdmin();
        FileStorage.StoredFile f = storage.load(path);
        if (f == null) throw new BusinessException("Proof document not found.");
        return f;
    }
}

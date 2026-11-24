package com.gharbaar.bl.controller;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.bl.domain.Admin;
import com.gharbaar.bl.domain.ListingStatus;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.ReportStatus;
import com.gharbaar.bl.domain.User;
import com.gharbaar.bl.service.Notifier;
import com.gharbaar.bl.service.Session;
import com.gharbaar.db.ListingRepository;
import com.gharbaar.db.ReportRepository;
import com.gharbaar.db.TenancyRepository;
import com.gharbaar.db.UserRepository;

import java.util.List;

/**
 * =====================================================================
 * [BL - CONTROLLER] AdminController   (GRASP Controller)
 * Admin panel: dashboard counts, manage users (suspend fraudulent
 * accounts) and remove any listing  -  "taking administrative actions and
 * tackling fraudulent use of the service" (Project Scope, Deliverable 2).
 * =====================================================================
 */
public class AdminController {

    /** numbers shown on the admin dashboard */
    public record DashboardStats(int customers, int agents, int verifiedListings, int pendingListings,
                                 int rejectedListings, int openReports, int pendingClaims, int pendingTenants) { }

    private final UserRepository userRepo;
    private final ListingRepository listingRepo;
    private final ReportRepository reportRepo;
    private final TenancyRepository tenancyRepo;
    private final Notifier notifier;

    public AdminController(UserRepository userRepo, ListingRepository listingRepo, ReportRepository reportRepo,
                           TenancyRepository tenancyRepo, Notifier notifier) {
        this.userRepo = userRepo;
        this.listingRepo = listingRepo;
        this.reportRepo = reportRepo;
        this.tenancyRepo = tenancyRepo;
        this.notifier = notifier;
    }

    private Admin requireAdmin() { return Session.getInstance().require(Admin.class); }

    public DashboardStats getDashboardStats() {
        requireAdmin();
        return new DashboardStats(userRepo.countByRole("CUSTOMER"), userRepo.countByRole("AGENT"),
                listingRepo.countByStatus(ListingStatus.VERIFIED), listingRepo.countByStatus(ListingStatus.PENDING_VERIFICATION),
                listingRepo.countByStatus(ListingStatus.REJECTED), reportRepo.countByStatus(ReportStatus.OPEN),
                tenancyRepo.countPendingClaims(), userRepo.findUnverifiedTenants().size());
    }

    // ---- manage users ----
    public List<User> getAllUsers() {
        requireAdmin();
        return userRepo.findAll();
    }

    public void setUserActive(int userId, boolean active) {
        Admin me = requireAdmin();
        if (me.getUserID() == userId) throw new BusinessException("You cannot suspend your own account.");
        User u = userRepo.findById(userId);
        if (u == null) throw new BusinessException("User not found.");
        if (u instanceof Admin) throw new BusinessException("Admin accounts cannot be suspended.");
        u.setActive(active);
        userRepo.update(u);
        notifier.notify(u, active ? "Your account has been re-activated by the Admin."
                                  : "Your account has been suspended by the Admin.");
    }

    // ---- manage all listings ----
    public List<PropertyListing> getAllListings() {
        requireAdmin();
        return listingRepo.findAll();
    }

    public void removeListing(int listingId, String reason) {
        requireAdmin();
        PropertyListing l = listingRepo.findById(listingId);
        if (l == null) throw new BusinessException("Listing not found.");
        if (reason == null || reason.trim().length() < 5) throw new BusinessException("Please enter a reason (at least 5 characters).");
        listingRepo.delete(l, "[Removed by Admin] " + reason.trim());
        notifier.notify(l.getOwner(), "Your listing \"" + l.getTitle() + "\" was removed by the Admin. Reason: " + reason.trim());
    }
}

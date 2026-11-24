package com.gharbaar.bl;

import com.gharbaar.bl.controller.AdminController;
import com.gharbaar.bl.controller.AuthController;
import com.gharbaar.bl.controller.ListingController;
import com.gharbaar.bl.controller.NotificationController;
import com.gharbaar.bl.controller.PropertySearchController;
import com.gharbaar.bl.controller.ReportController;
import com.gharbaar.bl.controller.TenancyController;
import com.gharbaar.bl.controller.VerificationController;
import com.gharbaar.bl.controller.VisitController;
import com.gharbaar.bl.service.DatabaseFileStorage;
import com.gharbaar.bl.service.FileStorage;
import com.gharbaar.bl.service.InAppNotifier;
import com.gharbaar.bl.service.Notifier;
import com.gharbaar.bl.service.PasswordHasher;
import com.gharbaar.db.FileRepository;
import com.gharbaar.db.ListingRepository;
import com.gharbaar.db.NotificationRepository;
import com.gharbaar.db.ReportRepository;
import com.gharbaar.db.TenancyRepository;
import com.gharbaar.db.UserRepository;
import com.gharbaar.db.VisitRequestRepository;

/**
 * =====================================================================
 * [BL] AppContext  - composition root (wires all objects once)
 * GRASP Low Coupling: UI screens only ask AppContext for a controller;
 * they never create repositories or services themselves.
 * To switch storage to local disk, replace DatabaseFileStorage with
 * new LocalFileStorage(...) here  (Protected Variations).
 * =====================================================================
 */
public final class AppContext {

    private static AppContext instance;

    // ---- DB layer ----
    private final UserRepository userRepo = new UserRepository();
    private final ListingRepository listingRepo = new ListingRepository();
    private final ReportRepository reportRepo = new ReportRepository();
    private final VisitRequestRepository visitRepo = new VisitRequestRepository(listingRepo);
    private final TenancyRepository tenancyRepo = new TenancyRepository(listingRepo);
    private final NotificationRepository notificationRepo = new NotificationRepository();
    private final FileRepository fileRepo = new FileRepository();

    // ---- services (behind interfaces) ----
    private final PasswordHasher hasher = new PasswordHasher();
    private final Notifier notifier = new InAppNotifier(notificationRepo);
    private final FileStorage storage = new DatabaseFileStorage(fileRepo);

    // ---- controllers ----
    public final AuthController auth = new AuthController(userRepo, hasher);
    public final ListingController listings = new ListingController(listingRepo, storage, notifier);
    public final PropertySearchController search = new PropertySearchController(listingRepo, tenancyRepo);
    public final VisitController visits = new VisitController(listingRepo, visitRepo, notifier);
    public final TenancyController tenancy = new TenancyController(tenancyRepo, listingRepo, userRepo, storage, notifier);
    public final ReportController reports = new ReportController(reportRepo, notifier);
    public final VerificationController verification = new VerificationController(listingRepo, userRepo, tenancyRepo, storage, notifier);
    public final AdminController admin = new AdminController(userRepo, listingRepo, reportRepo, tenancyRepo, notifier);
    public final NotificationController notifications = new NotificationController(notificationRepo);

    private AppContext() { }

    public static synchronized AppContext get() {
        if (instance == null) instance = new AppContext();
        return instance;
    }
}

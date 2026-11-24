package com.gharbaar.bl.controller;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.bl.domain.Admin;
import com.gharbaar.bl.domain.Report;
import com.gharbaar.bl.domain.ReportStatus;
import com.gharbaar.bl.domain.User;
import com.gharbaar.bl.service.Notifier;
import com.gharbaar.bl.service.Session;
import com.gharbaar.db.ReportRepository;

import java.util.List;

/**
 * =====================================================================
 * [BL - CONTROLLER] ReportController   (GRASP Controller)
 * UC-05 Report Issue (Customer/Agent), UC-08 Resolve Report (Admin)
 * =====================================================================
 */
public class ReportController {

    private final ReportRepository reportRepo;
    private final Notifier notifier;

    public ReportController(ReportRepository reportRepo, Notifier notifier) {
        this.reportRepo = reportRepo;
        this.notifier = notifier;
    }

    // ---------------------------------------------------------------
    // UC-05 Report Issue
    // ---------------------------------------------------------------
    public Report submitReport(String type, String description) {
        User u = Session.getInstance().require(User.class);
        Report r = u.fileReport(type, description);                                   // Creator: User (validates)
        reportRepo.save(r);                                                            // step 4 (status OPEN)
        notifier.notifyAdmins("New " + type + " report from " + u.getName() + ".");    // step 5
        return r;
    }

    public List<Report> getMyReports() {
        User u = Session.getInstance().require(User.class);
        return reportRepo.findByReporter(u.getUserID());
    }

    // ---------------------------------------------------------------
    // UC-08 Resolve Report
    // ---------------------------------------------------------------
    public List<Report> getOpenReports() {
        Session.getInstance().require(Admin.class);
        return reportRepo.findByStatus(ReportStatus.OPEN);
    }

    public List<Report> getResolvedReports() {
        Session.getInstance().require(Admin.class);
        return reportRepo.findByStatus(ReportStatus.RESOLVED);
    }

    public Report getReport(int reportId) {
        Session.getInstance().require(Admin.class);
        Report r = reportRepo.findById(reportId);
        if (r == null) throw new BusinessException("Report not found.");
        return r;
    }

    public void resolveReport(int reportId, String remarks) {
        Report r = getReport(reportId);
        r.resolve(remarks);                                                            // Expert (ext. 5a)
        reportRepo.update(r);                                                          // step 6
        notifier.notify(r.getReporter(), "Your report \"" + r.getType() + "\" was resolved: " + r.getResolutionRemarks());
    }
}

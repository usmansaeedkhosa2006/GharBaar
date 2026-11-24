package com.gharbaar.bl.domain;

import com.gharbaar.bl.BusinessException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * =====================================================================
 * [BL - DOMAIN] Report  (UC-05 Report Issue, UC-08 Resolve Report)
 * GRASP Expert: validates itself and resolves itself.
 * =====================================================================
 */
public class Report {

    public static final List<String> TYPES = List.of(
            "Fake Listing", "Fraud / Scam", "Wrong Information", "Agent Behaviour",
            "Technical Problem", "Other");

    private int reportID;
    private final User reporter;
    private final String type;
    private final String description;
    private ReportStatus status;
    private String resolutionRemarks;
    private final LocalDateTime dateTime;

    // [Constructor] created by User.fileReport(...)
    public Report(User reporter, String type, String description) {
        if (type == null || !TYPES.contains(type)) throw new BusinessException("Please select an issue type.");
        if (description == null || description.isBlank())                     // extension 4a
            throw new BusinessException("Please describe the issue.");
        if (description.trim().length() < 10) throw new BusinessException("Description must be at least 10 characters.");
        this.reporter = reporter;
        this.type = type;
        this.description = description.trim();
        this.status = ReportStatus.OPEN;                                       // step 4
        this.dateTime = LocalDateTime.now();
    }

    // [Constructor] rebuilt from database
    public Report(int id, User reporter, String type, String description, ReportStatus status,
                  String remarks, LocalDateTime dateTime) {
        this.reportID = id;
        this.reporter = reporter;
        this.type = type;
        this.description = description;
        this.status = status;
        this.resolutionRemarks = remarks;
        this.dateTime = dateTime;
    }

    // UC-08 step 5-6
    public void resolve(String remarks) {
        if (!isOpen()) throw new BusinessException("This report is already resolved.");
        if (remarks == null || remarks.isBlank())                              // extension 5a
            throw new BusinessException("Please enter the resolution remarks.");
        this.resolutionRemarks = remarks.trim();
        this.status = ReportStatus.RESOLVED;
    }

    public boolean isOpen() { return status == ReportStatus.OPEN; }

    public void assignId(int id) { if (reportID == 0) reportID = id; }

    public int getReportID() { return reportID; }
    public User getReporter() { return reporter; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public ReportStatus getStatus() { return status; }
    public String getResolutionRemarks() { return resolutionRemarks; }
    public LocalDateTime getDateTime() { return dateTime; }
}

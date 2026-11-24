package com.gharbaar.db;

import com.gharbaar.bl.domain.Report;
import com.gharbaar.bl.domain.ReportStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * [DB LAYER] ReportRepository  «pure fabrication»   -> table: reports
 */
public class ReportRepository extends BaseRepository {

    private static final String SELECT = "SELECT r.*, " + UserRepository.COLS +
            " FROM reports r JOIN users u ON u.user_id = r.reporter_id ";

    // UC-05
    public void save(Report r) {
        r.assignId(insert("INSERT INTO reports (reporter_id, type, description, status) VALUES (?,?,?,?)",
                r.getReporter().getUserID(), r.getType(), r.getDescription(), r.getStatus().name()));
    }

    // UC-08
    public void update(Report r) {
        execute("UPDATE reports SET status=?, resolution_remarks=?, resolved_at=CASE WHEN ?='RESOLVED' THEN NOW() ELSE NULL END WHERE report_id=?",
                r.getStatus().name(), r.getResolutionRemarks(), r.getStatus().name(), r.getReportID());
    }

    public Report findById(int reportId) {
        return queryOne(SELECT + "WHERE r.report_id=?", this::map, reportId);
    }

    public List<Report> findByStatus(ReportStatus s) {
        return query(SELECT + "WHERE r.status=? ORDER BY r.date_time DESC", this::map, s.name());
    }

    public List<Report> findByReporter(int userId) {
        return query(SELECT + "WHERE r.reporter_id=? ORDER BY r.date_time DESC", this::map, userId);
    }

    public int countByStatus(ReportStatus s) {
        return count("SELECT COUNT(*) FROM reports WHERE status=?", s.name());
    }

    private Report map(ResultSet rs) throws SQLException {
        return new Report(rs.getInt("report_id"), UserRepository.mapUser(rs), rs.getString("type"),
                rs.getString("description"), ReportStatus.valueOf(rs.getString("status")),
                rs.getString("resolution_remarks"), ts(rs, "date_time"));
    }
}

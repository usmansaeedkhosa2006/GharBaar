package com.gharbaar.db;

import com.gharbaar.bl.domain.Customer;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.VisitRequest;
import com.gharbaar.bl.domain.VisitStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * [DB LAYER] VisitRequestRepository  «pure fabrication»   -> table: visit_requests
 */
public class VisitRequestRepository extends BaseRepository {

    private final ListingRepository listingRepo;

    public VisitRequestRepository(ListingRepository listingRepo) { this.listingRepo = listingRepo; }

    private static final String SELECT = "SELECT v.*, " + UserRepository.COLS +
            " FROM visit_requests v JOIN users u ON u.user_id = v.customer_id ";

    // UC-12
    public void save(VisitRequest v) {
        v.assignId(insert("INSERT INTO visit_requests (customer_id, listing_id, proposed_datetime, notes, status) VALUES (?,?,?,?,?)",
                v.getCustomer().getUserID(), v.getListing().getListingID(), v.getProposedDateTime(), v.getNotes(),
                v.getStatus().name()));
    }

    public void update(VisitRequest v) {
        execute("UPDATE visit_requests SET status=? WHERE request_id=?", v.getStatus().name(), v.getRequestID());
    }

    public VisitRequest findById(int id) {
        return queryOne(SELECT + "WHERE v.request_id=?", this::map, id);
    }

    /** visit requests for all listings of one agent */
    public List<VisitRequest> findByAgent(int agentId) {
        return query(SELECT + "JOIN property_listings l ON l.listing_id = v.listing_id WHERE l.agent_id=? " +
                "ORDER BY v.status='PENDING' DESC, v.proposed_datetime", this::map, agentId);
    }

    public List<VisitRequest> findByCustomer(int customerId) {
        return query(SELECT + "WHERE v.customer_id=? ORDER BY v.proposed_datetime DESC", this::map, customerId);
    }

    private VisitRequest map(ResultSet rs) throws SQLException {
        PropertyListing l = listingRepo.findById(rs.getInt("listing_id"));
        return new VisitRequest(rs.getInt("request_id"), (Customer) UserRepository.mapUser(rs), l,
                ts(rs, "proposed_datetime"), rs.getString("notes"), VisitStatus.valueOf(rs.getString("status")),
                ts(rs, "created_at"));
    }
}

package com.gharbaar.db;

import com.gharbaar.bl.domain.ClaimStatus;
import com.gharbaar.bl.domain.Facility;
import com.gharbaar.bl.domain.FacilityConfirmation;
import com.gharbaar.bl.domain.FacilityReview;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.TenancyClaim;
import com.gharbaar.bl.domain.Tenant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =====================================================================
 * [DB LAYER] TenancyRepository  «pure fabrication»
 * Tables: tenancy_claims, facility_reviews, facility_confirmations
 * =====================================================================
 */
public class TenancyRepository extends BaseRepository {

    private final ListingRepository listingRepo;

    public TenancyRepository(ListingRepository listingRepo) { this.listingRepo = listingRepo; }

    private static final String SELECT = "SELECT c.*, " + UserRepository.COLS +
            " FROM tenancy_claims c JOIN users u ON u.user_id = c.tenant_id ";

    // ---- UC-13 ----
    public void saveClaim(TenancyClaim c) {
        c.assignId(insert("INSERT INTO tenancy_claims (tenant_id, listing_id, start_date, end_date, proof_document, status, flagged) " +
                        "VALUES (?,?,?,?,?,?,?)",
                c.getTenant().getUserID(), c.getListing().getListingID(), c.getStartDate(), c.getEndDate(),
                c.getProofDocument(), c.getStatus().name(), c.isFlagged()));
    }

    public void updateClaim(TenancyClaim c) {
        execute("UPDATE tenancy_claims SET status=?, flagged=?, rejection_reason=? WHERE claim_id=?",
                c.getStatus().name(), c.isFlagged(), c.getRejectionReason(), c.getClaimID());
    }

    public List<TenancyClaim> findClaimsByListing(int listingId) {
        return query(SELECT + "WHERE c.listing_id=? ORDER BY c.start_date", this::map, listingId);
    }

    public List<TenancyClaim> findClaimsByTenant(int tenantId) {
        return withReviews(query(SELECT + "WHERE c.tenant_id=? ORDER BY c.created_at DESC", this::map, tenantId));
    }

    /** UC-14 precondition: a VERIFIED claim of this tenant on this listing */
    public TenancyClaim findVerifiedClaim(int tenantId, int listingId) {
        List<TenancyClaim> list = withReviews(query(SELECT + "WHERE c.tenant_id=? AND c.listing_id=? AND c.status='VERIFIED' " +
                "ORDER BY c.end_date DESC", this::map, tenantId, listingId));
        // prefer a verified claim that has not been reviewed yet
        return list.stream().filter(x -> !x.hasReview()).findFirst().orElse(list.isEmpty() ? null : list.get(0));
    }

    public int countPendingClaims() {
        return count("SELECT COUNT(*) FROM tenancy_claims WHERE status='PENDING'");
    }

    // ---- UC-14: save review + confirmations atomically ----
    public void saveReview(FacilityReview r) {
        inTransaction(c -> {
            int id = insert(c, "INSERT INTO facility_reviews (claim_id, comments, submitted_date) VALUES (?,?,?)",
                    r.getClaim().getClaimID(), r.getComments(), r.getSubmittedDate());
            r.assignId(id);
            for (FacilityConfirmation fc : r.getConfirmations())
                insert(c, "INSERT INTO facility_confirmations (review_id, facility_id, confirmed) VALUES (?,?,?)",
                        id, fc.getFacility().getFacilityID(), fc.isConfirmed());
            return id;
        });
    }

    // ---- UC-11: all reviews of verified tenancies of a listing ----
    public List<FacilityReview> findReviewsByListing(int listingId) {
        Map<Integer, FacilityReview> byId = new HashMap<>();
        List<FacilityReview> reviews = query(
                "SELECT r.review_id, r.comments, r.submitted_date, c.start_date, c.end_date, u.name " +
                "FROM facility_reviews r JOIN tenancy_claims c ON c.claim_id = r.claim_id " +
                "JOIN users u ON u.user_id = c.tenant_id " +
                "WHERE c.listing_id=? AND c.status='VERIFIED' ORDER BY r.submitted_date DESC",
                rs -> {
                    FacilityReview fr = new FacilityReview(rs.getInt("review_id"), null, rs.getString("comments"),
                            rs.getDate("submitted_date").toLocalDate());
                    fr.setDisplayInfo(rs.getString("name"), rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate());
                    byId.put(fr.getReviewID(), fr);
                    return fr;
                }, listingId);
        if (reviews.isEmpty()) return reviews;
        query("SELECT fc.review_id, fc.confirmed, f.facility_id, f.name, f.is_active FROM facility_confirmations fc " +
              "JOIN facilities f ON f.facility_id = fc.facility_id WHERE f.listing_id=? ORDER BY f.facility_id",
                rs -> {
                    FacilityReview fr = byId.get(rs.getInt("review_id"));
                    if (fr != null) fr.addConfirmation(new Facility(rs.getInt("facility_id"), rs.getString("name"),
                            rs.getBoolean("is_active")), rs.getBoolean("confirmed"));
                    return null;
                }, listingId);
        return reviews;
    }

    // ---- mapping ----
    private TenancyClaim map(ResultSet rs) throws SQLException {
        PropertyListing l = listingRepo.findById(rs.getInt("listing_id"));
        return new TenancyClaim(rs.getInt("claim_id"), (Tenant) UserRepository.mapUser(rs), l,
                rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate(),
                rs.getString("proof_document"), ClaimStatus.valueOf(rs.getString("status")),
                rs.getBoolean("flagged"), rs.getString("rejection_reason"), ts(rs, "created_at"));
    }

    /** attaches existing reviews so hasReview() works (UC-14 ext. 1b) */
    private List<TenancyClaim> withReviews(List<TenancyClaim> claims) {
        for (TenancyClaim c : claims) {
            FacilityReview r = queryOne("SELECT review_id, comments, submitted_date FROM facility_reviews WHERE claim_id=?",
                    rs -> new FacilityReview(rs.getInt("review_id"), c, rs.getString("comments"),
                            rs.getDate("submitted_date").toLocalDate()), c.getClaimID());
            if (r != null) c.attachReview(r);
        }
        return claims;
    }
}

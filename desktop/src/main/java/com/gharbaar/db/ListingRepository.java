package com.gharbaar.db;

import com.gharbaar.bl.domain.Agent;
import com.gharbaar.bl.domain.Facility;
import com.gharbaar.bl.domain.ListingDetails;
import com.gharbaar.bl.domain.ListingStatus;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.PropertyStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =====================================================================
 * [DB LAYER] ListingRepository  «pure fabrication»
 * Tables: property_listings, listing_photos, facilities, listing_deletions
 * =====================================================================
 */
public class ListingRepository extends BaseRepository {

    private static final String SELECT =
            "SELECT l.*, a.name AS agent_name, a.phone AS agent_phone, a.username AS agent_username " +
            "FROM property_listings l JOIN users a ON a.user_id = l.agent_id ";

    // ---------------------------------------------------------------
    // [CREATE] UC-03: listing + photos + facilities in one transaction
    // ---------------------------------------------------------------
    public void save(PropertyListing l, List<String> photoPaths) {
        inTransaction(c -> {
            int id = insert(c, "INSERT INTO property_listings (agent_id,title,address,property_type,cda_sector,rent," +
                            "description,furnishing,occupancy_type,photo_path,listing_status,property_status) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    l.getOwner().getUserID(), l.getTitle(), l.getAddress(), l.getPropertyType(), l.getCdaSector(),
                    l.getRent(), l.getDescription(), l.isFurnishing(), l.getOccupancyType(), l.getPhotoPath(),
                    l.getListingStatus().name(), l.getPropertyStatus().name());
            l.assignId(id);
            savePhotos(c, id, photoPaths);
            for (Facility f : l.getAllFacilities())
                f.assignId(insert(c, "INSERT INTO facilities (listing_id, name, is_active) VALUES (?,?,?)", id, f.getName(), f.isActive()));
            return id;
        });
    }

    // ---------------------------------------------------------------
    // [UPDATE] UC-04 / UC-06: fields, status, facilities (and photos if replaced)
    // ---------------------------------------------------------------
    public void update(PropertyListing l) { update(l, null); }

    public void update(PropertyListing l, List<String> newPhotoPaths) {
        inTransaction(c -> {
            execute(c, "UPDATE property_listings SET title=?,address=?,property_type=?,cda_sector=?,rent=?,description=?," +
                            "furnishing=?,occupancy_type=?,photo_path=?,listing_status=?,property_status=?,rejection_reason=?," +
                            "updated_at=NOW() WHERE listing_id=?",
                    l.getTitle(), l.getAddress(), l.getPropertyType(), l.getCdaSector(), l.getRent(), l.getDescription(),
                    l.isFurnishing(), l.getOccupancyType(), l.getPhotoPath(), l.getListingStatus().name(),
                    l.getPropertyStatus().name(), l.getRejectionReason(), l.getListingID());
            for (Facility f : l.getAllFacilities()) {
                if (f.getFacilityID() == 0)
                    f.assignId(insert(c, "INSERT INTO facilities (listing_id, name, is_active) VALUES (?,?,?)",
                            l.getListingID(), f.getName(), f.isActive()));
                else
                    execute(c, "UPDATE facilities SET is_active=? WHERE facility_id=?", f.isActive(), f.getFacilityID());
            }
            if (newPhotoPaths != null && !newPhotoPaths.isEmpty()) {
                execute(c, "DELETE FROM listing_photos WHERE listing_id=?", l.getListingID());
                savePhotos(c, l.getListingID(), newPhotoPaths);
            }
            return null;
        });
    }

    private void savePhotos(java.sql.Connection c, int listingId, List<String> paths) {
        for (int i = 0; i < paths.size(); i++)
            insert(c, "INSERT INTO listing_photos (listing_id, photo_path, sort_order) VALUES (?,?,?)", listingId, paths.get(i), i);
    }

    // ---------------------------------------------------------------
    // [DELETE] UC-04: log the reason, then remove the property
    // ---------------------------------------------------------------
    public void delete(PropertyListing l, String reason) {
        inTransaction(c -> {
            insert(c, "INSERT INTO listing_deletions (listing_id, agent_id, title, reason) VALUES (?,?,?,?)",
                    l.getListingID(), l.getOwner().getUserID(), l.getTitle(), reason);
            execute(c, "DELETE FROM property_listings WHERE listing_id=?", l.getListingID());
            return null;
        });
    }

    // ---------------------------------------------------------------
    // [READ]
    // ---------------------------------------------------------------
    public PropertyListing findById(int listingId) {
        List<PropertyListing> list = loadChildren(query(SELECT + "WHERE l.listing_id=?", this::map, listingId));
        return list.isEmpty() ? null : list.get(0);
    }

    public List<PropertyListing> findByAgent(int agentId) {
        return loadChildren(query(SELECT + "WHERE l.agent_id=? ORDER BY l.created_at DESC", this::map, agentId));
    }

    public List<PropertyListing> findByStatus(ListingStatus s) {
        return loadChildren(query(SELECT + "WHERE l.listing_status=? ORDER BY l.created_at", this::map, s.name()));
    }

    /** UC-09: only verified listings are ever searched */
    public List<PropertyListing> findVerified() {
        return findByStatus(ListingStatus.VERIFIED);
    }

    public List<PropertyListing> findAll() {
        return loadChildren(query(SELECT + "ORDER BY l.created_at DESC", this::map));
    }

    public int countByStatus(ListingStatus s) {
        return count("SELECT COUNT(*) FROM property_listings WHERE listing_status=?", s.name());
    }

    // ---- row mapping ----
    private PropertyListing map(ResultSet rs) throws SQLException {
        Agent owner = new Agent(rs.getInt("agent_id"), rs.getString("agent_username"), null,
                rs.getString("agent_name"), rs.getString("agent_phone"));
        ListingDetails d = new ListingDetails(rs.getString("title"), rs.getString("address"),
                rs.getString("property_type"), rs.getString("cda_sector"), rs.getDouble("rent"),
                rs.getString("description"), rs.getBoolean("furnishing"), rs.getString("occupancy_type"),
                null, null, PropertyStatus.valueOf(rs.getString("property_status")));
        LocalDateTime created = ts(rs, "created_at");
        return new PropertyListing(rs.getInt("listing_id"), owner, d, rs.getString("photo_path"),
                ListingStatus.valueOf(rs.getString("listing_status")),
                PropertyStatus.valueOf(rs.getString("property_status")), rs.getString("rejection_reason"), created);
    }

    /** Loads facilities and photos for many listings with 2 queries (fast search, UC-09 "< 2 seconds"). */
    private List<PropertyListing> loadChildren(List<PropertyListing> listings) {
        if (listings.isEmpty()) return listings;
        Map<Integer, PropertyListing> byId = new HashMap<>();
        Object[] ids = new Object[listings.size()];
        for (int i = 0; i < listings.size(); i++) {
            byId.put(listings.get(i).getListingID(), listings.get(i));
            ids[i] = listings.get(i).getListingID();
        }
        String in = placeholders(ids.length);
        query("SELECT facility_id, listing_id, name, is_active FROM facilities WHERE listing_id IN (" + in + ") ORDER BY facility_id",
                rs -> { byId.get(rs.getInt("listing_id")).attachFacility(
                        new Facility(rs.getInt("facility_id"), rs.getString("name"), rs.getBoolean("is_active"))); return null; }, ids);
        query("SELECT listing_id, photo_path FROM listing_photos WHERE listing_id IN (" + in + ") ORDER BY sort_order",
                rs -> { byId.get(rs.getInt("listing_id")).attachPhoto(rs.getString("photo_path")); return null; }, ids);
        return listings;
    }
}

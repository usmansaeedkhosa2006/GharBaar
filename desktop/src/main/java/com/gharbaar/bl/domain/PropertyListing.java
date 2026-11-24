package com.gharbaar.bl.domain;

import com.gharbaar.bl.BusinessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * =====================================================================
 * [BL - DOMAIN] PropertyListing
 * Central domain class. GRASP Information Expert for:
 *   - approving / rejecting itself        (UC-06)
 *   - updating itself from ListingDetails (UC-04)
 *   - deciding if it matches a search     (UC-09)
 *   - its advertised facilities + tenant history (UC-10, UC-11, UC-14)
 * GRASP Creator: creates its own Facility objects.
 * =====================================================================
 */
public class PropertyListing {

    // ---- attributes (from DCD) ----
    private int listingID;
    private String title;
    private String address;
    private String propertyType;
    private String cdaSector;
    private double rent;
    private String description;
    private boolean furnishing;
    private String occupancyType;
    private String photoPath;
    private ListingStatus listingStatus;
    private PropertyStatus propertyStatus;
    private String rejectionReason;
    private LocalDateTime createdAt;

    // ---- associations ----
    private final Agent owner;
    private final List<Facility> facilities = new ArrayList<>();
    private final List<String> photos = new ArrayList<>();
    private List<FacilityReview> tenantHistory = new ArrayList<>();

    // ---------------------------------------------------------------
    // [Constructor] new listing created by an Agent (UC-03)
    // ---------------------------------------------------------------
    public PropertyListing(Agent owner, ListingDetails d, String photoPath) {
        this.owner = owner;
        copyDetails(d);
        this.photoPath = photoPath;
        this.listingStatus = ListingStatus.PENDING_VERIFICATION;   // step 5
        this.propertyStatus = PropertyStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
        for (String f : d.getFacilityNames()) addFacility(f);
    }

    // ---------------------------------------------------------------
    // [Constructor] rebuilt from the database by ListingRepository
    // ---------------------------------------------------------------
    public PropertyListing(int id, Agent owner, ListingDetails d, String photoPath, ListingStatus ls,
                           PropertyStatus ps, String rejectionReason, LocalDateTime createdAt) {
        this.listingID = id;
        this.owner = owner;
        copyDetails(d);
        this.photoPath = photoPath;
        this.listingStatus = ls;
        this.propertyStatus = ps;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    private void copyDetails(ListingDetails d) {
        this.title = d.getTitle();
        this.address = d.getAddress();
        this.propertyType = d.getPropertyType();
        this.cdaSector = d.getCdaSector();
        this.rent = d.getRent();
        this.description = d.getDescription();
        this.furnishing = d.isFurnishing();
        this.occupancyType = d.getOccupancyType();
    }

    // [Creator] the listing creates its own Facility objects
    public Facility addFacility(String name) {
        Facility f = new Facility(name);
        facilities.add(f);
        return f;
    }

    /** used by the repository when loading */
    public void attachFacility(Facility f) { facilities.add(f); }
    public void attachPhoto(String path) { photos.add(path); }
    public void attachTenantHistory(List<FacilityReview> reviews) { this.tenantHistory = new ArrayList<>(reviews); }

    // ---------------------------------------------------------------
    // [UC-04] Edit listing: update fields and sync facilities by name
    // ---------------------------------------------------------------
    public void update(ListingDetails d) {
        copyDetails(d);
        this.propertyStatus = d.getPropertyStatus();
        List<String> wanted = d.getFacilityNames();
        for (Facility f : facilities) f.setActive(containsIgnoreCase(wanted, f.getName()));
        for (String name : wanted) {
            boolean exists = facilities.stream().anyMatch(f -> f.getName().equalsIgnoreCase(name));
            if (!exists) addFacility(name);
        }
        // a rejected listing goes back to the Admin after the agent fixes it
        if (listingStatus == ListingStatus.REJECTED) {
            listingStatus = ListingStatus.PENDING_VERIFICATION;
            rejectionReason = null;
        }
    }

    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    // ---------------------------------------------------------------
    // [UC-06] Verify Listing
    // ---------------------------------------------------------------
    public void approve() {
        if (listingStatus == ListingStatus.VERIFIED) throw new BusinessException("Listing is already verified.");
        this.listingStatus = ListingStatus.VERIFIED;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) throw new BusinessException("Please enter the reason for rejection.");
        this.listingStatus = ListingStatus.REJECTED;
        this.rejectionReason = reason.trim();
    }

    public boolean isVerified() { return listingStatus == ListingStatus.VERIFIED; }
    public boolean isAvailable() { return isVerified() && propertyStatus == PropertyStatus.AVAILABLE; }

    // ---------------------------------------------------------------
    // [UC-09] Does this listing satisfy the customer's filters?
    // ---------------------------------------------------------------
    public boolean matches(SearchCriteria c) {
        if (!isVerified()) return false;
        if (c == null || c.isEmpty()) return true;
        if (c.getKeyword() != null && !c.getKeyword().isBlank()) {
            String k = c.getKeyword().toLowerCase(Locale.ROOT);
            String hay = (title + " " + address + " " + cdaSector + " " + propertyType + " " + description).toLowerCase(Locale.ROOT);
            if (!hay.contains(k)) return false;
        }
        if (c.getPropertyType() != null && !c.getPropertyType().equals(propertyType)) return false;
        if (c.getCdaSector() != null && !c.getCdaSector().equals(cdaSector)) return false;
        if (c.getMinRent() > 0 && rent < c.getMinRent()) return false;
        if (c.getMaxRent() > 0 && rent > c.getMaxRent()) return false;
        if (c.getFurnishing() != null && c.getFurnishing() != furnishing) return false;
        if (c.getOccupancyType() != null && !c.getOccupancyType().equals(occupancyType)) return false;
        for (String wanted : c.getFacilities())
            if (getAdvertisedFacilities().stream().noneMatch(f -> f.getName().equalsIgnoreCase(wanted))) return false;
        return true;
    }

    /** Facilities currently advertised (UC-14 step 2 shows these). */
    public List<Facility> getAdvertisedFacilities() {
        return facilities.stream().filter(Facility::isActive).toList();
    }

    public List<Facility> getAllFacilities() { return List.copyOf(facilities); }
    public List<FacilityReview> getTenantHistory() { return List.copyOf(tenantHistory); }

    private static boolean containsIgnoreCase(List<String> list, String s) {
        return list.stream().anyMatch(x -> x.equalsIgnoreCase(s));
    }

    public void assignId(int id) {
        if (listingID != 0) throw new IllegalStateException("ID already assigned");
        listingID = id;
    }

    // ---- getters ----
    public int getListingID() { return listingID; }
    public String getTitle() { return title; }
    public String getAddress() { return address; }
    public String getPropertyType() { return propertyType; }
    public String getCdaSector() { return cdaSector; }
    public double getRent() { return rent; }
    public String getDescription() { return description; }
    public boolean isFurnishing() { return furnishing; }
    public String getOccupancyType() { return occupancyType; }
    public String getPhotoPath() { return photoPath; }
    public List<String> getPhotos() { return photos.isEmpty() && photoPath != null ? List.of(photoPath) : List.copyOf(photos); }
    public ListingStatus getListingStatus() { return listingStatus; }
    public PropertyStatus getPropertyStatus() { return propertyStatus; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Agent getOwner() { return owner; }
    public List<String> getFacilityNames() { return getAdvertisedFacilities().stream().map(Facility::getName).toList(); }
}

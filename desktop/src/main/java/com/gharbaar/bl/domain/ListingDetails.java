package com.gharbaar.bl.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * =====================================================================
 * [BL - DOMAIN] ListingDetails  (data holder for the listing form)
 * Carries what the Agent typed in UC-03 / UC-04 and validates it.
 * GRASP Information Expert: it owns the data, so it validates itself.
 * =====================================================================
 */
public class ListingDetails {

    // ---- allowed values (shared by form, filters and validation) ----
    public static final List<String> PROPERTY_TYPES = List.of("Portion", "Flat", "Apartment", "Hostel Room");
    public static final List<String> OCCUPANCY_TYPES = List.of("Family", "Individual", "Shared");
    public static final List<String> CDA_SECTORS = List.of(
            "D-12", "E-7", "E-11", "F-6", "F-7", "F-8", "F-10", "F-11", "G-6", "G-7", "G-8",
            "G-9", "G-10", "G-11", "G-13", "G-14", "G-15", "H-13", "I-8", "I-9", "I-10", "B-17");
    public static final List<String> COMMON_FACILITIES = List.of(
            "Wi-Fi", "TV", "Furniture", "Elevator", "Air Conditioning", "Generator",
            "Parking", "Gas", "Security", "Gym", "Meals", "Laundry");

    private final String title;
    private final String address;
    private final String propertyType;
    private final String cdaSector;
    private final double rent;
    private final String description;
    private final boolean furnishing;
    private final String occupancyType;
    private final List<File> photos;
    private final List<String> facilityNames;
    private final PropertyStatus propertyStatus;

    public ListingDetails(String title, String address, String propertyType, String cdaSector, double rent,
                          String description, boolean furnishing, String occupancyType,
                          List<File> photos, List<String> facilityNames, PropertyStatus propertyStatus) {
        this.title = trim(title);
        this.address = trim(address);
        this.propertyType = propertyType;
        this.cdaSector = cdaSector;
        this.rent = rent;
        this.description = trim(description);
        this.furnishing = furnishing;
        this.occupancyType = occupancyType;
        this.photos = photos == null ? List.of() : List.copyOf(photos);
        // remove blanks + duplicates, keep order
        LinkedHashSet<String> f = new LinkedHashSet<>();
        if (facilityNames != null) for (String s : facilityNames) if (s != null && !s.isBlank()) f.add(s.trim());
        this.facilityNames = new ArrayList<>(f);
        this.propertyStatus = propertyStatus == null ? PropertyStatus.AVAILABLE : propertyStatus;
    }

    // ---------------------------------------------------------------
    // [Validation] UC-03 step 4 / UC-04 step 4
    // ---------------------------------------------------------------
    public List<String> validate() {
        List<String> e = new ArrayList<>();
        if (title.length() < 5 || title.length() > 100) e.add("Title must be 5-100 characters.");
        if (address.length() < 5 || address.length() > 200) e.add("Address must be 5-200 characters.");
        if (!PROPERTY_TYPES.contains(propertyType)) e.add("Select a valid property type.");
        if (!CDA_SECTORS.contains(cdaSector)) e.add("Select a valid CDA sector.");
        if (rent <= 0 || rent > 10_000_000) e.add("Rent must be between 1 and 10,000,000 PKR.");
        if (description.length() < 20) e.add("Description must be at least 20 characters.");
        if (!OCCUPANCY_TYPES.contains(occupancyType)) e.add("Select a valid occupancy type.");
        if (facilityNames.isEmpty()) e.add("Add at least one facility.");
        for (String f : facilityNames) if (f.length() > 50) e.add("Facility names must be under 50 characters.");
        return e;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    // ---- getters ----
    public String getTitle() { return title; }
    public String getAddress() { return address; }
    public String getPropertyType() { return propertyType; }
    public String getCdaSector() { return cdaSector; }
    public double getRent() { return rent; }
    public String getDescription() { return description; }
    public boolean isFurnishing() { return furnishing; }
    public String getOccupancyType() { return occupancyType; }
    public List<File> getPhotos() { return photos; }
    public List<String> getFacilityNames() { return facilityNames; }
    public PropertyStatus getPropertyStatus() { return propertyStatus; }
}

package com.gharbaar.bl.domain;

/**
 * [BL - DOMAIN] Facility
 * An amenity advertised by a PropertyListing (Wi-Fi, TV, Generator ...).
 * "active = false" means the agent removed it later; old reviews keep it.
 */
public class Facility {

    private int facilityID;
    private final String name;
    private boolean active = true;

    public Facility(int facilityID, String name, boolean active) {
        this.facilityID = facilityID;
        this.name = name;
        this.active = active;
    }

    public Facility(String name) { this(0, name, true); }

    public int getFacilityID() { return facilityID; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    void setActive(boolean active) { this.active = active; }
    public void assignId(int id) { if (facilityID == 0) facilityID = id; }

    @Override public String toString() { return name; }
}

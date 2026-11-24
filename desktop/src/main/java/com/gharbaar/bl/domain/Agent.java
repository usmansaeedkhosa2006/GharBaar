package com.gharbaar.bl.domain;

/**
 * =====================================================================
 * [BL - DOMAIN] Agent  extends User
 * Lists properties for rent (UC-03, UC-04).
 * GRASP Creator: an Agent creates his PropertyListing.
 * GRASP Expert:  knows whether he owns a listing.
 * =====================================================================
 */
public class Agent extends User {

    public Agent(int userID, String username, String passwordHash, String name, String phone) {
        super(userID, username, passwordHash, name, phone);
    }

    // [Creator] UC-03 Create Property Listing
    public PropertyListing createListing(ListingDetails d, String photoPath) {
        return new PropertyListing(this, d, photoPath);
    }

    // [Expert] UC-04 "Only agent can edit his listings"
    public boolean ownsListing(PropertyListing l) {
        return l != null && l.getOwner() != null && l.getOwner().getUserID() == this.userID;
    }

    @Override public String getRole() { return "AGENT"; }
    @Override public String getHomeView() { return "AgentHome"; }
}

package com.gharbaar.bl.domain;

import java.time.LocalDateTime;

/**
 * =====================================================================
 * [BL - DOMAIN] Customer  extends User
 * Searches properties, views details, requests visits.
 * GRASP Creator: a Customer creates his VisitRequest (UC-12).
 * =====================================================================
 */
public class Customer extends User {

    public Customer(int userID, String username, String passwordHash, String name, String phone) {
        super(userID, username, passwordHash, name, phone);
    }

    // [Creator] UC-12 Request Property Visit
    public VisitRequest makeVisitRequest(PropertyListing l, LocalDateTime dateTime, String notes) {
        return new VisitRequest(this, l, dateTime, notes);
    }

    @Override public String getRole() { return "CUSTOMER"; }
    @Override public String getHomeView() { return "CustomerHome"; }
}

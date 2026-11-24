package com.gharbaar.bl.domain;

/**
 * [BL - DOMAIN] FacilityConfirmation
 * One line of a FacilityReview: "was facility X actually provided?"
 */
public class FacilityConfirmation {

    private final Facility facility;
    private final boolean confirmed;

    public FacilityConfirmation(Facility f, boolean confirmed) {
        this.facility = f;
        this.confirmed = confirmed;
    }

    public Facility getFacility() { return facility; }
    public boolean isConfirmed() { return confirmed; }
}

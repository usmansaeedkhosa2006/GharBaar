package com.gharbaar.bl.domain;

/** [BL - DOMAIN ENUM] Verification state of a PropertyListing (UC-03, UC-06) */
public enum ListingStatus {
    PENDING_VERIFICATION, VERIFIED, REJECTED;

    /** Human readable label shown in the UI. */
    public String label() {
        String s = name().replace('_', ' ').toLowerCase();
        StringBuilder b = new StringBuilder();
        for (String w : s.split(" ")) b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return b.toString().trim();
    }
}

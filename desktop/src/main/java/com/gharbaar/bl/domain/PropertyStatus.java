package com.gharbaar.bl.domain;

/** [BL - DOMAIN ENUM] Occupancy state of a property shown on details page (UC-10) */
public enum PropertyStatus {
    AVAILABLE, OCCUPIED, UNDER_MAINTENANCE;

    /** Human readable label shown in the UI. */
    public String label() {
        String s = name().replace('_', ' ').toLowerCase();
        StringBuilder b = new StringBuilder();
        for (String w : s.split(" ")) b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return b.toString().trim();
    }
}

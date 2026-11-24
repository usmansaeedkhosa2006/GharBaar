package com.gharbaar.bl.domain;

/** [BL - DOMAIN ENUM] State of a VisitRequest (UC-12) */
public enum VisitStatus {
    PENDING, ACCEPTED, DECLINED;

    /** Human readable label shown in the UI. */
    public String label() {
        String s = name().replace('_', ' ').toLowerCase();
        StringBuilder b = new StringBuilder();
        for (String w : s.split(" ")) b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return b.toString().trim();
    }
}

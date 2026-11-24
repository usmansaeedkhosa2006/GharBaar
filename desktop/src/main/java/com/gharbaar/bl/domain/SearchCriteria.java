package com.gharbaar.bl.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 * [BL - DOMAIN] SearchCriteria  (UC-09 Search Properties)
 * Filters chosen by the customer. A null field means "any".
 * =====================================================================
 */
public class SearchCriteria {

    private String keyword;          // free text from the search bar
    private String propertyType;
    private String cdaSector;
    private double minRent;          // 0 = no minimum
    private double maxRent;          // 0 = no maximum
    private Boolean furnishing;      // null = any
    private String occupancyType;
    private List<String> facilities = new ArrayList<>();

    /** Extension 3a: all filters blank -> show all verified listings. */
    public boolean isEmpty() {
        return (keyword == null || keyword.isBlank()) && propertyType == null && cdaSector == null
                && minRent <= 0 && maxRent <= 0 && furnishing == null && occupancyType == null
                && facilities.isEmpty();
    }

    // ---- getters / setters (fluent for convenience) ----
    public String getKeyword() { return keyword; }
    public SearchCriteria setKeyword(String k) { this.keyword = k; return this; }
    public String getPropertyType() { return propertyType; }
    public SearchCriteria setPropertyType(String t) { this.propertyType = t; return this; }
    public String getCdaSector() { return cdaSector; }
    public SearchCriteria setCdaSector(String s) { this.cdaSector = s; return this; }
    public double getMinRent() { return minRent; }
    public SearchCriteria setMinRent(double v) { this.minRent = Math.max(0, v); return this; }
    public double getMaxRent() { return maxRent; }
    public SearchCriteria setMaxRent(double v) { this.maxRent = Math.max(0, v); return this; }
    public Boolean getFurnishing() { return furnishing; }
    public SearchCriteria setFurnishing(Boolean f) { this.furnishing = f; return this; }
    public String getOccupancyType() { return occupancyType; }
    public SearchCriteria setOccupancyType(String o) { this.occupancyType = o; return this; }
    public List<String> getFacilities() { return List.copyOf(facilities); }
    public SearchCriteria setFacilities(List<String> f) { this.facilities = f == null ? new ArrayList<>() : new ArrayList<>(f); return this; }
}

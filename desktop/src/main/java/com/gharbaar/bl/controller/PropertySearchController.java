package com.gharbaar.bl.controller;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.bl.domain.FacilityReview;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.SearchCriteria;
import com.gharbaar.db.ListingRepository;
import com.gharbaar.db.TenancyRepository;

import java.util.Comparator;
import java.util.List;

/**
 * =====================================================================
 * [BL - CONTROLLER] PropertySearchController   (GRASP Controller)
 * UC-09 Search Properties, UC-10 View Property Details,
 * UC-11 View Tenant History
 * =====================================================================
 */
public class PropertySearchController {

    private final ListingRepository listingRepo;
    private final TenancyRepository tenancyRepo;

    public PropertySearchController(ListingRepository listingRepo, TenancyRepository tenancyRepo) {
        this.listingRepo = listingRepo;
        this.tenancyRepo = tenancyRepo;
    }

    // ---------------------------------------------------------------
    // UC-09 Search Properties
    // ---------------------------------------------------------------
    public List<PropertyListing> search(SearchCriteria c) {
        if (c.getMinRent() > 0 && c.getMaxRent() > 0 && c.getMinRent() > c.getMaxRent())
            throw new BusinessException("Minimum rent cannot be greater than maximum rent.");
        List<PropertyListing> results = listingRepo.findVerified().stream()          // step 4: verified only
                .filter(l -> l.matches(c))                                           // Expert: listing decides
                .toList();
        return sortByRelevance(results, c);                                          // step 5
    }

    /** Relevance = more requested facilities matched, sector match, rent close to the requested range. */
    private List<PropertyListing> sortByRelevance(List<PropertyListing> results, SearchCriteria c) {
        if (c.isEmpty())
            return results.stream().sorted(Comparator.comparing(PropertyListing::getCreatedAt).reversed()).toList();
        double target = c.getMaxRent() > 0 ? (c.getMinRent() + c.getMaxRent()) / 2 : 0;
        return results.stream().sorted(Comparator.comparingDouble((PropertyListing l) -> {
            double score = l.getAdvertisedFacilities().size() * 0.5;               // more facilities = better
            if (l.isAvailable()) score += 5;                                       // available first
            if (target > 0) score -= Math.abs(l.getRent() - target) / Math.max(target, 1);
            return -score;
        })).toList();
    }

    // ---------------------------------------------------------------
    // UC-10 View Property Details
    // ---------------------------------------------------------------
    public PropertyListing getPropertyDetails(int listingId) {
        PropertyListing l = listingRepo.findById(listingId);
        if (l == null || !l.isVerified())                                            // ext. 1a
            throw new BusinessException("This listing is no longer available.");
        return l;
    }

    // ---------------------------------------------------------------
    // UC-11 View Tenant History
    // ---------------------------------------------------------------
    public List<FacilityReview> getTenantHistory(int listingId) {
        PropertyListing l = getPropertyDetails(listingId);
        l.attachTenantHistory(tenancyRepo.findReviewsByListing(listingId));
        return l.getTenantHistory();                                                 // empty => ext. 2a
    }
}

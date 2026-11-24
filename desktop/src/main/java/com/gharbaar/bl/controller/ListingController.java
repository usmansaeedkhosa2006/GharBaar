package com.gharbaar.bl.controller;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.bl.domain.Agent;
import com.gharbaar.bl.domain.ListingDetails;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.service.FileStorage;
import com.gharbaar.bl.service.Notifier;
import com.gharbaar.bl.service.Session;
import com.gharbaar.db.ListingRepository;

import java.util.Arrays;
import java.util.List;

/**
 * =====================================================================
 * [BL - CONTROLLER] ListingController   (GRASP Controller)
 * UC-03 Create Property Listing, UC-04 Edit Property Listing
 * =====================================================================
 */
public class ListingController {

    private final ListingRepository listingRepo;
    private final FileStorage storage;
    private final Notifier notifier;

    public ListingController(ListingRepository listingRepo, FileStorage storage, Notifier notifier) {
        this.listingRepo = listingRepo;
        this.storage = storage;
        this.notifier = notifier;
    }

    // ---------------------------------------------------------------
    // UC-03 Create Property Listing
    // ---------------------------------------------------------------
    public PropertyListing createListing(ListingDetails d) {
        Agent agent = Session.getInstance().require(Agent.class);
        List<String> errors = new java.util.ArrayList<>(d.validate());                 // step 4
        if (d.getPhotos().isEmpty()) errors.add("Add at least one photo of the property.");
        else if (!storage.isSupportedImage(d.getPhotos()))                             // special requirement
            errors.add("Photos must be JPG, PNG or WEBP and under 4 MB each.");
        if (!errors.isEmpty()) throw new BusinessException(errors);                    // ext. 4b

        List<String> paths = Arrays.asList(storage.store(d.getPhotos()).split(";"));
        PropertyListing l = agent.createListing(d, paths.get(0));                      // Creator: Agent
        listingRepo.save(l, paths);                                                    // step 5
        notifier.notifyAdmins("New listing \"" + l.getTitle() + "\" is waiting for verification.");
        return l;
    }

    // ---------------------------------------------------------------
    // UC-04 Edit Property Listing
    // ---------------------------------------------------------------
    public List<PropertyListing> getMyListings() {
        Agent agent = Session.getInstance().require(Agent.class);
        return listingRepo.findByAgent(agent.getUserID());
    }

    public PropertyListing getListing(int listingId) {
        Agent agent = Session.getInstance().require(Agent.class);
        PropertyListing l = listingRepo.findById(listingId);
        if (l == null) throw new BusinessException("This listing no longer exists.");
        if (!agent.ownsListing(l)) throw new BusinessException("You can only edit your own listings.");
        return l;
    }

    public void updateListing(int listingId, ListingDetails d) {
        PropertyListing l = getListing(listingId);
        List<String> errors = new java.util.ArrayList<>(d.validate());
        if (!d.getPhotos().isEmpty() && !storage.isSupportedImage(d.getPhotos()))
            errors.add("Photos must be JPG, PNG or WEBP and under 4 MB each.");
        if (!errors.isEmpty()) throw new BusinessException(errors);                    // ext. 4b

        boolean wasRejected = l.getRejectionReason() != null;
        l.update(d);                                                                   // Expert
        List<String> newPaths = null;
        if (!d.getPhotos().isEmpty()) {
            newPaths = Arrays.asList(storage.store(d.getPhotos()).split(";"));
            l.setPhotoPath(newPaths.get(0));
        }
        listingRepo.update(l, newPaths);                                               // step 5
        if (wasRejected)
            notifier.notifyAdmins("Listing \"" + l.getTitle() + "\" was corrected and resubmitted for verification.");
    }

    public void deleteListing(int listingId, String reason) {
        PropertyListing l = getListing(listingId);
        if (reason == null || reason.trim().length() < 5)
            throw new BusinessException("Please give a reason (at least 5 characters) for deleting the listing.");
        listingRepo.delete(l, reason.trim());
    }

    /** Photo bytes for listing images stored in the database (null for web URLs). */
    public byte[] loadImage(String path) {
        FileStorage.StoredFile f = storage.load(path);
        return f == null ? null : f.data();
    }
}

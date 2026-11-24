package com.gharbaar.bl.controller;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.bl.domain.Agent;
import com.gharbaar.bl.domain.Customer;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.VisitRequest;
import com.gharbaar.bl.service.Notifier;
import com.gharbaar.bl.service.Session;
import com.gharbaar.db.ListingRepository;
import com.gharbaar.db.VisitRequestRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * =====================================================================
 * [BL - CONTROLLER] VisitController   (GRASP Controller)
 * UC-12 Request Property Visit (+ agent answers the request)
 * =====================================================================
 */
public class VisitController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final ListingRepository listingRepo;
    private final VisitRequestRepository visitRepo;
    private final Notifier notifier;

    public VisitController(ListingRepository listingRepo, VisitRequestRepository visitRepo, Notifier notifier) {
        this.listingRepo = listingRepo;
        this.visitRepo = visitRepo;
        this.notifier = notifier;
    }

    // ---------------------------------------------------------------
    // UC-12 Request Property Visit
    // ---------------------------------------------------------------
    public VisitRequest requestVisit(int listingId, LocalDateTime dateTime, String notes) {
        Customer customer = Session.getInstance().require(Customer.class);
        if (dateTime == null) throw new BusinessException("Please choose a date and time.");
        if (!isFutureDate(dateTime))                                                   // ext. 3a
            throw new BusinessException("The visit date must be in the future. Please choose a valid date.");
        if (notes != null && notes.length() > 500) throw new BusinessException("Notes must be under 500 characters.");
        PropertyListing l = listingRepo.findById(listingId);
        if (l == null || !l.isAvailable())                                             // ext. 4a
            throw new BusinessException("Sorry, this property is no longer available.");

        VisitRequest v = customer.makeVisitRequest(l, dateTime, notes);                // Creator: Customer
        visitRepo.save(v);                                                             // step 4
        notifier.notify(l.getOwner(), customer.getName() + " requested a visit to \"" + l.getTitle()
                + "\" on " + dateTime.format(FMT) + ".");
        return v;
    }

    private boolean isFutureDate(LocalDateTime dateTime) {
        return dateTime.isAfter(LocalDateTime.now());
    }

    public List<VisitRequest> getMyVisits() {
        Customer c = Session.getInstance().require(Customer.class);
        return visitRepo.findByCustomer(c.getUserID());
    }

    // ---- Agent side: see and answer requests ----
    public List<VisitRequest> getAgentVisitRequests() {
        Agent a = Session.getInstance().require(Agent.class);
        return visitRepo.findByAgent(a.getUserID());
    }

    public void respondToVisit(int requestId, boolean accept) {
        Agent a = Session.getInstance().require(Agent.class);
        VisitRequest v = visitRepo.findById(requestId);
        if (v == null || !a.ownsListing(v.getListing())) throw new BusinessException("Visit request not found.");
        if (accept) v.accept(); else v.decline();
        visitRepo.update(v);
        notifier.notify(v.getCustomer(), "Your visit request for \"" + v.getListing().getTitle() + "\" on "
                + v.getProposedDateTime().format(FMT) + " was " + (accept ? "ACCEPTED" : "DECLINED") + " by the agent.");
    }
}

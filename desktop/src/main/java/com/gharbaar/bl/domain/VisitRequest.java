package com.gharbaar.bl.domain;

import com.gharbaar.bl.BusinessException;

import java.time.LocalDateTime;

/**
 * [BL - DOMAIN] VisitRequest  (UC-12 Request Property Visit)
 * Created by Customer.makeVisitRequest(...). The Agent accepts / declines it.
 */
public class VisitRequest {

    private int requestID;
    private final Customer customer;
    private final PropertyListing listing;
    private final LocalDateTime proposedDateTime;
    private final String notes;
    private VisitStatus status;
    private LocalDateTime createdAt;

    public VisitRequest(Customer c, PropertyListing l, LocalDateTime dateTime, String notes) {
        this(0, c, l, dateTime, notes, VisitStatus.PENDING, LocalDateTime.now());
    }

    public VisitRequest(int id, Customer c, PropertyListing l, LocalDateTime dateTime, String notes,
                        VisitStatus status, LocalDateTime createdAt) {
        this.requestID = id;
        this.customer = c;
        this.listing = l;
        this.proposedDateTime = dateTime;
        this.notes = notes == null ? "" : notes.trim();
        this.status = status;
        this.createdAt = createdAt;
    }

    public void accept() { requirePending(); status = VisitStatus.ACCEPTED; }
    public void decline() { requirePending(); status = VisitStatus.DECLINED; }

    private void requirePending() {
        if (status != VisitStatus.PENDING) throw new BusinessException("This visit request was already answered.");
    }

    public void assignId(int id) { if (requestID == 0) requestID = id; }

    public int getRequestID() { return requestID; }
    public Customer getCustomer() { return customer; }
    public PropertyListing getListing() { return listing; }
    public LocalDateTime getProposedDateTime() { return proposedDateTime; }
    public String getNotes() { return notes; }
    public VisitStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

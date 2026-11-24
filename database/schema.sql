-- =====================================================================
-- GharBaar: Real Estate (Rental) Management System
-- DATABASE SCHEMA (PostgreSQL)
-- Shared by BOTH the JavaFX desktop app (via JDBC) and the web app.
-- Each table maps to a class in the Design Class Diagram (Deliverable 5).
-- =====================================================================

-- ---------------------------------------------------------------------
-- [TABLE] users  ->  User / Customer / Agent / Admin / Tenant
--   role          : CUSTOMER | AGENT | ADMIN
--   tenant_status : NULL for normal customers; a Customer becomes a
--                   Tenant once he submits a tenancy claim
--                   (PENDING -> VERIFIED / REJECTED by Admin, UC-07)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id                 SERIAL PRIMARY KEY,
    username                VARCHAR(30)  NOT NULL UNIQUE,
    password_hash           VARCHAR(100) NOT NULL,
    name                    VARCHAR(80)  NOT NULL,
    phone                   VARCHAR(15)  NOT NULL,
    role                    VARCHAR(10)  NOT NULL CHECK (role IN ('CUSTOMER','AGENT','ADMIN')),
    cnic                    VARCHAR(15),
    tenant_status           VARCHAR(10)  CHECK (tenant_status IN ('PENDING','VERIFIED','REJECTED')),
    tenant_rejection_reason TEXT,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- [TABLE] stored_files  ->  FileStorage (DatabaseFileStorage)
--   Listing photos and tenancy proof documents.
--   admin_only = TRUE for proof documents (UC-13 special requirement)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stored_files (
    file_id       SERIAL PRIMARY KEY,
    file_name     VARCHAR(200) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    data          BYTEA        NOT NULL,
    admin_only    BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_by   INT          REFERENCES users(user_id) ON DELETE SET NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- [TABLE] property_listings  ->  PropertyListing
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS property_listings (
    listing_id        SERIAL PRIMARY KEY,
    agent_id          INT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title             VARCHAR(100) NOT NULL,
    address           VARCHAR(200) NOT NULL,
    property_type     VARCHAR(20)  NOT NULL CHECK (property_type IN ('Portion','Flat','Apartment','Hostel Room')),
    cda_sector        VARCHAR(10)  NOT NULL,
    rent              NUMERIC(12,2) NOT NULL CHECK (rent > 0),
    description       TEXT         NOT NULL,
    furnishing        BOOLEAN      NOT NULL DEFAULT FALSE,
    occupancy_type    VARCHAR(15)  NOT NULL CHECK (occupancy_type IN ('Family','Individual','Shared')),
    photo_path        TEXT,
    listing_status    VARCHAR(25)  NOT NULL DEFAULT 'PENDING_VERIFICATION'
                      CHECK (listing_status IN ('PENDING_VERIFICATION','VERIFIED','REJECTED')),
    property_status   VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE'
                      CHECK (property_status IN ('AVAILABLE','OCCUPIED','UNDER_MAINTENANCE')),
    rejection_reason  TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- [TABLE] listing_photos  ->  gallery photos of a PropertyListing
CREATE TABLE IF NOT EXISTS listing_photos (
    photo_id    SERIAL PRIMARY KEY,
    listing_id  INT  NOT NULL REFERENCES property_listings(listing_id) ON DELETE CASCADE,
    photo_path  TEXT NOT NULL,
    sort_order  INT  NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------
-- [TABLE] facilities  ->  Facility (advertised by a listing)
--   is_active = FALSE when the agent removes it in an edit; the row is
--   kept so old facility reviews stay intact (reviews are immutable).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS facilities (
    facility_id  SERIAL PRIMARY KEY,
    listing_id   INT         NOT NULL REFERENCES property_listings(listing_id) ON DELETE CASCADE,
    name         VARCHAR(50) NOT NULL,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE
);

-- [TABLE] listing_deletions  ->  audit log of UC-04 "delete with reason"
CREATE TABLE IF NOT EXISTS listing_deletions (
    deletion_id  SERIAL PRIMARY KEY,
    listing_id   INT          NOT NULL,
    agent_id     INT          REFERENCES users(user_id) ON DELETE SET NULL,
    title        VARCHAR(100) NOT NULL,
    reason       TEXT         NOT NULL,
    deleted_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- [TABLE] visit_requests  ->  VisitRequest (UC-12)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS visit_requests (
    request_id          SERIAL PRIMARY KEY,
    customer_id         INT       NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    listing_id          INT       NOT NULL REFERENCES property_listings(listing_id) ON DELETE CASCADE,
    proposed_datetime   TIMESTAMP NOT NULL,
    notes               TEXT,
    status              VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','DECLINED')),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- [TABLE] tenancy_claims  ->  TenancyClaim (UC-13)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenancy_claims (
    claim_id          SERIAL PRIMARY KEY,
    tenant_id         INT       NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    listing_id        INT       NOT NULL REFERENCES property_listings(listing_id) ON DELETE CASCADE,
    start_date        DATE      NOT NULL,
    end_date          DATE      NOT NULL,
    proof_document    TEXT      NOT NULL,
    status            VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','VERIFIED','REJECTED')),
    flagged           BOOLEAN   NOT NULL DEFAULT FALSE,
    rejection_reason  TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    CHECK (end_date >= start_date)
);

-- ---------------------------------------------------------------------
-- [TABLE] facility_reviews  ->  FacilityReview (UC-14)
--   claim_id UNIQUE  => one review per verified tenancy (no spam)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS facility_reviews (
    review_id       SERIAL PRIMARY KEY,
    claim_id        INT  NOT NULL UNIQUE REFERENCES tenancy_claims(claim_id) ON DELETE CASCADE,
    comments        TEXT,
    submitted_date  DATE NOT NULL DEFAULT CURRENT_DATE
);

-- [TABLE] facility_confirmations  ->  FacilityConfirmation
CREATE TABLE IF NOT EXISTS facility_confirmations (
    confirmation_id  SERIAL PRIMARY KEY,
    review_id        INT     NOT NULL REFERENCES facility_reviews(review_id) ON DELETE CASCADE,
    facility_id      INT     NOT NULL REFERENCES facilities(facility_id) ON DELETE CASCADE,
    confirmed        BOOLEAN NOT NULL
);

-- ---------------------------------------------------------------------
-- [TABLE] reports  ->  Report (UC-05 Report Issue, UC-08 Resolve Report)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reports (
    report_id           SERIAL PRIMARY KEY,
    reporter_id         INT         NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    type                VARCHAR(40) NOT NULL,
    description         TEXT        NOT NULL,
    status              VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RESOLVED')),
    resolution_remarks  TEXT,
    date_time           TIMESTAMP   NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMP
);

-- ---------------------------------------------------------------------
-- [TABLE] notifications  ->  written by InAppNotifier (Notifier interface)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    notification_id  SERIAL PRIMARY KEY,
    recipient_id     INT       NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    message          TEXT      NOT NULL,
    is_read          BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes used by search / dashboards
CREATE INDEX IF NOT EXISTS idx_listings_status  ON property_listings(listing_status);
CREATE INDEX IF NOT EXISTS idx_listings_agent   ON property_listings(agent_id);
CREATE INDEX IF NOT EXISTS idx_facilities_list  ON facilities(listing_id);
CREATE INDEX IF NOT EXISTS idx_claims_listing   ON tenancy_claims(listing_id);
CREATE INDEX IF NOT EXISTS idx_notif_recipient  ON notifications(recipient_id);

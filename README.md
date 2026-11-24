# GharBaar – Real Estate (Rental) Management System

CS3004 Software Design and Analysis – final prototype
Usman Saeed (25I-0707) · Abdul Rehman Tariq (25I-0715) · CS-3B

GharBaar helps people find verified rental homes (portions, flats, apartments, hostel rooms) in Islamabad's CDA sectors.
Agents list properties, the Admin verifies them, and verified past tenants confirm or deny whether the advertised
facilities were really provided (tenant history).

| Version | Tech | Where |
|---|---|---|
| **Desktop (graded prototype)** | Java 21 + **JavaFX**, JDBC, PostgreSQL | `desktop/` |
| **Web (hosted)** | Next.js / TypeScript, PostgreSQL | `web/` → https://gharbaar-pk.vercel.app |
| **Admin portal (sub-domain)** | same web app, admin host only | https://admin-gharbaar-pk.vercel.app |

Both versions use the **same database** (`database/schema.sql`), so data created in one shows up in the other.

---

## 1. Three layers (UI → BL → DB)

```
desktop/src/main/java/com/gharbaar/
├── ui/          UI layer: JavaFX screens (LoginView, MainShell, customer/, agent/, admin/, common/)
├── bl/          Business Logic layer
│   ├── domain/      User ← Customer ← Tenant, Agent, Admin, PropertyListing, Facility, ListingDetails,
│   │                SearchCriteria, VisitRequest, TenancyClaim, FacilityReview, FacilityConfirmation, Report
│   ├── controller/  AuthController, ListingController, PropertySearchController, VisitController,
│   │                TenancyController, ReportController, VerificationController, AdminController
│   ├── service/     Session, PasswordHasher, Notifier/InAppNotifier, FileStorage/DatabaseFileStorage/LocalFileStorage
│   └── AppContext   wires everything once
└── db/          DB layer: DBConnection (singleton), BaseRepository + one repository per table group

web/src/
├── app/         UI pages (React) + app/api/** (thin HTTP adapters that call controllers)
├── ui/          UI components / screens
├── bl/          same domain classes, controllers and services as the Java version
└── db/          DBConnection, BaseRepository, repositories.ts
```

Every section of code starts with a comment banner such as `// [UC-09] Search Properties`,
`[BL - CONTROLLER]`, `[DB LAYER]`, `[SECTION]` – search for these during the demo.

## 2. Use case → code map

| Use case | Controller method | JavaFX screen | Web page |
|---|---|---|---|
| UC-01 Register (Customer **or** Agent) | `AuthController.register` | `LoginView` (Register tab) | `/login` |
| UC-02 Login | `AuthController.login` | `LoginView` | `/login`, admin: `/admin/login` |
| UC-03 Create Property Listing | `ListingController.createListing` | `agent/ListingFormScreen` | `/agent/new` |
| UC-04 Edit / Delete Listing | `ListingController.updateListing / deleteListing` | `agent/ListingFormScreen`, `MyListingsScreen` | `/agent/[id]/edit` |
| UC-05 Report Issue | `ReportController.submitReport` | `common/ReportIssueScreen` | `/report` |
| UC-06 Verify Listing | `VerificationController.approveListing / rejectListing` | `admin/VerifyListingsScreen` | `/admin/listings` |
| UC-07 Verify Tenant | `VerificationController.approveTenant / rejectTenant` | `admin/VerifyTenantsScreen` | `/admin/tenants` |
| UC-08 Resolve Report | `ReportController.resolveReport` | `admin/ReportsScreen` | `/admin/reports` |
| UC-09 Search Properties | `PropertySearchController.search` | `customer/SearchScreen` | `/home` |
| UC-10 View Property Details | `PropertySearchController.getPropertyDetails` | `customer/PropertyDetailsScreen` | `/property/[id]` |
| UC-11 View Tenant History | `PropertySearchController.getTenantHistory` | details screen → "View Tenant History" | `/property/[id]` |
| UC-12 Request Property Visit | `VisitController.requestVisit` | `customer/VisitRequestDialog` | `/property/[id]` → Request Visit |
| UC-13 Submit Tenancy Claim | `TenancyController.submitClaim` | `customer/TenancyClaimDialog` | `/property/[id]` → I lived here |
| UC-14 Submit Facility Review | `TenancyController.submitReview` | `customer/FacilityReviewDialog` | `/property/[id]` or `/activity` |

Admin extras ("administrative actions and tackling fraudulent use"): dashboard, suspend/activate users,
remove any listing (`AdminController`).

## 3. Design patterns

**GRASP**
- **Controller** – the 8 `*Controller` classes receive every system event from the UI.
- **Information Expert** – `PropertyListing.matches / approve / reject / update`, `TenancyClaim.overlapsWith`,
  `Report.resolve`, `ListingDetails.validate`, `User.verifyPassword`.
- **Creator** – `Agent.createListing`, `Customer.makeVisitRequest`, `Tenant.submitClaim`, `TenancyClaim.createReview`,
  `FacilityReview.addConfirmation`, `User.fileReport`, `PropertyListing.addFacility`.
- **Low Coupling / High Cohesion** – UI only talks to controllers (via `AppContext`); SQL only lives in repositories.
- **Protected Variations** – `Notifier` and `FileStorage` interfaces (swap `DatabaseFileStorage` ↔ `LocalFileStorage`).
- **Pure Fabrication** – repositories, `Session`, `PasswordHasher`, `DBConnection`.

**GoF** – Singleton (`DBConnection`, `Session`, `AppContext`), Factory (`UserFactory`),
Template Method (`FormDialog`, `MasterDetailScreen`, `BaseRepository`), Strategy (`FileStorage`, `Notifier`).

**OOP** – abstract `User` with polymorphic `getRole()` / `getHomeView()`; `Tenant extends Customer`;
private fields with getters; abstract `Screen`, `FormDialog`, `BaseRepository`.

## 4. Database tables

`users`, `property_listings`, `listing_photos`, `facilities`, `listing_deletions`, `visit_requests`, `tenancy_claims`,
`facility_reviews`, `facility_confirmations`, `reports`, `notifications`, `stored_files` (see `database/schema.sql`).
All values from the user are validated in the domain/controllers; all SQL errors are wrapped in `DataAccessException`.

## 5. Running

### Desktop (JavaFX)
1. Copy `desktop/db.properties.example` to `desktop/db.properties` and fill in the Postgres connection
   (use the *direct / unpooled* Neon host).
2. `cd desktop` then `mvnw javafx:run`  (Windows: `mvnw.cmd javafx:run`)
   – or import `desktop` into Eclipse as a Maven project and run `com.gharbaar.Launcher`.

### Web
1. `cd web`, `npm install`, create `.env.local` (see `.env.example`, or `vercel env pull`).
2. `npm run db:init` (creates tables + demo data), then `npm run dev`.
3. Main site: http://localhost:3000 · Admin portal: http://admin.localhost:3000

### Demo accounts
| Role | Username | Password |
|---|---|---|
| Customer | `customer_sara` | `Demo@1234` |
| Customer (verified tenant) | `tenant_bilal` | `Demo@1234` |
| Agent | `agent_mahmud` | `Demo@1234` |
| Admin | `admin` | set by `ADMIN_PASSWORD` (not in the repo) |

`npm run db:reset` wipes the database and restores this demo data.

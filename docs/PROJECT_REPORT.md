# Project report — Aurelia

## Objective

Build a comprehensible Java hotel reservation application that demonstrates object-oriented design through real room pricing, persistent CRUD and correct date availability. The scope is a single hotel and a single application process, designed for an individual SE1020 demonstration.

## Implemented development checkpoints

These are implementation milestones, not invented historical Git commits.

1. **Configuration:** Maven, Java 21 release target, Spring Boot 3.5.7, Web, Thymeleaf and test dependencies. The workspace originally contained only `.git`, with no existing commits or application files.
2. **Models:** Abstract Room and three subclasses; private Customer/Reservation state; constructors/accessors; LocalDate and BigDecimal.
3. **Persistence:** Typed TXT repository, subtype-aware JSON lines, startup load, UUIDs, backups, atomic replacement and malformed-record write lock.
4. **Services:** Interface plus file-backed implementation; validated room/customer CRUD, detached reads and guarded deletion.
5. **Booking:** Correct half-open date intervals, capacity/maintenance checks, cancellation, updates, price calculation and concurrency protection within one JVM.
6. **Frontend:** All ten requested interfaces plus guest profile, edit forms and a helpful error page. Shared header/footer, responsive styles, live quote, search/filter, flash messages, confirmation printing and deletion prompts.
7. **Verification:** Service and full-template request tests; packaged application startup and HTTP smoke checks. The actual results and limits are recorded separately.
8. **Documentation:** This report, README, implementation class diagram, OOP mapping and individual viva preparation.

## Architecture

Controllers parse requests and select Thymeleaf pages. HotelService defines the application contract. FileHotelService owns validation, relationships, concurrency and quotes. FileRepository owns storage. Exceptions are translated into helpful error views. There is no SQL database, frontend framework, Docker or ORM.

Room inheritance directly controls financial outcomes and feature displays. A suite rate is base × 1.50, deluxe base × 1.20 and standard base × 1.00. Room references invoke overridden methods at runtime. Reservation references one room and customer by ID; each may have many reservations.

## User experience

The Aurelia design uses warm backgrounds, dark green/charcoal text, small gold labels, serif titles and large coastal imagery. Shared components maintain consistent navigation and spacing. Forms provide native browser validation and independent backend validation. Small screens use a menu toggle, stacked room cards and horizontally scrollable management tables. Errors return HTTP 400 and allow users to return to their form.

## Data integrity decisions

Bookings store a price snapshot. A later room price change does not silently alter it. Editing the booking deliberately re-quotes current rates. Rooms/customers with history cannot be removed; cancelled history can first be deleted explicitly. Maintenance/capacity changes cannot conflict with future bookings. A failed write does not update in-memory state. Malformed stored records are not silently overwritten.

TXT files do not provide transactions across entities or processes. This application serializes service calls in one JVM and writes one entity file per operation. Manual edits and multiple application processes are outside the supported operating model. Storage errors retain the previous state and provide recovery guidance in README.

## Limitations and future work

The hotel desk is a local demonstration without login or user roles. Public deployment needs authentication, authorization, CSRF protection and privacy controls. There are no payments, taxes, email delivery, refunds, seasonal pricing or check-in status workflows. External fonts, Bootstrap and images require internet; local CSS and standard fallback fonts preserve the core layout. Visual browser verification must be distinguished from automated template/request verification.

## Version control and academic integrity

The initial Git metadata is preserved and no remote push is performed. The genuine local implementation checkpoint is `25409dd` (created after implementation and validation). A separate documentation checkpoint records the handoff materials. No fabricated development history, individual contribution or test result is claimed. Study the actual Java implementation and use the viva guide as a prompt for understanding, not a substitute for it.

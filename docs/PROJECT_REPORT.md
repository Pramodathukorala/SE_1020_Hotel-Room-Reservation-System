# Project Report — Hotel Room Reservation System

**Module:** SE1020 Object-Oriented Programming
**Project type:** Individual
**Repository:** <https://github.com/Pramodathukorala/SE_1020_Hotel-Room-Reservation-System>

---

## 1. Introduction
The project is a web-based room reservation system for a fictional boutique hotel, *The Aurelia*. Guests use it to find and book rooms; hotel staff use it to manage rooms, customers and reservations. The main goals were:

1. to apply object-oriented design meaningfully in Java,
2. to provide complete CRUD operations,
3. to store data persistently using Java file handling, and
4. to deliver a usable, professional-looking interface.

## 2. Requirements and scope

### Functional requirements
| ID | Requirement | Implemented in |
|---|---|---|
| FR1 | Add, view, search, filter, edit and delete rooms | `RoomService`, `AdminRoomController`, `RoomController` |
| FR2 | Three room categories with different features and prices | `StandardRoom`, `DeluxeRoom`, `SuiteRoom` |
| FR3 | Show rooms available for chosen dates and guest count | `ReservationService.findAvailableRooms` |
| FR4 | Register, view, search, edit and delete customers | `CustomerService`, `AdminCustomerController` |
| FR5 | Create, view, search, edit, cancel and delete reservations | `ReservationService`, `BookingController`, `AdminReservationController` |
| FR6 | Prevent double booking (date overlap) | `StayPeriod.overlaps`, `Reservation.conflictsWith`, `ReservationService.checkBookingRules` |
| FR7 | Calculate totals automatically | `Room.calculateStayCost` + overridden `calculateNightlyRate` |
| FR8 | Guest booking confirmation and self-service cancellation | `confirmation.html`, `manage-booking.html` |
| FR9 | Admin dashboard with statistics | `ReservationService.getStatistics`, `admin/dashboard.html` |
| FR10 | Persist all data between runs | `AbstractFileRepository` and subclasses |

### Non-functional requirements
- Readable, explainable code with comments on important OOP decisions.
- Responsive layout (desktop, tablet, mobile) and accessible markup (labels, focus styles, skip link, ARIA attributes).
- Robust storage: atomic file replacement, escaping, tolerance of malformed lines.
- Works offline (Bootstrap and images are bundled locally).

### Out of scope
User authentication, online payments, email notifications and seasonal pricing.

## 3. Design

### 3.1 Architecture
```
Browser ──HTTP──▶ Controller ──▶ Service ──▶ Repository interface ──▶ File repository ──▶ data/*.txt
                 (web layer)     (rules)     (abstraction)             (Java NIO)
```
- **Controllers** (`controller`, `controller.admin`) handle requests, bind and validate forms, and choose Thymeleaf views. They contain no business rules and no file access.
- **Services** (`service`) contain all business rules: validation, availability, pricing, referential integrity.
- **Repositories** (`repository`) are interfaces. The `repository.file` package implements them with text files.
- **Models** (`model`) are plain Java objects that protect their own invariants.
- **DTOs** (`dto`) carry form data and read-only view data between layers.

All dependencies are injected through constructors.

### 3.2 Class design
The UML class diagrams are in [CLASS_DIAGRAM.md](CLASS_DIAGRAM.md). Key decisions:

- **Abstract `Room` with three subclasses.** The room types differ in their data (bed type, view/balcony, bedrooms/butler) and, more importantly, in **behaviour**: how a night is priced. Pricing is therefore an abstract method overridden in each subclass, and the final `calculateStayCost()` combines the nightly rates.
- **`StayPeriod` value object.** Date logic (nights, the overlap formula, list of nights) lives in one immutable class used by both pricing and availability.
- **Availability is derived, not stored.** There is no `isBooked` flag; a room's availability for a date range is computed from confirmed reservations each time.
- **Status as an enum with behaviour.** `ReservationStatus.blocksAvailability()` means cancelled reservations are ignored without special-case code.
- **Generic file repository (Template Method).** Loading, saving, escaping, backup and ID generation are written once in `AbstractFileRepository<T>`. Each concrete repository only maps one object to and from a list of strings.
- **Referential integrity.** Rooms and customers that have reservations cannot be deleted. Only cancelled reservations can be deleted permanently.

### 3.3 Data format
Pipe-separated text, one record per line, with a `#` header line documenting the columns. Special characters are escaped by `RecordCodec`. Dates use ISO-8601 (`2026-11-17`), money uses plain decimal strings (`105000.00`). Example records are shown in the README.

### 3.4 User interface
- **Guest website:** home (hero, availability search, featured rooms, amenities, call to action), room catalogue with filters and type tabs, room details with a server-side price and availability check, booking form with live summary, confirmation ticket, manage-booking page.
- **Admin console:** sidebar navigation, overview cards, occupancy, upcoming stays, room status tiles, searchable tables for rooms, customers and reservations, add/edit forms, profile pages, confirmation dialogs for destructive actions, toast notifications.
- **Visual identity:** ivory backgrounds, deep navy text, champagne-gold accents, Cormorant Garamond serif headings with Inter body text, rounded cards and soft shadows.
- **JavaScript** is used only for enhancements: linking check-in/check-out dates, showing the number of nights, confirmation dialogs, showing type-specific room fields, image preview, the mobile sidebar and toasts. Every action also works as a normal form submission.

## 4. Implementation summary

| Package | Classes |
|---|---|
| `model` | `Identifiable`, `Room`, `StandardRoom`, `DeluxeRoom`, `SuiteRoom`, `RoomType`, `Customer`, `Reservation`, `ReservationStatus`, `StayPeriod` |
| `repository` | `DataRepository<T>`, `RoomRepository`, `CustomerRepository`, `ReservationRepository` |
| `repository.file` | `AbstractFileRepository<T>`, `FileRoomRepository`, `FileCustomerRepository`, `FileReservationRepository` |
| `service` | `RoomService`, `CustomerService`, `ReservationService` |
| `controller` | `HomeController`, `RoomController`, `BookingController`, `GlobalControllerAdvice`, `FormErrors` |
| `controller.admin` | `AdminDashboardController`, `AdminRoomController`, `AdminCustomerController`, `AdminReservationController`, `RoomImages` |
| `dto` | `RoomForm`, `CustomerForm`, `ReservationForm`, `BookingForm`, `ReservationDetails`, `BookingQuote`, `DashboardStats` |
| `exception` | `HotelException`, `ResourceNotFoundException`, `ValidationException`, `BookingConflictException`, `StorageException` |
| `config` | `AppConfig`, `SampleDataInitializer` |
| `util` | `RecordCodec`, `Formats` |

Validation happens in three layers: Bean Validation annotations on form objects (format), service rules (business), and model setters/constructors (invariants).

## 5. Testing

### 5.1 Automated tests
`mvn test` runs **52 tests in 5 classes**. Result on 12 November 2026 (JDK 23, Maven 3.9.9): **52 passed, 0 failed, 0 skipped**.

| Class | Tests | Focus |
|---|---|---|
| `ModelTest` | 5 | polymorphism, overloading, overlap rule, encapsulation |
| `RoomAndCustomerServiceTest` | 14 | room and customer CRUD, uniqueness, validation, delete rules, search |
| `ReservationServiceTest` | 19 | booking, pricing for each room type, double booking, same-day turnover, cancellation, date/capacity/maintenance validation, edit, delete |
| `FilePersistenceTest` | 8 | restart persistence, subclass reconstruction, escaping, malformed and empty files, backups |
| `WebFlowTest` | 6 | full application through HTTP (MockMvc): pages, guest booking flow, validation messages, admin CRUD |

### 5.2 Manual verification
The running application was also checked over HTTP and visually (headless Chrome screenshots in [screenshots/](screenshots/)):
- all public and admin pages returned HTTP 200; an unknown room returned the 404 page, and an invalid filter value returned the 400 page;
- a guest booking redirected to the confirmation page; an overlapping booking for the same room was rejected; a booking starting on the previous guest's check-out date was accepted;
- the guest cancelled through "Manage booking", and the same dates became available again;
- admin create, update, delete and duplicate checks for rooms and customers; admin create, edit (with recalculated total), cancel and delete for reservations;
- after **stopping and restarting** the application, all rooms, customers and reservations were loaded back from `data/` and no sample data was re-added;
- layouts were checked at desktop width (1440 px) and at 520 px, the narrowest width headless Chrome renders reliably, which uses the mobile layout. A check on a real phone is still recommended.

Example totals checked by hand: Deluxe room at LKR 35,000, Fri–Sun → 2 × 38,500 = **LKR 77,000.00**; Panorama Suite at LKR 58,000, Tue–Sun → 3 × 58,000 + 2 × 66,700 = **LKR 307,400.00**.

## 6. Challenges and solutions
| Challenge | Solution |
|---|---|
| Rebuilding the correct `Room` subclass from a text file | A `type` column; `FileRoomRepository.fromFields()` switches on `RoomType`. |
| User text containing the field separator | Backslash escaping in `RecordCodec`, covered by tests. |
| Avoiding corrupted data files | Write to a temporary file, then an atomic move; back up files containing malformed lines before rewriting them. |
| Editing a reservation conflicted with itself | Availability check ignores the reservation being edited (`ignoreReservationId`). |
| Tests depending on today's date | `Clock` injected into `ReservationService`; tests use a fixed date. |
| Deleting a room or customer that is referenced by reservations | Deletion refused with a clear message; rooms can be marked "under maintenance". |

## 7. Limitations and future work
- Add authentication and roles for the admin area (for example, Spring Security).
- Replace file storage with a database by adding new repository implementations. No service changes would be needed.
- Seasonal pricing and discounts, implemented as further subclasses or a pricing strategy.
- Email confirmations and online payment.

## 8. Conclusion
The system meets the project requirements: complete CRUD for rooms, customers and reservations; persistent file storage that survives restarts; reliable double-booking prevention; and automatic pricing. The design uses inheritance, polymorphism, abstraction, encapsulation and information hiding where they solve a real problem, in particular the abstract `Room` with type-specific pricing and the interface-based repository layer. A tested, layered code base keeps every part explainable.

## 9. References
- Spring Boot reference documentation — <https://docs.spring.io/spring-boot/>
- Thymeleaf documentation — <https://www.thymeleaf.org/documentation.html>
- Bootstrap 5.3 documentation — <https://getbootstrap.com/docs/5.3/>
- Java SE 21 API (`java.time`, `java.nio.file`, `java.math.BigDecimal`) — <https://docs.oracle.com/en/java/javase/21/docs/api/>
- Photographs: Unsplash (Unsplash License) — <https://unsplash.com/license>

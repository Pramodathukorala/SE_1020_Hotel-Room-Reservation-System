# Project Report — Hotel Room Reservation System

**Module:** SE1020 Object-Oriented Programming
**Project type:** Individual
**Repository:** <https://github.com/Pramodathukorala/SE_1020_Hotel-Room-Reservation-System>

---

## 1. Introduction
The project is a web-based room reservation system for a fictional boutique hotel, *The Aurelia*. Guests use it to find and book rooms; hotel staff use it to manage rooms, customers and reservations. The main goals were:

1. to apply object-oriented design meaningfully in Java,
2. to provide complete CRUD operations,
3. to store data persistently in a MySQL (SQL) database, and
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
| FR10 | Persist all data between runs | MySQL tables via `AbstractJdbcRepository` and subclasses |
| FR11 | Guest registration and sign-in; "My bookings" with view, edit, cancel and delete; profile editing | `AccountService`, `AuthController`, `MyAccountController` |
| FR12 | Separate administrator sign-in, with admin logins stored in their own table | `admins` table, `AdminAccount`, `AuthInterceptor` |

### Non-functional requirements
- Readable, explainable code with comments on important OOP decisions.
- Responsive layout (desktop, tablet, mobile) and accessible markup (labels, focus styles, skip link, ARIA attributes).
- Data integrity: primary/unique/foreign keys and check constraints in MySQL, transactions, parameterised SQL (no SQL injection).
- Works offline (Bootstrap and images are bundled locally).

### Out of scope
Online payments, email notifications, password reset by email and seasonal pricing.

## 3. Design

### 3.1 Architecture
```
Browser ──HTTP──▶ Controller ──▶ Service ──▶ Repository interface ──▶ JDBC repository ──▶ MySQL
                 (web layer)     (rules)     (abstraction)             (JdbcTemplate, SQL)
```
- **Controllers** (`controller`, `controller.admin`) handle requests, bind and validate forms, and choose Thymeleaf views. They contain no business rules and no file access.
- **Services** (`service`) contain all business rules: validation, availability, pricing, referential integrity.
- **Repositories** (`repository`) are interfaces. The `repository.jdbc` package implements them with SQL statements run through Spring's `JdbcTemplate`.
- **Models** (`model`) are plain Java objects that protect their own invariants.
- **DTOs** (`dto`) carry form data and read-only view data between layers.

All dependencies are injected through constructors.

### 3.2 Class design
The UML class diagrams are in [CLASS_DIAGRAM.md](CLASS_DIAGRAM.md). Key decisions:

- **Abstract `Room` with three subclasses.** The room types differ in their data (bed type, view/balcony, bedrooms/butler) and, more importantly, in **behaviour**: how a night is priced. Pricing is therefore an abstract method overridden in each subclass, and the final `calculateStayCost()` combines the nightly rates.
- **`StayPeriod` value object.** Date logic (nights, the overlap formula, list of nights) lives in one immutable class used by both pricing and availability.
- **Availability is derived, not stored.** There is no `isBooked` flag; a room's availability for a date range is computed from confirmed reservations each time.
- **Status as an enum with behaviour.** `ReservationStatus.blocksAvailability()` means cancelled reservations are ignored without special-case code.
- **Generic JDBC repository (Template Method).** The SQL shared by every table (find, count, delete, insert-or-update, ID generation) is written once in `AbstractJdbcRepository<T>`. Each concrete repository only maps a row to an object and defines its own INSERT/UPDATE.
- **Single-table inheritance in MySQL.** All room subclasses share the `rooms` table; the `room_type` column tells the repository which subclass to create. Amenities are normalised into `room_amenities`.
- **Referential integrity.** Rooms and customers that have reservations cannot be deleted. Only cancelled reservations can be deleted permanently.

### 3.3 Database design
Six tables in the MySQL database `hotel_reservation` (`src/main/resources/schema.sql`):

| Table | Primary key | Other constraints |
|---|---|---|
| `rooms` | `id` | UNIQUE `room_number`; CHECK `room_type` in (STANDARD, DELUXE, SUITE), `base_price > 0`, `capacity` 1–10 |
| `room_amenities` | (`room_id`, `sort_order`) | FK `room_id` → `rooms` ON DELETE CASCADE |
| `customers` | `id` | UNIQUE `email` |
| `guest_accounts` | `id` (= customer id) | FK to `customers` ON DELETE CASCADE; BCrypt `password_hash` |
| `admins` | `id` | UNIQUE `username`; BCrypt `password_hash` |
| `reservations` | `id` | FK `customer_id` → `customers`, FK `room_id` → `rooms`; CHECK `check_out > check_in`, `guests >= 1`, status in (CONFIRMED, CANCELLED) |

Money is stored as `DECIMAL(12,2)` and mapped to `BigDecimal`. Dates use `DATE`, mapped to `LocalDate`. The ER diagram is in [CLASS_DIAGRAM.md](CLASS_DIAGRAM.md). Tables are created automatically at start-up with `CREATE TABLE IF NOT EXISTS`. The login is read from environment variables or a git-ignored local file, never from committed code.

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
| `repository.jdbc` | `AbstractJdbcRepository<T>`, `JdbcRoomRepository`, `JdbcCustomerRepository`, `JdbcReservationRepository` |
| `service` | `RoomService`, `CustomerService`, `ReservationService` |
| `controller` | `HomeController`, `RoomController`, `BookingController`, `GlobalControllerAdvice`, `FormErrors` |
| `controller.admin` | `AdminDashboardController`, `AdminRoomController`, `AdminCustomerController`, `AdminReservationController`, `RoomImages` |
| `dto` | `RoomForm`, `CustomerForm`, `ReservationForm`, `BookingForm`, `ReservationDetails`, `BookingQuote`, `DashboardStats` |
| `exception` | `HotelException`, `ResourceNotFoundException`, `ValidationException`, `BookingConflictException` |
| `config` | `AppConfig`, `SampleDataInitializer` |
| `util` | `Formats` |

Validation happens in three layers: Bean Validation annotations on form objects (format), service rules (business), and model setters/constructors (invariants).

## 5. Testing

### 5.1 Automated tests
`mvn clean test` runs **63 tests in 6 classes** against an in-memory H2 database in MySQL compatibility mode, built from the same `schema.sql`. Result on 12 November 2026 (JDK 23, Maven 3.9.9): **63 passed, 0 failed, 0 skipped**.

| Class | Tests | Focus |
|---|---|---|
| `ModelTest` | 5 | polymorphism, overloading, overlap rule, encapsulation |
| `RoomAndCustomerServiceTest` | 14 | room and customer CRUD, uniqueness, validation, delete rules, search |
| `ReservationServiceTest` | 19 | booking, pricing for each room type, double booking, same-day turnover, cancellation, date/capacity/maintenance validation, edit, delete |
| `DatabasePersistenceTest` | 7 | data surviving a restart (subclasses and amenities rebuilt from SQL), updates/deletes, SQL-injection-style input stored safely, foreign-key and unique constraints |
| `AccountServiceTest` | 8 | registration, BCrypt hashing, guest and admin sign-in, duplicate emails, ownership of bookings |
| `WebFlowTest` | 10 | full Spring application through HTTP (MockMvc): pages, guest booking flow, validation messages, admin CRUD |

### 5.2 Manual verification
The application was first built with text-file storage and checked by hand in a browser and over HTTP:
- all pages loaded, and an unknown room showed the 404 page;
- a booking led to the confirmation page, an overlapping booking was rejected, and same-day turnover was accepted;
- guest cancellation released the room;
- admin CRUD worked for rooms, customers and reservations;
- data survived a restart;
- layouts were checked at 1440 px and 520 px wide.

Storage was then moved to MySQL. Only the repository layer changed; the services, controllers and pages did not. The SQL version is covered by the automated tests above. On 12 November 2026 the application was run against **MySQL Server 8.0** (localhost:3306):
- the `hotel_reservation` database and its four tables were created automatically, and the 9 sample rooms (43 amenity rows) and 3 sample customers were added;
- all public and admin pages loaded;
- a guest booking was saved as a `reservations` row with the correct total (LKR 64,000.00 for two weekday nights at LKR 32,000);
- a second, overlapping booking was rejected;
- cancelling and deleting the test reservation and deleting the test customer removed the rows from MySQL (checked with SQL queries).

After the account features were added, they were checked against MySQL on the same day:
- the `admins` and `guest_accounts` tables were created, and the first admin was stored with a BCrypt hash;
- `/admin` and `/my-bookings` redirected to their login pages when not signed in, and a wrong admin password was rejected;
- a test guest signed in and booked; the booking was saved under that guest even when other guest details were sent;
- the guest changed the stay to 3 nights, and the total was recalculated to LKR 49,500.00 (3 × 16,500), then cancelled and deleted it;
- the admin deleted the test customer, which also removed its login (ON DELETE CASCADE).

Example totals checked by hand: Deluxe room at LKR 35,000, Fri–Sun → 2 × 38,500 = **LKR 77,000.00**; Panorama Suite at LKR 58,000, Tue–Sun → 3 × 58,000 + 2 × 66,700 = **LKR 307,400.00**.

## 6. Challenges and solutions
| Challenge | Solution |
|---|---|
| Storing an inheritance hierarchy in relational tables | Single-table inheritance: a `room_type` column; `JdbcRoomRepository.mapRow()` switches on `RoomType` to rebuild the subclass. |
| A room's list of amenities | Separate `room_amenities` table, loaded for all rooms in one query; saved in the same transaction as the room. |
| Moving from text files to MySQL without breaking the application | Services depend on repository interfaces, so only new `Jdbc...Repository` classes were needed. |
| Running tests without a MySQL server | H2 in-memory database in MySQL mode, created from the same `schema.sql`. |
| Keeping the database password out of GitHub | `${DB_PASSWORD}` environment variable or a git-ignored `application-local.properties`. |
| Editing a reservation conflicted with itself | Availability check ignores the reservation being edited (`ignoreReservationId`). |
| Tests depending on today's date | `Clock` injected into `ReservationService`; tests use a fixed date. |
| Deleting a room or customer that is referenced by reservations | Deletion refused with a clear message; rooms can be marked "under maintenance". |

## 7. Limitations and future work
- Replace the session interceptor with Spring Security to add CSRF protection and login-attempt limits.
- Move searching and filtering into SQL `WHERE` clauses with indexes for very large data sets.
- Seasonal pricing and discounts, implemented as further subclasses or a pricing strategy.
- Email confirmations and online payment.

## 8. Conclusion
The system meets the project requirements: complete CRUD for rooms, customers and reservations; persistent MySQL storage with integrity constraints; reliable double-booking prevention; and automatic pricing. The design uses inheritance, polymorphism, abstraction, encapsulation and information hiding where they solve a real problem, in particular the abstract `Room` with type-specific pricing and the interface-based repository layer. A tested, layered code base keeps every part explainable.

## 9. References
- Spring Boot reference documentation — <https://docs.spring.io/spring-boot/>
- Thymeleaf documentation — <https://www.thymeleaf.org/documentation.html>
- Bootstrap 5.3 documentation — <https://getbootstrap.com/docs/5.3/>
- Spring Framework — Data Access with JDBC — <https://docs.spring.io/spring-framework/reference/data-access/jdbc.html>
- MySQL 8.0 Reference Manual — <https://dev.mysql.com/doc/refman/8.0/en/>
- Java SE 21 API (`java.time`, `java.sql`, `java.math.BigDecimal`) — <https://docs.oracle.com/en/java/javase/21/docs/api/>
- Photographs: Unsplash (Unsplash License) — <https://unsplash.com/license>

# The Aurelia — Hotel Room Reservation System

**SE1020 Object-Oriented Programming project** · Java 21 · Spring Boot 3.5 · Thymeleaf · Bootstrap 5 · text-file persistence

A web-based hotel reservation system for a fictional boutique hotel, "The Aurelia". Guests can browse rooms, check real availability for their dates, see the exact price of every night, book a room and later look up or cancel their booking. Administrators manage rooms, customers and reservations from a dashboard.

The project focuses on **object-oriented design in Java**: an abstract `Room` class with three subclasses that each price a night differently, encapsulated entities with validating setters, repository interfaces that hide the file storage, and a layered Controller → Service → Repository → File architecture.

---

## 1. Main features

| Module | What it does |
|---|---|
| **Room management** | Add, view, search, filter (type, price, guests), edit, delete rooms. Three room types (Standard, Deluxe, Suite) with type-specific details. Rooms can be put under maintenance. |
| **Customer management** | Register, view (with booking history), search, edit and delete customers. Email, phone and name validation; duplicate emails are rejected. |
| **Reservation management** | Create (guest website or admin), view, search, edit, cancel and delete reservations. Real date-overlap checking prevents double booking; totals are calculated automatically with `BigDecimal`. |
| **Guest website** | Home page with availability search, room catalogue with filters, room details with per-night price breakdown, booking form, confirmation page, "Manage booking" (lookup + cancel). |
| **Admin dashboard** | Overview cards (rooms, customers, active bookings, reservations), tonight's occupancy, upcoming stays, room status, and full CRUD tables with search and confirmation dialogs. |
| **Persistence** | `data/rooms.txt`, `data/customers.txt`, `data/reservations.txt` — loaded at start-up, rewritten safely after every change. |

### Booking rules (enforced in `ReservationService`)
- Room and customer must exist.
- Check-out must be after check-in; check-in cannot be in the past; at most 30 nights.
- Guests must be between 1 and the room's capacity.
- Rooms under maintenance cannot be booked.
- Two **confirmed** stays in the same room conflict when `newCheckIn < existingCheckOut && newCheckOut > existingCheckIn`. Check-out is exclusive, so a new guest may arrive on the day the previous guest leaves.
- Cancelled reservations never block a room.

### Pricing rules (polymorphism)
| Room type | Nightly rate |
|---|---|
| `StandardRoom` | base price every night |
| `DeluxeRoom` | base price; **+10%** on Friday and Saturday nights |
| `SuiteRoom` | base price; **+15%** on Friday and Saturday nights; **+LKR 5,000** per night with butler service |

## 2. Technology stack
- **Backend:** Java 21, Spring Boot 3.5.7 (Spring Web MVC, Validation), Maven
- **Views:** Thymeleaf server-side templates
- **Frontend:** HTML5, CSS3, vanilla JavaScript, Bootstrap 5.3.3 and Bootstrap Icons 1.11.3 (stored locally in `static/vendor`, so the site works offline)
- **Fonts:** Cormorant Garamond and Inter (Google Fonts; falls back to Georgia/system fonts when offline)
- **Storage:** Java NIO file reading/writing (no database)
- **Tests:** JUnit 5, AssertJ, Spring MockMvc

## 3. Setup requirements
- JDK **21 or newer** (developed and tested with JDK 23, compiled with `--release 21`)
- Apache Maven 3.9+
- Internet access the first time Maven downloads dependencies
- VS Code with the *Extension Pack for Java* (recommended in `.vscode/extensions.json`)

## 4. How to run

### In VS Code
1. Open the project folder in VS Code and let the Java extension import the Maven project.
2. Open **Run and Debug** and choose **"Run Hotel Reservation System"**, or open `HotelReservationApplication.java` and click **Run**.
3. Visit <http://localhost:8080> (guest website) and <http://localhost:8080/admin> (admin dashboard).

### From a terminal
```bash
mvn spring-boot:run
```

On the first run the application creates the `data/` folder with 9 sample rooms and 3 sample customers. No sample reservations are created; every reservation comes from the real booking flow. To start again from the sample data, stop the application and delete the `data/` folder.

The data folder can be changed in `src/main/resources/application.properties` (`hotel.storage.data-dir`).

> There is no login: the admin area is open so that it can be demonstrated easily. Authentication is outside the scope of this OOP project.

## 5. Project structure

```
src/main/java/com/hotel/reservation
├── HotelReservationApplication.java     Spring Boot entry point
├── config/        AppConfig (Clock bean), SampleDataInitializer (first-run data)
├── model/         Room (abstract), StandardRoom, DeluxeRoom, SuiteRoom, RoomType,
│                  Customer, Reservation, ReservationStatus, StayPeriod, Identifiable
├── dto/           RoomForm, CustomerForm, ReservationForm, BookingForm (form objects)
│                  ReservationDetails, BookingQuote, DashboardStats (view records)
├── repository/    DataRepository<T>, RoomRepository, CustomerRepository, ReservationRepository
│   └── file/      AbstractFileRepository<T>, FileRoomRepository, FileCustomerRepository,
│                  FileReservationRepository
├── service/       RoomService, CustomerService, ReservationService (business rules)
├── controller/    HomeController, RoomController, BookingController,
│   │              GlobalControllerAdvice, FormErrors
│   └── admin/     AdminDashboardController, AdminRoomController,
│                  AdminCustomerController, AdminReservationController, RoomImages
├── exception/     HotelException, ResourceNotFoundException, ValidationException,
│                  BookingConflictException, StorageException
└── util/          RecordCodec (file line format), Formats (money/date formatting)

src/main/resources
├── application.properties
├── templates/     Thymeleaf pages (+ fragments/ and admin/)
└── static/        css/style.css, js/app.js, images/, vendor/ (Bootstrap)

src/test/java/com/hotel/reservation       automated tests
docs/                                      class diagram, OOP explanation, viva guide, report
```

## 6. OOP concepts (summary)
A full explanation with code references is in [docs/OOP_EXPLANATION.md](docs/OOP_EXPLANATION.md).

| Concept | Where |
|---|---|
| **Encapsulation** | Private fields with validating setters in `Room`, `Customer`; `Reservation` changes only through `updateBooking()` and `cancel()`; `getAmenities()` returns a read-only list. |
| **Inheritance** | `StandardRoom`, `DeluxeRoom`, `SuiteRoom` extend `Room`; `FileRoomRepository` etc. extend `AbstractFileRepository<T>`; custom exceptions extend `HotelException`. |
| **Polymorphism** | `calculateNightlyRate()`, `getRoomFeatures()` and `getPricingNote()` are overridden; `Room.calculateStayCost()` calls them through a `Room` reference. |
| **Abstraction** | Abstract `Room` and `AbstractFileRepository`; interfaces `Identifiable` and `DataRepository<T>`. |
| **Information hiding** | Services depend on repository *interfaces*; only the `repository.file` package knows about text files. |
| **Overloading** | `Room.calculateStayCost(StayPeriod)` and `calculateStayCost(LocalDate, LocalDate)`; `ReservationService.quote(...)` with and without a reservation to ignore. |
| **Enums** | `RoomType`, `ReservationStatus` (with behaviour: `blocksAvailability()`). |
| **Composition / association** | `Reservation` *has a* `StayPeriod`; `Room` owns its amenity list; `Reservation` refers to `Customer` and `Room` by ID. |

## 7. CRUD operations

| Entity | Create | Read | Update | Delete |
|---|---|---|---|---|
| Room | `POST /admin/rooms` | `/rooms`, `/rooms/{id}`, `/admin/rooms` | `POST /admin/rooms/{id}` | `POST /admin/rooms/{id}/delete` (refused if the room has reservations) |
| Customer | `POST /admin/customers`, or automatically when booking | `/admin/customers`, `/admin/customers/{id}` | `POST /admin/customers/{id}` | `POST /admin/customers/{id}/delete` (refused if the customer has reservations) |
| Reservation | `POST /book` (guest), `POST /admin/reservations` | `/admin/reservations`, `/admin/reservations/{id}`, `/manage-booking` | `POST /admin/reservations/{id}`; cancel: `POST .../cancel`, `POST /manage-booking/cancel` | `POST /admin/reservations/{id}/delete` (cancelled reservations only) |

## 8. File handling
Each file holds one record per line, with fields separated by `|`. The first line is a `#` comment describing the columns:

```
# id|customerId|roomId|checkIn|checkOut|guests|totalPrice|status|createdAt
RES-0001|C004|R006|2026-11-17|2026-11-20|2|105000.00|CANCELLED|2026-11-12T07:56:14.908548
```

- **Loading:** each repository reads its file once at start-up into a `LinkedHashMap` (ID → object).
- **Subclasses:** the `type` column in `rooms.txt` decides whether a `StandardRoom`, `DeluxeRoom` or `SuiteRoom` is rebuilt.
- **Saving:** every create, update or delete rewrites the file. The content is written to `rooms.txt.tmp` first, then moved over the real file in one step (atomic move), so a crash cannot leave a half-written file.
- **Escaping:** `|`, `\`, line breaks and `;` inside user text are escaped by `RecordCodec`, so input such as `Sea | Sky` cannot break the format.
- **Bad data:** missing files mean "no records yet"; empty and comment lines are ignored; malformed lines and duplicate IDs are skipped with a warning, and the original file is copied to `*.corrupt-<timestamp>.bak` before it is ever rewritten.
- **IDs:** `nextId()` finds the highest existing number and adds one (`R010`, `C004`, `RES-0003`), so IDs are never reused while records exist.

## 9. Screenshots
These were captured from the running application with headless Chrome while it was being tested. They show test data entered during development.

| Home | Room catalogue |
|---|---|
| ![Home page](docs/screenshots/home.jpg) | ![Rooms](docs/screenshots/rooms.jpg) |
| **Room details & price check** | **Booking form** |
| ![Room details](docs/screenshots/room-details.jpg) | ![Booking](docs/screenshots/booking.jpg) |
| **Admin dashboard** | **Admin room form** |
| ![Admin dashboard](docs/screenshots/admin-dashboard.jpg) | ![Room form](docs/screenshots/admin-room-form.jpg) |

Mobile layout (home and admin reservations): ![Mobile](docs/screenshots/mobile.jpg)

## 10. Testing
```bash
mvn test
```
52 automated tests in five classes:

| Test class | Covers |
|---|---|
| `ModelTest` | polymorphic nightly rates, overloaded `calculateStayCost`, `StayPeriod` overlap rule, encapsulation guards, cancel behaviour |
| `RoomAndCustomerServiceTest` | room create/update (including changing type)/delete/search, duplicate room numbers, customer register/update/delete/search, duplicate and invalid emails |
| `ReservationServiceTest` | booking, Standard/Deluxe/Suite totals, double-booking rejection, same-day turnover, cancellation releasing the room, past/invalid dates, capacity, maintenance, unknown IDs, edits, delete rules |
| `FilePersistenceTest` | data surviving a restart (subclasses rebuilt), escaping special characters, malformed lines skipped and backed up, empty/missing files, no leftover temp files |
| `WebFlowTest` | full Spring application through HTTP: pages render, guest booking → confirmation, double booking shown to the user, validation messages, admin CRUD |

The service tests use a fixed `Clock` (Monday 7 January 2030), so weekend pricing and "past date" checks give the same result on any day. Each test uses its own temporary data folder and never touches `data/`.

## 11. Documentation
- [docs/CLASS_DIAGRAM.md](docs/CLASS_DIAGRAM.md) — UML class diagrams (Mermaid)
- [docs/OOP_EXPLANATION.md](docs/OOP_EXPLANATION.md) — OOP principles with code references
- [docs/VIVA_PREPARATION.md](docs/VIVA_PREPARATION.md) — likely viva questions and answers
- [docs/PROJECT_REPORT.md](docs/PROJECT_REPORT.md) — technical report

## 12. GitHub
Repository: <https://github.com/Pramodathukorala/SE_1020_Hotel-Room-Reservation-System>

Individual project for SE1020. Runtime data (`data/`) and build output (`target/`) are excluded by `.gitignore`.

Room and hotel photographs are from [Unsplash](https://unsplash.com) and are used under the Unsplash License. The hotel is fictional.

# Viva Preparation Guide

Likely questions about this project, with short answers that point to the real code. Open the named file while you practise, so you can show the examiner the code as you answer.

---

## A. Demo script (about 5 minutes)

1. **Start the app** (`mvn spring-boot:run` or Run in VS Code). Point out the start-up log: `Empty database: added 9 sample rooms.` the very first time, and `Database contains 9 room(s)... - no sample data added.` on later runs.
2. **Home page** → choose dates → **Search**. Explain: the listing shows only rooms that are free for *every* night (`ReservationService.findAvailableRooms`).
3. Open a **Deluxe** room whose stay includes a Friday → **Check availability & price**. Show the per-night breakdown with the +10% weekend nights → explain polymorphism.
4. **Continue to booking** → submit once with an invalid email to show validation → fix it → **Confirm**. Show the reference `RES-....`.
5. Try to book the **same room and dates** again → "already booked" message (double booking prevented).
6. Book the same room **starting on the previous check-out date** → it succeeds (check-out is exclusive).
7. **Manage booking** → look up with reference + email → **Cancel** → book the original dates again → it works (cancelled bookings release the room).
8. **Register** a guest account and sign in, then open **My bookings**: edit a booking's dates (the total changes), cancel it, then delete it. Open `/admin/login` and sign in as the admin. Show that `/admin` redirects to the login page when signed out.
9. **Admin dashboard** → Rooms: add a room, edit it (change its type), try deleting a room that has reservations (refused), delete the new room. Customers: register, edit, delete.
10. Open **MySQL Workbench** (or the `mysql` client) and run `SELECT * FROM reservations;` to show the stored rows. **Restart** the app and show that everything is still there.
11. Run `mvn test`: 63 tests pass (on an in-memory H2 database).

---

## B. OOP questions

**Q: Where have you used inheritance?**
`StandardRoom`, `DeluxeRoom` and `SuiteRoom` extend the abstract class `Room` (`model/`). They inherit the common fields and methods, such as `calculateStayCost()` and `canAccommodate()`, and add their own fields: `bedType`; `view`/`balcony`; `bedrooms`/`butlerService`. The JDBC repositories also extend `AbstractJdbcRepository<T>`, and the exceptions extend `HotelException`.

**Q: Show me polymorphism.**
`Room.calculateNightlyRate(LocalDate)` is abstract and overridden in each subclass. `Room.calculateStayCost()` loops over the nights and calls `calculateNightlyRate()`. Java decides at runtime which subclass version runs. In `ReservationService.create()` the variable is declared as `Room`, yet a suite gets suite pricing. `getRoomFeatures()` is also overridden; each subclass calls `super.getRoomFeatures()` and adds its own items. See `ModelTest.polymorphicNightlyRates`.

**Q: What is the difference between overriding and overloading? Do you have both?**
Overriding means a subclass replaces a superclass method with the same signature, resolved at runtime (`calculateNightlyRate`). Overloading means the same method name with different parameters, resolved at compile time: `Room.calculateStayCost(StayPeriod)` and `calculateStayCost(LocalDate, LocalDate)`, and the two `ReservationService.quote(...)` methods.

**Q: Why is `Room` abstract and not an interface?**
Rooms share real state (ID, number, price, capacity, amenities) and real behaviour (`calculateStayCost`, validation in the setters). An abstract class can hold fields and implemented methods; an interface cannot hold instance fields. Only the pricing rule differs, so only that part is abstract.

**Q: Why is `calculateStayCost` final?**
The algorithm ("add up every night") must be the same for all rooms. Making it `final` stops a subclass from changing it by mistake. Subclasses customise only the nightly rate. This is the Template Method idea.

**Q: Where is encapsulation?**
All model fields are private. Setters validate, for example `Room.setCapacity()` and `Customer.setEmail()`. `Room.getAmenities()` returns an unmodifiable list. `Reservation` has no setters: status changes only through `cancel()` and details through `updateBooking()`.

**Q: What is the difference between abstraction and information hiding in your project?**
Abstraction is about *what* is exposed: the abstract `Room` and the `DataRepository<T>` interface describe operations without details. Information hiding is about *what is kept secret*: services never see `FileRoomRepository`, the text format or the internal `Map`. They depend only on the repository interfaces, injected through the constructor.

**Q: What is an interface you wrote, and why?**
`DataRepository<T extends Identifiable>` (CRUD contract), with `RoomRepository`, `CustomerRepository` and `ReservationRepository` extending it, plus `Identifiable` (anything with an ID). The generic repository can store any `Identifiable`, and services can use any implementation.

**Q: Where is composition and where is association?**
Composition: a `Reservation` *has a* `StayPeriod`, which is created for it and meaningless on its own. A `BookingForm` has a `CustomerForm`. Association: a `Reservation` refers to a `Customer` and a `Room` by their IDs, and those objects exist independently.

**Q: How do enums help?**
`ReservationStatus.blocksAvailability()` puts the rule "does this status occupy the room?" inside the enum. `Reservation.isActive()` simply calls it, with no string comparisons. `RoomType` restricts room categories to three valid values, and `JdbcRoomRepository` uses it to choose the subclass when reading a row.

**Q: Why `BigDecimal` instead of `double`?**
`double` cannot store values like 0.1 exactly, so money totals can drift (0.1 + 0.2 = 0.30000000000000004). `BigDecimal` is exact, and every amount is rounded with `setScale(2, RoundingMode.HALF_UP)`.

---

## C. Reservation logic questions

**Q: How do you prevent double booking?**
`ReservationService.checkBookingRules()` calls `isRoomAvailable()`, which checks every reservation for that room with `Reservation.conflictsWith()`. That method is true only if the reservation is CONFIRMED, for the same room, and `StayPeriod.overlaps()` is true:
`newCheckIn < existingCheckOut && newCheckOut > existingCheckIn`. A conflict throws `BookingConflictException`.

**Q: Can a guest check in on the day another guest checks out?**
Yes. Check-out is exclusive. If A stays 10th–12th and B books 12th–14th, then `12 < 12` is false, so there is no overlap. Test: `ReservationServiceTest.allowsSameDayTurnover`.

**Q: What happens when a booking is cancelled?**
`Reservation.cancel()` sets the status to CANCELLED. `CANCELLED.blocksAvailability()` is false, so `conflictsWith()` returns false and the nights become available immediately. Test: `cancellationReleasesAvailability`.

**Q: Why not store an `isBooked` boolean on the room?**
A room is free on some nights and booked on others, and a single flag cannot express that. It would also go out of date whenever a booking is cancelled or edited. Availability is always calculated from the reservations.

**Q: What validations are done when booking?**
Room and customer exist; dates present; check-out after check-in; check-in not in the past; at most 30 nights; 1 ≤ guests ≤ capacity; room not under maintenance; no overlap with a confirmed booking. There are two layers. Bean Validation annotations on the forms check format (empty fields, email, phone). The service checks the business rules. The model constructors are a final safety net.

**Q: How is the total calculated? Give an example.**
`room.calculateStayCost(stay)` adds `calculateNightlyRate()` for each night. Example: Deluxe room at LKR 20,000, Thursday to Sunday: Thu 20,000 + Fri 22,000 + Sat 22,000 = **64,000.00** (`ReservationServiceTest.deluxeWeekendPricing`).

**Q: When editing a reservation, why doesn't it clash with itself?**
`update()` passes the reservation's own ID as `ignoreReservationId` to `isRoomAvailable()`, so it is skipped. The total is recalculated with the new room and dates.

**Q: What if two people book the last room at the same moment?**
`create()`, `book()`, `update()` and `cancel()` are `synchronized`, so one finishes checking and saving before the other starts. The second one then sees the first booking and is rejected.

---

## D. Database (MySQL) questions

**Q: Which database do you use, and how does Java connect to it?**
MySQL 8. Spring Boot creates a connection pool (HikariCP) from the settings in `application.properties` (`spring.datasource.url`, username and password). The repositories run SQL through Spring's `JdbcTemplate`. It is a thin layer over plain JDBC that opens and closes connections and converts `SQLException`s into Spring's `DataAccessException`s.

**Q: Why `JdbcTemplate` and not JPA/Hibernate?**
Every SQL statement is visible in the code, so I can explain exactly what is sent to MySQL. JPA would also force annotations, no-argument constructors and setters onto the model classes, which would weaken their encapsulation.

**Q: Show me your tables.**
They are defined in `schema.sql`: `rooms`, `room_amenities`, `customers`, `reservations`.
- Primary keys on `id`.
- `UNIQUE` on `rooms.room_number` and `customers.email`.
- Foreign keys from `reservations` to `customers` and `rooms`, and from `room_amenities` to `rooms` (`ON DELETE CASCADE`).
- `CHECK` constraints such as `check_out > check_in` and `capacity BETWEEN 1 AND 10`.

**Q: How is inheritance stored in a relational database?**
With single-table inheritance: all room types share the `rooms` table, and the `room_type` column (`STANDARD`, `DELUXE`, `SUITE`) records the subclass. Columns used by only one type (`bed_type`; `view_name`, `balcony`; `bedrooms`, `butler_service`) are `NULL` for the others. `JdbcRoomRepository.mapRow()` switches on `room_type` and calls the right subclass constructor, so a suite saved to MySQL comes back as a `SuiteRoom` object. Test: `DatabasePersistenceTest.dataSurvivesARestart`.

**Q: Why is there a separate `room_amenities` table?**
A room has many amenities. Storing a list in one column would break first normal form, so each amenity is one row, linked by `room_id`, with `sort_order` to keep the order. `loadDetails()` loads amenities for *all* rooms in **one** query instead of one query per room.

**Q: Walk me through saving a new reservation.**
`ReservationService.create()` validates the rules, builds a `Reservation` and calls `reservationRepository.save()`. `AbstractJdbcRepository.save()` calls `existsById()`. The ID is new, so `JdbcReservationRepository.insert()` runs
`INSERT INTO reservations (id, customer_id, room_id, check_in, ...) VALUES (?, ?, ?, ?, ...)`.

**Q: What is SQL injection and how do you prevent it?**
It happens when user text is joined into an SQL string, so input like `'; DROP TABLE customers; --` becomes part of the command. Every query here uses `?` placeholders, and the driver sends the values separately from the SQL text. Test: `textWithQuotesAndSqlIsStoredSafely` stores exactly that text as an address, and the table is still there afterwards.

**Q: What is a transaction and where do you use one?**
A group of statements that succeed or fail together. `JdbcRoomRepository.save()` is `@Transactional`: it updates the `rooms` row, deletes the old amenity rows and inserts the new ones. If anything fails, all of it is rolled back, so a room is never left without its amenities.

**Q: If the service already checks for duplicates, why have UNIQUE and FOREIGN KEY constraints?**
Defence in depth. The service gives friendly messages, and the database still guarantees integrity if a rule is bypassed, for example by a bug or by direct SQL. Test: `foreignKeyStopsDeletingARoomThatHasReservations` calls the repository directly and the database refuses. `GlobalControllerAdvice` turns such errors into a friendly 409 page.

**Q: How are the tables created? Will restarting delete my data?**
`spring.sql.init.mode=always` runs `schema.sql` at start-up. Every statement is `CREATE TABLE IF NOT EXISTS`, so existing tables and rows are left alone. The database itself is created by `createDatabaseIfNotExist=true` in the connection URL. `SampleDataInitializer` only adds sample rooms and customers when all tables are empty.

**Q: How are IDs generated?**
`AbstractJdbcRepository.nextId()` selects the existing IDs with the prefix (`SELECT id FROM rooms WHERE id LIKE 'R%'`), finds the highest number and adds one (`R010`). Readable IDs such as `RES-0007` are easier for guests than auto-increment numbers.

**Q: Where is the password? Is it in GitHub?**
No. `application.properties` reads `${DB_USERNAME}` and `${DB_PASSWORD}`, or an `application-local.properties` file that is listed in `.gitignore`.

**Q: Your tests don't need MySQL. How?**
They use H2, an in-memory SQL database, in MySQL compatibility mode, created from the same `schema.sql`. Each test gets a fresh database, so tests are fast, independent and never touch real data.

**Q: What changed when you moved from text files to MySQL?**
Only the repository implementations (the new `repository.jdbc` package), the configuration and the tests. The services, controllers and pages did not change, because services depend on the repository *interfaces*. This is information hiding in practice.

---

## E. Login & security questions

**Q: How do guests and admins log in?**
Guests register at `/register` and sign in at `/login` with their email and password. Admins sign in at `/admin/login` with a username. Admins are stored in their own table, `admins`. Guest logins are in `guest_accounts`, where the id is the same as the customer id. `AccountService.authenticateGuest()` and `authenticateAdmin()` check the password and return a `SessionUser`, which is stored in the HTTP session.

**Q: How are passwords stored?**
Never in plain text. `BCryptPasswordEncoder.encode()` makes a salted one-way hash (for example `$2a$10$...`). At sign-in, `encoder.matches(typed, storedHash)` hashes the typed password again with the stored salt and compares the results. Even the database administrator cannot read the passwords. Test: `registeringCreatesACustomerAndAHashedLogin`.

**Q: How are the admin pages protected?**
`AuthInterceptor`, registered in `WebConfig`, runs before every controller method. For `/admin/**` it checks that the session contains an admin `SessionUser`; otherwise it redirects to `/admin/login`. For `/my-bookings/**` and `/account/**` it requires a signed-in guest. A guest session is *not* an admin session (test `adminPagesNeedAnAdminLogin`).

**Q: Can a guest open someone else's booking by changing the URL?**
No. `ReservationService.findOwned()` checks that the reservation's `customerId` equals the signed-in guest's ID; otherwise it throws "not found" (404), so the page does not even reveal that the reference exists. A signed-in guest also always books under their own profile, because the server replaces any guest details sent in the form.

**Q: Where is OOP in the login code?**
`UserAccount` is abstract; `AdminAccount` and `GuestAccount` extend it and override `getRole()` and `getHomePage()` (polymorphism). The password hash is encapsulated: only `passwordMatches()` can use it. `Role` is an enum.

**Q: What security measures did you add, and what is still missing?**
In place:
- BCrypt password hashes.
- The same error message for an unknown user and a wrong password.
- A new session ID at sign-in, to prevent session fixation.
- Redirects after login only go to local paths, to prevent open redirects.
- Parameterised SQL.
- The database password is kept out of Git.

Missing, with the fix: there is no CSRF protection and no limit on login attempts. Adding Spring Security would cover both.

---

## F. Architecture & Spring questions

**Q: Explain the layers.**
Controller (HTTP, forms, choosing a view) → Service (business rules) → Repository interface → JDBC repository → MySQL. For example, `AdminRoomController.create()` → `RoomService.create()` → `RoomRepository.save()` → `JdbcRoomRepository.insert()` → `INSERT INTO rooms ...`.

**Q: What is dependency injection here?**
Each class receives what it needs through its constructor, for example `RoomService(RoomRepository, ReservationRepository)`. Spring creates the objects (they are annotated `@Service`, `@Repository` and `@Controller`) and passes them in. Tests build the same objects by hand with `new`, in `TestHotel`.

**Q: What does `@Valid` + `BindingResult` do?**
Spring checks the annotations on the form object (`@NotBlank`, `@Email`, `@Pattern`, `@Min`, ...) and puts the errors in `BindingResult`. The controller redisplays the form, and Thymeleaf's `th:errors` shows each message under its field. Business-rule errors from services are added to the same `BindingResult` by `FormErrors.apply()`.

**Q: What is Thymeleaf?**
A server-side template engine. Controllers put data into the `Model`, and templates such as `rooms.html` read it with expressions like `${room.name}`. Shared parts (navbar, footer, room card) are fragments in `templates/fragments/`.

**Q: Why is the `Clock` injected?**
So "today" can be fixed in tests (`TestHotel.CLOCK` = Monday 7 Jan 2030). That makes rules like "check-in cannot be in the past" and weekend pricing testable on any day.

**Q: How are errors shown to the user?**
Form errors appear next to the fields. Flash messages appear as toast notifications after redirects. `GlobalControllerAdvice` turns `ResourceNotFoundException` into a 404 page, database constraint violations into a 409 page and database connection errors into a 500 page, and `error.html` covers anything else. Stack traces are never shown (`server.error.include-stacktrace=never`).

---

## G. Limitations (be honest if asked)
- Sign-in is session-based without Spring Security, so there is no CSRF protection and no lockout after repeated wrong passwords.
- No online payment: payment is "at the hotel".
- Searching is done in Java after loading the rows, which is simple and easy to explain. For very large data, filtering would move into SQL `WHERE` clauses and indexes.
- A room with booking history cannot be deleted. The design choice is to mark it "under maintenance" instead, to keep old reservations valid.
- MySQL must be installed, and its password supplied (environment variable or local file), before the app can run.
- Prices are fixed per room type rule; there are no seasonal prices or discounts.

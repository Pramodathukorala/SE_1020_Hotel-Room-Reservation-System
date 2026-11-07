# Individual viva preparation

Read the code and run the demonstration yourself. This guide describes the implementation; it does not claim you authored or understand code you have not reviewed.

## Classes and constructors

| Class | Responsibility / constructors / important methods |
| --- | --- |
| `HotelApplication` | Spring Boot entry point. `main` starts the application context and embedded Tomcat. |
| `Room` | Abstract common room state. Protected empty/full constructors; accessors; abstract rate, features and category methods. |
| `StandardRoom` | Public empty/full constructors. Overrides rate with base × 1.00 and returns standard features. |
| `DeluxeRoom` | Public empty/full constructors. Overrides rate with base × 1.20 and returns deluxe features. |
| `SuiteRoom` | Public empty/full constructors. Overrides rate with base × 1.50 and returns suite features. |
| `Customer` | Private guest fields. Empty constructor for deserialization/form binding, full constructor for creation, accessors. |
| `Reservation` | Stores associations, dates, guests, quoted total and cancellation state. Empty/full constructors and accessors. |
| `HotelService` | Public contract for reads, CRUD, availability and cancellation. No constructor because it is an interface. |
| `FileHotelService` | Constructor loads files using configured directory and ObjectMapper; creates initial rooms only for absent room storage. `saveRoom`, `saveCustomer`, `book`, `available`, `cancel` and deletion methods implement rules. Private `dates`, `require`, `identifier` and `copy` keep helpers hidden. |
| `FileRepository<T>` | Constructor takes a Path, ObjectMapper and entity class. `readAll` loads records; `writeAll` atomically saves a snapshot with backup. |
| `HotelController` | Constructor receives the service. GET methods prepare page models; POST methods delegate mutations and redirect to avoid refresh resubmission. `common` provides the shared service/date model attributes. |
| `ErrorHandler` | Controller advice: `invalid` translates business and form conversion failures into a helpful HTTP 400 page. |
| `HotelException` | Runtime exception with message-only and message/cause constructors. Represents validation/storage failures. |
| `StoredRecordValidator` | Non-instantiable utility. Static `validate` rejects incomplete stored entities before service use; private `text` checks required strings. |
| `HotelServiceTest` | Isolated service and persistence tests with JUnit temporary storage. |
| `WebTest` | Boots Spring with isolated files and renders Thymeleaf pages using MockMvc. |

## Explain one CRUD flow

Room creation: POST `/admin/rooms` → controller parses form fields → `saveRoom` validates number uniqueness, price, capacity and description → subtype is constructed with a UUID → repository writes a candidate snapshot → service assigns that snapshot → controller redirects to room management. Reads use `rooms()` or `room(id)`. Updates supply an existing ID to `saveRoom`. Deletes check reservation references before saving a list without the room.

Customer CRUD follows the same flow; email uniqueness, name and phone are checked. Reservation creation/update uses `book`; cancellation changes its status; permanent deletion requires cancellation. Existing IDs must resolve before updating. Service responses are copies so direct setter calls on returned objects cannot bypass validation.

## Explain the booking algorithm

1. Resolve an existing reservation for updates; reject cancelled updates.
2. Require non-null dates, checkout after check-in and check-in today or later.
3. Resolve the room and guest; check positive guests within capacity.
4. Reject maintenance and conflicting non-cancelled reservations for the same room.
5. Compute nights with `ChronoUnit.DAYS.between(in, out)`.
6. Call the actual room subtype's `calculateNightlyRate()` and multiply by nights using BigDecimal.
7. Save a new or replacement reservation with its final total before publishing it in memory.

The overlap test is:

```java
newCheckIn.isBefore(existingCheckOut)
    && newCheckOut.isAfter(existingCheckIn)
```

Intervals are `[checkIn, checkOut)`: checkout is excluded. June 10–12 and June 12–14 do not overlap. June 11–13 overlaps both. The edited reservation's ID is excluded from conflict checking. Cancellation removes a reservation from availability conflicts. The synchronized service lock covers both checking and saving, preventing a race between two requests in one JVM.

## Explain file handling

Each TXT line contains a JSON object. Jackson escapes special characters and preserves BigDecimal and LocalDate through configured modules. `type` selects StandardRoom, DeluxeRoom or SuiteRoom on loading. Files are created if missing. Blank lines are ignored; bad JSON or duplicate/missing IDs are reported and lock the file against writes. Records already readable may still be viewed. Original bad data remains untouched.

For a save, serialize the candidate list into a temporary file in the same directory, copy the current file to `.bak`, then atomically replace the target. If this fails, the service still holds its previous list. Atomic move is required; the implementation fails safely on a filesystem without this capability. Backups are one previous version, not a full disaster-recovery system. Do not run multiple JVMs against the same files.

## Common questions

**Why is Room abstract?** A generic room has shared state, but its category determines pricing and features. We should create concrete room types.

**Can an abstract class have constructors?** Yes. Subclass constructors call `super(...)` to initialize inherited state.

**Overloading versus overriding?** Overloading changes parameter lists in one API; overriding provides a subtype implementation of an inherited method with the same signature. Room pricing uses overriding.

**Where is dynamic dispatch?** In `selected.calculateNightlyRate()` during booking. The declared type is Room; the actual object selects the method.

**Why BigDecimal?** Decimal money should avoid binary floating-point rounding. Construct values from strings; the nightly rate rounds to two decimal places with HALF_UP.

**Why LocalDate?** A hotel stay is measured in calendar dates, without timezone or time-of-day arithmetic. The server's local calendar supplies today's validation date.

**What is dependency injection?** Spring constructs the service and gives it to the controller's constructor. The controller does not create its own repository.

**What if a save fails?** A user-friendly error appears; the previous in-memory list remains active and the previous target data is preserved if replacement did not complete.

**Why store IDs instead of nested Room/Customer objects?** To avoid duplicated, stale copies and keep relationships stable. Deletion guards prevent broken references through the UI.

**Does cancelling delete history?** No. It marks the reservation cancelled and frees availability. A separate action deletes a cancelled record.

**Can a guest change a booked room's capacity?** The management service blocks changes that would invalidate upcoming bookings.

**Does a room price edit change old booking totals?** No. The stored quote stays fixed until a booking is explicitly edited and re-quoted.

**Is this production-ready?** It is an assessed local demonstration. Authentication, authorization, CSRF protection, multi-process storage and payments would be additional work for public deployment.

**What did the tests prove?** Read `docs/VERIFICATION.md` for the actual execution. The tests exercise backend rules and rendered HTTP flows; they do not by themselves prove visual quality in a browser.

## Practice order

Start with Room and its subclasses, then Reservation, then `book`/`available`, then `FileRepository`, then a controller method. Run `mvn test`, read one test per business rule and demonstrate a rejected overlap plus an allowed adjacent stay. Explain every line in those paths before your viva.

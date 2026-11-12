# Viva Preparation Guide

Likely questions about this project, with short answers that point to the real code. Open the named file while you practise, so you can show the examiner the code as you answer.

---

## A. Demo script (about 5 minutes)

1. **Start the app** (`mvn spring-boot:run` or Run in VS Code). Point out the start-up log lines: `First run: added 9 sample rooms...` the very first time, and `Loaded 9 record(s) from data\rooms.txt` on later runs.
2. **Home page** → choose dates → **Search**. Explain: the listing shows only rooms that are free for *every* night (`ReservationService.findAvailableRooms`).
3. Open a **Deluxe** room whose stay includes a Friday → **Check availability & price**. Show the per-night breakdown with the +10% weekend nights → explain polymorphism.
4. **Continue to booking** → submit once with an invalid email to show validation → fix it → **Confirm**. Show the reference `RES-....`.
5. Try to book the **same room and dates** again → "already booked" message (double booking prevented).
6. Book the same room **starting on the previous check-out date** → it succeeds (check-out is exclusive).
7. **Manage booking** → look up with reference + email → **Cancel** → book the original dates again → it works (cancelled bookings release the room).
8. **Admin dashboard** → Rooms: add a room, edit it (change its type), try deleting a room that has reservations (refused), delete the new room. Customers: register, edit, delete.
9. Open `data/reservations.txt` in VS Code to show the stored records. **Restart** the app and show that everything is still there.
10. Run `mvn test` → 52 tests pass.

---

## B. OOP questions

**Q: Where have you used inheritance?**
`StandardRoom`, `DeluxeRoom` and `SuiteRoom` extend the abstract class `Room` (`model/`). They inherit the common fields and methods, such as `calculateStayCost()` and `canAccommodate()`, and add their own fields: `bedType`; `view`/`balcony`; `bedrooms`/`butlerService`. The file repositories also extend `AbstractFileRepository<T>`, and the exceptions extend `HotelException`.

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
`ReservationStatus.blocksAvailability()` puts the rule "does this status occupy the room?" inside the enum. `Reservation.isActive()` simply calls it, with no string comparisons. `RoomType` restricts room categories to three valid values, and the room file loader uses it to choose the subclass.

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

## D. File handling questions

**Q: How is data stored?**
In three text files in `data/`, one record per line, with fields separated by `|` (see the header line at the top of each file). Example:
`R007|SUITE|301|Panorama Suite|...|58000.00|4|Free Wi-Fi;Living room;...|/images/rooms/suite-panorama.jpg|false|2|false`

**Q: When are the files read and written?**
Each `File...Repository` reads its file once in its constructor (`AbstractFileRepository.load()`) into a `LinkedHashMap`. Every `save()` and `deleteById()` immediately rewrites the whole file (`writeFile()`).

**Q: How do you rebuild the right subclass when loading rooms?**
The second column holds the `RoomType`. `FileRoomRepository.fromFields()` switches on it and calls the `StandardRoom`, `DeluxeRoom` or `SuiteRoom` constructor with the type-specific columns. Test: `FilePersistenceTest.dataSurvivesARestart`.

**Q: What if a user types `|` in a description?**
`RecordCodec.encode()` escapes it as `\|`, and also `\` as `\\` and line breaks as `\n`. `decode()` reverses this. Test: `specialCharactersAreEscapedSafely`.

**Q: How do you avoid corrupting the file?**
The new content is written to `rooms.txt.tmp` first, then `Files.move(..., ATOMIC_MOVE)` replaces the real file in one step. If writing fails, the in-memory change is rolled back and a `StorageException` is shown as a friendly error page.

**Q: What if the file contains a broken line?**
That line is skipped with a log warning and the rest still loads. Before the file is ever rewritten, the original is copied to `rooms.txt.corrupt-<time>.bak`, so nothing is lost. A missing file just means "no records yet". Test: `malformedLinesAreSkippedAndTheOriginalFileIsBackedUp`.

**Q: How are IDs generated, and can they be duplicated?**
`AbstractFileRepository.nextId()` finds the highest number for the prefix (`R`, `C`, `RES-`) and adds 1. Duplicate IDs found while loading are rejected as malformed.

**Q: Will sample data overwrite my records on restart?**
No. `SampleDataInitializer` only seeds a file that does **not exist**, so deleted or edited records stay as they are.

**Q: Why not Java serialization (`ObjectOutputStream`)?**
Binary files cannot be read or fixed by hand, break when a class changes, and are harder to explain. A text format is transparent and easy to demonstrate.

---

## E. Architecture & Spring questions

**Q: Explain the layers.**
Controller (HTTP, forms, choosing a view) → Service (business rules) → Repository interface → File repository → text file. For example, `AdminRoomController.create()` → `RoomService.create()` → `RoomRepository.save()` → `AbstractFileRepository.writeFile()`.

**Q: What is dependency injection here?**
Each class receives what it needs through its constructor, for example `RoomService(RoomRepository, ReservationRepository)`. Spring creates the objects (they are annotated `@Service`, `@Repository` and `@Controller`) and passes them in. Tests build the same objects by hand with `new`, in `TestHotel`.

**Q: What does `@Valid` + `BindingResult` do?**
Spring checks the annotations on the form object (`@NotBlank`, `@Email`, `@Pattern`, `@Min`, ...) and puts the errors in `BindingResult`. The controller redisplays the form, and Thymeleaf's `th:errors` shows each message under its field. Business-rule errors from services are added to the same `BindingResult` by `FormErrors.apply()`.

**Q: What is Thymeleaf?**
A server-side template engine. Controllers put data into the `Model`, and templates such as `rooms.html` read it with expressions like `${room.name}`. Shared parts (navbar, footer, room card) are fragments in `templates/fragments/`.

**Q: Why is the `Clock` injected?**
So "today" can be fixed in tests (`TestHotel.CLOCK` = Monday 7 Jan 2030). That makes rules like "check-in cannot be in the past" and weekend pricing testable on any day.

**Q: How are errors shown to the user?**
Form errors appear next to the fields. Flash messages appear as toast notifications after redirects. `GlobalControllerAdvice` turns `ResourceNotFoundException` into a 404 page and `StorageException` into a 500 page, and `error.html` covers anything else. Stack traces are never shown (`server.error.include-stacktrace=never`).

---

## F. Limitations (be honest if asked)
- No login: the admin area is open, so it is easy to demonstrate.
- No online payment: payment is "at the hotel".
- The whole file is rewritten on every change. That is fine for hundreds of records but not for millions, where a database would be the next step. Because services depend on interfaces, it could be added without changing them.
- A room with booking history cannot be deleted. The design choice is to mark it "under maintenance" instead, to keep old reservations valid.
- Prices are fixed per room type rule; there are no seasonal prices or discounts.

# Object-Oriented Programming in the Hotel Reservation System

This document explains where each OOP principle appears in the code and why it was used. File paths are relative to `src/main/java/com/hotel/reservation/`.

---

## 1. Encapsulation

**Idea:** an object keeps its data private and controls every change to it, so it can never be put into an invalid state.

| Where | How |
|---|---|
| `model/Room.java` | All fields are `private`. `setBasePrice()` rejects zero/negative prices, `setCapacity()` only allows 1–10, `setRoomNumber()` checks the format. The constructor calls these setters, so even a newly created room is validated. |
| `model/Room.java` – `getAmenities()` | Returns `Collections.unmodifiableList(amenities)`. Callers can read the list but `room.getAmenities().add("x")` throws an exception; the only way to change it is `setAmenities()`. |
| `model/Customer.java` | `setEmail()` checks the email pattern and stores it in lower case; `setPhone()` checks for 9–15 digits; `setFullName()` and `setAddress()` check lengths. |
| `model/Reservation.java` | There are **no public setters**. The booking details change only through `updateBooking(...)` (which refuses cancelled bookings) and the status only through `cancel()`. The `id`, `customerId` and `createdAt` fields are `final`. |
| `model/StayPeriod.java` | Immutable: both fields are `final` and the constructor rejects a check-out that is not after the check-in. |

```java
public void setCapacity(int capacity) {
    if (capacity < 1 || capacity > MAX_CAPACITY) {
        throw new IllegalArgumentException("Capacity must be between 1 and " + MAX_CAPACITY + " guests.");
    }
    this.capacity = capacity;
}
```

## 2. Inheritance

**Idea:** a subclass reuses the code of its superclass and adds or changes behaviour ("IS-A" relationship).

| Superclass | Subclasses | What is inherited / what differs |
|---|---|---|
| `model/Room` (abstract) | `StandardRoom`, `DeluxeRoom`, `SuiteRoom` | All share ID, number, name, price, capacity, amenities, image, maintenance flag and the stay-cost algorithm. Each adds its own fields (`bedType`; `view` + `balcony`; `bedrooms` + `butlerService`) and its own pricing. |
| `repository/file/AbstractFileRepository<T>` | `FileRoomRepository`, `FileCustomerRepository`, `FileReservationRepository` | All inherit loading, saving, atomic file replacement, malformed-line handling and ID generation. Each only defines its own record format. |
| `exception/HotelException` | `ResourceNotFoundException`, `ValidationException`, `BookingConflictException`, `StorageException` | Controllers can catch all business errors with `catch (HotelException e)`, or a specific one when needed. |
| Interfaces | `RoomRepository extends DataRepository<Room>` | Interface inheritance: the room repository has all generic CRUD methods plus `findByRoomNumber()`. |

Subclass constructors call `super(...)`:
```java
public SuiteRoom(String id, ..., int bedrooms, boolean butlerService) {
    super(id, roomNumber, name, description, basePrice, capacity, amenities, imageUrl, underMaintenance);
    setBedrooms(bedrooms);
    this.butlerService = butlerService;
}
```

## 3. Polymorphism

**Idea:** the same method call behaves differently depending on the actual object type at runtime (method overriding + dynamic dispatch).

`Room` declares `calculateNightlyRate(LocalDate)` as abstract. Each subclass overrides it:

| Class | Rule |
|---|---|
| `StandardRoom` | returns the base price |
| `DeluxeRoom` | base price × 1.10 on Friday/Saturday |
| `SuiteRoom` | base price × 1.15 on Friday/Saturday, + LKR 5,000 if butler service |

The total is calculated in **one** place, `Room.calculateStayCost()`, which does not know what kind of room it is:

```java
public final BigDecimal calculateStayCost(StayPeriod stay) {
    BigDecimal total = BigDecimal.ZERO;
    for (LocalDate night : stay.getNightDates()) {
        total = total.add(calculateNightlyRate(night));   // dispatched to the subclass at runtime
    }
    return money(total);
}
```

`ReservationService` only ever holds a `Room` reference:
```java
Room room = roomRepository.findById(roomId)...;      // could be any subclass
BigDecimal total = room.calculateStayCost(stay);      // polymorphic call
```

Other overridden methods: `getRoomFeatures()` (each subclass calls `super.getRoomFeatures()` and adds its own features, e.g. "Private balcony"), `getPricingNote()` and `getType()`.

**Overloading (compile-time polymorphism):**
- `Room.calculateStayCost(StayPeriod)` and `Room.calculateStayCost(LocalDate, LocalDate)`
- `ReservationService.quote(roomId, in, out, guests)` and `quote(roomId, in, out, guests, editingReservationId)`
- `ReservationService.create(customerId, roomId, in, out, guests)` and `create(ReservationForm)`
- `FormErrors.apply(result, error)` and `apply(result, error, fieldPrefix)`

## 4. Abstraction

**Idea:** show *what* an object does and hide *how* it does it.

- **Abstract class `Room`:** you cannot write `new Room(...)`, because a generic room has no pricing rule. It defines the contract (`getType()`, `calculateNightlyRate()`, `getPricingNote()`) and leaves the details to subclasses.
- **Abstract class `AbstractFileRepository<T>`:** defines the steps of storing objects in a file and leaves three "hooks" (`fileHeader()`, `toFields()`, `fromFields()`) abstract. This is the **Template Method** pattern.
- **Interface `Identifiable`:** anything with `getId()` can be stored by the generic repository.
- **Interface `DataRepository<T>`:** describes CRUD operations without saying anything about files.

## 5. Information hiding

**Idea:** other parts of the program should not depend on internal details that might change.

- `RoomService`, `CustomerService` and `ReservationService` receive `RoomRepository`, `CustomerRepository` and `ReservationRepository` **interfaces** in their constructors. They do not know that the data lives in text files. Replacing `FileRoomRepository` with, for example, a database repository would require no change to any service.
- Controllers never open files. They call service methods only (Controller → Service → Repository → File).
- The in-memory `Map` inside `AbstractFileRepository` is `private`; `findAll()` returns a **copy** (`new ArrayList<>(records.values())`).
- The text format, including escaping of `|`, is hidden inside `RecordCodec` and the three repository classes.
- The web layer uses separate form classes (`RoomForm`, `CustomerForm`, …), so HTML field names are not tied to the domain classes.

## 6. Other Java concepts used

| Concept | Example |
|---|---|
| **Constructors / constructor chaining** | `super(...)` in each room subclass; validated constructors in all models. |
| **Enums with behaviour** | `ReservationStatus.blocksAvailability()` decides whether a status occupies a room, so availability code never has to compare strings. `RoomType.getDisplayName()`. |
| **Interfaces & generics** | `DataRepository<T extends Identifiable>`, `AbstractFileRepository<T>`. |
| **Collections** | `LinkedHashMap` (keeps insertion order) for records, `List`, `Set` (session bookings), `Map<LocalDate, BigDecimal>` for the price breakdown; Streams for searching/filtering. |
| **Exception handling** | Custom hierarchy of unchecked (runtime) exceptions under `HotelException`; `try/catch` in controllers turns them into form messages; `GlobalControllerAdvice` turns not-found and storage errors into friendly pages; the file loader catches errors per line so one bad record does not stop the application. |
| **Composition** | `Reservation` *has a* `StayPeriod`; `BookingForm` *has a* `CustomerForm`; `Room` *owns* its amenities list. |
| **Association** | `Reservation` → `Customer` and `Reservation` → `Room` by ID. |
| **Immutability / value objects** | `StayPeriod` (with `equals`/`hashCode`), Java `record`s `ReservationDetails`, `BookingQuote`, `DashboardStats`. |
| **Static members** | Constants (`Room.MAX_CAPACITY`, `SuiteRoom.BUTLER_FEE_PER_NIGHT`), utility methods (`RecordCodec.encode`). |
| **Pattern matching (Java 21)** | `switch (room) { case StandardRoom s -> ...; case DeluxeRoom d -> ... }` in `FileRoomRepository.toFields()`. |
| **BigDecimal** | All money values, rounded with `setScale(2, RoundingMode.HALF_UP)`, avoiding floating-point errors like `0.1 + 0.2 = 0.30000000000000004`. |
| **java.time** | `LocalDate`, `ChronoUnit.DAYS.between(...)` for nights, `Clock` injected for testable "today". |
| **Dependency injection** | Every service and controller receives its dependencies through its constructor; Spring creates and connects the objects. |
| **Synchronisation** | Repository methods and the booking methods in `ReservationService` are `synchronized`, so two simultaneous requests cannot both book the last free nights of a room. |

## 7. Why there is no permanent `isBooked` flag

A room is not simply "booked" or "free"; it is free on some nights and taken on others. Availability is therefore **calculated** each time from the confirmed reservations for that room:

```java
public boolean isRoomAvailable(String roomId, StayPeriod stay, String ignoreReservationId) {
    return reservationRepository.findByRoomId(roomId).stream()
            .filter(r -> !r.getId().equals(ignoreReservationId))
            .noneMatch(r -> r.conflictsWith(roomId, stay));
}
```

`Reservation.conflictsWith()` returns `false` for cancelled reservations, which is why cancelling immediately frees the room. `StayPeriod.overlaps()` implements `newCheckIn < existingCheckOut && newCheckOut > existingCheckIn`.

# UML Class Diagrams

These diagrams are drawn from the source code in `src/main/java/com/hotel/reservation`. Every class, attribute and method shown exists in the code. Simple getters and setters are left out to keep the diagrams readable. They render on GitHub and in VS Code with a Mermaid preview extension.

Notation: `+` public, `-` private, `#` protected, `*` abstract, `$` static. A hollow triangle means inheritance or interface realisation; a filled diamond means composition.

---

## 1. Domain model (package `model`)

```mermaid
classDiagram
    direction TB

    class Identifiable {
        <<interface>>
        +getId() String
    }

    class Room {
        <<abstract>>
        +MAX_CAPACITY int$
        -id String
        -roomNumber String
        -name String
        -description String
        -basePrice BigDecimal
        -capacity int
        -amenities List~String~
        -imageUrl String
        -underMaintenance boolean
        #Room(id, roomNumber, name, description, basePrice, capacity, amenities, imageUrl, underMaintenance)
        +getType()* RoomType
        +calculateNightlyRate(LocalDate night)* BigDecimal
        +getPricingNote()* String
        +getRoomFeatures() List~String~
        +calculateStayCost(StayPeriod stay) BigDecimal
        +calculateStayCost(LocalDate checkIn, LocalDate checkOut) BigDecimal
        +getNightlyBreakdown(StayPeriod stay) Map~LocalDate,BigDecimal~
        +canAccommodate(int guests) boolean
        +isBookable() boolean
        +getAmenities() List~String~
        +setBasePrice(BigDecimal) void
        +setCapacity(int) void
        #isWeekendNight(LocalDate)$ boolean
        #money(BigDecimal)$ BigDecimal
        #requireText(String, String, int)$ String
    }

    class StandardRoom {
        -bedType String
        +getType() RoomType
        +calculateNightlyRate(LocalDate night) BigDecimal
        +getPricingNote() String
        +getRoomFeatures() List~String~
    }

    class DeluxeRoom {
        +WEEKEND_MULTIPLIER BigDecimal$
        -view String
        -balcony boolean
        +getType() RoomType
        +calculateNightlyRate(LocalDate night) BigDecimal
        +getPricingNote() String
        +getRoomFeatures() List~String~
        +hasBalcony() boolean
    }

    class SuiteRoom {
        +WEEKEND_MULTIPLIER BigDecimal$
        +BUTLER_FEE_PER_NIGHT BigDecimal$
        +MAX_BEDROOMS int$
        -bedrooms int
        -butlerService boolean
        +getType() RoomType
        +calculateNightlyRate(LocalDate night) BigDecimal
        +getPricingNote() String
        +getRoomFeatures() List~String~
        +hasButlerService() boolean
    }

    class RoomType {
        <<enumeration>>
        STANDARD
        DELUXE
        SUITE
        -displayName String
        +getDisplayName() String
    }

    class Customer {
        +EMAIL_PATTERN Pattern$
        +PHONE_PATTERN Pattern$
        -id String
        -fullName String
        -email String
        -phone String
        -address String
        +setEmail(String) void
        +setPhone(String) void
        +getInitials() String
    }

    class Reservation {
        -id String
        -customerId String
        -roomId String
        -stay StayPeriod
        -guests int
        -totalPrice BigDecimal
        -status ReservationStatus
        -createdAt LocalDateTime
        +updateBooking(String roomId, StayPeriod stay, int guests, BigDecimal total) void
        +cancel() void
        +isActive() boolean
        +conflictsWith(String roomId, StayPeriod stay) boolean
        +getNights() long
        -applyBookingDetails(String, StayPeriod, int, BigDecimal) void
    }

    class ReservationStatus {
        <<enumeration>>
        CONFIRMED
        CANCELLED
        -displayName String
        -blocksAvailability boolean
        +blocksAvailability() boolean
    }

    class StayPeriod {
        <<value object>>
        -checkIn LocalDate
        -checkOut LocalDate
        +getNights() long
        +overlaps(StayPeriod other) boolean
        +getNightDates() List~LocalDate~
    }

    Identifiable <|.. Room
    Identifiable <|.. Customer
    Identifiable <|.. Reservation
    Room <|-- StandardRoom
    Room <|-- DeluxeRoom
    Room <|-- SuiteRoom
    Room ..> RoomType : getType()
    Room ..> StayPeriod : prices
    Reservation *-- "1" StayPeriod : stay
    Reservation --> "1" ReservationStatus : status
    Reservation "0..*" --> "1" Customer : customerId
    Reservation "0..*" --> "1" Room : roomId
```

**How to read it**
- `Room` is abstract (italic or `<<abstract>>`). Its three abstract methods are implemented differently by each subclass. `calculateStayCost()` is a `final` method that calls `calculateNightlyRate()`, so the subclass decides the price at runtime.
- A `Reservation` **composes** a `StayPeriod`: the stay has no meaning outside its reservation.
- A `Reservation` is **associated** with one `Customer` and one `Room` through their IDs, which is how the records are linked in the text files. A customer or room can have many reservations.

---

## 2. Persistence layer (package `repository`)

```mermaid
classDiagram
    direction TB

    class DataRepository~T~ {
        <<interface>>
        +findAll() List~T~
        +findById(String id) Optional~T~
        +existsById(String id) boolean
        +save(T entity) T
        +deleteById(String id) boolean
        +count() long
        +nextId() String
    }
    class RoomRepository {
        <<interface>>
        +findByRoomNumber(String) Optional~Room~
    }
    class CustomerRepository {
        <<interface>>
        +findByEmail(String) Optional~Customer~
    }
    class ReservationRepository {
        <<interface>>
        +findByRoomId(String) List~Reservation~
        +findByCustomerId(String) List~Reservation~
    }

    class AbstractFileRepository~T~ {
        <<abstract>>
        -file Path
        -idPrefix String
        -idDigits int
        -records Map~String,T~
        -existedBeforeStartup boolean
        -skippedRecordCount int
        #fileHeader()* String
        #toFields(T entity)* List~String~
        #fromFields(String[] fields)* T
        +isFirstRun() boolean
        +getSkippedRecordCount() int
        -load() void
        -writeFile() void
        -backUpOriginalFile() void
        #requireFieldCount(String[], int)$ void
        #parseBoolean(String)$ boolean
    }

    class FileRoomRepository {
        #fileHeader() String
        #toFields(Room) List~String~
        #fromFields(String[]) Room
        +findByRoomNumber(String) Optional~Room~
    }
    class FileCustomerRepository {
        #fileHeader() String
        #toFields(Customer) List~String~
        #fromFields(String[]) Customer
        +findByEmail(String) Optional~Customer~
    }
    class FileReservationRepository {
        #fileHeader() String
        #toFields(Reservation) List~String~
        #fromFields(String[]) Reservation
        +findByRoomId(String) List~Reservation~
        +findByCustomerId(String) List~Reservation~
    }

    class RecordCodec {
        <<utility>>
        +encode(List~String~)$ String
        +decode(String)$ String[]
        +encodeList(List~String~)$ String
        +decodeList(String)$ List~String~
    }

    DataRepository <|-- RoomRepository
    DataRepository <|-- CustomerRepository
    DataRepository <|-- ReservationRepository
    DataRepository <|.. AbstractFileRepository
    AbstractFileRepository <|-- FileRoomRepository
    AbstractFileRepository <|-- FileCustomerRepository
    AbstractFileRepository <|-- FileReservationRepository
    RoomRepository <|.. FileRoomRepository
    CustomerRepository <|.. FileCustomerRepository
    ReservationRepository <|.. FileReservationRepository
    AbstractFileRepository ..> RecordCodec : uses
```

`AbstractFileRepository<T>` follows the **Template Method** pattern. It implements loading, saving, safe file replacement and ID generation once. Each subclass only supplies `fileHeader()`, `toFields()` and `fromFields()`.

---

## 3. Services, controllers and their dependencies

```mermaid
classDiagram
    direction LR

    class RoomService {
        -roomRepository RoomRepository
        -reservationRepository ReservationRepository
        +findAll() List~Room~
        +findById(String) Room
        +search(keyword, type, maxPrice, guests, sort) List~Room~
        +findFeatured() List~Room~
        +create(RoomForm) Room
        +update(String id, RoomForm) Room
        +delete(String id) void
        -buildRoom(String id, RoomForm) Room
        -ensureRoomNumberIsUnique(String, String) void
    }
    class CustomerService {
        -customerRepository CustomerRepository
        -reservationRepository ReservationRepository
        +findAll() List~Customer~
        +findById(String) Customer
        +search(String) List~Customer~
        +register(CustomerForm) Customer
        +update(String id, CustomerForm) Customer
        +findOrRegister(CustomerForm) Customer
        +delete(String id) void
    }
    class ReservationService {
        +MAX_NIGHTS int$
        -reservationRepository ReservationRepository
        -roomRepository RoomRepository
        -customerRepository CustomerRepository
        -customerService CustomerService
        -clock Clock
        +create(customerId, roomId, checkIn, checkOut, guests) Reservation
        +create(ReservationForm) Reservation
        +book(BookingForm) Reservation
        +update(String id, ReservationForm) Reservation
        +cancel(String id) Reservation
        +delete(String id) void
        +isRoomAvailable(roomId, StayPeriod, ignoreId) boolean
        +findAvailableRooms(rooms, checkIn, checkOut, guests) List~Room~
        +quote(roomId, checkIn, checkOut, guests) BookingQuote
        +quote(roomId, checkIn, checkOut, guests, editingId) BookingQuote
        +search(String, ReservationStatus) List~ReservationDetails~
        +findForGuest(reference, email) ReservationDetails
        +getStatistics() DashboardStats
        -checkBookingRules(Room, StayPeriod, int, String, boolean) void
        -toStay(LocalDate, LocalDate) StayPeriod
    }

    class HomeController
    class RoomController
    class BookingController
    class AdminDashboardController
    class AdminRoomController
    class AdminCustomerController
    class AdminReservationController

    HomeController --> RoomService
    HomeController --> ReservationService
    RoomController --> RoomService
    RoomController --> ReservationService
    BookingController --> RoomService
    BookingController --> ReservationService
    AdminDashboardController --> RoomService
    AdminDashboardController --> CustomerService
    AdminDashboardController --> ReservationService
    AdminRoomController --> RoomService
    AdminCustomerController --> CustomerService
    AdminCustomerController --> ReservationService
    AdminReservationController --> ReservationService
    AdminReservationController --> RoomService
    AdminReservationController --> CustomerService

    ReservationService --> CustomerService
    RoomService --> RoomRepository
    RoomService --> ReservationRepository
    CustomerService --> CustomerRepository
    CustomerService --> ReservationRepository
    ReservationService --> ReservationRepository
    ReservationService --> RoomRepository
    ReservationService --> CustomerRepository
```

All dependencies are passed in through constructors (constructor-based dependency injection). Services depend only on the repository **interfaces**, never on the `File...Repository` classes.

---

## 4. Form objects, view records and exceptions

```mermaid
classDiagram
    direction LR

    class RoomForm {
        -roomNumber String
        -name String
        -type RoomType
        -description String
        -basePrice BigDecimal
        -capacity Integer
        -amenities String
        -imageUrl String
        -underMaintenance boolean
        -bedType String
        -view String
        -balcony boolean
        -bedrooms Integer
        -butlerService boolean
        +fromRoom(Room)$ RoomForm
        +amenityList() List~String~
    }
    class CustomerForm {
        -fullName String
        -email String
        -phone String
        -address String
        +fromCustomer(Customer)$ CustomerForm
    }
    class ReservationForm {
        -customerId String
        -roomId String
        -checkIn LocalDate
        -checkOut LocalDate
        -guests Integer
        +fromReservation(Reservation)$ ReservationForm
    }
    class BookingForm {
        -roomId String
        -checkIn LocalDate
        -checkOut LocalDate
        -guests Integer
        -customer CustomerForm
    }
    class ReservationDetails {
        <<record>>
        reservation Reservation
        customer Customer
        room Room
    }
    class BookingQuote {
        <<record>>
        room Room
        stay StayPeriod
        guests int
        available boolean
        message String
        total BigDecimal
        nightlyRates Map~LocalDate,BigDecimal~
        +nights() long
    }
    class DashboardStats {
        <<record>>
        totalRooms long
        availableRooms long
        activeReservations long
        confirmedRevenue BigDecimal
        +occupancyPercent() int
    }
    BookingForm *-- CustomerForm

    class HotelException
    class ResourceNotFoundException
    class ValidationException {
        -field String
        +getField() String
    }
    class BookingConflictException
    class StorageException
    RuntimeException <|-- HotelException
    HotelException <|-- ResourceNotFoundException
    HotelException <|-- ValidationException
    HotelException <|-- BookingConflictException
    HotelException <|-- StorageException
```

`DashboardStats` has more numeric fields than shown (`roomsUnderMaintenance`, `totalCustomers`, `totalReservations`, `cancelledReservations`, `arrivalsToday`, `occupiedTonight`). They are omitted here for space.

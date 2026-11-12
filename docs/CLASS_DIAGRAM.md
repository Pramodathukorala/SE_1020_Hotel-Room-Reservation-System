# UML Class Diagrams

These diagrams are drawn from the source code in `src/main/java/com/hotel/reservation`. Every class, attribute and method shown exists in the code. Simple getters and setters are left out to keep the diagrams readable. They render on GitHub and in VS Code with a Mermaid preview extension.

Notation: `+` public, `-` private, `#` protected, `*` abstract, `$` static. A hollow triangle means inheritance or interface realisation; a filled diamond means composition.

---

## Room class hierarchy (image)

![Room class diagram](diagrams/room-class-diagram.png)

`Room` is abstract and implements `Identifiable`. `StandardRoom`, `DeluxeRoom` and `SuiteRoom` extend it and override `getType()`, `calculateNightlyRate()`, `getPricingNote()` and `getRoomFeatures()`, shown in gold. `Room` uses `RoomType` and `StayPeriod`. A vector version for the report is in `diagrams/room-class-diagram.svg`.

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

## 1b. Accounts and sign-in (packages `model`, `security`, `service`)

```mermaid
classDiagram
    direction TB

    class UserAccount {
        <<abstract>>
        -id String
        -passwordHash String
        -createdAt LocalDateTime
        +getRole()* Role
        +getHomePage()* String
        +passwordMatches(String raw, PasswordEncoder encoder) boolean
        +changePasswordHash(String hash) void
    }
    class AdminAccount {
        -username String
        -fullName String
        +create(id, username, fullName, rawPassword, encoder, createdAt)$ AdminAccount
        +getRole() Role
        +getHomePage() String
    }
    class GuestAccount {
        +getRole() Role
        +getHomePage() String
        +getCustomerId() String
    }
    class Role {
        <<enumeration>>
        ADMIN
        GUEST
    }
    class SessionUser {
        <<record>>
        accountId String
        displayName String
        role Role
        +isAdmin() boolean
        +firstName() String
    }
    class AccountService {
        +registerGuest(RegistrationForm) GuestAccount
        +authenticateGuest(String email, String password) SessionUser
        +authenticateAdmin(String username, String password) SessionUser
        +createAdmin(String username, String fullName, String password) AdminAccount
        -toSessionUser(UserAccount, String, String, String) SessionUser
    }
    class AuthInterceptor {
        +preHandle(request, response, handler) boolean
    }
    class CurrentUser {
        <<utility>>
        +admin(HttpSession)$ SessionUser
        +guest(HttpSession)$ SessionUser
        +signIn(HttpServletRequest, SessionUser)$ void
    }

    Identifiable <|.. UserAccount
    UserAccount <|-- AdminAccount
    UserAccount <|-- GuestAccount
    UserAccount ..> Role
    GuestAccount "0..1" --> "1" Customer : id = customer id
    AccountService ..> UserAccount : checks password
    AccountService ..> SessionUser : creates
    AuthInterceptor ..> CurrentUser : reads session
    CurrentUser ..> SessionUser
```

`AccountService` depends on `GuestAccountRepository`, `AdminRepository`, `CustomerRepository`, `CustomerService` and a `PasswordEncoder` (BCrypt). `JdbcAdminRepository` and `JdbcGuestAccountRepository` extend `AbstractJdbcRepository` exactly like the other repositories.

---

## 2. Persistence layer (package `repository`, MySQL via JDBC)

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

    class AbstractJdbcRepository~T~ {
        <<abstract>>
        #jdbc JdbcTemplate
        -tableName String
        -idPrefix String
        -idDigits int
        #mapRow(ResultSet rs)* T
        #insert(T entity)* void
        #update(T entity)* void
        #loadDetails(List~T~ entities) void
        #query(String sql, Object... parameters) List~T~
        +save(T entity) T
        +nextId() String
    }

    class JdbcRoomRepository {
        #mapRow(ResultSet) Room
        #insert(Room) void
        #update(Room) void
        #loadDetails(List~Room~) void
        +save(Room) Room
        +findByRoomNumber(String) Optional~Room~
        -saveAmenities(Room) void
        -typeSpecificValues(Room)$ Object[]
    }
    class JdbcCustomerRepository {
        #mapRow(ResultSet) Customer
        #insert(Customer) void
        #update(Customer) void
        +findByEmail(String) Optional~Customer~
    }
    class JdbcReservationRepository {
        #mapRow(ResultSet) Reservation
        #insert(Reservation) void
        #update(Reservation) void
        +findByRoomId(String) List~Reservation~
        +findByCustomerId(String) List~Reservation~
    }

    DataRepository <|-- RoomRepository
    DataRepository <|-- CustomerRepository
    DataRepository <|-- ReservationRepository
    DataRepository <|.. AbstractJdbcRepository
    AbstractJdbcRepository <|-- JdbcRoomRepository
    AbstractJdbcRepository <|-- JdbcCustomerRepository
    AbstractJdbcRepository <|-- JdbcReservationRepository
    RoomRepository <|.. JdbcRoomRepository
    CustomerRepository <|.. JdbcCustomerRepository
    ReservationRepository <|.. JdbcReservationRepository
```

`AbstractJdbcRepository<T>` follows the **Template Method** pattern. It implements `findAll`, `findById`, `existsById`, `save` (INSERT or UPDATE), `deleteById`, `count` and `nextId` once. Each subclass supplies only `mapRow()` (row → object), `insert()` and `update()`. `JdbcRoomRepository` also overrides `loadDetails()` to read amenities and `save()` to write them in the same transaction.

### Database tables (entity–relationship view)

```mermaid
erDiagram
    rooms ||--o{ room_amenities : "has"
    customers ||--o| guest_accounts : "may have a login"
    rooms ||--o{ reservations : "is booked in"
    customers ||--o{ reservations : "makes"

    rooms {
        VARCHAR id PK
        VARCHAR room_type "STANDARD, DELUXE or SUITE"
        VARCHAR room_number UK
        VARCHAR name
        VARCHAR description
        DECIMAL base_price
        INT capacity
        VARCHAR image_url
        BOOLEAN under_maintenance
        VARCHAR bed_type "Standard only"
        VARCHAR view_name "Deluxe only"
        BOOLEAN balcony "Deluxe only"
        INT bedrooms "Suite only"
        BOOLEAN butler_service "Suite only"
    }
    room_amenities {
        VARCHAR room_id PK,FK
        INT sort_order PK
        VARCHAR amenity
    }
    customers {
        VARCHAR id PK
        VARCHAR full_name
        VARCHAR email UK
        VARCHAR phone
        VARCHAR address
    }
    guest_accounts {
        VARCHAR id PK,FK "same as customers.id"
        VARCHAR password_hash "BCrypt"
        DATETIME created_at
    }
    admins {
        VARCHAR id PK
        VARCHAR username UK
        VARCHAR full_name
        VARCHAR password_hash "BCrypt"
        DATETIME created_at
    }
    reservations {
        VARCHAR id PK
        VARCHAR customer_id FK
        VARCHAR room_id FK
        DATE check_in
        DATE check_out
        INT guests
        DECIMAL total_price
        VARCHAR status
        DATETIME created_at
    }
```

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

All dependencies are passed in through constructors (constructor-based dependency injection). Services depend only on the repository **interfaces**, never on the `Jdbc...Repository` classes.

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
    RuntimeException <|-- HotelException
    HotelException <|-- ResourceNotFoundException
    HotelException <|-- ValidationException
    HotelException <|-- BookingConflictException
```

`DashboardStats` has more numeric fields than shown (`roomsUnderMaintenance`, `totalCustomers`, `totalReservations`, `cancelledReservations`, `arrivalsToday`, `occupiedTonight`). They are omitted here for space.

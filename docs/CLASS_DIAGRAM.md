# Implemented class diagram

```mermaid
classDiagram
    class HotelApplication {
        +main(String[] args)$ void
    }
    class Room {
        <<abstract>>
        -String id
        -String number
        -String description
        -int capacity
        -BigDecimal basePrice
        -boolean maintenance
        +calculateNightlyRate()* BigDecimal
        +getRoomFeatures()* List~String~
        +getCategory()* String
    }
    class StandardRoom {
        +calculateNightlyRate() BigDecimal
        +getRoomFeatures() List~String~
        +getCategory() String
    }
    class DeluxeRoom {
        +calculateNightlyRate() BigDecimal
        +getRoomFeatures() List~String~
        +getCategory() String
    }
    class SuiteRoom {
        +calculateNightlyRate() BigDecimal
        +getRoomFeatures() List~String~
        +getCategory() String
    }
    class Customer {
        -String id
        -String name
        -String email
        -String phone
    }
    class Reservation {
        -String id
        -String roomId
        -String customerId
        -LocalDate checkIn
        -LocalDate checkOut
        -int guests
        -BigDecimal total
        -boolean cancelled
    }
    class HotelService {
        <<interface>>
        +rooms() List~Room~
        +customers() List~Customer~
        +reservations() List~Reservation~
        +room(String id) Room
        +customer(String id) Customer
        +reservation(String id) Reservation
        +saveRoom(...) Room
        +saveCustomer(...) Customer
        +book(...) Reservation
        +available(...) boolean
        +cancel(String id) void
        +deleteRoom(String id) void
        +deleteCustomer(String id) void
        +deleteReservation(String id) void
    }
    class FileHotelService {
        -List~Room~ rooms
        -List~Customer~ customers
        -List~Reservation~ reservations
        -copy(Object value, Class type) Object
        -dates(LocalDate in, LocalDate out) void
    }
    class FileRepository~T~ {
        -Path file
        -ObjectMapper mapper
        -Class~T~ type
        -boolean damaged
        +readAll() List~T~
        +writeAll(Collection~T~ values) void
    }
    class HotelController {
        -HotelService hotel
        +home(Model model) String
        +rooms(...) String
        +book(...) String
    }
    class HotelException
    class StoredRecordValidator {
        +validate(Object value)$ void
    }
    class ErrorHandler {
        +invalid(Exception error, Model model, HttpServletResponse response) String
    }
    Room <|-- StandardRoom
    Room <|-- DeluxeRoom
    Room <|-- SuiteRoom
    HotelService <|.. FileHotelService
    HotelController --> HotelService : constructor injection
    FileHotelService *-- FileRepository : three typed instances
    FileHotelService o-- Room
    FileHotelService o-- Customer
    FileHotelService o-- Reservation
    Reservation "0..*" --> "1" Room : roomId
    Reservation "0..*" --> "1" Customer : customerId
    FileRepository ..> HotelException
    FileRepository ..> StoredRecordValidator : checks restored state
    FileHotelService ..> HotelException
    ErrorHandler ..> HotelException : renders validation errors
    HotelApplication ..> HotelController : Spring component discovery
```

The Room, Customer and Reservation classes also provide no-argument constructors, full constructors and getters/setters for their private state. Room's constructors are protected. Only representative controller methods are shown; the complete route inventory is in README. Reservation associations use stable IDs rather than nested copies, so guest edits appear in booking views without duplicating guest data. `FileRepository<T>` handles three entity types without repeating file I/O code.

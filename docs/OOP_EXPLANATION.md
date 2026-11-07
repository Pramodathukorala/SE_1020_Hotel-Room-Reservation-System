# Meaningful OOP in the Java backend

| Concept | Implemented location | Practical purpose |
| --- | --- | --- |
| Encapsulation | `model/Room.java`, `Customer.java`, `Reservation.java` | Private state with constructors and accessors. A caller interacts with models through methods. |
| Inheritance | `StandardRoom`, `DeluxeRoom`, `SuiteRoom extends Room` | All rooms share identity, number, capacity, description, base price and maintenance state. |
| Polymorphism | `FileHotelService.book()` calls `selected.calculateNightlyRate()` on `Room` | The actual subtype selects a 1.00, 1.20 or 1.50 multiplier. Room pages also call each subtype's feature method. |
| Abstraction | abstract `Room`, interface `HotelService` | Room defines the required behavior without a universal rate implementation. Controllers depend on a service contract, not a storage mechanism. |
| Information hiding | `FileRepository` and private service fields/helpers | Controllers do not know file paths or serialization; repositories do not make booking decisions. Detached return values protect service-owned state. |
| Association | `Reservation.roomId` and `customerId` | One reservation belongs to one room and customer; each may have many reservations. Services enforce references and protect deletions. |

`Room selected = room(roomId)` is a real superclass reference. When `selected` is a `SuiteRoom`, Java dispatches to `SuiteRoom.calculateNightlyRate()`. A three-night stay at a $100 base price is `100 × 1.50 × 3 = $450.00`. The subclass changes an actual booking result, rather than existing only for demonstration.

Getters/setters alone do not enforce the hotel's rules. They support serialization and form models. `FileHotelService` validates all application mutations, returns detached copies and only publishes state after the repository saves it. This distinction is important in a viva: encapsulation is access control and a clear boundary, not a claim that every setter validates every business rule.

The repository is generic because the read/write algorithm is identical for all three record types. It receives its path, mapper and Java class in a constructor. The Jackson subtype discriminator restores Room subclasses after restart. No database, ORM or Java native object serialization is used.

Constructor injection gives `HotelController` a `HotelService`, and Spring supplies `FileHotelService`. Tests instantiate the same service with temporary directories. This makes dependencies explicit and keeps business rules testable without HTTP.

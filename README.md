# Aurelia — Hotel Room Reservation System

SE1020 individual OOP project: a Spring Boot and Thymeleaf hotel application with TXT-file persistence. The visual identity uses warm ivory, forest charcoal, champagne gold and coastal photography.

## Run locally

Install **JDK 21** and **Maven 3.9+**. From this repository:

```powershell
mvn test
mvn spring-boot:run
```

Open **http://localhost:8080**. Alternatively, package and run:

```powershell
mvn package
java -jar target/reservation-1.0.0.jar
```

In VS Code, install Extension Pack for Java and Spring Boot Extension Pack, open this folder, and run `HotelApplication`. The Maven compiler targets Java 21. Development verification on this machine uses the available JDK 23 with Java 21 release compilation; a JDK 21 runtime is recommended for assessment. Maven downloads dependencies on the first run. `.mvn/maven.config` uses an ignored repository-local dependency cache to avoid this machine's unwritable default Maven path.

## Demonstration walkthrough

1. Visit the home page and explore the three initial Standard, Deluxe and Suite rooms.
2. Open Hotel desk → Rooms. Add a room, edit its details, view its page, then delete an unused room.
3. Register a guest with a valid email and phone. Open the guest directory, search, view and edit the profile.
4. Reserve a room for future dates; the quote updates in the browser and the server calculates the final price.
5. View and print the confirmation. Search reservations and edit the dates or guest count.
6. Try another reservation for overlapping dates: the application rejects it. A booking beginning on the previous booking's checkout date succeeds.
7. Cancel a booking and book its dates again. Cancelled reservations can be permanently deleted.
8. Restart the application. Room, guest and booking changes remain.

The seeded room base prices are $120, $180 and $280. Nightly prices are $120, $216 and $420 respectively. Deluxe adds 20%; Suite adds 50%. Two nights in the Standard room cost $240. Prices are USD; no tax or payment processing is implemented.

## Screens and routes

| Screen | Route |
| --- | --- |
| Home | `/` |
| Search / availability | `/rooms` |
| Room details | `/rooms/{id}` |
| Guest registration | `/customers/register` |
| Booking / booking update | `/book`, `/book?id={id}` |
| Confirmation | `/confirmation/{id}` |
| Reservation management | `/reservations` |
| Dashboard | `/admin` |
| Room management | `/admin/rooms` |
| Guest management / profile | `/admin/customers`, `/admin/customers/{id}` |

## Architecture and OOP

`Controller → HotelService → FileHotelService → FileRepository<T> → TXT files`.

`Room` is abstract; `StandardRoom`, `DeluxeRoom` and `SuiteRoom` override rate and feature methods. Booking uses a `Room` reference to calculate its subtype's nightly rate at runtime. Private model fields, constructors and accessors demonstrate encapsulation. Service methods return detached copies, so changes must pass validation and safe persistence. All dependencies use constructor injection. See [OOP explanation](docs/OOP_EXPLANATION.md), [actual class diagram](docs/CLASS_DIAGRAM.md), [viva guide](docs/VIVA_PREPARATION.md) and [project report](docs/PROJECT_REPORT.md).

## File storage

The application creates `data/rooms.txt`, `data/customers.txt` and `data/reservations.txt`. These UTF-8 TXT files contain one JSON record per line; JSON safely escapes punctuation and line breaks. Room records contain a `type` discriminator to restore their subclasses; this is not a database. Dates use `LocalDate`; prices use `BigDecimal`.

Files load at startup. Successful CRUD saves a full snapshot using a temporary file and atomic replacement, retaining a `.bak` of the previous file. The live in-memory snapshot changes only after the write succeeds. Malformed JSON, incomplete fields, missing/duplicate IDs and unknown room subtypes are logged and the affected file becomes read-only to avoid destroying original data. Missing cross-file references stop startup with a repair message. To recover: stop the application, copy files for safekeeping, repair the reported records or restore the `.bak`, then restart. Do not edit live files while the server runs. Manual corruption of additional business constraints requires manual repair; files are intended to be maintained through the UI.

UUIDs prevent sequence reuse. A service-wide synchronization lock makes overlap checking and booking atomic inside this JVM. **Run only one application process per data directory.** Files are not shared-process transactional storage. Change the directory using `--hotel.data-dir=another-directory`. The three sample rooms are created only if `rooms.txt` does not yet exist; deleting every room does not recreate samples. No customer personal data is seeded. Runtime files and backups are excluded from Git.

Room and customer deletion is blocked while any reservation references them, preserving history. First cancel and delete the associated reservations if removal is necessary. Maintenance and capacity changes cannot invalidate upcoming bookings. Editing a reservation re-quotes it at current rates; later room price changes alone do not alter existing totals.

## Verification

Run `mvn test` for service tests and full Thymeleaf/MockMvc request tests. They cover CRUD, validation, overlap shapes, adjacent bookings, cancellation, price calculations, restart persistence, subtype restoration, protected relationships, concurrent booking and failed writes. Tests use temporary storage, never live `data/`. See [verification record](docs/VERIFICATION.md) for the recorded execution result and limits.

For an optional real HTTP and full-process restart check, first package the JAR, then run `node scripts/smoke.cjs` (Node 18+). It uses an isolated directory under `target/`, starts its own JVM on a random local port, and stops it when finished. Node is not needed to run the application itself.

## Scope and assets

This is a **local university demonstration**, with no authentication, authorization, CSRF protection or payments. The hotel desk and guest records are accessible to local application visitors. Use fictional guest information. Do not expose it publicly without adding access control and appropriate data protection. The application binds to loopback by default.

Bootstrap 5, Google Fonts and Unsplash images load from external CDNs; core styling, navigation and form logic are local and remain usable if those resources are unavailable. Photo containers retain colored fallbacks; typography falls back to Georgia/Arial. Photographs are served by Unsplash under the [Unsplash license](https://unsplash.com/license). External services require internet access.

## Git

The existing Git repository is preserved. Review with `git status` and `git diff --stat`. No remote pushes are performed. Development checkpoints and actual validation are documented in [project report](docs/PROJECT_REPORT.md); they are not claims of historical commits or authorship.

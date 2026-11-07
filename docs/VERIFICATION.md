# Verification record

Execution date: **7 November 2026**, Asia/Colombo.

Environment: Windows 11, Maven 3.9.9, JDK 23.0.2. Maven compiled the application with `release 21`. An actual JDK 21 runtime was not installed, so runtime execution was verified with JDK 23.

## Automated Java tests

The final test execution during `mvn -o -q package -Ddebug=false` passed:

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| HotelServiceTest | 19 | 0 | 0 | 0 |
| WebTest | 2 | 0 | 0 | 0 |
| Total | 21 | 0 | 0 | 0 |

Raw reports are generated in `target/surefire-reports/` (ignored build artifacts).

Service coverage includes room/customer/reservation CRUD; polymorphic pricing; invalid dates; all overlap shapes; adjacent checkout/check-in; cancellation releasing availability; capacity, maintenance and missing references; duplicate room numbers and emails; guarded deletion; exact monetary totals; restart persistence; restored room subclasses; detached reads; absent-file initialization; no reseeding of deliberately empty storage; malformed/incomplete/duplicate records; failed writes retaining state; and two simultaneous booking requests.

Web coverage renders every major screen with Thymeleaf, submits a booking, checks confirmation prices, renders populated reservation management, edits a booking, cancels/deletes it and validates helpful error responses. A regression assertion checks that saved booking dates render in ISO format suitable for HTML date inputs.

## Build and startup

The first packaging attempt after those tests could not rename the JAR because the earlier preview JVM was still running it on Windows. After stopping that owned process, `mvn -o package -DskipTests -Ddebug=false` completed with **BUILD SUCCESS**. This separate packaging step deliberately reused the already-passed tests; it did not rerun them.

The executable JAR is `target/reservation-1.0.0.jar`. A real JVM started Spring Boot/Tomcat on port 8080, and HTTP GET `/` returned **200**, the expected home headline and three seeded room cards.

## Real HTTP and full-process restart

`node scripts/smoke.cjs` completed with exit code **0** and reported:

```text
PASS: real HTTP pages, room/customer/reservation CRUD, double-booking rejection, price calculation, cancellation and full process restart persistence.
```

The harness exercised the packaged JAR over HTTP with isolated storage: page/static asset responses; room creation/update; guest creation/update; a Deluxe booking at $120/night; overlap and unsafe deletion rejection; reservation update from $240 to $360; full JVM stop/restart; restored guest and quoted total; ISO edit dates; cancellation freeing availability; replacement booking; and final reservation/room/guest deletion. It stopped its JVM afterward. This goes beyond an in-memory service restart, while remaining separate from browser interaction testing.

## Issues found and corrected

- Maven's original cache location was unwritable; `.mvn/maven.config` uses an ignored cache in the repository.
- Network-restricted dependency downloads required tool execution approval; dependencies and packaging plugins were subsequently cached.
- A new regression assertion exposed localized saved dates in booking edit inputs. ISO `DateTimeFormat` annotations fixed the browser date values.
- JUnit's OS-temp cleanup hit sandbox access restrictions. Tests now create isolated temporary directories under ignored `target/`.
- The optional HTTP smoke harness needed to resolve the real JVM behind Windows `javapath` to reliably stop its child, and normalize absolute redirect URLs and servlet session path suffixes. These were harness issues, separate from application business rules.

## Verification limits

No browser-control or screenshot tool was available. Desktop/mobile rendering, hover effects, image delivery, menu interaction, JavaScript quote display, native validation prompts and print layout were implemented but not visually inspected in a real browser. Automated request/template tests and real HTTP requests do not replace that visual review.

The local demonstration has no authentication, authorization, CSRF protection or payment processing. Multi-process access to one data directory is unsupported. There was no public deployment or GitHub push.

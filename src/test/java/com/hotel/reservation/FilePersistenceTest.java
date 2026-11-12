package com.hotel.reservation;

import com.hotel.reservation.model.Customer;
import com.hotel.reservation.model.DeluxeRoom;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.ReservationStatus;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.SuiteRoom;
import com.hotel.reservation.repository.file.FileRoomRepository;
import com.hotel.reservation.util.RecordCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static com.hotel.reservation.TestHotel.day;
import static org.assertj.core.api.Assertions.assertThat;

/** Text-file persistence: restart, subclass reconstruction, escaping, missing and corrupted files. */
class FilePersistenceTest {

    @TempDir
    Path dataDir;

    @Test
    void startsEmptyWhenFilesDoNotExist() {
        TestHotel hotel = new TestHotel(dataDir);
        assertThat(hotel.roomRepository.count()).isZero();
        assertThat(hotel.roomRepository.isFirstRun()).isTrue();
        assertThat(Files.exists(dataDir.resolve("rooms.txt"))).isFalse();
    }

    @Test
    void dataSurvivesARestart() {
        TestHotel hotel = new TestHotel(dataDir);
        Room suite = hotel.roomService.create(TestHotel.suiteRoom("301", "40000", 4, true));
        Room deluxe = hotel.roomService.create(TestHotel.deluxeRoom("201", "20000", 3));
        Customer guest = hotel.customerService.register(TestHotel.customer("Nimal Perera", "nimal@example.com"));
        Reservation booking = hotel.reservationService.create(guest.getId(), suite.getId(), day(4), day(6), 3);
        Reservation cancelled = hotel.reservationService.create(guest.getId(), deluxe.getId(), day(1), day(3), 1);
        hotel.reservationService.cancel(cancelled.getId());

        TestHotel restarted = hotel.restart();

        // subclasses are rebuilt from the "type" column
        Room loadedSuite = restarted.roomService.findById(suite.getId());
        assertThat(loadedSuite).isInstanceOf(SuiteRoom.class);
        assertThat(((SuiteRoom) loadedSuite).hasButlerService()).isTrue();
        assertThat(((SuiteRoom) loadedSuite).getBedrooms()).isEqualTo(2);
        assertThat(restarted.roomService.findById(deluxe.getId())).isInstanceOf(DeluxeRoom.class);
        assertThat(loadedSuite.getAmenities()).containsExactly("Wi-Fi", "Air conditioning");

        Customer loadedGuest = restarted.customerService.findById(guest.getId());
        assertThat(loadedGuest.getEmail()).isEqualTo("nimal@example.com");

        Reservation loaded = restarted.reservationService.findById(booking.getId());
        assertThat(loaded.getCheckIn()).isEqualTo(day(4));
        assertThat(loaded.getCheckOut()).isEqualTo(day(6));
        assertThat(loaded.getGuests()).isEqualTo(3);
        assertThat(loaded.getTotalPrice()).isEqualByComparingTo(booking.getTotalPrice());
        assertThat(restarted.reservationService.findById(cancelled.getId()).getStatus()).isEqualTo(ReservationStatus.CANCELLED);

        // the restored reservation still blocks double booking, and IDs continue
        assertThat(restarted.reservationService.quote(suite.getId(), day(5), day(7), 2).available()).isFalse();
        assertThat(restarted.reservationRepository.nextId()).isEqualTo("RES-0003");
        assertThat(restarted.roomRepository.isFirstRun()).isFalse();
    }

    @Test
    void updatesAndDeletesArePersisted() {
        TestHotel hotel = new TestHotel(dataDir);
        Room a = hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        Room b = hotel.roomService.create(TestHotel.standardRoom("102", "15000", 2));
        hotel.roomService.update(a.getId(), TestHotel.standardRoom("101", "17000", 2));
        hotel.roomService.delete(b.getId());

        TestHotel restarted = hotel.restart();
        assertThat(restarted.roomService.findAll()).hasSize(1);
        assertThat(restarted.roomService.findById(a.getId()).getBasePrice()).isEqualByComparingTo("17000");
    }

    @Test
    void specialCharactersAreEscapedSafely() {
        TestHotel hotel = new TestHotel(dataDir);
        var form = TestHotel.standardRoom("101", "15000", 2);
        form.setName("Sea | Sky \\ Suite");
        form.setDescription("Line one\nLine two; with | pipes");
        form.setAmenities("Tea; coffee, Wi-Fi");
        Room room = hotel.roomService.create(form);

        Room loaded = hotel.restart().roomService.findById(room.getId());
        assertThat(loaded.getName()).isEqualTo("Sea | Sky \\ Suite");
        assertThat(loaded.getDescription()).isEqualTo("Line one\nLine two; with | pipes");
        assertThat(loaded.getAmenities()).containsExactly("Tea; coffee", "Wi-Fi");
    }

    @Test
    void codecRoundTrip() {
        List<String> fields = List.of("a|b", "c\\d", "", "line\nbreak", "plain");
        assertThat(RecordCodec.decode(RecordCodec.encode(fields))).containsExactlyElementsOf(fields);
        List<String> items = List.of("x;y", "z");
        assertThat(RecordCodec.decodeList(RecordCodec.encodeList(items))).containsExactlyElementsOf(items);
    }

    @Test
    void malformedLinesAreSkippedAndTheOriginalFileIsBackedUp() throws IOException {
        TestHotel hotel = new TestHotel(dataDir);
        hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        hotel.roomService.create(TestHotel.deluxeRoom("201", "20000", 2));

        Path file = dataDir.resolve("rooms.txt");
        Files.writeString(file, Files.readString(file)
                + "this line is not a room record\n"
                + "R009|PENTHOUSE|999|Bad|Bad|1|1||x|false||\n"         // unknown type
                + "R001|STANDARD|777|Duplicate|Dup|100|2||x|false|Queen|\n", // duplicate ID
                StandardCharsets.UTF_8);

        FileRoomRepository reloaded = new FileRoomRepository(dataDir.toString());

        assertThat(reloaded.count()).isEqualTo(2);
        assertThat(reloaded.getSkippedRecordCount()).isEqualTo(3);
        try (Stream<Path> files = Files.list(dataDir)) {
            assertThat(files.map(p -> p.getFileName().toString()))
                    .anyMatch(name -> name.startsWith("rooms.txt.corrupt-") && name.endsWith(".bak"));
        }
    }

    @Test
    void emptyFileLoadsWithoutErrors() throws IOException {
        Files.writeString(dataDir.resolve("customers.txt"), "", StandardCharsets.UTF_8);
        TestHotel hotel = new TestHotel(dataDir);
        assertThat(hotel.customerRepository.count()).isZero();
        assertThat(hotel.customerRepository.isFirstRun()).isFalse();
    }

    @Test
    void noTemporaryFileIsLeftBehind() throws IOException {
        TestHotel hotel = new TestHotel(dataDir);
        hotel.customerService.register(TestHotel.customer("Nimal Perera", "nimal@example.com"));
        try (Stream<Path> files = Files.list(dataDir)) {
            assertThat(files.map(p -> p.getFileName().toString())).noneMatch(name -> name.endsWith(".tmp"));
        }
    }
}

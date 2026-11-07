package com.hotel.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.model.*;
import com.hotel.reservation.repository.FileRepository;
import com.hotel.reservation.service.FileHotelService;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class HotelServiceTest {
    private Path directory;
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
    private FileHotelService hotel;
    private Room room;
    private Customer guest;
    private final LocalDate arrival = LocalDate.now().plusDays(10);
    @BeforeEach void setup() throws Exception {
        directory = Files.createTempDirectory(Path.of("target"), "hotel-test-");
        hotel = new FileHotelService(directory.toString(), mapper);
        room = hotel.rooms().getFirst();
        guest = hotel.saveCustomer(null,"Alex Silva","alex@example.com","+94 77 123 4567");
    }
    private Reservation book(LocalDate in, LocalDate out) { return hotel.book(null,room.getId(),guest.getId(),in,out,2); }
    @Test void roomCrudAndPolymorphism() {
        Room suite = hotel.saveRoom(null,"SUITE","402",4,new BigDecimal("100.00"),"Ocean suite",false);
        assertInstanceOf(SuiteRoom.class, suite); assertEquals(new BigDecimal("150.00"),suite.calculateNightlyRate());
        Room updated = hotel.saveRoom(suite.getId(),"DELUXE","403",3,new BigDecimal("100.00"),"Updated",false);
        assertInstanceOf(DeluxeRoom.class, updated); assertEquals(new BigDecimal("120.00"),updated.calculateNightlyRate());
        hotel.deleteRoom(suite.getId()); assertThrows(HotelException.class,()->hotel.room(suite.getId()));
    }
    @Test void customerCrud() {
        Customer updated = hotel.saveCustomer(guest.getId(),"Alex Fernando","new@example.com","0771234567");
        assertEquals("Alex Fernando",hotel.customer(updated.getId()).getName());
        hotel.deleteCustomer(updated.getId()); assertTrue(hotel.customers().isEmpty());
    }
    @Test void reservationCrudAndTotals() {
        Reservation booking = book(arrival,arrival.plusDays(3)); assertEquals(new BigDecimal("360.00"),booking.getTotal());
        Reservation updated = hotel.book(booking.getId(),room.getId(),guest.getId(),arrival,arrival.plusDays(2),1);
        assertEquals(new BigDecimal("240.00"),updated.getTotal()); assertEquals(1,hotel.reservations().size());
        hotel.cancel(booking.getId()); assertTrue(hotel.reservation(booking.getId()).isCancelled());
        hotel.deleteReservation(booking.getId()); assertTrue(hotel.reservations().isEmpty());
    }
    @Test void preventsAllOverlapShapes() {
        book(arrival,arrival.plusDays(3));
        for (LocalDate[] pair : List.of(new LocalDate[]{arrival,arrival.plusDays(3)},new LocalDate[]{arrival.plusDays(1),arrival.plusDays(2)},new LocalDate[]{arrival.minusDays(1),arrival.plusDays(4)},new LocalDate[]{arrival.minusDays(1),arrival.plusDays(1)},new LocalDate[]{arrival.plusDays(2),arrival.plusDays(4)}))
            assertThrows(HotelException.class,()->book(pair[0],pair[1]));
    }
    @Test void permitsAdjacentStays() {
        book(arrival,arrival.plusDays(2)); assertDoesNotThrow(()->book(arrival.plusDays(2),arrival.plusDays(4))); assertDoesNotThrow(()->book(arrival.minusDays(2),arrival));
    }
    @Test void cancellationReleasesRoomAndCannotBeEdited() {
        Reservation b=book(arrival,arrival.plusDays(2));hotel.cancel(b.getId());assertTrue(hotel.available(room.getId(),arrival,arrival.plusDays(2),null));
        assertThrows(HotelException.class,()->hotel.book(b.getId(),room.getId(),guest.getId(),arrival,arrival.plusDays(2),1));
        assertDoesNotThrow(()->book(arrival,arrival.plusDays(2)));
    }
    @Test void validatesDates() {
        assertThrows(HotelException.class,()->book(arrival,arrival)); assertThrows(HotelException.class,()->book(arrival,arrival.minusDays(1)));
        assertThrows(HotelException.class,()->book(LocalDate.now().minusDays(1),arrival)); assertThrows(HotelException.class,()->book(null,arrival));
    }
    @Test void validatesCapacityMaintenanceAndReferences() {
        assertThrows(HotelException.class,()->hotel.book(null,room.getId(),guest.getId(),arrival,arrival.plusDays(1),3));
        assertThrows(HotelException.class,()->hotel.book(null,room.getId(),"missing",arrival,arrival.plusDays(1),1));
        assertThrows(HotelException.class,()->hotel.book(null,"missing",guest.getId(),arrival,arrival.plusDays(1),1));
        hotel.saveRoom(room.getId(),"STANDARD",room.getNumber(),2,new BigDecimal("120"),"Maintenance",true);
        assertThrows(HotelException.class,()->book(arrival,arrival.plusDays(1)));
    }
    @Test void validatesRoomsAndDuplicateNumbers() {
        assertThrows(HotelException.class,()->hotel.saveRoom(null,"STANDARD",room.getNumber(),2,BigDecimal.TEN,"Duplicate",false));
        assertThrows(HotelException.class,()->hotel.saveRoom(null,"STANDARD","500",2,new BigDecimal("-1"),"Invalid",false));
        assertThrows(HotelException.class,()->hotel.saveRoom(null,"INVALID","500",2,BigDecimal.TEN,"Invalid",false));
    }
    @Test void validatesGuests() {
        assertThrows(HotelException.class,()->hotel.saveCustomer(null,"","bad","1"));
        assertThrows(HotelException.class,()->hotel.saveCustomer(null,"Valid Name","bad","0771234567"));
        assertThrows(HotelException.class,()->hotel.saveCustomer(null,"Valid Name","valid@example.com","abcdefg"));
        assertThrows(HotelException.class,()->hotel.saveCustomer(null,"Valid Name",guest.getEmail(),guest.getPhone()));
    }
    @Test void protectsReferencedRecordsAndActiveBookings() {
        Reservation b=book(arrival,arrival.plusDays(2));
        assertThrows(HotelException.class,()->hotel.deleteRoom(room.getId())); assertThrows(HotelException.class,()->hotel.deleteCustomer(guest.getId()));
        assertThrows(HotelException.class,()->hotel.deleteReservation(b.getId()));
        assertThrows(HotelException.class,()->hotel.saveRoom(room.getId(),"STANDARD",room.getNumber(),1,new BigDecimal("120"),"Reduced",false));
    }
    @Test void restartRestoresSubclassesGuestsBookingsAndPrice() {
        Reservation b=book(arrival,arrival.plusDays(2)); FileHotelService restarted=new FileHotelService(directory.toString(),mapper);
        assertInstanceOf(SuiteRoom.class,restarted.rooms().get(2)); assertEquals(guest.getName(),restarted.customer(guest.getId()).getName());
        assertEquals(new BigDecimal("240.00"),restarted.reservation(b.getId()).getTotal()); assertFalse(restarted.available(room.getId(),arrival,arrival.plusDays(1),null));
    }
    @Test void returnedObjectsCannotMutateStoredState() { hotel.room(room.getId()).setCapacity(999);assertEquals(2,hotel.room(room.getId()).getCapacity()); }
    @Test void noReseedingAfterAllRoomsDeleted() {for(Room r:hotel.rooms())hotel.deleteRoom(r.getId());assertTrue(new FileHotelService(directory.toString(),mapper).rooms().isEmpty());}
    @Test void malformedRecordsLockWritesWithoutDestroyingOriginal() throws Exception {
        Path file=directory.resolve("broken.txt");Files.writeString(file,"not json\n");FileRepository<Customer> store=new FileRepository<>(file,mapper,Customer.class);
        assertTrue(store.readAll().isEmpty());assertThrows(HotelException.class,()->store.writeAll(List.of(guest)));assertEquals("not json\n",Files.readString(file));
    }
    @Test void duplicateIdsAreDetectedAndPreserved() throws Exception {
        Path file=directory.resolve("duplicate.txt");String json=mapper.writeValueAsString(guest);Files.write(file,List.of(json,json));
        FileRepository<Customer> store=new FileRepository<>(file,mapper,Customer.class);assertEquals(1,store.readAll().size());assertThrows(HotelException.class,()->store.writeAll(List.of(guest)));
    }
    @Test void failedWritesDoNotChangeInMemoryState() throws Exception {
        Path file=directory.resolve("customers.txt");Files.delete(file);Files.createDirectory(file);Files.writeString(file.resolve("block"),"block");
        assertThrows(HotelException.class,()->hotel.saveCustomer(null,"Other Guest","other@example.com","0771234567"));assertEquals(1,hotel.customers().size());
    }
    @Test void incompleteJsonRecordIsSafelyRejected() throws Exception {
        Path file=directory.resolve("incomplete.txt");Files.writeString(file,"{\"id\":\"guest-id\"}\n");
        FileRepository<Customer> store=new FileRepository<>(file,mapper,Customer.class);
        assertTrue(store.readAll().isEmpty());assertThrows(HotelException.class,()->store.writeAll(List.of(guest)));
    }
    @Test void concurrentRequestsCannotDoubleBook() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {Callable<Boolean> task=()->{try{book(arrival,arrival.plusDays(2));return true;}catch(HotelException e){return false;}};
            List<Future<Boolean>> outcomes=pool.invokeAll(List.of(task,task));int successes=0;for(Future<Boolean> outcome:outcomes)if(outcome.get())successes++;assertEquals(1,successes);
        }finally{pool.shutdownNow();}
    }
}

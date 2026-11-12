package com.hotel.reservation.service;

import com.hotel.reservation.dto.BookingForm;
import com.hotel.reservation.dto.BookingQuote;
import com.hotel.reservation.dto.DashboardStats;
import com.hotel.reservation.dto.ReservationDetails;
import com.hotel.reservation.dto.ReservationForm;
import com.hotel.reservation.exception.BookingConflictException;
import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.exception.ValidationException;
import com.hotel.reservation.model.Customer;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.ReservationStatus;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.StayPeriod;
import com.hotel.reservation.repository.CustomerRepository;
import com.hotel.reservation.repository.ReservationRepository;
import com.hotel.reservation.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for reservations - the heart of the system.
 *
 * Booking rules enforced here:
 * <ol>
 *   <li>room and customer must exist;</li>
 *   <li>check-out must be after check-in, and check-in cannot be in the past;</li>
 *   <li>a stay is limited to {@value #MAX_NIGHTS} nights;</li>
 *   <li>the room must not be under maintenance;</li>
 *   <li>guests cannot exceed the room capacity;</li>
 *   <li>the stay must not overlap another CONFIRMED reservation for the same room
 *       (cancelled reservations never block a room).</li>
 * </ol>
 * Availability is always calculated from the reservations and dates - there is
 * no permanent "isBooked" flag that could become out of date.
 */
@Service
public class ReservationService {

    public static final int MAX_NIGHTS = 30;

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final Clock clock;   // injected so tests can control what "today" is

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository,
                              CustomerRepository customerRepository, CustomerService customerService, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.clock = clock;
    }

    // =================================================================== READ

    public List<Reservation> findAll() {
        return reservationRepository.findAll().stream()
                .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
                .toList();
    }

    public Reservation findById(String id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }

    public ReservationDetails getDetails(String id) {
        return toDetails(findById(id));
    }

    public List<ReservationDetails> findDetailsByCustomer(String customerId) {
        return reservationRepository.findByCustomerId(customerId).stream()
                .sorted(Comparator.comparing(Reservation::getCheckIn).reversed())
                .map(this::toDetails)
                .toList();
    }

    /** Search by reservation ID, customer ID/name/email or room number, optionally filtered by status. */
    public List<ReservationDetails> search(String keyword, ReservationStatus status) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return findAll().stream()
                .filter(r -> status == null || r.getStatus() == status)
                .map(this::toDetails)
                .filter(d -> needle.isEmpty() || matchesKeyword(d, needle))
                .toList();
    }

    /** Lets a guest look up their own booking with the reference and the email used to book. */
    public ReservationDetails findForGuest(String reference, String email) {
        if (reference == null || reference.isBlank() || email == null || email.isBlank()) {
            throw new ValidationException("Please enter both your booking reference and email address.");
        }
        ReservationDetails details = reservationRepository.findById(reference.trim().toUpperCase(Locale.ROOT))
                .map(this::toDetails)
                .orElse(null);
        if (details == null || details.customer() == null
                || !details.customer().getEmail().equalsIgnoreCase(email.trim())) {
            // same message in both cases so references cannot be guessed
            throw new ValidationException("No booking matches that reference and email address.");
        }
        return details;
    }

    // ============================================================ AVAILABILITY

    /**
     * True if no CONFIRMED reservation for this room overlaps the stay.
     * @param ignoreReservationId a reservation to ignore (the one being edited), or null
     */
    public boolean isRoomAvailable(String roomId, StayPeriod stay, String ignoreReservationId) {
        return reservationRepository.findByRoomId(roomId).stream()
                .filter(r -> !r.getId().equals(ignoreReservationId))
                .noneMatch(r -> r.conflictsWith(roomId, stay));
    }

    /** Rooms that are bookable, big enough and free for the whole stay. */
    public List<Room> findAvailableRooms(List<Room> candidates, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        StayPeriod stay = toStay(checkIn, checkOut);
        return candidates.stream()
                .filter(Room::isBookable)
                .filter(room -> guests == null || room.canAccommodate(guests))
                .filter(room -> isRoomAvailable(room.getId(), stay, null))
                .toList();
    }

    /**
     * Price and availability check used by the room details page and booking page.
     * Never throws for business-rule problems: it returns a quote with a message instead.
     */
    public BookingQuote quote(String roomId, LocalDate checkIn, LocalDate checkOut, int guests) {
        return quote(roomId, checkIn, checkOut, guests, null);
    }

    /**
     * Overload used when editing: the reservation being edited is ignored in the overlap check,
     * and its unchanged dates may already have started.
     */
    public BookingQuote quote(String roomId, LocalDate checkIn, LocalDate checkOut, int guests,
                              String editingReservationId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        StayPeriod originalStay = editingReservationId == null ? null
                : reservationRepository.findById(editingReservationId).map(Reservation::getStay).orElse(null);
        StayPeriod stay;
        try {
            stay = toStay(checkIn, checkOut);
        } catch (ValidationException e) {
            return new BookingQuote(room, null, guests, false, e.getMessage(), BigDecimal.ZERO, Map.of());
        }
        BigDecimal total = room.calculateStayCost(stay);              // polymorphic call
        Map<LocalDate, BigDecimal> nightly = room.getNightlyBreakdown(stay);
        try {
            checkBookingRules(room, stay, guests, editingReservationId, !stay.equals(originalStay));
            return new BookingQuote(room, stay, guests, true,
                    "Available for " + stay.getNights() + (stay.getNights() == 1 ? " night." : " nights."),
                    total, nightly);
        } catch (HotelException e) {
            return new BookingQuote(room, stay, guests, false, e.getMessage(), total, nightly);
        }
    }

    // ================================================================== CREATE

    /**
     * Creates a confirmed reservation. synchronized: two requests for the same room
     * cannot both pass the availability check before either is saved.
     */
    public synchronized Reservation create(String customerId, String roomId, LocalDate checkIn,
                                           LocalDate checkOut, int guests) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ValidationException("customerId", "Customer '" + customerId + "' does not exist."));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ValidationException("roomId", "Room '" + roomId + "' does not exist."));
        StayPeriod stay = toStay(checkIn, checkOut);
        checkBookingRules(room, stay, guests, null, true);

        Reservation reservation = new Reservation(reservationRepository.nextId(), customer.getId(), room.getId(),
                stay, guests, room.calculateStayCost(stay), ReservationStatus.CONFIRMED, LocalDateTime.now(clock));
        return reservationRepository.save(reservation);
    }

    public Reservation create(ReservationForm form) {
        return create(form.getCustomerId(), form.getRoomId(), form.getCheckIn(), form.getCheckOut(), nullToZero(form.getGuests()));
    }

    /**
     * Public booking: checks the stay first, then finds or registers the guest,
     * then books. Checking first avoids creating a customer for a booking that would fail.
     */
    public synchronized Reservation book(BookingForm form) {
        Room room = roomRepository.findById(form.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", form.getRoomId()));
        StayPeriod stay = toStay(form.getCheckIn(), form.getCheckOut());
        checkBookingRules(room, stay, nullToZero(form.getGuests()), null, true);
        Customer customer = customerService.findOrRegister(form.getCustomer());
        return create(customer.getId(), room.getId(), form.getCheckIn(), form.getCheckOut(), nullToZero(form.getGuests()));
    }

    // ================================================================== UPDATE

    /** Changes room, dates or guests of a confirmed reservation and recalculates the total. */
    public synchronized Reservation update(String id, ReservationForm form) {
        Reservation reservation = findById(id);
        if (!reservation.isActive()) {
            throw new ValidationException("Cancelled reservations cannot be edited.");
        }
        Room room = roomRepository.findById(form.getRoomId())
                .orElseThrow(() -> new ValidationException("roomId", "Room '" + form.getRoomId() + "' does not exist."));
        StayPeriod stay = toStay(form.getCheckIn(), form.getCheckOut());
        boolean datesChanged = !stay.equals(reservation.getStay());
        checkBookingRules(room, stay, nullToZero(form.getGuests()), reservation.getId(), datesChanged);

        reservation.updateBooking(room.getId(), stay, nullToZero(form.getGuests()), room.calculateStayCost(stay));
        return reservationRepository.save(reservation);
    }

    public synchronized Reservation cancel(String id) {
        Reservation reservation = findById(id);
        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new ValidationException(e.getMessage());
        }
        return reservationRepository.save(reservation);
    }

    // ================================================================== DELETE

    /** Only cancelled reservations may be removed permanently, so no active booking disappears by accident. */
    public synchronized void delete(String id) {
        Reservation reservation = findById(id);
        if (reservation.isActive()) {
            throw new ValidationException("Cancel reservation " + id + " before deleting it.");
        }
        reservationRepository.deleteById(id);
    }

    // =============================================================== DASHBOARD

    public DashboardStats getStatistics() {
        LocalDate today = LocalDate.now(clock);
        StayPeriod tonight = new StayPeriod(today, today.plusDays(1));
        List<Room> rooms = roomRepository.findAll();
        List<Reservation> reservations = reservationRepository.findAll();
        List<Reservation> active = reservations.stream().filter(Reservation::isActive).toList();

        long maintenance = rooms.stream().filter(Room::isUnderMaintenance).count();
        Set<String> occupiedRoomIds = active.stream()
                .filter(r -> r.getStay().overlaps(tonight))
                .map(Reservation::getRoomId)
                .collect(Collectors.toSet());
        long freeTonight = rooms.stream()
                .filter(room -> room.isBookable() && !occupiedRoomIds.contains(room.getId()))
                .count();
        long arrivalsToday = active.stream().filter(r -> r.getCheckIn().equals(today)).count();
        BigDecimal revenue = active.stream().map(Reservation::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        long occupiedTonight = occupiedRoomIds.size();

        return new DashboardStats(rooms.size(), freeTonight, maintenance,
                customerRepository.count(), reservations.size(), active.size(),
                reservations.size() - active.size(), arrivalsToday, occupiedTonight, revenue);
    }

    /** Confirmed stays that have not ended yet, soonest first. */
    public List<ReservationDetails> findUpcoming(int limit) {
        LocalDate today = LocalDate.now(clock);
        return reservationRepository.findAll().stream()
                .filter(Reservation::isActive)
                .filter(r -> r.getCheckOut().isAfter(today))
                .sorted(Comparator.comparing(Reservation::getCheckIn))
                .limit(limit)
                .map(this::toDetails)
                .toList();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // ================================================================= helpers

    /** Converts two dates into a StayPeriod, turning model errors into friendly form errors. */
    private StayPeriod toStay(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null) {
            throw new ValidationException("checkIn", "Please choose a check-in date.");
        }
        if (checkOut == null) {
            throw new ValidationException("checkOut", "Please choose a check-out date.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("checkOut", "Check-out date must be after the check-in date.");
        }
        StayPeriod stay = new StayPeriod(checkIn, checkOut);
        if (stay.getNights() > MAX_NIGHTS) {
            throw new ValidationException("checkOut", "A single stay can be at most " + MAX_NIGHTS + " nights.");
        }
        return stay;
    }

    private void checkBookingRules(Room room, StayPeriod stay, int guests, String ignoreReservationId,
                                   boolean checkInMustBeInFuture) {
        if (checkInMustBeInFuture && stay.getCheckIn().isBefore(LocalDate.now(clock))) {
            throw new ValidationException("checkIn", "Check-in date cannot be in the past.");
        }
        if (!room.isBookable()) {
            throw new ValidationException("roomId", "Room " + room.getRoomNumber() + " is under maintenance and cannot be booked.");
        }
        if (guests < 1) {
            throw new ValidationException("guests", "At least one guest is required.");
        }
        if (!room.canAccommodate(guests)) {
            throw new ValidationException("guests", "Room " + room.getRoomNumber() + " sleeps at most "
                    + room.getCapacity() + (room.getCapacity() == 1 ? " guest." : " guests."));
        }
        if (!isRoomAvailable(room.getId(), stay, ignoreReservationId)) {
            throw new BookingConflictException("Room " + room.getRoomNumber() + " is already booked for some of the nights between "
                    + stay.getCheckIn() + " and " + stay.getCheckOut() + ". Please choose other dates or another room.");
        }
    }

    private ReservationDetails toDetails(Reservation reservation) {
        return new ReservationDetails(reservation,
                customerRepository.findById(reservation.getCustomerId()).orElse(null),
                roomRepository.findById(reservation.getRoomId()).orElse(null));
    }

    private static boolean matchesKeyword(ReservationDetails d, String needle) {
        Reservation r = d.reservation();
        return r.getId().toLowerCase(Locale.ROOT).contains(needle)
                || r.getCustomerId().toLowerCase(Locale.ROOT).contains(needle)
                || (d.customer() != null && (d.customer().getFullName().toLowerCase(Locale.ROOT).contains(needle)
                    || d.customer().getEmail().contains(needle)))
                || (d.room() != null && d.room().getRoomNumber().toLowerCase(Locale.ROOT).contains(needle));
    }

    private static int nullToZero(Integer value) {
        return Objects.requireNonNullElse(value, 0);
    }
}

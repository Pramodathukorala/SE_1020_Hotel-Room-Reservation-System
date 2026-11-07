package com.hotel.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.model.*;
import com.hotel.reservation.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** All mutations share one lock, so availability checking and saving are one operation. */
@Service
public class FileHotelService implements HotelService {
    private final FileRepository<Room> roomStore;
    private final FileRepository<Customer> customerStore;
    private final FileRepository<Reservation> reservationStore;
    private final ObjectMapper mapper;
    private List<Room> rooms;
    private List<Customer> customers;
    private List<Reservation> reservations;

    public FileHotelService(@Value("${hotel.data-dir:data}") String directory, ObjectMapper mapper) {
        this.mapper = mapper;
        Path dir = Path.of(directory);
        roomStore = new FileRepository<>(dir.resolve("rooms.txt"), mapper, Room.class);
        customerStore = new FileRepository<>(dir.resolve("customers.txt"), mapper, Customer.class);
        reservationStore = new FileRepository<>(dir.resolve("reservations.txt"), mapper, Reservation.class);
        boolean firstRun = !java.nio.file.Files.exists(dir.resolve("rooms.txt"));
        rooms = roomStore.readAll(); customers = customerStore.readAll(); reservations = reservationStore.readAll();
        for (Reservation booking : reservations) {
            if (rooms.stream().noneMatch(r -> r.getId().equals(booking.getRoomId()))
                    || customers.stream().noneMatch(c -> c.getId().equals(booking.getCustomerId())))
                throw new HotelException("Stored reservation references a missing room or customer. Repair or restore the data files before restarting.");
        }
        if (firstRun) {
            saveRoom(null,"STANDARD","101",2,new BigDecimal("120.00"),"A quiet retreat framed by tropical gardens. Natural textures, soft linen and thoughtful comforts invite you to slow down.",false);
            saveRoom(null,"DELUXE","201",3,new BigDecimal("180.00"),"Wake to the Indian Ocean. Your private balcony, generous king bed and sunlit interiors make every morning extraordinary.",false);
            saveRoom(null,"SUITE","301",4,new BigDecimal("280.00"),"Room to unwind, space to dream. A separate living area and panoramic coastal views create an unforgettable escape.",false);
        }
    }
    // Return detached copies: callers cannot change stored objects without validation and persistence.
    private <T> T copy(T value, Class<T> type) { return mapper.convertValue(value, type); }
    @Override public synchronized List<Room> rooms() { return rooms.stream().map(r -> copy(r, Room.class)).toList(); }
    @Override public synchronized List<Customer> customers() { return customers.stream().map(c -> copy(c, Customer.class)).toList(); }
    @Override public synchronized List<Reservation> reservations() { return reservations.stream().map(r -> copy(r, Reservation.class)).toList(); }
    @Override public synchronized Room room(String id) { return rooms().stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow(() -> new HotelException("Room not found.")); }
    @Override public synchronized Customer customer(String id) { return customers().stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow(() -> new HotelException("Customer not found.")); }
    @Override public synchronized Reservation reservation(String id) { return reservations().stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow(() -> new HotelException("Reservation not found.")); }
    private String identifier(String id) { return id == null || id.isBlank() ? UUID.randomUUID().toString() : id; }
    private void require(boolean condition, String message) { if (!condition) throw new HotelException(message); }
    @Override public synchronized Room saveRoom(String id, String category, String number, int capacity, BigDecimal price, String description, boolean maintenance) {
        if (id != null && !id.isBlank()) room(id);
        require(number != null && number.trim().matches("[A-Za-z0-9-]{1,12}"), "Use a room number of 1–12 letters, numbers or hyphens.");
        require(capacity > 0 && capacity <= 20, "Room capacity must be between 1 and 20.");
        require(price != null && price.signum() > 0 && price.scale() <= 2 && price.compareTo(new BigDecimal("1000000")) <= 0, "Enter a positive base price with at most two decimal places (maximum 1,000,000).");
        require(description != null && !description.isBlank() && description.length() <= 2000, "Provide a description of up to 2,000 characters.");
        require(rooms.stream().noneMatch(r -> r.getNumber().equalsIgnoreCase(number.trim()) && !r.getId().equals(id)), "That room number already exists.");
        require(reservations.stream().noneMatch(r -> r.getRoomId().equals(id) && !r.isCancelled() && r.getCheckOut().isAfter(LocalDate.now()) && (maintenance || r.getGuests() > capacity)), "Existing bookings conflict with this capacity or maintenance change.");
        String key = identifier(id);
        Room value = switch (category == null ? "" : category) {
            case "STANDARD" -> new StandardRoom(key, number.trim(), capacity, price, description.trim(), maintenance);
            case "DELUXE" -> new DeluxeRoom(key, number.trim(), capacity, price, description.trim(), maintenance);
            case "SUITE" -> new SuiteRoom(key, number.trim(), capacity, price, description.trim(), maintenance);
            default -> throw new HotelException("Choose a valid room category.");
        };
        List<Room> next = new ArrayList<>(rooms); next.removeIf(r -> r.getId().equals(key)); next.add(value);
        roomStore.writeAll(next); rooms = next; return copy(value, Room.class);
    }
    @Override public synchronized Customer saveCustomer(String id, String name, String email, String phone) {
        if (id != null && !id.isBlank()) customer(id);
        require(name != null && name.trim().length() >= 2 && name.length() <= 100 && name.matches(".*\\p{L}.*"), "Enter a name of 2–100 characters.");
        require(email != null && email.length() <= 200 && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"), "Enter a valid email address.");
        require(phone != null && phone.matches("\\+?[0-9 ()-]{7,25}") && phone.replaceAll("\\D", "").length() >= 7 && phone.replaceAll("\\D", "").length() <= 15, "Enter a valid phone number with 7–15 digits.");
        require(customers.stream().noneMatch(c -> c.getEmail().equalsIgnoreCase(email.trim()) && !c.getId().equals(id)), "This email is already registered. Use the existing customer profile.");
        Customer value = new Customer(identifier(id), name.trim(), email.trim().toLowerCase(Locale.ROOT), phone.trim());
        List<Customer> next = new ArrayList<>(customers); next.removeIf(c -> c.getId().equals(value.getId())); next.add(value);
        customerStore.writeAll(next); customers = next; return copy(value, Customer.class);
    }
    private void dates(LocalDate in, LocalDate out) {
        require(in != null && out != null && out.isAfter(in), "Check-out must be after check-in.");
    }
    @Override public synchronized boolean available(String roomId, LocalDate in, LocalDate out, String excluded) {
        dates(in, out);
        return !room(roomId).isMaintenance() && reservations.stream().noneMatch(r -> !r.isCancelled() && r.getRoomId().equals(roomId) && !r.getId().equals(excluded) && in.isBefore(r.getCheckOut()) && out.isAfter(r.getCheckIn()));
    }
    @Override public synchronized Reservation book(String id, String roomId, String customerId, LocalDate in, LocalDate out, int guests) {
        Reservation existing = id == null || id.isBlank() ? null : reservation(id);
        require(existing == null || !existing.isCancelled(), "Cancelled reservations cannot be edited. Create a new booking.");
        dates(in, out); require(!in.isBefore(LocalDate.now()), "Check-in cannot be in the past.");
        Room selected = room(roomId); customer(customerId);
        require(guests > 0 && guests <= selected.getCapacity(), "Guest count exceeds this room's capacity or is invalid.");
        require(available(roomId, in, out, id), "This room is unavailable for those dates or under maintenance.");
        BigDecimal total = selected.calculateNightlyRate().multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(in, out)));
        Reservation value = new Reservation(identifier(id), roomId, customerId, in, out, guests, total, false);
        List<Reservation> next = new ArrayList<>(reservations); next.removeIf(r -> r.getId().equals(value.getId())); next.add(value);
        reservationStore.writeAll(next); reservations = next; return copy(value, Reservation.class);
    }
    @Override public synchronized void deleteRoom(String id) {
        room(id); require(reservations.stream().noneMatch(r -> r.getRoomId().equals(id)), "This room has booking history. Keep it for accurate records; mark it for maintenance when no future booking exists.");
        List<Room> next = new ArrayList<>(rooms); next.removeIf(r -> r.getId().equals(id)); roomStore.writeAll(next); rooms = next;
    }
    @Override public synchronized void deleteCustomer(String id) {
        customer(id); require(reservations.stream().noneMatch(r -> r.getCustomerId().equals(id)), "This customer has booking history and cannot be deleted.");
        List<Customer> next = new ArrayList<>(customers); next.removeIf(c -> c.getId().equals(id)); customerStore.writeAll(next); customers = next;
    }
    @Override public synchronized void cancel(String id) {
        Reservation value = reservation(id); value.setCancelled(true);
        List<Reservation> next = new ArrayList<>(reservations); next.replaceAll(r -> r.getId().equals(id) ? value : r); reservationStore.writeAll(next); reservations = next;
    }
    @Override public synchronized void deleteReservation(String id) {
        require(reservation(id).isCancelled(), "Cancel this reservation before deleting its record.");
        List<Reservation> next = new ArrayList<>(reservations); next.removeIf(r -> r.getId().equals(id)); reservationStore.writeAll(next); reservations = next;
    }
}

package com.hotel.reservation.service;

import com.hotel.reservation.dto.RoomForm;
import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.exception.ValidationException;
import com.hotel.reservation.model.DeluxeRoom;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.RoomType;
import com.hotel.reservation.model.StandardRoom;
import com.hotel.reservation.model.SuiteRoom;
import com.hotel.reservation.repository.ReservationRepository;
import com.hotel.reservation.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Business logic for rooms (CRUD + search).
 * Controllers call this class; this class calls the repository. Controllers never touch files.
 */
@Service
public class RoomService {

    /** Default ordering: Standard, Deluxe, Suite, then by room number. */
    private static final Comparator<Room> BY_TYPE_THEN_NUMBER =
            Comparator.comparing(Room::getType).thenComparing(Room::getRoomNumber);

    // OOP - ABSTRACTION: interfaces only - this class does not know the data is in MySQL
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    // Constructor-based dependency injection: Spring passes in the repository implementations
    public RoomService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    // ------------------------------------------------------------------ READ

    public List<Room> findAll() {
        return roomRepository.findAll().stream().sorted(BY_TYPE_THEN_NUMBER).toList();
    }

    public Room findById(String id) {
        return roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }

    public long count() {
        return roomRepository.count();
    }

    /**
     * Filters rooms by keyword (number/name/description/amenity), type, maximum price and guest count.
     * Any criterion that is null or blank is ignored.
     */
    public List<Room> search(String keyword, RoomType type, BigDecimal maxPrice, Integer guests, String sort) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return roomRepository.findAll().stream()
                .filter(room -> needle.isEmpty() || matchesKeyword(room, needle))
                .filter(room -> type == null || room.getType() == type)
                .filter(room -> maxPrice == null || room.getBasePrice().compareTo(maxPrice) <= 0)
                .filter(room -> guests == null || room.canAccommodate(guests))
                .sorted(comparatorFor(sort))
                .toList();
    }

    /** The cheapest bookable room of each type, used for the "featured rooms" section. */
    public List<Room> findFeatured() {
        List<Room> rooms = roomRepository.findAll();
        return Arrays.stream(RoomType.values())
                .map(type -> rooms.stream()
                        .filter(room -> room.getType() == type && room.isBookable())
                        .min(Comparator.comparing(Room::getBasePrice)))
                .flatMap(Optional::stream)
                .toList();
    }

    // ---------------------------------------------------------- CREATE / UPDATE

    public Room create(RoomForm form) {
        ensureRoomNumberIsUnique(form.getRoomNumber(), null);
        Room room = buildRoom(roomRepository.nextId(), form);
        return roomRepository.save(room);
    }

    /**
     * Rebuilds the room from the form (keeping its ID). Creating a fresh object instead of
     * changing the stored one means a validation error can never leave a half-updated room,
     * and it also allows the room type (and therefore the subclass) to change.
     */
    public Room update(String id, RoomForm form) {
        Room existing = findById(id);
        ensureRoomNumberIsUnique(form.getRoomNumber(), existing.getId());
        Room updated = buildRoom(existing.getId(), form);
        return roomRepository.save(updated);
    }

    // ------------------------------------------------------------------ DELETE

    /**
     * A room that appears in any reservation cannot be deleted, otherwise those
     * reservations would point to a room that no longer exists (referential integrity).
     * Such rooms can be marked "under maintenance" instead.
     */
    public void delete(String id) {
        Room room = findById(id);
        int linked = reservationRepository.findByRoomId(id).size();
        if (linked > 0) {
            throw new ValidationException("Room " + room.getRoomNumber() + " has " + linked
                    + " reservation(s) on record and cannot be deleted. Mark it as under maintenance instead.");
        }
        roomRepository.deleteById(id);
    }

    // ----------------------------------------------------------------- helpers

    /**
     * Creates the correct Room SUBCLASS for the chosen type.
     * OOP - POLYMORPHISM: the method returns the parent type "Room", but the real object
     * is a StandardRoom, DeluxeRoom or SuiteRoom. The rest of the program just uses "Room".
     */
    private Room buildRoom(String id, RoomForm form) {
        if (form.getType() == null) {
            throw new ValidationException("type", "Please choose a room type.");
        }
        try {
            return switch (form.getType()) {
                case STANDARD -> new StandardRoom(id, form.getRoomNumber(), form.getName(), form.getDescription(),
                        form.getBasePrice(), valueOf(form.getCapacity()), form.amenityList(), form.getImageUrl(),
                        form.isUnderMaintenance(), form.getBedType());
                case DELUXE -> new DeluxeRoom(id, form.getRoomNumber(), form.getName(), form.getDescription(),
                        form.getBasePrice(), valueOf(form.getCapacity()), form.amenityList(), form.getImageUrl(),
                        form.isUnderMaintenance(), form.getView(), form.isBalcony());
                case SUITE -> new SuiteRoom(id, form.getRoomNumber(), form.getName(), form.getDescription(),
                        form.getBasePrice(), valueOf(form.getCapacity()), form.amenityList(), form.getImageUrl(),
                        form.isUnderMaintenance(), valueOf(form.getBedrooms()), form.isButlerService());
            };
        } catch (IllegalArgumentException e) {
            // the model's own validation rejected a value - report it as a form error
            throw new ValidationException(e.getMessage());
        }
    }

    private void ensureRoomNumberIsUnique(String roomNumber, String ownId) {
        roomRepository.findByRoomNumber(roomNumber)
                .filter(other -> !other.getId().equals(ownId))
                .ifPresent(other -> {
                    throw new ValidationException("roomNumber", "Room number " + roomNumber.trim() + " already exists.");
                });
    }

    private static int valueOf(Integer number) {
        return number == null ? 0 : number;
    }

    private static boolean matchesKeyword(Room room, String needle) {
        return room.getRoomNumber().toLowerCase(Locale.ROOT).contains(needle)
                || room.getName().toLowerCase(Locale.ROOT).contains(needle)
                || room.getDescription().toLowerCase(Locale.ROOT).contains(needle)
                || room.getType().getDisplayName().toLowerCase(Locale.ROOT).contains(needle)
                || room.getAmenities().stream().anyMatch(a -> a.toLowerCase(Locale.ROOT).contains(needle));
    }

    private static Comparator<Room> comparatorFor(String sort) {
        if (sort == null) {
            return BY_TYPE_THEN_NUMBER;
        }
        return switch (sort) {
            case "price-asc" -> Comparator.comparing(Room::getBasePrice);
            case "price-desc" -> Comparator.comparing(Room::getBasePrice).reversed();
            case "capacity" -> Comparator.comparingInt(Room::getCapacity).reversed();
            default -> BY_TYPE_THEN_NUMBER;
        };
    }
}

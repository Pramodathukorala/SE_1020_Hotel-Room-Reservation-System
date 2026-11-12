package com.hotel.reservation.repository.jdbc;

import com.hotel.reservation.model.DeluxeRoom;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.RoomType;
import com.hotel.reservation.model.StandardRoom;
import com.hotel.reservation.model.SuiteRoom;
import com.hotel.reservation.repository.RoomRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores rooms in the "rooms" table and their amenities in "room_amenities".
 *
 * All three subclasses share one table. The room_type column tells {@link #mapRow}
 * which subclass to create, so a SuiteRoom saved to the database comes back as a SuiteRoom.
 */
@Repository
// OOP - INHERITANCE: extends AbstractJdbcRepository (reuses its SQL code)
// OOP - ABSTRACTION: implements the Room repository interface
public class JdbcRoomRepository extends AbstractJdbcRepository<Room> implements RoomRepository {

    public JdbcRoomRepository(JdbcTemplate jdbc) {
        super(jdbc, "rooms", "R", 3);
    }

    // ------------------------------------------------------------------ reading

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected Room mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        RoomType type = RoomType.valueOf(rs.getString("room_type"));
        String roomNumber = rs.getString("room_number");
        String name = rs.getString("name");
        String description = rs.getString("description");
        BigDecimal price = rs.getBigDecimal("base_price");
        int capacity = rs.getInt("capacity");
        String imageUrl = rs.getString("image_url");
        boolean maintenance = rs.getBoolean("under_maintenance");
        List<String> noAmenitiesYet = List.of();   // filled in by loadDetails()

        // The stored type decides which subclass object is created
        return switch (type) {
            case STANDARD -> new StandardRoom(id, roomNumber, name, description, price, capacity,
                    noAmenitiesYet, imageUrl, maintenance, rs.getString("bed_type"));
            case DELUXE -> new DeluxeRoom(id, roomNumber, name, description, price, capacity,
                    noAmenitiesYet, imageUrl, maintenance, rs.getString("view_name"), rs.getBoolean("balcony"));
            case SUITE -> new SuiteRoom(id, roomNumber, name, description, price, capacity,
                    noAmenitiesYet, imageUrl, maintenance, rs.getInt("bedrooms"), rs.getBoolean("butler_service"));
        };
    }

    /** Loads the amenities of all the given rooms with ONE extra query (not one query per room). */
    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void loadDetails(List<Room> rooms) {
        if (rooms.isEmpty()) {
            return;
        }
        Map<String, List<String>> amenitiesByRoom = new HashMap<>();
        jdbc.query("SELECT room_id, amenity FROM room_amenities ORDER BY room_id, sort_order", rs -> {
            amenitiesByRoom.computeIfAbsent(rs.getString("room_id"), key -> new ArrayList<>()).add(rs.getString("amenity"));
        });
        for (Room room : rooms) {
            room.setAmenities(amenitiesByRoom.getOrDefault(room.getId(), List.of()));
        }
    }

    @Override
    public Optional<Room> findByRoomNumber(String roomNumber) {
        if (roomNumber == null) {
            return Optional.empty();
        }
        return query("SELECT * FROM rooms WHERE LOWER(room_number) = LOWER(?)", roomNumber.trim()).stream().findFirst();
    }

    // ------------------------------------------------------------------ writing

    /** A room and its amenities are written in one transaction: either everything is saved or nothing is. */
    @Override
    @Transactional
    public Room save(Room room) {
        super.save(room);
        saveAmenities(room);
        return room;
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void insert(Room room) {
        Object[] typeColumns = typeSpecificValues(room);
        jdbc.update("""
                INSERT INTO rooms (id, room_type, room_number, name, description, base_price, capacity, image_url,
                                   under_maintenance, bed_type, view_name, balcony, bedrooms, butler_service)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                room.getId(), room.getType().name(), room.getRoomNumber(), room.getName(), room.getDescription(),
                room.getBasePrice(), room.getCapacity(), room.getImageUrl(), room.isUnderMaintenance(),
                typeColumns[0], typeColumns[1], typeColumns[2], typeColumns[3], typeColumns[4]);
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void update(Room room) {
        Object[] typeColumns = typeSpecificValues(room);
        jdbc.update("""
                UPDATE rooms SET room_type = ?, room_number = ?, name = ?, description = ?, base_price = ?,
                                 capacity = ?, image_url = ?, under_maintenance = ?, bed_type = ?, view_name = ?,
                                 balcony = ?, bedrooms = ?, butler_service = ?
                WHERE id = ?
                """,
                room.getType().name(), room.getRoomNumber(), room.getName(), room.getDescription(),
                room.getBasePrice(), room.getCapacity(), room.getImageUrl(), room.isUnderMaintenance(),
                typeColumns[0], typeColumns[1], typeColumns[2], typeColumns[3], typeColumns[4], room.getId());
    }

    /** Replaces the room's amenity rows, keeping their order with the "sort_order" column. */
    private void saveAmenities(Room room) {
        jdbc.update("DELETE FROM room_amenities WHERE room_id = ?", room.getId());
        List<String> amenities = room.getAmenities();
        for (int order = 0; order < amenities.size(); order++) {
            jdbc.update("INSERT INTO room_amenities (room_id, sort_order, amenity) VALUES (?, ?, ?)",
                    room.getId(), order, amenities.get(order));
        }
    }

    /**
     * Values for the columns bed_type, view_name, balcony, bedrooms, butler_service.
     * Java 21 pattern matching picks the right values for the actual subclass;
     * columns that do not apply to that type are stored as NULL.
     */
    private static Object[] typeSpecificValues(Room room) {
        return switch (room) {
            case StandardRoom s -> new Object[]{s.getBedType(), null, null, null, null};
            case DeluxeRoom d -> new Object[]{null, d.getView(), d.hasBalcony(), null, null};
            case SuiteRoom s -> new Object[]{null, null, null, s.getBedrooms(), s.hasButlerService()};
            default -> throw new IllegalArgumentException("Unknown room class " + room.getClass());
        };
    }
}

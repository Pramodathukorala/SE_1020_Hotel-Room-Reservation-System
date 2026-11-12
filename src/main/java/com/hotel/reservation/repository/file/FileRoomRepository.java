package com.hotel.reservation.repository.file;

import com.hotel.reservation.model.DeluxeRoom;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.RoomType;
import com.hotel.reservation.model.StandardRoom;
import com.hotel.reservation.model.SuiteRoom;
import com.hotel.reservation.repository.RoomRepository;
import com.hotel.reservation.util.RecordCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Stores rooms in data/rooms.txt.
 * The "type" column tells the loader which subclass to rebuild, so a SuiteRoom
 * saved to the file comes back as a SuiteRoom (not a plain Room) after a restart.
 */
@Repository
public class FileRoomRepository extends AbstractFileRepository<Room> implements RoomRepository {

    public FileRoomRepository(@Value("${hotel.storage.data-dir:data}") String dataDirectory) {
        super(Path.of(dataDirectory), "rooms.txt", "R", 3);
    }

    @Override
    protected String fileHeader() {
        return "# id|type|roomNumber|name|description|pricePerNight|capacity|amenities(;)|imageUrl|underMaintenance|"
                + "extra1|extra2   (STANDARD: bedType|-  DELUXE: view|balcony  SUITE: bedrooms|butlerService)";
    }

    @Override
    protected List<String> toFields(Room room) {
        // Java 21 pattern matching: pick the subclass-specific columns
        List<String> extra = switch (room) {
            case StandardRoom s -> List.of(s.getBedType(), "");
            case DeluxeRoom d -> List.of(d.getView(), String.valueOf(d.hasBalcony()));
            case SuiteRoom s -> List.of(String.valueOf(s.getBedrooms()), String.valueOf(s.hasButlerService()));
            default -> throw new IllegalArgumentException("Unknown room class " + room.getClass());
        };
        return List.of(
                room.getId(),
                room.getType().name(),
                room.getRoomNumber(),
                room.getName(),
                room.getDescription(),
                room.getBasePrice().toPlainString(),
                String.valueOf(room.getCapacity()),
                RecordCodec.encodeList(room.getAmenities()),
                room.getImageUrl(),
                String.valueOf(room.isUnderMaintenance()),
                extra.get(0),
                extra.get(1));
    }

    @Override
    protected Room fromFields(String[] f) {
        requireFieldCount(f, 12);
        String id = f[0];
        RoomType type = RoomType.valueOf(f[1]);
        String roomNumber = f[2];
        String name = f[3];
        String description = f[4];
        BigDecimal price = new BigDecimal(f[5]);
        int capacity = Integer.parseInt(f[6]);
        List<String> amenities = RecordCodec.decodeList(f[7]);
        String imageUrl = f[8];
        boolean maintenance = parseBoolean(f[9]);

        // The stored type decides which subclass object is created
        return switch (type) {
            case STANDARD -> new StandardRoom(id, roomNumber, name, description, price, capacity,
                    amenities, imageUrl, maintenance, f[10]);
            case DELUXE -> new DeluxeRoom(id, roomNumber, name, description, price, capacity,
                    amenities, imageUrl, maintenance, f[10], parseBoolean(f[11]));
            case SUITE -> new SuiteRoom(id, roomNumber, name, description, price, capacity,
                    amenities, imageUrl, maintenance, Integer.parseInt(f[10]), parseBoolean(f[11]));
        };
    }

    @Override
    public synchronized Optional<Room> findByRoomNumber(String roomNumber) {
        if (roomNumber == null) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(room -> room.getRoomNumber().equalsIgnoreCase(roomNumber.trim()))
                .findFirst();
    }
}

package com.hotel.reservation.repository.file;

import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.ReservationStatus;
import com.hotel.reservation.model.StayPeriod;
import com.hotel.reservation.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Stores reservations in data/reservations.txt. Dates use the ISO format yyyy-MM-dd. */
@Repository
public class FileReservationRepository extends AbstractFileRepository<Reservation> implements ReservationRepository {

    public FileReservationRepository(@Value("${hotel.storage.data-dir:data}") String dataDirectory) {
        super(Path.of(dataDirectory), "reservations.txt", "RES-", 4);
    }

    @Override
    protected String fileHeader() {
        return "# id|customerId|roomId|checkIn|checkOut|guests|totalPrice|status|createdAt";
    }

    @Override
    protected List<String> toFields(Reservation r) {
        return List.of(
                r.getId(),
                r.getCustomerId(),
                r.getRoomId(),
                r.getCheckIn().toString(),
                r.getCheckOut().toString(),
                String.valueOf(r.getGuests()),
                r.getTotalPrice().toPlainString(),
                r.getStatus().name(),
                r.getCreatedAt().toString());
    }

    @Override
    protected Reservation fromFields(String[] f) {
        requireFieldCount(f, 9);
        StayPeriod stay = new StayPeriod(LocalDate.parse(f[3]), LocalDate.parse(f[4]));
        return new Reservation(f[0], f[1], f[2], stay, Integer.parseInt(f[5]),
                new BigDecimal(f[6]), ReservationStatus.valueOf(f[7]), LocalDateTime.parse(f[8]));
    }

    @Override
    public synchronized List<Reservation> findByRoomId(String roomId) {
        return findAll().stream().filter(r -> r.getRoomId().equals(roomId)).toList();
    }

    @Override
    public synchronized List<Reservation> findByCustomerId(String customerId) {
        return findAll().stream().filter(r -> r.getCustomerId().equals(customerId)).toList();
    }
}

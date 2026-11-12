package com.hotel.reservation.repository.jdbc;

import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.ReservationStatus;
import com.hotel.reservation.model.StayPeriod;
import com.hotel.reservation.repository.ReservationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Stores reservations in the "reservations" table (DATE columns for the stay, DECIMAL for the price). */
@Repository
// OOP - INHERITANCE: extends AbstractJdbcRepository (reuses its SQL code)
// OOP - ABSTRACTION: implements the Reservation repository interface
public class JdbcReservationRepository extends AbstractJdbcRepository<Reservation> implements ReservationRepository {

    public JdbcReservationRepository(JdbcTemplate jdbc) {
        super(jdbc, "reservations", "RES-", 4);
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected Reservation mapRow(ResultSet rs) throws SQLException {
        StayPeriod stay = new StayPeriod(rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class));
        return new Reservation(rs.getString("id"), rs.getString("customer_id"), rs.getString("room_id"), stay,
                rs.getInt("guests"), rs.getBigDecimal("total_price"), ReservationStatus.valueOf(rs.getString("status")),
                rs.getObject("created_at", LocalDateTime.class));
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void insert(Reservation r) {
        jdbc.update("""
                INSERT INTO reservations (id, customer_id, room_id, check_in, check_out, guests, total_price, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                r.getId(), r.getCustomerId(), r.getRoomId(), r.getCheckIn(), r.getCheckOut(), r.getGuests(),
                r.getTotalPrice(), r.getStatus().name(), r.getCreatedAt());
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void update(Reservation r) {
        jdbc.update("""
                UPDATE reservations SET room_id = ?, check_in = ?, check_out = ?, guests = ?, total_price = ?, status = ?
                WHERE id = ?
                """,
                r.getRoomId(), r.getCheckIn(), r.getCheckOut(), r.getGuests(), r.getTotalPrice(),
                r.getStatus().name(), r.getId());
    }

    @Override
    public List<Reservation> findByRoomId(String roomId) {
        return query("SELECT * FROM reservations WHERE room_id = ? ORDER BY check_in", roomId);
    }

    @Override
    public List<Reservation> findByCustomerId(String customerId) {
        return query("SELECT * FROM reservations WHERE customer_id = ? ORDER BY check_in", customerId);
    }
}

package com.hotel.reservation.repository.jdbc;

import com.hotel.reservation.model.GuestAccount;
import com.hotel.reservation.repository.GuestAccountRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Stores guest logins in the "guest_accounts" table (id = customer id, password as a BCrypt hash). */
@Repository
// OOP - INHERITANCE: extends AbstractJdbcRepository (reuses its SQL code)
// OOP - ABSTRACTION: implements the GuestAccount repository interface
public class JdbcGuestAccountRepository extends AbstractJdbcRepository<GuestAccount> implements GuestAccountRepository {

    public JdbcGuestAccountRepository(JdbcTemplate jdbc) {
        super(jdbc, "guest_accounts", "C", 3);
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected GuestAccount mapRow(ResultSet rs) throws SQLException {
        return new GuestAccount(rs.getString("id"), rs.getString("password_hash"),
                rs.getObject("created_at", LocalDateTime.class));
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void insert(GuestAccount g) {
        jdbc.update("INSERT INTO guest_accounts (id, password_hash, created_at) VALUES (?, ?, ?)",
                g.getId(), g.getPasswordHash(), g.getCreatedAt());
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void update(GuestAccount g) {
        jdbc.update("UPDATE guest_accounts SET password_hash = ? WHERE id = ?", g.getPasswordHash(), g.getId());
    }
}

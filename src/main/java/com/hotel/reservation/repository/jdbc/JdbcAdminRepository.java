package com.hotel.reservation.repository.jdbc;

import com.hotel.reservation.model.AdminAccount;
import com.hotel.reservation.repository.AdminRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/** Stores admin logins in the "admins" table (password as a BCrypt hash). */
@Repository
// OOP - INHERITANCE: extends AbstractJdbcRepository (reuses its SQL code)
// OOP - ABSTRACTION: implements the AdminAccount repository interface
public class JdbcAdminRepository extends AbstractJdbcRepository<AdminAccount> implements AdminRepository {

    public JdbcAdminRepository(JdbcTemplate jdbc) {
        super(jdbc, "admins", "A", 3);
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected AdminAccount mapRow(ResultSet rs) throws SQLException {
        return new AdminAccount(rs.getString("id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("password_hash"), rs.getObject("created_at", LocalDateTime.class));
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void insert(AdminAccount a) {
        jdbc.update("INSERT INTO admins (id, username, full_name, password_hash, created_at) VALUES (?, ?, ?, ?, ?)",
                a.getId(), a.getUsername(), a.getFullName(), a.getPasswordHash(), a.getCreatedAt());
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void update(AdminAccount a) {
        jdbc.update("UPDATE admins SET username = ?, full_name = ?, password_hash = ? WHERE id = ?",
                a.getUsername(), a.getFullName(), a.getPasswordHash(), a.getId());
    }

    @Override
    public Optional<AdminAccount> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return query("SELECT * FROM admins WHERE username = ?", username.trim().toLowerCase(Locale.ROOT))
                .stream().findFirst();
    }
}

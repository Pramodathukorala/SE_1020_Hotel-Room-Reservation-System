package com.hotel.reservation.repository.jdbc;

import com.hotel.reservation.model.Customer;
import com.hotel.reservation.repository.CustomerRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

/** Stores customers in the "customers" table. */
@Repository
// OOP - INHERITANCE: extends AbstractJdbcRepository (reuses its SQL code)
// OOP - ABSTRACTION: implements the Customer repository interface
public class JdbcCustomerRepository extends AbstractJdbcRepository<Customer> implements CustomerRepository {

    public JdbcCustomerRepository(JdbcTemplate jdbc) {
        super(jdbc, "customers", "C", 3);
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(rs.getString("id"), rs.getString("full_name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("address"));
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void insert(Customer c) {
        jdbc.update("INSERT INTO customers (id, full_name, email, phone, address) VALUES (?, ?, ?, ?, ?)",
                c.getId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getAddress());
    }

    @Override   // OOP - POLYMORPHISM: this table's own version
    protected void update(Customer c) {
        jdbc.update("UPDATE customers SET full_name = ?, email = ?, phone = ?, address = ? WHERE id = ?",
                c.getFullName(), c.getEmail(), c.getPhone(), c.getAddress(), c.getId());
    }

    /** Emails are stored in lower case (see Customer.setEmail), so the search value is lower-cased too. */
    @Override
    public Optional<Customer> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return query("SELECT * FROM customers WHERE email = ?", email.trim().toLowerCase(Locale.ROOT))
                .stream().findFirst();
    }
}

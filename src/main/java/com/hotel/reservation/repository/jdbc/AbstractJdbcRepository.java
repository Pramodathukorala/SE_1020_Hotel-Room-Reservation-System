package com.hotel.reservation.repository.jdbc;

import com.hotel.reservation.model.Identifiable;
import com.hotel.reservation.repository.DataRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * The PARENT class of all database repositories.
 *
 * OOP - ABSTRACTION: abstract class with 3 abstract methods (mapRow, insert, update).
 * OOP - INHERITANCE: JdbcRoomRepository, JdbcCustomerRepository, JdbcReservationRepository,
 *       JdbcAdminRepository and JdbcGuestAccountRepository extend this class and REUSE
 *       findAll, findById, save, deleteById, count and nextId - written only once here.
 * OOP - POLYMORPHISM: save() calls insert()/update(), and query() calls mapRow() -
 *       each subclass runs its own version.
 *
 * Generic SQL storage for any entity type (Template Method pattern).
 *
 * The operations that are the same for every table - find all, find by ID, count,
 * delete and ID generation - are written once here. Each concrete repository only
 * supplies what is different for its table:
 * <ul>
 *   <li>{@link #mapRow} - how one result-set row becomes a Java object,</li>
 *   <li>{@link #insert} and {@link #update} - the SQL that writes one object.</li>
 * </ul>
 *
 * All SQL uses "?" parameters, never string concatenation of user input,
 * so values such as O'Brien are stored safely and SQL injection is impossible.
 */
public abstract class AbstractJdbcRepository<T extends Identifiable> implements DataRepository<T> {

    protected final JdbcTemplate jdbc;
    private final String tableName;
    private final String idPrefix;
    private final int idDigits;

    protected AbstractJdbcRepository(JdbcTemplate jdbc, String tableName, String idPrefix, int idDigits) {
        this.jdbc = jdbc;
        this.tableName = tableName;
        this.idPrefix = idPrefix;
        this.idDigits = idDigits;
    }

    // ---- OOP - ABSTRACTION: abstract "hook" methods - each subclass writes its own version ----

    /** Builds one object from the current row of the result set. */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    /** INSERT a new row for the entity. */
    protected abstract void insert(T entity);

    /** UPDATE the existing row for the entity. */
    protected abstract void update(T entity);

    /** Optional hook: load extra data (e.g. child rows) for objects that were just read. Default: nothing. */
    protected void loadDetails(List<T> entities) {
    }

    // ---- DataRepository implementation ----

    @Override
    public List<T> findAll() {
        return query("SELECT * FROM " + tableName + " ORDER BY id");
    }

    @Override
    public Optional<T> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return query("SELECT * FROM " + tableName + " WHERE id = ?", id).stream().findFirst();
    }

    @Override
    public boolean existsById(String id) {
        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE id = ?", Integer.class, id);
        return rows != null && rows > 0;
    }

    /** INSERT when the ID is new, otherwise UPDATE. OOP - POLYMORPHISM: runs the subclass's insert()/update(). */
    @Override
    public T save(T entity) {
        if (existsById(entity.getId())) {
            update(entity);
        } else {
            insert(entity);
        }
        return entity;
    }

    @Override
    public boolean deleteById(String id) {
        return jdbc.update("DELETE FROM " + tableName + " WHERE id = ?", id) > 0;
    }

    @Override
    public long count() {
        Long rows = jdbc.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return rows == null ? 0 : rows;
    }

    /** Finds the highest existing number for this prefix (e.g. R009) and returns the next one (R010). */
    @Override
    public synchronized String nextId() {
        List<String> ids = jdbc.queryForList("SELECT id FROM " + tableName + " WHERE id LIKE ?", String.class, idPrefix + "%");
        int highest = 0;
        for (String id : ids) {
            try {
                highest = Math.max(highest, Integer.parseInt(id.substring(idPrefix.length())));
            } catch (NumberFormatException ignored) {
                // an ID with a non-numeric suffix does not affect numbering
            }
        }
        return idPrefix + String.format("%0" + idDigits + "d", highest + 1);
    }

    /** Runs a SELECT, maps every row with {@link #mapRow}, then lets the subclass load extra details. */
    protected List<T> query(String sql, Object... parameters) {
        List<T> results = jdbc.query(sql, (rs, rowNumber) -> mapRow(rs), parameters);
        loadDetails(results);
        return results;
    }

    protected String getTableName() {
        return tableName;
    }
}

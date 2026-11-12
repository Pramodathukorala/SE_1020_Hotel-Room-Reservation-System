package com.hotel.reservation.repository.file;

import com.hotel.reservation.exception.StorageException;
import com.hotel.reservation.model.Identifiable;
import com.hotel.reservation.repository.DataRepository;
import com.hotel.reservation.util.RecordCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic, reusable file storage for any entity type (Template Method pattern).
 *
 * This class does the parts that are identical for rooms, customers and reservations:
 * reading the file, skipping bad lines, keeping records in memory, generating IDs and
 * writing the file back safely. Subclasses only decide how ONE object becomes a list of
 * text fields and back again ({@link #toFields} and {@link #fromFields}).
 *
 * How data is kept safe:
 * <ul>
 *   <li>All records are loaded into a LinkedHashMap when the application starts.</li>
 *   <li>Every create/update/delete rewrites the file immediately.</li>
 *   <li>The new content is first written to a temporary file, which then replaces the
 *       real file in one move - a crash mid-write cannot leave a half-written data file.</li>
 *   <li>Malformed lines are skipped (the app keeps running) and the original file is
 *       backed up before it is ever rewritten, so no data is silently lost.</li>
 * </ul>
 */
public abstract class AbstractFileRepository<T extends Identifiable> implements DataRepository<T> {

    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Path file;
    private final String idPrefix;
    private final int idDigits;
    private final Map<String, T> records = new LinkedHashMap<>();
    private final boolean existedBeforeStartup;
    private int skippedRecordCount;

    protected AbstractFileRepository(Path dataDirectory, String fileName, String idPrefix, int idDigits) {
        this.file = dataDirectory.resolve(fileName);
        this.idPrefix = idPrefix;
        this.idDigits = idDigits;
        this.existedBeforeStartup = Files.exists(file);
        load();
    }

    // ---- the two "hooks" each concrete repository must implement ----

    /** Header comment written at the top of the file to document the column order. */
    protected abstract String fileHeader();

    /** Converts one object to the ordered list of text fields stored in the file. */
    protected abstract List<String> toFields(T entity);

    /** Rebuilds one object from its text fields. Throws an exception if the fields are invalid. */
    protected abstract T fromFields(String[] fields);

    // ---- DataRepository implementation ----

    @Override
    public synchronized List<T> findAll() {
        return new ArrayList<>(records.values());
    }

    @Override
    public synchronized Optional<T> findById(String id) {
        return Optional.ofNullable(id == null ? null : records.get(id));
    }

    @Override
    public synchronized boolean existsById(String id) {
        return id != null && records.containsKey(id);
    }

    @Override
    public synchronized T save(T entity) {
        T previous = records.put(entity.getId(), entity);
        try {
            writeFile();
        } catch (StorageException e) {
            // keep memory consistent with the file if the write failed
            if (previous == null) {
                records.remove(entity.getId());
            } else {
                records.put(entity.getId(), previous);
            }
            throw e;
        }
        return entity;
    }

    @Override
    public synchronized boolean deleteById(String id) {
        T removed = records.remove(id);
        if (removed == null) {
            return false;
        }
        try {
            writeFile();
        } catch (StorageException e) {
            records.put(id, removed);
            throw e;
        }
        return true;
    }

    @Override
    public synchronized long count() {
        return records.size();
    }

    @Override
    public synchronized String nextId() {
        int highest = 0;
        for (String id : records.keySet()) {
            if (id.startsWith(idPrefix)) {
                try {
                    highest = Math.max(highest, Integer.parseInt(id.substring(idPrefix.length())));
                } catch (NumberFormatException ignored) {
                    // an ID with a non-numeric suffix does not affect numbering
                }
            }
        }
        return idPrefix + String.format("%0" + idDigits + "d", highest + 1);
    }

    /** True if the data file did not exist when the app started (used to seed sample data once). */
    public boolean isFirstRun() {
        return !existedBeforeStartup;
    }

    public int getSkippedRecordCount() {
        return skippedRecordCount;
    }

    public Path getFile() {
        return file;
    }

    // ---- file reading and writing ----

    private void load() {
        if (!Files.exists(file)) {
            log.info("{} does not exist yet - starting with no records.", file);
            return;
        }
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;   // empty lines and comments are ignored
                }
                try {
                    T entity = fromFields(RecordCodec.decode(line));
                    if (records.containsKey(entity.getId())) {
                        throw new IllegalArgumentException("duplicate ID " + entity.getId());
                    }
                    records.put(entity.getId(), entity);
                } catch (RuntimeException e) {
                    skippedRecordCount++;
                    log.warn("Skipping malformed record in {} at line {}: {}", file.getFileName(), lineNumber, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new StorageException("Could not read data file " + file, e);
        }
        if (skippedRecordCount > 0) {
            backUpOriginalFile();
        }
        log.info("Loaded {} record(s) from {}", records.size(), file);
    }

    private void backUpOriginalFile() {
        Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + LocalDateTime.now().format(BACKUP_STAMP) + ".bak");
        try {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            log.warn("{} malformed record(s) found. Original file backed up to {}", skippedRecordCount, backup);
        } catch (IOException e) {
            log.error("Could not back up {}", file, e);
        }
    }

    private void writeFile() {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                writer.write(fileHeader());
                writer.newLine();
                for (T entity : records.values()) {
                    writer.write(RecordCodec.encode(toFields(entity)));
                    writer.newLine();
                }
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Could not save data to " + file, e);
        }
    }

    // ---- small parsing helpers shared by subclasses ----

    protected static void requireFieldCount(String[] fields, int expected) {
        if (fields.length != expected) {
            throw new IllegalArgumentException("expected " + expected + " fields but found " + fields.length);
        }
    }

    protected static boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("invalid boolean '" + value + "'");
    }
}

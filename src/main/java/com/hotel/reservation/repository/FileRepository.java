package com.hotel.reservation.repository;
import com.fasterxml.jackson.databind.*;
import com.hotel.reservation.exception.HotelException;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import org.slf4j.LoggerFactory;
import com.hotel.reservation.util.StoredRecordValidator;
/** One JSON record per TXT line. Atomic replacement and backup protect committed data. */
public class FileRepository<T> {
    private final Path file;
    private final ObjectMapper mapper;
    private final Class<T> type;
    private boolean damaged;

    public FileRepository(Path file, ObjectMapper mapper, Class<T> type) {
        this.file = file;
        this.mapper = mapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.type = type;
    }

    public synchronized List<T> readAll() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            if (!Files.exists(file)) Files.createFile(file);
            List<T> result = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            int line = 0;
            for (String text : Files.readAllLines(file)) {
                line++;
                if (text.isBlank()) continue;
                try {
                    T value = mapper.readValue(text, type);
                    StoredRecordValidator.validate(value);
                    String id = (String) type.getMethod("getId").invoke(value);
                    if (!ids.add(id)) throw new IllegalArgumentException("Duplicate ID");
                    result.add(value);
                } catch (Exception error) {
                    damaged = true;
                    LoggerFactory.getLogger(FileRepository.class).error(
                            "Malformed record in {} at line {}; writes locked to preserve original data", file, line);
                }
            }
            return result;
        } catch (IOException error) {
            throw new HotelException("Cannot read hotel data. Check storage permissions.", error);
        }
    }

    public synchronized void writeAll(Collection<T> values) {
        if (damaged) throw new HotelException("Storage contains malformed records. Restore or repair the TXT file before making changes; original records are preserved.");
        Path temporary = null;
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "hotel-", ".tmp");
            List<String> lines = new ArrayList<>();
            for (T value : values) lines.add(mapper.writerFor(type).writeValueAsString(value));
            Files.write(temporary, lines);
            if (Files.exists(file)) Files.copy(file, Path.of(file + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            // Fail safely on filesystems without atomic replacement support.
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new HotelException("Could not save changes safely. Your previous data has been preserved.", error);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* Target is intact. */ }
        }
    }
}

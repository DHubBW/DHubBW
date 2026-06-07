package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.domain.FileStorageStrategy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory file storage for the dev profile (H2).
 *
 * <p>Since H2 does not support PostgreSQL Large Objects, this
 * implementation stores file bytes in a simple map and returns
 * a synthetic reference ID. In dev mode, file data is also kept
 * directly on the {@code Datei.data} byte[] field via JPA/H2,
 * so this strategy is primarily used for consistency with the
 * production interface contract.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (storage adapter)
 */
@Component
@Profile("dev")
public final class ByteArrayFileStorage implements FileStorageStrategy {

    /** In-memory store mapping synthetic OIDs to file bytes. */
    private final Map<Long, byte[]> storage =
        new ConcurrentHashMap<>();
    /** Sequence generator for synthetic reference IDs. */
    private final AtomicLong sequence = new AtomicLong(1);

    @Override
    public long store(final byte[] data) {
        long id = sequence.getAndIncrement();
        storage.put(id, data);
        return id;
    }

    @Override
    public byte[] retrieve(final long reference) {
        byte[] data = storage.get(reference);
        if (data == null) {
            throw new IllegalArgumentException(
                "No data found for reference: " + reference);
        }
        return data;
    }

    @Override
    public void delete(final long reference) {
        storage.remove(reference);
    }

    @Override
    public boolean requiresInlineData() {
        return true;
    }
}

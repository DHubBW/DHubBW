package com.se.dhubbw.datei.domain;

/**
 * Abstraction for binary file storage.
 *
 * <p>Implementations differ by environment: in development (H2) files
 * are stored as {@code byte[]} in-memory; in production (PostgreSQL)
 * files are stored as Large Objects for streaming efficiency.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (core abstraction)
 */
public interface FileStorageStrategy {

    /**
     * Stores binary data and returns a reference identifier.
     *
     * @param data the file bytes to store
     * @return a storage reference (OID as long for Large Objects,
     *     or a synthetic ID for in-memory storage)
     */
    long store(byte[] data);

    /**
     * Retrieves binary data by its storage reference.
     *
     * @param reference the storage reference returned by
     *     {@link #store(byte[])}
     * @return the file bytes
     */
    byte[] retrieve(long reference);

    /**
     * Deletes binary data by its storage reference.
     *
     * @param reference the storage reference to delete
     */
    void delete(long reference);

    /**
     * Whether file bytes should also be stored inline on the
     * entity's {@code data} field. Returns {@code true} for
     * environments without external object storage (e.g. H2).
     *
     * @return true if inline byte[] storage is needed
     */
    default boolean requiresInlineData() {
        return false;
    }
}

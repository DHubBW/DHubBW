package com.se.dhubbw.datei.infrastructure;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production integration test for {@link LargeObjectFileStorage}.
 *
 * <p>Starts a real PostgreSQL container via Testcontainers and
 * verifies that file store, retrieve, and delete operations work
 * correctly against the PostgreSQL Large Object API. This ensures
 * that the deployment will function as expected in production.</p>
 *
 * <p>This test does NOT boot the full Spring context. It directly
 * instantiates {@link LargeObjectFileStorage} with a DataSource
 * pointing to the Testcontainer, wrapped in explicit transactions
 * (as required by the PostgreSQL Large Object API).</p>
 */
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LargeObjectFileStorageIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    private static LargeObjectFileStorage storage;
    private static TransactionTemplate txTemplate;

    @BeforeAll
    static void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUsername(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");

        storage = new LargeObjectFileStorage(ds);

        DataSourceTransactionManager txManager =
            new DataSourceTransactionManager(ds);
        txTemplate = new TransactionTemplate(txManager);
    }

    @Test
    @Order(1)
    public void givenSmallFile_whenStoreAndRetrieve_thenDataMatches() {
        byte[] original = "Hello DHubBW!".getBytes(
            StandardCharsets.UTF_8);

        byte[] retrieved = txTemplate.execute(status -> {
            long oid = storage.store(original);
            return storage.retrieve(oid);
        });

        assertArrayEquals(original, retrieved);
    }

    @Test
    @Order(2)
    public void givenLargeFile_whenStoreAndRetrieve_thenDataMatches() {
        byte[] original = new byte[1024 * 1024];
        Arrays.fill(original, (byte) 0xAB);

        byte[] retrieved = txTemplate.execute(status -> {
            long oid = storage.store(original);
            return storage.retrieve(oid);
        });

        assertArrayEquals(original, retrieved);
    }

    @Test
    @Order(3)
    public void givenEmptyFile_whenStoreAndRetrieve_thenDataMatches() {
        byte[] original = new byte[0];

        byte[] retrieved = txTemplate.execute(status -> {
            long oid = storage.store(original);
            return storage.retrieve(oid);
        });

        assertArrayEquals(original, retrieved);
    }

    @Test
    @Order(4)
    public void givenStoredFile_whenDelete_thenRetrieveFails() {
        byte[] original = "to be deleted"
            .getBytes(StandardCharsets.UTF_8);

        long oid = txTemplate.execute(status ->
            storage.store(original));

        // Verify it was stored
        assertDoesNotThrow(() ->
            txTemplate.execute(status ->
                storage.retrieve(oid)));

        // Delete it
        txTemplate.executeWithoutResult(status ->
            storage.delete(oid));

        // After delete, retrieve should throw
        assertThrows(Exception.class, () ->
            txTemplate.execute(status ->
                storage.retrieve(oid)));
    }

    @Test
    @Order(5)
    public void givenMultipleFiles_whenStored_thenDistinctOids() {
        byte[] file1 = "file one".getBytes(StandardCharsets.UTF_8);
        byte[] file2 = "file two".getBytes(StandardCharsets.UTF_8);

        long[] oids = txTemplate.execute(status -> {
            long oid1 = storage.store(file1);
            long oid2 = storage.store(file2);
            return new long[]{oid1, oid2};
        });

        assertNotEquals(oids[0], oids[1]);

        byte[] retrieved1 = txTemplate.execute(status ->
            storage.retrieve(oids[0]));
        byte[] retrieved2 = txTemplate.execute(status ->
            storage.retrieve(oids[1]));

        assertArrayEquals(file1, retrieved1);
        assertArrayEquals(file2, retrieved2);
    }

    @Test
    @Order(6)
    public void givenBinaryContent_whenStoreAndRetrieve_thenIntact() {
        byte[] original = new byte[256];
        for (int i = 0; i < 256; i++) {
            original[i] = (byte) i;
        }

        byte[] retrieved = txTemplate.execute(status -> {
            long oid = storage.store(original);
            return storage.retrieve(oid);
        });

        assertArrayEquals(original, retrieved);
    }

    @Test
    @Order(7)
    public void givenLargeObjectStorage_whenChecked_thenNoInlineData() {
        assertTrue(!storage.requiresInlineData(),
            "LargeObjectFileStorage should NOT require inline data");
    }
}

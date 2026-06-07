package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.domain.FileStorageStrategy;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * PostgreSQL Large Object storage for the prod profile.
 *
 * <p>Uses the PostgreSQL {@link LargeObjectManager} API to store
 * and retrieve binary file data as Large Objects. Large Objects
 * live in the {@code pg_largeobject} system table and are
 * referenced by an OID value stored in the {@code datei.data_oid}
 * column.</p>
 *
 * <p>Advantages over BYTEA:</p>
 * <ul>
 *   <li>Streaming support — no need to load entire file into
 *       heap memory</li>
 *   <li>Better performance for files &gt; 1 MB</li>
 *   <li>Independent vacuum and storage management</li>
 * </ul>
 *
 * <p><strong>Important:</strong> All Large Object operations
 * MUST run within an existing transaction managed by Spring.
 * The calling service method must be annotated with
 * {@code @Transactional} or {@code @RetryableTransactional}.
 * This class participates in the same transaction via
 * {@link DataSourceUtils}.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (storage adapter)
 */
@Component
@Profile("prod")
public final class LargeObjectFileStorage implements FileStorageStrategy {

    /** The application data source for obtaining transactional connections. */
    private final DataSource dataSource;
    /** Logger instance for structured error reporting. */
    private final ColorLogger colorLogger =
        new ColorLogger(LargeObjectFileStorage.class);

    /**
     * Constructs a new LargeObjectFileStorage.
     *
     * @param ds the application data source
     */
    public LargeObjectFileStorage(final DataSource ds) {
        this.dataSource = ds;
    }

    @Override
    public long store(final byte[] data) {
        try {
            Connection conn = getTransactionalConnection();
            LargeObjectManager lom = conn
                .unwrap(PGConnection.class)
                .getLargeObjectAPI();
            long oid = lom.createLO(
                LargeObjectManager.READ
                    | LargeObjectManager.WRITE);
            LargeObject lo = lom.open(oid,
                LargeObjectManager.WRITE);
            lo.write(data);
            lo.close();
            return oid;
        } catch (SQLException ex) {
            colorLogger.colorError(
                "Failed to store Large Object: {}",
                ex.getMessage(), ex);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Fehler beim Speichern der Datei");
        }
    }

    @Override
    public byte[] retrieve(final long reference) {
        try {
            Connection conn = getTransactionalConnection();
            LargeObjectManager lom = conn
                .unwrap(PGConnection.class)
                .getLargeObjectAPI();
            LargeObject lo = lom.open(reference,
                LargeObjectManager.READ);
            byte[] data = lo.read(lo.size());
            lo.close();
            return data;
        } catch (SQLException ex) {
            colorLogger.colorError(
                "Failed to retrieve Large Object {}: {}",
                reference, ex.getMessage(), ex);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Fehler beim Lesen der Datei");
        }
    }

    @Override
    public void delete(final long reference) {
        try {
            Connection conn = getTransactionalConnection();
            LargeObjectManager lom = conn
                .unwrap(PGConnection.class)
                .getLargeObjectAPI();
            lom.delete(reference);
        } catch (SQLException ex) {
            colorLogger.colorError(
                "Failed to delete Large Object {}: {}",
                reference, ex.getMessage(), ex);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Fehler beim Loeschen der Datei");
        }
    }

    /**
     * Obtains the JDBC connection bound to the current Spring
     * transaction. This ensures Large Object operations
     * participate in the same transaction as the calling service.
     *
     * @return the transactional connection
     */
    private Connection getTransactionalConnection() {
        return DataSourceUtils.getConnection(dataSource);
    }
}

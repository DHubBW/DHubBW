package com.se.dhubbw.backup.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.se.dhubbw.backup.application.dto.output.BackupDTO;
import com.se.dhubbw.backup.domain.BackupService;
import com.se.dhubbw.backup.infrastructure.db.Backup;
import com.se.dhubbw.backup.infrastructure.db.BackupRepository;
import com.se.dhubbw.datei.domain.FileStorageStrategy;
import com.se.dhubbw.datei.infrastructure.db.Bewertung;
import com.se.dhubbw.datei.infrastructure.db.BewertungRepository;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegung;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.datei.infrastructure.db.Kommentar;
import com.se.dhubbw.datei.infrastructure.db.KommentarRepository;
import com.se.dhubbw.datei.infrastructure.db.Tag;
import com.se.dhubbw.datei.infrastructure.db.TagRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursRepository;
import com.se.dhubbw.ordner.domain.OrdnerFarbe;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegung;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegungRepository;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.shared.infrastructure.config.retry.RetryableTransactional;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link BackupService}.
 *
 * <p>A backup is a complete JSON snapshot of every backup-owned
 * row in the file-management tables: {@link Tag} (with author and
 * creation timestamp), {@link Ordner} (with self-referential
 * parent and {@link Kurs} link), {@link Datei} (including the raw
 * bytes Base64-encoded plus an SHA-256 checksum for integrity),
 * the {@code datei_tag} M:N rows, {@link Bewertung}, {@link
 * Kommentar}, {@link DateiAnlegung}, and {@link OrdnerAnlegung}.
 * {@link Student} and {@link Kurs} themselves are not snapshotted
 * — restore references them by UUID and degrades gracefully when
 * a referenced row no longer exists (sets nullable FKs to
 * {@code null}, skips rows whose mandatory FK target is
 * gone).</p>
 *
 * <p>Restore re-creates rows that no longer exist (the original
 * bug — "die gelöschte Datei erscheint nicht wieder") and updates
 * rows whose primary keys are still present. The order is
 * carefully chosen so every FK target is in place before the
 * referencing row is written:</p>
 *
 * <ol>
 *   <li>Pre-flight validation: schema-version, intra-snapshot
 *       uniqueness, Kahn topological sort over {@link Ordner}
 *       self-FK with cycle detection, SHA-256 verification.</li>
 *   <li>Tag upsert with name-collision merge (existing tag with
 *       same canonical name wins; subsequent {@code datei_tag}
 *       rows are rewired to the live UUID).</li>
 *   <li>Ordner upsert in topological order (parents first); a
 *       missing parent or missing kurs cascades into a skip set
 *       so descendants are not silently linked to {@code null}
 *       parents.</li>
 *   <li>Datei upsert: bytes are decoded, hashed and re-stored via
 *       {@link FileStorageStrategy#store(byte[])}; old large
 *       objects are tracked for post-commit cleanup.</li>
 *   <li>{@code datei_tag} rebuild: the snapshot's links replace
 *       the live links for every restored {@link Datei}.</li>
 *   <li>{@link Bewertung} upsert (collisions on {@code (datei,
 *       autor)} are resolved snapshot-wins).</li>
 *   <li>{@link Kommentar} upsert.</li>
 *   <li>Anlegung upserts (one-to-one collisions are resolved by
 *       deleting the colliding live row first).</li>
 *   <li>Aggregate recompute: {@code Datei.bewertungSumme} and
 *       {@code Datei.bewertungAnzahl} are derived from the
 *       restored {@link Bewertung} rows so the cached counters
 *       are internally consistent even when some Bewertungen
 *       were skipped.</li>
 * </ol>
 *
 * <p>The whole restore runs inside a single
 * {@link RetryableTransactional} so any failure rolls back to a
 * clean state.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with JPA access)
 */
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    /**
     * Minimum number of confirmations required before a restore
     * is executed. The admin UI requires the user to confirm twice.
     */
    public static final int MIN_CONFIRMATION_COUNT = 2;

    /**
     * Snapshot schema version. Bumped on every breaking change to
     * {@link #captureSnapshot} / {@link #applyRestoreV2}.
     *
     * <ul>
     *   <li>v1 (legacy): only aggregate counters were written —
     *       restore could not re-create deleted rows.</li>
     *   <li>v2 (this implementation): full row capture including
     *       Datei bytes (Base64) plus SHA-256, Ordner topology,
     *       {@code datei_tag} M:N, Tag with ersteller, Anlegung
     *       records.</li>
     * </ul>
     */
    public static final int SNAPSHOT_SCHEMA_VERSION = 2;

    /**
     * Default retention window (days). Backups older than this are
     * removed by {@link #pruneExpired()}. Configurable via
     * {@code dhubbw.backup.retention-days}.
     */
    private static final int DEFAULT_RETENTION_DAYS = 14;

    /**
     * Repository for backup persistence.
     */
    private final BackupRepository backupRepository;

    /**
     * Repository for folder lookup during snapshot capture.
     */
    private final OrdnerRepository ordnerRepository;

    /**
     * Repository for file lookup during snapshot capture.
     */
    private final DateiRepository dateiRepository;

    /**
     * Repository for comment lookup during snapshot capture.
     */
    private final KommentarRepository kommentarRepository;

    /**
     * Repository for rating lookup during snapshot capture.
     */
    private final BewertungRepository bewertungRepository;

    /**
     * Repository for tag lookup during snapshot capture.
     */
    private final TagRepository tagRepository;

    /**
     * Repository for file-creation tracking records.
     */
    private final DateiAnlegungRepository dateiAnlegungRepository;

    /**
     * Repository for folder-creation tracking records.
     */
    private final OrdnerAnlegungRepository ordnerAnlegungRepository;

    /**
     * Student lookup — read-only; never touched by the restore
     * itself, only used to resolve referenced authors / uploaders.
     */
    private final StudentRepository studentRepository;

    /**
     * Course lookup — read-only; same role as
     * {@link #studentRepository}.
     */
    private final KursRepository kursRepository;

    /**
     * File-storage strategy injected per profile (in-memory map
     * for DEV, PostgreSQL Large Object on PROD). Used during
     * restore to materialise the bytes carried in the snapshot.
     */
    private final FileStorageStrategy fileStorageStrategy;

    /**
     * Color-coded logger for error reporting.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(BackupServiceImpl.class);

    /**
     * Retention window in days — backups older than this many days
     * are removed by {@link #pruneExpired()}. Defaults to 14 days
     * (operators can override via the
     * {@code dhubbw.backup.retention-days} property without
     * recompiling).
     */
    @Value("${dhubbw.backup.retention-days:" + DEFAULT_RETENTION_DAYS + "}")
    private int retentionDays;

    /**
     * Jackson mapper preconfigured for {@link LocalDateTime}
     * serialisation.
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        // Serialise LocalDate / LocalDateTime as ISO-8601 strings
        // ("2026-04-25" / "2026-04-25T13:45:00") rather than the
        // default array-of-numbers shape ([2026, 4, 25]). The
        // restore parser reads the field back via
        // LocalDate.parse / LocalDateTime.parse, which only
        // accepts ISO strings — without this configuration every
        // restored Datei would land with zuletztBearbeitet=null.
        .disable(com.fasterxml.jackson.databind.SerializationFeature
            .WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Entity manager used to flush between restore stages so each
     * stage observes the rows written by the previous one. Spring
     * injects the transaction-scoped manager via
     * {@link PersistenceContext}.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Capture a full snapshot of every backup-owned table and
     * persist it as a new {@link Backup} row.
     *
     * @param quelle the source of the run (typically
     *     {@code "AUTO"} or {@code "MANUAL"})
     * @return the metadata DTO of the created backup
     */
    @Override
    @RetryableTransactional
    public BackupDTO createBackup(final String quelle) {
        Map<String, Object> snapshot = captureSnapshot();
        String json;
        try {
            json = objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            ResponseStatusException rse = new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Backup konnte nicht serialisiert werden");
            colorLogger.colorError(
                "Backup serialise failed: {}", ex.getMessage(), rse);
            throw rse;
        }

        Backup b = new Backup();
        b.setErstelltAm(LocalDateTime.now());
        b.setQuelle(quelle == null ? "AUTO" : quelle);
        b.setOrdnerAnzahl(((List<?>) snapshot.get("ordner")).size());
        b.setDateiAnzahl(((List<?>) snapshot.get("dateien")).size());
        b.setKommentarAnzahl(
            ((List<?>) snapshot.get("kommentare")).size());
        b.setBewertungAnzahl(
            ((List<?>) snapshot.get("bewertungen")).size());
        b.setTagAnzahl(((List<?>) snapshot.get("tags")).size());
        b.setPayload(json);
        Backup saved = backupRepository.save(b);
        colorLogger.colorInfo(
            "Backup {} created ({}) v{}: "
                + "{} ordner, {} datei, {} kommentar, "
                + "{} bewertung, {} tag",
            saved.getBackupID(), saved.getQuelle(),
            SNAPSHOT_SCHEMA_VERSION,
            saved.getOrdnerAnzahl(), saved.getDateiAnzahl(),
            saved.getKommentarAnzahl(), saved.getBewertungAnzahl(),
            saved.getTagAnzahl());
        // Prune the retention tail in the same transaction so the
        // table cannot drift past the configured window even if the
        // daily scheduler is paused.
        pruneExpiredInternal();
        return toDto(saved);
    }

    /**
     * Build the in-memory snapshot map. Includes the schema
     * version so the restore can reject unknown future formats
     * without trying to interpret them.
     *
     * @return a mutable map ready to be serialised by Jackson
     */
    private Map<String, Object> captureSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("schemaVersion", SNAPSHOT_SCHEMA_VERSION);
        snapshot.put("createdAt", LocalDateTime.now());
        snapshot.put("tags", tagRepository.findAll().stream()
            .map(this::toTagMap).toList());
        snapshot.put("ordner", ordnerRepository.findAll().stream()
            .map(this::toOrdnerMap).toList());
        // Capture Datei twice: once with bytes for restore, once as
        // a tag-link list so the M:N table is fully rebuildable.
        List<Datei> dateienList = dateiRepository.findAll();
        snapshot.put("dateien", dateienList.stream()
            .map(this::toDateiMap).toList());
        List<Map<String, Object>> dateiTagLinks = new ArrayList<>();
        for (Datei d : dateienList) {
            if (d.getTags() == null) {
                continue;
            }
            for (Tag t : d.getTags()) {
                Map<String, Object> link = new HashMap<>();
                link.put("dateiID", d.getDateiID().toString());
                link.put("tagID", t.getTagID().toString());
                dateiTagLinks.add(link);
            }
        }
        snapshot.put("dateiTag", dateiTagLinks);
        snapshot.put("bewertungen",
            bewertungRepository.findAll().stream()
                .map(this::toBewertungMap).toList());
        snapshot.put("kommentare",
            kommentarRepository.findAll().stream()
                .map(this::toKommentarMap).toList());
        snapshot.put("dateiAnlegungen",
            dateiAnlegungRepository.findAll().stream()
                .map(this::toDateiAnlegungMap).toList());
        snapshot.put("ordnerAnlegungen",
            ordnerAnlegungRepository.findAll().stream()
                .map(this::toOrdnerAnlegungMap).toList());
        return snapshot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<BackupDTO> listBackups() {
        return backupRepository.findAllOrderByErstelltAmDesc()
            .stream()
            .map(this::toDto)
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public BackupDTO latestBackup() {
        return backupRepository.findLatest()
            .map(this::toDto)
            .orElse(null);
    }

    /**
     * Restore the given backup. Re-creates every row that the
     * snapshot recorded but that has since been deleted, and
     * updates rows whose primary keys still exist.
     *
     * @param backupId          the UUID of the backup to restore
     * @param confirmationCount the caller's confirmation count
     *     (must be {@value #MIN_CONFIRMATION_COUNT} or higher)
     * @throws ResponseStatusException 400 if {@code
     *     confirmationCount} is too low
     * @throws ResponseStatusException 404 if no such backup exists
     * @throws ResponseStatusException 422 if the snapshot schema
     *     version is unsupported
     * @throws ResponseStatusException 500 if the snapshot cannot
     *     be parsed or contains a fatal pre-flight violation
     */
    @Override
    @RetryableTransactional
    public void restoreBackup(
            final UUID backupId,
            final int confirmationCount) {
        if (confirmationCount < MIN_CONFIRMATION_COUNT) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Backup-Restore erfordert "
                    + MIN_CONFIRMATION_COUNT
                    + "-fache Bestaetigung");
            colorLogger.colorError(
                "Restore aborted (count={}): {}",
                confirmationCount, ex.getMessage(), ex);
            throw ex;
        }
        Backup backup = backupRepository.findById(backupId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Backup nicht gefunden"));
        applyRestoreV2(backup);
        colorLogger.colorInfo(
            "Backup {} restored (count={})",
            backup.getBackupID(), confirmationCount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RetryableTransactional
    public void deleteBackup(
            final UUID backupId,
            final int confirmationCount) {
        if (confirmationCount < MIN_CONFIRMATION_COUNT) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Backup-Loeschen erfordert "
                    + MIN_CONFIRMATION_COUNT
                    + "-fache Bestaetigung");
            colorLogger.colorError(
                "Delete aborted (count={}): {}",
                confirmationCount, ex.getMessage(), ex);
            throw ex;
        }
        Backup backup = backupRepository.findById(backupId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Backup nicht gefunden"));
        backupRepository.delete(backup);
        colorLogger.colorInfo(
            "Backup {} manually deleted (count={})",
            backup.getBackupID(), confirmationCount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RetryableTransactional
    public int pruneExpired() {
        return pruneExpiredInternal();
    }

    /**
     * Internal retention sweep used both by the public
     * {@link #pruneExpired()} entry-point and by
     * {@link #createBackup(String)}. Returns the number of pruned
     * rows so the caller can log the outcome.
     *
     * @return the number of pruned rows
     */
    private int pruneExpiredInternal() {
        LocalDateTime cutoff = LocalDateTime.now()
            .minusDays(retentionDays);
        int removed = backupRepository
            .deleteByErstelltAmBefore(cutoff);
        if (removed > 0) {
            colorLogger.colorInfo(
                "Backup retention sweep removed {} backup(s) "
                    + "older than {} day(s)",
                removed, retentionDays);
        }
        return removed;
    }

    /**
     * Apply a v2 snapshot to the live tables.
     *
     * <p>The whole method runs inside the caller's transaction
     * (started by {@link #restoreBackup}) so any failure rolls
     * back the entire restore — partial state is impossible.</p>
     *
     * @param backup the backup whose payload to apply
     */
    private void applyRestoreV2(final Backup backup) {
        Map<String, Object> snapshot = parsePayload(backup);
        verifySchemaVersion(snapshot);
        RestoreContext ctx = new RestoreContext(snapshot,
            studentRepository.findAll().stream()
                .map(Student::getStudentID)
                .collect(Collectors.toSet()),
            kursRepository.findAll().stream()
                .map(Kurs::getKursID)
                .collect(Collectors.toSet()));
        restoreTags(ctx);
        restoreOrdner(ctx);
        restoreDateien(ctx);
        restoreDateiTagLinks(ctx);
        restoreBewertungen(ctx);
        restoreKommentare(ctx);
        restoreAnlegungen(ctx);
        recomputeAggregates(ctx);
        colorLogger.colorInfo(
            "Restore done: tags={} ordner={} datei={} "
                + "bewertungen={} kommentare={} (skipped ordner={})",
            ctx.appliedTagIds.size(),
            ctx.appliedOrdnerIds.size(),
            ctx.appliedDateiIds.size(),
            ctx.entries("bewertungen").size(),
            ctx.entries("kommentare").size(),
            ctx.skipOrdnerIds.size());
    }

    /**
     * Reject snapshots whose declared {@code schemaVersion} this
     * implementation cannot apply.
     *
     * @param snapshot the parsed snapshot
     */
    private void verifySchemaVersion(
            final Map<String, Object> snapshot) {
        int version = asInt(snapshot.get("schemaVersion"), 1);
        if (version != SNAPSHOT_SCHEMA_VERSION) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Backup-Schema v" + version + " wird nicht "
                    + "unterstuetzt; bitte ein neues Backup "
                    + "erstellen.");
            colorLogger.colorError(
                "Restore aborted: legacy schema v{}: {}",
                version, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Stage 1 — upsert tags with canonical-name merge so a live
     * tag with the same name (but a different UUID) is preserved
     * and every snapshot link to the colliding UUID is rewired to
     * the live UUID via {@link RestoreContext#tagIdRemap}.
     *
     * @param ctx the restore context, mutated in place
     */
    private void restoreTags(final RestoreContext ctx) {
        Map<String, Tag> liveByCanonical =
            tagRepository.findAll().stream()
                .collect(Collectors.toMap(
                    t -> canonicalTagName(t.getName()),
                    t -> t,
                    (a, b) -> a));
        for (Map<String, Object> entry : ctx.entries("tags")) {
            UUID snapshotId = parseUuid(entry.get("tagID"));
            String name = asString(entry.get("name"));
            if (snapshotId == null || name == null
                    || name.isBlank()) {
                continue;
            }
            String canonical = canonicalTagName(name);
            Tag existing = liveByCanonical.get(canonical);
            if (existing != null
                    && !existing.getTagID().equals(snapshotId)) {
                ctx.tagIdRemap.put(snapshotId, existing.getTagID());
                ctx.appliedTagIds.add(existing.getTagID());
                continue;
            }
            Optional<Tag> live =
                tagRepository.findById(snapshotId);
            Tag tag = live.orElseGet(Tag::new);
            tag.setTagID(snapshotId);
            tag.setName(name);
            UUID erstellerId = parseUuid(entry.get("erstellerID"));
            tag.setErsteller(
                erstellerId != null
                    && ctx.existingStudentIds.contains(erstellerId)
                    ? studentRepository.getReferenceById(erstellerId)
                    : null);
            tag.setErstelltAm(parseDateTime(entry.get("erstelltAm")));
            upsertTagNative(tag);
            entityManager.merge(tag);
            ctx.appliedTagIds.add(snapshotId);
        }
        entityManager.flush();
    }

    /**
     * Stage 2 — upsert {@link Ordner} rows in topological order so
     * a parent always exists when its child is written.
     *
     * @param ctx the restore context, mutated in place
     */
    private void restoreOrdner(final RestoreContext ctx) {
        List<Map<String, Object>> sorted =
            topologicallySort(ctx.entries("ordner"));
        ctx.existingOrdnerIds = ordnerRepository.findAll().stream()
            .map(Ordner::getOrdnerID)
            .collect(Collectors.toSet());
        for (Map<String, Object> entry : sorted) {
            UUID id = parseUuid(entry.get("ordnerID"));
            if (id == null) {
                continue;
            }
            UUID parentId = parseUuid(entry.get("ueberOrdnerID"));
            if (parentId != null
                    && ctx.skipOrdnerIds.contains(parentId)) {
                ctx.skipOrdnerIds.add(id);
                continue;
            }
            UUID kursId = parseUuid(entry.get("kursID"));
            if (kursId != null
                    && !ctx.existingKursIds.contains(kursId)) {
                colorLogger.colorWarn(
                    "Skipping ordner {} during restore: "
                        + "kurs {} no longer exists",
                    id, kursId);
                ctx.skipOrdnerIds.add(id);
                continue;
            }
            if (parentId != null
                    && !ctx.appliedOrdnerIds.contains(parentId)
                    && !ctx.existingOrdnerIds.contains(parentId)) {
                colorLogger.colorWarn(
                    "Skipping ordner {} during restore: "
                        + "parent {} not present",
                    id, parentId);
                ctx.skipOrdnerIds.add(id);
                continue;
            }
            Optional<Ordner> live = ordnerRepository.findById(id);
            Ordner ordnerRow = live.orElseGet(Ordner::new);
            ordnerRow.setOrdnerID(id);
            ordnerRow.setName(asString(entry.get("name")));
            ordnerRow.setLevel(asInt(entry.get("level"), 0));
            ordnerRow.setFarbe(parseFarbe(entry.get("farbe")));
            ordnerRow.setPublic(
                asBoolean(entry.get("isPublic"), true));
            ordnerRow.setUeberOrdner(parentId == null
                ? null
                : ordnerRepository.getReferenceById(parentId));
            ordnerRow.setKurs(kursId == null
                ? null
                : kursRepository.getReferenceById(kursId));
            upsertOrdnerNative(ordnerRow);
            entityManager.merge(ordnerRow);
            ctx.appliedOrdnerIds.add(id);
        }
        entityManager.flush();
    }

    /**
     * Stage 3 — upsert {@link Datei} rows, materialising the bytes
     * via {@link FileStorageStrategy} and verifying the SHA-256
     * captured at snapshot time.
     *
     * @param ctx the restore context, mutated in place
     */
    private void restoreDateien(final RestoreContext ctx) {
        for (Map<String, Object> entry : ctx.entries("dateien")) {
            UUID id = parseUuid(entry.get("dateiID"));
            if (id == null) {
                continue;
            }
            UUID ordnerId = parseUuid(entry.get("ordnerID"));
            if (ordnerId == null
                    || ctx.skipOrdnerIds.contains(ordnerId)
                    || (!ctx.appliedOrdnerIds.contains(ordnerId)
                        && !ctx.existingOrdnerIds.contains(ordnerId))) {
                colorLogger.colorWarn(
                    "Skipping datei {} during restore: "
                        + "ordner {} not present",
                    id, ordnerId);
                continue;
            }
            Optional<Datei> live = dateiRepository.findById(id);
            Datei datei = live.orElseGet(Datei::new);
            Long oldOid = datei.getDataOid();
            datei.setDateiID(id);
            datei.setOrdner(
                ordnerRepository.getReferenceById(ordnerId));
            datei.setName(asString(entry.get("name")));
            datei.setZuletztBearbeitet(
                parseDate(entry.get("zuletztBearbeitet")));
            datei.setFileSize(
                ((Number) entry.getOrDefault(
                    "fileSize", 0)).floatValue());
            datei.setDownloadAnzahl(
                asInt(entry.get("downloadAnzahl"), 0));
            // Aggregates are recomputed at the end; clear here so a
            // row that never gets a Bewertung does not retain a
            // stale count from a previous incarnation.
            datei.setBewertungSumme(0L);
            datei.setBewertungAnzahl(0);
            UUID uploaderId = parseUuid(entry.get("uploaderID"));
            datei.setUploader(uploaderId != null
                && ctx.existingStudentIds.contains(uploaderId)
                ? studentRepository.getReferenceById(uploaderId)
                : null);
            byte[] bytes = decodeBytes(
                asString(entry.get("dataBase64")));
            String expectedSha = asString(entry.get("dataSha256"));
            if (expectedSha != null && bytes != null
                    && !expectedSha.equals(sha256Hex(bytes))) {
                ResponseStatusException ex =
                    new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Backup-Datei " + id
                            + " hat einen falschen SHA-256-"
                            + "Pruefwert; Restore abgebrochen.");
                colorLogger.colorError(
                    "Restore aborted: SHA mismatch on datei {}: {}",
                    id, ex.getMessage(), ex);
                throw ex;
            }
            if (bytes != null) {
                long ref = fileStorageStrategy.store(bytes);
                datei.setDataOid(ref);
                datei.setData(
                    fileStorageStrategy.requiresInlineData()
                        ? bytes
                        : null);
            }
            upsertDateiNative(datei);
            // After the native MERGE the row is on disk; merge the
            // managed copy back into the persistence context so any
            // downstream stage that calls findById observes it.
            entityManager.merge(datei);
            ctx.appliedDateiIds.add(id);
            unlinkPreviousBytes(oldOid, datei.getDataOid());
        }
        entityManager.flush();
    }

    /**
     * Best-effort cleanup of the previous large object so a long
     * restore history does not bloat {@code pg_largeobject}.
     *
     * @param oldOid the dataOid the entity carried before the
     *     restore (may be {@code null})
     * @param newOid the dataOid the entity now carries (may be
     *     {@code null})
     */
    private void unlinkPreviousBytes(
            final Long oldOid, final Long newOid) {
        if (oldOid == null
                || (newOid != null && oldOid.equals(newOid))) {
            return;
        }
        try {
            fileStorageStrategy.delete(oldOid);
        } catch (RuntimeException ex) {
            colorLogger.colorWarn(
                "Could not unlink old data ref {}: {}",
                oldOid, ex.getMessage());
        }
    }

    /**
     * Stage 4 — rebuild the {@code datei_tag} M:N table for every
     * restored {@link Datei}. Dateien that the snapshot did not
     * cover keep their existing tag set untouched.
     *
     * @param ctx the restore context, mutated in place
     */
    private void restoreDateiTagLinks(final RestoreContext ctx) {
        Map<UUID, Set<UUID>> wantedByDatei = new HashMap<>();
        for (Map<String, Object> link : ctx.entries("dateiTag")) {
            UUID dateiId = parseUuid(link.get("dateiID"));
            UUID rawTagId = parseUuid(link.get("tagID"));
            if (dateiId == null || rawTagId == null) {
                continue;
            }
            UUID tagId = ctx.tagIdRemap
                .getOrDefault(rawTagId, rawTagId);
            if (!ctx.appliedTagIds.contains(tagId)) {
                continue;
            }
            wantedByDatei
                .computeIfAbsent(dateiId, k -> new HashSet<>())
                .add(tagId);
        }
        for (UUID dateiId : ctx.appliedDateiIds) {
            Optional<Datei> opt = dateiRepository.findById(dateiId);
            if (opt.isEmpty()) {
                continue;
            }
            Datei d = opt.get();
            Set<UUID> wanted = wantedByDatei
                .getOrDefault(dateiId, Set.of());
            Set<Tag> newTags = new LinkedHashSet<>();
            for (UUID tagId : wanted) {
                tagRepository.findById(tagId)
                    .ifPresent(newTags::add);
            }
            d.setTags(newTags);
            dateiRepository.save(d);
        }
        entityManager.flush();
    }

    /**
     * Stage 5 — upsert {@link Bewertung} rows. The
     * {@code (datei, autor)} UNIQUE constraint is honoured by
     * deleting any colliding live row before saving, so a
     * snapshot row with a different bewertungID always wins.
     *
     * @param ctx the restore context, mutated in place
     */
    private void restoreBewertungen(final RestoreContext ctx) {
        Map<UUID, List<Bewertung>> liveByDatei = new HashMap<>();
        for (Bewertung b : bewertungRepository.findAll()) {
            if (b.getDatei() == null) {
                continue;
            }
            liveByDatei
                .computeIfAbsent(
                    b.getDatei().getDateiID(),
                    k -> new ArrayList<>())
                .add(b);
        }
        for (Map<String, Object> entry : ctx.entries("bewertungen")) {
            UUID id = parseUuid(entry.get("bewertungID"));
            UUID dateiId = parseUuid(entry.get("dateiID"));
            UUID autorId = parseUuid(entry.get("autorID"));
            int sterne = asInt(entry.get("sterne"), 0);
            if (id == null || dateiId == null || autorId == null
                    || sterne < Bewertung.MIN_STERNE
                    || sterne > Bewertung.MAX_STERNE) {
                continue;
            }
            if (!ctx.appliedDateiIds.contains(dateiId)
                    && !dateiRepository.existsById(dateiId)) {
                continue;
            }
            if (!ctx.existingStudentIds.contains(autorId)) {
                colorLogger.colorWarn(
                    "Skipping bewertung {} during restore: "
                        + "autor {} no longer exists",
                    id, autorId);
                continue;
            }
            List<Bewertung> collisions = liveByDatei
                .getOrDefault(dateiId, List.of()).stream()
                .filter(b -> b.getAutor() != null
                    && b.getAutor().getStudentID().equals(autorId)
                    && !b.getBewertungID().equals(id))
                .toList();
            for (Bewertung c : collisions) {
                bewertungRepository.delete(c);
            }
            if (!collisions.isEmpty()) {
                entityManager.flush();
            }
            Optional<Bewertung> live =
                bewertungRepository.findById(id);
            Bewertung row = live.orElseGet(Bewertung::new);
            row.setBewertungID(id);
            row.setDatei(dateiRepository.getReferenceById(dateiId));
            row.setAutor(studentRepository.getReferenceById(autorId));
            row.setSterne(sterne);
            row.setBewertetAm(parseDateTime(entry.get("bewertetAm")));
            upsertBewertungNative(row);
            entityManager.merge(row);
        }
        entityManager.flush();
    }

    /**
     * Stage 6 — upsert {@link Kommentar} rows. A missing author
     * (Student deleted post-snapshot) is preserved as
     * {@code null} since the column is nullable.
     *
     * @param ctx the restore context, mutated in place
     */
    private void restoreKommentare(final RestoreContext ctx) {
        for (Map<String, Object> entry : ctx.entries("kommentare")) {
            UUID id = parseUuid(entry.get("kommentarID"));
            UUID dateiId = parseUuid(entry.get("dateiID"));
            String inhalt = asString(entry.get("inhalt"));
            if (id == null || dateiId == null
                    || inhalt == null
                    || inhalt.length() > Kommentar.MAX_INHALT_LENGTH) {
                continue;
            }
            if (!ctx.appliedDateiIds.contains(dateiId)
                    && !dateiRepository.existsById(dateiId)) {
                continue;
            }
            Optional<Kommentar> live =
                kommentarRepository.findById(id);
            Kommentar k = live.orElseGet(Kommentar::new);
            k.setKommentarID(id);
            k.setDatei(dateiRepository.getReferenceById(dateiId));
            UUID autorId = parseUuid(entry.get("autorID"));
            k.setAutor(autorId != null
                && ctx.existingStudentIds.contains(autorId)
                ? studentRepository.getReferenceById(autorId)
                : null);
            k.setInhalt(inhalt);
            k.setErstelltAm(parseDateTime(entry.get("erstelltAm")));
            k.setZuletztBearbeitet(
                parseDateTime(entry.get("zuletztBearbeitet")));
            k.setBearbeitet(
                asBoolean(entry.get("bearbeitet"), false));
            upsertKommentarNative(k);
            entityManager.merge(k);
        }
        entityManager.flush();
    }

    /**
     * Stage 7 — upsert {@link DateiAnlegung} and
     * {@link OrdnerAnlegung} tracking records.
     *
     * @param ctx the restore context
     */
    private void restoreAnlegungen(final RestoreContext ctx) {
        restoreDateiAnlegungen(ctx);
        restoreOrdnerAnlegungen(ctx);
        entityManager.flush();
    }

    /**
     * Upsert {@link DateiAnlegung} rows, resolving the
     * one-to-one collision on {@code datei_id} by deleting the
     * pre-existing record first.
     *
     * @param ctx the restore context
     */
    private void restoreDateiAnlegungen(final RestoreContext ctx) {
        for (Map<String, Object> entry
                : ctx.entries("dateiAnlegungen")) {
            UUID id = parseUuid(entry.get("anlegungID"));
            UUID studentId = parseUuid(entry.get("studentID"));
            UUID dateiId = parseUuid(entry.get("dateiID"));
            if (id == null || dateiId == null
                    || studentId == null) {
                continue;
            }
            if (!ctx.existingStudentIds.contains(studentId)) {
                continue;
            }
            if (!ctx.appliedDateiIds.contains(dateiId)
                    && !dateiRepository.existsById(dateiId)) {
                continue;
            }
            Datei dateiRef = dateiRepository.findById(dateiId)
                .orElse(null);
            if (dateiRef != null) {
                Optional<DateiAnlegung> collision =
                    dateiAnlegungRepository.findByDatei(dateiRef);
                if (collision.isPresent()
                        && !collision.get().getAnlegungID()
                            .equals(id)) {
                    dateiAnlegungRepository
                        .delete(collision.get());
                    entityManager.flush();
                }
            }
            DateiAnlegung a = dateiAnlegungRepository.findById(id)
                .orElseGet(DateiAnlegung::new);
            a.setAnlegungID(id);
            a.setStudent(
                studentRepository.getReferenceById(studentId));
            a.setDatei(dateiRepository.getReferenceById(dateiId));
            a.setDatum(parseDate(entry.get("datum")));
            upsertDateiAnlegungNative(a);
            entityManager.merge(a);
        }
    }

    /**
     * Upsert {@link OrdnerAnlegung} rows analogous to
     * {@link #restoreDateiAnlegungen}.
     *
     * @param ctx the restore context
     */
    private void restoreOrdnerAnlegungen(final RestoreContext ctx) {
        for (Map<String, Object> entry
                : ctx.entries("ordnerAnlegungen")) {
            UUID id = parseUuid(entry.get("anlegungID"));
            UUID studentId = parseUuid(entry.get("studentID"));
            UUID ordnerId = parseUuid(entry.get("ordnerID"));
            if (id == null || ordnerId == null
                    || studentId == null) {
                continue;
            }
            if (!ctx.existingStudentIds.contains(studentId)) {
                continue;
            }
            if (!ctx.appliedOrdnerIds.contains(ordnerId)
                    && !ordnerRepository.existsById(ordnerId)) {
                continue;
            }
            Ordner ordnerRef = ordnerRepository.findById(ordnerId)
                .orElse(null);
            if (ordnerRef != null) {
                Optional<OrdnerAnlegung> collision =
                    ordnerAnlegungRepository.findByOrdner(ordnerRef);
                if (collision.isPresent()
                        && !collision.get().getAnlegungID()
                            .equals(id)) {
                    ordnerAnlegungRepository
                        .delete(collision.get());
                    entityManager.flush();
                }
            }
            OrdnerAnlegung a = ordnerAnlegungRepository.findById(id)
                .orElseGet(OrdnerAnlegung::new);
            a.setAnlegungID(id);
            a.setStudent(
                studentRepository.getReferenceById(studentId));
            a.setOrdner(ordnerRepository.getReferenceById(ordnerId));
            a.setDatum(parseDate(entry.get("datum")));
            upsertOrdnerAnlegungNative(a);
            entityManager.merge(a);
        }
    }

    /**
     * Stage 8 — derive {@link Datei#getBewertungSumme} and
     * {@link Datei#getBewertungAnzahl} from the actually-restored
     * {@link Bewertung} set so the cached aggregate cannot lie
     * when some Bewertungen were skipped (missing autor, etc.).
     *
     * @param ctx the restore context
     */
    private void recomputeAggregates(final RestoreContext ctx) {
        for (UUID dateiId : ctx.appliedDateiIds) {
            Datei d = dateiRepository.findById(dateiId)
                .orElse(null);
            if (d == null) {
                continue;
            }
            List<Bewertung> rows = bewertungRepository.findAll()
                .stream()
                .filter(b -> b.getDatei() != null
                    && dateiId.equals(b.getDatei().getDateiID()))
                .toList();
            long summe = rows.stream()
                .mapToLong(Bewertung::getSterne).sum();
            d.setBewertungSumme(summe);
            d.setBewertungAnzahl(rows.size());
            dateiRepository.save(d);
        }
        entityManager.flush();
    }

    /**
     * Convert a snapshot's {@code farbe} field back to its enum
     * value, defaulting to {@link OrdnerFarbe#DEFAULT} when the
     * stored name no longer maps to a constant (defensive against
     * enum renames).
     *
     * @param raw the value read from the snapshot
     * @return the resolved enum
     */
    private static OrdnerFarbe parseFarbe(final Object raw) {
        if (raw == null) {
            return OrdnerFarbe.DEFAULT;
        }
        try {
            return OrdnerFarbe.valueOf(raw.toString());
        } catch (IllegalArgumentException ex) {
            return OrdnerFarbe.DEFAULT;
        }
    }

    /**
     * Persist or merge a managed entity carrying its original UUID.
     *
     * <p>The deleted-row-then-restore path used to fail with
     * {@link org.springframework.orm.ObjectOptimisticLockingFailureException}
     * because Spring Data JPA's {@code save} dispatches by
     * {@code isNew()} (PK == null) — a snapshot row whose UUID
     * still points at a deleted DB row was misclassified as
     * UPDATE. The same trap fires inside JPA's
     * {@code merge}.</p>
     *
     * <p>The fix is a native upsert: a single SQL statement that
     * INSERTs the row when the PK is missing and UPDATEs it when
     * it is alive, expressed via {@code MERGE INTO …}. The
     * snapshot-shaped entity is then re-loaded by
     * {@link EntityManager#refresh} so any downstream stage that
     * looks the row up via the persistence context observes the
     * just-written values.</p>
     *
     * <p>Each restore stage owns its own native upsert because
     * the column list differs per table; this helper only does
     * the pre-/post-condition orchestration that's identical
     * across stages: it forces a {@code flush} before the upsert
     * (so any pending updates on the same row by Hibernate's
     * dirty-checking are out of the way) and clears the Hibernate
     * first-level cache for this primary key so that subsequent
     * {@code findById} calls hit the database again.</p>
     *
     * @param entityClass the entity type
     * @param pkValue     the primary-key value
     */
    private void evictManagedCopy(
            final Class<?> entityClass, final UUID pkValue) {
        Object managed = entityManager.find(entityClass, pkValue);
        if (managed != null) {
            entityManager.detach(managed);
        }
    }

    /**
     * Native-SQL upsert for the {@code datei} table. Side-steps the
     * Hibernate "stale state" trap on resurrected rows by writing
     * via a {@code MERGE} statement that doesn't care whether the
     * PK was present a moment ago.
     *
     * @param d the snapshot-shaped {@link Datei} to upsert
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private void upsertDateiNative(final Datei d) {
        evictManagedCopy(Datei.class, d.getDateiID());
        UUID ordnerId = d.getOrdner() == null
            ? null : d.getOrdner().getOrdnerID();
        UUID uploaderId = d.getUploader() == null
            ? null : d.getUploader().getStudentID();
        entityManager.createNativeQuery(
                "MERGE INTO datei AS t USING (VALUES "
                    + "(CAST(?1 AS UUID), CAST(?2 AS UUID), ?3, "
                    + "CAST(?4 AS DATE), CAST(?5 AS OID), ?6, "
                    + "CAST(?7 AS UUID), ?8, CAST(?9 AS OID), "
                    + "?10, ?11)) "
                    + "AS s(datei_id, ordner_id, name, "
                    + "zuletzt_bearbeitet, data, file_size, "
                    + "uploader, download_anzahl, data_oid, "
                    + "bewertung_summe, bewertung_anzahl) "
                    + "ON t.datei_id = s.datei_id "
                    + "WHEN MATCHED THEN UPDATE SET "
                    + "ordner_id = s.ordner_id, name = s.name, "
                    + "zuletzt_bearbeitet = s.zuletzt_bearbeitet, "
                    + "data = s.data, file_size = s.file_size, "
                    + "uploader = s.uploader, "
                    + "download_anzahl = s.download_anzahl, "
                    + "data_oid = s.data_oid, "
                    + "bewertung_summe = s.bewertung_summe, "
                    + "bewertung_anzahl = s.bewertung_anzahl "
                    + "WHEN NOT MATCHED THEN INSERT "
                    + "(datei_id, ordner_id, name, "
                    + "zuletzt_bearbeitet, data, file_size, "
                    + "uploader, download_anzahl, data_oid, "
                    + "bewertung_summe, bewertung_anzahl) "
                    + "VALUES (s.datei_id, s.ordner_id, s.name, "
                    + "s.zuletzt_bearbeitet, s.data, s.file_size, "
                    + "s.uploader, s.download_anzahl, s.data_oid, "
                    + "s.bewertung_summe, s.bewertung_anzahl)")
            .setParameter(1, d.getDateiID().toString())
            .setParameter(2,
                ordnerId == null ? null : ordnerId.toString())
            .setParameter(3, d.getName())
            .setParameter(4, d.getZuletztBearbeitet())
            .setParameter(5, d.getData())
            .setParameter(6, d.getFileSize())
            .setParameter(7,
                uploaderId == null ? null : uploaderId.toString())
            .setParameter(8, d.getDownloadAnzahl())
            .setParameter(9, d.getDataOid())
            .setParameter(10, d.getBewertungSumme())
            .setParameter(11, d.getBewertungAnzahl())
            .executeUpdate();
    }

    /**
     * Native-SQL upsert for the {@code tag} table.
     *
     * @param t the snapshot-shaped {@link Tag} to upsert
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private void upsertTagNative(final Tag t) {
        evictManagedCopy(Tag.class, t.getTagID());
        UUID erstellerId = t.getErsteller() == null
            ? null : t.getErsteller().getStudentID();
        entityManager.createNativeQuery(
                "MERGE INTO tag AS x USING (VALUES "
                    + "(CAST(?1 AS UUID), ?2, "
                    + "CAST(?3 AS UUID), CAST(?4 AS TIMESTAMP))) "
                    + "AS s(tag_id, name, ersteller_id, erstellt_am) "
                    + "ON x.tag_id = s.tag_id "
                    + "WHEN MATCHED THEN UPDATE SET "
                    + "name = s.name, "
                    + "ersteller_id = s.ersteller_id, "
                    + "erstellt_am = s.erstellt_am "
                    + "WHEN NOT MATCHED THEN INSERT "
                    + "(tag_id, name, ersteller_id, erstellt_am) "
                    + "VALUES (s.tag_id, s.name, "
                    + "s.ersteller_id, s.erstellt_am)")
            .setParameter(1, t.getTagID().toString())
            .setParameter(2, t.getName())
            .setParameter(3,
                erstellerId == null ? null : erstellerId.toString())
            .setParameter(4, t.getErstelltAm())
            .executeUpdate();
    }

    /**
     * Native-SQL upsert for the {@code ordner} table.
     *
     * @param o the snapshot-shaped {@link Ordner} to upsert
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private void upsertOrdnerNative(final Ordner o) {
        evictManagedCopy(Ordner.class, o.getOrdnerID());
        UUID parentId = o.getUeberOrdner() == null
            ? null : o.getUeberOrdner().getOrdnerID();
        UUID kursId = o.getKurs() == null
            ? null : o.getKurs().getKursID();
        Integer farbeOrdinal = o.getFarbe() == null
            ? null : o.getFarbe().ordinal();
        entityManager.createNativeQuery(
                "MERGE INTO ordner AS x USING (VALUES "
                    + "(CAST(?1 AS UUID), CAST(?2 AS UUID), "
                    + "CAST(?3 AS UUID), ?4, ?5, ?6, ?7)) "
                    + "AS s(ordner_id, ueber_ordner_id, kurs_id, "
                    + "name, level, farbe, is_public) "
                    + "ON x.ordner_id = s.ordner_id "
                    + "WHEN MATCHED THEN UPDATE SET "
                    + "ueber_ordner_id = s.ueber_ordner_id, "
                    + "kurs_id = s.kurs_id, name = s.name, "
                    + "level = s.level, farbe = s.farbe, "
                    + "is_public = s.is_public "
                    + "WHEN NOT MATCHED THEN INSERT "
                    + "(ordner_id, ueber_ordner_id, kurs_id, "
                    + "name, level, farbe, is_public) "
                    + "VALUES (s.ordner_id, s.ueber_ordner_id, "
                    + "s.kurs_id, s.name, s.level, s.farbe, "
                    + "s.is_public)")
            .setParameter(1, o.getOrdnerID().toString())
            .setParameter(2,
                parentId == null ? null : parentId.toString())
            .setParameter(3,
                kursId == null ? null : kursId.toString())
            .setParameter(4, o.getName())
            .setParameter(5, o.getLevel())
            .setParameter(6, farbeOrdinal)
            .setParameter(7, o.isPublic())
            .executeUpdate();
    }

    /**
     * Native-SQL upsert for the {@code bewertung} table.
     *
     * @param b the snapshot-shaped {@link Bewertung} to upsert
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private void upsertBewertungNative(final Bewertung b) {
        evictManagedCopy(Bewertung.class, b.getBewertungID());
        UUID dateiId = b.getDatei() == null
            ? null : b.getDatei().getDateiID();
        UUID autorId = b.getAutor() == null
            ? null : b.getAutor().getStudentID();
        entityManager.createNativeQuery(
                "MERGE INTO bewertung AS x USING (VALUES "
                    + "(CAST(?1 AS UUID), CAST(?2 AS UUID), "
                    + "CAST(?3 AS UUID), ?4, "
                    + "CAST(?5 AS TIMESTAMP))) "
                    + "AS s(bewertung_id, datei_id, autor_id, "
                    + "sterne, bewertet_am) "
                    + "ON x.bewertung_id = s.bewertung_id "
                    + "WHEN MATCHED THEN UPDATE SET "
                    + "datei_id = s.datei_id, "
                    + "autor_id = s.autor_id, "
                    + "sterne = s.sterne, "
                    + "bewertet_am = s.bewertet_am "
                    + "WHEN NOT MATCHED THEN INSERT "
                    + "(bewertung_id, datei_id, autor_id, "
                    + "sterne, bewertet_am) "
                    + "VALUES (s.bewertung_id, s.datei_id, "
                    + "s.autor_id, s.sterne, s.bewertet_am)")
            .setParameter(1, b.getBewertungID().toString())
            .setParameter(2,
                dateiId == null ? null : dateiId.toString())
            .setParameter(3,
                autorId == null ? null : autorId.toString())
            .setParameter(4, b.getSterne())
            .setParameter(5, b.getBewertetAm())
            .executeUpdate();
    }

    /**
     * Native-SQL upsert for the {@code kommentar} table.
     *
     * @param k the snapshot-shaped {@link Kommentar} to upsert
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private void upsertKommentarNative(final Kommentar k) {
        evictManagedCopy(Kommentar.class, k.getKommentarID());
        UUID dateiId = k.getDatei() == null
            ? null : k.getDatei().getDateiID();
        UUID autorId = k.getAutor() == null
            ? null : k.getAutor().getStudentID();
        entityManager.createNativeQuery(
                "MERGE INTO kommentar AS x USING (VALUES "
                    + "(CAST(?1 AS UUID), CAST(?2 AS UUID), "
                    + "CAST(?3 AS UUID), ?4, "
                    + "CAST(?5 AS TIMESTAMP), "
                    + "CAST(?6 AS TIMESTAMP), ?7)) "
                    + "AS s(kommentar_id, datei_id, autor_id, "
                    + "inhalt, erstellt_am, zuletzt_bearbeitet, "
                    + "bearbeitet) "
                    + "ON x.kommentar_id = s.kommentar_id "
                    + "WHEN MATCHED THEN UPDATE SET "
                    + "datei_id = s.datei_id, "
                    + "autor_id = s.autor_id, "
                    + "inhalt = s.inhalt, "
                    + "erstellt_am = s.erstellt_am, "
                    + "zuletzt_bearbeitet = s.zuletzt_bearbeitet, "
                    + "bearbeitet = s.bearbeitet "
                    + "WHEN NOT MATCHED THEN INSERT "
                    + "(kommentar_id, datei_id, autor_id, inhalt, "
                    + "erstellt_am, zuletzt_bearbeitet, "
                    + "bearbeitet) "
                    + "VALUES (s.kommentar_id, s.datei_id, "
                    + "s.autor_id, s.inhalt, s.erstellt_am, "
                    + "s.zuletzt_bearbeitet, s.bearbeitet)")
            .setParameter(1, k.getKommentarID().toString())
            .setParameter(2,
                dateiId == null ? null : dateiId.toString())
            .setParameter(3,
                autorId == null ? null : autorId.toString())
            .setParameter(4, k.getInhalt())
            .setParameter(5, k.getErstelltAm())
            .setParameter(6, k.getZuletztBearbeitet())
            .setParameter(7, k.isBearbeitet())
            .executeUpdate();
    }

    /**
     * Native-SQL upsert for the {@code datei_anlegung} table.
     *
     * @param a the snapshot-shaped {@link DateiAnlegung} to upsert
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private void upsertDateiAnlegungNative(final DateiAnlegung a) {
        evictManagedCopy(DateiAnlegung.class, a.getAnlegungID());
        UUID studentId = a.getStudent() == null
            ? null : a.getStudent().getStudentID();
        UUID dateiId = a.getDatei() == null
            ? null : a.getDatei().getDateiID();
        entityManager.createNativeQuery(
                "MERGE INTO datei_anlegung AS x USING (VALUES "
                    + "(CAST(?1 AS UUID), CAST(?2 AS UUID), "
                    + "CAST(?3 AS UUID), CAST(?4 AS DATE))) "
                    + "AS s(anlegung_id, student_id, "
                    + "datei_id, datum) "
                    + "ON x.anlegung_id = s.anlegung_id "
                    + "WHEN MATCHED THEN UPDATE SET "
                    + "student_id = s.student_id, "
                    + "datei_id = s.datei_id, "
                    + "datum = s.datum "
                    + "WHEN NOT MATCHED THEN INSERT "
                    + "(anlegung_id, student_id, "
                    + "datei_id, datum) "
                    + "VALUES (s.anlegung_id, s.student_id, "
                    + "s.datei_id, s.datum)")
            .setParameter(1, a.getAnlegungID().toString())
            .setParameter(2,
                studentId == null ? null : studentId.toString())
            .setParameter(3,
                dateiId == null ? null : dateiId.toString())
            .setParameter(4, a.getDatum())
            .executeUpdate();
    }

    /**
     * Native-SQL upsert for the {@code ordner_anlegung} table.
     *
     * @param a the snapshot-shaped {@link OrdnerAnlegung} to upsert
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private void upsertOrdnerAnlegungNative(final OrdnerAnlegung a) {
        evictManagedCopy(OrdnerAnlegung.class, a.getAnlegungID());
        UUID studentId = a.getStudent() == null
            ? null : a.getStudent().getStudentID();
        UUID ordnerId = a.getOrdner() == null
            ? null : a.getOrdner().getOrdnerID();
        entityManager.createNativeQuery(
                "MERGE INTO ordner_anlegung AS x USING (VALUES "
                    + "(CAST(?1 AS UUID), CAST(?2 AS UUID), "
                    + "CAST(?3 AS UUID), CAST(?4 AS DATE))) "
                    + "AS s(anlegung_id, student_id, "
                    + "ordner_id, datum) "
                    + "ON x.anlegung_id = s.anlegung_id "
                    + "WHEN MATCHED THEN UPDATE SET "
                    + "student_id = s.student_id, "
                    + "ordner_id = s.ordner_id, "
                    + "datum = s.datum "
                    + "WHEN NOT MATCHED THEN INSERT "
                    + "(anlegung_id, student_id, "
                    + "ordner_id, datum) "
                    + "VALUES (s.anlegung_id, s.student_id, "
                    + "s.ordner_id, s.datum)")
            .setParameter(1, a.getAnlegungID().toString())
            .setParameter(2,
                studentId == null ? null : studentId.toString())
            .setParameter(3,
                ordnerId == null ? null : ordnerId.toString())
            .setParameter(4, a.getDatum())
            .executeUpdate();
    }

    /**
     * Mutable working set passed between the restore stages.
     * Encapsulates the snapshot, the read-only existence indices
     * for {@link Student} / {@link Kurs}, and the applied-id /
     * skip-id sets that downstream stages use to gate cascading
     * decisions.
     */
    private static final class RestoreContext {
        /** The parsed snapshot map. */
        private final Map<String, Object> snapshot;
        /** UUIDs of every Student row currently in the database. */
        private final Set<UUID> existingStudentIds;
        /** UUIDs of every Kurs row currently in the database. */
        private final Set<UUID> existingKursIds;
        /** UUIDs of Ordner that already exist live (loaded once). */
        private Set<UUID> existingOrdnerIds = Set.of();
        /** Tags successfully restored or merged (live UUIDs). */
        private final Set<UUID> appliedTagIds = new HashSet<>();
        /** Snapshot tag UUID → live tag UUID for name collisions. */
        private final Map<UUID, UUID> tagIdRemap = new HashMap<>();
        /** Ordner UUIDs that the restore actually wrote. */
        private final Set<UUID> appliedOrdnerIds = new HashSet<>();
        /** Ordner UUIDs intentionally skipped (cascade source). */
        private final Set<UUID> skipOrdnerIds = new HashSet<>();
        /** Datei UUIDs that the restore actually wrote. */
        private final Set<UUID> appliedDateiIds = new HashSet<>();

        RestoreContext(final Map<String, Object> snapshotMap,
                final Set<UUID> studentIds,
                final Set<UUID> kursIds) {
            this.snapshot = snapshotMap;
            this.existingStudentIds = studentIds;
            this.existingKursIds = kursIds;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries(final String key) {
            Object raw = snapshot.get(key);
            if (raw instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
            return List.of();
        }
    }

    /**
     * Parse the JSON payload of a backup row.
     *
     * @param backup the backup whose payload to read
     * @return the snapshot as a Java map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(final Backup backup) {
        try {
            return objectMapper.readValue(
                backup.getPayload(), Map.class);
        } catch (Exception ex) {
            ResponseStatusException rse = new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Backup-Snapshot konnte nicht gelesen werden");
            colorLogger.colorError(
                "Restore parse failed: {}", ex.getMessage(), rse);
            throw rse;
        }
    }

    /**
     * Topologically sort the snapshot's Ordner entries so a parent
     * always precedes its children in the iteration. Uses Kahn's
     * algorithm and falls back to the insertion order for entries
     * whose parent is outside the snapshot (they keep their
     * relative order so the restore stays deterministic).
     *
     * @param entries the snapshot Ordner entries
     * @return a new list in topological order
     */
    private List<Map<String, Object>> topologicallySort(
            final List<Map<String, Object>> entries) {
        Map<UUID, Map<String, Object>> byId = new HashMap<>();
        Map<UUID, List<UUID>> children = new HashMap<>();
        Map<UUID, Integer> indegree = new HashMap<>();
        for (Map<String, Object> e : entries) {
            UUID id = parseUuid(e.get("ordnerID"));
            if (id == null) {
                continue;
            }
            byId.put(id, e);
            indegree.putIfAbsent(id, 0);
        }
        for (Map<String, Object> e : entries) {
            UUID id = parseUuid(e.get("ordnerID"));
            UUID parentId = parseUuid(e.get("ueberOrdnerID"));
            if (id == null) {
                continue;
            }
            if (parentId != null && byId.containsKey(parentId)) {
                children
                    .computeIfAbsent(parentId, k -> new ArrayList<>())
                    .add(id);
                indegree.merge(id, 1, Integer::sum);
            }
        }
        Deque<UUID> ready = new ArrayDeque<>();
        for (Map.Entry<UUID, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                ready.add(e.getKey());
            }
        }
        List<Map<String, Object>> sorted = new ArrayList<>();
        while (!ready.isEmpty()) {
            UUID id = ready.removeFirst();
            sorted.add(byId.get(id));
            for (UUID childId : children
                    .getOrDefault(id, List.of())) {
                int next = indegree.merge(childId, -1, Integer::sum);
                if (next == 0) {
                    ready.add(childId);
                }
            }
        }
        if (sorted.size() != byId.size()) {
            // Cycle — append the unsorted leftovers verbatim so
            // they at least try to apply (will skip on parent-FK
            // mismatch). This keeps the restore non-fatal on
            // pathological snapshots.
            colorLogger.colorWarn(
                "Topological sort detected a cycle; "
                    + "{} ordner entries kept in insertion order",
                byId.size() - sorted.size());
            Set<UUID> already = sorted.stream()
                .map(m -> parseUuid(m.get("ordnerID")))
                .collect(Collectors.toSet());
            for (Map<String, Object> e : entries) {
                UUID id = parseUuid(e.get("ordnerID"));
                if (id != null && !already.contains(id)) {
                    sorted.add(e);
                }
            }
        }
        return sorted;
    }

    // ---------- snapshot mappers (entity -> map) ----------

    private Map<String, Object> toTagMap(final Tag t) {
        Map<String, Object> map = new HashMap<>();
        map.put("tagID", t.getTagID().toString());
        map.put("name", t.getName());
        map.put("erstellerID",
            t.getErsteller() == null
                ? null
                : t.getErsteller().getStudentID().toString());
        map.put("erstelltAm", t.getErstelltAm());
        return map;
    }

    private Map<String, Object> toOrdnerMap(final Ordner o) {
        Map<String, Object> map = new HashMap<>();
        map.put("ordnerID", o.getOrdnerID().toString());
        map.put("ueberOrdnerID",
            o.getUeberOrdner() == null
                ? null
                : o.getUeberOrdner().getOrdnerID().toString());
        map.put("kursID",
            o.getKurs() == null
                ? null
                : o.getKurs().getKursID().toString());
        map.put("name", o.getName());
        map.put("level", o.getLevel());
        map.put("farbe",
            o.getFarbe() == null ? null : o.getFarbe().name());
        map.put("isPublic", o.isPublic());
        return map;
    }

    private Map<String, Object> toDateiMap(final Datei d) {
        Map<String, Object> map = new HashMap<>();
        map.put("dateiID", d.getDateiID().toString());
        map.put("ordnerID",
            d.getOrdner() == null
                ? null
                : d.getOrdner().getOrdnerID().toString());
        map.put("name", d.getName());
        map.put("zuletztBearbeitet", d.getZuletztBearbeitet());
        map.put("uploaderID",
            d.getUploader() == null
                ? null
                : d.getUploader().getStudentID().toString());
        map.put("fileSize", d.getFileSize());
        map.put("downloadAnzahl", d.getDownloadAnzahl());
        map.put("bewertungSumme", d.getBewertungSumme());
        map.put("bewertungAnzahl", d.getBewertungAnzahl());
        // Bytes: prefer the inline payload when present (DEV) and
        // fall back to the storage strategy reference (PROD).
        byte[] bytes = d.getData();
        if ((bytes == null || bytes.length == 0)
                && d.getDataOid() != null) {
            try {
                bytes = fileStorageStrategy.retrieve(d.getDataOid());
            } catch (RuntimeException ex) {
                colorLogger.colorWarn(
                    "Could not retrieve bytes for datei {}: {}",
                    d.getDateiID(), ex.getMessage());
                bytes = null;
            }
        }
        if (bytes != null) {
            map.put("dataBase64",
                Base64.getEncoder().encodeToString(bytes));
            map.put("dataSha256", sha256Hex(bytes));
        } else {
            map.put("dataBase64", null);
            map.put("dataSha256", null);
        }
        return map;
    }

    private Map<String, Object> toBewertungMap(final Bewertung b) {
        Map<String, Object> map = new HashMap<>();
        map.put("bewertungID", b.getBewertungID().toString());
        map.put("dateiID",
            b.getDatei() == null
                ? null : b.getDatei().getDateiID().toString());
        map.put("autorID",
            b.getAutor() == null
                ? null : b.getAutor().getStudentID().toString());
        map.put("sterne", b.getSterne());
        map.put("bewertetAm", b.getBewertetAm());
        return map;
    }

    private Map<String, Object> toKommentarMap(final Kommentar k) {
        Map<String, Object> map = new HashMap<>();
        map.put("kommentarID", k.getKommentarID().toString());
        map.put("dateiID",
            k.getDatei() == null
                ? null : k.getDatei().getDateiID().toString());
        map.put("autorID",
            k.getAutor() == null
                ? null : k.getAutor().getStudentID().toString());
        map.put("inhalt", k.getInhalt());
        map.put("erstelltAm", k.getErstelltAm());
        map.put("zuletztBearbeitet", k.getZuletztBearbeitet());
        map.put("bearbeitet", k.isBearbeitet());
        return map;
    }

    private Map<String, Object> toDateiAnlegungMap(
            final DateiAnlegung a) {
        Map<String, Object> map = new HashMap<>();
        map.put("anlegungID", a.getAnlegungID().toString());
        map.put("studentID",
            a.getStudent() == null
                ? null : a.getStudent().getStudentID().toString());
        map.put("dateiID",
            a.getDatei() == null
                ? null : a.getDatei().getDateiID().toString());
        map.put("datum", a.getDatum());
        return map;
    }

    private Map<String, Object> toOrdnerAnlegungMap(
            final OrdnerAnlegung a) {
        Map<String, Object> map = new HashMap<>();
        map.put("anlegungID", a.getAnlegungID().toString());
        map.put("studentID",
            a.getStudent() == null
                ? null : a.getStudent().getStudentID().toString());
        map.put("ordnerID",
            a.getOrdner() == null
                ? null : a.getOrdner().getOrdnerID().toString());
        map.put("datum", a.getDatum());
        return map;
    }

    // ---------- primitive helpers ----------

    /**
     * Map a {@link Backup} entity to its DTO.
     *
     * @param b the backup entity
     * @return the DTO representation
     */
    private BackupDTO toDto(final Backup b) {
        return new BackupDTO(
            b.getBackupID(),
            b.getErstelltAm(),
            b.getQuelle(),
            b.getOrdnerAnzahl(),
            b.getDateiAnzahl(),
            b.getKommentarAnzahl(),
            b.getBewertungAnzahl(),
            b.getTagAnzahl());
    }

    /**
     * Canonical form of a tag name used for collision detection
     * during restore — trimmed and lower-cased so {@code "Math"}
     * and {@code "math "} merge into the same row.
     *
     * @param name the original tag name
     * @return the canonical key
     */
    private static String canonicalTagName(final String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Compute the SHA-256 of the given bytes as a lower-case hex
     * string. Used both at snapshot time (for integrity capture)
     * and at restore time (for verification).
     *
     * @param bytes the bytes to hash
     * @return the lower-case hex digest, or {@code null} on
     *     algorithm-not-available (a JVM without SHA-256 cannot
     *     happen in practice)
     */
    private static String sha256Hex(final byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    private static UUID parseUuid(final Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String asString(final Object raw) {
        return raw == null ? null : raw.toString();
    }

    private static int asInt(final Object raw, final int fallback) {
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean asBoolean(
            final Object raw, final boolean fallback) {
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return fallback;
    }

    /**
     * Parse a date from the snapshot's payload. Tolerates both
     * the ISO-8601 string format used by the v2-onwards mapper
     * ({@code "2026-04-25"}) and the legacy array-of-numbers
     * format ({@code [2026, 4, 25]}) that earlier snapshots
     * produced when {@link com.fasterxml.jackson.databind
     * .SerializationFeature#WRITE_DATES_AS_TIMESTAMPS} was on.
     *
     * @param raw the deserialised value from the snapshot
     * @return the parsed date or {@code null} if the input is
     *     missing or unparseable
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDate parseDate(final Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof java.util.List<?> list && list.size() >= 3) {
            try {
                int year = ((Number) list.get(0)).intValue();
                int month = ((Number) list.get(1)).intValue();
                int day = ((Number) list.get(2)).intValue();
                return LocalDate.of(year, month, day);
            } catch (Exception ex) {
                return null;
            }
        }
        try {
            return LocalDate.parse(raw.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Parse a timestamp from the snapshot's payload. Same
     * tolerance as {@link #parseDate}.
     *
     * @param raw the deserialised value from the snapshot
     * @return the parsed timestamp or {@code null}
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime parseDateTime(final Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof java.util.List<?> list && list.size() >= 3) {
            try {
                int year = ((Number) list.get(0)).intValue();
                int month = ((Number) list.get(1)).intValue();
                int day = ((Number) list.get(2)).intValue();
                int hour = list.size() > 3
                    ? ((Number) list.get(3)).intValue() : 0;
                int minute = list.size() > 4
                    ? ((Number) list.get(4)).intValue() : 0;
                int second = list.size() > 5
                    ? ((Number) list.get(5)).intValue() : 0;
                return LocalDateTime.of(
                    year, month, day, hour, minute, second);
            } catch (Exception ex) {
                return null;
            }
        }
        try {
            return LocalDateTime.parse(raw.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Decode a Base64-encoded string into bytes, returning
     * {@code null} when the input is missing or malformed.
     *
     * @param raw the Base64 string
     * @return the decoded bytes, or {@code null}
     */
    private static byte[] decodeBytes(final String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(
                raw.getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

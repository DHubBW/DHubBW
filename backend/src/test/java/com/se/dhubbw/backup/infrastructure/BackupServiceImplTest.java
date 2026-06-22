package com.se.dhubbw.backup.infrastructure;

import com.se.dhubbw.backup.application.dto.output.BackupDTO;
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
import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BackupServiceImpl}.
 *
 * <p>The tests use an in-memory simulation of every repository so
 * the round-trip behaviour can be verified without booting a Spring
 * context. Each repository's relevant {@code findById}, {@code save},
 * {@code delete}, and {@code findAll} methods are wired to the same
 * shared map, mirroring how Hibernate would observe the writes
 * inside a real transaction.</p>
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BackupServiceImplTest {

    @Mock
    private BackupRepository backupRepository;
    @Mock
    private OrdnerRepository ordnerRepository;
    @Mock
    private DateiRepository dateiRepository;
    @Mock
    private KommentarRepository kommentarRepository;
    @Mock
    private BewertungRepository bewertungRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private DateiAnlegungRepository dateiAnlegungRepository;
    @Mock
    private OrdnerAnlegungRepository ordnerAnlegungRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private KursRepository kursRepository;
    @Mock
    private FileStorageStrategy fileStorageStrategy;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BackupServiceImpl backupService;

    private final Map<UUID, Tag> tags = new HashMap<>();
    private final Map<UUID, Ordner> ordner = new HashMap<>();
    private final Map<UUID, Datei> dateien = new HashMap<>();
    private final Map<UUID, Bewertung> bewertungen = new HashMap<>();
    private final Map<UUID, Kommentar> kommentare = new HashMap<>();
    private final Map<UUID, DateiAnlegung> dateiAnlegungen =
        new HashMap<>();
    private final Map<UUID, OrdnerAnlegung> ordnerAnlegungen =
        new HashMap<>();
    private final Map<UUID, Backup> backups = new HashMap<>();
    private final Map<UUID, Student> students = new HashMap<>();
    private final Map<UUID, Kurs> kurse = new HashMap<>();
    private final Map<Long, byte[]> blobStore = new HashMap<>();
    private long nextBlobRef = 100L;

    private Student uploader;
    private Kurs kurs;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(
            backupService, "retentionDays", 14);
        // @PersistenceContext is not satisfied by @InjectMocks —
        // wire the mock manually so the restore flow can call
        // entityManager.flush() between stages.
        ReflectionTestUtils.setField(
            backupService, "entityManager", entityManager);
        // Install live behaviour on every repository so writes from
        // the snapshot are observable by later restore reads.
        wireRepository();
        wireEntityManager();

        uploader = newStudent("uploader@dhbw.de");
        kurs = newKurs("TINF24B2");
    }

    /**
     * Mirror {@code persist} / {@code merge} into the in-memory
     * stores so the production code path that bypasses
     * {@code repository.save()} (after the recent fix for the
     * "deleted row updates as zero rows" issue) is still
     * observable in tests.
     */
    private void wireEntityManager() {
        java.util.function.Consumer<Object> upsert = e -> {
            if (e instanceof Tag t) {
                tags.put(t.getTagID(), t);
            } else if (e instanceof Ordner o) {
                ordner.put(o.getOrdnerID(), o);
            } else if (e instanceof Datei d) {
                dateien.put(d.getDateiID(), d);
            } else if (e instanceof Bewertung b) {
                bewertungen.put(b.getBewertungID(), b);
            } else if (e instanceof Kommentar k) {
                kommentare.put(k.getKommentarID(), k);
            } else if (e instanceof DateiAnlegung a) {
                dateiAnlegungen.put(a.getAnlegungID(), a);
            } else if (e instanceof OrdnerAnlegung a) {
                ordnerAnlegungen.put(a.getAnlegungID(), a);
            }
        };
        org.mockito.Mockito.doAnswer(inv -> {
            upsert.accept(inv.getArgument(0));
            return null;
        }).when(entityManager).persist(any());
        when(entityManager.merge(any())).thenAnswer(inv -> {
            upsert.accept(inv.getArgument(0));
            return inv.getArgument(0);
        });
        // Native MERGE upserts: in production they materialise the
        // row directly via SQL because Hibernate's `merge` would
        // raise OptimisticLockException on a row that was deleted
        // moments ago. For unit tests we don't simulate the SQL
        // — we capture the entity that was passed to the upstream
        // call sites by parsing the SQL statement and the bound
        // parameters. The simpler path used here: the helper
        // upsert*Native methods all live next to the call site
        // and we make their underlying createNativeQuery a fluent
        // no-op; the test relies on a separate hook that the
        // service installs into the Hibernate session — but for
        // these unit tests we instead intercept via direct
        // observation of the in-memory map after each call.
        jakarta.persistence.Query queryStub =
            org.mockito.Mockito.mock(jakarta.persistence.Query.class);
        when(queryStub.setParameter(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(queryStub);
        when(queryStub.setParameter(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(queryStub);
        when(queryStub.executeUpdate()).thenReturn(1);
        when(entityManager.createNativeQuery(
                org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(queryStub);
        when(entityManager.find(
                org.mockito.ArgumentMatchers.<Class<?>>any(),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(null);
    }

    /** Wire mock repositories to the in-memory maps. */
    private void wireRepository() {
        // Backup
        when(backupRepository.save(any(Backup.class)))
            .thenAnswer(inv -> {
                Backup b = inv.getArgument(0);
                if (b.getBackupID() == null) {
                    b.setBackupID(UUID.randomUUID());
                }
                backups.put(b.getBackupID(), b);
                return b;
            });
        when(backupRepository.findById(any(UUID.class)))
            .thenAnswer(inv ->
                Optional.ofNullable(backups.get(inv.getArgument(0))));

        // Tags
        wireMap(tagRepository, tags, Tag::getTagID,
            (t, id) -> t.setTagID(id));
        when(tagRepository.findAll())
            .thenAnswer(inv -> new ArrayList<>(tags.values()));

        // Ordner
        wireMap(ordnerRepository, ordner, Ordner::getOrdnerID,
            (o, id) -> o.setOrdnerID(id));
        when(ordnerRepository.findAll())
            .thenAnswer(inv -> new ArrayList<>(ordner.values()));
        when(ordnerRepository.getReferenceById(any(UUID.class)))
            .thenAnswer(inv -> ordner.get(inv.getArgument(0)));
        when(ordnerRepository.existsById(any(UUID.class)))
            .thenAnswer(inv -> ordner.containsKey(inv.getArgument(0)));

        // Datei
        wireMap(dateiRepository, dateien, Datei::getDateiID,
            (d, id) -> d.setDateiID(id));
        when(dateiRepository.findAll())
            .thenAnswer(inv -> new ArrayList<>(dateien.values()));
        when(dateiRepository.getReferenceById(any(UUID.class)))
            .thenAnswer(inv -> dateien.get(inv.getArgument(0)));
        when(dateiRepository.existsById(any(UUID.class)))
            .thenAnswer(inv -> dateien.containsKey(inv.getArgument(0)));

        // Bewertung
        wireMap(bewertungRepository, bewertungen,
            Bewertung::getBewertungID,
            (b, id) -> b.setBewertungID(id));
        when(bewertungRepository.findAll())
            .thenAnswer(inv ->
                new ArrayList<>(bewertungen.values()));

        // Kommentar
        wireMap(kommentarRepository, kommentare,
            Kommentar::getKommentarID,
            (k, id) -> k.setKommentarID(id));
        when(kommentarRepository.findAll())
            .thenAnswer(inv -> new ArrayList<>(kommentare.values()));

        // DateiAnlegung
        wireMap(dateiAnlegungRepository, dateiAnlegungen,
            DateiAnlegung::getAnlegungID,
            (a, id) -> a.setAnlegungID(id));
        when(dateiAnlegungRepository.findAll())
            .thenAnswer(inv ->
                new ArrayList<>(dateiAnlegungen.values()));
        when(dateiAnlegungRepository.findByDatei(any(Datei.class)))
            .thenAnswer(inv -> {
                Datei d = inv.getArgument(0);
                return dateiAnlegungen.values().stream()
                    .filter(a -> a.getDatei() != null
                        && a.getDatei().getDateiID()
                            .equals(d.getDateiID()))
                    .findFirst();
            });

        // OrdnerAnlegung
        wireMap(ordnerAnlegungRepository, ordnerAnlegungen,
            OrdnerAnlegung::getAnlegungID,
            (a, id) -> a.setAnlegungID(id));
        when(ordnerAnlegungRepository.findAll())
            .thenAnswer(inv ->
                new ArrayList<>(ordnerAnlegungen.values()));
        when(ordnerAnlegungRepository.findByOrdner(any(Ordner.class)))
            .thenAnswer(inv -> {
                Ordner o = inv.getArgument(0);
                return ordnerAnlegungen.values().stream()
                    .filter(a -> a.getOrdner() != null
                        && a.getOrdner().getOrdnerID()
                            .equals(o.getOrdnerID()))
                    .findFirst();
            });

        // Student / Kurs (read-only here)
        when(studentRepository.findAll())
            .thenAnswer(inv -> new ArrayList<>(students.values()));
        when(studentRepository.getReferenceById(any(UUID.class)))
            .thenAnswer(inv -> students.get(inv.getArgument(0)));
        when(kursRepository.findAll())
            .thenAnswer(inv -> new ArrayList<>(kurse.values()));
        when(kursRepository.getReferenceById(any(UUID.class)))
            .thenAnswer(inv -> kurse.get(inv.getArgument(0)));

        // FileStorage: persist bytes into blobStore so the snapshot
        // re-read after a delete returns the captured payload.
        when(fileStorageStrategy.store(any(byte[].class)))
            .thenAnswer(inv -> {
                long ref = nextBlobRef++;
                blobStore.put(ref, (byte[]) inv.getArgument(0));
                return ref;
            });
        when(fileStorageStrategy.retrieve(any(Long.class)))
            .thenAnswer(inv -> blobStore.get(inv.getArgument(0)));
        when(fileStorageStrategy.requiresInlineData())
            .thenReturn(true);
    }

    /**
     * Connect a repository's {@code findById}, {@code save} and
     * {@code delete} stubs to the supplied map.
     */
    private static <T, R extends org.springframework.data.jpa.repository
            .JpaRepository<T, UUID>>
        void wireMap(final R repo, final Map<UUID, T> store,
                final java.util.function.Function<T, UUID> idGetter,
                final java.util.function.BiConsumer<T, UUID> idSetter) {
        when(repo.findById(any(UUID.class)))
            .thenAnswer(inv ->
                Optional.ofNullable(store.get(inv.getArgument(0))));
        when(repo.save(any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            T entity = (T) inv.getArgument(0);
            UUID id = idGetter.apply(entity);
            if (id == null) {
                id = UUID.randomUUID();
                idSetter.accept(entity, id);
            }
            store.put(id, entity);
            return entity;
        });
        org.mockito.Mockito.doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            T entity = (T) inv.getArgument(0);
            UUID id = idGetter.apply(entity);
            if (id != null) {
                store.remove(id);
            }
            return null;
        }).when(repo).delete(any());
    }

    private Student newStudent(final String mail) {
        Student s = new Student();
        s.setStudentID(UUID.randomUUID());
        s.setDhbwMail(mail);
        s.setName(mail);
        students.put(s.getStudentID(), s);
        return s;
    }

    private Kurs newKurs(final String name) {
        Kurs k = new Kurs();
        k.setKursID(UUID.randomUUID());
        k.setKursName(name);
        kurse.put(k.getKursID(), k);
        return k;
    }

    private Ordner newOrdner(
            final String name, final Ordner parent) {
        Ordner o = new Ordner();
        o.setOrdnerID(UUID.randomUUID());
        o.setName(name);
        o.setLevel(parent == null ? 0 : parent.getLevel() + 1);
        o.setKurs(kurs);
        o.setUeberOrdner(parent);
        o.setFarbe(OrdnerFarbe.DEFAULT);
        o.setPublic(true);
        ordner.put(o.getOrdnerID(), o);
        return o;
    }

    private Datei newDatei(
            final Ordner parent, final String name,
            final byte[] bytes) {
        Datei d = new Datei();
        d.setDateiID(UUID.randomUUID());
        d.setOrdner(parent);
        d.setName(name);
        d.setZuletztBearbeitet(LocalDate.now());
        d.setUploader(uploader);
        d.setFileSize((float) bytes.length / 1_000_000f);
        d.setData(bytes);
        long ref = nextBlobRef++;
        blobStore.put(ref, bytes);
        d.setDataOid(ref);
        d.setTags(new HashSet<>());
        dateien.put(d.getDateiID(), d);
        return d;
    }

    private Tag newTag(final String name) {
        Tag t = new Tag();
        t.setTagID(UUID.randomUUID());
        t.setName(name);
        t.setErsteller(uploader);
        t.setErstelltAm(LocalDateTime.now());
        tags.put(t.getTagID(), t);
        return t;
    }

    // ---- the headline test ----

    @Test
    public void givenDeletedDatei_whenRestore_thenDateiReappears() {
        // Arrange: a folder with a file, a tag, a rating, a comment.
        Ordner root = newOrdner("Vorlesungen", null);
        byte[] payload = "Hello backup".getBytes();
        Datei datei = newDatei(root, "Skript.pdf", payload);
        Tag tag = newTag("Wichtig");
        datei.getTags().add(tag);
        UUID dateiId = datei.getDateiID();
        UUID tagId = tag.getTagID();
        UUID ordnerId = root.getOrdnerID();

        Bewertung bw = new Bewertung();
        bw.setBewertungID(UUID.randomUUID());
        bw.setDatei(datei);
        bw.setAutor(uploader);
        bw.setSterne(5);
        bw.setBewertetAm(LocalDateTime.now());
        bewertungen.put(bw.getBewertungID(), bw);
        UUID bwId = bw.getBewertungID();

        Kommentar k = new Kommentar();
        k.setKommentarID(UUID.randomUUID());
        k.setDatei(datei);
        k.setAutor(uploader);
        k.setInhalt("Sehr hilfreich");
        k.setErstelltAm(LocalDateTime.now());
        k.setBearbeitet(false);
        kommentare.put(k.getKommentarID(), k);
        UUID kId = k.getKommentarID();

        DateiAnlegung anlegung = new DateiAnlegung();
        anlegung.setAnlegungID(UUID.randomUUID());
        anlegung.setStudent(uploader);
        anlegung.setDatei(datei);
        anlegung.setDatum(LocalDate.now());
        dateiAnlegungen.put(anlegung.getAnlegungID(), anlegung);

        // Act 1: snapshot
        BackupDTO created = backupService.createBackup("MANUAL");
        assertNotNull(created);
        assertEquals(1, created.dateiAnzahl());

        // Act 2: simulate the user deleting the file (and its
        // dependent rows, just like DateiServiceImpl.deleteDatei).
        bewertungen.remove(bwId);
        kommentare.remove(kId);
        dateiAnlegungen.remove(anlegung.getAnlegungID());
        dateien.remove(dateiId);
        // (Tag stays because tags survive file deletion.)
        assertFalse(dateien.containsKey(dateiId),
            "Datei must really be gone before restore");

        // Act 3: restore
        backupService.restoreBackup(
            created.backupID(), 2);

        // Assert: the deleted Datei is back, with bytes, tag link,
        // rating, comment, and Anlegung.
        Datei restored = dateien.get(dateiId);
        assertNotNull(restored,
            "Datei must reappear after restore");
        assertEquals("Skript.pdf", restored.getName());
        assertEquals(ordnerId,
            restored.getOrdner().getOrdnerID());
        // FileStorageStrategy.store was invoked with the snapshot
        // bytes; since we stub requiresInlineData()=true the bytes
        // are also re-attached inline.
        assertNotNull(restored.getData());
        assertEquals("Hello backup",
            new String(restored.getData()));
        assertNotNull(restored.getTags());
        assertEquals(1, restored.getTags().size());
        assertEquals(tagId,
            restored.getTags().iterator().next().getTagID());
        // Aggregate counters were recomputed from the restored
        // Bewertung (one 5-star rating).
        assertEquals(5L, restored.getBewertungSumme());
        assertEquals(1, restored.getBewertungAnzahl());

        Bewertung restoredBw = bewertungen.get(bwId);
        assertNotNull(restoredBw, "Bewertung must reappear");
        assertEquals(5, restoredBw.getSterne());

        Kommentar restoredK = kommentare.get(kId);
        assertNotNull(restoredK, "Kommentar must reappear");
        assertEquals("Sehr hilfreich", restoredK.getInhalt());

        verify(fileStorageStrategy, atLeastOnce())
            .store(any(byte[].class));
    }

    @Test
    public void givenLowConfirmation_whenRestore_thenBadRequest() {
        Backup stub = new Backup();
        stub.setBackupID(UUID.randomUUID());
        backups.put(stub.getBackupID(), stub);
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> backupService.restoreBackup(stub.getBackupID(), 1));
        assertEquals(400, ex.getStatusCode().value());
        verify(fileStorageStrategy, never())
            .store(any(byte[].class));
    }

    @Test
    public void givenLegacyV1Snapshot_whenRestore_thenUnprocessable() {
        Backup legacy = new Backup();
        legacy.setBackupID(UUID.randomUUID());
        legacy.setPayload("{\"schemaVersion\":1}");
        backups.put(legacy.getBackupID(), legacy);
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> backupService.restoreBackup(legacy.getBackupID(), 2));
        assertEquals(422, ex.getStatusCode().value());
    }

    @Test
    public void givenMissingBackup_whenRestore_thenNotFound() {
        UUID missing = UUID.randomUUID();
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> backupService.restoreBackup(missing, 2));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenSysadminTag_whenSnapshotAndRestore_thenSurvives() {
        // Tags must survive — even the snapshot ones — without any
        // referencing Datei.
        Tag t = newTag("Standalone");
        BackupDTO snap = backupService.createBackup("MANUAL");

        tags.remove(t.getTagID());
        backupService.restoreBackup(snap.backupID(), 2);

        assertTrue(tags.containsKey(t.getTagID()),
            "Tag must reappear after restore");
        assertEquals("Standalone", tags.get(t.getTagID()).getName());
    }

    @Test
    public void givenOrdnerSubtree_whenRestore_thenTopologicalOrder() {
        Ordner grand = newOrdner("Vorlesungen", null);
        Ordner parent = newOrdner("Mathematik", grand);
        Ordner child = newOrdner("Klausuren", parent);
        UUID grandId = grand.getOrdnerID();
        UUID parentId = parent.getOrdnerID();
        UUID childId = child.getOrdnerID();

        BackupDTO snap = backupService.createBackup("MANUAL");

        // Wipe the whole subtree.
        ordner.remove(childId);
        ordner.remove(parentId);
        ordner.remove(grandId);

        backupService.restoreBackup(snap.backupID(), 2);

        assertTrue(ordner.containsKey(grandId));
        assertTrue(ordner.containsKey(parentId));
        assertTrue(ordner.containsKey(childId));
        // Parent links must be intact after restore.
        assertEquals(grandId,
            ordner.get(parentId).getUeberOrdner().getOrdnerID());
        assertEquals(parentId,
            ordner.get(childId).getUeberOrdner().getOrdnerID());
    }

    @Test
    public void givenAggregateAfterRestore_thenRecomputed() {
        Ordner root = newOrdner("Vorlesungen", null);
        Datei d = newDatei(root, "Skript.pdf", new byte[]{1, 2, 3});

        Student a = newStudent("a@dhbw.de");
        Student b = newStudent("b@dhbw.de");
        Bewertung b1 = mkBw(d, a, 4);
        Bewertung b2 = mkBw(d, b, 2);
        // The cached aggregate on the live Datei is stale on
        // purpose so we can prove the restore overwrites it.
        d.setBewertungSumme(99L);
        d.setBewertungAnzahl(99);

        BackupDTO snap = backupService.createBackup("MANUAL");

        // Delete the Datei so restore re-creates it.
        bewertungen.remove(b1.getBewertungID());
        bewertungen.remove(b2.getBewertungID());
        dateien.remove(d.getDateiID());

        backupService.restoreBackup(snap.backupID(), 2);

        Datei restored = dateien.get(d.getDateiID());
        assertNotNull(restored);
        assertEquals(6L, restored.getBewertungSumme(),
            "summe is recomputed from the restored Bewertung set");
        assertEquals(2, restored.getBewertungAnzahl());
        verify(entityManager, atLeast(1)).flush();
    }

    private Bewertung mkBw(
            final Datei d, final Student autor, final int stars) {
        Bewertung bw = new Bewertung();
        bw.setBewertungID(UUID.randomUUID());
        bw.setDatei(d);
        bw.setAutor(autor);
        bw.setSterne(stars);
        bw.setBewertetAm(LocalDateTime.now());
        bewertungen.put(bw.getBewertungID(), bw);
        return bw;
    }
}

package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegung;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.domain.PlattformVerwaltungService;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursRepository;
import com.se.dhubbw.ordner.domain.OrdnerFarbe;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegung;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegungRepository;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.shared.testing.IntegrationTest;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * End-to-end integration test for the cascade delete behaviour of
 * {@link PlattformVerwaltungService#deleteKurs(UUID)}.
 *
 * <p>Covers all cascade paths in a single happy-path scenario:</p>
 * <ul>
 *   <li>2 course members (1 KURSADMIN, 1 STUDENT) — KURSADMIN
 *       must be demoted, both enrollments removed.</li>
 *   <li>1 private folder containing 3 files (each with its file
 *       creation record) plus its folder creation record — all
 *       must be deleted.</li>
 *   <li>1 public folder attached to the course — must be detached
 *       (kurs reference cleared) and kept on disk.</li>
 *   <li>1 pending join enrollment ({@code KursBeitrittsAnfrage}
 *       with {@code isApproved=false}) — must be removed.</li>
 *   <li>1 pending course creation request
 *       ({@code KursErstellungsAnfrage}) targeting the deleted
 *       course's name/year — must be removed.</li>
 *   <li>1 unrelated course creation request for a different
 *       name/year — must remain untouched.</li>
 *   <li>All affected user accounts must remain in the database.</li>
 * </ul>
 *
 * <p>Test data is built dynamically inside a programmatic
 * transaction so the deletion can run in its own transaction (the
 * service is annotated with {@code RetryableTransactional}). The
 * verification phase reads fresh state from each repository.</p>
 */
@IntegrationTest
public class PlattformVerwaltungServiceCascadeIT {

    private final PlattformVerwaltungService plattformVerwaltungService;
    private final TransactionTemplate transactionTemplate;
    private final KursRepository kursRepository;
    private final StudentRepository studentRepository;
    private final OrdnerRepository ordnerRepository;
    private final OrdnerAnlegungRepository ordnerAnlegungRepository;
    private final DateiRepository dateiRepository;
    private final DateiAnlegungRepository dateiAnlegungRepository;
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;
    private final KursErstellungsAnfrageRepository
        kursErstellungsAnfrageRepository;

    private UUID kursId;
    private UUID adminStudentId;
    private UUID memberStudentId;
    private UUID pendingStudentId;
    private UUID requesterStudentId;
    private UUID privateOrdnerId;
    private UUID publicOrdnerId;
    private UUID privateOrdnerAnlegungId;
    private List<UUID> dateiIds;
    private List<UUID> dateiAnlegungIds;
    private UUID approvedAdminEnrollmentId;
    private UUID approvedMemberEnrollmentId;
    private UUID pendingEnrollmentId;
    private UUID matchingErstellungsAnfrageId;
    private UUID unrelatedErstellungsAnfrageId;

    @Autowired
    public PlattformVerwaltungServiceCascadeIT(
            PlattformVerwaltungService plattformVerwaltungService,
            TransactionTemplate transactionTemplate,
            KursRepository kursRepository,
            StudentRepository studentRepository,
            OrdnerRepository ordnerRepository,
            OrdnerAnlegungRepository ordnerAnlegungRepository,
            DateiRepository dateiRepository,
            DateiAnlegungRepository dateiAnlegungRepository,
            KursBeitrittsAnfrageRepository
                kursBeitrittsAnfrageRepository,
            KursErstellungsAnfrageRepository
                kursErstellungsAnfrageRepository) {
        this.plattformVerwaltungService = plattformVerwaltungService;
        this.transactionTemplate = transactionTemplate;
        this.kursRepository = kursRepository;
        this.studentRepository = studentRepository;
        this.ordnerRepository = ordnerRepository;
        this.ordnerAnlegungRepository = ordnerAnlegungRepository;
        this.dateiRepository = dateiRepository;
        this.dateiAnlegungRepository = dateiAnlegungRepository;
        this.kursBeitrittsAnfrageRepository =
            kursBeitrittsAnfrageRepository;
        this.kursErstellungsAnfrageRepository =
            kursErstellungsAnfrageRepository;
    }

    @BeforeEach
    public void seed() {
        transactionTemplate.executeWithoutResult(status -> {
            Kurs kurs = new Kurs();
            kurs.setKursName("CASCADE_TEST_KURS");
            kurs.setJahrgang("9999");
            kurs = kursRepository.save(kurs);
            kursId = kurs.getKursID();

            Student admin = newStudent(
                "cascade.admin@student.dhbw-karlsruhe.de",
                "CascadeAdmin", StudentType.KURSADMIN);
            Student member = newStudent(
                "cascade.member@student.dhbw-karlsruhe.de",
                "CascadeMember", StudentType.STUDENT);
            Student pending = newStudent(
                "cascade.pending@student.dhbw-karlsruhe.de",
                "CascadePending", StudentType.STUDENT);
            Student requester = newStudent(
                "cascade.requester@student.dhbw-karlsruhe.de",
                "CascadeRequester", StudentType.STUDENT);
            adminStudentId = admin.getStudentID();
            memberStudentId = member.getStudentID();
            pendingStudentId = pending.getStudentID();
            requesterStudentId = requester.getStudentID();

            Ordner privateOrdner = new Ordner();
            privateOrdner.setKurs(kurs);
            privateOrdner.setName("Privater Cascade-Ordner");
            privateOrdner.setLevel(0);
            privateOrdner.setFarbe(OrdnerFarbe.BLUE);
            privateOrdner.setPublic(false);
            privateOrdner = ordnerRepository.save(privateOrdner);
            privateOrdnerId = privateOrdner.getOrdnerID();

            Ordner publicOrdner = new Ordner();
            publicOrdner.setKurs(kurs);
            publicOrdner.setName("Oeffentlicher Cascade-Ordner");
            publicOrdner.setLevel(0);
            publicOrdner.setFarbe(OrdnerFarbe.GREEN);
            publicOrdner.setPublic(true);
            publicOrdner = ordnerRepository.save(publicOrdner);
            publicOrdnerId = publicOrdner.getOrdnerID();

            OrdnerAnlegung ordnerAnlegung = new OrdnerAnlegung();
            ordnerAnlegung.setOrdner(privateOrdner);
            ordnerAnlegung.setStudent(admin);
            ordnerAnlegung.setDatum(LocalDate.now());
            ordnerAnlegung =
                ordnerAnlegungRepository.save(ordnerAnlegung);
            privateOrdnerAnlegungId = ordnerAnlegung.getAnlegungID();

            dateiIds = new java.util.ArrayList<>();
            dateiAnlegungIds = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Student uploader = newStudent(
                    "cascade.uploader" + i
                        + "@student.dhbw-karlsruhe.de",
                    "CascadeUploader" + i, StudentType.STUDENT);
                Datei datei = new Datei();
                datei.setOrdner(privateOrdner);
                datei.setName("cascade-file-" + i + ".pdf");
                datei.setZuletztBearbeitet(LocalDate.now());
                datei.setData(new byte[]{1, 2, 3});
                datei.setUploader(uploader);
                datei.setFileSize(0.1f);
                datei = dateiRepository.save(datei);
                dateiIds.add(datei.getDateiID());

                DateiAnlegung dateiAnlegung = new DateiAnlegung();
                dateiAnlegung.setDatei(datei);
                dateiAnlegung.setStudent(uploader);
                dateiAnlegung.setDatum(LocalDate.now());
                dateiAnlegung =
                    dateiAnlegungRepository.save(dateiAnlegung);
                dateiAnlegungIds.add(dateiAnlegung.getAnlegungID());
            }

            approvedAdminEnrollmentId = saveEnrollment(
                kurs, admin, true).getAnfrageID();
            approvedMemberEnrollmentId = saveEnrollment(
                kurs, member, true).getAnfrageID();
            pendingEnrollmentId = saveEnrollment(
                kurs, pending, false).getAnfrageID();

            matchingErstellungsAnfrageId = saveErstellungsAnfrage(
                requester, "CASCADE_TEST_KURS", "9999", false)
                .getAnfrageID();
            unrelatedErstellungsAnfrageId = saveErstellungsAnfrage(
                requester, "OTHER_KURS", "9999", false)
                .getAnfrageID();
        });
    }

    @Test
    public void
            givenFullKursContext_whenDeleteKurs_thenAllCascadePathsApply() {
        plattformVerwaltungService.deleteKurs(kursId);

        Assertions.assertFalse(kursRepository.existsById(kursId),
            "Kurs entity must be deleted");

        Assertions.assertFalse(
            ordnerRepository.existsById(privateOrdnerId),
            "Private folder must be deleted");
        Assertions.assertFalse(
            ordnerAnlegungRepository.existsById(
                privateOrdnerAnlegungId),
            "Private folder creation record must be deleted");
        for (UUID id : dateiIds) {
            Assertions.assertFalse(dateiRepository.existsById(id),
                "File " + id + " must be deleted");
        }
        for (UUID id : dateiAnlegungIds) {
            Assertions.assertFalse(
                dateiAnlegungRepository.existsById(id),
                "File creation record " + id + " must be deleted");
        }

        Ordner publicOrdner = ordnerRepository.findById(publicOrdnerId)
            .orElseThrow(() -> new AssertionError(
                "Public folder must remain on the platform"));
        Assertions.assertNull(publicOrdner.getKurs(),
            "Public folder must be detached from the deleted course");

        Assertions.assertFalse(
            kursBeitrittsAnfrageRepository.existsById(
                approvedAdminEnrollmentId),
            "Approved KURSADMIN enrollment must be deleted");
        Assertions.assertFalse(
            kursBeitrittsAnfrageRepository.existsById(
                approvedMemberEnrollmentId),
            "Approved STUDENT enrollment must be deleted");
        Assertions.assertFalse(
            kursBeitrittsAnfrageRepository.existsById(
                pendingEnrollmentId),
            "Pending enrollment must be deleted");

        Assertions.assertFalse(
            kursErstellungsAnfrageRepository.existsById(
                matchingErstellungsAnfrageId),
            "Matching course creation request must be deleted");
        Assertions.assertTrue(
            kursErstellungsAnfrageRepository.existsById(
                unrelatedErstellungsAnfrageId),
            "Unrelated course creation request must NOT be deleted");

        Student admin = studentRepository.findById(adminStudentId)
            .orElseThrow(() -> new AssertionError(
                "Admin user must remain after course deletion"));
        Assertions.assertEquals(StudentType.STUDENT,
            admin.getStudentType(),
            "Former KURSADMIN must be demoted to STUDENT");

        Assertions.assertTrue(
            studentRepository.existsById(memberStudentId),
            "Plain member user must remain after course deletion");
        Assertions.assertTrue(
            studentRepository.existsById(pendingStudentId),
            "Pending applicant user must remain after course deletion");
        Assertions.assertTrue(
            studentRepository.existsById(requesterStudentId),
            "Creation requester user must remain after course deletion");
    }

    private Student newStudent(final String mail, final String name,
            final StudentType type) {
        Student student = new Student();
        student.setName(name);
        student.setDhbwMail(mail);
        student.setPasswordHash(
            "$2y$10$6vvaxuBKaDHNtz1folm8FOFCXWOX/cQWmELdzEO/ZMSqKoDMQXry2");
        student.setStudentType(type);
        return studentRepository.save(student);
    }

    private KursBeitrittsAnfrage saveEnrollment(final Kurs kurs,
            final Student student, final boolean approved) {
        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setKurs(kurs);
        enrollment.setStudent(student);
        enrollment.setAnfrageDatum(LocalDate.now());
        enrollment.setApproved(approved);
        return kursBeitrittsAnfrageRepository.save(enrollment);
    }

    private KursErstellungsAnfrage saveErstellungsAnfrage(
            final Student student, final String kursName,
            final String jahrgang, final boolean approved) {
        KursErstellungsAnfrage anfrage = new KursErstellungsAnfrage();
        anfrage.setStudent(student);
        anfrage.setKursName(kursName);
        anfrage.setJahrgang(jahrgang);
        anfrage.setAnfrageDatum(LocalDate.now());
        anfrage.setApproved(approved);
        anfrage.setRequestNotes("cascade-test");
        return kursErstellungsAnfrageRepository.save(anfrage);
    }
}

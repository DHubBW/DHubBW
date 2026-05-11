package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.application.dto.KursAdminStatsDTO;
import com.se.dhubbw.kurs.application.dto.KursmemberDTO;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KursMitgliederServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    @Mock
    private DateiRepository dateiRepository;

    @InjectMocks
    private KursMitgliederService kursMitgliederService;

    private Student admin;
    private Student member;
    private Kurs kurs;
    private KursBeitrittsAnfrage adminAnfrage;
    private KursBeitrittsAnfrage memberAnfrage;

    @BeforeEach
    public void setUp() {
        kurs = new Kurs();
        kurs.setKursID(UUID.randomUUID());

        admin = new Student();
        admin.setStudentID(UUID.randomUUID());
        admin.setDhbwMail("admin@dhbw.de");
        admin.setStudentType(StudentType.KURSADMIN);

        member = new Student();
        member.setStudentID(UUID.randomUUID());
        member.setDhbwMail("member@dhbw.de");
        member.setStudentType(StudentType.STUDENT);

        adminAnfrage = new KursBeitrittsAnfrage();
        adminAnfrage.setKurs(kurs);
        adminAnfrage.setStudent(admin);
        adminAnfrage.setApproved(true);

        memberAnfrage = new KursBeitrittsAnfrage();
        memberAnfrage.setKurs(kurs);
        memberAnfrage.setStudent(member);
        memberAnfrage.setApproved(true);
    }

    @Test
    public void givenAdminWithCourse_whenRetrieveStats_thenReturnsStats() {
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(adminAnfrage, memberAnfrage));
        when(dateiRepository
            .findByKursIdOrderByZuletztBearbeitetDesc(
                kurs.getKursID()))
            .thenReturn(Collections.emptyList());
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedFalse(kurs))
            .thenReturn(Collections.emptyList());

        KursAdminStatsDTO stats =
            kursMitgliederService.retrieveKursAdminStats(
                "admin@dhbw.de");

        assertNotNull(stats);
        assertEquals(2, stats.getAnzahlMitglieder());
        assertEquals(0, stats.getAnzahlDateien());
        assertEquals(0, stats.getAnzahlAnfragen());
    }

    @Test
    public void givenUnknownAdmin_whenRetrieveStats_thenThrows() {
        when(studentRepository.findByDhbwMail("unknown@dhbw.de"))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> kursMitgliederService.retrieveKursAdminStats(
                "unknown@dhbw.de"));
    }

    @Test
    public void givenAdminWithCourse_whenRetrieveMembers_thenReturnsList() {
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(adminAnfrage, memberAnfrage));

        List<KursmemberDTO> result =
            kursMitgliederService.retrieveAllCourseMembers(
                "admin@dhbw.de");

        assertEquals(2, result.size());
    }

    @Test
    public void givenTwoKas_whenResign_thenChangesType() {
        Student secondKa = new Student();
        secondKa.setStudentID(UUID.randomUUID());
        secondKa.setStudentType(StudentType.KURSADMIN);

        KursBeitrittsAnfrage secondKaAnfrage =
            new KursBeitrittsAnfrage();
        secondKaAnfrage.setKurs(kurs);
        secondKaAnfrage.setStudent(secondKa);
        secondKaAnfrage.setApproved(true);

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(adminAnfrage, secondKaAnfrage));

        kursMitgliederService.kursadminNiederlegen("admin@dhbw.de");

        assertEquals(StudentType.STUDENT, admin.getStudentType());
        verify(studentRepository).save(admin);
    }

    @Test
    public void givenOnlyOneKa_whenResign_thenThrowsConflict() {
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(adminAnfrage));

        assertThrows(IllegalStateException.class,
            () -> kursMitgliederService.kursadminNiederlegen(
                "admin@dhbw.de"));
    }

    @Test
    public void givenStudentMember_whenAddKursadmin_thenChangesType() {
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(member.getStudentID()))
            .thenReturn(Optional.of(member));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(member))
            .thenReturn(Optional.of(memberAnfrage));

        kursMitgliederService.addKursadmin(
            member.getStudentID(), "admin@dhbw.de");

        assertEquals(StudentType.KURSADMIN, member.getStudentType());
        verify(studentRepository).save(member);
    }

    @Test
    public void givenSystemAdmin_whenAddKursadmin_thenThrowsBadRequest() {
        member.setStudentType(StudentType.SYSTEMADMIN);

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(member.getStudentID()))
            .thenReturn(Optional.of(member));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(member))
            .thenReturn(Optional.of(memberAnfrage));

        assertThrows(IllegalArgumentException.class,
            () -> kursMitgliederService.addKursadmin(
                member.getStudentID(), "admin@dhbw.de"));
    }

    @Test
    public void givenAlreadyKa_whenAddKursadmin_thenThrowsConflict() {
        member.setStudentType(StudentType.KURSADMIN);

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(member.getStudentID()))
            .thenReturn(Optional.of(member));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(member))
            .thenReturn(Optional.of(memberAnfrage));

        assertThrows(IllegalStateException.class,
            () -> kursMitgliederService.addKursadmin(
                member.getStudentID(), "admin@dhbw.de"));
    }

    @Test
    public void givenUnknownStudentId_whenAddKursadmin_thenThrows() {
        UUID unknownId = UUID.randomUUID();

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> kursMitgliederService.addKursadmin(
                unknownId, "admin@dhbw.de"));
    }

    @Test
    public void givenMemberNotInCourse_whenAddKursadmin_thenThrows() {
        Kurs otherKurs = new Kurs();
        otherKurs.setKursID(UUID.randomUUID());
        KursBeitrittsAnfrage otherAnfrage =
            new KursBeitrittsAnfrage();
        otherAnfrage.setKurs(otherKurs);
        otherAnfrage.setStudent(member);

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(member.getStudentID()))
            .thenReturn(Optional.of(member));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(member))
            .thenReturn(Optional.of(otherAnfrage));

        assertThrows(NoSuchElementException.class,
            () -> kursMitgliederService.addKursadmin(
                member.getStudentID(), "admin@dhbw.de"));
    }

    @Test
    public void givenKaMemberAndOtherKa_whenRemoveKursadmin_thenChangesType() {
        member.setStudentType(StudentType.KURSADMIN);

        Student otherKa = new Student();
        otherKa.setStudentID(UUID.randomUUID());
        otherKa.setStudentType(StudentType.KURSADMIN);
        KursBeitrittsAnfrage otherKaAnfrage =
            new KursBeitrittsAnfrage();
        otherKaAnfrage.setKurs(kurs);
        otherKaAnfrage.setStudent(otherKa);

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(member.getStudentID()))
            .thenReturn(Optional.of(member));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(member))
            .thenReturn(Optional.of(memberAnfrage));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(adminAnfrage, memberAnfrage,
                otherKaAnfrage));

        kursMitgliederService.removeKursadmin(
            member.getStudentID(), "admin@dhbw.de");

        assertEquals(StudentType.STUDENT, member.getStudentType());
        verify(studentRepository).save(member);
    }

    @Test
    public void givenOnlyOneKa_whenRemoveKursadmin_thenThrowsConflict() {
        member.setStudentType(StudentType.KURSADMIN);
        admin.setStudentType(StudentType.STUDENT);

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(member.getStudentID()))
            .thenReturn(Optional.of(member));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(member))
            .thenReturn(Optional.of(memberAnfrage));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(adminAnfrage, memberAnfrage));

        assertThrows(IllegalStateException.class,
            () -> kursMitgliederService.removeKursadmin(
                member.getStudentID(), "admin@dhbw.de"));
    }

    @Test
    public void givenNotKa_whenRemoveKursadmin_thenThrowsBadRequest() {
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(member.getStudentID()))
            .thenReturn(Optional.of(member));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(member))
            .thenReturn(Optional.of(memberAnfrage));

        assertThrows(IllegalArgumentException.class,
            () -> kursMitgliederService.removeKursadmin(
                member.getStudentID(), "admin@dhbw.de"));
    }

    @Test
    public void givenStudentMember_whenRemoveMember_thenDeletes() {
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(member.getStudentID()))
            .thenReturn(Optional.of(member));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(member))
            .thenReturn(Optional.of(memberAnfrage));

        kursMitgliederService.removeKursMember(
            member.getStudentID(), "admin@dhbw.de");

        verify(kursBeitrittsAnfrageRepository).delete(memberAnfrage);
    }

    @Test
    public void givenKaMember_whenRemoveMember_thenThrowsConflict() {
        member.setStudentType(StudentType.KURSADMIN);

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(member.getStudentID()))
            .thenReturn(Optional.of(member));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(member))
            .thenReturn(Optional.of(memberAnfrage));

        assertThrows(IllegalStateException.class,
            () -> kursMitgliederService.removeKursMember(
                member.getStudentID(), "admin@dhbw.de"));
    }

    @Test
    public void givenUnknownStudent_whenRemoveMember_thenThrows() {
        UUID unknownId = UUID.randomUUID();

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(admin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin))
            .thenReturn(Optional.of(adminAnfrage));
        when(studentRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> kursMitgliederService.removeKursMember(
                unknownId, "admin@dhbw.de"));
    }
}

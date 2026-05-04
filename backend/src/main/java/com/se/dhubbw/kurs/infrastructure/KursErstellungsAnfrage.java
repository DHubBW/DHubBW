package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.student.infrastructure.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for course creation requests.
 *
 * <p>The referenced course does not yet exist when the request is
 * created, so the desired course is described via {@code kursName}
 * and {@code jahrgang} rather than a foreign key to {@link Kurs}.</p>
 */
@Entity
@Table(name = "kurs_erstellungs_anfrage")
public class KursErstellungsAnfrage {

    /**
     * Request identifier.
     */
    @Id
    @Getter
    @Setter
    @Column(name = "anfrage_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID anfrageID;

    /**
     * Student requesting the course creation.
     */
    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    /**
     * Desired name of the course to be created.
     */
    @Getter
    @Setter
    @Column(name = "kurs_name")
    private String kursName;

    /**
     * Desired cohort year of the course to be created.
     */
    @Getter
    @Setter
    @Column(name = "jahrgang")
    private String jahrgang;

    /**
     * Request submission date.
     */
    @Getter
    @Setter
    @Column(name = "anfrage_datum")
    private LocalDate anfrageDatum;

    /**
     * Approval status.
     */
    @Getter
    @Setter
    @Column(name = "is_approved")
    @ColumnDefault("false")
    private boolean isApproved;

    /**
     * Optional notes for the request.
     */
    @Getter
    @Setter
    @Column(name = "request_notes")
    @ColumnDefault("'No notes were provided.'")
    private String requestNotes;
}

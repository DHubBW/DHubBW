package com.se.dhubbw.kurs.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
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
 * JPA entity for course enrollment requests (join requests).
 * Stores a student's application to join a course with approval status.
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Infrastructure Layer (data persistence layer)
 * @architecture.category T-Software (JPA entity mapping)
 */
@Table(name = "kurs_beitritts_anfrage")
@Entity
@Getter
@Setter
public class KursBeitrittsAnfrage {

    /**
     * Request identifier.
     */
    @Id
    @Column(name = "anfrage_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID anfrageID;

    /**
     * Target course.
     */
    @ManyToOne
    @JoinColumn(name = "kurs_id")
    private Kurs kurs;

    /**
     * Student requesting enrollment.
     */
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    /**
     * Request submission date.
     */
    @Column(name = "datum")
    private LocalDate anfrageDatum;

    /**
     * Approval status.
     */
    @Column(name = "is_approved")
    @ColumnDefault("false")
    private boolean isApproved;
}

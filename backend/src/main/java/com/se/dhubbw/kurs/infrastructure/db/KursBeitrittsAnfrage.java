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
 * JPA entity for course enrollment (join) requests.
 */
@Table(name = "kurs_beitritts_anfrage")
@Entity
public class KursBeitrittsAnfrage {

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
     * Target course.
     */
    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "kurs_id")
    private Kurs kurs;

    /**
     * Student requesting enrollment.
     */
    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    /**
     * Request submission date.
     */
    @Getter
    @Setter
    @Column(name = "datum")
    private LocalDate anfrageDatum;

    /**
     * Approval status.
     */
    @Getter
    @Setter
    @Column(name = "is_approved")
    @ColumnDefault("false")
    private boolean isApproved;
}

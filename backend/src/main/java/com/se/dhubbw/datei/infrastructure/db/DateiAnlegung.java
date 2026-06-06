package com.se.dhubbw.datei.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for tracking file creation.
 * Records which student created a file and when.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data persistence layer)
 * @architecture.category T-Software (JPA entity mapping)
 */
@Table
@Entity
@Getter
@Setter
public class DateiAnlegung {

    /**
     * Creation tracking identifier.
     */
    @Id
    @Column(name = "anlegung_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID anlegungID;

    /**
     * Student who created the file.
     */
    @OneToOne
    @JoinColumn(name = "student_id")
    private Student student;

    /**
     * Created file reference.
     */
    @OneToOne
    @JoinColumn(name = "datei_id")
    private Datei datei;

    /**
     * Creation date.
     */
    @Column(name = "datum")
    private LocalDate datum;
}

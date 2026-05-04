package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.student.infrastructure.Student;
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
 * JPA entity for file creation tracking.
 */
@Table
@Entity
public class DateiAnlegung {

    /**
     * Creation tracking identifier.
     */
    @Getter
    @Setter
    @Id
    @Column(name = "anlegung_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID anlegungID;

    /**
     * Student who created the file.
     */
    @Getter
    @Setter
    @OneToOne
    @JoinColumn(name = "student_id")
    private Student student;

    /**
     * Created file reference.
     */
    @Getter
    @Setter
    @OneToOne
    @JoinColumn(name = "datei_id")
    private Datei datei;

    /**
     * Creation date.
     */
    @Getter
    @Setter
    @Column(name = "datum")
    private LocalDate datum;
}

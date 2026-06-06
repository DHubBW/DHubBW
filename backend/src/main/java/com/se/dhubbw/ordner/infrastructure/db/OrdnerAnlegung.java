package com.se.dhubbw.ordner.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for tracking folder creation.
 * Records which student created a folder and when.
 *
 * @architecture.component Ordnerverwaltung
 * @architecture.layer Infrastructure Layer (data persistence layer)
 * @architecture.category T-Software (JPA entity mapping)
 */
@Table(name = "ordner_anlegung")
@Entity
public class OrdnerAnlegung {
    /**
     * Creation tracking identifier.
     */
    @Id
    @Column(name = "anlegung_id")
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID anlegungID;

    /**
     * Student who created the folder.
     */
    @ManyToOne
    @JoinColumn(name = "student_id")
    @Getter
    @Setter
    private Student student;

    /**
     * Created folder reference.
     */
    @OneToOne
    @JoinColumn(name = "ordner_id")
    @Getter
    @Setter
    private Ordner ordner;

    /**
     * Creation date.
     */
    @Column(name = "datum")
    @Getter
    @Setter
    private LocalDate datum;
}

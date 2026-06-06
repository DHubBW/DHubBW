package com.se.dhubbw.kurs.infrastructure.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

/**
 * JPA entity representing a course in the system.
 * Stores the identifier, name, and cohort year of a DHBW course.
 *
 * <p>The entity contains the unique course identifier, the course name,
 * and the cohort year.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Infrastructure Layer (data persistence layer)
 * @architecture.category T-Software (JPA entity mapping)
 */
@Entity
@Table(name = "kurs")
public class Kurs {
    /**
     * Unique identifier of the course.
     */
    @Id
    @Getter
    @Setter
    @Column(name = "kurs_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID kursID;

    /**
     * Name of the course.
     */
    @Getter
    @Setter
    @Column(name = "kurs_name")
    private String kursName;

    /**
     * Year in which the course started.
     */
    @Getter
    @Setter
    @Column(name = "jahrgang")
    private String jahrgang;
}

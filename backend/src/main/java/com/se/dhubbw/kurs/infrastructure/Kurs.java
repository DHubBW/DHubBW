package com.se.dhubbw.kurs.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

/**
 * JPA entity for storing course data in the {@code kurs} table.
 *
 * <p>The entity contains the unique course identifier, the course name,
 * and the cohort year.</p>
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

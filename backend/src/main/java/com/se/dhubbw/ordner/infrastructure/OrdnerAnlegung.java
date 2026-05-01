package com.se.dhubbw.ordner.infrastructure;

import com.se.dhubbw.student.infrastructure.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * JPA entity for folder creation tracking.
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
    @Column
    @Getter
    @Setter
    private LocalDate datum;
}

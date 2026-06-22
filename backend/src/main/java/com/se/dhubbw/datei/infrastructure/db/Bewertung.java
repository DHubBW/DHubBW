package com.se.dhubbw.datei.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for a single user's star rating on a file.
 *
 * <p>Each {@code (datei, autor)} pair is unique — a user always has
 * at most one rating per file. The combined per-file aggregate
 * (sum + count) is held redundantly on {@link Datei} to avoid
 * recomputing the average for every list view.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data persistence layer)
 * @architecture.category T-Software (JPA entity mapping)
 */
@Entity
@Table(name = "bewertung",
    uniqueConstraints = @UniqueConstraint(
        // Reads as "one rating per (file, author) pair" — the
        // schema-level safeguard for the business rule that a
        // student may rate any given file at most once.
        name = "uk_bewertung_one_per_file_per_author",
        columnNames = {"datei_id", "autor_id"}))
@Getter
@Setter
public class Bewertung {

    /**
     * Minimum allowed star count. Half-stars are not supported.
     */
    public static final int MIN_STERNE = 1;

    /**
     * Maximum allowed star count.
     */
    public static final int MAX_STERNE = 5;

    /**
     * Unique rating identifier.
     */
    @Id
    @Column(name = "bewertung_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID bewertungID;

    /**
     * The file being rated. Mandatory — a rating without a file
     * makes no sense and the unique constraint relies on this
     * column being set.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "datei_id", nullable = false)
    private Datei datei;

    /**
     * Student who issued the rating. Mandatory — when a student
     * is removed from the platform, their ratings are deleted in
     * the same transaction (see {@code BewertungRepository
     * .deleteByAutor}). Persisting a {@code null} author would
     * orphan the row and break the unique constraint.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Student autor;

    /**
     * Star count between {@link #MIN_STERNE} and {@link #MAX_STERNE}.
     */
    @Column(name = "sterne", nullable = false)
    private int sterne;

    /**
     * Timestamp the rating was last set or updated. Mandatory —
     * set both on insert and on update by the service layer.
     */
    @Column(name = "bewertet_am", nullable = false)
    private LocalDateTime bewertetAm;
}

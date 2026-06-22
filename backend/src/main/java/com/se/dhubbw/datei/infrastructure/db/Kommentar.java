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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a comment on a file.
 * Stores author, content, creation timestamp, and edit-tracking
 * metadata.
 *
 * <p>A comment always belongs to exactly one {@link Datei} and is
 * authored by exactly one {@link Student}. Editing a comment updates
 * {@link #bearbeitet} to {@code true} and records the modification
 * time in {@link #zuletztBearbeitet}.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data persistence layer)
 * @architecture.category T-Software (JPA entity mapping)
 */
@Entity
@Table(name = "kommentar")
@Getter
@Setter
public class Kommentar {

    /**
     * Maximum length of a comment body in characters; mirrors the
     * service-level validation cap on user input.
     */
    public static final int MAX_INHALT_LENGTH = 2000;

    /**
     * Unique comment identifier.
     */
    @Id
    @Column(name = "kommentar_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID kommentarID;

    /**
     * The file this comment is attached to.
     */
    @ManyToOne
    @JoinColumn(name = "datei_id")
    private Datei datei;

    /**
     * Student who authored the comment.
     */
    @ManyToOne
    @JoinColumn(name = "autor_id")
    private Student autor;

    /**
     * Comment text body.
     */
    @Column(name = "inhalt", length = MAX_INHALT_LENGTH)
    private String inhalt;

    /**
     * Timestamp the comment was originally created.
     */
    @Column(name = "erstellt_am")
    private LocalDateTime erstelltAm;

    /**
     * Timestamp of the last edit, or {@code null} if the comment
     * has never been edited.
     */
    @Column(name = "zuletzt_bearbeitet")
    private LocalDateTime zuletztBearbeitet;

    /**
     * Flag indicating whether the comment has been edited at
     * least once after creation.
     */
    @Column(name = "bearbeitet", nullable = false)
    @ColumnDefault("false")
    private boolean bearbeitet;
}

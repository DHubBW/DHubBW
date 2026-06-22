package com.se.dhubbw.datei.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity representing a single named tag that can be attached
 * to multiple files.
 *
 * <p>Tag names are unique (case-insensitive). Only KURSADMINs and
 * SYSTEMADMINs may create new tags; uploaders may attach existing
 * tags to their own files. The reverse mapping ({@link #dateien}) is
 * the inverse side of the many-to-many relationship defined on
 * {@link Datei}.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data persistence layer)
 * @architecture.category T-Software (JPA entity mapping)
 */
@Entity
@Table(name = "tag")
@Getter
@Setter
public class Tag {

    /**
     * Maximum length of a tag name in characters.
     */
    public static final int MAX_NAME_LENGTH = 64;

    /**
     * Unique tag identifier.
     */
    @Id
    @Column(name = "tag_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tagID;

    /**
     * Display name of the tag (case-sensitive surface form,
     * uniqueness enforced case-insensitively at the service layer).
     */
    @Column(name = "name",
        nullable = false,
        unique = true,
        length = MAX_NAME_LENGTH)
    private String name;

    /**
     * Student who created this tag — KURSADMIN or SYSTEMADMIN.
     */
    @ManyToOne
    @JoinColumn(name = "ersteller_id")
    private Student ersteller;

    /**
     * Timestamp the tag was created.
     */
    @Column(name = "erstellt_am")
    private LocalDateTime erstelltAm;

    /**
     * Inverse side of the {@link Datei}-tag relationship — files
     * carrying this tag.
     */
    @ManyToMany(mappedBy = "tags")
    private Set<Datei> dateien = new HashSet<>();
}

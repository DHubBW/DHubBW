package com.se.dhubbw.datei.infrastructure.db;

import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.student.infrastructure.db.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity representing a file in the system.
 * Stores metadata (name, size, timestamp) and binary data
 * (as Large Object) of an uploaded file.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data persistence layer)
 * @architecture.category T-Software (JPA entity mapping)
 */
@Entity
@Table(name = "datei")
public class Datei {
    /**
     * Unique file identifier.
     */
    @Id
    @Getter
    @Setter
    @Column(name = "datei_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID dateiID;

    /**
     * Parent folder reference.
     */
    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "ordner_id")
    private Ordner ordner;

    /**
     * File name.
     */
    @Getter
    @Setter
    @Column(name = "name")
    private String name;

    /**
     * Last modification date.
     */
    @Getter
    @Setter
    @Column(name = "zuletzt_bearbeitet")
    private LocalDate zuletztBearbeitet;

    /**
     * File data content (used in dev profile with H2).
     * In production, data is stored as PostgreSQL Large Object
     * referenced by {@link #dataOid}.
     */
    @Getter
    @Setter
    @Lob
    @Column(name = "data")
    private byte[] data;

    /**
     * OID reference to PostgreSQL Large Object (used in prod).
     * Null in dev profile where {@link #data} is used directly.
     */
    @Getter
    @Setter
    @Column(name = "data_oid")
    private Long dataOid;

    /**
     * Student who uploaded the file.
     */
    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "uploader")
    private Student uploader;

    /**
     * Size of the Document (in MB).
     */
    @Getter
    @Setter
    @Column(name = "file_size")
    private float fileSize;

    /**
     * Number of times this file has been downloaded.
     */
    @Getter
    @Setter
    @Column(name = "download_anzahl", nullable = false)
    private int downloadAnzahl;

    /**
     * Cached aggregate of all individual ratings: sum of stars
     * across every {@link Bewertung} attached to this file.
     *
     * <p>Held redundantly so the average rating can be served
     * without re-aggregating every list view —
     * {@code average = bewertungSumme / bewertungAnzahl}.</p>
     */
    @Getter
    @Setter
    @Column(name = "bewertung_summe", nullable = false)
    @ColumnDefault("0")
    private long bewertungSumme;

    /**
     * Cached aggregate of all individual ratings: count of
     * {@link Bewertung} rows attached to this file.
     */
    @Getter
    @Setter
    @Column(name = "bewertung_anzahl", nullable = false)
    @ColumnDefault("0")
    private int bewertungAnzahl;

    /**
     * Tags attached to this file.
     *
     * <p>Many-to-many relationship: each tag can decorate any
     * number of files and each file can carry any number of tags.
     * The link rows live in their own join table {@code datei_tag}
     * — defined here via {@link JoinTable} rather than the more
     * common {@link JoinColumn} because a single FK column on
     * {@code datei} would only allow one tag per file. {@code
     * joinColumns} declares the FK on the owning side ({@code
     * datei.datei_id} → {@code datei_tag.datei_id}) and {@code
     * inverseJoinColumns} declares the FK on the inverse side
     * ({@code tag.tag_id} → {@code datei_tag.tag_id}). The Tag
     * entity exposes the same relationship as
     * {@code @ManyToMany(mappedBy = "tags")} so navigating from
     * either side returns the partner set.</p>
     */
    @Getter
    @Setter
    @ManyToMany
    @JoinTable(
        name = "datei_tag",
        joinColumns = @JoinColumn(name = "datei_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

}

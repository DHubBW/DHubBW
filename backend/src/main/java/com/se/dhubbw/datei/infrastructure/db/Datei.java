package com.se.dhubbw.datei.infrastructure.db;

import com.se.dhubbw.ordner.infrastructure.Ordner;
import com.se.dhubbw.student.infrastructure.db.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for file storage.
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
     * File data content.
     */
    @Getter
    @Setter
    @Lob
    @Column(name = "data")
    private byte[] data;

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

}

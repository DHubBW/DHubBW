package com.se.dhubbw.ordner.infrastructure;

import com.se.dhubbw.datei.infrastructure.Datei;
import com.se.dhubbw.kurs.infrastructure.Kurs;
import com.se.dhubbw.ordner.util.OrdnerFarbe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;
import java.util.UUID;

/**
 * JPA entity for folders.
 */
@Entity
@Table(name = "ordner")
public class Ordner {
    /**
     * Folder identifier.
     */
    @Id
    @Getter
    @Setter
    @Column(name = "ordner_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID ordnerID;

    /**
     * Parent folder (for nested folders).
     */
    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ueber_ordner_id")
    private Ordner ueberOrdner;

    /**
     * Associated course.
     */
    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "kurs_id")
    private Kurs kurs;

    /**
     * Folder name.
     */
    @Getter
    @Setter
    @Column(name = "name")
    private String name;

    /**
     * Folder nesting level.
     */
    @Getter
    @Setter
    @Column(name = "level")
    private int level;

    /**
     * Folder color.
     */
    @Getter
    @Setter
    @Enumerated(EnumType.ORDINAL)
    private OrdnerFarbe farbe;

    /**
     * Files in this folder.
     */
    @Getter
    @Setter
    @OneToMany(mappedBy = "ordner")
    private List<Datei> dateien;

    /**
     * Visibility status.
     */
    @Getter
    @Setter
    @Column(name = "is_public")
    @ColumnDefault("true")
    private boolean isPublic;
}

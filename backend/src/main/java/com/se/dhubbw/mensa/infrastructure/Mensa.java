package com.se.dhubbw.mensa.infrastructure;

import com.se.dhubbw.mensa.infrastructure.util.Ernaehrung;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for storing mensa meal entries in the {@code mensa} table.
 *
 * <p>The entity contains the unique meal identifier, the serving date,
 * the meal name, allergen information, the price, and the dietary category.
 */
@Entity
@Table(name = "mensa")
public class Mensa {
    /**
     * Unique identifier of the meal entry.
     */
    @Id
    @Getter
    @Setter
    @Column(name = "gericht_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID gerichtID;

    /**
     * Date on which the meal is served.
     */
    @Getter
    @Setter
    @Column(name = "datum")
    private LocalDate datum;

    /**
     * Name or description of the meal.
     */
    @Getter
    @Setter
    @Column(name = "essensbezeichnung")
    private String essensbezeichnung;

    /**
     * Allergens associated with the meal.
     */
    @Getter
    @Setter
    @Column(name = "allergene")
    private String allergene;

    /**
     * Price of the meal.
     */
    @Getter
    @Setter
    @Column(name = "preis")
    private double preis;

    /**
     * Dietary classification of the meal.
     */
    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "ernaehrung")
    private Ernaehrung ernaehrung;
}

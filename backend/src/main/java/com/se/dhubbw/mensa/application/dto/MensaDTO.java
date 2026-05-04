package com.se.dhubbw.mensa.application.dto;

import com.se.dhubbw.mensa.infrastructure.util.Ernaehrung;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object for Mensa meal information.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MensaDTO {
    /**
     * Unique identifier of the meal.
     */
    private UUID gerichtID;

    /**
     * Date of the meal.
     */
    private LocalDate datum;

    /**
     * Name of the meal.
     */
    private String essensbezeichnung;

    /**
     * Allergen information.
     */
    private String allergene;

    /**
     * Price of the meal.
     */
    private double preis;

    /**
     * Dietary classification.
     */
    private Ernaehrung ernaehrung;
}

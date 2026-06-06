package com.se.dhubbw.mensa.infrastructure.util;

/**
 * Enumeration of dietary types for mensa meals (vegetarian, vegan, meat).
 * Used for categorizing meals in the mensa meal plan.
 *
 * @architecture.component Mensaverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category A-Software (domain value object)
 */
public enum Ernaehrung {
    /**
     * vegetarian diet (e.g. cheese, vegetables, fruits, ...)
     */
    VEGETARISCH,
    /**
     * vegan diet (e.g. avocado, rice, soja, ...)
     * */
    VEGAN,
    /**
     * carnivore nutrition (e.g. fish, pork, ...)
     * */
    FLEISCH
}

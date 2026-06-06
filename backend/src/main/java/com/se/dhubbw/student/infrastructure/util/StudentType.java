package com.se.dhubbw.student.infrastructure.util;

/**
 * Enumeration of user roles in the system (STUDENT, KURSADMIN, SYSTEMADMIN).
 * Defines the hierarchical permission levels.
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category A-Software (domain value object)
 */
public enum StudentType {
    /**
     * Standard student role.
     */
    STUDENT,

    /**
     * Course administrator role.
     */
    KURSADMIN,

    /**
     * System administrator role (inherits {@code KURSADMIN} permissions).
     */
    SYSTEMADMIN
}

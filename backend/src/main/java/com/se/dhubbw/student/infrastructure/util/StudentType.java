package com.se.dhubbw.student.infrastructure.util;

/**
 * User role enumeration.
 *
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

package com.se.dhubbw.student.infrastructure.db;

import com.se.dhubbw.student.infrastructure.util.StudentType;
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

import java.util.UUID;

/**
 * JPA entity representing a student (user) in the system.
 * Stores identification, name, password hash, email, and role.
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Infrastructure Layer (data persistence layer)
 * @architecture.category T-Software (JPA entity mapping)
 */
@Entity
@Table(name = "student")
public class Student {
    /**
     * Student identifier.
     */
    @Id
    @Getter
    @Setter
    @Column(name = "student_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID studentID;

    /**
     * Student role type.
     */
    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "student_type")
    private StudentType studentType;

    /**
     * Student full name.
     */
    @Getter
    @Setter
    @Column(name = "name")
    private String name;

    /**
     * Password hash.
     */
    @Getter
    @Setter
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * DHBW email address.
     */
    @Getter
    @Setter
    @Column(name = "dhbw_mail")
    private String dhbwMail;
}

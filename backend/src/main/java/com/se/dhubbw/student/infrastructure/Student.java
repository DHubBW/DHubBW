package com.se.dhubbw.student.infrastructure;

import com.se.dhubbw.student.infrastructure.util.StudentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * JPA entity for student users.
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


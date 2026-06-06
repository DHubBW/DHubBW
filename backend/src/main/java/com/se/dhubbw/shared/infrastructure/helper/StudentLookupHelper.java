package com.se.dhubbw.shared.infrastructure.helper;

import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Shared helper component for student resolution (by email or UUID).
 * Centralises the recurring pattern of student lookups across multiple modules.
 *
 * <p>Centralises the repeated pattern of finding a student by
 * email or UUID and throwing a {@link NoSuchElementException}
 * when no match exists. Services in the {@code datei},
 * {@code ordner}, {@code kurs} and {@code student} modules all
 * previously duplicated this logic privately.</p>
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Component, cross-module)
 */
@Component
@RequiredArgsConstructor
public class StudentLookupHelper {

    /**
     * Repository for student lookups.
     */
    private final StudentRepository studentRepository;

    /**
     * Find a student by their DHBW email address.
     *
     * @param dhbwMail the DHBW email address to search for
     * @return the matching student
     * @throws NoSuchElementException if no student is found
     */
    public Student findStudentByEmail(final String dhbwMail) {
        return studentRepository.findByDhbwMail(dhbwMail)
            .orElseThrow(() -> new NoSuchElementException(
                "Student not found: " + dhbwMail));
    }

    /**
     * Find a student by their UUID.
     *
     * @param studentId the student identifier
     * @return the matching student
     * @throws NoSuchElementException if no student is found
     */
    public Student findStudentById(final UUID studentId) {
        return studentRepository.findById(studentId)
            .orElseThrow(() -> new NoSuchElementException(
                "Student not found: " + studentId));
    }
}

package com.se.dhubbw.student.infrastructure;

import com.se.dhubbw.shared.testing.IntegrationTest;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
public class StudentRepositoryIT {
    private final StudentRepository studentRepository;

    @Autowired
    public StudentRepositoryIT(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenStudentsAreInserted() {
        // DESCRIBE & ACT
        long actual = studentRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}


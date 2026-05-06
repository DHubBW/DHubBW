package com.se.dhubbw.student.infrastructure;

import com.se.dhubbw.student.infrastructure.db.StudentRepository;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Profile("dev")
public class StudentRepositoryIT {
    private final StudentRepository studentRepository;

    @Test
    public void givenDataSQL_whenApplicationStarts_thenStudentsAreInserted() {
        // DESCRIBE & ACT
        long actual = studentRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}


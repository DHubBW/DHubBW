package com.se.dhubbw.student.infrastructure.db.initializer;

import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of the system administrator initialization.
 * Creates a SYSTEMADMIN account with a generated password on
 * application startup if none exists yet.
 *
 * <p>Creates a SYSTEMADMIN user with a randomly generated password
 * and logs the credentials in orange for production and development.</p>
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring CommandLineRunner with
 *     Security integration)
 */
@Component
@Profile({"prod", "dev"})
@RequiredArgsConstructor
public class SysAdminInitializerServiceImpl
        implements com.se.dhubbw.student.domain.SysAdminInitializerService,
        CommandLineRunner {

    /**
     * Email address for the system administrator account.
     */
    private static final String SYSADMIN_EMAIL =
            "system.administrator.a24@student.dhbw-karlsruhe.de";

    /**
     * Name for the system administrator account.
     */
    private static final String SYSADMIN_NAME = "System Administrator";

    /**
     * Length of the generated random password.
     */
    private static final int PASSWORD_LENGTH = 20;

    /**
     * Character set for password generation.
     * Includes uppercase, lowercase, digits, and special characters.
     */
    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
            + "0123456789!@#$%^&*()";

    /**
     * ColorLogger instance for logging operations.
     */
    private final ColorLogger colorLogger =
            new ColorLogger(this.getClass());

    /**
     * Repository for student/user management.
     */
    private final StudentRepository studentRepository;

    /**
     * Password encoder for hashing passwords.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Secure random number generator for password creation.
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Run method executed on application startup.
     * Creates or updates the system administrator account.
     *
     * @param args command line arguments
     */
    @Override
    public void run(final String... args) {
        colorLogger.colorInfo(
                "Starting system administrator initialization...");

        // Check if sysadmin already exists
        if (studentRepository.findByDhbwMail(SYSADMIN_EMAIL).isPresent()) {
            colorLogger.colorInfo(
                    "System administrator account already exists "
                    + "with email: '{}'",
                    SYSADMIN_EMAIL);
            return;
        }

        // Generate random password
        String randomPassword = generateRandomPassword();

        // Create sysadmin account
        Student sysadmin = new Student();
        sysadmin.setDhbwMail(SYSADMIN_EMAIL);
        sysadmin.setName(SYSADMIN_NAME);
        sysadmin.setPasswordHash(passwordEncoder.encode(randomPassword));
        sysadmin.setStudentType(StudentType.SYSTEMADMIN);

        studentRepository.save(sysadmin);

        // Log credentials in ORANGE
        colorLogger.colorNewAdmin(
                "=== SYSTEM ADMINISTRATOR CREDENTIALS ===");
        colorLogger.colorNewAdmin("Email: {}", SYSADMIN_EMAIL);
        colorLogger.colorNewAdmin("Password: {}", randomPassword);
        colorLogger.colorNewAdmin(
                "========================================");

        colorLogger.colorInfo(
                "Successfully created system administrator account "
                + "with email: '{}'",
                SYSADMIN_EMAIL);
    }

    /**
     * Generate a cryptographically secure random password.
     *
     * @return randomly generated password string
     */
    private String generateRandomPassword() {
        return IntStream.range(0, PASSWORD_LENGTH)
                .map(i -> secureRandom.nextInt(PASSWORD_CHARS.length()))
                .mapToObj(PASSWORD_CHARS::charAt)
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}

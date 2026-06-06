package com.se.dhubbw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point of the DHubBW Spring Boot application.
 * Starts the embedded web server and initializes the Spring application
 * context.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Boot application starter)
 */
//CHECKSTYLE:OFF
@SpringBootApplication
public class DhubbwApplication {

    /**
     * Private constructor to hide implicit public constructor.
     */
    private DhubbwApplication() {
    }

    /**
     * Main method to run the Spring Boot application.
     *
     * @param args command-line arguments passed to the application
     */
    static void main(final String[] args) {
        SpringApplication.run(DhubbwApplication.class, args);
    }
}

package com.se.dhubbw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the DHBW DHUBBW Spring Boot application.
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

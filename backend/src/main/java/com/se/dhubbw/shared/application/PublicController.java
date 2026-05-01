package com.se.dhubbw.shared.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for student-related endpoints.
 *
 * <p>This controller provides endpoints for student-level operations
 * such as accessing mensa information and course details.
 */
@RestController
@RequestMapping("/public")
public final class PublicController {
    /**
     * Test endpoint to verify student service availability.
     *
     * <p>Returns a simple success message confirming that the student service
     * is operational and accessible.
     *
     * @return a {@code String} containing the success status message
     */
    @GetMapping("")
    public String testEndpoint() {
        return "admin success";
    }
}

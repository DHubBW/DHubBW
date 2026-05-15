package com.se.dhubbw.student.application;

import com.se.dhubbw.student.domain.StudentDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * REST controller for student dashboard operations.
 *
 * <p>Exposes endpoints for students to manage their own
 * course membership and account. All endpoints require
 * authentication as a STUDENT.</p>
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class StudentDashboardController {

    /**
     * Service handling dashboard operations.
     */
    private final StudentDashboardService studentDashboardService;

    /**
     * Remove the authenticated student from their current course.
     *
     * <p>Deletes the student's course enrollment. If the student
     * is a KURSADMIN, at least one other KURSADMIN must remain.</p>
     *
     * @param authentication the current user's security context
     * @return 200 on success with confirmation message,
     *     400 if the student is the last KURSADMIN,
     *     or 404 if no course enrollment exists
     */
    @Operation(
            summary = "Leave current course",
            description = "Removes the authenticated student "
                + "from their course. Fails if they are the "
                + "last KURSADMIN."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully left the course"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Cannot leave: last KURSADMIN in course"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Student has no course enrollment"
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/leave-kurs")
    public ResponseEntity<String> leaveKurs(
            final Authentication authentication) {
        try {
            studentDashboardService.leaveKurs(
                authentication.getName());
            return ResponseEntity.ok(
                "Successfully left course for student: '"
                    + authentication.getName() + "'.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Student has no course enrollment: '"
                    + authentication.getName() + "'.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Cannot leave course: "
                    + "You are the last KURSADMIN in this course.");
        }
    }

    /**
     * Delete the authenticated student's account completely.
     *
     * <p>Removes the student and all associated data. Fails if
     * the student is the last SYSTEMADMIN or last KURSADMIN
     * in their course.</p>
     *
     * @param authentication the current user's security context
     * @return 200 on success with confirmation message,
     *     or 400 if the student is the last admin
     */
    @Operation(
            summary = "Delete student account",
            description = "Permanently deletes the authenticated "
                + "student's account and all associated data. "
                + "Fails if they are the last SYSTEMADMIN or "
                + "last KURSADMIN."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Account successfully deleted"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Cannot delete: last admin"
    )
    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteAccount(
            final Authentication authentication) {
        try {
            studentDashboardService.deleteAccount(
                authentication.getName());
            return ResponseEntity.ok(
                "Successfully deleted account for student: '"
                    + authentication.getName() + "'.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
        }
    }
}

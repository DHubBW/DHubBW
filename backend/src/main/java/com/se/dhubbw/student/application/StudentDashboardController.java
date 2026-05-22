package com.se.dhubbw.student.application;

import com.se.dhubbw.shared.application.dto.RequestStatusResponseDTO;
import com.se.dhubbw.shared.application.dto.SmallResponseDTO;
import com.se.dhubbw.shared.application.dto.util.PersistenceOperation;
import com.se.dhubbw.student.application.dto.StudentProfileDTO;
import com.se.dhubbw.student.domain.StudentDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
     * Whether cookies should be secure (HTTPS only).
     */
    @Value("${jwt.cookie.secure:true}")
    private boolean cookieSecure;

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
    public ResponseEntity<RequestStatusResponseDTO> leaveKurs(
            final Authentication authentication) {
        try {
            studentDashboardService.leaveKurs(
                authentication.getName());
            return ResponseEntity.status(HttpStatus.OK).body(
                new RequestStatusResponseDTO(
                    "Successfully left course for student: '"
                + authentication.getName() + "'.",
                    PersistenceOperation.DELETE));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RequestStatusResponseDTO(
                    "Student has no course enrollment: '"
                    + authentication.getName() + "'.",
                    PersistenceOperation.DELETE));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RequestStatusResponseDTO("Cannot leave course: "
                    + "You are the last KURSADMIN in this course.",
                    PersistenceOperation.DELETE));
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
    public ResponseEntity<RequestStatusResponseDTO> deleteAccount(
            final Authentication authentication) {

        studentDashboardService.deleteAccount(authentication.getName());
        return ResponseEntity.status(HttpStatus.OK).body(
            new RequestStatusResponseDTO(
                "Successfully deleted account for student: '"
                + authentication.getName() + "'.",
                PersistenceOperation.DELETE));

    }

    /**
     * Retrieve the authenticated student's profile information.
     *
     * @param authentication the current user's security context
     * @return 200 with StudentProfileDTO containing name, email,
     *     role, and course name
     */
    @Operation(
            summary = "Retrieve student profile",
            description = "Returns the authenticated student's "
                + "profile information including name, email, "
                + "role, and course name (if enrolled)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Profile successfully retrieved",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = StudentProfileDTO.class)
            )
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/profile")
    public ResponseEntity<StudentProfileDTO> retrieveProfile(
            final Authentication authentication) {
        StudentProfileDTO profile = studentDashboardService.retrieveProfile(
            authentication.getName());
        return ResponseEntity.ok(profile);
    }

    /**
     * Log out the authenticated student.
     *
     * <p>Invalidates the JWT cookie by setting its max age to 0.</p>
     *
     * @param response the HTTP response to add the invalidated cookie to
     * @return 200 with confirmation message
     */
    @Operation(
            summary = "Log out student",
            description = "Invalidates the JWT session cookie "
                + "and logs out the student."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully logged out"
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/logout")
    public ResponseEntity<SmallResponseDTO> logout(
            final HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("jwt", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(cookieSecure);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);
        return ResponseEntity.status(HttpStatus.OK)
            .body(new SmallResponseDTO("Successfully logged out."));
    }

    /**
     * Retrieve the course name for the authenticated student.
     *
     * @param authentication the current user's security context
     * @return 200 with course name, or "Kein Kurs" if not enrolled
     */
    @Operation(
            summary = "Retrieve course for user",
            description = "Returns the course name for the "
                + "authenticated student, or 'Kein Kurs' if "
                + "the student has no course enrollment."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Course information retrieved"
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/kurs")
    public ResponseEntity<SmallResponseDTO> retrieveKursForUser(
            final Authentication authentication) {
        String kursName = studentDashboardService.retrieveKursForUser(
            authentication.getName());
        if (kursName == null) {
            return ResponseEntity.status(HttpStatus.OK).body(
                new SmallResponseDTO("Kein Kurs"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(
            new SmallResponseDTO(kursName));
    }
}

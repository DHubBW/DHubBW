package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.application.dto.KursErstellungsAnfrageRequestDTO;
import com.se.dhubbw.kurs.application.dto.KursYearDTO;
import com.se.dhubbw.kurs.infrastructure.KursServiceImpl;
import com.se.dhubbw.shared.application.dto.RequestStatusResponseDTO;
import com.se.dhubbw.shared.application.dto.util.PersistenceOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for course endpoints (list courses, join request,
 * creation request).
 * Provides HTTP endpoints for students to manage courses.
 *
 * <p>Provides endpoints for retrieving available courses,
 * submitting course join requests, and submitting course
 * creation requests.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category T-Software (REST endpoints with Spring Web
 *     annotations)
 */
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class KursController {

    /**
     * Service containing all course-related business logic.
     */
    private final KursServiceImpl kursService;

    /**
     * Retrieve all available courses with their identifier,
     * name, and cohort year.
     *
     * <p>Requires at least the {@code STUDENT} role.</p>
     *
     * @return 200 with the list of all courses; may be empty
     */
    @Operation(
            summary = "Retrieve all courses",
            description = "Returns identifier, name, and cohort "
                + "year for every available course."
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of all courses successfully loaded",
            content =
            @Content(mediaType = "application/json",
                    array = @ArraySchema
                        (schema = @Schema(
                            implementation = KursYearDTO.class))
            )
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/kurse")
    public ResponseEntity<List<KursYearDTO>> retrieveKursNamesAndYears() {
        return ResponseEntity.ok(kursService.retrieveKursNamesAndYears());
    }

    /**
     * Open a join request for the authenticated student.
     *
     * <p>Validates that the course exists, the student is not
     * already enrolled in any course, and the student has no
     * other open join request.</p>
     *
     * @param kursId         the UUID of the course to join
     * @param authentication the current user's security context
     * @return 201 if the request was created successfully
     */
    @Operation(
            summary = "Open a course join request",
            description = "Creates a pending join request for "
                + "the authenticated student. The course must "
                + "exist and the student must not already be "
                + "enrolled or have an open join request."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Join request created successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(
            responseCode = "409",
            description = "Student already enrolled "
                + "or has an open join request",
            content = @Content(mediaType = "application/json"))
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/kurse/beitreten/{kursId}")
    public ResponseEntity<RequestStatusResponseDTO> openJoinKursRequest(
            @Parameter(
                    description = "UUID of the course to join",
                    required = true)
            @PathVariable final UUID kursId,
            final Authentication authentication) {
        kursService.openJoinKursRequest(
                kursId, authentication.getName());
        RequestStatusResponseDTO workingRequestStatus =
            new RequestStatusResponseDTO(
            "Join request opened successfully", PersistenceOperation.INSERT);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(workingRequestStatus);
    }

    /**
     * Open a course creation request for the authenticated student.
     *
     * <p>Validates that no course with the same name and cohort
     * year already exists before persisting the request.</p>
     *
     * @param request        request body containing course name,
     *     cohort year, and optional notes
     * @param authentication the current user's security context
     * @return 201 if the creation request was submitted
     */
    @Operation(
            summary = "Open a course creation request",
            description = "Submits a course creation request for "
                + "the authenticated student. The combination of "
                + "course name and cohort year must not exist."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Course creation request submitted")
    @ApiResponse(
            responseCode = "409",
            description = "Student already enrolled, or course "
                + "with this name and cohort year already exists",
            content = @Content(mediaType = "application/json"))
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/kurse/erstellen")
    public ResponseEntity<RequestStatusResponseDTO> openCreateCourseRequest(
            @Valid @RequestBody final KursErstellungsAnfrageRequestDTO request,
            final Authentication authentication) {
        kursService.openCreateCourseRequest(
            request.getKursName(),
            request.getJahrgang(),
            request.getAdditionalNotes(),
            authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new RequestStatusResponseDTO(
                "Create Kurs Request opened successfully",
                PersistenceOperation.INSERT));
    }
}

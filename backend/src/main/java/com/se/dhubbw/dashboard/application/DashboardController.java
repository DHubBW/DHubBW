package com.se.dhubbw.dashboard.application;

import com.se.dhubbw.dashboard.domain.DashboardService;
import com.se.dhubbw.datei.application.dto.DateiDTO;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.mensa.application.dto.MensaDTO;
import com.se.dhubbw.mensa.application.dto.mapper.MensaDTOMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for dashboard aggregation endpoints.
 *
 * <p>Provides today's mensa meals and the latest files visible to
 * the authenticated user: all files for system administrators,
 * course-scoped files for students.</p>
 */
@RestController
@RequestMapping("/public/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    /**
     * Spring authority name for the system-administrator role.
     */
    private static final String ROLE_SYSTEMADMIN = "ROLE_SYSTEMADMIN";

    /**
     * Service that aggregates dashboard data.
     */
    private final DashboardService dashboardService;

    /**
     * Component for mapping the mensa entity to a DTO.
     */
    private final MensaDTOMapper mensaDTOMapper;

    /**
     * Retrieve all mensa meals for today.
     *
     * @return 200 with the list of today's meals, which may be empty
     */
    @Operation(
            summary = "Retrieve today's mensa meals",
            description = "Returns all meals available today."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Today's meals successfully loaded",
            content =
            @Content(mediaType = "application/json",
                    array = @ArraySchema
                            (schema = @Schema(implementation = MensaDTO.class))
            )
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/meals/today")
    public ResponseEntity<List<MensaDTO>> retrieveMealsForToday() {
        List<MensaDTO> gerichte = dashboardService.getMealsForToday().stream()
            .map(mensaDTOMapper::convertMensaEntityToMensaDTO)
            .toList();
        return ResponseEntity.ok(gerichte);
    }

    /**
     * Retrieve the latest files visible to the authenticated user.
     *
     * <p>System administrators receive all files across every course.
     * Students receive files scoped to their enrolled course.</p>
     *
     * @param authentication the current user's security context
     * @return 200 with the relevant file list, or 403 if the student
     *     has no approved course enrollment
     */
    @Operation(
            summary = "Retrieve latest visible files",
            description = "Returns all files for admins, "
                + "or course-scoped files for students."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Latest files successfully loaded",
            content =
            @Content(mediaType = "application/json",
                    array = @ArraySchema
                            (schema = @Schema(implementation = DateiDTO.class))
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Student has no approved course enrollment",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/dateien/latest")
    public ResponseEntity<List<DateiDTO>> retrieveLatestDateien(
            final Authentication authentication) {
        if (isSystemAdmin(authentication)) {
            return ResponseEntity.ok(
                dashboardService.getAllLatestDateien().stream()
                    .map(DashboardController::toDateiDto)
                    .toList()
            );
        }
        Optional<List<Datei>> dateien =
            dashboardService.getLatestDateienForStudent(
                authentication.getName());
        if (dateien.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(
            dateien.get().stream()
                .map(DashboardController::toDateiDto)
                .toList()
        );
    }

    /**
     * Check whether the authenticated user holds the SYSTEMADMIN role.
     *
     * @param authentication the current user's security context
     * @return {@code true} if the user is a system administrator
     */
    private static boolean isSystemAdmin(final Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(auth ->
                auth.getAuthority().equals(ROLE_SYSTEMADMIN));
    }

    /**
     * Map a {@link Datei} entity to its DTO representation.
     *
     * @param datei the entity to convert
     * @return the corresponding {@link DateiDTO}
     */
    private static DateiDTO toDateiDto(final Datei datei) {
        String uploaderName = datei.getUploader() != null
            ? datei.getUploader().getName() : null;
        return new DateiDTO(
            datei.getDateiID(),
            datei.getName(),
            datei.getZuletztBearbeitet(),
            datei.getFileSize(),
            uploaderName,
            datei.getOrdner().getOrdnerID(),
            datei.getOrdner().getName()
        );
    }
}

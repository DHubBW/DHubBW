package com.se.dhubbw.mensa.application;

import com.se.dhubbw.mensa.application.dto.MensaDTO;
import com.se.dhubbw.mensa.domain.MensaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for retrieving mensa-related data.
 *
 * <p>This controller provides endpoints for students to
 * retrieve meals for specific days or entire weeks.</p>
 */
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class MensaController {

    /**
     * Application service providing mensa meal data.
     */
    private final MensaService mensaService;

    /**
     * Retrieve all meals for a specific day.
     *
     * @param date the requested day in ISO format (YYYY-MM-DD)
     * @return list of meals, or 400 if no date was supplied
     */
    @Operation(
            summary = "retrieve meals for a specific day",
            description = "returns array of meals for a specific day."
    )
    @ApiResponse(
            responseCode = "200",
            description = "meals for day successfully loaded",
            content =
            @Content(mediaType = "application/json",
                    array = @ArraySchema
                            (schema = @Schema(implementation = MensaDTO.class))
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Date parameter is required",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/meals")
    public ResponseEntity<List<MensaDTO>> retrieveMealsForDay(
            @Parameter(
                    description = "The date in ISO format (YYYY-MM-DD)",
                    example = "2026-05-03",
                    required = true
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            final LocalDate date) {
        List<MensaDTO> gerichte = mensaService.getMealsForDay(date).stream()
            .map(MensaDTO::from)
            .toList();
        return ResponseEntity.ok(gerichte);
    }

    /**
     * Retrieve meals for the current calendar week.
     *
     * <p>Returns a map where keys are weekday abbreviations
     * ("Mo", "Di", "Mi", "Do", "Fr") and values are lists of meals.</p>
     *
     * @return map of weekdays to meal lists (200 OK)
     */
    @Operation(
            summary = "retrieve meals for current week",
            description = "returns map of weekdays to meal lists "
                    + "for the current calendar week (Monday-Friday)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "current week meals successfully loaded",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/meals/current-week")
    public ResponseEntity<Map<String, List<MensaDTO>>>
            retrieveCurrentWeek() {
        Map<String, List<MensaDTO>> weekMeals = mensaService
                .getMealsForCurrentWeek()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(MensaDTO::from)
                                .toList()
                ));
        return ResponseEntity.ok(weekMeals);
    }

    /**
     * Retrieve meals for the next calendar week.
     *
     * <p>Returns a map where keys are weekday abbreviations
     * ("Mo", "Di", "Mi", "Do", "Fr") and values are lists of meals.</p>
     *
     * @return map of weekdays to meal lists (200 OK)
     */
    @Operation(
            summary = "retrieve meals for next week",
            description = "returns map of weekdays to meal lists "
                    + "for the next calendar week (Monday-Friday)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "next week meals successfully loaded",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/meals/next-week")
    public ResponseEntity<Map<String, List<MensaDTO>>> retrieveNextWeek() {
        Map<String, List<MensaDTO>> weekMeals = mensaService
                .getMealsForNextWeek()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(MensaDTO::from)
                                .toList()
                ));
        return ResponseEntity.ok(weekMeals);
    }
}


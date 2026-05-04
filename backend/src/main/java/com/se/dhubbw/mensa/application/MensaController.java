package com.se.dhubbw.mensa.application;

import com.se.dhubbw.mensa.application.dto.MensaDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for retrieving mensa-related data.
 *
 * <p>This controller provides an endpoint for students to retrieve meals.</p>
 */
@RestController
@RequestMapping("/public")
public class MensaController {

    /**
     * Retrieve all meals.
     * @param date as the selected day.
     * @return ArrayList of meals.
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
        if (date == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        List<MensaDTO> gerichte = new ArrayList<>();
        gerichte.add(new MensaDTO());
        return ResponseEntity.ok(gerichte);
    }

}

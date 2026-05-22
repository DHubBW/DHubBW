package com.se.dhubbw.mensa.application;

import com.se.dhubbw.mensa.domain.MensaService;
import com.se.dhubbw.shared.application.dto.SmallResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing system-admin operations for mensa data.
 *
 * <p>Exposes a manual sync trigger that forces an immediate re-scrape
 * of the mensa meal plan. Access is restricted to users with the
 * {@code SYSTEMADMIN} role.
 */
@RestController
@RequestMapping("/sysadmin/mensa")
@RequiredArgsConstructor
public class MensaSyncController {

    /**
     * Application service used to trigger the mensa data sync.
     */
    private final MensaService mensaService;

    /**
     * Manually trigger a full mensa data sync.
     *
     * @return 200 OK with a confirmation message
     */
    @Operation(
            summary = "Manually trigger mensa sync",
            description = "Forces an immediate scrape of the mensa "
                + "meal plan and overwrites the stored data "
                + "for all fetched dates."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Sync completed successfully"
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @PostMapping("/sync")
    public ResponseEntity<SmallResponseDTO> triggerSync() {
        mensaService.sync();
        return ResponseEntity.status(HttpStatus.OK)
            .body(new SmallResponseDTO("Mensa sync completed."));
    }
}

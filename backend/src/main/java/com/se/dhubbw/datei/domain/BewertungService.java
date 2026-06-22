package com.se.dhubbw.datei.domain;

import com.se.dhubbw.datei.application.dto.output.DateiBewertungDTO;

import java.util.UUID;

/**
 * Domain interface for file star-rating management.
 *
 * <p>Each user may submit at most one rating per file, with whole
 * stars between 1 and 5. The aggregate (average + count) is held
 * redundantly on the file entity so list views never recompute it.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface, technology-free)
 */
public interface BewertungService {

    /**
     * Set or update the caller's rating on the given file.
     *
     * <p>Authorisation: the caller must be allowed to read the
     * file (anyone who can see it may rate it).</p>
     *
     * @param dateiId  the UUID of the file to rate
     * @param sterne   star count between 1 and 5 (inclusive)
     * @param dhbwMail the DHBW mail of the rating student
     * @return the updated aggregate plus the caller's own rating
     */
    DateiBewertungDTO setBewertung(UUID dateiId,
            int sterne,
            String dhbwMail);

    /**
     * Remove the caller's rating from the file.
     *
     * <p>Authorisation: only the rating's own author may remove
     * it. The aggregate is updated as part of the deletion.</p>
     *
     * @param dateiId  the UUID of the file to un-rate
     * @param dhbwMail the DHBW mail of the rating student
     * @return the updated aggregate plus the caller's own rating
     *     (now {@code 0})
     */
    DateiBewertungDTO removeBewertung(UUID dateiId, String dhbwMail);

    /**
     * Retrieve the current aggregate plus the caller's own rating.
     *
     * @param dateiId  the UUID of the file
     * @param dhbwMail the DHBW mail of the requesting student
     * @return the aggregate plus the caller's own rating
     */
    DateiBewertungDTO getBewertung(UUID dateiId, String dhbwMail);
}

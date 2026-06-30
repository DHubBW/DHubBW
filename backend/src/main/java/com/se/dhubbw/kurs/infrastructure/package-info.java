/**
 * Infrastructure layer for kurs domain.
 *
 * <p>This package contains JPA entities and repository implementations
 * for persisting courses (kurse), course join requests
 * (kurs_beitritts_anfrage) and course creation requests
 * (kurs_erstellungs_anfrage) data in the database.</p>
 *
 * <p>It also contains the {@link
 * com.se.dhubbw.kurs.infrastructure.KursServiceImpl}
 * which implements the course-related use cases (retrieve courses,
 * open join requests, open creation requests), and the {@link
 * com.se.dhubbw.kurs.infrastructure.KursMitgliederServiceImpl}
 * which implements member and role management use cases.</p>
 *
 * @architecture.layer Infrastructure Layer
 */
package com.se.dhubbw.kurs.infrastructure;


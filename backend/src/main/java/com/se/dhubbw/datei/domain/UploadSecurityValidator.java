package com.se.dhubbw.datei.domain;

import org.springframework.web.multipart.MultipartFile;

/**
 * Pure-domain interface that vets an incoming
 * {@link MultipartFile} against the platform's upload-security
 * policy before any bytes are persisted.
 *
 * <p>Production rules enforced today:</p>
 * <ul>
 *   <li>Reject empty files (size {@code 0} bytes).</li>
 *   <li>Reject files larger than the configured per-file cap
 *       (default {@code 50 MB}, overridable via
 *       {@code dhubbw.upload.max-file-mb}).</li>
 *   <li>Reject names with path-traversal segments
 *       ({@code /}, {@code \\}, {@code ..}).</li>
 *   <li>Reject extensions on the executable / dangerous-content
 *       blocklist (e.g. {@code exe}, {@code bat}, {@code js},
 *       {@code apk}, {@code msi}).</li>
 *   <li>Reject Content-Types that announce executable / script
 *       content (e.g. {@code application/x-msdownload},
 *       {@code application/x-sh},
 *       {@code application/javascript}).</li>
 * </ul>
 *
 * <p>Implementations must throw a Spring
 * {@link org.springframework.web.server.ResponseStatusException}
 * with the appropriate HTTP status:
 * {@code 400} for empty / malformed name,
 * {@code 413} for size,
 * {@code 415} for blocklisted type / extension.
 * The controller layer keeps relying on Spring's default mapping
 * for {@code ResponseStatusException}, so no controller changes
 * are required to surface these statuses.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface,
 *     technology-free aside from the Spring multipart abstraction
 *     which is shared with the upload contract).
 */
public interface UploadSecurityValidator {

    /**
     * Validate the given upload against the security policy.
     *
     * @param file        the multipart file submitted by the user
     * @param displayName the display name attached by the caller
     *     (the {@code name} request parameter from the upload
     *     endpoint); may differ from
     *     {@link MultipartFile#getOriginalFilename()}.
     * @throws org.springframework.web.server.ResponseStatusException
     *     when the upload violates any rule
     */
    void validate(MultipartFile file, String displayName);
}

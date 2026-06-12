package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.domain.UploadSecurityValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

/**
 * Default {@link UploadSecurityValidator} implementation backed by
 * a static blocklist of executable / script extensions plus a
 * Content-Type danger list.
 *
 * <p>Rejection rules and their HTTP statuses match the contract
 * described in {@link UploadSecurityValidator}.</p>
 *
 * <p>The blocklist is intentionally conservative: extensions and
 * MIME types that have a track record of being abused for malware
 * delivery (Windows binaries, native scripts, mobile installers,
 * disk images, browser-executable JavaScript) are rejected.
 * Everything else is allowed — DHBW course material is an
 * open-ended set of formats (PDF, Office documents, code archives,
 * media), so a strict whitelist would block legitimate files. The
 * platform-wide storage cap and the Spring multipart limit remain
 * the second line of defence.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Component)
 */
@Component
public class DefaultUploadSecurityValidator
    implements UploadSecurityValidator {

    /** Bytes per megabyte for the size-cap conversion. */
    private static final long BYTES_PER_MB = 1_000_000L;

    /**
     * HTTP 413 Payload Too Large status. Hard-coded to avoid the
     * Spring 7 deprecation warning on
     * {@link HttpStatus#PAYLOAD_TOO_LARGE} that the rest of the
     * project also sidesteps.
     */
    private static final int HTTP_PAYLOAD_TOO_LARGE = 413;

    /**
     * Blocklisted file extensions (lower-cased, no leading dot).
     *
     * <p>Covers Windows binaries, native scripts, mobile
     * installers, disk images, and browser-executable
     * JavaScript. Update with care — every entry rejects an
     * entire upload class.</p>
     */
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "com", "pif", "scr", "msi", "msp",
        "dll", "sys", "drv", "ocx", "cpl", "gadget", "hta",
        "vb", "vbs", "vbe", "wsf", "wsh", "ps1", "ps1xml",
        "ps2", "psc1", "psc2", "ws", "lnk", "inf",
        "sh", "bash", "zsh", "ksh", "csh", "fish",
        "jar", "js", "jse", "mjs",
        "apk", "ipa", "dmg", "iso", "img",
        "app", "deb", "rpm",
        "reg", "hlp", "chm");

    /**
     * Blocklisted Content-Type prefixes / exact values
     * (lower-cased).
     *
     * <p>Anything starting with one of these strings is rejected.
     * Browsers occasionally send unfortunate mappings (e.g.
     * {@code text/javascript} for {@code .js}), so the prefix
     * match keeps the policy permissive for benign sub-types.</p>
     */
    private static final Set<String> BLOCKED_CONTENT_TYPES = Set.of(
        "application/x-msdownload",
        "application/x-msdos-program",
        "application/x-ms-installer",
        "application/x-msi",
        "application/x-sh",
        "application/x-csh",
        "application/x-shellscript",
        "application/x-bat",
        "application/x-bash",
        "application/x-executable",
        "application/x-dosexec",
        "application/x-mach-binary",
        "application/x-elf",
        "application/x-iso9660-image",
        "application/vnd.android.package-archive",
        "application/vnd.apple.installer+xml",
        "application/javascript",
        "application/ecmascript",
        "application/hta",
        "text/javascript",
        "text/ecmascript",
        "text/x-shellscript",
        "text/x-sh");

    /**
     * Maximum allowed per-file size in megabytes.
     *
     * <p>Defaults to {@code 50.0 MB} so the value matches the
     * {@code spring.servlet.multipart.max-file-size} setting in
     * {@code application.yaml}. Operators can override it via
     * the {@code dhubbw.upload.max-file-mb} property without
     * touching the multipart limit, e.g. to keep a soft cap below
     * the hard one.</p>
     */
    @Value("${dhubbw.upload.max-file-mb:50.0}")
    private double maxFileMb;

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(
            final MultipartFile file,
            final String displayName) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Datei ist leer");
        }
        long sizeBytes = file.getSize();
        long maxBytes = (long) (maxFileMb * BYTES_PER_MB);
        if (sizeBytes > maxBytes) {
            throw new ResponseStatusException(
                HttpStatus.valueOf(HTTP_PAYLOAD_TOO_LARGE),
                String.format(
                    Locale.ROOT,
                    "Datei zu groß: %.1f MB. Limit: %.1f MB.",
                    sizeBytes / (double) BYTES_PER_MB, maxFileMb));
        }
        if (containsTraversal(displayName)
                || containsTraversal(file.getOriginalFilename())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ungültiger Dateiname");
        }
        String extension = lowerExtensionOf(displayName);
        if (extension.isEmpty()) {
            extension = lowerExtensionOf(file.getOriginalFilename());
        }
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Dateityp nicht erlaubt: ." + extension);
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            String normalized = contentType
                .toLowerCase(Locale.ROOT)
                .trim();
            int semicolon = normalized.indexOf(';');
            if (semicolon >= 0) {
                normalized = normalized.substring(0, semicolon).trim();
            }
            for (String blocked : BLOCKED_CONTENT_TYPES) {
                if (normalized.equals(blocked)) {
                    throw new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Dateityp nicht erlaubt: " + contentType);
                }
            }
        }
    }

    /**
     * Returns the lower-cased file extension (no leading dot).
     *
     * @param name the file name to inspect, may be {@code null}
     * @return the lower-case extension, or an empty string when
     *     the name is {@code null}/empty or has no extension
     */
    private static String lowerExtensionOf(final String name) {
        if (name == null) {
            return "";
        }
        int slash = Math.max(
            name.lastIndexOf('/'),
            name.lastIndexOf('\\'));
        String base = slash >= 0
            ? name.substring(slash + 1)
            : name;
        int dot = base.lastIndexOf('.');
        if (dot < 0 || dot == base.length() - 1) {
            return "";
        }
        return base.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Returns {@code true} when the given file name contains a
     * path separator or a parent-directory segment.
     *
     * @param name the file name to inspect
     * @return {@code true} when the name is unsafe
     */
    private static boolean containsTraversal(final String name) {
        if (name == null) {
            return false;
        }
        return name.contains("..")
            || name.contains("/")
            || name.contains("\\");
    }
}

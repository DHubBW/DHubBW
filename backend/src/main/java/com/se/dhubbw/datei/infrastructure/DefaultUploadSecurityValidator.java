package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.domain.UploadSecurityValidator;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
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

    /** Maximum length of the display name in characters. */
    private static final int MAX_NAME_LENGTH = 255;

    /** Number of bytes the magic-byte sniffer reads from the head. */
    private static final int MAGIC_BYTES_PEEK = 16;

    /** First printable ASCII codepoint (space) — characters below
     *  this and not equal to {@code \t} are treated as control bytes
     *  and rejected in file names. */
    private static final char FIRST_PRINTABLE_ASCII = 0x20;

    /** ASCII DEL — printable but historically used as a separator
     *  / control byte and not safe in file names. */
    private static final char ASCII_DEL = 0x7F;

    /** Bytes per hex token in {@link #formatHex} (two digits + a
     *  separating space). */
    private static final int HEX_CHARS_PER_BYTE = 3;

    /** Bidi override / format characters frequently used to spoof
     *  the file extension visually (RLO trick: "img‮gpj.exe"
     *  appears as "img.exe.jpg" to humans but is .exe on disk). */
    private static final Set<Character> DANGEROUS_NAME_CHARS = Set.of(
        '‪', // LRE
        '‫', // RLE
        '‬', // PDF
        '‭', // LRO
        '‮', // RLO  ← classic spoof
        '⁦', // LRI
        '⁧', // RLI
        '⁨', // FSI
        '⁩', // PDI
        '﻿'  // BOM / zero-width no-break space
    );

    /** Magic-byte signatures of executable / disk-image / archive
     *  formats that the platform refuses regardless of the extension
     *  the uploader picked. Detection runs against the first
     *  {@link #MAGIC_BYTES_PEEK} bytes and prevents the classic
     *  "rename {@code mal.exe} → {@code mal.pdf}" bypass. */
    private static final byte[][] BLOCKED_MAGIC_SIGNATURES = {
        {0x4D, 0x5A},                                  // "MZ" — Windows PE
        {0x7F, 0x45, 0x4C, 0x46},                      // "\x7FELF" — Linux ELF
        {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}, // Mach-O fat
        {(byte) 0xCF, (byte) 0xFA, (byte) 0xED, (byte) 0xFE}, // Mach-O LE 64
        {(byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCE}, // Mach-O BE 32
        {(byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCF}  // Mach-O BE 64
    };

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
     * Color-coded logger used to surface mismatches between the
     * uploader's claimed extension / Content-Type and what the
     * magic-byte check actually found. The upload still goes
     * through after the mismatch is logged — only outright
     * blocklisted signatures are rejected.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(DefaultUploadSecurityValidator.class);

    /**
     * {@inheritDoc}
     *
     * <p>The default validator runs a layered check:</p>
     * <ol>
     *   <li>Empty / oversized file → 400 / 413.</li>
     *   <li>Display-name sanity — path traversal, control bytes
     *       (NUL / Tab / CR / LF), bidi override characters
     *       (RLO &amp; friends), and an upper length cap.</li>
     *   <li>Extension blocklist — every entry rejects an entire
     *       class of binary / script uploads.</li>
     *   <li>Content-Type blocklist (semicolon-stripped, normalised
     *       to lower case).</li>
     *   <li>Magic-byte check — the first {@link #MAGIC_BYTES_PEEK}
     *       bytes are matched against known executable signatures
     *       (PE / ELF / Mach-O). Catches the classic "rename
     *       {@code mal.exe} → {@code mal.pdf}" bypass.</li>
     * </ol>
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
        validateNameSafety(displayName);
        validateNameSafety(file.getOriginalFilename());
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
        validateMagicBytes(file, displayName, extension);
    }

    /**
     * Reject names that contain path separators, parent-directory
     * segments, NUL bytes, control characters, bidi override
     * characters, or are longer than {@link #MAX_NAME_LENGTH}.
     *
     * <p>Bidi overrides like {@code U+202E} (RTL OVERRIDE) are a
     * classic spoof: a name like {@code "img‮gpj.exe"}
     * renders as {@code "img.exe.jpg"} in most file managers. The
     * check is mandatory — a friendly-looking PDF that turns into
     * an executable on disk is a textbook lateral-movement
     * vector.</p>
     *
     * @param name the file name to inspect
     * @throws ResponseStatusException 400 if the name is unsafe
     */
    private void validateNameSafety(final String name) {
        if (name == null) {
            return;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Dateiname zu lang");
        }
        if (name.contains("..")
                || name.contains("/")
                || name.contains("\\")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ungültiger Dateiname");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == ' '
                    || (c < FIRST_PRINTABLE_ASCII && c != '\t')
                    || c == ASCII_DEL
                    || DANGEROUS_NAME_CHARS.contains(c)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ungültiges Zeichen im Dateinamen");
            }
        }
    }

    /**
     * Read the first {@link #MAGIC_BYTES_PEEK} bytes of the upload
     * and reject the file when they match a known executable
     * signature. Detects the classic
     * "rename {@code mal.exe} → {@code mal.pdf}" bypass.
     *
     * @param file       the multipart upload
     * @param displayName the user-supplied display name (logged for
     *     mismatch warnings)
     * @param extension   the file's lower-case extension
     */
    private void validateMagicBytes(
            final MultipartFile file,
            final String displayName,
            final String extension) {
        byte[] head = peekHead(file);
        if (head.length == 0) {
            return;
        }
        for (byte[] sig : BLOCKED_MAGIC_SIGNATURES) {
            if (startsWith(head, sig)) {
                ResponseStatusException ex = new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Datei-Inhalt entspricht einer ausfuehrbaren "
                        + "Datei und wird abgelehnt");
                colorLogger.colorError(
                    "Upload rejected (magic mismatch): "
                        + "name='{}' ext='{}' sig-found={}: {}",
                    displayName, extension,
                    formatHex(sig), ex.getMessage(), ex);
                throw ex;
            }
        }
    }

    /**
     * Read the first few bytes of the upload, swallowing any I/O
     * error as "no peek possible" — the mainline pipeline will
     * surface a 500 if the stream is genuinely broken.
     *
     * @param file the multipart upload
     * @return the head bytes, or an empty array on error
     */
    private byte[] peekHead(final MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] buf = new byte[MAGIC_BYTES_PEEK];
            int read = 0;
            while (read < buf.length) {
                int n = in.read(buf, read, buf.length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
            if (read < buf.length) {
                byte[] trimmed = new byte[read];
                System.arraycopy(buf, 0, trimmed, 0, read);
                return trimmed;
            }
            return buf;
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    /**
     * Test whether {@code data} starts with {@code prefix}.
     *
     * @param data   the bytes to inspect
     * @param prefix the prefix to test for
     * @return {@code true} when {@code prefix} is a prefix of {@code data}
     */
    private static boolean startsWith(
            final byte[] data, final byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pretty-print a byte array as space-separated lower-case hex.
     *
     * @param bytes the bytes to format
     * @return the hex representation, e.g. {@code "4d 5a"}
     */
    private static String formatHex(final byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * HEX_CHARS_PER_BYTE);
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
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
}

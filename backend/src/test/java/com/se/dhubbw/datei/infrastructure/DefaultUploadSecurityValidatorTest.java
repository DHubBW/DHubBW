package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.shared.testing.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link DefaultUploadSecurityValidator}.
 *
 * <p>Verifies that the validator returns the expected HTTP
 * status codes for every rejection rule (size, blocklist
 * extension, blocklist Content-Type, empty file, traversal) and
 * that legitimate uploads pass.</p>
 */
@UnitTest
public class DefaultUploadSecurityValidatorTest {

    /** HTTP 413 Payload Too Large. */
    private static final int HTTP_PAYLOAD_TOO_LARGE = 413;

    /** Per-file cap used by the test fixture (5 MB). */
    private static final double TEST_MAX_MB = 5.0;

    private DefaultUploadSecurityValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new DefaultUploadSecurityValidator();
        ReflectionTestUtils.setField(
            validator, "maxFileMb", TEST_MAX_MB);
    }

    @Test
    public void givenValidPdf_whenValidate_thenAccepts() {
        MultipartFile file = new MockMultipartFile(
            "file", "skript.pdf",
            "application/pdf",
            new byte[]{1, 2, 3});
        assertDoesNotThrow(
            () -> validator.validate(file, "skript.pdf"));
    }

    @Test
    public void givenEmptyFile_whenValidate_thenThrows400() {
        MultipartFile file = new MockMultipartFile(
            "file", "empty.txt",
            "text/plain",
            new byte[0]);
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> validator.validate(file, "empty.txt"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    public void givenTooLargeFile_whenValidate_thenThrows413() {
        // 6 MB > 5 MB cap.
        byte[] payload = new byte[6_000_000];
        MultipartFile file = new MockMultipartFile(
            "file", "big.pdf",
            "application/pdf",
            payload);
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> validator.validate(file, "big.pdf"));
        assertEquals(
            HTTP_PAYLOAD_TOO_LARGE,
            ex.getStatusCode().value());
    }

    @Test
    public void givenBlockedExtension_whenValidate_thenThrows415() {
        MultipartFile file = new MockMultipartFile(
            "file", "virus.exe",
            "application/octet-stream",
            new byte[]{1, 2, 3});
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> validator.validate(file, "virus.exe"));
        assertEquals(415, ex.getStatusCode().value());
    }

    @Test
    public void givenBlockedContentType_whenValidate_thenThrows415() {
        MultipartFile file = new MockMultipartFile(
            "file", "harmless.dat",
            "application/x-msdownload",
            new byte[]{1, 2, 3});
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> validator.validate(file, "harmless.dat"));
        assertEquals(415, ex.getStatusCode().value());
    }

    @Test
    public void givenJavaScriptContentType_whenValidate_thenThrows415() {
        MultipartFile file = new MockMultipartFile(
            "file", "innocent.txt",
            "application/javascript",
            new byte[]{1, 2, 3});
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> validator.validate(file, "innocent.txt"));
        assertEquals(415, ex.getStatusCode().value());
    }

    @Test
    public void givenPathTraversalDisplayName_whenValidate_thenThrows400() {
        MultipartFile file = new MockMultipartFile(
            "file", "skript.pdf",
            "application/pdf",
            new byte[]{1, 2, 3});
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> validator.validate(file, "../etc/passwd"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    public void givenContentTypeWithCharset_whenValidate_thenAccepts() {
        // text/plain; charset=utf-8 must NOT match the blocklist
        // because the charset suffix is stripped before the check.
        MultipartFile file = new MockMultipartFile(
            "file", "hinweis.txt",
            "text/plain; charset=utf-8",
            new byte[]{1, 2, 3});
        assertDoesNotThrow(
            () -> validator.validate(file, "hinweis.txt"));
    }

    @Test
    public void givenUppercaseExtension_whenValidate_thenStillBlocks() {
        MultipartFile file = new MockMultipartFile(
            "file", "Trojaner.EXE",
            "application/octet-stream",
            new byte[]{1, 2, 3});
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> validator.validate(file, "Trojaner.EXE"));
        assertEquals(415, ex.getStatusCode().value());
    }
}

package com.se.dhubbw.shared.infrastructure.exception;

import com.se.dhubbw.shared.testing.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Each test fires a request to a dedicated stub controller
 * that throws a specific exception, and asserts that the global
 * handler maps it to the correct HTTP status code.</p>
 */
@UnitTest
public class GlobalExceptionHandlerTest {

    /**
     * MockMvc instance configured with the handler under test.
     */
    private MockMvc mockMvc;

    /**
     * Set up a standalone MockMvc wired with the
     * {@link GlobalExceptionHandler} and the stub controller.
     */
    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new StubController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    /**
     * A {@link ResponseStatusException} with 404 must be forwarded
     * as-is.
     */
    @Test
    public void givenResponseStatusException_whenHandled_thenCorrectStatus()
            throws Exception {
        mockMvc.perform(get("/stub/response-status"))
            .andExpect(status().isNotFound());
    }

    /**
     * A {@link NoSuchElementException} must be mapped to 404.
     */
    @Test
    public void givenNoSuchElementException_whenHandled_thenReturns404()
            throws Exception {
        mockMvc.perform(get("/stub/not-found"))
            .andExpect(status().isNotFound());
    }

    /**
     * An {@link IllegalStateException} must be mapped to 409.
     */
    @Test
    public void givenIllegalStateException_whenHandled_thenReturns409()
            throws Exception {
        mockMvc.perform(get("/stub/conflict"))
            .andExpect(status().isConflict());
    }

    /**
     * An {@link IllegalArgumentException} must be mapped to 400.
     */
    @Test
    public void givenIllegalArgumentException_whenHandled_thenReturns400()
            throws Exception {
        mockMvc.perform(get("/stub/bad-request"))
            .andExpect(status().isBadRequest());
    }

    /**
     * An unhandled {@link RuntimeException} must fall back to 500.
     */
    @Test
    public void givenUnhandledException_whenHandled_thenReturns500()
            throws Exception {
        mockMvc.perform(get("/stub/server-error"))
            .andExpect(status().isInternalServerError());
    }

    /**
     * An {@link AccessDeniedException} must be mapped to 403.
     */
    @Test
    public void givenAccessDeniedException_whenHandled_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/stub/forbidden"))
            .andExpect(status().isForbidden());
    }

    /**
     * A missing required request parameter must be mapped to 400.
     */
    @Test
    public void givenMissingParam_whenHandled_thenReturns400()
            throws Exception {
        mockMvc.perform(get("/stub/requires-param"))
            .andExpect(status().isBadRequest());
    }

    /**
     * Stub controller that deliberately throws various exception types
     * to exercise the {@link GlobalExceptionHandler}.
     */
    @RestController
    @RequestMapping("/stub")
    static final class StubController {

        /**
         * Throws a {@link ResponseStatusException} with 404.
         *
         * @return never returns
         */
        @GetMapping("/response-status")
        public ResponseEntity<Void> throwResponseStatus() {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "not found");
        }

        /**
         * Throws a {@link NoSuchElementException}.
         *
         * @return never returns
         */
        @GetMapping("/not-found")
        public ResponseEntity<Void> throwNotFound() {
            throw new NoSuchElementException("element not found");
        }

        /**
         * Throws an {@link IllegalStateException}.
         *
         * @return never returns
         */
        @GetMapping("/conflict")
        public ResponseEntity<Void> throwConflict() {
            throw new IllegalStateException("conflict");
        }

        /**
         * Throws an {@link IllegalArgumentException}.
         *
         * @return never returns
         */
        @GetMapping("/bad-request")
        public ResponseEntity<Void> throwBadRequest() {
            throw new IllegalArgumentException("bad argument");
        }

        /**
         * Throws a generic {@link RuntimeException}.
         *
         * @return never returns
         */
        @GetMapping("/server-error")
        public ResponseEntity<Void> throwServerError() {
            throw new RuntimeException("unexpected error");
        }

        /**
         * Throws an {@link AccessDeniedException}.
         *
         * @return never returns
         */
        @GetMapping("/forbidden")
        public ResponseEntity<Void> throwForbidden() {
            throw new AccessDeniedException("access denied");
        }

        /**
         * Requires a request parameter to trigger
         * {@code MissingServletRequestParameterException}.
         *
         * @param value the required parameter
         * @return 200 OK when parameter is present
         */
        @GetMapping("/requires-param")
        public ResponseEntity<String> requiresParam(
                @RequestParam final String value) {
            return ResponseEntity.ok(value);
        }
    }
}

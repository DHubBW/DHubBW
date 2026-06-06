/**
 * Client-side validators for DHBW e-mail and full-name format.
 *
 * Both regexes are taken **1:1** from the Spring backend
 * (`AuthenticationController.java`), so a green client-side validation
 * guarantees the backend will not reject the value with a 422 response.
 *
 * Sources of truth:
 * - Mail regex: `AuthenticationController.java:88`
 *   `^[a-z]+\.[a-z]+\.[a-z0-9]+@student\.dhbw-karlsruhe\.de$`
 * - Name regex: `AuthenticationController.java:95-97`
 *   `^[A-ZÄÖÜ][a-zäöüß]+(?:[- ][A-ZÄÖÜ][a-zäöüß]+)*\s+[A-ZÄÖÜ][a-zäöüß]+(?:[- ][A-ZÄÖÜ][a-zäöüß]+)*$`
 *
 * Hint: When the backend regexes change, update this file in lockstep —
 * mismatches are silently tolerated by the backend (it just rejects with 422)
 * but produce a confusing UX.
 */

/** DHBW Karlsruhe student-mail regex, identical to backend Z. 87-88. */
const DHBW_MAIL_RE =
  /^[a-z]+\.[a-z]+\.[a-z0-9]+@student\.dhbw-karlsruhe\.de$/;

/** Full-name regex (`Vorname Nachname`), identical to backend Z. 95-97. */
const NAME_RE =
  /^[A-ZÄÖÜ][a-zäöüß]+(?:[- ][A-ZÄÖÜ][a-zäöüß]+)*\s+[A-ZÄÖÜ][a-zäöüß]+(?:[- ][A-ZÄÖÜ][a-zäöüß]+)*$/;

/**
 * Returns `true` if the given string matches the backend DHBW-mail regex.
 *
 * @param value - Candidate e-mail string (already trimmed).
 */
export function isValidDhbwMail(value: string): boolean {
  return DHBW_MAIL_RE.test(value);
}

/**
 * Returns `true` if the given string matches the backend full-name regex.
 * Format: `Vorname Nachname`, optional hyphenated/double given/family names.
 *
 * @param value - Candidate full-name string (already trimmed).
 */
export function isValidName(value: string): boolean {
  return NAME_RE.test(value);
}

/**
 * UI-friendly error message for an invalid DHBW mail.
 *
 * Returns `null` when the value is valid — convenient for direct binding
 * to a form-error slot.
 */
export function getMailErrorMessage(value: string): string | null {
  if (isValidDhbwMail(value)) return null;
  return 'Bitte eine gültige DHBW-Mail im Format vorname.nachname.kurs@student.dhbw-karlsruhe.de eingeben.';
}

/**
 * UI-friendly error message for an invalid full-name.
 *
 * Returns `null` when the value is valid.
 */
export function getNameErrorMessage(value: string): string | null {
  if (isValidName(value)) return null;
  return 'Bitte den vollständigen Namen im Format „Vorname Nachname" eingeben.';
}

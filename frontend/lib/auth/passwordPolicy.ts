/**
 * Shared password-policy definitions used by both the registration and the
 * change-password flows.
 *
 * The same shape is mirrored on the backend (regex + min/max length); see
 * {@link PASSWORD_REGEX} for the single combined expression. The per-rule
 * structure exposed via {@link PASSWORD_RULES} / {@link evaluatePassword}
 * exists so the UI can render live, granular feedback rather than a single
 * pass/fail.
 */

/** Minimum length (inclusive) the policy requires. */
export const MIN_PASSWORD_LENGTH = 10;

/** Maximum length (inclusive) the policy allows. */
export const MAX_PASSWORD_LENGTH = 128;

/**
 * Single combined regex matching the must-have rules. Backend uses the
 * Java equivalent of this expression.
 *
 * - lookahead: at least one lowercase
 * - lookahead: at least one uppercase
 * - lookahead: at least one digit
 * - lookahead: at least one special (non-letter, non-digit, non-whitespace)
 * - body: 10..128 non-whitespace characters
 */
export const PASSWORD_REGEX =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\s])\S{10,128}$/;

/** A single, named password-policy rule. */
export interface PasswordRule {
  /** Stable identifier (used as React key). */
  id: string;
  /** German user-facing label. */
  label: string;
  /** Returns true iff the candidate password satisfies this rule. */
  test: (pw: string) => boolean;
}

/**
 * The list of rules surfaced to the user. The user sees seven rows in total —
 * a min-length, a max-length, four character-class rules, and the
 * no-whitespace guard.
 */
export const PASSWORD_RULES: PasswordRule[] = [
  {
    id: "min-length",
    label: "Mindestens 10 Zeichen",
    test: (pw) => pw.length >= MIN_PASSWORD_LENGTH,
  },
  {
    id: "max-length",
    label: "Maximal 128 Zeichen",
    test: (pw) => pw.length <= MAX_PASSWORD_LENGTH,
  },
  {
    id: "lowercase",
    label: "Mindestens ein Kleinbuchstabe",
    test: (pw) => /[a-z]/.test(pw),
  },
  {
    id: "uppercase",
    label: "Mindestens ein Großbuchstabe",
    test: (pw) => /[A-Z]/.test(pw),
  },
  {
    id: "digit",
    label: "Mindestens eine Ziffer",
    test: (pw) => /[0-9]/.test(pw),
  },
  {
    id: "special",
    label: "Mindestens ein Sonderzeichen",
    test: (pw) => /[^A-Za-z0-9\s]/.test(pw),
  },
  {
    id: "no-whitespace",
    label: "Keine Leerzeichen",
    test: (pw) => pw.length > 0 && !/\s/.test(pw),
  },
];

/** Per-rule evaluation result returned by {@link evaluatePassword}. */
export interface EvaluatedRule {
  id: string;
  label: string;
  passed: boolean;
}

/** Aggregate result for a candidate password. */
export interface PasswordEvaluation {
  rules: EvaluatedRule[];
  allPassed: boolean;
}

/**
 * Evaluate a candidate password against {@link PASSWORD_RULES}.
 *
 * @param pw - The candidate password.
 * @returns The per-rule outcomes plus an aggregate `allPassed` flag.
 */
export function evaluatePassword(pw: string): PasswordEvaluation {
  const rules = PASSWORD_RULES.map((rule) => ({
    id: rule.id,
    label: rule.label,
    passed: rule.test(pw),
  }));
  const allPassed = rules.every((r) => r.passed);
  return { rules, allPassed };
}

// Handcoded fallback for the OpenAPI-generated DTOs.
//
// TODO: regenerate via `npm run generate:api` once the backend runs locally
// so `openapi-typescript` can replace this file with the canonical output —
// the generated version additionally provides path-typed endpoint helpers
// (e.g. `paths['/public/auth/login']['post']['responses']['200']`) which
// this fallback cannot express.
//
// Field-name casing follows the Java DTOs verbatim — notably `kursID` with
// upper-case `D` (record property name preserved by the JSON serialiser)
// and `dhbwMail` (camelCase). `studentType` and `kursName` MUST stay in
// sync with their consumers in `components/layout/navItems.ts`
// (Sidebar / BottomNav), which is why `lib/auth/AuthContext.tsx` exports
// the `AuthUser = StudentProfileDTO` alias.
//
// Backend sources (one DTO per file unless noted):
// - student/application/dto/AuthResponseDTO.java         (record)
// - student/application/dto/StudentProfileDTO.java       (Lombok @Getter)
// - student/application/dto/PendingKursRequestDTO.java   (Lombok @Getter)
// - student/application/dto/LoginRequestDTO.java
// - student/application/dto/RegisterRequestDTO.java
// - shared/application/dto/SmallResponseDTO.java
// - shared/application/dto/RequestStatusResponseDTO.java
// - kurs/application/dto/KursYearDTO.java                (record)
// - kurs/application/dto/KursErstellungsAnfrageRequestDTO.java (Lombok @Getter)
// - datei/application/dto/output/DateiUploadDTO.java           (record)

export interface LoginRequestDTO {
  dhbwMail: string;
  password: string;
}

export interface RegisterRequestDTO {
  dhbwMail: string;
  name: string;
  password: string;
}

export interface AuthResponseDTO {
  message: string;
  /** null im Error-Fall (z.B. 422 bei Mail-/Name-Regex-Mismatch). */
  token: string | null;
  /** ms-since-epoch. 0 im Error-Fall. */
  expiresAt: number;
}

export type StudentType = 'STUDENT' | 'KURSADMIN' | 'SYSTEMADMIN';

/**
 * Pending course request shown on the student dashboard.
 *
 * Covers both course-join requests (`BEITRITT`) and creation requests
 * (`ERSTELLUNG`). For `ERSTELLUNG` the `kursName` may be `null` until the
 * request has been confirmed.
 */
export interface PendingKursRequestDTO {
  type: 'BEITRITT' | 'ERSTELLUNG';
  /** Target course name. May be null for `ERSTELLUNG` before confirmation. */
  kursName: string | null;
  /**
   * Submission date as ISO-`YYYY-MM-DD` string. Backend serialises Java
   * `LocalDate` without time/zone information.
   */
  anfrageDatum: string;
}

export interface StudentProfileDTO {
  name: string;
  dhbwMail: string;
  studentType: StudentType;
  /**
   * Sauberes JSON-`null`, wenn der Student keinen Kurs hat.
   * **Quirk:** der `/student/dashboard/kurs`-Endpoint liefert stattdessen den
   * Literal-String `'Kein Kurs'` (siehe StudentDashboardController.java:232) —
   * nutze `lib/auth/normalizeKurs.ts` für diesen Endpoint. Profile NICHT.
   */
  kursName: string | null;
  /**
   * Open course request of the student. `null` when there is no pending
   * join or creation request.
   */
  pendingKursRequest: PendingKursRequestDTO | null;
}

export interface SmallResponseDTO {
  response: string;
}

export interface RequestStatusResponseDTO {
  response: string;
  persistenceOperation: 'INSERT' | 'UPDATE' | 'DELETE';
}

/**
 * Single course entry returned by `GET /student/kurse`.
 *
 * Mirrors the backend record `KursYearDTO`. Field-name casing follows the
 * Java record verbatim — note the upper-case `D` in `kursID`, which the
 * controller's JSON serialiser preserves.
 */
export interface KursYearDTO {
  /** UUID of the course (string-encoded). */
  kursID: string;
  /** Display name, e.g. `"TINF24B2"`. */
  kursName: string;
  /** Cohort year as free-text, e.g. `"2024"`. */
  jahrgang: string;
}

/**
 * Request body for `POST /student/kurse/erstellen`.
 *
 * `additionalNotes` is optional on the backend (no `@NotBlank`) but the
 * frontend caps it at 500 characters before sending — see `NewKursDialog`.
 */
export interface KursErstellungsAnfrageRequestDTO {
  /** Desired course name. Backend constraint: `@NotBlank`. */
  kursName: string;
  /** Cohort year free-text. Backend constraint: `@NotBlank`. */
  jahrgang: string;
  /** Optional notes; omit or send `null` when empty. */
  additionalNotes?: string | null;
}

/**
 * Response body for `PUT /student/dateien/upload` on a 201 Created.
 *
 * Mirrors the backend record `DateiUploadDTO(UUID dateiID)`. Field-name
 * casing follows the Java record verbatim — note the upper-case `D` in
 * `dateiID`, which Jackson preserves (same convention as `kursID`).
 */
export interface DateiUploadDTO {
  /** UUID of the newly created file (string-encoded). */
  dateiID: string;
}

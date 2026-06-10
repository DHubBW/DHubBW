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

export type StudentType = "STUDENT" | "KURSADMIN" | "SYSTEMADMIN";

/**
 * Pending course request shown on the student dashboard.
 *
 * Covers both course-join requests (`BEITRITT`) and creation requests
 * (`ERSTELLUNG`). For `ERSTELLUNG` the `kursName` may be `null` until the
 * request has been confirmed.
 */
export interface PendingKursRequestDTO {
  type: "BEITRITT" | "ERSTELLUNG";
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
  persistenceOperation: "INSERT" | "UPDATE" | "DELETE";
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

/**
 * Request body for `PATCH /student/dateien/{dateiId}`.
 *
 * Mirrors `RenameDateiRequestDTO` in the backend
 * (`datei/application/dto/input/RenameDateiRequestDTO.java`). The single
 * `newName` property is the file's new display name; backend trims and
 * validates it (non-blank).
 */
export interface RenameDateiRequestDTO {
  /** New display name for the file. */
  newName: string;
}

/** Dietary classification of a Mensa dish. */
export type Ernaehrung = "VEGETARISCH" | "VEGAN" | "FLEISCH";

/**
 * Single dish entry returned by the Mensa week endpoints.
 *
 * Mirrors `MensaDTO` in the backend (`mensa/application/dto/`).
 */
export interface MensaDTO {
  /** UUID of the dish (string-encoded). */
  gerichtID: string;
  /** Serving date as ISO `"YYYY-MM-DD"`. */
  datum: string;
  /** Display name of the dish. */
  essensbezeichnung: string;
  /** Comma-separated allergen list, e.g. `"Gluten, Laktose"`. */
  allergene: string;
  /** Price in EUR (student tariff). */
  preis: number;
  ernaehrung: Ernaehrung;
  /** Serving line label, e.g. `"Wahlessen 1"`, `"Beilagen"`, `"Suppe"`. */
  linie: string;
}

/**
 * Map returned by `GET /student/meals/current-week` and
 * `GET /student/meals/next-week`.
 *
 * Keys are German weekday abbreviations: `"Mo"`, `"Di"`, `"Mi"`, `"Do"`, `"Fr"`.
 */
export type WeekMealsMap = Record<string, MensaDTO[]>;

/** Folder colour as defined in the backend `OrdnerFarbe` enum. */
export type OrdnerFarbe = "DEFAULT" | "RED" | "BLUE" | "GREEN";

/**
 * Request body for `PUT /student/ordner`.
 *
 * Mirrors `CreateOrdnerRequestDTO` in the backend
 * (`ordner/application/dto/input/CreateOrdnerRequestDTO.java`). The
 * `isPublic` JSON property is preserved verbatim — Jackson is configured
 * with `@JsonProperty("isPublic")` to override the default Java-Beans
 * stripping of the `is` prefix.
 */
export interface CreateOrdnerRequestDTO {
  /** Parent folder UUID, or `null` for a root-level folder. */
  ueberOrdnerId: string | null;
  /** Display name for the new folder. */
  name: string;
  /** Whether the folder is publicly visible. */
  isPublic: boolean;
  /** Folder colour. */
  farbe: OrdnerFarbe;
}

/**
 * Request body for `PATCH /student/ordner/{ordnerId}`.
 *
 * Mirrors `RenameOrdnerRequestDTO` in the backend
 * (`ordner/application/dto/input/RenameOrdnerRequestDTO.java`).
 */
export interface RenameOrdnerRequestDTO {
  /** New display name for the folder. */
  newName: string;
}

/**
 * Folder metadata returned by the `/student/ordner` endpoints.
 *
 * Mirrors `OrdnerDTO` in the backend (`ordner/application/dto/output/`).
 * Field-name casing follows the Java DTO verbatim — note the upper-case
 * `D` in `ordnerID` / `ueberOrdnerID` / `kursID` (Lombok `@Getter` for
 * `private UUID ordnerID;` keeps the property name through Jackson),
 * and the `public` field, which Jackson derives from the boolean
 * accessor `isPublic()` (Java-Beans convention strips the `is` prefix).
 */
export interface OrdnerDTO {
  /** UUID of the folder (string-encoded). */
  ordnerID: string;
  /** Folder name. */
  name: string;
  /** Nesting level (0 = root). */
  level: number;
  /** Folder colour. */
  farbe: OrdnerFarbe;
  /**
   * Whether the folder is publicly visible. Backend property is
   * `private boolean isPublic;`, Jackson serialises this as `public`.
   */
  public: boolean;
  /** Parent folder UUID, or `null` for root folders. */
  ueberOrdnerID: string | null;
  /** UUID of the course this folder belongs to. */
  kursID: string;
}

/**
 * File metadata returned by `GET /student/dashboard/dateien/latest`.
 *
 * Mirrors `DateiDTO` in the backend (`datei/application/dto/output/`).
 * Binary content is excluded — display metadata only.
 */
export interface DateiDTO {
  /** UUID of the file (string-encoded). */
  dateiID: string;
  /** File name including extension. */
  name: string;
  /** ISO `"YYYY-MM-DD"` date of last modification. */
  zuletztBearbeitet: string;
  /** File size in megabytes. */
  fileSize: number;
  /** Full name of the uploading student, or `null` when unknown. */
  uploaderName: string | null;
  /** UUID of the containing folder (string-encoded). */
  ordnerID: string;
  /** Display name of the containing folder. */
  ordnerName: string;
  /** Total download count. */
  downloadAnzahl: number;
}

/**
 * Course entry returned by `GET /systemadmin/kurse`.
 *
 * Mirrors `KursDTO` in the backend.
 */
export interface KursDTO {
  /** UUID of the course (string-encoded). */
  kursID: string;
  /** Display name, e.g. `"TINF24B2"`. */
  kursName: string;
  /** Cohort year as free-text, e.g. `"2024"`. */
  jahrgang: string;
}

/**
 * Course-creation request entry returned by `GET /systemadmin/anfragen`.
 *
 * Mirrors `KursErstellungsAnfrageDTO` in the backend.
 */
export interface KursErstellungsAnfrageDTO {
  /** UUID of the creation request (string-encoded). */
  anfrageID: string;
  /** UUID of the requesting student (string-encoded). */
  studentID: string;
  /** Full name of the requesting student. */
  studentName: string;
  /** Desired course name. */
  kursName: string;
  /** Desired cohort year. */
  jahrgang: string;
  /** Submission date as ISO `"YYYY-MM-DD"`. */
  anfrageDatum: string;
  /** Optional notes provided with the request. */
  requestNotes: string | null;
}

/**
 * Student entry returned by `GET /systemadmin/studenten`.
 *
 * Mirrors `StudentDTO` in the backend.
 */
export interface StudentDTO {
  /** UUID of the student (string-encoded). */
  studentID: string;
  /** Full name of the student. */
  name: string;
  /** DHBW email address. */
  dhbwMail: string;
  /** Current role of the student. */
  studentType: StudentType;
  /** Course name — null if the student has no course. */
  kursName?: string | null;
}

/**
 * Course-member entry returned by `GET /systemadmin/kurse/{id}/mitglieder`.
 *
 * Mirrors `KursMitgliedRolleDTO` in the backend.
 */
export interface KursMitgliedRolleDTO {
  /** UUID of the course member (string-encoded). */
  studentID: string;
  /** Full name of the course member. */
  studentName: string;
  /** Current role of the course member. */
  rolle: StudentType;
}

/**
 * Course-member entry returned by `GET /kursadmin/mitglieder`.
 *
 * Mirrors `KursmemberDTO` in the backend (`kurs/application/dto/output/`).
 * Note: field is `studentType` (not `rolle`) — different from the sysadmin
 * variant `KursMitgliedRolleDTO`.
 */
export interface KursmemberDTO {
  /** UUID of the course member (string-encoded). */
  studentID: string;
  /** Full name of the course member. */
  name: string;
  /** DHBW email address. */
  dhbwMail: string;
  /** Current role of the course member. */
  studentType: StudentType;
}

/**
 * Enrollment request returned by `GET /kursadmin/anfragen`.
 *
 * Mirrors `KursanfrageDTO` in the backend (`kurs/application/dto/output/`).
 */
export interface KursanfrageDTO {
  /** UUID of the enrollment request (string-encoded). */
  anfrageID: string;
  /** UUID of the target course (string-encoded). */
  kursID: string;
  /** Name of the target course. */
  kursName: string;
  /** UUID of the requesting student (string-encoded). */
  studentID: string;
  /** Full name of the requesting student. */
  studentName: string;
  /** Submission date as ISO `"YYYY-MM-DD"`. */
  anfrageDatum: string;
}
export interface KursAdminStatsDTO {
  /** Total enrolled members in the course. */
  anzahlMitglieder: number;
  /** Total files in the course. */
  anzahlDateien: number;
  /** Open join requests pending approval. */
  anzahlAnfragen: number;
}

/**
 * Platform-wide KPI stats for a system admin returned by
 * `GET /systemadmin/stats`.
 *
 * Mirrors `SystemAdminStatsDTO` in the backend.
 */
export interface SystemAdminStatsDTO {
  /** Total number of courses. */
  anzahlKurse: number;
  /** Total registered users. */
  anzahlNutzer: number;
  /** Total files stored platform-wide. */
  anzahlDateien: number;
  /** Used storage in megabytes. */
  megabyteBelegt: number;
  /** Available storage in megabytes. */
  megabyteVerfuegbar: number;
  /** Pending course creation requests. */
  anzahlKurserstellungsanfragen: number;
}

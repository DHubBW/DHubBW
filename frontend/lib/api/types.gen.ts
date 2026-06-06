// FALLBACK: TODO regenerate via `npm run generate:api` nach BE-Lokal-Run.
// Stand 2026-06-06 — BE-1 (#201, AuthResponseDTO body.token + cookie hardening)
// und BE-5 (#205, StudentProfileDTO.pendingKursRequest + PendingKursRequestDTO)
// sind in dieser handcoded Fallback-Datei abgedeckt.
//
// Diese Datei wurde handcoded angelegt, weil das Backend zum Sub-A-Zeitpunkt
// nicht lokal lief. Sub-B hat sie um BE-5-Felder ergänzt. Sobald
// `cd ../backend && ./mvnw spring-boot:run` läuft, ersetzt
// `npm run generate:api` diesen Inhalt durch den `openapi-typescript`-Output —
// dieser zusätzlich liefert path-typed Endpoint-Types, die der Fallback nicht
// abdeckt (z.B. `paths['/public/auth/login']['post']['responses']['200']`).
//
// Quellen (Backend-DTOs):
// - student/application/dto/AuthResponseDTO.java         (record)
// - student/application/dto/StudentProfileDTO.java       (Lombok @Getter)
// - student/application/dto/PendingKursRequestDTO.java   (Lombok @Getter, BE-5)
// - student/application/dto/LoginRequestDTO.java
// - student/application/dto/RegisterRequestDTO.java
// - shared/application/dto/SmallResponseDTO.java
// - shared/application/dto/RequestStatusResponseDTO.java
//
// Wichtig: `studentType` und `kursName` MÜSSEN mit den Konsumenten in
// `components/layout/navItems.ts` (Sidebar/BottomNav) übereinstimmen, sonst
// brechen die FE-S2-Komponenten. Sub-B exportiert deshalb in
// `lib/auth/AuthContext.tsx` einen Type-Alias `AuthUser = StudentProfileDTO`.

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
 * Pending course request shown on the student dashboard (BE-5, #205).
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
   * Open course request of the student (BE-5, #205).
   * `null` when there is no pending join or creation request.
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

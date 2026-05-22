# DHubBW – Backend

Spring Boot REST API für die DHubBW Studierendenplattform.

**Technologie-Stack:** Java · Spring Boot 4 · Maven · H2 (Dev) · PostgreSQL (Prod) · Spring Security · OpenAPI · JaCoCo · Checkstyle · Flyway

---

## Überblick

Das Backend stellt eine REST-API zur Verfügung und verwaltet die Kernfunktionalitäten der Plattform:

| Modul | Funktion |
|---|---|
| `kurs` | Kursverwaltung, Beitritts- und Erstellungsanfragen |
| `student` | Studentenstammdaten und Rollenverwaltung |
| `mensa` | Mensa-Speiseplan (automatischer Scraper) |
| `datei` | Datei-Upload und -Download |
| `ordner` | Ordnerverwaltung |
| `dashboard` | Aggregation von Dashboard-Daten |
| `shared` | Querschnittsbelange: Security, Logging, OpenAPI, Scheduling |

**Rollen:** `STUDENT` → `KURSADMIN` → `SYSTEMADMIN` (hierarchisch)

---

## Schnellstart

```bash
# Lokal starten (Dev-Profil mit H2 in-memory DB)
./mvnw spring-boot:run

# Tests & Checkstyle ausführen
./mvnw verify
./mvnw checkstyle:check

# Docker-Image bauen und starten
docker build -f backend/docker/Dockerfile -t dhubbw-backend:latest backend/
docker run -p 8080:8080 dhubbw-backend:latest
```

Health-Check nach dem Start: `GET http://localhost:8080/actuator/health`

Für ausführliche Docker-Dokumentation: [backend/docker/README.md](docker/README.md)

---

## Ordnerstruktur

```
backend/
├── checkstyle.xml                  — Checkstyle-Regeln
├── docker/
│   ├── Dockerfile                  — Docker-Image für das Backend
│   └── README.md                   — Docker-Deployment-Anleitung
├── mvnw / mvnw.cmd                 — Maven Wrapper (kein lokales Maven nötig)
├── pom.xml                         — Maven Build-Konfiguration
└── src/
    ├── main/
    │   ├── java/com/se/dhubbw/
    │   │   ├── DhubbwApplication.java      — Einstiegspunkt
    │   │   ├── datei/                      — Datei-Upload & -Download
    │   │   │   ├── application/            — Controller, Services, DTOs
    │   │   │   └── infrastructure/db/      — JPA-Entities, Repositories
    │   │   ├── mensa/                      — Mensa-Speiseplan
    │   │   ├── student/                    — Studentenverwaltung
    │   │   ├── ordner/                     — Ordnerverwaltung
    │   │   ├── kurs/                       — Kursverwaltung
    │   │   │   ├── application/            — Controller, Services, DTOs
    │   │   │   └── infrastructure/db/      — Entities (Kurs, Anfragen, ...)
    │   │   ├── dashboard/                  — Dashboard-Aggregation
    │   │   │   ├── application/            — Controller, ServiceImpl
    │   │   │   └── domain/                 — Service-Interface
    │   │   └── shared/                     — Querschnittsbelange
    │   │       ├── application/            — PublicController, AdminController
    │   │       └── infrastructure/
    │   │           ├── config/
    │   │           │   ├── security/       — DevSecurityConfiguration, ProdSecurityConfiguration, JwtService, JwtAuthenticationFilter
    │   │           │   └── modules/        — OpenApiConfig, SchedulingConfig
    │   │           ├── aspects/logging/    — AOP Repository-Logging
    │   │           └── logger/             — ColorLogger
    │   └── resources/
    │       ├── application.yaml            — Basis-Konfiguration
    │       ├── application-dev.yaml        — Dev-Profil (H2, Logging)
    │       ├── application-prod.yaml       — Prod-Profil (PostgreSQL, kein Swagger)
    │       ├── data.sql                    — Testdaten für Dev-Profil
    │       └── db/migration/               — Flyway-SQL-Migrationen
    └── test/
        └── java/com/se/dhubbw/            — Integrations- & Unit-Tests
```

Jedes Feature-Modul folgt dem gleichen Muster:
- `application/` → Controller (`@RestController`), Services (`@Service`), DTOs
- `application/dto/` → Request/Response-DTOs (erfordert `package-info.java`)
- `infrastructure/db/` → JPA-Entities (`@Entity`), Repositories (`JpaRepository`)

---

## Konfiguration

### Spring-Profile

| Profil | Datei | Datenbank | Auth-Methode | Besonderheiten |
|---|---|---|---|---|
| `dev` (Standard) | `application-dev.yaml` | H2 in-memory | JWT (Cookie) | `data.sql` lädt Testdaten, Swagger aktiv, SQL-Logging |
| `prod` | `application-prod.yaml` | PostgreSQL | JWT (Cookie) | Credentials via Umgebungsvariablen, Flyway-Migrationen, kein Swagger |

**Authentifizierung:**

Beide Profile nutzen JWT-basierte Authentifizierung mit Cookie-Storage:

- **Login:** `POST /public/auth/login` mit `{"dhbwMail": "...", "password": "..."}`
- **Registrierung:** `POST /public/auth/register` mit vollständigen Studentendaten
- **JWT:** Wird als `HttpOnly` Cookie gesetzt (Dev: `Secure=false`, Prod: `Secure=true`)

**Dev-Profil Umgebungsvariablen (optional):**

```bash
JWT_SECRET=dev-secret-key-minimum-256-bits-required-for-hs256-algorithm-security
```

Falls nicht gesetzt, wird der Standard-Dev-Secret verwendet.

### Prod-Profil Umgebungsvariablen

```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db>
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
JWT_SECRET=<secure-secret-key-minimum-256-bits>
```

### Checkstyle (`checkstyle.xml`)

Checkstyle läuft automatisch bei `mvn verify` (nur `src/main/java`):

- Max. Zeilenlänge: 80 Zeichen
- Alle `public`-Felder, -Methoden und -Klassen benötigen Javadoc
- Jedes neue Package benötigt eine `package-info.java`
- Alle Methoden-Parameter müssen `final` sein
- Keine Wildcard-Imports, keine Magic Numbers

---

## Vorsicht – diese Dateien nicht einfach so ändern

| Datei / Verzeichnis | Warum Vorsicht |
|---|---|
| `shared/infrastructure/config/security/ProdSecurityConfiguration.java` | Produktiv-Security — Änderungen können den Produktivbetrieb gefährden |
| `src/main/resources/data.sql` | Integrationstests setzen bestimmte IDs und E-Mail-Adressen voraus — nur ergänzen, nie vorhandene Einträge verändern |
| `checkstyle.xml` | Nur nach Team-Absprache ändern — alle müssen danach ihre Checkstyle-Fehler beheben |
| `src/main/resources/db/migration/` | Flyway-Migrationen sind immutable — bestehende SQL-Dateien **nie** bearbeiten, nur neue hinzufügen |
| `target/` | Maven-Ausgabe — wird vom Maven-Wrapper generiert und **nicht** committet |

---

## Links & Dokumentation

### Automatisch generierte Dokumentation
- **[Javadoc](https://dhubbw-be460f.gitlab.io/apidocs/)** – API-Dokumentation aus dem Java-Quellcode
- **[JaCoCo Test Coverage](https://dhubbw-be460f.gitlab.io/jacoco/)** – Testabdeckungsreport
- **[OpenAPI Spec](https://dhubbw-be460f.gitlab.io/openapi.json)** – REST-API-Spezifikation (JSON)
- **[Swagger UI](http://localhost:8080/swagger-ui.html)** – Interaktiver API-Explorer (nur Dev-Profil)

### Wiki
- **[Architektur](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Design-Programmentwurf/Architektur)** – Systemarchitektur und Designentscheidungen
- **[Datenbank](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Design-Programmentwurf/Datenbank)** – Datenbankdesign und Datenmodellierung
- **[APIs](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Technische-Dokumentation/Codebasis/APIs)** – API-Referenzen und Schnittstellenbeschreibungen
- **[Spring Boot File-Upload & Download](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Technische-Dokumentation/Codebasis/Spring-Boot-File-Upload-Download)** – Multipart-Config, Controller-Signaturen, Download-Endpoint
- **[Docker & Ubuntu VM Setup](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Technische-Dokumentation/Tutorials/Docker-Ubuntu-VM-Setup)** – Einrichtung der Entwicklungsumgebung
- **[Docker Grundlagen & GitLab](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Technische-Dokumentation/Tutorials/Docker-Grundlagen-und-GitLab)** – Docker-Konzepte und CI/CD-Workflow
- **[Backend-Technologien](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Requirements-Engineering/WIE-Ebene/Technologieabwägung/Backend-Technologien)** – Technologieabwägung und -auswahl

---

_Stand: Mai 2026 | DHBW Karlsruhe TINF24B2 Gruppe 4_

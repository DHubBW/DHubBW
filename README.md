<p align="center">
    <img src="./DHubBW-Logo.png" alt="DHubBW Logo" width="200" />
</p>

<h1 align="center">DHubBW – Studierendenplattform für die DHBW Karlsruhe</h1>

<p align="center"><em>Ein Hub für Dateien, Stundenpläne, Umfragen, Gruppen und Wissen – sicher, einfach und kollaborativ.</em></p>

<p align="center"><sub>Auftraggeber: DHBW Karlsruhe</sub></p>

<p align="center">
    <a href="https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dhubbw"><b>DHubBW App</b></a> •
    <a href="https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/sonstiges">Sonstiges</a> •
    <a href="https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/home">Wiki</a> •
    <a href="https://sonstiges-42cd1e.gitlab.io">Statusreports</a> •
    <a href="https://dhubbw-be460f.gitlab.io">Dokumentation</a> •
    <a href="https://vm553.ipp-vps.net">Live-App</a>
</p>

---

## Live-Deployment

> **Die Anwendung ist produktiv erreichbar unter: [https://vm553.ipp-vps.net](https://vm553.ipp-vps.net)**

Die deployte Instanz läuft auf einer VM mit PostgreSQL, Spring Boot Backend und React Native Frontend (via Docker Compose). Login und Registrierung sind über die Web-Oberfläche möglich.

---

## Deployment

Die ausführbaren Systeme werden als **Docker-Images** ausgeliefert. Sowohl das Spring Boot Backend als auch das React Native (Web) Frontend werden bei jedem `main`-Push automatisch durch die CI/CD-Pipeline in die **GitLab Container Registry dieses Projekts** gepusht.

Die Deployment-Dateien (`docker-compose.yml` + `.env.example`) liegen auf einem dedizierten **`deployment`-Branch** dieses Repos. Dort sind ausschließlich die zum Deployment nötigen Dateien vorhanden – Backend- und Frontend-Source-Code sind dort nicht enthalten, sodass sie als kompaktes Paket abrufbar sind. Zusätzlich existieren dieselben Dateien unter [`deploy/`](./deploy/) im `main`-Branch, weil der CI/CD-Deploy-Job sie von dort liest.

Der `deployment`-Branch wird bei Änderungen unter `deploy/` aktualisiert; er enthält nie Code-Änderungen aus `main`.

### Bezogene Images

| Komponente | Image |
|---|---|
| Backend  | `registry.gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dhubbw/backend:latest`  |
| Frontend | `registry.gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dhubbw/frontend:latest` |
| Datenbank | offizielles `postgres:17` von Docker Hub |

Jeder Push taggt zusätzlich mit dem Commit-SHA (`...:<short-sha>`), sodass auch ältere Stände reproduzierbar deployt werden können.

### Deployment auf einer eigenen VM

1. Dateien vom `deployment`-Branch holen (alternativ: aus `deploy/` auf `main`):

   ```bash
   git clone --branch deployment --single-branch --depth 1 \
     https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dhubbw.git deployment
   cd deployment
   cp .env.example .env
   ```

2. `.env` an die Umgebung anpassen (siehe [unten](#zu-konfigurierende-werte-in-env)).

3. Bei der GitLab Container Registry anmelden (das Projekt ist privat, ein Pull-Token mit Scope `read_registry` ist erforderlich):

   ```bash
   docker login registry.gitlab.com -u <gitlab-username> -p <personal-access-token>
   ```

4. Stack hochfahren:

   ```bash
   docker compose --env-file .env -f docker-compose.yml pull
   docker compose --env-file .env -f docker-compose.yml up -d
   ```

   Nach ca. 60 Sekunden ist das Frontend auf `http://<host>/` (bzw. `https://<host>/` mit TLS) und Swagger unter `/swagger-ui/index.html` erreichbar.

### Zu konfigurierende Werte in `.env`

Die [`deploy/.env.example`](./deploy/.env.example) enthält **funktionierende Default-Werte**, sodass für einen einfachen Testbetrieb keine Anpassung notwendig ist. Da es sich nicht um ein produktives Deployment handelt, liegen die Default-Zugangsdaten bewusst im Repository. Für einen produktiven Einsatz **müssen** mindestens diese Werte angepasst werden:

| Variable | Default | Beschreibung |
|---|---|---|
| `POSTGRES_DB` | `dhubbw` | Datenbankname |
| `POSTGRES_USER` | `dhubbw` | DB-Benutzer |
| `POSTGRES_PASSWORD` | `dhubbw-demo-pw` | DB-Passwort – **vor Produktiveinsatz ändern** |
| `JWT_SECRET` | Demo-Wert (56 Zeichen) | Signaturschlüssel für JWTs (HS256, ≥ 32 Bytes) – **vor Produktiveinsatz** mit `openssl rand -base64 48` generieren |
| `POSTGRES_DATA_DIR` | `./pgdata` (im `.env.example`) – Compose-Default ohne `.env`-Eintrag: `/opt/projects/devstack/db/data` (Projekt-VM) | Host-Verzeichnis für DB-Daten (Persistenz) |
| `BACKEND_IMAGE` | `registry.gitlab.com/.../backend:latest` | Auf einen Commit-SHA festsetzbar |
| `FRONTEND_IMAGE` | `registry.gitlab.com/.../frontend:latest` | s.o. |
| `FRONTEND_HTTP_PORT` | `80` | Host-Port für HTTP |
| `FRONTEND_HTTPS_PORT` | `443` | Host-Port für HTTPS |
| `LETSENCRYPT_DIR` | `/etc/letsencrypt` | TLS-Zertifikate (für HTTP-only-Betrieb irrelevant) |
| `CERTBOT_WEBROOT` | `/var/www/certbot` | Webroot für Let's Encrypt |

### HTTP-only-Betrieb (ohne TLS)

Falls auf der Ziel-VM kein Let's-Encrypt-Zertifikat vorhanden ist, müssen die beiden TLS-Volumes im `frontend`-Service der `docker-compose.yml` auskommentiert werden – nginx liefert dann nur auf Port 80 aus. Der Stack startet dadurch ohne Vorbedingungen.

---

## Was ist DHubBW?

**DHubBW** ist eine integrierte Webplattform, die das Studium an der DHBW Karlsruhe vereinfacht. Anstatt zwischen Dutzenden Tools und Systemen zu jonglieren, bündelt DHubBW Kursmaterialien, Dateien, Termine, Kommunikation und Wissen an einem zentralen Ort.

**Kernziel:** Den organisatorischen Aufwand im Studienalltag senken und Zusammenarbeit, Transparenz sowie Wissensaustausch nachhaltig verbessern.

---

## Dokumentation & Wiki

Die vollständige Projektdokumentation befindet sich im zentralen 
## → **[DHubBW Wiki](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/home)←**.

---

## Projektorganisation

### Repositories

| Repository | Beschreibung |
|---|---|
| **[DHubBW (dieses Repository)](https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dhubbw)** | Anwendungscode (Frontend, Backend, Tests & Infrastruktur) |
| **[Sonstiges](https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/sonstiges)** | Präsentationen, Artefakte sowie sonstige Projektdokumente |

### Weitere Links

- **[Live-App (Produktiv-Deployment)](https://vm553.ipp-vps.net)** – DHubBW Anwendung auf der VM (PostgreSQL + Backend + Frontend)
- **[Projektstrukturplan (PSP)](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/epics/10)** – Hierarchische Gliederung aller Arbeitspakete und Meilensteine
- **[Issue-Tracking](https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dhubbw/-/issues)** – Alle Issues, Bugs und Feature-Requests
- **[Statusreports](https://sonstiges-42cd1e.gitlab.io)** – Wöchentliche Projektberichte als interaktive Webpräsentationen
- **[Dokumentation](https://dhubbw-be460f.gitlab.io)** – Javadoc, JaCoCo Test Coverage & OpenAPI Spec (automatisch generiert via CI/CD)

---

## Repository-Struktur

### GitLab-Templates

Im Verzeichnis **`.gitlab/`** befinden sich alle Templates für GitLab:

- **Issue-Templates** – Vorlagen für Bug-Reports, Feature-Requests und weitere Issue-Typen
- **Merge-Request-Templates** – Strukturierte Vorlagen für Code-Reviews und Pull-Requests
- **CI/CD-Konfigurationen** – Pipeline-Definitionen für automatisierte Tests und Deployments

### Source Code

| Verzeichnis | README | Inhalt |
|---|---|---|
| **[`backend/`](./backend/)** | **[backend/README.md](./backend/README.md)** | Spring Boot REST API – Überblick, Ordnerstruktur, Konfiguration, Vorsichtshinweise |
| **[`frontend/`](./frontend/)** | **[frontend/README.md](./frontend/README.md)** | Expo (React Native) App – Überblick, Ordnerstruktur, Konfiguration, Vorsichtshinweise |
| **[`deploy/`](./deploy/)** | – | `docker-compose.yml` für den Produktivbetrieb (PostgreSQL + Backend + Frontend) |

---

## Mitarbeit & Feedback

**Du hast Ideen, Fragen oder hast einen Bug gefunden?**

- **[Neue Issues erstellen](https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dhubbw/-/issues/new)** – Melde Bugs, schlage neue Features vor oder stelle Fragen (nutze unsere Issue-Templates aus `.gitlab/`)
- **Bestehende Issues kommentieren** – Teile deine Meinung zu geplanten Features oder Problemlösungen

Jeder Beitrag hilft uns, DHubBW besser zu machen!

---

_Stand: Mai 2026 | DHBW Karlsruhe TINF24B2 Gruppe 4_

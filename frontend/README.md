# DHubBW – Frontend

Expo (React Native) App für die DHubBW Studierendenplattform. Läuft auf iOS, Android und im Browser; das Web-Build wird als statisches nginx-Bundle deployed.

**Technologie-Stack:** TypeScript · React Native · Expo · expo-router · React Navigation · NativeWind · nginx (Web-Deployment)

---

## Überblick

Das Frontend bildet die Benutzeroberfläche der DHubBW-Plattform. Es kommuniziert mit dem Backend über die REST-API.

- **Routing:** File-based via `expo-router` – neue Screens → neue Datei unter `app/`
- **Styling:** React Native StyleSheet, plattformspezifische Anpassungen via `hooks/`
- **Web-Deployment:** Multi-stage Docker-Build (`npm run build-web` → nginx)

---

## Schnellstart

```bash
# Abhängigkeiten installieren
npm install

# .env aus Vorlage erstellen und anpassen
cp .env.example .env

# Expo Dev Server starten (QR-Code für Expo Go)
npm start

# Plattform-spezifisch starten
npm run web       # Browser (http://localhost:8081)
npm run ios       # iOS Simulator (macOS + Xcode erforderlich)
npm run android   # Android Emulator (Android Studio erforderlich)

# Code-Qualität prüfen
npm run lint

# Web-Bundle für Deployment bauen
npm run build-web    # Ausgabe: dist/
```

### `.env`-Konfiguration

Erstelle eine `.env`-Datei im `frontend/`-Verzeichnis (Vorlage: `.env.example`):

| Variable                         | Beispiel                | Beschreibung                                                                                |
| -------------------------------- | ----------------------- | ------------------------------------------------------------------------------------------- |
| `EXPO_PUBLIC_API_BASE_URL`       | `http://localhost:8080` | Backend-URL. Für Expo Go auf Smartphone: LAN-IP verwenden (z.B. `http://192.168.1.42:8080`) |
| `EXPO_PUBLIC_MAX_UPLOAD_MB`      | `10`                    | Maximale Dateigröße für Uploads in MB                                                       |
| `EXPO_PUBLIC_ENABLE_QUICK_LOGIN` | `false`                 | Quick-Login-Modus (siehe unten)                                                             |

### Quick-Login-Flag

`EXPO_PUBLIC_ENABLE_QUICK_LOGIN=true` aktiviert vorbelegte Login-Felder für die Entwicklung:

- Zeigt Demo-Accounts direkt im Login-Formular an (Student, Kursadmin, Systemadmin)
- **Nur für lokale Entwicklung verwenden — nie in Produktion setzen**

### Native Build mit EAS

Das Projekt nutzt [Expo Application Services (EAS)](https://expo.dev/eas) für native iOS- und Android-Builds:

```bash
# EAS CLI installieren (einmalig)
npm install -g eas-cli

# Beim Expo-Account anmelden
eas login

# Development Build erstellen (für physisches Gerät)
eas build --profile development --platform ios
eas build --profile development --platform android

# Production Build erstellen
eas build --profile production --platform all
```

> **Hinweis:** EAS-Builds benötigen ein Expo-Konto und sind für CI/CD-Pipelines konfigurierbar. Für reine Web-Deployments genügt `npm run build-web`.

---

## Tests

```bash
# Alle Unit- und Integrationstests ausführen
npm test

# Tests im Watch-Modus (interaktiv während der Entwicklung)
npm run test:watch

# Tests mit Coverage-Report
npm run test:coverage

# E2E-Tests (Playwright, Web) – Expo-Webserver wird automatisch gestartet
npm run test:e2e

# E2E-Tests mit sichtbarem Browser
npm run test:e2e:headed
```

### Teststruktur

```
tests/
├── components/       — Unit-Tests für UI-Komponenten (@testing-library/react-native)
├── integration/      — Integrationstests mit MSW-Mock-Server
└── mocks/
    ├── handlers.ts   — MSW Request-Handler (Auth, Profile, Mensa)
    └── server.ts     — MSW setupServer (gestartet in jest.setup.js)
e2e/
└── login.spec.ts     — Playwright E2E-Tests (Web)
```

Der MSW-Mock-Server wird automatisch vor jedem Test-File gestartet und danach gestoppt.
Pre-Commit-Hook (Husky + lint-staged): linted und formatiert `.ts`/`.tsx`-Dateien vor jedem Commit.

---

## Ordnerstruktur

```
frontend/
├── app/                        — Routen (Expo Router, file-based)
│   ├── (app)/                  — Authentifizierte Screens (Auth-Guard)
│   │   ├── dashboard.tsx
│   │   ├── dateien.tsx
│   │   ├── mensa.tsx
│   │   └── verwaltung.tsx
│   ├── (auth)/                 — Login / Registrierung
│   │   ├── login.tsx
│   │   └── register.tsx
│   ├── (legal)/                — Statische Rechtstexte (kein Auth-Guard)
│   │   ├── impressum.tsx       — Impressum gemäß § 5 TMG
│   │   └── datenschutz.tsx     — Datenschutzerklärung gemäß DSGVO
│   ├── _layout.tsx             — Root-Layout (Fonts, Splash Screen, Navigation)
│   └── index.tsx               — Redirect-Entry
├── assets/                     — Statische Ressourcen
│   ├── fonts/                  — Schriftarten
│   └── images/                 — App-Icons, Splash Screens
├── components/                 — Wiederverwendbare UI-Komponenten
│   ├── layout/
│   │   ├── AppLayout.tsx       — Responsives Shell-Layout (Header+BottomNav / Sidebar)
│   │   ├── Footer.tsx          — Footer mit Impressum- und Datenschutz-Links
│   │   ├── Header.tsx
│   │   ├── Sidebar.tsx
│   │   └── BottomNav.tsx
│   ├── CookieBanner.tsx        — DSGVO Cookie-Consent-Banner (Web only)
│   └── ui/                     — rn-reusables / shadcn-Stil Primitives
├── lib/
│   ├── consent/
│   │   └── useCookieConsent.ts — Hook: Cookie-Consent-Zustand (Web: localStorage)
│   ├── auth/
│   ├── api/
│   ├── i18n/
│   ├── storage/
│   └── theme/
├── tests/                      — Unit- und Integrationstests
│   ├── components/
│   └── integration/
├── e2e/                        — Playwright E2E-Tests
├── scripts/
│   └── reset-project.js        — Setzt App auf Starter-Zustand zurück
├── Dockerfile                  — Multi-stage: expo export + nginx
├── app.json                    — Expo-Konfiguration
├── global.css                  — Tailwind-Direktiven + CSS-Variablen für Light/Dark
├── tailwind.config.js
├── eslint.config.js
├── package.json
└── tsconfig.json
```

Neue Screens kommen als Datei unter `app/` hinzu – `expo-router` leitet automatisch darauf.
Neue Tabs kommen unter `app/(tabs)/` und werden in `app/(tabs)/_layout.tsx` registriert.

---

## Konfiguration

### `app.json`

Zentrale Expo-Konfiguration: App-Name, Bundle-ID, Icon, Splash Screen, Android Adaptive Icons.
Änderungen hier wirken sich auf den nativen Build aus – nach Änderungen ggf. `npx expo prebuild` ausführen.

### `tsconfig.json`

Erbt von `expo/tsconfig.base`. Pfad-Aliases sind über `expo-router` bereits vorkonfiguriert (`~/` → Repo-Root).

### `eslint.config.js`

Verwendet das Expo-ESLint-Preset. Linting vor dem Commit: `npm run lint`.

### `Dockerfile` (Web-Deployment)

Multi-stage Build:

1. `node:22-alpine` – `npm ci` + `npm run build-web` → erzeugt `dist/`
2. `nginx:alpine` – serviert `dist/` über Port 80

Im Produktivbetrieb wird das Image durch die CI/CD-Pipeline gebaut und via `docker compose` gestartet (siehe [`deploy/docker-compose.yml`](../deploy/docker-compose.yml)).

---

## Vorsicht – diese Dateien nicht einfach so ändern

| Datei / Verzeichnis        | Warum Vorsicht                                                                                                                             |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `scripts/reset-project.js` | Löscht den gesamten App-Inhalt und ersetzt ihn durch ein leeres Starter-Template – **nie versehentlich** `npm run reset-project` ausführen |
| `dist/`                    | Wird durch `npm run build-web` generiert – **nicht committen**                                                                             |
| `app/_layout.tsx`          | Root-Layout mit Fonts und Splash Screen – fehlerhafte Änderungen hier führen zu einem weißen Startbildschirm                               |
| `components/ui/`           | rn-reusables/shadcn-generierte Primitives – **nicht manuell bearbeiten**                                                                   |

---

## Rechtliche Seiten (Impressum & Datenschutz)

Die Seiten unter `app/(legal)/` sind statisch und öffentlich zugänglich (kein Auth-Guard):

| Route          | Datei                         | Beschreibung                             |
| -------------- | ----------------------------- | ---------------------------------------- |
| `/impressum`   | `app/(legal)/impressum.tsx`   | Impressum gemäß § 5 TMG                  |
| `/datenschutz` | `app/(legal)/datenschutz.tsx` | Datenschutzerklärung gemäß DSGVO / TTDSG |

Der `Footer` (`components/layout/Footer.tsx`) verlinkt beide Seiten und ist in:

- `components/layout/AppLayout.tsx` — alle authentifizierten Screens
- `app/(auth)/login.tsx` und `app/(auth)/register.tsx` — Login/Register

**Cookie-Consent-Banner (`components/CookieBanner.tsx`):** Erscheint nur auf Web und nur beim
ersten Besuch. Die Entscheidung wird im `localStorage` unter dem Key `cookie_consent` gespeichert.
Der Hook `lib/consent/useCookieConsent.ts` verwaltet den Zustand. Auf Native (iOS/Android) ist der
Consent immer `'accepted'` (kein Browser-Cookie-Konzept).

---

## Links & Dokumentation

### Live-App

**[https://vm553.ipp-vps.net](https://vm553.ipp-vps.net)** – Produktiv-Deployment der Anwendung (PostgreSQL + Backend + Frontend via Docker Compose auf VM `vm553.ipp-vps.net`)

### Wiki

- **[GUI-Entwurf](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Design-Programmentwurf/GUI-Entwurf)** – Grafische Benutzeroberfläche und User Experience
- **[Architektur](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Design-Programmentwurf/Architektur)** – Systemarchitektur und Designentscheidungen
- **[Frontend-Technologien](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Requirements-Engineering/WIE-Ebene/Technologieabwägung/Frontend-Technologien)** – Technologieabwägung und -auswahl
- **[Docker & Ubuntu VM Setup](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Technische-Dokumentation/Tutorials/Docker-Ubuntu-VM-Setup)** – Einrichtung der Entwicklungsumgebung
- **[Docker Grundlagen & GitLab](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Technische-Dokumentation/Tutorials/Docker-Grundlagen-und-GitLab)** – Docker-Konzepte und CI/CD-Workflow

---

_Stand: Mai 2026 | DHBW Karlsruhe TINF24B2 Gruppe 4_


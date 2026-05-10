# DHubBW – Frontend

Expo (React Native) App für die DHubBW Studierendenplattform. Läuft auf iOS, Android und im Browser; das Web-Build wird als statisches nginx-Bundle deployed.

**Technologie-Stack:** TypeScript · React Native · Expo · expo-router · React Navigation · nginx (Web-Deployment)

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

---

## Ordnerstruktur

```
frontend/
├── app/                        — Routen (Expo Router, file-based)
│   ├── (tabs)/                 — Tab-Navigation
│   │   ├── _layout.tsx         — Tab-Bar-Konfiguration (Icons, Labels)
│   │   ├── index.tsx           — Home-Tab
│   │   └── explore.tsx         — Explore-Tab
│   ├── _layout.tsx             — Root-Layout (Fonts, Splash Screen, Navigation)
│   └── modal.tsx               — Modal-Bildschirm
├── assets/                     — Statische Ressourcen
│   ├── fonts/                  — Schriftarten
│   └── images/                 — App-Icons, Splash Screens
├── components/                 — Wiederverwendbare UI-Komponenten
│   ├── ui/                     — Plattformspezifische Icons und Tabs
│   ├── external-link.tsx       — Link-Komponente (öffnet externen Browser)
│   ├── haptic-tab.tsx          — Tab mit haptischem Feedback (iOS)
│   ├── parallax-scroll-view.tsx
│   ├── themed-text.tsx         — Text-Komponente mit Theme-Support
│   └── themed-view.tsx         — View-Komponente mit Theme-Support
├── constants/                  — App-Konstanten
│   └── Colors.ts               — Farbpalette (Light/Dark Mode)
├── hooks/                      — Custom React Hooks
│   ├── use-color-scheme.ts     — Aktives Farbschema (native)
│   ├── use-color-scheme.web.ts — Aktives Farbschema (Web)
│   └── use-theme-color.ts      — Farbe aus Farbpalette
├── scripts/
│   └── reset-project.js        — Setzt App auf Starter-Zustand zurück
├── Dockerfile                  — Multi-stage: expo export + nginx
├── app.json                    — Expo-Konfiguration
├── eslint.config.js            — ESLint-Regeln
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

| Datei / Verzeichnis | Warum Vorsicht |
|---|---|
| `scripts/reset-project.js` | Löscht den gesamten App-Inhalt und ersetzt ihn durch ein leeres Starter-Template – **nie versehentlich** `npm run reset-project` ausführen |
| `dist/` | Wird durch `npm run build-web` generiert – **nicht committen** |
| `app/_layout.tsx` | Root-Layout mit Fonts und Splash Screen – fehlerhafte Änderungen hier führen zu einem weißen Startbildschirm |

---

## Links & Dokumentation

### Wiki
- **[GUI-Entwurf](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Design-Programmentwurf/GUI-Entwurf)** – Grafische Benutzeroberfläche und User Experience
- **[Architektur](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Design-Programmentwurf/Architektur)** – Systemarchitektur und Designentscheidungen
- **[Frontend-Technologien](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Requirements-Engineering/WIE-Ebene/Technologieabwägung/Frontend-Technologien)** – Technologieabwägung und -auswahl
- **[Docker & Ubuntu VM Setup](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Technische-Dokumentation/Tutorials/Docker-Ubuntu-VM-Setup)** – Einrichtung der Entwicklungsumgebung
- **[Docker Grundlagen & GitLab](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Technische-Dokumentation/Tutorials/Docker-Grundlagen-und-GitLab)** – Docker-Konzepte und CI/CD-Workflow

---

_Stand: Mai 2026 | DHBW Karlsruhe TINF24B2 Gruppe 4_

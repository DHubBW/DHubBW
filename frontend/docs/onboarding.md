# DHubBW Frontend – Onboarding für neue Entwickler

Willkommen im DHubBW-Frontend! Diese Anleitung hilft dir, schnell produktiv zu werden.

---

## Voraussetzungen

- **Node.js** ≥ 20 (empfohlen: 22 LTS)
- **npm** ≥ 10
- **Expo CLI** (wird via `npx expo` aufgerufen, kein globales Install nötig)
- Optional: Xcode (iOS) oder Android Studio (Android) für native Simulatoren

---

## Erste Schritte

```bash
# Im frontend/-Verzeichnis:
npm install
cp .env.example .env   # .env anpassen (API-URL etc.)
npm run web            # Web-Dev-Server starten → http://localhost:8081
```

Vollständige Kommandos: siehe [README.md](../README.md).

---

## Architektur-Überblick

Das Frontend ist eine **React Native / Expo**-App mit dateibasiertem Routing via `expo-router`.

```
frontend/
├── app/             – Screens (Routing, Seiten-Aufbau)
│   ├── (app)/       – Auth-geschützte Screens (Dashboard, Dateien, Mensa …)
│   ├── (auth)/      – Öffentliche Screens (Login, Registrierung)
│   └── (legal)/     – Impressum, Datenschutz
├── components/      – Wiederverwendbare UI-Komponenten
│   ├── layout/      – App-Shell (Header, Sidebar, BottomNav)
│   └── ui/          – Primitive Bausteine (rn-reusables/shadcn-Stil)
├── hooks/           – Custom Hooks (Daten, Logik)
├── lib/             – Services, Auth, API, Theme, i18n
├── tests/           – Unit- und Integrationstests
└── e2e/             – Playwright E2E-Tests
```

### Schichtenaufteilung

| Schicht          | Verzeichnis                  | Verantwortung              |
| ---------------- | ---------------------------- | -------------------------- |
| Routing / Seiten | `app/`                       | Navigation, Screen-Aufbau  |
| App-Shell        | `components/layout/`         | Header, BottomNav, Sidebar |
| UI-Primitives    | `components/ui/`             | Zustandslose Bausteine     |
| Business-Logik   | `hooks/`, `lib/`             | Auth, Theme, i18n, API     |
| Utilities        | `lib/utils.ts`, `constants/` | `cn()`, Farben             |

Einen neuen Screen anlegen:

1. Datei in `app/(app)/` erstellen → expo-router leitet automatisch darauf
2. Eintrag in `components/layout/navItems.ts` hinzufügen (für Navigation)
3. Keine manuelle Stack-Registrierung nötig

Detaillierte Architektur-Dokumentation: [Wiki → Architektur](https://gitlab.com/groups/dhbw-se/se-tinf24b2/gruppe-4/-/wikis/Design-Programmentwurf/Architektur)

---

## Auth-Flow

Die App verwendet **JWT-Cookie-Auth**. Der Auth-Kontext (`lib/auth/AuthContext.tsx`) stellt `useAuth()` bereit:

```tsx
import { useAuth } from "@/lib/auth/AuthContext";

const { user, isLoading } = useAuth();
// user: AuthUser | null
// AuthUser: { id, name, dhbwMail, studentType, kursName }
// studentType: 'STUDENT' | 'KURSADMIN' | 'SYSTEMADMIN'
```

**Auth-Guard** läuft in `app/(app)/_layout.tsx`: nicht authentifizierte Nutzer werden zu `/login` umgeleitet.

**Quick-Login für Entwicklung:** `EXPO_PUBLIC_ENABLE_QUICK_LOGIN=true` in `.env` zeigt Demo-Account-Buttons im Login-Formular an.

---

## Styling

Das Projekt verwendet **NativeWind v4** (Tailwind CSS für React Native):

```tsx
// Immer Tailwind-Token statt Hex-Farben:
<View className="flex-1 bg-background p-4">
  <Text className="text-lg text-foreground">Hallo</Text>
</View>;

// Bedingte Klassen mit cn():
import { cn } from "@/lib/utils";
<View className={cn("rounded-lg p-4", isActive && "bg-primary")} />;
```

Wichtige Token: `bg-background`, `text-foreground`, `bg-card`, `bg-primary`, `bg-muted`, `border-border`. Vollständige Liste in `global.css`.

Dark Mode wird automatisch über NativeWind + `ThemeProvider` gesteuert — keine manuellen `colorScheme`-Abfragen nötig.

---

## API-Kommunikation

Das Backend läuft lokal unter `http://localhost:8080` (konfigurierbar via `EXPO_PUBLIC_API_BASE_URL` in `.env`).

- API-Calls immer in Hooks (`hooks/`) oder Services (`lib/api/`) auslagern — nie direkt im Screen
- React Query (`@tanstack/react-query`) für Fetching und Caching
- Auth: JWT-Cookie (wird automatisch via `credentials: 'include'` mitgeschickt)
- Swagger-Doku (autoritativ): `http://localhost:8080/swagger-ui/index.html`

---

## Issue-Konvention

Issues werden in GitLab verwaltet: [gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dhubbw](https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dhubbw).

**Branch-Naming:**

```
feature/<issue-id>-<kurze-beschreibung>
fix/<issue-id>-<kurze-beschreibung>
```

**Workflow:**

1. Branch von `main` erstellen
2. Feature implementieren (Definition of Done beachten)
3. `npx expo lint` + `npx prettier --check .` müssen grün sein
4. MR erstellen → mindestens 1 Approval vor Merge

Detaillierte Konventionen: `ki-documents/gitlab-issues.md` (im Repo-Root).

---

## Tests ausführen

```bash
npm test                  # Unit + Integrationstests
npm run test:watch        # Watch-Modus
npm run test:coverage     # Mit Coverage-Report
npm run test:e2e          # Playwright E2E (Web)
```

Neue Komponenten: Unit-Test unter `tests/components/`.
Neue API-Endpunkte: MSW-Handler in `tests/mocks/handlers.ts` + Integrationstest in `tests/integration/`.

---

_Stand: Juni 2026 | DHBW Karlsruhe TINF24B2 Gruppe 4_

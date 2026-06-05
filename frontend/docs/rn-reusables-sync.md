# rn-reusables Sync

Stand: **2026-06-03**

## Versions

| Paket | Version |
|---|---|
| nativewind | 4.2.4 |
| tailwindcss | 3.4.19 |
| tailwindcss-animate | 1.0.7 |
| @react-native-reusables/cli | n/a (siehe unten) |
| @rn-primitives/portal | 1.4.0 |
| @rn-primitives/slot | 1.4.0 |
| @rn-primitives/avatar | 1.4.0 |
| @rn-primitives/dialog | 1.4.0 |
| @rn-primitives/label | 1.4.0 |
| @rn-primitives/separator | 1.4.0 |
| @rn-primitives/tooltip | 1.4.0 |
| class-variance-authority | 0.7.1 |
| clsx | 2.1.1 |
| tailwind-merge | 3.6.0 |
| lucide-react-native | 1.17.0 |
| react-native-svg | 15.15.5 |
| sonner-native | 0.24.0 |

## Komponenten im Repo

Quelle: `https://reactnativereusables.com/r/nativewind/<name>.json` (Stil
`new-york`). Zielordner: [components/ui/](../components/ui/), Helper:
[lib/utils.ts](../lib/utils.ts) (`cn(...)` aus clsx + tailwind-merge).

| Komponente | Datei | Auto-Dep |
|---|---|---|
| Button | components/ui/button.tsx | text |
| Card | components/ui/card.tsx | — |
| Dialog | components/ui/dialog.tsx | native-only-animated-view |
| Input | components/ui/input.tsx | — |
| Label | components/ui/label.tsx | — |
| Avatar | components/ui/avatar.tsx | — |
| Badge | components/ui/badge.tsx | — |
| Separator | components/ui/separator.tsx | — |
| Tooltip | components/ui/tooltip.tsx | — |
| Icon | components/ui/icon.tsx | — |
| Text *(auto)* | components/ui/text.tsx | dependency von Button |
| native-only-animated-view *(auto)* | components/ui/native-only-animated-view.tsx | dependency von Dialog |

Toast wird **nicht** als rn-reusables-Komponente eingezogen — `sonner-native`
erfüllt diese Rolle und ist im Root-Layout via `<Toaster>` registriert.

## Sync-Strategie

Komponenten sind **ins Repo kopiert** (shadcn-Pattern). Es gibt **kein
Auto-Update via `npm update`** — wer eine neuere Version pullen will, läuft
das Sync-Skript erneut und reviewt den Diff:

```bash
node scripts/sync-rn-reusables.js
```

## Warum ein eigenes Skript statt `@react-native-reusables/cli`

CLI v1.0.0 ist erkennbar für **Greenfield-Setups** geschrieben:

- `init` erstellt einen neuen `my-app/`-Subordner, auch wenn man im bereits
  vorhandenen Expo-Router-Projekt steht — die Option *"Initialize anyway"*
  überschreibt nicht in-place.
- `add` läuft im bestehenden Repo durch (incl. Spinner), schreibt aber keine
  Dateien (vermutlich erwartet die CLI, dass `init` zuerst eine bestimmte
  Projekt-Struktur angelegt hat).
- `--yes` / non-interactive-Modus wird vom Wrapper geslupft, der trotzdem auf
  TTY-Eingaben wartet.

Lösung: `scripts/sync-rn-reusables.js` ruft die **gleichen Registry-Endpoints
auf, die die CLI intern verwendet** (`https://reactnativereusables.com/r/nativewind/<name>.json`),
parst das Schema, und schreibt mit Pfad-Rewrite `@/registry/nativewind/...` →
`@/...` (analog zu `components.json`-Aliasen).

Output ist byte-identisch mit dem, was die CLI nach `add` schreiben würde.

**TODO Sub-C / Future Maintainer:** Falls die CLI v1.x in einer späteren
Minor-Version In-Place-Add unterstützt, kann das Skript wegfallen. Drift wird
durch `components.json` (rn-reusables-konformes Format) niedrig gehalten.

## Peer-Deps Audit

Nach Sync verifizieren, dass kein `@rn-primitives/...`-Import in
`components/ui/*.tsx` ohne installierte Dependency landet:

```bash
grep -h "from '@rn-primitives" components/ui/*.tsx | sort -u
```

aktueller Stand → alle 6 Primitives (`avatar`, `dialog`, `label`, `separator`,
`tooltip`, `slot`) plus `portal` für die Root-Mount sind in `package.json`.

## Showcase

Sub-C baut den `app/_dev/components.tsx`-Showcase, der jede der 10
Komponenten in Light + Dark rendert und damit die Theme-Token-Bindung
visuell verifiziert.

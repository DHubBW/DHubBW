# DHubBW – Studentenplattform für die DHBW Karlsruhe

Willkommen bei DHubBW, der zentralen Plattform für Studierende der DHBW Karlsruhe. Ziel ist ein Ort, an dem alles zusammenläuft: Dateien, Termine, Abstimmungen, Gruppen, Wissenstransfer und mehr – sicher, einfach und kollaborativ.

– Auftraggeber: DHBW Karlsruhe

## Inhalte

- [Überblick](#überblick)
- [Zielgruppe und Scope](#zielgruppe-und-scope)
- [Kernfunktionen (Muss-Anforderungen)](#kernfunktionen-muss-anforderungen)
- [Optionale Features (Soll/Nice-to-have)](#optionale-features-sollnice-to-have)
- [Ausblick (Kann-Anforderungen)](#ausblick-kann-anforderungen)
- [Requirements Engineering](#requirements-engineering)
- [Technischer Vorschlag (Tech-Stack)](#technischer-vorschlag-tech-stack)
- [Zusätzliche Notizen (aus der Diskussion)](#zusätzliche-notizen-aus-der-diskussion)
- [Zusammenarbeit & Workflow](#zusammenarbeit--workflow)
- [Weitere Inhalte und Links](#weitere-inhalte-und-links)

## Schnelleinstieg

Für organisatorische und technische Details siehe:

- Projektmanagement: [../projektmanagement/README.md](../projektmanagement/README.md)
- Dokumentation & Entwürfe: [../dokumentation/README.md](../dokumentation/README.md)

## Überblick

Die App bündelt studiumsrelevante Informationen und Werkzeuge:

- Zentraler Hub für Dateien, Termine, Abstimmungen und Gruppen
- Integration bestehender Systeme (z. B. Rapla/Moodle) mit Fokus auf studentische Nutzung
- Gemeinsames Lernen über Kursgrenzen hinweg, Wissen teilen und vernetzen

## Zielgruppe und Scope

- Primär: Studierende der DHBW Karlsruhe
- Sekundär: Lehrende/Dozierende (Abstimmungen, Kursorganisation)
- Start mit DHBW Karlsruhe, perspektivisch auf weitere Hochschulen erweiterbar

## Kernfunktionen (Muss-Anforderungen)

- Filesharing: Upload/Download, Versionen, Quotas; Teilen von Cheat Sheets, Notizen, Arbeitsblättern
- Kalenderintegration: Stundenplan, Abgabetermine, Klausurtermine; Mensa-Speiseplan
- Abstimmungen/Umfragen mit und für Studierende
- Accounts: Registrierung und Anmeldung für Studierende; Rollen & Rechte (Admin, Kursrollen)
- Wissenstransfer: Beiträge/Notizen teilen, Mitmachen für alle
- Vernetzung: Lerngruppenbildung, Studierende kursübergreifend kennenlernen

## Optionale Features (Soll/Nice-to-have)

- Verbindliche Abstimmungen (z. B. Kurs-Entscheidungen)
- Lernstatistiken (z. B. „Wie viele haben am x.y.2025 mit dem Lernen begonnen?“)
- „Find your Buddy“: Matchmaking für Lernpartner:innen nach Präferenzen
- Online-Messenger (Basis-Chat)
- Erweiterte Dateianmerkungen: Orientierung an GoodNotes Web (PDF markieren/zeichnen)
- Erweiterte Admin-Verwaltung für Accounts

## Ausblick (Kann-Anforderungen)

- Foren-Funktionen, Diskussions-Threads
- Freunde-/Kontakt-Matching
- Plattformfähigkeit über mehrere Hochschulen

## Requirements Engineering

- Gemeinsamer Workshop: Anforderungen im Kurs erheben, priorisieren (Muss/Soll/Kann)
- Stakeholder-Interviews (Studierende, Dozierende, Admins)
- Laufendes Backlog-Management und Abgleich mit Scope DHBW KA

## Technischer Vorschlag (Tech-Stack)

Frontend
- Next.js (App Router, TypeScript) – Framework & SSR
- React – UI-Basis
- Tailwind CSS – Styling
- FullCalendar – Kalender-UI
- Uppy – Upload-Handling (resumable, Drag&Drop)

Backend
- Next.js API Route Handlers (REST) – Geschäftslogik
- Supabase Auth – Anmeldung (E-Mail/OAuth)
- Postgres (Supabase) – Datenbank + Row Level Security
- Supabase Storage – Dateispeicher (private Buckets)
- Postgres FTS – Suche in Dateimetadaten/Tags

Kernfunktionen (technisch)
- Filesharing (Upload, Download, Quotas, Versionen, Shares)
- Gruppen & Rollen (owner, mod, member)
- Kalender (CRUD, Teilnehmer, ICS-Export)
- Sicherheit (RLS, Audit, DSGVO-konform)

Infrastruktur
- Containerisierung (Docker/Compose) auf VMs
- Caddy/Nginx – Reverse Proxy + SSL

## Zusätzliche Notizen (aus der Diskussion)

- Funktionsfähiger „Zurück“-Button (UX-Detail)
- Rechte & Rollen: Admins, Kurse etc.
- Dozentenbewertung mit Formular
- Accounts per Matrikelnummer (so weit wie möglich), sicherer Umgang mit Daten

## Zusammenarbeit & Workflow

- Keine Force-Pushes auf den Haupt-Branch
- Änderungen via Pull/Merge Requests; MRs referenzieren zugehörige Issues
- Branch-Strategie: feature/<kurzbeschreibung>, fix/<kurzbeschreibung>
- Code Reviews verpflichtend vor Merge

## Weitere Inhalte und Links

- Projektmanagement: [../projektmanagement/README.md](../projektmanagement/README.md)
- Dokumentation & Entwürfe: [../dokumentation/README.md](../dokumentation/README.md)
- Präsentationsfähiger Entwurf: siehe Dokumentation (wird dort gesammelt)

---

Wenn ihr Ergänzungen oder neue Ideen habt, öffnet bitte ein Issue oder erstellt direkt einen PR mit kurzer Begründung und Verweis auf den Kontext (Workshop/Feedback etc.).

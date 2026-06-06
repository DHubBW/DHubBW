## Was wurde gemacht?

<!-- Kurze Beschreibung der Änderungen und des Kontexts. -->

Schließt #<!-- Issue-Nummer -->

---

## Pflicht-DoD-Checkliste (Author)

_Alle Punkte müssen erfüllt sein, bevor der MR in Review geht._

- [ ] Feature vollständig umgesetzt (alle Akzeptanzkriterien aus dem Issue)
- [ ] Expo Router-Routing korrekt eingebunden (`app/`-Verzeichnis)
- [ ] Theming verwendet (`@/constants/theme`, kein Hardcoded-Color)
- [ ] ESLint + Prettier grün (`npm run lint`)
- [ ] TypeScript strict-mode: keine `any`-Typen, kein `@ts-ignore`
- [ ] Unit-Tests vorhanden: mind. 1 Happy-Path + 1 Error-Case pro Sub-Feature
- [ ] TypeDoc-Kommentare an allen exportierten Funktionen und Komponenten
- [ ] Keine PII (personenbezogene Daten) in Logs oder Testdaten (DSGVO)
- [ ] Issue verknüpft (`Closes #...`) und Label gesetzt
- [ ] Branch wird nach Merge gelöscht

---

## Reviewer-Checkliste

_Bitte jeden Punkt aktiv prüfen – nicht nur überfliegen._

### Funktionalität & Plattform

- [ ] **Cross-Platform-QA:** Feature auf Web, iOS und Android manuell durchgespielt?
- [ ] **Network-Error-Pfad:** UI verhält sich korrekt bei fehlgeschlagenen API-Calls (z. B. Netzwerk weg, Timeout)?
- [ ] **401-Pfad:** Abgelaufener/ungültiger Token wird korrekt behandelt (Redirect zu Login oder Toast)?
- [ ] **403-Pfad:** Bei rollenabhängigen Endpoints – fehlende Berechtigung sauber abgefangen?
- [ ] **Loading-/Empty-/Error-States:** Alle drei Zustände für datengetriebene Screens implementiert und sichtbar?

### Code-Qualität

- [ ] **Unit-Tests:** Mind. 1 Happy-Path + 1 Error-Case pro neuem Sub-Feature vorhanden und grün?
- [ ] **TypeDoc:** Exportierte Funktionen und Komponenten dokumentiert?
- [ ] **DSGVO/Privacy:** Keine PII in Logs, Konsole oder Testdaten?
- [ ] **Code-Style:** ESLint + Prettier ohne Fehler (`npm run lint`)?

### Architektur

- [ ] Schichtentrennung eingehalten (`app/` nur Routing, Logik in `hooks/`, UI in `components/`)?
- [ ] Keine direkten API-Calls in Komponenten (über Custom Hooks gekapselt)?
- [ ] Theming konsistent – kein Hardcoded-Farbwert außerhalb von `constants/theme`?

---

## Screenshots / Screen-Recording

<!-- Web + mindestens eine native Plattform -->

| Screen | Web | iOS / Android |
|--------|-----|---------------|
| Normal-State | | |
| Error-State | | |
| Empty-State | | |

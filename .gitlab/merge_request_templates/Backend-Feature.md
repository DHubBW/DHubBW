## Was wurde gemacht?

<!-- Kurze Beschreibung der Änderungen und des Kontexts. -->

Schließt #<!-- Issue-Nummer -->

---

## Pflicht-DoD-Checkliste (Author)

_Alle Punkte müssen erfüllt sein, bevor der MR in Review geht._

- [ ] Feature vollständig umgesetzt (alle Akzeptanzkriterien aus dem Issue)
- [ ] Architektur eingehalten: Trennung `application/` (Controller) und `infrastructure/` (Business-Logik)
- [ ] Keine Business-Logik im Controller
- [ ] HTTP-Methoden und Statuscodes semantisch korrekt (`201 Created`, `204 No Content`, …)
- [ ] Checkstyle grün (`mvn checkstyle:check`)
- [ ] Alle Tests grün (`mvn test`)
- [ ] Unit-Tests vorhanden: mind. Positiv-Fall + Fehlerfall (400/404) pro Endpunkt
- [ ] Integrationstests für neue Controller-Endpunkte vorhanden (`@SpringBootTest` + MockMvc)
- [ ] JavaDocs an allen `public` Klassen und Methoden
- [ ] Neues Package hat `package-info.java`
- [ ] Keine PII (personenbezogene Daten) in Logs oder Testdaten (DSGVO)
- [ ] Issue verknüpft (`Closes #...`) und Label gesetzt
- [ ] Branch wird nach Merge gelöscht

---

## Reviewer-Checkliste

_Bitte jeden Punkt aktiv prüfen – nicht nur überfliegen._

### Korrektheit & Sicherheit

- [ ] **Rollen-Autorisierung:** `@PreAuthorize`-Annotations an allen Endpunkten korrekt gesetzt?
- [ ] **Eingabevalidierung:** Findet am Controller statt, nicht in der Service-Schicht?
- [ ] **Fehlerbehandlung:** Exceptions werden mit `ColorLogger.colorError()` geloggt und sauber geworfen?
- [ ] **Statuscodes:** Entsprechen die HTTP-Antworten dem semantischen Standard (kein 200 für Erstellungen etc.)?

### Tests

- [ ] **Positiv-Fall:** Mindestens ein Test pro Endpunkt mit gültigem Input und erwartetem Ergebnis?
- [ ] **Fehlerfall:** 400 (Bad Request) und 404 (Not Found) abgedeckt?
- [ ] **Integrationstests:** Laufen gegen echte Spring-Security-Konfiguration (kein vollständiges Mocking)?
- [ ] **Alle Tests grün** (`mvn test`)?

### Code-Qualität

- [ ] **Checkstyle:** `mvn checkstyle:check` ohne Fehler?
- [ ] **JavaDoc:** Alle `public` Methoden und Klassen dokumentiert (Zweck, Parameter, Rückgabewert)?
- [ ] **Naming:** Methoden-, Klassen- und Variablennamen fachlich eindeutig und konsistent?
- [ ] **Keine ungenutzten Imports**, keine leeren Catch-Blöcke, keine Magic Numbers?

### Architektur

- [ ] Schichtentrennung eingehalten (`application/` vs. `infrastructure/`)?
- [ ] DTOs nur für Datentransfer – keine Business-Logik in DTOs oder Entities?
- [ ] Transaktionssteuerung korrekt (`@Transactional` in Service, nicht im Controller)?

---

## API-Änderungen

<!-- Neue oder geänderte Endpunkte hier kurz dokumentieren -->
<!-- (Pfad, Methode, Beschreibung, Request/Response-Format) -->

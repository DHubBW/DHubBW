# Tutorial für das Deployen der Spring Boot Applikation
Das folgende Tutorial gibt
Unsere Spring Boot Web App kann über maven lokal gestartet werden:
## Lokale Voraussetzungen
Um das Projekt deployen zu können werden einige Grundvoraussetzungen benötigt. Im Folgenden sind zu installierende Packages aufgelistet
- Docker
```bash
brew install docker
```
- maven
```bash
brew install maven
```
## Nutzung der VM
### Java/Maven Setup
Wir nutzen den mvnw wrapper um maven zu nutzen, ohne es auf der VM installiert zu haben.
Zunächst benötigt die VM eine valide 
## Build der .jar
```bash
cd ../ &&
mvn clean install &&
mvn clean package
```

## Setup über Docker
Um unsere App in einem Docker Container zu starten, nutzen wir ein [Dockerfile](./Dockerfile).
### Build und Start
```bash
cd .. &&
docker build -f docker/Dockerfile --tag=spring-backend:latest . &&
docker run -d -p 8080:8080 spring-backend:latest
```

Flags:
- `-d`: Container läuft im Hintergrund
- `-p 8080:8080`: Port-Mapping (Container-Port:Host-Port)
- `-e`: Umgebungsvariablen setzen (optional, siehe Konfiguration)

## Konfiguration mit Umgebungsvariablen

Das Dockerfile setzt `SPRING_PROFILES_ACTIVE=dev` als Standard. Beim `docker run` können Profile und Datenbank-Credentials via `-e` überschrieben werden:

```bash
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_URL=jdbc:h2:mem:testdb \
  -e DB_USERNAME=sa \
  -e DB_PASSWORD=password \
  spring-backend:latest
```

Verfügbare Umgebungsvariablen:
- `SPRING_PROFILES_ACTIVE`: Spring-Profil (dev, prod). Default: dev
- `DB_URL`: Datenbank-URL. Default: `jdbc:h2:mem:testdb`
- `DB_USERNAME`: DB-Benutzer. Default: sa
- `DB_PASSWORD`: DB-Passwort. Default: password

### Lokales Testen der Endpunkte
Um die App lokal zu testen, öffne einen API Client deiner Wahl (z.B.: Postman, Bruno, Yaak, ...). Jetzt kannst du den Health Endpoint testen.
Wenn die du die Antwort 200 (ok) erhältst, dann läuft das Backend :D
```bash
curl -X GET 'http://localhost:8080/actuator/health' \
  --header 'User-Agent: yaak' \
  --header 'Accept: */*'
```

### Mock Basic Authentication (Dev-Profil)
Im Dev-Profil ist ein Mock-User hardcoded: `user` / `user`.
Details siehe [DevSecurityConfig](../src/main/java/com/se/dhubbw/shared/infrastructure/config/security/DevSecurityConfig.java).

### Deployen in der VM (ohne Pipeline)
Stelle sicher, dass die neuste Version des Programms als jar Datei vorhanden ist. Anschließend

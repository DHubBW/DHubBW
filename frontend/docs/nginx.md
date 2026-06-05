# nginx-Stage (Frontend Web-Prod-Bundle)

Die Frontend-Container-Stage ist ein zweistufiges Multi-Stage-Build:

1. **`build`-Stage** (`node:22-alpine`) — `npm ci` + `npm run build-web`
   (= `expo export --platform web`). Output: `/app/dist/`.
2. **`runtime`-Stage** (`nginx:alpine`) — kopiert `dist/` nach
   `/usr/share/nginx/html`, ersetzt die Default-`default.conf` durch
   `frontend/nginx.conf` und exposed Port 80.

## `frontend/nginx.conf`-Locations

| Pfad-Match                       | Verhalten                                                                   |
| -------------------------------- | --------------------------------------------------------------------------- |
| `/assets/...`, `/_expo/static/...` | statische Build-Files mit `Cache-Control: public, max-age=31536000, immutable` (Hash im Dateinamen → safe). `/_expo/static/` enthält die JS/CSS-Bundles, `/assets/` die expo-router-Image-Registry. |
| `/api/...`                       | Reverse-Proxy auf `http://backend:8080/`. Docker-Resolver `127.0.0.11` löst `backend` zur Request-Zeit auf (sonst Container-Crash beim Boot, wenn Backend noch nicht läuft). `rewrite ^/api/(.*)$ /$1 break;` strippt das `/api`-Präfix. |
| alles andere                     | `try_files $uri $uri.html $uri/index.html /index.html /+not-found.html =404` — expo-router exportiert pro Route eine eigene `.html` (statt einer einzelnen SPA-`index.html`). Die Fallback-Kette deckt Direct-Navigation (z.B. `/dashboard` → `/dashboard.html`) und unbekannte Routen (→ Hydration zeigt dann Client-side `+not-found`) ab. |

`/api/`-Pfad-Konvention: Backend antwortet auf `/student/...`, `/public/...`,
`/kursadmin/...`, `/systemadmin/...`. nginx strippt `/api`. Frontend ruft also
`/api/student/dashboard/profile`, Backend bekommt `/student/dashboard/profile`.

**Wichtig:** das Repo enthält `app/index.tsx` als Root-Redirect →
`/(app)/dashboard`. Damit produziert `expo export` ein `dist/index.html`,
auf das die SPA-Fallback-Kette zurückfallen kann.

## Lokal-Run

```bash
cd dhubbw/frontend
docker build -t dhubbw-frontend:smoke -f Dockerfile .
docker run -d --name dhubbw-frontend-smoke -p 8081:80 dhubbw-frontend:smoke

# danach: http://localhost:8081/

docker stop dhubbw-frontend-smoke && docker rm dhubbw-frontend-smoke
```

Build-Context ist `frontend/`, **nicht** Repo-Root. Sonst greift
`COPY package*.json ./` und `COPY . .` falsch.

## Smoke-Test (manuell, ohne Backend-Container)

| Route                                            | Erwartet                                                | Begründung                                                  |
| ------------------------------------------------ | ------------------------------------------------------- | ----------------------------------------------------------- |
| `GET /`                                          | 200 + `<div id="root">` im Body                         | Root-Redirect → Dashboard (über expo-router)                |
| `GET /login`                                     | 200 (kommt aus `/login.html`)                           | `.html`-Fallback                                            |
| `GET /dashboard`                                 | 200                                                     | analog                                                      |
| `GET /unknown-xyz`                               | 200 (kommt aus `/index.html` oder `/+not-found.html`)   | SPA-Fallback-Kette                                          |
| `GET /api/public/auth/test`                      | **502 oder 504**                                        | Backend nicht da → Reverse-Proxy timeoutet. **404 = Bug** (Location nicht aktiv) |
| `GET /_expo/static/css/<hash>.css`               | 200 + `Cache-Control: public, max-age=31536000, immutable` | Hashed Bundle                                            |
| `GET /assets/.../<hash>.png`                     | 200 + immutable Cache-Header                            | Asset-Registry                                              |

Beispiel:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/dashboard
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/api/public/auth/test
```

## Compose-Integration (Out-of-Scope für FE-S2)

Das `docker-compose.yml` für End-to-End wird erst durch FE-S1 (#206) und das
finale Ops-Setup gebraucht. Für FE-S2 reicht der reine Image-Smoke ohne
Backend-Erreichbarkeit; der Resolver-Trick im `/api/`-Block stellt sicher, dass
nginx auch ohne erreichbaren Backend-Host startet.

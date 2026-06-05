# Frontend — Puesta en Producción · Documento para el equipo de Frontend

> **Destinatarios:** equipo de Frontend (Angular).
> **Objetivo:** dejar el frontend listo para servirse en un **piloto real**, empaquetado en una imagen Docker, detrás del reverse proxy del backend.
> **Fecha:** 2026-05-25 · **Stack:** Angular 17 · `@stomp/stompjs` · Docker · nginx.

---

## 1. Contexto y arquitectura objetivo

En producción **no** se usa `ng serve`. El SPA se compila a estáticos y se sirve desde una imagen `nginx`. Delante de todo hay un reverse proxy **Caddy** (lo provee el equipo de backend) que:

- termina TLS (HTTPS),
- enruta `/api/*` y `/ws/*` → backend (Spring Boot),
- enruta **todo lo demás** → el contenedor del frontend (este documento).

```
Navegador ──HTTPS──▶ Caddy ──┬── /api/*  ─▶ backend:8080
                             ├── /ws/*   ─▶ backend:8080  (WebSocket)
                             └── /*       ─▶ frontend:80   (este SPA)
```

**Implicaciones clave para el frontend:**
- El frontend se comunica con la API por **mismo origen** y ruta **relativa** `/api` (ya configurado en `environment.ts`). No hay CORS en prod porque todo sale del mismo dominio.
- El contenedor nginx del frontend **solo sirve estáticos con fallback de SPA**. **No** necesita hacer `proxy_pass` de `/api` ni de `/ws`: de eso se encarga Caddy.

---

## 2. Lo que el equipo de frontend debe entregar

1. Corregir el **bug de la URL del WebSocket en producción** (Tarea A — bloqueante).
2. Confirmar la configuración de `environment.ts` de producción (Tarea B).
3. Crear el **Dockerfile de producción** + `nginx.conf` + `.dockerignore` (Tarea C).
4. Asegurar que `npm run build` pasa los budgets de producción (Tarea D).

**Entregable final:** una imagen Docker que sirve el SPA y un servicio `frontend` añadido al `docker-compose.prod.yml`, probado en local.

---

## Tarea A — 🔴 Corregir la URL del WebSocket en producción (BLOQUEANTE)

**Archivo:** `frontend/src/app/core/services/websocket.service.ts`

**Problema:** `getWsUrl()` deriva la URL del WebSocket reemplazando `http`→`ws` sobre `apiBaseUrl`:

```ts
private getWsUrl(): string {
  const apiBase = environment.apiBaseUrl;          // dev: 'http://localhost:8080/api'  | prod: '/api'
  const httpBase = apiBase.replace(/\/api$/, '');   // dev: 'http://localhost:8080'      | prod: ''
  return httpBase.replace(/^http/, 'ws') + '/ws';   // dev: 'ws://localhost:8080/ws'     | prod: '/ws'  ❌
}
```

En **producción** `apiBaseUrl` es `'/api'` (relativo), así que el resultado es `'/ws'` — una ruta relativa. Pero `@stomp/stompjs` exige un `brokerURL` **absoluto** (`ws://` o `wss://`); con `'/ws'` la conexión **no se establece** y se rompe todo el tiempo real (mesas, comandas, notificaciones).

Además, sobre HTTPS hay que usar `wss://` (no `ws://`), o el navegador bloquea la conexión por *mixed content*.

**Solución:** cuando `apiBaseUrl` sea relativo, construir la URL absoluta a partir de `window.location`, eligiendo `wss:` si la página es `https:`.

**Reemplazar** el método `getWsUrl()` por:

```ts
private getWsUrl(): string {
  const apiBase = environment.apiBaseUrl;        // dev absoluto | prod relativo ('/api')
  const httpBase = apiBase.replace(/\/api$/, ''); // quita el sufijo '/api'

  // apiBaseUrl absoluto (dev): convertir el esquema http(s) -> ws(s).
  if (/^https?:\/\//.test(httpBase)) {
    return httpBase.replace(/^http/, 'ws') + '/ws';
  }

  // apiBaseUrl relativo (prod): construir desde el origen actual.
  // https -> wss (obligatorio sobre TLS); http -> ws.
  const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${scheme}://${window.location.host}${httpBase}/ws`;
}
```

**Verificación manual:**
- **Dev** (`npm start`): la URL sigue siendo `ws://localhost:8080/ws`. Las suscripciones (mesas, comandas) funcionan igual que ahora.
- **Prod simulada:** servir el `dist` por HTTP en un puerto y confirmar en la consola del navegador que el cliente STOMP conecta a `ws://<host>/ws` (y a `wss://...` cuando hay TLS), sin errores de *Invalid URL* ni *mixed content*.

**Commit sugerido:**
```
fix(websocket): construir brokerURL absoluto (wss) cuando apiBaseUrl es relativo en prod
```

---

## Tarea B — Confirmar `environment.ts` de producción

**Archivo:** `frontend/src/environments/environment.ts`

Verificar que el contenido sea exactamente:

```ts
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  useMockApi: false
};
```

- `apiBaseUrl: '/api'` → mismo origen, lo enruta Caddy. **No** poner `http://localhost:8080`.
- `useMockApi: false` → usa la API real, no los mocks.

`angular.json` ya tiene `defaultConfiguration: "production"` en `build`, por lo que `npm run build` usa este archivo (en `development` se reemplaza por `environment.development.ts` vía `fileReplacements`). No hay cambios que hacer salvo confirmar los valores.

---

## Tarea C — Dockerfile de producción + nginx + .dockerignore

**Archivos a crear:**
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `frontend/.dockerignore`

> ⚠️ **Ruta de salida del build:** Angular 17 usa el builder `@angular-devkit/build-angular:application`, que emite en `dist/al-toro-gastrobar-frontend/**browser**/` (subcarpeta `browser`). El `COPY` del Dockerfile debe apuntar ahí.

### `frontend/Dockerfile`

```dockerfile
# ============================================================
# STAGE 1: BUILD
# ============================================================
FROM node:20-alpine AS builder
WORKDIR /app

# Cachear dependencias primero
COPY package.json package-lock.json ./
RUN npm ci

# Compilar el SPA en modo producción
COPY . .
RUN npm run build

# ============================================================
# STAGE 2: RUNTIME (nginx sirviendo estáticos)
# ============================================================
FROM nginx:1.27-alpine

# Config con fallback de SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar SOLO los estáticos del build (builder 'application' => subcarpeta browser)
COPY --from=builder /app/dist/al-toro-gastrobar-frontend/browser /usr/share/nginx/html

EXPOSE 80
```

### `frontend/nginx.conf`

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # Compresión de texto
    gzip on;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;
    gzip_min_length 1024;

    # Cache largo para assets con hash (outputHashing: all)
    location ~* \.(?:js|css|woff2?|ttf|eot|svg|png|jpg|jpeg|gif|ico)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        try_files $uri =404;
    }

    # Fallback de SPA: cualquier ruta no encontrada sirve index.html
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

> No incluir `proxy_pass` de `/api` ni `/ws` aquí: lo hace Caddy. Si el equipo prefiriera que nginx también proxee la API (despliegue sin Caddy), avisar al equipo de backend para coordinar — **no es la arquitectura acordada**.

### `frontend/.dockerignore`

```
node_modules
dist
.angular
.git
*.log
```

**Verificación local (sin Caddy):**
```bash
cd frontend
docker build -t altoro-frontend .
docker run --rm -p 8088:80 altoro-frontend
# Abrir http://localhost:8088  -> debe cargar el SPA
# Navegar a una ruta profunda (ej. /reservas) y recargar (F5):
#   debe seguir cargando (fallback de SPA), NO dar 404.
```
> Nota: las llamadas a `/api` fallarán en esta prueba aislada (no hay backend ni Caddi delante); eso es esperado. El objetivo aquí es validar que los estáticos y el fallback de SPA funcionan.

**Commit sugerido:**
```
feat(docker): imagen de produccion del frontend (nginx + fallback SPA)
```

---

## Tarea D — Validar los budgets de producción

**Contexto:** `angular.json` define budgets en la configuración `production`:
- `initial`: warning a 1 MB, **error a 2 MB**.
- `anyComponentStyle`: warning 4 KB, **error 12 KB**.

Si el bundle supera el límite, `npm run build` **falla**. Antes de entregar:

```bash
cd frontend
npm run build
```
Expected: build sin errores de budget. Si falla:
- revisar imports pesados / librerías no usadas,
- o ajustar el budget en `angular.json` (coordinar el cambio, no subirlo a ciegas).

---

## Tarea E — Añadir el servicio `frontend` a `docker-compose.prod.yml`

**Archivo:** `docker-compose.prod.yml` (raíz del repositorio, lo edita backend/infra)

El Caddyfile ya tiene la regla `handle { reverse_proxy frontend:80 }` — pero el servicio `frontend` aún **no existe** en `docker-compose.prod.yml`. Sin él, Caddy no puede alcanzar el contenedor y toda ruta que no sea `/api`, `/ws`, `/images` o `/uploads` dará `502 Bad Gateway`.

**Añadir** al final de la sección `services:` en `docker-compose.prod.yml`:

```yaml
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: altoro_frontend
    mem_limit: 64m
    restart: unless-stopped
    networks:
      - altoro_network
    depends_on:
      api:
        condition: service_healthy
    # Sin 'ports': solo Caddy expone al exterior; lo alcanza por la red interna.
```

**Prerequisito:** Tarea C (Dockerfile del frontend) debe estar completada antes de añadir este servicio.

**Verificación:**
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml config --services
# Debe listar: postgres, rabbitmq, api, db-backup, caddy, frontend
```

**Commit sugerido:**
```
feat(infra): añadir servicio frontend al stack de producción
```

---

## 3. Integración con el `docker-compose.prod.yml` (coordinar con backend)

El Caddyfile de producción (`Caddyfile` en la raíz) ya tiene el bloque de enrutamiento:

```
handle {
    reverse_proxy frontend:80
}
```

Caddy enrutará todo lo que no sea `/api`, `/ws`, `/images` ni `/uploads` hacia `frontend:80`. El frontend **no** expone puertos directamente. La Tarea E formaliza el servicio en el compose file.

---

## 4. Checklist de entrega

- [ ] **Tarea A** — `getWsUrl()` corregido; WS conecta con `wss://` en HTTPS y `ws://` en dev. Tiempo real verificado.
- [x] **Tarea B** — `environment.ts` con `apiBaseUrl: '/api'` y `useMockApi: false`. ✅ Verificado 2026-06-05.
- [ ] **Tarea C** — `Dockerfile`, `nginx.conf`, `.dockerignore` creados; imagen construye y sirve el SPA con fallback.
- [ ] **Tarea D** — `npm run build` pasa los budgets de producción.
- [ ] **Tarea E** — Servicio `frontend` añadido a `docker-compose.prod.yml` (ver sección 3).
- [x] **Repo** — `package-lock.json` commiteado (lo necesita `npm ci` en CI y en el Dockerfile). ✅ Verificado 2026-06-05.

## 5. Qué NO debe hacer el frontend (lo cubre backend/infra)

- TLS / certificados (Caddy).
- Proxy de `/api` y `/ws` (Caddy).
- CORS (en prod es mismo origen; no aplica).
- Dominio / DNS / hosting.

---

## Resumen para el equipo

El cambio crítico es la **Tarea A** (sin ella, el tiempo real no funciona en producción). El resto es empaquetado estándar: compilar el SPA y servirlo con nginx + fallback de SPA, dentro de una imagen Docker que se integra detrás de Caddy. Todo es probable en local antes de que exista dominio o servidor.

# Deployment Guide — Al Toro Gastrobar

Infraestructura de producción basada en Docker Compose con Caddy como reverse proxy y TLS automático via Let's Encrypt.

---

## Tabla de contenidos

- [Stack de producción](#stack-de-producción)
- [Archivos de configuración](#archivos-de-configuración)
- [Variables de entorno requeridas](#variables-de-entorno-requeridas)
- [Routing (Caddyfile)](#routing-caddyfile)
- [Límites de memoria](#límites-de-memoria)
- [Volúmenes](#volúmenes)
- [Backups automáticos](#backups-automáticos)
- [Despliegue inicial](#despliegue-inicial)
- [Actualización (rolling deploy)](#actualización-rolling-deploy)
- [Comandos útiles](#comandos-útiles)
- [Flyway en producción](#flyway-en-producción)

---

## Stack de producción

| Servicio | Imagen | Puerto expuesto | Función |
|----------|--------|-----------------|---------|
| `api` | build local (`backend/Dockerfile`) | interno | Spring Boot 3.5 (API REST + WebSocket) |
| `postgres` | `postgres:15-alpine` | interno | Base de datos principal |
| `rabbitmq` | `rabbitmq:3.13-management-alpine` | interno | Broker de mensajes |
| `caddy` | `caddy:2-alpine` | 80, 443 | Reverse proxy + TLS |
| `db-backup` | `postgres-backup-local:15-alpine` | — | Backups automáticos diarios |

PostgreSQL y RabbitMQ no exponen puertos al exterior — solo son accesibles dentro de la red Docker `altoro_network`.

---

## Archivos de configuración

| Archivo | Propósito |
|---------|-----------|
| `docker-compose.yml` | Servicios base compartidos (dev + prod) |
| `docker-compose.prod.yml` | Overrides de producción: profiles, límites de memoria, Caddy, backup |
| `.env.prod` | Variables secretas de producción (no se commitea; usar `.env.prod.example` como plantilla) |
| `Caddyfile` | Routing HTTP/HTTPS y proxy inverso |

---

## Variables de entorno requeridas

Copiar `.env.prod.example` → `.env.prod` y completar todos los valores:

| Variable | Descripción |
|----------|-------------|
| `POSTGRES_DB` | Nombre de base de datos |
| `POSTGRES_USER` | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL |
| `RABBITMQ_USERNAME` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | Contraseña de RabbitMQ |
| `JWT_SECRET` | Secret para firmar tokens JWT |
| `JWT_EXPIRATION` | Expiración del access token en ms (default: 1800000 = 30 min) |
| `MAIL_HOST` | Servidor SMTP |
| `MAIL_PORT` | Puerto SMTP (default: 587) |
| `MAIL_USERNAME` | Usuario SMTP |
| `MAIL_PASSWORD` | Contraseña SMTP |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos separados por coma |
| `BOOTSTRAP_ADMIN_EMAIL` | Email del administrador inicial |
| `BOOTSTRAP_ADMIN_PASSWORD` | Contraseña del administrador inicial |
| `SITE_ADDRESS` | Dominio para Caddy |
| `SPRING_PROFILES_ACTIVE` | Perfil activo: `dev` o `prod` (inyectado por docker-compose) |

---

## Routing (Caddyfile)

Caddy reenvía las peticiones al servicio `api` según el path:

| Path | Destino | Notas |
|------|---------|-------|
| `/api/*` | `api:8080` | API REST |
| `/ws/*` | `api:8080` | WebSocket STOMP (Caddy reenvía `Upgrade` automáticamente) |
| `/images/*` | `api:8080` | Imágenes del catálogo base (seed, servidas por Spring Boot) |
| `/uploads/*` | `api:8080` | Imágenes subidas por ADMIN (volumen `altoro_uploads`) |
| `*` | `frontend:80` | SPA Angular (servicio separado del equipo frontend) |

Con `SITE_ADDRESS=altoro.tudominio.com`, Caddy gestiona TLS automáticamente via Let's Encrypt. Con `SITE_ADDRESS=:80`, corre sin TLS (para pruebas locales).

---

## Límites de memoria

| Servicio | Límite |
|----------|--------|
| `api` | 1536 MB |
| `postgres` | 1024 MB |
| `rabbitmq` | 512 MB |
| `caddy` | 128 MB |
| `db-backup` | 64 MB |

La JVM en el contenedor `api` está configurada con cap en el Dockerfile (`-XX:MaxRAMPercentage`).

---

## Volúmenes

| Volumen | Contenido |
|---------|-----------|
| `postgres_data` | Datos de PostgreSQL |
| `rabbitmq_data` | Datos de RabbitMQ |
| `altoro_uploads` | Imágenes subidas por el ADMIN (montado en `/opt/altoro/uploads`) |
| `caddy_data` | Certificados TLS de Let's Encrypt |
| `caddy_config` | Configuración interna de Caddy |
| `./backups` | Backups de PostgreSQL (directorio local del host) |

---

## Backups automáticos

El servicio `db-backup` ejecuta un dump de PostgreSQL con retención configurable:

- Diario: últimos 7 días se guarda en `backups/daily/`
- Semanal: últimas 4 semanas se guarda en `backups/weekly/`
- Mensual: últimos 6 meses se guarda en `backups/monthly/`

Los archivos de backup se guardan en `./backups/` en el host (directorio en `.gitignore`). `TZ: America/Bogota` asegura que el schedule aplique en la zona horaria correcta.

### Listar backups disponibles

```bash
ls -lh backups/daily/
```

### Restaurar (detener la app primero)

```bash
docker compose stop api
gunzip -c backups/daily/<archivo>.sql.gz | \
  docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
docker compose start api
```

### Verificar

```bash
docker compose exec postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c \
  "SELECT count(*) FROM restaurante.usuario; SELECT count(*) FROM restaurante.reserva; SELECT count(*) FROM restaurante.venta;"
```
### Forzar

```bash
docker compose exec db-backup /backup.sh
```

---

## Despliegue inicial

```bash
# 1. Clonar el repositorio en el VPS
git clone <repo-url> && cd al_toro_gastrobar

# 2. Crear el archivo de secrets
cp .env.prod.example .env.prod

# 3. Levantar los servicios
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build

# 4. Verificar estado
docker compose ps
docker compose logs -f api
```

En el primer arranque, Flyway aplica `V1__init_schema.sql` y `V2__seed_data.sql`. Si `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` están definidas, se crea el primer usuario ADMIN automáticamente.

---

## Actualización (rolling deploy)

```bash
# 1. Traer cambios del repositorio
git pull origin main

# 2. Reconstruir y redeployar solo el servicio api
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build api

# 3. Verificar arranque
docker compose logs -f api
```

---

## Comandos útiles

```bash
# Logs en tiempo real
docker compose logs -f api

# Estado de los servicios
docker compose ps

# Detener sin borrar datos
docker compose down

# Reset completo (BORRA todos los datos)
docker compose down -v

# Acceder a PostgreSQL
docker exec -it altoro_postgres psql -U altoro_prod -d altoro_db
```

---

## Flyway en producción

Flyway aplica automáticamente las migraciones pendientes al arrancar la API. El perfil `prod` usa `spring.jpa.hibernate.ddl-auto=none` — Flyway es el único que modifica el schema.

V3, V4, V5 no se aplican en producción (solo en dev/test). Ver `docs/reference/database.md` para la estrategia completa.

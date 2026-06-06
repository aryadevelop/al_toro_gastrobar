# Al Toro Gastrobar

---

## Tabla de contenidos

- [Descripción](#descripción)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Módulos del sistema](#módulos-del-sistema)
- [Roles del sistema](#roles-del-sistema)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Puesta en marcha](#puesta-en-marcha)
- [Variables de entorno](#variables-de-entorno)
- [Perfiles de Spring Boot](#perfiles-de-spring-boot)
- [Pruebas de API](#pruebas-de-api)
- [Flujo de trabajo Git](#flujo-de-trabajo-git)
- [Equipo](#equipo)

---

## Descripción

Al Toro Gastrobar requiere una solución tecnológica que integre en tiempo real los cuatro nodos operativos del restaurante: **servicio** (meseros), **producción** (cocina y barra), **caja** (cajeros) y **portal del cliente**. El sistema elimina la gestión manual en papel, reduce errores en la transmisión de pedidos y centraliza la información operativa y administrativa.

---

## Tecnologías

| Capa | Tecnología | Versión |
|------|------------|---------|
| Frontend | Angular (TypeScript) | 17.3.x |
| Backend | Java + Spring Boot | 21 / 3.5.13 |
| Base de datos | PostgreSQL | 15 |
| Mensajería | RabbitMQ | 3.13 |
| Infraestructura | Docker + Docker Compose | 24+ / 2.20+ |
| Control de versiones | Git + GitHub | — |
| Gestión de tareas | Jira | — |

---

## Arquitectura

El sistema sigue una arquitectura cliente-servidor desacoplada. El frontend Angular consume la API REST del backend Spring Boot mediante HTTP/JSON. El backend utiliza RabbitMQ para comunicación asíncrona y WebSocket (STOMP) para actualizaciones en tiempo real. En producción, Caddy actúa como reverse proxy y sirve el frontend compilado.

```text
Cliente (navegador)
  │
  ├── HTTP/JSON ──────────────▶ Caddy (reverse proxy, prod)
  │                                  │
  │                                  ├── /api/* ──▶ Backend (Spring Boot :8080)
  │                                  │                  │
  │                                  │                  ├──▶ PostgreSQL
  │                                  │                  └──▶ RabbitMQ
  │                                  │
  │                                  └── /* ────▶ Frontend compilado (Angular)
  │
  └── WebSocket/STOMP ──────────▶ Backend (Spring Boot :8080)
```

---

## Módulos del sistema

| Épica | Módulo | Actores principales |
|-------|--------|---------------------|
| HE-01 | Autenticación y perfiles | Todos los roles |
| HE-02 | Reservas y consumo | Cliente |
| HE-03 | Mesas, comandas y consulta de reservas | Mesero, Cajero |
| HE-04 | Producción e inventario | Cocinero, Bartender, Administrador |
| HE-05 | Pagos y caja | Cajero |
| HE-06 | Histórico y reportes | Administrador |
| HE-07 | Inventario y decoraciones | Administrador |
| HE-08 | Personal y clientes | Administrador |

---

## Roles del sistema

| Rol | Descripción de acceso |
|-----|-----------------------|
| CLIENTE | Reservas propias, pre-orden, historial de visitas y puntos de fidelización |
| MESERO | Consulta de reservas activas, mesas asignadas, comandas y notificaciones en tiempo real |
| COCINERO | Comandas de la estación cocina |
| BARTENDER | Comandas de la estación barra |
| CAJERO | Reservas, pagos, cierre de venta y mapa de mesas |
| ADMIN | Acceso total: configuración, reportes, inventario, personal y consulta de reservas |

---

## Estructura del repositorio

```text
al_toro_gastrobar/
├── frontend/                    # Aplicación Angular (no dockerizada)
├── backend/                     # API REST Spring Boot
│   ├── src/
│   │   ├── main/java/.../modules/   # Módulos de negocio
│   │   ├── main/resources/
│   │   │   ├── db/migration/        # Migraciones Flyway (V1, V2)
│   │   │   └── db/migration-dev/    # Datos de desarrollo (V3, excluido en prod)
│   │   └── test/
│   ├── postman/                 # Colecciones de pruebas de API
│   └── Dockerfile               # Imagen de producción (multi-stage, JRE 21)
├── docs/                        # Documentación del proyecto
│   ├── backend/                 # Convenciones y referencia técnica
│   ├── ops/                     # Guías operativas (despliegue, infraestructura)
│   └── superpowers/             # Planes de implementación
├── .github/
│   └── workflows/
│       ├── validar-rama.yml     # Valida nombres de rama en PRs
│       └── ci.yml               # Pipeline de integración continua
├── Caddyfile                    # Configuración del reverse proxy (producción)
├── docker-compose.yml           # Orquestación base (api, postgres, rabbitmq)
├── docker-compose.override.yml  # Overrides de desarrollo (puertos expuestos)
├── docker-compose.prod.yml      # Overrides de producción (caddy, db-backup, límites de memoria)
├── .env.prod.example            # Plantilla de variables de producción
├── .gitignore
├── README.md
└── CONTRIBUTING.md              # Reglas de Git y flujo de trabajo
```

---

## Puesta en marcha

### Requisitos

| Herramienta | Versión mínima |
|-------------|----------------|
| Docker | 24+ |
| Docker Compose | 2.20+ |
| Node.js (solo frontend local) | 18+ |
| Java (solo backend sin Docker) | 21 |
| Maven (solo backend sin Docker) | 3.9 |

### Backend e infraestructura (Docker)

Todos los comandos se ejecutan desde la **raíz del proyecto**.

#### Desarrollo

```bash
docker compose up --build
```

Docker fusiona `docker-compose.yml` + `docker-compose.override.yml` automáticamente.

| Servicio | URL |
|----------|-----|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ consola | http://localhost:15672 (guest / guest) |
| PostgreSQL | localhost:5432 (altoro / altoro123) |

#### Producción

```bash
# 1. Crear el archivo de secrets a partir de la plantilla
cp .env.prod.example .env.prod

# 2. Completar todos los valores en .env.prod

# 3. Levantar los servicios
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up --build -d
```

En producción: Postgres y RabbitMQ no exponen puertos al exterior. Caddy expone los puertos 80 y 443 y actúa como reverse proxy hacia la API. Swagger UI está deshabilitado. El servicio `db-backup` ejecuta backups diarios automáticos con retención de 7 días / 4 semanas / 6 meses.

#### Comandos útiles

```bash
docker compose logs -f api        # Logs de la API en tiempo real
docker compose up --build         # Reconstruir imagen tras cambios
docker compose down               # Detener (conserva volúmenes)
docker compose down -v            # Detener y borrar volúmenes (borra la BD)
```

### Frontend

```bash
cd frontend
npm install
npm start
```

La app queda disponible en http://localhost:4200. El frontend no está dockerizado; en producción se compila con `npm run build` y el artefacto es servido por Caddy.

---

## Variables de entorno

Basadas en `.env.prod.example`. Los valores de dev se definen como defaults en `docker-compose.override.yml`.

| Variable | Descripción |
|----------|-------------|
| `POSTGRES_DB` | Nombre de la base de datos |
| `POSTGRES_USER` | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL |
| `RABBITMQ_USERNAME` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | Contraseña de RabbitMQ |
| `JWT_SECRET` | Secret para firmar tokens JWT (mín. 32 chars; generar con `openssl rand -base64 48`) |
| `JWT_EXPIRATION` | Expiración del access token en ms (default: 1800000 = 30 min) |
| `MAIL_HOST` | Servidor SMTP |
| `MAIL_PORT` | Puerto SMTP (default: 587) |
| `MAIL_USERNAME` | Usuario SMTP |
| `MAIL_PASSWORD` | Contraseña SMTP |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos separados por coma (ej: `https://altoro.com`) |
| `BOOTSTRAP_ADMIN_EMAIL` | Email del administrador inicial (bootstrap automático en prod si no existe ADMIN) |
| `BOOTSTRAP_ADMIN_PASSWORD` | Contraseña del administrador inicial (cambiar tras el primer login) |
| `SITE_ADDRESS` | Dominio para Caddy (`":80"` en local, dominio real en prod) |
| `SPRING_PROFILES_ACTIVE` | Perfil activo: `dev` o `prod` (inyectado por docker-compose) |

---

## Perfiles de Spring Boot

| Característica | `dev` | `prod` |
|----------------|-------|--------|
| Migraciones Flyway | `migration/` + `migration-dev/` | `migration/` únicamente |
| Logs SQL | sí | no |
| Swagger UI | habilitado (`/swagger-ui.html`) | deshabilitado |
| Nivel de log root | INFO/DEBUG | WARN |
| Mensajes de error | detallados | ocultos |
| Correo | Mailtrap (intercepta envíos) | SMTP real |
| WebSocket | habilitado | habilitado |
| Actuator endpoints expuestos | `/actuator/health` | `/actuator/health` |

---

## Pruebas de API

Las colecciones de pruebas están organizadas en `backend/postman/postman/` usando el formato YAML de **Postman for VS Code**.

```text
backend/postman/postman/
├── environments/
│   └── Al Toro – Local.environment.yaml     # Variables de entorno (baseUrl, credenciales)
└── collections/
    ├── 01_auth/                              # Autenticación
    ├── 02_reservas/                          # Gestión de reservas
    ├── 03_mesas/                             # Mesas y asistencia
    ├── 04_comandas_produccion/               # Comandas de producción
    ├── inventario/                           # Insumos y productos
    ├── notificaciones/                       # Atención de notificaciones
    ├── usuarios/                             # Clientes y puntos
    ├── ventas/                               # Pagos y cierre de venta
    ├── visitas/                              # Visitas activas
    └── e2e/                                  # Flujos end-to-end
```

### Características de los tests

- Autónomos: cada test incluye script `beforeRequest` con login propio — no dependen del orden de la colección
- Independientes: ejecutables en cualquier orden
- Auto-cleanup: los tests que crean datos los eliminan automáticamente en `afterResponse`
- Aserciones completas: validan estructura, estado HTTP, campos obligatorios y reglas de negocio

### Desde Postman for VS Code

1. Instalar la extensión **Postman** en VS Code
2. Abrir la carpeta `backend/postman/postman/`
3. Seleccionar el environment **Al Toro – Local** en la barra lateral
4. Ejecutar tests individuales o colecciones completas desde la UI

### Desde la app de Postman

Los archivos YAML son compatibles con Postman Desktop:

1. Importar `environments/Al Toro – Local.environment.yaml`
2. Importar las carpetas de `collections/` (arrastrándolas a Postman)
3. Seleccionar el environment **Al Toro – Local**
4. Ejecutar tests individuales o runner de colección

Nota: algunos tests requieren datos de seed específicos. Ejecutar `./mvnw flyway:clean flyway:migrate` desde `backend/` para resetear la BD a su estado inicial si los tests fallan por datos inconsistentes.

---

## Flujo de trabajo Git

Todas las reglas operativas de Git —nombres de ramas, formato de commits, protección de ramas y procedimiento para Pull Requests— están documentadas en:
[`CONTRIBUTING.md`](./CONTRIBUTING.md)

Antes de realizar el primer commit, cada miembro del equipo debe leer y seguir ese documento.

---

## Equipo

| Nombre | Rol en Scrum |
|--------|--------------|
| Paula Andrea Muñoz Delgado | Scrum Master · Desarrollador · Analista · Tester |
| Adrián Camilo Bergaño Ortega | Desarrollador |
| Yeixón Julián Gembuel Ciclos | Tester |
| Rubeiro Romero | Desarrollador · Analista |

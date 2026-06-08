# Al Toro Gastro-Bar

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
- [Documentación técnica del backend](#documentación-técnica-del-backend)
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
| Infraestructura | Docker + Docker Compose | 24+ / 2.20+ |
| Control de versiones | Git + GitHub | — |
| Gestión de tareas | Jira | — |

---

## Arquitectura

El sistema sigue una arquitectura cliente-servidor desacoplada. El frontend Angular consume la API REST del backend Spring Boot mediante HTTP/JSON. El backend utiliza WebSocket (STOMP) para actualizaciones en tiempo real. En producción, Caddy actúa como reverse proxy y sirve el frontend compilado.

```text
Cliente (navegador)
  │
  ├── HTTP/JSON ──────────────▶ Caddy (reverse proxy, prod)
  │                                  │
  │                                  ├── /api/* ──▶ Backend (Spring Boot :8080)
  │                                  │                  │
  │                                  │                  └──▶ PostgreSQL
  │                                  │
  │                                  └── /* ────▶ Frontend (Angular)
  │
  └── WebSocket/STOMP ──────────▶ Backend (Spring Boot :8080)
```

---

## Módulos del sistema

| Épica | Módulo | Responsabilidad |
|-------|--------|-----------------|
| HE-01 | auth | Gestiona el ciclo de vida de sesiones JWT |
| HE-02 | reservas | Gestiona el ciclo de vida completo de reservas |
| HE-03 | mesas_comandas | Gestiona el mapa de mesas y comandas en borrador en tiempo real |
| HE-04 | mesas_comandas | Gestiona el flujo de producción en cocina y barra |
| HE-05 | pagos_caja | Cajero |
| HE-06 | reportes | Administrador |
| HE-07 | inventario | Gestiona el catálogo de productos, los movimientos manuales y cambo de estado de inventario |
| HE-08 | usuarios | Gestiona perfiles de clientes, administración de empleados y puntos de fidelización |

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
├── frontend/                    # Aplicación Angular
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
│   ├── backend/                 # Convenciones y referencia técnica del backend
│   ├── ops/                     # Guías operativas
│   └── frontend/                # Convenciones y referencia técnica del frontend
├── .github/
│   └── workflows/
│       ├── validar-rama.yml     # Valida nombres de rama en PRs
│       └── ci.yml               # Pipeline de integración continua
├── Caddyfile                    # Configuración del reverse proxy
├── docker-compose.yml           # Orquestación base (api, postgres)
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

Todos los comandos se ejecutan desde la raíz del proyecto.

#### Desarrollo

```bash
docker compose up --build
```

Docker fusiona `docker-compose.yml` + `docker-compose.override.yml` automáticamente.

| Servicio | URL |
|----------|-----|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 (altoro / altoro123) |

#### Producción

```bash
# 1. Crear el archivo de secrets a partir de la plantilla
cp .env.prod.example .env.prod

# 2. Completar todos los valores en .env.prod

# 3. Levantar los servicios
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up --build -d
```

En producción: Postgres no expone puertos al exterior. Caddy expone los puertos 80 y 443 y actúa como reverse proxy hacia la API. Swagger UI está deshabilitado. El servicio `db-backup` ejecuta backups diarios automáticos con retención de 7 días / 4 semanas / 6 meses.

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
| `POSTGRES_DB` | Nombre de base de datos |
| `POSTGRES_USER` | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL |
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

## Perfiles de Spring Boot

| Característica | `dev` | `prod` |
|----------------|-------|--------|
| Migraciones Flyway | `migration/` + `migration-dev/` | `migration/` únicamente |
| Logs SQL | sí | no |
| Swagger UI | habilitado (`/swagger-ui.html`) | deshabilitado |
| Nivel de log root | INFO/DEBUG | WARN |
| Mensajes de error | detallados | ocultos |
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
    ├── 02_inventario/                        # Insumos y productos
    ├── 03_comandas_borrador/                 # Comandas en borrador
    ├── 03_comandas_produccion/               # Comandas de producción
    ├── 03_mesas/                             # Mesas y asistencia
    ├── 03_visitas/                           # Visitas activas
    ├── 04_notificaciones/                    # Atención de notificaciones
    ├── 05_pagos_caja/                        # Pagos y cierre de venta
    ├── 07_reservas/                          # Gestión de reservas
    ├── 08_usuarios/                          # Clientes y puntos
    ├── e2e/                                  # Flujos end-to-end
    └── manual-testing/                       # Endpoints sin automatizar
```

### Características de los tests

- Autónomos: cada test incluye script `beforeRequest` con login propio — no dependen del orden de la colección
- Independientes: ejecutables en cualquier orden
- Auto-cleanup: los tests que crean datos los eliminan automáticamente en `afterResponse`
- Aserciones completas: validan estructura, estado HTTP, campos obligatorios y reglas de negocio

### Desde Postman for VS Code

1. Instalar la extensión Postman en VS Code
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

## Documentación técnica del backend

Referencia técnica del backend ubicada en `backend/docs/reference/`:

| Documento | Descripción |
|-----------|-------------|
| [`components.md`](backend/docs/reference/components.md) | Diagrama C4 Nivel 3: capas por módulo y relaciones entre componentes |
| [`endpoints.md`](backend/docs/reference/endpoints.md) | 79 endpoints: verbo, ruta, roles y contrato de respuesta |
| [`domain-model.md`](backend/docs/reference/domain-model.md) | 28 entidades en 8 módulos: campos, relaciones y enums del dominio |
| [`database.md`](backend/docs/reference/database.md) | Esquema PostgreSQL `restaurante`: 23 tablas, índices y migraciones Flyway |
| [`security.md`](backend/docs/reference/security.md) | JWT stateless: flujo de autenticación, roles, ownership y endpoints públicos |
| [`websocket.md`](backend/docs/reference/websocket.md) | STOMP/WebSocket: tópicos `/topic/*`, contratos de eventos y flujos en tiempo real |
| [`error-codes.md`](backend/docs/reference/error-codes.md) | Códigos `ENT-`, `AUTH-`, `NEG-`, `VAL-` con HTTP status y condición de disparo |
| [`testing.md`](backend/docs/reference/testing.md) | Estrategia de pruebas: JaCoCo, patrones por capa y colecciones Postman |
| [`deployment.md`](backend/docs/reference/deployment.md) | Infraestructura de producción: Docker Compose, Caddy y TLS automático |

---

## Documentación técnica del frontend

Referencia técnica y arquitectónica del frontend ubicada en `frontend/docs/`:

| Documento | Descripción |
|-----------|-------------|
| [`architecture.md`](frontend/docs/conventions/architecture.md) | Convenciones de arquitectura Standalone y estructura de características |
| [`coding-patterns.md`](frontend/docs/conventions/coding-patterns.md) | Patrones de código, uso de Signals y Formularios Reactivos |
| [`routing-and-guards.md`](frontend/docs/reference/routing-and-guards.md) | Estrategia de enrutamiento perezoso y protección de rutas por roles |
| [`services-and-websockets.md`](frontend/docs/reference/services-and-websockets.md) | Integración HTTP, WebSockets (STOMP) y tiempo real |
| [`deployment.md`](frontend/docs/reference/deployment.md) | Estrategia de despliegue Docker Multi-stage y Nginx SPA Fallback |

---

## Flujo de trabajo Git

Todas las reglas operativas de Git —nombres de ramas, formato de commits, protección de ramas y procedimiento para Pull Requests— están documentadas en:
[`CONTRIBUTING.md`](./CONTRIBUTING.md)

Antes de realizar el primer commit, cada miembro del equipo debe leer y seguir ese documento.

---

## Equipo

| Nombre | Rol en Scrum |
|--------|--------------|
| Paula Andrea Muñoz Delgado | Scrum Master · Desarrollador · Analista |
| Adrián Camilo Bergaño Ortega | Desarrollador · Analista |
| Yeixón Julián Gembuel Ciclos | Desarrollador · Tester |
| Rubeiro Romero | Desarrollador · Tester |

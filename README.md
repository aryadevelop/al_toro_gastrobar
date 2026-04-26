# Al Toro Gastrobar — Sistema Integral de Gestión

> Sistema web multiplataforma para la gestión operativa del restaurante Al Toro Gastrobar.
> Desarrollado por el equipo **ARYA** — Ingeniería de Sistemas, Universidad del Cauca.

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
- [Equipo](#equipo)
- [Pruebas de API](#pruebas-de-api)
- [Flujo de trabajo Git](#flujo-de-trabajo-git)

---

## Descripción

Al Toro Gastrobar requiere una solución tecnológica que integre en tiempo real los cuatro nodos operativos del restaurante: **servicio** (meseros), **producción** (cocina y barra), **caja** (cajeros) y **portal del cliente**. El sistema elimina la gestión manual en papel, reduce errores en la transmisión de pedidos y centraliza la información operativa y administrativa.

---

## Tecnologías

| Capa | Tecnología |
|------|------------|
| Frontend | Angular 17 (TypeScript) |
| Backend | Java 21 + Spring Boot 3.5 |
| Base de datos | PostgreSQL 15 |
| Mensajería | RabbitMQ 3.13 |
| Orquestación | Docker + Docker Compose |
| Control de versiones | Git + GitHub |
| Gestión de tareas | Jira |

---

## Arquitectura

El sistema sigue una arquitectura cliente-servidor desacoplada. El frontend Angular consume la API REST del backend Spring Boot mediante HTTP/JSON. El backend usa RabbitMQ para comunicación asíncrona y WebSocket (STOMP) para actualizaciones en tiempo real.

```
Frontend (Angular) ──HTTP/JSON──▶ Backend (Spring Boot) ──▶ PostgreSQL
       │                              │
       │                              ├──────────────▶ RabbitMQ
       │                              │
       └────WebSocket/STOMP───────────┘
```

Actualmente el backend y la infraestructura (PostgreSQL, RabbitMQ) corren en Docker.
El frontend corre localmente sin dockerizar.

---

## Módulos del sistema

| Épica | Módulo | Actores principales | Estado |
|-------|--------|---------------------|--------|
| HE-01 | Autenticación y perfiles | Todos los roles | 🚧 En desarrollo |
| HE-02 | Reservas y consumo | Cliente | 🚧 En desarrollo |
| HE-03 | Mesas, comandas y consulta de reservas | Mesero, Cajero | 🚧 En desarrollo |
| HE-04 | Producción | Cocinero, Bartender | 🚧 En desarrollo |
| HE-05 | Pagos y caja | Cajero | 🚧 En desarrollo |
| HE-06 | Histórico y reportes | Administrador | 📋 Pendiente |
| HE-07 | Inventario y decoraciones | Administrador | 📋 Pendiente |
| HE-08 | Personal y clientes | Administrador | 📋 Pendiente |

---

## Roles del sistema

| Rol | Acceso |
|-----|--------|
| Administrador | Acceso total: configuración, reportes, inventario, personal y consulta de reservas |
| Mesero | Consulta de reservas activas, mesas asignadas, comandas y notificaciones en tiempo real |
| Cocinero / Bartender | Comandas por estación (cocina/barra) |
| Cajero | Reservas, pagos, cierre de caja y mapa de mesas |
| Cliente | Reservas, pre-orden, historial de visitas y puntos de fidelización |

---

## Estructura del repositorio

```
al_toro_gastrobar/
├── frontend/                   # Aplicación Angular
├── backend/                    # API REST con Spring Boot
│   ├── src/
│   └── Dockerfile
├── docs/                       # Documentación del proyecto
├── .github/
│   └── workflows/
│       └── validar-rama.yml    # GitHub Action: valida nombres de rama en PRs
├── docker-compose.yml          # Orquestación base (api, postgres, rabbitmq)
├── docker-compose.override.yml # Overrides de desarrollo (puertos expuestos)
├── docker-compose.prod.yml     # Overrides de producción
├── .env                        # Variables de entorno dev (no se commitea)
├── .env.prod.example           # Plantilla de variables de producción
├── .gitignore
├── README.md
└── CONTRIBUTING.md             # Reglas de Git y flujo de trabajo
```

---

## Puesta en marcha

### Requisitos

| Herramienta | Versión mínima |
|---|---|
| Docker | 24+ |
| Docker Compose | 2.20+ |
| Node.js (para el frontend) | 18+ |
| Java (solo sin Docker) | 21 |
| Maven (solo sin Docker) | 3.9 |

### Backend + infraestructura (Docker)

Todos los comandos se ejecutan desde la **raíz del proyecto**.

**Desarrollo:**

```bash
docker compose up --build
```

Docker fusiona `docker-compose.yml` + `docker-compose.override.yml` automáticamente.

| Servicio | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ consola | http://localhost:15672 (guest / guest) |
| PostgreSQL | localhost:5432 (altoro / altoro1234) |

Los emails en dev son interceptados por Mailtrap — no se envían realmente.
Configura las credenciales en `.env` (`MAIL_USERNAME` / `MAIL_PASSWORD`).

**Producción:**

```bash
# 1. Crear el archivo de secrets a partir de la plantilla
cp .env.prod.example .env.prod

# 2. Completar todos los valores en .env.prod

# 3. Levantar los servicios
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d
```

En producción: Postgres y RabbitMQ no exponen puertos al exterior. Solo se expone el puerto `8080` de la API. Swagger UI está deshabilitado.

**Comandos útiles:**

```bash
docker compose logs -f api       # Logs de la API en tiempo real
docker compose up --build        # Reconstruir imagen tras cambios
docker compose down              # Detener (conserva volúmenes)
docker compose down -v           # Detener y borrar volúmenes (borra la BD)
```

### Frontend (local, sin Docker)

```bash
cd frontend
npm install
npm start
```

La app queda disponible en http://localhost:4200.

### Backend sin Docker

Requiere PostgreSQL y RabbitMQ corriendo localmente.

```bash
cd backend
./mvnw spring-boot:run
```

---

## Variables de entorno

| Variable | Descripción |
|---|---|
| `POSTGRES_DB` | Nombre de la base de datos |
| `POSTGRES_USER` | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL |
| `RABBITMQ_USERNAME` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | Contraseña de RabbitMQ |
| `JWT_SECRET` | Secret para firmar tokens JWT (mín. 32 chars en prod) |
| `JWT_EXPIRATION` | Expiración del token en ms (default: 86400000 = 24 h) |
| `MAIL_HOST` | Servidor SMTP |
| `MAIL_PORT` | Puerto SMTP (default: 587) |
| `MAIL_USERNAME` | Usuario SMTP |
| `MAIL_PASSWORD` | Contraseña SMTP |
| `SPRING_PROFILES_ACTIVE` | Perfil activo: `dev` o `prod` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos (ej: http://localhost:4200) |

Los valores de dev están en [`.env`](.env.prod.example) (no se commitea).
Los valores de prod van en `.env.prod` — usar [`.env.prod.example`](.env.prod.example) como plantilla.

---

## Perfiles de Spring Boot

| Característica | `dev` | `prod` |
|---|---|---|
| `ddl-auto` | `validate` (Flyway) | `validate` (Flyway) |
| Logs SQL | sí | no |
| Swagger UI | habilitado | deshabilitado |
| Logs nivel root | INFO/DEBUG | WARN |
| Mensajes de error | detallados | ocultos |
| Correo | Mailtrap | SMTP real |
| WebSocket | habilitado | habilitado |

---

## Equipo

| Nombre | Rol en Scrum |
|--------|--------------|
| Paula Andrea Muñoz Delgado | Scrum Master · Desarrollador · Analista · Tester |
| Adrián Camilo Bergaño Ortega | Desarrollador |
| Yeixón Julián Gembuel Ciclos | Tester |
| Rubeiro Romero | Desarrollador · Analista |

---

## Pruebas de API

Las colecciones de pruebas están organizadas en `backend/postman/postman/` usando el formato YAML de **Postman for VS Code**:

```
backend/postman/postman/
├── environments/
│   └── Al Toro – Local.environment.yaml        # Variables de entorno (baseUrl, credenciales)
├── collections/
│   ├── auth/                                   # Autenticación (login, registro, logout, refresh)
│   ├── reservas/                               # Gestión de reservas
│   │   ├── Al Toro – GET -api-reservas-disponibilidad/
│   │   ├── Al Toro – POST -api-reservas/
│   │   ├── Al Toro – PUT -api-reservas-{reservaId}/
│   │   ├── Al Toro – PATCH -api-reservas-{reservaId}-cancelar/
│   │   ├── Al Toro – GET -api-reservas-cliente-futuras/
│   │   ├── Al Toro – GET -api-reservas-mesero-consulta/
│   │   └── Al Toro – GET -api-reservas-mesero-{reservaId}-detalle/
│   ├── mesas_comandas/                         # Visitas y asistencia
│   ├── notificaciones/                         # Atención de notificaciones
│   ├── usuarios/                               # Puntos de fidelización
│   └── produccion/                             # Menú y productos
```

### Características de los tests

- **Autónomos**: Cada test incluye `beforeRequest` script con login autónomo — no dependen de la colección
- **Independientes**: Pueden ejecutarse en cualquier orden
- **Auto-cleanup**: Los tests que crean datos los limpian automáticamente en `afterResponse`
- **Aserciones completas**: Validan estructura, estado HTTP, campos obligatorios y reglas de negocio

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

**Nota**: Algunos tests requieren datos de seed específicos. Ejecutar `./mvnw flyway:clean flyway:migrate` desde `backend/` para resetear la BD a su estado inicial si los tests fallan por datos inconsistentes.

## Flujo de trabajo Git

Todas las reglas operativas de Git —nombres de ramas, formato de commits, protección de ramas y procedimiento para Pull Requests— están documentadas en:
[`CONTRIBUTING.md`](./CONTRIBUTING.md)

Antes de realizar el primer commit, cada miembro del equipo debe leer y seguir ese documento.

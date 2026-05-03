# API Endpoints - Al Toro Gastrobar

Base URL: `http://localhost:8080/api`

---

## Auth (`/api/auth`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/login` | **Public** | Autenticación de usuario. Retorna `accessToken` + `refreshToken`. Soporta `forceSessionOverride` para cerrar sesiones previas. |
| POST | `/register` | **Public** | Registro de nuevo usuario. Crea `Usuario` + `Cliente` con rol `CLIENTE`. |
| POST | `/refresh` | **Public** | Rotación de tokens. Invalida refresh token actual y retorna nuevo par de tokens. |
| GET | `/me` | **Authenticated** | Perfil del usuario actual con todos sus roles asignados. |
| POST | `/logout` | **Authenticated** | Cierre de sesión. Invalida todas las sesiones activas del usuario. |

---

## Reservas (`/api/reservas`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/disponibilidad?fechaHora=` | **CLIENTE** | Consulta zonas y decoraciones disponibles para una fecha/hora. Solo horario 17:00–22:00. Retorna capacidad disponible por zona. |
| POST | `/` | **CLIENTE** | Crear reserva. Puede incluir pre-orden de comanda (estado `PRE_RESERVA`). Valida capacidad de zona, decoración exclusiva, y horarios. |
| PUT | `/{reservaId}` | **CLIENTE** | Modificar reserva futura. Cutoff: 13:00 día anterior (BASICA), 23:00 día anterior (ESPECIAL). Solo `PENDIENTE`/`CONFIRMADA`. |
| PATCH | `/{reservaId}/cancelar` | **CLIENTE** | Cancelar reserva. Sin restricción de tiempo. Retorna flag `requiereWhatsApp` si hay abono a reembolsar. |
| PATCH | `/{reservaId}/marcar-inasistencia` | **MESERO / ADMIN** | Marcar reserva como inasistencia tras 30 minutos de tolerancia. Solo `CONFIRMADA`. Libera zona y decoración. Cambio irreversible. |
| GET | `/cliente/futuras?emailCliente=` | **CLIENTE** | Lista reservas futuras del cliente (`PENDIENTE`/`CONFIRMADA`) ordenadas ASC por fecha. Ownership validation. |
| GET | `/cliente/canceladas-devueltas?emailCliente=` | **CLIENTE** | Historial de reservas canceladas (`CANCELADA`/`DEVUELTA`) del cliente. Ownership validation. |
| GET | `/{reservaId}/detalle` | **CLIENTE / ADMIN** | Detalle completo de reserva: zona, decoración, pre-orden con ítems, abonos. CLIENTE: ownership validation. |
| GET | `/mesero/consulta?fecha=&identificador=` | **MESERO / ADMIN** | Lista reservas activas del día (o fecha especificada). Si se proporciona `identificador`, busca por ID de reserva. Retorna campo `mostrarBotonInasistencia` calculado dinámicamente. |
| GET | `/mesero/{reservaId}/detalle` | **MESERO / ADMIN** | Detalle completo para meseros: incluye teléfono cliente, modificaciones de pre-orden, información de contacto. |

---

## Clientes / Puntos (`/api/clientes`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/me/puntos?emailCliente=` | **CLIENTE** | Consulta puntos propios: `puntosActuales` (canjeables) + `puntosAcumulados` (lifetime). Ownership validation. |
| GET | `/{clienteId}/puntos` | **CAJERO / ADMIN** | Consulta puntos de cualquier cliente. Para verificar antes de canje. |
| POST | `/{clienteId}/canje-puntos?emailEmpleado=` | **CAJERO** | Canjear puntos del cliente. Resetea `puntosActuales` a 0, `puntosAcumulados` no cambia. Crea registro `CanjePuntos`. |

---

## Productos (`/api/productos`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/carta` | **Authenticated** | Menú agrupado por categorías (`CategoriaCarta`). Solo productos NO especiales, activos, ordenados por `orden`. |
| GET | `/menu-especial` | **Authenticated** | Menús especiales con grupos de opciones de modificación. Para crear pre-órdenes con personalizaciones. |

---

## Mesas (`/api/mesas`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/` | **MESERO / ADMIN** | Obtener mapa completo de mesas agrupadas por zona. Incluye estado de cada mesa, identificador asignado, y visita activa. |
| GET | `/{mesaId}/detalle` | **MESERO / ADMIN** | Detalle de mesa específica: identificador, estado, visita activa con cliente, items de comandas pendientes. |
| GET | `/{mesaId}/items-produccion` | **MESERO / ADMIN** | Items de comandas en producción para la mesa (estados `PENDIENTE`, `EN_PREPARACION`, `LISTO`). Agrupados por comanda y estación (COCINA/BARRA). |
| POST | `/` | **MESERO / ADMIN** | Asignar identificador a mesa. Requiere `mesaId` y `identificador`. Valida unicidad y reglas de asignación. Publica evento WebSocket. |
| GET | `/zonas-disponibles` | **MESERO / ADMIN** | Lista zonas con mesas disponibles para asignar. Excluye mesas ya asignadas o con visita activa. |

---

## Visitas (`/api/visitas`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/cliente/historial?emailCliente=` | **CLIENTE** | Historial de visitas pasadas (con `fechaHoraFin`). Ordenadas DESC por fecha. Ownership validation. |
| GET | `/cliente/{visitaId}/detalle` | **CLIENTE** | Detalle completo de visita: comandas, ítems agrupados y ordenados por categoría, venta. Ownership validation. |
| GET | `/activa` | **CLIENTE / MESERO / CAJERO / ADMIN** | Estado de visita activa (`fechaHoraFin` IS NULL): ítems ordenados por categoría, total, flag `asistenciaSolicitada`, `notificacionAsistenciaId`. **CLIENTE**: usa token (ownership). **Otros roles**: requieren `?emailCliente=`. |
| POST | `/{visitaId}/asistencia` | **CLIENTE** | Solicita asistencia de mesero. Crea notificación tipo `ATENCION`. Retorna 409 si ya existe solicitud activa para la mesa. Publica evento WebSocket. |

---

## Ventas (`/api/ventas`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/` | **CAJERO** | Cerrar cuenta de visita. Crea registro `Venta`, cierra visita (`fechaHoraFin`), otorga +1 punto al cliente. Publica `CuentaCerradaWsMessage` con `puntosActuales` vía WebSocket. |

---

## Notificaciones (`/api/notificaciones`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| PATCH | `/{notificacionId}/atender` | **MESERO / ADMIN** | Marca solicitud de asistencia como atendida (`ACTIVA` → `ATENDIDA`). Publica evento WebSocket al cliente de la visita. |

---

## Resumen por Rol

### CLIENTE
- **Auth**: login, register, refresh, me, logout
- **Reservas**: disponibilidad, crear, modificar, cancelar, futuras, canceladas, detalle
- **Puntos**: me/puntos
- **Productos**: carta, menu-especial
- **Visitas**: historial, detalle, activa (propia), asistencia

### MESERO
- **Auth**: login, refresh, me, logout
- **Reservas**: consulta, detalle (mesero)
- **Productos**: carta, menu-especial
- **Mesas**: mapa, detalle, items-produccion, asignar, zonas-disponibles
- **Visitas**: activa (cualquier cliente con param)
- **Notificaciones**: atender

### CAJERO
- **Auth**: login, refresh, me, logout
- **Clientes**: puntos (cualquier cliente), canje-puntos
- **Productos**: carta, menu-especial
- **Visitas**: activa (cualquier cliente con param)
- **Ventas**: cerrar cuenta

### ADMIN
- **Auth**: login, refresh, me, logout
- **Reservas**: detalle, consulta (mesero), detalle (mesero)
- **Clientes**: puntos (cualquier cliente)
- **Productos**: carta, menu-especial
- **Mesas**: mapa, detalle, items-produccion, asignar, zonas-disponibles
- **Visitas**: activa (cualquier cliente con param)
- **Notificaciones**: atender

### Public (sin autenticación)
- **Auth**: login, register, refresh

---

## Notas Importantes

### Ownership Validation
Endpoints con ownership validation (CLIENTE solo accede a recursos propios):
- `GET /api/reservas/cliente/futuras`
- `GET /api/reservas/cliente/canceladas-devueltas`
- `GET /api/reservas/{reservaId}/detalle` (CLIENTE)
- `GET /api/clientes/me/puntos`
- `GET /api/visitas/cliente/historial`
- `GET /api/visitas/cliente/{visitaId}/detalle`
- `GET /api/visitas/activa` (cuando se usa con token CLIENTE)
- `POST /api/visitas/{visitaId}/asistencia`

### Multi-rol Endpoints
Endpoints que sirven a múltiples roles con comportamiento diferenciado:
- `GET /api/visitas/activa` — CLIENTE usa token, otros roles usan `?emailCliente=`
- `GET /api/reservas/{reservaId}/detalle` — CLIENTE con ownership, ADMIN sin restricción

### WebSocket Integration
Endpoints que publican eventos WebSocket:
- `POST /api/mesas` → `/topic/mesas`
- `POST /api/visitas/{visitaId}/asistencia` → `/topic/mesas/asistencia`
- `PATCH /api/notificaciones/{notificacionId}/atender` → `/topic/visita/{visitaId}/asistencia`
- `POST /api/ventas` → `/topic/visita/{visitaId}/cuenta`

### Business Rules
- **Reservation hours**: 17:00–22:00 only
- **Modification cutoff**: BASICA 13:00, ESPECIAL 23:00 (day before)
- **Active visit**: `visitaFechaHoraFin IS NULL`
- **Loyalty points**: +1 per closed Venta
- **Point redemption**: resets `puntosActuales` to 0, `puntosAcumulados` unchanged

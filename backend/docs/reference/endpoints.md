# Referencia de endpoints — Al Toro Gastrobar

Base URL: `http://localhost:8080/api`. Todas las respuestas siguen el contrato `ApiResponse<T>` con campos `message`, `data` y `timestamp`.

---

## Auth (`/api/auth`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | Público | Registrar nueva cuenta de cliente |
| POST | `/api/auth/login` | Público | Iniciar sesión; retorna par de tokens JWT |
| POST | `/api/auth/refresh` | Público | Rotar par de tokens con refresh token válido |
| GET | `/api/auth/me` | Autenticado | Perfil del usuario cuya sesión está activa |
| POST | `/api/auth/logout` | Autenticado | Invalidar todas las sesiones activas del usuario |

---

## Reservas (`/api/reservas`, `/api/reservas/mesero`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/reservas/disponibilidad` | CLIENTE | Consultar disponibilidad de zonas y decoraciones para fecha/hora |
| POST | `/api/reservas` | CLIENTE | Crear nueva reserva con pre-orden opcional |
| PUT | `/api/reservas/{reservaId}` | CLIENTE, ADMIN | Modificar reserva futura (cutoff BASICA 13:00, ESPECIAL 23:00) |
| PATCH | `/api/reservas/{reservaId}/cancelar` | CLIENTE, ADMIN, CAJERO | Cancelar reserva; sin restricción horaria |
| PATCH | `/api/reservas/{reservaId}/confirmar` | CAJERO, ADMIN | Confirmar reserva ESPECIAL pendiente |
| PATCH | `/api/reservas/{reservaId}/marcar-inasistencia` | MESERO, ADMIN | Marcar inasistencia tras 30 min de tolerancia |
| GET | `/api/reservas/cliente/futuras` | CLIENTE, ADMIN | Reservas futuras activas del cliente (PENDIENTE o CONFIRMADA) |
| GET | `/api/reservas/cliente/canceladas-devueltas` | CLIENTE, ADMIN | Reservas canceladas o devueltas del cliente |
| GET | `/api/reservas/{reservaId}/detalle` | CLIENTE, MESERO, CAJERO, ADMIN | Detalle completo de una reserva |
| POST | `/api/reservas/{reservaId}/abonos` | CAJERO, ADMIN | Registrar anticipo o devolución sobre una reserva |
| GET | `/api/reservas/{reservaId}/resumen-pago` | CAJERO, ADMIN | Resumen financiero de pagos de una reserva |
| GET | `/api/reservas/mesero/consulta` | MESERO, CAJERO, ADMIN | Listar reservas del día o buscar por identificador; vista diferenciada por rol |
| GET | `/api/reservas/mesero/{reservaId}/detalle` | MESERO, ADMIN | Detalle completo de una reserva (vista mesero) |

---

## Clientes (`/api/clientes`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/clientes/me` | CLIENTE | Perfil del cliente autenticado |
| PUT | `/api/clientes/me` | CLIENTE | Actualizar datos del perfil propio |
| POST | `/api/clientes/me/cambiar-contraseña` | CLIENTE | Cambiar contraseña del cliente autenticado |
| GET | `/api/clientes/me/puntos` | CLIENTE | Consultar puntos de fidelización propios |
| GET | `/api/clientes/{clienteId}/puntos` | CAJERO, ADMIN | Consultar puntos de fidelización de un cliente |
| POST | `/api/clientes/{clienteId}/canje-puntos` | CAJERO, ADMIN | Registrar canje de puntos de fidelización |
| GET | `/api/clientes/buscar` | CAJERO, ADMIN | Buscar clientes por email (coincidencia parcial) |
| GET | `/api/clientes` | ADMIN | Listar clientes con filtros (visitas, fechas, estado, nombre, cumpleaños) |
| GET | `/api/clientes/{clienteId}/ventas` | ADMIN | Historial completo de ventas por cliente |
| GET | `/api/clientes/{clienteId}/ventas/resumen` | ADMIN | Resumen del historial de ventas del cliente |
| GET | `/api/clientes/{clienteId}/ventas/agrupadas/anio` | ADMIN | Totales de ventas agrupadas por año |
| GET | `/api/clientes/{clienteId}/ventas/agrupadas/mes` | ADMIN | Totales de ventas agrupadas por mes |
| POST | `/api/clientes/{clienteId}/ventas/recordatorio` | ADMIN | Enviar recordatorio a cliente inactivo |
| GET | `/api/clientes/buscar/nombre` | ADMIN | Buscar clientes por nombre |
| GET | `/api/clientes/buscar/correo` | ADMIN | Buscar clientes por correo |
| GET | `/api/clientes/buscar/telefono` | ADMIN | Buscar clientes por teléfono |

---

## Empleados (`/api/empleados`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/api/empleados` | ADMIN | Crear nuevo empleado con roles asignados |
| GET | `/api/empleados` | ADMIN | Listar empleados con filtros por rol, estado y nombre |
| PATCH | `/api/empleados/{empleadoId}/estado` | ADMIN | Cambiar estado de un empleado entre activo e inactivo |

---

## Productos (`/api/productos`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/productos/carta` | CLIENTE | Carta de platos y bebidas agrupada por categoría (excluye menú especial) |
| GET | `/api/productos/menu-especial` | CLIENTE | Menús especiales activos con grupos y opciones de modificación |
| GET | `/api/productos` | ADMIN | Listar productos de inventario con filtros opcionales por categoría y nombre |
| GET | `/api/productos/buscar` | Autenticado | Buscar productos por nombre (parcial, excluye menú especial e inactivos) |
| GET | `/api/productos/{productoId}/estado/implicaciones` | ADMIN | Evaluar implicaciones del cambio de estado de un producto |
| PUT | `/api/productos/{productoId}/estado` | ADMIN | Cambiar estado de un producto (ACTIVO/INACTIVO) |

---

## Inventario (`/api/inventario`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/inventario/buscar` | PRODUCCION, ADMIN | Buscar productos e insumos activos por nombre para ajuste |
| POST | `/api/inventario/movimientos` | PRODUCCION, ADMIN | Registrar ajuste manual de inventario (ingreso o egreso) |
| GET | `/api/inventario/insumos/{insumoId}/estado/implicaciones` | ADMIN | Evaluar implicaciones del cambio de estado de un insumo |
| PUT | `/api/inventario/insumos/{insumoId}/estado` | ADMIN | Cambiar estado de un insumo (ACTIVO/INACTIVO) |

---

## Mesas (`/api/mesas`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/mesas` | MESERO, CAJERO, ADMIN | Mapa de mesas con todas las zonas; filtrable por zonaId |
| GET | `/api/mesas/{mesaId}/detalle` | MESERO, CAJERO, ADMIN | Detalle de una mesa; CAJERO recibe campos extra (clienteId, puntos, etc.) |
| GET | `/api/mesas/{mesaId}/items-produccion` | MESERO, ADMIN | Ítems en producción de una mesa (solo mesero asignado o ADMIN) |
| POST | `/api/mesas` | MESERO, ADMIN | Asignar identificador a mesa (walk-in o llegada de reserva confirmada) |
| GET | `/api/mesas/zonas-disponibles` | MESERO, ADMIN | Listar zonas con disponibilidad calculada en tiempo real |

---

## Comandas (`/api/comandas`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/comandas/borrador` | MESERO, ADMIN | Obtener borrador completo de la visita (estructura vacía si no existe) |
| POST | `/api/comandas/borrador/items` | MESERO, ADMIN | Agregar ítem al borrador; enruta a COCINA o BARRA según categoría |
| PATCH | `/api/comandas/borrador/items/{itemId}` | MESERO, ADMIN | Modificar cantidad o descripción de un ítem del borrador |
| DELETE | `/api/comandas/borrador/items/{itemId}` | MESERO, ADMIN | Eliminar ítem del borrador (en menú especial elimina el par COCINA+BARRA) |
| POST | `/api/comandas/borrador/{comandaId}/enviar` | MESERO, ADMIN | Enviar comanda a producción (BORRADOR → PENDIENTE, valida stock) |
| DELETE | `/api/comandas/borrador` | MESERO, ADMIN | Cancelar formulario y eliminar todas las comandas BORRADOR de la visita |
| PATCH | `/api/comandas/borrador/{comandaId}/notas` | MESERO, ADMIN | Actualizar notas de cocina o barra en el borrador |
| GET | `/api/comandas/produccion` | PRODUCCION | Tablero de producción: pendientes, en preparación y listos de la estación |
| GET | `/api/comandas/produccion/{comandaId}` | PRODUCCION | Detalle de una comanda visible para la estación del usuario |
| POST | `/api/comandas/produccion/{comandaId}/iniciar` | PRODUCCION | Iniciar preparación (PENDIENTE → EN_PREPARACION); descuenta inventario |
| POST | `/api/comandas/produccion/{comandaId}/listo` | PRODUCCION | Marcar comanda como lista (EN_PREPARACION → LISTO); crea notificación al mesero |

---

## Visitas (`/api/visitas`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/visitas/cliente/historial` | CLIENTE, ADMIN | Historial de visitas del cliente, ordenado desc por fecha; ownership para CLIENTE |
| GET | `/api/visitas/cliente/{visitaId}/detalle` | CLIENTE, CAJERO, MESERO, ADMIN | Detalle completo de una visita; ownership para CLIENTE |
| GET | `/api/visitas/activa` | CLIENTE, MESERO, CAJERO, ADMIN | Estado de la visita activa; CLIENTE usa su propio email, demás roles requieren emailCliente |
| POST | `/api/visitas/{visitaId}/asistencia` | CLIENTE | Solicitar asistencia de mesero; crea notificación ATENCION y broadcast WS |
| PATCH | `/api/visitas/{visitaId}/cliente` | CAJERO | Asignar cliente a la visita o continuar como invitado |
| PATCH | `/api/visitas/{visitaId}/items` | CAJERO | Ajustar cantidades, precios y eliminar ítems de la cuenta |

---

## Ventas (`/api/ventas`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/ventas/{visitaId}/cuenta` | CAJERO | Cuenta preliminar: ítems, totales, decoración, abonos, saldo y puntos |
| POST | `/api/ventas` | CAJERO | Registrar venta y cerrar cuenta de una visita; suma puntos de lealtad |
| GET | `/api/ventas/{visitaId}/detalle` | ADMIN | Detalle completo de una venta |
| GET | `/api/ventas` | ADMIN | Listar ventas con filtros (ventaId, fechas, método de pago) |
| GET | `/api/ventas/dashboard` | ADMIN | Resumen diario del dashboard administrativo |

---

## Notificaciones (`/api/notificaciones`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| PATCH | `/api/notificaciones/{notificacionId}/atender` | MESERO, CAJERO, ADMIN | Marcar solicitud de asistencia como atendida |
| PATCH | `/api/notificaciones/{notificacionId}/servir-platos` | MESERO, CAJERO, ADMIN | Confirmar entrega de platos listos a la mesa |
| PATCH | `/api/notificaciones/{notificacionId}/servir-bebidas` | MESERO, CAJERO, ADMIN | Confirmar entrega de bebidas listas a la mesa |
| POST | `/api/notificaciones/cambio` | PRODUCCION | Notificar cambio sobre comanda pendiente; crea notificación CAMBIO en mesa |
| PATCH | `/api/notificaciones/{notificacionId}/atender-cambio` | MESERO, CAJERO, ADMIN | Atender cambio de comanda; retorna comandaId listo para edición |

---

## Resumen por rol

### CLIENTE
- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`
- `GET /api/auth/me`, `POST /api/auth/logout`
- `GET /api/clientes/me`, `PUT /api/clientes/me`, `POST /api/clientes/me/cambiar-contraseña`, `GET /api/clientes/me/puntos`
- `GET /api/reservas/disponibilidad`, `POST /api/reservas`, `PUT /api/reservas/{id}` (solo propias), `PATCH /api/reservas/{id}/cancelar` (solo propias)
- `GET /api/reservas/cliente/futuras` (solo propias), `GET /api/reservas/cliente/canceladas-devueltas` (solo propias), `GET /api/reservas/{id}/detalle` (solo propias)
- `GET /api/productos/carta`, `GET /api/productos/menu-especial`
- `GET /api/visitas/cliente/historial` (solo propias), `GET /api/visitas/cliente/{id}/detalle` (solo propias), `GET /api/visitas/activa` (propia)
- `POST /api/visitas/{id}/asistencia`

### MESERO
- `GET /api/reservas/mesero/consulta`, `GET /api/reservas/mesero/{id}/detalle`
- `PATCH /api/reservas/{id}/marcar-inasistencia`
- `GET /api/mesas`, `GET /api/mesas/{id}/detalle`, `GET /api/mesas/{id}/items-produccion`, `POST /api/mesas`, `GET /api/mesas/zonas-disponibles`
- `GET /api/comandas/borrador`, `POST /api/comandas/borrador/items`, `PATCH /api/comandas/borrador/items/{id}`, `DELETE /api/comandas/borrador/items/{id}`, `POST /api/comandas/borrador/{id}/enviar`, `DELETE /api/comandas/borrador`, `PATCH /api/comandas/borrador/{id}/notas`
- `GET /api/visitas/cliente/{id}/detalle`, `GET /api/visitas/activa`
- `PATCH /api/notificaciones/{id}/atender`, `PATCH /api/notificaciones/{id}/servir-platos`, `PATCH /api/notificaciones/{id}/servir-bebidas`, `PATCH /api/notificaciones/{id}/atender-cambio`
- `GET /api/reservas/{id}/detalle`

### CAJERO
- `GET /api/reservas/mesero/consulta`
- `PATCH /api/reservas/{id}/cancelar`, `PATCH /api/reservas/{id}/confirmar`
- `POST /api/reservas/{id}/abonos`, `GET /api/reservas/{id}/resumen-pago`
- `GET /api/reservas/{id}/detalle`
- `GET /api/clientes/{id}/puntos`, `POST /api/clientes/{id}/canje-puntos`, `GET /api/clientes/buscar`
- `GET /api/mesas`, `GET /api/mesas/{id}/detalle`
- `GET /api/visitas/cliente/{id}/detalle`, `GET /api/visitas/activa`, `PATCH /api/visitas/{id}/cliente`, `PATCH /api/visitas/{id}/items`
- `GET /api/ventas/{id}/cuenta`, `POST /api/ventas`
- `PATCH /api/notificaciones/{id}/atender`, `PATCH /api/notificaciones/{id}/servir-platos`, `PATCH /api/notificaciones/{id}/servir-bebidas`, `PATCH /api/notificaciones/{id}/atender-cambio`

### COCINERO / BARTENDER (rol `PRODUCCION`)
- `GET /api/inventario/buscar`, `POST /api/inventario/movimientos`
- `GET /api/comandas/produccion`, `GET /api/comandas/produccion/{id}`, `POST /api/comandas/produccion/{id}/iniciar`, `POST /api/comandas/produccion/{id}/listo`
- `POST /api/notificaciones/cambio`

### ADMIN
- Acceso completo a todos los endpoints listados arriba
- Endpoints exclusivos: `GET /api/empleados`, `POST /api/empleados`, `PATCH /api/empleados/{id}/estado`
- `GET /api/productos`, `GET /api/productos/{id}/estado/implicaciones`, `PUT /api/productos/{id}/estado`
- `GET /api/inventario/insumos/{id}/estado/implicaciones`, `PUT /api/inventario/insumos/{id}/estado`
- `GET /api/clientes`, `GET /api/clientes/{id}/ventas`, `GET /api/clientes/{id}/ventas/resumen`, `GET /api/clientes/{id}/ventas/agrupadas/anio`, `GET /api/clientes/{id}/ventas/agrupadas/mes`, `POST /api/clientes/{id}/ventas/recordatorio`
- `GET /api/clientes/buscar/nombre`, `GET /api/clientes/buscar/correo`, `GET /api/clientes/buscar/telefono`
- `GET /api/ventas/{id}/detalle`, `GET /api/ventas`, `GET /api/ventas/dashboard`

### Público (sin autenticación)
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`

---

## Notas técnicas

### Ownership validation
Endpoints donde CLIENTE solo puede acceder a sus propios recursos:
- `PUT /api/reservas/{reservaId}` — email tomado del token, no del body
- `PATCH /api/reservas/{reservaId}/cancelar` — email tomado del token
- `GET /api/reservas/cliente/futuras` — parámetro `emailCliente` debe coincidir con token
- `GET /api/reservas/cliente/canceladas-devueltas` — parámetro `emailCliente` debe coincidir con token
- `GET /api/reservas/{reservaId}/detalle` — servicio valida propiedad
- `GET /api/visitas/cliente/historial` — parámetro `emailCliente` debe coincidir con token
- `GET /api/visitas/cliente/{visitaId}/detalle` — servicio valida propiedad

### Endpoints con comportamiento diferenciado por rol
- `GET /api/mesas/{mesaId}/detalle` — CAJERO recibe campos extra: `clienteId`, `puntosFidelizacion`, `esCumpleanos`, `puedeGenerarCuenta`
- `GET /api/reservas/mesero/consulta` — MESERO/ADMIN: reservas activas con indicador de inasistencia; CAJERO: todas las reservas con botones de acción
- `GET /api/visitas/activa` — CLIENTE usa email del token; MESERO/CAJERO/ADMIN requieren parámetro `emailCliente`

### Reglas de negocio
- Cutoff de modificación de reserva: BASICA 13:00 del día previo, ESPECIAL 23:00 del día previo
- Inasistencia: solo marcable cuando han transcurrido al menos 30 minutos desde la hora programada
- Puntos de lealtad: +1 punto por cada venta cerrada; `clientePuntos` = canjeables, `clientePuntosAcumulados` = acumulados lifetime
- Envío de comanda a producción valida stock disponible antes de la transición BORRADOR → PENDIENTE
- Inicio de preparación (`/produccion/{id}/iniciar`) descuenta inventario al ejecutar la transición PENDIENTE → EN_PREPARACION
- Solicitud de asistencia (`/visitas/{id}/asistencia`): retorna 409 si ya existe una notificación ATENCION activa para la misma mesa

### Eventos WebSocket
Ver contratos completos, tópicos y flujos en `docs/reference/websocket.md`.

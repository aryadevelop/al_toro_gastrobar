# Diagrama de Componentes — Nivel 3 C4

Monolito modularizado. Cada módulo sigue `controller → service → repository → entity + DTOs + mapper`.
Componentes marcados con *(futuro)* están planificados pero no implementados.

---

## Módulos y sus capas

### `auth` — Autenticación
JWT issue/refresh, registro de clientes, sesiones.

| Capa | Responsabilidad |
|------|-----------------|
| Controller | Login, registro, refresh de token |
| Service | Valida credenciales y coordina emisión de tokens |
| Security | Genera/firma/valida JWT (`JwtTokenProvider`) e intercepta peticiones (`JwtAuthenticationFilter`) |
| Repository | Usuarios y sesiones activas |

---

### `usuarios` — Usuarios
Perfiles de clientes y empleados, puntos de fidelización, roles.

| Capa | Responsabilidad |
|------|-----------------|
| Controller — Clientes | Consulta/canje de puntos y modificación de perfil |
| Controller — Empleados *(futuro)* | Crear, consultar y cambiar estado de empleados (HE-08) |
| Service — Clientes | Actualización de perfil, cambio de contraseña, acumulación y canje de puntos |
| Service — Empleados *(futuro)* | Creación de empleados, asignación de roles y cambio de estado |
| Repository | Clientes, empleados, roles, canjes |

---

### `reservas` — Reservas
Ciclo de vida de reservas: creación, modificación, cancelación, pre-orden y consulta histórica.

| Capa | Responsabilidad |
|------|-----------------|
| Controller — Escritura | Crear, modificar, cancelar reservas y agregar abonos |
| Controller — Consulta | Historial, detalle, disponibilidad, decoraciones (solo lectura) |
| Service — Escritura | Estados, tipo BÁSICA/ESPECIAL, abonos, WhatsApp, orquesta pre-orden |
| Service — Consulta | Historial paginado, detalle con pre-orden incluida |
| PreOrdenGestor | Valida, persiste y elimina comandas `PRE_RESERVA` con split COCINA/BARRA |
| ReservaValidador | Ventana horaria, cutoff de modificación, capacidad de zona |
| DisponibilidadConsultador | Disponibilidad de fechas/horas/zonas según bloqueos y reservas activas |
| Mapper | Fusiona pares COCINA+BARRA en un único ítem de respuesta con campo `bebida` |
| Repository | Reservas, decoraciones, zonas, bloqueos, comandas PRE_RESERVA |

---

### `mesas_comandas` — Mesas y Comandas
Mapa de mesas en tiempo real, visitas y flujo de producción de comandas.

| Capa | Responsabilidad |
|------|-----------------|
| Controller — Mesas | Mapa por zona, marcar llegada/inasistencia |
| Controller — Visitas y Comandas | Asignar mesa, modificar comanda, solicitar asistencia, consultar estado de orden |
| Service — Mesas | Estados del mapa, zonas, disponibilidad, asignación al iniciar visita |
| Service — Visitas y Comandas | Crear visitas, agregar/quitar ítems, enviar a producción, cambios de estado desde cocina/barra (HE-04) |
| MesaValidador | Reglas de negocio antes de asignar mesa |
| MesaWsPublisher | Publica eventos de mesas y visitas al tópico WebSocket |
| Mapper | Mesa, visita y comanda → DTOs de respuesta |
| Repository | Mesas, zonas, visitas, comandas, ítems, modificaciones de menú |

---

### `produccion_inventario` — Productos e Inventario
Catálogo de productos (carta y menú especial) y gestión de inventario de insumos.

| Capa | Responsabilidad |
|------|-----------------|
| Controller — Catálogo | Carta por categoría y menú especial con modificaciones y bebidas |
| Controller — Administración *(futuro)* | Cambiar estado de productos/insumos, registrar movimientos, gestionar decoraciones (HE-07) |
| Service — Catálogo | Agrupa productos activos por categoría, construye respuesta de menú especial |
| Service — Inventario *(futuro)* | Ingresos, egresos manuales y automáticos de insumos y productos |
| Mapper | Producto → DTOs de carta y menú especial |
| Repository | Productos, insumos, recetas, movimientos, opciones de modificación, bebidas por menú, decoraciones |

---

### `pagos_caja` — Caja y Pagos
Cierre de cuentas, registro de ventas y liberación de mesas.

| Capa | Responsabilidad |
|------|-----------------|
| Controller | Cierre de venta por el cajero |
| Service | Consolida ítems consumidos, aplica puntos, cierra visita y libera mesa |
| Repository | Ventas y abonos |

---

### `notificaciones` — Notificaciones
Persistencia de alertas y distribución de eventos en tiempo real por WebSocket.

| Capa | Responsabilidad |
|------|-----------------|
| Controller | Consultar notificaciones activas de zona y marcarlas como atendidas |
| Service | Crea y persiste notificaciones de tipo ATENCION, PLATOS_LISTOS, BEBIDAS_LISTAS, CAMBIO |
| NotificacionWsPublisher | Publica mensajes al broker STOMP en los tópicos suscritos |
| EmailService *(futuro)* | Correos transaccionales al cliente: confirmaciones, recordatorios, cancelaciones |
| ImpresionService *(futuro)* | Tickets térmicos via RabbitMQ al bridge de impresión (HE-04-HU-01) |
| Repository | Notificaciones activas e historial |

---

### `reportes` — Panel y Reportes *(futuro)*
Métricas operativas en tiempo real e histórico de ventas (HE-06).

| Capa | Responsabilidad |
|------|-----------------|
| Controller *(futuro)* | Panel diario en tiempo real y consulta histórica de ventas con filtros |
| Service *(futuro)* | Métricas del día y reportes históricos en modo solo lectura sobre tablas de ventas |

---

## Interacciones entre módulos

### Flujos principales

**Creación de reserva con pre-orden**
`reservas.ReservaService` → `reservas.PreOrdenGestor` → crea `Comanda`/`ComandaItem` en `mesas_comandas`
`PreOrdenGestor` valida en `produccion_inventario`: producto activo, opción pertenece al menú, bebida disponible.

**Llegada del cliente**
`mesas_comandas.MesaAsignarService` → lee `Reserva` de `reservas` → transiciona comandas `PRE_RESERVA → PENDIENTE` → `MesaWsPublisher` notifica mapa en tiempo real.

**Envío a producción**
`mesas_comandas.VisitaService` → agrega ítems validando en `produccion_inventario` → `MesaWsPublisher` publica estado de visita.

**Comanda lista (cocina/barra)**
`mesas_comandas.VisitaEstadoService` → `notificaciones.NotificacionService` (persiste alerta) → `NotificacionWsPublisher` (publica al tópico del mesero).

**Cierre de cuenta**
`pagos_caja.VentaService` → cierra `Visita` en `mesas_comandas` → `usuarios.PuntosService` suma puntos → `notificaciones.NotificacionWsPublisher` publica cuenta cerrada.

**Autenticación transversal**
`auth.JwtAuthenticationFilter` → `CustomUserDetailsService` → `UsuarioRepository` antes de cada petición.

### Mapa de dependencias

```
auth ──────────────────────────────→ todos los módulos (filtro JWT)
reservas ──→ produccion_inventario  valida productos, opciones y bebidas
reservas ──→ mesas_comandas         crea comandas PRE_RESERVA
mesas_comandas ──→ reservas         lee reserva al iniciar visita
mesas_comandas ──→ notificaciones   persiste alertas y publica WS
mesas_comandas ──→ produccion_inventario  valida productos en comanda
pagos_caja ──→ mesas_comandas       cierra visita y comandas
pagos_caja ──→ usuarios             suma puntos al cliente
pagos_caja ──→ notificaciones       publica WS cuenta cerrada
notificaciones ──→ RabbitMQ / WebSocket   distribución externa
```

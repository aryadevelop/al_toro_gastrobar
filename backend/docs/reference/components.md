# Diagrama de componentes — nivel 3 C4

El backend es un monolito modularizado en Spring Boot. Cada módulo sigue el patrón `controller → service → repository → entity + DTOs + mapper` bajo el paquete `modules/`.

---

## Módulos y sus capas

Cada módulo organiza sus clases en subcarpetas `controller/`, `service/`, `repository/`, `entity/`, `dto/` y `mapper/`.

### `auth` — Autenticación

Gestiona el ciclo de vida de sesiones JWT: registro de clientes, login, refresh de tokens y logout. Las sesiones se persisten en la tabla `Sesion` para soportar invalidación explícita.

| Capa | Responsabilidad |
|------|-----------------|
| `AuthController` | Endpoints `/api/auth`: register, login, refresh, me, logout |

| `AuthService` | Valida credenciales, gestiona sesiones activas, emite y rota par de tokens |
| `CustomUserDetailsService` | Carga usuario desde `UsuarioRepository` para Spring Security |
| `JwtTokenProvider` | Genera, firma y valida tokens HMAC-SHA256 |
| `JwtAuthenticationFilter` | Intercepta cada petición, extrae token del header y valida `sesionActiva=true` |

| `UsuarioRepository` | Consulta por email; lectura de credenciales |
| `SesionRepository` | Lectura y escritura de sesiones activas por token y refresh token |
| `UsuarioRolRepository` | Consulta de roles asignados al usuario |

---

### `inventario` — Productos e inventario

Gestiona el catálogo de productos del menú, el menú especial con modificaciones, los insumos y sus recetas, los movimientos manuales de inventario y el cambio de estado de productos e insumos.

| Capa | Responsabilidad |
|------|-----------------|
| `ProductoController` | Endpoints `/api/productos`: carta por categoría, menú especial con opciones y bebidas, búsqueda por nombre, cambio de estado de producto con evaluación de implicaciones |
| `MovimientoInventarioController` | Endpoints `/api/inventario`: búsqueda de ítems para ajuste, registro de movimiento ingreso/egreso |
| `InsumoEstadoController` | Endpoints `/api/inventario/insumos`: evaluar implicaciones de cambio de estado, cambiar estado de insumo |

| `ProductoService` | Construye la carta agrupada por categoría y la respuesta de menú especial con grupos de modificación |
| `EstadoInventarioService` | Evalúa implicaciones antes de cambiar estado de producto o insumo; ejecuta el cambio |
| `MovimientoInventarioService` | Registra ingresos y egresos manuales; actualiza stock de productos e insumos |
| `InventarioDescuentoService` | Descuenta stock automáticamente al enviar una comanda a producción, según las recetas |

| `ProductoRepository` / `CategoriaCartaRepository` | Catálogo de productos y categorías |
| `InsumoRepository` / `RecetaRepository` | Insumos y recetas que los vinculan a productos |
| `MovimientoInventarioRepository` | Registro histórico de movimientos |
| `OpcionModificacionRepository` / `ProductoOpcionModificacionRepository` | Opciones de modificación del menú especial |
| `MenuBebidaDisponibleRepository` | Bebidas disponibles por menú especial |

---

### `mesas_comandas` — Mesas y comandas

Gestiona el mapa de mesas en tiempo real, la apertura y estado de visitas, el borrador de comandas del mesero y el flujo de producción en cocina y barra.

| Capa | Responsabilidad |
|------|-----------------|
| `ComandaController` | Endpoints `/api/comandas/borrador`: obtener borrador, agregar ítem, modificar ítem, eliminar ítem, enviar a producción, cancelar formulario |
| `ComandaProduccionController` | Endpoints `/api/comandas/produccion`: tablero por estación, detalle de comanda, iniciar preparación, marcar listo |
| `MesaController` | Endpoints `/api/mesas`: mapa de mesas, zonas disponibles para asignación, detalle de mesa, ítems en producción; asignar mesa con o sin reserva |
| `VisitaController` | Endpoints `/api/visitas`: historial de visitas del cliente, detalle de visita activa, solicitar asistencia, ajustar ítems de cuenta |

| `MesaService` | Construye el mapa de mesas por zona; evalúa y actualiza estados de mesa |
| `MesaAsignarService` | Valida reserva, asigna mesa, transiciona comandas `PRE_RESERVA → PENDIENTE` |
| `ComandaBorradorService` | Gestiona el borrador (agregar/modificar/eliminar ítems, split COCINA/BARRA); valida stock mediante `ComandaBorradorValidador`; publica a RabbitMQ al enviar a producción |
| `ComandaProduccionService` | Provee el tablero y detalle por estación; gestiona transiciones `PENDIENTE→EN_PREPARACION→LISTO` |
| `VisitaService` | Consulta historial y detalle de visitas; lógica de ajuste de ítems de cuenta |
| `VisitaEstadoService` | Consulta el estado activo de la visita; notifica cambios al cliente vía WS |
| `CuentaAjusteService` | Elimina ítems de la cuenta preliminar antes del cierre |
| `EstacionResolver` | Resuelve la(s) estación(es) accesibles según el rol del usuario autenticado |
| `MesaValidador` | Aplica reglas de negocio antes de asignar mesa (horario de atención, estado de reserva) |
| `MesaWsPublisher` | Publica eventos de mapa de mesas al tópico `/topic/mesas` |
| `VisitaEventoPublisher` | Publica estado de visita al tópico `/topic/visita/{id}/orden` |

| `MesaRepository` / `ZonaRepository` | Mesas y zonas del restaurante |
| `ComandaRepository` / `ComandaItemRepository` | Comandas y sus ítems con estados |
| `ComandaMenuModificacionRepository` | Modificaciones libres de ítems de menú especial |
| `VisitaRepository` | Visitas activas e historial |

---

### `notificaciones` — Notificaciones

Persiste alertas de mesa y distribuye eventos en tiempo real a través de WebSocket y RabbitMQ.

| Capa | Responsabilidad |
|------|-----------------|
| `NotificacionController` | Endpoints `/api/notificaciones`: atender asistencia, atender notificación de platos listos, notificar cambio de mesero |

| `NotificacionService` | Crea y persiste notificaciones de tipo `ATENCION`, `PLATOS_LISTOS`, `BEBIDAS_LISTAS`, `CAMBIO`; marca notificaciones como `ATENDIDA` |
| `NotificacionWsPublisher` | Publica mensajes STOMP a los tópicos `/topic/mesas/asistencia`, `/topic/visita/{id}/asistencia` y `/topic/visita/{id}/orden` |

| `NotificacionRepository` | Consulta de notificaciones activas por visita y tipo |

---

### `pagos_caja` — Caja y pagos

Gestiona la cuenta preliminar y el cierre de venta, incluyendo el cálculo de totales, la aplicación de abonos y la acumulación de puntos.

| Capa | Responsabilidad |
|------|-----------------|
| `CuentaController` | Endpoint `GET /api/ventas/{visitaId}/cuenta`: cuenta preliminar con ítems, totales, abonos, saldo pendiente y puntos del cliente |
| `VentaController` | Endpoint `POST /api/ventas`: registra venta final y cierra la cuenta |

| `CuentaService` | Consolida ítems consumidos, calcula subtotal, descuentos y saldo pendiente |
| `VentaService` | Crea la entidad `Venta`, cierra la `Visita`, actualiza puntos del cliente vía `PuntosService`, publica evento WS de cuenta cerrada |

| `VentaRepository` | Registro de ventas cerradas |
| `AbonoRepository` | Consulta de abonos de reserva para calcular saldo neto |

---

### `reportes` — Reportes administrativos

Provee consultas históricas de ventas por cliente y detalle administrativo de visitas cerradas para el rol ADMIN.

| Capa | Responsabilidad |
|------|-----------------|
| `ClienteVentasAdminController` | Endpoints `/api/clientes/{id}/ventas`: historial completo y resumen de ventas por cliente, agrupado por año y mes |
| `VentaDetalleAdminController` | Endpoints administrativos de detalle de visita: ítems consumidos, métodos de pago y totales |

| `ClienteVentasAdminService` | Agrega ventas históricas por cliente con agrupación por período |
| `ClienteRecordatorioService` | Consulta clientes para recordatorios y seguimiento |
| `VentaDetalleAdminService` | Construye el detalle administrativo de una visita cerrada |

| `ClienteAdminRepository` | Consultas de clientes con filtros para administración |
| `VentaAdminRepository` | Consultas de ventas históricas con agregaciones |
| `MesaDetalleAdminRepository` / `ComandaItemDetalleAdminRepository` | Detalle de mesas y comandas para reportes |

---

### `reservas` — Reservas

Gestiona el ciclo de vida completo de reservas: creación, modificación, cancelación, confirmación, inasistencia, abonos y pre-orden de comandas.

| Capa contro | Responsabilidad |
|------|-----------------|
| `ReservaController` | Endpoints `/api/reservas`: crear, modificar, confirmar, cancelar, inasistencia, registrar abono, resumen de pago, detalle, disponibilidad, lista |
| `ReservaConsultaController` | Endpoints `/api/reservas/mesero`: listado por fecha o identificador para MESERO/CAJERO/ADMIN |

| `ReservaService` | Orquesta estados (PENDIENTE→CONFIRMADA→ATENDIDA/CANCELADA/INASISTENCIA), valida ventanas horarias, gestiona abonos, delega en `PreOrdenGestor` |
| `ReservaConsultaService` | Listados históricos y consulta de detalle por ID o fecha |
| `PreOrdenGestor` | Valida productos activos y opciones de menú; crea y elimina comandas `PRE_RESERVA` con split COCINA/BARRA |
| `ReservaValidador` | Valida cutoff de modificación, capacidad de zona y elegibilidad para cada transición de estado |
| `DisponibilidadConsultador` | Evalúa disponibilidad de fechas, horas, zonas y decoraciones según bloqueos y reservas activas |

| `ReservaRepository` | Consultas de reservas activas, por cliente, por fecha y por estado |
| `BloqueDisponibilidadRepository` | Bloqueos de horarios |
| `DecoracionRepository` / `DecoracionZonaRepository` | Catálogo de decoraciones y asignación por zona |

---

### `usuarios` — Usuarios

Gestiona perfiles de clientes, administración de empleados y puntos de fidelización. Concentra la lógica de acumulación y canje de puntos por visita.

| Capa | Responsabilidad |
|------|-----------------|
| `ClienteProfileController` | Endpoints `/api/clientes/me`: obtener perfil, actualizar datos personales, cambiar contraseña |
| `ClienteController` | Endpoints `/api/clientes`: consulta de puntos, búsqueda por fragmento de nombre, canje de puntos |
| `EmpleadoController` | Endpoints `/api/empleados`: crear empleado, listar con filtros por rol/estado/nombre, cambiar estado |

| `ClienteProfileService` | Actualiza datos personales y contraseña; valida ownership |
| `PuntosService` | Consulta puntos actuales y acumulados; ejecuta el canje reduciendo `clientePuntos` |
| `ClienteBusquedaService` | Búsqueda accent-insensitive de clientes para el cajero |
| `EmpleadoService` | Crea empleados con roles asignados; filtra y cambia estado |

| `ClienteRepository` | Consulta de perfil y puntos por email o ID |
| `EmpleadoRepository` | Consulta de empleados con filtros |
| `CanjePuntosRepository` | Registro histórico de canjes |

---

## Interacciones entre módulos

Los módulos se comunican por llamada directa entre servicios Spring para operaciones síncronas, y por RabbitMQ para desacoplamiento en el envío a producción y notificaciones WebSocket.

### Flujos principales

**Creación de reserva con pre-orden**
- `ReservaController` recibe solicitud con ítems del cliente
- `ReservaService` delega en `PreOrdenGestor` la validación y persistencia
- `PreOrdenGestor` consulta `ProductoService` (producto activo, opción de menú válida, bebida disponible)
- `PreOrdenGestor` crea comandas `PRE_RESERVA` en `ComandaRepository` con split automático COCINA/BARRA

**Llegada del cliente — asignación de mesa**
- `MesaController` recibe solicitud con reservaId opcional
- `MesaAsignarService` lee la `Reserva` de `ReservaRepository`
- `MesaAsignarService` transiciona comandas `PRE_RESERVA → PENDIENTE` en `ComandaRepository`
- `MesaWsPublisher` publica actualización del mapa al tópico `/topic/mesas`

**Envío de comanda a producción**
- `ComandaController` recibe envío del borrador del mesero
- `ComandaBorradorService` valida stock mediante `ComandaBorradorValidador` e `InventarioDescuentoService`
- `ComandaBorradorService` publica mensaje a RabbitMQ (routing key `comanda.nueva`)
- `ComandaProduccionService` recibe el evento y actualiza el tablero de cocina o barra
- `ComandaProduccionEventoWsMessage` se envía al tópico `/topic/produccion/{cocina|barra}`

**Comanda lista — cocina o barra**
- `ComandaProduccionController` recibe acción "marcar listo" del COCINERO o BARTENDER
- `ComandaProduccionService` transiciona estado `EN_PREPARACION → LISTO`
- `NotificacionService` crea y persiste notificación de tipo `PLATOS_LISTOS` o `BEBIDAS_LISTAS`
- `NotificacionWsPublisher` publica alerta al tópico `/topic/visita/{id}/orden`

**Cierre de cuenta**
- `CuentaController` entrega cuenta preliminar al cajero
- `VentaController` recibe confirmación de pago del cajero
- `VentaService` crea la entidad `Venta`, cierra `Visita` en `VisitaRepository`
- `VentaService` invoca `PuntosService` para incrementar `clientePuntos` y `clientePuntosAcumulados`
- `VentaService` publica `CuentaCerradaWsMessage` al tópico `/topic/visita/{id}/cuenta`

**Autenticación transversal**
- `JwtAuthenticationFilter` intercepta cada petición antes del controller
- `CustomUserDetailsService` carga el usuario desde `UsuarioRepository`
- Verifica firma del token, expiración y que `sesionActiva=true` en `SesionRepository`

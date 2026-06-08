# WebSocket — Al Toro Gastrobar

El backend expone un endpoint STOMP sobre WebSocket en `/ws`. Los clientes se suscriben a tópicos `/topic/xxx` para recibir eventos del servidor; el prefijo `/app` se reserva para mensajes dirigidos a métodos `@MessageMapping` del servidor.

---

## Tabla de contenidos

- [Configuración](#configuración)
- [Tópicos del sistema](#tópicos-del-sistema)
- [Contratos de mensajes](#contratos-de-mensajes)
- [Reglas de uso](#reglas-de-uso)

---

## Configuración

| Parámetro | Valor |
|-----------|-------|
| Endpoint de conexión | `/ws` |
| Broker in-memory | `/topic` |
| Prefijo aplicación | `/app` |
| Protocolo | STOMP sobre WebSocket (con fallback SockJS) |
| Serialización | JSON (Jackson) |

El endpoint `/ws/**` es público — no requiere token JWT para establecer la conexión.

---

## Tópicos del sistema

| Tópico | Broadcast / Unicast | Suscriptores | Evento que lo dispara |
|--------|--------------------|--------------|-----------------------|
| `/topic/mesas` | Broadcast | Meseros, Cajeros | Cualquier cambio de estado de mesa o visita |
| `/topic/mesas/asistencia` | Broadcast | Todos los empleados | Cliente solicita asistencia |
| `/topic/visita/{visitaId}/orden` | Unicast por visita | Cliente de la visita | Mesero agrega, modifica o elimina ítems de comanda |
| `/topic/visita/{visitaId}/cuenta` | Unicast por visita | Cliente de la visita | Cajero cierra la cuenta |
| `/topic/visita/{visitaId}/asistencia` | Unicast por visita | Cliente de la visita | Mesero atiende la solicitud de asistencia |
| `/topic/reservas/cambios` | Broadcast | Meseros, Cajeros | Reserva creada, modificada, confirmada, cancelada o con abono |
| `/topic/produccion/cocina` | Broadcast | Cocineros | Evento de ciclo de vida de comanda en estación COCINA |
| `/topic/produccion/barra` | Broadcast | Bartenders | Evento de ciclo de vida de comanda en estación BARRA |
| `/topic/inventario` | Broadcast | Cualquier pantalla que muestre stock | Ajuste manual de inventario aplicado |

---

## Contratos de mensajes

### `/topic/mesas` — `MesaWsMessage`

Publicado por `MesaWsPublisher`.

```json
{
  "visitaId": 42,
  "tipoEvento": "ACTUALIZAR",
  "nuevoEstado": "ATENDIDA",
  "timestamp": 1717600000000
}
```

| Campo | Tipo | Valores posibles |
|-------|------|-----------------|
| `tipoEvento` | String | `CREAR`, `ACTUALIZAR`, `CERRAR`, `NOTIFICACION` |
| `nuevoEstado` | String | `ESPERA`, `EN_PREPARACION`, `ATENDIDA`, `CERRADA`; null si tipoEvento ≠ ACTUALIZAR |

---

### `/topic/mesas/asistencia` — `AsistenciaSolicitadaWsMessage`

Publicado por `NotificacionWsPublisher` cuando el cliente solicita asistencia.

```json
{
  "visitaId": 42,
  "notificacionId": 7,
  "mesaIdentificador": "M-03",
  "clienteNombre": "Paola Rojas",
  "fechaHora": "2026-06-05T19:30:00"
}
```

---

### `/topic/visita/{visitaId}/orden` — `VisitaActualizadaWsMessage`

Publicado por `NotificacionWsPublisher` cuando el mesero modifica la comanda de una visita activa.

```json
{
  "visitaId": 42,
  "items": [
    {
      "comandaItemId": 1,
      "nombreProducto": "Hamburguesa",
      "cantidad": 2,
      "precio": 28000,
      "descripcion": null
    }
  ],
  "total": 56000
}
```

---

### `/topic/visita/{visitaId}/cuenta` — `CuentaCerradaWsMessage`

Publicado por `NotificacionWsPublisher` al cerrar la cuenta. Permite que el frontend actualice el saldo de puntos del cliente sin request adicional.

```json
{
  "visitaId": 42,
  "mensaje": "Cuenta cerrada exitosamente",
  "puntosActuales": 5
}
```

---

### `/topic/visita/{visitaId}/asistencia` — `AsistenciaAtendidaWsMessage`

Publicado por `NotificacionWsPublisher` cuando el mesero marca la solicitud como atendida. Rehabilita el botón de asistencia en el frontend del cliente.

```json
{
  "visitaId": 42,
  "asistenciaAtendida": true
}
```

---

### `/topic/reservas/cambios` — `ReservaActualizadaWsMessage`

Publicado por `NotificacionWsPublisher` cuando cambia el estado de una reserva activa.

```json
{
  "reservaId": 15,
  "tipoEvento": "CONFIRMADA",
  "clienteNombre": "Paola Rojas",
  "horaLlegada": "19:00",
  "zonaNombre": "Terraza"
}
```

| Campo `tipoEvento` | Condición |
|--------------------|-----------|
| `CREADA` | Reserva nueva |
| `MODIFICADA` | Cambio de datos |
| `CONFIRMADA` | Cajero confirma |
| `CANCELADA` | Cancelación |
| `INASISTENCIA` | Marcada inasistencia |
| `ANTICIPO` | Abono registrado |
| `DEVOLUCION` | Devolución registrada |

---

### `/topic/produccion/cocina` y `/topic/produccion/barra` — `ComandaProduccionEventoWsMessage`

Contrato unificado para todos los eventos del ciclo de vida de comandas. El campo `resumen` viaja solo en `CREADA`; `nuevoEstado` viaja solo en `ACTUALIZADA`. Los campos `null` se omiten en la serialización JSON (`@JsonInclude(NON_NULL)`).

```json
{
  "tipo": "CREADA",
  "estacion": "COCINA",
  "comandaId": 88,
  "resumen": { "...": "ver ComandaProduccionResumenResponse" }
}
```

```json
{
  "tipo": "ACTUALIZADA",
  "estacion": "BARRA",
  "comandaId": 88,
  "nuevoEstado": "EN_PREPARACION"
}
```

| Campo `tipo` | Cuándo se emite | Campos presentes |
|--------------|-----------------|-----------------|
| `CREADA` | Mesero envía comanda a producción | `tipo`, `estacion`, `comandaId`, `resumen` |
| `ACTUALIZADA` | Cocinero/bartender cambia estado | `tipo`, `estacion`, `comandaId`, `nuevoEstado` |
| `ELIMINADA` | Comanda eliminada del tablero | `tipo`, `estacion`, `comandaId` |
| `COMPLETADA` | Comanda marcada COMPLETADO | `tipo`, `estacion`, `comandaId` |

---

### `/topic/inventario` — `StockActualizadoWsMessage`

Publicado por `InventarioWsPublisher` tras un ajuste manual de stock. Exactamente uno de `productoId` / `insumoId` viaja con valor.

```json
{
  "productoId": 36,
  "stockActual": 12.500
}
```

```json
{
  "insumoId": 5,
  "stockActual": 2.750
}
```

---

## Reglas de uso

1. **No crear tópicos nuevos** sin actualizar este documento y `CLAUDE.md`.
2. Publicar siempre desde un publisher dedicado — nunca inyectar `SimpMessagingTemplate` directamente en un service de negocio.
3. Los tópicos de visita (`/topic/visita/{visitaId}/...`) son unicast por convención: el cliente solo se suscribe al `visitaId` de su sesión activa.
4. El tópico `/topic/produccion/{estacion}` reemplaza al legado `/topic/comandas/completado`; no usar el legado.

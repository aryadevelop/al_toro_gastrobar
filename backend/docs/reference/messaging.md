# Mensajería — Al Toro Gastrobar

El backend usa RabbitMQ como broker de mensajería asíncrona para el procesamiento de comandas, impresión de tickets, correos transaccionales y distribución de notificaciones en tiempo real via WebSocket.

---

## Tabla de contenidos

- [Configuración](#configuración)
- [Topología](#topología)
- [Routing keys](#routing-keys)
- [Colas](#colas)
- [Publishers](#publishers)
- [Reglas de uso](#reglas-de-uso)

---

## Configuración

| Parámetro | Valor |
|-----------|-------|
| Imagen | `rabbitmq:3.13-management-alpine` |
| Puerto AMQP | 5672 (solo interno en producción) |
| Consola de administración | 15672 (expuesto en dev: `guest / guest`) |
| Exchange principal | `altoro.topic` (durable, tipo `topic`) |
| Serialización de mensajes | JSON via `Jackson2JsonMessageConverter` |

En producción, el broker no expone puertos al exterior. Las credenciales se configuran con `RABBITMQ_USERNAME` y `RABBITMQ_PASSWORD` en `.env.prod`.

---

## Topología

```
Publishers                Exchange               Queues              Consumers
─────────────────         ──────────────         ──────────────      ──────────────────────────
RabbitTemplate  ──────▶   altoro.topic   ──────▶  q.comanda.produccion  ──▶  ProduccionService
                          (topic,durable)
```

---

## Routing keys

| Constante | Valor | Descripción |
|-----------|-------|-------------|
| `RabbitMQConfig.RK_COMANDA_NUEVA` | `comanda.nueva` | Nueva comanda enviada a producción |

---

## Colas

| Constante | Nombre | Tipo | Estado |
|-----------|--------|------|--------|
| `RabbitMQConfig.Q_COMANDA_PRODUCCION` | `q.comanda.produccion` | durable | Activa — consumida por `ProduccionService` |

---

## Publishers

El único punto de publicación activo es `RabbitTemplate.convertAndSend(exchange, routingKey, payload)`. Los mensajes se serializan automáticamente a JSON.

Ejemplo de publicación:

```java
rabbitTemplate.convertAndSend(
    RabbitMQConfig.EXCHANGE,
    RabbitMQConfig.RK_COMANDA_NUEVA,
    comandaNuevaMessage
);
```

El payload `ComandaNuevaMessage` incluye el `comandaId` y la estación (`COCINA` o `BARRA`).

---

## Reglas de uso

1. **Siempre referenciar las constantes** de `RabbitMQConfig` — nunca usar strings literales.
2. **No crear exchanges adicionales** — toda la mensajería pasa por `altoro.topic`.
3. Agregar nuevas routing keys y colas como constantes en `RabbitMQConfig` antes de usarlas.
4. Las notificaciones WebSocket en tiempo real no pasan por RabbitMQ actualmente — se publican directamente con `SimpMessagingTemplate`. Ver `docs/reference/websocket.md`.

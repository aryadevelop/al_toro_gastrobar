# PA-96 — Estado de la Visita Activa y Solicitud de Asistencia

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar los endpoints REST y la infraestructura WebSocket para que un cliente con visita activa vea el estado de su visita en tiempo real y pueda solicitar la asistencia de un mesero (HE-02-HU-06, CA-01 a CA-06).

**Architecture:** El cliente autentica vía JWT y llama a `GET /api/visitas/activa` para obtener el estado actual de su visita (ítems, total, estado de asistencia). Las actualizaciones en tiempo real se entregan vía STOMP/WebSocket: el servidor publica en `/topic/visita/{visitaId}/orden` cuando cambia la comanda, en `/topic/visita/{visitaId}/cuenta` cuando se cierra la cuenta y en `/topic/visita/{visitaId}/asistencia` cuando el mesero atiende la solicitud. `POST /api/visitas/{visitaId}/asistencia` crea la notificación de atención y hace broadcast a `/topic/mesas/asistencia`. El mesero llama a `PATCH /api/notificaciones/{notificacionId}/atender` para marcar la solicitud como atendida y desbloquear el botón del cliente.

**Multi-role:** El endpoint `GET /api/visitas/activa` es accesible para `CLIENTE`, `MESERO`, `CAJERO` y `ADMIN`. Para `CLIENTE` el email se extrae del token. Para otros roles se requiere el parámetro `?emailCliente=`. Patrón tomado de `VisitaController.obtenerDetalleVisita`.

**Tech Stack:** Spring Boot 3.5, Java 21, Spring WebSocket (STOMP in-memory broker), Spring Security JWT, JPA/Hibernate, JUnit 5 + Mockito, AssertJ, Postman YAML.

---

## Dependency note (CA-02)

CA-02 requiere que el servidor empuje actualizaciones cuando el mesero modifica la comanda. La infraestructura WebSocket (`NotificacionWsPublisher`) se construye aquí; el call site concreto (`publicarVisitaActualizada(...)`) debe añadirse en el servicio de creación/modificación de ítems de comanda, que se implementará en la siguiente HU (creación de mesa). En este plan, el método está disponible para ser llamado; no se crea el call site porque el servicio destino no existe aún.

---

## File Map

### Archivos nuevos

| Path | Responsabilidad |
|------|-----------------|
| `backend/src/main/java/.../mesas_comandas/dto/response/EstadoVisitaResponse.java` | DTO de respuesta: estado completo de la visita activa |
| `backend/src/main/java/.../mesas_comandas/dto/response/ItemVisitaResponse.java` | DTO de ítem de visita con estado derivado ("En preparación"/"Servido") |
| `backend/src/main/java/.../mesas_comandas/mapper/VisitaEstadoMapper.java` | Mapper entity→DTO para EstadoVisitaResponse e ItemVisitaResponse |
| `backend/src/main/java/.../notificaciones/dto/ws/VisitaActualizadaWsMessage.java` | Payload WS para cambios de comanda |
| `backend/src/main/java/.../notificaciones/dto/ws/CuentaCerradaWsMessage.java` | Payload WS cuando se cierra la cuenta (CA-03); incluye `puntosActuales` |
| `backend/src/main/java/.../notificaciones/dto/ws/AsistenciaAtendidaWsMessage.java` | Payload WS al atender asistencia (CA-05) |
| `backend/src/main/java/.../notificaciones/dto/ws/AsistenciaSolicitadaWsMessage.java` | Payload WS broadcast a empleados (CA-04) |
| `backend/src/main/java/.../notificaciones/dto/response/NotificacionAsistenciaResponse.java` | DTO de respuesta al solicitar asistencia |
| `backend/src/main/java/.../notificaciones/repository/NotificacionRepository.java` | Repositorio JPA para Notificacion |
| `backend/src/main/java/.../notificaciones/service/NotificacionWsPublisher.java` | Wraps SimpMessagingTemplate; 4 métodos de publicación WS |
| `backend/src/main/java/.../notificaciones/service/NotificacionService.java` | Lógica de solicitar y atender asistencia |
| `backend/src/main/java/.../notificaciones/controller/NotificacionController.java` | `PATCH /api/notificaciones/{id}/atender` |
| `backend/src/main/java/.../mesas_comandas/service/VisitaEstadoService.java` | Lógica de estado de visita activa |
| `backend/src/test/java/.../mesas_comandas/service/VisitaEstadoServiceTest.java` | Tests unitarios de VisitaEstadoService |
| `backend/src/test/java/.../mesas_comandas/controller/VisitaControllerTest.java` | Tests unitarios de los nuevos endpoints en VisitaController |
| `backend/src/test/java/.../notificaciones/service/NotificacionServiceTest.java` | Tests unitarios de NotificacionService |
| `backend/src/test/java/.../notificaciones/controller/NotificacionControllerTest.java` | Tests unitarios de NotificacionController |

### Archivos modificados

| Path | Cambio |
|------|--------|
| `backend/src/main/java/.../mesas_comandas/repository/VisitaRepository.java` | Añadir `findActiveByClienteEmail` (JPQL con `visitaFechaHoraFin IS NULL`) |
| `backend/src/main/java/.../mesas_comandas/controller/VisitaController.java` | Añadir `GET /api/visitas/activa` y `POST /api/visitas/{id}/asistencia`; inyectar `VisitaEstadoService` y `NotificacionService` |
| `backend/src/main/java/.../pagos_caja/service/VentaService.java` | Inyectar `NotificacionWsPublisher`; al cerrar cuenta: setear `visitaFechaHoraFin`, publicar `CuentaCerradaWsMessage` con `puntosActuales` |
| `backend/CLAUDE.md` | Añadir regla de mappers y confirmar regla de documentación |

---

Paquete base: `co.edu.unicauca.backend`
Prefijo de módulo: `modules/`

---

## Task 1: NotificacionRepository + findActiveByClienteEmail

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/repository/NotificacionRepository.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/VisitaRepository.java`

- [ ] **Step 1: Añadir `findActiveByClienteEmail` a VisitaRepository**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/VisitaRepository.java
// Añadir imports:
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Dentro de la interfaz, añadir:

/**
 * Devuelve la visita activa del cliente (sin fecha de fin registrada).
 * Una visita es activa mientras {@code visitaFechaHoraFin} sea {@code null}.
 *
 * @param email correo del cliente autenticado
 * @return la visita activa del cliente, o empty si no tiene ninguna
 */
@Query("SELECT v FROM Visita v WHERE v.cliente.usuario.usuarioEmail = :email AND v.visitaFechaHoraFin IS NULL")
Optional<Visita> findActiveByClienteEmail(@Param("email") String email);
```

- [ ] **Step 2: Crear NotificacionRepository**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/repository/NotificacionRepository.java
package co.edu.unicauca.backend.modules.notificaciones.repository;

import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para {@link Notificacion}.
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Devuelve la primera notificación activa de un tipo concreto para la mesa de una visita.
     * Usado para verificar si ya existe una solicitud de asistencia sin atender.
     *
     * @param visitaId identificador de la visita (= PK de Mesa)
     * @param tipo     tipo de notificación a filtrar
     * @param estado   estado a filtrar
     * @return primera notificación que coincida, o empty
     */
    Optional<Notificacion> findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
            Long visitaId, TipoNotificacion tipo, EstadoNotificacion estado);
}
```

- [ ] **Step 3: Compilar para verificar sin errores**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```
Resultado esperado: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/repository/NotificacionRepository.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/VisitaRepository.java
git commit -m "feat(mesas_comandas): añadir NotificacionRepository y findActiveByClienteEmail en VisitaRepository"
```

---

## Task 2: DTOs de respuesta REST + VisitaEstadoMapper

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/EstadoVisitaResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemVisitaResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/response/NotificacionAsistenciaResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java`

- [ ] **Step 1: Crear ItemVisitaResponse**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemVisitaResponse.java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Ítem de la visita activa tal como se muestra al cliente en el dashboard.
 * El estado ("En preparación" / "Servido") se deriva del estado de la
 * {@link co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda} padre.
 */
@Getter
@Builder
public class ItemVisitaResponse {

    /** Identificador del ítem de comanda. */
    private final Long comandaItemId;

    /** Nombre del producto. */
    private final String nombreProducto;

    /** Unidades pedidas. */
    private final Integer cantidad;

    /**
     * Estado visible para el cliente: {@code "En preparación"} o {@code "Servido"}.
     * Derivado de {@code EstadoComanda}: PENDIENTE/EN_PREPARACION → "En preparación";
     * LISTO/COMPLETADO → "Servido".
     */
    private final String estadoItem;

    /** Precio capturado al momento del pedido; no varía si el catálogo cambia. */
    private final BigDecimal precioUnitario;

    /** {@code precioUnitario × cantidad}. */
    private final BigDecimal subtotal;
}
```

- [ ] **Step 2: Crear EstadoVisitaResponse**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/EstadoVisitaResponse.java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estado completo de la visita activa del cliente.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstadoVisitaResponse {

    /** Identificador de la visita activa. */
    private final Long visitaId;

    /** Etiqueta física de la mesa; {@code null} si el mesero no ha asignado mesa aún. */
    private final String mesaIdentificador;

    /** {@code true} si la cuenta ya fue cerrada (visitaFechaHoraFin != null). */
    private final boolean visitaCerrada;

    /** Lista de ítems de la visita (todas las comandas activas). */
    private final List<ItemVisitaResponse> items;

    /** Suma de todos los subtotales; {@code BigDecimal.ZERO} si no hay ítems. */
    private final BigDecimal total;

    /**
     * {@code true} si hay una notificación ATENCION ACTIVA para esta mesa.
     * El frontend usa este campo al recargar para saber si el botón está deshabilitado.
     */
    private final boolean asistenciaSolicitada;

    /**
     * ID de la notificación de asistencia activa; {@code null} si no hay ninguna.
     */
    private final Long notificacionAsistenciaId;
}
```

- [ ] **Step 3: Crear NotificacionAsistenciaResponse**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/response/NotificacionAsistenciaResponse.java
package co.edu.unicauca.backend.modules.notificaciones.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Respuesta al cliente cuando solicita asistencia de un mesero.
 */
@Getter
@Builder
public class NotificacionAsistenciaResponse {

    /** ID de la notificación creada. */
    private final Long notificacionId;

    /** Estado de la notificación: siempre "ACTIVA" al crearse. */
    private final String estado;
}
```

- [ ] **Step 4: Crear VisitaEstadoMapper**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java
package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Mapper para construir las respuestas del estado de visita activa.
 *
 * <p>Centraliza toda la lógica de conversión entity→DTO para que
 * {@link co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaEstadoService}
 * no contenga lógica de presentación.
 */
@Component
public class VisitaEstadoMapper {

    /**
     * Construye {@link ItemVisitaResponse} desde un ítem de comanda.
     *
     * @param item    ítem de comanda con producto y precio
     * @param estado  estado de la comanda padre (determina "En preparación" o "Servido")
     * @return DTO listo para serializar
     */
    public ItemVisitaResponse toItemVisitaResponse(ComandaItem item, EstadoComanda estado) {
        BigDecimal subtotal = item.getComandaItemPrecio()
                .multiply(BigDecimal.valueOf(item.getComandaItemCantidad()));

        return ItemVisitaResponse.builder()
                .comandaItemId(item.getComandaItemId())
                .nombreProducto(item.getProducto().getProductoNombre())
                .cantidad(item.getComandaItemCantidad())
                .estadoItem(resolverEstadoItem(estado))
                .precioUnitario(item.getComandaItemPrecio())
                .subtotal(subtotal)
                .build();
    }

    /**
     * Construye {@link EstadoVisitaResponse} con todos los campos calculados.
     *
     * @param visita              visita activa
     * @param mesaIdentificador   etiqueta de la mesa; {@code null} si no hay mesa asignada
     * @param items               lista de ítems ya mapeados
     * @param asistenciaActiva    notificación activa, o empty si no hay solicitud pendiente
     * @return DTO completo del estado de la visita
     */
    public EstadoVisitaResponse toEstadoVisitaResponse(
            Visita visita,
            String mesaIdentificador,
            List<ItemVisitaResponse> items,
            Optional<Notificacion> asistenciaActiva) {

        BigDecimal total = items.stream()
                .map(ItemVisitaResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EstadoVisitaResponse.builder()
                .visitaId(visita.getVisitaId())
                .mesaIdentificador(mesaIdentificador)
                .visitaCerrada(visita.getVisitaFechaHoraFin() != null)
                .items(items)
                .total(total)
                .asistenciaSolicitada(asistenciaActiva.isPresent())
                .notificacionAsistenciaId(
                        asistenciaActiva.map(Notificacion::getNotificacionId).orElse(null))
                .build();
    }

    /**
     * Convierte el estado interno de la comanda al texto visible para el cliente.
     * LISTO y COMPLETADO se muestran como "Servido"; cualquier otro estado como "En preparación".
     */
    private String resolverEstadoItem(EstadoComanda estado) {
        return (estado == EstadoComanda.LISTO || estado == EstadoComanda.COMPLETADO)
                ? "Servido"
                : "En preparación";
    }
}
```

- [ ] **Step 5: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```
Resultado esperado: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/EstadoVisitaResponse.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemVisitaResponse.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/response/NotificacionAsistenciaResponse.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java
git commit -m "feat(mesas_comandas): añadir EstadoVisitaResponse, ItemVisitaResponse, NotificacionAsistenciaResponse y VisitaEstadoMapper"
```

---

## Task 3: DTOs de mensajes WebSocket

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/VisitaActualizadaWsMessage.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/CuentaCerradaWsMessage.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/AsistenciaAtendidaWsMessage.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/AsistenciaSolicitadaWsMessage.java`

- [ ] **Step 1: Crear VisitaActualizadaWsMessage**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/VisitaActualizadaWsMessage.java
package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mensaje WebSocket publicado en {@code /topic/visita/{visitaId}/orden} cuando
 * el mesero agrega o modifica ítems de comanda.
 *
 * <p>El call site se añadirá en la HU de creación de visita (mesa).
 */
@Getter
@Builder
public class VisitaActualizadaWsMessage {

    private final Long visitaId;
    private final List<ItemVisitaResponse> items;
    private final BigDecimal total;
}
```

- [ ] **Step 2: Crear CuentaCerradaWsMessage**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/CuentaCerradaWsMessage.java
package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import lombok.Builder;
import lombok.Getter;

/**
 * Mensaje WebSocket publicado en {@code /topic/visita/{visitaId}/cuenta}
 * cuando el cajero cierra la cuenta.
 *
 * <p>El frontend usa {@code puntosActuales} para actualizar en tiempo real el
 * saldo de puntos del cliente sin necesidad de un request adicional.
 * El campo {@code mensaje} se muestra como popup de confirmación.
 */
@Getter
@Builder
public class CuentaCerradaWsMessage {

    private final Long visitaId;
    private final String mensaje;

    /**
     * Saldo de puntos del cliente después del cierre (incluye el +1 de esta visita).
     * Permite al frontend actualizar el indicador de puntos en tiempo real.
     */
    private final Integer puntosActuales;
}
```

- [ ] **Step 3: Crear AsistenciaAtendidaWsMessage**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/AsistenciaAtendidaWsMessage.java
package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import lombok.Builder;
import lombok.Getter;

/**
 * Mensaje WebSocket publicado en {@code /topic/visita/{visitaId}/asistencia}
 * cuando el mesero marca la solicitud como atendida.
 * El frontend re-habilita el botón "Solicitar asistencia".
 */
@Getter
@Builder
public class AsistenciaAtendidaWsMessage {

    private final Long visitaId;
    private final boolean asistenciaAtendida;
}
```

- [ ] **Step 4: Crear AsistenciaSolicitadaWsMessage**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/AsistenciaSolicitadaWsMessage.java
package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Mensaje WebSocket publicado en {@code /topic/mesas/asistencia} para
 * todos los empleados conectados cuando el cliente solicita asistencia.
 */
@Getter
@Builder
public class AsistenciaSolicitadaWsMessage {

    private final Long visitaId;
    private final Long notificacionId;
    private final String mesaIdentificador;
    private final String clienteNombre;
    private final LocalDateTime fechaHora;
}
```

- [ ] **Step 5: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```
Resultado esperado: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/
git commit -m "feat(notificaciones): añadir mensajes DTO WebSocket para estado de visita y asistencia"
```

---

## Task 4: NotificacionWsPublisher

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionWsPublisher.java`

- [ ] **Step 1: Crear NotificacionWsPublisher**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionWsPublisher.java
package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.CuentaCerradaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica mensajes WebSocket a los tópicos STOMP del sistema.
 *
 * <p>Tópicos:
 * <ul>
 *   <li>{@code /topic/visita/{visitaId}/orden} — actualización de ítems de comanda.</li>
 *   <li>{@code /topic/visita/{visitaId}/cuenta} — cuenta cerrada (incluye puntos actualizados).</li>
 *   <li>{@code /topic/visita/{visitaId}/asistencia} — asistencia atendida (CA-05).</li>
 *   <li>{@code /topic/mesas/asistencia} — broadcast a empleados.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class NotificacionWsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifica al cliente que la lista de ítems de su visita cambió.
     * Call site: servicio de creación/modificación de comanda (HU siguiente).
     */
    public void publicarVisitaActualizada(Long visitaId, VisitaActualizadaWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/visita/" + visitaId + "/orden", mensaje);
    }

    /**
     * Notifica al cliente que la cuenta fue cerrada.
     * El mensaje incluye {@code puntosActuales} para que el frontend actualice el saldo.
     * Call site: VentaService.cerrarCuenta.
     */
    public void publicarCuentaCerrada(Long visitaId, CuentaCerradaWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/visita/" + visitaId + "/cuenta", mensaje);
    }

    /**
     * Notifica al cliente que su solicitud de asistencia fue atendida.
     * Call site: NotificacionService.atenderAsistencia.
     */
    public void publicarAsistenciaAtendida(Long visitaId, AsistenciaAtendidaWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/visita/" + visitaId + "/asistencia", mensaje);
    }

    /**
     * Broadcast a todos los empleados conectados que hay una nueva solicitud de asistencia.
     * Call site: NotificacionService.solicitarAsistencia.
     */
    public void publicarAsistenciaSolicitada(AsistenciaSolicitadaWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/mesas/asistencia", mensaje);
    }
}
```

- [ ] **Step 2: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```
Resultado esperado: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionWsPublisher.java
git commit -m "feat(notificaciones): añadir NotificacionWsPublisher con 4 métodos de publicación STOMP"
```

---

## Task 5: VisitaEstadoService + nuevos endpoints en VisitaController

**Nota:** Los nuevos endpoints `GET /api/visitas/activa` y `POST /api/visitas/{id}/asistencia` se añaden directamente a `VisitaController` (mismo path base `/api/visitas`) en lugar de crear un controlador separado.

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoService.java`
- Create (test): `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoServiceTest.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaController.java`

- [ ] **Step 1: Escribir test fallido para VisitaEstadoService**

```java
// backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoServiceTest.java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VisitaEstadoService")
class VisitaEstadoServiceTest {

    @Mock VisitaRepository visitaRepository;
    @Mock MesaRepository mesaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock NotificacionRepository notificacionRepository;
    @Spy  VisitaEstadoMapper visitaEstadoMapper;

    @InjectMocks VisitaEstadoService visitaEstadoService;

    private static final String EMAIL = "cliente@test.com";
    private static final Long VISITA_ID = 10L;

    private Visita visitaActiva() {
        return Visita.builder().visitaId(VISITA_ID).build();
    }

    private Comanda comanda(Long id, EstadoComanda estado) {
        return Comanda.builder()
                .comandaId(id)
                .comandaEstado(estado)
                .comandaEstacion(EstacionComanda.COCINA)
                .build();
    }

    private ComandaItem item(Long id, Comanda comanda, String nombre, int qty, BigDecimal precio) {
        Producto producto = Producto.builder().productoNombre(nombre).build();
        return ComandaItem.builder()
                .comandaItemId(id)
                .comanda(comanda)
                .producto(producto)
                .comandaItemCantidad(qty)
                .comandaItemPrecio(precio)
                .build();
    }

    @Nested
    @DisplayName("obtenerEstadoVisitaActiva")
    class ObtenerEstadoVisitaActiva {

        @Test
        @DisplayName("retorna estado con ítems y total cuando la visita está activa")
        void retornaEstadoConItems() {
            Visita visita = visitaActiva();
            Mesa mesa = Mesa.builder().visitaId(VISITA_ID).mesaIdentificador("T-01").build();
            Comanda c1 = comanda(1L, EstadoComanda.EN_PREPARACION);
            ComandaItem i1 = item(100L, c1, "Bandeja", 2, new BigDecimal("18000"));

            when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(comandaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(List.of(c1));
            when(comandaItemRepository.findByComanda_ComandaId(1L)).thenReturn(List.of(i1));
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());

            EstadoVisitaResponse res = visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL);

            assertThat(res.getVisitaId()).isEqualTo(VISITA_ID);
            assertThat(res.getMesaIdentificador()).isEqualTo("T-01");
            assertThat(res.isVisitaCerrada()).isFalse();
            assertThat(res.getItems()).hasSize(1);
            assertThat(res.getItems().get(0).getEstadoItem()).isEqualTo("En preparación");
            assertThat(res.getItems().get(0).getSubtotal()).isEqualByComparingTo("36000");
            assertThat(res.getTotal()).isEqualByComparingTo("36000");
            assertThat(res.isAsistenciaSolicitada()).isFalse();
        }

        @Test
        @DisplayName("estadoItem es 'Servido' cuando la comanda está en LISTO")
        void estadoItemServidoCuandoListo() {
            Visita visita = visitaActiva();
            Comanda c1 = comanda(2L, EstadoComanda.LISTO);
            ComandaItem i1 = item(101L, c1, "Limonada", 1, new BigDecimal("8000"));

            when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(comandaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(List.of(c1));
            when(comandaItemRepository.findByComanda_ComandaId(2L)).thenReturn(List.of(i1));
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());

            EstadoVisitaResponse res = visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL);

            assertThat(res.getItems().get(0).getEstadoItem()).isEqualTo("Servido");
        }

        @Test
        @DisplayName("asistenciaSolicitada es true cuando hay notificación ATENCION ACTIVA")
        void asistenciaSolicitadaCuandoNotificacionActiva() {
            Visita visita = visitaActiva();
            Notificacion notif = Notificacion.builder()
                    .notificacionId(99L)
                    .notificacionTipo(TipoNotificacion.ATENCION)
                    .notificacionEstado(EstadoNotificacion.ACTIVA)
                    .build();

            when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(comandaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(List.of());
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.of(notif));

            EstadoVisitaResponse res = visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL);

            assertThat(res.isAsistenciaSolicitada()).isTrue();
            assertThat(res.getNotificacionAsistenciaId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("lanza BusinessException cuando el cliente no tiene visita activa")
        void lanzaExcepcionSinVisitaActiva() {
            when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("total es ZERO cuando no hay ítems en la visita")
        void totalCeroCuandoSinItems() {
            Visita visita = visitaActiva();

            when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(comandaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(List.of());
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());

            EstadoVisitaResponse res = visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL);

            assertThat(res.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
```

- [ ] **Step 2: Ejecutar test y verificar que falla (VisitaEstadoService no existe)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=VisitaEstadoServiceTest -q 2>&1 | tail -5
```
Resultado esperado: COMPILATION ERROR — `VisitaEstadoService` no existe.

- [ ] **Step 3: Implementar VisitaEstadoService**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoService.java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la consulta del estado de la visita activa del cliente.
 *
 * <p>Permite al cliente (y a empleados autorizados) ver los ítems pedidos,
 * el total acumulado y si hay una solicitud de asistencia pendiente.
 * La conversión entity→DTO se delega a {@link VisitaEstadoMapper}.
 *
 * @see EstadoVisitaResponse
 * @see VisitaEstadoMapper
 */
@Service
@RequiredArgsConstructor
public class VisitaEstadoService {

    private final VisitaRepository visitaRepository;
    private final MesaRepository mesaRepository;
    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final NotificacionRepository notificacionRepository;
    private final VisitaEstadoMapper visitaEstadoMapper;

    /**
     * Devuelve el estado completo de la visita activa identificada por el email del cliente.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Busca la visita con {@code visitaFechaHoraFin IS NULL} para el email dado.</li>
     *   <li>Recupera la mesa asignada (puede ser null si el mesero aún no la asignó).</li>
     *   <li>Consolida los ítems de todas las comandas, excluyendo estado PRE_RESERVA.</li>
     *   <li>Verifica si existe una notificación de asistencia ACTIVA para la mesa.</li>
     *   <li>Delega la construcción del DTO al mapper.</li>
     * </ol>
     *
     * @param emailCliente correo del cliente cuya visita activa se consulta
     * @return estado de la visita activa con ítems, total y estado de asistencia
     * @throws BusinessException con HTTP 404 si el cliente no tiene visita activa
     */
    @Transactional(readOnly = true)
    public EstadoVisitaResponse obtenerEstadoVisitaActiva(String emailCliente) {

        // Busca la visita activa del cliente (sin fecha de fin)
        Visita visita = visitaRepository.findActiveByClienteEmail(emailCliente)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BUSINESS_ERROR,
                        "No tienes una visita activa en este momento.",
                        HttpStatus.NOT_FOUND));

        Long visitaId = visita.getVisitaId();

        // La mesa puede no estar asignada aún si el mesero no la ha registrado
        Optional<Mesa> mesaOpt = mesaRepository.findByVisita_VisitaId(visitaId);
        String mesaIdentificador = mesaOpt.map(Mesa::getMesaIdentificador).orElse(null);

        // Obtiene todas las comandas de la visita
        List<Comanda> comandas = comandaRepository.findByVisita_VisitaId(visitaId);

        // Mapea ítems excluyendo comandas en estado PRE_RESERVA (pendientes de inicio de visita)
        List<ItemVisitaResponse> items = comandas.stream()
                .filter(c -> c.getComandaEstado() != EstadoComanda.PRE_RESERVA)
                .flatMap(c -> comandaItemRepository.findByComanda_ComandaId(c.getComandaId())
                        .stream()
                        .map(item -> visitaEstadoMapper.toItemVisitaResponse(item, c.getComandaEstado())))
                .collect(Collectors.toList());

        // Verifica si hay una solicitud de asistencia sin atender para esta mesa
        Optional<Notificacion> asistenciaActiva = notificacionRepository
                .findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                        visitaId, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA);

        return visitaEstadoMapper.toEstadoVisitaResponse(visita, mesaIdentificador, items, asistenciaActiva);
    }
}
```

- [ ] **Step 4: Ejecutar tests y verificar que pasan**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=VisitaEstadoServiceTest -q 2>&1 | tail -5
```
Resultado esperado: `Tests run: 5, Failures: 0, Errors: 0`.

- [ ] **Step 5: Añadir endpoints a VisitaController**

Abrir `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaController.java`.

Añadir imports:
```java
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaEstadoService;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionService;
import org.springframework.http.HttpStatus;
```

Añadir campos al controlador:
```java
private final VisitaEstadoService visitaEstadoService;
private final NotificacionService notificacionService;
```

Añadir los dos nuevos métodos dentro de la clase `VisitaController`:

```java
/**
 * Retorna el estado actual de la visita activa del cliente o de la visita indicada.
 *
 * <p>Si el solicitante tiene rol {@code CLIENTE}, el email se extrae del token y se
 * consulta su propia visita activa. Para otros roles ({@code MESERO}, {@code CAJERO},
 * {@code ADMIN}), el parámetro {@code emailCliente} es obligatorio y permite consultar
 * la visita activa de cualquier cliente.
 *
 * @param emailCliente   correo del cliente a consultar; ignorado si el solicitante es CLIENTE
 * @param authentication contexto de seguridad del request
 * @return estado de la visita activa con ítems, total y estado de asistencia
 */
@GetMapping("/activa")
@PreAuthorize("hasAnyRole('CLIENTE', 'MESERO', 'CAJERO', 'ADMIN')")
@Operation(summary = "Obtener estado de la visita activa")
public ResponseEntity<ApiResponse<EstadoVisitaResponse>> obtenerEstadoVisitaActiva(
        @RequestParam(required = false) String emailCliente,
        Authentication authentication) {

    boolean esCliente = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));

    // CLIENTE siempre consulta su propia visita; otros roles usan el parámetro emailCliente
    String emailAConsultar = esCliente ? authentication.getName() : emailCliente;

    if (emailAConsultar == null || emailAConsultar.isBlank()) {
        throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                "El parámetro emailCliente es requerido para este rol.", HttpStatus.BAD_REQUEST);
    }

    EstadoVisitaResponse response = visitaEstadoService.obtenerEstadoVisitaActiva(emailAConsultar);
    return ResponseEntity.ok(ApiResponse.ok(response));
}

/**
 * Solicita la asistencia de un mesero para la mesa de la visita activa.
 *
 * <p>Crea una notificación {@code ATENCION} en la DB y publica un broadcast
 * WebSocket en {@code /topic/mesas/asistencia} para todos los empleados conectados.
 * Devuelve 409 si ya existe una solicitud activa para la misma mesa (CA-04).
 *
 * @param visitaId       identificador de la visita activa del cliente
 * @param authentication contexto de seguridad del request
 * @return DTO con el ID de la notificación creada
 */
@PostMapping("/{visitaId}/asistencia")
@PreAuthorize("hasRole('CLIENTE')")
@Operation(summary = "Solicitar asistencia de un mesero para la mesa actual")
public ResponseEntity<ApiResponse<NotificacionAsistenciaResponse>> solicitarAsistencia(
        @PathVariable Long visitaId,
        Authentication authentication) {

    NotificacionAsistenciaResponse response =
            notificacionService.solicitarAsistencia(visitaId, authentication.getName());

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("Solicitud de asistencia enviada.", response));
}
```

- [ ] **Step 6: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```
Nota: fallará si `NotificacionService` no existe. Avanzar al Task 6 para crearlo y volver a compilar.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoService.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaController.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoServiceTest.java
git commit -m "feat(mesas_comandas): implementar VisitaEstadoService y añadir endpoints activa/asistencia a VisitaController"
```

---

## Task 6: NotificacionService (solicitarAsistencia + atenderAsistencia)

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionService.java`
- Create (test): `backend/src/test/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionServiceTest.java`

- [ ] **Step 1: Escribir tests fallidos para NotificacionService**

```java
// backend/src/test/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionServiceTest.java
package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificacionService")
class NotificacionServiceTest {

    @Mock VisitaRepository visitaRepository;
    @Mock MesaRepository mesaRepository;
    @Mock NotificacionRepository notificacionRepository;
    @Mock NotificacionWsPublisher wsPublisher;

    @InjectMocks NotificacionService notificacionService;

    private static final Long VISITA_ID = 10L;
    private static final String EMAIL = "cliente@test.com";

    private Visita visitaConCliente() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioEmail(EMAIL);
        Cliente cliente = Cliente.builder().usuarioId(1L).clienteNombre("Juan").build();
        cliente.setUsuario(usuario);
        return Visita.builder().visitaId(VISITA_ID).cliente(cliente).build();
    }

    private Mesa mesaConMesero() {
        Empleado mesero = Empleado.builder().usuarioId(5L).build();
        return Mesa.builder().visitaId(VISITA_ID).mesaIdentificador("T-01").mesero(mesero).build();
    }

    @Nested
    @DisplayName("solicitarAsistencia")
    class SolicitarAsistencia {

        @Test
        @DisplayName("crea notificación ACTIVA y publica broadcast WS")
        void creaNotificacionYPublicaBroadcast() {
            Visita visita = visitaConCliente();
            Mesa mesa = mesaConMesero();

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());
            Notificacion saved = Notificacion.builder().notificacionId(50L)
                    .notificacionEstado(EstadoNotificacion.ACTIVA)
                    .notificacionTipo(TipoNotificacion.ATENCION)
                    .mesa(mesa).empleado(mesa.getMesero()).build();
            when(notificacionRepository.save(any())).thenReturn(saved);

            NotificacionAsistenciaResponse res = notificacionService.solicitarAsistencia(VISITA_ID, EMAIL);

            assertThat(res.getNotificacionId()).isEqualTo(50L);
            assertThat(res.getEstado()).isEqualTo("ACTIVA");

            ArgumentCaptor<AsistenciaSolicitadaWsMessage> captor =
                    ArgumentCaptor.forClass(AsistenciaSolicitadaWsMessage.class);
            verify(wsPublisher).publicarAsistenciaSolicitada(captor.capture());
            assertThat(captor.getValue().getVisitaId()).isEqualTo(VISITA_ID);
            assertThat(captor.getValue().getMesaIdentificador()).isEqualTo("T-01");
        }

        @Test
        @DisplayName("lanza BusinessException si ya hay solicitud activa (CA-04 no duplicar)")
        void lanzaExcepcionSiYaHaySolicitudActiva() {
            Visita visita = visitaConCliente();
            Mesa mesa = mesaConMesero();
            Notificacion activa = Notificacion.builder().notificacionId(1L)
                    .notificacionEstado(EstadoNotificacion.ACTIVA).build();

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.of(activa));

            assertThatThrownBy(() -> notificacionService.solicitarAsistencia(VISITA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class);
            verify(notificacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza BusinessException si el cliente no es dueño de la visita")
        void lanzaExcepcionSiClienteNoEsDueno() {
            Visita visita = visitaConCliente();
            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));

            assertThatThrownBy(() -> notificacionService.solicitarAsistencia(VISITA_ID, "otro@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("lanza BusinessException si la visita no tiene mesa asignada")
        void lanzaExcepcionSinMesaAsignada() {
            Visita visita = visitaConCliente();

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificacionService.solicitarAsistencia(VISITA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("atenderAsistencia")
    class AtenderAsistencia {

        @Test
        @DisplayName("marca notificación como ATENDIDA y publica WS al cliente (CA-05)")
        void marcaAtendidaYPublicaWs() {
            Mesa mesa = mesaConMesero();
            Notificacion notif = Notificacion.builder()
                    .notificacionId(50L)
                    .notificacionEstado(EstadoNotificacion.ACTIVA)
                    .notificacionTipo(TipoNotificacion.ATENCION)
                    .mesa(mesa).empleado(mesa.getMesero()).build();

            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(notif));
            when(notificacionRepository.save(any())).thenReturn(notif);

            notificacionService.atenderAsistencia(50L, "mesero@test.com");

            assertThat(notif.getNotificacionEstado()).isEqualTo(EstadoNotificacion.ATENDIDA);

            ArgumentCaptor<AsistenciaAtendidaWsMessage> captor =
                    ArgumentCaptor.forClass(AsistenciaAtendidaWsMessage.class);
            verify(wsPublisher).publicarAsistenciaAtendida(eq(VISITA_ID), captor.capture());
            assertThat(captor.getValue().isAsistenciaAtendida()).isTrue();
        }

        @Test
        @DisplayName("lanza BusinessException si la notificación ya fue atendida")
        void lanzaExcepcionSiYaAtendida() {
            Mesa mesa = mesaConMesero();
            Notificacion notif = Notificacion.builder()
                    .notificacionId(50L)
                    .notificacionEstado(EstadoNotificacion.ATENDIDA)
                    .mesa(mesa).empleado(mesa.getMesero()).build();

            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(notif));

            assertThatThrownBy(() -> notificacionService.atenderAsistencia(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException si la notificación no existe")
        void lanzaNotFoundSiNotificacionNoExiste() {
            when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificacionService.atenderAsistencia(99L, "mesero@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
```

- [ ] **Step 2: Ejecutar test y verificar que falla**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=NotificacionServiceTest -q 2>&1 | tail -5
```
Resultado esperado: COMPILATION ERROR — `NotificacionService` no existe.

- [ ] **Step 3: Implementar NotificacionService**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionService.java
package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de negocio para solicitar y atender asistencia de mesero en mesa.
 *
 * <p>Coordina la persistencia de {@link Notificacion} y la publicación
 * de eventos WebSocket vía {@link NotificacionWsPublisher}.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final VisitaRepository visitaRepository;
    private final MesaRepository mesaRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionWsPublisher wsPublisher;

    /**
     * Registra una solicitud de asistencia para la mesa de la visita indicada.
     *
     * <p>Valida que:
     * <ul>
     *   <li>La visita exista y pertenezca al cliente autenticado.</li>
     *   <li>La visita tenga una mesa asignada.</li>
     *   <li>No exista ya una solicitud de asistencia ACTIVA para esa mesa.</li>
     * </ul>
     * Al completar, persiste la notificación y publica broadcast en
     * {@code /topic/mesas/asistencia}.
     *
     * @param visitaId     identificador de la visita activa del cliente
     * @param emailCliente correo del cliente autenticado
     * @return DTO con el ID de la notificación creada y su estado
     * @throws ResourceNotFoundException si la visita no existe
     * @throws BusinessException         si el cliente no es dueño, sin mesa, o ya hay solicitud activa
     */
    @Transactional
    public NotificacionAsistenciaResponse solicitarAsistencia(Long visitaId, String emailCliente) {

        // Verifica que la visita exista
        Visita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Visita", visitaId));

        // Solo el cliente dueño de la visita puede solicitar asistencia
        boolean esDelCliente = visita.getCliente() != null &&
                emailCliente.equals(visita.getCliente().getUsuario().getUsuarioEmail());
        if (!esDelCliente) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "No tienes acceso a esta visita.", HttpStatus.FORBIDDEN);
        }

        // No se puede duplicar una solicitud mientras haya una ACTIVA
        boolean asistenciaActiva = notificacionRepository
                .findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                        visitaId, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA)
                .isPresent();
        if (asistenciaActiva) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Ya existe una solicitud de asistencia activa para esta mesa.",
                    HttpStatus.CONFLICT);
        }

        // La visita debe tener mesa asignada para poder notificar al mesero
        Mesa mesa = mesaRepository.findByVisita_VisitaId(visitaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "Esta visita no tiene mesa asignada aún.",
                        HttpStatus.CONFLICT));

        // Persiste la notificación de asistencia
        Notificacion notificacion = Notificacion.builder()
                .mesa(mesa)
                .empleado(mesa.getMesero())
                .notificacionTipo(TipoNotificacion.ATENCION)
                .notificacionEstado(EstadoNotificacion.ACTIVA)
                .build();
        notificacion = notificacionRepository.save(notificacion);

        String clienteNombre = visita.getCliente() != null
                ? visita.getCliente().getClienteNombre()
                : "Cliente";

        // Broadcast a todos los empleados conectados con los datos de la mesa
        wsPublisher.publicarAsistenciaSolicitada(AsistenciaSolicitadaWsMessage.builder()
                .visitaId(visitaId)
                .notificacionId(notificacion.getNotificacionId())
                .mesaIdentificador(mesa.getMesaIdentificador())
                .clienteNombre(clienteNombre)
                .fechaHora(LocalDateTime.now())
                .build());

        return NotificacionAsistenciaResponse.builder()
                .notificacionId(notificacion.getNotificacionId())
                .estado("ACTIVA")
                .build();
    }

    /**
     * Marca una solicitud de asistencia como atendida y notifica al cliente.
     *
     * <p>Cambia el estado de ACTIVA a ATENDIDA y publica en
     * {@code /topic/visita/{visitaId}/asistencia} para que el frontend
     * re-habilite el botón "Solicitar asistencia".
     *
     * @param notificacionId identificador de la notificación a atender
     * @param emailEmpleado  correo del mesero autenticado (para auditoría futura)
     * @throws ResourceNotFoundException si la notificación no existe
     * @throws BusinessException         si la notificación ya fue atendida
     */
    @Transactional
    public void atenderAsistencia(Long notificacionId, String emailEmpleado) {

        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificacionId));

        // No se puede atender una solicitud que ya fue procesada
        if (notificacion.getNotificacionEstado() == EstadoNotificacion.ATENDIDA) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Esta solicitud de asistencia ya fue atendida.",
                    HttpStatus.CONFLICT);
        }

        notificacion.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
        notificacionRepository.save(notificacion);

        Long visitaId = notificacion.getMesa().getVisitaId();

        // Notifica al cliente que puede volver a solicitar asistencia
        wsPublisher.publicarAsistenciaAtendida(visitaId, AsistenciaAtendidaWsMessage.builder()
                .visitaId(visitaId)
                .asistenciaAtendida(true)
                .build());
    }
}
```

- [ ] **Step 4: Ejecutar tests y verificar que pasan**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=NotificacionServiceTest -q 2>&1 | tail -5
```
Resultado esperado: `Tests run: 6, Failures: 0, Errors: 0`.

- [ ] **Step 5: Compilar todo (incluyendo VisitaController que depende de NotificacionService)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```
Resultado esperado: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionService.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionServiceTest.java
git commit -m "feat(notificaciones): implementar NotificacionService para solicitar y atender asistencia"
```

---

## Task 7: NotificacionController (PATCH /api/notificaciones/{id}/atender)

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionController.java`
- Create (test): `backend/src/test/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionControllerTest.java`

- [ ] **Step 1: Escribir test fallido para NotificacionController**

```java
// backend/src/test/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionControllerTest.java
package co.edu.unicauca.backend.modules.notificaciones.controller;

import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionService;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificacionController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("NotificacionController")
class NotificacionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean NotificacionService notificacionService;

    @Test
    @WithMockUser(roles = "MESERO")
    @DisplayName("PATCH /api/notificaciones/{id}/atender — 200 OK cuando MESERO atiende")
    void atenderAsistencia_ok() throws Exception {
        doNothing().when(notificacionService).atenderAsistencia(eq(50L), any());

        mockMvc.perform(patch("/api/notificaciones/50/atender"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("PATCH /api/notificaciones/{id}/atender — 403 Forbidden cuando CLIENTE")
    void atenderAsistencia_403paraCliente() throws Exception {
        mockMvc.perform(patch("/api/notificaciones/50/atender"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MESERO")
    @DisplayName("PATCH /api/notificaciones/{id}/atender — 409 cuando ya fue atendida")
    void atenderAsistencia_409yaAtendida() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_STATE, "Ya atendida.", HttpStatus.CONFLICT))
                .when(notificacionService).atenderAsistencia(eq(50L), any());

        mockMvc.perform(patch("/api/notificaciones/50/atender"))
                .andExpect(status().isConflict());
    }
}
```

- [ ] **Step 2: Ejecutar test y verificar que falla**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=NotificacionControllerTest -q 2>&1 | tail -5
```
Resultado esperado: COMPILATION ERROR — `NotificacionController` no existe.

- [ ] **Step 3: Implementar NotificacionController**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionController.java
package co.edu.unicauca.backend.modules.notificaciones.controller;

import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para operaciones sobre notificaciones de mesa.
 *
 * <p>Expone endpoints bajo {@code /api/notificaciones} para que los empleados
 * puedan marcar solicitudes de asistencia como atendidas.
 *
 * @see NotificacionService
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Gestión de notificaciones de mesa para empleados")
public class NotificacionController {

    private final NotificacionService notificacionService;

    /**
     * Marca una solicitud de asistencia como atendida.
     *
     * <p>El mesero llama a este endpoint desde el mapa de mesas.
     * Al completar, el cliente recibe un evento WS que re-habilita el botón
     * "Solicitar asistencia" en su dashboard.
     *
     * @param notificacionId identificador de la notificación a atender
     * @param authentication contexto de seguridad del request
     * @return 200 OK con mensaje de confirmación
     */
    @PatchMapping("/{notificacionId}/atender")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Marcar solicitud de asistencia como atendida")
    public ResponseEntity<ApiResponse<Void>> atenderAsistencia(
            @PathVariable Long notificacionId,
            Authentication authentication) {

        notificacionService.atenderAsistencia(notificacionId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.message("Asistencia atendida."));
    }
}
```

- [ ] **Step 4: Ejecutar tests y verificar que pasan**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=NotificacionControllerTest -q 2>&1 | tail -5
```
Resultado esperado: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionController.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionControllerTest.java
git commit -m "feat(notificaciones): implementar NotificacionController para atender solicitud de asistencia"
```

---

## Task 8: VisitaControllerTest + modificación de VentaService (CA-03)

**Files:**
- Create (test): `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaControllerTest.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/pagos_caja/service/VentaService.java`

- [ ] **Step 1: Escribir tests para los nuevos endpoints de VisitaController**

```java
// backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaControllerTest.java
package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaEstadoService;
import co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaService;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionService;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisitaController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("VisitaController — endpoints activa y asistencia")
class VisitaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean VisitaService visitaService;
    @MockBean VisitaEstadoService visitaEstadoService;
    @MockBean NotificacionService notificacionService;

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    @DisplayName("GET /api/visitas/activa — 200 OK con ítems (CLIENTE usa su propio email)")
    void obtenerEstadoVisita_ok_cliente() throws Exception {
        EstadoVisitaResponse res = EstadoVisitaResponse.builder()
                .visitaId(10L)
                .mesaIdentificador("T-01")
                .visitaCerrada(false)
                .items(List.of(ItemVisitaResponse.builder()
                        .comandaItemId(1L).nombreProducto("Bandeja").cantidad(1)
                        .estadoItem("En preparación").precioUnitario(new BigDecimal("18000"))
                        .subtotal(new BigDecimal("18000")).build()))
                .total(new BigDecimal("18000"))
                .asistenciaSolicitada(false)
                .build();

        when(visitaEstadoService.obtenerEstadoVisitaActiva("cliente@test.com")).thenReturn(res);

        mockMvc.perform(get("/api/visitas/activa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.visitaId").value(10))
                .andExpect(jsonPath("$.data.items[0].estadoItem").value("En preparación"))
                .andExpect(jsonPath("$.data.total").value(18000));
    }

    @Test
    @WithMockUser(username = "mesero@test.com", roles = "MESERO")
    @DisplayName("GET /api/visitas/activa — 200 OK cuando MESERO consulta con emailCliente")
    void obtenerEstadoVisita_ok_mesero() throws Exception {
        EstadoVisitaResponse res = EstadoVisitaResponse.builder()
                .visitaId(10L).visitaCerrada(false)
                .items(List.of()).total(BigDecimal.ZERO).asistenciaSolicitada(false)
                .build();

        when(visitaEstadoService.obtenerEstadoVisitaActiva("cliente@test.com")).thenReturn(res);

        mockMvc.perform(get("/api/visitas/activa").param("emailCliente", "cliente@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visitaId").value(10));
    }

    @Test
    @WithMockUser(username = "mesero@test.com", roles = "MESERO")
    @DisplayName("GET /api/visitas/activa — 400 cuando MESERO no envía emailCliente")
    void obtenerEstadoVisita_400_meseroSinEmail() throws Exception {
        mockMvc.perform(get("/api/visitas/activa"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    @DisplayName("GET /api/visitas/activa — 404 cuando no hay visita activa")
    void obtenerEstadoVisita_404sinVisitaActiva() throws Exception {
        when(visitaEstadoService.obtenerEstadoVisitaActiva("cliente@test.com"))
                .thenThrow(new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "No tienes una visita activa.", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/visitas/activa"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    @DisplayName("POST /api/visitas/{id}/asistencia — 201 Created al solicitar asistencia")
    void solicitarAsistencia_ok() throws Exception {
        NotificacionAsistenciaResponse notifRes = NotificacionAsistenciaResponse.builder()
                .notificacionId(50L).estado("ACTIVA").build();

        when(notificacionService.solicitarAsistencia(eq(10L), any())).thenReturn(notifRes);

        mockMvc.perform(post("/api/visitas/10/asistencia").with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.notificacionId").value(50));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    @DisplayName("POST /api/visitas/{id}/asistencia — 409 cuando ya hay solicitud activa (CA-04)")
    void solicitarAsistencia_409yaActiva() throws Exception {
        when(notificacionService.solicitarAsistencia(eq(10L), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATE,
                        "Ya existe solicitud activa.", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/visitas/10/asistencia").with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "MESERO")
    @DisplayName("POST /api/visitas/{id}/asistencia — 403 cuando MESERO intenta solicitar asistencia")
    void solicitarAsistencia_403mesero() throws Exception {
        mockMvc.perform(post("/api/visitas/10/asistencia").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Ejecutar tests**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=VisitaControllerTest -q 2>&1 | tail -5
```
Resultado esperado: `Tests run: 7, Failures: 0, Errors: 0`.

- [ ] **Step 3: Modificar VentaService para setear visitaFechaHoraFin y publicar WS con puntos (CA-03)**

Abrir `backend/src/main/java/co/edu/unicauca/backend/modules/pagos_caja/service/VentaService.java`.

Añadir imports:
```java
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.CuentaCerradaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import java.time.LocalDateTime;
```

Añadir campo al servicio:
```java
private final NotificacionWsPublisher wsPublisher;
```

Al final del método `cerrarCuenta`, después del bloque `if (cliente != null)`, añadir:
```java
// Registrar hora de cierre de la visita
visita.setVisitaFechaHoraFin(LocalDateTime.now());
visitaRepository.save(visita);

// Notificar al cliente vía WebSocket: cuenta cerrada + puntos actualizados
// puntosActuales se incluye para que el frontend actualice el saldo sin llamada extra
Integer puntosActuales = (cliente != null) ? cliente.getClientePuntos() : null;
wsPublisher.publicarCuentaCerrada(visita.getVisitaId(),
        CuentaCerradaWsMessage.builder()
                .visitaId(visita.getVisitaId())
                .mensaje("La cuenta ya está cerrada. ¡Gracias por tu visita!")
                .puntosActuales(puntosActuales)
                .build());
```

Eliminar los TODOs correspondientes ya resueltos:
```java
// TODO: Verificar si se registró la hora de salida de la visita; si no, registrar la hora actual
// TODO: Notificar cierre de visita al dashboard del cliente vía WebSocket (+1 punto, mover a historial)
```

- [ ] **Step 4: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```
Resultado esperado: BUILD SUCCESS.

- [ ] **Step 5: Ejecutar suite completa para verificar no hay regresiones**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -q 2>&1 | tail -10
```
Resultado esperado: todos los tests existentes pasan + los nuevos. No debe haber tests en rojo.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/pagos_caja/service/VentaService.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaControllerTest.java
git commit -m "feat(pagos_caja): publicar CuentaCerradaWsMessage con puntosActuales y registrar visitaFechaHoraFin al cerrar cuenta"
```

---

## Task 9: Actualizar backend/CLAUDE.md

**Files:**
- Modify: `backend/CLAUDE.md`

- [ ] **Step 1: Añadir regla de mappers y documentación en la sección Working Rules**

En la sección `## Working Rules`, añadir después de la regla de documentación:

```markdown
- **Mappers:** Toda transformación entity→DTO debe implementarse en una clase mapper dedicada (en `mapper/` dentro del módulo). Los servicios no deben construir DTOs con builders inline en streams; deben delegar al mapper. Esto aplica tanto para mappers nuevos como para extender los existentes (`VisitaMapper`, `ReservaMapper`, `ProductoMapper`, etc.).
- **Acceso multi-rol:** Al diseñar endpoints que sirven tanto a `CLIENTE` como a empleados, seguir el patrón de `VisitaController`: `@PreAuthorize("hasAnyRole(...)")` + validación de ownership dentro del servicio solo para el rol `CLIENTE`. Los otros roles acceden sin restricción de propiedad.
```

- [ ] **Step 2: Actualizar la tabla de endpoints en la sección API Endpoints — Visitas**

Añadir las nuevas rutas a la tabla de `/api/visitas`:

```markdown
| GET | `/activa` | CLIENTE / MESERO / CAJERO / ADMIN | Estado de la visita activa: ítems, total, asistencia. CLIENTE usa token; otros roles requieren `?emailCliente=` |
| POST | `/{visitaId}/asistencia` | CLIENTE | Solicita asistencia de mesero; 409 si ya hay solicitud activa |
```

- [ ] **Step 3: Actualizar la tabla de endpoints en la sección API Endpoints — Notificaciones (nueva)**

```markdown
### Notificaciones (`/api/notificaciones`)
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| PATCH | `/{notificacionId}/atender` | MESERO / ADMIN | Marca solicitud de asistencia como atendida; publica WS al cliente |
```

- [ ] **Step 4: Commit**

```bash
git add backend/CLAUDE.md
git commit -m "docs(backend): actualizar CLAUDE.md con reglas de mappers, acceso multi-rol y nuevos endpoints"
```

---

## Task 10: Colecciones Postman

**Files:**
- Create: `backend/postman/postman/collections/mesas_comandas/Al Toro – GET -api-visitas-activa/.resources/definition.yaml`
- Create: `backend/postman/postman/collections/mesas_comandas/Al Toro – GET -api-visitas-activa/OA-01 … .request.yaml` (CAs 01, 02, 03, 06)
- Create: `backend/postman/postman/collections/mesas_comandas/Al Toro – POST -api-visitas-{id}-asistencia/.resources/definition.yaml`
- Create: `backend/postman/postman/collections/mesas_comandas/Al Toro – POST -api-visitas-{id}-asistencia/OB-01 … .request.yaml` (CA-04)
- Create: `backend/postman/postman/collections/notificaciones/Al Toro – PATCH -api-notificaciones-{id}-atender/.resources/definition.yaml`
- Create: `backend/postman/postman/collections/notificaciones/Al Toro – PATCH -api-notificaciones-{id}-atender/NC-01 … .request.yaml` (CA-05)

Usar `backend/postman/prompt` como base para el formato exacto de cada archivo.

- [ ] **Step 1: Crear definition.yaml para GET /api/visitas/activa**

```yaml
# backend/postman/postman/collections/mesas_comandas/Al Toro – GET -api-visitas-activa/.resources/definition.yaml
$kind: collection
name: Al Toro – GET /api/visitas/activa
description: |-
  Retorna el estado actual de la visita activa: ítems, total acumulado y estado del botón de asistencia.

  **Criterios de Aceptación cubiertos:** CA-01 (contenido del dashboard), CA-06 (recarga mantiene estado).

  **Control de acceso:**
  - CLIENTE: usa su propio token, sin parámetros adicionales.
  - MESERO/CAJERO/ADMIN: requiere ?emailCliente=. Sin parámetro → 400.
  - Sin token → 401.

  **Campos de EstadoVisitaResponse:** visitaId, mesaIdentificador (nullable), visitaCerrada, items[], total, asistenciaSolicitada, notificacionAsistenciaId (nullable)

  | ID | Escenario | HTTP |
  |----|-----------|------|
  | OA-01 | CLIENTE con visita activa y ítems — 200 OK (CA-01) | 200 |
  | OA-02 | CLIENTE con visita activa sin ítems — 200 OK lista vacía | 200 |
  | OA-03 | CLIENTE sin visita activa — 404 Not Found | 404 |
  | OA-04 | Sin token de autenticación — 401 Unauthorized | 401 |
  | OA-05 | MESERO con emailCliente válido — 200 OK | 200 |
  | OA-06 | MESERO sin emailCliente — 400 Bad Request | 400 |
  | OA-07 | Segunda llamada retorna datos consistentes (CA-06) | 200 |
scripts:
  - type: http:beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: pm.environment.get('emailCliente'),
            password: pm.environment.get('passwordValida'),
            forceSessionOverride: true
          })
        }
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('clienteToken', res.json().accessToken);
        } else {
          console.warn('OA: login CLIENTE falló', err, res && res.code);
        }
      });
    language: text/javascript
```

- [ ] **Step 2: Crear definition.yaml para POST /api/visitas/{id}/asistencia**

```yaml
# backend/postman/postman/collections/mesas_comandas/Al Toro – POST -api-visitas-{id}-asistencia/.resources/definition.yaml
$kind: collection
name: Al Toro – POST /api/visitas/{visitaId}/asistencia
description: |-
  Solicita la asistencia de un mesero para la mesa de la visita activa del cliente.

  **Criterios de Aceptación cubiertos:** CA-04 (solicitud de asistencia, no duplicar).

  **Control de acceso:** Solo CLIENTE. MESERO/ADMIN → 403.

  | ID | Escenario | HTTP |
  |----|-----------|------|
  | OB-01 | CLIENTE solicita asistencia — 201 Created | 201 |
  | OB-02 | CLIENTE solicita segunda vez (ya activa) — 409 Conflict | 409 |
  | OB-03 | CLIENTE en visita sin mesa asignada — 409 Conflict | 409 |
  | OB-04 | CLIENTE en visita de otro cliente — 403 Forbidden | 403 |
  | OB-05 | MESERO intenta solicitar asistencia — 403 Forbidden | 403 |
```

- [ ] **Step 3: Crear definition.yaml para PATCH /api/notificaciones/{id}/atender**

```yaml
# backend/postman/postman/collections/notificaciones/Al Toro – PATCH -api-notificaciones-{id}-atender/.resources/definition.yaml
$kind: collection
name: Al Toro – PATCH /api/notificaciones/{notificacionId}/atender
description: |-
  El mesero marca una solicitud de asistencia como atendida.
  Al completar, el cliente recibe un evento WebSocket que re-habilita el botón de asistencia.

  **Criterios de Aceptación cubiertos:** CA-05 (mesero atiende la solicitud).

  **Control de acceso:** MESERO y ADMIN. CLIENTE → 403.

  | ID | Escenario | HTTP |
  |----|-----------|------|
  | NC-01 | MESERO atiende solicitud activa — 200 OK | 200 |
  | NC-02 | MESERO intenta atender notificación ya atendida — 409 Conflict | 409 |
  | NC-03 | MESERO intenta atender notificación inexistente — 404 Not Found | 404 |
  | NC-04 | CLIENTE intenta atender — 403 Forbidden | 403 |
```

- [ ] **Step 4: Crear archivos .request.yaml para cada caso de prueba**

Seguir el formato de `backend/postman/prompt` para cada caso listado en los definition.yaml anteriores. Cada test incluye `beforeRequest` con login autónomo al rol correspondiente.

- [ ] **Step 5: Commit**

```bash
git add backend/postman/postman/collections/mesas_comandas/
git add backend/postman/postman/collections/notificaciones/
git commit -m "test(postman): añadir colecciones para GET /api/visitas/activa, POST asistencia y PATCH atender"
```

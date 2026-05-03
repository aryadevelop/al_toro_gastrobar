# Gestionar Notificaciones (HE-03-HU-06) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar 3 endpoints REST + eventos WebSocket para que el mesero sirva platos (CA-04), sirva bebidas (CA-05) y atienda notificaciones de cambio (CA-06), con evaluación centralizada del estado de la mesa tras cada acción.

**Architecture:** REST endpoint → `NotificacionService` (validación + persistencia) → `MesaAsignarService.evaluarYActualizarEstadoMesa()` (evaluador centralizado) → `WsPublisher` (difusión en tiempo real). El estado de la mesa se gestiona en `MesaAsignarService`, único componente que escribe `mesaEstado`. Nuevo tópico WS `/topic/comandas/completado` para que el dashboard del cocinero/bartender elimine la comanda de "Listas" sin recargar la página.

**Tech Stack:** Spring Boot 3.5 · Java 21 · JPA/Hibernate · PostgreSQL 15 · STOMP WebSocket · JUnit 5 · Mockito · AssertJ · Postman YAML collections

---

## Reglas y Patrones del Proyecto (validados contra código existente)

### Autorización
- **Cualquier mesero o admin** puede atender PLATOS_LISTOS / BEBIDAS_LISTAS / CAMBIO. **NO se valida ownership** del mesero asignado a la mesa. Solo se controla el rol con `@PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")` en el controller.

### Excepciones
- **404 Not Found** → `new ResourceNotFoundException("EntidadNombre", id)` (NO `BusinessException(ENTITY_NOT_FOUND, ...)`).
- **409 Conflict (estado inválido)** → `new BusinessException(ErrorCode.INVALID_STATE, "...", HttpStatus.CONFLICT)`.
- **400 Bad Request (regla de negocio)** → `new BusinessException(ErrorCode.BUSINESS_ERROR, "...", HttpStatus.BAD_REQUEST)`.

### Service signature
- Recibe `String emailEmpleado` (extraído de `authentication.getName()` en el controller). NO recibe `Authentication`.
- Anotaciones: `@Service @RequiredArgsConstructor`. Métodos write con `@Transactional`.

### ApiResponse factory methods existentes
- `ApiResponse.ok(T data)` — exitosa, solo data.
- `ApiResponse.ok(String message, T data)` — exitosa con mensaje + data.
- `ApiResponse.created(String message, T data)` — alias de ok para 201.
- `ApiResponse.message(String message)` — exitosa, solo mensaje (sin data).
- `ApiResponse.error(String code, String message)` — error.
- **NO existe** `ApiResponse.success(...)`.

### Documentación obligatoria
- **Javadoc en métodos public** del service y controller con: descripción + `<p>` para flujo paso a paso (`<ol>` o `<ul>`), `@param`, `@return`, `@throws`.
- **Comentarios inline en español** sobre líneas no obvias (igual que en `solicitarAsistencia`).
- **Javadoc en clases** con propósito, dependencias, e índices/relaciones relevantes.
- **Javadoc en cada campo de DTOs/entidades** con valores posibles cuando aplique.

### Tests unitarios (JaCoCo > 90%)
- Service: `@ExtendWith(MockitoExtension.class)` + `@MockitoSettings(strictness = Strictness.LENIENT)` + `@DisplayName` clase.
- Controller: `@WebMvcTest(controllers = X.class)` + `@Import(PermissiveSecurityConfig.class)` + `@MockitoBean` para `NotificacionService`, `JwtTokenProvider`, `UserDetailsService`, `SesionRepository`.
- Imports correctos: `JwtTokenProvider` está en `co.edu.unicauca.backend.modules.auth.security`, `SesionRepository` en `co.edu.unicauca.backend.modules.auth.repository`.
- Tests con `@WithMockUser(username = "...", roles = "MESERO")`.
- Helpers builder privados (ej. `mesaConMesero()`, `visitaConCliente()`) para construir entidades de prueba.
- Usar `ArgumentCaptor` para verificar mensajes WS publicados.
- Cubrir TODAS las ramas: cada `if/else`, validación, edge case (null, optional empty, estado inválido).

### Postman — Automated Collections
- 1 carpeta por endpoint con `definition.yaml` (collection-level beforeRequest con login secuencial).
- Variables: `emailMesero`, `emailAdmin`, `emailCajero`, `emailCliente`, `passwordValida`, `meseroToken`, `adminToken`, etc.
- Tests obligatorios en `afterResponse` con `pm.test(...)`.
- Codes: `ENT-001`, `AUTH-001`, `AUTH-002`, `NEG-001`, `NEG-002`.
- **No solo happy path** — incluir 401, 403 (otros roles), 404, 409, 400 según aplique.

### Postman — Manual Testing
- Patrón `XX-YY Descripción – ROL.request.yaml`.
- `tmpXxx` variables para IDs y tokens.
- Login autónomo con credenciales hardcoded `password123` (patrón actual del 70-01).
- `afterResponse`: solo `pm.environment.unset(...)` (NO tests).

---

## Mapa de Archivos

### Modificar

| Archivo | Cambio |
|---------|--------|
| `backend/src/main/resources/db/migration/V1__init_schema.sql` | Añadir `comanda_id BIGINT NULL` + FK + índice en tabla `Notificacion` |
| `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/entity/Notificacion.java` | Añadir `@ManyToOne Comanda comanda` |
| `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/repository/NotificacionRepository.java` | Añadir `existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado` |
| `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaRepository.java` | Añadir `existsByVisita_VisitaIdAndComandaEstadoIn` |
| `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaAsignarService.java` | Añadir `evaluarYActualizarEstadoMesa(Long)` + nuevas dependencias |
| `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionWsPublisher.java` | Añadir `publicarComandaCompletada(Long, String)` |
| `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionService.java` | Añadir `servirPlatos`, `servirBebidas`, `atenderCambio` + nuevas dependencias |
| `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionController.java` | Añadir 3 endpoints `PATCH` |
| `backend/src/test/java/.../notificaciones/service/NotificacionServiceTest.java` | Añadir 3 `@Nested` con tests para servir/cambio |
| `backend/src/test/java/.../notificaciones/controller/NotificacionControllerTest.java` | Añadir 3 `@Nested` con tests para los nuevos endpoints |

### Crear

| Archivo | Propósito |
|---------|-----------|
| `backend/src/main/java/.../notificaciones/dto/response/AtenderCambioResponse.java` | DTO respuesta CA-06 (devuelve `comandaId`) |
| `backend/src/main/java/.../notificaciones/dto/ws/ComandaCompletadaWsMessage.java` | Record WS para `/topic/comandas/completado` |
| `backend/src/test/java/.../mesas_comandas/service/MesaAsignarEvaluadorTest.java` | Tests unitarios `evaluarYActualizarEstadoMesa` (5 ramas) |
| `backend/postman/postman/collections/notificaciones/Al Toro – PATCH -api-notificaciones-{id}-servir-platos/` | 7 requests automatizados (SP-01 a SP-07) |
| `backend/postman/postman/collections/notificaciones/Al Toro – PATCH -api-notificaciones-{id}-servir-bebidas/` | 7 requests automatizados (SB-01 a SB-07) |
| `backend/postman/postman/collections/notificaciones/Al Toro – PATCH -api-notificaciones-{id}-atender-cambio/` | 6 requests automatizados (AC-01 a AC-06) |
| `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/70-02 ... 70-04 *.request.yaml` | 3 requests manuales |

### Tópicos WebSocket — estado final

| Tópico | Existente | Propósito |
|--------|-----------|-----------|
| `/topic/visita/{visitaId}/asistencia` | ✅ | Notificar al cliente que su asistencia fue atendida |
| `/topic/mesas/asistencia` | ✅ | Broadcast solicitud asistencia a empleados (mostrar ícono) |
| `/topic/mesas` | ✅ (uso ampliado) | Mapa de mesas: cambios de estado **+ notificaciones atendidas (quitar ícono)** |
| `/topic/visita/{visitaId}/orden` | ✅ | Actualizar orden del cliente |
| `/topic/visita/{visitaId}/cuenta` | ✅ | Notificar cierre de cuenta |
| `/topic/reservas/cambios` | ✅ | Cambios de reservas a meseros |
| `/topic/comandas/completado` | 🆕 | Notificar cocinero/bartender que comanda fue servida (eliminar de "Listas") |

### Estrategia WS al atender una notificación (CA-03/04/05/06)

Al marcar **CUALQUIER** notificación como ATENDIDA, se debe publicar a `/topic/mesas` para que **todos los meseros** suscritos al mapa eliminen el ícono correspondiente en tiempo real.

**Reutilizar el método existente** `MesaWsPublisher.publicarActualizacionMesa(visitaId, TipoEventoMesa.NOTIFICACION)`. NO crear DTO nuevo. El payload es `MesaWsMessage(visitaId, tipoEvento, nuevoEstado, timestamp)` y el enum `TipoEventoMesa` ya incluye el valor `NOTIFICACION`.

**Patrón vigente del proyecto:** WS es señal, no transporte de data. El frontend recibe `(visitaId, tipoEvento)` y hace re-fetch vía REST `GET /api/mesas`. No se envía la mesa completa por WS — esto coincide con `publicarCambioEstadoMesa` y `publicarActualizacionMesa(CREAR)` ya existentes.

| Operación | WS publicados |
|-----------|---------------|
| `atenderAsistencia` (existente — gap a corregir) | `/topic/visita/{id}/asistencia` (existente, al cliente) **+** `/topic/mesas` con `TipoEventoMesa.NOTIFICACION` (nuevo, refresco mapa) |
| `servirPlatos` (CA-04) | `/topic/comandas/completado` (cocinero) **+** `/topic/mesas` con `NOTIFICACION` (refresco mapa) **+** posible `/topic/mesas` con `ACTUALIZAR` por cambio de estado vía evaluador |
| `servirBebidas` (CA-05) | `/topic/comandas/completado` (bartender) **+** `/topic/mesas` con `NOTIFICACION` **+** posible `/topic/mesas` con `ACTUALIZAR` por cambio de estado |
| `atenderCambio` (CA-06) | `/topic/mesas` con `NOTIFICACION` (refresco mapa). No publica en `/topic/comandas/completado` ni cambia estado |

### DTOs y respuestas — sin cambios

- `MesaMapaResponse` (lo que ve cada mesero del mapa): incluye `mesaId`, `identificador`, `numeroPersonas`, `estado`, `nombreMesero`, `esMesaPropia`, `tieneBorrador`, `notificacionesActivas: List<NotificacionActivaResponse>`. **No requiere cambios**: tras recibir el WS, el frontend re-consulta `GET /api/mesas` y obtiene la mesa con sus notificaciones actualizadas.
- `NotificacionActivaResponse(notificacionId, tipo, fechaHora)`. **No requiere cambios** — el frontend ya muestra ícono por `tipo`.

### Cobertura de Criterios de Aceptación

| CA | Estado en el plan |
|----|-------------------|
| CA-01 (aparece ícono al crearse notificación) | ❌ FUERA de scope. La creación de PLATOS_LISTOS / BEBIDAS_LISTAS / CAMBIO viene de HE-04-HU-03-CA-05 y CA-02 (cocinero/bartender). |
| CA-02 (íconos en mesas de otros meseros) | ❌ FUERA de scope, misma razón. (El backend ya devuelve todas las notificaciones al mesero en `MesaMapaResponse.notificacionesActivas` independientemente de quién es dueño de la mesa, así que esto ya queda cubierto a nivel de datos.) |
| CA-03 (atender atención) | ✅ Endpoint existente `/atender` + fix WS mapa (Task 6 Step 6). |
| CA-04 (servir platos) | ✅ Endpoint nuevo `/servir-platos` (Task 6 + 7). |
| CA-05 (servir bebidas) | ✅ Endpoint nuevo `/servir-bebidas` (Task 6 + 7). |
| CA-06 (atender cambio) | ✅ Endpoint nuevo `/atender-cambio` con `comandaId` en respuesta (Task 6 + 7). |

---

## Task 1: Schema — añadir comanda_id a Notificacion

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`

> **Regla crítica (CLAUDE.md):** NUNCA crear migraciones nuevas. Schema changes → modificar V1. Luego: `docker compose down -v && docker compose up --build`.

- [ ] **Step 1: Añadir columna y FK al `CREATE TABLE Notificacion` (línea ~242)**

```sql
CREATE TABLE Notificacion (
    notificacion_id BIGSERIAL PRIMARY KEY,
    mesa_id BIGINT NOT NULL,
    empleado_id BIGINT NOT NULL,
    comanda_id BIGINT NULL,
    notificacion_estado VARCHAR(20) NOT NULL,
    notificacion_tipo VARCHAR(20) NOT NULL,
    notificacion_fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notificacion_mesa FOREIGN KEY (mesa_id)
        REFERENCES Mesa(visita_id) ON DELETE CASCADE,
    CONSTRAINT fk_notificacion_empleado FOREIGN KEY (empleado_id)
        REFERENCES Empleado(usuario_id) ON DELETE RESTRICT,
    CONSTRAINT fk_notificacion_comanda FOREIGN KEY (comanda_id)
        REFERENCES Comanda(comanda_id) ON DELETE SET NULL,
    CONSTRAINT chk_notificacion_estado CHECK (notificacion_estado IN ('ACTIVA', 'ATENDIDA')),
    CONSTRAINT chk_notificacion_tipo CHECK (notificacion_tipo IN ('ATENCION', 'PLATOS_LISTOS', 'BEBIDAS_LISTAS', 'CAMBIO'))
);
```

- [ ] **Step 2: Añadir índice (línea ~577)**

```sql
CREATE INDEX idx_notificacion_comanda_id ON Notificacion(comanda_id);
```

- [ ] **Step 3: Verificar que `CREATE TABLE Comanda` aparece antes de `CREATE TABLE Notificacion` en el archivo**

Si no, mover el bloque de Notificacion después de Comanda (la FK requiere que la tabla referenciada exista al momento de la creación).

- [ ] **Step 4: Resetear base de datos**

```bash
docker compose down -v
docker compose up --build
```

Verificar logs del container `api` para confirmar que Flyway ejecuta V1 sin errores.

- [ ] **Step 5: Commit**

```
feat(db): añadir comanda_id a Notificacion para trazabilidad platos/bebidas/cambio
```

---

## Task 2: Entity — campo comanda en Notificacion

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/entity/Notificacion.java`

- [ ] **Step 1: Añadir el campo `comanda` con su Javadoc**

Insertar después del campo `empleado`:

```java
/**
 * Comanda asociada a la notificación.
 *
 * <p>Aplica solo a notificaciones generadas desde producción:
 * <ul>
 *   <li>{@code PLATOS_LISTOS} — comanda de cocina cuyos platos están listos para servir.</li>
 *   <li>{@code BEBIDAS_LISTAS} — comanda de barra cuyas bebidas están listas para servir.</li>
 *   <li>{@code CAMBIO} — comanda que el cliente solicitó modificar.</li>
 * </ul>
 *
 * <p>Es {@code null} para notificaciones {@code ATENCION} (solicitadas por el cliente,
 * sin relación con una comanda específica).
 */
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "comanda_id",
            foreignKey = @ForeignKey(name = "fk_notificacion_comanda"))
private Comanda comanda;
```

Import necesario: `import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;`

- [ ] **Step 2: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

Resultado esperado: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```
feat(notificaciones): añadir relación ManyToOne con Comanda en entidad Notificacion
```

---

## Task 3: Repository — nuevas queries booleanas

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/repository/NotificacionRepository.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaRepository.java`

- [ ] **Step 1: NotificacionRepository — query booleana**

Añadir tras los métodos existentes:

```java
/**
 * Verifica si existe al menos una notificación de un tipo y estado dados para la mesa.
 *
 * <p>El evaluador de estado de mesa la utiliza para decidir si la mesa puede
 * transicionar automáticamente a {@code ATENDIDA}.
 *
 * @param visitaId identificador de la visita (PK de Mesa)
 * @param tipo     tipo de notificación a verificar
 * @param estado   estado de la notificación a verificar
 * @return {@code true} si existe alguna notificación que cumple las tres condiciones
 */
boolean existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
        Long visitaId, TipoNotificacion tipo, EstadoNotificacion estado);
```

- [ ] **Step 2: ComandaRepository — query booleana**

```java
/**
 * Verifica si existe al menos una comanda de la visita en alguno de los estados indicados.
 *
 * <p>Se utiliza para determinar si quedan comandas en producción
 * (PENDIENTE, EN_PREPARACION, LISTO) antes de transicionar la mesa a {@code ATENDIDA}.
 * Las comandas en {@code BORRADOR} o {@code PRE_RESERVA} no afectan la evaluación
 * porque aún no se enviaron a producción.
 *
 * @param visitaId identificador de la visita
 * @param estados  lista de estados de comanda a buscar
 * @return {@code true} si existe al menos una comanda en alguno de los estados dados
 */
boolean existsByVisita_VisitaIdAndComandaEstadoIn(Long visitaId, List<EstadoComanda> estados);
```

Import: `import java.util.List;`

- [ ] **Step 3: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

- [ ] **Step 4: Commit**

```
feat(notificaciones): añadir queries booleanas para evaluación de estado de mesa
```

---

## Task 4: WS Publisher — solo comanda completada (mesa map ya cubierto)

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/ComandaCompletadaWsMessage.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionWsPublisher.java`

> **Importante — NO crear DTO ni método nuevo en `MesaWsPublisher`:**
> Para refrescar el mapa de mesas al atender una notificación, **reutilizar el método existente** `MesaWsPublisher.publicarActualizacionMesa(visitaId, TipoEventoMesa.NOTIFICACION)`. El enum `TipoEventoMesa` ya incluye el valor `NOTIFICACION`. El payload `MesaWsMessage(visitaId, tipoEvento, nuevoEstado, timestamp)` actúa como señal y el frontend hace re-fetch vía REST. Esto sigue el patrón establecido del proyecto.

- [ ] **Step 1: Crear record del mensaje WS para comanda completada**

```java
package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

/**
 * Mensaje WebSocket emitido al tópico {@code /topic/comandas/completado}
 * cuando un mesero registra el servicio de platos o bebidas (CA-04 / CA-05).
 *
 * <p>Los dashboards del cocinero y del bartender, suscritos a este tópico,
 * eliminan en tiempo real la comanda de su columna "Listas" sin necesidad
 * de refrescar la página (HE-04-HU-03-CA-06).
 *
 * @param comandaId identificador de la comanda marcada como {@code COMPLETADO}
 * @param estacion  estación productora: {@code "COCINA"} o {@code "BARRA"}
 */
public record ComandaCompletadaWsMessage(Long comandaId, String estacion) {}
```

- [ ] **Step 2: Añadir método en NotificacionWsPublisher**

```java
/**
 * Publica al tópico {@code /topic/comandas/completado} que la comanda fue
 * servida al cliente.
 *
 * <p>El payload incluye la estación para que cada dashboard (cocinero o
 * bartender) decida si la comanda le concierne.
 *
 * @param comandaId identificador de la comanda completada
 * @param estacion  nombre del enum {@code EstacionComanda}: "COCINA" o "BARRA"
 */
public void publicarComandaCompletada(Long comandaId, String estacion) {
    messagingTemplate.convertAndSend(
            "/topic/comandas/completado",
            new ComandaCompletadaWsMessage(comandaId, estacion));
}
```

Import: `import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaCompletadaWsMessage;`

- [ ] **Step 3: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

- [ ] **Step 4: Commit**

```
feat(notificaciones): añadir publicarComandaCompletada en NotificacionWsPublisher
```

---

## Task 5: Evaluador centralizado de estado de mesa (TDD)

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaAsignarService.java`
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaAsignarEvaluadorTest.java`

> **Nota:** El método se añade en `MesaAsignarService` porque es el único componente que actualmente escribe `mesa.mesaEstado`. Para JaCoCo > 90% se requieren tests para cada rama (4 condiciones de retorno temprano + 1 happy path = 5 tests).

- [ ] **Step 1: Escribir tests (TDD — Red)**

Crear `MesaAsignarEvaluadorTest.java`:

```java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MesaAsignarService.evaluarYActualizarEstadoMesa")
class MesaAsignarEvaluadorTest {

    @Mock MesaRepository mesaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock NotificacionRepository notificacionRepository;
    @Mock MesaWsPublisher mesaWsPublisher;
    @InjectMocks MesaAsignarService service;

    private static final Long VISITA_ID = 1L;
    private static final List<EstadoComanda> ESTADOS_PRODUCCION =
            List.of(EstadoComanda.PENDIENTE, EstadoComanda.EN_PREPARACION, EstadoComanda.LISTO);

    private Mesa mesaEnEstado(EstadoMesa estado) {
        return Mesa.builder().visitaId(VISITA_ID).mesaEstado(estado).build();
    }

    @Nested
    @DisplayName("transiciona a ATENDIDA")
    class TransicionaAtendida {

        @Test
        @DisplayName("sin notificaciones activas y sin comandas en producción → mesa ATENDIDA y publica WS")
        void todasLasCondicionesCumplidas_transicionaAtendida() {
            Mesa mesa = mesaEnEstado(EstadoMesa.EN_PREPARACION);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(comandaRepository.existsByVisita_VisitaIdAndComandaEstadoIn(VISITA_ID, ESTADOS_PRODUCCION))
                    .thenReturn(false);
            when(mesaRepository.findById(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(mesaRepository.save(mesa)).thenReturn(mesa);

            service.evaluarYActualizarEstadoMesa(VISITA_ID);

            assertThat(mesa.getMesaEstado()).isEqualTo(EstadoMesa.ATENDIDA);
            verify(mesaRepository).save(mesa);
            verify(mesaWsPublisher).publicarCambioEstadoMesa(VISITA_ID, EstadoMesa.ATENDIDA);
        }
    }

    @Nested
    @DisplayName("retorna sin cambios (early return por rama)")
    class NoTransiciona {

        @Test
        @DisplayName("rama 1 — hay PLATOS_LISTOS activas → no consulta más")
        void conPlatosListos_retornaTemprano() {
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(true);

            service.evaluarYActualizarEstadoMesa(VISITA_ID);

            verify(notificacionRepository, never()).existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA);
            verify(comandaRepository, never()).existsByVisita_VisitaIdAndComandaEstadoIn(any(), any());
            verify(mesaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rama 2 — hay BEBIDAS_LISTAS activas → no consulta comandas")
        void conBebidasListas_retornaTemprano() {
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)).thenReturn(true);

            service.evaluarYActualizarEstadoMesa(VISITA_ID);

            verify(comandaRepository, never()).existsByVisita_VisitaIdAndComandaEstadoIn(any(), any());
            verify(mesaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rama 3 — hay comandas en producción → no carga mesa")
        void conComandasEnProduccion_retornaTemprano() {
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(comandaRepository.existsByVisita_VisitaIdAndComandaEstadoIn(VISITA_ID, ESTADOS_PRODUCCION))
                    .thenReturn(true);

            service.evaluarYActualizarEstadoMesa(VISITA_ID);

            verify(mesaRepository, never()).findById(any());
            verify(mesaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rama 4 — mesa ya está ATENDIDA → idempotente, no re-guarda ni publica WS")
        void mesaYaAtendida_idempotente() {
            Mesa mesa = mesaEnEstado(EstadoMesa.ATENDIDA);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(comandaRepository.existsByVisita_VisitaIdAndComandaEstadoIn(VISITA_ID, ESTADOS_PRODUCCION))
                    .thenReturn(false);
            when(mesaRepository.findById(VISITA_ID)).thenReturn(Optional.of(mesa));

            service.evaluarYActualizarEstadoMesa(VISITA_ID);

            verify(mesaRepository, never()).save(any());
            verify(mesaWsPublisher, never()).publicarCambioEstadoMesa(any(), any());
        }
    }
}
```

- [ ] **Step 2: Ejecutar test (debe fallar — método no existe)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -pl backend -Dtest="MesaAsignarEvaluadorTest" -q
```

- [ ] **Step 3: Inyectar nuevas dependencias en `MesaAsignarService`**

Añadir como `private final` (Lombok `@RequiredArgsConstructor` se encarga del constructor):

```java
private final NotificacionRepository notificacionRepository;
private final ComandaRepository comandaRepository;
private final MesaWsPublisher mesaWsPublisher;
```

Imports:
```java
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import java.util.List;
```

- [ ] **Step 4: Implementar el método (TDD — Green)**

```java
/**
 * Evalúa si la mesa debe transicionar automáticamente al estado {@code ATENDIDA}
 * y aplica el cambio cuando se cumplen todas las condiciones.
 *
 * <p>Condiciones (todas deben cumplirse simultáneamente):
 * <ol>
 *   <li>No existen notificaciones {@code PLATOS_LISTOS} en estado {@code ACTIVA} para la visita.</li>
 *   <li>No existen notificaciones {@code BEBIDAS_LISTAS} en estado {@code ACTIVA} para la visita.</li>
 *   <li>No existen comandas en estados de producción ({@code PENDIENTE},
 *       {@code EN_PREPARACION} o {@code LISTO}) para la visita.</li>
 * </ol>
 *
 * <p>Las comandas en {@code BORRADOR} o {@code PRE_RESERVA} no afectan la evaluación
 * porque aún no fueron enviadas a producción.
 *
 * <p>El método es <b>idempotente</b>: si la mesa ya está en {@code ATENDIDA},
 * no se persiste cambio ni se publica evento WS.
 *
 * <p>Cuando aplica el cambio, publica al tópico {@code /topic/mesas} para que
 * el frontend actualice el mapa en tiempo real.
 *
 * @param visitaId identificador de la visita (PK de Mesa)
 * @throws ResourceNotFoundException si no existe mesa para esa visita
 */
@Transactional
public void evaluarYActualizarEstadoMesa(Long visitaId) {

    // Si hay notificaciones PLATOS_LISTOS activas, la mesa aún espera servicio
    if (notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
            visitaId, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)) {
        return;
    }

    // Mismo razonamiento para BEBIDAS_LISTAS
    if (notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
            visitaId, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)) {
        return;
    }

    // Si hay comandas pendientes/en preparación/listas, la mesa aún tiene producción en curso
    if (comandaRepository.existsByVisita_VisitaIdAndComandaEstadoIn(
            visitaId,
            List.of(EstadoComanda.PENDIENTE, EstadoComanda.EN_PREPARACION, EstadoComanda.LISTO))) {
        return;
    }

    Mesa mesa = mesaRepository.findById(visitaId)
            .orElseThrow(() -> new ResourceNotFoundException("Mesa", visitaId));

    // Idempotencia: evita publicaciones WS duplicadas
    if (mesa.getMesaEstado() == EstadoMesa.ATENDIDA) {
        return;
    }

    mesa.setMesaEstado(EstadoMesa.ATENDIDA);
    mesaRepository.save(mesa);

    // Notifica al frontend del mapa de mesas el cambio de estado
    mesaWsPublisher.publicarCambioEstadoMesa(visitaId, EstadoMesa.ATENDIDA);
}
```

- [ ] **Step 5: Ejecutar tests (deben pasar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -pl backend -Dtest="MesaAsignarEvaluadorTest" -q
```

Resultado esperado: 5 tests passed.

- [ ] **Step 6: Suite completa sin regresiones**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -q
```

- [ ] **Step 7: Commit**

```
feat(mesas): añadir evaluarYActualizarEstadoMesa con tests de las 5 ramas
```

---

## Task 6: NotificacionService — servirPlatos, servirBebidas, atenderCambio (TDD)

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/response/AtenderCambioResponse.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionService.java`
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionServiceTest.java`

> **Importante:** Cualquier mesero puede atender estas notificaciones — NO se valida ownership del mesero asignado a la mesa. El control de rol se hace solo en el controller con `@PreAuthorize`.

> **Aprovechar el touch de NotificacionService para corregir gap de WS en `atenderAsistencia`:** el método existente solo notifica al cliente, pero NO refresca el mapa de mesas para los demás meseros (el ícono de campana queda fantasma). Añadiremos la llamada `mesaWsPublisher.publicarNotificacionAtendida(...)` también en `atenderAsistencia` para consistencia con CA-04/05/06.

- [ ] **Step 1: Crear DTO `AtenderCambioResponse`**

```java
package co.edu.unicauca.backend.modules.notificaciones.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Respuesta al atender una notificación de tipo {@code CAMBIO} (CA-06).
 *
 * <p>Devuelve el identificador de la comanda que el mesero debe cargar
 * en modo edición tras aceptar la solicitud de cambio del cliente.
 */
@Getter
@Builder
public class AtenderCambioResponse {

    /** Identificador de la comanda lista para ser modificada por el mesero. */
    private final Long comandaId;
}
```

- [ ] **Step 2: Añadir tests en `NotificacionServiceTest` (TDD — Red)**

Añadir los nuevos imports y los 3 `@Nested` al final de la clase existente:

```java
// Imports adicionales a añadir
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaAsignarService;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.AtenderCambioResponse;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
```

Añadir los nuevos mocks como campos de la clase:

```java
@Mock ComandaRepository comandaRepository;
@Mock MesaAsignarService mesaAsignarService;
@Mock MesaWsPublisher mesaWsPublisher;
```

Imports adicionales:
```java
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaWsPublisher;
```

Helpers privados a añadir antes de los `@Nested` nuevos:

```java
private Comanda comandaListo(EstacionComanda estacion) {
    return Comanda.builder()
            .comandaId(80L)
            .comandaEstacion(estacion)
            .comandaEstado(EstadoComanda.LISTO)
            .build();
}

private Notificacion notificacionConComanda(TipoNotificacion tipo,
                                            EstadoNotificacion estado,
                                            Comanda comanda) {
    return Notificacion.builder()
            .notificacionId(50L)
            .mesa(mesaConMesero())
            .empleado(mesaConMesero().getMesero())
            .notificacionTipo(tipo)
            .notificacionEstado(estado)
            .comanda(comanda)
            .build();
}
```

`@Nested` para servirPlatos:

```java
@Nested
@DisplayName("servirPlatos")
class ServirPlatos {

    @Test
    @DisplayName("happy path → comanda COMPLETADO, notificación ATENDIDA, WS publicado y evaluador llamado")
    void platosListosActiva_completaComandaYPublicaWs() {
        Comanda comanda = comandaListo(EstacionComanda.COCINA);
        Notificacion n = notificacionConComanda(
                TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA, comanda);
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
        when(notificacionRepository.save(any())).thenReturn(n);
        when(comandaRepository.save(any())).thenReturn(comanda);

        notificacionService.servirPlatos(50L, "mesero@test.com");

        assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.COMPLETADO);
        assertThat(n.getNotificacionEstado()).isEqualTo(EstadoNotificacion.ATENDIDA);
        verify(wsPublisher).publicarComandaCompletada(80L, "COCINA");
        verify(mesaWsPublisher).publicarActualizacionMesa(VISITA_ID, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
        verify(mesaAsignarService).evaluarYActualizarEstadoMesa(VISITA_ID);
    }

    @Test
    @DisplayName("notificación inexistente → ResourceNotFoundException")
    void notificacionNoExiste_lanzaNotFound() {
        when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.servirPlatos(99L, "mesero@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(mesaAsignarService, never()).evaluarYActualizarEstadoMesa(any());
    }

    @Test
    @DisplayName("tipo distinto a PLATOS_LISTOS → BusinessException INVALID_STATE")
    void tipoIncorrecto_lanzaBusinessException() {
        Notificacion n = notificacionConComanda(
                TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA, comandaListo(EstacionComanda.COCINA));
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.servirPlatos(50L, "mesero@test.com"))
                .isInstanceOf(BusinessException.class);
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("notificación ya ATENDIDA → BusinessException INVALID_STATE")
    void yaAtendida_lanzaBusinessException() {
        Notificacion n = notificacionConComanda(
                TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ATENDIDA, comandaListo(EstacionComanda.COCINA));
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.servirPlatos(50L, "mesero@test.com"))
                .isInstanceOf(BusinessException.class);
        verify(comandaRepository, never()).save(any());
    }

    @Test
    @DisplayName("notificación sin comanda asociada → BusinessException BUSINESS_ERROR")
    void sinComanda_lanzaBusinessException() {
        Notificacion n = notificacionConComanda(
                TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA, null);
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.servirPlatos(50L, "mesero@test.com"))
                .isInstanceOf(BusinessException.class);
        verify(wsPublisher, never()).publicarComandaCompletada(any(), any());
    }
}
```

`@Nested` para servirBebidas (paralelo a servirPlatos):

```java
@Nested
@DisplayName("servirBebidas")
class ServirBebidas {

    @Test
    @DisplayName("happy path → comanda BARRA COMPLETADO, WS BARRA publicado y evaluador llamado")
    void bebidasListasActiva_completaComandaBarra() {
        Comanda comanda = comandaListo(EstacionComanda.BARRA);
        Notificacion n = notificacionConComanda(
                TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA, comanda);
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
        when(notificacionRepository.save(any())).thenReturn(n);
        when(comandaRepository.save(any())).thenReturn(comanda);

        notificacionService.servirBebidas(50L, "mesero@test.com");

        assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.COMPLETADO);
        verify(wsPublisher).publicarComandaCompletada(80L, "BARRA");
        verify(mesaWsPublisher).publicarActualizacionMesa(VISITA_ID, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
        verify(mesaAsignarService).evaluarYActualizarEstadoMesa(VISITA_ID);
    }

    @Test
    @DisplayName("notificación inexistente → ResourceNotFoundException")
    void notificacionNoExiste_lanzaNotFound() {
        when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.servirBebidas(99L, "mesero@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("tipo distinto a BEBIDAS_LISTAS → BusinessException INVALID_STATE")
    void tipoIncorrecto_lanzaBusinessException() {
        Notificacion n = notificacionConComanda(
                TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA, comandaListo(EstacionComanda.COCINA));
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.servirBebidas(50L, "mesero@test.com"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("notificación ya ATENDIDA → BusinessException INVALID_STATE")
    void yaAtendida_lanzaBusinessException() {
        Notificacion n = notificacionConComanda(
                TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ATENDIDA, comandaListo(EstacionComanda.BARRA));
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.servirBebidas(50L, "mesero@test.com"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("notificación sin comanda asociada → BusinessException BUSINESS_ERROR")
    void sinComanda_lanzaBusinessException() {
        Notificacion n = notificacionConComanda(
                TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA, null);
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.servirBebidas(50L, "mesero@test.com"))
                .isInstanceOf(BusinessException.class);
    }
}
```

`@Nested` para atenderCambio:

```java
@Nested
@DisplayName("atenderCambio")
class AtenderCambio {

    @Test
    @DisplayName("happy path → notificación ATENDIDA, devuelve comandaId, NO evalúa estado de mesa")
    void cambioActivo_atiendeYRetornaComandaId() {
        Comanda comanda = Comanda.builder()
                .comandaId(80L)
                .comandaEstado(EstadoComanda.PENDIENTE)
                .build();
        Notificacion n = notificacionConComanda(
                TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA, comanda);
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
        when(notificacionRepository.save(any())).thenReturn(n);

        AtenderCambioResponse res = notificacionService.atenderCambio(50L, "mesero@test.com");

        assertThat(res.getComandaId()).isEqualTo(80L);
        assertThat(n.getNotificacionEstado()).isEqualTo(EstadoNotificacion.ATENDIDA);
        // CA-06 no cambia estado de comanda ni evalúa estado de mesa, pero SÍ refresca el mapa
        assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.PENDIENTE);
        verify(mesaWsPublisher).publicarActualizacionMesa(VISITA_ID, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
        verify(mesaAsignarService, never()).evaluarYActualizarEstadoMesa(any());
        verify(wsPublisher, never()).publicarComandaCompletada(any(), any());
    }

    @Test
    @DisplayName("notificación inexistente → ResourceNotFoundException")
    void notificacionNoExiste_lanzaNotFound() {
        when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.atenderCambio(99L, "mesero@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("tipo distinto a CAMBIO → BusinessException INVALID_STATE")
    void tipoIncorrecto_lanzaBusinessException() {
        Notificacion n = notificacionConComanda(
                TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA, comandaListo(EstacionComanda.COCINA));
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.atenderCambio(50L, "mesero@test.com"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("notificación ya ATENDIDA → BusinessException INVALID_STATE")
    void yaAtendida_lanzaBusinessException() {
        Notificacion n = notificacionConComanda(
                TipoNotificacion.CAMBIO, EstadoNotificacion.ATENDIDA, comandaListo(EstacionComanda.COCINA));
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.atenderCambio(50L, "mesero@test.com"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("notificación sin comanda → BusinessException BUSINESS_ERROR")
    void sinComanda_lanzaBusinessException() {
        Notificacion n = notificacionConComanda(
                TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA, null);
        when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.atenderCambio(50L, "mesero@test.com"))
                .isInstanceOf(BusinessException.class);
    }
}
```

- [ ] **Step 3: Ejecutar tests (deben fallar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -pl backend -Dtest="NotificacionServiceTest" -q
```

- [ ] **Step 4: Inyectar nuevas dependencias en `NotificacionService`**

Añadir como `private final`:

```java
private final ComandaRepository comandaRepository;
private final MesaAsignarService mesaAsignarService;
private final MesaWsPublisher mesaWsPublisher;
```

Imports adicionales:
```java
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaAsignarService;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.AtenderCambioResponse;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
```

- [ ] **Step 5: Implementar los 3 métodos (TDD — Green)**

Añadir tras `atenderAsistencia`:

```java
/**
 * Registra que el mesero sirvió los platos listos de una comanda (CA-04).
 *
 * <p>Flujo:
 * <ol>
 *   <li>Localiza la notificación; lanza {@link ResourceNotFoundException} si no existe.</li>
 *   <li>Valida que sea de tipo {@code PLATOS_LISTOS} y esté {@code ACTIVA}.</li>
 *   <li>Recupera la comanda asociada; si es {@code null}, lanza {@link BusinessException}.</li>
 *   <li>Marca la comanda como {@code COMPLETADO} y la notificación como {@code ATENDIDA}.</li>
 *   <li>Publica al tópico {@code /topic/comandas/completado} para que el dashboard
 *       del cocinero elimine la comanda de la columna "Listas".</li>
 *   <li>Invoca el evaluador de estado de mesa para una posible transición a {@code ATENDIDA}.</li>
 * </ol>
 *
 * <p>Cualquier mesero o admin puede ejecutar esta operación; no se valida
 * ownership del mesero asignado a la mesa.
 *
 * @param notificacionId identificador de la notificación PLATOS_LISTOS
 * @param emailEmpleado  correo del empleado autenticado (registro/auditoría)
 * @throws ResourceNotFoundException si la notificación no existe
 * @throws BusinessException         si el tipo no es PLATOS_LISTOS, ya está atendida o no tiene comanda
 */
@Transactional
public void servirPlatos(Long notificacionId, String emailEmpleado) {

    Notificacion notificacion = notificacionRepository.findById(notificacionId)
            .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificacionId));

    // Solo notificaciones de platos listos pueden activar este flujo
    if (notificacion.getNotificacionTipo() != TipoNotificacion.PLATOS_LISTOS) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                "La notificación no es de tipo PLATOS_LISTOS.", HttpStatus.CONFLICT);
    }

    // Una notificación ATENDIDA no se puede volver a procesar
    if (notificacion.getNotificacionEstado() != EstadoNotificacion.ACTIVA) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                "La notificación ya fue atendida.", HttpStatus.CONFLICT);
    }

    Comanda comanda = obtenerComandaObligatoria(notificacion);

    comanda.setComandaEstado(EstadoComanda.COMPLETADO);
    comandaRepository.save(comanda);

    notificacion.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
    notificacionRepository.save(notificacion);

    // Notifica al dashboard del cocinero para eliminar la comanda de "Listas"
    wsPublisher.publicarComandaCompletada(comanda.getComandaId(), comanda.getComandaEstacion().name());

    Long visitaId = notificacion.getMesa().getVisitaId();

    // Refresca el mapa de mesas de TODOS los meseros (eliminar ícono de platos listos).
    // Se reutiliza el método existente con TipoEventoMesa.NOTIFICACION; el frontend
    // re-consulta GET /api/mesas tras recibir la señal.
    mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

    // Re-evalúa si la mesa puede pasar a ATENDIDA (puede emitir un segundo evento WS si transiciona)
    mesaAsignarService.evaluarYActualizarEstadoMesa(visitaId);
}

/**
 * Registra que el mesero sirvió las bebidas listas de una comanda (CA-05).
 *
 * <p>Idéntico a {@link #servirPlatos(Long, String)} pero valida tipo {@code BEBIDAS_LISTAS}
 * y publica la estación {@code BARRA} en el evento WS.
 *
 * @param notificacionId identificador de la notificación BEBIDAS_LISTAS
 * @param emailEmpleado  correo del empleado autenticado
 * @throws ResourceNotFoundException si la notificación no existe
 * @throws BusinessException         si el tipo no es BEBIDAS_LISTAS, ya está atendida o no tiene comanda
 */
@Transactional
public void servirBebidas(Long notificacionId, String emailEmpleado) {

    Notificacion notificacion = notificacionRepository.findById(notificacionId)
            .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificacionId));

    if (notificacion.getNotificacionTipo() != TipoNotificacion.BEBIDAS_LISTAS) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                "La notificación no es de tipo BEBIDAS_LISTAS.", HttpStatus.CONFLICT);
    }

    if (notificacion.getNotificacionEstado() != EstadoNotificacion.ACTIVA) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                "La notificación ya fue atendida.", HttpStatus.CONFLICT);
    }

    Comanda comanda = obtenerComandaObligatoria(notificacion);

    comanda.setComandaEstado(EstadoComanda.COMPLETADO);
    comandaRepository.save(comanda);

    notificacion.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
    notificacionRepository.save(notificacion);

    // Notifica al dashboard del bartender para eliminar la comanda de "Listas"
    wsPublisher.publicarComandaCompletada(comanda.getComandaId(), comanda.getComandaEstacion().name());

    Long visitaId = notificacion.getMesa().getVisitaId();

    // Refresca el mapa de mesas de TODOS los meseros (eliminar ícono de bebidas listas).
    mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

    mesaAsignarService.evaluarYActualizarEstadoMesa(visitaId);
}

/**
 * Atiende una notificación de cambio de comanda (CA-06).
 *
 * <p>Marca la notificación como {@code ATENDIDA} y devuelve el {@code comandaId}
 * para que el frontend cargue la comanda en modo edición.
 *
 * <p>A diferencia de {@code servirPlatos}/{@code servirBebidas}, este flujo:
 * <ul>
 *   <li>NO cambia el estado de la comanda.</li>
 *   <li>NO publica eventos al dashboard de producción.</li>
 *   <li>NO evalúa transición de estado de mesa.</li>
 * </ul>
 *
 * @param notificacionId identificador de la notificación CAMBIO
 * @param emailEmpleado  correo del empleado autenticado
 * @return DTO con el ID de la comanda a modificar
 * @throws ResourceNotFoundException si la notificación no existe
 * @throws BusinessException         si el tipo no es CAMBIO, ya está atendida o no tiene comanda
 */
@Transactional
public AtenderCambioResponse atenderCambio(Long notificacionId, String emailEmpleado) {

    Notificacion notificacion = notificacionRepository.findById(notificacionId)
            .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificacionId));

    if (notificacion.getNotificacionTipo() != TipoNotificacion.CAMBIO) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                "La notificación no es de tipo CAMBIO.", HttpStatus.CONFLICT);
    }

    if (notificacion.getNotificacionEstado() != EstadoNotificacion.ACTIVA) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                "La notificación ya fue atendida.", HttpStatus.CONFLICT);
    }

    Comanda comanda = obtenerComandaObligatoria(notificacion);

    notificacion.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
    notificacionRepository.save(notificacion);

    // Refresca el mapa de mesas de TODOS los meseros (eliminar ícono de cambio).
    mesaWsPublisher.publicarActualizacionMesa(
            notificacion.getMesa().getVisitaId(),
            MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

    return AtenderCambioResponse.builder()
            .comandaId(comanda.getComandaId())
            .build();
}

/**
 * Devuelve la comanda asociada a la notificación o lanza {@link BusinessException}
 * si la relación es {@code null}. Las notificaciones {@code PLATOS_LISTOS},
 * {@code BEBIDAS_LISTAS} y {@code CAMBIO} siempre deben tener una comanda asignada
 * por el flujo de creación (cocinero, bartender o cliente).
 *
 * @param notificacion notificación de la que se extrae la comanda
 * @return comanda no nula
 * @throws BusinessException si {@code notificacion.getComanda() == null}
 */
private Comanda obtenerComandaObligatoria(Notificacion notificacion) {
    Comanda comanda = notificacion.getComanda();
    if (comanda == null) {
        throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                "La notificación no tiene una comanda asociada.", HttpStatus.BAD_REQUEST);
    }
    return comanda;
}
```

- [ ] **Step 6: Aprovechar el touch para corregir gap WS en `atenderAsistencia` existente**

Localizar el final del método `atenderAsistencia` (después de `wsPublisher.publicarAsistenciaAtendida(...)`) y añadir la publicación al mapa reutilizando el método existente:

```java
// Refresca el mapa de mesas de TODOS los meseros (eliminar ícono de campana)
mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
```

Añadir test correspondiente al `@Nested AtenderAsistencia` existente en `NotificacionServiceTest`:

```java
@Test
@DisplayName("publica WS al mapa de mesas para que todos los meseros eliminen el ícono")
void publicaRefreshMapaMesas() {
    Mesa mesa = mesaConMesero();
    Notificacion notif = Notificacion.builder()
            .notificacionId(50L)
            .notificacionEstado(EstadoNotificacion.ACTIVA)
            .notificacionTipo(TipoNotificacion.ATENCION)
            .mesa(mesa).empleado(mesa.getMesero()).build();

    when(notificacionRepository.findById(50L)).thenReturn(Optional.of(notif));
    when(notificacionRepository.save(any())).thenReturn(notif);

    notificacionService.atenderAsistencia(50L, "mesero@test.com");

    verify(mesaWsPublisher).publicarActualizacionMesa(
            VISITA_ID, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
}
```

- [ ] **Step 7: Ejecutar tests (deben pasar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -pl backend -Dtest="NotificacionServiceTest" -q
```

Resultado esperado: BUILD SUCCESS — todos los tests existentes + 16 nuevos en verde.

- [ ] **Step 8: Commit**

```
feat(notificaciones): implementar servirPlatos, servirBebidas, atenderCambio + WS refresh mapa mesas
```

---

## Task 7: Controller — 3 endpoints PATCH

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionController.java`
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionControllerTest.java`

- [ ] **Step 1: Añadir tests al `NotificacionControllerTest` existente**

Imports adicionales a añadir:

```java
import co.edu.unicauca.backend.modules.notificaciones.dto.response.AtenderCambioResponse;
import static org.mockito.Mockito.when;
```

Añadir 3 `@Nested` tras el existente `AtenderAsistencia`:

```java
@Nested
@DisplayName("PATCH /api/notificaciones/{notificacionId}/servir-platos")
class ServirPlatos {

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("MESERO sirve platos válido → 200 OK con mensaje")
    void servirOk() throws Exception {
        doNothing().when(notificacionService).servirPlatos(eq(50L), any());

        mockMvc.perform(patch("/api/notificaciones/50/servir-platos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Platos servidos."));
    }

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("Notificación inexistente → 404 Not Found")
    void notificacionNoExiste_retorna404() throws Exception {
        doThrow(new ResourceNotFoundException("Notificacion", 99L))
                .when(notificacionService).servirPlatos(eq(99L), any());

        mockMvc.perform(patch("/api/notificaciones/99/servir-platos"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("Tipo incorrecto → 409 Conflict")
    void tipoIncorrecto_retorna409() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_STATE,
                "La notificación no es de tipo PLATOS_LISTOS.", HttpStatus.CONFLICT))
                .when(notificacionService).servirPlatos(eq(50L), any());

        mockMvc.perform(patch("/api/notificaciones/50/servir-platos"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("Notificación ya atendida → 409 Conflict")
    void yaAtendida_retorna409() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_STATE,
                "La notificación ya fue atendida.", HttpStatus.CONFLICT))
                .when(notificacionService).servirPlatos(eq(50L), any());

        mockMvc.perform(patch("/api/notificaciones/50/servir-platos"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("Sin comanda asociada → 400 Bad Request")
    void sinComanda_retorna400() throws Exception {
        doThrow(new BusinessException(ErrorCode.BUSINESS_ERROR,
                "La notificación no tiene una comanda asociada.", HttpStatus.BAD_REQUEST))
                .when(notificacionService).servirPlatos(eq(50L), any());

        mockMvc.perform(patch("/api/notificaciones/50/servir-platos"))
                .andExpect(status().isBadRequest());
    }
}

@Nested
@DisplayName("PATCH /api/notificaciones/{notificacionId}/servir-bebidas")
class ServirBebidas {

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("MESERO sirve bebidas válido → 200 OK con mensaje")
    void servirOk() throws Exception {
        doNothing().when(notificacionService).servirBebidas(eq(50L), any());

        mockMvc.perform(patch("/api/notificaciones/50/servir-bebidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bebidas servidas."));
    }

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("Notificación inexistente → 404 Not Found")
    void notificacionNoExiste_retorna404() throws Exception {
        doThrow(new ResourceNotFoundException("Notificacion", 99L))
                .when(notificacionService).servirBebidas(eq(99L), any());

        mockMvc.perform(patch("/api/notificaciones/99/servir-bebidas"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("Notificación ya atendida → 409 Conflict")
    void yaAtendida_retorna409() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_STATE, "Ya atendida.", HttpStatus.CONFLICT))
                .when(notificacionService).servirBebidas(eq(50L), any());

        mockMvc.perform(patch("/api/notificaciones/50/servir-bebidas"))
                .andExpect(status().isConflict());
    }
}

@Nested
@DisplayName("PATCH /api/notificaciones/{notificacionId}/atender-cambio")
class AtenderCambio {

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("MESERO atiende cambio válido → 200 OK con comandaId")
    void atenderOk() throws Exception {
        AtenderCambioResponse dto = AtenderCambioResponse.builder().comandaId(80L).build();
        when(notificacionService.atenderCambio(eq(50L), any())).thenReturn(dto);

        mockMvc.perform(patch("/api/notificaciones/50/atender-cambio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Comanda lista para modificar."))
                .andExpect(jsonPath("$.data.comandaId").value(80));
    }

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("Notificación inexistente → 404 Not Found")
    void notificacionNoExiste_retorna404() throws Exception {
        doThrow(new ResourceNotFoundException("Notificacion", 99L))
                .when(notificacionService).atenderCambio(eq(99L), any());

        mockMvc.perform(patch("/api/notificaciones/99/atender-cambio"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
    @DisplayName("Tipo incorrecto → 409 Conflict")
    void tipoIncorrecto_retorna409() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_STATE,
                "La notificación no es de tipo CAMBIO.", HttpStatus.CONFLICT))
                .when(notificacionService).atenderCambio(eq(50L), any());

        mockMvc.perform(patch("/api/notificaciones/50/atender-cambio"))
                .andExpect(status().isConflict());
    }
}
```

- [ ] **Step 2: Ejecutar tests (deben fallar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -pl backend -Dtest="NotificacionControllerTest" -q
```

- [ ] **Step 3: Añadir los 3 endpoints al `NotificacionController`**

Tras el endpoint existente `atenderAsistencia`:

```java
/**
 * Registra que el mesero sirvió los platos listos de una comanda (CA-04).
 *
 * <p>Marca la notificación PLATOS_LISTOS como ATENDIDA, la comanda como COMPLETADO,
 * publica evento WS al dashboard del cocinero y evalúa una posible transición
 * automática del estado de la mesa a ATENDIDA.
 *
 * @param notificacionId identificador de la notificación PLATOS_LISTOS
 * @param authentication contexto de seguridad del request
 * @return 200 OK con mensaje de confirmación
 */
@PatchMapping("/{notificacionId}/servir-platos")
@PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
@Operation(summary = "Registrar servicio de platos (CA-04)")
public ResponseEntity<ApiResponse<Void>> servirPlatos(
        @PathVariable Long notificacionId,
        Authentication authentication) {

    notificacionService.servirPlatos(notificacionId, authentication.getName());
    return ResponseEntity.ok(ApiResponse.message("Platos servidos."));
}

/**
 * Registra que el mesero sirvió las bebidas listas de una comanda (CA-05).
 *
 * <p>Marca la notificación BEBIDAS_LISTAS como ATENDIDA, la comanda como COMPLETADO,
 * publica evento WS al dashboard del bartender y evalúa una posible transición
 * automática del estado de la mesa a ATENDIDA.
 *
 * @param notificacionId identificador de la notificación BEBIDAS_LISTAS
 * @param authentication contexto de seguridad del request
 * @return 200 OK con mensaje de confirmación
 */
@PatchMapping("/{notificacionId}/servir-bebidas")
@PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
@Operation(summary = "Registrar servicio de bebidas (CA-05)")
public ResponseEntity<ApiResponse<Void>> servirBebidas(
        @PathVariable Long notificacionId,
        Authentication authentication) {

    notificacionService.servirBebidas(notificacionId, authentication.getName());
    return ResponseEntity.ok(ApiResponse.message("Bebidas servidas."));
}

/**
 * Atiende una notificación de cambio de comanda (CA-06).
 *
 * <p>Marca la notificación CAMBIO como ATENDIDA y devuelve el {@code comandaId}
 * para que el frontend cargue la comanda en modo edición. No cambia el estado
 * de la comanda ni evalúa el estado de la mesa.
 *
 * @param notificacionId identificador de la notificación CAMBIO
 * @param authentication contexto de seguridad del request
 * @return 200 OK con el ID de la comanda a modificar
 */
@PatchMapping("/{notificacionId}/atender-cambio")
@PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
@Operation(summary = "Atender notificación de cambio de comanda (CA-06)")
public ResponseEntity<ApiResponse<AtenderCambioResponse>> atenderCambio(
        @PathVariable Long notificacionId,
        Authentication authentication) {

    AtenderCambioResponse response =
            notificacionService.atenderCambio(notificacionId, authentication.getName());
    return ResponseEntity.ok(ApiResponse.ok("Comanda lista para modificar.", response));
}
```

Import: `import co.edu.unicauca.backend.modules.notificaciones.dto.response.AtenderCambioResponse;`

- [ ] **Step 4: Ejecutar tests del controller**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -pl backend -Dtest="NotificacionControllerTest" -q
```

- [ ] **Step 5: Suite completa + cobertura JaCoCo**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean test jacoco:report -q
```

Verificar `backend/target/site/jacoco/index.html`:
- `NotificacionService` ≥ 90%
- `NotificacionController` ≥ 85%
- `MesaAsignarService` ≥ 90% (al menos para `evaluarYActualizarEstadoMesa`)

- [ ] **Step 6: Commit**

```
feat(notificaciones): añadir endpoints servir-platos, servir-bebidas, atender-cambio
```

---

## Task 8: Postman — Manual Testing (3 requests)

**Files:**
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/70-02 Servir Platos – MESERO.request.yaml`
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/70-03 Servir Bebidas – MESERO.request.yaml`
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/70-04 Atender Cambio – MESERO.request.yaml`

> **Patrón base:** copiar la estructura de `70-01 Atender Asistencia – MESERO.request.yaml` (login con `password123`, `tmpMeseroToken`, `tmpNotificacionId`, cleanup en `afterResponse`).

- [ ] **Step 1: 70-02 Servir Platos**

```yaml
$kind: http-request
name: 70-02 Servir Platos – MESERO
description: Registrar que el mesero sirvió los platos listos de una comanda (CA-04)
url: "{{baseUrl}}/api/notificaciones/{{tmpNotificacionId}}/servir-platos"
method: PATCH
headers:
  Authorization: Bearer {{tmpMeseroToken}}
scripts:
  - type: beforeRequest
    code: |-
      // Ajustar al ID de una notificación PLATOS_LISTOS activa con comanda asociada
      pm.environment.set('tmpNotificacionId', '1');

      pm.sendRequest({
        url: '{{baseUrl}}/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: 'mesero1@altoro.com',
            password: 'password123',
            forceSessionOverride: true
          })
        }
      }, (err, res) => {
        if (!err && res && res.code === 200) {
          pm.environment.set('tmpMeseroToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.environment.unset('tmpNotificacionId');
      pm.environment.unset('tmpMeseroToken');
    language: text/javascript
order: 7020
```

- [ ] **Step 2: 70-03 Servir Bebidas**

Idéntico a 70-02 cambiando:
- `name: 70-03 Servir Bebidas – MESERO`
- `description: Registrar que el mesero sirvió las bebidas listas de una comanda (CA-05)`
- `url`: `.../servir-bebidas`
- `tmpNotificacionId`: `'2'`
- `order: 7030`

- [ ] **Step 3: 70-04 Atender Cambio**

Idéntico a 70-02 cambiando:
- `name: 70-04 Atender Cambio – MESERO`
- `description: Atender notificación de cambio (CA-06). Devuelve comandaId.`
- `url`: `.../atender-cambio`
- `tmpNotificacionId`: `'3'`
- `order: 7040`

- [ ] **Step 4: Commit**

```
test(postman): añadir requests manuales 70-02, 70-03, 70-04 para servir/cambio
```

---

## Task 9: Postman — Automated Collections (cobertura completa)

**Files (3 colecciones, cada una con `definition.yaml` + 6-7 request files):**
- Create: `backend/postman/postman/collections/notificaciones/Al Toro – PATCH -api-notificaciones-{id}-servir-platos/.resources/definition.yaml`
- Create: `.../servir-platos/SP-01 ... SP-07 *.request.yaml` (7 archivos)
- Create: `.../servir-bebidas/.resources/definition.yaml` + `SB-01 ... SB-07 *.request.yaml`
- Create: `.../atender-cambio/.resources/definition.yaml` + `AC-01 ... AC-06 *.request.yaml`

> **Cobertura por colección (no solo happy path):**
> - 200 OK happy path
> - 401 sin token
> - 403 con CAJERO
> - 403 con CLIENTE
> - 404 ID inexistente
> - 409 ya atendida
> - 409 tipo incorrecto (solo SP/SB)
> - 400 sin comanda (opcional, requiere setup específico)

> **Seed requerido en V3:** Notificaciones con `comanda_id` para los 3 tipos (PLATOS_LISTOS, BEBIDAS_LISTAS, CAMBIO).

- [ ] **Step 1: Añadir seed en `V3__dev_data.sql`**

Localizar el bloque INSERT INTO restaurante.Notificacion (si existe) o añadir uno nuevo:

```sql
-- Seed para tests automatizados HE-03-HU-06 (gestionar notificaciones)
-- Asume: visita_id=1, mesero usuario_id=2, comanda_id=1 (cocina), comanda_id=2 (barra)
INSERT INTO restaurante.Notificacion
    (mesa_id, empleado_id, comanda_id, notificacion_estado, notificacion_tipo)
VALUES
    (1, 2, 1, 'ACTIVA', 'PLATOS_LISTOS'),    -- SP tests
    (1, 2, 2, 'ACTIVA', 'BEBIDAS_LISTAS'),   -- SB tests
    (1, 2, 1, 'ACTIVA', 'CAMBIO');            -- AC tests
```

Ajustar IDs según los seeds existentes en V2/V3. Documentar en environment variables: `seedNotificacionPlatosListosId`, `seedNotificacionBebidasListasId`, `seedNotificacionCambioId`.

- [ ] **Step 2: Crear `definition.yaml` para servir-platos**

```yaml
$kind: collection
name: Al Toro – PATCH /api/notificaciones/{id}/servir-platos
description: |-
  # PATCH /api/notificaciones/{id}/servir-platos

  Registra que el mesero sirvió los platos de una comanda (CA-04).

  **Criterios de Aceptación cubiertos:**
  - CA-04: Servir platos — marca notificación ATENDIDA + comanda COMPLETADO + evalúa estado mesa.

  **Control de acceso:**
  - MESERO/ADMIN: acceso permitido (cualquier mesero, sin validación de ownership).
  - CAJERO/CLIENTE: 403 Forbidden.
  - Sin token: 401 Unauthorized.

  | ID | Escenario | HTTP |
  |----|-----------|------|
  | SP-01 | MESERO sirve platos válido — 200 OK | 200 |
  | SP-02 | ADMIN sirve platos — 200 OK | 200 |
  | SP-03 | Sin token — 401 Unauthorized | 401 |
  | SP-04 | CAJERO intenta acceder — 403 Forbidden | 403 |
  | SP-05 | CLIENTE intenta acceder — 403 Forbidden | 403 |
  | SP-06 | ID inexistente — 404 Not Found | 404 |
  | SP-07 | Notificación ya atendida — 409 Conflict | 409 |

scripts:
  - type: http:beforeRequest
    code: |-
      // Login secuencial: MESERO, ADMIN, CAJERO, CLIENTE
      const logins = [
        { emailKey: 'emailMesero',  tokenKey: 'meseroToken'  },
        { emailKey: 'emailAdmin',   tokenKey: 'adminToken'   },
        { emailKey: 'emailCajero',  tokenKey: 'cajeroToken'  },
        { emailKey: 'emailCliente', tokenKey: 'clienteToken' }
      ];
      const runLogin = (index) => {
        if (index >= logins.length) return;
        const { emailKey, tokenKey } = logins[index];
        pm.sendRequest({
          url: pm.environment.get('baseUrl') + '/api/auth/login',
          method: 'POST',
          header: { 'Content-Type': 'application/json' },
          body: {
            mode: 'raw',
            raw: JSON.stringify({
              email: pm.environment.get(emailKey),
              password: pm.environment.get('passwordValida'),
              forceSessionOverride: true
            })
          }
        }, (err, res) => {
          if (err || !res || res.code !== 200) {
            console.warn('SP pre-request: login ' + emailKey + ' falló');
          } else {
            pm.environment.set(tokenKey, res.json().accessToken);
          }
          runLogin(index + 1);
        });
      };
      runLogin(0);
    language: text/javascript
order: 7100
```

- [ ] **Step 3: Crear los 7 archivos de request para servir-platos**

**SP-01 MESERO sirve platos válido – 200 OK:**

```yaml
$kind: http-request
name: SP-01 MESERO sirve platos válido – 200 OK
description: |-
  **Criterio:** CA-04 — MESERO sirve platos de comanda PLATOS_LISTOS activa.
  **Resultado esperado:** 200 OK, success=true, message="Platos servidos.".
url: "{{baseUrl}}/api/notificaciones/{{seedNotificacionPlatosListosId}}/servir-platos"
method: PATCH
headers:
  Authorization: Bearer {{meseroToken}}
scripts:
  - type: afterResponse
    code: |-
      pm.test('HTTP 200', () => pm.response.to.have.status(200));
      pm.test('success=true', () => {
        pm.expect(pm.response.json().success).to.equal(true);
      });
      pm.test('message="Platos servidos."', () => {
        pm.expect(pm.response.json().message).to.equal('Platos servidos.');
      });
    language: text/javascript
order: 7101
```

**SP-02 ADMIN sirve platos – 200 OK:** (usa `adminToken` y un seed alternativo o re-seed previo)

```yaml
$kind: http-request
name: SP-02 ADMIN sirve platos – 200 OK
description: |-
  **Criterio:** CA-04 — ADMIN puede servir platos sin restricciones.
  **Pre-condición:** Existe segunda notificación PLATOS_LISTOS activa (seedNotificacionPlatosListosId2).
  **Resultado esperado:** 200 OK.
url: "{{baseUrl}}/api/notificaciones/{{seedNotificacionPlatosListosId2}}/servir-platos"
method: PATCH
headers:
  Authorization: Bearer {{adminToken}}
scripts:
  - type: afterResponse
    code: |-
      pm.test('HTTP 200', () => pm.response.to.have.status(200));
    language: text/javascript
order: 7102
```

**SP-03 Sin token – 401:**

```yaml
$kind: http-request
name: SP-03 Sin token de autenticación – 401 Unauthorized
description: |-
  **Objetivo:** Verificar que la ausencia de JWT retorna 401.
  **Resultado esperado:** 401 Unauthorized.
url: "{{baseUrl}}/api/notificaciones/1/servir-platos"
method: PATCH
scripts:
  - type: afterResponse
    code: |-
      pm.test('HTTP 401', () => pm.response.to.have.status(401));
    language: text/javascript
order: 7103
```

**SP-04 CAJERO intenta acceder – 403:**

```yaml
$kind: http-request
name: SP-04 CAJERO intenta acceder – 403 Forbidden
description: |-
  **Objetivo:** Verificar que CAJERO no puede servir platos.
  **Resultado esperado:** 403 Forbidden, code="AUTH-002".
url: "{{baseUrl}}/api/notificaciones/1/servir-platos"
method: PATCH
headers:
  Authorization: Bearer {{cajeroToken}}
scripts:
  - type: afterResponse
    code: |-
      pm.test('HTTP 403', () => pm.response.to.have.status(403));
      pm.test('code=AUTH-002', () => {
        pm.expect(pm.response.json().code).to.equal('AUTH-002');
      });
    language: text/javascript
order: 7104
```

**SP-05 CLIENTE intenta acceder – 403:** (estructura idéntica a SP-04 con `clienteToken`, order 7105).

**SP-06 ID inexistente – 404:**

```yaml
$kind: http-request
name: SP-06 ID de notificación inexistente – 404 Not Found
description: |-
  **Objetivo:** Verificar que un ID inexistente retorna 404.
  **Resultado esperado:** 404 Not Found, code="ENT-001".
url: "{{baseUrl}}/api/notificaciones/999999/servir-platos"
method: PATCH
headers:
  Authorization: Bearer {{meseroToken}}
scripts:
  - type: afterResponse
    code: |-
      pm.test('HTTP 404', () => pm.response.to.have.status(404));
      pm.test('code=ENT-001', () => {
        pm.expect(pm.response.json().code).to.equal('ENT-001');
      });
    language: text/javascript
order: 7106
```

**SP-07 Notificación ya atendida – 409:** (ejecutar después de SP-01 que dejó la notificación ATENDIDA, mismo `seedNotificacionPlatosListosId`)

```yaml
$kind: http-request
name: SP-07 Notificación ya atendida – 409 Conflict
description: |-
  **Pre-condición:** SP-01 ya marcó esta notificación como ATENDIDA en la misma run.
  **Resultado esperado:** 409 Conflict, code="NEG-002".
url: "{{baseUrl}}/api/notificaciones/{{seedNotificacionPlatosListosId}}/servir-platos"
method: PATCH
headers:
  Authorization: Bearer {{meseroToken}}
scripts:
  - type: afterResponse
    code: |-
      pm.test('HTTP 409', () => pm.response.to.have.status(409));
      pm.test('code=NEG-002', () => {
        pm.expect(pm.response.json().code).to.equal('NEG-002');
      });
    language: text/javascript
order: 7107
```

- [ ] **Step 4: Crear colección servir-bebidas (SB-01 a SB-07)**

Replicar la estructura de servir-platos cambiando:
- Path: `/servir-bebidas`
- Tipo: `BEBIDAS_LISTAS`
- Mensaje 200: `"Bebidas servidas."`
- Variable seed: `seedNotificacionBebidasListasId` (y `...Id2` para SB-02)
- IDs: `SB-01` a `SB-07`
- Order: 7200, 7201..7207
- Description: ajustar a CA-05.

- [ ] **Step 5: Crear colección atender-cambio (AC-01 a AC-06)**

> **Diferencias respecto a SP/SB:**
> - Solo 6 casos (no aplica "tipo incorrecto" como caso separado porque CAMBIO no transiciona estado de comanda; el flujo de error 400 aplica si no hay comanda).
> - Happy path verifica `data.comandaId`.

**AC-01 happy path** (verifica `data.comandaId`):

```yaml
$kind: http-request
name: AC-01 MESERO atiende cambio válido – 200 OK
description: |-
  **Criterio:** CA-06 — MESERO atiende notificación CAMBIO y obtiene comandaId.
  **Resultado esperado:** 200 OK, data.comandaId es un número, message="Comanda lista para modificar.".
url: "{{baseUrl}}/api/notificaciones/{{seedNotificacionCambioId}}/atender-cambio"
method: PATCH
headers:
  Authorization: Bearer {{meseroToken}}
scripts:
  - type: afterResponse
    code: |-
      pm.test('HTTP 200', () => pm.response.to.have.status(200));
      pm.test('success=true', () => {
        pm.expect(pm.response.json().success).to.equal(true);
      });
      pm.test('message="Comanda lista para modificar."', () => {
        pm.expect(pm.response.json().message).to.equal('Comanda lista para modificar.');
      });
      pm.test('data.comandaId es un número', () => {
        const body = pm.response.json();
        pm.expect(body.data).to.have.property('comandaId');
        pm.expect(body.data.comandaId).to.be.a('number');
      });
    language: text/javascript
order: 7301
```

**AC-02 ADMIN, AC-03 sin token, AC-04 CAJERO, AC-05 CLIENTE, AC-06 ya atendida:** Replicar la estructura SP-02 a SP-07 ajustando path, mensaje, variables y order (7300, 7302..7306).

- [ ] **Step 6: Definition.yaml para servir-bebidas y atender-cambio**

Replicar `definition.yaml` de servir-platos cambiando:
- `name`: `Al Toro – PATCH /api/notificaciones/{id}/servir-bebidas` (resp. `/atender-cambio`)
- Tabla de escenarios actualizada
- `order`: 7200 (resp. 7300)

- [ ] **Step 7: Commit**

```
test(postman): añadir colecciones automatizadas SP, SB, AC con cobertura completa
```

---

## Verificación Final

- [ ] **Suite completa de tests + JaCoCo**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean test jacoco:report -q
```

Verificaciones obligatorias en `backend/target/site/jacoco/index.html`:
- `co.edu.unicauca.backend.modules.notificaciones.service.NotificacionService` ≥ 90%
- `co.edu.unicauca.backend.modules.notificaciones.controller.NotificacionController` ≥ 85%
- `co.edu.unicauca.backend.modules.mesas_comandas.service.MesaAsignarService` ≥ 90%
- Branches coverage en `evaluarYActualizarEstadoMesa` = 100% (4 ramas + happy path).

- [ ] **Compilar JAR**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean package -DskipTests -q
```

- [ ] **Ejecución manual end-to-end**

1. `docker compose down -v && docker compose up --build`
2. Importar colección manual y ejecutar 70-02, 70-03, 70-04 con IDs reales del seed.
3. Importar las 3 colecciones automatizadas y ejecutarlas en orden: servir-platos → servir-bebidas → atender-cambio.

---

## Scope — NO incluido en este plan

- **Frontend Angular:** visualización de íconos en el mapa, suscripción WS, carga de comanda en modo edición.
- **Creación de notificaciones PLATOS_LISTOS / BEBIDAS_LISTAS / CAMBIO** por parte del cocinero/bartender (HE-04-HU-03-CA-05 y CA-02). Esas HUs se encargarán de crear las notificaciones con `comanda_id` no nulo.
- **Suscripción frontend a `/topic/comandas/completado`** para eliminar la comanda del dashboard cocinero/bartender (CA-06 frontend).
- **Validación de ownership de mesero** sobre la mesa: cualquier mesero o admin puede atender cualquier notificación.

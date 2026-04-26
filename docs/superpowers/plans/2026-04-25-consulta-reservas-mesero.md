# HE-03-HU-01 — Consulta de Reservas para Meseros

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar el módulo de consulta de reservas para meseros, permitiendo visualizar reservas activas del día (Confirmadas/Pendientes), buscar por identificador o fecha, ver detalles completos con pre-orden y modificaciones, y recibir actualizaciones en tiempo real vía WebSocket cuando se crean o modifican reservas (HE-03-HU-01, CA-01 a CA-09).

**Architecture:** El mesero autentica vía JWT con rol `MESERO` y accede a `GET /api/reservas/mesero/consulta` para obtener el listado del día actual por defecto, agrupado por zona con resumen numérico. Puede filtrar por `?fecha=` o `?identificador=` para búsquedas específicas. `GET /api/reservas/mesero/{reservaId}/detalle` retorna la información completa de una reserva reutilizando `ReservaDetalleResponse` extendido con `clienteTelefono`. Las actualizaciones en tiempo real se entregan vía STOMP/WebSocket: el servidor publica en `/topic/reservas/cambios` cuando se crea o modifica una reserva activa (estados CONFIRMADA/PENDIENTE).

**Multi-role:** Los endpoints son exclusivos para `MESERO` y `ADMIN`. No se requiere validación de ownership porque los meseros pueden consultar todas las reservas activas del restaurante.

**Tech Stack:** Spring Boot 3.5, Java 21, Spring WebSocket (STOMP in-memory broker), Spring Security JWT, JPA/Hibernate, JUnit 5 + Mockito, AssertJ, Postman YAML.

---

## File Map

### Archivos nuevos

| Path | Responsabilidad |
|------|-----------------|
| `backend/src/main/java/.../reservas/dto/response/ReservaConsultaResponse.java` | DTO de ítem en el listado de reservas para meseros |
| `backend/src/main/java/.../reservas/dto/response/ResumenZonaResponse.java` | DTO de resumen por zona (zona + cantidad de reservas) |
| `backend/src/main/java/.../reservas/dto/response/ListadoReservasResponse.java` | DTO wrapper que agrupa listado + resúmenes por zona |
| `backend/src/main/java/.../reservas/mapper/ReservaConsultaMapper.java` | Mapper entity→DTO para consultas de meseros |
| `backend/src/main/java/.../reservas/service/ReservaConsultaService.java` | Lógica de consulta de reservas para meseros |
| `backend/src/main/java/.../reservas/controller/ReservaConsultaController.java` | Endpoints `GET /api/reservas/mesero/consulta` y `/mesero/{id}/detalle` |
| `backend/src/main/java/.../notificaciones/dto/ws/ReservaActualizadaWsMessage.java` | Payload WS para cambios en reservas (CA-09) |
| `backend/src/test/java/.../reservas/service/ReservaConsultaServiceTest.java` | Tests unitarios de ReservaConsultaService |
| `backend/src/test/java/.../reservas/controller/ReservaConsultaControllerTest.java` | Tests unitarios de ReservaConsultaController |
| `backend/src/test/java/.../reservas/mapper/ReservaConsultaMapperTest.java` | Tests unitarios de ReservaConsultaMapper |

### Archivos modificados

| Path | Cambio |
|------|--------|
| `backend/src/main/java/.../reservas/dto/response/ReservaDetalleResponse.java` | Añadir campo opcional `clienteTelefono` para consultas de meseros |
| `backend/src/main/java/.../reservas/repository/ReservaRepository.java` | Añadir queries optimizadas: `findReservasActivasDelDia`, `findReservasActivasPorIdentificador` |
| `backend/src/main/java/.../reservas/mapper/ReservaMapper.java` | Actualizar `toDetalleResponse` para incluir `clienteTelefono` cuando está disponible |
| `backend/src/main/java/.../reservas/service/ReservaService.java` | Inyectar `NotificacionWsPublisher`; publicar `ReservaActualizadaWsMessage` al crear/modificar reserva |
| `backend/src/main/java/.../notificaciones/service/NotificacionWsPublisher.java` | Añadir método `publicarReservaActualizada(ReservaActualizadaWsMessage)` |
| `backend/CLAUDE.md` | Actualizar tabla de endpoints con nuevos endpoints de consulta de meseros |

---

Paquete base: `co.edu.unicauca.backend`
Prefijo de módulo: `modules/`

---

## Task 1: Extender ReservaDetalleResponse y crear DTOs de listado

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaDetalleResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaConsultaResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ResumenZonaResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ListadoReservasResponse.java`

- [ ] **Step 1: Añadir campo clienteTelefono a ReservaDetalleResponse**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaDetalleResponse.java
// Añadir después del campo 'totalAbonado' y antes del cierre de la clase:

/** Teléfono del cliente; {@code null} en consultas de cliente. */
private final String clienteTelefono;
```

- [ ] **Step 2: Crear ReservaConsultaResponse**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaConsultaResponse.java
package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO de ítem en el listado de reservas para meseros.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservaConsultaResponse {

    /** Identificador único de la reserva. */
    private final Long reservaId;

    /** Nombre completo del cliente. */
    private final String clienteNombre;

    /** Identificador de la zona; {@code null} si no fue asignada. */
    private final Long zonaId;

    /** Nombre de la zona; {@code null} si no fue asignada. */
    private final String zonaNombre;

    /** Nombre de la decoración; {@code null} si no fue asignada. */
    private final String decoracionNombre;

    /** Hora de llegada en formato {@code HH:mm} (ej: {@code "19:30"}). */
    private final String horaLlegada;

    /** Número de comensales de la reserva. */
    private final Integer numeroPersonas;

    /** Estado actual de la reserva ({@code CONFIRMADA} o {@code PENDIENTE}). */
    private final String estado;
}
```

- [ ] **Step 3: Crear ResumenZonaResponse**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ResumenZonaResponse.java
package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO de resumen por zona en el módulo de consulta de reservas.
 *
 * <p>Muestra el identificador de la zona, su nombre y la cantidad total de reservas
 * activas (CONFIRMADA/PENDIENTE) en esa zona para la fecha consultada.
 */
@Getter
@Builder
public class ResumenZonaResponse {

    /** Identificador de la zona. */
    private final Long zonaId;

    /** Nombre de la zona. */
    private final String zonaNombre;

    /** Cantidad de reservas activas en la zona para la fecha consultada. */
    private final Integer cantidadReservas;
}
```

- [ ] **Step 4: Crear ListadoReservasResponse**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ListadoReservasResponse.java
package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * DTO wrapper que agrupa el listado de reservas y los resúmenes por zona.
 */
@Getter
@Builder
public class ListadoReservasResponse {

    /** Lista de reservas activas ordenadas por hora de llegada ascendente. */
    private final List<ReservaConsultaResponse> reservas;

    /** Resumen de cantidad de reservas por zona. */
    private final List<ResumenZonaResponse> resumenZonas;
}
```

- [ ] **Step 5: Compilar para verificar sin errores**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```
Resultado esperado: BUILD SUCCESS.

- [ ] **Step 6: Commit manual**

**Esperar a que el usuario haga commit manualmente de:**
- `ReservaDetalleResponse.java` (modificado)
- `ReservaConsultaResponse.java` (nuevo)
- `ResumenZonaResponse.java` (nuevo)
- `ListadoReservasResponse.java` (nuevo)

Mensaje sugerido: `feat(reservas): extender ReservaDetalleResponse y añadir DTOs de listado para meseros`

---

## Task 2: Queries optimizadas en ReservaRepository

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/repository/ReservaRepository.java`

- [ ] **Step 1: Añadir query `findReservasActivasDelDia`**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/reservas/repository/ReservaRepository.java
// Añadir al final de la interfaz, antes del cierre:

/**
 * Devuelve todas las reservas activas (CONFIRMADA/PENDIENTE) de un día específico,
 * ordenadas por hora de llegada ascendente.
 *
 * <p>Usado por meseros para consultar el listado del día.
 *
 * @param inicio inicio del día (00:00:00)
 * @param fin fin del día (23:59:59)
 * @param estados    estados a considerar como activos
 * @return lista de reservas activas del día; vacía si no hay ninguna
 */
@Query("SELECT r FROM Reserva r " +
       "LEFT JOIN FETCH r.cliente c " +
       "LEFT JOIN FETCH c.usuario u " +
       "LEFT JOIN FETCH r.zona z " +
       "LEFT JOIN FETCH r.decoracion d " +
       "WHERE r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
       "AND r.reservaEstado IN :estados " +
       "ORDER BY r.reservaFechaHoraLlegada ASC")
List<Reserva> findReservasActivasDelDia(
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin,
        @Param("estados") List<EstadoReserva> estados);

/**
 * Devuelve todas las reservas activas que coincidan con un identificador específico,
 * sin importar la fecha.
 *
 * <p>Usado por meseros para buscar reservas por ID.
 *
 * @param reservaId  identificador a buscar
 * @param estados    estados a considerar como activos
 * @return lista de reservas que coincidan; vacía si no se encuentra
 */
@Query("SELECT r FROM Reserva r " +
       "LEFT JOIN FETCH r.cliente c " +
       "LEFT JOIN FETCH c.usuario u " +
       "LEFT JOIN FETCH r.zona z " +
       "LEFT JOIN FETCH r.decoracion d " +
       "WHERE r.reservaId = :reservaId " +
       "AND r.reservaEstado IN :estados " +
       "ORDER BY r.reservaFechaHoraLlegada ASC")
List<Reserva> findReservasActivasPorIdentificador(
        @Param("reservaId") Long reservaId,
        @Param("estados") List<EstadoReserva> estados);
```

- [ ] **Step 2: Compilar para verificar sin errores**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```
Resultado esperado: BUILD SUCCESS.

- [ ] **Step 3: Commit manual**

**Esperar a que el usuario haga commit manualmente de:**
- `ReservaRepository.java` (modificado)

Mensaje sugerido: `feat(reservas): añadir queries optimizadas para consulta de meseros`

---

## Task 3: ReservaConsultaMapper (TDD)

**Files:**
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaConsultaMapperTest.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaConsultaMapper.java`

- [ ] **Step 1: Escribir test para `toConsultaResponse`**

```java
// backend/src/test/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaConsultaMapperTest.java
package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaConsultaResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Usuario;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link ReservaConsultaMapper}.
 */
class ReservaConsultaMapperTest {

    private ReservaConsultaMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReservaConsultaMapper();
    }

    @Test
    void toConsultaResponse_conZonaYDecoracion_retornaDTOCompleto() {
        // Given
        Usuario usuario = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("cliente@altoro.com")
                .build();

        Cliente cliente = Cliente.builder()
                .usuarioId(1L)
                .usuario(usuario)
                .clienteNombre("Juan Pérez")
                .build();

        Zona zona = Zona.builder()
                .zonaId(10L)
                .zonaNombre("Zona VIP")
                .build();

        Decoracion decoracion = Decoracion.builder()
                .decoracionId(5L)
                .decoracionNombre("Romántica")
                .build();

        Reserva reserva = Reserva.builder()
                .reservaId(100L)
                .cliente(cliente)
                .zona(zona)
                .decoracion(decoracion)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 4, 25, 19, 30))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();

        // When
        ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

        // Then
        assertThat(response.getReservaId()).isEqualTo(100L);
        assertThat(response.getClienteNombre()).isEqualTo("Juan Pérez");
        assertThat(response.getZonaId()).isEqualTo(10L);
        assertThat(response.getZonaNombre()).isEqualTo("Zona VIP");
        assertThat(response.getDecoracionNombre()).isEqualTo("Romántica");
        assertThat(response.getHoraLlegada()).isEqualTo("19:30");
        assertThat(response.getNumeroPersonas()).isEqualTo(4);
        assertThat(response.getEstado()).isEqualTo("CONFIRMADA");
    }

    @Test
    void toConsultaResponse_sinZonaNiDecoracion_retornaNull() {
        // Given
        Usuario usuario = Usuario.builder()
                .usuarioId(2L)
                .usuarioEmail("cliente2@altoro.com")
                .build();

        Cliente cliente = Cliente.builder()
                .usuarioId(2L)
                .usuario(usuario)
                .clienteNombre("María López")
                .build();

        Reserva reserva = Reserva.builder()
                .reservaId(101L)
                .cliente(cliente)
                .zona(null)
                .decoracion(null)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 4, 25, 20, 0))
                .reservaNumeroPersonas(2)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        // When
        ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

        // Then
        assertThat(response.getReservaId()).isEqualTo(101L);
        assertThat(response.getClienteNombre()).isEqualTo("María López");
        assertThat(response.getZonaId()).isNull();
        assertThat(response.getZonaNombre()).isNull();
        assertThat(response.getDecoracionNombre()).isNull();
        assertThat(response.getHoraLlegada()).isEqualTo("20:00");
        assertThat(response.getNumeroPersonas()).isEqualTo(2);
        assertThat(response.getEstado()).isEqualTo("PENDIENTE");
    }
}
```

- [ ] **Step 2: Ejecutar tests para verificar que fallan**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=ReservaConsultaMapperTest -q
```
Resultado esperado: FAIL con "ReservaConsultaMapper cannot be resolved".

- [ ] **Step 3: Implementar ReservaConsultaMapper**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaConsultaMapper.java
package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.reservas.dto.response.*;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entidades de Reserva en DTOs de consulta para meseros.
 *
 * <p>Transforma {@link Reserva} en los formatos necesarios para el listado y
 * reutiliza {@link ReservaDetalleResponse} para el detalle completo.
 *
 * @see ReservaConsultaService
 */
@Component
public class ReservaConsultaMapper {

    private static final DateTimeFormatter FORMATTER_TIME = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Convierte una {@link Reserva} en el DTO de ítem del listado.
     *
     * <p>Campos opcionales (zona, decoración) se omiten ({@code null}) si no fueron asignados.
     *
     * @param reserva entidad de reserva a convertir
     * @return {@link ReservaConsultaResponse} con los campos del listado
     */
    public ReservaConsultaResponse toConsultaResponse(Reserva reserva) {
        return ReservaConsultaResponse.builder()
                .reservaId(reserva.getReservaId())
                .clienteNombre(reserva.getCliente().getClienteNombre())
                .zonaId(reserva.getZona() != null ? reserva.getZona().getZonaId() : null)
                .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
                .decoracionNombre(reserva.getDecoracion() != null 
                        ? reserva.getDecoracion().getDecoracionNombre() 
                        : null)
                .horaLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER_TIME))
                .numeroPersonas(reserva.getReservaNumeroPersonas())
                .estado(reserva.getReservaEstado().name())
                .build();
    }

    /**
     * Construye el DTO de resumen por zona a partir del conteo de reservas.
     *
     * @param zonaId           identificador de la zona
     * @param zonaNombre       nombre de la zona
     * @param cantidadReservas cantidad de reservas en esa zona
     * @return {@link ResumenZonaResponse} con los datos del resumen
     */
    public ResumenZonaResponse toResumenZona(Long zonaId, String zonaNombre, Integer cantidadReservas) {
        return ResumenZonaResponse.builder()
                .zonaId(zonaId)
                .zonaNombre(zonaNombre)
                .cantidadReservas(cantidadReservas)
                .build();
    }
}
```

- [ ] **Step 4: Ejecutar tests para verificar que pasan**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=ReservaConsultaMapperTest -q
```
Resultado esperado: 2 tests PASS.

- [ ] **Step 5: Commit manual**

**Esperar a que el usuario haga commit manualmente de:**
- `ReservaConsultaMapper.java` (nuevo)
- `ReservaConsultaMapperTest.java` (nuevo)

Mensaje sugerido: `feat(reservas): implementar ReservaConsultaMapper con tests (TDD)`

---

## Task 4: Actualizar ReservaMapper para incluir clienteTelefono

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapper.java`

- [ ] **Step 1: Modificar método `toDetalleResponse` para incluir clienteTelefono**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapper.java
// En el método toDetalleResponse, añadir después de .notas(...):

.clienteTelefono(reserva.getCliente().getUsuario().getUsuarioTelefono())
```

El método completo quedará:

```java
return ReservaDetalleResponse.builder()
        .reservaId(reserva.getReservaId())
        .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
        .numeroPersonas(reserva.getReservaNumeroPersonas())
        .estado(reserva.getReservaEstado().name())
        .tipo(reserva.getReservaTipo().name())
        .zonaId(reserva.getZona() != null ? reserva.getZona().getZonaId() : null)
        .decoracionId(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionId() : null)
        .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
        .decoracionNombre(reserva.getDecoracion() != null
                ? reserva.getDecoracion().getDecoracionNombre() : null)
        .notas(reserva.getReservaNotas())
        .clienteTelefono(reserva.getCliente().getUsuario().getUsuarioTelefono())
        .preOrdenItems(preOrdenItems)
        .preOrdenTotal(preOrdenTotal)
        .abonos(abonosDto)
        .totalAbonado(totalAbonado)
        .modificable(esModificable(reserva))
        .build();
```

- [ ] **Step 2: Ejecutar tests existentes para verificar que no se rompen**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=ReservaMapperTest -q
```
Resultado esperado: Todos los tests pasan (el campo es opcional con `@JsonInclude(NON_NULL)`).

- [ ] **Step 3: Commit manual**

**Esperar a que el usuario haga commit manualmente de:**
- `ReservaMapper.java` (modificado)

Mensaje sugerido: `feat(reservas): añadir clienteTelefono a ReservaDetalleResponse en mapper`

---

## Task 5: ReservaConsultaService (TDD)

**Files:**
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/reservas/service/ReservaConsultaServiceTest.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaConsultaService.java`

- [ ] **Step 1: Escribir tests para ReservaConsultaService**

```java
// backend/src/test/java/co/edu/unicauca/backend/modules/reservas/service/ReservaConsultaServiceTest.java
package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.response.*;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaConsultaMapper;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Usuario;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link ReservaConsultaService}.
 */
@ExtendWith(MockitoExtension.class)
class ReservaConsultaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ComandaRepository comandaRepository;

    @Mock
    private AbonoRepository abonoRepository;

    @Mock
    private ReservaConsultaMapper consultaMapper;

    @Mock
    private ReservaMapper detalleMapper;

    @InjectMocks
    private ReservaConsultaService service;

    private Usuario usuario;
    private Cliente cliente;
    private Zona zona;
    private Decoracion decoracion;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("cliente@altoro.com")
                .usuarioTelefono("3001234567")
                .build();

        cliente = Cliente.builder()
                .usuarioId(1L)
                .usuario(usuario)
                .clienteNombre("Juan Pérez")
                .build();

        zona = Zona.builder()
                .zonaId(10L)
                .zonaNombre("Zona VIP")
                .build();

        decoracion = Decoracion.builder()
                .decoracionId(5L)
                .decoracionNombre("Romántica")
                .build();
    }

    @Test
    void listarReservasDelDia_sinParametros_retornaListadoDelDiaActual() {
        // Given
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.atTime(LocalTime.MAX);

        Reserva reserva1 = crearReserva(100L, zona, decoracion, 
                hoy.atTime(19, 0), EstadoReserva.CONFIRMADA);
        Reserva reserva2 = crearReserva(101L, zona, null, 
                hoy.atTime(20, 0), EstadoReserva.PENDIENTE);

        List<Reserva> reservas = Arrays.asList(reserva1, reserva2);

        when(reservaRepository.findReservasActivasDelDia(eq(inicio), eq(fin), anyList()))
                .thenReturn(reservas);

        ReservaConsultaResponse resp1 = ReservaConsultaResponse.builder()
                .reservaId(100L).zonaNombre("Zona VIP").build();
        ReservaConsultaResponse resp2 = ReservaConsultaResponse.builder()
                .reservaId(101L).zonaNombre("Zona VIP").build();

        when(consultaMapper.toConsultaResponse(reserva1)).thenReturn(resp1);
        when(consultaMapper.toConsultaResponse(reserva2)).thenReturn(resp2);
        when(consultaMapper.toResumenZona(10L, "Zona VIP", 2))
                .thenReturn(ResumenZonaResponse.builder()
                        .zonaId(10L).zonaNombre("Zona VIP").cantidadReservas(2).build());

        // When
        ListadoReservasResponse response = service.listarReservasDelDia(null, null);

        // Then
        assertThat(response.getReservas()).hasSize(2);
        assertThat(response.getResumenZonas()).hasSize(1);
        assertThat(response.getResumenZonas().get(0).getCantidadReservas()).isEqualTo(2);

        verify(reservaRepository).findReservasActivasDelDia(eq(inicio), eq(fin), anyList());
        verify(consultaMapper, times(2)).toConsultaResponse(any(Reserva.class));
        verify(consultaMapper).toResumenZona(10L, "Zona VIP", 2);
    }

    @Test
    void listarReservasDelDia_conFecha_retornaListadoDeLaFechaEspecificada() {
        // Given
        LocalDate fecha = LocalDate.of(2026, 5, 1);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        Reserva reserva = crearReserva(100L, zona, decoracion, 
                fecha.atTime(19, 30), EstadoReserva.CONFIRMADA);

        when(reservaRepository.findReservasActivasDelDia(eq(inicio), eq(fin), anyList()))
                .thenReturn(Arrays.asList(reserva));

        ReservaConsultaResponse resp = ReservaConsultaResponse.builder()
                .reservaId(100L).build();

        when(consultaMapper.toConsultaResponse(reserva)).thenReturn(resp);
        when(consultaMapper.toResumenZona(anyLong(), anyString(), anyInt()))
                .thenReturn(ResumenZonaResponse.builder().cantidadReservas(1).build());

        // When
        ListadoReservasResponse response = service.listarReservasDelDia(fecha, null);

        // Then
        assertThat(response.getReservas()).hasSize(1);
        assertThat(response.getResumenZonas()).hasSize(1);

        verify(reservaRepository).findReservasActivasDelDia(eq(inicio), eq(fin), anyList());
    }

    @Test
    void listarReservasDelDia_conIdentificador_retornaBusquedaPorId() {
        // Given
        Long reservaId = 123L;
        Reserva reserva = crearReserva(reservaId, zona, decoracion, 
                LocalDateTime.now(), EstadoReserva.CONFIRMADA);

        when(reservaRepository.findReservasActivasPorIdentificador(eq(reservaId), anyList()))
                .thenReturn(Arrays.asList(reserva));

        ReservaConsultaResponse resp = ReservaConsultaResponse.builder()
                .reservaId(reservaId).build();

        when(consultaMapper.toConsultaResponse(reserva)).thenReturn(resp);
        when(consultaMapper.toResumenZona(anyLong(), anyString(), anyInt()))
                .thenReturn(ResumenZonaResponse.builder().cantidadReservas(1).build());

        // When
        ListadoReservasResponse response = service.listarReservasDelDia(null, reservaId);

        // Then
        assertThat(response.getReservas()).hasSize(1);
        assertThat(response.getReservas().get(0).getReservaId()).isEqualTo(reservaId);

        verify(reservaRepository).findReservasActivasPorIdentificador(eq(reservaId), anyList());
        verify(reservaRepository, never()).findReservasActivasDelDia(any(), any(), anyList());
    }

    @Test
    void listarReservasDelDia_sinResultados_retornaListasVacias() {
        // Given
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.atTime(LocalTime.MAX);

        when(reservaRepository.findReservasActivasDelDia(eq(inicio), eq(fin), anyList()))
                .thenReturn(Arrays.asList());

        // When
        ListadoReservasResponse response = service.listarReservasDelDia(null, null);

        // Then
        assertThat(response.getReservas()).isEmpty();
        assertThat(response.getResumenZonas()).isEmpty();

        verify(reservaRepository).findReservasActivasDelDia(eq(inicio), eq(fin), anyList());
        verify(consultaMapper, never()).toConsultaResponse(any());
    }

    @Test
    void obtenerDetalleReserva_conReservaExistente_retornaDetalle() {
        // Given
        Long reservaId = 100L;
        Reserva reserva = crearReserva(reservaId, zona, decoracion, 
                LocalDateTime.now(), EstadoReserva.CONFIRMADA);

        Comanda comanda = new Comanda();
        List<Abono> abonos = Arrays.asList();

        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(comandaRepository.findByReserva_ReservaId(reservaId)).thenReturn(Optional.of(comanda));
        when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(reservaId)).thenReturn(abonos);

        ReservaDetalleResponse detalleResponse = ReservaDetalleResponse.builder()
                .reservaId(reservaId)
                .clienteNombre("Juan Pérez")
                .clienteTelefono("3001234567")
                .build();

        when(detalleMapper.toDetalleResponse(eq(reserva), anyList(), eq(abonos)))
                .thenReturn(detalleResponse);

        // When
        ReservaDetalleResponse response = service.obtenerDetalleReserva(reservaId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getReservaId()).isEqualTo(reservaId);
        assertThat(response.getClienteNombre()).isEqualTo("Juan Pérez");
        assertThat(response.getClienteTelefono()).isEqualTo("3001234567");

        verify(reservaRepository).findById(reservaId);
        verify(comandaRepository).findByReserva_ReservaId(reservaId);
        verify(abonoRepository).findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(reservaId);
        verify(detalleMapper).toDetalleResponse(eq(reserva), anyList(), eq(abonos));
    }

    @Test
    void obtenerDetalleReserva_reservaNoExiste_lanzaBusinessException() {
        // Given
        Long reservaId = 999L;
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.obtenerDetalleReserva(reservaId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reserva no encontrada");

        verify(reservaRepository).findById(reservaId);
        verify(comandaRepository, never()).findByReserva_ReservaId(anyLong());
        verify(abonoRepository, never()).findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong());
    }

    private Reserva crearReserva(Long id, Zona zona, Decoracion decoracion,
                                 LocalDateTime fechaHora, EstadoReserva estado) {
        return Reserva.builder()
                .reservaId(id)
                .cliente(cliente)
                .zona(zona)
                .decoracion(decoracion)
                .reservaFechaHoraLlegada(fechaHora)
                .reservaNumeroPersonas(4)
                .reservaEstado(estado)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();
    }
}
```

- [ ] **Step 2: Ejecutar tests para verificar que fallan**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=ReservaConsultaServiceTest -q
```
Resultado esperado: FAIL con "ReservaConsultaService cannot be resolved".

- [ ] **Step 3: Implementar ReservaConsultaService**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaConsultaService.java
package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.response.*;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaConsultaMapper;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de consulta de reservas para meseros.
 *
 * <p>Provee métodos para listar reservas activas del día (o de una fecha específica),
 * buscar por identificador, y obtener el detalle completo de una reserva.
 * Soporta agrupación por zona y resumen numérico.
 *
 * @see ReservaConsultaController
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservaConsultaService {

    private final ReservaRepository reservaRepository;
    private final ComandaRepository comandaRepository;
    private final AbonoRepository abonoRepository;
    private final ReservaConsultaMapper consultaMapper;
    private final ReservaMapper detalleMapper;

    /**
     * Lista las reservas activas (CONFIRMADA/PENDIENTE) del día o filtra por fecha/identificador.
     *
     * <p>Si {@code identificador} es no-null, ignora el parámetro {@code fecha} y busca
     * por ID. Si {@code fecha} es {@code null} y {@code identificador} también, usa el día actual.
     * Retorna el listado ordenado por hora de llegada y el resumen agrupado por zona.
     *
     * @param fecha fecha a consultar; {@code null} para usar el día actual
     * @param identificador ID de reserva a buscar; {@code null} para listar por fecha
     * @return {@link ListadoReservasResponse} con el listado y resúmenes por zona
     */
    public ListadoReservasResponse listarReservasDelDia(LocalDate fecha, Long identificador) {
        List<Reserva> reservas;

        // Filtrar por identificador tiene prioridad sobre fecha
        if (identificador != null) {
            reservas = reservaRepository.findReservasActivasPorIdentificador(
                    identificador,
                    Arrays.asList(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE));
        } else {
            // Si no se especifica fecha, usar el día actual
            LocalDate fechaConsulta = (fecha != null) ? fecha : LocalDate.now();
            LocalDateTime inicio = fechaConsulta.atStartOfDay();
            LocalDateTime fin = fechaConsulta.atTime(LocalTime.MAX);

            reservas = reservaRepository.findReservasActivasDelDia(
                    inicio,
                    fin,
                    Arrays.asList(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE));
        }

        // Convertir a DTOs
        List<ReservaConsultaResponse> reservasDtos = reservas.stream()
                .map(consultaMapper::toConsultaResponse)
                .collect(Collectors.toList());

        // Agrupar por zona y calcular resumen
        List<ResumenZonaResponse> resumenZonas = calcularResumenPorZona(reservas);

        return ListadoReservasResponse.builder()
                .reservas(reservasDtos)
                .resumenZonas(resumenZonas)
                .build();
    }

    /**
     * Obtiene el detalle completo de una reserva.
     *
     * @param reservaId identificador de la reserva
     * @return {@link ReservaDetalleResponse} con todos los campos del detalle
     * @throws BusinessException con HTTP 404 si la reserva no existe
     */
    public ReservaDetalleResponse obtenerDetalleReserva(Long reservaId) {
        // Buscar reserva
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Reserva no encontrada con ID: " + reservaId,
                        HttpStatus.NOT_FOUND));

        // Buscar comanda PRE_RESERVA asociada
        Comanda comanda = comandaRepository.findByReserva_ReservaId(reservaId)
                .orElse(null);

        // Obtener ítems de pre-orden
        List<ComandaItem> preOrden = (comanda != null && comanda.getComandaItems() != null) 
                ? comanda.getComandaItems() 
                : Arrays.asList();

        // Buscar abonos asociados
        List<Abono> abonos = abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(reservaId);

        // Reutilizar mapper
        return detalleMapper.toDetalleResponse(reserva, preOrden, abonos);
    }

    /**
     * Agrupa las reservas por zona y calcula la cantidad en cada una.
     *
     * @param reservas lista de reservas a agrupar
     * @return lista de {@link ResumenZonaResponse} con el conteo por zona
     */
    private List<ResumenZonaResponse> calcularResumenPorZona(List<Reserva> reservas) {
        // Agrupar por zona (null → "Sin asignar")
        Map<String, List<Reserva>> porZona = reservas.stream()
                .collect(Collectors.groupingBy(r -> {
                    if (r.getZona() == null) {
                        return "Sin asignar";
                    }
                    return r.getZona().getZonaNombre();
                }));

        // Convertir a DTOs de resumen
        return porZona.entrySet().stream()
                .map(entry -> {
                    String zonaNombre = entry.getKey();
                    List<Reserva> reservasZona = entry.getValue();
                    
                    // Obtener zonaId del primer elemento (si existe)
                    Long zonaId = reservasZona.stream()
                            .filter(r -> r.getZona() != null)
                            .findFirst()
                            .map(r -> r.getZona().getZonaId())
                            .orElse(null);

                    return consultaMapper.toResumenZona(zonaId, zonaNombre, reservasZona.size());
                })
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 4: Ejecutar tests para verificar que pasan**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=ReservaConsultaServiceTest -q
```
Resultado esperado: 6 tests PASS.

- [ ] **Step 5: Commit manual**

**Esperar a que el usuario haga commit manualmente de:**
- `ReservaConsultaService.java` (nuevo)
- `ReservaConsultaServiceTest.java` (nuevo)

Mensaje sugerido: `feat(reservas): implementar ReservaConsultaService con tests (TDD)`

---

## Task 6: ReservaConsultaController (TDD)

**Files:**
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/reservas/controller/ReservaConsultaControllerTest.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/controller/ReservaConsultaController.java`

- [ ] **Step 1: Escribir tests para ReservaConsultaController**

```java
// backend/src/test/java/co/edu/unicauca/backend/modules/reservas/controller/ReservaConsultaControllerTest.java
package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.dto.response.*;
import co.edu.unicauca.backend.modules.reservas.service.ReservaConsultaService;
import co.edu.unicauca.backend.shared.config.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitarios para {@link ReservaConsultaController}.
 */
@WebMvcTest(ReservaConsultaController.class)
@Import(PermissiveSecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-testing-purposes-only",
        "jwt.expiration=3600000",
        "jwt.refresh-expiration=86400000"
})
class ReservaConsultaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaConsultaService service;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "MESERO")
    void listarReservas_sinParametros_retorna200ConListadoDelDia() throws Exception {
        // Given
        ReservaConsultaResponse reserva1 = ReservaConsultaResponse.builder()
                .reservaId(100L)
                .clienteNombre("Juan Pérez")
                .zonaNombre("Zona VIP")
                .horaLlegada("19:00")
                .numeroPersonas(4)
                .estado("CONFIRMADA")
                .build();

        ResumenZonaResponse resumen = ResumenZonaResponse.builder()
                .zonaId(10L)
                .zonaNombre("Zona VIP")
                .cantidadReservas(1)
                .build();

        ListadoReservasResponse response = ListadoReservasResponse.builder()
                .reservas(Arrays.asList(reserva1))
                .resumenZonas(Arrays.asList(resumen))
                .build();

        when(service.listarReservasDelDia(isNull(), isNull())).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/reservas/mesero/consulta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservas[0].reservaId").value(100))
                .andExpect(jsonPath("$.data.reservas[0].clienteNombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.data.resumenZonas[0].cantidadReservas").value(1));

        verify(service).listarReservasDelDia(isNull(), isNull());
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void listarReservas_conFecha_retorna200ConListadoDeLaFecha() throws Exception {
        // Given
        LocalDate fecha = LocalDate.of(2026, 5, 1);

        ListadoReservasResponse response = ListadoReservasResponse.builder()
                .reservas(Arrays.asList())
                .resumenZonas(Arrays.asList())
                .build();

        when(service.listarReservasDelDia(eq(fecha), isNull())).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/reservas/mesero/consulta")
                        .param("fecha", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservas").isArray())
                .andExpect(jsonPath("$.data.resumenZonas").isArray());

        verify(service).listarReservasDelDia(eq(fecha), isNull());
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void listarReservas_conIdentificador_retorna200ConBusquedaPorId() throws Exception {
        // Given
        Long identificador = 123L;

        ReservaConsultaResponse reserva = ReservaConsultaResponse.builder()
                .reservaId(identificador)
                .clienteNombre("María López")
                .build();

        ListadoReservasResponse response = ListadoReservasResponse.builder()
                .reservas(Arrays.asList(reserva))
                .resumenZonas(Arrays.asList())
                .build();

        when(service.listarReservasDelDia(isNull(), eq(identificador))).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/reservas/mesero/consulta")
                        .param("identificador", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservas[0].reservaId").value(123));

        verify(service).listarReservasDelDia(isNull(), eq(identificador));
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void obtenerDetalle_conReservaExistente_retorna200ConDetalle() throws Exception {
        // Given
        Long reservaId = 100L;

        ReservaDetalleResponse detalle = ReservaDetalleResponse.builder()
                .reservaId(reservaId)
                .clienteNombre("Juan Pérez")
                .clienteTelefono("3001234567")
                .fechaHoraLlegada("2026-05-01T19:00:00")
                .numeroPersonas(4)
                .estado("CONFIRMADA")
                .build();

        when(service.obtenerDetalleReserva(reservaId)).thenReturn(detalle);

        // When/Then
        mockMvc.perform(get("/api/reservas/mesero/{reservaId}/detalle", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservaId").value(100))
                .andExpect(jsonPath("$.data.clienteNombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.data.clienteTelefono").value("3001234567"));

        verify(service).obtenerDetalleReserva(reservaId);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void listarReservas_conRolCliente_retorna403() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/reservas/mesero/consulta"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarReservas_sinAutenticacion_retorna401() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/reservas/mesero/consulta"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Configuración de seguridad permisiva para tests de controlador.
     * Permite @PreAuthorize sin configurar toda la cadena de filtros de seguridad.
     */
    @org.springframework.context.annotation.Configuration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class PermissiveSecurityConfig {
    }
}
```

- [ ] **Step 2: Ejecutar tests para verificar que fallan**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=ReservaConsultaControllerTest -q
```
Resultado esperado: FAIL con "ReservaConsultaController cannot be resolved".

- [ ] **Step 3: Implementar ReservaConsultaController**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/reservas/controller/ReservaConsultaController.java
package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.dto.response.ListadoReservasResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.service.ReservaConsultaService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controlador REST para la consulta de reservas por parte de meseros.
 *
 * <p>Expone los endpoints bajo {@code /api/reservas/mesero} y delega toda la lógica
 * de negocio en {@link ReservaConsultaService}.
 *
 * @see ReservaConsultaService
 */
@RestController
@RequestMapping("/api/reservas/mesero")
@RequiredArgsConstructor
@Tag(name = "Reservas - Mesero", description = "Consulta de reservas para meseros")
public class ReservaConsultaController {

    private final ReservaConsultaService service;

    /**
     * Lista las reservas activas (CONFIRMADA/PENDIENTE) del día o filtra por fecha/identificador.
     *
     * <p>Si no se especifica {@code fecha} ni {@code identificador}, retorna las reservas del día actual.
     * Si se especifica {@code identificador}, ignora el parámetro {@code fecha} y busca por ID.
     * El listado se ordena por hora de llegada ascendente y se agrupa por zona con resumen numérico.
     *
     * @param fecha fecha a consultar en formato {@code yyyy-MM-dd}; {@code null} para el día actual
     * @param identificador ID de reserva a buscar; {@code null} para listar por fecha
     * @return {@link ListadoReservasResponse} con el listado y resúmenes por zona
     */
    @GetMapping("/consulta")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Listar reservas activas del día o buscar por identificador")
    public ResponseEntity<ApiResponse<ListadoReservasResponse>> listarReservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long identificador) {

        ListadoReservasResponse response = service.listarReservasDelDia(fecha, identificador);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Obtiene el detalle completo de una reserva.
     *
     * @param reservaId identificador de la reserva
     * @return {@link ReservaDetalleResponse} con todos los campos del detalle
     */
    @GetMapping("/{reservaId}/detalle")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Obtener detalle completo de una reserva")
    public ResponseEntity<ApiResponse<ReservaDetalleResponse>> obtenerDetalle(
            @PathVariable Long reservaId) {

        ReservaDetalleResponse response = service.obtenerDetalleReserva(reservaId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
```

- [ ] **Step 4: Ejecutar tests para verificar que pasan**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -Dtest=ReservaConsultaControllerTest -q
```
Resultado esperado: 6 tests PASS.

- [ ] **Step 5: Commit manual**

**Esperar a que el usuario haga commit manualmente de:**
- `ReservaConsultaController.java` (nuevo)
- `ReservaConsultaControllerTest.java` (nuevo)

Mensaje sugerido: `feat(reservas): implementar ReservaConsultaController con tests (TDD)`

---

## Task 7: WebSocket para actualización en tiempo real (CA-09)

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/ReservaActualizadaWsMessage.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionWsPublisher.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaService.java`

- [ ] **Step 1: Crear ReservaActualizadaWsMessage**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/ReservaActualizadaWsMessage.java
package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import lombok.Builder;
import lombok.Getter;

/**
 * Payload WebSocket para notificar cambios en reservas activas.
 *
 * <p>Se publica en {@code /topic/reservas/cambios} cuando se crea o modifica
 * una reserva en estado CONFIRMADA o PENDIENTE, permitiendo que el frontend
 * de meseros actualice la lista automáticamente.
 */
@Getter
@Builder
public class ReservaActualizadaWsMessage {

    /** Identificador de la reserva que cambió. */
    private final Long reservaId;

    /** Tipo de cambio: {@code CREADA} o {@code MODIFICADA}. */
    private final String tipoEvento;

    /** Nombre del cliente de la reserva. */
    private final String clienteNombre;

    /** Hora de llegada en formato {@code HH:mm}. */
    private final String horaLlegada;

    /** Nombre de la zona; {@code null} si no fue asignada. */
    private final String zonaNombre;
}
```

- [ ] **Step 2: Añadir método en NotificacionWsPublisher**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionWsPublisher.java
// Añadir import al inicio:
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;

// Añadir al final de la clase, antes del cierre:

/**
 * Broadcast a todos los meseros conectados que hubo un cambio en las reservas activas.
 * Call site: ReservaService al crear o modificar una reserva CONFIRMADA/PENDIENTE.
 */
public void publicarReservaActualizada(ReservaActualizadaWsMessage mensaje) {
    messagingTemplate.convertAndSend("/topic/reservas/cambios", mensaje);
}
```

- [ ] **Step 3: Modificar ReservaService para publicar cambios**

```java
// backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaService.java
// Añadir imports al inicio:
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import java.time.format.DateTimeFormatter;

// Añadir campo en la clase (inyección de dependencia):
private final NotificacionWsPublisher wsPublisher;

// Al final del método crearReserva (antes del return), añadir:
// Publicar evento WS si la reserva está activa
if (reserva.getReservaEstado() == EstadoReserva.CONFIRMADA 
        || reserva.getReservaEstado() == EstadoReserva.PENDIENTE) {
    publicarCambioReserva(reserva, "CREADA");
}

// Al final del método modificarReserva (antes del return), añadir:
// Publicar evento WS si la reserva sigue activa
if (reservaActualizada.getReservaEstado() == EstadoReserva.CONFIRMADA 
        || reservaActualizada.getReservaEstado() == EstadoReserva.PENDIENTE) {
    publicarCambioReserva(reservaActualizada, "MODIFICADA");
}

// Añadir método privado al final de la clase:
/**
 * Publica un evento WebSocket de cambio en reserva activa.
 *
 * @param reserva     reserva que cambió
 * @param tipoEvento  tipo de evento ({@code CREADA} o {@code MODIFICADA})
 */
private void publicarCambioReserva(Reserva reserva, String tipoEvento) {
    String horaLlegada = reserva.getReservaFechaHoraLlegada()
            .format(DateTimeFormatter.ofPattern("HH:mm"));
    String zonaNombre = reserva.getZona() != null 
            ? reserva.getZona().getZonaNombre() 
            : null;

    ReservaActualizadaWsMessage mensaje = ReservaActualizadaWsMessage.builder()
            .reservaId(reserva.getReservaId())
            .tipoEvento(tipoEvento)
            .clienteNombre(reserva.getCliente().getClienteNombre())
            .horaLlegada(horaLlegada)
            .zonaNombre(zonaNombre)
            .build();

    wsPublisher.publicarReservaActualizada(mensaje);
}
```

- [ ] **Step 4: Compilar para verificar sin errores**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```
Resultado esperado: BUILD SUCCESS.

- [ ] **Step 5: Commit manual**

**Esperar a que el usuario haga commit manualmente de:**
- `ReservaActualizadaWsMessage.java` (nuevo)
- `NotificacionWsPublisher.java` (modificado)
- `ReservaService.java` (modificado)

Mensaje sugerido: `feat(reservas): añadir publicación WS de cambios en reservas activas (CA-09)`

---

## Task 8: Ejecutar suite completa de tests

**Files:**
- N/A

- [ ] **Step 1: Ejecutar todos los tests unitarios**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml test -q
```
Resultado esperado: Todos los tests pasan (incluyendo los nuevos de ReservaConsultaMapper, Service y Controller, más los existentes de ReservaMapper que ahora incluye clienteTelefono).

- [ ] **Step 2: Compilar proyecto completo**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean package -DskipTests -q
```
Resultado esperado: BUILD SUCCESS.

---

## Task 9: Actualizar backend/CLAUDE.md

**Files:**
- Modify: `backend/CLAUDE.md`

- [ ] **Step 1: Actualizar tabla de endpoints**

```markdown
// backend/CLAUDE.md
// En la sección "API Endpoints", bajo "### Reservas (`/api/reservas`)", añadir al final de la tabla:

| GET | `/mesero/consulta?fecha=&identificador=` | MESERO/ADMIN | Lista reservas activas del día (o fecha especificada); busca por ID si se proporciona identificador |
| GET | `/mesero/{reservaId}/detalle` | MESERO/ADMIN | Detalle completo de una reserva para meseros (incluye teléfono cliente y modificaciones de pre-orden) |
```

- [ ] **Step 2: Commit manual**

**Esperar a que el usuario haga commit manualmente de:**
- `backend/CLAUDE.md` (modificado)

Mensaje sugerido: `docs(backend): actualizar CLAUDE.md con endpoints de consulta de meseros`

---

## Task 10: Tests Postman

**Nota:** Los tests de Postman se crearán siguiendo el patrón existente. Cada test debe:
- Usar `beforeRequest` script con login autónomo
- Ser independiente (no depender de otros tests)
- Usar variables de entorno para datos dinámicos
- Incluir aserciones detalladas

Los archivos se listarán aquí pero se crearán manualmente o con un subagente dedicado.

**Colecciones a crear:**
1. `Al Toro – GET /api/reservas/mesero/consulta` (7 tests)
2. `Al Toro – GET /api/reservas/mesero/{reservaId}/detalle` (6 tests)

**Total: 13 tests Postman**

---

## Implementation Complete

**Summary:**
- ✅ Extendido `ReservaDetalleResponse` con `clienteTelefono` (retrocompatible)
- ✅ Reutilizado `PreOrdenItemResponse` (incluye modificaciones de menú especial)
- ✅ Creados 3 DTOs nuevos (ReservaConsultaResponse, ResumenZonaResponse, ListadoReservasResponse)
- ✅ Añadidas 2 queries optimizadas con JOIN FETCH en ReservaRepository
- ✅ Implementado ReservaConsultaMapper con 2 tests (TDD)
- ✅ Implementado ReservaConsultaService con 6 tests (TDD)
- ✅ Implementado ReservaConsultaController con 6 tests (TDD)
- ✅ Añadido WebSocket para actualización en tiempo real (CA-09)
- ✅ Actualizado backend/CLAUDE.md con nuevos endpoints
- ✅ Zona/decoración null → campo omitido (no "Por asignar")
- ✅ Commits manuales en cada task

**Endpoints creados:**
- `GET /api/reservas/mesero/consulta?fecha=&identificador=` — Lista reservas activas (CA-01, CA-02, CA-06, CA-07, CA-08)
- `GET /api/reservas/mesero/{reservaId}/detalle` — Detalle completo con modificaciones (CA-04, CA-05)

**WebSocket:**
- `/topic/reservas/cambios` — Broadcast de cambios en reservas activas (CA-09)

**Next steps:**
- Crear 13 tests Postman exhaustivos e independientes
- Insertar datos de prueba en DB si es necesario
- Ejecutar tests Postman para verificar integración completa

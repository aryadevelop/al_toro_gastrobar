# HE-02-HU-05 Cancelar Reserva — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exponer `PATCH /api/reservas/{reservaId}/cancelar` que permite a un CLIENTE cancelar su propia reserva activa, determinando si corresponde reembolso vía WhatsApp según tipo de reserva y momento de cancelación.

**Architecture:** Se agrega un único método `cancelarReserva()` en `ReservaService`, un nuevo método `validarElegibilidadCancelacion()` en `ReservaValidador` (stateless, sigue el mismo contrato que `validarElegibilidadModificacion`), una constante nueva en `MensajeWhatsAppBuilder`, un método `toCancelarResponse()` en `ReservaMapper` y un nuevo DTO de respuesta. El endpoint es `PATCH /{reservaId}/cancelar` accesible solo para `ROLE_CLIENTE`.

**Tech Stack:** Spring Boot 3.5, JPA/Hibernate, JUnit 5, Mockito, AssertJ, Postman YAML plugin format.

---

## Criterios de Aceptación cubiertos

| CA | Tipo reserva | Condición | Estado final | WhatsApp |
|----|-------------|-----------|--------------|----------|
| CA-01 | BASICA | Sin abono neto | CANCELADA | No |
| CA-02 | BASICA | Con abono neto > 0 | CANCELADA | Sí — reembolso |
| CA-03 | ESPECIAL | Antes 16:00 del día de la reserva | CANCELADA | Sí — reembolso |
| CA-04 | ESPECIAL | Después/igual 16:00 del día de la reserva | CANCELADA | No |
| CA-05 | Cualquier | Cliente no confirma (no llama el endpoint) | Sin cambios | N/A — puramente frontend |

---

## Mapa de archivos

### Crear
| Archivo | Responsabilidad |
|---------|----------------|
| `modules/reservas/dto/response/CancelarReservaResponse.java` | DTO inmutable de respuesta con `reservaId`, `estado`, `tipo`, `fechaHoraLlegada`, `numeroPersonas`, `requiereWhatsApp`, `mensajeWhatsApp` |
| `modules/reservas/service/ReservaServiceCancelarTest.java` | Pruebas unitarias de `cancelarReserva()` — 7 escenarios |
| `postman/collections/reservas/Al Toro – PATCH -api-reservas-{reservaId}-cancelar/.resources/definition.yaml` | Definición de colección Postman |
| `postman/.../CR-01 ... CR-12` | 12 casos de prueba Postman |

### Modificar
| Archivo | Cambio |
|---------|--------|
| `service/MensajeWhatsAppBuilder.java` | Nueva constante `MSG_WA_CANCELACION_REEMBOLSO` |
| `service/ReservaValidador.java` | Nuevo método `validarElegibilidadCancelacion()` |
| `mapper/ReservaMapper.java` | Nuevo método `toCancelarResponse()` |
| `service/ReservaService.java` | Nuevo método público `cancelarReserva()` |
| `controller/ReservaController.java` | Nuevo endpoint `@PatchMapping("/{reservaId}/cancelar")` |
| `service/ReservaValidadorTest.java` | Nested class `ValidarElegibilidadCancelacion` |
| `mapper/ReservaMapperTest.java` | Tests del nuevo método mapper |
| `controller/ReservaControllerTest.java` | Nested class `CancelarReserva` |

---

## Task 1: CancelarReservaResponse DTO

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/CancelarReservaResponse.java`

- [ ] **Step 1: Crear el DTO**

```java
package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Respuesta de la operación de cancelación de una reserva.
 *
 * <p>Cuando {@code requiereWhatsApp} es {@code true}, el campo {@code mensajeWhatsApp}
 * contiene el texto precompuesto que el frontend debe enviar al chat de la empresa
 * para gestionar el reembolso del abono.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelarReservaResponse {

    /** Identificador de la reserva cancelada. */
    private Long reservaId;

    /** Estado resultante; siempre {@code "CANCELADA"}. */
    private String estado;

    /** Tipo de la reserva cancelada: {@code "BASICA"} o {@code "ESPECIAL"}. */
    private String tipo;

    /** Fecha y hora de llegada formateada como {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private String fechaHoraLlegada;

    /** Número de personas de la reserva cancelada. */
    private Integer numeroPersonas;

    /**
     * {@code true} si el cliente debe ser redirigido al chat de WhatsApp
     * para gestionar el reembolso del abono (CA-02 y CA-03).
     */
    private boolean requiereWhatsApp;

    /**
     * Mensaje precompuesto para WhatsApp; {@code null} cuando {@code requiereWhatsApp}
     * es {@code false}.
     */
    private String mensajeWhatsApp;
}
```

- [ ] **Step 2: Compilar para verificar que no hay errores de sintaxis**

Run: `cd backend && ./mvnw clean compile -q`
Expected: BUILD SUCCESS (sin errores de compilación)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/CancelarReservaResponse.java
git commit -m "feat(reservas): agregar CancelarReservaResponse DTO para HU-05"
```

---

## Task 2: MensajeWhatsAppBuilder — constante de reembolso por cancelación

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/MensajeWhatsAppBuilder.java`

- [ ] **Step 1: Agregar la constante después de `MSG_WA_ABONO_AJUSTE`**

Añadir el siguiente bloque justo después de la declaración de `MSG_WA_ABONO_AJUSTE` (línea ~57):

```java
    /**
     * Nota para cuando una reserva es cancelada y el cliente tiene un abono pendiente
     * de reembolso que debe gestionar directamente con el restaurante.
     */
    public static final String MSG_WA_CANCELACION_REEMBOLSO =
            "Comunícate para gestionar el reembolso de tu abono.";
```

- [ ] **Step 2: Compilar para verificar**

Run: `cd backend && ./mvnw clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/MensajeWhatsAppBuilder.java
git commit -m "feat(reservas): agregar constante MSG_WA_CANCELACION_REEMBOLSO en MensajeWhatsAppBuilder"
```

---

## Task 3: TDD — ReservaValidador.validarElegibilidadCancelacion

**Files:**
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/reservas/service/ReservaValidadorTest.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaValidador.java`

- [ ] **Step 1: Escribir los tests que fallan — agregar después de la clase `ValidarElegibilidadModificacion` en `ReservaValidadorTest`**

```java
    // ── validarElegibilidadCancelacion ────────────────────────────────────────

    @Nested
    @DisplayName("validarElegibilidadCancelacion — ownership + estado")
    class ValidarElegibilidadCancelacion {

        @Test
        @DisplayName("Propietario con estado PENDIENTE → no lanza excepción")
        void propietarioPendiente_noLanza() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.PENDIENTE);

            assertThatCode(() -> validador.validarElegibilidadCancelacion(reserva, "cliente@altoro.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Propietario con estado CONFIRMADA → no lanza excepción")
        void propietarioConfirmada_noLanza() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);

            assertThatCode(() -> validador.validarElegibilidadCancelacion(reserva, "cliente@altoro.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Email no coincide con propietario → BusinessException 403 FORBIDDEN")
        void emailDistinto_lanzaForbidden() {
            Reserva reserva = reservaCon("otro@altoro.com", EstadoReserva.PENDIENTE);

            assertThatThrownBy(() -> validador.validarElegibilidadCancelacion(reserva, "cliente@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("Estado CANCELADA → BusinessException 422 UNPROCESSABLE_ENTITY")
        void estadoCancelada_lanzaUnprocessable() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CANCELADA);

            assertThatThrownBy(() -> validador.validarElegibilidadCancelacion(reserva, "cliente@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("Estado DEVUELTA → BusinessException 422 UNPROCESSABLE_ENTITY")
        void estadoDevuelta_lanzaUnprocessable() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.DEVUELTA);

            assertThatThrownBy(() -> validador.validarElegibilidadCancelacion(reserva, "cliente@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("Email coincide en mayúsculas → no lanza (comparación case-insensitive)")
        void emailMayusculas_noLanza() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);

            assertThatCode(() -> validador.validarElegibilidadCancelacion(reserva, "CLIENTE@ALTORO.COM"))
                    .doesNotThrowAnyException();
        }
    }
```

- [ ] **Step 2: Ejecutar los tests — deben fallar (método no existe)**

Run: `cd backend && ./mvnw test -pl . -Dtest=ReservaValidadorTest#ValidarElegibilidadCancelacion -q 2>&1 | tail -20`
Expected: FAIL — `cannot find symbol: method validarElegibilidadCancelacion`

- [ ] **Step 3: Implementar `validarElegibilidadCancelacion` en `ReservaValidador.java`**

Agregar el siguiente método después del bloque `validarElegibilidadModificacion` (después de la línea 74):

```java
    // -----------------------------------------------------------------------
    // Validación de elegibilidad de cancelación
    // -----------------------------------------------------------------------

    /**
     * Valida que el cliente sea el dueño de la reserva y que esta se encuentre en un estado
     * que permita cancelación (PENDIENTE o CONFIRMADA).
     *
     * <p>A diferencia de la modificación, la cancelación no tiene restricción de hora límite;
     * siempre es posible cancelar una reserva activa. La política de reembolso se determina
     * en {@link ReservaService#cancelarReserva} según tipo y momento de cancelación.</p>
     *
     * @param reserva       entidad de la reserva a cancelar
     * @param emailCliente  correo del cliente autenticado que intenta cancelar
     * @throws BusinessException si el cliente no es dueño (403) o el estado no es activo (422)
     */
    public void validarElegibilidadCancelacion(Reserva reserva, String emailCliente) {

        // Verificar que el email del cliente autenticado coincide con el dueño de la reserva
        if (!reserva.getCliente().getUsuario().getUsuarioEmail().equalsIgnoreCase(emailCliente)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes cancelar tus propias reservas.", HttpStatus.FORBIDDEN);
        }

        // Solo se pueden cancelar reservas activas (PENDIENTE o CONFIRMADA)
        List<EstadoReserva> estadosActivos = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);
        if (!estadosActivos.contains(reserva.getReservaEstado())) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "No es posible cancelar esta reserva.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
```

- [ ] **Step 4: Ejecutar los tests — deben pasar**

Run: `cd backend && ./mvnw test -pl . -Dtest=ReservaValidadorTest -q 2>&1 | tail -10`
Expected: BUILD SUCCESS — Tests run: N, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaValidador.java \
        backend/src/test/java/co/edu/unicauca/backend/modules/reservas/service/ReservaValidadorTest.java
git commit -m "feat(reservas): agregar validarElegibilidadCancelacion en ReservaValidador (TDD)"
```

---

## Task 4: TDD — ReservaMapper.toCancelarResponse

**Files:**
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapperTest.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapper.java`

- [ ] **Step 1: Escribir los tests que fallan — agregar al final de `ReservaMapperTest`**

```java
    // ── toCancelarResponse ────────────────────────────────────────────────────

    @Nested
    @DisplayName("toCancelarResponse — conversión a DTO de cancelación")
    class ToCancelarResponse {

        private Reserva reservaMock;

        @BeforeEach
        void setUp() {
            reservaMock = mock(Reserva.class);
            Cliente clienteMock = mock(Cliente.class);
            when(reservaMock.getReservaId()).thenReturn(1L);
            when(reservaMock.getReservaEstado()).thenReturn(EstadoReserva.CANCELADA);
            when(reservaMock.getReservaTipo()).thenReturn(TipoReserva.BASICA);
            when(reservaMock.getReservaFechaHoraLlegada())
                    .thenReturn(LocalDateTime.of(2027, 6, 15, 19, 0));
            when(reservaMock.getReservaNumeroPersonas()).thenReturn(3);
            when(reservaMock.getCliente()).thenReturn(clienteMock);
            when(reservaMock.getDecoracion()).thenReturn(null);
            when(reservaMock.getZona()).thenReturn(null);
        }

        @Test
        @DisplayName("Sin WhatsApp → mensajeWhatsApp es null y requiereWhatsApp=false")
        void sinWhatsApp_camposNulos() {
            CancelarReservaResponse response = mapper.toCancelarResponse(reservaMock, false, null);

            assertThat(response.getReservaId()).isEqualTo(1L);
            assertThat(response.getEstado()).isEqualTo("CANCELADA");
            assertThat(response.getTipo()).isEqualTo("BASICA");
            assertThat(response.getFechaHoraLlegada()).isEqualTo("2027-06-15T19:00:00");
            assertThat(response.getNumeroPersonas()).isEqualTo(3);
            assertThat(response.isRequiereWhatsApp()).isFalse();
            assertThat(response.getMensajeWhatsApp()).isNull();
        }

        @Test
        @DisplayName("Con WhatsApp → mensajeWhatsApp y requiereWhatsApp=true presentes")
        void conWhatsApp_camposPresentes() {
            String mensaje = "Comunícate para gestionar el reembolso de tu abono.";

            CancelarReservaResponse response = mapper.toCancelarResponse(reservaMock, true, mensaje);

            assertThat(response.isRequiereWhatsApp()).isTrue();
            assertThat(response.getMensajeWhatsApp()).isEqualTo(mensaje);
        }

        @Test
        @DisplayName("Estado siempre es 'CANCELADA' independientemente del enum")
        void estadoEsCancelada() {
            CancelarReservaResponse response = mapper.toCancelarResponse(reservaMock, false, null);

            assertThat(response.getEstado()).isEqualTo("CANCELADA");
        }

        @Test
        @DisplayName("Tipo ESPECIAL se mapea correctamente")
        void tipoEspecial_seMapea() {
            when(reservaMock.getReservaTipo()).thenReturn(TipoReserva.ESPECIAL);

            CancelarReservaResponse response = mapper.toCancelarResponse(reservaMock, true, "msg");

            assertThat(response.getTipo()).isEqualTo("ESPECIAL");
        }
    }
```

Los imports adicionales necesarios en `ReservaMapperTest`:
```java
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import java.time.LocalDateTime;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
```

- [ ] **Step 2: Ejecutar los tests — deben fallar (método no existe)**

Run: `cd backend && ./mvnw test -pl . -Dtest=ReservaMapperTest#ToCancelarResponse -q 2>&1 | tail -20`
Expected: FAIL — `cannot find symbol: method toCancelarResponse`

- [ ] **Step 3: Implementar `toCancelarResponse` en `ReservaMapper.java`**

Agregar el siguiente método al final de la clase `ReservaMapper`, antes del último `}`:

```java
    /**
     * Convierte una {@link Reserva} cancelada en su DTO de respuesta para la operación de
     * cancelación.
     *
     * @param reserva           entidad actualizada con estado {@code CANCELADA}
     * @param requiereWhatsApp  {@code true} si el cliente debe gestionar el reembolso por WhatsApp
     * @param mensajeWhatsApp   mensaje precompuesto para el chat; {@code null} si no aplica
     * @return {@link CancelarReservaResponse} con los datos de la reserva cancelada
     */
    public CancelarReservaResponse toCancelarResponse(Reserva reserva,
                                                       boolean requiereWhatsApp,
                                                       String mensajeWhatsApp) {
        return CancelarReservaResponse.builder()
                .reservaId(reserva.getReservaId())
                .estado(reserva.getReservaEstado().name())
                .tipo(reserva.getReservaTipo().name())
                .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
                .numeroPersonas(reserva.getReservaNumeroPersonas())
                .requiereWhatsApp(requiereWhatsApp)
                .mensajeWhatsApp(mensajeWhatsApp)
                .build();
    }
```

Agregar el import si no existe:
```java
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
```

- [ ] **Step 4: Ejecutar los tests — deben pasar**

Run: `cd backend && ./mvnw test -pl . -Dtest=ReservaMapperTest -q 2>&1 | tail -10`
Expected: BUILD SUCCESS — Tests run: N, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapper.java \
        backend/src/test/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapperTest.java
git commit -m "feat(reservas): agregar toCancelarResponse en ReservaMapper (TDD)"
```

---

## Task 5: TDD — ReservaService.cancelarReserva

**Files:**
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/reservas/service/ReservaServiceCancelarTest.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaService.java`

- [ ] **Step 1: Crear `ReservaServiceCancelarTest.java`**

```java
package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link ReservaService#cancelarReserva}.
 *
 * <p>Cubre los cuatro criterios de aceptación de la HU-05:
 * <ul>
 *   <li>CA-01: BASICA sin abono → no WhatsApp.</li>
 *   <li>CA-02: BASICA con abono → WhatsApp para reembolso.</li>
 *   <li>CA-03: ESPECIAL antes 16:00 del día de la reserva → WhatsApp para reembolso.</li>
 *   <li>CA-04: ESPECIAL después 16:00 del día de la reserva → no WhatsApp.</li>
 * </ul>
 * Además cubre los errores esperados: 404, 403 (ownership) y 422 (estado no cancelable).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReservaService — cancelarReserva")
class ReservaServiceCancelarTest {

    @Mock ReservaRepository reservaRepository;
    @Mock DecoracionRepository decoracionRepository;
    @Mock ZonaRepository zonaRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock ReservaMapper reservaMapper;
    @Mock AbonoRepository abonoRepository;
    @Mock ReservaValidador reservaValidador;
    @Mock DisponibilidadConsultador disponibilidadConsultador;
    @Mock PreOrdenGestor preOrdenGestor;
    @Mock MensajeWhatsAppBuilder mensajeWhatsAppBuilder;

    @InjectMocks
    ReservaService service;

    private static final String EMAIL = "cliente@test.com";
    private static final Long RESERVA_ID = 1L;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Crea una Reserva usando reflection para establecer campos privados. */
    private Reserva buildReserva(TipoReserva tipo, EstadoReserva estado, LocalDateTime fechaHoraLlegada) {
        Cliente cliente = buildCliente(EMAIL);
        Reserva r = new Reserva();
        setField(r, Reserva.class, "reservaId", RESERVA_ID);
        setField(r, Reserva.class, "cliente", cliente);
        setField(r, Reserva.class, "reservaTipo", tipo);
        setField(r, Reserva.class, "reservaEstado", estado);
        setField(r, Reserva.class, "reservaFechaHoraLlegada", fechaHoraLlegada);
        setField(r, Reserva.class, "reservaNumeroPersonas", 2);
        return r;
    }

    private Cliente buildCliente(String email) {
        Usuario usuario = new Usuario();
        setField(usuario, Usuario.class, "usuarioEmail", email);
        Cliente cliente = new Cliente();
        try {
            java.lang.reflect.Field f = Cliente.class.getSuperclass().getDeclaredField("usuario");
            f.setAccessible(true);
            f.set(cliente, usuario);
        } catch (Exception e1) {
            try {
                java.lang.reflect.Field f = Cliente.class.getDeclaredField("usuario");
                f.setAccessible(true);
                f.set(cliente, usuario);
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
        }
        return cliente;
    }

    private void setField(Object target, Class<?> clazz, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException e) {
            Class<?> parent = clazz.getSuperclass();
            if (parent != null && parent != Object.class) {
                setField(target, parent, fieldName, value);
            } else {
                throw new RuntimeException("Field not found: " + fieldName, e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Crea un Abono de tipo ANTICIPO con el monto dado. */
    private Abono buildAbono(BigDecimal monto) {
        Abono a = new Abono();
        setField(a, Abono.class, "abonoMonto", monto);
        setField(a, Abono.class, "abonoTipo", TipoAbono.ANTICIPO);
        return a;
    }

    private CancelarReservaResponse stubResponse(boolean requiereWa) {
        return CancelarReservaResponse.builder()
                .reservaId(RESERVA_ID).estado("CANCELADA").tipo("BASICA")
                .requiereWhatsApp(requiereWa).build();
    }

    // ── Pruebas de error ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Errores previos a la cancelación")
    class Errores {

        @Test
        @DisplayName("Reserva inexistente → ResourceNotFoundException")
        void reservaNoExiste_lanzaNotFound() {
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelarReserva(RESERVA_ID, EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Validador lanza ACCESS_DENIED → BusinessException se propaga")
        void ownershipFalla_propagaBusinessException() {
            // La reserva existe pero el validador lanza excepción de ownership
            Reserva reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.PENDIENTE,
                    LocalDateTime.now().plusDays(2));
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doThrow(new BusinessException(
                    co.edu.unicauca.backend.shared.exception.ErrorCode.ACCESS_DENIED,
                    "Solo puedes cancelar tus propias reservas.",
                    org.springframework.http.HttpStatus.FORBIDDEN))
                    .when(reservaValidador).validarElegibilidadCancelacion(reserva, EMAIL);

            assertThatThrownBy(() -> service.cancelarReserva(RESERVA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Validador lanza INVALID_STATE → BusinessException se propaga")
        void estadoNoActivo_propagaBusinessException() {
            Reserva reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.CANCELADA,
                    LocalDateTime.now().plusDays(2));
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doThrow(new BusinessException(
                    co.edu.unicauca.backend.shared.exception.ErrorCode.INVALID_STATE,
                    "No es posible cancelar esta reserva.",
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY))
                    .when(reservaValidador).validarElegibilidadCancelacion(reserva, EMAIL);

            assertThatThrownBy(() -> service.cancelarReserva(RESERVA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── CA-01: BASICA sin abono ───────────────────────────────────────────────

    @Nested
    @DisplayName("CA-01: BASICA sin abono → CANCELADA sin WhatsApp")
    class BasicaSinAbono {

        private Reserva reserva;

        @BeforeEach
        void setUp() {
            reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.CONFIRMADA,
                    LocalDateTime.now().plusDays(2));
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
            // Sin abonos
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of());
            when(reservaMapper.toCancelarResponse(any(), eq(false), eq(null)))
                    .thenReturn(stubResponse(false));
        }

        @Test
        @DisplayName("El estado de la reserva guardada es CANCELADA")
        void estadoEsCancelada() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CANCELADA);
        }

        @Test
        @DisplayName("No se construye mensaje de WhatsApp")
        void noSeConstruyeMensajeWhatsApp() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(mensajeWhatsAppBuilder, never()).construirMensaje(any(), any());
        }

        @Test
        @DisplayName("El mapper es llamado con requiereWhatsApp=false y mensajeWhatsApp=null")
        void mapperLlamadoConFalseNull() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(reservaMapper).toCancelarResponse(any(Reserva.class), eq(false), eq(null));
        }
    }

    // ── CA-02: BASICA con abono ───────────────────────────────────────────────

    @Nested
    @DisplayName("CA-02: BASICA con abono → CANCELADA con WhatsApp de reembolso")
    class BasicaConAbono {

        private static final String MSG = "Comunícate para gestionar el reembolso de tu abono.";
        private Reserva reserva;

        @BeforeEach
        void setUp() {
            reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.CONFIRMADA,
                    LocalDateTime.now().plusDays(2));
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
            // Con abono de $50
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of(buildAbono(new BigDecimal("50.00"))));
            when(mensajeWhatsAppBuilder.construirMensaje(any(), eq(MensajeWhatsAppBuilder.MSG_WA_CANCELACION_REEMBOLSO)))
                    .thenReturn(MSG);
            when(reservaMapper.toCancelarResponse(any(), eq(true), eq(MSG)))
                    .thenReturn(stubResponse(true));
        }

        @Test
        @DisplayName("El estado de la reserva guardada es CANCELADA")
        void estadoEsCancelada() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CANCELADA);
        }

        @Test
        @DisplayName("Se construye mensaje de WhatsApp con constante de reembolso")
        void seConstruyeMensajeWhatsApp() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(mensajeWhatsAppBuilder).construirMensaje(any(Reserva.class),
                    eq(MensajeWhatsAppBuilder.MSG_WA_CANCELACION_REEMBOLSO));
        }

        @Test
        @DisplayName("El mapper es llamado con requiereWhatsApp=true y el mensaje")
        void mapperLlamadoConTrueYMensaje() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(reservaMapper).toCancelarResponse(any(Reserva.class), eq(true), eq(MSG));
        }
    }

    // ── CA-03: ESPECIAL antes 16:00 ──────────────────────────────────────────

    @Nested
    @DisplayName("CA-03: ESPECIAL antes 16:00 del día de la reserva → CANCELADA con WhatsApp")
    class EspecialAntes16h {

        private static final String MSG = "Comunícate para gestionar el reembolso de tu abono.";
        private Reserva reserva;

        @BeforeEach
        void setUp() {
            // Fecha 2 años en el futuro: siempre antes de las 16:00 de ese día futuro
            LocalDateTime fechaFutura = LocalDateTime.now().plusDays(730).withHour(19).withMinute(0);
            reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE, fechaFutura);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mensajeWhatsAppBuilder.construirMensaje(any(), eq(MensajeWhatsAppBuilder.MSG_WA_CANCELACION_REEMBOLSO)))
                    .thenReturn(MSG);
            when(reservaMapper.toCancelarResponse(any(), eq(true), eq(MSG)))
                    .thenReturn(stubResponse(true));
        }

        @Test
        @DisplayName("El estado de la reserva guardada es CANCELADA")
        void estadoEsCancelada() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CANCELADA);
        }

        @Test
        @DisplayName("Se construye mensaje de WhatsApp con constante de reembolso")
        void seConstruyeMensajeWhatsApp() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(mensajeWhatsAppBuilder).construirMensaje(any(Reserva.class),
                    eq(MensajeWhatsAppBuilder.MSG_WA_CANCELACION_REEMBOLSO));
        }

        @Test
        @DisplayName("No se consultan abonos (ESPECIAL no depende del abono para la política)")
        void noSEConsultanAbonos() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(abonoRepository, never())
                    .findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID);
        }
    }

    // ── CA-04: ESPECIAL después/igual 16:00 ──────────────────────────────────

    @Nested
    @DisplayName("CA-04: ESPECIAL después 16:00 del día de la reserva → CANCELADA sin WhatsApp")
    class EspecialDespues16h {

        private Reserva reserva;

        @BeforeEach
        void setUp() {
            // Fecha en el pasado: la hora 16:00 de ese día ya pasó (ahora > 16:00 de ayer)
            LocalDateTime fechaPasada = LocalDateTime.now().minusDays(1).withHour(19).withMinute(0);
            reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE, fechaPasada);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
            when(reservaMapper.toCancelarResponse(any(), eq(false), eq(null)))
                    .thenReturn(stubResponse(false));
        }

        @Test
        @DisplayName("El estado de la reserva guardada es CANCELADA")
        void estadoEsCancelada() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CANCELADA);
        }

        @Test
        @DisplayName("No se construye mensaje de WhatsApp (sin reembolso)")
        void noSeConstruyeMensajeWhatsApp() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(mensajeWhatsAppBuilder, never()).construirMensaje(any(), any());
        }

        @Test
        @DisplayName("El mapper es llamado con requiereWhatsApp=false y mensajeWhatsApp=null")
        void mapperLlamadoConFalseNull() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(reservaMapper).toCancelarResponse(any(Reserva.class), eq(false), eq(null));
        }
    }
}
```

- [ ] **Step 2: Ejecutar los tests — deben fallar (método no existe)**

Run: `cd backend && ./mvnw test -pl . -Dtest=ReservaServiceCancelarTest -q 2>&1 | tail -20`
Expected: FAIL — `cannot find symbol: method cancelarReserva`

- [ ] **Step 3: Implementar `cancelarReserva` en `ReservaService.java`**

Agregar los siguientes imports si no existen:
```java
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
```

Agregar el método público `cancelarReserva` después del bloque `// CRUD - UPDATE` (por ejemplo, después de `modificarReserva`), antes del separador `// Lógica interna`:

```java
    // -----------------------------------------------------------------------
    // CRUD - DELETE (cancelación)
    // -----------------------------------------------------------------------

    /**
     * Cancela una reserva activa del cliente, actualizando su estado a
     * {@link EstadoReserva#CANCELADA}.
     *
     * <p>Reglas de redirección a WhatsApp según tipo y momento de cancelación:
     * <ul>
     *   <li><b>BÁSICA sin abono neto</b>: no requiere WhatsApp (CA-01).</li>
     *   <li><b>BÁSICA con abono neto &gt; 0</b>: requiere WhatsApp para gestionar el
     *       reembolso (CA-02).</li>
     *   <li><b>ESPECIAL antes de las 16:00 del día de la reserva</b>: requiere
     *       WhatsApp para gestionar el reembolso (CA-03).</li>
     *   <li><b>ESPECIAL después/igual a las 16:00 del día de la reserva</b>: sin
     *       reembolso, no requiere WhatsApp (CA-04).</li>
     * </ul>
     *
     * <p>A diferencia de la modificación, no hay hora límite para cancelar: se puede
     * cancelar en cualquier momento mientras el estado sea {@code PENDIENTE} o
     * {@code CONFIRMADA}.
     *
     * @param reservaId    identificador de la reserva a cancelar
     * @param emailCliente email del cliente autenticado (tomado del token)
     * @return {@link CancelarReservaResponse} con el estado final y el indicador de WhatsApp
     * @throws ResourceNotFoundException si la reserva no existe
     * @throws BusinessException         si el cliente no es el propietario (403)
     *                                   o el estado no es cancelable (422)
     */
    @Transactional
    public CancelarReservaResponse cancelarReserva(Long reservaId, String emailCliente) {

        // 1. Verificar existencia de la reserva
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        // 2. Validar ownership y estado activo (PENDIENTE o CONFIRMADA)
        reservaValidador.validarElegibilidadCancelacion(reserva, emailCliente);

        // 3. Cambiar estado a CANCELADA y persistir
        reserva.setReservaEstado(EstadoReserva.CANCELADA);
        Reserva guardada = reservaRepository.save(reserva);

        // 4. Determinar política de reembolso según tipo y momento de cancelación
        boolean esEspecial = reserva.getReservaTipo() == TipoReserva.ESPECIAL;
        boolean requiereWhatsApp;

        if (esEspecial) {
            // ESPECIAL: el límite de 16:00 del día de la reserva define si hay reembolso (CA-03/CA-04)
            LocalDateTime limite16h = reserva.getReservaFechaHoraLlegada()
                    .toLocalDate().atTime(HORA_LIMITE_ESTANDAR);
            requiereWhatsApp = LocalDateTime.now().isBefore(limite16h);
        } else {
            // BÁSICA: depende de si hay abono neto pendiente de reembolso (CA-01/CA-02)
            requiereWhatsApp = calcularAbonoNeto(reservaId).compareTo(BigDecimal.ZERO) > 0;
        }

        String mensajeWhatsApp = requiereWhatsApp
                ? mensajeWhatsAppBuilder.construirMensaje(guardada,
                        MensajeWhatsAppBuilder.MSG_WA_CANCELACION_REEMBOLSO)
                : null;

        return reservaMapper.toCancelarResponse(guardada, requiereWhatsApp, mensajeWhatsApp);
    }
```

- [ ] **Step 4: Ejecutar los tests — deben pasar**

Run: `cd backend && ./mvnw test -pl . -Dtest=ReservaServiceCancelarTest -q 2>&1 | tail -10`
Expected: BUILD SUCCESS — Tests run: N, Failures: 0, Errors: 0

- [ ] **Step 5: Ejecutar todos los tests del módulo para detectar regresiones**

Run: `cd backend && ./mvnw test -pl . -Dtest="co.edu.unicauca.backend.modules.reservas.**" -q 2>&1 | tail -15`
Expected: BUILD SUCCESS — todos los tests anteriores siguen pasando

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaService.java \
        backend/src/test/java/co/edu/unicauca/backend/modules/reservas/service/ReservaServiceCancelarTest.java
git commit -m "feat(reservas): implementar cancelarReserva en ReservaService (TDD)"
```

---

## Task 6: TDD — ReservaController — endpoint PATCH /{reservaId}/cancelar

**Files:**
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/reservas/controller/ReservaControllerTest.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/controller/ReservaController.java`

- [ ] **Step 1: Agregar imports necesarios en `ReservaControllerTest.java`**

Agregar al bloque de imports del archivo si no están presentes:
```java
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
```

- [ ] **Step 2: Agregar la clase `@Nested CancelarReserva` al final de `ReservaControllerTest`**

```java
    @Nested
    @DisplayName("PATCH /{reservaId}/cancelar — cancelarReserva")
    class CancelarReserva {

        private final Long RESERVA_ID = 5L;

        private CancelarReservaResponse responseBasicaSinWa() {
            return CancelarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CANCELADA").tipo("BASICA")
                    .requiereWhatsApp(false).mensajeWhatsApp(null).build();
        }

        private CancelarReservaResponse responseBasicaConWa() {
            return CancelarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CANCELADA").tipo("BASICA")
                    .requiereWhatsApp(true)
                    .mensajeWhatsApp("Comunícate para gestionar el reembolso de tu abono.")
                    .build();
        }

        private CancelarReservaResponse responseEspecialSinWa() {
            return CancelarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CANCELADA").tipo("ESPECIAL")
                    .requiereWhatsApp(false).mensajeWhatsApp(null).build();
        }

        // ── Autorización ──────────────────────────────────────────────────────

        @Test
        @DisplayName("Sin token → 401 Unauthorized")
        void sinToken_401() throws Exception {
            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("MESERO sin permiso → 403 Forbidden")
        @WithMockUser(username = "mesero@test.com", roles = "MESERO")
        void meseroSinPermiso_403() throws Exception {
            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CAJERO sin permiso → 403 Forbidden")
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        void cajeroSinPermiso_403() throws Exception {
            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN sin permiso → 403 Forbidden")
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        void adminSinPermiso_403() throws Exception {
            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isForbidden());
        }

        // ── Errores de negocio ────────────────────────────────────────────────

        @Test
        @DisplayName("Reserva no encontrada → 404")
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        void reservaNoEncontrada_404() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenThrow(new co.edu.unicauca.backend.shared.exception.ResourceNotFoundException(
                            "Reserva", RESERVA_ID));

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Reserva de otro cliente → 403 Forbidden")
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        void reservaOtroCliente_403() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenThrow(new co.edu.unicauca.backend.shared.exception.BusinessException(
                            co.edu.unicauca.backend.shared.exception.ErrorCode.ACCESS_DENIED,
                            "Solo puedes cancelar tus propias reservas.",
                            org.springframework.http.HttpStatus.FORBIDDEN));

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Reserva ya cancelada → 422 Unprocessable Entity")
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        void reservaYaCancelada_422() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenThrow(new co.edu.unicauca.backend.shared.exception.BusinessException(
                            co.edu.unicauca.backend.shared.exception.ErrorCode.INVALID_STATE,
                            "No es posible cancelar esta reserva.",
                            org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY));

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isUnprocessableEntity());
        }

        // ── Caminos felices ───────────────────────────────────────────────────

        @Test
        @DisplayName("CA-01: BASICA sin abono → 200 OK, requiereWhatsApp=false")
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        void basicaSinAbono_200SinWhatsApp() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenReturn(responseBasicaSinWa());

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reservaId").value(RESERVA_ID))
                    .andExpect(jsonPath("$.data.estado").value("CANCELADA"))
                    .andExpect(jsonPath("$.data.tipo").value("BASICA"))
                    .andExpect(jsonPath("$.data.requiereWhatsApp").value(false))
                    .andExpect(jsonPath("$.data.mensajeWhatsApp").doesNotExist());
        }

        @Test
        @DisplayName("CA-02: BASICA con abono → 200 OK, requiereWhatsApp=true con mensaje")
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        void basicaConAbono_200ConWhatsApp() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenReturn(responseBasicaConWa());

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.requiereWhatsApp").value(true))
                    .andExpect(jsonPath("$.data.mensajeWhatsApp").isString());
        }

        @Test
        @DisplayName("CA-04: ESPECIAL después 16h → 200 OK, requiereWhatsApp=false")
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        void especialDespues16h_200SinWhatsApp() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenReturn(responseEspecialSinWa());

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tipo").value("ESPECIAL"))
                    .andExpect(jsonPath("$.data.requiereWhatsApp").value(false));
        }

        @Test
        @DisplayName("El email se toma del token, no del cuerpo de la petición")
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        void emailDelToken_noDelBody() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenReturn(responseBasicaSinWa());

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isOk());

            // Verifica que el servicio fue llamado con el email del token
            verify(reservaService).cancelarReserva(eq(RESERVA_ID), eq("cliente@test.com"));
        }
    }
```

Notas sobre imports adicionales en `ReservaControllerTest`:
```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
```

- [ ] **Step 3: Ejecutar los tests — deben fallar (endpoint no existe)**

Run: `cd backend && ./mvnw test -pl . -Dtest=ReservaControllerTest#CancelarReserva -q 2>&1 | tail -20`
Expected: FAIL — 404 o `cannot find symbol`

- [ ] **Step 4: Implementar el endpoint en `ReservaController.java`**

Agregar el import si no existe:
```java
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
```

Agregar el siguiente método después del TODO de cancelar reservas (reemplazando el TODO comment, línea ~135):

```java
    /**
     * Cancela una reserva futura del cliente autenticado.
     *
     * <p>Solo el cliente propietario puede cancelar su reserva ({@code ROLE_CLIENTE}).
     * El email del cliente se toma del token de autenticación, nunca del body ni de
     * query params.
     *
     * <p>Cuando {@code requiereWhatsApp} es {@code true} en la respuesta, el frontend
     * debe redirigir al cliente al chat de WhatsApp de la empresa con el mensaje
     * precompuesto para gestionar el reembolso del abono.
     *
     * <p>A diferencia de la modificación, no existe hora límite para cancelar: se puede
     * cancelar en cualquier momento mientras el estado sea {@code PENDIENTE} o
     * {@code CONFIRMADA}.
     *
     * @param reservaId      identificador de la reserva a cancelar
     * @param authentication contexto de seguridad del request
     * @return {@code 200 OK} con el estado final de la reserva cancelada y el indicador
     *         de redirección a WhatsApp
     */
    @PatchMapping("/{reservaId}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Cancelar una reserva futura del cliente")
    public ResponseEntity<ApiResponse<CancelarReservaResponse>> cancelarReserva(
            @PathVariable Long reservaId,
            Authentication authentication) {

        String emailCliente = authentication.getName();
        CancelarReservaResponse response = reservaService.cancelarReserva(reservaId, emailCliente);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
```

- [ ] **Step 5: Ejecutar los tests del controlador — deben pasar**

Run: `cd backend && ./mvnw test -pl . -Dtest=ReservaControllerTest -q 2>&1 | tail -10`
Expected: BUILD SUCCESS — Tests run: N, Failures: 0, Errors: 0

- [ ] **Step 6: Ejecutar todos los tests del módulo para verificar que no hay regresiones**

Run: `cd backend && ./mvnw test -q 2>&1 | tail -15`
Expected: BUILD SUCCESS — Tests run: N, Failures: 0, Errors: 0

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/controller/ReservaController.java \
        backend/src/test/java/co/edu/unicauca/backend/modules/reservas/controller/ReservaControllerTest.java
git commit -m "feat(reservas): agregar endpoint PATCH /{reservaId}/cancelar en ReservaController (TDD)"
```

---

## Task 7: Postman — Colección PATCH /api/reservas/{reservaId}/cancelar

**Files (crear):**
- `backend/postman/postman/collections/reservas/Al Toro – PATCH -api-reservas-{reservaId}-cancelar/.resources/definition.yaml`
- `CR-01` a `CR-12` — 12 archivos `.request.yaml`

### Estructura de carpeta

```
reservas/
└── Al Toro – PATCH -api-reservas-{reservaId}-cancelar/
    ├── .resources/
    │   └── definition.yaml
    ├── CR-01 Sin token – 401 Unauthorized.request.yaml
    ├── CR-02 MESERO sin permiso – 403 Forbidden.request.yaml
    ├── CR-03 CAJERO sin permiso – 403 Forbidden.request.yaml
    ├── CR-04 COCINERO sin permiso – 403 Forbidden.request.yaml
    ├── CR-05 ADMIN sin permiso – 403 Forbidden.request.yaml
    ├── CR-06 Reserva no encontrada – 404 Not Found.request.yaml
    ├── CR-07 Reserva de otro cliente – 403 Forbidden.request.yaml
    ├── CR-08 Reserva ya cancelada – 422 Unprocessable Entity.request.yaml
    ├── CR-09 CA-01 BASICA sin abono – 200 OK sin WhatsApp.request.yaml
    ├── CR-10 CA-02 BASICA con abono – 200 OK requiereWhatsApp.request.yaml
    ├── CR-11 CA-03 ESPECIAL antes 16h – 200 OK requiereWhatsApp.request.yaml
    └── CR-12 CA-04 ESPECIAL despues 16h – 200 OK sin WhatsApp.request.yaml
```

---

- [ ] **Step 1: Crear `definition.yaml`**

```yaml
$kind: collection
name: Al Toro – PATCH /api/reservas/{reservaId}/cancelar
```

- [ ] **Step 2: Crear CR-01 — Sin token**

```yaml
$kind: http-request
name: CR-01 Sin token JWT – 401 Unauthorized
description: |
  Verifica que el endpoint requiere autenticación.
  Sin Bearer token → 401.
url: "{{baseUrl}}/api/reservas/1/cancelar"
method: PATCH
scripts:
  - type: afterResponse
    code: |-
      pm.test('Sin token → 401', function () {
        pm.response.to.have.status(401);
      });
    language: text/javascript
order: 100
```

- [ ] **Step 3: Crear CR-02 a CR-05 — Roles sin permiso (MESERO, CAJERO, COCINERO, ADMIN)**

**CR-02 MESERO:**
```yaml
$kind: http-request
name: CR-02 MESERO sin permiso – 403 Forbidden
description: |
  Solo CLIENTE puede cancelar reservas.
  Token de MESERO → 403.
url: "{{baseUrl}}/api/reservas/1/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{meseroToken}}
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailMesero'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('meseroToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('MESERO → 403', function () {
        pm.response.to.have.status(403);
      });
    language: text/javascript
order: 200
```

**CR-03 CAJERO** (mismo patrón, reemplazar `mesero` → `cajero` y `Mesero` → `Cajero`):
```yaml
$kind: http-request
name: CR-03 CAJERO sin permiso – 403 Forbidden
description: |
  Solo CLIENTE puede cancelar reservas.
  Token de CAJERO → 403.
url: "{{baseUrl}}/api/reservas/1/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{cajeroToken}}
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailCajero'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('cajeroToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('CAJERO → 403', function () {
        pm.response.to.have.status(403);
      });
    language: text/javascript
order: 300
```

**CR-04 COCINERO** (mismo patrón con `emailCocinero` / `cocineroToken`):
```yaml
$kind: http-request
name: CR-04 COCINERO sin permiso – 403 Forbidden
description: |
  Solo CLIENTE puede cancelar reservas.
  Token de COCINERO → 403.
url: "{{baseUrl}}/api/reservas/1/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{cocineroToken}}
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailCocinero'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('cocineroToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('COCINERO → 403', function () {
        pm.response.to.have.status(403);
      });
    language: text/javascript
order: 400
```

**CR-05 ADMIN** (mismo patrón con `emailAdmin` / `adminToken`):
```yaml
$kind: http-request
name: CR-05 ADMIN sin permiso – 403 Forbidden
description: |
  Solo CLIENTE puede cancelar reservas.
  Token de ADMIN → 403.
url: "{{baseUrl}}/api/reservas/1/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{adminToken}}
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailAdmin'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('adminToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('ADMIN → 403', function () {
        pm.response.to.have.status(403);
      });
    language: text/javascript
order: 500
```

- [ ] **Step 4: Crear CR-06 — Reserva no encontrada**

```yaml
$kind: http-request
name: CR-06 Reserva no encontrada – 404 Not Found
description: |
  ID de reserva que no existe en base de datos → 404.
url: "{{baseUrl}}/api/reservas/999999/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{clienteToken}}
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailCliente'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('clienteToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('ID inexistente → 404', function () {
        pm.response.to.have.status(404);
      });
    language: text/javascript
order: 600
```

- [ ] **Step 5: Crear CR-07 — Reserva de otro cliente**

```yaml
$kind: http-request
name: CR-07 Reserva de otro cliente – 403 Forbidden
description: |
  CLIENTE intenta cancelar una reserva que pertenece a otro usuario → 403.
  Pre-condición: reservaIdOtroCliente apunta a una reserva activa de otro cliente.
url: "{{baseUrl}}/api/reservas/{{reservaIdOtroCliente}}/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{clienteToken}}
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailCliente'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('clienteToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('Reserva ajena → 403', function () {
        pm.response.to.have.status(403);
      });
      const body = pm.response.json();
      pm.test('Mensaje de error presente', function () {
        pm.expect(body.message).to.be.a('string').and.not.empty;
      });
    language: text/javascript
order: 700
```

- [ ] **Step 6: Crear CR-08 — Reserva ya cancelada**

```yaml
$kind: http-request
name: CR-08 Reserva ya cancelada – 422 Unprocessable Entity
description: |
  Intentar cancelar una reserva con estado CANCELADA → 422.
  Pre-condición: reservaIdCancelada apunta a una reserva con estado CANCELADA o DEVUELTA.
url: "{{baseUrl}}/api/reservas/{{reservaIdCancelada}}/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{clienteToken}}
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailCliente'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('clienteToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('Estado no cancelable → 422', function () {
        pm.response.to.have.status(422);
      });
    language: text/javascript
order: 800
```

- [ ] **Step 7: Crear CR-09 — CA-01: BASICA sin abono (camino feliz, repetible)**

```yaml
$kind: http-request
name: CR-09 CA-01 BASICA sin abono – 200 OK sin WhatsApp
description: |
  **Criterio:** CA-01 — Cancelación de reserva básica sin abono.
  El pre-request crea una nueva reserva BASICA (sin decoración con costo, sin pre-orden)
  y la captura en tmpReservaId. Luego se cancela esa reserva.
  Resultado: 200, estado=CANCELADA, requiereWhatsApp=false, mensajeWhatsApp ausente.
  Este test es repetible: crea su propio dato de prueba.
url: "{{baseUrl}}/api/reservas/{{tmpReservaId}}/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{clienteToken}}
scripts:
  - type: beforeRequest
    code: |-
      const pad = n => String(n).padStart(2, '0');
      const d = new Date();
      d.setFullYear(d.getFullYear() + 2 + Math.floor(Math.random() * 3));
      d.setMonth(Math.floor(Math.random() * 12));
      d.setDate(1 + Math.floor(Math.random() * 28));
      d.setHours(17 + Math.floor(Math.random() * 5), 0, 0, 0);
      const fecha = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:00:00`;

      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailCliente'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (err || !res || res.code !== 200) return;
        const token = res.json().accessToken;
        pm.environment.set('clienteToken', token);

        pm.sendRequest({
          url: pm.environment.get('baseUrl') + '/api/reservas',
          method: 'POST',
          header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
          body: { mode: 'raw', raw: JSON.stringify({
            fechaHoraLlegada: fecha,
            numeroPersonas: 2,
            zonaId: null,
            decoracionId: null,
            notas: null,
            preOrden: null
          })}
        }, function (e2, r2) {
          if (!e2 && r2 && r2.code === 201) {
            pm.environment.set('tmpReservaId', r2.json().data.reservaId);
          } else {
            console.warn('CR-09: creación de reserva BASICA falló', e2, r2 && r2.code);
          }
        });
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('El sistema retorna HTTP 200', function () {
        pm.response.to.have.status(200);
      });
      const body = pm.response.json();
      pm.test('El campo success es true', function () {
        pm.expect(body.success).to.be.true;
      });
      pm.test('El estado es CANCELADA', function () {
        pm.expect(body.data.estado).to.eql('CANCELADA');
      });
      pm.test('El tipo es BASICA', function () {
        pm.expect(body.data.tipo).to.eql('BASICA');
      });
      pm.test('requiereWhatsApp es false (sin abono)', function () {
        pm.expect(body.data.requiereWhatsApp).to.be.false;
      });
      pm.test('mensajeWhatsApp está ausente o es null (sin reembolso)', function () {
        pm.expect(body.data.mensajeWhatsApp == null || body.data.mensajeWhatsApp === undefined).to.be.true;
      });
      pm.test('reservaId coincide con la reserva cancelada', function () {
        pm.expect(String(body.data.reservaId)).to.eql(String(pm.environment.get('tmpReservaId')));
      });
      pm.environment.unset('tmpReservaId');
    language: text/javascript
order: 900
```

- [ ] **Step 8: Crear CR-10 — CA-02: BASICA con abono (usa seed, no repetible)**

```yaml
$kind: http-request
name: CR-10 CA-02 BASICA con abono – 200 OK requiereWhatsApp
description: |
  **Criterio:** CA-02 — Cancelación de reserva básica con abono.
  Usa la variable reservaIdBasicaConAbono (reserva BASICA + PENDIENTE/CONFIRMADA con abono
  en seed). Al cancelar → requiereWhatsApp=true y mensajeWhatsApp contiene el texto de
  gestión de reembolso.
  **NOTA:** Este test no es repetible. Requiere flyway:clean flyway:migrate para restaurar.
  Obtener ID de la DB: SELECT r.reserva_id FROM restaurante.reserva r
    JOIN restaurante.abono a ON a.reserva_id = r.reserva_id
    WHERE r.reserva_tipo = 'BASICA' AND r.reserva_estado IN ('PENDIENTE','CONFIRMADA')
    LIMIT 1;
url: "{{baseUrl}}/api/reservas/{{reservaIdBasicaConAbono}}/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{clienteToken}}
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailCliente'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('clienteToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('El sistema retorna HTTP 200', function () {
        pm.response.to.have.status(200);
      });
      const body = pm.response.json();
      pm.test('El estado es CANCELADA', function () {
        pm.expect(body.data.estado).to.eql('CANCELADA');
      });
      pm.test('requiereWhatsApp es true (tiene abono)', function () {
        pm.expect(body.data.requiereWhatsApp).to.be.true;
      });
      pm.test('mensajeWhatsApp contiene texto de reembolso', function () {
        pm.expect(body.data.mensajeWhatsApp).to.be.a('string').and.not.empty;
        pm.expect(body.data.mensajeWhatsApp).to.include('reembolso');
      });
    language: text/javascript
order: 1000
```

- [ ] **Step 9: Crear CR-11 — CA-03: ESPECIAL antes 16h (camino feliz, repetible)**

```yaml
$kind: http-request
name: CR-11 CA-03 ESPECIAL antes 16h – 200 OK requiereWhatsApp
description: |
  **Criterio:** CA-03 — Cancelación de reserva especial con reembolso.
  El pre-request crea una reserva ESPECIAL (decoración con costo) para 2+ años en el
  futuro. Como ahora < 16:00 de ese día futuro, el sistema devuelve requiereWhatsApp=true.
  Este test es repetible: crea su propio dato de prueba.
  Pre-condición: decoracionConCostoId apunta a una decoración ACTIVA con costo > 0.
url: "{{baseUrl}}/api/reservas/{{tmpReservaId}}/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{clienteToken}}
scripts:
  - type: beforeRequest
    code: |-
      const pad = n => String(n).padStart(2, '0');
      const d = new Date();
      d.setFullYear(d.getFullYear() + 2 + Math.floor(Math.random() * 3));
      d.setMonth(Math.floor(Math.random() * 12));
      d.setDate(1 + Math.floor(Math.random() * 28));
      d.setHours(17 + Math.floor(Math.random() * 5), 0, 0, 0);
      const fecha = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:00:00`;

      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailCliente'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (err || !res || res.code !== 200) return;
        const token = res.json().accessToken;
        pm.environment.set('clienteToken', token);

        pm.sendRequest({
          url: pm.environment.get('baseUrl') + '/api/reservas',
          method: 'POST',
          header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
          body: { mode: 'raw', raw: JSON.stringify({
            fechaHoraLlegada: fecha,
            numeroPersonas: 2,
            zonaId: null,
            decoracionId: parseInt(pm.environment.get('decoracionConCostoId')),
            notas: null,
            preOrden: null
          })}
        }, function (e2, r2) {
          if (!e2 && r2 && r2.code === 201) {
            pm.environment.set('tmpReservaId', r2.json().data.reservaId);
          } else {
            console.warn('CR-11: creación de reserva ESPECIAL falló', e2, r2 && r2.code, r2 && r2.text());
          }
        });
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('El sistema retorna HTTP 200', function () {
        pm.response.to.have.status(200);
      });
      const body = pm.response.json();
      pm.test('El estado es CANCELADA', function () {
        pm.expect(body.data.estado).to.eql('CANCELADA');
      });
      pm.test('El tipo es ESPECIAL', function () {
        pm.expect(body.data.tipo).to.eql('ESPECIAL');
      });
      pm.test('requiereWhatsApp es true (fecha futura, antes de 16h)', function () {
        pm.expect(body.data.requiereWhatsApp).to.be.true;
      });
      pm.test('mensajeWhatsApp contiene texto de reembolso', function () {
        pm.expect(body.data.mensajeWhatsApp).to.be.a('string').and.not.empty;
        pm.expect(body.data.mensajeWhatsApp).to.include('reembolso');
      });
      pm.environment.unset('tmpReservaId');
    language: text/javascript
order: 1100
```

- [ ] **Step 10: Crear CR-12 — CA-04: ESPECIAL después 16h (usa seed, no repetible)**

```yaml
$kind: http-request
name: CR-12 CA-04 ESPECIAL despues 16h – 200 OK sin WhatsApp
description: |
  **Criterio:** CA-04 — Cancelación de reserva especial sin reembolso.
  Usa reservaIdEspecialFechaPasada: reserva ESPECIAL con fecha pasada (la hora 16:00
  de ese día ya transcurrió) y estado PENDIENTE/CONFIRMADA. Al cancelar → no hay reembolso.
  **NOTA:** Este test no es repetible. Requiere flyway:clean flyway:migrate para restaurar.
  Obtener ID de la DB: SELECT reserva_id FROM restaurante.reserva
    WHERE reserva_tipo = 'ESPECIAL' AND reserva_estado IN ('PENDIENTE','CONFIRMADA')
    AND reserva_fecha_hora_llegada < NOW() LIMIT 1;
url: "{{baseUrl}}/api/reservas/{{reservaIdEspecialFechaPasada}}/cancelar"
method: PATCH
headers:
  Authorization: Bearer {{clienteToken}}
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
          email: pm.environment.get('emailCliente'),
          password: pm.environment.get('passwordValida'),
          forceSessionOverride: true
        })}
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('clienteToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('El sistema retorna HTTP 200', function () {
        pm.response.to.have.status(200);
      });
      const body = pm.response.json();
      pm.test('El estado es CANCELADA', function () {
        pm.expect(body.data.estado).to.eql('CANCELADA');
      });
      pm.test('requiereWhatsApp es false (después 16h, sin reembolso)', function () {
        pm.expect(body.data.requiereWhatsApp).to.be.false;
      });
      pm.test('mensajeWhatsApp está ausente o es null', function () {
        pm.expect(body.data.mensajeWhatsApp == null || body.data.mensajeWhatsApp === undefined).to.be.true;
      });
    language: text/javascript
order: 1200
```

- [ ] **Step 11: Agregar variables de entorno nuevas**

En `backend/postman/postman/environments/Al Toro – Local.environment.yaml`, agregar las siguientes entradas en la sección de variables:

```yaml
# Variables para cancelar reserva (HU-05)
- key: reservaIdBasicaConAbono
  value: ""
  description: >
    SELECT r.reserva_id FROM restaurante.reserva r
    JOIN restaurante.abono a ON a.reserva_id = r.reserva_id
    WHERE r.reserva_tipo = 'BASICA' AND r.reserva_estado IN ('PENDIENTE','CONFIRMADA')
    LIMIT 1;
- key: reservaIdEspecialFechaPasada
  value: ""
  description: >
    SELECT reserva_id FROM restaurante.reserva
    WHERE reserva_tipo = 'ESPECIAL' AND reserva_estado IN ('PENDIENTE','CONFIRMADA')
    AND reserva_fecha_hora_llegada < NOW() LIMIT 1;
```

- [ ] **Step 12: Commit de la colección Postman**

```bash
git add backend/postman/postman/collections/reservas/Al\ Toro\ –\ PATCH\ -api-reservas-\{reservaId\}-cancelar/
git add backend/postman/postman/environments/
git commit -m "test(postman): agregar colección CR-01..CR-12 para PATCH /api/reservas/{id}/cancelar"
```

---

## Verificación final

- [ ] **Ejecutar suite completa de tests**

Run: `cd backend && ./mvnw test -q 2>&1 | tail -20`
Expected: BUILD SUCCESS — 0 failures, 0 errors

- [ ] **Verificar cobertura de criterios de aceptación**

| CA | Test unitario | Test controller | Test Postman |
|----|--------------|-----------------|--------------|
| CA-01 | `BasicaSinAbono.*` | `basicaSinAbono_200SinWhatsApp` | CR-09 |
| CA-02 | `BasicaConAbono.*` | `basicaConAbono_200ConWhatsApp` | CR-10 |
| CA-03 | `EspecialAntes16h.*` | (cubierto por CA-02 de WhatsApp) | CR-11 |
| CA-04 | `EspecialDespues16h.*` | `especialDespues16h_200SinWhatsApp` | CR-12 |
| CA-05 | `Errores.estadoNoActivo_*` | `reservaYaCancelada_422` | CR-08 |
| Auth  | N/A | `sinToken_401`, `*SinPermiso_403` | CR-01 a CR-05 |
| 404   | `Errores.reservaNoExiste_*` | `reservaNoEncontrada_404` | CR-06 |
| 403   | `Errores.ownershipFalla_*` | `reservaOtroCliente_403` | CR-07 |

---

## Variables de entorno — resumen para CR-10 y CR-12

| Variable | Cómo obtener |
|----------|-------------|
| `reservaIdBasicaConAbono` | `SELECT r.reserva_id FROM restaurante.reserva r JOIN restaurante.abono a ON a.reserva_id = r.reserva_id WHERE r.reserva_tipo = 'BASICA' AND r.reserva_estado IN ('PENDIENTE','CONFIRMADA') LIMIT 1` |
| `reservaIdEspecialFechaPasada` | `SELECT reserva_id FROM restaurante.reserva WHERE reserva_tipo = 'ESPECIAL' AND reserva_estado IN ('PENDIENTE','CONFIRMADA') AND reserva_fecha_hora_llegada < NOW() LIMIT 1` |
| `reservaIdOtroCliente` | Ya definido en CLAUDE.md — usar el mismo |

CR-10 y CR-12 consumen datos de seed y no son repetibles. Ejecutar `./mvnw flyway:clean flyway:migrate` para restaurar el estado.

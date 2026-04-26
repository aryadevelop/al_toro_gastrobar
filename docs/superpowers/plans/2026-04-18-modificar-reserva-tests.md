# Modificar Reserva (HE-02-HU-04) — Plan de Tests

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Contexto:** Tests unitarios y Postman para `PUT /api/reservas/{reservaId}`. Dependen de que el
endpoint esté implementado ([plan principal](./2026-04-18-modificar-reserva.md)).

**Pre-condición de entorno (Postman):** Las siguientes variables deben estar configuradas:
```sql
-- decoracionConCostoId: decoración con costo > 0
SELECT decoracion_id FROM restaurante.decoracion
WHERE decoracion_costo_adicional > 0 AND decoracion_estado = 'ACTIVO' LIMIT 1;

-- decoracionConCostoId2: segunda decoración con costo > 0 (para test MR-13)
SELECT decoracion_id FROM restaurante.decoracion
WHERE decoracion_costo_adicional > 0 AND decoracion_estado = 'ACTIVO' OFFSET 1 LIMIT 1;
```

---

## Task 11: Tests unitarios — ReservaServiceModificarTest

**Archivos:**
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/reservas/service/ReservaServiceModificarTest.java`

- [ ] **Step 1: Escribir los tests**

```java
package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.inventario.repository.OpcionModificacionRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaMenuModificacionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.produccion.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.ModificarReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.BloqueDisponibilidadRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionZonaRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaService.modificarReserva")
class ReservaServiceModificarTest {

    @Mock ReservaRepository reservaRepository;
    @Mock DecoracionRepository decoracionRepository;
    @Mock DecoracionZonaRepository decoracionZonaRepository;
    @Mock ZonaRepository zonaRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock BloqueDisponibilidadRepository bloqueRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock ComandaMenuModificacionRepository comandaMenuModificacionRepository;
    @Mock ProductoRepository productoRepository;
    @Mock OpcionModificacionRepository opcionModificacionRepository;
    @Mock ProductoOpcionModificacionRepository productoOpcionModificacionRepository;
    @Mock ReservaMapper reservaMapper;
    @Mock AbonoRepository abonoRepository;

    @InjectMocks
    ReservaService service;

    private static final String EMAIL = "cliente@test.com";
    private static final Long RESERVA_ID = 1L;

    /** Reserva BÁSICA CONFIRMADA con fecha de llegada mañana a las 19:00. */
    private Reserva reservaBasicaConfirmada;

    /** Reserva ESPECIAL PENDIENTE con fecha de llegada mañana a las 19:00. */
    private Reserva reservaEspecialPendiente;

    /** Request de modificación mínima válida (solo fecha y personas). */
    private ModificarReservaRequest requestMinima;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        try {
            java.lang.reflect.Field emailField = Usuario.class.getDeclaredField("usuarioEmail");
            emailField.setAccessible(true);
            emailField.set(usuario, EMAIL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Cliente cliente = new Cliente();
        try {
            java.lang.reflect.Field usuarioField = Cliente.class.getSuperclass().getDeclaredField("usuario");
            usuarioField.setAccessible(true);
            usuarioField.set(cliente, usuario);
        } catch (Exception ex) {
            try {
                java.lang.reflect.Field usuarioField = Cliente.class.getDeclaredField("usuario");
                usuarioField.setAccessible(true);
                usuarioField.set(cliente, usuario);
            } catch (Exception ex2) {
                throw new RuntimeException(ex2);
            }
        }

        LocalDateTime llegadaManana = LocalDate.now().plusDays(1).atTime(19, 0);

        reservaBasicaConfirmada = Reserva.builder()
                .reservaId(RESERVA_ID)
                .cliente(cliente)
                .reservaFechaHoraLlegada(llegadaManana)
                .reservaNumeroPersonas(2)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        reservaEspecialPendiente = Reserva.builder()
                .reservaId(RESERVA_ID)
                .cliente(cliente)
                .reservaFechaHoraLlegada(llegadaManana)
                .reservaNumeroPersonas(2)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();

        requestMinima = new ModificarReservaRequest();
        try {
            java.lang.reflect.Field f1 = ModificarReservaRequest.class.getDeclaredField("fechaHoraLlegada");
            f1.setAccessible(true);
            f1.set(requestMinima, llegadaManana);
            java.lang.reflect.Field f2 = ModificarReservaRequest.class.getDeclaredField("numeroPersonas");
            f2.setAccessible(true);
            f2.set(requestMinima, 3);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Validaciones previas")
    class ValidacionesPrevias {

        @Test
        @DisplayName("Reserva no encontrada → ResourceNotFoundException")
        void reservaNoEncontrada_lanzaNotFoundException() {
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, requestMinima))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Cliente diferente al dueño → BusinessException ACCESS_DENIED")
        void clienteDiferente_lanzaAccessDenied() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));

            assertThatThrownBy(() ->
                    service.modificarReserva(RESERVA_ID, "otro@test.com", requestMinima))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("propias reservas");
        }

        @Test
        @DisplayName("Reserva en estado CANCELADA → BusinessException INVALID_STATE")
        void reservaCancelada_lanzaInvalidState() {
            Reserva cancelada = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(reservaBasicaConfirmada.getCliente())
                    .reservaFechaHoraLlegada(LocalDate.now().plusDays(1).atTime(19, 0))
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CANCELADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(cancelada));

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, requestMinima))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No es posible modificar esta reserva");
        }

        @Test
        @DisplayName("Hora límite estándar pasada (reserva de ayer) → BusinessException")
        void horaLimitePasada_lanzaInvalidState() {
            // Reserva para ayer: el límite estándar ya pasó
            LocalDateTime llegadaAyer = LocalDate.now().minusDays(1).atTime(19, 0);
            Reserva reservaAyer = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(reservaBasicaConfirmada.getCliente())
                    .reservaFechaHoraLlegada(llegadaAyer)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reservaAyer));
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, requestMinima))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo puedes cancelarla");
        }

        @Test
        @DisplayName("Request con menú especial y límite especial pasado → BusinessException")
        void nuevoMenuEspecialConLimiteEspecialPasado_lanzaInvalidState() {
            // Reserva básica para mañana pero el nuevo request incluye menú especial.
            // El límite especial es 23:00 del día anterior (hoy), que ya pasó si son más de las 23:00.
            // Para hacer el test determinístico: ponemos la reserva pasado mañana
            // y simulamos que el request tiene esMenuEspecial=true pero la fecha de límite ya venció.
            // Usamos una reserva para HOY (límite especial sería ayer a las 23:00, siempre vencido).
            LocalDateTime llegadaHoy = LocalDate.now().atTime(19, 0);
            Reserva reservaHoy = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(reservaBasicaConfirmada.getCliente())
                    .reservaFechaHoraLlegada(llegadaHoy)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reservaHoy));
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                    .thenReturn(Optional.empty());

            // Request con menú especial para forzar aplicaLimiteEspecial=true
            ModificarReservaRequest reqConMenuEspecial = new ModificarReservaRequest();
            try {
                java.lang.reflect.Field f1 = ModificarReservaRequest.class.getDeclaredField("fechaHoraLlegada");
                f1.setAccessible(true);
                f1.set(reqConMenuEspecial, llegadaHoy);
                java.lang.reflect.Field f2 = ModificarReservaRequest.class.getDeclaredField("numeroPersonas");
                f2.setAccessible(true);
                f2.set(reqConMenuEspecial, 15);
                co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest item =
                        new co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest();
                java.lang.reflect.Field fEsME = item.getClass().getDeclaredField("esMenuEspecial");
                fEsME.setAccessible(true);
                fEsME.set(item, Boolean.TRUE);
                java.lang.reflect.Field fProd = item.getClass().getDeclaredField("productoId");
                fProd.setAccessible(true);
                fProd.set(item, 1L);
                java.lang.reflect.Field fCant = item.getClass().getDeclaredField("cantidad");
                fCant.setAccessible(true);
                fCant.set(item, 1);
                java.lang.reflect.Field fPO = ModificarReservaRequest.class.getDeclaredField("preOrden");
                fPO.setAccessible(true);
                fPO.set(reqConMenuEspecial, List.of(item));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, reqConMenuEspecial))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo puedes cancelarla");
        }
    }

    @Nested
    @DisplayName("Transición BASICA → BASICA")
    class BasicaABasica {

        @Test
        @DisplayName("Modifica campos y mantiene CONFIRMADA; sin abono → no requiere WhatsApp")
        void basicaABasica_actualizaYMantiene() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));
            when(bloqueRepository.countBloquesParaFechaHora(any(), any())).thenReturn(0L);
            when(zonaRepository.findAll()).thenReturn(List.of(zonaConCapacidad(10L, 20)));
            when(reservaRepository.findPersonasPorZonaEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(any())).thenReturn(List.of());
            when(reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong()))
                    .thenReturn(0);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                    .thenReturn(Optional.empty());
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.save(any())).thenReturn(reservaBasicaConfirmada);
            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("BASICA")
                    .requiereWhatsApp(false).build();
            when(reservaMapper.toModificarResponse(any(), anyBoolean(), any()))
                    .thenReturn(respuestaMock);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, requestMinima);

            assertThat(respuesta.isRequiereWhatsApp()).isFalse();
            assertThat(respuesta.getEstado()).isEqualTo("CONFIRMADA");

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        }

        @Test
        @DisplayName("Con abono neto > platos → requiere WhatsApp (MSG_WA_ABONO_AJUSTE)")
        void basicaABasicaConAbonoMayorPlatos_requiereWhatsApp() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));
            when(bloqueRepository.countBloquesParaFechaHora(any(), any())).thenReturn(0L);
            when(zonaRepository.findAll()).thenReturn(List.of(zonaConCapacidad(10L, 20)));
            when(reservaRepository.findPersonasPorZonaEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(any())).thenReturn(List.of());
            when(reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong()))
                    .thenReturn(0);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                    .thenReturn(Optional.empty());

            // Abono de 50.000 pero nueva pre-orden vacía → totalPlatos = 0 < totalAbonos
            Abono anticipo = abonoConMonto(new BigDecimal("50000"), TipoAbono.ANTICIPO);
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of(anticipo));
            when(reservaRepository.save(any())).thenReturn(reservaBasicaConfirmada);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("BASICA")
                    .requiereWhatsApp(true).mensajeWhatsApp("msg").build();
            when(reservaMapper.toModificarResponse(any(), eq(true), anyString()))
                    .thenReturn(respuestaMock);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, requestMinima);

            assertThat(respuesta.isRequiereWhatsApp()).isTrue();
        }
    }

    @Nested
    @DisplayName("Transición ESPECIAL → BASICA")
    class EspecialABasica {

        @Test
        @DisplayName("Actualiza en-lugar a BASICA CONFIRMADA; sin abono → no requiere WhatsApp")
        void especialABasica_actualizaInPlace() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaEspecialPendiente));
            when(bloqueRepository.countBloquesParaFechaHora(any(), any())).thenReturn(0L);
            when(zonaRepository.findAll()).thenReturn(List.of(zonaConCapacidad(10L, 20)));
            when(reservaRepository.findPersonasPorZonaEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(any())).thenReturn(List.of());
            when(reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong()))
                    .thenReturn(0);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                    .thenReturn(Optional.empty());
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.save(any())).thenReturn(reservaEspecialPendiente);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("BASICA")
                    .requiereWhatsApp(false).build();
            when(reservaMapper.toModificarResponse(any(), anyBoolean(), any()))
                    .thenReturn(respuestaMock);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, requestMinima);

            // Update-in-place: un solo save, mismo reservaId
            verify(reservaRepository, times(1)).save(any(Reserva.class));
            assertThat(respuesta.getReservaId()).isEqualTo(RESERVA_ID);
            assertThat(respuesta.isRequiereWhatsApp()).isFalse();
        }

        @Test
        @DisplayName("Con abono neto > platos → requiere WhatsApp (MSG_WA_CAMBIO_ESPECIAL)")
        void especialABasicaConAbonoMayorPlatos_requiereWhatsApp() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaEspecialPendiente));
            when(bloqueRepository.countBloquesParaFechaHora(any(), any())).thenReturn(0L);
            when(zonaRepository.findAll()).thenReturn(List.of(zonaConCapacidad(10L, 20)));
            when(reservaRepository.findPersonasPorZonaEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(any())).thenReturn(List.of());
            when(reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong()))
                    .thenReturn(0);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                    .thenReturn(Optional.empty());

            Abono anticipo = abonoConMonto(new BigDecimal("80000"), TipoAbono.ANTICIPO);
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of(anticipo));
            when(reservaRepository.save(any())).thenReturn(reservaEspecialPendiente);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("BASICA")
                    .requiereWhatsApp(true).mensajeWhatsApp("msg").build();
            when(reservaMapper.toModificarResponse(any(), eq(true), anyString()))
                    .thenReturn(respuestaMock);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, requestMinima);

            assertThat(respuesta.isRequiereWhatsApp()).isTrue();
        }
    }

    @Nested
    @DisplayName("Transición ESPECIAL → ESPECIAL")
    class EspecialAEspecial {

        @Test
        @DisplayName("Sin cambio de valor (misma decoración, mismas personas) → no requiere WhatsApp")
        void sinCambioDeValor_noRequiereWhatsApp() {
            // Decoración con costo de 50.000
            Decoracion decoracion = decoracionConCosto(5L, new BigDecimal("50000"));
            Reserva reservaConDec = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(reservaEspecialPendiente.getCliente())
                    .reservaFechaHoraLlegada(LocalDate.now().plusDays(1).atTime(19, 0))
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.ESPECIAL)
                    .decoracion(decoracion)
                    .build();

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reservaConDec));
            when(bloqueRepository.countBloquesParaFechaHora(any(), any())).thenReturn(0L);
            when(zonaRepository.findAll()).thenReturn(List.of(zonaConCapacidad(10L, 20)));
            when(reservaRepository.findPersonasPorZonaEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(any())).thenReturn(List.of(decoracion));
            when(decoracionRepository.findById(5L)).thenReturn(Optional.of(decoracion));
            when(decoracionZonaRepository.findByDecoracionId(anyLong())).thenReturn(List.of());
            when(reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong()))
                    .thenReturn(0);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                    .thenReturn(Optional.empty());
            when(reservaRepository.save(any())).thenReturn(reservaConDec);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("PENDIENTE").tipo("ESPECIAL")
                    .requiereWhatsApp(false).build();
            when(reservaMapper.toModificarResponse(any(), anyBoolean(), any()))
                    .thenReturn(respuestaMock);

            // Request con la misma decoración y mismas personas → valorAnterior == valorNuevo
            ModificarReservaRequest reqMismaDecoracion = new ModificarReservaRequest();
            try {
                java.lang.reflect.Field f1 = ModificarReservaRequest.class.getDeclaredField("fechaHoraLlegada");
                f1.setAccessible(true);
                f1.set(reqMismaDecoracion, LocalDate.now().plusDays(1).atTime(19, 0));
                java.lang.reflect.Field f2 = ModificarReservaRequest.class.getDeclaredField("numeroPersonas");
                f2.setAccessible(true);
                f2.set(reqMismaDecoracion, 2);
                java.lang.reflect.Field f3 = ModificarReservaRequest.class.getDeclaredField("decoracionId");
                f3.setAccessible(true);
                f3.set(reqMismaDecoracion, 5L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, reqMismaDecoracion);

            assertThat(respuesta.isRequiereWhatsApp()).isFalse();
        }

        @Test
        @DisplayName("Decoración con costo diferente → valor cambia → requiere WhatsApp")
        void cambioDeValorPorDecoracion_requiereWhatsApp() {
            Decoracion decoracionAnterior = decoracionConCosto(5L, new BigDecimal("50000"));
            Decoracion decoracionNueva    = decoracionConCosto(6L, new BigDecimal("80000"));

            Reserva reservaConDecAnterior = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(reservaEspecialPendiente.getCliente())
                    .reservaFechaHoraLlegada(LocalDate.now().plusDays(1).atTime(19, 0))
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.ESPECIAL)
                    .decoracion(decoracionAnterior)
                    .build();

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reservaConDecAnterior));
            when(bloqueRepository.countBloquesParaFechaHora(any(), any())).thenReturn(0L);
            when(zonaRepository.findAll()).thenReturn(List.of(zonaConCapacidad(10L, 20)));
            when(reservaRepository.findPersonasPorZonaEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(any()))
                    .thenReturn(List.of(decoracionAnterior, decoracionNueva));
            when(decoracionRepository.findById(6L)).thenReturn(Optional.of(decoracionNueva));
            when(decoracionZonaRepository.findByDecoracionId(anyLong())).thenReturn(List.of());
            when(reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong()))
                    .thenReturn(0);
            // Pre-orden actual vacía (valorAnterior = solo decoracion = 50.000)
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                    .thenReturn(Optional.empty());
            when(reservaRepository.save(any())).thenReturn(reservaConDecAnterior);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("PENDIENTE").tipo("ESPECIAL")
                    .requiereWhatsApp(true).mensajeWhatsApp("msg").build();
            when(reservaMapper.toModificarResponse(any(), eq(true), anyString()))
                    .thenReturn(respuestaMock);

            ModificarReservaRequest reqNuevaDecoracion = new ModificarReservaRequest();
            try {
                java.lang.reflect.Field f1 = ModificarReservaRequest.class.getDeclaredField("fechaHoraLlegada");
                f1.setAccessible(true);
                f1.set(reqNuevaDecoracion, LocalDate.now().plusDays(1).atTime(19, 0));
                java.lang.reflect.Field f2 = ModificarReservaRequest.class.getDeclaredField("numeroPersonas");
                f2.setAccessible(true);
                f2.set(reqNuevaDecoracion, 2);
                java.lang.reflect.Field f3 = ModificarReservaRequest.class.getDeclaredField("decoracionId");
                f3.setAccessible(true);
                f3.set(reqNuevaDecoracion, 6L); // decoracionNueva con costo 80.000
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, reqNuevaDecoracion);

            assertThat(respuesta.isRequiereWhatsApp()).isTrue();
        }
    }

    @Nested
    @DisplayName("Disponibilidad cambió")
    class DisponibilidadCambio {

        @Test
        @DisplayName("Sin zonas libres → BusinessException con mensaje de disponibilidad")
        void sinZonasLibres_lanzaCambioDisponibilidad() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));
            when(bloqueRepository.countBloquesParaFechaHora(any(), any())).thenReturn(0L);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                    .thenReturn(Optional.empty());

            // Zona completamente ocupada
            Zona zonaLlena = zonaConCapacidad(10L, 5);
            when(zonaRepository.findAll()).thenReturn(List.of(zonaLlena));
            when(reservaRepository.findPersonasPorZonaEnDiaExcluyendo(any(), any(), any(), anyLong()))
                    .thenReturn(List.of(new Object[]{10L, 5L}));

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, requestMinima))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("disponibilidad cambió");
        }
    }

    // ─── utilidades de test ───────────────────────────────────────────────────

    private Zona zonaConCapacidad(Long id, int capacidad) {
        Zona z = new Zona();
        try {
            java.lang.reflect.Field fId = Zona.class.getDeclaredField("zonaId");
            fId.setAccessible(true);
            fId.set(z, id);
            java.lang.reflect.Field fCap = Zona.class.getDeclaredField("zonaCapacidadPersonas");
            fCap.setAccessible(true);
            fCap.set(z, capacidad);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return z;
    }

    private Decoracion decoracionConCosto(Long id, BigDecimal costo) {
        Decoracion d = new Decoracion();
        try {
            java.lang.reflect.Field fId = Decoracion.class.getDeclaredField("decoracionId");
            fId.setAccessible(true);
            fId.set(d, id);
            java.lang.reflect.Field fCosto = Decoracion.class.getDeclaredField("decoracionCostoAdicional");
            fCosto.setAccessible(true);
            fCosto.set(d, costo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return d;
    }

    private Abono abonoConMonto(BigDecimal monto, TipoAbono tipo) {
        Abono a = new Abono();
        try {
            java.lang.reflect.Field fMonto = Abono.class.getDeclaredField("abonoMonto");
            fMonto.setAccessible(true);
            fMonto.set(a, monto);
            java.lang.reflect.Field fTipo = Abono.class.getDeclaredField("abonoTipo");
            fTipo.setAccessible(true);
            fTipo.set(a, tipo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return a;
    }
}
```

- [ ] **Step 2: Ejecutar solo los nuevos tests**

```bash
cd backend && ./mvnw test -pl . -Dtest=ReservaServiceModificarTest -q
```
Esperado: todos los tests del archivo en verde.

- [ ] **Step 3: Ejecutar suite completa**

```bash
cd backend && ./mvnw test -q
```
Esperado: BUILD SUCCESS, 0 fallos.

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/co/edu/unicauca/backend/modules/reservas/service/ReservaServiceModificarTest.java
git commit -m "test(reservas): añadir ReservaServiceModificarTest para modificarReserva"
```

---

## Task 12: Postman — colección PUT /api/reservas/{reservaId}

**Archivos:**
- Create: `backend/postman/postman/collections/reservas/Al Toro – PUT -api-reservas-{reservaId}/`
  - `.resources/definition.yaml`
  - `MR-01 Sin token – 401 Unauthorized.request.yaml`
  - `MR-02 MESERO sin permiso – 403 Forbidden.request.yaml`
  - `MR-03 CAJERO sin permiso – 403 Forbidden.request.yaml`
  - `MR-04 COCINERO sin permiso – 403 Forbidden.request.yaml`
  - `MR-05 ADMIN sin permiso – 403 Forbidden.request.yaml`
  - `MR-06 Reserva no encontrada – 404.request.yaml`
  - `MR-07 Reserva de otro cliente – 403 Forbidden.request.yaml`
  - `MR-08 Reserva ya cancelada – 422.request.yaml`
  - `MR-09 Hora límite pasada – 422.request.yaml`
  - `MR-10 Modificar BASICA a BASICA – 200 OK.request.yaml`
  - `MR-11 Modificar BASICA a ESPECIAL – 200 requiereWhatsApp.request.yaml`
  - `MR-12 Modificar ESPECIAL a BASICA – 200 mismo reservaId.request.yaml`
  - `MR-13 ESPECIAL a ESPECIAL con cambio de valor – 200 requiereWhatsApp.request.yaml`
  - `MR-14 ESPECIAL a ESPECIAL sin cambio de valor – 200 sin WhatsApp.request.yaml`

- [ ] **Step 1: Crear definition.yaml**

```yaml
$kind: collection
name: Al Toro – PUT /api/reservas/{reservaId}
```

- [ ] **Step 2: Crear MR-01 Sin token – 401**

```yaml
$kind: http-request
name: MR-01 Sin token JWT – 401 Unauthorized
description: |
  Sin cabecera Authorization, el filtro JWT rechaza el request antes de llegar al controlador.
  Resultado esperado: 401 Unauthorized.
url: "{{baseUrl}}/api/reservas/1"
method: PUT
headers:
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "2028-06-15T19:00:00",
      "numeroPersonas": 2
    }
scripts:
  - type: afterResponse
    code: |-
      pm.test('Sin token → 401', function () {
        pm.response.to.have.status(401);
      });
    language: text/javascript
order: 1000
```

- [ ] **Step 3: Crear MR-02 a MR-05 (roles sin permiso – 403)**

**MR-02 (MESERO):**
```yaml
$kind: http-request
name: MR-02 MESERO sin permiso – 403 Forbidden
url: "{{baseUrl}}/api/reservas/1"
method: PUT
headers:
  Authorization: Bearer {{meseroToken}}
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "2028-06-15T19:00:00",
      "numeroPersonas": 2
    }
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
      pm.test('MESERO no puede modificar reservas → 403', function () {
        pm.response.to.have.status(403);
      });
    language: text/javascript
order: 2000
```

Crear MR-03, MR-04, MR-05 con la misma estructura para CAJERO, COCINERO y ADMIN respectivamente, cambiando `emailMesero`/`meseroToken` por las variables de entorno correspondientes (`emailCajero`/`cajeroToken`, `emailCocinero`/`cocineroToken`, `emailAdmin`/`adminToken`) y el `order` por 3000, 4000, 5000.

- [ ] **Step 4: Crear MR-06 Reserva no encontrada – 404**

```yaml
$kind: http-request
name: MR-06 Reserva no encontrada – 404 Not Found
description: |
  ID de reserva inexistente. El servicio lanza ResourceNotFoundException → 404.
url: "{{baseUrl}}/api/reservas/999999"
method: PUT
headers:
  Authorization: Bearer {{clienteToken}}
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "2028-06-15T19:00:00",
      "numeroPersonas": 2
    }
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
      pm.test('Reserva inexistente → 404', function () {
        pm.response.to.have.status(404);
      });
    language: text/javascript
order: 6000
```

- [ ] **Step 5: Crear MR-07 Reserva de otro cliente – 403**

```yaml
$kind: http-request
name: MR-07 Reserva de otro cliente – 403 Forbidden
description: |
  El cliente autenticado intenta modificar una reserva que pertenece a otro cliente.
  El servicio detecta el mismatch de ownership → BusinessException ACCESS_DENIED → 403.
  Pre-condición: `reservaIdOtroCliente` debe apuntar a una reserva de seed activa de un cliente diferente.
url: "{{baseUrl}}/api/reservas/{{reservaIdOtroCliente}}"
method: PUT
headers:
  Authorization: Bearer {{clienteToken}}
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "2028-06-15T19:00:00",
      "numeroPersonas": 2
    }
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
      pm.test('Reserva de otro cliente → 403', function () {
        pm.response.to.have.status(403);
      });
    language: text/javascript
order: 7000
```

**Nota de entorno:** `reservaIdOtroCliente` = `SELECT r.reserva_id FROM restaurante.reserva r JOIN restaurante.usuario u ON u.usuario_id = r.cliente_id WHERE u.usuario_email <> '{{emailCliente}}' AND r.reserva_estado IN ('PENDIENTE','CONFIRMADA') LIMIT 1`

- [ ] **Step 6: Crear MR-08 Reserva ya cancelada – 422**

```yaml
$kind: http-request
name: MR-08 Reserva ya cancelada – 422 Unprocessable
description: |
  Intento de modificar una reserva con estado CANCELADA o DEVUELTA.
  El servicio verifica el estado antes de proceder → BusinessException INVALID_STATE → 422.
  Pre-condición: `reservaIdCancelada` apunta a una reserva en estado terminal (ya existe en CLAUDE.md).
url: "{{baseUrl}}/api/reservas/{{reservaIdCancelada}}"
method: PUT
headers:
  Authorization: Bearer {{clienteToken}}
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "2028-06-15T19:00:00",
      "numeroPersonas": 2
    }
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
      pm.test('Reserva cancelada → 422', function () {
        pm.response.to.have.status(422);
      });
    language: text/javascript
order: 8000
```

- [ ] **Step 7: Crear MR-09 Hora límite pasada – 422**

```yaml
$kind: http-request
name: MR-09 Hora límite pasada – 422 Unprocessable
description: |
  El cliente intenta modificar una reserva cuyo límite de modificación ya venció.
  El beforeRequest crea una reserva para HOY a las 19:00; como el límite estándar
  es hoy a las 13:00 y casi siempre ya pasó (o el test corre en horario nocturno), 
  se verifica la regla. Si el test se ejecuta antes de las 13:00 puede dar 200 — 
  en ese caso usar una reserva de ayer del seed.
  Resultado esperado: 422 Unprocessable Entity.
url: "{{baseUrl}}/api/reservas/{{tmpReservaHoyId}}"
method: PUT
headers:
  Authorization: Bearer {{clienteToken}}
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "2028-06-15T19:00:00",
      "numeroPersonas": 4
    }
scripts:
  - type: beforeRequest
    code: |-
      const pad = n => String(n).padStart(2, '0');
      const hoy = new Date();
      const fechaHoy = hoy.getFullYear() + '-' + pad(hoy.getMonth() + 1) + '-' + pad(hoy.getDate()) + 'T19:00:00';

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
        pm.environment.set('clienteToken', res.json().accessToken);

        pm.sendRequest({
          url: pm.environment.get('baseUrl') + '/api/reservas',
          method: 'POST',
          header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + res.json().accessToken },
          body: { mode: 'raw', raw: JSON.stringify({ fechaHoraLlegada: fechaHoy, numeroPersonas: 2 })}
        }, function (e2, r2) {
          if (!e2 && r2 && r2.code === 201) {
            pm.environment.set('tmpReservaHoyId', r2.json().data.reservaId);
          }
        });
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('Hora límite pasada → 422', function () {
        pm.response.to.have.status(422);
      });
      pm.environment.unset('tmpReservaHoyId');
    language: text/javascript
order: 9000
```

- [ ] **Step 8: Crear MR-10 Modificar BASICA → BASICA – 200 OK**

```yaml
$kind: http-request
name: MR-10 Modificar BASICA a BASICA – 200 OK sin WhatsApp
description: |
  Crea una reserva BASICA y la modifica cambiando solo el número de personas.
  Resultado esperado: 200, tipo=BASICA, estado=CONFIRMADA, requiereWhatsApp=false.
url: "{{baseUrl}}/api/reservas/{{tmpReservaId}}"
method: PUT
headers:
  Authorization: Bearer {{clienteToken}}
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "{{tmpFechaLibre}}",
      "numeroPersonas": 4
    }
scripts:
  - type: beforeRequest
    code: |-
      const pad = n => String(n).padStart(2, '0');
      const d = new Date();
      d.setFullYear(d.getFullYear() + 2 + Math.floor(Math.random() * 3));
      d.setMonth(Math.floor(Math.random() * 12));
      d.setDate(1 + Math.floor(Math.random() * 28));
      d.setHours(17 + Math.floor(Math.random() * 5), 0, 0, 0);
      const fecha = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
        + 'T' + pad(d.getHours()) + ':00:00';
      pm.environment.set('tmpFechaLibre', fecha);

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
        pm.environment.set('clienteToken', res.json().accessToken);

        pm.sendRequest({
          url: pm.environment.get('baseUrl') + '/api/reservas',
          method: 'POST',
          header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + res.json().accessToken },
          body: { mode: 'raw', raw: JSON.stringify({ fechaHoraLlegada: fecha, numeroPersonas: 2 })}
        }, function (e2, r2) {
          if (!e2 && r2 && r2.code === 201) {
            pm.environment.set('tmpReservaId', r2.json().data.reservaId);
          }
        });
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('Modificación BASICA→BASICA → 200', function () {
        pm.response.to.have.status(200);
      });
      pm.test('Tipo sigue siendo BASICA y estado CONFIRMADA', function () {
        const data = pm.response.json().data;
        pm.expect(data.tipo).to.eql('BASICA');
        pm.expect(data.estado).to.eql('CONFIRMADA');
        pm.expect(data.requiereWhatsApp).to.be.false;
        pm.expect(data.numeroPersonas).to.eql(4);
      });
      pm.environment.unset('tmpReservaId');
      pm.environment.unset('tmpFechaLibre');
    language: text/javascript
order: 10000
```

- [ ] **Step 9: Crear MR-11 Modificar BASICA → ESPECIAL – 200 requiereWhatsApp**

```yaml
$kind: http-request
name: MR-11 Modificar BASICA a ESPECIAL (decoracion con costo) – 200 requiereWhatsApp
description: |
  Crea una reserva BASICA y la modifica añadiendo una decoración con costo > 0.
  Resultado esperado: 200, tipo=ESPECIAL, estado=PENDIENTE, requiereWhatsApp=true.
  Pre-condición: variable de entorno `decoracionConCostoId` con ID de decoración costosa.
url: "{{baseUrl}}/api/reservas/{{tmpReservaId}}"
method: PUT
headers:
  Authorization: Bearer {{clienteToken}}
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "{{tmpFechaLibre}}",
      "numeroPersonas": 2,
      "decoracionId": {{decoracionConCostoId}}
    }
scripts:
  - type: beforeRequest
    code: |-
      const pad = n => String(n).padStart(2, '0');
      const d = new Date();
      d.setFullYear(d.getFullYear() + 2 + Math.floor(Math.random() * 3));
      d.setMonth(Math.floor(Math.random() * 12));
      d.setDate(1 + Math.floor(Math.random() * 28));
      d.setHours(17 + Math.floor(Math.random() * 5), 0, 0, 0);
      const fecha = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
        + 'T' + pad(d.getHours()) + ':00:00';
      pm.environment.set('tmpFechaLibre', fecha);

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
        pm.environment.set('clienteToken', res.json().accessToken);

        pm.sendRequest({
          url: pm.environment.get('baseUrl') + '/api/reservas',
          method: 'POST',
          header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + res.json().accessToken },
          body: { mode: 'raw', raw: JSON.stringify({ fechaHoraLlegada: fecha, numeroPersonas: 2 })}
        }, function (e2, r2) {
          if (!e2 && r2 && r2.code === 201) {
            pm.environment.set('tmpReservaId', r2.json().data.reservaId);
          }
        });
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('Modificación BASICA→ESPECIAL → 200', function () {
        pm.response.to.have.status(200);
      });
      pm.test('Tipo ESPECIAL, estado PENDIENTE, requiereWhatsApp=true con mensaje', function () {
        const data = pm.response.json().data;
        pm.expect(data.tipo).to.eql('ESPECIAL');
        pm.expect(data.estado).to.eql('PENDIENTE');
        pm.expect(data.requiereWhatsApp).to.be.true;
        pm.expect(data.mensajeWhatsApp).to.be.a('string').and.not.empty;
      });
      pm.environment.unset('tmpReservaId');
      pm.environment.unset('tmpFechaLibre');
    language: text/javascript
order: 11000
```

- [ ] **Step 10: Crear MR-12 Modificar ESPECIAL → BASICA – 200 mismo reservaId**

```yaml
$kind: http-request
name: MR-12 Modificar ESPECIAL a BASICA – 200 mismo reservaId sin WhatsApp
description: |
  Crea una reserva ESPECIAL (decoración con costo) y la modifica quitando la decoración.
  El sistema actualiza la reserva en-lugar: la entidad pasa a BASICA CONFIRMADA conservando el mismo reservaId.
  Sin abonos previos → requiereWhatsApp=false.
  Resultado esperado: 200, tipo=BASICA, estado=CONFIRMADA, reservaId igual al original, requiereWhatsApp=false.
  Pre-condición: `decoracionConCostoId` disponible en el entorno.
url: "{{baseUrl}}/api/reservas/{{tmpReservaEspecialId}}"
method: PUT
headers:
  Authorization: Bearer {{clienteToken}}
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "{{tmpFechaLibre}}",
      "numeroPersonas": 2
    }
scripts:
  - type: beforeRequest
    code: |-
      const pad = n => String(n).padStart(2, '0');
      const d = new Date();
      d.setFullYear(d.getFullYear() + 2 + Math.floor(Math.random() * 3));
      d.setMonth(Math.floor(Math.random() * 12));
      d.setDate(1 + Math.floor(Math.random() * 28));
      d.setHours(17 + Math.floor(Math.random() * 5), 0, 0, 0);
      const fecha = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
        + 'T' + pad(d.getHours()) + ':00:00';
      pm.environment.set('tmpFechaLibre', fecha);

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
        pm.environment.set('clienteToken', res.json().accessToken);

        pm.sendRequest({
          url: pm.environment.get('baseUrl') + '/api/reservas',
          method: 'POST',
          header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + res.json().accessToken },
          body: { mode: 'raw', raw: JSON.stringify({
            fechaHoraLlegada: fecha,
            numeroPersonas: 2,
            decoracionId: parseInt(pm.environment.get('decoracionConCostoId'))
          })}
        }, function (e2, r2) {
          if (!e2 && r2 && r2.code === 201) {
            pm.environment.set('tmpReservaEspecialId', r2.json().data.reservaId);
          }
        });
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('Modificación ESPECIAL→BASICA → 200', function () {
        pm.response.to.have.status(200);
      });
      pm.test('Tipo BASICA, estado CONFIRMADA, mismo reservaId, sin WhatsApp', function () {
        const data = pm.response.json().data;
        pm.expect(data.tipo).to.eql('BASICA');
        pm.expect(data.estado).to.eql('CONFIRMADA');
        pm.expect(data.requiereWhatsApp).to.be.false;
        pm.expect(data.reservaId).to.eql(parseInt(pm.environment.get('tmpReservaEspecialId')));
      });
      pm.environment.unset('tmpReservaEspecialId');
      pm.environment.unset('tmpFechaLibre');
    language: text/javascript
order: 12000
```

- [ ] **Step 11: Crear MR-13 ESPECIAL → ESPECIAL con cambio de valor – 200 requiereWhatsApp**

```yaml
$kind: http-request
name: MR-13 ESPECIAL a ESPECIAL con cambio de decoracion – 200 requiereWhatsApp
description: |
  Crea una reserva ESPECIAL con decoracionConCostoId y la modifica cambiando a decoracionConCostoId2
  (otra decoración con costo diferente). El valor total cambia → requiereWhatsApp=true.
  Resultado esperado: 200, tipo=ESPECIAL, estado=PENDIENTE, requiereWhatsApp=true.
  Pre-condición: `decoracionConCostoId` y `decoracionConCostoId2` con costos distintos.
url: "{{baseUrl}}/api/reservas/{{tmpReservaEspecialId}}"
method: PUT
headers:
  Authorization: Bearer {{clienteToken}}
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "{{tmpFechaLibre}}",
      "numeroPersonas": 2,
      "decoracionId": {{decoracionConCostoId2}}
    }
scripts:
  - type: beforeRequest
    code: |-
      const pad = n => String(n).padStart(2, '0');
      const d = new Date();
      d.setFullYear(d.getFullYear() + 2 + Math.floor(Math.random() * 3));
      d.setMonth(Math.floor(Math.random() * 12));
      d.setDate(1 + Math.floor(Math.random() * 28));
      d.setHours(17 + Math.floor(Math.random() * 5), 0, 0, 0);
      const fecha = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
        + 'T' + pad(d.getHours()) + ':00:00';
      pm.environment.set('tmpFechaLibre', fecha);

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
        pm.environment.set('clienteToken', res.json().accessToken);

        pm.sendRequest({
          url: pm.environment.get('baseUrl') + '/api/reservas',
          method: 'POST',
          header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + res.json().accessToken },
          body: { mode: 'raw', raw: JSON.stringify({
            fechaHoraLlegada: fecha,
            numeroPersonas: 2,
            decoracionId: parseInt(pm.environment.get('decoracionConCostoId'))
          })}
        }, function (e2, r2) {
          if (!e2 && r2 && r2.code === 201) {
            pm.environment.set('tmpReservaEspecialId', r2.json().data.reservaId);
          }
        });
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('ESPECIAL→ESPECIAL con cambio de valor → 200', function () {
        pm.response.to.have.status(200);
      });
      pm.test('Tipo ESPECIAL, estado PENDIENTE, requiereWhatsApp=true con mensaje', function () {
        const data = pm.response.json().data;
        pm.expect(data.tipo).to.eql('ESPECIAL');
        pm.expect(data.estado).to.eql('PENDIENTE');
        pm.expect(data.requiereWhatsApp).to.be.true;
        pm.expect(data.mensajeWhatsApp).to.be.a('string').and.not.empty;
      });
      pm.environment.unset('tmpReservaEspecialId');
      pm.environment.unset('tmpFechaLibre');
    language: text/javascript
order: 13000
```

- [ ] **Step 12: Crear MR-14 ESPECIAL → ESPECIAL sin cambio de valor – 200 sin WhatsApp**

```yaml
$kind: http-request
name: MR-14 ESPECIAL a ESPECIAL sin cambio de valor – 200 sin WhatsApp
description: |
  Crea una reserva ESPECIAL con decoracionConCostoId y la modifica con la misma decoración y personas.
  El valor total no cambia → requiereWhatsApp=false.
  Resultado esperado: 200, tipo=ESPECIAL, estado=PENDIENTE, requiereWhatsApp=false.
url: "{{baseUrl}}/api/reservas/{{tmpReservaEspecialId}}"
method: PUT
headers:
  Authorization: Bearer {{clienteToken}}
  Content-Type: application/json
body:
  type: text
  content: |-
    {
      "fechaHoraLlegada": "{{tmpFechaLibre}}",
      "numeroPersonas": 2,
      "decoracionId": {{decoracionConCostoId}}
    }
scripts:
  - type: beforeRequest
    code: |-
      const pad = n => String(n).padStart(2, '0');
      const d = new Date();
      d.setFullYear(d.getFullYear() + 2 + Math.floor(Math.random() * 3));
      d.setMonth(Math.floor(Math.random() * 12));
      d.setDate(1 + Math.floor(Math.random() * 28));
      d.setHours(17 + Math.floor(Math.random() * 5), 0, 0, 0);
      const fecha = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
        + 'T' + pad(d.getHours()) + ':00:00';
      pm.environment.set('tmpFechaLibre', fecha);

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
        pm.environment.set('clienteToken', res.json().accessToken);

        pm.sendRequest({
          url: pm.environment.get('baseUrl') + '/api/reservas',
          method: 'POST',
          header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + res.json().accessToken },
          body: { mode: 'raw', raw: JSON.stringify({
            fechaHoraLlegada: fecha,
            numeroPersonas: 2,
            decoracionId: parseInt(pm.environment.get('decoracionConCostoId'))
          })}
        }, function (e2, r2) {
          if (!e2 && r2 && r2.code === 201) {
            pm.environment.set('tmpReservaEspecialId', r2.json().data.reservaId);
          }
        });
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('ESPECIAL→ESPECIAL sin cambio de valor → 200', function () {
        pm.response.to.have.status(200);
      });
      pm.test('Tipo ESPECIAL, estado PENDIENTE, requiereWhatsApp=false', function () {
        const data = pm.response.json().data;
        pm.expect(data.tipo).to.eql('ESPECIAL');
        pm.expect(data.estado).to.eql('PENDIENTE');
        pm.expect(data.requiereWhatsApp).to.be.false;
      });
      pm.environment.unset('tmpReservaEspecialId');
      pm.environment.unset('tmpFechaLibre');
    language: text/javascript
order: 14000
```

- [ ] **Step 13: Registrar en CLAUDE.md la nueva colección Postman**

En `backend/CLAUDE.md`, en la sección "Colecciones Postman generadas", añadir:

```markdown
| `reservas/Al Toro – PUT -api-reservas-{reservaId}/` | `PUT /api/reservas/{reservaId}` |
```

Y añadir en "Variables de entorno":

```markdown
| `decoracionConCostoId`  | `SELECT decoracion_id FROM restaurante.decoracion WHERE decoracion_costo_adicional > 0 AND decoracion_estado = 'ACTIVO' LIMIT 1` |
| `decoracionConCostoId2` | Segunda decoración con costo para MR-13; misma query con `OFFSET 1 LIMIT 1` |
| `reservaIdOtroCliente`  | `SELECT r.reserva_id FROM restaurante.reserva r JOIN restaurante.usuario u ON u.usuario_id = r.cliente_id WHERE u.usuario_email <> '<emailCliente>' AND r.reserva_estado IN ('PENDIENTE','CONFIRMADA') LIMIT 1` |
```

- [ ] **Step 14: Commit final**

```bash
git add backend/postman/
git add backend/CLAUDE.md
git commit -m "test(reservas): añadir colección Postman MR-01..MR-14 para PUT /api/reservas/{reservaId}"
```

---

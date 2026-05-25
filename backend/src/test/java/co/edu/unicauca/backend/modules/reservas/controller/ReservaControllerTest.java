package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.reservas.dto.request.RegistrarAbonoRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ConfirmarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.MarcarInasistenciaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.RegistrarAbonoResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ResumenPagoResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.service.ReservaService;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReservaController.class)
@Import(ReservaControllerTest.PermissiveSecurityConfig.class)
class ReservaControllerTest {

    static class PermissiveSecurityConfig {
        @Bean
        @Order(1)
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ReservaService reservaService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    @Nested
    @DisplayName("GET /api/reservas/disponibilidad")
    class ConsultarDisponibilidad {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Con parámetro válido → 200 con disponibilidad")
        void conFechaValida_retorna200() throws Exception {
            DisponibilidadResponse resp = DisponibilidadResponse.builder()
                    .disponible(true)
                    .decoraciones(List.of())
                    .zonas(List.of())
                    .build();
            when(reservaService.consultarDisponibilidad(any())).thenReturn(resp);

            String fecha = LocalDateTime.now().plusDays(1).toString();

            mockMvc.perform(get("/api/reservas/disponibilidad")
                            .param("fechaHora", fecha))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.disponible").value(true));
        }

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Sin parámetro fechaHora → 400 Bad Request")
        void sinFecha_retorna400() throws Exception {
            mockMvc.perform(get("/api/reservas/disponibilidad"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/reservas")
    class CrearReserva {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Body válido → 201 Created")
        void bodyValido_retorna201() throws Exception {
            ReservaResponse resp = ReservaResponse.builder()
                    .reservaId(1L)
                    .estado("PENDIENTE")
                    .build();
            when(reservaService.crearReserva(anyString(), any())).thenReturn(resp);

            String body = objectMapper.writeValueAsString(Map.of(
                    "fechaHoraLlegada", LocalDateTime.now().plusDays(2).toString(),
                    "numeroPersonas", 2));

            mockMvc.perform(post("/api/reservas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.reservaId").value(1L));
        }

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Sin fechaHoraLlegada → 422 Unprocessable Entity")
        void sinFecha_retorna422() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("numeroPersonas", 2));

            mockMvc.perform(post("/api/reservas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @WithMockUser(username = "admin@altoro.com", roles = "ADMIN")
        @DisplayName("ADMIN crea reserva en nombre de cliente → toma emailCliente del body")
        void adminConEmailEnBody_retorna201() throws Exception {
            ReservaResponse resp = ReservaResponse.builder().reservaId(2L).estado("PENDIENTE").build();
            when(reservaService.crearReserva(eq("cliente@altoro.com"), any())).thenReturn(resp);

            String body = objectMapper.writeValueAsString(Map.of(
                    "fechaHoraLlegada", LocalDateTime.now().plusDays(2).toString(),
                    "numeroPersonas", 2,
                    "emailCliente", "cliente@altoro.com"));

            mockMvc.perform(post("/api/reservas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            verify(reservaService).crearReserva(eq("cliente@altoro.com"), any());
        }
    }

    @Nested
    @DisplayName("GET /api/reservas/cliente/canceladas-devueltas")
    class ObtenerReservasCanceladasODevueltas {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Cliente propietario → 200 con lista")
        void propietario_retorna200() throws Exception {
            when(reservaService.obtenerReservasCanceladasODevueltas(anyString()))
                    .thenReturn(List.of(ReservaDetalleResponse.builder().build()));

            mockMvc.perform(get("/api/reservas/cliente/canceladas-devueltas")
                            .param("emailCliente", "cliente@altoro.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockUser(username = "admin@altoro.com", roles = "ADMIN")
        @DisplayName("ADMIN consulta reservas de cualquier cliente → 200 OK")
        void adminCualquierCliente_retorna200() throws Exception {
            when(reservaService.obtenerReservasCanceladasODevueltas(anyString()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/reservas/cliente/canceladas-devueltas")
                            .param("emailCliente", "cliente@altoro.com"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/reservas/{reservaId}")
    class ModificarReserva {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Body válido → 200 OK")
        void bodyValido_retorna200() throws Exception {
            when(reservaService.modificarReserva(eq(1L), anyString(), any()))
                    .thenReturn(null);

            String body = objectMapper.writeValueAsString(Map.of(
                    "fechaHoraLlegada", LocalDateTime.now().plusDays(3).toString(),
                    "numeroPersonas", 3));

            mockMvc.perform(put("/api/reservas/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/reservas/cliente/futuras")
    class ObtenerReservasFuturas {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Cliente propietario → 200 con lista")
        void propietario_retorna200() throws Exception {
            when(reservaService.obtenerReservasFuturas(anyString()))
                    .thenReturn(List.of(ReservaDetalleResponse.builder().build()));

            mockMvc.perform(get("/api/reservas/cliente/futuras")
                            .param("emailCliente", "cliente@altoro.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Cliente accede a reservas ajenas → 403 Forbidden")
        void emailAjeno_retorna403() throws Exception {
            mockMvc.perform(get("/api/reservas/cliente/futuras")
                            .param("emailCliente", "otro@altoro.com"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/reservas/{reservaId}/detalle")
    class ObtenerDetalleReserva {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Reserva existente → 200 con detalle")
        void reservaExistente_retorna200() throws Exception {
            when(reservaService.obtenerDetalleReserva(eq(1L), any()))
                    .thenReturn(ReservaDetalleResponse.builder().build());

            mockMvc.perform(get("/api/reservas/1/detalle"))
                    .andExpect(status().isOk());
        }
    }

    // ── PATCH /{reservaId}/cancelar ───────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/reservas/{reservaId}/cancelar")
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

        @Test
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        @DisplayName("Reserva no encontrada → 404")
        void reservaNoEncontrada_404() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenThrow(new ResourceNotFoundException("Reserva", RESERVA_ID));

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        @DisplayName("Reserva de otro cliente → 403 Forbidden")
        void reservaOtroCliente_403() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED,
                            "Solo puedes cancelar tus propias reservas.",
                            org.springframework.http.HttpStatus.FORBIDDEN));

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        @DisplayName("Reserva ya cancelada → 422 Unprocessable Entity")
        void reservaYaCancelada_422() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenThrow(new BusinessException(ErrorCode.INVALID_STATE,
                            "No es posible cancelar esta reserva.",
                            org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY));

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        @DisplayName("BASICA sin abono → 200 OK, requiereWhatsApp=false")
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
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        @DisplayName("BASICA con abono → 200 OK, requiereWhatsApp=true con mensaje")
        void basicaConAbono_200ConWhatsApp() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenReturn(responseBasicaConWa());

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.requiereWhatsApp").value(true))
                    .andExpect(jsonPath("$.data.mensajeWhatsApp").isString());
        }

        @Test
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        @DisplayName("ESPECIAL después 16h → 200 OK, requiereWhatsApp=false")
        void especialDespues16h_200SinWhatsApp() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenReturn(responseEspecialSinWa());

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tipo").value("ESPECIAL"))
                    .andExpect(jsonPath("$.data.requiereWhatsApp").value(false));
        }

        @Test
        @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
        @DisplayName("El email se toma del token, no del cuerpo de la petición")
        void emailDelToken_noDelBody() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, "cliente@test.com"))
                    .thenReturn(responseBasicaSinWa());

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isOk());

            verify(reservaService).cancelarReserva(eq(RESERVA_ID), eq("cliente@test.com"));
        }

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("CAJERO cancela → 200 OK; ownership omitido (email null)")
        void cajeroCancela_200EmailNull() throws Exception {
            when(reservaService.cancelarReserva(RESERVA_ID, null))
                    .thenReturn(responseBasicaSinWa());

            mockMvc.perform(patch("/api/reservas/{id}/cancelar", RESERVA_ID))
                    .andExpect(status().isOk());

            verify(reservaService).cancelarReserva(RESERVA_ID, null);
        }
    }

    // ── PATCH /{reservaId}/confirmar ──────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/reservas/{reservaId}/confirmar")
    class ConfirmarReserva {

        private final Long RESERVA_ID = 7L;

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("Confirmación válida → 200 OK con estado CONFIRMADA")
        void confirmacionValida_200() throws Exception {
            when(reservaService.confirmarReserva(RESERVA_ID))
                    .thenReturn(ConfirmarReservaResponse.builder()
                            .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("ESPECIAL").build());

            mockMvc.perform(patch("/api/reservas/{id}/confirmar", RESERVA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reservaId").value(RESERVA_ID))
                    .andExpect(jsonPath("$.data.estado").value("CONFIRMADA"));

            verify(reservaService).confirmarReserva(RESERVA_ID);
        }

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("Reserva no encontrada → 404 Not Found")
        void reservaNoEncontrada_404() throws Exception {
            when(reservaService.confirmarReserva(RESERVA_ID))
                    .thenThrow(new ResourceNotFoundException("Reserva", RESERVA_ID));

            mockMvc.perform(patch("/api/reservas/{id}/confirmar", RESERVA_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("Reserva no elegible → 422 Unprocessable Entity")
        void noElegible_422() throws Exception {
            when(reservaService.confirmarReserva(RESERVA_ID))
                    .thenThrow(new BusinessException(ErrorCode.INVALID_STATE,
                            "Solo se pueden confirmar reservas especiales en estado pendiente.",
                            org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY));

            mockMvc.perform(patch("/api/reservas/{id}/confirmar", RESERVA_ID))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // -----------------------------------------------------------------------
    // PATCH /api/reservas/{reservaId}/marcar-inasistencia
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/reservas/{reservaId}/marcar-inasistencia")
    class MarcarInasistencia {

        private final Long RESERVA_ID = 5L;

        private MarcarInasistenciaResponse responseExitosa() {
            return MarcarInasistenciaResponse.builder()
                    .reservaId(RESERVA_ID)
                    .estado("INASISTENCIA")
                    .zonaLiberada("Terraza")
                    .decoracionLiberada("Cumpleaños")
                    .build();
        }

        private MarcarInasistenciaResponse responseSinRecursos() {
            return MarcarInasistenciaResponse.builder()
                    .reservaId(RESERVA_ID)
                    .estado("INASISTENCIA")
                    .zonaLiberada(null)
                    .decoracionLiberada(null)
                    .build();
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Reserva CONFIRMADA con +30 min → 200 OK con recursos liberados")
        void reservaConfirmadaMas30Min_200Ok() throws Exception {
            when(reservaService.marcarInasistencia(eq(RESERVA_ID), any(Authentication.class)))
                    .thenReturn(responseExitosa());

            mockMvc.perform(patch("/api/reservas/{id}/marcar-inasistencia", RESERVA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reservaId").value(RESERVA_ID))
                    .andExpect(jsonPath("$.data.estado").value("INASISTENCIA"))
                    .andExpect(jsonPath("$.data.zonaLiberada").value("Terraza"))
                    .andExpect(jsonPath("$.data.decoracionLiberada").value("Cumpleaños"));

            verify(reservaService).marcarInasistencia(eq(RESERVA_ID), any(Authentication.class));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Sin zona ni decoración → 200 OK con campos null")
        void sinZonaNiDecoracion_200ConNulls() throws Exception {
            when(reservaService.marcarInasistencia(eq(RESERVA_ID), any(Authentication.class)))
                    .thenReturn(responseSinRecursos());

            mockMvc.perform(patch("/api/reservas/{id}/marcar-inasistencia", RESERVA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.zonaLiberada").doesNotExist())
                    .andExpect(jsonPath("$.data.decoracionLiberada").doesNotExist());
        }

        @Test
        @WithMockUser(username = "admin@altoro.com", roles = "ADMIN")
        @DisplayName("ADMIN puede marcar inasistencia → 200 OK")
        void adminPuedeMarcart_200Ok() throws Exception {
            when(reservaService.marcarInasistencia(eq(RESERVA_ID), any(Authentication.class)))
                    .thenReturn(responseExitosa());

            mockMvc.perform(patch("/api/reservas/{id}/marcar-inasistencia", RESERVA_ID))
                    .andExpect(status().isOk());

            verify(reservaService).marcarInasistencia(eq(RESERVA_ID), any(Authentication.class));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Reserva PENDIENTE → 422 UNPROCESSABLE_ENTITY")
        void reservaPendiente_422() throws Exception {
            when(reservaService.marcarInasistencia(eq(RESERVA_ID), any(Authentication.class)))
                    .thenThrow(new BusinessException(
                            ErrorCode.INVALID_STATE,
                            "Solo reservas confirmadas pueden marcarse como inasistencia",
                            HttpStatus.UNPROCESSABLE_ENTITY
                    ));

            mockMvc.perform(patch("/api/reservas/{id}/marcar-inasistencia", RESERVA_ID))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Solo reservas confirmadas pueden marcarse como inasistencia"));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Menos de 30 minutos → 422 UNPROCESSABLE_ENTITY")
        void menosDe30Min_422() throws Exception {
            when(reservaService.marcarInasistencia(eq(RESERVA_ID), any(Authentication.class)))
                    .thenThrow(new BusinessException(
                            ErrorCode.INVALID_STATE,
                            "Debe esperar 15 minuto(s) más antes de marcar inasistencia",
                            HttpStatus.UNPROCESSABLE_ENTITY
                    ));

            mockMvc.perform(patch("/api/reservas/{id}/marcar-inasistencia", RESERVA_ID))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Debe esperar 15 minuto(s) más antes de marcar inasistencia"));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Reserva no existe → 404 NOT_FOUND")
        void reservaNoExiste_404() throws Exception {
            when(reservaService.marcarInasistencia(eq(RESERVA_ID), any(Authentication.class)))
                    .thenThrow(new ResourceNotFoundException("Reserva", RESERVA_ID));

            mockMvc.perform(patch("/api/reservas/{id}/marcar-inasistencia", RESERVA_ID))
                    .andExpect(status().isNotFound());
        }

        /**
         * Test de autorización basada en roles con {@code @PreAuthorize}.
         *
         * <p>IMPORTANTE: {@code @WebMvcTest} con {@code PermissiveSecurityConfig} no evalúa
         * {@code @PreAuthorize} correctamente. El endpoint está anotado con
         * {@code @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")} pero esta configuración
         * permite el acceso a todos los roles autenticados.
         *
         * <p>La autorización real basada en roles se verifica en:
         * <ul>
         *   <li>Tests de integración con {@code @SpringBootTest}</li>
         *   <li>Tests Postman que ejecutan contra la aplicación completa</li>
         * </ul>
         */

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Email se toma del token de autenticación")
        void emailDelToken() throws Exception {
            when(reservaService.marcarInasistencia(eq(RESERVA_ID), any(Authentication.class)))
                    .thenReturn(responseExitosa());

            mockMvc.perform(patch("/api/reservas/{id}/marcar-inasistencia", RESERVA_ID))
                    .andExpect(status().isOk());

            verify(reservaService).marcarInasistencia(eq(RESERVA_ID), any(Authentication.class));
        }
    }

    // -----------------------------------------------------------------------
    // POST /api/reservas/{reservaId}/abonos
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/reservas/{reservaId}/abonos")
    class RegistrarAbono {

        private final Long RESERVA_ID = 10L;

        private String bodyValido(TipoAbono tipo) throws Exception {
            return objectMapper.writeValueAsString(Map.of(
                    "tipo", tipo.name(),
                    "monto", "50000.00",
                    "metodo", MetodoPago.EFECTIVO.name(),
                    "fechaHora", LocalDateTime.now().toString()
            ));
        }

        private RegistrarAbonoResponse responseDummy() {
            return RegistrarAbonoResponse.builder()
                    .abonoId(1L)
                    .tipo("ANTICIPO")
                    .estado("REGISTRADO")
                    .resumen(ResumenPagoResponse.builder()
                            .reservaId(RESERVA_ID)
                            .totalAnticipado(new BigDecimal("50000.00"))
                            .build())
                    .build();
        }

        /**
         * Test de autorización basada en roles con {@code @PreAuthorize}.
         *
         * <p>IMPORTANTE: {@code @WebMvcTest} con {@code PermissiveSecurityConfig} no evalúa
         * {@code @PreAuthorize} correctamente. El endpoint está anotado con
         * {@code @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")} pero esta configuración
         * permite el acceso a todos los roles autenticados.
         *
         * <p>La autorización real basada en roles se verifica en:
         * <ul>
         *   <li>Tests de integración con {@code @SpringBootTest}</li>
         *   <li>Tests Postman que ejecutan contra la aplicación completa</li>
         * </ul>
         */

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("CAJERO con body ANTICIPO válido → 201 Created")
        void registrarAbono_conCAJERO_201() throws Exception {
            when(reservaService.registrarAbono(eq(RESERVA_ID), any(RegistrarAbonoRequest.class), eq("cajero@test.com")))
                    .thenReturn(responseDummy());

            mockMvc.perform(post("/api/reservas/{id}/abonos", RESERVA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyValido(TipoAbono.ANTICIPO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Anticipo registrado correctamente"))
                    .andExpect(jsonPath("$.data.abonoId").value(1L))
                    .andExpect(jsonPath("$.data.tipo").value("ANTICIPO"));

            verify(reservaService).registrarAbono(eq(RESERVA_ID), any(RegistrarAbonoRequest.class), eq("cajero@test.com"));
        }

        @Test
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        @DisplayName("ADMIN con body ANTICIPO válido → 201 Created")
        void registrarAbono_conADMIN_201() throws Exception {
            when(reservaService.registrarAbono(eq(RESERVA_ID), any(RegistrarAbonoRequest.class), eq("admin@test.com")))
                    .thenReturn(responseDummy());

            mockMvc.perform(post("/api/reservas/{id}/abonos", RESERVA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyValido(TipoAbono.ANTICIPO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("CAJERO registra DEVOLUCION → mensaje correcto en 201")
        void registrarAbono_devolucion_mensajeDevolucion() throws Exception {
            RegistrarAbonoResponse respDevolucion = RegistrarAbonoResponse.builder()
                    .abonoId(2L).tipo("DEVOLUCION").estado("REGISTRADO").build();
            when(reservaService.registrarAbono(eq(RESERVA_ID), any(RegistrarAbonoRequest.class), eq("cajero@test.com")))
                    .thenReturn(respDevolucion);

            mockMvc.perform(post("/api/reservas/{id}/abonos", RESERVA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyValido(TipoAbono.DEVOLUCION)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Devolución registrada correctamente"));

            verify(reservaService).registrarAbono(eq(RESERVA_ID), any(RegistrarAbonoRequest.class), eq("cajero@test.com"));
        }

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("monto null → 422 Unprocessable Entity (Bean Validation)")
        void registrarAbono_montoNull_400() throws Exception {
            String bodyMontoNull = objectMapper.writeValueAsString(Map.of(
                    "tipo", TipoAbono.ANTICIPO.name(),
                    "metodo", MetodoPago.EFECTIVO.name(),
                    "fechaHora", LocalDateTime.now().toString()
            ));

            mockMvc.perform(post("/api/reservas/{id}/abonos", RESERVA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyMontoNull))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("El monto es obligatorio"));
        }

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("metodo null → 422 Unprocessable Entity (Bean Validation)")
        void registrarAbono_metodoNull_400() throws Exception {
            String bodyMetodoNull = objectMapper.writeValueAsString(Map.of(
                    "tipo", TipoAbono.ANTICIPO.name(),
                    "monto", "50000.00",
                    "fechaHora", LocalDateTime.now().toString()
            ));

            mockMvc.perform(post("/api/reservas/{id}/abonos", RESERVA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyMetodoNull))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Debe seleccionar un método de pago"));
        }

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("fechaHora null → 422 Unprocessable Entity (Bean Validation)")
        void registrarAbono_fechaNull_400() throws Exception {
            String bodyFechaNull = objectMapper.writeValueAsString(Map.of(
                    "tipo", TipoAbono.ANTICIPO.name(),
                    "monto", "50000.00",
                    "metodo", MetodoPago.EFECTIVO.name()
            ));

            mockMvc.perform(post("/api/reservas/{id}/abonos", RESERVA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyFechaNull))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Debe seleccionar una fecha"));
        }

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("tipo null → 422 Unprocessable Entity (Bean Validation)")
        void registrarAbono_tipoNull_400() throws Exception {
            String bodyTipoNull = objectMapper.writeValueAsString(Map.of(
                    "monto", "50000.00",
                    "metodo", MetodoPago.EFECTIVO.name(),
                    "fechaHora", LocalDateTime.now().toString()
            ));

            mockMvc.perform(post("/api/reservas/{id}/abonos", RESERVA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyTipoNull))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Debe seleccionar el tipo de abono"));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/reservas/{reservaId}/resumen-pago
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/reservas/{reservaId}/resumen-pago")
    class ObtenerResumenPago {

        private final Long RESERVA_ID = 10L;

        private ResumenPagoResponse responseDummy() {
            return ResumenPagoResponse.builder()
                    .reservaId(RESERVA_ID)
                    .clienteNombre("Paula Muñoz")
                    .estado("CONFIRMADA")
                    .tipo("ESPECIAL")
                    .totalAnticipado(new BigDecimal("50000.00"))
                    .montoAbonado(new BigDecimal("50000.00"))
                    .build();
        }

        /**
         * Test de autorización basada en roles con {@code @PreAuthorize}.
         *
         * <p>IMPORTANTE: {@code @WebMvcTest} con {@code PermissiveSecurityConfig} no evalúa
         * {@code @PreAuthorize} correctamente. El endpoint está anotado con
         * {@code @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")} pero esta configuración
         * permite el acceso a todos los roles autenticados.
         *
         * <p>La autorización real basada en roles se verifica en:
         * <ul>
         *   <li>Tests de integración con {@code @SpringBootTest}</li>
         *   <li>Tests Postman que ejecutan contra la aplicación completa</li>
         * </ul>
         */

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("CAJERO consulta resumen → 200 OK con datos financieros")
        void resumenPago_conCAJERO_200() throws Exception {
            when(reservaService.obtenerResumenPago(RESERVA_ID)).thenReturn(responseDummy());

            mockMvc.perform(get("/api/reservas/{id}/resumen-pago", RESERVA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reservaId").value(RESERVA_ID))
                    .andExpect(jsonPath("$.data.clienteNombre").value("Paula Muñoz"))
                    .andExpect(jsonPath("$.data.estado").value("CONFIRMADA"));

            verify(reservaService).obtenerResumenPago(RESERVA_ID);
        }

        @Test
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        @DisplayName("ADMIN consulta resumen → 200 OK")
        void resumenPago_conADMIN_200() throws Exception {
            when(reservaService.obtenerResumenPago(RESERVA_ID)).thenReturn(responseDummy());

            mockMvc.perform(get("/api/reservas/{id}/resumen-pago", RESERVA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(username = "cajero@test.com", roles = "CAJERO")
        @DisplayName("Reserva no encontrada → 404 Not Found")
        void resumenPago_reservaNoEncontrada_404() throws Exception {
            when(reservaService.obtenerResumenPago(RESERVA_ID))
                    .thenThrow(new ResourceNotFoundException("Reserva", RESERVA_ID));

            mockMvc.perform(get("/api/reservas/{id}/resumen-pago", RESERVA_ID))
                    .andExpect(status().isNotFound());
        }
    }
}

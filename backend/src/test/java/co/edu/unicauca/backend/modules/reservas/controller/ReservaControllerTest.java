package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.service.ReservaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
        @DisplayName("CA-01: BASICA sin abono → 200 OK, requiereWhatsApp=false")
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
        @DisplayName("CA-02: BASICA con abono → 200 OK, requiereWhatsApp=true con mensaje")
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
        @DisplayName("CA-04: ESPECIAL después 16h → 200 OK, requiereWhatsApp=false")
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
    }
}

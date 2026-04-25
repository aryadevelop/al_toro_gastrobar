package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaEstadoService;
import co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaService;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionService;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VisitaController.class)
@Import(VisitaControllerTest.PermissiveSecurityConfig.class)
class VisitaControllerTest {

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

    @MockitoBean VisitaService visitaService;
    @MockitoBean VisitaEstadoService visitaEstadoService;
    @MockitoBean NotificacionService notificacionService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    @Nested
    @DisplayName("GET /api/visitas/cliente/historial")
    class ObtenerHistorial {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Propietario → 200 con lista")
        void propietario_retorna200() throws Exception {
            when(visitaService.obtenerHistorialVisitas(anyString()))
                    .thenReturn(List.of(VisitaResumenResponse.builder().build()));

            mockMvc.perform(get("/api/visitas/cliente/historial")
                            .param("emailCliente", "cliente@altoro.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Email ajeno → 403 Forbidden")
        void emailAjeno_retorna403() throws Exception {
            mockMvc.perform(get("/api/visitas/cliente/historial")
                            .param("emailCliente", "otro@altoro.com"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin@altoro.com", roles = "ADMIN")
        @DisplayName("ADM accede a historial de cualquier cliente → 200")
        void admin_retorna200() throws Exception {
            when(visitaService.obtenerHistorialVisitas(anyString()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/visitas/cliente/historial")
                            .param("emailCliente", "cliente@altoro.com"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/visitas/cliente/{visitaId}/detalle")
    class ObtenerDetalle {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Visita existente CLIENTE propietario → 200")
        void propietario_retorna200() throws Exception {
            when(visitaService.obtenerDetalleVisita(anyLong(), any()))
                    .thenReturn(VisitaDetalleResponse.builder().build());

            mockMvc.perform(get("/api/visitas/cliente/1/detalle"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "cajero@altoro.com", roles = "CAJERO")
        @DisplayName("CAJERO accede a visita → 200 sin restricción de ownership")
        void cajero_retorna200() throws Exception {
            when(visitaService.obtenerDetalleVisita(anyLong(), any()))
                    .thenReturn(VisitaDetalleResponse.builder().build());

            mockMvc.perform(get("/api/visitas/cliente/1/detalle"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/visitas/activa")
    class ObtenerEstadoVisitaActiva {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("CLIENTE consulta su visita activa → 200 con ítems")
        void cliente_retorna200() throws Exception {
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

            when(visitaEstadoService.obtenerEstadoVisitaActiva("cliente@altoro.com")).thenReturn(res);

            mockMvc.perform(get("/api/visitas/activa"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.visitaId").value(10))
                    .andExpect(jsonPath("$.data.items[0].estadoItem").value("En preparación"))
                    .andExpect(jsonPath("$.data.total").value(18000));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("MESERO con emailCliente válido → 200")
        void mesero_conEmail_retorna200() throws Exception {
            EstadoVisitaResponse res = EstadoVisitaResponse.builder()
                    .visitaId(10L).visitaCerrada(false)
                    .items(List.of()).total(BigDecimal.ZERO).asistenciaSolicitada(false)
                    .build();

            when(visitaEstadoService.obtenerEstadoVisitaActiva("cliente@altoro.com")).thenReturn(res);

            mockMvc.perform(get("/api/visitas/activa").param("emailCliente", "cliente@altoro.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.visitaId").value(10));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("MESERO sin emailCliente → 400 Bad Request")
        void mesero_sinEmail_retorna400() throws Exception {
            mockMvc.perform(get("/api/visitas/activa"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Cliente sin visita activa → 404 Not Found")
        void sinVisitaActiva_retorna404() throws Exception {
            when(visitaEstadoService.obtenerEstadoVisitaActiva("cliente@altoro.com"))
                    .thenThrow(new BusinessException(ErrorCode.BUSINESS_ERROR,
                            "No tienes una visita activa.", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/visitas/activa"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/visitas/{visitaId}/asistencia")
    class SolicitarAsistencia {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("CLIENTE solicita asistencia → 201 Created")
        void cliente_retorna201() throws Exception {
            NotificacionAsistenciaResponse res = NotificacionAsistenciaResponse.builder()
                    .notificacionId(50L).estado("ACTIVA").build();

            when(notificacionService.solicitarAsistencia(eq(10L), any())).thenReturn(res);

            mockMvc.perform(post("/api/visitas/10/asistencia").with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.notificacionId").value(50));
        }

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Solicitud activa duplicada → 409 Conflict")
        void duplicada_retorna409() throws Exception {
            when(notificacionService.solicitarAsistencia(eq(10L), any()))
                    .thenThrow(new BusinessException(ErrorCode.INVALID_STATE,
                            "Ya existe solicitud activa.", HttpStatus.CONFLICT));

            mockMvc.perform(post("/api/visitas/10/asistencia").with(csrf()))
                    .andExpect(status().isConflict());
        }
    }
}

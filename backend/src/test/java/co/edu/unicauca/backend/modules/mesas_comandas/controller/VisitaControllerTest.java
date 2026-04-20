package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}

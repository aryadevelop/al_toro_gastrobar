package co.edu.unicauca.backend.modules.usuarios.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.usuarios.dto.response.ClientePuntosResponse;
import co.edu.unicauca.backend.modules.usuarios.service.PuntosService;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClienteController.class)
@Import(ClienteControllerTest.PermissiveSecurityConfig.class)
class ClienteControllerTest {

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

    @MockitoBean PuntosService puntosService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    private ClientePuntosResponse puntosStub() {
        return ClientePuntosResponse.builder().puntosActuales(5).puntosAcumulados(20).build();
    }

    @Nested
    @DisplayName("GET /api/clientes/me/puntos")
    class ObtenerMisPuntos {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Propietario → 200 con puntos")
        void propietario_retorna200() throws Exception {
            when(puntosService.obtenerPuntos(anyString())).thenReturn(puntosStub());

            mockMvc.perform(get("/api/clientes/me/puntos")
                            .param("emailCliente", "cliente@altoro.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.puntosActuales").value(5));
        }

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Email ajeno → 403 Forbidden")
        void emailAjeno_retorna403() throws Exception {
            mockMvc.perform(get("/api/clientes/me/puntos")
                            .param("emailCliente", "otro@altoro.com"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/clientes/{clienteId}/puntos")
    class ObtenerPuntosCliente {

        @Test
        @WithMockUser(username = "cajero@altoro.com", roles = "CAJERO")
        @DisplayName("CAJERO → 200 con puntos")
        void cajero_retorna200() throws Exception {
            when(puntosService.obtenerPuntosPorId(anyLong())).thenReturn(puntosStub());

            mockMvc.perform(get("/api/clientes/1/puntos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.puntosActuales").value(5));
        }
    }

    @Nested
    @DisplayName("POST /api/clientes/{clienteId}/canje-puntos")
    class CanjearPuntos {

        @Test
        @WithMockUser(username = "cajero@altoro.com", roles = "CAJERO")
        @DisplayName("CAJERO canjea puntos → 200 con saldo actualizado")
        void cajero_retorna200() throws Exception {
            when(puntosService.canjearPuntos(anyLong(), anyString()))
                    .thenReturn(ClientePuntosResponse.builder().puntosActuales(0).puntosAcumulados(20).build());

            mockMvc.perform(post("/api/clientes/1/canje-puntos")
                            .param("emailEmpleado", "cajero@altoro.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.puntosActuales").value(0));
        }
    }
}

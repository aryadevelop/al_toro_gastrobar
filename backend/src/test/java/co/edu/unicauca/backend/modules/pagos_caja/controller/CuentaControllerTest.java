package co.edu.unicauca.backend.modules.pagos_caja.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.pagos_caja.dto.response.CuentaPreliminarResponse;
import co.edu.unicauca.backend.modules.pagos_caja.service.CuentaService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CuentaController.class)
@Import(CuentaControllerTest.PermissiveSecurityConfig.class)
class CuentaControllerTest {

    static class PermissiveSecurityConfig {
        @Bean
        @Order(1)
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http.securityMatcher("/**").csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll()).build();
        }
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean CuentaService cuentaService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    @Nested
    @DisplayName("GET /api/ventas/{id}/cuenta")
    class ObtenerCuenta {

        @Test
        @WithMockUser(username = "cajero@altoro.com", roles = "CAJERO")
        @DisplayName("CAJERO con visita válida → 200 OK")
        void cajero_visitaValida_retorna200() throws Exception {
            when(cuentaService.obtenerCuenta(5L))
                    .thenReturn(CuentaPreliminarResponse.builder().visitaId(5L).build());

            mockMvc.perform(get("/api/ventas/5/cuenta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.visitaId").value(5));
            verify(cuentaService).obtenerCuenta(5L);
        }
    }
}

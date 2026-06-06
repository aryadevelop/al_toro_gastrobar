package co.edu.unicauca.backend.modules.reportes.ventas_detalle.controller;

import co.edu.unicauca.backend.modules.auth.security.JwtAuthenticationFilter;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.VentaListadoResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.service.VentaDetalleAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VentaDetalleAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(VentaDetalleAdminControllerTest.PermissiveSecurityConfig.class)
class VentaDetalleAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VentaDetalleAdminService detalleService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

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

    @Test
    void listarVentas_noEncontradas_retornaMensaje() throws Exception {
        when(detalleService.listarVentas(any(), any(), any(), any()))
                .thenReturn(VentaListadoResponse.builder()
                        .ventas(List.of())
                        .totalPeriodo(BigDecimal.ZERO)
                        .build());

        mockMvc.perform(get("/api/ventas")
                        .param("desdeFecha", "2026-06-01")
                        .param("hastaFecha", "2026-06-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("No se encontraron ventas con los filtros seleccionados. Intenta ampliar el rango de búsqueda."))
                .andExpect(jsonPath("$.data.totalPeriodo").value(0));
    }
}

package co.edu.unicauca.backend.modules.reportes.clientes.controller;

import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.ClienteListadoResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.service.ClienteVentasAdminService;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClienteVentasAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ClienteVentasAdminControllerTest.PermissiveSecurityConfig.class)
class ClienteVentasAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteVentasAdminService ventasService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private SesionRepository sesionRepository;

    @TestConfiguration
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
    void listarClientes_sinFiltros_retornaListaCompleta() throws Exception {
        ClienteListadoResponse cliente = ClienteListadoResponse.builder()
                .clienteId(100L)
                .nombre("Ana Rivera")
                .correoElectronico("ana.rr@altoro.com")
                .telefono("3001234567")
                .totalVisitas(5L)
                .totalGastado(java.math.BigDecimal.valueOf(150000))
                .puntosAcumulados(120)
                .estado("Activo")
                .clienteFrecuente(false)
                .build();

        when(ventasService.listarClientes(null, null, null, null, null, null, null, null, null))
                .thenReturn(List.of(cliente));

        mockMvc.perform(get("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].clienteId").value(100))
                .andExpect(jsonPath("$.data[0].nombre").value("Ana Rivera"))
                .andExpect(jsonPath("$.data[0].estado").value("Activo"));
    }

    @Test
    void listarClientes_conFiltros_retornaClienteFiltrado() throws Exception {
        ClienteListadoResponse cliente = ClienteListadoResponse.builder()
                .clienteId(101L)
                .nombre("Carolina Gómez")
                .correoElectronico("carolina@altoro.com")
                .telefono("3009876543")
                .totalVisitas(12L)
                .totalGastado(java.math.BigDecimal.valueOf(320000))
                .puntosAcumulados(220)
                .estado("Activo")
                .clienteFrecuente(true)
                .build();

        when(ventasService.listarClientes(10, null, null, null, "ACTIVO", "Carolina", null, null, null))
                .thenReturn(List.of(cliente));

        mockMvc.perform(get("/api/clientes")
                        .param("minVisitas", "10")
                        .param("estado", "ACTIVO")
                        .param("nombre", "Carolina")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].clienteId").value(101))
                .andExpect(jsonPath("$.data[0].clienteFrecuente").value(true));
    }

    @Test
    void listarClientes_conReservasRecientes_retornaClienteFiltrado() throws Exception {
        ClienteListadoResponse cliente = ClienteListadoResponse.builder()
                .clienteId(102L)
                .nombre("Diego Ramírez")
                .correoElectronico("diego@altoro.com")
                .telefono("3005556666")
                .totalVisitas(3L)
                .totalGastado(java.math.BigDecimal.valueOf(90000))
                .puntosAcumulados(45)
                .estado("Activo")
                .clienteFrecuente(false)
                .build();

        when(ventasService.listarClientes(null, null, null, null, null, null, null, null, 2))
                .thenReturn(List.of(cliente));

        mockMvc.perform(get("/api/clientes")
                        .param("reservasUltimosMeses", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].clienteId").value(102))
                .andExpect(jsonPath("$.data[0].nombre").value("Diego Ramírez"));
    }

    @Test
    void listarClientes_hastaRegistroMayorHoy_retornaError() throws Exception {
        mockMvc.perform(get("/api/clientes")
                        .param("hastaRegistro", LocalDate.now().plusDays(1).toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL-001"))
                .andExpect(jsonPath("$.message").value("La fecha 'hastaRegistro' no puede ser mayor al día de hoy."));
    }

    @Test
    void listarClientes_desdeMayorHasta_retornaError() throws Exception {
        mockMvc.perform(get("/api/clientes")
                        .param("desdeRegistro", "2026-05-10")
                        .param("hastaRegistro", "2026-05-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL-001"))
                .andExpect(jsonPath("$.message").value("La fecha 'desdeRegistro' no puede ser mayor a 'hastaRegistro'."));
    }

    @Test
    void listarClientes_fechaMalFormateada_retornaError() throws Exception {
        mockMvc.perform(get("/api/clientes")
                        .param("desdeRegistro", "01-05-2026")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL-001"))
                .andExpect(jsonPath("$.message").value("Fecha inválida para 'desdeRegistro'. Use el formato YYYY-MM-DD."));
    }
}

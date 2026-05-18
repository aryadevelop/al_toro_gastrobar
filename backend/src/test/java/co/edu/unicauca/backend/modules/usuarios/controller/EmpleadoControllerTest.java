package co.edu.unicauca.backend.modules.usuarios.controller;

import co.edu.unicauca.backend.modules.usuarios.dto.response.EmpleadoListadoResponse;
import co.edu.unicauca.backend.modules.usuarios.dto.response.EmpleadoResponse;
import co.edu.unicauca.backend.modules.usuarios.service.EmpleadoService;
import org.junit.jupiter.api.Test;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmpleadoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(EmpleadoControllerTest.PermissiveSecurityConfig.class)
class EmpleadoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private EmpleadoService empleadoService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private SesionRepository sesionRepository;

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
    void crearEmpleado_retorna201() throws Exception {
        EmpleadoResponse response = EmpleadoResponse.builder()
                .empleadoId(10L)
                .nombre("Ana Rivera")
                .correoElectronico("ana@altoro.com")
                .telefono("3001234567")
                .direccion("Calle 15")
                .fechaIngreso(LocalDate.now())
                .roles(List.of("CAJERO"))
                .warning(null)
                .build();

        when(empleadoService.crearEmpleado(any())).thenReturn(response);

        String payload = "{\n" +
                "  \"nombre\": \"Ana Rivera\",\n" +
                "  \"correoElectronico\": \"ana@altoro.com\",\n" +
                "  \"telefono\": \"3001234567\",\n" +
                "  \"direccion\": \"Calle 15\",\n" +
                "  \"roles\": [\"CAJERO\"],\n" +
                "  \"fechaIngreso\": \"" + LocalDate.now() + "\",\n" +
                "  \"password\": \"Password1!\",\n" +
                "  \"passwordConfirmacion\": \"Password1!\"\n" +
                "}";

        mockMvc.perform(post("/api/empleados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nombre").value("Ana Rivera"));
    }

    @Test
    void listarEmpleados_retornaLista() throws Exception {
        EmpleadoListadoResponse item = EmpleadoListadoResponse.builder()
                .empleadoId(10L)
                .nombre("Ana Rivera")
                .correoElectronico("ana@altoro.com")
                .telefono("3001234567")
                .direccion("Calle 15")
                .fechaIngreso(LocalDate.now())
                .roles(List.of("MESERO"))
                .estado("Activo")
                .build();

        when(empleadoService.listarEmpleados("MESERO", "ACTIVO", "Ana"))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/empleados")
                        .param("rol", "MESERO")
                        .param("estado", "ACTIVO")
                        .param("nombre", "Ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nombre").value("Ana Rivera"));
    }

    @Test
    void listarEmpleados_sinFiltros_retornaListaCompleta() throws Exception {
        EmpleadoListadoResponse item = EmpleadoListadoResponse.builder()
                .empleadoId(11L)
                .nombre("Carlos Pérez")
                .correoElectronico("carlos@altoro.com")
                .telefono("3004444444")
                .direccion("Calle 44")
                .fechaIngreso(LocalDate.now())
                .roles(List.of("CAJERO"))
                .estado("Activo")
                .build();

        when(empleadoService.listarEmpleados(null, null, null)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/empleados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].correoElectronico").value("carlos@altoro.com"));
    }
}

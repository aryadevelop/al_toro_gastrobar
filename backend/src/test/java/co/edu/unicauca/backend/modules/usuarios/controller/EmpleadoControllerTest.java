package co.edu.unicauca.backend.modules.usuarios.controller;

import co.edu.unicauca.backend.modules.usuarios.dto.response.EmpleadoResponse;
import co.edu.unicauca.backend.modules.usuarios.service.EmpleadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmpleadoController.class)
@Import(EmpleadoControllerTest.PermissiveSecurityConfig.class)
class EmpleadoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private EmpleadoService empleadoService;

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
}

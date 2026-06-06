package co.edu.unicauca.backend.modules.inventario.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.inventario.dto.response.AjusteInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ItemAjusteInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.MovimientoInventarioHistorialResponse;
import co.edu.unicauca.backend.modules.inventario.service.MovimientoInventarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MovimientoInventarioController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@DisplayName("MovimientoInventarioController")
class MovimientoInventarioControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean MovimientoInventarioService movimientoService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    @Nested
    @DisplayName("GET /api/inventario/buscar")
    class Buscar {

        @Test
        @DisplayName("con coincidencias → 200 con lista")
        void conCoincidencias_retorna200() throws Exception {
            ItemAjusteInventarioResponse item = ItemAjusteInventarioResponse.builder()
                    .tipo("INSUMO").id(1L).nombre("Sal").stockActual(new BigDecimal("2"))
                    .unidadMedida("KG").build();
            when(movimientoService.buscarItemsAjuste("sa")).thenReturn(List.of(item));

            mockMvc.perform(get("/api/inventario/buscar").param("q", "sa")
                            .with(user("cocinero@altoro.com").roles("COCINERO")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].tipo").value("INSUMO"));
        }

        @Test
        @DisplayName("sin coincidencias → 200 con lista vacía")
        void sinCoincidencias_retorna200ListaVacia() throws Exception {
            when(movimientoService.buscarItemsAjuste(anyString())).thenReturn(List.of());

            mockMvc.perform(get("/api/inventario/buscar").param("q", "zzz")
                            .with(user("cocinero@altoro.com").roles("COCINERO")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/inventario/movimientos")
    class RegistrarAjuste {

        @Test
        @DisplayName("body válido → 201 con mensaje de éxito")
        void bodyValido_retorna201() throws Exception {
            AjusteInventarioResponse resp = AjusteInventarioResponse.builder()
                    .movimientoId(99L).stockActualizado(new BigDecimal("7")).comandasNotificadas(0).build();
            when(movimientoService.registrarAjuste(any(), any())).thenReturn(resp);

            mockMvc.perform(post("/api/inventario/movimientos")
                            .contentType("application/json")
                            .content("{\"productoId\":1,\"cantidad\":3.000,\"tipo\":\"EGRESO\"}")
                            .with(user("cocinero@altoro.com").roles("COCINERO")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Ajuste de inventario registrado correctamente."))
                    .andExpect(jsonPath("$.data.movimientoId").value(99))
                    .andExpect(jsonPath("$.data.comandasNotificadas").value(0));
        }

        @Test
        @DisplayName("body sin cantidad → 422 por validación")
        void bodySinCantidad_retorna422() throws Exception {
            mockMvc.perform(post("/api/inventario/movimientos")
                            .contentType("application/json")
                            .content("{\"productoId\":1,\"tipo\":\"EGRESO\"}")
                            .with(user("cocinero@altoro.com").roles("COCINERO")))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("POST /api/inventario/movimientos/registro")
    class RegistrarMovimiento {

        @Test
        @DisplayName("body válido con fecha → 201")
        void bodyValidoConFecha_retorna201() throws Exception {
            AjusteInventarioResponse resp = AjusteInventarioResponse.builder()
                    .movimientoId(101L).stockActualizado(new BigDecimal("5")).comandasNotificadas(0).build();
            when(movimientoService.registrarMovimiento(any(), any())).thenReturn(resp);

            mockMvc.perform(post("/api/inventario/movimientos/registro")
                            .contentType("application/json")
                            .content("{\"productoId\":1,\"cantidad\":3.000,\"tipo\":\"INGRESO\",\"fecha\":\"2026-06-05T10:30:00\",\"observaciones\":\"Ingreso prueba\"}")
                            .with(user("cocinero@altoro.com").roles("COCINERO")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Movimiento de inventario registrado correctamente."))
                    .andExpect(jsonPath("$.data.movimientoId").value(101));
        }
    }

    @Nested
    @DisplayName("GET /api/inventario/movimientos")
    class ListarHistorico {

        @Test
        @DisplayName("retorna historial de movimientos")
        void retornaHistorialDeMovimientos() throws Exception {
            MovimientoInventarioHistorialResponse item = MovimientoInventarioHistorialResponse.builder()
                    .movimientoId(55L)
                    .tipo("INGRESO")
                    .cantidad(new BigDecimal("3"))
                    .movimientoFechaHora(LocalDateTime.of(2026, 6, 5, 10, 0))
                    .observaciones("Ingreso test")
                    .productoId(1L)
                    .build();
            when(movimientoService.listarMovimientosHistorico()).thenReturn(List.of(item));

            mockMvc.perform(get("/api/inventario/movimientos")
                            .with(user("cocinero@altoro.com").roles("COCINERO")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].movimientoId").value(55));
        }
    }
}

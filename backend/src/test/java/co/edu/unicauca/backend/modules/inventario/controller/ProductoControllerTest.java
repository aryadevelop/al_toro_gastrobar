package co.edu.unicauca.backend.modules.inventario.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.inventario.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.inventario.service.ProductoService;

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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProductoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class ProductoControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ProductoService productoService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    @Nested
    @DisplayName("GET /api/productos/carta")
    class ObtenerCarta {

        @Test
        @DisplayName("Con productos → 200 con lista de categorías")
        void conProductos_retorna200ConLista() throws Exception {
            CategoriaCartaResponse cat = new CategoriaCartaResponse();
            when(productoService.obtenerCarta()).thenReturn(List.of(cat));

            mockMvc.perform(get("/api/productos/carta")
                            .with(user("cliente@altoro.com").roles("CLIENTE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        @DisplayName("Sin productos → 200 con lista vacía")
        void sinProductos_retorna200ListaVacia() throws Exception {
            when(productoService.obtenerCarta()).thenReturn(List.of());

            mockMvc.perform(get("/api/productos/carta")
                            .with(user("cliente@altoro.com").roles("CLIENTE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/productos/menu-especial")
    class ObtenerMenuEspecial {

        @Test
        @DisplayName("Con menús → 200 con lista")
        void conMenus_retorna200() throws Exception {
            when(productoService.obtenerMenusEspeciales()).thenReturn(List.of(new MenuEspecialResponse()));

            mockMvc.perform(get("/api/productos/menu-especial")
                            .with(user("cliente@altoro.com").roles("CLIENTE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Sin menús → 200 con lista vacía")
        void sinMenus_retorna200ListaVacia() throws Exception {
            when(productoService.obtenerMenusEspeciales()).thenReturn(List.of());

            mockMvc.perform(get("/api/productos/menu-especial")
                            .with(user("cliente@altoro.com").roles("CLIENTE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }
}

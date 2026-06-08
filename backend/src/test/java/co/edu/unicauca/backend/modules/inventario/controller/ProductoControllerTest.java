package co.edu.unicauca.backend.modules.inventario.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.inventario.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoBusquedaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.service.EstadoInventarioService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
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
    @MockitoBean EstadoInventarioService estadoInventarioService;
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

    @Nested
    @DisplayName("GET /api/productos")
    class ListarProductosInventario {

        @Test
        @DisplayName("Sin filtros retorna 200 con lista de productos")
        void sinFiltros_retorna200ConLista() throws Exception {
            ProductoInventarioResponse r = ProductoInventarioResponse.builder()
                    .productoId(1L)
                    .productoNombre("Empanada")
                    .categoriaNombre("Entradas")
                    .productoPrecio(BigDecimal.valueOf(12000))
                    .stockActual(BigDecimal.valueOf(5))
                    .productoEstado("ACTIVO")
                    .build();
            when(productoService.listarProductosInventario(null, null)).thenReturn(List.of(r));

            mockMvc.perform(get("/api/productos")
                            .with(user("admin@altoro.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].categoriaNombre").value("Entradas"));
        }

        @Test
        @DisplayName("Filtra por categoria y nombre")
        void filtraCategoriaYNombre_retorna200() throws Exception {
            ProductoInventarioResponse r = ProductoInventarioResponse.builder()
                    .productoId(2L)
                    .productoNombre("Sopa")
                    .categoriaNombre("Entradas")
                    .productoPrecio(BigDecimal.valueOf(8500))
                    .stockActual(BigDecimal.valueOf(0))
                    .productoEstado("INACTIVO")
                    .build();
            when(productoService.listarProductosInventario("Entradas", "sop")).thenReturn(List.of(r));

            mockMvc.perform(get("/api/productos")
                            .param("categoria", "Entradas")
                            .param("q", "sop")
                            .with(user("admin@altoro.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].productoNombre").value("Sopa"))
                    .andExpect(jsonPath("$.data[0].productoEstado").value("INACTIVO"));
        }
    }

    @Nested
    @DisplayName("GET /api/productos/buscar")
    class BuscarProductos {

        @Test
        @DisplayName("Con coincidencias → 200 con lista")
        void conCoincidencias_retorna200ConLista() throws Exception {
            ProductoBusquedaResponse r = ProductoBusquedaResponse.builder()
                    .productoId(1L)
                    .productoNombre("Empanada")
                    .productoPrecio(BigDecimal.valueOf(5000))
                    .stockActual(BigDecimal.valueOf(5))
                    .productoEstado("ACTIVO")
                    .productoCategoria("PLATO")
                    .build();
            when(productoService.buscarProductos("emp")).thenReturn(List.of(r));

            mockMvc.perform(get("/api/productos/buscar")
                            .param("q", "emp")
                            .with(user("mesero@altoro.com").roles("MESERO")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].productoNombre").value("Empanada"))
                    .andExpect(jsonPath("$.data[0].stockActual").value(5))
                    .andExpect(jsonPath("$.data[0].productoEstado").value("ACTIVO"));
        }

        @Test
        @DisplayName("Sin coincidencias → 200 con lista vacía")
        void sinCoincidencias_retorna200ListaVacia() throws Exception {
            when(productoService.buscarProductos(anyString())).thenReturn(List.of());

            mockMvc.perform(get("/api/productos/buscar")
                            .param("q", "xyz")
                            .with(user("mesero@altoro.com").roles("MESERO")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }
}

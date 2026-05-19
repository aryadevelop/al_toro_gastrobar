package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import co.edu.unicauca.backend.shared.enums.UnidadMedida;
import co.edu.unicauca.backend.modules.inventario.entity.CategoriaCarta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ajustedb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS restaurante",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@DisplayName("AjusteInventarioRepositoryTest")
class AjusteInventarioRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ProductoRepository productoRepository;
    @Autowired InsumoRepository insumoRepository;
    @Autowired RecetaRepository recetaRepository;

    private CategoriaCarta categoria() {
        return em.persistAndFlush(CategoriaCarta.builder()
                .categoriaNombre("Cat" + System.nanoTime())
                .build());
    }

    private Producto producto(String nombre, EstadoGenerico estado, Boolean menuEspecial, BigDecimal stock) {
        return em.persistAndFlush(Producto.builder()
                .categoriaCarta(categoria())
                .productoNombre(nombre)
                .productoEstado(estado)
                .productoPrecio(new BigDecimal("10000"))
                .productoTipo(TipoProducto.PREPARACION)
                .productoCategoria(CategoriaProducto.PLATO)
                .menuEspecial(menuEspecial)
                .stockActual(stock)
                .build());
    }

    private Insumo insumo(String nombre, EstadoGenerico estado) {
        return em.persistAndFlush(Insumo.builder()
                .insumoNombre(nombre)
                .insumoEstado(estado)
                .insumoUnidad(UnidadMedida.KG)
                .insumoStockActual(new BigDecimal("10"))
                .build());
    }

    // ── buscarParaAjuste ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("buscarParaAjuste")
    class BuscarParaAjuste {

        @Test
        @DisplayName("devuelve producto activo coincidente y excluye inactivo")
        void activoCoincide_inactivoExcluido() {
            producto("Lomo al trapo", EstadoGenerico.ACTIVO, false, new BigDecimal("5"));
            producto("Lomo fino", EstadoGenerico.INACTIVO, false, null);

            List<Producto> result = productoRepository.buscarParaAjuste("lomo", EstadoGenerico.ACTIVO);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductoNombre()).isEqualTo("Lomo al trapo");
        }

        @Test
        @DisplayName("excluye productos con menuEspecial=true")
        void menuEspecial_excluido() {
            producto("Plato especial", EstadoGenerico.ACTIVO, true, new BigDecimal("3"));
            producto("Plato normal", EstadoGenerico.ACTIVO, false, new BigDecimal("3"));

            List<Producto> result = productoRepository.buscarParaAjuste("plato", EstadoGenerico.ACTIVO);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductoNombre()).isEqualTo("Plato normal");
        }

        @Test
        @DisplayName("incluye producto activo con stockActual null")
        void stockNull_incluido() {
            producto("Lomo sin stock", EstadoGenerico.ACTIVO, false, null);

            List<Producto> result = productoRepository.buscarParaAjuste("lomo", EstadoGenerico.ACTIVO);

            assertThat(result).hasSize(1);
        }
    }

    // ── buscarPorNombreActivo ────────────────────────────────────────────────

    @Nested
    @DisplayName("buscarPorNombreActivo")
    class BuscarPorNombreActivo {

        @Test
        @DisplayName("devuelve activos coincidentes y excluye inactivo")
        void activosCoincidentes_inactivoExcluido() {
            insumo("Sal marina", EstadoGenerico.ACTIVO);
            insumo("Salsa de tomate", EstadoGenerico.ACTIVO);
            insumo("Sal gruesa", EstadoGenerico.INACTIVO);

            List<Insumo> result = insumoRepository.buscarPorNombreActivo("sal", EstadoGenerico.ACTIVO);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Insumo::getInsumoNombre)
                    .containsExactlyInAnyOrder("Sal marina", "Salsa de tomate");
        }
    }

    // ── findProductoIdsByInsumoId ────────────────────────────────────────────

    @Nested
    @DisplayName("findProductoIdsByInsumoId")
    class FindProductoIdsByInsumoId {

        @Test
        @DisplayName("devuelve los productoId de las recetas del insumo")
        void devuelveProductoIds() {
            Insumo ins = insumo("Carne", EstadoGenerico.ACTIVO);
            Producto p1 = producto("Hamburguesa", EstadoGenerico.ACTIVO, false, null);
            Producto p2 = producto("Albóndigas", EstadoGenerico.ACTIVO, false, null);

            em.persistAndFlush(Receta.builder()
                    .insumoId(ins.getInsumoId()).productoId(p1.getProductoId())
                    .insumo(ins).producto(p1).recetaCantidad(new BigDecimal("0.200")).build());
            em.persistAndFlush(Receta.builder()
                    .insumoId(ins.getInsumoId()).productoId(p2.getProductoId())
                    .insumo(ins).producto(p2).recetaCantidad(new BigDecimal("0.150")).build());

            List<Long> ids = recetaRepository.findProductoIdsByInsumoId(ins.getInsumoId());

            assertThat(ids).containsExactlyInAnyOrder(p1.getProductoId(), p2.getProductoId());
        }

        @Test
        @DisplayName("insumo sin recetas devuelve lista vacía")
        void sinRecetas_listaVacia() {
            Insumo ins = insumo("Aceite", EstadoGenerico.ACTIVO);

            List<Long> ids = recetaRepository.findProductoIdsByInsumoId(ins.getInsumoId());

            assertThat(ids).isEmpty();
        }
    }
}

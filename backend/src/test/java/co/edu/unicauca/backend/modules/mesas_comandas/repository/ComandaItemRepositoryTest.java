package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.inventario.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoInsumo;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import co.edu.unicauca.backend.shared.enums.UnidadMedida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:comandaitemdb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS restaurante",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("ComandaItemRepository")
class ComandaItemRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ComandaItemRepository repo;

    private Comanda comandaBorrador;
    private Producto productoPlato;
    private Producto productoBebida;

    @BeforeEach
    void setUp() {
        Usuario usuario = em.persistAndFlush(Usuario.builder()
                .usuarioEmail("ci" + System.nanoTime() + "@altoro.com")
                .usuarioPassword("pass")
                .build());

        Cliente cliente = em.persistAndFlush(Cliente.builder()
                .usuario(usuario)
                .clienteNombre("Cliente Test")
                .clienteTelefono("3001112233")
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build());

        Visita visita = em.persistAndFlush(Visita.builder()
                .cliente(cliente)
                .visitaFechaHoraInicio(LocalDateTime.now())
                .build());

        comandaBorrador = em.persistAndFlush(Comanda.builder()
                .visita(visita)
                .comandaEstado(EstadoComanda.BORRADOR)
                .comandaEstacion(EstacionComanda.COCINA)
                .build());

        CategoriaCarta cat = em.persistAndFlush(CategoriaCarta.builder()
                .categoriaNombre("Cat" + System.nanoTime())
                .build());

        productoPlato = em.persistAndFlush(Producto.builder()
                .categoriaCarta(cat)
                .productoNombre("Pasta")
                .productoEstado(EstadoGenerico.ACTIVO)
                .productoPrecio(new BigDecimal("12000"))
                .productoTipo(TipoProducto.VENTA_DIRECTA)
                .productoCategoria(CategoriaProducto.PLATO)
                .build());

        productoBebida = em.persistAndFlush(Producto.builder()
                .categoriaCarta(cat)
                .productoNombre("Agua")
                .productoEstado(EstadoGenerico.ACTIVO)
                .productoPrecio(new BigDecimal("3000"))
                .productoTipo(TipoProducto.VENTA_DIRECTA)
                .productoCategoria(CategoriaProducto.BEBIDA)
                .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // sumCantidadComprometidaByProducto
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sumCantidadComprometidaByProducto")
    class SumCantidadComprometida {

        @Test
        @DisplayName("sin items comprometidos → devuelve 0")
        void sinItems_devuelveCero() {
            Long total = repo.sumCantidadComprometidaByProducto(productoPlato.getProductoId());
            assertThat(total).isZero();
        }

        @Test
        @DisplayName("item en BORRADOR sin menuGrupo → suma la cantidad")
        void itemBorradorSinMenuGrupo_sumaLaCantidad() {
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaBorrador)
                    .producto(productoPlato)
                    .comandaItemCantidad(3)
                    .comandaItemPrecio(productoPlato.getProductoPrecio())
                    .build());

            Long total = repo.sumCantidadComprometidaByProducto(productoPlato.getProductoId());
            assertThat(total).isEqualTo(3L);
        }

        @Test
        @DisplayName("item con menuGrupo → no se cuenta (menú especial)")
        void itemConMenuGrupo_noSeContabiliza() {
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaBorrador)
                    .producto(productoPlato)
                    .comandaItemCantidad(5)
                    .comandaItemPrecio(productoPlato.getProductoPrecio())
                    .comandaItemMenuGrupo("uuid-grupo-menu")
                    .build());

            Long total = repo.sumCantidadComprometidaByProducto(productoPlato.getProductoId());
            assertThat(total).isZero();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // findByComanda_ComandaIdOrderByProductoNombreAsc
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findByComanda_ComandaIdOrderByProductoNombreAsc")
    class FindOrdenado {

        @Test
        @DisplayName("items en comanda → retorna ordenados alfabéticamente por nombre")
        void itemsEnComanda_ordenaAlfabeticamente() {
            // productoPlato = "Pasta", productoBebida = "Agua" → Agua antes que Pasta
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaBorrador)
                    .producto(productoPlato)
                    .comandaItemCantidad(1)
                    .comandaItemPrecio(productoPlato.getProductoPrecio())
                    .build());
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaBorrador)
                    .producto(productoBebida)
                    .comandaItemCantidad(2)
                    .comandaItemPrecio(productoBebida.getProductoPrecio())
                    .build());

            List<ComandaItem> items = repo.findByComanda_ComandaIdOrderByProductoNombreAsc(
                    comandaBorrador.getComandaId());

            assertThat(items).hasSize(2);
            assertThat(items.get(0).getProducto().getProductoNombre()).isEqualTo("Agua");
            assertThat(items.get(1).getProducto().getProductoNombre()).isEqualTo("Pasta");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // sumTotalActivosByVisita
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sumTotalActivosByVisita")
    class SumTotalActivos {

        @Test
        @DisplayName("items con precio → suma precio × cantidad")
        void itemsConPrecio_sumaCorrectamente() {
            // 1 × 12000 + 2 × 3000 = 18000
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaBorrador)
                    .producto(productoPlato)
                    .comandaItemCantidad(1)
                    .comandaItemPrecio(new BigDecimal("12000"))
                    .build());
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaBorrador)
                    .producto(productoBebida)
                    .comandaItemCantidad(2)
                    .comandaItemPrecio(new BigDecimal("3000"))
                    .build());

            BigDecimal total = repo.sumTotalActivosByVisita(
                    comandaBorrador.getVisita().getVisitaId());

            assertThat(total).isEqualByComparingTo(new BigDecimal("18000"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // sumCantidadByComandaIdIn
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sumCantidadByComandaIdIn")
    class SumCantidadByComandaIdIn {

        @Test
        @DisplayName("agrupa cantidades por comanda para los identificadores indicados")
        void agrupa_cantidades_por_comanda() {
            Comanda comandaA = em.persistAndFlush(Comanda.builder()
                    .visita(comandaBorrador.getVisita())
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build());
            Comanda comandaB = em.persistAndFlush(Comanda.builder()
                    .visita(comandaBorrador.getVisita())
                    .comandaEstacion(EstacionComanda.BARRA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build());

            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaA).producto(productoPlato)
                    .comandaItemCantidad(2)
                    .comandaItemPrecio(new BigDecimal("5000"))
                    .build());
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaA).producto(productoBebida)
                    .comandaItemCantidad(3)
                    .comandaItemPrecio(new BigDecimal("4000"))
                    .build());
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaB).producto(productoBebida)
                    .comandaItemCantidad(7)
                    .comandaItemPrecio(new BigDecimal("4000"))
                    .build());

            java.util.List<Object[]> resultado = repo.sumCantidadByComandaIdIn(
                    java.util.Set.of(comandaA.getComandaId(), comandaB.getComandaId()));

            java.util.Map<Long, Integer> mapa = new java.util.HashMap<>();
            for (Object[] fila : resultado) {
                mapa.put(((Number) fila[0]).longValue(), ((Number) fila[1]).intValue());
            }

            assertThat(mapa).containsEntry(comandaA.getComandaId(), 5);
            assertThat(mapa).containsEntry(comandaB.getComandaId(), 7);
        }

        @Test
        @DisplayName("comandas sin ítems quedan fuera del resultado")
        void comandas_sin_items_se_omiten() {
            Comanda vacia = em.persistAndFlush(Comanda.builder()
                    .visita(comandaBorrador.getVisita())
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build());

            java.util.List<Object[]> resultado = repo.sumCantidadByComandaIdIn(
                    java.util.Set.of(vacia.getComandaId()));

            assertThat(resultado).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // sumCantidadInsumoComprometida
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sumCantidadInsumoComprometida")
    class SumCantidadInsumoComprometida {

        private Insumo insumo;
        private Producto productoPreparacion;
        private Producto productoVentaDirecta;

        @BeforeEach
        void setUpInsumoReceta() {
            insumo = em.persistAndFlush(Insumo.builder()
                    .insumoNombre("Carne" + System.nanoTime())
                    .insumoUnidad(UnidadMedida.KG)
                    .insumoEstado(EstadoGenerico.ACTIVO)
                    .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                    .insumoStockActual(new BigDecimal("10.000"))
                    .build());

            CategoriaCarta cat = em.persistAndFlush(CategoriaCarta.builder()
                    .categoriaNombre("CatReceta" + System.nanoTime())
                    .build());

            productoPreparacion = em.persistAndFlush(Producto.builder()
                    .categoriaCarta(cat)
                    .productoNombre("Lomo" + System.nanoTime())
                    .productoEstado(EstadoGenerico.ACTIVO)
                    .productoPrecio(new BigDecimal("25000"))
                    .productoTipo(TipoProducto.PREPARACION)
                    .productoCategoria(CategoriaProducto.PLATO)
                    .build());

            productoVentaDirecta = em.persistAndFlush(Producto.builder()
                    .categoriaCarta(cat)
                    .productoNombre("Gaseosa" + System.nanoTime())
                    .productoEstado(EstadoGenerico.ACTIVO)
                    .productoPrecio(new BigDecimal("5000"))
                    .productoTipo(TipoProducto.VENTA_DIRECTA)
                    .productoCategoria(CategoriaProducto.BEBIDA)
                    .build());

            // Receta: 1 unidad de Lomo requiere 0.250 kg de Carne
            em.persistAndFlush(Receta.builder()
                    .insumoId(insumo.getInsumoId())
                    .productoId(productoPreparacion.getProductoId())
                    .insumo(insumo)
                    .producto(productoPreparacion)
                    .recetaCantidad(new BigDecimal("0.250"))
                    .build());
        }

        @Test
        @DisplayName("sin comandas → retorna cero")
        void sinComandas_retornaCero() {
            BigDecimal result = repo.sumCantidadInsumoComprometida(insumo.getInsumoId());
            assertThat(result).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("ítem en BORRADOR con cantidad 4 y recetaCantidad 0.250 → suma 1.000")
        void itemEnBorrador_sumaCantidad() {
            // comanda BORRADOR ya existe en setUp() como comandaBorrador
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaBorrador)
                    .producto(productoPreparacion)
                    .comandaItemCantidad(4)
                    .comandaItemPrecio(productoPreparacion.getProductoPrecio())
                    .build());

            BigDecimal result = repo.sumCantidadInsumoComprometida(insumo.getInsumoId());
            assertThat(result).isEqualByComparingTo("1.000");
        }

        @Test
        @DisplayName("ítem en PENDIENTE → contribuye a la suma")
        void itemEnPendiente_suma() {
            Comanda comandaPendiente = em.persistAndFlush(Comanda.builder()
                    .visita(comandaBorrador.getVisita())
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build());

            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaPendiente)
                    .producto(productoPreparacion)
                    .comandaItemCantidad(2)
                    .comandaItemPrecio(productoPreparacion.getProductoPrecio())
                    .build());

            // 2 × 0.250 = 0.500
            BigDecimal result = repo.sumCantidadInsumoComprometida(insumo.getInsumoId());
            assertThat(result).isEqualByComparingTo("0.500");
        }

        @Test
        @DisplayName("ítem en COMPLETADO → se excluye de la suma")
        void itemEnCompletado_seExcluye() {
            Comanda comandaCompletada = em.persistAndFlush(Comanda.builder()
                    .visita(comandaBorrador.getVisita())
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.COMPLETADO)
                    .build());

            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaCompletada)
                    .producto(productoPreparacion)
                    .comandaItemCantidad(3)
                    .comandaItemPrecio(productoPreparacion.getProductoPrecio())
                    .build());

            BigDecimal result = repo.sumCantidadInsumoComprometida(insumo.getInsumoId());
            assertThat(result).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("ítem con comandaItemMenuGrupo no nulo → se excluye (menú especial)")
        void itemConMenuGrupo_seExcluye() {
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaBorrador)
                    .producto(productoPreparacion)
                    .comandaItemCantidad(2)
                    .comandaItemPrecio(productoPreparacion.getProductoPrecio())
                    .comandaItemMenuGrupo("uuid-menu-especial")
                    .build());

            BigDecimal result = repo.sumCantidadInsumoComprometida(insumo.getInsumoId());
            assertThat(result).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("producto VENTA_DIRECTA con el mismo insumo → se excluye")
        void productoVentaDirecta_seExcluye() {
            // Receta para el producto de VENTA_DIRECTA con el mismo insumo
            em.persistAndFlush(Receta.builder()
                    .insumoId(insumo.getInsumoId())
                    .productoId(productoVentaDirecta.getProductoId())
                    .insumo(insumo)
                    .producto(productoVentaDirecta)
                    .recetaCantidad(new BigDecimal("0.100"))
                    .build());

            em.persistAndFlush(ComandaItem.builder()
                    .comanda(comandaBorrador)
                    .producto(productoVentaDirecta)
                    .comandaItemCantidad(5)
                    .comandaItemPrecio(productoVentaDirecta.getProductoPrecio())
                    .build());

            BigDecimal result = repo.sumCantidadInsumoComprometida(insumo.getInsumoId());
            assertThat(result).isEqualByComparingTo("0");
        }
    }
}

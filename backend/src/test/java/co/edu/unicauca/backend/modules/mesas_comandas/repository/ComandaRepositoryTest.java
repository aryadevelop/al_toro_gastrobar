package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.inventario.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
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
        "spring.datasource.url=jdbc:h2:mem:comandadb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS restaurante",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("ComandaRepository")
class ComandaRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ComandaRepository repo;

    private Visita visita;
    private Producto producto;

    @BeforeEach
    void setUp() {
        Usuario usuario = em.persistAndFlush(Usuario.builder()
                .usuarioEmail("cr" + System.nanoTime() + "@altoro.com")
                .usuarioPassword("pass")
                .build());

        Cliente cliente = em.persistAndFlush(Cliente.builder()
                .usuario(usuario)
                .clienteNombre("Cliente Comanda")
                .clienteTelefono("3009998877")
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build());

        visita = em.persistAndFlush(Visita.builder()
                .cliente(cliente)
                .visitaFechaHoraInicio(LocalDateTime.now())
                .build());

        CategoriaCarta cat = em.persistAndFlush(CategoriaCarta.builder()
                .categoriaNombre("Platos" + System.nanoTime())
                .build());

        producto = em.persistAndFlush(Producto.builder()
                .categoriaCarta(cat)
                .productoNombre("Bandeja Paisa")
                .productoEstado(EstadoGenerico.ACTIVO)
                .productoPrecio(new BigDecimal("18000"))
                .productoTipo(TipoProducto.VENTA_DIRECTA)
                .productoCategoria(CategoriaProducto.PLATO)
                .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // findItemsEnProduccionByVisita
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findItemsEnProduccionByVisita")
    class FindItemsEnProduccion {

        @Test
        @DisplayName("comanda PENDIENTE → sus items aparecen en producción")
        void comandaPendiente_aparece() {
            Comanda pendiente = em.persistAndFlush(Comanda.builder()
                    .visita(visita)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .build());
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(pendiente)
                    .producto(producto)
                    .comandaItemCantidad(1)
                    .comandaItemPrecio(producto.getProductoPrecio())
                    .build());

            List<ComandaItem> items = repo.findItemsEnProduccionByVisita(visita.getVisitaId());

            assertThat(items).hasSize(1);
            assertThat(items.get(0).getProducto().getProductoNombre()).isEqualTo("Bandeja Paisa");
        }

        @Test
        @DisplayName("comanda BORRADOR → sus items NO aparecen en producción")
        void comandaBorrador_noAparece() {
            Comanda borrador = em.persistAndFlush(Comanda.builder()
                    .visita(visita)
                    .comandaEstado(EstadoComanda.BORRADOR)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .build());
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(borrador)
                    .producto(producto)
                    .comandaItemCantidad(2)
                    .comandaItemPrecio(producto.getProductoPrecio())
                    .build());

            List<ComandaItem> items = repo.findItemsEnProduccionByVisita(visita.getVisitaId());

            assertThat(items).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // findAllItemsActivosByVisita
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findAllItemsActivosByVisita")
    class FindAllItemsActivos {

        @Test
        @DisplayName("comandas BORRADOR y PENDIENTE → ambos items aparecen")
        void borradorYPendiente_ambosSonActivos() {
            Comanda borrador = em.persistAndFlush(Comanda.builder()
                    .visita(visita)
                    .comandaEstado(EstadoComanda.BORRADOR)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .build());
            Comanda pendiente = em.persistAndFlush(Comanda.builder()
                    .visita(visita)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .comandaEstacion(EstacionComanda.BARRA)
                    .build());
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(borrador)
                    .producto(producto)
                    .comandaItemCantidad(1)
                    .comandaItemPrecio(producto.getProductoPrecio())
                    .build());
            em.persistAndFlush(ComandaItem.builder()
                    .comanda(pendiente)
                    .producto(producto)
                    .comandaItemCantidad(1)
                    .comandaItemPrecio(producto.getProductoPrecio())
                    .build());

            List<ComandaItem> items = repo.findAllItemsActivosByVisita(visita.getVisitaId());

            assertThat(items).hasSize(2);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion")
    class FindByVisitaEstadoEstacion {

        @Test
        @DisplayName("filtra por visita + estado + estación correctamente")
        void filtroExacto_encuentraLaComandaCorrecta() {
            em.persistAndFlush(Comanda.builder()
                    .visita(visita)
                    .comandaEstado(EstadoComanda.BORRADOR)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .build());
            em.persistAndFlush(Comanda.builder()
                    .visita(visita)
                    .comandaEstado(EstadoComanda.BORRADOR)
                    .comandaEstacion(EstacionComanda.BARRA)
                    .build());

            var resultado = repo.findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
                    visita.getVisitaId(), EstadoComanda.BORRADOR, EstacionComanda.COCINA);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getComandaEstacion()).isEqualTo(EstacionComanda.COCINA);
        }
    }
}

package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ComandaProduccionMapper")
class ComandaProduccionMapperTest {

    private ComandaProduccionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ComandaProduccionMapper();
    }

    private Comanda comanda() {
        Comanda c = Comanda.builder()
                .comandaId(10L)
                .comandaEstacion(EstacionComanda.COCINA)
                .comandaEstado(EstadoComanda.PENDIENTE)
                .comandaFechaHoraInicio(LocalDateTime.of(2026, 5, 12, 18, 0))
                .comandaFechaHoraListo(null)
                .comandaNotas("Sin cebolla")
                .build();
        c.setCreatedAt(LocalDateTime.of(2026, 5, 12, 17, 55));
        return c;
    }

    private Mesa mesa() {
        return Mesa.builder()
                .visitaId(7L)
                .mesaIdentificador("T-03")
                .mesero(Empleado.builder().empleadoNombre("Ana").build())
                .build();
    }

    private Producto producto(Long id, String nombre, CategoriaProducto cat) {
        return Producto.builder()
                .productoId(id)
                .productoNombre(nombre)
                .productoCategoria(cat)
                .build();
    }

    private ComandaItem item(Long id, Producto p, int cantidad, String descripcion) {
        return ComandaItem.builder()
                .comandaItemId(id)
                .producto(p)
                .comandaItemCantidad(cantidad)
                .comandaItemDescripcion(descripcion)
                .modificaciones(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("toResumen mapea campos básicos y resuelve mesa+mesero")
    void toResumen_camposBasicos() {
        ComandaProduccionResumenResponse r = mapper.toResumen(comanda(), mesa(), 5);

        assertThat(r.getComandaId()).isEqualTo(10L);
        assertThat(r.getEstacion()).isEqualTo("COCINA");
        assertThat(r.getComandaEstado()).isEqualTo("PENDIENTE");
        assertThat(r.getMesaIdentificador()).isEqualTo("T-03");
        assertThat(r.getMeseroNombre()).isEqualTo("Ana");
        assertThat(r.getTotalItems()).isEqualTo(5);
        assertThat(r.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 12, 17, 55));
        assertThat(r.getFechaHoraInicio()).isEqualTo(LocalDateTime.of(2026, 5, 12, 18, 0));
        assertThat(r.getFechaHoraListo()).isNull();
        assertThat(r.getNotas()).isEqualTo("Sin cebolla");
    }

    @Test
    @DisplayName("toResumen tolera mesa nula")
    void toResumen_mesaNula() {
        ComandaProduccionResumenResponse r = mapper.toResumen(comanda(), null, 0);
        assertThat(r.getMesaIdentificador()).isNull();
        assertThat(r.getMeseroNombre()).isNull();
    }

    @Test
    @DisplayName("toDetalle agrupa ítems por categoría y los ordena alfabéticamente")
    void toDetalle_agrupaYOrdena() {
        Producto plato1 = producto(1L, "Bandeja", CategoriaProducto.PLATO);
        Producto plato2 = producto(2L, "Arepa", CategoriaProducto.PLATO);
        Producto bebida = producto(3L, "Limonada", CategoriaProducto.BEBIDA);

        List<ComandaItem> items = List.of(
                item(100L, plato1, 1, null),
                item(101L, plato2, 2, null),
                item(102L, bebida, 1, "Sin azúcar"));

        ComandaProduccionDetalleResponse d = mapper.toDetalle(comanda(), mesa(), items);

        assertThat(d.getPlatos()).extracting("productoNombre")
                .containsExactly("Arepa", "Bandeja");
        assertThat(d.getBebidas()).extracting("productoNombre")
                .containsExactly("Limonada");
        assertThat(d.getBebidas().get(0).getDescripcion()).isEqualTo("Sin azúcar");
    }

    @Test
    @DisplayName("toDetalle: ítem con modificaciones expone los nombres de opciones ordenados")
    void toDetalle_modificaciones() {
        Producto plato = producto(1L, "Pasta", CategoriaProducto.PLATO);
        ComandaItem itemConMods = item(100L, plato, 1, null);
        itemConMods.getModificaciones().add(ComandaMenuModificacion.builder()
                .opcion(OpcionModificacion.builder().opcionNombre("Pesto").build())
                .build());
        itemConMods.getModificaciones().add(ComandaMenuModificacion.builder()
                .opcion(OpcionModificacion.builder().opcionNombre("Doble queso").build())
                .build());

        ComandaProduccionDetalleResponse d = mapper.toDetalle(comanda(), mesa(), List.of(itemConMods));

        assertThat(d.getPlatos()).hasSize(1);
        assertThat(d.getPlatos().get(0).getModificacionesMenu())
                .containsExactly("Doble queso", "Pesto");
    }

    @Test
    @DisplayName("toDetalle: ítem con lista de modificaciones vacía expone lista vacía")
    void toDetalle_modificacionesListaVacia() {
        Producto plato = producto(1L, "Pasta", CategoriaProducto.PLATO);
        ComandaProduccionDetalleResponse d = mapper.toDetalle(
                comanda(), mesa(), List.of(item(100L, plato, 1, null)));

        assertThat(d.getPlatos().get(0).getModificacionesMenu()).isEmpty();
    }

    @Test
    @DisplayName("toDetalle: ítem con modificaciones null expone lista vacía")
    void toDetalle_modificacionesNull() {
        Producto plato = producto(1L, "Pasta", CategoriaProducto.PLATO);
        ComandaItem item = ComandaItem.builder()
                .comandaItemId(100L)
                .producto(plato)
                .comandaItemCantidad(1)
                .modificaciones(null)
                .build();

        ComandaProduccionDetalleResponse d = mapper.toDetalle(comanda(), mesa(), List.of(item));

        assertThat(d.getPlatos().get(0).getModificacionesMenu()).isEmpty();
    }

    @Test
    @DisplayName("toDetalle: mesa nula → mesaIdentificador y meseroNombre quedan en null")
    void toDetalle_mesaNula() {
        ComandaProduccionDetalleResponse d = mapper.toDetalle(comanda(), null, List.of());

        assertThat(d.getMesaIdentificador()).isNull();
        assertThat(d.getMeseroNombre()).isNull();
    }

    @Test
    @DisplayName("toDetalle: comanda sin ítems devuelve las tres listas vacías")
    void toDetalle_sinItems() {
        ComandaProduccionDetalleResponse d = mapper.toDetalle(comanda(), mesa(), List.of());
        assertThat(d.getPlatos()).isEmpty();
        assertThat(d.getBebidas()).isEmpty();
    }
}

package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.BorradorComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemBorradorResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.TipoComponenteMenu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link ComandaBorradorMapper}.
 * Cobertura objetivo: 100% líneas / ≥95% ramas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComandaBorradorMapper")
class ComandaBorradorMapperTest {

    @Mock
    private ComandaItemRepository comandaItemRepository;

    @InjectMocks
    private ComandaBorradorMapper mapper;

    // -------------------------------------------------------------------------
    // toBorradorResponse
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("toBorradorResponse")
    class ToBorradorResponse {

        @Test
        @DisplayName("sinBorradores_CocinaYBarraNull_RetornaListasVaciasYFlagsFalse")
        void toBorradorResponse_SinBorradores_RetornaListasVaciasYFlagsFalse() {
            // Arrange
            Mesa mesa = mesa(10L, "T-01");
            BigDecimal totalAcumulado = BigDecimal.valueOf(50);

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(), totalAcumulado);

            // Assert
            assertThat(resp.getVisitaId()).isEqualTo(10L);
            assertThat(resp.getMesaIdentificador()).isEqualTo("T-01");
            assertThat(resp.getComandaCocinaId()).isNull();
            assertThat(resp.getComandaBarraId()).isNull();
            assertThat(resp.getPlatos()).isEmpty();
            assertThat(resp.getBebidas()).isEmpty();
            assertThat(resp.getSubTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(resp.getTotal()).isEqualByComparingTo(totalAcumulado);
            assertThat(resp.getNotasCocina()).isNull();
            assertThat(resp.getNotasBarra()).isNull();
            assertThat(resp.getPuedeEnviarCocina()).isFalse();
            assertThat(resp.getPuedeEnviarBarra()).isFalse();
        }

        @Test
        @DisplayName("totalAcumuladoNull_UsaBigDecimalZero")
        void toBorradorResponse_TotalAcumuladoNull_UsaBigDecimalZero() {
            // Arrange
            Mesa mesa = mesa(1L, "T-02");

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(), null);

            // Assert
            assertThat(resp.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("soloCocina_PlatosNoVacios_PuedeEnviarCocinaTrue")
        void toBorradorResponse_SoloCocina_PuedeEnviarCocinaTrue() {
            // Arrange
            Mesa mesa = mesa(1L, "T-03");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, "Notas cocina");
            ComandaItem plato = item(1L, "Bandeja Paisa", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(25_000), 2, null, null, List.of());

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(plato));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(cocina), BigDecimal.ZERO);

            // Assert
            assertThat(resp.getComandaCocinaId()).isEqualTo(100L);
            assertThat(resp.getComandaBarraId()).isNull();
            assertThat(resp.getPlatos()).hasSize(1);
            assertThat(resp.getBebidas()).isEmpty();
            assertThat(resp.getPuedeEnviarCocina()).isTrue();
            assertThat(resp.getPuedeEnviarBarra()).isFalse();
            assertThat(resp.getNotasCocina()).isEqualTo("Notas cocina");
            assertThat(resp.getNotasBarra()).isNull();
        }

        @Test
        @DisplayName("soloBarra_BebidasNoVacias_PuedeEnviarBarraTrue")
        void toBorradorResponse_SoloBarra_PuedeEnviarBarraTrue() {
            // Arrange
            Mesa mesa = mesa(1L, "T-04");
            Comanda barra = comanda(200L, EstacionComanda.BARRA, "Notas barra");
            ComandaItem bebida = item(2L, "Limonada", CategoriaProducto.BEBIDA,
                    BigDecimal.valueOf(8_000), 1, null, null, List.of());

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(200L))
                    .thenReturn(List.of(bebida));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(barra), BigDecimal.ZERO);

            // Assert
            assertThat(resp.getComandaCocinaId()).isNull();
            assertThat(resp.getComandaBarraId()).isEqualTo(200L);
            assertThat(resp.getPlatos()).isEmpty();
            assertThat(resp.getBebidas()).hasSize(1);
            assertThat(resp.getPuedeEnviarCocina()).isFalse();
            assertThat(resp.getPuedeEnviarBarra()).isTrue();
            assertThat(resp.getNotasCocina()).isNull();
            assertThat(resp.getNotasBarra()).isEqualTo("Notas barra");
        }

        @Test
        @DisplayName("ambosEstaciones_SubtotalSumaAmbasEstaciones")
        void toBorradorResponse_AmbosEstaciones_SubtotalCorrecto() {
            // Arrange
            Mesa mesa = mesa(1L, "T-05");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, null);
            Comanda barra  = comanda(200L, EstacionComanda.BARRA, null);

            // plato: 25000 * 2 = 50000
            ComandaItem plato = item(1L, "Bandeja Paisa", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(25_000), 2, null, null, List.of());
            // bebida: 8000 * 3 = 24000
            ComandaItem bebida = item(2L, "Limonada", CategoriaProducto.BEBIDA,
                    BigDecimal.valueOf(8_000), 3, null, null, List.of());

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(plato));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(200L))
                    .thenReturn(List.of(bebida));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(
                    mesa, List.of(cocina, barra), BigDecimal.valueOf(100_000));

            // Assert — subtotal = 50000 + 24000 = 74000
            assertThat(resp.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(74_000));
            assertThat(resp.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
            assertThat(resp.getPuedeEnviarCocina()).isTrue();
            assertThat(resp.getPuedeEnviarBarra()).isTrue();
        }

        @Test
        @DisplayName("itemConPrecioNull_NoContribuyeAlSubtotal")
        void toBorradorResponse_ItemPrecioNull_SubtotalIgnoraItem() {
            // Arrange
            Mesa mesa = mesa(1L, "T-06");
            Comanda barra = comanda(200L, EstacionComanda.BARRA, null);

            // bebida del menú: precio null (costo absorbido por el plato del menú)
            ComandaItem bebidaMenu = item(3L, "Agua", CategoriaProducto.BEBIDA,
                    null, 1, "grupo-abc", null, List.of());

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(200L))
                    .thenReturn(List.of(bebidaMenu));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(barra), BigDecimal.ZERO);

            // Assert — subtotal = 0 porque precio es null
            assertThat(resp.getSubTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("itemMenuGrupo_BebidaEmbebidaEnPlato")
        void toBorradorResponse_ItemConMenuGrupo_BebidaEmbebidaEnPlato() {
            // Arrange
            Mesa mesa = mesa(1L, "T-07");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, null);
            Comanda barra  = comanda(200L, EstacionComanda.BARRA, null);

            String grupo = "grupo-uuid-1";
            ComandaItem plato = item(1L, "Bandeja Menú", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(35_000), 1, grupo, null, List.of());
            ComandaItem bebida = item(2L, "Jugo Natural", CategoriaProducto.BEBIDA,
                    null, 1, grupo, null, List.of());

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(plato));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(200L))
                    .thenReturn(List.of(bebida));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(
                    mesa, List.of(cocina, barra), BigDecimal.ZERO);

            // Assert — el plato de cocina debe tener la bebida embebida
            assertThat(resp.getPlatos()).hasSize(1);
            ItemBorradorResponse platoResp = resp.getPlatos().get(0);
            assertThat(platoResp.getMenuGrupo()).isEqualTo(grupo);
            assertThat(platoResp.getBebida()).isNotNull();
            assertThat(platoResp.getBebida().getProductoNombre()).isEqualTo("Jugo Natural");
            assertThat(platoResp.getBebida().getProductoId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("itemMenuGrupo_SinContraparteEnBarra_BebidaNull")
        void toBorradorResponse_ItemConMenuGrupoSinContraparte_BebidaNull() {
            // Arrange — solo cocina, sin barra
            Mesa mesa = mesa(1L, "T-08");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, null);
            String grupo = "grupo-sin-par";
            ComandaItem plato = item(1L, "Bandeja Solo", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(35_000), 1, grupo, null, List.of());

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(plato));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(cocina), BigDecimal.ZERO);

            // Assert — sin contraparte en barra, bebida debe ser null
            assertThat(resp.getPlatos()).hasSize(1);
            assertThat(resp.getPlatos().get(0).getBebida()).isNull();
        }

        @Test
        @DisplayName("itemConModificacionesMenu_ModificacionesMapeadas")
        void toBorradorResponse_ItemConModificaciones_ModificacionesMapeadas() {
            // Arrange
            Mesa mesa = mesa(1L, "T-09");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, null);

            OpcionModificacion opcion = opcion(5L, "Con extra arroz", TipoComponenteMenu.ARROZ);
            ComandaMenuModificacion mod = modificacion(opcion);
            ComandaItem plato = item(1L, "Bandeja", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(30_000), 1, null, null, List.of(mod));

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(plato));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(cocina), BigDecimal.ZERO);

            // Assert
            ItemBorradorResponse platoResp = resp.getPlatos().get(0);
            assertThat(platoResp.getModificacionesMenu()).hasSize(1);
            assertThat(platoResp.getModificacionesMenu().get(0).getOpcionId()).isEqualTo(5L);
            assertThat(platoResp.getModificacionesMenu().get(0).getOpcionNombre()).isEqualTo("Con extra arroz");
            assertThat(platoResp.getModificacionesMenu().get(0).getTipoComponente()).isEqualTo("ARROZ");
        }

        @Test
        @DisplayName("itemSinModificaciones_ModificacionesVacias")
        void toBorradorResponse_ItemSinModificaciones_ModificacionesVacias() {
            // Arrange
            Mesa mesa = mesa(1L, "T-10");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, null);
            ComandaItem plato = item(1L, "Caldo", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(15_000), 1, null, null, List.of());

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(plato));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(cocina), BigDecimal.ZERO);

            // Assert
            assertThat(resp.getPlatos().get(0).getModificacionesMenu()).isEmpty();
        }

        @Test
        @DisplayName("itemConModificacionesNull_ModificacionesVacias")
        void toBorradorResponse_ItemConModificacionesNull_ModificacionesVacias() {
            // Arrange
            Mesa mesa = mesa(1L, "T-11");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, null);
            // Lista de modificaciones null (no @Builder.Default aplicado manualmente)
            ComandaItem plato = item(1L, "Sopa", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(12_000), 1, null, null, null);

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(plato));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(cocina), BigDecimal.ZERO);

            // Assert — mapearModificacionesMenu rama null
            assertThat(resp.getPlatos().get(0).getModificacionesMenu()).isEmpty();
        }

        @Test
        @DisplayName("itemConDescripcion_DescripcionMapeada")
        void toBorradorResponse_ItemConDescripcion_DescripcionMapeada() {
            // Arrange
            Mesa mesa = mesa(1L, "T-12");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, null);
            ComandaItem plato = item(1L, "Arroz", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(10_000), 1, null, "Sin sal", List.of());

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(plato));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(cocina), BigDecimal.ZERO);

            // Assert
            assertThat(resp.getPlatos().get(0).getDescripcion()).isEqualTo("Sin sal");
        }

        @Test
        @DisplayName("variosItemsCocina_OrdenadosAlfabeticamente")
        void toBorradorResponse_VariosItemsCocina_OrdenadosAlfabeticamente() {
            // Arrange
            Mesa mesa = mesa(1L, "T-13");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, null);
            ComandaItem itemZ = item(2L, "Zancocho", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(20_000), 1, null, null, List.of());
            ComandaItem itemA = item(1L, "Arepa", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(5_000), 1, null, null, List.of());

            // El repositorio devuelve ya ordenado por nombre (ORDER BY productoNombre ASC)
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(itemA, itemZ));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(mesa, List.of(cocina), BigDecimal.ZERO);

            // Assert — mapper re-ordena con POR_NOMBRE (case-insensitive), debe respetar A antes de Z
            assertThat(resp.getPlatos()).extracting(ItemBorradorResponse::getProductoNombre)
                    .containsExactly("Arepa", "Zancocho");
        }

        @Test
        @DisplayName("itemConMenuGrupoNullEnContraparte_NoBuscaBebida")
        void toBorradorResponse_ItemSinMenuGrupo_BebidaNull() {
            // Arrange — ítem sin menuGrupo: buscarBebidaDelMenu retorna null por grupo==null
            Mesa mesa = mesa(1L, "T-14");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, null);
            Comanda barra  = comanda(200L, EstacionComanda.BARRA, null);

            ComandaItem plato = item(1L, "Plato Carta", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(20_000), 1, null /*menuGrupo null*/, null, List.of());
            ComandaItem bebida = item(2L, "Cerveza", CategoriaProducto.BEBIDA,
                    BigDecimal.valueOf(5_000), 1, null, null, List.of());

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(plato));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(200L))
                    .thenReturn(List.of(bebida));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(
                    mesa, List.of(cocina, barra), BigDecimal.ZERO);

            // Assert — menuGrupo null → bebida null aunque haya contraparte
            assertThat(resp.getPlatos().get(0).getBebida()).isNull();
        }

        @Test
        @DisplayName("menuGrupoSinCoincidenciaEnContraparte_BebidaNull")
        void toBorradorResponse_MenuGrupoSinCoincidencia_BebidaNull() {
            // Arrange
            Mesa mesa = mesa(1L, "T-15");
            Comanda cocina = comanda(100L, EstacionComanda.COCINA, null);
            Comanda barra  = comanda(200L, EstacionComanda.BARRA, null);

            ComandaItem plato = item(1L, "Bandeja", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(30_000), 1, "grupo-A", null, List.of());
            // La barra tiene un ítem con grupo diferente
            ComandaItem bebida = item(2L, "Jugo", CategoriaProducto.BEBIDA,
                    null, 1, "grupo-B", null, List.of());

            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(100L))
                    .thenReturn(List.of(plato));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(200L))
                    .thenReturn(List.of(bebida));

            // Act
            BorradorComandaResponse resp = mapper.toBorradorResponse(
                    mesa, List.of(cocina, barra), BigDecimal.ZERO);

            // Assert — grupos no coinciden → bebida null
            assertThat(resp.getPlatos().get(0).getBebida()).isNull();
        }

    }

    // -------------------------------------------------------------------------
    // toItemsResponse
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("toItemsResponse")
    class ToItemsResponse {

        @Test
        @DisplayName("listaNull_RetornaListaVacia")
        void toItemsResponse_ListaNull_RetornaListaVacia() {
            assertThat(mapper.toItemsResponse(null)).isEmpty();
        }

        @Test
        @DisplayName("listaVacia_RetornaListaVacia")
        void toItemsResponse_ListaVacia_RetornaListaVacia() {
            assertThat(mapper.toItemsResponse(List.of())).isEmpty();
        }

        @Test
        @DisplayName("unItem_MapeosCampoACampo")
        void toItemsResponse_UnItem_MapeosCampoACampo() {
            // Arrange
            ComandaItem plato = item(7L, "Sobrebarriga", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(28_000), 3, "grupo-xyz", "Extra picante", List.of());
            plato.getProducto().setProductoId(42L);
            plato.getProducto().setStockActual(BigDecimal.valueOf(5));

            // Act
            List<ItemBorradorResponse> result = mapper.toItemsResponse(List.of(plato));

            // Assert — mapearItem con subtotal=ZERO y bebida=null (toItemsResponse no funde menú)
            assertThat(result).hasSize(1);
            ItemBorradorResponse r = result.get(0);
            assertThat(r.getComandaItemId()).isEqualTo(7L);
            assertThat(r.getProductoId()).isEqualTo(42L);
            assertThat(r.getProductoNombre()).isEqualTo("Sobrebarriga");
            assertThat(r.getCategoriaProducto()).isEqualTo("PLATO");
            assertThat(r.getPrecioUnitario()).isEqualByComparingTo(BigDecimal.valueOf(28_000));
            assertThat(r.getCantidad()).isEqualTo(3);
            // subtotal es ZERO porque toItemsResponse lo pasa fijo como BigDecimal.ZERO
            assertThat(r.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(r.getDescripcion()).isEqualTo("Extra picante");
            assertThat(r.getMenuGrupo()).isEqualTo("grupo-xyz");
            assertThat(r.getBebida()).isNull();
        }

        @Test
        @DisplayName("variosItems_OrdenadosAlfabeticamente")
        void toItemsResponse_VariosItems_OrdenadosAlfabeticamente() {
            // Arrange
            ComandaItem itemZ = item(2L, "Zancocho", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(20_000), 1, null, null, List.of());
            ComandaItem itemA = item(1L, "Arepa", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(5_000), 1, null, null, List.of());
            ComandaItem itemM = item(3L, "Mondongo", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(18_000), 1, null, null, List.of());

            // Act
            List<ItemBorradorResponse> result = mapper.toItemsResponse(List.of(itemZ, itemM, itemA));

            // Assert
            assertThat(result).extracting(ItemBorradorResponse::getProductoNombre)
                    .containsExactly("Arepa", "Mondongo", "Zancocho");
        }

        @Test
        @DisplayName("itemConModificaciones_ModificacionesMapeadas")
        void toItemsResponse_ItemConModificaciones_ModificacionesMapeadas() {
            // Arrange
            OpcionModificacion opcion = opcion(10L, "Sin gluten", TipoComponenteMenu.SALSA_PROTEINA_1);
            ComandaMenuModificacion mod = modificacion(opcion);
            ComandaItem plato = item(1L, "Pechuga", CategoriaProducto.PLATO,
                    BigDecimal.valueOf(22_000), 1, null, null, List.of(mod));

            // Act
            List<ItemBorradorResponse> result = mapper.toItemsResponse(List.of(plato));

            // Assert
            assertThat(result.get(0).getModificacionesMenu()).hasSize(1);
            assertThat(result.get(0).getModificacionesMenu().get(0).getTipoComponente())
                    .isEqualTo("SALSA_PROTEINA_1");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers privados estáticos
    // -------------------------------------------------------------------------

    /**
     * Crea una {@link Mesa} mínima con visitaId e identificador dados.
     */
    private static Mesa mesa(Long visitaId, String identificador) {
        Mesa mesa = new Mesa();
        mesa.setVisitaId(visitaId);
        mesa.setMesaIdentificador(identificador);
        return mesa;
    }

    /**
     * Crea una {@link Comanda} con estación, estado BORRADOR y notas dadas.
     */
    private static Comanda comanda(Long id, EstacionComanda estacion, String notas) {
        Comanda c = new Comanda();
        c.setComandaId(id);
        c.setComandaEstacion(estacion);
        c.setComandaEstado(EstadoComanda.BORRADOR);
        c.setComandaNotas(notas);
        return c;
    }

    /**
     * Crea un {@link ComandaItem} con todos sus campos configurados.
     *
     * @param id           identificador del ítem
     * @param nombre       nombre del producto
     * @param categoria    categoría del producto
     * @param precio       precio unitario (null para bebidas de menú)
     * @param cantidad     cantidad pedida
     * @param menuGrupo    UUID del grupo de menú especial; null para carta
     * @param descripcion  texto de modificación libre; null para ítem base
     * @param modificaciones lista de {@link ComandaMenuModificacion}; null se permitirá
     */
    private static ComandaItem item(Long id,
                                    String nombre,
                                    CategoriaProducto categoria,
                                    BigDecimal precio,
                                    int cantidad,
                                    String menuGrupo,
                                    String descripcion,
                                    List<ComandaMenuModificacion> modificaciones) {
        Producto producto = new Producto();
        producto.setProductoId(id);
        producto.setProductoNombre(nombre);
        producto.setProductoCategoria(categoria);

        ComandaItem ci = new ComandaItem();
        ci.setComandaItemId(id);
        ci.setProducto(producto);
        ci.setComandaItemPrecio(precio);
        ci.setComandaItemCantidad(cantidad);
        ci.setComandaItemMenuGrupo(menuGrupo);
        ci.setComandaItemDescripcion(descripcion);
        // Lista de modificaciones — puede ser null para ejercitar la rama null
        if (modificaciones != null) {
            ci.setModificaciones(new ArrayList<>(modificaciones));
        } else {
            ci.setModificaciones(null);
        }
        return ci;
    }

    /**
     * Crea una {@link OpcionModificacion} con los campos básicos.
     */
    private static OpcionModificacion opcion(Long id, String nombre, TipoComponenteMenu tipo) {
        OpcionModificacion op = new OpcionModificacion();
        op.setOpcionId(id);
        op.setOpcionNombre(nombre);
        op.setTipoComponente(tipo);
        return op;
    }

    /**
     * Crea un {@link ComandaMenuModificacion} que apunta a la opción dada.
     */
    private static ComandaMenuModificacion modificacion(OpcionModificacion opcion) {
        ComandaMenuModificacion m = new ComandaMenuModificacion();
        m.setOpcion(opcion);
        return m;
    }
}

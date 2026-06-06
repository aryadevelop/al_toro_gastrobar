package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.inventario.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.*;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.*;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MesaMapper")
class MesaMapperTest {

    private final MesaMapper mapper = new MesaMapper();

    @Test
    @DisplayName("toNotificacionActivaResponse mapea correctamente")
    void toNotificacionActivaResponse_DeberiaMapearCorrectamente() {
        // Arrange
        LocalDateTime fechaHora = LocalDateTime.of(2026, 4, 29, 19, 30);
        Notificacion notificacion = Notificacion.builder()
                .notificacionId(1L)
                .notificacionTipo(TipoNotificacion.ATENCION)
                .notificacionFechaHora(fechaHora)
                .build();

        // Act
        NotificacionActivaResponse response = mapper.toNotificacionActivaResponse(notificacion);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getNotificacionId()).isEqualTo(1L);
        assertThat(response.getTipo()).isEqualTo("ATENCION");  // Enum → String
        assertThat(response.getFechaHora()).isEqualTo(fechaHora);
    }

    @Test
    @DisplayName("toMesaMapaResponse con mesa propia retorna nombreMesero null")
    void toMesaMapaResponse_MesaPropia_NombreMeseroNull() {
        // Arrange
        Mesa mesa = crearMesa("T-01", "mesero1@altoro.com", "Juan Pérez");
        List<Notificacion> notificaciones = List.of();

        // Act
        MesaMapaResponse response = mapper.toMesaMapaResponse(mesa, notificaciones, false, "mesero1@altoro.com", false);

        // Assert
        assertThat(response.getMesaId()).isEqualTo(1L);
        assertThat(response.getVisitaId()).isEqualTo(1L);
        assertThat(response.getNombreMesero()).isNull();  // RN-04: mesa propia
        assertThat(response.getEsMesaPropia()).isTrue();
        assertThat(response.getTieneBorrador()).isFalse();
    }

    @Test
    @DisplayName("toMesaMapaResponse con mesa ajena retorna nombreMesero")
    void toMesaMapaResponse_MesaAjena_NombreMeseroPresente() {
        // Arrange
        Mesa mesa = crearMesa("T-02", "mesero1@altoro.com", "Juan Pérez");
        Notificacion notif = Notificacion.builder()
                .notificacionId(1L)
                .notificacionTipo(TipoNotificacion.PLATOS_LISTOS)
                .notificacionFechaHora(LocalDateTime.now())
                .build();

        // Act
        MesaMapaResponse response = mapper.toMesaMapaResponse(mesa, List.of(notif), true, "mesero2@altoro.com", false);

        // Assert
        assertThat(response.getNombreMesero()).isEqualTo("Juan Pérez");  // RN-04: mesa ajena
        assertThat(response.getEsMesaPropia()).isFalse();
        assertThat(response.getTieneBorrador()).isTrue();
        assertThat(response.getNotificacionesActivas()).hasSize(1);
    }

    @Test
    @DisplayName("toMesaMapaResponse con esCajero=true ignora ownership y siempre llena nombreMesero")
    void toMesaMapaResponse_Cajero_EsMesaPropiaFalseYNombrePoblado() {
        // Arrange
        Mesa mesa = crearMesa("T-03", "mesero1@altoro.com", "Carlos Pérez");

        // Act — pasa email que coincide con el del mesero pero esCajero=true debe ignorarlo
        MesaMapaResponse response = mapper.toMesaMapaResponse(mesa, List.of(), false, "mesero1@altoro.com", true);

        // Assert
        assertThat(response.getEsMesaPropia()).isFalse();
        assertThat(response.getNombreMesero()).isEqualTo("Carlos Pérez");
    }

    @Test
    @DisplayName("agruparItemsEnProduccion agrupa items EN_PREPARACION con mismo nombre y estado")
    void agruparItemsEnProduccion_AgrupaCorrectamente() {
        // Arrange
        ComandaItem item1 = crearItem("Bandeja Paisa", "Sin frijoles", 2, EstadoComanda.EN_PREPARACION, CategoriaProducto.PLATO);
        ComandaItem item2 = crearItem("Bandeja Paisa", "Sin frijoles", 1, EstadoComanda.EN_PREPARACION, CategoriaProducto.PLATO);

        // Act
        List<ItemComandaEnProduccionResponse> resultado = mapper.agruparItemsEnProduccion(List.of(item1, item2));

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCantidad()).isEqualTo(3);  // 2 + 1
        assertThat(resultado.get(0).getNombreProducto()).isEqualTo("Bandeja Paisa");
    }

    @Test
    @DisplayName("agruparItemsEnProduccion NO agrupa items PENDIENTE")
    void agruparItemsEnProduccion_NoAgrupaPendientes() {
        // Arrange
        ComandaItem item1 = crearItem("Limonada", null, 2, EstadoComanda.PENDIENTE, CategoriaProducto.BEBIDA);
        ComandaItem item2 = crearItem("Limonada", null, 1, EstadoComanda.PENDIENTE, CategoriaProducto.BEBIDA);

        // Act
        List<ItemComandaEnProduccionResponse> resultado = mapper.agruparItemsEnProduccion(List.of(item1, item2));

        // Assert - RN-06: PENDIENTE no se agrupa
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getCantidad()).isEqualTo(2);
        assertThat(resultado.get(1).getCantidad()).isEqualTo(1);
    }

    @Test
    @DisplayName("toMesaDetalleResponse mapea correctamente con cliente y reserva")
    void toMesaDetalleResponse_ConClienteYReserva() {
        // Arrange
        Mesa mesa = crearMesaConClienteYReserva();
        List<ItemComandaEnProduccionResponse> items = List.of(
                ItemComandaEnProduccionResponse.builder()
                        .nombreProducto("Bandeja Paisa")
                        .cantidad(2)
                        .build()
        );

        // Act
        MesaDetalleResponse response = mapper.toMesaDetalleResponse(mesa, List.of(), items, false);

        // Assert
        assertThat(response.getNombreCliente()).isEqualTo("María López");
        assertThat(response.getNotasReserva()).isEqualTo("Sin cebolla");
        assertThat(response.getNotasMesa()).isEqualTo("Cliente frecuente, atención preferencial");
        assertThat(response.getItemsComanda()).hasSize(1);
    }

    @Test
    @DisplayName("toMesaDetalleResponse mapea mesaNotas null correctamente")
    void toMesaDetalleResponse_MesaNotasNull() {
        // Arrange
        Mesa mesa = crearMesa("T-05", "mesero1@altoro.com", "Juan Pérez");
        mesa.setVisita(new Visita());
        mesa.getVisita().setVisitaId(1L);
        mesa.getVisita().setVisitaFechaHoraInicio(LocalDateTime.now());
        mesa.setMesaNotas(null);  // Explicitly null

        // Act
        MesaDetalleResponse response = mapper.toMesaDetalleResponse(mesa, List.of(), List.of(), false);

        // Assert
        assertThat(response.getNotasMesa()).isNull();
        assertThat(response.getNombreCliente()).isNull();  // No cliente
        assertThat(response.getNotasReserva()).isNull();  // No reserva
    }

    @Test
    @DisplayName("agruparItemsEnProduccion: items EN_PREPARACION con descripcion null se agrupan por clave 'nombre|'")
    void agruparItemsEnProduccion_DescripcionNullSeAgrupan() {
        // Arrange - dos items confirmados con descripcion null deben agruparse (clave "nombre|")
        ComandaItem item1 = crearItem("Cerveza", null, 2, EstadoComanda.EN_PREPARACION, CategoriaProducto.BEBIDA);
        ComandaItem item2 = crearItem("Cerveza", null, 3, EstadoComanda.LISTO, CategoriaProducto.BEBIDA);

        // Act
        List<ItemComandaEnProduccionResponse> resultado = mapper.agruparItemsEnProduccion(List.of(item1, item2));

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCantidad()).isEqualTo(5);
        assertThat(resultado.get(0).getDescripcion()).isNull();
        assertThat(resultado.get(0).getEstadoComanda()).isEqualTo("CONFIRMADO");
    }

    @Test
    @DisplayName("agruparItemsEnProduccion: respeta orden PLATO -> BEBIDA en resultado final")
    void agruparItemsEnProduccion_OrdenPorCategoria() {
        // Arrange
        ComandaItem bebida = crearItem("Limonada", null, 1, EstadoComanda.EN_PREPARACION, CategoriaProducto.BEBIDA);
        ComandaItem plato = crearItem("Bandeja", null, 1, EstadoComanda.COMPLETADO, CategoriaProducto.PLATO);

        // Act
        List<ItemComandaEnProduccionResponse> resultado = mapper.agruparItemsEnProduccion(List.of(bebida, plato));

        // Assert: orden PLATO -> BEBIDA
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getCategoriaProducto()).isEqualTo("PLATO");
        assertThat(resultado.get(1).getCategoriaProducto()).isEqualTo("BEBIDA");
    }

    @Test
    @DisplayName("toMesaDetalleResponse: concatena notas de comandas distintas y descarta vacías/null")
    void toMesaDetalleResponse_ConcatenaNotasComandasDistintas() {
        // Arrange - 3 items con notas distintas: una válida, una null, una vacía/whitespace, una duplicada
        Mesa mesa = crearMesa("T-07", "mesero1@altoro.com", "Juan Pérez");
        Visita visita = new Visita();
        visita.setVisitaId(1L);
        visita.setVisitaFechaHoraInicio(LocalDateTime.now());
        mesa.setVisita(visita);

        ComandaItem itemConNota = crearItemConNotaComanda("Cerveza", "Sin hielo");
        ComandaItem itemNotaNull = crearItemConNotaComanda("Limonada", null);
        ComandaItem itemNotaVacia = crearItemConNotaComanda("Agua", "   ");
        ComandaItem itemNotaDuplicada = crearItemConNotaComanda("Cerveza2", "Sin hielo");
        ComandaItem itemOtraNota = crearItemConNotaComanda("Vino", "Servido frío");

        // Act
        MesaDetalleResponse response = mapper.toMesaDetalleResponse(
                mesa,
                List.of(itemConNota, itemNotaNull, itemNotaVacia, itemNotaDuplicada, itemOtraNota),
                List.of(),
                false);

        // Assert: distincts only, separadas por " | ", null/blank descartadas
        assertThat(response.getNotasComandas()).contains("Sin hielo");
        assertThat(response.getNotasComandas()).contains("Servido frío");
        assertThat(response.getNotasComandas().split(" \\| ")).hasSize(2);
    }

    @Test
    @DisplayName("toMesaDetalleResponse: todos los items con notas vacías/null → notasComandas null")
    void toMesaDetalleResponse_TodasNotasNullONotasVacias() {
        // Arrange
        Mesa mesa = crearMesa("T-08", "mesero1@altoro.com", "Juan Pérez");
        Visita visita = new Visita();
        visita.setVisitaId(1L);
        visita.setVisitaFechaHoraInicio(LocalDateTime.now());
        mesa.setVisita(visita);

        ComandaItem itemA = crearItemConNotaComanda("X", null);
        ComandaItem itemB = crearItemConNotaComanda("Y", "");
        ComandaItem itemC = crearItemConNotaComanda("Z", "  ");

        // Act
        MesaDetalleResponse response = mapper.toMesaDetalleResponse(
                mesa, List.of(itemA, itemB, itemC), List.of(), false);

        // Assert
        assertThat(response.getNotasComandas()).isNull();
    }

    @Test
    @DisplayName("toMesaItemsProduccionResponse mapea correctamente")
    void toMesaItemsProduccionResponse_MapeaCorrectamente() {
        // Arrange
        List<ItemComandaEnProduccionResponse> items = List.of(
                ItemComandaEnProduccionResponse.builder()
                        .nombreProducto("Limonada")
                        .cantidad(3)
                        .build()
        );

        // Act
        MesaItemsProduccionResponse response = mapper.toMesaItemsProduccionResponse("T-05", items);

        // Assert
        assertThat(response.getIdentificadorMesa()).isEqualTo("T-05");
        assertThat(response.getItemsEnProduccion()).hasSize(1);
    }

    @Nested
    @DisplayName("EsCumpleanosHoy")
    class EsCumpleanosHoy {

        @Test
        @DisplayName("fechaNull → false")
        void fechaNull_retornaFalse() {
            assertThat(MesaMapper.esCumpleanosHoy(null)).isFalse();
        }

        @Test
        @DisplayName("mismo mes y día → true")
        void mismoMesYDia_retornaTrue() {
            LocalDate hoy = LocalDate.now();
            LocalDate hace30Anios = hoy.minusYears(30);
            assertThat(MesaMapper.esCumpleanosHoy(hace30Anios)).isTrue();
        }

        @Test
        @DisplayName("día distinto → false")
        void diaDistinto_retornaFalse() {
            LocalDate noEsHoy = LocalDate.now().minusDays(1).minusYears(20);
            assertThat(MesaMapper.esCumpleanosHoy(noEsHoy)).isFalse();
        }
    }

    @Nested
    @DisplayName("toMesaDetalleResponse — esCajero")
    class ToMesaDetalleResponseCajero {

        @Test
        @DisplayName("cajero con cliente cumpleaños hoy → clienteId, puntos, cumpleaños=true, puedeGenerarCuenta=true")
        void cajero_visitaConClienteCumpleHoy_camposLlenos() {
            // Arrange
            LocalDate hoy = LocalDate.now();
            LocalDate fechaNacimiento = hoy.minusYears(25);  // cumpleaños hoy

            Usuario usuario = new Usuario();
            usuario.setUsuarioEmail("mesero1@altoro.com");

            Empleado empleado = new Empleado();
            empleado.setUsuario(usuario);
            empleado.setEmpleadoNombre("Juan Pérez");

            Cliente cliente = new Cliente();
            cliente.setUsuarioId(100L);
            cliente.setClienteNombre("María López");
            cliente.setClienteFechaNacimiento(fechaNacimiento);
            cliente.setClientePuntos(50);

            Visita visita = new Visita();
            visita.setVisitaId(1L);
            visita.setVisitaFechaHoraInicio(LocalDateTime.now());
            visita.setCliente(cliente);

            Mesa mesa = new Mesa();
            mesa.setVisitaId(1L);
            mesa.setMesaIdentificador("T-01");
            mesa.setMesaNumeroPersonas(2);
            mesa.setMesaEstado(EstadoMesa.ATENDIDA);
            mesa.setMesero(empleado);
            mesa.setVisita(visita);

            // Act
            MesaDetalleResponse response = mapper.toMesaDetalleResponse(mesa, List.of(), List.of(), true);

            // Assert
            assertThat(response.getClienteId()).isEqualTo(100L);
            assertThat(response.getPuntosFidelizacion()).isEqualTo(50);
            assertThat(response.getEsCumpleanos()).isTrue();
            assertThat(response.getPuedeGenerarCuenta()).isTrue();
        }

        @Test
        @DisplayName("cajero con cliente sin cumpleaños hoy → esCumpleanos=false")
        void cajero_visitaConClienteSinCumple_esCumpleanosFalse() {
            // Arrange
            LocalDate ayer = LocalDate.now().minusDays(1);
            LocalDate fechaNacimiento = ayer.minusYears(25);  // cumpleaños ayer

            Usuario usuario = new Usuario();
            usuario.setUsuarioEmail("mesero1@altoro.com");

            Empleado empleado = new Empleado();
            empleado.setUsuario(usuario);
            empleado.setEmpleadoNombre("Juan Pérez");

            Cliente cliente = new Cliente();
            cliente.setUsuarioId(101L);
            cliente.setClienteNombre("Pedro García");
            cliente.setClienteFechaNacimiento(fechaNacimiento);
            cliente.setClientePuntos(25);

            Visita visita = new Visita();
            visita.setVisitaId(2L);
            visita.setVisitaFechaHoraInicio(LocalDateTime.now());
            visita.setCliente(cliente);

            Mesa mesa = new Mesa();
            mesa.setVisitaId(2L);
            mesa.setMesaIdentificador("T-02");
            mesa.setMesaNumeroPersonas(3);
            mesa.setMesaEstado(EstadoMesa.ATENDIDA);
            mesa.setMesero(empleado);
            mesa.setVisita(visita);

            // Act
            MesaDetalleResponse response = mapper.toMesaDetalleResponse(mesa, List.of(), List.of(), true);

            // Assert
            assertThat(response.getEsCumpleanos()).isFalse();
        }

        @Test
        @DisplayName("cajero walk-in sin cliente → clienteId null, puntos null, esCumpleanos null")
        void cajero_visitaSinCliente_camposClienteNull() {
            // Arrange
            Usuario usuario = new Usuario();
            usuario.setUsuarioEmail("mesero1@altoro.com");

            Empleado empleado = new Empleado();
            empleado.setUsuario(usuario);
            empleado.setEmpleadoNombre("Juan Pérez");

            Visita visita = new Visita();
            visita.setVisitaId(3L);
            visita.setVisitaFechaHoraInicio(LocalDateTime.now());
            visita.setCliente(null);  // walk-in

            Mesa mesa = new Mesa();
            mesa.setVisitaId(3L);
            mesa.setMesaIdentificador("T-03");
            mesa.setMesaNumeroPersonas(4);
            mesa.setMesaEstado(EstadoMesa.ATENDIDA);
            mesa.setMesero(empleado);
            mesa.setVisita(visita);

            // Act
            MesaDetalleResponse response = mapper.toMesaDetalleResponse(mesa, List.of(), List.of(), true);

            // Assert
            assertThat(response.getClienteId()).isNull();
            assertThat(response.getPuntosFidelizacion()).isNull();
            assertThat(response.getEsCumpleanos()).isNull();
            assertThat(response.getPuedeGenerarCuenta()).isTrue();  // ATENDIDA → true
        }

        @Test
        @DisplayName("cajero mesa ESPERA → puedeGenerarCuenta=false")
        void cajero_mesaEspera_puedeGenerarCuentaFalse() {
            // Arrange
            Usuario usuario = new Usuario();
            usuario.setUsuarioEmail("mesero1@altoro.com");

            Empleado empleado = new Empleado();
            empleado.setUsuario(usuario);
            empleado.setEmpleadoNombre("Juan Pérez");

            Visita visita = new Visita();
            visita.setVisitaId(4L);
            visita.setVisitaFechaHoraInicio(LocalDateTime.now());
            visita.setCliente(null);

            Mesa mesa = new Mesa();
            mesa.setVisitaId(4L);
            mesa.setMesaIdentificador("T-04");
            mesa.setMesaNumeroPersonas(2);
            mesa.setMesaEstado(EstadoMesa.ESPERA);
            mesa.setMesero(empleado);
            mesa.setVisita(visita);

            // Act
            MesaDetalleResponse response = mapper.toMesaDetalleResponse(mesa, List.of(), List.of(), true);

            // Assert
            assertThat(response.getPuedeGenerarCuenta()).isFalse();
        }

        @Test
        @DisplayName("mesero (esCajero=false) → los 4 campos extra son null")
        void mesero_esCajeroFalse_cuatroCamposNull() {
            // Arrange
            Usuario usuario = new Usuario();
            usuario.setUsuarioEmail("mesero1@altoro.com");

            Empleado empleado = new Empleado();
            empleado.setUsuario(usuario);
            empleado.setEmpleadoNombre("Juan Pérez");

            Cliente cliente = new Cliente();
            cliente.setUsuarioId(102L);
            cliente.setClienteNombre("Ana Martínez");
            cliente.setClienteFechaNacimiento(LocalDate.now().minusYears(30));
            cliente.setClientePuntos(100);

            Visita visita = new Visita();
            visita.setVisitaId(5L);
            visita.setVisitaFechaHoraInicio(LocalDateTime.now());
            visita.setCliente(cliente);

            Mesa mesa = new Mesa();
            mesa.setVisitaId(5L);
            mesa.setMesaIdentificador("T-05");
            mesa.setMesaNumeroPersonas(2);
            mesa.setMesaEstado(EstadoMesa.ATENDIDA);
            mesa.setMesero(empleado);
            mesa.setVisita(visita);

            // Act
            MesaDetalleResponse response = mapper.toMesaDetalleResponse(mesa, List.of(), List.of(), false);

            // Assert
            assertThat(response.getClienteId()).isNull();
            assertThat(response.getPuntosFidelizacion()).isNull();
            assertThat(response.getEsCumpleanos()).isNull();
            assertThat(response.getPuedeGenerarCuenta()).isNull();
        }
    }

    // Helpers

    private Mesa crearMesa(String identificador, String emailMesero, String nombreMesero) {
        Usuario usuario = new Usuario();
        usuario.setUsuarioEmail(emailMesero);

        Empleado empleado = new Empleado();
        empleado.setUsuario(usuario);
        empleado.setEmpleadoNombre(nombreMesero);

        Visita visita = new Visita();
        visita.setVisitaId(1L);
        visita.setVisitaFechaHoraInicio(LocalDateTime.now());

        Mesa mesa = new Mesa();
        mesa.setVisitaId(1L);
        mesa.setMesaIdentificador(identificador);
        mesa.setMesaNumeroPersonas(4);
        mesa.setMesaEstado(EstadoMesa.EN_PREPARACION);
        mesa.setMesero(empleado);
        mesa.setVisita(visita);

        return mesa;
    }

    private Mesa crearMesaConClienteYReserva() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioEmail("mesero1@altoro.com");

        Empleado empleado = new Empleado();
        empleado.setUsuario(usuario);
        empleado.setEmpleadoNombre("Juan Pérez");

        Cliente cliente = new Cliente();
        cliente.setClienteNombre("María López");

        Reserva reserva = new Reserva();
        reserva.setReservaNotas("Sin cebolla");

        Visita visita = new Visita();
        visita.setVisitaId(1L);
        visita.setVisitaFechaHoraInicio(LocalDateTime.now());
        visita.setCliente(cliente);
        visita.setReserva(reserva);

        Mesa mesa = new Mesa();
        mesa.setVisitaId(1L);
        mesa.setMesaIdentificador("T-10");
        mesa.setMesaNumeroPersonas(2);
        mesa.setMesaEstado(EstadoMesa.ATENDIDA);
        mesa.setMesero(empleado);
        mesa.setVisita(visita);
        mesa.setMesaNotas("Cliente frecuente, atención preferencial");

        return mesa;
    }

    private ComandaItem crearItemConNotaComanda(String nombreProducto, String notaComanda) {
        Producto producto = new Producto();
        producto.setProductoNombre(nombreProducto);
        producto.setProductoCategoria(CategoriaProducto.BEBIDA);

        Comanda comanda = new Comanda();
        comanda.setComandaId(1L);
        comanda.setComandaNotas(notaComanda);
        comanda.setComandaEstado(EstadoComanda.PENDIENTE);

        ComandaItem item = new ComandaItem();
        item.setProducto(producto);
        item.setComandaItemCantidad(1);
        item.setComanda(comanda);
        return item;
    }

    private ComandaItem crearItem(String nombreProducto, String descripcion, int cantidad, EstadoComanda estadoComanda, CategoriaProducto categoria) {
        CategoriaCarta categoriaCarta = new CategoriaCarta();
        categoriaCarta.setCategoriacartaId(1);

        Producto producto = new Producto();
        producto.setProductoId(1L);
        producto.setProductoNombre(nombreProducto);
        producto.setProductoCategoria(categoria);
        producto.setCategoriaCarta(categoriaCarta);

        Comanda comanda = new Comanda();
        comanda.setComandaId(1L);
        comanda.setComandaEstado(estadoComanda);

        ComandaItem item = new ComandaItem();
        item.setComandaItemId(1L);
        item.setProducto(producto);
        item.setComandaItemDescripcion(descripcion);
        item.setComandaItemCantidad(cantidad);
        item.setComanda(comanda);

        return item;
    }
}

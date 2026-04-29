# Diseño del Refactoring: Comandas Bajo Demanda

**Fecha:** 2026-04-26  
**Autor:** Equipo de desarrollo  
**Tipo:** Refactoring integral  
**Objetivo:** Migrar la arquitectura de comandas a un modelo "bajo demanda" donde solo se crean comandas cuando hay items de esa estación

---

## 1. Alcance del Refactoring

### Qué SE modifica

✅ **Esquema de base de datos (V1):**
- Permitir `comanda_fecha_hora_inicio` NULL
- Añadir estado `BORRADOR` a constraint de `comanda_estado`
- Consolidar migraciones V4 y V5 en V3

✅ **Entidades y enums:**
- `EstadoComanda`: añadir valor `BORRADOR`
- `Comanda`: hacer `comandaFechaHoraInicio` nullable

✅ **DTOs:**
- `ReservaDetalleResponse`: añadir `clienteNombre` y `clienteTelefono`
- `ReservaConsultaResponse`: añadir `clienteTelefono`
- `ItemVisitaResponse`: añadir `descripcion`, eliminar `comandaItemId`
- `ComandaItemResponse`: añadir `descripcion`

✅ **Servicios:**
- `ReservaService.crearReserva()`: comandas bajo demanda por estación
- `ReservaService.modificarReserva()`: actualizar comandas bajo demanda
- `VisitaEstadoService`: agrupar items por (nombre + descripcion)
- `VisitaMapper`: agrupar items en detalle de visita

✅ **Mappers:**
- `ReservaMapper`: mapear múltiples comandas, añadir clienteNombre/Telefono
- `ReservaConsultaMapper`: añadir clienteTelefono
- `VisitaEstadoMapper`: método de agrupación de items

✅ **Tests:**
- Actualizar todos los tests de reservas
- Añadir tests para comandas bajo demanda
- Añadir tests para agrupación de items

✅ **Postman:**
- Actualizar colecciones de reservas
- Actualizar colecciones de visitas
- Verificar estructura de DTOs

### Qué NO se modifica

❌ Nada relacionado con mapa de mesas (HE-03-HU-04)  
❌ Nada relacionado con modificar comanda (HE-03-HU-05)  
❌ Funcionalidad de visitas/ventas  
❌ Otros módulos

---

## 2. Arquitectura de Comandas Bajo Demanda

### Principio fundamental

**SOLO crear comandas cuando HAY items de esa estación.**

### Flujo de creación de comandas

```
Al crear/modificar reserva con pre-orden:
  1. Separar items por categoría del producto:
     - PLATO → EstacionComanda.COCINA
     - BEBIDA → EstacionComanda.BARRA
  
  2. Para cada estación (COCINA, BARRA):
     SI hay items de esa estación:
       - Crear/actualizar comanda con estación asignada
       - Añadir items a esa comanda
     SINO:
       - NO crear comanda
       - SI existía comanda de esa estación: eliminarla
```

### Estados del ciclo de vida

```
PRE_RESERVA (al crear reserva con pre-orden)
    ↓
BORRADOR (al marcar llegada de reserva, o al guardar sin enviar)
    ↓
PENDIENTE (al enviar a producción) ← SE ASIGNA comandaFechaHoraInicio
    ↓
EN_PREPARACION
    ↓
LISTO
    ↓
COMPLETADO
```

### Campos clave

| Campo | PRE_RESERVA | BORRADOR | PENDIENTE+ |
|-------|-------------|----------|------------|
| `comandaEstacion` | COCINA/BARRA | COCINA/BARRA | COCINA/BARRA |
| `comandaFechaHoraInicio` | **NULL** | **NULL** | **NOT NULL** |
| `visita` | NULL | NULL/NOT NULL | NOT NULL |
| `reserva` | NOT NULL | NOT NULL/NULL | NOT NULL/NULL |

---

## 3. Cambios en Base de Datos

### Migración V1 (editar archivo existente)

**Archivo:** `backend/src/main/resources/db/migration/V1__init_schema.sql`

**Línea 393 - ANTES:**
```sql
comanda_fecha_hora_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
```

**Línea 393 - DESPUÉS:**
```sql
comanda_fecha_hora_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
```

**Línea 405 - ANTES:**
```sql
CONSTRAINT chk_comanda_estado CHECK (comanda_estado IN ('PRE_RESERVA', 'PENDIENTE', 'EN_PREPARACION', 'LISTO', 'COMPLETADO')),
```

**Línea 405 - DESPUÉS:**
```sql
CONSTRAINT chk_comanda_estado CHECK (comanda_estado IN ('PRE_RESERVA', 'BORRADOR', 'PENDIENTE', 'EN_PREPARACION', 'LISTO', 'COMPLETADO')),
```

**Línea 406 - ANTES:**
```sql
CONSTRAINT chk_comanda_fechas CHECK (comanda_fecha_hora_listo IS NULL OR comanda_fecha_hora_listo >= comanda_fecha_hora_inicio)
```

**Línea 406 - DESPUÉS:**
```sql
CONSTRAINT chk_comanda_fechas CHECK (comanda_fecha_hora_listo IS NULL OR comanda_fecha_hora_inicio IS NULL OR comanda_fecha_hora_listo >= comanda_fecha_hora_inicio)
```

**Línea 412 - ANTES:**
```sql
COMMENT ON COLUMN Comanda.comanda_estacion IS 'NULL en estado PRE_RESERVA; se asigna al convertir a comanda activa';
```

**Línea 412 - DESPUÉS:**
```sql
COMMENT ON COLUMN Comanda.comanda_estacion IS 'NULL en estado PRE_RESERVA o BORRADOR; se asigna al enviar a producción (PENDIENTE)';
```

**Añadir después de línea 412:**
```sql
COMMENT ON COLUMN Comanda.comanda_fecha_hora_inicio IS 'Momento en que se envió la comanda a producción; NULL mientras está en PRE_RESERVA o BORRADOR';
```

### Migración V3 (consolidar V4 y V5)

**Archivo:** `backend/src/main/resources/db/migration/V3__dev_data.sql`

**Añadir al final:**
```sql
-- =====================================================
-- DATOS DE SOPORTE PARA TESTS POSTMAN (ex-V4 y V5)
-- =====================================================

-- Segunda decoración con costo para test MR-13
INSERT INTO Decoracion (decoracion_nombre, decoracion_estado, decoracion_costo_adicional, decoracion_imagen_url) 
VALUES ('Bodas Premium', 'ACTIVO', 60000.00, 'https://picsum.photos/seed/decor-bodas/360/220');

-- Reserva CANCELADA para carlos.perez@gmail.com (test MR-08)
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
SELECT u.usuario_id, NULL, NULL, NOW() - INTERVAL '2 days', 2, NULL, 'CANCELADA', 'BASICA', NOW() - INTERVAL '3 days'
FROM Usuario u WHERE u.usuario_email = 'carlos.perez@gmail.com';

-- Reserva BASICA CONFIRMADA con anticipo para andres.morales@gmail.com (CR-10)
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
SELECT u.usuario_id, NULL, NULL, NOW() + INTERVAL '30 days', 2, NULL, 'CONFIRMADA', 'BASICA', NOW() - INTERVAL '1 day'
FROM Usuario u WHERE u.usuario_email = 'andres.morales@gmail.com';

-- Abono ANTICIPO para la reserva
INSERT INTO Abono (cajero_id, reserva_id, abono_monto, abono_fecha_hora, abono_metodo, abono_tipo)
SELECT 2, r.reserva_id, 40000, NOW() - INTERVAL '12 hours', 'TRANSFERENCIA', 'ANTICIPO'
FROM Reserva r
JOIN Usuario u ON u.usuario_id = r.cliente_id
WHERE u.usuario_email = 'andres.morales@gmail.com'
  AND r.reserva_estado = 'CONFIRMADA'
  AND r.reserva_tipo = 'BASICA'
ORDER BY r.reserva_id DESC
LIMIT 1;

-- Reserva ESPECIAL PENDIENTE con fecha pasada para andres.morales@gmail.com (CR-12)
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
SELECT u.usuario_id, NULL, NULL, NOW() - INTERVAL '1 day', 2, NULL, 'PENDIENTE', 'ESPECIAL', NOW() - INTERVAL '3 days'
FROM Usuario u WHERE u.usuario_email = 'andres.morales@gmail.com';
```

**Eliminar archivos:**
- `V4__mr_test_seed.sql`
- `V5__cr_test_seed.sql`

---

## 4. Cambios en Entidades y Enums

### EstadoComanda.java

**Ubicación:** `backend/src/main/java/co/edu/unicauca/backend/shared/enums/EstadoComanda.java`

```java
/**
 * Estados posibles de una {@link co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda}.
 *
 * <p>Ciclo de vida:
 * <pre>
 *   PRE_RESERVA → BORRADOR → PENDIENTE → EN_PREPARACION → LISTO → COMPLETADO
 * </pre>
 *
 * <ul>
 *   <li>{@code PRE_RESERVA} — pre-orden registrada al crear una reserva; aún no enviada a producción.</li>
 *   <li>{@code BORRADOR} — comanda guardada por el mesero pero no enviada a producción.</li>
 *   <li>{@code PENDIENTE} — comanda enviada a la estación de producción, en espera de ser tomada.</li>
 *   <li>{@code EN_PREPARACION} — la estación de producción inició la preparación.</li>
 *   <li>{@code LISTO} — preparación finalizada; pendiente de entrega al cliente.</li>
 *   <li>{@code COMPLETADO} — entregada al cliente; ciclo cerrado.</li>
 * </ul>
 */
public enum EstadoComanda {
    PRE_RESERVA("Pre-Reserva"),
    BORRADOR("Borrador"),  // ← NUEVO
    PENDIENTE("Pendiente"),
    EN_PREPARACION("En Preparación"),
    LISTO("Listo"),
    COMPLETADO("Completado");

    private final String descripcion;

    EstadoComanda(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
```

### Comanda.java

**Ubicación:** `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/entity/Comanda.java`

**Cambios:**

1. **Actualizar Javadoc de clase:**
```java
/**
 * Orden de producción asociada a una visita en curso o a una reserva con pre-orden.
 *
 * <p>Una comanda en estado {@code PRE_RESERVA} representa la pre-orden registrada por el
 * cliente al crear una reserva. Una comanda en estado {@code BORRADOR} representa una orden
 * guardada por el mesero pero no enviada a producción. En ambos estados {@code visita} y 
 * {@code comanda_estacion} pueden ser {@code null} dependiendo del contexto.
 *
 * <p>Ciclo de vida del estado ({@link EstadoComanda}):
 * <pre>
 *   PRE_RESERVA → BORRADOR → PENDIENTE → EN_PREPARACION → LISTO → COMPLETADO
 * </pre>
 * La transición a {@code PENDIENTE} ocurre al enviar la comanda a producción, momento en que
 * se asigna {@code comandaFechaHoraInicio}.
 * ...
 */
```

2. **Campo comandaFechaHoraInicio:**
```java
/**
 * Fecha y hora en que se envió la comanda a producción.
 * {@code null} mientras está en estado {@code PRE_RESERVA} o {@code BORRADOR};
 * se asigna al pasar a {@code PENDIENTE}.
 */
@Column(name = "comanda_fecha_hora_inicio")
private LocalDateTime comandaFechaHoraInicio;
```

3. **Método @PrePersist:**
```java
@PrePersist
protected void onCreate() {
    super.onCreate();
    // comandaFechaHoraInicio se asigna explícitamente al enviar a producción
}
```

---

## 5. Cambios en DTOs

### ReservaDetalleResponse.java

**Añadir después de `reservaId`:**
```java
/** Nombre completo del cliente que realizó la reserva. */
private final String clienteNombre;

/** Teléfono del cliente; {@code null} en consultas de cliente. */
private final String clienteTelefono;
```

**Actualizar Javadoc:**
```java
/**
 * DTO de respuesta con el detalle completo de una reserva.
 *
 * <p>Incluye los datos de la reserva, información básica del cliente (nombre y teléfono),
 * la pre-orden solicitada al momento de reservar y el historial de abonos registrados por caja.
 *
 * <p>Campos opcionales (pueden ser {@code null} si no aplican):
 * <ul>
 *   <li>{@code clienteNombre} y {@code clienteTelefono} — solo visibles para meseros/admin, {@code null} para clientes.</li>
 *   <li>{@code zonaId} y {@code zonaNombre} — si el cliente no seleccionó zona.</li>
 *   <li>{@code decoracionId} y {@code decoracionNombre} — si la reserva no incluyó decoración.</li>
 *   <li>{@code notas} — si no se registraron observaciones especiales.</li>
 *   <li>{@code preOrdenItems} y {@code preOrdenTotal} — si la reserva no tiene pre-orden.</li>
 *   <li>{@code abonos} y {@code totalAbonado} — si no se registraron anticipos.</li>
 * </ul>
 */
```

### ReservaConsultaResponse.java

**Añadir después de `clienteNombre`:**
```java
/** Teléfono del cliente. */
private final String clienteTelefono;
```

### ItemVisitaResponse.java

**ANTES:**
```java
@Getter
@Builder
public class ItemVisitaResponse {
    private final Long comandaItemId;
    private final String nombreProducto;
    private final Integer cantidad;
    private final String estadoItem;
    private final BigDecimal precioUnitario;
    private final BigDecimal subtotal;
}
```

**DESPUÉS:**
```java
@Getter
@Builder
public class ItemVisitaResponse {
    // comandaItemId ELIMINADO (no tiene sentido en items agrupados)
    private final String nombreProducto;
    private final String descripcion;  // ← NUEVO (puede ser null)
    private final Integer cantidad;
    private final String estadoItem;
    private final BigDecimal precioUnitario;
    private final BigDecimal subtotal;
}
```

### ComandaItemResponse.java

**AÑADIR:**
```java
/** Descripción o modificaciones del ítem; {@code null} si no aplica. */
private final String descripcion;
```

---

## 6. Cambios en Servicios

### ReservaService.java

#### Método: crearReserva() - Comandas bajo demanda

**Nuevo método privado:**
```java
/**
 * Crea comandas PRE_RESERVA bajo demanda según los items de la pre-orden.
 * Solo crea comanda para una estación si hay items de esa categoría.
 * 
 * @param reserva reserva recién creada
 * @param items lista de items de la pre-orden
 */
private void crearComandasPreReservaBajoDemanda(Reserva reserva, List<PreOrdenItemRequest> items) {
    // Separar items por categoría del producto
    Map<EstacionComanda, List<PreOrdenItemRequest>> itemsPorEstacion = items.stream()
        .collect(Collectors.groupingBy(item -> {
            Producto producto = productoRepository.findById(item.getProductoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, 
                    "Producto no encontrado", HttpStatus.NOT_FOUND));
            return producto.getProductoCategoria() == CategoriaCarta.BEBIDA 
                ? EstacionComanda.BARRA : EstacionComanda.COCINA;
        }));
    
    // Crear comanda por estación SOLO si hay items
    itemsPorEstacion.forEach((estacion, itemsEstacion) -> {
        if (!itemsEstacion.isEmpty()) {  // ← COMANDAS BAJO DEMANDA
            Comanda comanda = Comanda.builder()
                .reserva(reserva)
                .comandaEstacion(estacion)
                .comandaEstado(EstadoComanda.PRE_RESERVA)
                .comandaFechaHoraInicio(null)  // NULL hasta enviar a producción
                .build();
            comandaRepository.save(comanda);
            
            // Crear items de comanda
            itemsEstacion.forEach(itemReq -> {
                Producto producto = productoRepository.findById(itemReq.getProductoId()).get();
                ComandaItem comandaItem = ComandaItem.builder()
                    .comanda(comanda)
                    .producto(producto)
                    .comandaItemCantidad(itemReq.getCantidad())
                    .comandaItemPrecio(producto.getProductoPrecio())
                    .comandaItemDescripcion(itemReq.getDescripcion())
                    .build();
                comandaItemRepository.save(comandaItem);
                
                // Guardar modificaciones de menú especial si aplica
                if (itemReq.getModificaciones() != null && !itemReq.getModificaciones().isEmpty()) {
                    guardarModificacionesMenuItem(comandaItem, itemReq.getModificaciones());
                }
            });
        }
    });
}
```

**Modificar crearReserva():**
```java
// Crear comandas bajo demanda si hay pre-orden
if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
    crearComandasPreReservaBajoDemanda(reserva, request.getPreOrden());
}
```

#### Método: modificarReserva() - Actualizar comandas bajo demanda

**Nuevo método privado:**
```java
/**
 * Actualiza las comandas PRE_RESERVA de una reserva bajo demanda.
 * - Si hay items de una estación: crea/actualiza comanda de esa estación
 * - Si no hay items de una estación: elimina comanda de esa estación (si existía)
 * 
 * @param reserva reserva a modificar
 * @param nuevosItems nuevos items de la pre-orden (null = eliminar todas las comandas)
 */
private void actualizarComandasPreReserva(Reserva reserva, List<PreOrdenItemRequest> nuevosItems) {
    // Obtener comandas PRE_RESERVA existentes
    List<Comanda> comandasExistentes = comandaRepository.findByReservaAndComandaEstado(
        reserva, EstadoComanda.PRE_RESERVA);
    
    if (nuevosItems == null || nuevosItems.isEmpty()) {
        // Eliminar todas las comandas PRE_RESERVA
        comandasExistentes.forEach(comanda -> {
            comandaMenuModificacionRepository.deleteByComandaItem_Comanda(comanda);
            comandaItemRepository.deleteByComanda(comanda);
            comandaRepository.delete(comanda);
        });
        return;
    }
    
    // Separar items por estación
    Map<EstacionComanda, List<PreOrdenItemRequest>> itemsPorEstacion = 
        separarItemsPorEstacion(nuevosItems);
    
    // Para cada estación (COCINA, BARRA)
    for (EstacionComanda estacion : EstacionComanda.values()) {
        List<PreOrdenItemRequest> itemsEstacion = itemsPorEstacion.get(estacion);
        Comanda comandaExistente = comandasExistentes.stream()
            .filter(c -> c.getComandaEstacion() == estacion)
            .findFirst().orElse(null);
        
        if (itemsEstacion != null && !itemsEstacion.isEmpty()) {
            // HAY items de esta estación
            if (comandaExistente != null) {
                // Actualizar comanda existente: eliminar items viejos, crear nuevos
                comandaMenuModificacionRepository.deleteByComandaItem_Comanda(comandaExistente);
                comandaItemRepository.deleteByComanda(comandaExistente);
                crearItemsParaComanda(comandaExistente, itemsEstacion);
            } else {
                // Crear nueva comanda para esta estación
                crearComandaConItems(reserva, estacion, itemsEstacion);
            }
        } else {
            // NO hay items de esta estación
            if (comandaExistente != null) {
                // Eliminar comanda que ya no tiene items
                comandaMenuModificacionRepository.deleteByComandaItem_Comanda(comandaExistente);
                comandaItemRepository.deleteByComanda(comandaExistente);
                comandaRepository.delete(comandaExistente);
            }
            // Si no existía, no hacer nada
        }
    }
}

/**
 * Separa items por estación según categoría del producto.
 */
private Map<EstacionComanda, List<PreOrdenItemRequest>> separarItemsPorEstacion(
        List<PreOrdenItemRequest> items) {
    return items.stream()
        .collect(Collectors.groupingBy(item -> {
            Producto producto = productoRepository.findById(item.getProductoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, 
                    "Producto no encontrado", HttpStatus.NOT_FOUND));
            return producto.getProductoCategoria() == CategoriaCarta.BEBIDA 
                ? EstacionComanda.BARRA : EstacionComanda.COCINA;
        }));
}

/**
 * Crea items para una comanda existente.
 */
private void crearItemsParaComanda(Comanda comanda, List<PreOrdenItemRequest> items) {
    items.forEach(itemReq -> {
        Producto producto = productoRepository.findById(itemReq.getProductoId()).get();
        ComandaItem comandaItem = ComandaItem.builder()
            .comanda(comanda)
            .producto(producto)
            .comandaItemCantidad(itemReq.getCantidad())
            .comandaItemPrecio(producto.getProductoPrecio())
            .comandaItemDescripcion(itemReq.getDescripcion())
            .build();
        comandaItemRepository.save(comandaItem);
        
        if (itemReq.getModificaciones() != null && !itemReq.getModificaciones().isEmpty()) {
            guardarModificacionesMenuItem(comandaItem, itemReq.getModificaciones());
        }
    });
}

/**
 * Crea una nueva comanda con items.
 */
private void crearComandaConItems(Reserva reserva, EstacionComanda estacion, 
                                  List<PreOrdenItemRequest> items) {
    Comanda comanda = Comanda.builder()
        .reserva(reserva)
        .comandaEstacion(estacion)
        .comandaEstado(EstadoComanda.PRE_RESERVA)
        .comandaFechaHoraInicio(null)
        .build();
    comandaRepository.save(comanda);
    crearItemsParaComanda(comanda, items);
}
```

### VisitaEstadoService.java

#### Método: obtenerEstadoVisitaActiva() - Agrupar items

**ANTES:**
```java
List<ItemVisitaResponse> items = comandas.stream()
    .filter(c -> c.getComandaEstado() != EstadoComanda.PRE_RESERVA)
    .flatMap(c -> comandaItemRepository.findByComanda_ComandaId(c.getComandaId())
            .stream()
            .map(item -> visitaEstadoMapper.toItemVisitaResponse(item, c.getComandaEstado())))
    .collect(Collectors.toList());
```

**DESPUÉS:**
```java
// Obtener todas las comandas no PRE_RESERVA
List<Comanda> comandasActivas = comandas.stream()
    .filter(c -> c.getComandaEstado() != EstadoComanda.PRE_RESERVA)
    .toList();

// Obtener todos los items
List<ComandaItem> todosLosItems = comandasActivas.stream()
    .flatMap(c -> comandaItemRepository.findByComanda_ComandaId(c.getComandaId()).stream())
    .toList();

// Determinar estado más avanzado para mostrar
EstadoComanda estadoGeneral = comandasActivas.isEmpty() 
    ? EstadoComanda.PENDIENTE
    : comandasActivas.stream()
        .map(Comanda::getComandaEstado)
        .max(Comparator.comparingInt(Enum::ordinal))
        .orElse(EstadoComanda.PENDIENTE);

// Agrupar y mapear
List<ItemVisitaResponse> items = visitaEstadoMapper.agruparYMapearItems(
    todosLosItems, 
    estadoGeneral
);
```

---

## 7. Cambios en Mappers

### ReservaMapper.java

**Método: toReservaDetalleResponse()**
```java
public ReservaDetalleResponse toReservaDetalleResponse(
        Reserva reserva, 
        boolean incluirDatosCliente) {
    
    return ReservaDetalleResponse.builder()
        .reservaId(reserva.getReservaId())
        .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
        .clienteNombre(incluirDatosCliente ? reserva.getCliente().getClienteNombre() : null)
        .clienteTelefono(incluirDatosCliente ? reserva.getCliente().getClienteTelefono() : null)
        // ... resto de campos
        .build();
}
```

**Método: toPreOrdenItemsResponse() - Múltiples comandas**
```java
/**
 * Mapea items de múltiples comandas PRE_RESERVA a lista unificada.
 */
public List<PreOrdenItemResponse> toPreOrdenItemsResponse(List<Comanda> comandas) {
    return comandas.stream()
        .flatMap(comanda -> comanda.getComandaItems().stream())
        .map(this::toPreOrdenItemResponse)
        .sorted(Comparator.comparing(PreOrdenItemResponse::getProductoNombre))
        .toList();
}

private PreOrdenItemResponse toPreOrdenItemResponse(ComandaItem item) {
    // ... mapeo individual existente
}
```

### ReservaConsultaMapper.java

```java
public ReservaConsultaResponse toReservaConsultaResponse(Reserva reserva) {
    return ReservaConsultaResponse.builder()
        .reservaId(reserva.getReservaId())
        .clienteNombre(reserva.getCliente().getClienteNombre())
        .clienteTelefono(reserva.getCliente().getClienteTelefono())  // ← NUEVO
        .zonaId(reserva.getZona() != null ? reserva.getZona().getZonaId() : null)
        .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
        .decoracionNombre(reserva.getDecoracion() != null ? 
            reserva.getDecoracion().getDecoracionNombre() : null)
        .horaLlegada(formatearHora(reserva.getReservaFechaHoraLlegada()))
        .numeroPersonas(reserva.getReservaNumeroPersonas())
        .estado(reserva.getReservaEstado().name())
        .build();
}
```

### VisitaEstadoMapper.java

**Nuevo método:**
```java
/**
 * Agrupa items por (nombreProducto + descripcion) y suma cantidades.
 * 
 * @param items lista de items a agrupar
 * @param estadoGeneral estado de comanda a mostrar
 * @return lista de items agrupados ordenados por nombre
 */
public List<ItemVisitaResponse> agruparYMapearItems(
        List<ComandaItem> items, 
        EstadoComanda estadoGeneral) {
    
    // Clave de agrupación: nombreProducto + "|" + descripcion (null-safe)
    Map<String, List<ComandaItem>> agrupados = items.stream()
        .collect(Collectors.groupingBy(item -> 
            item.getProducto().getProductoNombre() + "|" + 
            (item.getComandaItemDescripcion() != null ? item.getComandaItemDescripcion() : "")
        ));
    
    return agrupados.values().stream()
        .map(grupo -> {
            ComandaItem primero = grupo.get(0);
            int cantidadTotal = grupo.stream()
                .mapToInt(ComandaItem::getComandaItemCantidad)
                .sum();
            BigDecimal subtotal = primero.getComandaItemPrecio()
                .multiply(BigDecimal.valueOf(cantidadTotal));
            
            return ItemVisitaResponse.builder()
                .nombreProducto(primero.getProducto().getProductoNombre())
                .descripcion(primero.getComandaItemDescripcion())
                .cantidad(cantidadTotal)
                .estadoItem(resolverEstadoItem(estadoGeneral))
                .precioUnitario(primero.getComandaItemPrecio())
                .subtotal(subtotal)
                .build();
        })
        .sorted(Comparator.comparing(ItemVisitaResponse::getNombreProducto))
        .toList();
}
```

### VisitaMapper.java

**Nuevo método privado:**
```java
/**
 * Agrupa items de comanda por (nombreProducto + descripcion) y suma cantidades.
 */
private List<ComandaItemResponse> agruparItems(List<ComandaItem> items) {
    Map<String, List<ComandaItem>> agrupados = items.stream()
        .collect(Collectors.groupingBy(item -> 
            item.getProducto().getProductoNombre() + "|" + 
            (item.getComandaItemDescripcion() != null ? item.getComandaItemDescripcion() : "")
        ));
    
    return agrupados.values().stream()
        .map(grupo -> {
            ComandaItem primero = grupo.get(0);
            int cantidadTotal = grupo.stream()
                .mapToInt(ComandaItem::getComandaItemCantidad)
                .sum();
            
            return ComandaItemResponse.builder()
                .nombreProducto(primero.getProducto().getProductoNombre())
                .descripcion(primero.getComandaItemDescripcion())
                .cantidad(cantidadTotal)
                .precioUnitario(primero.getComandaItemPrecio())
                .subtotal(primero.getComandaItemPrecio()
                    .multiply(BigDecimal.valueOf(cantidadTotal)))
                .build();
        })
        .sorted(Comparator.comparing(ComandaItemResponse::getNombreProducto))
        .toList();
}
```

**Modificar toDetalle():**
```java
List<ComandaItemResponse> itemsDto = itemsComanda.isEmpty() ? null :
    agruparItems(itemsComanda);
```

---

## 8. Cambios en Repositories

**ComandaRepository.java:**
```java
/**
 * Encuentra todas las comandas de una reserva en un estado específico.
 */
List<Comanda> findByReservaAndComandaEstado(Reserva reserva, EstadoComanda estado);
```

**ComandaItemRepository.java:**
```java
/**
 * Elimina todos los items de una comanda.
 */
@Modifying
@Transactional
void deleteByComanda(Comanda comanda);
```

**ComandaMenuModificacionRepository.java:**
```java
/**
 * Elimina todas las modificaciones de items de una comanda.
 */
@Modifying
@Transactional
void deleteByComandaItem_Comanda(Comanda comanda);
```

---

## 9. Plan de Implementación (Tasks)

### ✅ Task 0: Consolidar migraciones
- Mover contenido de V4 y V5 a V3
- Eliminar V4__mr_test_seed.sql y V5__cr_test_seed.sql
- **Checkpoint:** `mvn flyway:clean flyway:migrate` exitoso

### ✅ Task 1: Actualizar esquema (V1)
- Modificar V1 según especificado
- **Checkpoint:** `mvn flyway:clean flyway:migrate` + `mvn clean compile` exitoso

### Task 2: Actualizar enums y entidades
- Modificar EstadoComanda.java (añadir BORRADOR)
- Modificar Comanda.java (nullable, quitar @PrePersist default)
- **Checkpoint:** `mvn clean compile` sin errores

### Task 2.5: Añadir descripcion a DTOs y agrupación

#### Paso 1: BASELINE
```bash
mvn test -Dtest=VisitaEstadoServiceTest > baseline-visitaestado.log
mvn test -Dtest=VisitaServiceTest > baseline-visita.log
mvn test -Dtest=VisitaControllerTest > baseline-controller.log
```

#### Paso 2: Modificar DTOs
- ItemVisitaResponse: añadir `descripcion`, ELIMINAR `comandaItemId`
- ComandaItemResponse: añadir `descripcion`

#### Paso 3: Modificar mappers
- VisitaEstadoMapper: añadir `agruparYMapearItems()`
- VisitaMapper: añadir `agruparItems()`

#### Paso 4: Modificar servicios
- VisitaEstadoService: usar agrupación
- VisitaMapper.toDetalle(): usar agrupación

#### Paso 5: Actualizar tests
- VisitaEstadoServiceTest: añadir `descripcion` a helper `item()`, añadir tests de agrupación
- VisitaServiceTest: similar
- VisitaControllerTest: actualizar mocks

**Checkpoint:** `mvn test -Dtest=Visita*` todos pasan

### Task 3: Actualizar DTOs de reserva
- Modificar ReservaDetalleResponse (añadir clienteNombre)
- Modificar ReservaConsultaResponse (añadir clienteTelefono)
- **Checkpoint:** `mvn clean compile` sin errores

### Task 4: Actualizar repositories
- Añadir queries en ComandaRepository, ComandaItemRepository, ComandaMenuModificacionRepository
- **Checkpoint:** `mvn clean compile` sin errores

### Task 5: Actualizar mappers de reserva
- Modificar ReservaMapper (clienteNombre/Telefono, múltiples comandas)
- Modificar ReservaConsultaMapper (clienteTelefono)
- **Checkpoint:** `mvn clean compile` sin errores

### Task 6: Refactorizar ReservaService.crearReserva()
- Implementar `crearComandasPreReservaBajoDemanda()`
- Modificar método principal
- **Checkpoint:** `mvn test -Dtest=ReservaServiceTest#crearReserva*` pasan

### Task 7: Actualizar tests de crear reserva
- Modificar tests existentes para verificar comandas bajo demanda
- Añadir nuevos tests:
  - Solo platos → 1 comanda COCINA
  - Solo bebidas → 1 comanda BARRA
  - Platos + bebidas → 2 comandas
  - Sin pre-orden → 0 comandas
- **Checkpoint:** `mvn test -Dtest=ReservaServiceTest` todos pasan

### Task 8: Refactorizar ReservaService.modificarReserva()
- Implementar `actualizarComandasPreReserva()`
- Implementar métodos helper (separarItemsPorEstacion, etc.)
- Modificar método principal
- **Checkpoint:** `mvn test -Dtest=ReservaServiceModificarTest#modificarReserva*` pasan

### Task 9: Actualizar tests de modificar reserva
- Modificar tests existentes
- Añadir nuevos tests:
  - Agregar bebidas a cocina existente → crea comanda BARRA
  - Eliminar todos los platos → elimina comanda COCINA
  - Eliminar todos los items → elimina todas las comandas
- **Checkpoint:** `mvn test -Dtest=ReservaServiceModificarTest` todos pasan

### Task 10: Verificación integral
- Ejecutar **TODA** la suite: `mvn test`
- **Checkpoint:** 152+ tests pasan

### Task 11: Actualizar Postman

#### Colección: POST /api/reservas
```javascript
// Verificar cantidad de comandas según items
if (tienePlatosYBebidas) {
    pm.expect(comandas.length).to.equal(2);
} else if (tienePlatos || tieneBebidas) {
    pm.expect(comandas.length).to.equal(1);
} else {
    pm.expect(comandas.length).to.equal(0);
}
```

#### Colección: GET /api/visitas/activa
```javascript
// ELIMINAR test de comandaItemId (ya no existe)
// AÑADIR test de descripcion
pm.test('Cada ítem tiene descripcion (puede ser null)', function() {
    const items = pm.response.json().data.items;
    items.forEach(item => {
        pm.expect(item).to.have.property('descripcion');
    });
});
```

**Checkpoint:** Todas las colecciones pasan

### Task 12: Verificación final
- `mvn spring-boot:run`
- Probar crear reserva con pre-orden (platos + bebidas)
- Verificar en BD: 2 comandas PRE_RESERVA (COCINA + BARRA)
- Probar modificar reserva
- **Checkpoint:** Todo funciona

---

## 10. Entregables

✅ Migraciones consolidadas (V1, V2, V3)  
✅ EstadoComanda con BORRADOR  
✅ Comanda.comandaFechaHoraInicio nullable  
✅ DTOs actualizados (clienteNombre, clienteTelefono, descripcion)  
✅ Servicios con comandas bajo demanda  
✅ Agrupación de items por (nombre + descripcion)  
✅ Tests actualizados (152+)  
✅ Postman actualizado  
✅ Documentación (Javadocs)

---

## 11. Riesgos y Mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Tests fallan después de cambios en DTOs | Alta | Alto | Baseline antes de cambios, actualizar tests task por task |
| Comandas no se crean correctamente bajo demanda | Media | Alto | Tests exhaustivos de crear/modificar reserva |
| Items no se agrupan correctamente | Media | Medio | Tests específicos de agrupación por (nombre + descripcion) |
| Postman falla por cambio en estructura | Alta | Medio | Actualizar scripts task por task, verificar al final |
| Frontend rompe por cambio en DTOs | Alta | Alto | ⚠️ **COORDINAR CON FRONTEND** antes de desplegar |

**IMPORTANTE:** Este refactoring cambia contratos de API. Requiere coordinación con frontend antes de merge a develop.

---

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>

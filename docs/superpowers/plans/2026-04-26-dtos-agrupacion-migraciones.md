# DTOs, Agrupación y Migraciones Base - Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mejorar DTOs con información de cliente, implementar agrupación de items por (nombre + descripcion), y preparar migraciones base para estado BORRADOR

**Architecture:** Refactoring incremental sin cambiar lógica de negocio de comandas. Solo mejoras en capa de presentación (DTOs), agregación de datos (mappers), y preparación de esquema para futuras funcionalidades

**Tech Stack:** Spring Boot 3.5, PostgreSQL 15, Flyway, JUnit 5, AssertJ, Postman

---

## File Structure

### Migraciones
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql` (líneas 393, 405-406, 412+)
- Modify: `backend/src/main/resources/db/migration/V3__dev_data.sql` (añadir al final)
- Delete: `backend/src/main/resources/db/migration/V4__mr_test_seed.sql`
- Delete: `backend/src/main/resources/db/migration/V5__cr_test_seed.sql`

### Enums y Entidades
- Modify: `backend/src/main/java/co/edu/unicauca/backend/shared/enums/EstadoComanda.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/entity/Comanda.java`

### DTOs
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaDetalleResponse.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaConsultaResponse.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemVisitaResponse.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ComandaItemResponse.java`

### Mappers
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapper.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaConsultaMapper.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaMapper.java`

### Servicios
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoService.java`

### Tests
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoServiceTest.java`
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaControllerTest.java`

### Postman
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing.collection.yaml`

---

## Task 1: Consolidar migraciones V4 y V5 en V3

**Files:**
- Modify: `backend/src/main/resources/db/migration/V3__dev_data.sql`
- Delete: `backend/src/main/resources/db/migration/V4__mr_test_seed.sql`
- Delete: `backend/src/main/resources/db/migration/V5__cr_test_seed.sql`

- [ ] **Step 1: Añadir contenido de V4 y V5 al final de V3**

Abrir `V3__dev_data.sql` y añadir al final:

```sql
-- =====================================================
-- DATOS DE SOPORTE PARA TESTS POSTMAN 
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

- [ ] **Step 2: Eliminar archivos V4 y V5**

```bash
rm backend/src/main/resources/db/migration/V4__mr_test_seed.sql
rm backend/src/main/resources/db/migration/V5__cr_test_seed.sql
```

- [ ] **Step 3: Verificar migraciones**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" flyway:clean flyway:migrate
```

Expected: Migraciones V1, V2, V3 aplican exitosamente

---

## Task 2: Actualizar esquema para estado BORRADOR

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql:393,405-406,412+`

- [ ] **Step 1: Hacer comanda_fecha_hora_inicio nullable**

En `V1__init_schema.sql` línea 393, cambiar:

```sql
-- ANTES:
comanda_fecha_hora_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

-- DESPUÉS:
comanda_fecha_hora_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
```

- [ ] **Step 2: Añadir BORRADOR al constraint de estado**

En línea 405, cambiar:

```sql
-- ANTES:
CONSTRAINT chk_comanda_estado CHECK (comanda_estado IN ('PRE_RESERVA', 'PENDIENTE', 'EN_PREPARACION', 'LISTO', 'COMPLETADO')),

-- DESPUÉS:
CONSTRAINT chk_comanda_estado CHECK (comanda_estado IN ('PRE_RESERVA', 'BORRADOR', 'PENDIENTE', 'EN_PREPARACION', 'LISTO', 'COMPLETADO')),
```

- [ ] **Step 3: Actualizar constraint de fechas para permitir NULL**

En línea 406, cambiar:

```sql
-- ANTES:
CONSTRAINT chk_comanda_fechas CHECK (comanda_fecha_hora_listo IS NULL OR comanda_fecha_hora_listo >= comanda_fecha_hora_inicio)

-- DESPUÉS:
CONSTRAINT chk_comanda_fechas CHECK (comanda_fecha_hora_listo IS NULL OR comanda_fecha_hora_inicio IS NULL OR comanda_fecha_hora_listo >= comanda_fecha_hora_inicio)
```

- [ ] **Step 4: Actualizar comentarios**

En línea 412, cambiar:

```sql
-- ANTES:
COMMENT ON COLUMN Comanda.comanda_estacion IS 'NULL en estado PRE_RESERVA; se asigna al convertir a comanda activa';

-- DESPUÉS:
COMMENT ON COLUMN Comanda.comanda_estacion IS 'NULL en estado PRE_RESERVA o BORRADOR; se asigna al enviar a producción (PENDIENTE)';
```

Añadir después de línea 412:

```sql
COMMENT ON COLUMN Comanda.comanda_fecha_hora_inicio IS 'Momento en que se envió la comanda a producción; NULL mientras está en PRE_RESERVA o BORRADOR';
```

- [ ] **Step 5: Verificar migración**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" flyway:clean flyway:migrate
```

Expected: V1 aplica exitosamente con nuevos constraints

---

## Task 3: Actualizar enum EstadoComanda

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/shared/enums/EstadoComanda.java`

- [ ] **Step 1: Añadir valor BORRADOR al enum**

```java
package co.edu.unicauca.backend.shared.enums;

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

- [ ] **Step 2: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

---

## Task 4: Actualizar entidad Comanda

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/entity/Comanda.java`

- [ ] **Step 1: Actualizar Javadoc de clase**

Cambiar líneas 14-26:

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
 *
 * <p>Estrategia de índices:
 * ...
```

- [ ] **Step 2: Hacer comandaFechaHoraInicio nullable**

Cambiar líneas 94-98:

```java
/**
 * Fecha y hora en que se envió la comanda a producción.
 * {@code null} mientras está en estado {@code PRE_RESERVA} o {@code BORRADOR};
 * se asigna al pasar a {@code PENDIENTE}.
 */
@Column(name = "comanda_fecha_hora_inicio")
private LocalDateTime comandaFechaHoraInicio;
```

- [ ] **Step 3: Actualizar método @PrePersist**

Cambiar líneas 117-123:

```java
@PrePersist
protected void onCreate() {
    super.onCreate();
    // comandaFechaHoraInicio se asigna explícitamente al enviar a producción
}
```

- [ ] **Step 4: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

---

## Task 5: Añadir campos de cliente a ReservaDetalleResponse

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaDetalleResponse.java`

- [ ] **Step 1: Añadir clienteNombre después de reservaId**

Después de línea 31 (reservaId), añadir:

```java
/** Nombre completo del cliente que realizó la reserva. */
private final String clienteNombre;
```

- [ ] **Step 2: Actualizar Javadoc de clase**

Cambiar líneas 10-23:

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

- [ ] **Step 3: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

---

## Task 6: Añadir clienteTelefono a ReservaConsultaResponse

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaConsultaResponse.java`

- [ ] **Step 1: Añadir clienteTelefono después de clienteNombre**

Después de línea 19 (clienteNombre), añadir:

```java
/** Teléfono del cliente. */
private final String clienteTelefono;
```

- [ ] **Step 2: Actualizar Javadoc de clase**

Cambiar líneas 7-9:

```java
/**
 * DTO de ítem en el listado de reservas para meseros.
 * 
 * <p>Incluye información básica de la reserva y datos de contacto del cliente
 * necesarios para la operación del restaurante.
 */
```

- [ ] **Step 3: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

---

## Task 7: Actualizar ItemVisitaResponse con descripcion

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemVisitaResponse.java`

- [ ] **Step 1: Eliminar comandaItemId y añadir descripcion**

Reemplazar contenido completo:

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Ítem de la visita activa tal como se muestra al cliente en el dashboard.
 * 
 * <p>Items agrupados por (nombreProducto + descripcion) de todas las comandas.
 * El estado ("En preparación" / "Servido") se deriva del estado de la comanda más avanzada.
 */
@Getter
@Builder
public class ItemVisitaResponse {

    /** Nombre del producto. */
    private final String nombreProducto;
    
    /** Descripción o modificaciones del ítem; {@code null} si no aplica. */
    private final String descripcion;

    /** Unidades pedidas (suma de todas las comandas). */
    private final Integer cantidad;

    /**
     * Estado visible para el cliente: {@code "En preparación"} o {@code "Servido"}.
     * Derivado del {@code EstadoComanda} más avanzado.
     */
    private final String estadoItem;

    /** Precio capturado al momento del pedido; no varía si el catálogo cambia. */
    private final BigDecimal precioUnitario;

    /** {@code precioUnitario × cantidad}. */
    private final BigDecimal subtotal;
}
```

- [ ] **Step 2: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Errores de compilación en VisitaEstadoMapper (esperado, se arreglará en siguiente task)

---

## Task 8: Actualizar ComandaItemResponse con descripcion

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ComandaItemResponse.java`

- [ ] **Step 1: Añadir descripcion después de nombreProducto**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * DTO de respuesta para un ítem de comanda dentro del detalle de visita.
 * 
 * <p>Items agrupados por (nombreProducto + descripcion) de todas las comandas.
 */
@Getter
@Builder
public class ComandaItemResponse {

    /** Nombre del producto consumido. */
    private final String nombreProducto;
    
    /** Descripción o modificaciones del ítem; {@code null} si no aplica. */
    private final String descripcion;

    /** Cantidad de unidades del producto (suma de todas las comandas). */
    private final Integer cantidad;

    /** Precio unitario en el momento del pedido. */
    private final BigDecimal precioUnitario;

    /** Subtotal del ítem ({@code precioUnitario × cantidad}). */
    private final BigDecimal subtotal;
}
```

- [ ] **Step 2: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Errores de compilación en VisitaMapper (esperado, se arreglará en siguiente task)


---

## Task 9: Actualizar ReservaMapper con clienteNombre

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapper.java`

- [ ] **Step 1: Modificar toReservaDetalleResponse**

Buscar el método `toReservaDetalleResponse` y cambiar el parámetro `boolean incluirTelefono` a `boolean incluirDatosCliente`.

Añadir mapeo de clienteNombre:

```java
public ReservaDetalleResponse toReservaDetalleResponse(
        Reserva reserva, 
        boolean incluirDatosCliente) {
    
    return ReservaDetalleResponse.builder()
        .reservaId(reserva.getReservaId())
        .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
        .clienteNombre(incluirDatosCliente ? reserva.getCliente().getClienteNombre() : null)
        .clienteTelefono(incluirDatosCliente ? reserva.getCliente().getClienteTelefono() : null)
        .numeroPersonas(reserva.getReservaNumeroPersonas())
        .estado(reserva.getReservaEstado().name())
        .tipo(reserva.getReservaTipo().name())
        .zonaId(reserva.getZona() != null ? reserva.getZona().getZonaId() : null)
        .decoracionId(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionId() : null)
        .modificable(reservaValidador.puedeModificarse(reserva))
        .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
        .decoracionNombre(reserva.getDecoracion() != null ? 
            reserva.getDecoracion().getDecoracionNombre() : null)
        .notas(reserva.getReservaNotas())
        .preOrdenItems(mapearPreOrdenItems(reserva))
        .preOrdenTotal(calcularTotalPreOrden(reserva))
        .abonos(mapearAbonos(reserva))
        .totalAbonado(calcularTotalAbonado(reserva))
        .build();
}
```

- [ ] **Step 2: Actualizar llamadas en ReservaService**

Buscar en `ReservaService.java` las llamadas a `toReservaDetalleResponse`:

```java
// Cambiar de:
reservaMapper.toReservaDetalleResponse(reserva, incluirTelefono)

// A:
reservaMapper.toReservaDetalleResponse(reserva, incluirDatosCliente)
```

- [ ] **Step 3: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

---

## Task 10: Actualizar ReservaConsultaMapper con clienteTelefono

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaConsultaMapper.java`

- [ ] **Step 1: Añadir mapeo de clienteTelefono**

Buscar el método `toReservaConsultaResponse` y añadir:

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

- [ ] **Step 2: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

---

## Task 11: Implementar agrupación en VisitaEstadoMapper

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java`

- [ ] **Step 1: Actualizar toItemVisitaResponse para incluir descripcion**

Modificar el método existente:

```java
/**
 * Construye {@link ItemVisitaResponse} desde un ítem de comanda.
 *
 * @param item    ítem de comanda con producto y precio
 * @param estado  estado de la comanda padre (determina "En preparación" o "Servido")
 * @return DTO listo para serializar
 */
public ItemVisitaResponse toItemVisitaResponse(ComandaItem item, EstadoComanda estado) {
    BigDecimal subtotal = item.getComandaItemPrecio()
            .multiply(BigDecimal.valueOf(item.getComandaItemCantidad()));

    return ItemVisitaResponse.builder()
            .nombreProducto(item.getProducto().getProductoNombre())
            .descripcion(item.getComandaItemDescripcion())  // ← AÑADIR
            .cantidad(item.getComandaItemCantidad())
            .estadoItem(resolverEstadoItem(estado))
            .precioUnitario(item.getComandaItemPrecio())
            .subtotal(subtotal)
            .build();
}
```

- [ ] **Step 2: Añadir método de agrupación**

Añadir nuevo método después de `toItemVisitaResponse`:

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

- [ ] **Step 3: Añadir imports necesarios**

Añadir al inicio del archivo:

```java
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;
```

- [ ] **Step 4: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java
git commit -m "feat(mappers): añadir agrupación de items en VisitaEstadoMapper

Implementa agruparYMapearItems() que agrupa por (nombre + descripcion)
y suma cantidades de todas las comandas.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 12: Implementar agrupación en VisitaMapper

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaMapper.java`

- [ ] **Step 1: Añadir método privado de agrupación**

Después del método `toDetalle`, añadir:

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

- [ ] **Step 2: Usar agrupación en toDetalle**

Modificar el método `toDetalle` en la sección de mapeo de items (líneas 84-93):

```java
// ANTES:
List<ComandaItemResponse> itemsDto = itemsComanda.isEmpty() ? null :
    itemsComanda.stream()
        .map(d -> ComandaItemResponse.builder()
                .nombreProducto(d.getProducto().getProductoNombre())
                .cantidad(d.getComandaItemCantidad())
                .precioUnitario(d.getComandaItemPrecio())
                .subtotal(d.getComandaItemPrecio()
                        .multiply(BigDecimal.valueOf(d.getComandaItemCantidad())))
                .build())
        .collect(Collectors.toList());

// DESPUÉS:
List<ComandaItemResponse> itemsDto = itemsComanda.isEmpty() ? null :
    agruparItems(itemsComanda);
```

- [ ] **Step 3: Añadir imports necesarios**

```java
import java.util.Map;
import java.util.Comparator;
```

- [ ] **Step 4: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

---

## Task 13: Usar agrupación en VisitaEstadoService

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoService.java`

- [ ] **Step 1: Refactorizar obtenerEstadoVisitaActiva para usar agrupación**

Cambiar líneas 84-90:

```java
// ANTES:
List<ItemVisitaResponse> items = comandas.stream()
    .filter(c -> c.getComandaEstado() != EstadoComanda.PRE_RESERVA)
    .flatMap(c -> comandaItemRepository.findByComanda_ComandaId(c.getComandaId())
            .stream()
            .map(item -> visitaEstadoMapper.toItemVisitaResponse(item, c.getComandaEstado())))
    .collect(Collectors.toList());

// DESPUÉS:
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

- [ ] **Step 2: Añadir import de Comparator**

```java
import java.util.Comparator;
```

- [ ] **Step 3: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoService.java
git commit -m "feat(services): usar agrupación de items en VisitaEstadoService

Agrupa items de todas las comandas por (nombre + descripcion)
antes de retornar al cliente.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 14: Actualizar tests de VisitaEstadoService

**Files:**
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoServiceTest.java`

- [ ] **Step 1: Actualizar método helper item() para incluir descripcion**

Cambiar líneas 69-78:

```java
private ComandaItem item(Long id, Comanda comanda, String nombre, 
                         String descripcion, int qty, BigDecimal precio) {
    Producto producto = Producto.builder().productoNombre(nombre).build();
    return ComandaItem.builder()
            .comandaItemId(id)
            .comanda(comanda)
            .producto(producto)
            .comandaItemCantidad(qty)
            .comandaItemPrecio(precio)
            .comandaItemDescripcion(descripcion)  // ← AÑADIR
            .build();
}
```

- [ ] **Step 2: Actualizar llamadas existentes añadiendo null como descripcion**

Buscar todas las llamadas a `item()` y añadir `null` como tercer parámetro:

```java
// ANTES:
ComandaItem i1 = item(100L, c1, "Bandeja", 2, new BigDecimal("18000"));

// DESPUÉS:
ComandaItem i1 = item(100L, c1, "Bandeja", null, 2, new BigDecimal("18000"));
```

- [ ] **Step 3: Añadir test para agrupación de items duplicados**

Añadir al final de la clase `ObtenerEstadoVisitaActiva`:

```java
@Test
@DisplayName("agrupa ítems con mismo nombre y descripción de diferentes comandas")
void agrupaItemsDuplicados() {
    Visita visita = visitaActiva();
    Mesa mesa = Mesa.builder().visitaId(VISITA_ID).mesaIdentificador("T-01").build();
    
    Comanda c1 = comanda(1L, EstadoComanda.EN_PREPARACION);
    Comanda c2 = comanda(2L, EstadoComanda.LISTO);
    
    // Mismo producto con misma descripción en 2 comandas
    ComandaItem i1 = item(100L, c1, "Pollo", "al limón", 5, new BigDecimal("15000"));
    ComandaItem i2 = item(101L, c2, "Pollo", "al limón", 3, new BigDecimal("15000"));

    when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(Optional.of(visita));
    when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
    when(comandaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(List.of(c1, c2));
    when(comandaItemRepository.findByComanda_ComandaId(1L)).thenReturn(List.of(i1));
    when(comandaItemRepository.findByComanda_ComandaId(2L)).thenReturn(List.of(i2));
    when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
            VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
            .thenReturn(Optional.empty());

    EstadoVisitaResponse res = visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL);

    // VERIFICAR: 1 item agrupado con cantidad 8
    assertThat(res.getItems()).hasSize(1);
    assertThat(res.getItems().get(0).getNombreProducto()).isEqualTo("Pollo");
    assertThat(res.getItems().get(0).getDescripcion()).isEqualTo("al limón");
    assertThat(res.getItems().get(0).getCantidad()).isEqualTo(8);
    assertThat(res.getItems().get(0).getSubtotal())
        .isEqualByComparingTo(new BigDecimal("120000"));  // 15000 * 8
}

@Test
@DisplayName("NO agrupa ítems con mismo nombre pero diferente descripción")
void noAgrupaItemsConDiferenteDescripcion() {
    Visita visita = visitaActiva();
    Mesa mesa = Mesa.builder().visitaId(VISITA_ID).mesaIdentificador("T-01").build();
    
    Comanda c1 = comanda(1L, EstadoComanda.EN_PREPARACION);
    
    // Mismo producto con DIFERENTE descripción
    ComandaItem i1 = item(100L, c1, "Pollo", "al limón", 5, new BigDecimal("15000"));
    ComandaItem i2 = item(101L, c1, "Pollo", "a la naranja", 3, new BigDecimal("15000"));

    when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(Optional.of(visita));
    when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
    when(comandaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(List.of(c1));
    when(comandaItemRepository.findByComanda_ComandaId(1L)).thenReturn(List.of(i1, i2));
    when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
            VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
            .thenReturn(Optional.empty());

    EstadoVisitaResponse res = visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL);

    // VERIFICAR: 2 items separados
    assertThat(res.getItems()).hasSize(2);
    assertThat(res.getItems().get(0).getDescripcion()).isIn("al limón", "a la naranja");
    assertThat(res.getItems().get(1).getDescripcion()).isIn("al limón", "a la naranja");
    assertThat(res.getItems().get(0).getDescripcion())
        .isNotEqualTo(res.getItems().get(1).getDescripcion());
}
```

- [ ] **Step 4: Ejecutar tests**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" test -Dtest=VisitaEstadoServiceTest
```

Expected: Todos los tests pasan

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoServiceTest.java
git commit -m "test: actualizar VisitaEstadoServiceTest para agrupación

- Añadir descripcion a método helper item()
- Actualizar llamadas existentes
- Añadir tests de agrupación por (nombre + descripcion)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 15: Actualizar tests de VisitaController

**Files:**
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaControllerTest.java`

- [ ] **Step 1: Actualizar builder de ItemVisitaResponse en mocks**

Buscar líneas con `ItemVisitaResponse.builder()` y actualizar:

```java
// ANTES:
.items(List.of(ItemVisitaResponse.builder()
        .comandaItemId(1L)
        .nombreProducto("Bandeja")
        .cantidad(2)
        .estadoItem("En preparación")
        .precioUnitario(new BigDecimal("18000"))
        .subtotal(new BigDecimal("36000"))
        .build()))

// DESPUÉS:
.items(List.of(ItemVisitaResponse.builder()
        .nombreProducto("Bandeja")
        .descripcion(null)  // ← AÑADIR
        .cantidad(2)
        .estadoItem("En preparación")
        .precioUnitario(new BigDecimal("18000"))
        .subtotal(new BigDecimal("36000"))
        .build()))
```

- [ ] **Step 2: Ejecutar tests**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" test -Dtest=VisitaControllerTest
```

Expected: Todos los tests pasan

---

## Task 16: Verificación integral de tests

**Files:** N/A

- [ ] **Step 1: Ejecutar toda la suite de tests**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" test
```

Expected: 152+ tests pasan

- [ ] **Step 2: Verificar que la app compila y arranca**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean package -DskipTests
```

Expected: JAR generado exitosamente

---

## Task 17: Crear carpeta Postman para pruebas manuales

**Files:**
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing.collection.yaml`

- [ ] **Step 1: Crear directorio**

```bash
mkdir -p backend/postman/postman/collections/manual-testing
```

- [ ] **Step 2: Crear colección base**

Crear archivo `Al Toro - Manual Testing.collection.yaml`:

```yaml
name: Al Toro - Manual Testing
description: Colección para pruebas manuales del flujo completo con login automático
requests:
  # ============================================
  # AUTH
  # ============================================
  - name: Login Cliente
    request:
      method: POST
      url: "{{baseUrl}}/api/auth/login"
      headers:
        - key: Content-Type
          value: application/json
      body:
        mode: raw
        raw: |
          {
            "email": "{{emailCliente}}",
            "password": "{{passwordValida}}",
            "forceSessionOverride": true
          }
    scripts:
      - type: afterResponse
        code: |
          if (pm.response.code === 200) {
            const token = pm.response.json().accessToken;
            pm.environment.set('clienteToken', token);
            console.log('✓ Token de cliente guardado');
          }

  - name: Login Mesero
    request:
      method: POST
      url: "{{baseUrl}}/api/auth/login"
      headers:
        - key: Content-Type
          value: application/json
      body:
        mode: raw
        raw: |
          {
            "email": "{{emailMesero}}",
            "password": "{{passwordValida}}",
            "forceSessionOverride": true
          }
    scripts:
      - type: afterResponse
        code: |
          if (pm.response.code === 200) {
            const token = pm.response.json().accessToken;
            pm.environment.set('meseroToken', token);
            console.log('✓ Token de mesero guardado');
          }

  - name: Login Cajero
    request:
      method: POST
      url: "{{baseUrl}}/api/auth/login"
      headers:
        - key: Content-Type
          value: application/json
      body:
        mode: raw
        raw: |
          {
            "email": "{{emailCajero}}",
            "password": "{{passwordValida}}",
            "forceSessionOverride": true
          }
    scripts:
      - type: afterResponse
        code: |
          if (pm.response.code === 200) {
            const token = pm.response.json().accessToken;
            pm.environment.set('cajeroToken', token);
            console.log('✓ Token de cajero guardado');
          }

  # ============================================
  # RESERVAS - Cliente
  # ============================================
  - name: GET Disponibilidad
    request:
      method: GET
      url: "{{baseUrl}}/api/reservas/disponibilidad?fechaHora={{fechaHoraFutura}}"
      headers:
        - key: Authorization
          value: "Bearer {{clienteToken}}"
    scripts:
      - type: beforeRequest
        code: |
          // Auto-login si no hay token
          if (!pm.environment.get('clienteToken')) {
            pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({
                email: pm.environment.get('emailCliente'),
                password: pm.environment.get('passwordValida'),
                forceSessionOverride: true
              })}
            }, (err, res) => {
              if (!err && res.code === 200) {
                pm.environment.set('clienteToken', res.json().accessToken);
              }
            });
          }

  - name: POST Crear Reserva (sin pre-orden)
    request:
      method: POST
      url: "{{baseUrl}}/api/reservas"
      headers:
        - key: Authorization
          value: "Bearer {{clienteToken}}"
        - key: Content-Type
          value: application/json
      body:
        mode: raw
        raw: |
          {
            "emailCliente": "{{emailCliente}}",
            "fechaHoraLlegada": "{{fechaHoraFutura}}",
            "numeroPersonas": 4,
            "zonaId": null,
            "decoracionId": null,
            "notas": "Prueba manual",
            "preOrden": null
          }
    scripts:
      - type: beforeRequest
        code: |
          if (!pm.environment.get('clienteToken')) {
            pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({
                email: pm.environment.get('emailCliente'),
                password: pm.environment.get('passwordValida'),
                forceSessionOverride: true
              })}
            }, (err, res) => {
              if (!err && res.code === 200) {
                pm.environment.set('clienteToken', res.json().accessToken);
              }
            });
          }
      - type: afterResponse
        code: |
          if (pm.response.code === 201) {
            const reservaId = pm.response.json().data.reservaId;
            pm.environment.set('lastReservaId', reservaId);
            console.log('✓ Reserva creada:', reservaId);
          }

  - name: GET Reservas Futuras
    request:
      method: GET
      url: "{{baseUrl}}/api/reservas/cliente/futuras?emailCliente={{emailCliente}}"
      headers:
        - key: Authorization
          value: "Bearer {{clienteToken}}"
    scripts:
      - type: beforeRequest
        code: |
          if (!pm.environment.get('clienteToken')) {
            pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({
                email: pm.environment.get('emailCliente'),
                password: pm.environment.get('passwordValida'),
                forceSessionOverride: true
              })}
            }, (err, res) => {
              if (!err && res.code === 200) {
                pm.environment.set('clienteToken', res.json().accessToken);
              }
            });
          }

  - name: GET Detalle de Reserva
    request:
      method: GET
      url: "{{baseUrl}}/api/reservas/{{lastReservaId}}/detalle"
      headers:
        - key: Authorization
          value: "Bearer {{clienteToken}}"
    scripts:
      - type: beforeRequest
        code: |
          if (!pm.environment.get('clienteToken')) {
            pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({
                email: pm.environment.get('emailCliente'),
                password: pm.environment.get('passwordValida'),
                forceSessionOverride: true
              })}
            }, (err, res) => {
              if (!err && res.code === 200) {
                pm.environment.set('clienteToken', res.json().accessToken);
              }
            });
          }

  # ============================================
  # RESERVAS - Mesero
  # ============================================
  - name: GET Consulta Reservas (Mesero)
    request:
      method: GET
      url: "{{baseUrl}}/api/reservas/mesero/consulta"
      headers:
        - key: Authorization
          value: "Bearer {{meseroToken}}"
    scripts:
      - type: beforeRequest
        code: |
          if (!pm.environment.get('meseroToken')) {
            pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({
                email: pm.environment.get('emailMesero'),
                password: pm.environment.get('passwordValida'),
                forceSessionOverride: true
              })}
            }, (err, res) => {
              if (!err && res.code === 200) {
                pm.environment.set('meseroToken', res.json().accessToken);
              }
            });
          }

  # ============================================
  # VISITAS
  # ============================================
  - name: GET Visita Activa (Cliente)
    request:
      method: GET
      url: "{{baseUrl}}/api/visitas/activa"
      headers:
        - key: Authorization
          value: "Bearer {{clienteToken}}"
    scripts:
      - type: beforeRequest
        code: |
          if (!pm.environment.get('clienteToken')) {
            pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({
                email: pm.environment.get('emailCliente'),
                password: pm.environment.get('passwordValida'),
                forceSessionOverride: true
              })}
            }, (err, res) => {
              if (!err && res.code === 200) {
                pm.environment.set('clienteToken', res.json().accessToken);
              }
            });
          }

  - name: GET Historial Visitas
    request:
      method: GET
      url: "{{baseUrl}}/api/visitas/cliente/historial?emailCliente={{emailCliente}}"
      headers:
        - key: Authorization
          value: "Bearer {{clienteToken}}"
    scripts:
      - type: beforeRequest
        code: |
          if (!pm.environment.get('clienteToken')) {
            pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({
                email: pm.environment.get('emailCliente'),
                password: pm.environment.get('passwordValida'),
                forceSessionOverride: true
              })}
            }, (err, res) => {
              if (!err && res.code === 200) {
                pm.environment.set('clienteToken', res.json().accessToken);
              }
            });
          }

  # ============================================
  # PRODUCTOS
  # ============================================
  - name: GET Carta
    request:
      method: GET
      url: "{{baseUrl}}/api/productos/carta"
      headers:
        - key: Authorization
          value: "Bearer {{clienteToken}}"
    scripts:
      - type: beforeRequest
        code: |
          if (!pm.environment.get('clienteToken')) {
            pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({
                email: pm.environment.get('emailCliente'),
                password: pm.environment.get('passwordValida'),
                forceSessionOverride: true
              })}
            }, (err, res) => {
              if (!err && res.code === 200) {
                pm.environment.set('clienteToken', res.json().accessToken);
              }
            });
          }

  - name: GET Menú Especial
    request:
      method: GET
      url: "{{baseUrl}}/api/productos/menu-especial"
      headers:
        - key: Authorization
          value: "Bearer {{clienteToken}}"
    scripts:
      - type: beforeRequest
        code: |
          if (!pm.environment.get('clienteToken')) {
            pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({
                email: pm.environment.get('emailCliente'),
                password: pm.environment.get('passwordValida'),
                forceSessionOverride: true
              })}
            }, (err, res) => {
              if (!err && res.code === 200) {
                pm.environment.set('clienteToken', res.json().accessToken);
              }
            });
          }

  # ============================================
  # PUNTOS
  # ============================================
  - name: GET Mis Puntos
    request:
      method: GET
      url: "{{baseUrl}}/api/clientes/me/puntos?emailCliente={{emailCliente}}"
      headers:
        - key: Authorization
          value: "Bearer {{clienteToken}}"
    scripts:
      - type: beforeRequest
        code: |
          if (!pm.environment.get('clienteToken')) {
            pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({
                email: pm.environment.get('emailCliente'),
                password: pm.environment.get('passwordValida'),
                forceSessionOverride: true
              })}
            }, (err, res) => {
              if (!err && res.code === 200) {
                pm.environment.set('clienteToken', res.json().accessToken);
              }
            });
          }
```

- [ ] **Step 3: Commit**

```bash
git add backend/postman/postman/collections/manual-testing/
git commit -m "feat(postman): añadir colección para pruebas manuales

Colección con login automático en beforeRequest para testing
manual del flujo completo. Incluye endpoints de:
- Auth (login por rol)
- Reservas (cliente y mesero)
- Visitas
- Productos
- Puntos

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 18: Documentación de uso de colección manual

**Files:**
- Create: `backend/postman/postman/collections/manual-testing/README.md`

- [ ] **Step 1: Crear README**

```markdown
# Colección de Pruebas Manuales - Al Toro Gastrobar

Esta colección está diseñada para pruebas manuales del flujo completo con **login automático** en cada request.

## Características

✅ **Auto-login**: Cada endpoint tiene un `beforeRequest` script que hace login automáticamente si no hay token  
✅ **Sin afterRequest tests**: Solo para pruebas manuales, sin validaciones automáticas  
✅ **Flujo completo**: Cubre todos los módulos principales del sistema  

## Configuración

### Variables de Entorno Necesarias

Asegúrate de tener estas variables en tu entorno `Al Toro - Local`:

```yaml
baseUrl: http://localhost:8080
emailCliente: cliente@altoro.com
emailMesero: mesero@altoro.com
emailCajero: cajero@altoro.com
passwordValida: <password-común-para-todos>
fechaHoraFutura: 2026-12-25T19:00:00
```

## Uso

### 1. Login Manual (Opcional)

Aunque cada endpoint hace login automático, puedes ejecutar los requests de login manualmente para verificar credenciales:

- `Login Cliente`
- `Login Mesero`
- `Login Cajero`

Esto guardará los tokens en variables de entorno.

### 2. Flujo de Cliente

1. `GET Disponibilidad` - Ver zonas y decoraciones disponibles
2. `POST Crear Reserva` - Crear una reserva (guarda ID en `lastReservaId`)
3. `GET Reservas Futuras` - Ver todas las reservas futuras del cliente
4. `GET Detalle de Reserva` - Ver detalle de la última reserva creada
5. `GET Visita Activa` - Ver estado de la visita actual (si hay una activa)
6. `GET Historial Visitas` - Ver historial de visitas pasadas
7. `GET Carta` - Ver productos disponibles
8. `GET Menú Especial` - Ver menús especiales con opciones
9. `GET Mis Puntos` - Ver puntos de lealtad

### 3. Flujo de Mesero

1. `Login Mesero` (manual)
2. `GET Consulta Reservas` - Ver reservas del día

### 4. Validaciones Manuales

Después de cada request, verifica en la respuesta:

**Para endpoints con agrupación de items:**
- `GET Visita Activa`:
  - Items agrupados por (nombreProducto + descripcion)
  - Campo `descripcion` presente (puede ser null)
  - NO debe haber `comandaItemId`
  
**Para endpoints con datos de cliente:**
- `GET Detalle de Reserva` (como mesero):
  - `clienteNombre` presente
  - `clienteTelefono` presente
- `GET Consulta Reservas`:
  - `clienteTelefono` presente en cada reserva

## Verificación de Cambios Implementados

### ✅ Agrupación de Items

**Probar:** Crear visita con 2 comandas que tengan el mismo producto

```
Comanda 1: 5x Pollo al limón
Comanda 2: 3x Pollo al limón
```

**Verificar en `GET Visita Activa`:**
```json
{
  "items": [
    {
      "nombreProducto": "Pollo",
      "descripcion": "al limón",
      "cantidad": 8,  // ← Suma de 5 + 3
      "precioUnitario": 15000,
      "subtotal": 120000
    }
  ]
}
```

### ✅ Descripcion en Items

**Probar:** Crear items con y sin descripción

**Verificar:**
- Items con descripción: `"descripcion": "al limón"`
- Items sin descripción: `"descripcion": null`

### ✅ Datos de Cliente

**Probar como mesero:** `GET Detalle de Reserva`

**Verificar:**
```json
{
  "clienteNombre": "Juan Pérez",
  "clienteTelefono": "3001234567",
  ...
}
```

## Notas

- Los tokens se regeneran automáticamente si expiran
- `lastReservaId` se actualiza al crear una reserva
- Todos los endpoints usan el token correspondiente a su rol

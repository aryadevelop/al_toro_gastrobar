# DTOs, Agrupación y Migraciones Base - Plan de Implementación (v2 - CORREGIDO)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mejorar DTOs con información de cliente, implementar agrupación de items SOLO en detalle de visita, y preparar migraciones base para estado BORRADOR

**Architecture:** Refactoring incremental sin cambiar lógica de negocio de comandas. Agrupación SOLO en vista histórica de cliente, NO en estado activo

**Tech Stack:** Spring Boot 3.5, PostgreSQL 15, Flyway, JUnit 5, AssertJ, Postman

**CAMBIOS CRÍTICOS vs v1:** 
- `ItemVisitaResponse` (GET /api/visitas/activa) → MANTIENE `comandaItemId`, AÑADE `descripcion`, **NO** agrupa
- `ComandaItemResponse` (GET /api/visitas/cliente/{id}/detalle) → AÑADE `descripcion`, **SÍ** agrupa
- `VisitaEstadoMapper` y `VisitaEstadoService` → **NO** se modifican para agrupación
- **NUEVO:** Ordenamiento por categoría (PLATO → BEBIDA → OTRO) en todos los mappers/servicios que listan items

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
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemVisitaResponse.java` ← CAMBIO: mantiene comandaItemId
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ComandaItemResponse.java`

### Mappers
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapper.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaConsultaMapper.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/PreOrdenMapper.java` ← Añade ordenamiento por categoría
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java` ← CAMBIO: solo añade descripcion, NO agrupación
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaMapper.java` ← Implementa agrupación + ordenamiento

### Servicios
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoService.java` ← Añade ordenamiento por categoría

### Tests
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoServiceTest.java`
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaMapperTest.java`

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

- [ ] **Step 3: Verificar checksum**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" flyway:clean flyway:migrate
```

Expected: Migraciones ejecutadas sin error, seed consolidado en V3

- [ ] **Step 4: Commit**

esperar commit del usurio
git commit -m "refactor(migrations): consolidar V4 y V5 en V3__dev_data"

---

## Task 2: Actualizar V1 para soportar estado BORRADOR

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`

- [ ] **Step 1: Hacer comandaFechaHoraInicio nullable**

Línea 393, eliminar `NOT NULL`:

```sql
-- ANTES:
comanda_fecha_hora_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

-- DESPUÉS:
comanda_fecha_hora_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
```

- [ ] **Step 2: Añadir BORRADOR al CHECK de estado**

Línea 405, añadir 'BORRADOR':

```sql
-- ANTES:
CONSTRAINT chk_comanda_estado CHECK (comanda_estado IN ('PRE_RESERVA', 'PENDIENTE', 'EN_PREPARACION', 'LISTO', 'COMPLETADO')),

-- DESPUÉS:
CONSTRAINT chk_comanda_estado CHECK (comanda_estado IN ('PRE_RESERVA', 'BORRADOR', 'PENDIENTE', 'EN_PREPARACION', 'LISTO', 'COMPLETADO')),
```

- [ ] **Step 3: Actualizar CHECK de comandaFechaHoraListo para permitir inicio NULL**

Línea 406:

```sql
-- ANTES:
CONSTRAINT chk_comanda_fecha_hora_listo CHECK (comanda_fecha_hora_listo IS NULL OR comanda_fecha_hora_listo >= comanda_fecha_hora_inicio),

-- DESPUÉS:
CONSTRAINT chk_comanda_fecha_hora_listo CHECK (comanda_fecha_hora_listo IS NULL OR comanda_fecha_hora_inicio IS NULL OR comanda_fecha_hora_listo >= comanda_fecha_hora_inicio),
```

- [ ] **Step 4: Verificar no hay más DEFAULT CURRENT_TIMESTAMP**

Buscar en el archivo si hay otros usos de DEFAULT para comandaFechaHoraInicio y eliminarlos si existen.

- [ ] **Step 5: Resetear DB y verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" flyway:clean flyway:migrate
```

Expected: Migraciones ejecutadas sin error

- [ ] **Step 6: Commit**

esperar commit del usurio
git commit -m "feat(migrations): soportar estado BORRADOR en comandas"

---

## Task 3: Actualizar enum EstadoComanda

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/shared/enums/EstadoComanda.java`

- [ ] **Step 1: Añadir BORRADOR después de PRE_RESERVA**

```java
package co.edu.unicauca.backend.shared.enums;

/**
 * Estados del ciclo de vida de una comanda.
 * 
 * <p>Flujo típico: PRE_RESERVA → BORRADOR → PENDIENTE → EN_PREPARACION → LISTO → COMPLETADO
 * 
 * <ul>
 *   <li><b>PRE_RESERVA:</b> Comanda creada desde reserva, aún no enviada a producción</li>
 *   <li><b>BORRADOR:</b> Comanda walk-in guardada pero no enviada a producción</li>
 *   <li><b>PENDIENTE:</b> Enviada a cocina/barra, esperando preparación</li>
 *   <li><b>EN_PREPARACION:</b> Estación trabajando en los items</li>
 *   <li><b>LISTO:</b> Terminado, pendiente de marcar servido</li>
 *   <li><b>COMPLETADO:</b> Servido al cliente</li>
 * </ul>
 */
public enum EstadoComanda {
    
    PRE_RESERVA("Pre-Reserva"),
    
    BORRADOR("Borrador"),
    
    PENDIENTE("Pendiente"),
    
    EN_PREPARACION("En Preparación"),
    
    LISTO("Listo"),
    
    COMPLETADO("Completado");

    private final String displayName;

    EstadoComanda(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

- [ ] **Step 2: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

- [ ] **Step 3: Commit**

esperar commit del usurio
git commit -m "feat(enums): añadir estado BORRADOR a EstadoComanda"


---

## Task 4: Actualizar entity Comanda

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/entity/Comanda.java`

- [ ] **Step 1: Hacer comandaFechaHoraInicio nullable**

Eliminar anotación `@NotNull` y actualizar Javadoc:

```java
/**
 * Fecha y hora en que la comanda fue enviada a producción (cocina/barra).
 * 
 * <p>{@code NULL} mientras la comanda está en estado {@code PRE_RESERVA} o {@code BORRADOR}.
 * Se asigna automáticamente cuando la comanda transiciona a {@code PENDIENTE}.
 */
@Column(name = "comanda_fecha_hora_inicio")
private LocalDateTime comandaFechaHoraInicio;
```

- [ ] **Step 2: Eliminar asignación automática en @PrePersist**

Eliminar o comentar la línea que asigna `comandaFechaHoraInicio` en el método `onCreate()`:

```java
@PrePersist
protected void onCreate() {
    super.onCreate();
    // comandaFechaHoraInicio se asigna explícitamente al enviar a producción
}
```

- [ ] **Step 3: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

- [ ] **Step 4: Commit**

esperar commit del usurio
git commit -m "feat(entities): comandaFechaHoraInicio nullable para BORRADOR"

---

## Task 5: Añadir clienteNombre a ReservaDetalleResponse

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaDetalleResponse.java`

- [ ] **Step 1: Añadir campo clienteNombre**

Después de `reservaId` (línea 31), añadir:

```java
/** Nombre del cliente; {@code null} en consultas de cliente autenticado. */
private final String clienteNombre;
```

- [ ] **Step 2: Actualizar Javadoc de clienteTelefono**

Línea 36, aclarar que también es null para cliente:

```java
/** Teléfono del cliente; {@code null} en consultas de cliente autenticado. */
private final String clienteTelefono;
```

- [ ] **Step 3: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Errores de compilación en ReservaMapper (esperado, se arreglará en Task 9)

---

## Task 6: Añadir clienteTelefono a ReservaConsultaResponse

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaConsultaResponse.java`

- [ ] **Step 1: Añadir campo después de numeroPersonas**

Alrededor de línea 54, después de `numeroPersonas`:

```java
/** Teléfono del cliente que realizó la reserva. */
private final String clienteTelefono;
```

- [ ] **Step 2: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Errores en ReservaConsultaMapper (esperado, se arreglará en Task 10)

---

## Task 7: Actualizar ItemVisitaResponse (MANTENER comandaItemId, AÑADIR descripcion)

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemVisitaResponse.java`

- [ ] **Step 1: Actualizar Javadoc de clase**

```java
/**
 * Ítem de la visita activa tal como se muestra al cliente en el dashboard.
 * 
 * <p>Items individuales de cada comanda, SIN agrupar. Cada ítem muestra su
 * estado real según la comanda padre. El cliente ve el progreso item por item.
 */
```

- [ ] **Step 2: Añadir campo descripcion después de nombreProducto**

```java
/** Identificador del ítem de comanda. */
private final Long comandaItemId;

/** Nombre del producto. */
private final String nombreProducto;

/** Descripción o modificaciones del ítem; {@code null} si no aplica. */
private final String descripcion;

/** Unidades pedidas. */
private final Integer cantidad;
```

- [ ] **Step 3: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Errores de compilación en VisitaEstadoMapper (esperado, se arreglará en Task 11)

---

## Task 8: Actualizar ComandaItemResponse con descripcion

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ComandaItemResponse.java`

- [ ] **Step 1: Actualizar Javadoc de clase**

```java
/**
 * DTO de respuesta para un ítem de comanda dentro del detalle de visita.
 * 
 * <p>Items agrupados por (nombreProducto + descripcion) de todas las comandas.
 * La cantidad representa la suma de items idénticos a través de todas las comandas.
 */
```

- [ ] **Step 2: Añadir descripcion después de nombreProducto**

```java
/** Nombre del producto consumido. */
private final String nombreProducto;

/** Descripción o modificaciones del ítem; {@code null} si no aplica. */
private final String descripcion;

/** Cantidad de unidades del producto (suma de todas las comandas). */
private final Integer cantidad;
```

- [ ] **Step 3: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Errores de compilación en VisitaMapper (esperado, se arreglará en Task 12)

---

## Task 9: Actualizar ReservaMapper con clienteNombre

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapper.java`

- [ ] **Step 1: Añadir clienteNombre en toDetalle()**

En el método `toDetalle()`, encontrar el builder de `ReservaDetalleResponse` y añadir:

```java
.clienteNombre(reserva.getCliente().getClienteNombre())
```

Justo después de `.reservaId(...)` y antes de `.fechaHoraLlegada(...)`

- [ ] **Step 2: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

---

## Task 10: Actualizar ReservaConsultaMapper con clienteTelefono

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaConsultaMapper.java`

- [ ] **Step 1: Añadir clienteTelefono en toConsultaResponse()**

En el builder, añadir:

```java
.clienteTelefono(reserva.getCliente().getClienteTelefono())
```

Después de `.numeroPersonas(...)` y antes de `.estadoReserva(...)`

- [ ] **Step 2: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

---

## Task 11: Actualizar VisitaEstadoMapper con descripcion (SIN agrupación)

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java`

- [ ] **Step 1: Añadir descripcion en toItemVisitaResponse()**

Modificar el builder para incluir descripcion:

```java
public ItemVisitaResponse toItemVisitaResponse(ComandaItem item, EstadoComanda estado) {
    BigDecimal subtotal = item.getComandaItemPrecio()
            .multiply(BigDecimal.valueOf(item.getComandaItemCantidad()));

    return ItemVisitaResponse.builder()
            .comandaItemId(item.getComandaItemId())
            .nombreProducto(item.getProducto().getProductoNombre())
            .descripcion(item.getComandaItemDescripcion())  // ← AÑADIR
            .cantidad(item.getComandaItemCantidad())
            .estadoItem(resolverEstadoItem(estado))
            .precioUnitario(item.getComandaItemPrecio())
            .subtotal(subtotal)
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

## Task 12: Implementar agrupación SOLO en VisitaMapper

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaMapper.java`

- [ ] **Step 1: Añadir imports necesarios**

Al inicio del archivo, después de los imports existentes:

```java
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;
```

- [ ] **Step 2: Añadir método privado de agrupación**

Después del método `toDetalle`, añadir:

```java
/**
 * Agrupa items de comanda por (nombreProducto + descripcion) y suma cantidades.
 * 
 * <p>Utilizado en detalle de visita para mostrar al cliente una vista consolidada
 * de todos los items pedidos, sin importar en cuántas comandas se dividieron.
 * 
 * @param items lista de items a agrupar
 * @return lista de items agrupados ordenados por nombre
 */
private List<ComandaItemResponse> agruparItems(List<ComandaItem> items) {
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

- [ ] **Step 3: Modificar toDetalle para usar agrupación**

En el método `toDetalle()`, reemplazar el mapeo inline de items por la llamada al nuevo método:

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
List<ComandaItemResponse> itemsDto = itemsComanda.isEmpty() ? null : agruparItems(itemsComanda);
```

- [ ] **Step 4: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

---

## Task 13: Actualizar tests baseline - VisitaEstadoServiceTest

**Files:**
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoServiceTest.java`

- [ ] **Step 1: Ejecutar tests actuales para capturar baseline**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" test -Dtest=VisitaEstadoServiceTest
```

Expected: Tests PASAN (actualmente NO hay agrupación, esto es correcto)

- [ ] **Step 2: Verificar que NO se rompió nada**

Si todos los tests pasan, no hay cambios necesarios en este archivo. La adición de `descripcion` no rompe tests existentes porque:
- El mapper sigue retornando items individuales
- Solo añadimos un campo nuevo que puede ser null

---

## Task 14: Actualizar tests - VisitaMapperTest para agrupación

**Files:**
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaMapperTest.java`

- [ ] **Step 1: Añadir test para agrupación de items**

Al final de la clase, añadir:

```java
@Test
@DisplayName("Agrupa items duplicados por nombre y descripcion en detalle de visita")
void agrupaItemsDuplicadosEnDetalle() {
    // Arrange: 2 items del mismo producto con misma descripcion
    ComandaItem item1 = ComandaItem.builder()
        .comandaItemId(1L)
        .producto(producto)
        .comandaItemCantidad(2)
        .comandaItemPrecio(new BigDecimal("15000"))
        .comandaItemDescripcion("Sin cebolla")
        .build();
    
    ComandaItem item2 = ComandaItem.builder()
        .comandaItemId(2L)
        .producto(producto)
        .comandaItemCantidad(3)
        .comandaItemPrecio(new BigDecimal("15000"))
        .comandaItemDescripcion("Sin cebolla")
        .build();
    
    List<ComandaItem> items = List.of(item1, item2);
    
    // Act
    VisitaDetalleResponse response = visitaMapper.toDetalle(
        visita, items, Optional.empty(), Optional.empty(), Optional.empty()
    );
    
    // Assert
    assertThat(response.getItemsComanda()).hasSize(1);
    ComandaItemResponse itemAgrupado = response.getItemsComanda().get(0);
    assertThat(itemAgrupado.getNombreProducto()).isEqualTo("Bandeja Paisa");
    assertThat(itemAgrupado.getDescripcion()).isEqualTo("Sin cebolla");
    assertThat(itemAgrupado.getCantidad()).isEqualTo(5);  // 2 + 3
    assertThat(itemAgrupado.getSubtotal()).isEqualByComparingTo(new BigDecimal("75000")); // 15000 * 5
}

@Test
@DisplayName("No agrupa items con diferente descripcion en detalle de visita")
void noAgrupaItemsConDiferenteDescripcion() {
    // Arrange: 2 items del mismo producto con DIFERENTE descripcion
    ComandaItem item1 = ComandaItem.builder()
        .comandaItemId(1L)
        .producto(producto)
        .comandaItemCantidad(2)
        .comandaItemPrecio(new BigDecimal("15000"))
        .comandaItemDescripcion("Sin cebolla")
        .build();
    
    ComandaItem item2 = ComandaItem.builder()
        .comandaItemId(2L)
        .producto(producto)
        .comandaItemCantidad(3)
        .comandaItemPrecio(new BigDecimal("15000"))
        .comandaItemDescripcion("Extra picante")
        .build();
    
    List<ComandaItem> items = List.of(item1, item2);
    
    // Act
    VisitaDetalleResponse response = visitaMapper.toDetalle(
        visita, items, Optional.empty(), Optional.empty(), Optional.empty()
    );
    
    // Assert: NO se agrupan, quedan 2 items separados
    assertThat(response.getItemsComanda()).hasSize(2);
}
```

- [ ] **Step 2: Ejecutar tests**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" test -Dtest=VisitaMapperTest
```

Expected: Todos los tests PASAN, incluyendo los 2 nuevos

- [ ] **Step 3: Commit**

esperar commit del usurio
git commit -m "test(mappers): añadir tests de agrupación en VisitaMapper"

---

## Task 15: Ejecutar suite completa de tests

**Files:**
- None (verification task)

- [ ] **Step 1: Ejecutar todos los tests del backend**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" test
```

Expected: 152+ tests PASAN

- [ ] **Step 2: Si algún test falla, identificar el problema**

Revisar el output de Maven para identificar:
- ¿Qué test falló?
- ¿Cuál es el error específico?
- ¿Es por falta de campo descripcion en algún DTO?

- [ ] **Step 3: Fix si es necesario**

Si hay fallos relacionados con campos faltantes, añadir el campo en el builder correspondiente.

- [ ] **Step 4: Commit si hubo fixes**

esperar commit del usurio
git commit -m "fix(tests): corregir tests rotos por cambios en DTOs"

---

## Task 16: Crear colección Postman para pruebas manuales

**Files:**
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing.collection.yaml`

- [ ] **Step 1: Crear directorio**

```bash
mkdir -p "backend/postman/postman/collections/manual-testing"
```

- [ ] **Step 2: Crear archivo de colección**

Crear `Al Toro - Manual Testing.collection.yaml`:

```yaml
$kind: collection
name: Al Toro - Manual Testing
description: |-
  Colección para pruebas manuales exploratorias del backend Al Toro Gastrobar.
  
  **Propósito:** Verificar manualmente flujos end-to-end, explorar respuestas,
  y validar comportamiento sin assertions automáticas.
  
  **Uso:** Ejecutar requests individualmente en Postman for VS Code o Postman Desktop.
  Cada request incluye login automático en beforeRequest para ejecutarse de forma aislada.
  
  **Módulos cubiertos:**
  - Autenticación (login, refresh, logout, me)
  - Reservas (crear, modificar, cancelar, consultar - cliente y mesero)
  - Visitas (historial, detalle, estado activa, asistencia)
  - Productos (carta, menú especial)
  - Puntos (consultar, canjear)
  
  **NO incluye:** Test assertions (pm.test). Solo para exploración manual y debugging.
version: 1.0.0
```

- [ ] **Step 3: Crear carpeta .resources**

```bash
mkdir -p "backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/.resources"
```

- [ ] **Step 4: Crear definition.yaml**

Crear `.resources/definition.yaml`:

```yaml
$kind: collection-definition
description: |-
  Colección de pruebas manuales sin assertions automáticas.
  
  Cada request ejecuta login autónomo en beforeRequest usando:
  - emailCliente / emailMesero / emailCajero / emailAdmin
  - passwordValida
  
  Variables de ambiente utilizadas: baseUrl, todos los emails de roles, passwordValida.
```

---

## Task 17: Crear requests base en colección manual

**Files:**
- Create: Multiple request files in `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/`

- [ ] **Step 1: Crear request de login**

`00-01 Login CLIENTE.request.yaml`:

```yaml
$kind: http-request
name: 00-01 Login CLIENTE
description: Login manual como cliente para obtener tokens
url: "{{baseUrl}}/api/auth/login"
method: POST
headers:
  Content-Type: application/json
body:
  mode: raw
  raw: |-
    {
      "email": "{{emailCliente}}",
      "password": "{{passwordValida}}",
      "forceSessionOverride": true
    }
scripts:
  - type: afterResponse
    code: |-
      // Guardar token en ambiente para requests subsiguientes
      if (pm.response.code === 200) {
        const body = pm.response.json();
        pm.environment.set('clienteToken', body.accessToken);
        pm.environment.set('clienteRefreshToken', body.refreshToken);
        console.log('✓ Login exitoso - token guardado en clienteToken');
      } else {
        console.warn('✗ Login falló con código', pm.response.code);
      }
    language: text/javascript
order: 100
```

- [ ] **Step 2: Crear request de consulta estado visita activa**

`10-01 Estado visita activa – CLIENTE.request.yaml`:

```yaml
$kind: http-request
name: 10-01 Estado visita activa – CLIENTE
description: Consulta el estado de la visita activa del cliente autenticado
url: "{{baseUrl}}/api/visitas/activa"
method: GET
headers:
  Authorization: Bearer {{clienteToken}}
scripts:
  - type: beforeRequest
    code: |-
      // Login autónomo
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: pm.environment.get('emailCliente'),
            password: pm.environment.get('passwordValida'),
            forceSessionOverride: true
          })
        }
      }, (err, res) => {
        if (!err && res && res.code === 200) {
          pm.environment.set('clienteToken', res.json().accessToken);
        }
      });
    language: text/javascript
order: 1000
```

- [ ] **Step 3: Crear request de detalle de visita**

`10-02 Detalle visita – CLIENTE.request.yaml`:

```yaml
$kind: http-request
name: 10-02 Detalle visita – CLIENTE
description: |-
  Obtiene el detalle completo de una visita específica.
  MANUAL: Reemplazar {{visitaIdConReserva}} con un ID real antes de ejecutar.
url: "{{baseUrl}}/api/visitas/cliente/{{visitaIdConReserva}}/detalle"
method: GET
headers:
  Authorization: Bearer {{clienteToken}}
scripts:
  - type: beforeRequest
    code: |-
      // Login autónomo
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: pm.environment.get('emailCliente'),
            password: pm.environment.get('passwordValida'),
            forceSessionOverride: true
          })
        }
      }, (err, res) => {
        if (!err && res && res.code === 200) {
          pm.environment.set('clienteToken', res.json().accessToken);
        }
      });
    language: text/javascript
order: 1010
```

- [ ] **Step 4: Crear request de consulta de reservas (mesero)**

`20-01 Consulta reservas – MESERO.request.yaml`:

```yaml
$kind: http-request
name: 20-01 Consulta reservas – MESERO
description: |-
  Consulta de reservas del día actual por mesero.
  Sin parámetro fecha = día actual. Sin identificador = todas las reservas.
url: "{{baseUrl}}/api/reservas/mesero/consulta"
method: GET
headers:
  Authorization: Bearer {{meseroToken}}
scripts:
  - type: beforeRequest
    code: |-
      // Login autónomo
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: pm.environment.get('emailMesero'),
            password: pm.environment.get('passwordValida'),
            forceSessionOverride: true
          })
        }
      }, (err, res) => {
        if (!err && res && res.code === 200) {
          pm.environment.set('meseroToken', res.json().accessToken);
        }
      });
    language: text/javascript
order: 2000
```

- [ ] **Step 5: Commit**

esperar commit del usurio
git commit -m "feat(postman): crear colección de pruebas manuales"
---

## Task 18: Documentar uso de colección Postman

**Files:**
- Create: `backend/postman/postman/collections/manual-testing/README.md`

- [ ] **Step 1: Crear README**

```markdown
# Colección de Pruebas Manuales - Al Toro Gastrobar

Esta colección está diseñada para **exploración manual** del backend, sin assertions automáticas.

## Cómo usar

### 1. Configurar ambiente

Asegurarse de que el ambiente `Al Toro – Local.environment.yaml` tenga:

```yaml
baseUrl: http://localhost:8080
emailCliente: carlos.perez@gmail.com
emailMesero: juan.gomez@gmail.com
emailCajero: sofia.lopez@gmail.com
emailAdmin: admin@altoro.com
passwordValida: <tu-password-seed>
```

### 2. Ejecutar servidor backend

```bash
cd backend
./mvnw spring-boot:run
```

### 3. Abrir Postman for VS Code

1. Instalar extensión "Postman for VS Code"
2. Abrir carpeta `backend/postman`
3. Seleccionar ambiente "Al Toro – Local"
4. Navegar a colección "Al Toro - Manual Testing"

### 4. Ejecutar requests

Cada request tiene login automático en `beforeRequest`, por lo que:
- ✅ Se pueden ejecutar de forma aislada
- ✅ No requieren ejecutar login primero
- ✅ Tokens se refrescan automáticamente

**Ejemplo de flujo manual:**

1. Ejecutar `00-01 Login CLIENTE` → Verificar que retorna 200 y guarda token
2. Ejecutar `10-01 Estado visita activa – CLIENTE` → Ver items, total, asistencia
3. Ejecutar `10-02 Detalle visita – CLIENTE` → Verificar agrupación de items
4. Ejecutar `20-01 Consulta reservas – MESERO` → Ver reservas del día

## Requests disponibles

### 00 - Autenticación
- `00-01 Login CLIENTE` — Guardar token manualmente
- `00-02 Login MESERO`
- `00-03 Login CAJERO`
- `00-04 Login ADMIN`

### 10 - Visitas
- `10-01 Estado visita activa – CLIENTE` — Estado en tiempo real
- `10-02 Detalle visita – CLIENTE` — Items agrupados

### 20 - Reservas (Mesero)
- `20-01 Consulta reservas – MESERO` — Reservas del día

```

- [ ] **Step 2: Commit**

esperar commit del usurio
git commit -m "docs(postman): documentar uso de colección manual"

---

## Task 19: Ordenar items por categoría en PreOrdenMapper

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/PreOrdenMapper.java`

- [ ] **Step 1: Añadir Comparator como constante**

Al inicio de la clase, después de las declaraciones de clase:

```java
/**
 * Comparador para ordenar items por categoría de producto.
 * Orden: PLATO (0) → BEBIDA (1) → OTRO (2)
 */
private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA = 
    Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());
```

- [ ] **Step 2: Añadir import de Comparator**

```java
import java.util.Comparator;
```

- [ ] **Step 3: Aplicar ordenamiento en toPreOrdenItems**

Buscar el stream que mapea `List<ComandaItem>` a `List<PreOrdenItemResponse>` y añadir `.sorted()`:

```java
// ANTES:
items.stream()
    .map(item -> PreOrdenItemResponse.builder()
        // ...
        .build())
    .toList();

// DESPUÉS:
items.stream()
    .sorted(COMPARATOR_POR_CATEGORIA)
    .map(item -> PreOrdenItemResponse.builder()
        // ...
        .build())
    .toList();
```

- [ ] **Step 4: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

- [ ] **Step 5: Commit**

esperar commit del usurio
git commit -m "feat(mappers): ordenar items de pre-orden por categoría"
---

## Task 20: Ordenar items por categoría en VisitaEstadoMapper

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java`

- [ ] **Step 1: Añadir Comparator como constante**

Después de los imports, antes del primer método:

```java
/**
 * Comparador para ordenar items por categoría de producto.
 * Orden: PLATO (0) → BEBIDA (1) → OTRO (2)
 */
private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA = 
    Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());
```

- [ ] **Step 2: Añadir import de Comparator**

```java
import java.util.Comparator;
```

- [ ] **Step 3: Actualizar toEstadoVisitaResponse para ordenar items**

En el método `toEstadoVisitaResponse`, el parámetro `List<ItemVisitaResponse> items` ya está mapeado.
El ordenamiento debe aplicarse en el SERVICE, no en este mapper (porque aquí recibe ItemVisitaResponse, no ComandaItem).

**SKIP ESTE PASO** - el ordenamiento se hará en VisitaEstadoService (Task 21)

---

## Task 21: Ordenar items por categoría en VisitaEstadoService

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoService.java`

- [ ] **Step 1: Añadir Comparator como constante**

Después de las declaraciones de dependencias:

```java
/**
 * Comparador para ordenar items por categoría de producto.
 * Orden: PLATO (0) → BEBIDA (1) → OTRO (2)
 */
private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA = 
    Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());
```

- [ ] **Step 2: Añadir import de Comparator**

```java
import java.util.Comparator;
```

- [ ] **Step 3: Aplicar ordenamiento en obtenerEstadoVisitaActiva**

Buscar el flatMap que mapea items y añadir `.sorted()`:

```java
// ANTES:
List<ItemVisitaResponse> items = comandas.stream()
        .filter(c -> c.getComandaEstado() != EstadoComanda.PRE_RESERVA)
        .flatMap(c -> comandaItemRepository.findByComanda_ComandaId(c.getComandaId())
                .stream()
                .map(item -> visitaEstadoMapper.toItemVisitaResponse(item, c.getComandaEstado())))
        .collect(Collectors.toList());

// DESPUÉS:
List<ItemVisitaResponse> items = comandas.stream()
        .filter(c -> c.getComandaEstado() != EstadoComanda.PRE_RESERVA)
        .flatMap(c -> comandaItemRepository.findByComanda_ComandaId(c.getComandaId())
                .stream()
                .sorted(COMPARATOR_POR_CATEGORIA)
                .map(item -> visitaEstadoMapper.toItemVisitaResponse(item, c.getComandaEstado())))
        .collect(Collectors.toList());
```

- [ ] **Step 4: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

- [ ] **Step 5: Commit**

esperar commit del usurio
git commit -m "feat(services): ordenar items por categoría en estado activa"

## Task 22: Ordenar items por categoría en VisitaMapper (agrupados)

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaMapper.java`

- [ ] **Step 1: Añadir Comparator como constante**

Después de las declaraciones de clase:

```java
/**
 * Comparador para ordenar items por categoría de producto.
 * Orden: PLATO (0) → BEBIDA (1) → OTRO (2)
 */
private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA = 
    Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());
```

- [ ] **Step 2: Añadir import de Comparator si no existe**

Verificar que esté:
```java
import java.util.Comparator;
```

- [ ] **Step 3: Aplicar ordenamiento ANTES de agrupar**

En el método `agruparItems()`, añadir `.sorted()` antes de `.collect()`:

```java
private List<ComandaItemResponse> agruparItems(List<ComandaItem> items) {
    // Clave de agrupación: nombreProducto + "|" + descripcion (null-safe)
    Map<String, List<ComandaItem>> agrupados = items.stream()
        .sorted(COMPARATOR_POR_CATEGORIA)  // ← AÑADIR AQUÍ
        .collect(Collectors.groupingBy(item -> 
            item.getProducto().getProductoNombre() + "|" + 
            (item.getComandaItemDescripcion() != null ? item.getComandaItemDescripcion() : "")
        ));
    
    // ... resto del método sin cambios
}
```

- [ ] **Step 4: Compilar para verificar**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" clean compile
```

Expected: Compilación exitosa

- [ ] **Step 5: Commit**

esperar commit del usurio
git commit -m "feat(mappers): ordenar items por categoría en detalle de visita"

---

## Task 23: Verificar ordenamiento con suite de tests

**Files:**
- None (verification task)

- [ ] **Step 1: Ejecutar tests completos**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" test
```

Expected: Todos los tests PASAN (el ordenamiento no rompe lógica, solo cambia el orden)

- [ ] **Step 2: Verificar manualmente con Postman**

Ejecutar:
1. `10-01 Estado visita activa – CLIENTE` → Verificar que items aparecen primero PLATO, luego BEBIDA, luego OTRO
2. `10-02 Detalle visita – CLIENTE` → Verificar mismo orden en items agrupados

- [ ] **Step 3: Commit si todo pasa**

esperar commit del usurio
git commit --allow-empty -m "test: verificar ordenamiento por categoría en todos los endpoints"

---

## Resumen de cambios vs Plan v1

| Aspecto | Plan v1 (Incorrecto) | Plan v2 (Correcto) |
|---------|----------------------|-------------------|
| `ItemVisitaResponse.comandaItemId` | ❌ Eliminado | ✅ Mantenido |
| `ItemVisitaResponse.descripcion` | ✅ Añadido | ✅ Añadido |
| `VisitaEstadoMapper` | ❌ Implementa agrupación | ✅ Solo añade descripcion |
| `VisitaMapper` | ✅ Implementa agrupación | ✅ Implementa agrupación |
| `VisitaEstadoService` | ❌ Usa agrupación | ✅ Sin cambios (+ ordenamiento) |
| Endpoint `/api/visitas/activa` | ❌ Items agrupados | ✅ Items individuales con comandaId |
| Endpoint `/api/visitas/cliente/{id}/detalle` | ✅ Items agrupados | ✅ Items agrupados |
| **Ordenamiento por categoría** | ❌ No implementado | ✅ PLATO → BEBIDA → OTRO |
| Total de tasks | 18 | 23 (+ ordenamiento) |

**Conclusión:** Este plan v2 separa correctamente los dos casos de uso, mantiene la estructura real de comandas en el endpoint de estado activa, y añade ordenamiento por categoría en todos los endpoints que listan items.

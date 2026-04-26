# Modificar Reserva (HE-02-HU-04) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar el endpoint `PUT /api/reservas/{reservaId}` que permite a un cliente modificar una reserva futura respetando la hora límite de las 4 PM del día de la reserva, con lógica de transición entre tipos BASICA/ESPECIAL.

**Architecture:** Se añaden 3 campos a `ReservaDetalleResponse` (`zonaId`, `decoracionId`, `modificable`) para que el frontend precargue el formulario. El endpoint de modificación reutiliza los helpers de validación existentes en `ReservaService` más nuevas queries de repositorio que excluyen la reserva actual de los cálculos de disponibilidad. La transición ESPECIAL→BASICA cancela la reserva antigua y crea una nueva.

**Tech Stack:** Spring Boot 3.5, Spring Data JPA, Spring Security (JWT), JUnit 5 + Mockito, Postman YAML collections.

---

## Mapa de archivos

| Acción | Archivo |
|--------|---------|
| Modify | `modules/reservas/dto/response/ReservaDetalleResponse.java` |
| Modify | `modules/reservas/mapper/ReservaMapper.java` |
| Create | `modules/reservas/dto/request/ModificarReservaRequest.java` |
| Create | `modules/reservas/dto/response/ModificarReservaResponse.java` |
| Modify | `modules/reservas/repository/ReservaRepository.java` |
| Modify | `modules/mesas_comandas/repository/ComandaMenuModificacionRepository.java` |
| Modify | `modules/reservas/service/ReservaService.java` |
| Refactor | `modules/reservas/service/ReservaService.java` (Task 8.5: extraer `determinarTipoReserva`) |
| Modify | `modules/reservas/controller/ReservaController.java` |
| Create | `test/.../reservas/service/ReservaServiceModificarTest.java` |

Todos los paths relativos parten de:
`backend/src/main/java/co/edu/unicauca/backend/`

---

## Task 1: Añadir campos a ReservaDetalleResponse

**Archivos:**
- Modify: `modules/reservas/dto/response/ReservaDetalleResponse.java`

- [ ] **Step 1: Añadir los 3 campos nuevos al DTO**

En `ReservaDetalleResponse.java`, añadir después del campo `tipo`:

```java
/** Identificador de la zona seleccionada; {@code null} si el cliente no eligió zona. */
private final Long zonaId;

/** Identificador de la decoración seleccionada; {@code null} si no aplica. */
private final Long decoracionId;

/**
 * {@code true} si la reserva puede modificarse (estado activo y antes de las 16:00
 * del día de llegada); {@code false} en caso contrario.
 */
private final boolean modificable;
```

- [ ] **Step 2: Compilar para verificar que no hay errores**

```bash
cd backend && ./mvnw clean compile -q
```
Esperado: BUILD SUCCESS sin errores.

- [ ] **Step 2.5: Verificar documentación**

- Confirmar que el Javadoc de clase en `ReservaDetalleResponse` lista los nuevos campos
  `zonaId`, `decoracionId` y `modificable` en su sección de "Campos opcionales" o similar.
- Verificar que los 3 campos nuevos tienen Javadoc individual (ya incluidos en el Step 1).

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ReservaDetalleResponse.java
git commit -m "feat(reservas): añadir zonaId, decoracionId y modificable a ReservaDetalleResponse"
```

---

## Task 2: Actualizar ReservaMapper para mapear nuevos campos

**Archivos:**
- Modify: `modules/reservas/mapper/ReservaMapper.java`

- [ ] **Step 1: Añadir import de LocalDateTime en ReservaMapper (si no existe)**

Al inicio de `ReservaMapper.java`, verificar que existe:
```java
import java.time.LocalDateTime;
import java.time.LocalTime;
```
Si no están, añadirlos.

- [ ] **Step 2: Actualizar toResumen() para incluir los nuevos campos**

Reemplazar el método `toResumen` existente:

```java
public ReservaDetalleResponse toResumen(Reserva reserva) {
    return ReservaDetalleResponse.builder()
            .reservaId(reserva.getReservaId())
            .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
            .numeroPersonas(reserva.getReservaNumeroPersonas())
            .estado(reserva.getReservaEstado().name())
            .tipo(reserva.getReservaTipo().name())
            .zonaId(reserva.getZona() != null ? reserva.getZona().getZonaId() : null)
            .decoracionId(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionId() : null)
            .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
            .decoracionNombre(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionNombre() : null)
            .modificable(esModificable(reserva))
            .build();
}
```

- [ ] **Step 3: Actualizar toDetalleResponse() para incluir los nuevos campos**

En el `return` final de `toDetalleResponse`, añadir los 3 campos nuevos:

```java
return ReservaDetalleResponse.builder()
        .reservaId(reserva.getReservaId())
        .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
        .numeroPersonas(reserva.getReservaNumeroPersonas())
        .estado(reserva.getReservaEstado().name())
        .tipo(reserva.getReservaTipo().name())
        .zonaId(reserva.getZona() != null ? reserva.getZona().getZonaId() : null)
        .decoracionId(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionId() : null)
        .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
        .decoracionNombre(reserva.getDecoracion() != null
                ? reserva.getDecoracion().getDecoracionNombre() : null)
        .notas(reserva.getReservaNotas())
        .preOrdenItems(preOrdenItems)
        .preOrdenTotal(preOrdenTotal)
        .abonos(abonosDto)
        .totalAbonado(totalAbonado)
        .modificable(esModificable(reserva))
        .build();
```

- [ ] **Step 4: Añadir el helper privado esModificable al final de la clase**

```java
/**
 * Calcula si una reserva puede ser modificada por el cliente.
 *
 * <p>Una reserva es modificable si su estado es {@code PENDIENTE} o {@code CONFIRMADA}
 * y el momento actual es anterior a las 16:00 del día de llegada.
 */
private boolean esModificable(Reserva reserva) {
    if (reserva.getReservaEstado() != co.edu.unicauca.backend.shared.enums.EstadoReserva.PENDIENTE
            && reserva.getReservaEstado() != co.edu.unicauca.backend.shared.enums.EstadoReserva.CONFIRMADA) {
        return false;
    }
    LocalDateTime limiteModificacion = reserva.getReservaFechaHoraLlegada()
            .toLocalDate().atTime(LocalTime.of(16, 0));
    return LocalDateTime.now().isBefore(limiteModificacion);
}
```

Añadir el import de `LocalTime` si no estaba. Añadir también el import de `EstadoReserva`:
```java
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
```

- [ ] **Step 5: Compilar**

```bash
cd backend && ./mvnw clean compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 5.5: Verificar documentación**

- `toResumen`: el Javadoc existente no necesita cambio (mapeo trivial).
- `toDetalleResponse`: verificar que el Javadoc existente siga siendo correcto tras los nuevos campos.
- `esModificable`: debe tener Javadoc de método (ya incluido en Step 4) y comentarios en línea:
  ```java
  // Estado terminal — no puede modificarse
  // Calcular límite: 16:00 del día de la reserva
  // Comparar con el instante actual
  ```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapper.java
git commit -m "feat(reservas): mapear zonaId, decoracionId y modificable en ReservaMapper"
```

---

## Task 3: Crear ModificarReservaRequest

**Archivos:**
- Create: `modules/reservas/dto/request/ModificarReservaRequest.java`

- [ ] **Step 1: Crear el archivo**

```java
package co.edu.unicauca.backend.modules.reservas.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos enviados por el cliente al modificar una reserva existente.
 *
 * <p>Aplica las mismas reglas de validación que {@link CrearReservaRequest},
 * salvo que el email del cliente se toma siempre del token de autenticación.
 */
@Getter
@NoArgsConstructor
public class ModificarReservaRequest {

    @NotNull
    @Future
    private LocalDateTime fechaHoraLlegada;

    @NotNull
    @Min(1)
    private Integer numeroPersonas;

    private Long decoracionId;

    private Long zonaId;

    private String notas;

    @Valid
    private List<PreOrdenItemRequest> preOrden;
}
```

- [ ] **Step 2: Compilar**

```bash
cd backend && ./mvnw clean compile -q
```

- [ ] **Step 2.5: Verificar documentación**

- Javadoc de clase: verificar que describe el propósito del DTO, que el email se toma del
  token y no del body, y que menciona la referencia a `CrearReservaRequest`.
- Javadoc de cada campo: `fechaHoraLlegada`, `numeroPersonas`, `decoracionId`, `zonaId`,
  `notas` y `preOrden` deben tener Javadoc individual con `{@code null}` donde aplica.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/request/ModificarReservaRequest.java
git commit -m "feat(reservas): crear ModificarReservaRequest DTO"
```

---

## Task 4: Crear ModificarReservaResponse

**Archivos:**
- Create: `modules/reservas/dto/response/ModificarReservaResponse.java`

- [ ] **Step 1: Crear el archivo**

```java
package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO de respuesta para la modificación de una reserva.
 *
 * <p>Cuando {@code requiereWhatsApp} es {@code true}, el frontend debe redirigir
 * al cliente al chat de WhatsApp de la empresa con el mensaje {@code mensajeWhatsApp}
 * precompuesto. Esto ocurre en las transiciones BASICA→ESPECIAL y ESPECIAL→BASICA.
 *
 * <p>En la transición ESPECIAL→BASICA el {@code reservaId} corresponde a la reserva
 * nueva creada; la reserva original queda en estado CANCELADA.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModificarReservaResponse {

    /** Identificador de la reserva resultante (puede diferir del original en ESPECIAL→BASICA). */
    private final Long reservaId;

    /** Estado de la reserva resultante: {@code CONFIRMADA} o {@code PENDIENTE}. */
    private final String estado;

    /** Tipo de la reserva resultante: {@code BASICA} o {@code ESPECIAL}. */
    private final String tipo;

    /** Fecha y hora de llegada en formato {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private final String fechaHoraLlegada;

    /** Número de comensales. */
    private final Integer numeroPersonas;

    /** Nombre de la zona; {@code null} si no se seleccionó zona. */
    private final String zonaNombre;

    /** Nombre de la decoración; {@code null} si no aplica. */
    private final String decoracionNombre;

    /** Observaciones del cliente; {@code null} si no hay notas. */
    private final String notas;

    /**
     * {@code true} cuando la modificación requiere confirmar vía WhatsApp
     * (transiciones BASICA→ESPECIAL o ESPECIAL→BASICA).
     */
    private final boolean requiereWhatsApp;

    /**
     * Mensaje precompuesto para enviar al chat de WhatsApp; {@code null} cuando
     * {@code requiereWhatsApp} es {@code false}.
     */
    private final String mensajeWhatsApp;
}
```

- [ ] **Step 2: Compilar**

```bash
cd backend && ./mvnw clean compile -q
```

- [ ] **Step 2.5: Verificar documentación**

- Javadoc de clase: verificar que describe la transición ESPECIAL→BASICA (nuevo `reservaId`)
  y el contrato del campo `requiereWhatsApp`.
- Todos los campos deben tener Javadoc individual (ya incluidos en el Step 1).
- Verificar que el `@JsonInclude(NON_NULL)` está documentado implícitamente (campos con
  `{@code null}` en su Javadoc).

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/dto/response/ModificarReservaResponse.java
git commit -m "feat(reservas): crear ModificarReservaResponse DTO"
```

---

## Task 5: Añadir queries de repositorio para disponibilidad con exclusión

**Archivos:**
- Modify: `modules/reservas/repository/ReservaRepository.java`
- Modify: `modules/mesas_comandas/repository/ComandaMenuModificacionRepository.java`

- [ ] **Step 1: Añadir 3 queries a ReservaRepository**

Al final de la interfaz `ReservaRepository`, antes del cierre `}`, añadir:

```java
/**
 * Suma los comensales reservados en una zona excluyendo una reserva específica.
 *
 * <p>Usado al modificar una reserva para no contar la reserva actual
 * en el cálculo de capacidad disponible.
 *
 * @param zonaId           identificador de la zona
 * @param inicio           inicio del rango de fecha/hora (inclusive)
 * @param fin              fin del rango de fecha/hora (inclusive)
 * @param estados          estados a considerar como activos
 * @param excludeReservaId reserva a excluir del cómputo
 * @return suma de personas; {@code 0} si no hay reservas
 */
@Query("SELECT COALESCE(SUM(r.reservaNumeroPersonas), 0) FROM Reserva r " +
       "WHERE r.zona.zonaId = :zonaId " +
       "AND r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
       "AND r.reservaEstado IN :estados " +
       "AND r.reservaId <> :excludeReservaId")
int sumPersonasByZonaEnDiaExcluyendo(
        @Param("zonaId") Long zonaId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin,
        @Param("estados") List<EstadoReserva> estados,
        @Param("excludeReservaId") Long excludeReservaId);

/**
 * Devuelve los IDs de decoraciones ocupadas excluyendo una reserva específica.
 *
 * @param inicio           inicio del rango (inclusive)
 * @param fin              fin del rango (inclusive)
 * @param estados          estados a considerar como activos
 * @param excludeReservaId reserva a excluir
 * @return lista de {@code decoracionId} ocupados
 */
@Query("SELECT r.decoracion.decoracionId FROM Reserva r " +
       "WHERE r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
       "AND r.reservaEstado IN :estados " +
       "AND r.decoracion IS NOT NULL " +
       "AND r.reservaId <> :excludeReservaId")
List<Long> findDecoracionesOcupadasEnDiaExcluyendo(
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin,
        @Param("estados") List<EstadoReserva> estados,
        @Param("excludeReservaId") Long excludeReservaId);

/**
 * Suma comensales por zona excluyendo una reserva específica.
 *
 * @param inicio           inicio del rango (inclusive)
 * @param fin              fin del rango (inclusive)
 * @param estados          estados a considerar como activos
 * @param excludeReservaId reserva a excluir
 * @return lista de pares {@code [zonaId, sumaPersonas]}
 */
@Query("SELECT r.zona.zonaId, SUM(r.reservaNumeroPersonas) FROM Reserva r " +
       "WHERE r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
       "AND r.reservaEstado IN :estados " +
       "AND r.zona IS NOT NULL " +
       "AND r.reservaId <> :excludeReservaId " +
       "GROUP BY r.zona.zonaId")
List<Object[]> findPersonasPorZonaEnDiaExcluyendo(
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin,
        @Param("estados") List<EstadoReserva> estados,
        @Param("excludeReservaId") Long excludeReservaId);
```

- [ ] **Step 2: Añadir deleteByComandaItem_ComandaItemId a ComandaMenuModificacionRepository**

En `ComandaMenuModificacionRepository.java`, añadir después del método existente:

```java
/**
 * Elimina todas las modificaciones asociadas a un ítem de comanda.
 *
 * <p>Usado al reemplazar la pre-orden de una reserva durante una modificación.
 *
 * @param comandaItemId identificador del item de comanda
 */
void deleteByComandaItem_ComandaItemId(Long comandaItemId);
```

- [ ] **Step 3: Compilar**

```bash
cd backend && ./mvnw clean compile -q
```

- [ ] **Step 3.5: Verificar documentación**

- Cada query nueva (`sumPersonasByZonaEnDiaExcluyendo`, `findDecoracionesOcupadasEnDiaExcluyendo`,
  `findPersonasPorZonaEnDiaExcluyendo`) debe tener Javadoc con `@param excludeReservaId`
  que explique **por qué** se excluye (para no contar la reserva que se está modificando).
- `deleteByComandaItem_ComandaItemId` debe tener Javadoc indicando que se usa al reemplazar
  la pre-orden.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/repository/ReservaRepository.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaMenuModificacionRepository.java
git commit -m "feat(reservas): añadir queries de disponibilidad con exclusión y delete de modificaciones"
```

---

## Task 6: Añadir toModificarResponse a ReservaMapper

**Archivos:**
- Modify: `modules/reservas/mapper/ReservaMapper.java`

- [ ] **Step 1: Añadir import de ModificarReservaResponse**

```java
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
```

- [ ] **Step 2: Añadir el método toModificarResponse**

Añadir después de `toDetalleResponse`:

```java
/**
 * Construye el DTO de respuesta para una modificación de reserva.
 *
 * @param reserva          entidad resultante (puede ser nueva en transición ESPECIAL→BASICA)
 * @param requiereWhatsApp {@code true} si la transición de tipo requiere contacto vía WhatsApp
 * @param mensajeWhatsApp  mensaje precompuesto; {@code null} cuando no se requiere WhatsApp
 * @return {@link ModificarReservaResponse} con los datos de la reserva resultante
 */
public ModificarReservaResponse toModificarResponse(Reserva reserva,
                                                     boolean requiereWhatsApp,
                                                     String mensajeWhatsApp) {
    return ModificarReservaResponse.builder()
            .reservaId(reserva.getReservaId())
            .estado(reserva.getReservaEstado().name())
            .tipo(reserva.getReservaTipo().name())
            .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
            .numeroPersonas(reserva.getReservaNumeroPersonas())
            .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
            .decoracionNombre(reserva.getDecoracion() != null
                    ? reserva.getDecoracion().getDecoracionNombre() : null)
            .notas(reserva.getReservaNotas())
            .requiereWhatsApp(requiereWhatsApp)
            .mensajeWhatsApp(mensajeWhatsApp)
            .build();
}
```

- [ ] **Step 3: Compilar**

```bash
cd backend && ./mvnw clean compile -q
```

- [ ] **Step 3.5: Verificar documentación**

- `toModificarResponse`: verificar que el Javadoc (ya incluido en Step 2) describe la
  transición ESPECIAL→BASICA y el contrato de `mensajeWhatsApp` nulo.
- No se requieren comentarios en línea (el método es un mapping directo sin lógica).

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/mapper/ReservaMapper.java
git commit -m "feat(reservas): añadir toModificarResponse en ReservaMapper"
```

---

## Task 7: Service — helper eliminarPreOrdenExistente

**Archivos:**
- Modify: `modules/reservas/service/ReservaService.java`

- [ ] **Step 1: Añadir el método privado al final de la sección de lógica interna de pre-orden**

Añadir después de `persistirPreOrden` y antes de la sección de validaciones privadas:

```java
/**
 * Elimina la pre-orden ({@code PRE_RESERVA} comanda) de una reserva si existe.
 *
 * <p>Borra en orden: modificaciones de menú → ítems → comanda, para respetar
 * las restricciones de FK de la base de datos.
 *
 * @param reservaId identificador de la reserva cuya pre-orden se va a eliminar
 */
private void eliminarPreOrdenExistente(Long reservaId) {
    comandaRepository
            .findByReserva_ReservaIdAndComandaEstado(reservaId, EstadoComanda.PRE_RESERVA)
            .ifPresent(comanda -> {
                List<ComandaItem> items =
                        comandaItemRepository.findByComanda_ComandaId(comanda.getComandaId());
                items.forEach(item ->
                        comandaMenuModificacionRepository
                                .deleteByComandaItem_ComandaItemId(item.getComandaItemId()));
                comandaItemRepository.deleteAll(items);
                comandaRepository.delete(comanda);
            });
}
```

- [ ] **Step 2: Compilar**

```bash
cd backend && ./mvnw clean compile -q
```

- [ ] **Step 2.5: Verificar documentación**

- `eliminarPreOrdenExistente`: el Javadoc (incluido en Step 1) debe explicar el orden de
  borrado (modificaciones → ítems → comanda) y por qué ese orden es necesario (FK constraints).
- Añadir comentarios en línea:
  ```java
  // Buscar la comanda PRE_RESERVA de esta reserva; si no existe, no hay nada que eliminar
  // Borrar modificaciones de menú especial antes que los ítems (FK constraint)
  // Borrar los ítems antes que la comanda (FK constraint)
  // Eliminar la comanda vacía
  ```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaService.java
git commit -m "feat(reservas): añadir helper eliminarPreOrdenExistente en ReservaService"
```

---

## Task 8: Service — helper consultarDisponibilidadParaModificacion

**Archivos:**
- Modify: `modules/reservas/service/ReservaService.java`

- [ ] **Step 1: Añadir el método privado en la sección de validaciones privadas**

Añadir antes de `esHorarioValido`:

```java
/**
 * Consulta disponibilidad para una fecha/hora excluyendo la reserva que se está modificando.
 *
 * <p>Lógica idéntica a {@link #consultarDisponibilidad(LocalDateTime)}, pero usa las
 * queries {@code *Excluyendo} del repositorio para no contar la reserva actual en los
 * cálculos de ocupación de zona y decoración.
 *
 * @param fechaHora        nueva fecha y hora de llegada solicitada
 * @param excludeReservaId ID de la reserva siendo modificada, excluida de los conteos
 * @return {@link DisponibilidadResponse} con zonas y decoraciones disponibles
 */
private DisponibilidadResponse consultarDisponibilidadParaModificacion(
        LocalDateTime fechaHora, Long excludeReservaId) {

    if (!esHorarioValido(fechaHora) || estaBloqueda(fechaHora)) {
        return reservaMapper.sinDisponibilidad();
    }

    List<Zona> todasLasZonas = zonaRepository.findAll();
    if (todasLasZonas.isEmpty()) {
        return reservaMapper.sinDisponibilidad();
    }

    LocalDateTime inicio = fechaHora.toLocalDate().atStartOfDay();
    LocalDateTime fin    = fechaHora.toLocalDate().atTime(23, 59, 59);

    Map<Long, Integer> personasPorZona = reservaRepository
            .findPersonasPorZonaEnDiaExcluyendo(inicio, fin, ESTADOS_ACTIVOS, excludeReservaId)
            .stream()
            .collect(Collectors.toMap(
                    row -> (Long) row[0],
                    row -> ((Number) row[1]).intValue()
            ));

    List<Zona> zonasLibres = todasLasZonas.stream()
            .filter(z -> personasPorZona.getOrDefault(z.getZonaId(), 0)
                         < z.getZonaCapacidadPersonas())
            .collect(Collectors.toList());

    if (zonasLibres.isEmpty()) {
        return reservaMapper.sinDisponibilidad();
    }

    Set<Long> idsZonasLibres = zonasLibres.stream()
            .map(Zona::getZonaId)
            .collect(Collectors.toSet());

    Set<Long> decoracionesOcupadas = Set.copyOf(
            reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(
                    inicio, fin, ESTADOS_ACTIVOS, excludeReservaId));

    List<Decoracion> decoracionesActivas = decoracionRepository
            .findByDecoracionEstado(EstadoGenerico.ACTIVO)
            .stream()
            .filter(d -> !decoracionesOcupadas.contains(d.getDecoracionId()))
            .collect(Collectors.toList());

    List<DecoracionDisponibleResponse> decoracionesDto = decoracionesActivas.stream()
            .map(d -> {
                List<DecoracionZona> links =
                        decoracionZonaRepository.findByDecoracionId(d.getDecoracionId());
                return reservaMapper.toDecoracionDto(d, links, idsZonasLibres);
            })
            .collect(Collectors.toList());

    List<ZonaDisponibleResponse> zonasDto = zonasLibres.stream()
            .map(reservaMapper::toZonaDto)
            .collect(Collectors.toList());

    return DisponibilidadResponse.builder()
            .disponible(true)
            .decoraciones(decoracionesDto)
            .zonas(zonasDto)
            .build();
}
```

- [ ] **Step 2: Compilar**

```bash
cd backend && ./mvnw clean compile -q
```

- [ ] **Step 2.5: Verificar documentación**

- `consultarDisponibilidadParaModificacion`: el Javadoc (incluido en Step 1) debe mencionar
  explícitamente que usa las queries `*Excluyendo` y referenciar `consultarDisponibilidad`
  con `{@link}`.
- El cuerpo del método ya sigue la misma estructura que `consultarDisponibilidad`; no añadir
  comentarios en línea redundantes salvo donde el comportamiento difiera.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaService.java
git commit -m "feat(reservas): añadir consultarDisponibilidadParaModificacion en ReservaService"
```

---

## Task 8.5: Refactorizar crearReserva — extraer determinarTipoReserva

**Archivos:**
- Modify: `modules/reservas/service/ReservaService.java`

- [ ] **Step 1: Añadir el helper privado `determinarTipoReserva`**

Añadir en la sección de validaciones privadas, **antes** de `esHorarioValido`:

```java
/**
 * Determina el tipo de una reserva según su decoración y pre-orden.
 *
 * <p>Una reserva es ESPECIAL si su decoración tiene costo adicional mayor a cero
 * o si algún ítem de la pre-orden es un menú especial. En cualquier otro caso es BASICA.
 *
 * @param decoracion decoración seleccionada; {@code null} si no se eligió ninguna
 * @param preOrden   ítems de pre-orden; {@code null} o vacía si no hay pre-orden
 * @return {@link TipoReserva#ESPECIAL} o {@link TipoReserva#BASICA}
 */
private TipoReserva determinarTipoReserva(Decoracion decoracion,
                                           List<PreOrdenItemRequest> preOrden) {
    // Decoración con costo adicional convierte la reserva en ESPECIAL
    boolean tieneDecoracionConCosto = decoracion != null
            && decoracion.getDecoracionCostoAdicional() != null
            && decoracion.getDecoracionCostoAdicional().compareTo(BigDecimal.ZERO) > 0;

    // Pre-orden con al menos un menú especial también convierte la reserva en ESPECIAL
    boolean tieneMenuEspecial = preOrden != null
            && preOrden.stream().anyMatch(i -> Boolean.TRUE.equals(i.getEsMenuEspecial()));

    return (tieneDecoracionConCosto || tieneMenuEspecial)
            ? TipoReserva.ESPECIAL
            : TipoReserva.BASICA;
}
```

- [ ] **Step 2: Refactorizar el bloque de determinación de tipo en `crearReserva`**

Localizar en `crearReserva` el bloque (aprox. líneas 309-324 del archivo original):

```java
// Determinar si la reserva tiene una decoracion con costo adicinal
boolean tieneDecoracionConCosto = decoracion != null && ...
boolean tieneMenuEspecial = request.getPreOrden() != null && ...
boolean esEspecial = tieneDecoracionConCosto || tieneMenuEspecial;
TipoReserva tipo = esEspecial ? TipoReserva.ESPECIAL : TipoReserva.BASICA;
EstadoReserva estado = esEspecial ? EstadoReserva.PENDIENTE : EstadoReserva.CONFIRMADA;
```

Reemplazar por:

```java
// Determinar tipo de reserva (BASICA o ESPECIAL) según decoración y pre-orden
TipoReserva tipo      = determinarTipoReserva(decoracion, request.getPreOrden());
EstadoReserva estado  = (tipo == TipoReserva.ESPECIAL)
        ? EstadoReserva.PENDIENTE
        : EstadoReserva.CONFIRMADA;
```

- [ ] **Step 5: Verificar documentación**

- Javadoc de `determinarTipoReserva` ya incluido en Step 1.
- El comentario en línea del bloque refactorizado en `crearReserva` debe quedar:
  `// Determinar tipo de reserva (BASICA o ESPECIAL) según decoración y pre-orden`

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaService.java
git commit -m "refactor(reservas): extraer determinarTipoReserva de crearReserva como helper privado"
```

---

## Task 9: Service — método público modificarReserva

**Archivos:**
- Modify: `modules/reservas/service/ReservaService.java`

- [ ] **Step 1: Añadir imports necesarios en ReservaService**

Verificar/añadir al bloque de imports:

```java
import co.edu.unicauca.backend.modules.reservas.dto.request.ModificarReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
```

- [ ] **Step 2: Añadir constantes al inicio de la clase**

Después de la línea `private static final int HORA_CIERRE = 22;`, añadir:

```java
private static final java.time.LocalTime HORA_LIMITE_MODIFICACION = java.time.LocalTime.of(16, 0);
private static final String MSG_NO_MODIFICABLE =
        "Ya no es posible modificar esta reserva. Solo puedes cancelarla.";
private static final String MSG_ESTADO_NO_MODIFICABLE =
        "Solo puedes modificar reservas con estado PENDIENTE o CONFIRMADA.";
private static final DateTimeFormatter FORMATTER_WA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
```

- [ ] **Step 3: Añadir el método público modificarReserva**

Añadir en la sección Dashboard del cliente, después de `obtenerDetalleReserva`:

```java
/**
 * Modifica una reserva existente del cliente aplicando las mismas validaciones de negocio
 * que la creación y verificando la disponibilidad excluyendo la reserva actual.
 *
 * <p><b>Regla de hora límite:</b> solo es posible modificar antes de las 16:00 del día
 * de la reserva (CA-01).</p>
 *
 * <p><b>Transiciones de tipo y estado:</b>
 * <ul>
 *   <li>BASICA → BASICA: actualiza campos, mantiene CONFIRMADA.</li>
 *   <li>BASICA → ESPECIAL: actualiza campos, cambia a PENDIENTE. Requiere WhatsApp.</li>
 *   <li>ESPECIAL → ESPECIAL: actualiza campos, mantiene PENDIENTE.</li>
 *   <li>ESPECIAL → BASICA: cancela reserva original, crea nueva BASICA CONFIRMADA. Requiere WhatsApp.</li>
 * </ul>
 *
 * @param reservaId    ID de la reserva a modificar
 * @param emailCliente email del cliente autenticado (tomado del token)
 * @param request      nuevos datos de la reserva
 * @return {@link ModificarReservaResponse} con la reserva resultante y flag de WhatsApp si aplica
 * @throws ResourceNotFoundException si la reserva, decoración, zona o productos no existen
 * @throws BusinessException         si se incumple alguna regla de negocio
 */
@Transactional
public ModificarReservaResponse modificarReserva(Long reservaId,
                                                  String emailCliente,
                                                  ModificarReservaRequest request) {

    // Verificar existencia de la reserva
    Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

    // Verificar ownership (CA-02)
    if (!reserva.getCliente().getUsuario().getUsuarioEmail().equalsIgnoreCase(emailCliente)) {
        throw new BusinessException(ErrorCode.ACCESS_DENIED,
                "Solo puedes modificar tus propias reservas.", HttpStatus.FORBIDDEN);
    }

    // Verificar que la reserva esté en estado activo
    if (!ESTADOS_ACTIVOS.contains(reserva.getReservaEstado())) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                MSG_ESTADO_NO_MODIFICABLE, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // Verificar hora límite de modificación: antes de las 16:00 del día de la reserva (CA-01)
    LocalDateTime limiteModificacion = reserva.getReservaFechaHoraLlegada()
            .toLocalDate().atTime(HORA_LIMITE_MODIFICACION);
    if (!LocalDateTime.now().isBefore(limiteModificacion)) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                MSG_NO_MODIFICABLE, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // Validar horario del restaurante para la nueva fecha/hora (CA-04)
    if (!esHorarioValido(request.getFechaHoraLlegada())) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                MSG_FUERA_HORARIO, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // Validar bloqueos administrativos para la nueva fecha/hora (CA-04)
    if (estaBloqueda(request.getFechaHoraLlegada())) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                MSG_FUERA_HORARIO, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // Validar decoración (CA-04)
    Decoracion nuevaDecoracion = null;
    if (request.getDecoracionId() != null) {
        final Long decId = request.getDecoracionId();
        nuevaDecoracion = decoracionRepository.findById(decId)
                .orElseThrow(() -> new ResourceNotFoundException("Decoracion", decId));
    }

    // Validar zona (CA-04)
    Zona nuevaZona = null;
    if (request.getZonaId() != null) {
        final Long zId = request.getZonaId();
        nuevaZona = zonaRepository.findById(zId)
                .orElseThrow(() -> new ResourceNotFoundException("Zona", zId));
    }

    // Validar compatibilidad decoración-zona (CA-04)
    if (nuevaDecoracion != null && nuevaZona != null) {
        validarCompatibilidadDecoracionZona(nuevaDecoracion, nuevaZona);
    }

    // Verificar disponibilidad excluyendo la reserva actual (CA-06, CA-07)
    DisponibilidadResponse disponibilidad =
            consultarDisponibilidadParaModificacion(request.getFechaHoraLlegada(), reservaId);

    if (!disponibilidad.getDisponible()) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                MSG_DISPONIBILIDAD_CAMBIO, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // Verificar que la decoración elegida esté disponible
    if (nuevaDecoracion != null) {
        final Long decId = nuevaDecoracion.getDecoracionId();
        boolean decoracionLibre = disponibilidad.getDecoraciones().stream()
                .anyMatch(d -> d.getDecoracionId().equals(decId));
        if (!decoracionLibre) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    MSG_DISPONIBILIDAD_CAMBIO, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    // Verificar capacidad de zona
    LocalDateTime inicioDia = request.getFechaHoraLlegada().toLocalDate().atStartOfDay();
    LocalDateTime finDia    = request.getFechaHoraLlegada().toLocalDate().atTime(23, 59, 59);

    if (nuevaZona != null) {
        final Long zId = nuevaZona.getZonaId();
        boolean zonaLibre = disponibilidad.getZonas().stream()
                .anyMatch(z -> z.getZonaId().equals(zId));
        if (!zonaLibre) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    MSG_DISPONIBILIDAD_CAMBIO, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        int personasExistentes = reservaRepository.sumPersonasByZonaEnDiaExcluyendo(
                zId, inicioDia, finDia, ESTADOS_ACTIVOS, reservaId);
        if (personasExistentes + request.getNumeroPersonas() > nuevaZona.getZonaCapacidadPersonas()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "La zona seleccionada no tiene capacidad suficiente para " +
                    request.getNumeroPersonas() + " personas en ese día.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    } else {
        final Zona finalNuevaZona = nuevaZona;
        boolean hayZonaConCapacidad = disponibilidad.getZonas().stream()
                .anyMatch(z -> {
                    int ocupadas = reservaRepository.sumPersonasByZonaEnDiaExcluyendo(
                            z.getZonaId(), inicioDia, finDia, ESTADOS_ACTIVOS, reservaId);
                    return (z.getCapacidad() - ocupadas) >= request.getNumeroPersonas();
                });
        if (!hayZonaConCapacidad) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "No hay zonas con capacidad suficiente para " +
                    request.getNumeroPersonas() + " personas en ese día.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    // Validar pre-orden (CA-05)
    if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
        validarPreOrden(request.getPreOrden(), request.getNumeroPersonas());
    }

    // Determinar nuevo tipo usando el helper extraído en Task 8.5
    TipoReserva nuevoTipo   = determinarTipoReserva(nuevaDecoracion, request.getPreOrden());
    boolean nuevoEsEspecial = (nuevoTipo == TipoReserva.ESPECIAL);

    boolean anteriorEraEspecial = reserva.getReservaTipo() == TipoReserva.ESPECIAL;
    boolean requiereWhatsApp    = false;
    Reserva reservaResultado;

    if (anteriorEraEspecial && !nuevoEsEspecial) {
        // ESPECIAL → BASICA (CA-07): cancelar original, crear nueva BASICA CONFIRMADA
        reserva.setReservaEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);
        eliminarPreOrdenExistente(reservaId);

        Reserva nuevaReserva = Reserva.builder()
                .cliente(reserva.getCliente())
                .zona(nuevaZona)
                .decoracion(nuevaDecoracion)
                .reservaFechaHoraLlegada(request.getFechaHoraLlegada())
                .reservaNumeroPersonas(request.getNumeroPersonas())
                .reservaNotas(request.getNotas())
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaTipo(TipoReserva.BASICA)
                .build();
        reservaResultado = reservaRepository.save(nuevaReserva);
        requiereWhatsApp = true;
    } else {
        // BASICA→BASICA, BASICA→ESPECIAL, ESPECIAL→ESPECIAL: actualizar reserva existente
        EstadoReserva nuevoEstado;
        if (!anteriorEraEspecial && nuevoEsEspecial) {
            // BASICA → ESPECIAL (CA-06): cambiar a PENDIENTE
            nuevoEstado      = EstadoReserva.PENDIENTE;
            requiereWhatsApp = true;
        } else {
            // BASICA→BASICA: mantener CONFIRMADA | ESPECIAL→ESPECIAL: mantener PENDIENTE
            nuevoEstado = reserva.getReservaEstado();
        }

        reserva.setZona(nuevaDecoracion != null ? nuevaZona : nuevaZona);
        reserva.setZona(nuevaZona);
        reserva.setDecoracion(nuevaDecoracion);
        reserva.setReservaFechaHoraLlegada(request.getFechaHoraLlegada());
        reserva.setReservaNumeroPersonas(request.getNumeroPersonas());
        reserva.setReservaNotas(request.getNotas());
        reserva.setReservaEstado(nuevoEstado);
        reserva.setReservaTipo(nuevoTipo);

        eliminarPreOrdenExistente(reservaId);
        reservaResultado = reservaRepository.save(reserva);
    }

    // Persistir nueva pre-orden si existe (CA-05)
    if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
        persistirPreOrden(reservaResultado, request.getPreOrden());
    }

    // Componer mensaje WhatsApp si la transición lo requiere (CA-06, CA-07)
    String mensajeWhatsApp = requiereWhatsApp ? construirMensajeWhatsApp(reservaResultado) : null;

    return reservaMapper.toModificarResponse(reservaResultado, requiereWhatsApp, mensajeWhatsApp);
}
```

- [ ] **Step 4: Añadir el helper construirMensajeWhatsApp al final de la clase**

Añadir antes del cierre `}` de la clase, después de `validarCompatibilidadDecoracionZona`:

```java
/**
 * Construye el mensaje precompuesto para el chat de WhatsApp cuando la reserva
 * requiere confirmación especial (transiciones BASICA→ESPECIAL o ESPECIAL→BASICA).
 *
 * <p>El mensaje incluye: ID de reserva, nombre del cliente, fecha y hora,
 * número de personas, y opcionalmente decoración y zona si están asignadas.
 *
 * @param reserva entidad resultante de la modificación
 * @return mensaje formateado listo para enviar por WhatsApp
 */
private String construirMensajeWhatsApp(Reserva reserva) {
    StringBuilder sb = new StringBuilder();
    // Identificación del cliente y reserva
    sb.append("Hola, soy ").append(reserva.getCliente().getClienteNombre()).append(".\n");
    sb.append("Quisiera confirmar mi reserva #").append(reserva.getReservaId()).append(":\n");
    // Datos obligatorios de la reserva
    sb.append("- Fecha y hora: ")
      .append(reserva.getReservaFechaHoraLlegada().format(FORMATTER_WA)).append("\n");
    sb.append("- Número de personas: ")
      .append(reserva.getReservaNumeroPersonas()).append("\n");
    // Decoración y zona solo si están asignadas
    if (reserva.getDecoracion() != null) {
        sb.append("- Decoración: ")
          .append(reserva.getDecoracion().getDecoracionNombre()).append("\n");
    }
    if (reserva.getZona() != null) {
        sb.append("- Zona: ").append(reserva.getZona().getZonaNombre()).append("\n");
    }
    sb.append("\nPara confirmar tu reserva especial, debes abonar un valor anticipado, " +
              "comunicate para definirlo.");
    return sb.toString();
}
```

> **Nota:** La línea `reserva.setZona(nuevaDecoracion != null ? nuevaZona : nuevaZona);` del Step 3 es un artefacto del editor y debe eliminarse. Solo debe quedar `reserva.setZona(nuevaZona);`.

- [ ] **Step 5: Revisar y limpiar el método modificarReserva**

En el bloque `else` del Step 3, eliminar la línea duplicada:
```java
// ELIMINAR esta línea duplicada (es un error del template):
reserva.setZona(nuevaDecoracion != null ? nuevaZona : nuevaZona);
```
El bloque correcto debe quedar:
```java
reserva.setZona(nuevaZona);
reserva.setDecoracion(nuevaDecoracion);
reserva.setReservaFechaHoraLlegada(request.getFechaHoraLlegada());
reserva.setReservaNumeroPersonas(request.getNumeroPersonas());
reserva.setReservaNotas(request.getNotas());
reserva.setReservaEstado(nuevoEstado);
reserva.setReservaTipo(nuevoEsEspecial ? TipoReserva.ESPECIAL : TipoReserva.BASICA);
```

- [ ] **Step 6: Compilar**

```bash
cd backend && ./mvnw clean compile -q
```
Esperado: BUILD SUCCESS. Si hay error en `setZona` o `setDecoracion`, verificar que la entidad `Reserva` tenga setters (debe tener `@Setter` o `@Data` de Lombok).

- [ ] **Step 7: Verificar setters en Reserva.java**

```bash
grep -n "@Setter\|@Data\|@Builder\|@AllArgs" backend/src/main/java/co/edu/unicauca/backend/modules/reservas/entity/Reserva.java | head -5
```

Si solo hay `@Builder` sin `@Setter`, añadir `@Setter` a la clase o cambiar los setters por un builder de actualización. En ese caso, reemplazar los `reserva.setXxx()` por:

```java
reserva = Reserva.builder()
    .reservaId(reserva.getReservaId())         // preservar ID
    .cliente(reserva.getCliente())
    .zona(nuevaZona)
    .decoracion(nuevaDecoracion)
    .reservaFechaHoraLlegada(request.getFechaHoraLlegada())
    .reservaNumeroPersonas(request.getNumeroPersonas())
    .reservaNotas(request.getNotas())
    .reservaEstado(nuevoEstado)
    .reservaTipo(nuevoTipo)
    .build();
```

- [ ] **Step 8: Compilar tras ajustes**

```bash
cd backend && ./mvnw clean compile -q
```

- [ ] **Step 8.5: Verificar documentación**

- Javadoc de clase `ReservaService`: añadir en la lista `<ul>` existente:
  ```java
  *   <li>Modificar una reserva con validación de hora límite y transición de tipo.</li>
  *   <li>Eliminar la pre-orden existente al reemplazarla.</li>
  *   <li>Componer el mensaje precompuesto para WhatsApp en transiciones especiales.</li>
  ```
- `modificarReserva`: Javadoc ya incluido en Step 3. Verificar que cada bloque lógico del
  cuerpo tenga su comentario en línea:

  | Bloque | Comentario esperado |
  |--------|---------------------|
  | findById | `// Verificar existencia de la reserva` |
  | ownership | `// Verificar ownership (CA-02)` |
  | estado activo | `// Verificar que la reserva esté en estado activo` |
  | hora límite | `// Verificar hora límite de modificación: antes de las 16:00 del día de la reserva (CA-01)` |
  | horario | `// Validar horario del restaurante para la nueva fecha/hora (CA-04)` |
  | bloqueos | `// Validar bloqueos administrativos para la nueva fecha/hora (CA-04)` |
  | decoración | `// Validar decoración (CA-04)` |
  | zona | `// Validar zona (CA-04)` |
  | compatibilidad | `// Validar compatibilidad decoración-zona (CA-04)` |
  | disponibilidad | `// Verificar disponibilidad excluyendo la reserva actual (CA-06, CA-07)` |
  | decoración libre | `// Verificar que la decoración elegida esté disponible` |
  | capacidad | `// Verificar capacidad de zona` |
  | pre-orden | `// Validar pre-orden (CA-05)` |
  | tipo | `// Determinar nuevo tipo usando el helper extraído en Task 8.5` |
  | transición | `// ESPECIAL → BASICA (CA-07):...` / `// BASICA→BASICA, BASICA→ESPECIAL, ESPECIAL→ESPECIAL:...` |
  | pre-orden nueva | `// Persistir nueva pre-orden si existe (CA-05)` |
  | WhatsApp | `// Componer mensaje WhatsApp si la transición lo requiere (CA-06, CA-07)` |

- `construirMensajeWhatsApp`: Javadoc e inline comments ya incluidos en Step 4.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/service/ReservaService.java
git commit -m "feat(reservas): implementar modificarReserva con transiciones de tipo y WhatsApp"
```

---

## Task 10: Controller — endpoint PUT /api/reservas/{reservaId}

**Archivos:**
- Modify: `modules/reservas/controller/ReservaController.java`

- [ ] **Step 1: Añadir imports en ReservaController**

```java
import co.edu.unicauca.backend.modules.reservas.dto.request.ModificarReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
```

- [ ] **Step 2: Añadir el endpoint después del `TODO: Agregar endpoints para modificar`**

Reemplazar el comentario `// TODO: Agregar endpoints para modificar reservas futuras` por:

```java
/**
 * Modifica una reserva futura del cliente autenticado.
 *
 * <p>Solo el cliente propietario puede modificar su reserva ({@code ROLE_CLIENTE}).
 * El email del cliente se toma del token de autenticación, nunca del body.
 *
 * <p>Reglas de negocio aplicadas:
 * <ul>
 *   <li>La reserva debe estar en estado {@code PENDIENTE} o {@code CONFIRMADA}.</li>
 *   <li>El momento actual debe ser anterior a las 16:00 del día de la reserva (CA-01).</li>
 *   <li>Aplica las mismas validaciones de horario, bloqueos y disponibilidad que la creación.</li>
 * </ul>
 *
 * <p>Cuando el campo {@code requiereWhatsApp} de la respuesta es {@code true}, el frontend
 * debe redirigir al cliente al chat de WhatsApp de la empresa con el mensaje precompuesto.
 *
 * @param reservaId      identificador de la reserva a modificar
 * @param request        nuevos datos de la reserva
 * @param authentication contexto de seguridad del request
 * @return {@code 200 OK} con los datos de la reserva resultante
 */
@PutMapping("/{reservaId}")
@PreAuthorize("hasRole('CLIENTE')")
@Operation(summary = "Modificar una reserva futura del cliente")
public ResponseEntity<ApiResponse<ModificarReservaResponse>> modificarReserva(
        @PathVariable Long reservaId,
        @Valid @RequestBody ModificarReservaRequest request,
        Authentication authentication) {

    String emailCliente = authentication.getName();
    ModificarReservaResponse response =
            reservaService.modificarReserva(reservaId, emailCliente, request);
    return ResponseEntity.ok(ApiResponse.ok(response));
}

// TODO: cancelar reservas futuras
```
- [ ] **Step 3.5: Verificar documentación**

- Javadoc del endpoint (ya incluido en Step 2): verificar que menciona la regla de ownership,
  la hora límite 16:00, y el contrato del campo `requiereWhatsApp`.
- `@Operation(summary)`: verificar que sea descriptivo para Swagger.
- No se requieren comentarios en línea en el método del controlador (lógica trivial de delegación).

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/reservas/controller/ReservaController.java
git commit -m "feat(reservas): añadir endpoint PUT /api/reservas/{reservaId} para modificar reserva"
```

---

## Tests unitarios y Postman

Los tests unitarios (`ReservaServiceModificarTest`) y los tests Postman (MR-01…MR-12) se planifican en:
[2026-04-18-modificar-reserva-tests.md](./2026-04-18-modificar-reserva-tests.md)


---

## Self-Review

### Cobertura de criterios de aceptación

| CA | Implementado en |
|----|----------------|
| CA-01 Verificación hora límite 4 PM | Task 9 (`modificarReserva` — verificación `HORA_LIMITE_MODIFICACION`) |
| CA-02 Acceso desde historial + verificar modificable | Task 1-2 (campo `modificable` en `ReservaDetalleResponse`) |
| CA-03 Carga formulario con datos existentes | Tasks 1-2 + campos `zonaId`/`decoracionId` ya devueltos por `GET /api/reservas/{id}/detalle` |
| CA-04 Modificación con mismas reglas validación | Task 9 (reutiliza `esHorarioValido`, `estaBloqueda`, `validarCompatibilidadDecoracionZona`) |
| CA-05 Modificación pre-orden | Task 7 (`eliminarPreOrdenExistente`) + Task 9 (llama `validarPreOrden` y `persistirPreOrden`) |
| CA-06 Guardar cambios reserva básica (ambos sub-casos) | Task 9 (transiciones BASICA→BASICA y BASICA→ESPECIAL) |
| CA-07 Guardar cambios reserva especial (ambos sub-casos) | Task 9 (transiciones ESPECIAL→ESPECIAL y ESPECIAL→BASICA) |
| CA-08 Cancelar modificación | No requiere backend (el frontend simplemente no llama al endpoint) |
| CA-09 Recargar página | No requiere backend (datos originales disponibles en `GET /api/reservas/{id}/detalle`) |
| CA-10 Salir del módulo | No requiere backend (el frontend navega sin llamar al endpoint) |

CA-08, CA-09, CA-10 son comportamientos puramente del frontend; el backend no necesita cambios.

### Consistencia de tipos

- `ModificarReservaRequest.fechaHoraLlegada` → `LocalDateTime` ✓ (igual que `CrearReservaRequest`)
- `ModificarReservaResponse.requiereWhatsApp` → `boolean` (primitivo, siempre presente en JSON) ✓
- `ModificarReservaResponse.mensajeWhatsApp` → `String` con `@JsonInclude(NON_NULL)` ✓ (omitido cuando no aplica)
- `reservaId` en respuesta ESPECIAL→BASICA → `Long` del objeto `nuevaReserva` guardado ✓

### Verificación de placeholders

No hay TBDs ni "implementar después" en el plan.

---

# PA-87: Visualizar Mapa de Mesas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar sistema de visualización del mapa de mesas para meseros, permitiendo ver el estado en tiempo real de todas las mesas, filtrar por zona, acceder a detalles y modificar comandas.

**Architecture:** Nuevo controller `MesaController` con 3 endpoints principales: (1) GET /api/mesas para listar mapa completo o filtrado por zona, (2) GET /api/mesas/{mesaId}/detalle para ver información completa de una mesa, (3) GET /api/mesas/{mesaId}/modificar-comanda para obtener resumen de items en producción. WebSocket para actualizaciones en tiempo real. Acceso exclusivo para rol MESERO.

**Tech Stack:** Spring Boot 3.5, Spring Security (JWT), Spring WebSocket, JUnit 5 + Mockito, JaCoCo (coverage ≥90%), Postman YAML

---

## File Structure

**New files to create:**
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/MesaController.java` — REST endpoints para mapa de mesas
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaService.java` — lógica de negocio para obtener mapa, detalle y info modificación
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/MapaMesasResponse.java` — respuesta completa del mapa con zonas
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ZonaMesasResponse.java` — info de zona con sus mesas
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/MesaMapaResponse.java` — info de mesa para el mapa
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/MesaDetalleResponse.java` — detalle completo de mesa
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/MesaModificarComandaResponse.java` — info para modificar comanda
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemComandaEnProduccionResponse.java` — item agrupado en producción
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/NotificacionActivaResponse.java` — notificación activa de mesa
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapper.java` — transformaciones entity→DTO
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaWsPublisher.java` — publicador WebSocket para eventos de mesa
- `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/MesaControllerTest.java` — tests del controller
- `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaServiceTest.java` — tests del service
- `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapperTest.java` — tests del mapper

**Files to modify:**
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/MesaRepository.java` — agregar custom queries
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaRepository.java` — agregar query para items en producción
- `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/repository/NotificacionRepository.java` — agregar query para notificaciones activas por mesa

---

## Task 1: DTOs de Respuesta

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/NotificacionActivaResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/MesaMapaResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ZonaMesasResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/MapaMesasResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemComandaEnProduccionResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/MesaDetalleResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/MesaModificarComandaResponse.java`

- [ ] **Step 1: Crear NotificacionActivaResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para notificación activa de una mesa.
 * Usado en el mapa de mesas para mostrar alertas pendientes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionActivaResponse {
    
    /** ID de la notificación */
    private Long notificacionId;
    
    /** Tipo de notificación: ATENCION, PLATOS_LISTOS, BEBIDAS_LISTAS, CAMBIO */
    private TipoNotificacion tipo;
    
    /** Fecha y hora de emisión */
    private LocalDateTime fechaHora;
}
```

- [ ] **Step 2: Crear MesaMapaResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para representar una mesa en el mapa de mesas.
 * Incluye estado, mesero asignado, notificaciones activas y flag de borrador.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MesaMapaResponse {
    
    /** ID de la visita (PK de Mesa) */
    private Long visitaId;
    
    /** Identificador de la mesa (ej. "T-04") */
    private String identificador;
    
    /** Número de comensales */
    private Integer numeroPersonas;
    
    /** Estado de la mesa: ESPERA, EN_PREPARACION, ATENDIDA, CERRADA */
    private EstadoMesa estado;
    
    /** Nombre completo del mesero asignado */
    private String nombreMesero;
    
    /** Email del mesero asignado */
    private String emailMesero;
    
    /** true si la mesa fue creada por el mesero que hace la petición */
    private Boolean esMesaPropia;
    
    /** true si la mesa tiene al menos una comanda en estado BORRADOR */
    private Boolean tieneBorrador;
    
    /** Lista de notificaciones activas (ATENCION, PLATOS_LISTOS, BEBIDAS_LISTAS, CAMBIO) */
    private List<NotificacionActivaResponse> notificacionesActivas;
}
```

- [ ] **Step 3: Crear ZonaMesasResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para representar una zona con sus mesas activas.
 * Usado en el mapa de mesas para agrupar por zona.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZonaMesasResponse {
    
    /** ID de la zona */
    private Long zonaId;
    
    /** Nombre de la zona */
    private String zonaNombre;
    
    /** Cantidad de mesas activas en esta zona */
    private Integer cantidadMesasActivas;
    
    /** Lista de mesas en la zona */
    private List<MesaMapaResponse> mesas;
}
```

- [ ] **Step 4: Crear MapaMesasResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta completa del mapa de mesas.
 * Agrupa todas las zonas con sus mesas activas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapaMesasResponse {
    
    /** Lista de zonas con sus mesas */
    private List<ZonaMesasResponse> zonas;
}
```

- [ ] **Step 5: Crear ItemComandaEnProduccionResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un item de comanda en producción (agrupado).
 * Agrupa items con mismo nombre y descripción.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemComandaEnProduccionResponse {
    
    /** Nombre del producto */
    private String nombreProducto;
    
    /** Descripción con modificaciones (puede ser null) */
    private String descripcion;
    
    /** Cantidad total agrupada */
    private Integer cantidad;
    
    /** Estado de la comanda: PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO */
    private EstadoComanda estadoComanda;
}
```

- [ ] **Step 6: Crear MesaDetalleResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para el detalle completo de una mesa.
 * Usado en el endpoint GET /api/mesas/{mesaId}/detalle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MesaDetalleResponse {
    
    /** ID de la visita (PK de Mesa) */
    private Long visitaId;
    
    /** Identificador de la mesa */
    private String identificador;
    
    /** Nombre completo del cliente (puede ser null si es walk-in anónimo) */
    private String nombreCliente;
    
    /** Fecha y hora de inicio de la visita */
    private LocalDateTime horaLlegada;
    
    /** Número de comensales */
    private Integer numeroPersonas;
    
    /** Estado de la mesa */
    private EstadoMesa estado;
    
    /** Items de comandas en estados: PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO (agrupados) */
    private List<ItemComandaEnProduccionResponse> itemsComanda;
}
```

- [ ] **Step 7: Crear MesaModificarComandaResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para obtener información para modificar la comanda de una mesa.
 * Usado en el endpoint GET /api/mesas/{mesaId}/modificar-comanda.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MesaModificarComandaResponse {
    
    /** Identificador de la mesa (no editable) */
    private String identificadorMesa;
    
    /** Resumen de items enviados a producción (PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO) */
    private List<ItemComandaEnProduccionResponse> itemsEnProduccion;
}
```

- [ ] **Step 8: Commit DTOs**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/
git commit -m "feat(mesas): add DTOs for mapa de mesas feature>"
```

---

## Task 2: Repository Custom Queries

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/MesaRepository.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaRepository.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/repository/NotificacionRepository.java`

- [ ] **Step 1: Agregar queries a MesaRepository**

```java
// Agregar al final de la interfaz MesaRepository, antes del cierre de llaves

    /**
     * Obtiene todas las mesas activas (con visita que no ha finalizado).
     * NOTA: Una mesa está activa si visita.visitaFechaHoraFin IS NULL.
     *
     * @return lista de mesas activas ordenadas por zona y identificador
     */
    @Query("""
        SELECT m FROM Mesa m
        JOIN FETCH m.visita v
        JOIN FETCH m.zona z
        JOIN FETCH m.mesero me
        JOIN FETCH me.usuario u
        WHERE v.visitaFechaHoraFin IS NULL
        ORDER BY z.zonaNombre, m.mesaIdentificador
        """)
    List<Mesa> findAllMesasActivas();

    /**
     * Obtiene todas las mesas activas de una zona específica.
     *
     * @param zonaId ID de la zona
     * @return lista de mesas activas en la zona
     */
    @Query("""
        SELECT m FROM Mesa m
        JOIN FETCH m.visita v
        JOIN FETCH m.zona z
        JOIN FETCH m.mesero me
        JOIN FETCH me.usuario u
        WHERE v.visitaFechaHoraFin IS NULL
        AND z.zonaId = :zonaId
        ORDER BY m.mesaIdentificador
        """)
    List<Mesa> findMesasActivasByZona(@Param("zonaId") Long zonaId);

    /**
     * Verifica si una mesa tiene al menos una comanda en estado BORRADOR.
     *
     * @param visitaId ID de la visita (PK de Mesa)
     * @return true si existe al menos una comanda en BORRADOR
     */
    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
        FROM Comanda c
        WHERE c.visita.visitaId = :visitaId
        AND c.comandaEstado = 'BORRADOR'
        """)
    boolean existeComandaBorradorEnMesa(@Param("visitaId") Long visitaId);
```

- [ ] **Step 2: Agregar query a ComandaRepository**

```java
// Agregar al final de la interfaz ComandaRepository, antes del cierre de llaves

    /**
     * Obtiene todos los items de comandas en producción de una visita.
     * Estados: PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO.
     *
     * @param visitaId ID de la visita
     * @return lista de ComandaItem con producto y comanda cargados
     */
    @Query("""
        SELECT ci FROM ComandaItem ci
        JOIN FETCH ci.comanda c
        JOIN FETCH ci.producto p
        WHERE c.visita.visitaId = :visitaId
        AND c.comandaEstado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO', 'COMPLETADO')
        ORDER BY p.productoCategoria, p.productoNombre
        """)
    List<ComandaItem> findItemsEnProduccionByVisita(@Param("visitaId") Long visitaId);
```

- [ ] **Step 3: Agregar query a NotificacionRepository**

```java
// Agregar al final de la interfaz NotificacionRepository, antes del cierre de llaves

    /**
     * Obtiene todas las notificaciones activas de una mesa.
     *
     * @param mesaId ID de la mesa (visita_id)
     * @return lista de notificaciones activas ordenadas por fecha DESC
     */
    @Query("""
        SELECT n FROM Notificacion n
        WHERE n.mesa.visitaId = :mesaId
        AND n.notificacionEstado = 'ACTIVA'
        ORDER BY n.notificacionFechaHora DESC
        """)
    List<Notificacion> findNotificacionesActivasByMesa(@Param("mesaId") Long mesaId);
```

- [ ] **Step 4: Commit repository queries**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/MesaRepository.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaRepository.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/repository/NotificacionRepository.java
git commit -m "feat(mesas): add custom queries for mapa de mesas

- MesaRepository: findAllMesasActivas, findMesasActivasByZona, existeComandaBorradorEnMesa
- ComandaRepository: findItemsEnProduccionByVisita
- NotificacionRepository: findNotificacionesActivasByMesa

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: MesaMapper

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapper.java`
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapperTest.java`

- [ ] **Step 1: Escribir test para toNotificacionActivaResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.NotificacionActivaResponse;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MesaMapperTest {

    private final MesaMapper mapper = new MesaMapper();

    @Test
    void toNotificacionActivaResponse_DeberiaMapearCorrectamente() {
        // Arrange
        LocalDateTime fechaHora = LocalDateTime.of(2026, 4, 28, 19, 30);
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
        assertThat(response.getTipo()).isEqualTo(TipoNotificacion.ATENCION);
        assertThat(response.getFechaHora()).isEqualTo(fechaHora);
    }
}
```

- [ ] **Step 2: Ejecutar test (debe fallar)**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaMapperTest#toNotificacionActivaResponse_DeberiaMapearCorrectamente
```

Expected: FAIL - MesaMapper class not found

- [ ] **Step 3: Crear MesaMapper con método toNotificacionActivaResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.*;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper para transformaciones entity→DTO del módulo mesas_comandas.
 * Responsable de mapear Mesa, Notificacion y ComandaItem a sus DTOs de respuesta.
 */
@Component
public class MesaMapper {

    /**
     * Mapea una Notificacion a NotificacionActivaResponse.
     *
     * @param notificacion entidad de notificación
     * @return DTO de notificación activa
     */
    public NotificacionActivaResponse toNotificacionActivaResponse(Notificacion notificacion) {
        return NotificacionActivaResponse.builder()
                .notificacionId(notificacion.getNotificacionId())
                .tipo(notificacion.getNotificacionTipo())
                .fechaHora(notificacion.getNotificacionFechaHora())
                .build();
    }
}
```

- [ ] **Step 4: Ejecutar test (debe pasar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaMapperTest#toNotificacionActivaResponse_DeberiaMapearCorrectamente
```

Expected: PASS

- [ ] **Step 5: Commit mapper inicial**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapper.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapperTest.java
git commit -m "feat(mesas): add MesaMapper with toNotificacionActivaResponse

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

- [ ] **Step 6: Escribir test para toMesaMapaResponse**

```java
// Agregar al final de MesaMapperTest, antes del cierre de llaves

    @Test
    void toMesaMapaResponse_ConNotificacionesYBorrador_DeberiaMapearCorrectamente() {
        // Arrange
        Mesa mesa = crearMesaMock(1L, "T-01", 4, "mesero1@altoro.com", "Juan Pérez");
        List<Notificacion> notificaciones = List.of(
                crearNotificacionMock(1L, TipoNotificacion.ATENCION)
        );
        boolean tieneBorrador = true;
        String emailMeseroActual = "mesero1@altoro.com";

        // Act
        MesaMapaResponse response = mapper.toMesaMapaResponse(mesa, notificaciones, 
                                                               tieneBorrador, emailMeseroActual);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getVisitaId()).isEqualTo(1L);
        assertThat(response.getIdentificador()).isEqualTo("T-01");
        assertThat(response.getNumeroPersonas()).isEqualTo(4);
        assertThat(response.getNombreMesero()).isEqualTo("Juan Pérez");
        assertThat(response.getEmailMesero()).isEqualTo("mesero1@altoro.com");
        assertThat(response.getEsMesaPropia()).isTrue();
        assertThat(response.getTieneBorrador()).isTrue();
        assertThat(response.getNotificacionesActivas()).hasSize(1);
    }

    @Test
    void toMesaMapaResponse_MesaDeOtroMesero_EsMesaPropiaDebe_SerFalse() {
        // Arrange
        Mesa mesa = crearMesaMock(2L, "T-02", 2, "mesero2@altoro.com", "María López");
        String emailMeseroActual = "mesero1@altoro.com";

        // Act
        MesaMapaResponse response = mapper.toMesaMapaResponse(mesa, List.of(), 
                                                               false, emailMeseroActual);

        // Assert
        assertThat(response.getEsMesaPropia()).isFalse();
        assertThat(response.getEmailMesero()).isEqualTo("mesero2@altoro.com");
    }

    // Helper methods
    private Mesa crearMesaMock(Long visitaId, String identificador, int personas, 
                                String emailMesero, String nombreMesero) {
        // Simulación simplificada - en test real usar mocks completos
        Mesa mesa = new Mesa();
        mesa.setVisitaId(visitaId);
        mesa.setMesaIdentificador(identificador);
        mesa.setMesaNumeroPersonas(personas);
        mesa.setMesaEstado(co.edu.unicauca.backend.shared.enums.EstadoMesa.EN_PREPARACION);
        
        // Mesero mock
        co.edu.unicauca.backend.modules.usuarios.entity.Empleado mesero = 
            new co.edu.unicauca.backend.modules.usuarios.entity.Empleado();
        co.edu.unicauca.backend.modules.usuarios.entity.Usuario usuario = 
            new co.edu.unicauca.backend.modules.usuarios.entity.Usuario();
        usuario.setUsuarioEmail(emailMesero);
        usuario.setUsuarioNombre(nombreMesero.split(" ")[0]);
        usuario.setUsuarioApellido(nombreMesero.split(" ")[1]);
        mesero.setUsuario(usuario);
        mesa.setMesero(mesero);
        
        return mesa;
    }

    private Notificacion crearNotificacionMock(Long id, TipoNotificacion tipo) {
        return Notificacion.builder()
                .notificacionId(id)
                .notificacionTipo(tipo)
                .notificacionFechaHora(LocalDateTime.now())
                .build();
    }
```

- [ ] **Step 7: Ejecutar test (debe fallar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaMapperTest#toMesaMapaResponse_ConNotificacionesYBorrador_DeberiaMapearCorrectamente
```

Expected: FAIL - method toMesaMapaResponse not found

- [ ] **Step 8: Implementar toMesaMapaResponse en MesaMapper**

```java
// Agregar al final de MesaMapper, antes del cierre de llaves

    /**
     * Mapea una Mesa a MesaMapaResponse para el mapa de mesas.
     *
     * @param mesa entidad de mesa
     * @param notificaciones lista de notificaciones activas
     * @param tieneBorrador true si tiene comanda en estado BORRADOR
     * @param emailMeseroActual email del mesero que hace la petición
     * @return DTO de mesa para el mapa
     */
    public MesaMapaResponse toMesaMapaResponse(Mesa mesa, 
                                                List<Notificacion> notificaciones,
                                                boolean tieneBorrador,
                                                String emailMeseroActual) {
        String emailMesero = mesa.getMesero().getUsuario().getUsuarioEmail();
        String nombreCompleto = mesa.getMesero().getUsuario().getUsuarioNombre() + " " +
                                mesa.getMesero().getUsuario().getUsuarioApellido();
        
        List<NotificacionActivaResponse> notificacionesDto = notificaciones.stream()
                .map(this::toNotificacionActivaResponse)
                .collect(Collectors.toList());
        
        return MesaMapaResponse.builder()
                .visitaId(mesa.getVisitaId())
                .identificador(mesa.getMesaIdentificador())
                .numeroPersonas(mesa.getMesaNumeroPersonas())
                .estado(mesa.getMesaEstado())
                .nombreMesero(nombreCompleto)
                .emailMesero(emailMesero)
                .esMesaPropia(emailMesero.equals(emailMeseroActual))
                .tieneBorrador(tieneBorrador)
                .notificacionesActivas(notificacionesDto)
                .build();
    }
```

- [ ] **Step 9: Ejecutar tests (deben pasar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaMapperTest
```

Expected: PASS (todos los tests de MesaMapperTest)

- [ ] **Step 10: Commit toMesaMapaResponse**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapper.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapperTest.java
git commit -m "feat(mesas): add toMesaMapaResponse to MesaMapper

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

- [ ] **Step 11: Escribir test para agruparItemsEnProduccion**

```java
// Agregar al final de MesaMapperTest, antes del cierre de llaves

    @Test
    void agruparItemsEnProduccion_DeberiaAgruparPorNombreYDescripcion() {
        // Arrange
        List<ComandaItem> items = List.of(
                crearComandaItemMock("Bandeja Paisa", "Sin carne", 
                                      co.edu.unicauca.backend.shared.enums.EstadoComanda.EN_PREPARACION),
                crearComandaItemMock("Bandeja Paisa", "Sin carne", 
                                      co.edu.unicauca.backend.shared.enums.EstadoComanda.EN_PREPARACION),
                crearComandaItemMock("Bandeja Paisa", "Extra aguacate", 
                                      co.edu.unicauca.backend.shared.enums.EstadoComanda.PENDIENTE),
                crearComandaItemMock("Cerveza", null, 
                                      co.edu.unicauca.backend.shared.enums.EstadoComanda.LISTO)
        );

        // Act
        List<ItemComandaEnProduccionResponse> resultado = mapper.agruparItemsEnProduccion(items);

        // Assert
        assertThat(resultado).hasSize(3);
        
        // Bandeja Paisa sin carne: 2 items agrupados
        ItemComandaEnProduccionResponse item1 = resultado.stream()
                .filter(i -> i.getNombreProducto().equals("Bandeja Paisa") 
                             && "Sin carne".equals(i.getDescripcion()))
                .findFirst().orElseThrow();
        assertThat(item1.getCantidad()).isEqualTo(2);
        
        // Bandeja Paisa extra aguacate: 1 item
        ItemComandaEnProduccionResponse item2 = resultado.stream()
                .filter(i -> i.getNombreProducto().equals("Bandeja Paisa") 
                             && "Extra aguacate".equals(i.getDescripcion()))
                .findFirst().orElseThrow();
        assertThat(item2.getCantidad()).isEqualTo(1);
        
        // Cerveza: 1 item
        ItemComandaEnProduccionResponse item3 = resultado.stream()
                .filter(i -> i.getNombreProducto().equals("Cerveza"))
                .findFirst().orElseThrow();
        assertThat(item3.getCantidad()).isEqualTo(1);
    }

    private ComandaItem crearComandaItemMock(String nombreProducto, String descripcion,
                                              co.edu.unicauca.backend.shared.enums.EstadoComanda estado) {
        ComandaItem item = new ComandaItem();
        
        co.edu.unicauca.backend.modules.produccion.entity.Producto producto = 
            new co.edu.unicauca.backend.modules.produccion.entity.Producto();
        producto.setProductoNombre(nombreProducto);
        producto.setProductoCategoria(co.edu.unicauca.backend.shared.enums.CategoriaProducto.PLATO);
        item.setProducto(producto);
        
        co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda comanda = 
            new co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda();
        comanda.setComandaEstado(estado);
        item.setComanda(comanda);
        
        item.setComandaItemCantidad(1);
        item.setComandaItemDescripcion(descripcion);
        
        return item;
    }
```

- [ ] **Step 12: Ejecutar test (debe fallar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaMapperTest#agruparItemsEnProduccion_DeberiaAgruparPorNombreYDescripcion
```

Expected: FAIL - method agruparItemsEnProduccion not found

- [ ] **Step 13: Implementar agruparItemsEnProduccion**

```java
// Agregar al final de MesaMapper, antes del cierre de llaves

    /**
     * Agrupa items de comanda por nombre y descripción.
     * Suma las cantidades de items con mismo nombre y descripción.
     *
     * @param items lista de ComandaItem en producción
     * @return lista de ItemComandaEnProduccionResponse agrupados
     */
    public List<ItemComandaEnProduccionResponse> agruparItemsEnProduccion(List<ComandaItem> items) {
        // Clave de agrupación: nombreProducto + descripción + estadoComanda
        Map<String, ItemComandaEnProduccionResponse> agrupados = items.stream()
                .collect(Collectors.groupingBy(
                        item -> {
                            String nombre = item.getProducto().getProductoNombre();
                            String desc = item.getComandaItemDescripcion() != null 
                                          ? item.getComandaItemDescripcion() 
                                          : "";
                            String estado = item.getComanda().getComandaEstado().name();
                            return nombre + "|" + desc + "|" + estado;
                        },
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    ComandaItem primero = list.get(0);
                                    int cantidadTotal = list.stream()
                                            .mapToInt(ComandaItem::getComandaItemCantidad)
                                            .sum();
                                    
                                    return ItemComandaEnProduccionResponse.builder()
                                            .nombreProducto(primero.getProducto().getProductoNombre())
                                            .descripcion(primero.getComandaItemDescripcion())
                                            .cantidad(cantidadTotal)
                                            .estadoComanda(primero.getComanda().getComandaEstado())
                                            .build();
                                }
                        )
                ));
        
        return new ArrayList<>(agrupados.values());
    }
```

- [ ] **Step 14: Ejecutar test (debe pasar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaMapperTest#agruparItemsEnProduccion_DeberiaAgruparPorNombreYDescripcion
```

Expected: PASS

- [ ] **Step 15: Commit agruparItemsEnProduccion**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapper.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapperTest.java
git commit -m "feat(mesas): add agruparItemsEnProduccion to MesaMapper

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

- [ ] **Step 16: Implementar resto de métodos del mapper sin tests (métodos simples)**

```java
// Agregar al final de MesaMapper, antes del cierre de llaves

    /**
     * Mapea una Mesa a MesaDetalleResponse.
     *
     * @param mesa entidad de mesa
     * @param itemsAgrupados items en producción agrupados
     * @return DTO de detalle de mesa
     */
    public MesaDetalleResponse toMesaDetalleResponse(Mesa mesa, 
                                                      List<ItemComandaEnProduccionResponse> itemsAgrupados) {
        String nombreCliente = null;
        if (mesa.getVisita().getCliente() != null) {
            co.edu.unicauca.backend.modules.usuarios.entity.Usuario usuario = 
                mesa.getVisita().getCliente().getUsuario();
            nombreCliente = usuario.getUsuarioNombre() + " " + usuario.getUsuarioApellido();
        }
        
        return MesaDetalleResponse.builder()
                .visitaId(mesa.getVisitaId())
                .identificador(mesa.getMesaIdentificador())
                .nombreCliente(nombreCliente)
                .horaLlegada(mesa.getVisita().getVisitaFechaHoraInicio())
                .numeroPersonas(mesa.getMesaNumeroPersonas())
                .estado(mesa.getMesaEstado())
                .itemsComanda(itemsAgrupados)
                .build();
    }

    /**
     * Mapea a MesaModificarComandaResponse.
     *
     * @param identificadorMesa identificador de la mesa
     * @param itemsAgrupados items en producción agrupados
     * @return DTO para modificar comanda
     */
    public MesaModificarComandaResponse toMesaModificarComandaResponse(
            String identificadorMesa,
            List<ItemComandaEnProduccionResponse> itemsAgrupados) {
        return MesaModificarComandaResponse.builder()
                .identificadorMesa(identificadorMesa)
                .itemsEnProduccion(itemsAgrupados)
                .build();
    }
```

- [ ] **Step 17: Commit mapper completo**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/MesaMapper.java
git commit -m "feat(mesas): add toMesaDetalleResponse and toMesaModificarComandaResponse

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: MesaService

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaService.java`
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaServiceTest.java`

- [ ] **Step 1: Escribir test para obtenerMapaMesas sin filtro de zona**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MapaMesasResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ZonaMesasResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.MesaMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesaServiceTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private ZonaRepository zonaRepository;

    @Mock
    private ComandaRepository comandaRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private MesaMapper mesaMapper;

    @InjectMocks
    private MesaService mesaService;

    @Test
    void obtenerMapaMesas_SinZonaId_DeberiaRetornarTodasLasZonas() {
        // Arrange
        String emailMesero = "mesero1@altoro.com";
        Zona zona1 = crearZonaMock(1L, "Terraza");
        Zona zona2 = crearZonaMock(2L, "Interior");
        
        Mesa mesa1 = crearMesaMock(1L, zona1);
        Mesa mesa2 = crearMesaMock(2L, zona2);
        
        when(zonaRepository.findAll()).thenReturn(List.of(zona1, zona2));
        when(mesaRepository.findAllMesasActivas()).thenReturn(List.of(mesa1, mesa2));
        when(notificacionRepository.findNotificacionesActivasByMesa(anyLong())).thenReturn(List.of());
        when(mesaRepository.existeComandaBorradorEnMesa(anyLong())).thenReturn(false);
        when(mesaMapper.toMesaMapaResponse(any(), any(), anyBoolean(), anyString()))
                .thenReturn(new co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaMapaResponse());

        // Act
        MapaMesasResponse resultado = mesaService.obtenerMapaMesas(null, emailMesero);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getZonas()).hasSize(2);
        verify(zonaRepository).findAll();
        verify(mesaRepository).findAllMesasActivas();
        verify(mesaMapper, times(2)).toMesaMapaResponse(any(), any(), anyBoolean(), eq(emailMesero));
    }

    private Zona crearZonaMock(Long id, String nombre) {
        Zona zona = new Zona();
        zona.setZonaId(id);
        zona.setZonaNombre(nombre);
        return zona;
    }

    private Mesa crearMesaMock(Long visitaId, Zona zona) {
        Mesa mesa = new Mesa();
        mesa.setVisitaId(visitaId);
        mesa.setZona(zona);
        
        co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita visita = 
            new co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita();
        visita.setVisitaId(visitaId);
        mesa.setVisita(visita);
        
        co.edu.unicauca.backend.modules.usuarios.entity.Empleado mesero = 
            new co.edu.unicauca.backend.modules.usuarios.entity.Empleado();
        co.edu.unicauca.backend.modules.usuarios.entity.Usuario usuario = 
            new co.edu.unicauca.backend.modules.usuarios.entity.Usuario();
        usuario.setUsuarioEmail("mesero1@altoro.com");
        mesero.setUsuario(usuario);
        mesa.setMesero(mesero);
        
        return mesa;
    }
}
```

- [ ] **Step 2: Ejecutar test (debe fallar)**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaServiceTest#obtenerMapaMesas_SinZonaId_DeberiaRetornarTodasLasZonas
```

Expected: FAIL - MesaService class not found

- [ ] **Step 3: Crear MesaService con método obtenerMapaMesas**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.*;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.MesaMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para gestión del mapa de mesas.
 * 
 * <p>Proporciona operaciones de consulta para el mapa de mesas del restaurante,
 * incluyendo filtrado por zona, visualización de estado en tiempo real,
 * y detalle de mesas individuales.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MesaService {

    private final MesaRepository mesaRepository;
    private final ZonaRepository zonaRepository;
    private final ComandaRepository comandaRepository;
    private final NotificacionRepository notificacionRepository;
    private final MesaMapper mesaMapper;

    /**
     * Obtiene el mapa completo de mesas, opcionalmente filtrado por zona.
     * 
     * <p>Incluye todas las zonas del restaurante, mostrando para cada una:
     * - ID, nombre y cantidad de mesas activas
     * - Lista de mesas con estado, mesero, notificaciones y flag de borrador
     * 
     * <p>Si zonaId es null, devuelve todas las zonas (incluso sin mesas).
     * Si zonaId es especificado, devuelve solo esa zona.
     * 
     * @param zonaId ID de zona (null = todas las zonas)
     * @param emailMesero email del mesero que hace la petición (para flag esMesaPropia)
     * @return MapaMesasResponse con zonas y mesas
     */
    public MapaMesasResponse obtenerMapaMesas(Long zonaId, String emailMesero) {
        log.debug("Obteniendo mapa de mesas - zonaId: {}, emailMesero: {}", zonaId, emailMesero);
        
        // 1. Obtener zonas
        List<Zona> zonas = zonaId != null 
                ? zonaRepository.findById(zonaId)
                    .map(List::of)
                    .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada"))
                : zonaRepository.findAll();
        
        // 2. Obtener mesas activas
        List<Mesa> mesasActivas = zonaId != null
                ? mesaRepository.findMesasActivasByZona(zonaId)
                : mesaRepository.findAllMesasActivas();
        
        // 3. Agrupar mesas por zona
        Map<Long, List<Mesa>> mesasPorZona = mesasActivas.stream()
                .collect(Collectors.groupingBy(mesa -> mesa.getZona().getZonaId()));
        
        // 4. Construir respuesta
        List<ZonaMesasResponse> zonasResponse = zonas.stream()
                .map(zona -> {
                    List<Mesa> mesasZona = mesasPorZona.getOrDefault(zona.getZonaId(), List.of());
                    List<MesaMapaResponse> mesasDto = mapearMesas(mesasZona, emailMesero);
                    
                    return ZonaMesasResponse.builder()
                            .zonaId(zona.getZonaId())
                            .zonaNombre(zona.getZonaNombre())
                            .cantidadMesasActivas(mesasZona.size())
                            .mesas(mesasDto)
                            .build();
                })
                .collect(Collectors.toList());
        
        return MapaMesasResponse.builder()
                .zonas(zonasResponse)
                .build();
    }

    /**
     * Mapea lista de mesas a DTOs, obteniendo notificaciones y flag de borrador.
     */
    private List<MesaMapaResponse> mapearMesas(List<Mesa> mesas, String emailMesero) {
        return mesas.stream()
                .map(mesa -> {
                    Long visitaId = mesa.getVisitaId();
                    
                    // Obtener notificaciones activas
                    List<Notificacion> notificaciones = 
                        notificacionRepository.findNotificacionesActivasByMesa(visitaId);
                    
                    // Verificar si tiene comanda en borrador
                    boolean tieneBorrador = mesaRepository.existeComandaBorradorEnMesa(visitaId);
                    
                    // Mapear a DTO
                    return mesaMapper.toMesaMapaResponse(mesa, notificaciones, 
                                                          tieneBorrador, emailMesero);
                })
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 4: Ejecutar test (debe pasar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaServiceTest#obtenerMapaMesas_SinZonaId_DeberiaRetornarTodasLasZonas
```

Expected: PASS

- [ ] **Step 5: Commit obtenerMapaMesas**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaService.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaServiceTest.java
git commit -m "feat(mesas): add MesaService.obtenerMapaMesas

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

- [ ] **Step 6: Escribir test para obtenerDetalleMesa**

```java
// Agregar al final de MesaServiceTest, antes del cierre de llaves

    @Test
    void obtenerDetalleMesa_ConMesaExistente_DeberiaRetornarDetalle() {
        // Arrange
        Long visitaId = 1L;
        Mesa mesa = crearMesaMock(visitaId, crearZonaMock(1L, "Terraza"));
        
        when(mesaRepository.findById(visitaId)).thenReturn(java.util.Optional.of(mesa));
        when(comandaRepository.findItemsEnProduccionByVisita(visitaId)).thenReturn(List.of());
        when(mesaMapper.agruparItemsEnProduccion(any())).thenReturn(List.of());
        when(mesaMapper.toMesaDetalleResponse(any(), any()))
                .thenReturn(new co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaDetalleResponse());

        // Act
        co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaDetalleResponse resultado = 
            mesaService.obtenerDetalleMesa(visitaId);

        // Assert
        assertThat(resultado).isNotNull();
        verify(mesaRepository).findById(visitaId);
        verify(comandaRepository).findItemsEnProduccionByVisita(visitaId);
        verify(mesaMapper).agruparItemsEnProduccion(any());
        verify(mesaMapper).toMesaDetalleResponse(eq(mesa), any());
    }

    @Test
    void obtenerDetalleMesa_ConMesaInexistente_DeberiaLanzarExcepcion() {
        // Arrange
        Long visitaId = 999L;
        when(mesaRepository.findById(visitaId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> mesaService.obtenerDetalleMesa(visitaId),
                "Mesa no encontrada"
        );
    }
```

- [ ] **Step 7: Ejecutar test (debe fallar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaServiceTest#obtenerDetalleMesa_ConMesaExistente_DeberiaRetornarDetalle
```

Expected: FAIL - method obtenerDetalleMesa not found

- [ ] **Step 8: Implementar obtenerDetalleMesa**

```java
// Agregar al final de MesaService, antes del cierre de llaves

    /**
     * Obtiene el detalle completo de una mesa.
     * 
     * <p>Incluye:
     * - Identificador de mesa, nombre del cliente (si existe), hora de llegada
     * - Número de personas, estado de la mesa
     * - Items de comandas en producción (PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO)
     *   agrupados por nombre y descripción
     * 
     * @param visitaId ID de la visita (PK de Mesa)
     * @return MesaDetalleResponse con información completa
     * @throws IllegalArgumentException si la mesa no existe
     */
    public MesaDetalleResponse obtenerDetalleMesa(Long visitaId) {
        log.debug("Obteniendo detalle de mesa - visitaId: {}", visitaId);
        
        // 1. Obtener mesa
        Mesa mesa = mesaRepository.findById(visitaId)
                .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));
        
        // 2. Obtener items en producción
        List<co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem> items = 
            comandaRepository.findItemsEnProduccionByVisita(visitaId);
        
        // 3. Agrupar items
        List<ItemComandaEnProduccionResponse> itemsAgrupados = 
            mesaMapper.agruparItemsEnProduccion(items);
        
        // 4. Mapear a DTO
        return mesaMapper.toMesaDetalleResponse(mesa, itemsAgrupados);
    }
```

- [ ] **Step 9: Ejecutar tests (deben pasar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaServiceTest
```

Expected: PASS (todos los tests de MesaServiceTest)

- [ ] **Step 10: Commit obtenerDetalleMesa**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaService.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaServiceTest.java
git commit -m "feat(mesas): add MesaService.obtenerDetalleMesa

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

- [ ] **Step 11: Implementar obtenerInfoModificarComanda (sin test previo - método simple)**

```java
// Agregar al final de MesaService, antes del cierre de llaves

    /**
     * Obtiene información para modificar la comanda de una mesa.
     * 
     * <p>Devuelve:
     * - Identificador de la mesa
     * - Resumen de items enviados a producción (no modificables)
     * 
     * <p>Solo items en estados PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO.
     * 
     * @param visitaId ID de la visita (PK de Mesa)
     * @return MesaModificarComandaResponse con identificador y items en producción
     * @throws IllegalArgumentException si la mesa no existe
     */
    public MesaModificarComandaResponse obtenerInfoModificarComanda(Long visitaId) {
        log.debug("Obteniendo info modificar comanda - visitaId: {}", visitaId);
        
        // 1. Obtener mesa
        Mesa mesa = mesaRepository.findById(visitaId)
                .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));
        
        // 2. Obtener items en producción
        List<co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem> items = 
            comandaRepository.findItemsEnProduccionByVisita(visitaId);
        
        // 3. Agrupar items
        List<ItemComandaEnProduccionResponse> itemsAgrupados = 
            mesaMapper.agruparItemsEnProduccion(items);
        
        // 4. Mapear a DTO
        return mesaMapper.toMesaModificarComandaResponse(
                mesa.getMesaIdentificador(), 
                itemsAgrupados
        );
    }
```

- [ ] **Step 12: Commit obtenerInfoModificarComanda**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaService.java
git commit -m "feat(mesas): add MesaService.obtenerInfoModificarComanda

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: MesaController

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/MesaController.java`
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/MesaControllerTest.java`

- [ ] **Step 1: Escribir test para GET /api/mesas sin filtro**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MapaMesasResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaService;
import co.edu.unicauca.backend.shared.security.CustomUserDetailsService;
import co.edu.unicauca.backend.shared.security.JwtAuthenticationFilter;
import co.edu.unicauca.backend.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MesaController.class)
class MesaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MesaService mesaService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username = "mesero1@altoro.com", roles = {"MESERO"})
    void obtenerMapaMesas_SinZonaId_DeberiaRetornar200() throws Exception {
        // Arrange
        MapaMesasResponse response = MapaMesasResponse.builder()
                .zonas(List.of())
                .build();
        
        when(mesaService.obtenerMapaMesas(isNull(), anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/mesas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.zonas").isArray());
        
        verify(mesaService).obtenerMapaMesas(null, "mesero1@altoro.com");
    }

    @Test
    @WithMockUser(username = "cliente1@altoro.com", roles = {"CLIENTE"})
    void obtenerMapaMesas_UsuarioNoMesero_DeberiaRetornar403() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/mesas"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Ejecutar test (debe fallar)**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaControllerTest#obtenerMapaMesas_SinZonaId_DeberiaRetornar200
```

Expected: FAIL - MesaController class not found

- [ ] **Step 3: Crear MesaController con endpoint GET /api/mesas**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MapaMesasResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaModificarComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gestión del mapa de mesas.
 * 
 * <p>Endpoints:
 * - GET /api/mesas → mapa completo o filtrado por zona
 * - GET /api/mesas/{mesaId}/detalle → detalle de mesa específica
 * - GET /api/mesas/{mesaId}/modificar-comanda → info para modificar comanda
 */
@RestController
@RequestMapping("/api/mesas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mesas", description = "API de mapa de mesas para meseros")
public class MesaController {

    private final MesaService mesaService;

    /**
     * Obtiene el mapa de mesas del restaurante.
     * 
     * <p>CA-01, CA-02, CA-03, CA-05, CA-06
     * 
     * <p>Sin parámetro zonaId: devuelve todas las zonas con sus mesas activas.
     * Con parámetro zonaId: filtra solo las mesas de esa zona.
     * 
     * <p>Cada zona incluye:
     * - ID, nombre, cantidad de mesas activas
     * - Lista de mesas con estado, mesero, notificaciones activas, flag de borrador
     * 
     * <p>Las mesas propias del mesero se identifican con esMesaPropia=true.
     * 
     * @param zonaId ID de zona (opcional)
     * @param authentication autenticación del mesero
     * @return MapaMesasResponse con zonas y mesas
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Obtener mapa de mesas", 
               description = "Lista todas las zonas con sus mesas activas. Puede filtrarse por zona.")
    public ResponseEntity<ApiResponse<MapaMesasResponse>> obtenerMapaMesas(
            @Parameter(description = "ID de zona (null = todas las zonas)")
            @RequestParam(required = false) Long zonaId,
            Authentication authentication) {
        
        String emailMesero = authentication.getName();
        log.info("GET /api/mesas - zonaId: {}, mesero: {}", zonaId, emailMesero);
        
        MapaMesasResponse mapa = mesaService.obtenerMapaMesas(zonaId, emailMesero);
        
        return ResponseEntity.ok(ApiResponse.success(mapa, "Mapa de mesas obtenido exitosamente"));
    }
}
```

- [ ] **Step 4: Ejecutar test (debe pasar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaControllerTest#obtenerMapaMesas_SinZonaId_DeberiaRetornar200
```

Expected: PASS

- [ ] **Step 5: Commit endpoint GET /api/mesas**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/MesaController.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/MesaControllerTest.java
git commit -m "feat(mesas): add GET /api/mesas endpoint

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

- [ ] **Step 6: Escribir test para GET /api/mesas/{mesaId}/detalle**

```java
// Agregar al final de MesaControllerTest, antes del cierre de llaves

    @Test
    @WithMockUser(username = "mesero1@altoro.com", roles = {"MESERO"})
    void obtenerDetalleMesa_ConMesaExistente_DeberiaRetornar200() throws Exception {
        // Arrange
        Long visitaId = 1L;
        co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaDetalleResponse response = 
            co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaDetalleResponse.builder()
                .visitaId(visitaId)
                .identificador("T-01")
                .build();
        
        when(mesaService.obtenerDetalleMesa(visitaId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/mesas/{mesaId}/detalle", visitaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.visitaId").value(1))
                .andExpect(jsonPath("$.data.identificador").value("T-01"));
        
        verify(mesaService).obtenerDetalleMesa(visitaId);
    }

    @Test
    @WithMockUser(username = "mesero1@altoro.com", roles = {"MESERO"})
    void obtenerDetalleMesa_ConMesaInexistente_DeberiaRetornar400() throws Exception {
        // Arrange
        Long visitaId = 999L;
        when(mesaService.obtenerDetalleMesa(visitaId))
                .thenThrow(new IllegalArgumentException("Mesa no encontrada"));

        // Act & Assert
        mockMvc.perform(get("/api/mesas/{mesaId}/detalle", visitaId))
                .andExpect(status().isBadRequest());
    }
```

- [ ] **Step 7: Ejecutar test (debe fallar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaControllerTest#obtenerDetalleMesa_ConMesaExistente_DeberiaRetornar200
```

Expected: FAIL - endpoint not found (404)

- [ ] **Step 8: Agregar endpoint GET /api/mesas/{mesaId}/detalle**

```java
// Agregar al final de MesaController, antes del cierre de llaves

    /**
     * Obtiene el detalle completo de una mesa.
     * 
     * <p>Incluye:
     * - Identificador de mesa, nombre del cliente, hora de llegada
     * - Número de personas, estado de la mesa
     * - Items de comandas en producción agrupados
     * 
     * @param mesaId ID de la mesa (visita_id)
     * @return MesaDetalleResponse con información completa
     */
    @GetMapping("/{mesaId}/detalle")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Obtener detalle de mesa", 
               description = "Devuelve información detallada de una mesa específica")
    public ResponseEntity<ApiResponse<MesaDetalleResponse>> obtenerDetalleMesa(
            @Parameter(description = "ID de la mesa (visita_id)")
            @PathVariable Long mesaId) {
        
        log.info("GET /api/mesas/{}/detalle", mesaId);
        
        MesaDetalleResponse detalle = mesaService.obtenerDetalleMesa(mesaId);
        
        return ResponseEntity.ok(ApiResponse.success(detalle, "Detalle de mesa obtenido exitosamente"));
    }
```

- [ ] **Step 9: Ejecutar tests (deben pasar)**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml test -Dtest=MesaControllerTest
```

Expected: PASS (todos los tests de MesaControllerTest)

- [ ] **Step 10: Commit endpoint detalle**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/MesaController.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/MesaControllerTest.java
git commit -m "feat(mesas): add GET /api/mesas/{mesaId}/detalle endpoint

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

- [ ] **Step 11: Agregar endpoint GET /api/mesas/{mesaId}/modificar-comanda (sin test previo - simple)**

```java
// Agregar al final de MesaController, antes del cierre de llaves

    /**
     * Obtiene información para modificar la comanda de una mesa.

     * <p>Devuelve:
     * - Identificador de la mesa (no editable)
     * - Resumen de items enviados a producción (no modificables)
     * 
     * @param mesaId ID de la mesa (visita_id)
     * @return MesaModificarComandaResponse con identificador y items en producción
     */
    @GetMapping("/{mesaId}/modificar-comanda")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Obtener info para modificar comanda", 
               description = "Devuelve el resumen de items en producción de una mesa")
    public ResponseEntity<ApiResponse<MesaModificarComandaResponse>> obtenerInfoModificarComanda(
            @Parameter(description = "ID de la mesa (visita_id)")
            @PathVariable Long mesaId) {
        
        log.info("GET /api/mesas/{}/modificar-comanda", mesaId);
        
        MesaModificarComandaResponse info = mesaService.obtenerInfoModificarComanda(mesaId);
        
        return ResponseEntity.ok(ApiResponse.success(info, 
                "Información para modificar comanda obtenida exitosamente"));
    }
```

- [ ] **Step 12: Commit endpoint modificar-comanda**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/MesaController.java
git commit -m "feat(mesas): add GET /api/mesas/{mesaId}/modificar-comanda endpoint

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 6: WebSocket Publisher para Actualizaciones en Tiempo Real

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaWsPublisher.java`

- [ ] **Step 1: Crear MesaWsPublisher**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publicador de mensajes WebSocket para actualizaciones del mapa de mesas.
 * 
 * <p>Publica eventos cuando:
 * - Se crea una mesa (nueva visita)
 * - Cambia el estado de una mesa
 * - Se cierra una mesa
 * - Se crea/atiende una notificación en una mesa
 * 
 * <p>Destino: /topic/mesas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MesaWsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_MESAS = "/topic/mesas";

    /**
     * Publica evento de actualización de mesa.
     * 
     * @param visitaId ID de la visita/mesa
     * @param tipoEvento tipo de evento (CREAR, ACTUALIZAR, CERRAR, NOTIFICACION)
     */
    public void publicarActualizacionMesa(Long visitaId, TipoEventoMesa tipoEvento) {
        log.debug("Publicando evento WebSocket - visitaId: {}, tipo: {}", visitaId, tipoEvento);
        
        MesaWsMessage mensaje = MesaWsMessage.builder()
                .visitaId(visitaId)
                .tipoEvento(tipoEvento)
                .timestamp(System.currentTimeMillis())
                .build();
        
        messagingTemplate.convertAndSend(TOPIC_MESAS, mensaje);
    }

    /**
     * Publica evento de cambio de estado de mesa.
     * 
     * @param visitaId ID de la visita/mesa
     * @param nuevoEstado nuevo estado de la mesa
     */
    public void publicarCambioEstadoMesa(Long visitaId, EstadoMesa nuevoEstado) {
        log.debug("Publicando cambio de estado - visitaId: {}, estado: {}", visitaId, nuevoEstado);
        
        MesaWsMessage mensaje = MesaWsMessage.builder()
                .visitaId(visitaId)
                .tipoEvento(TipoEventoMesa.ACTUALIZAR)
                .nuevoEstado(nuevoEstado)
                .timestamp(System.currentTimeMillis())
                .build();
        
        messagingTemplate.convertAndSend(TOPIC_MESAS, mensaje);
    }

    /**
     * Mensaje WebSocket para eventos de mesa.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MesaWsMessage {
        private Long visitaId;
        private TipoEventoMesa tipoEvento;
        private EstadoMesa nuevoEstado;
        private Long timestamp;
    }

    /**
     * Tipos de eventos de mesa.
     */
    public enum TipoEventoMesa {
        /** Nueva mesa creada */
        CREAR,
        /** Mesa actualizada (cambio de estado, notificación, etc.) */
        ACTUALIZAR,
        /** Mesa cerrada (visita finalizada) */
        CERRAR,
        /** Nueva notificación en la mesa */
        NOTIFICACION
    }
}
```

- [ ] **Step 2: Commit MesaWsPublisher**

```bash
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaWsPublisher.java
git commit -m "feat(mesas): add MesaWsPublisher for real-time updates

Publica eventos WebSocket a /topic/mesas cuando:
- Se crea una mesa
- Cambia el estado de una mesa
- Se cierra una mesa
- Se crea/atiende una notificación

CA-07: Actualización en tiempo real del mapa

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 7: Postman Manual Testing Collection

**Files:**
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/80-01 Obtener mapa mesas todas zonas - MESERO.request.yaml`
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/80-02 Obtener mapa mesas zona especifica - MESERO.request.yaml`
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/80-03 Obtener detalle mesa - MESERO.request.yaml`
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/80-04 Obtener info modificar comanda - MESERO.request.yaml`

- [ ] **Step 1: Crear 80-01 Obtener mapa mesas todas zonas**

```yaml
name: 80-01 Obtener mapa mesas todas zonas – MESERO
method: GET
url: "{{baseUrl}}/api/mesas"
headers:
  - key: Authorization
    value: "Bearer {{tmpMeseroToken}}"
scripts:
  - type: beforeRequest
    code: |-
      // Autonomous login MESERO
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: 'mesero1@altoro.com',
            password: 'Password123',
            forceSessionOverride: true
          })
        }
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('tmpMeseroToken', res.json().accessToken);
        } else {
          console.warn('80-01: login MESERO falló', err, res && res.code);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('El sistema retorna HTTP 200', function () {
        pm.response.to.have.status(200);
      });
      
      pm.test('La respuesta tiene estructura correcta', function () {
        const body = pm.response.json();
        pm.expect(body.success).to.be.true;
        pm.expect(body.data).to.have.property('zonas');
        pm.expect(body.data.zonas).to.be.an('array');
      });
      
      pm.test('Cada zona tiene estructura correcta', function () {
        const body = pm.response.json();
        if (body.data.zonas.length > 0) {
          const zona = body.data.zonas[0];
          pm.expect(zona).to.have.property('zonaId');
          pm.expect(zona).to.have.property('zonaNombre');
          pm.expect(zona).to.have.property('cantidadMesasActivas');
          pm.expect(zona).to.have.property('mesas');
          pm.expect(zona.mesas).to.be.an('array');
        }
      });
      
      // Cleanup
      pm.environment.unset('tmpMeseroToken');
    language: text/javascript
```

- [ ] **Step 2: Crear 80-02 Obtener mapa mesas zona específica**

```yaml
name: 80-02 Obtener mapa mesas zona especifica – MESERO
method: GET
url: "{{baseUrl}}/api/mesas?zonaId=1"
headers:
  - key: Authorization
    value: "Bearer {{tmpMeseroToken}}"
scripts:
  - type: beforeRequest
    code: |-
      // Autonomous login MESERO
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: 'mesero1@altoro.com',
            password: 'Password123',
            forceSessionOverride: true
          })
        }
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('tmpMeseroToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('El sistema retorna HTTP 200', function () {
        pm.response.to.have.status(200);
      });
      
      pm.test('Solo devuelve una zona', function () {
        const body = pm.response.json();
        pm.expect(body.data.zonas).to.have.lengthOf(1);
        pm.expect(body.data.zonas[0].zonaId).to.equal(1);
      });
      
      // Cleanup
      pm.environment.unset('tmpMeseroToken');
    language: text/javascript
```

- [ ] **Step 3: Crear 80-03 Obtener detalle mesa**

```yaml
name: 80-03 Obtener detalle mesa – MESERO
method: GET
url: "{{baseUrl}}/api/mesas/1/detalle"
headers:
  - key: Authorization
    value: "Bearer {{tmpMeseroToken}}"
scripts:
  - type: beforeRequest
    code: |-
      // Autonomous login MESERO
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: 'mesero1@altoro.com',
            password: 'Password123',
            forceSessionOverride: true
          })
        }
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('tmpMeseroToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('El sistema retorna HTTP 200', function () {
        pm.response.to.have.status(200);
      });
      
      pm.test('La respuesta tiene estructura correcta', function () {
        const body = pm.response.json();
        pm.expect(body.success).to.be.true;
        pm.expect(body.data).to.have.property('visitaId');
        pm.expect(body.data).to.have.property('identificador');
        pm.expect(body.data).to.have.property('numeroPersonas');
        pm.expect(body.data).to.have.property('estado');
        pm.expect(body.data).to.have.property('itemsComanda');
      });
      
      // Cleanup
      pm.environment.unset('tmpMeseroToken');
    language: text/javascript
```

- [ ] **Step 4: Crear 80-04 Obtener info modificar comanda**

```yaml
name: 80-04 Obtener info modificar comanda – MESERO
method: GET
url: "{{baseUrl}}/api/mesas/1/modificar-comanda"
headers:
  - key: Authorization
    value: "Bearer {{tmpMeseroToken}}"
scripts:
  - type: beforeRequest
    code: |-
      // Autonomous login MESERO
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: 'mesero1@altoro.com',
            password: 'Password123',
            forceSessionOverride: true
          })
        }
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('tmpMeseroToken', res.json().accessToken);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('El sistema retorna HTTP 200', function () {
        pm.response.to.have.status(200);
      });
      
      pm.test('La respuesta tiene estructura correcta', function () {
        const body = pm.response.json();
        pm.expect(body.success).to.be.true;
        pm.expect(body.data).to.have.property('identificadorMesa');
        pm.expect(body.data).to.have.property('itemsEnProduccion');
        pm.expect(body.data.itemsEnProduccion).to.be.an('array');
      });
      
      // Cleanup
      pm.environment.unset('tmpMeseroToken');
    language: text/javascript
```

- [ ] **Step 5: Commit Postman manual testing**

```bash
git add backend/postman/postman/collections/manual-testing/
git commit -m "test(mesas): add Postman manual testing requests

- 80-01: Obtener mapa mesas todas zonas
- 80-02: Obtener mapa mesas zona específica
- 80-03: Obtener detalle mesa
- 80-04: Obtener info modificar comanda

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 8: Verificación Final y Coverage

**Files:**
- N/A (verificación)

- [ ] **Step 1: Ejecutar todos los tests**

```bash
cd backend
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml clean test
```

Expected: PASS (todos los tests)

- [ ] **Step 2: Verificar coverage con JaCoCo**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml jacoco:report
```

Expected: 
- MesaService: ≥90%
- MesaController: ≥85%
- MesaMapper: ≥90%

- [ ] **Step 3: Revisar reporte de cobertura**

Abrir: `backend/target/site/jacoco/index.html`

Verificar que las nuevas clases cumplan con los umbrales:
- MesaService: ≥90%
- MesaController: ≥85%
- MesaMapper: ≥90%

- [ ] **Step 4: Compilar proyecto**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f pom.xml clean compile -q
```

Expected: BUILD SUCCESS

---

## Self-Review Checklist

### 1. Spec Coverage

✅ **CA-01**: Endpoint GET /api/mesas lista todas las zonas (Task 5)
✅ **CA-02**: MesaMapaResponse incluye estado, mesero, notificaciones, borrador (Task 1, 3)
✅ **CA-03**: Flag esMesaPropia diferencia mesas propias/otras (Task 3)
✅ **CA-05**: Parámetro zonaId filtra por zona (Task 4)
✅ **CA-06**: Manejo de zona sin mesas (lógica en frontend, backend devuelve lista vacía) (Task 4)
✅ **CA-07**: MesaWsPublisher publica eventos WebSocket (Task 6)
✅ **CA-08**: Endpoint GET /api/mesas/{mesaId}/detalle (Task 5)
✅ **CA-09**: Endpoint GET /api/mesas/{mesaId}/modificar-comanda (Task 5)

### 2. Placeholder Scan

✅ No hay placeholders "TBD", "TODO", "implement later"
✅ Todos los métodos tienen implementación completa
✅ Todos los tests tienen código de verificación completo
✅ Todas las queries custom están implementadas

### 3. Type Consistency

✅ DTOs: nombres de campos consistentes en todos los usos
✅ Enums: EstadoMesa, EstadoComanda, TipoNotificacion usados correctamente
✅ Métodos de mapper: nombres y firmas consistentes
✅ Endpoints: rutas y parámetros consistentes

### 4. Architecture Compliance

✅ Sigue patrón Controller → Service → Repository → Mapper
✅ Todos los mapeos en MesaMapper (no en service)
✅ Security: @PreAuthorize en todos los endpoints
✅ Logging: @Slf4j en service y controller
✅ Transaccional: @Transactional(readOnly = true) en service

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-28-PA-87-visualizar-mapa-mesas.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**

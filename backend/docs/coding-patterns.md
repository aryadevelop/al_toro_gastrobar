# Coding Patterns — Backend

## Services

**Reglas obligatorias:**
- ❌ NO logging (`@Slf4j`, `log.debug()`)
- ✅ Javadoc detallado con flujo paso a paso
- ✅ `@Transactional(readOnly = true)` en consultas; `@Transactional` en escritura
- ✅ `@RequiredArgsConstructor` para inyección
- ✅ `Optional.orElseThrow()` con `ErrorCode` específico o `ResourceNotFoundException`

**Estructura:**
```java
@Service
@RequiredArgsConstructor
public class MiService {
    private final MiRepository miRepository;
    private final MiMapper miMapper;

    @Transactional(readOnly = true)
    public MiResponse obtener(Long id) {
        // 1. Buscar entidad con orElseThrow
        MiEntidad e = miRepository.findById(id).orElseThrow(() ->
            new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "...", HttpStatus.NOT_FOUND));
        // 2. Consultas auxiliares
        Optional<Related> related = relatedRepo.findByXxx(id);
        // 3. Delegar mapeo (NO construir DTOs aquí)
        return miMapper.toResponse(e, related);
    }
}
```

**Excepciones:** Para 404, preferir `new ResourceNotFoundException("Entidad", id)` (patrón usado en `NotificacionService`). Para validaciones de estado o reglas de negocio, usar `BusinessException(ErrorCode.X, mensaje, HttpStatus.Y)`.

---

## Mappers

**Reglas obligatorias:**
- ✅ **SIEMPRE ordenar items por categoría ANTES de agrupar/mapear**
- ✅ Reutilizar comparador estático
- ✅ Enums → String usando `.name()`
- ✅ `empleadoNombre` (campo completo), NO separación nombre/apellido
- ✅ `usuarioNombre` (campo único), NO existe `usuarioApellido`
- ❌ NO builders inline en services — toda transformación entity→DTO en mapper dedicado

**Comparador estándar:**
```java
private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA =
    Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());

public List<ItemResponse> mapearOrdenados(List<ComandaItem> items) {
    return items.stream()
        .sorted(COMPARATOR_POR_CATEGORIA)  // ORDENAR PRIMERO
        .map(this::toItemResponse)          // LUEGO MAPEAR
        .collect(Collectors.toList());
}
```

**Agrupación** (nombreProducto + descripcion):
```java
Map<String, List<ComandaItem>> agrupados = items.stream()
    .sorted(COMPARATOR_POR_CATEGORIA)  // ORDENAR ANTES
    .collect(Collectors.groupingBy(item ->
        item.getProducto().getProductoNombre() + "|" +
        (item.getComandaItemDescripcion() != null ? item.getComandaItemDescripcion() : "")
    ));
```

---

## DTOs

**Reglas:**
- ✅ Enums → String en DTOs (`.name()` en mappers)
- ✅ Inmutabilidad: `@Getter` + `@Builder` + campos `final`
- ✅ Javadoc por campo con valores posibles cuando aplique
- ✅ Incluir `String categoriaProducto` cuando se muestran items

```java
@Getter @Builder
public class ItemResponse {
    /** Nombre del producto */
    private final String nombreProducto;
    /** Categoría: "PLATO", "BEBIDA", "OTRO" */
    private final String categoriaProducto;
    private final Integer cantidad;
}
```

---

## ErrorCode Usage

| Código | Enum | HTTP | Uso |
|--------|------|------|-----|
| `ENT-001` | `ENTITY_NOT_FOUND` | 404 | Entidad no encontrada |
| `ENT-002` | `ENTITY_ALREADY_EXISTS` | 409 | Duplicada |
| `AUTH-001` | `INVALID_CREDENTIALS` | 401 | Credenciales inválidas |
| `AUTH-002` | `ACCESS_DENIED` | 403 | Sin permisos |
| `NEG-001` | `BUSINESS_ERROR` | 400/409 | Regla violada |
| `NEG-002` | `INVALID_STATE` | 409 | Estado inválido |
| `VAL-001` | `VALIDATION_ERROR` | 400 | Validación |

---

## ApiResponse — Factory Methods

```java
ApiResponse.ok(data)                      // 200 con data
ApiResponse.ok(message, data)             // 200 con mensaje + data
ApiResponse.created(message, data)        // 201 con mensaje + data (alias de ok)
ApiResponse.message(message)              // 200 solo mensaje
ApiResponse.error(code, message)          // error con código + mensaje
```

**No existe** `ApiResponse.success(...)`.

---

## Controller Tests

**Reglas:**
- ✅ `@WebMvcTest(controllers = MiController.class)`
- ✅ Importar `PermissiveSecurityConfig` interna
- ✅ `@MockitoBean` para services y dependencias seguridad (Spring Boot 3.4+)
- ✅ `@Nested` + `@DisplayName` agrupar por endpoint
- ✅ Nombres: `condicion_resultadoEsperado()`
- ✅ `@WithMockUser(username = "...", roles = "MESERO")`

**Imports correctos:**
- `JwtTokenProvider` desde `co.edu.unicauca.backend.modules.auth.security`
- `SesionRepository` desde `co.edu.unicauca.backend.modules.auth.repository`

```java
@WebMvcTest(controllers = MiController.class)
@Import(MiControllerTest.PermissiveSecurityConfig.class)
class MiControllerTest {
    static class PermissiveSecurityConfig {
        @Bean @Order(1)
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http.securityMatcher("/**").csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }
    }
    @Autowired MockMvc mockMvc;
    @MockitoBean MiService miService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    @Nested @DisplayName("GET /api/mi-endpoint")
    class ObtenerDato {
        @Test @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("ID válido → 200 OK")
        void idValido_retorna200() throws Exception {
            when(miService.obtener(1L)).thenReturn(response);
            mockMvc.perform(get("/api/mi-endpoint/1"))
                .andExpect(status().isOk());
            verify(miService).obtener(1L);
        }
    }
}
```

---

## Service Tests

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MiService")
class MiServiceTest {
    @Mock MiRepository miRepository;
    @InjectMocks MiService miService;

    // Helpers privados para construir entidades de prueba
    private MiEntidad miEntidad() { return MiEntidad.builder()...build(); }

    @Nested @DisplayName("metodo")
    class Metodo {
        @Test
        @DisplayName("happy path → resultado esperado")
        void happyPath() { ... }
    }
}
```

---

## WebSocket Integration

**Patrón obligatorio:** REST + Publisher (NO `@MessageMapping` para operaciones con persistencia).

**Cuándo enviar WS:**
- Crear/modificar/eliminar mesas, cambiar estado
- Crear/atender notificaciones
- Cerrar cuenta, actualizar items
- Cambios de reservas

**Cuándo NO enviar:**
- Consultas GET
- Operaciones que no afectan otros usuarios

**Patrón:**
```java
@Service @RequiredArgsConstructor
public class MiWsPublisher {
    private final SimpMessagingTemplate messagingTemplate;
    public void publicarEvento(Long id, MiWsMessage msg) {
        messagingTemplate.convertAndSend("/topic/mi-recurso/" + id, msg);
    }
}

// En Service:
@Transactional
public MiResponse crear(MiRequest req) {
    MiEntidad e = miRepository.save(...);
    MiResponse res = miMapper.toResponse(e);
    wsPublisher.publicarEvento(e.getId(), buildWsMessage(e));  // WS DESPUÉS de persistir
    return res;
}
```

**Tópicos existentes** (NO crear duplicados):

| Tópico | Propósito |
|--------|-----------|
| `/topic/mesas` | Mapa de mesas (estado, notificaciones) |
| `/topic/mesas/asistencia` | Broadcast solicitud asistencia |
| `/topic/visita/{visitaId}/orden` | Orden del cliente |
| `/topic/visita/{visitaId}/cuenta` | Cierre de cuenta |
| `/topic/visita/{visitaId}/asistencia` | Asistencia atendida (al cliente) |
| `/topic/reservas/cambios` | Cambios de reservas |
| `/topic/produccion/{cocina\|barra}` | Eventos de ciclo de vida de comanda en producción (contrato unificado) |

**Patrón "señal, no data":** El payload de `/topic/mesas` es minimal (`visitaId`, `tipoEvento`). El frontend recibe la señal y hace re-fetch vía REST. Usar `MesaWsPublisher.publicarActualizacionMesa(visitaId, TipoEventoMesa.NOTIFICACION)` para refrescar el mapa al atender notificaciones.

**Patrón "evento unificado de producción":** Los tópicos `/topic/produccion/cocina` y `/topic/produccion/barra` transportan `ComandaProduccionEventoWsMessage(tipo, estacion, comandaId, resumen)` donde `tipo ∈ {CREADA, ELIMINADA, COMPLETADA}`. El campo `resumen` solo viaja en `CREADA` para que el cliente pueda añadir la comanda al tablero sin un GET adicional. Publicar mediante `NotificacionWsPublisher.publicarEventoProduccion(EstacionComanda, ComandaProduccionEventoWsMessage)`. Cualquier emisión a estos tópicos debe pasar por ese método para preservar el contrato.

---

## Git Commits

**Reglas:**
- ❌ NO ejecutar commits automáticamente — reportar mensaje en español
- ✅ Formato: `<tipo>(<módulo>): <descripción en español>`
- ✅ Co-author obligatorio: `Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>`

**Tipos:** `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`

```bash
git commit -m "feat(mesas): añadir endpoint GET /api/mesas para consultar mapa"
```

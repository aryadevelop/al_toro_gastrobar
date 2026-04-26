# Spec: Cobertura de Tests al 100% — Al Toro Gastrobar Backend

**Fecha:** 2026-04-19  
**Rama:** PA-72-modificar-reserva-futura  
**Objetivo:** Alcanzar cobertura de líneas ≥ 90% (efectivamente 100% en lógica de negocio) desde entidades hasta controladores.

---

## Contexto

El backend tiene 15 archivos de test que cubren bien auth, seguridad, configuración y parte del módulo `reservas`. Sin embargo, los siguientes módulos y capas tienen cobertura nula o mínima:

- **Capa controladores**: ningún endpoint está probado directamente (salvo el manejo de excepciones)
- **Servicios no probados**: `DisponibilidadConsultador`, `PreOrdenGestor`, `VisitaService`, `VentaService`, `ProductoService`, `CustomUserDetailsService`, y métodos CRUD de `ReservaService`
- **Módulos sin tests**: `mesas_comandas`, `pagos_caja`, `produccion`
- **Validaciones de entidades/DTOs**: solo `LoginRequest` está cubierta

---

## Alcance

**20 clases de test nuevas**, agrupadas en 3 fases:

| Fase | Enfoque | Clases |
|---|---|---|
| 1 | Servicios con lógica compleja | 7 |
| 2 | Validaciones de entidades y DTOs | 7 |
| 3 | Controladores vía `@WebMvcTest` | 6 |

---

## Patrones de Testing (ya establecidos en el proyecto)

- **Unit (Mockito):** `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` + `@Mock`
- **Controladores:** `@WebMvcTest` + `@MockitoBean` + `MockMvc` + `@WithMockUser`
- **Entidades/DTOs:** `Validation.buildDefaultValidatorFactory()` (igual que `LoginRequestValidationTest`)
- **Assertions:** AssertJ (`assertThat`, `assertThatThrownBy`)
- **Organización:** `@Nested` + `@DisplayName`
- **Captura de argumentos:** `ArgumentCaptor` para verificar mutaciones

---

## Fase 1 — Servicios

### 1.1 `DisponibilidadConsultadorTest`
**Paquete:** `modules/reservas/service`  
**Mocks:** `ReservaValidador`, `ReservaMapper`, `ReservaRepository`, `ZonaRepository`, `DecoracionRepository`, `DecoracionZonaRepository`  
**~14 métodos** — cubre: horario inválido, bloqueado, zonas llenas, decoraciones ocupadas/libres, query con/sin exclusión de reservaId

### 1.2 `PreOrdenGestorTest`
**Paquete:** `modules/reservas/service`  
**Mocks:** `ProductoRepository`, `OpcionModificacionRepository`, `ProductoOpcionModificacionRepository`, `ComandaRepository`, `ComandaItemRepository`, `ComandaMenuModificacionRepository`  
**~16 métodos** — cubre: validación (no menu especial, >10 personas, solo 1 menu), persistencia (producto inactivo, modificaciones, opcion inválida), eliminación de pre-orden existente

### 1.3 `ReservaServiceCrudTest`
**Paquete:** `modules/reservas/service`  
**Mocks:** mismos 12 del `ReservaServiceModificarTest` existente  
**~18 métodos** — cubre los métodos aún no testeados: `crearReserva`, `obtenerReservasFuturas`, `obtenerDetalleReserva`, `obtenerReservasCanceladasODevueltas`  
**Reglas clave:** anticipación mínima 1 día para ESPECIAL, tipo determinado por costo decoración, email del token vs email del body

### 1.4 `VentaServiceTest`
**Paquete:** `modules/pagos_caja/service`  
**Mocks:** `VisitaRepository`, `VentaRepository`, `EmpleadoRepository`  
**~8 métodos** — **regla crítica:** si `visita.cliente != null` → `puntosActuales+1` Y `puntosAcumulados+1`; si cliente es null → sin cambios

### 1.5 `VisitaServiceTest`
**Paquete:** `modules/mesas_comandas/service`  
**Mocks:** `ClienteRepository`, `VisitaRepository`, `MesaRepository`, `ComandaRepository`, `ComandaItemRepository`, `VentaRepository`, `AbonoRepository`, `VisitaMapper`  
**~12 métodos** — cubre: ownership CLIENTE vs ADM, consolidación de múltiples comandas, relaciones opcionales null-safe (sin reserva → sin abonos)

### 1.6 `ProductoServiceTest`
**Paquete:** `modules/produccion/service`  
**Mocks:** `ProductoRepository`, `ProductoOpcionModificacionRepository`, `ProductoMapper`  
**~8 métodos** — cubre: agrupación por categoría, orden de categorías, filtro por `menuEspecial=true`

### 1.7 `CustomUserDetailsServiceTest`
**Paquete:** `modules/auth/service`  
**Mocks:** `UsuarioRepository`, `UsuarioRolRepository`  
**~6 métodos** — cubre: usuario no existe, sin roles activos, authorities mapeadas, roles duplicados deduplicados

---

## Fase 2 — Validaciones de Entidades y DTOs

Todas siguen el patrón de `LoginRequestValidationTest`: instanciación directa + `ValidatorFactory`.

| Clase de test | Entidad/DTO | ~Métodos |
|---|---|---|
| `ReservaValidationTest` | `Reserva` | 6 |
| `DecoracionValidationTest` | `Decoracion` | 5 |
| `BloqueDisponibilidadValidationTest` | `BloqueDisponibilidad` | 4 |
| `ClienteValidationTest` | `Cliente` | 7 |
| `CrearReservaRequestValidationTest` | `CrearReservaRequest` | 5 |
| `CerrarCuentaRequestValidationTest` | `CerrarCuentaRequest` | 7 |
| `OtrasEntidadesValidationTest` | `Venta`, `Abono`, `Producto`, `Zona`, `Mesa`, `Comanda`, `ComandaItem`, `Empleado`, `Notificacion` | 12 |

---

## Fase 3 — Controladores

Patrón base (igual que `GlobalExceptionHandlerTest`):
```java
@WebMvcTest(XxxController.class)
@Import(SecurityConfig.class)  // o con security deshabilitada
class XxxControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean XxxService xxxService;
}
```
Usar `@WithMockUser(roles = "CLIENTE")` / `"CAJERO"` / `"ADM"` según endpoint.

| Clase de test | Controlador | ~Métodos |
|---|---|---|
| `AuthControllerTest` | `AuthController` | 10 |
| `ReservaControllerTest` | `ReservaController` | 16 |
| `ClienteControllerTest` | `ClienteController` | 10 |
| `VisitaControllerTest` | `VisitaController` | 9 |
| `ProductoControllerTest` | `ProductoController` | 6 |
| `VentaControllerTest` | `VentaController` | 6 |

**Por cada controlador se verifica:**
- Código HTTP correcto (200, 201, 400, 403, 404, 409, 422)
- Validación de request body (campos obligatorios)
- Ownership: rol CLIENTE pasa su email, rol ADM pasa null
- Delegación al servicio: `verify(service).metodo(argCaptor.capture())`

---

## Fase 4 — Repositorios (queries personalizadas)

Patrón: `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@ActiveProfiles("dev")` (igual que `SchemaValidationIT` — requiere PostgreSQL corriendo).

| Clase de test | Repositorio | Queries a probar | ~Métodos |
|---|---|---|---|
| `ReservaRepositoryTest` | `ReservaRepository` | `findByCliente_UsuarioId...`, `findPersonasPorZonaEnDia`, `findPersonasPorZonaEnDiaExcluyendo`, estados y fechas | 8 |
| `BloqueDisponibilidadRepositoryTest` | `BloqueDisponibilidadRepository` | `countBloquesParaFechaHora` (date range + optional time range, full-day vs partial) | 5 |
| `SesionRepositoryTest` | `SesionRepository` | `findByToken`, `findActiveByUsuario`, `revokeAll` | 4 |
| `ClienteRepositoryTest` | `ClienteRepository` | `findByUsuario_UsuarioEmail`, puntos queries | 3 |
| `VisitaRepositoryTest` | `VisitaRepository` | `findByCliente_UsuarioIdOrderBy...`, activas vs cerradas | 4 |

**Reglas para estos tests:**
- Usar `@Sql` o `@BeforeEach` con `TestEntityManager` para insertar datos de prueba
- Cada test limpia su estado con `@Transactional` (rollback automático)
- Probar las **condiciones de borde**: rango exacto de fechas, bloqueos solapados parcialmente, sesiones revocadas vs activas

---

## Herramienta de cobertura

Añadir plugin JaCoCo en `backend/pom.xml`:
```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution><id>prepare-agent</id><goals><goal>prepare-agent</goal></goals></execution>
    <execution><id>report</id><phase>verify</phase><goals><goal>report</goal></goals></execution>
  </executions>
</plugin>
```
Reporte: `backend/target/site/jacoco/index.html`

---

## Verificación

```bash
cd backend
mvn test          # solo unit tests (sin DB)
mvn verify        # unit + reporte JaCoCo
```

Cobertura esperada tras implementar las 20 clases:
- **Servicios:** ≥ 90% líneas
- **Controladores:** ≥ 85% líneas  
- **Entidades (constraints):** ≥ 80% (el resto lo cubre `SchemaValidationIT`)
- **Código Lombok/JPA lifecycle:** cubierto indirectamente por las pruebas de servicios

---

## Resumen de totales

- **25 clases de test nuevas** (20 unit/WebMvc + 5 repositorios)
- **~209 métodos de test nuevos**
- **Secuencia recomendada:** VentaService → CustomUserDetails → DisponibilidadConsultador → PreOrdenGestor → ReservaServiceCrud → VisitaService → ProductoService → validaciones (lote) → controladores (lote) → repositorios (requieren DB)

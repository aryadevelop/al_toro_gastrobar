# Testing — estrategia y cobertura

Este documento describe la estrategia de testing del backend, los comandos disponibles, el estado actual de la suite, la configuración real de cobertura JaCoCo y los patrones por tipo de test.

---

## Comandos

```bash
./mvnw test                         # ejecutar todos los tests
./mvnw clean test jacoco:report     # tests + reporte de cobertura HTML
./mvnw verify                       # tests + fase verify (activa la regla JaCoCo)
```

Reporte de cobertura: `backend/target/site/jacoco/index.html`.

---

## Estado actual

Conteo de tests: ver salida de `./mvnw test`.

Archivos de test por tipo (141 archivos de test activos):

- **Service / Validador** (26 archivos): `ReservaValidadorTest` (19 casos), `ReservaServiceModificarTest` (12), `ReservaServiceCrudTest`, `ReservaServiceCancelarTest`, `ReservaServiceConfirmarTest`, `ReservaServiceInasistenciaTest`, `ReservaServiceRegistrarAbonoTest`, `ReservaServiceResumenPagoTest`, `ReservaServiceBranchGapsTest`, `ComandaBorradorServiceTest`, `ComandaProduccionServiceTest`, `MesaServiceTest`, `VisitaServiceTest`, `PuntosServiceTest` (8), `AuthServiceTest`, `NotificacionServiceTest`, `VentaServiceTest`, `CuentaServiceTest`, entre otros
- **Controller** (17 archivos): `ReservaControllerTest`, `ReservaConsultaControllerTest`, `AuthControllerTest`, `ComandaControllerTest`, `ComandaProduccionControllerTest`, `MesaControllerTest`, `VisitaControllerTest`, `NotificacionControllerTest`, `ClienteControllerTest`, `VentaControllerTest`, `ProductoControllerTest`, entre otros
- **Mapper** (12 archivos): `ReservaMapperTest`, `AbonoMapperTest`, `ReservaConsultaMapperTest`, `ComandaBorradorMapperTest`, `ComandaProduccionMapperTest`, `MesaMapperTest`, `VisitaMapperTest`, `ProductoMapperTest`, `CuentaMapperTest`, entre otros
- **Repository** (9 archivos): `ReservaRepositoryTest`, `BloqueDisponibilidadRepositoryTest`, `ComandaRepositoryTest`, `MesaRepositoryTest`, `VisitaRepositoryTest`, `ClienteRepositoryTest`, `AjusteInventarioRepositoryTest`, `SesionRepositoryTest`, `ComandaItemRepositoryTest`
- **Entity / Validation** (35 archivos): tests de constraints Jakarta, getters/setters y constructores de entidades de todos los módulos
- **Seguridad / Infraestructura** (7 archivos): `JwtTokenProviderTest`, `JwtAuthenticationFilterTest`, `SecurityConfigTest`, `RabbitMQConfigTest`, `WebSocketConfigTest`, `GlobalExceptionHandlerTest`, `ProdAdminBootstrapTest`
- **Otros** (3 archivos): `BackendApplicationTests`, `SchemaValidationIT`, `ResumenFinancieroCalculatorTest`

---

## Cobertura mínima por capa

**Sin umbrales de fallo configurados.** El `pom.xml` declara JaCoCo 0.8.12 con las fases `prepare-agent` y `report`, pero no define elementos `<rule>` ni `<limit>` con valores mínimos. El reporte se genera pero no bloquea el build por cobertura insuficiente.

Los objetivos de cobertura son referencia del equipo, no restricciones de CI:

| Capa | Objetivo de cobertura |
|---|---|
| Services / Validadores | 90–95 % instrucciones |
| Controllers | 85–90 % instrucciones |
| Mappers | 90–95 % instrucciones |
| Repositories | 80–90 % instrucciones |

---

## Exclusiones de cobertura

Clases excluidas del reporte JaCoCo (configuradas en `pom.xml`):

- `**/BackendApplication.*`

---

## Reglas

1. Implementar tests antes del commit: ejecutar `./mvnw clean test jacoco:report` y verificar que pasa.
2. Cubrir ramas: cada `if/else`, `switch` y `try/catch` requiere casos para todas las ramas.
3. Cubrir edge cases: validaciones fallidas, errores de negocio, nulls y listas vacías.
4. Nombrar tests con el patrón `<condición>_<resultado>` (ej. `conFechaValida_retorna200`).
5. Usar `@DisplayName` y `@Nested` para agrupar casos relacionados.
6. Prohibido usar `log.debug()` ni `@Slf4j` en tests.

---

## Patrones por tipo de test

### Tests de service

Anotación de clase: `@ExtendWith(MockitoExtension.class)`. Cuando existen dependencias con comportamiento complejo que requiere configuración no estricta, se añade `@MockitoSettings(strictness = Strictness.LENIENT)`.

Estructura base:

```java
@ExtendWith(MockitoExtension.class)
class ReservaValidadorTest {

    @Mock
    BloqueDisponibilidadRepository bloqueRepository;

    @InjectMocks
    ReservaValidador validador;

    @Nested
    @DisplayName("validarElegibilidadModificacion")
    class ValidarElegibilidad {

        @Test
        @DisplayName("Con estado CANCELADA → lanza BusinessException")
        void conEstadoCancelada_lanzaException() {
            // arrange
            // act / assert
            assertThatThrownBy(() -> validador.validarElegibilidadModificacion(...))
                .isInstanceOf(BusinessException.class);
        }
    }
}
```

Helpers privados: se admiten métodos `private` que construyan entidades de prueba (builders de fixture), siempre sin lógica de negocio propia.

Aserciones: AssertJ (`assertThat`, `assertThatThrownBy`, `assertThatCode`).

### Tests de controller

Anotación de clase: `@WebMvcTest(controllers = XxxController.class)` más `@Import(XxxControllerTest.PermissiveSecurityConfig.class)`.

La clase interna `PermissiveSecurityConfig` deshabilita CSRF y permite todas las peticiones, eliminando la necesidad de configurar tokens reales en cada test:

```java
@WebMvcTest(controllers = ReservaController.class)
@Import(ReservaControllerTest.PermissiveSecurityConfig.class)
class ReservaControllerTest {

    static class PermissiveSecurityConfig {
        @Bean
        @Order(1)
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ReservaService reservaService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;
```

Los mocks de infraestructura (`JwtTokenProvider`, `UserDetailsService`, `SesionRepository`) se declaran en todos los controller tests para satisfacer el contexto de Spring Security, aunque no se usen directamente en las aserciones.

La anotación `@WithMockUser` se usa cuando el endpoint bajo prueba lee el principal autenticado.

### Tests de Postman

Ver `postman-conventions.md` para la estructura de colecciones manuales y automatizadas, nomenclatura de casos y variables de entorno.

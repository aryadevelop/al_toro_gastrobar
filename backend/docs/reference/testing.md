# Testing — estrategia, cobertura y colecciones Postman

Documento de referencia para la estrategia de pruebas del backend. Cubre los tests unitarios Java (JaCoCo, patrones por capa) y los tests de integración con Postman (colecciones manuales y automatizadas).

---

# Tabla de contenido

- [Tests Java](#tests-java)
  - [Cobertura mínima por capa](#cobertura-mínima-por-capa)
  - [Exclusiones de cobertura](#exclusiones-de-cobertura)
  - [Reglas de tests Java](#reglas-de-tests-java)
  - [Patrones por tipo de test](#patrones-por-tipo-de-test)
- [Tests Postman](#tests-postman)
  - [Convenciones de colecciones](#convenciones-de-colecciones)
  - [Reglas críticas de Postman](#reglas-críticas-de-postman)
  - [Cobertura obligatoria por endpoint](#cobertura-obligatoria-por-endpoint)
  - [Checklist de revisión post-generación](#checklist-de-revisión-post-generación)

---

## Tests Java

El proyecto ejecuta tests unitarios con JUnit 5 y Mockito. JaCoCo genera el reporte de cobertura configurado en `pom.xml` con los goals `prepare-agent` y `report`.

```bash
./mvnw test                         # ejecutar todos los tests
./mvnw clean test jacoco:report     # tests + reporte de cobertura HTML
./mvnw verify                       # tests + fase verify (activa la regla JaCoCo)
```

El reporte de cobertura se ubica en `backend/target/site/jacoco/index.html`.

---

### Cobertura mínima por capa

No hay reglas `<rule>` ni límites `<limit>` configurados en `pom.xml`; la compilación no falla por cobertura insuficiente. Los valores de la tabla son objetivos de referencia del equipo, no umbrales de fallo automatizados.

| Capa | Objetivo del equipo |
|------|---------------------|
| Service / Validador | 90% de instrucciones |
| Controller | 90% de instrucciones |
| Mapper | 100% de instrucciones |
| Repository | No aplica |
| Entity | No aplica |

---

### Exclusiones de cobertura

Clases excluidas del reporte JaCoCo (configuradas en `pom.xml`):

- `**/BackendApplication.*`

---

### Reglas de tests Java

1. Implementar tests antes del commit: ejecutar `./mvnw clean test jacoco:report` y verificar que pasa.
2. Cubrir ramas: cada `if/else`, `switch` y `try/catch` requiere casos para todas las ramas.
3. Cubrir edge cases: validaciones fallidas, errores de negocio, nulls y listas vacías.
4. Nombrar tests con el patrón `<condición>_<resultado>` (p. ej. `conFechaValida_retorna200`).
5. Usar `@DisplayName` y `@Nested` para agrupar casos relacionados.

---

### Patrones por tipo de test

#### Tests de service

La anotación de clase es `@ExtendWith(MockitoExtension.class)`. Cuando existen dependencias con comportamiento complejo que requiere configuración no estricta, se añade `@MockitoSettings(strictness = Strictness.LENIENT)`.

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

Se admiten métodos `private` que construyan entidades de prueba (builders de fixture), siempre sin lógica de negocio propia. Las aserciones usan AssertJ (`assertThat`, `assertThatThrownBy`, `assertThatCode`).

#### Tests de controller

La anotación de clase es `@WebMvcTest(controllers = XxxController.class)` más `@Import(XxxControllerTest.PermissiveSecurityConfig.class)`.

La clase interna `PermissiveSecurityConfig` deshabilita CSRF y permite todas las peticiones, eliminando la necesidad de configurar tokens reales en cada test.

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
}
```

Los mocks de infraestructura (`JwtTokenProvider`, `UserDetailsService`, `SesionRepository`) se declaran en todos los controller tests para satisfacer el contexto de Spring Security, aunque no se usen directamente en las aserciones. La anotación `@WithMockUser` se usa cuando el endpoint bajo prueba lee el principal autenticado.

---

### Tests Postman

Postman cubre los tests de integración del proyecto. Existen dos tipos de colección con propósitos distintos: las colecciones manuales permiten validación exploratoria rápida; las colecciones automatizadas cubren los criterios de aceptación de forma repetible e independiente.

| Aspecto | Manual (`manual-testing/`) | Automatizado (`collections/`) |
|---------|---------------------------|-------------------------------|
| Propósito | Validación exploratoria rápida | Cobertura completa de criterios de aceptación |
| Credenciales | Hardcoded | Variables de entorno |
| Contraseña | `Al.Toro2026!` | `passwordValida` |
| `afterResponse` | Solo cleanup (`unset`) | Tests + guardar IDs |
| Cleanup de estado | En `afterResponse` | En `beforeRequest` del siguiente request |
| Independencia de orden | No requerida | **Obligatoria** |
| Formato de nombre | `XX-YY Descripción – ROL.request.yaml` | `XX-YY Descripción – Código HTTP.request.yaml` |

---

### Convenciones de colecciones

Las colecciones se escriben en formato YAML (plugin Postman para VS Code). El archivo `.resources/definition.yaml` contiene los hooks a nivel de colección.

#### Naming de variables de token

| Rol | Variable manual | Variable automatizada |
|-----|-----------------|----------------------|
| CLIENTE | `tmpClienteToken` | `clienteToken` |
| MESERO | `tmpMeseroToken` | `meseroToken` |
| CAJERO | `tmpCajeroToken` | `cajeroToken` |
| ADMIN | `tmpAdminToken` | `adminToken` |

#### Numeración de módulos (colecciones manuales)

| Rango | Módulo |
|-------|--------|
| `00-XX` | Auth |
| `10-XX` | Productos |
| `20-XX` | Reservas (CLIENTE) |
| `30-XX` | Reservas (MESERO) |
| `40-XX` | Visitas |
| `50-XX` | Puntos |
| `60-XX` | Ventas |
| `70-XX` | Notificaciones |
| `80-XX` | Mesas |

#### Prerrequisito de ejecución

Antes de ejecutar cualquier colección, limpiar el estado de la base de datos:

```bash
psql -U postgres -d altoro_db -f postman/cleanup-notificaciones.sql
```

---

### Reglas críticas de Postman

#### Autonomous Login Pattern

**Todos los tests** incluyen script `beforeRequest` con login autónomo vía `pm.sendRequest`. **Nunca** usar `{{tokenVariable}}` sin un `beforeRequest` que la establezca.

#### Variables en URLs dinámicas

Una variable vacía en la URL produce URLs malformadas (`//`) y lanza `RequestRejectedException`. Usar una variable temporal con prefijo `tmp` establecida en `beforeRequest`:

```javascript
const reservaId = pm.environment.get('reservaIdConPreOrden') || '10';
pm.environment.set('tmpReservaId', reservaId);
// afterResponse: pm.environment.unset('tmpReservaId');
```

**Nunca** usar `{{var}}` directamente en la URL si puede estar vacío.

#### Fechas dinámicas

**Nunca** usar fechas fijas (`2026-12-25`). Calcular siempre en relación al día actual:

```javascript
const d = new Date();
d.setDate(d.getDate() + 1);
d.setHours(19, 0, 0, 0);
const fechaHora = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T19:00:00`;
```

---

### Cobertura obligatoria por endpoint

Al añadir o modificar un endpoint, crear:

1. Una request manual siguiendo el template `00-01 Login CLIENTE.request.yaml`.
2. Una carpeta automatizada con `definition.yaml` que cubra:
   - 200 OK happy path
   - 401 sin token
   - 403 con cada rol no autorizado
   - 404 ID inexistente
   - 409 estado inválido
   - 400 validaciones específicas del endpoint

---

### Checklist de revisión post-generación

Antes de integrar la colección al repositorio, verificar:

1. Validar que el JSON es válido e importable en Postman sin errores.
2. Verificar que la colección tiene `description` completa en formato Markdown.
3. Verificar que cada carpeta de endpoint tiene `description` con matriz de casos.
4. Verificar que cada request tiene `description` con los cuatro campos obligatorios.
5. Verificar que el nombre de cada request sigue el patrón `[ID] [Descripción] – [HTTP] [Estado]`.
6. Confirmar que no hay valores hardcodeados de credenciales, URLs ni tokens.
7. Confirmar que todos los requests con body tienen `Content-Type: application/json`.
8. Confirmar que los requests protegidos tienen `Authorization: Bearer {{token}}`.
9. Verificar que cada request tiene al menos un `pm.test()`.
10. Verificar que los tests comprueban HTTP status y contenido de la respuesta.
11. Confirmar que las variables temporales se limpian con `pm.environment.unset()`.
12. Confirmar que los grupos con dependencia de token tienen pre-request de carpeta.
13. Confirmar que los tokens se guardan solo dentro de `if (pm.response.code === 2xx)`.

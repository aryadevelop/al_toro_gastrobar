# Pruebas Unitarias HU-05 + Cobertura JaCoCo — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Llevar a 100% líneas / ≥95% ramas la cobertura sobre archivos nuevos/modificados de HU-05 (Modificar Comanda) y elevar la cobertura global del proyecto excluyendo `reportes*` y `auth` del reporte JaCoCo.

**Architecture:** Tests unitarios puros (JUnit 5 + Mockito + AssertJ) sobre services/validadores/mappers/entities; `@DataJpaTest` con H2 para repositories con `@Query`; patrón existente (`MesaControllerTest`/`VisitaControllerTest`) replicado para controllers. Configuración JaCoCo en `backend/pom.xml` con `<excludes>` por módulo.

**Tech Stack:** Spring Boot 3.5 · Java 21 · JUnit 5 · Mockito · AssertJ · H2 (test) · JaCoCo 0.8.12 · Maven.

**Spec base:** `docs/superpowers/specs/2026-05-11-pruebas-unitarias-hu05-y-cobertura-design.md`.

**Convenciones globales (aplican a todas las tareas):**
- AAA (Arrange / Act / Assert). `@Nested` para agrupar por método bajo prueba.
- Naming: `metodo_Condicion_Resultado` o `cuandoXEntoncesY` — verificar el predominante en el test sibling antes de empezar y mantener consistencia.
- Services: `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`. **Sin Spring context**.
- Declarar SIEMPRE como `@Mock` todas las dependencias inyectadas por el service bajo prueba (lección NPE 11-may con `VisitaEstadoMapper`, `MesaValidador`).
- Mappers: assertions campo por campo con AssertJ (`assertThat(...)...`).
- `lenient()` solo en helpers compartidos (e.g. `crearAuthentication()`) cuando no todos los tests usan el stub.
- Prohibido `log.debug()` en productivo; en tests no se usa.
- Cada tarea termina con su propio commit (mensaje en formato `<tipo>(<modulo>): <descripcion>`).
- **NO ejecutar `git commit` sin que el usuario lo autorice explícitamente** (regla `feedback_no_commits.md`). Reportar el mensaje propuesto en cada paso de commit y esperar instrucción.

**Comandos clave (desde `backend/`):**
- Test simple: `./mvnw test -Dtest=<ClassName>`
- Test método: `./mvnw test -Dtest=<ClassName>#<methodName>`
- Suite completa: `./mvnw clean test`
- Coverage: `./mvnw clean test jacoco:report` → `target/site/jacoco/index.html`

---

## Fase 0 — Preparación

### Tarea 0.1: Verificar baseline del build y suite actual

**Files:**
- Leer: `backend/pom.xml`

- [ ] **Paso 1: Verificar que la suite actual está verde**

Ejecutar (desde `backend/`):
```bash
./mvnw clean test
```
Esperado: BUILD SUCCESS, 0 tests fallidos. Si hay fallos, **detenerse y reportar** — el plan asume baseline verde tras los fixes del 11-may (obs 1100–1117).

- [ ] **Paso 2: Generar reporte JaCoCo inicial**

Ejecutar:
```bash
./mvnw clean test jacoco:report
```
Esperado: BUILD SUCCESS y existe `backend/target/site/jacoco/index.html`.

- [ ] **Paso 3: Anotar % global de líneas y ramas**

Abrir `backend/target/site/jacoco/index.html` y registrar en notas (no commitear) los % de:
- Total proyecto (líneas, ramas)
- Módulo `mesas_comandas`
- Módulo `inventario`
- Módulo `reservas`

Estos valores servirán como baseline para la Fase B.

---

## Fase 1 — JaCoCo Excludes

### Tarea 1.1: Configurar `<excludes>` en JaCoCo

**Files:**
- Modify: `backend/pom.xml` (bloque `jacoco-maven-plugin`)

- [ ] **Paso 1: Localizar el bloque actual del plugin**

Buscar en `backend/pom.xml`:
```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  ...
</plugin>
```

Confirmado por obs 1136: el plugin ya existe sin `<excludes>`.

- [ ] **Paso 2: Agregar `<configuration><excludes>...</excludes></configuration>`**

Reemplazar el bloque del plugin por:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <configuration>
    <excludes>
      <exclude>**/modules/reportes/**</exclude>
      <exclude>**/modules/reportes_clientes/**</exclude>
      <exclude>**/modules/reportes_ventas_detalle/**</exclude>
      <exclude>**/modules/auth/**</exclude>
      <exclude>**/BackendApplication.*</exclude>
    </excludes>
  </configuration>
  <executions>
    <execution>
      <id>prepare-agent</id>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>verify</phase>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

Mantener `<version>` si la actual difiere (no degradar). Mantener cualquier `<execution>` adicional ya presente.

- [ ] **Paso 3: Regenerar reporte y verificar exclusión**

```bash
./mvnw clean test jacoco:report
```
Abrir `target/site/jacoco/index.html`. Verificar que los paquetes `*.modules.reportes`, `*.modules.reportes_clientes`, `*.modules.reportes_ventas_detalle`, `*.modules.auth` **no aparecen** en el listado de paquetes.

- [ ] **Paso 4: Commit (reportar mensaje, esperar autorización)**

```bash
git add backend/pom.xml
git commit -m "chore(jacoco): excluir modulos reportes y auth del reporte de cobertura"
```

---

## Fase 2 — Tests HU-05 (Fase A del spec)

### Convenciones para todas las tareas de Fase 2

- Package del test: mismo package que la clase bajo prueba pero en `src/test/java/...`.
- Builder helpers: si el test necesita >3 entidades repetidas, extraer helpers `private static` al final del archivo (e.g. `private static Producto producto(Long id, String nombre, BigDecimal precio, Integer stock, CategoriaProducto cat)`).
- Authentication: helper `private Authentication crearAuthentication(String username, String... roles)` con `lenient()` si se reusa entre tests.
- Verificar al iniciar la tarea: leer 30–50 líneas del test sibling más cercano (`MesaServiceTest` o `VisitaServiceTest`) para detectar convenciones locales (anotaciones, estilo de stubs, naming).

---

### Tarea 2.1: `ComandaBorradorValidadorTest` (NUEVO)

**Files:**
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorValidadorTest.java`
- Reference: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorValidador.java`

Primero porque no tiene dependencias mockeables complejas y los otros tests lo usan conceptualmente.

- [ ] **Paso 1: Leer el validador completo y enumerar métodos públicos**

```bash
cat backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorValidador.java
```
Esperados según spec §4.2: `validarStock`, `resolverEstacion`, `validarTieneItems`. Si la firma actual difiere, ajustar el test a la firma real (NO modificar producción).

- [ ] **Paso 2: Crear esqueleto del archivo de test**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComandaBorradorValidadorTest {

    private final ComandaBorradorValidador validador = new ComandaBorradorValidador();

    @Nested
    @DisplayName("validarStock")
    class ValidarStock { /* tests aquí */ }

    @Nested
    @DisplayName("resolverEstacion")
    class ResolverEstacion { /* tests aquí */ }

    @Nested
    @DisplayName("validarTieneItems")
    class ValidarTieneItems { /* tests aquí */ }

    private static Producto producto(Long id, Integer stock, CategoriaProducto cat) {
        Producto p = new Producto();
        p.setProductoId(id);
        p.setProductoNombre("X");
        p.setProductoPrecio(BigDecimal.ONE);
        p.setProductoStock(stock);
        p.setProductoCategoria(cat);
        return p;
    }
}
```

- [ ] **Paso 3: Escribir tests `validarStock` (6 tests)**

Dentro de `class ValidarStock`:

```java
@Test
void stockNullNoValida() {
    Producto p = producto(1L, null, CategoriaProducto.PLATO);
    // assume signature: validarStock(Producto, int cantidadNueva, int cantidadAnterior)
    // si stock null => no debe lanzar
    validador.validarStock(p, 5, 0);
}

@Test
void cantidadIgualADisponibleOk() {
    Producto p = producto(1L, 3, CategoriaProducto.PLATO);
    validador.validarStock(p, 3, 0);
}

@Test
void cantidadMayorADisponibleLanzaInsufficientStock() {
    Producto p = producto(1L, 2, CategoriaProducto.PLATO);
    assertThatThrownBy(() -> validador.validarStock(p, 3, 0))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Solo hay")
        .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
}

@Test
void descuentaCantidadAnteriorAlValidar() {
    Producto p = producto(1L, 5, CategoriaProducto.PLATO);
    // disponible = 5, anterior = 3 → cantidad efectiva nueva max = 5+3 = 8 (o 5-(nueva-anterior))
    // verificar comportamiento real con un caso donde sin descuento fallaría pero con descuento pasa
    validador.validarStock(p, 7, 3); // (7-3)=4 ≤ 5 → OK
}

@Test
void mensajeDeError_ContieneCantidadDisponible() {
    Producto p = producto(1L, 2, CategoriaProducto.PLATO);
    assertThatThrownBy(() -> validador.validarStock(p, 5, 0))
        .hasMessageContaining("2"); // disponible reportado
}

@Test
void disponibleNegativoSeReportaComoCero() {
    Producto p = producto(1L, -3, CategoriaProducto.PLATO);
    assertThatThrownBy(() -> validador.validarStock(p, 1, 0))
        .hasMessageContaining("0");
}
```

> Si la firma real es distinta (e.g. `validarStock(Producto, int cantidadEfectivaSolicitada)` sin `cantidadAnterior`), ajustar tests a la firma real y mantener los 6 casos del spec.

- [ ] **Paso 4: Escribir tests `resolverEstacion` (4 tests, parametrizado)**

```java
@ParameterizedTest
@CsvSource({
    "PLATO,COCINA",
    "BEBIDA,BARRA"
})
void resuelveEstacionPorCategoria(CategoriaProducto cat, EstacionComanda esperada) {
    Producto p = producto(1L, 1, cat);
    assertThat(validador.resolverEstacion(p)).isEqualTo(esperada);
}

@Test
void categoriaOtroLanza() {
    Producto p = producto(1L, 1, CategoriaProducto.OTRO);
    assertThatThrownBy(() -> validador.resolverEstacion(p))
        .isInstanceOf(BusinessException.class);
}

@Test
void categoriaNullLanza() {
    Producto p = producto(1L, 1, null);
    assertThatThrownBy(() -> validador.resolverEstacion(p))
        .isInstanceOf(Exception.class);
}
```

- [ ] **Paso 5: Escribir tests `validarTieneItems` (2 tests)**

```java
@Test
void listaVaciaLanza() {
    assertThatThrownBy(() -> validador.validarTieneItems(List.of()))
        .isInstanceOf(BusinessException.class);
}

@Test
void listaNullLanza() {
    assertThatThrownBy(() -> validador.validarTieneItems(null))
        .isInstanceOf(BusinessException.class);
}

@Test
void listaNoVaciaPasa() {
    // construir un ComandaItem mínimo
    var item = new co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem();
    validador.validarTieneItems(List.of(item));
}
```

- [ ] **Paso 6: Ejecutar y verificar verde**

```bash
./mvnw test -Dtest=ComandaBorradorValidadorTest
```
Esperado: 12+ tests pasan. Si algún test choca con la firma real, ajustar (NO modificar producción) y re-correr.

- [ ] **Paso 7: Verificar cobertura del validador**

```bash
./mvnw clean test jacoco:report
```
Abrir `target/site/jacoco/co.edu.unicauca.backend.modules.mesas_comandas.service/ComandaBorradorValidador.html`. Verificar 100% líneas y ≥95% ramas.

- [ ] **Paso 8: Commit (reportar y esperar)**

```bash
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorValidadorTest.java
git commit -m "test(comandas): cubrir ComandaBorradorValidador al 100%"
```

---

### Tarea 2.2: `ComandaBorradorMapperTest` (NUEVO)

**Files:**
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/ComandaBorradorMapperTest.java`
- Reference: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/ComandaBorradorMapper.java`

- [ ] **Paso 1: Leer el mapper y enumerar métodos públicos**

```bash
cat backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/ComandaBorradorMapper.java
```
Esperados según spec §4.3 y obs 1066–1075:
- `toBorradorResponse(...)`
- `toItemsResponse(...)`
- `toItemVisitaResponse(...)` (privado o package — eliminado del mapper, mover si aplica)
- `toItemsVisitaResponse(...)`
- Helpers privados de modificaciones de menú.

- [ ] **Paso 2: Crear esqueleto y helpers**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComandaBorradorMapperTest {

    private final ComandaBorradorMapper mapper = new ComandaBorradorMapper();

    @Nested
    class ToBorradorResponse { /* 6 tests */ }

    @Nested
    class ToItemsResponse { /* 2 tests */ }

    @Nested
    class MapearModificacionesMenu { /* 2 tests */ }

    // ------- helpers -------
    private static Producto producto(Long id, String nombre, BigDecimal precio, CategoriaProducto cat) {
        Producto p = new Producto();
        p.setProductoId(id); p.setProductoNombre(nombre);
        p.setProductoPrecio(precio); p.setProductoCategoria(cat);
        return p;
    }
    private static ComandaItem item(Producto p, int cantidad, String desc) {
        ComandaItem i = new ComandaItem();
        i.setProducto(p); i.setComandaItemCantidad(cantidad); i.setComandaItemDescripcion(desc);
        return i;
    }
    private static Comanda comanda(EstacionComanda est, EstadoComanda estado, List<ComandaItem> items) {
        Comanda c = new Comanda();
        c.setComandaEstacion(est); c.setComandaEstado(estado);
        c.setComandaItems(items);
        return c;
    }
}
```

- [ ] **Paso 3: Tests `toBorradorResponse`**

Casos (asegurar uno por test):
1. `sinBorradores_DevuelveEstructuraVaciaConFlagsFalse` — input listas vacías.
2. `soloCocina_LlenaBorradorCocinaYBarraVacia`.
3. `soloBarra_LlenaBorradorBarraYCocinaVacia`.
4. `ambasEstaciones_LlenaAmbosBorradoresYSubtotalSumaTodo`.
5. `subtotalCalcula_Cantidad_Por_Precio_Por_Item` — verificar precisión `BigDecimal`.
6. `precioNullEnProducto_NoRompeYSubtotalIgnoraItem` (o `BigDecimal.ZERO` según implementación).
7. `flagPuedeEnviarCocina_TrueSoloSiBorradorCocinaTieneItems`.
8. `flagPuedeEnviarBarra_TrueSoloSiBorradorBarraTieneItems`.

> Si 6 tests del spec se cuentan 1.+2.+3.+4.+5.+6., agrupar 7 y 8 dentro de 4 con assertions múltiples.

Plantilla por test (variante 4):
```java
@Test
void ambasEstaciones_LlenaAmbosBorradoresYSubtotalSumaTodo() {
    Producto plato = producto(1L, "Pasta", new BigDecimal("10000"), CategoriaProducto.PLATO);
    Producto bebida = producto(2L, "Cola", new BigDecimal("3000"), CategoriaProducto.BEBIDA);
    Comanda cocina = comanda(EstacionComanda.COCINA, EstadoComanda.BORRADOR, List.of(item(plato, 2, null)));
    Comanda barra = comanda(EstacionComanda.BARRA, EstadoComanda.BORRADOR, List.of(item(bebida, 3, null)));

    var resp = mapper.toBorradorResponse(99L, List.of(cocina, barra));

    assertThat(resp.getVisitaId()).isEqualTo(99L);
    assertThat(resp.getBorradorCocina().getItems()).hasSize(1);
    assertThat(resp.getBorradorBarra().getItems()).hasSize(1);
    assertThat(resp.getSubtotal()).isEqualByComparingTo("29000"); // 2*10000 + 3*3000
    assertThat(resp.getPuedeEnviarCocina()).isTrue();
    assertThat(resp.getPuedeEnviarBarra()).isTrue();
}
```

- [ ] **Paso 4: Tests `toItemsResponse`**

```java
@Test
void listaVacia_DevuelveVacia() {
    assertThat(mapper.toItemsResponse(List.of())).isEmpty();
}

@Test
void ordenAlfabeticoCaseInsensitive() {
    Producto a = producto(1L, "zorro", BigDecimal.ONE, CategoriaProducto.PLATO);
    Producto b = producto(2L, "Avena", BigDecimal.ONE, CategoriaProducto.PLATO);
    var items = List.of(item(a, 1, null), item(b, 1, null));
    var resp = mapper.toItemsResponse(items);
    assertThat(resp).extracting("nombre").containsExactly("Avena", "zorro");
}
```

> Si el método no aplica orden (lo aplica el caller), eliminar el segundo test y reemplazar por uno que verifique mapping campo a campo.

- [ ] **Paso 5: Tests modificaciones menú**

Dos casos: con opciones (lista no vacía) y sin opciones (null o vacía). Verificar que el response refleja `menuGrupo` cuando existe (obs 1047).

- [ ] **Paso 6: Run + cobertura + commit**

```bash
./mvnw test -Dtest=ComandaBorradorMapperTest
./mvnw clean test jacoco:report
```
Verificar 100% líneas en `ComandaBorradorMapper.html`. Commit:

```bash
git add backend/src/test/java/.../ComandaBorradorMapperTest.java
git commit -m "test(comandas): cubrir ComandaBorradorMapper al 100%"
```

---

### Tarea 2.3: `ComandaBorradorServiceTest` (NUEVO, ~45 tests)

**Files:**
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorServiceTest.java`
- Reference: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorService.java`

**Sub-tareas**: dividir en 7 commits, uno por `@Nested`. Cada sub-tarea sigue el mismo flujo Red→Green→Refactor.

- [ ] **Paso 1: Crear esqueleto con todas las 12 dependencias mockeadas**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.ComandaBorradorMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.EstacionWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ComandaBorradorServiceTest {

    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock ProductoRepository productoRepository;
    @Mock MesaRepository mesaRepository;
    @Mock ComandaBorradorMapper borradorMapper;
    @Mock VisitaEstadoMapper visitaEstadoMapper;
    @Mock ComandaBorradorValidador validador;
    @Mock MesaValidador mesaValidador;
    @Mock MesaWsPublisher mesaWsPublisher;
    @Mock NotificacionWsPublisher notificacionWsPublisher;
    @Mock EstacionWsPublisher estacionWsPublisher;
    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks ComandaBorradorService service;

    private Authentication auth;

    @BeforeEach
    void setUp() {
        auth = new UsernamePasswordAuthenticationToken(
            "mesero@unicauca.edu.co",
            "x",
            List.of(new SimpleGrantedAuthority("ROLE_MESERO")));
    }

    @Nested @DisplayName("obtenerBorrador")        class ObtenerBorrador {}
    @Nested @DisplayName("agregarItem")            class AgregarItem {}
    @Nested @DisplayName("modificarItem")          class ModificarItem {}
    @Nested @DisplayName("eliminarItem")           class EliminarItem {}
    @Nested @DisplayName("actualizarNotas")        class ActualizarNotas {}
    @Nested @DisplayName("enviarAProduccion")      class EnviarAProduccion {}
    @Nested @DisplayName("cancelarFormulario")     class CancelarFormulario {}
}
```

Verificar que compila: `./mvnw test-compile`. Esperado: BUILD SUCCESS (sin tests todavía).

- [ ] **Paso 2: Sub-tarea A — `ObtenerBorrador` (2 tests)**

Tests:
1. `cuandoVisitaTieneBorradoresEntoncesDevuelveResponseDelMapper` — `mesaValidador.validarOwnership` retorna Mesa; `comandaRepository.findByVisita...` retorna lista; `borradorMapper.toBorradorResponse` retorna DTO; verifica retorno.
2. `cuandoVisitaNoTieneBorradoresEntoncesDevuelveEstructuraVacia` — lista vacía; verifica que el mapper fue invocado con `List.of()` y se retornó su resultado.

Run: `./mvnw test -Dtest=ComandaBorradorServiceTest$ObtenerBorrador`. Commit:
```
test(comandas): ComandaBorradorService.obtenerBorrador (2 tests)
```

- [ ] **Paso 3: Sub-tarea B — `AgregarItem` (8 tests)**

Tests (un test por caso del spec §4.1 fila `AgregarItem`):
1. `productoInexistente_Lanza404` — `productoRepository.findById` vacío.
2. `productoMenuEspecial_LanzaBusiness` — producto con flag `esMenuEspecial=true`.
3. `acumulaCantidadCuandoCoincideProductoYDescripcion` — `comandaItemRepository.findByComanda...AndDescripcion` retorna item existente; verifica que se incrementa `cantidad`.
4. `revalidaStockAlAcumular` — el `validador.validarStock` debe ser invocado con la cantidad acumulada.
5. `productoNuevo_CreaItem` — no hay item previo; se persiste nuevo.
6. `creaComandaBorradorCocinaSiNoExiste` — `validador.resolverEstacion` retorna COCINA; no hay comanda BORRADOR en COCINA; se crea.
7. `creaComandaBorradorBarraSiNoExiste` — análogo para BARRA.
8. `publicaTopicMesas` — `verify(mesaWsPublisher).publicar...(...)`.

Patrón de un test:
```java
@Test
void productoInexistente_Lanza404() {
    var req = new AgregarItemRequest();
    req.setVisitaId(99L); req.setProductoId(7L); req.setCantidad(1);
    when(mesaValidador.validarOwnership(99L, auth)).thenReturn(new Mesa());
    when(productoRepository.findById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.agregarItem(req, auth))
        .isInstanceOf(ResourceNotFoundException.class);
}
```

Run + commit:
```
test(comandas): ComandaBorradorService.agregarItem (8 tests)
```

- [ ] **Paso 4: Sub-tarea C — `ModificarItem` (7 tests)**

Casos: item 404; comanda no BORRADOR (INVALID_STATE); cantidad=0 (INVALID_STATE); solo cantidad; solo descripción; ambos campos; revalida stock al cambiar cantidad. Commit:
```
test(comandas): ComandaBorradorService.modificarItem (7 tests)
```

- [ ] **Paso 5: Sub-tarea D — `EliminarItem` (8 tests)**

Casos: item 404; comanda no BORRADOR; ítem con `menuGrupo` elimina par cocina+barra (verifica 2 deletes); ítem base sin descripción arrastra modificados (verifica que items con misma `productoId` y `descripcion!=null` también se eliminan); ítem modificado solo elimina ese; comanda queda vacía → se elimina comanda; publica `/topic/mesas` (2 variantes: con borrador remanente y sin). Commit:
```
test(comandas): ComandaBorradorService.eliminarItem (8 tests)
```

- [ ] **Paso 6: Sub-tarea E — `ActualizarNotas` (4 tests)**

Casos: comanda 404; comanda no BORRADOR; set notas; set null limpia notas; verifica que NO publica `/topic/mesas` (`verify(mesaWsPublisher, never())...`). Commit:
```
test(comandas): ComandaBorradorService.actualizarNotas (4 tests)
```

- [ ] **Paso 7: Sub-tarea F — `EnviarAProduccion` (8 tests)**

Casos: comanda 404; no BORRADOR; sin items → BusinessException; stock insuficiente para un item; menú especial NO valida stock; transición BORRADOR→PENDIENTE y sella `fechaHoraInicio`; mesa ESPERA→EN_PREPARACION; mesa ya EN_PREPARACION no cambia; publica RabbitMQ (`rabbitTemplate.convertAndSend`) + estación + mapa + cliente. Commit:
```
test(comandas): ComandaBorradorService.enviarAProduccion (8 tests)
```

- [ ] **Paso 8: Sub-tarea G — `CancelarFormulario` (3 tests)**

Casos: con borradores (verifica deletes); sin borradores (idempotente, no falla); publica `/topic/mesas` siempre. Commit:
```
test(comandas): ComandaBorradorService.cancelarFormulario (3 tests)
```

- [ ] **Paso 9: Verificar cobertura completa del service**

```bash
./mvnw clean test jacoco:report
```
Abrir `target/site/jacoco/.../ComandaBorradorService.html`. Verificar ≥100% líneas y ≥95% ramas. Si quedan ramas no cubiertas, agregar tests adicionales bajo el `@Nested` correspondiente y un nuevo commit `test(comandas): cubrir ramas faltantes de ComandaBorradorService`.

---

### Tarea 2.4: `ComandaControllerTest` (NUEVO)

**Files:**
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/ComandaControllerTest.java`
- Reference: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/ComandaController.java`

- [ ] **Paso 1: Detectar el patrón usado**

```bash
head -60 backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/MesaControllerTest.java
head -60 backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaControllerTest.java
```

Si ambos usan `@WebMvcTest` + `MockMvc`: replicar. Si usan unit-puro con `@InjectMocks` y `MockMvcBuilders.standaloneSetup`: replicar ese patrón. **No mezclar estilos.**

- [ ] **Paso 2: Crear el test con 2 tests por endpoint × 7 endpoints**

Endpoints (spec §4.4):
- `GET /api/comandas/borrador?visitaId={id}` — happy + auth/error
- `POST /api/comandas/borrador/items` — happy + body inválido (`@Valid` falla por campo faltante)
- `PATCH /api/comandas/borrador/items/{itemId}` — happy + body inválido
- `DELETE /api/comandas/borrador/items/{itemId}` — happy + service lanza `ResourceNotFoundException`
- `POST /api/comandas/borrador/{comandaId}/enviar` — happy + service lanza `BusinessException`
- `DELETE /api/comandas/borrador?visitaId={id}` — happy + sin parámetro (400)
- `PATCH /api/comandas/borrador/{comandaId}/notas` — happy + body inválido

Plantilla con `@WebMvcTest`:
```java
@WebMvcTest(controllers = ComandaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class ComandaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;
    @MockBean ComandaBorradorService service;

    @Test
    @WithMockUser(roles = "MESERO")
    void getBorrador_devuelve200ConCuerpo() throws Exception {
        when(service.obtenerBorrador(eq(99L), any())).thenReturn(new BorradorComandaResponse());
        mockMvc.perform(get("/api/comandas/borrador").param("visitaId", "99"))
            .andExpect(status().isOk());
    }
    // ... resto
}
```

> Si se está usando standaloneSetup, sustituir.

- [ ] **Paso 3: Run, cobertura, commit**

```bash
./mvnw test -Dtest=ComandaControllerTest
./mvnw clean test jacoco:report
```
Verificar `ComandaController.html` ≥85% líneas. Commit:
```
test(comandas): cubrir ComandaController con 14 tests (7 endpoints × happy/error)
```

---

### Tarea 2.5: Tests de repositories (`@DataJpaTest` + H2)

**Files:**
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaItemRepositoryTest.java`
- Create: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaRepositoryTest.java`
- Create (si tiene queries no triviales): `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaMenuModificacionRepositoryTest.java`

- [ ] **Paso 1: Verificar compatibilidad H2 de cada `@Query`**

```bash
grep -n "@Query" backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaItemRepository.java backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaRepository.java backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaMenuModificacionRepository.java
```

Para cada query: si usa funciones PostgreSQL específicas (`array_agg`, `jsonb_*`, `::tipo`, etc.), **excluir del test H2** y documentar en el archivo con `// PG-only: ver SchemaValidationIT`. Para queries JPQL puras o SQL ANSI: testeables en H2.

- [ ] **Paso 2: `ComandaItemRepositoryTest` (5 tests)**

Plantilla:
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ComandaItemRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ComandaItemRepository repo;

    @Test
    void sumCantidadComprometidaByProducto_sinItems_devuelveCero() { /* ... */ }

    @Test
    void sumCantidadComprometidaByProducto_conItemsEnVariosEstados_sumaCorrecto() { /* ... */ }

    @Test
    void sumTotalActivosByVisita_listaVacia_devuelveCero() { /* ... */ }

    @Test
    void findByComanda_yProducto_yDescripcionExacta_matcheaIncluyendoDescripcionNull() { /* ... */ }

    @Test
    void findByComanda_OrderByProductoNombreAsc_respetaOrden() { /* ... */ }
}
```

Construir las entidades vía `em.persistAndFlush(...)`. Cada test independiente — sin `@BeforeAll` con seed compartido.

- [ ] **Paso 3: `ComandaRepositoryTest` (3 tests)**

```java
@Test void findAllItemsActivosByVisita_excluyeCompletado() { /* obs 1066 */ }
@Test void findByVisitaYEstadoYEstacion_aplicaTodosLosFiltros() { }
@Test void findByVisitaYEstado_devuelveAmbasEstaciones() { }
```

- [ ] **Paso 4: `ComandaMenuModificacionRepositoryTest` (si aplica)**

Solo si hay `@Query` no trivial. En caso contrario, omitir el archivo y anotar en el commit.

- [ ] **Paso 5: Run, cobertura, commit**

```bash
./mvnw test -Dtest='*RepositoryTest'
```
Commit:
```
test(comandas): tests @DataJpaTest sobre ComandaItem/ComandaRepository
```

---

### Tarea 2.6: Tests de entidades — completar huecos

**Files:**
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/entity/ComandaTest.java`
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/entity/ComandaItemTest.java`
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/entity/ComandaMenuModificacionTest.java`

- [ ] **Paso 1: Diff de cobertura por entidad**

```bash
./mvnw clean test jacoco:report
```
Abrir cada archivo HTML en `target/site/jacoco/.../entity/`. Listar líneas no cubiertas por entidad.

- [ ] **Paso 2: Agregar tests para métodos calculados sin cobertura**

Para cada hueco: un test enfocado. **NO testear getters/setters Lombok.** Patrón:
```java
@Test
void getVisitaId_delegaAMesa() {
    Mesa m = new Mesa();
    m.setVisitaId(42L);
    Comanda c = new Comanda();
    c.setMesa(m);
    assertThat(c.getVisitaId()).isEqualTo(42L);
}
```

- [ ] **Paso 3: Run, verificar, commit**

```bash
./mvnw test -Dtest='*EntityTest,ComandaTest,ComandaItemTest,ComandaMenuModificacionTest'
```
Commit:
```
test(comandas): completar cobertura de entities Comanda/ComandaItem/ComandaMenuModificacion
```

---

### Tarea 2.7: Tests modificados (existentes) — verificar regresiones

**Files:**
- Modify (si necesario): `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaAsignarServiceTest.java`
- Modify (si necesario): `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/VisitaEstadoServiceTest.java`
- Modify: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaControllerTest.java`

- [ ] **Paso 1: Confirmar `MesaAsignarServiceTest` y `VisitaEstadoServiceTest` siguen verdes**

```bash
./mvnw test -Dtest=MesaAsignarServiceTest,VisitaEstadoServiceTest
```
Si pasan: anotar y continuar. Si fallan: aplicar fixes documentados en obs 1100, 1101, 1083, 1084.

- [ ] **Paso 2: Agregar tests en `VisitaControllerTest` para el cambio del commit `70c5d85`**

Lectura previa:
```bash
git show 70c5d85 -- backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/VisitaController.java
```
Identificar el método del controller modificado. Agregar 1–2 tests que verifiquen que la llamada WS unificada se invoca exactamente una vez (no dos como antes).

- [ ] **Paso 3: Run, commit**

```bash
./mvnw test -Dtest=VisitaControllerTest
```
Commit:
```
test(visita): cubrir actualizacion unificada de estado por WS (70c5d85)
```

---

### Tarea 2.8: Gate de Fase A — verificar 100% en archivos HU-05

- [ ] **Paso 1: Reporte completo**

```bash
./mvnw clean test jacoco:report
```

- [ ] **Paso 2: Tabla de verificación**

Revisar manualmente cada uno de estos archivos en `target/site/jacoco/`:

| Archivo                              | Líneas | Ramas |
|--------------------------------------|--------|-------|
| `ComandaBorradorService`             | 100%   | ≥95%  |
| `ComandaBorradorValidador`           | 100%   | ≥95%  |
| `ComandaBorradorMapper`              | 100%   | ≥95%  |
| `ComandaController`                  | ≥85%   | —     |
| `ComandaItemRepository` (queries)    | testeadas | — |
| `ComandaRepository` (queries nuevas) | testeadas | — |
| `Comanda`, `ComandaItem`, `ComandaMenuModificacion` | ≥80% | — |
| `VisitaEstadoMapper` (refactor 11-may) | 100%  | ≥95%  |

Si falta cobertura: volver a la tarea correspondiente, agregar test específico, commit `test(comandas): completar rama X de Y`.

- [ ] **Paso 3: Commit de cierre de Fase A si hay ajustes**

(Si no hay cambios, omitir.)

---

## Fase 3 — Cobertura del resto del proyecto (Fase B del spec)

### Tarea 3.0: Análisis de gaps

**Files:**
- Create (temporal, no commitear): `docs/superpowers/plans/_jacoco-gaps-2026-05-11.md`

- [ ] **Paso 1: Generar reporte y abrir XML**

```bash
./mvnw clean test jacoco:report
```
Abrir `backend/target/site/jacoco/jacoco.xml`.

- [ ] **Paso 2: Extraer tabla por clase**

Por cada `<class name="...">` con `LINE` y `BRANCH` covered/missed:
- Calcular `% líneas = covered/(covered+missed)*100`.
- Calcular `% ramas = ...`.
- Excluir clases bajo `modules/reportes*`, `modules/auth`, `BackendApplication`.

- [ ] **Paso 3: Priorizar**

Tabla con columnas `[modulo, clase, tipo, % líneas, % ramas, prioridad, umbral]`. Aplicar criterios §6.2 del spec:

| Prioridad | Tipo                                  | Umbral líneas | Umbral ramas |
|-----------|---------------------------------------|---------------|--------------|
| 1         | Services / Validadores / Mappers      | ≥90%          | ≥80%         |
| 2         | Controllers REST                      | ≥85%          | —            |
| 3         | Repositories con `@Query` no trivial  | testeados directos | —       |
| 4         | Entities con métodos de negocio       | ≥80%          | —            |
| 5         | DTOs con validaciones                 | tests Validator | —          |
| —         | DTOs puros / configs Lombok-only      | omitir        | —            |

Guardar la tabla en `_jacoco-gaps-2026-05-11.md` (archivo de trabajo, NO commitear).

---

### Tarea 3.1: Módulo `mesas_comandas` (no-HU05)

Archivos a revisar (spec §6.3):
- `VisitaService`, `MesaService`, `MesaAsignarService`, `MesaValidador`, `MesaAsignarEvaluador`, `VisitaEstadoService`
- Mappers no-HU05: `MesaMapper`, `VisitaMapper`, `VisitaEstadoMapper`

- [ ] **Paso 1: Para cada archivo bajo umbral, agregar tests a su test sibling existente**

Ejemplo: si `MesaService.html` muestra 78% líneas con líneas 145–160 no cubiertas, abrir `MesaService.java:145-160`, identificar la rama y agregar un test en `MesaServiceTest` que la ejercite. **No reescribir tests existentes.** Solo agregar.

- [ ] **Paso 2: Run + verificar incremento**

```bash
./mvnw test -Dtest='Mesa*Test,Visita*Test'
./mvnw clean test jacoco:report
```

- [ ] **Paso 3: Commit por archivo o por sub-módulo**

Cada test agregado va en un commit pequeño:
```
test(mesas): cubrir rama X en MesaService
```

---

### Tarea 3.2: Módulo `inventario`

Archivos foco: `ProductoService`, `ProductoMapper`, `ProductoRepository` (queries custom).

- [ ] **Paso 1: Revisar tests existentes**

```bash
ls backend/src/test/java/co/edu/unicauca/backend/modules/inventario/
```

- [ ] **Paso 2: Agregar tests para alcanzar prioridad 1 (≥90% líneas)**

Patrón: extender los tests existentes con casos nuevos.

- [ ] **Paso 3: Run + commits incrementales**

```
test(inventario): cubrir ProductoService al 90%+
test(inventario): @DataJpaTest sobre queries de ProductoRepository
```

---

### Tarea 3.3: Módulo `reservas`

Archivos foco: `ReservaServiceCrud`, `ReservaServiceModificar`, `DisponibilidadConsultador`, mappers de reservas.

Mismo patrón que Tarea 3.1/3.2. Commit incremental por archivo.

---

### Tarea 3.4: Módulo `pagos_caja`

Archivo foco: `VentaService`.

Mismo patrón.

---

### Tarea 3.5: Módulo `usuarios`

Archivos foco: `PuntosService`, mappers de usuarios.

Mismo patrón.

---

### Tarea 3.6: Módulo `notificaciones`

Archivos foco: `NotificacionService`, `MesaWsPublisher`, `NotificacionWsPublisher`, `EstacionWsPublisher`.

Para publishers: stub `SimpMessagingTemplate` y verificar `convertAndSend(...)` con los topics y payloads exactos. Lista de topics permitida en CLAUDE.md (`backend/CLAUDE.md` sección WebSocket).

Commit:
```
test(notificaciones): cubrir NotificacionService y publishers WS
```

---

### Tarea 3.7: `shared` (excepciones, configs)

Archivos foco: `GlobalExceptionHandler`.

Para `GlobalExceptionHandler`: test unitario que lanza cada tipo de excepción registrada (`ResourceNotFoundException`, `BusinessException`, `AccessDeniedException`, etc.) y verifica `status` y `errorCode` del `ApiResponse`.

Commit:
```
test(shared): cubrir GlobalExceptionHandler para cada errorCode
```

---

### Tarea 3.8: Gate final de Fase B

- [ ] **Paso 1: Reporte final**

```bash
./mvnw clean test jacoco:report
```

- [ ] **Paso 2: Documentar gaps no resueltos**

Si un archivo no alcanza su umbral, documentar la razón en `docs/testing.md` (sección "Cobertura — excepciones documentadas") con:
- Archivo
- % alcanzado
- Razón (código defensivo inalcanzable, branch determinado por entorno, etc.)

Esto cuenta como cumplimiento del criterio de hecho (spec §7).

- [ ] **Paso 3: Verificación final**

```bash
./mvnw clean test
./mvnw clean test jacoco:report
```
Ambos: BUILD SUCCESS. Reporte JaCoCo sin `reportes*` / `auth`.

- [ ] **Paso 4: Commit de cierre**

```
docs(testing): documentar excepciones de cobertura tras Fase B
```

---

## Done Criteria

- [ ] `backend/pom.xml` con `<excludes>` de JaCoCo aplicado y verificado.
- [ ] Archivos HU-05 (§4) en 100% líneas / ≥95% ramas.
- [ ] `ComandaController` ≥85% líneas con tests `@WebMvcTest`.
- [ ] Repositories de comandas con `@DataJpaTest` para queries nuevas.
- [ ] Archivos prioridad 1–2 de Fase B en su umbral o con excepción documentada.
- [ ] `./mvnw clean test` BUILD SUCCESS.
- [ ] `./mvnw clean test jacoco:report` BUILD SUCCESS, reporte sin `reportes*`/`auth`.
- [ ] Todos los commits del plan listos para review (sin push automático).

---

## Riesgos y mitigaciones (recordatorio del spec §8)

- **H2 ↔ PostgreSQL**: queries con funciones PG-only deben excluirse de `@DataJpaTest`. Anotar con `// PG-only` en el repositorio.
- **NPE por mocks faltantes**: declarar SIEMPRE las 12 dependencias de `ComandaBorradorService` (lección 11-may).
- **`UnnecessaryStubbingException`**: usar `lenient()` solo en helpers compartidos (`crearAuthentication`).
- **Volumen**: ~95 tests Fase A + N tests Fase B subirán el tiempo de suite. Aceptado.
- **Regresiones**: tras §1 (JaCoCo), correr full build para detectar efectos colaterales antes de iniciar Fase 2.

# Diseño — Pruebas Unitarias HU-05 + Aumento de Cobertura

**Fecha:** 2026-05-11
**Rama:** `PA-90-modificar-comanda`
**Autor:** Claude + PMunoz

---

## 1. Objetivo

1. **Cobertura 100%** (líneas, y ≥95% ramas) sobre los archivos nuevos/modificados por HU-05 (Modificar Comanda).
2. **Aumentar cobertura** del resto del proyecto en archivos con lógica de negocio (services, mappers, validadores, controllers, repositories con `@Query`).
3. **Excluir** del reporte JaCoCo los módulos `reportes`, `reportes_clientes`, `reportes_ventas_detalle` y `auth`.

## 2. Alcance

**Incluye:**
- Tests unitarios y de slice (`@DataJpaTest`, `@WebMvcTest`) sobre todos los archivos listados en §4 y §6.
- Modificación de `backend/pom.xml` para excluir módulos del reporte JaCoCo.

**Excluye:**
- Tests de integración end-to-end (`@SpringBootTest` completo).
- Tests sobre módulos `reportes*` y `auth`.
- Postman collection.
- Tests sobre el frontend.

---

## 3. Convenciones técnicas

- **Stack**: JUnit 5 + Mockito + AssertJ (vía spring-boot-starter-test).
- **Pattern**: AAA. `@Nested` para agrupar por método bajo prueba.
- **Naming**: `cuandoXEntoncesY` o `metodo_Condicion_Resultado` (alineado con tests existentes — verificar predominante en `MesaServiceTest`).
- **Services**: unit puro con `@Mock` y `@InjectMocks`. Sin Spring context.
- **Validadores**: tests parametrizados (`@ParameterizedTest`) para tablas de casos.
- **Mappers**: input → output con assertions sobre cada campo, incluyendo collections vacías/null y agrupaciones.
- **Controllers**: replicar el patrón ya usado en `MesaControllerTest` y `VisitaControllerTest`. Verificar si usan `@WebMvcTest` + `MockMvc` o unit puro y mantener consistencia.
- **Repositories con `@Query`**: `@DataJpaTest` con H2 (ya disponible como dependencia test).
- **Entities con lógica**: tests sobre constructores, invariantes, métodos de dominio. NO tests de getters/setters Lombok.
- **Mocks compartidos críticos**: `MesaValidador` y `VisitaEstadoMapper` siempre declarados cuando un service los inyecta (lección del 11-may: NPEs por mocks faltantes).
- **`lenient()`** solo cuando un helper compartido stub-ea cosas que no todos los tests usan.
- **Sin `log.debug()`** en código productivo (regla del proyecto). En tests está permitido pero no se usa.

---

## 4. Fase A — Cobertura HU-05

### 4.1 `ComandaBorradorServiceTest` (NUEVO)

Concentra la mayor parte del esfuerzo. Estimado ~45 tests.

| `@Nested`               | Tests | Casos clave                                                                                                                                                                                                                                       |
|-------------------------|------:|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ObtenerBorrador`       |     2 | con borrador / sin borrador (estructura vacía válida)                                                                                                                                                                                            |
| `AgregarItem`           |     8 | producto inexistente → 404; producto menú especial → BusinessException; acumulación misma desc; revalidación de stock al acumular; producto nuevo crea ítem; crea Comanda BORRADOR de cocina; crea Comanda BORRADOR de barra; publica `/topic/mesas`     |
| `ModificarItem`         |     7 | item 404; comanda no-BORRADOR → INVALID_STATE; cantidad=0 → INVALID_STATE; solo cantidad; solo descripción; ambos campos; revalida stock cuando cantidad cambia                                                                                       |
| `EliminarItem`          |     8 | item 404; comanda no-BORRADOR; ítem con `menuGrupo` elimina par cocina+barra; ítem base sin descripción arrastra modificados; ítem modificado solo elimina ese; elimina comanda si queda vacía; publica `/topic/mesas` (2 variantes)                  |
| `ActualizarNotas`       |     4 | comanda 404; no-BORRADOR; set notas; set null borra; verifica que NO publica `/topic/mesas`                                                                                                                                                          |
| `EnviarAProduccion`     |     8 | 404; no-BORRADOR; sin ítems → BusinessException; stock insuficiente; menú especial NO valida stock; transición + sella `fechaHoraInicio`; mesa ESPERA→EN_PREPARACION; mesa ya EN_PREPARACION no cambia; publica RabbitMQ + estación + mapa + cliente |
| `CancelarFormulario`    |     3 | con borradores; sin borradores (idempotente); publica `/topic/mesas` siempre                                                                                                                                                                       |

**Mocks requeridos:** `ComandaRepository`, `ComandaItemRepository`, `ProductoRepository`, `MesaRepository`, `ComandaBorradorMapper`, `VisitaEstadoMapper`, `ComandaBorradorValidador`, `MesaValidador`, `MesaWsPublisher`, `NotificacionWsPublisher`, `EstacionWsPublisher`, `RabbitTemplate`.

### 4.2 `ComandaBorradorValidadorTest` (NUEVO)

~12 tests.

- `validarStock` — 6: stock null no valida; cantidad ≤ disponible OK; cantidad > disponible → `INSUFFICIENT_STOCK`; descuento correcto de `cantidadAnterior`; mensaje "Solo hay X unidades..."; disponible negativo se reporta como 0.
- `resolverEstacion` — 4: PLATO→COCINA; BEBIDA→BARRA; OTRO→error; (parametrizado para enums futuros).
- `validarTieneItems` — 2: lista null/vacía → error; no-vacía OK.

### 4.3 `ComandaBorradorMapperTest` (NUEVO)

~10 tests.

- `toBorradorResponse` — 6: sin borradores; solo cocina; solo barra; ambas; subtotal correcto; flags `puedeEnviar*`; `menuGrupo` con bebida embebida; precio null → subtotal 0.
- `toItemsResponse` — 2: vacío; orden alfabético case-insensitive.
- `mapearModificacionesMenu` (vía `toBorradorResponse`) — 2: con opciones; sin opciones.

### 4.4 `ComandaControllerTest` (NUEVO)

~14 tests (2 por endpoint × 7 endpoints):

- `GET /api/comandas/borrador` — happy path + sin auth/error
- `POST /api/comandas/borrador/items` — happy path + body inválido (`@Valid`)
- `PATCH /api/comandas/borrador/items/{itemId}` — happy + body inválido
- `DELETE /api/comandas/borrador/items/{itemId}` — happy + service lanza
- `POST /api/comandas/borrador/{comandaId}/enviar` — happy + service lanza
- `DELETE /api/comandas/borrador` — happy + sin visitaId
- `PATCH /api/comandas/borrador/{comandaId}/notas` — happy + body inválido

Patrón a confirmar al iniciar implementación leyendo `MesaControllerTest` / `VisitaControllerTest`.

### 4.5 Tests modificados (existentes)

Verificar y completar si hace falta:

- `MesaAsignarServiceTest` — ya parcheado el 11-may (mock de `VisitaEstadoMapper`, stubs de `findAllItemsActivosByVisita` / `toItemsVisitaResponse`). Confirmar cobertura.
- `VisitaEstadoServiceTest` — ya migrado a `findAllItemsActivosByVisita`. Confirmar cobertura.
- `VisitaControllerTest` — agregar tests para los cambios del commit `70c5d85` (unificar llamada WS).

### 4.6 Repositories nuevos (`@DataJpaTest` + H2)

- `ComandaItemRepositoryTest` (~5 tests):
  - `sumCantidadComprometidaByProducto` — sin items / con items en distintos estados
  - `sumTotalActivosByVisita` — vacía / con items
  - `findByComanda_ComandaIdAndProducto_ProductoIdAndComandaItemDescripcion` — match exacto incluyendo desc null
  - `findByComanda_...DescripcionIsNotNull` — filtra base correctamente
  - `findByComanda_ComandaIdOrderByProductoNombreAsc` — orden
- `ComandaRepositoryTest` (~3 tests):
  - `findAllItemsActivosByVisita` — incluye estados activos, excluye COMPLETADO
  - `findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion` — filtros combinados
  - `findByVisita_VisitaIdAndComandaEstado` — múltiples estaciones
- `ComandaMenuModificacionRepositoryTest` (~2 tests, según queries presentes — verificar al implementar).

### 4.7 Entities con lógica

Reusar tests existentes (`ComandaTest`, `ComandaItemTest`, `ComandaMenuModificacionTest`) y completar huecos para constructores nuevos, métodos calculados (`getVisitaId()`, etc.) y builders.

### 4.8 Estimación Fase A

- ~95 tests nuevos
- Ajustes en 3 tests existentes
- Modificación de 0 archivos productivos (solo tests).

---

## 5. Configuración JaCoCo

Modificar `backend/pom.xml` para agregar bloque `<configuration>` al plugin `jacoco-maven-plugin`:

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

Verificación: `./mvnw clean test jacoco:report` debe generar `target/site/jacoco/index.html` sin las clases excluidas.

---

## 6. Fase B — Cobertura del resto del proyecto

### 6.1 Flujo de identificación de gaps

1. `./mvnw clean test jacoco:report` (tras aplicar §5)
2. Parsear `backend/target/site/jacoco/jacoco.xml`
3. Por clase: extraer `LINE` y `BRANCH` covered/missed
4. Calcular % por clase, agrupar por módulo
5. Generar tabla priorizada (markdown intermedio)

### 6.2 Criterio de priorización

| Prioridad | Tipo de archivo                                    | Umbral objetivo                       |
|-----------|----------------------------------------------------|---------------------------------------|
| 1         | Services / Validadores / Mappers                   | ≥ 90% líneas, ≥ 80% ramas             |
| 2         | Controllers (REST)                                 | ≥ 85% líneas                          |
| 3         | Repositories con `@Query` no trivial               | tests directos con `@DataJpaTest`     |
| 4         | Entities con métodos de negocio                    | ≥ 80% líneas                          |
| 5         | DTOs con validaciones (`@NotNull`, `@Size`, etc.)  | tests de validación con `Validator`   |
| —         | DTOs puros / configs / records / Lombok-only       | se aceptan como vienen                |

Se omiten archivos triviales (POJOs Lombok, configuraciones cubiertas por smoke tests existentes).

### 6.3 Orden por módulo

1. **mesas_comandas (no-HU05)** — `VisitaService`, `MesaService`, `MesaAsignarService`, `MesaValidador`, `MesaAsignarEvaluador`, `VisitaEstadoService`, mappers
2. **inventario** — `ProductoService`, `ProductoMapper`, `ProductoRepository` (queries custom)
3. **reservas** — completar gaps en `ReservaServiceCrud`, `ReservaServiceModificar`, `DisponibilidadConsultador`, mappers
4. **pagos_caja** — `VentaService`
5. **usuarios** — `PuntosService`, mappers
6. **notificaciones** — `NotificacionService`, publishers WS
7. **shared** — `GlobalExceptionHandler`, configs no triviales

### 6.4 Sub-flujo por módulo

Para cada módulo:
1. Leer JaCoCo y listar archivos bajo umbral
2. Escribir tests (siguiendo §3 — convenciones)
3. Re-correr `./mvnw test jacoco:report` y verificar
4. Pasar al siguiente módulo

Sin gates de aprobación intermedios — la usuaria pidió que no se le pida aprobación.

---

## 7. Criterios de "hecho"

- **Fase A:** 100% líneas y ≥ 95% ramas en archivos listados en §4. Verificable en `target/site/jacoco/index.html`.
- **Fase B:** cada archivo prioridad 1–2 cumple su umbral, o tiene justificación documentada en el reporte final (código defensivo inalcanzable, etc.).
- **Build:** `./mvnw clean test` pasa sin errores.
- **JaCoCo:** reporte excluye `reportes*` y `auth` correctamente.

---

## 8. Riesgos y consideraciones

- **`@DataJpaTest` con H2**: H2 no soporta todas las funciones SQL de PostgreSQL. Si una `@Query` usa funciones específicas (`array_agg`, `jsonb_*`, etc.), el test fallará. Mitigación: leer cada query antes de testearla; si requiere PostgreSQL, marcarla como integración (`SchemaValidationIT` ya usa Testcontainers o DB real).
- **Mocks faltantes**: ya pasó con `VisitaEstadoMapper` y `MesaValidador`. Cada vez que se mockea un service, declarar TODAS sus dependencias inyectadas.
- **`UnnecessaryStubbingException`**: usar `lenient()` solo en helpers compartidos cuando aplique. No abusarlo.
- **Volumen de tests**: ~95 nuevos en Fase A + N adicionales en Fase B. Tiempo estimado de ejecución de la suite puede subir notablemente. Aceptable.
- **Tests existentes que rompan**: posibles efectos colaterales si el JaCoCo configurado cambia algo. Verificar con full build tras §5.

---

## 9. Próximo paso

Tras aprobación del spec, generar el plan de implementación detallado vía `superpowers:writing-plans`.

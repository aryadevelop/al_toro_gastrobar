# Testing — JaCoCo & Estrategia

## Comandos

```bash
./mvnw test                              # ejecutar todos los tests
./mvnw clean test jacoco:report          # tests + reporte de cobertura
```

Reporte: `backend/target/site/jacoco/index.html`.

## Estado actual

152 tests pass, 1 skipped.

**Test files clave:** `JwtTokenProviderTest`, `AuthServiceTest`, `ReservaValidadorTest` (19), `ReservaServiceModificarTest` (12), `PuntosServiceTest` (8), `NotificacionServiceTest`, `NotificacionControllerTest`.

---

## Cobertura mínima por capa

| Capa | Cobertura |
|------|-----------|
| Services | 90–95% |
| Controllers | 85–90% |
| Mappers | 90–95% |
| Validators | 95%+ |
| Repositories | 70–80% (solo custom queries) |

## Reglas

1. **NUNCA** implementar feature sin tests — TDD.
2. Tests **ANTES** del commit: `./mvnw clean test jacoco:report`.
3. Cubrir edge cases: validaciones, errores, casos límite, nulls, listas vacías.
4. **Branch coverage** — cada `if/else`, `switch`, `try/catch` con tests para TODAS las ramas.

## Exclusiones válidas

- DTOs / Entities sin lógica de negocio
- `*Application.java`
- Exception classes con solo constructores
- Config classes

---

## Patrones — ver `coding-patterns.md`

- Service Tests: `@ExtendWith(MockitoExtension.class)` + `@MockitoSettings(strictness = Strictness.LENIENT)` + helpers privados.
- Controller Tests: `@WebMvcTest` + `PermissiveSecurityConfig` interna + `@MockitoBean` + `@WithMockUser`.
- Postman tests: ver `postman-conventions.md`.

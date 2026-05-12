# JaCoCo Coverage Gaps — 2026-05-11

Generado a partir de `backend/target/site/jacoco/jacoco.xml`.

Umbrales: Service/Validador/Mapper ≥90% líneas, ≥80% ramas · Controller ≥85% líneas · Entity ≥80% líneas.

Excluidos: modules/reportes*, modules/auth, BackendApplication, DTOs, Repositories.


## Módulo: `mesas_comandas`

| Clase | Tipo | %Líneas | %Ramas | Umbral | Gap |
|---|---|---:|---:|---|---|
| `MesaValidador` | service | 56.4% | 44.4% | L≥90% B≥80% | L:90%-56.4%=33.6pp; B:80%-44.4%=35.6pp |
| `VisitaMapper` | mapper | 90.0% | 43.3% | L≥90% B≥80% | B:80%-43.3%=36.7pp |
| `MesaMapper` | mapper | 100.0% | 58.3% | L≥90% B≥80% | B:80%-58.3%=21.7pp |

## Módulo: `inventario`

| Clase | Tipo | %Líneas | %Ramas | Umbral | Gap |
|---|---|---:|---:|---|---|
| `ProductoService` | service | 78.6% | 0.0% | L≥90% B≥80% | L:90%-78.6%=11.4pp; B:80%-0.0%=80.0pp |
| `ProductoMapper` | mapper | 88.0% | — | L≥90% B≥80% | L:90%-88.0%=2.0pp |
| `ProductoController` | controller | 50.0% | — | L≥85% | L:85%-50.0%=35.0pp |
| `MenuBebidaDisponible` | entity | 0.0% | — | L≥80% | L:80%-0.0%=80.0pp |
| `MenuBebidaDisponibleId` | entity | 0.0% | 0.0% | L≥80% | L:80%-0.0%=80.0pp |

## Módulo: `reservas`

| Clase | Tipo | %Líneas | %Ramas | Umbral | Gap |
|---|---|---:|---:|---|---|
| `ReservaMapper` | mapper | 86.1% | 69.0% | L≥90% B≥80% | L:90%-86.1%=3.9pp; B:80%-69.0%=11.0pp |
| `ReservaConsultaService` | service | 97.7% | 78.6% | L≥90% B≥80% | B:80%-78.6%=1.4pp |

## Módulo: `pagos_caja`

| Clase | Tipo | %Líneas | %Ramas | Umbral | Gap |
|---|---|---:|---:|---|---|
| `Venta` | entity | 30.0% | 16.7% | L≥80% | L:80%-30.0%=50.0pp |

## Módulo: `usuarios`

| Clase | Tipo | %Líneas | %Ramas | Umbral | Gap |
|---|---|---:|---:|---|---|
| `ClienteProfileService` | service | 0.0% | 0.0% | L≥90% B≥80% | L:90%-0.0%=90.0pp; B:80%-0.0%=80.0pp |
| `ClienteProfileController` | controller | 0.0% | — | L≥85% | L:85%-0.0%=85.0pp |
| `CanjePuntos` | entity | 0.0% | 0.0% | L≥80% | L:80%-0.0%=80.0pp |

## Módulo: `notificaciones`

| Clase | Tipo | %Líneas | %Ramas | Umbral | Gap |
|---|---|---:|---:|---|---|
| `NotificacionWsPublisher` | service | 0.0% | — | L≥90% B≥80% | L:90%-0.0%=90.0pp |
| `MesaWsPublisher` | service | 0.0% | — | L≥90% B≥80% | L:90%-0.0%=90.0pp |
| `EstacionWsPublisher` | service | 0.0% | — | L≥90% B≥80% | L:90%-0.0%=90.0pp |

## Resumen — clases bajo umbral por módulo

| Módulo | Clases con gap |
|---|---:|
| mesas_comandas | 3 |
| inventario | 5 |
| reservas | 2 |
| pagos_caja | 1 |
| usuarios | 3 |
| notificaciones | 3 |
| shared | 0 |
| produccion | 0 |
| **Total** | **17** |

## Top-5 peor cobertura líneas

- `usuarios/CanjePuntos` (entity) — 0.0%
- `usuarios/ClienteProfileController` (controller) — 0.0%
- `usuarios/ClienteProfileService` (service) — 0.0%
- `inventario/MenuBebidaDisponible` (entity) — 0.0%
- `inventario/MenuBebidaDisponibleId` (entity) — 0.0%

# Al Toro Gastrobar — Sistema Integral de Gestión

> Sistema web multiplataforma para la gestión operativa del restaurante Al Toro Gastrobar.
> Desarrollado por el equipo **ARYA** — Ingeniería de Sistemas, Universidad del Cauca.

---

## Tabla de contenidos

- [Descripción](#descripción)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Módulos del sistema](#módulos-del-sistema)
- [Roles del sistema](#roles-del-sistema)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Equipo](#equipo)
- [Flujo de trabajo Git](#flujo-de-trabajo-git)

---

## Descripción

Al Toro Gastrobar requiere una solución tecnológica que integre en tiempo real los cuatro nodos operativos del restaurante: **servicio** (meseros), **producción** (cocina y barra), **caja** (cajeros) y **portal del cliente**. El sistema elimina la gestión manual en papel, reduce errores en la transmisión de pedidos y centraliza la información operativa y administrativa.

---

## Tecnologías

| Capa | Tecnología |
|------|------------|
| Frontend | Angular 17 (TypeScript) |
| Backend | Java 17 + Spring Boot |
| Base de datos | PostgreSQL |
| Control de versiones | Git + GitHub |
| Gestión de tareas | Jira |

---

## Arquitectura

El sistema sigue una arquitectura cliente-servidor desacoplada. El frontend Angular consume la API REST del backend Spring Boot mediante HTTP/JSON. Ambas capas se despliegan de forma independiente.

```
Frontend (Angular) ──HTTP/JSON──▶ Backend (Spring Boot) ──▶ PostgreSQL
```

---

## Módulos del sistema

| Épica | Módulo | Actores principales |
|-------|--------|---------------------|
| HE-01 | Autenticación y perfiles | Todos los roles |
| HE-02 | Reservas y consumo | Cliente |
| HE-03 | Mesas y comandas | Mesero, Cajero |
| HE-04 | Producción | Cocinero, Bartender |
| HE-05 | Pagos y caja | Cajero |
| HE-06 | Histórico y reportes | Administrador |
| HE-07 | Inventario y decoraciones | Administrador |
| HE-08 | Personal y clientes | Administrador |

---

## Roles del sistema

| Rol | Acceso |
|-----|--------|
| Administrador | Acceso total: configuración, reportes, inventario y personal |
| Mesero | Mesas asignadas, comandas y notificaciones |
| Cocinero / Bartender | Comandas por estación |
| Cajero | Reservas, pagos, cierre de caja y mapa de mesas |
| Cliente | Reservas, pre-orden, historial de visitas y puntos de fidelización |

---

## Estructura del repositorio

```
al_toro_gastrobar/
├── frontend/               # Aplicación Angular
├── backend/                # API REST con Spring Boot
├── docs/                   # Documentación del proyecto
├── .github/
│   └── workflows/
│       └── validar-rama.yml  # GitHub Action: valida nombres de rama en PRs
├── .gitignore
├── README.md
└── CONTRIBUTING.md         # Reglas de Git y flujo de trabajo
```

---

## Equipo

| Nombre | Rol en Scrum |
|--------|--------------|
| Paula Andrea Muñoz Delgado | Scrum Master · Desarrollador · Analista · Tester |
| Adrián Camilo Bergaño Ortega | Desarrollador |
| Yeixón Julián Gembuel Ciclos | Tester |
| Rubeiro Romero | Desarrollador · Analista |

---

## Flujo de trabajo Git

Todas las reglas operativas de Git —nombres de ramas, formato de commits, protección de ramas y procedimiento para Pull Requests— están documentadas en:
[`CONTRIBUTING.md`](./CONTRIBUTING.md)

Antes de realizar el primer commit, cada miembro del equipo debe leer y seguir ese documento.

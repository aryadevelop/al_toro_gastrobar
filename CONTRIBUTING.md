# Guía de contribución — Al Toro Gastrobar

Este documento define las reglas que **todos los miembros del equipo** deben seguir al trabajar con el repositorio. El incumplimiento de estas reglas bloqueará la integración del código.

---

## Tabla de contenidos

- [Ramas protegidas](#ramas-protegidas)
- [Convención para nombres de ramas](#convención-para-nombres-de-ramas)
- [Convención para commits](#convención-para-commits)
- [Flujo de trabajo paso a paso](#flujo-de-trabajo-paso-a-paso)
- [Protección de ramas en GitHub](#protección-de-ramas-en-github)

---

## Ramas protegidas

| Rama | Propósito | Protección |
|------|-----------|------------|
| `main` | Código listo para producción | Requiere PR + aprobación + checks pasados |
| `develop` | Integración continua del equipo | Requiere PR + aprobación + checks pasados |

**Nadie puede hacer push directo a `main` o `develop`.** Todos los cambios entran a través de Pull Requests (PRs).

---

## Convención para nombres de ramas

Cada rama de trabajo debe corresponder a una **subtarea de Jira**. El formato es obligatorio:

```
PA-{numero}-{descripcion-con-guiones}
```

- `PA` — prefijo fijo del proyecto (siempre en mayúsculas).
- `numero` — ID de la subtarea en Jira.
- `descripcion` — texto en minúsculas, palabras separadas por guiones.

### Ejemplos válidos

```
PA-11-ingresar-al-sistema-back
PA-12-ingresar-al-sistema-front
PA-27-crear-cuenta-cliente-back
PA-28-crear-cuenta-cliente-front
```

### Ejemplos inválidos

```
feature/login          # Falta prefijo PA-{numero}
PA-11-ingresar         # Descripción demasiado corta
pa-11-login            # El prefijo PA debe ir en mayúsculas
```

---

## Convención para commits

El formato de cada mensaje de commit debe ser:

```
<tipo>(<scope opcional>): <descripción en minúsculas>
```

### Tipos permitidos

| Tipo | Uso |
|------|-----|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de error |
| `docs` | Solo documentación |
| `style` | Formato o espacios sin cambio lógico |
| `refactor` | Reestructuración de código sin cambiar comportamiento |
| `test` | Agregar o corregir pruebas |
| `chore` | Mantenimiento, dependencias o configuración |

### Ejemplos

```
feat(mesas): agregar mapa de mesas en tiempo real
fix(auth): corregir expiración de JWT
docs(readme): actualizar instrucciones de instalación
test(reservas): agregar pruebas unitarias al servicio
chore(deps): actualizar spring-boot a 2.7.0
```

---

## Flujo de trabajo paso a paso

### 1. Mantener `develop` actualizado

```bash
git checkout develop
git pull origin develop
```

### 2. Crear rama de tarea desde `develop`

```bash
git checkout -b PA-XX-descripcion-con-guiones
```

### 3. Desarrollar y hacer commits frecuentes

```bash
git add .
git commit -m "feat(scope): descripción clara"
```

### 4. Sincronizar con los últimos cambios de `develop` (rebase)

```bash
git fetch origin
git rebase origin/develop
```

Si hay conflictos, resuélvelos y continúa con:

```bash
git rebase --continue
```

### 5. Subir la rama a GitHub

```bash
git push origin PA-XX-descripcion-con-guiones
```

### 6. Abrir Pull Request (PR) hacia `develop`

1. Ve al repositorio en GitHub → **Pull requests** → **New pull request**.
2. Configura: `base: develop` y `compare: PA-XX-...`.
3. Escribe una descripción clara de los cambios realizados.

### 7. Esperar la revisión

- El PR debe pasar la validación automática del nombre de la rama.
- Al menos un compañero debe aprobar el PR.
- Si hay comentarios, resuélvelos antes de continuar.

### 8. Merge a `develop`

Una vez que todos los checks estén en verde y cuentes con la aprobación requerida, haz clic en **Merge pull request**.

> **Nota:** Al final de cada sprint se abre un PR desde `develop` hacia `main`, siguiendo el mismo proceso descrito anteriormente.

---

## Protección de ramas en GitHub

Las ramas `main` y `develop` están protegidas con las siguientes reglas (configuradas en **Settings → Branches**):

- Requerir un Pull Request antes de hacer merge.
- Requerir al menos 1 aprobación.
- Descartar aprobaciones anteriores cuando se suben nuevos commits.
- Requerir que los status checks pasen (incluyendo `validar-rama`).
- Requerir que se resuelvan todas las conversaciones abiertas.
- No permitir bypass (aplica también a administradores).

---

**¿Puedo escribir el mensaje del commit en español?**
Sí. La descripción del commit puede ir en español, pero el tipo y el formato deben respetarse tal como se indica (`feat`, `fix`, `docs`, etc.).

---

*Última actualización: Abril 2026 — Equipo ARYA, Universidad del Cauca.*

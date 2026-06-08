# Convenciones de documentación — Al Toro Gastrobar

Guía de estilo para todos los documentos técnicos del proyecto. Aplica a archivos `.md` en `backend/docs/`, `README.md` y `CONTRIBUTING.md`.

---

## Estructura de un documento

Todo documento sigue esta plantilla base:

```
# Título del documento — subtítulo si aplica

Descripción del documento (qué es, para qué sirve).

--- 
# Tabla de contenido
- [X](#x)
---

## Sección principal

Párrafo introductorio si la sección lo requiere.

### Subsección

Contenido: tabla, viñetas o bloque de código según corresponda.

---

## Siguiente sección
```

### Reglas de estructura

1. Separador `---` entre secciones de nivel 2
2. Títulos con mayúscula solo en la primera palabra, excepto nombres propios, siglas y nombres de herramientas (`Spring Boot`, `JWT`)
3. Tablas cuando hay dos o más atributos comparables
4. Viñetas cuando son ítems sin atributos secundarios
5. Bloques de código con lenguaje especificado (`bash`, `java`, `yaml`, `sql`)
6. Negritas solo para énfasis crítico: advertencias, nombres de clases clave, términos definidos por primera vez
7. Sin emojis en ninguna parte del documento

---

## Convención de redacción

La documentación usa modo mixto: formal descriptivo para explicaciones, telegráfico para reglas y tablas.

### Cuándo usar cada modo

| Contexto | Modo |
|----------|------|
| Descripciones de módulos, arquitectura y propósito | Formal descriptivo |
| Reglas, restricciones y advertencias | Telegráfico imperativo |
| Pasos de comandos o procedimientos | Telegráfico imperativo |
| Celdas de tablas y viñetas de atributos | Telegráfico nominal |

---

### Modo formal descriptivo

Se usa en párrafos de introducción, descripciones de módulos y explicaciones de flujos.

**Reglas:**

1. Oraciones completas con sujeto, verbo y complemento
2. Tercera persona — nunca primera persona ("el sistema valida", no "validamos")
3. Presente indicativo — no futuro ("el servicio emite", no "el servicio emitirá")
4. Máximo dos oraciones por párrafo
5. **No** usar gerundios como verbo principal ("valida y emite", no "validando y emitiendo")

**Ejemplo correcto:**
> El módulo `auth` gestiona el ciclo de vida de las sesiones de usuario. Emite tokens JWT firmados y rechaza intentos de sesión simultánea para roles operativos.

**Ejemplo incorrecto:**
> En este módulo básicamente estamos manejando todo lo relacionado con autenticación, validando credenciales y emitiendo tokens para que los usuarios puedan acceder al sistema.

---

### Modo telegráfico imperativo

Se usa en reglas numeradas, advertencias y pasos de procedimientos.

**Reglas:**

1. Verbo en infinitivo o imperativo al inicio — sin sujeto
2. Sin artículos innecesarios
3. Una sola idea por línea
4. **Negritas** solo en la palabra crítica de la regla

**Ejemplo correcto:**
> 1. **Nunca** implementar funcionalidad sin tests
> 2. Ejecutar `./mvnw clean test` antes de cada commit
> 3. Cubrir todas las ramas de `if/else` y `try/catch`

**Ejemplo incorrecto:**
> 1. Es importante que siempre se implementen tests antes de hacer commits
> 2. Se debe ejecutar el comando de tests
> 3. Hay que cubrir los casos de las ramas condicionales

---

### Modo telegráfico nominal

Se usa dentro de celdas de tablas y viñetas de atributos o responsabilidades.

**Reglas:**

1. Sustantivo o verbo en tercera persona sin sujeto explícito
2. Sin punto final
3. Sin artículos al inicio de la celda
4. Verbos de acción: "Valida", "Emite", "Consolida", "Publica", "Coordina"

**Ejemplo correcto:**

| Capa | Responsabilidad |
|------|-----------------|
| Controller | Recibe petición y delega al service |
| Service | Valida reglas de negocio y coordina repositorios |
| Repository | Ejecuta consultas contra la base de datos |

**Ejemplo incorrecto:**

| Capa | Responsabilidad |
|------|-----------------|
| Controller | Es el encargado de recibir las peticiones que llegan |
| Service | Se encarga de la lógica de negocio del sistema |
| Repository | Es donde se hacen las consultas a la base de datos |

---

## Prohibiciones globales

1. Sin emojis en ningún contexto
2. Sin coloquialismos ("básicamente", "simplemente", "o sea", "en este caso")
3. Sin voz pasiva cuando hay agente claro ("el service valida", no "la validación es realizada por el service")
4. Sin mezclar idiomas en una misma oración
5. Sin anglicismos cuando existe término en español — usar "contraseña", no "password"; "módulo", no "module"; "rama", no "branch" en prosa

### Términos técnicos que se mantienen en inglés

Estos términos no se traducen porque son nombres de herramienta o conceptos sin equivalente exacto en el contexto del proyecto:

`token`, `endpoint`, `payload`, `commit`, `branch`, `pull request`, `JWT`, `WebSocket`, `STOMP`, `Docker`, `Spring Boot`, `controller`, `service`, `repository`, `mapper`, `DTO`, `middleware`, `seed`

---

## Referencia rápida

| Situación | Qué hacer |
|-----------|-----------|
| Describir qué hace un módulo | Formal descriptivo, presente, tercera persona |
| Listar responsabilidades en tabla | Telegráfico nominal, sin punto final |
| Escribir una regla o advertencia | Telegráfico imperativo, verbo al inicio |
| Documentar un comando | Bloque de código `bash` con comentario descriptivo |
| Mencionar una clase o archivo | Backticks: `NombreClase`, `archivo.md` |
| Mencionar una herramienta | Nombre oficial: `Spring Boot`, `PostgreSQL`|
| Señalar algo crítico | **Negrita** en la palabra clave, no en la oración completa |

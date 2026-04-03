# \# Al Toro Gastrobar — Sistema Integral

# 

# Sistema web multiplataforma para la gestión operativa del restaurante Al Toro Gastrobar,

# desarrollado por el equipo ARYA como proyecto de Ingeniería de Sistemas — Universidad del Cauca.

# 

# \---

# 

# \## Tecnologías

# 

# | Capa | Tecnología |

# |------|------------|

# | Frontend | Angular (TypeScript) |

# | Backend | Java 17 + Spring Boot |

# | Base de datos | PostgreSQL |

# | Gestión de tareas | Jira (proyecto `PA`) |

# | Control de versiones | Git + GitHub |

# 

# \---

# 

# \## Estructura del repositorio

# 

# ```

# al\_toro\_gastrobar/

# ├── frontend/          # Aplicación Angular

# ├── backend/           # API REST con Spring Boot

# ├── docs/              # Documentación del proyecto

# ├── .github/

# │   └── workflows/

# │       └── validar-rama.yml   # Valida nombres de rama en PRs

# ├── .gitignore

# └── README.md

# ```

# 

# \---

# 

# \## Módulos del sistema

# 

# | Módulo | Épica | Actores principales |

# |--------|-------|---------------------|

# | Autenticación y perfiles | HE-01 | Todos los roles |

# | Reservas y consumo | HE-02 | Cliente |

# | Mesas y comandas | HE-03 | Mesero, Cajero |

# | Producción | HE-04 | Cocinero, Bartender |

# | Pagos y caja | HE-05 | Cajero |

# | Histórico y reportes | HE-06 | Administrador |

# | Inventario y decoraciones | HE-07 | Administrador |

# | Personal y clientes | HE-08 | Administrador |

# 

# \---

# 

# \## Equipo ARYA

# 

# | Nombre | Rol en Scrum |

# |--------|--------------|

# | Paula Andrea Muñoz Delgado | Scrum Master, Desarrollador, Analista, Tester |

# | Adrián Camilo Bergaño Ortega | Desarrollador |

# | Yeixón Julián Gembuel Ciclos | Tester |

# | Rubeiro Romero | Desarrollador, Analista |

# 

# \---

# 

# \## Flujo de trabajo Git

# 

# \### Ramas permanentes

# 

# | Rama | Propósito |

# |------|-----------|

# | `main` | Código estable y aprobado — protegida |

# | `develop` | Integración continua del equipo — protegida |

# 

# \### Convención de nombres de ramas

# 

# ```

# PA-{numero}-{descripcion-con-guiones}

# ```

# 

# El número corresponde al issue de la subtarea asignada en Jira (proyecto `PA`).

# El formato es validado automáticamente por GitHub Actions al abrir un Pull Request.

# 

# \*\*Ejemplos:\*\*

# ```

# PA-11-ingresar-al-sistema-back

# PA-12-ingresar-al-sistema-front

# PA-26-crear-cuenta-cliente

# ```

# 

# \### Convención de commits

# 

# ```

# tipo(scope): descripción corta

# 

# Tipos: feat | fix | docs | style | refactor | test | chore

# ```

# 

# \---

# 

# \## Gestión del proyecto

# 

# Las historias de usuario y subtareas se gestionan en Jira bajo el proyecto `PA`.

# El sistema cuenta con 8 épicas distribuidas en sprints de 2 semanas bajo metodología Scrum.


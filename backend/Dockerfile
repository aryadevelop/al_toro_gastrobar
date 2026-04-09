# ============================================================
# STAGE 1: BUILD
# ============================================================

# Usar la imagen oficial de Maven con Java 21.
FROM maven:3.9-eclipse-temurin-21 AS builder

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar primero solo el pom.xml para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el codigo fuente y compilar
COPY src ./src
RUN mvn package -DskipTests -B

# ============================================================
# STAGE 2: RUNTIME
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root por seguridad.
RUN addgroup -S altoro && adduser -S altoro -G altoro
USER altoro

# Copiar solo el JAR generado en el stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Puerto en el que escucha Spring Boot
EXPOSE 8080

# Comando de inicio.
ENTRYPOINT ["java", "-jar", "app.jar"]
